type VercelRequest = {
  method?: string;
  query: Record<string, string | string[] | undefined>;
};

type VercelResponse = {
  status(code: number): VercelResponse;
  setHeader(name: string, value: string): void;
  json(body: unknown): VercelResponse;
};

declare const process: { env: Record<string, string | undefined> };

function first(value: string | string[] | undefined): string {
  return Array.isArray(value) ? value[0] ?? "" : value ?? "";
}

export default async function handler(req: VercelRequest, res: VercelResponse) {
  res.setHeader("Cache-Control", "private, no-store");
  res.setHeader("X-Content-Type-Options", "nosniff");

  if (req.method !== "GET") {
    res.setHeader("Allow", "GET");
    return res.status(405).json({ error: "Method not allowed." });
  }

  const apiKey = (process.env.SERPAPI_API_KEY ?? process.env.SERP_API)?.trim();
  if (!apiKey) {
    return res.status(503).json({ error: "Search service is not configured." });
  }

  const query = first(req.query.q).trim();
  const provider = first(req.query.provider) || "google_images";
  const imageUrl = first(req.query.imageUrl).trim();
  const start = Number(first(req.query.start) || "1");
  const num = Number(first(req.query.num) || "100");
  const highQuality = first(req.query.highQuality) !== "false";
  const imageQuality = first(req.query.quality).toUpperCase() || "HIGH";
  const safe = first(req.query.safe) === "active" ? "active" : "off";

  if (query.length < 2 || query.length > 150) {
    if (!(provider === "yandex_reverse" && query.length === 0)) {
      return res.status(400).json({ error: "Query must be between 2 and 150 characters." });
    }
  }
  const engineByProvider: Record<string, string> = {
    google: "google",
    google_images: "google_images",
    yandex_images: "yandex_images",
    yandex_reverse: "yandex_images",
    tiktok_discover: "tiktok_discover",
  };
  const engine = engineByProvider[provider];
  if (!engine) return res.status(400).json({ error: "Invalid search provider." });
  if (provider === "yandex_reverse") {
    try {
      const parsedImageUrl = new URL(imageUrl);
      if (parsedImageUrl.protocol !== "https:" || imageUrl.length > 2048) {
        throw new Error("Invalid URL");
      }
    } catch {
      return res.status(400).json({ error: "Reverse image search requires a valid HTTPS image URL." });
    }
  }
  if (!Number.isInteger(start) || start < 1 || start > 9901 ||
      !Number.isInteger(num) || num !== 100) {
    return res.status(400).json({ error: "Invalid pagination values." });
  }
  const imageSizeByQuality: Record<string, string> = {
    ANY: "",
    LOW: "i",
    MEDIUM: "m",
    HIGH: "l",
    ULTRA: "4mp",
  };
  const imageSize = imageSizeByQuality[imageQuality];
  if (imageQuality !== "ANY" && !imageSize) {
    return res.status(400).json({ error: "Invalid image quality." });
  }

  const pageSize = provider === "yandex_images" || provider === "yandex_reverse" ? 30 : num;
  const page = Math.floor((start - 1) / pageSize);
  const offset = (start - 1) % pageSize;
  const isImageSearch = provider === "google_images" || provider === "yandex_images" ||
    provider === "yandex_reverse";
  const searchUrl = new URL("https://serpapi.com/search.json");
  searchUrl.searchParams.set("engine", engine);
  searchUrl.searchParams.set("api_key", apiKey);
  if (provider === "yandex_reverse") {
    searchUrl.searchParams.set("url", imageUrl);
    searchUrl.searchParams.set("tab", "similar");
  } else if (provider === "yandex_images") {
    searchUrl.searchParams.set("text", query);
  } else {
    searchUrl.searchParams.set("q", query);
  }
  if (isImageSearch) {
    searchUrl.searchParams.set(provider === "google_images" ? "ijn" : "p", String(page));
    if (provider === "google_images") {
      searchUrl.searchParams.set("hl", "pt-br");
      searchUrl.searchParams.set("gl", "br");
      searchUrl.searchParams.set("safe", safe);
      if (highQuality && imageSize) searchUrl.searchParams.set("imgsz", imageSize);
    } else if (provider === "yandex_images" || provider === "yandex_reverse") {
      searchUrl.searchParams.set("family_mode", safe === "active" ? "2" : "0");
    }
  } else if (provider === "google") {
    searchUrl.searchParams.set("safe", safe);
  }

  try {
    const response = await fetch(searchUrl, { signal: AbortSignal.timeout(15000) });
    const body = await response.json().catch(() => null) as {
      images_results?: Array<{
        title?: string;
        original?: string;
        thumbnail?: string;
        source?: string;
        original_width?: number;
        original_height?: number;
      }>;
      visual_matches?: Array<{
        title?: string;
        link?: string;
        thumbnail?: string;
        source?: string;
      }>;
      image_results?: Array<{
        title?: string;
        original?: string;
        link?: string;
        thumbnail?: string;
        source?: string;
        original_width?: number;
        original_height?: number;
      }>;
      organic_results?: Array<{
        title?: string;
        link?: string;
        snippet?: string;
        thumbnail?: string;
      }>;
      video_results?: Array<{
        description?: string;
        link?: string;
        thumbnail?: string;
        author?: { username?: string; name?: string; link?: string };
      }>;
      serpapi_pagination?: { next?: string };
      error?: string;
    } | null;
    if (!response.ok) {
      const status = response.status === 429 ? 429 : 502;
      const reason = response.status === 401 ? "unauthorized" :
        response.status === 403 ? "forbidden" :
        response.status === 429 ? "quota_exceeded" : "upstream_error";
      console.error("SerpApi search failed", { provider, status: response.status, reason });
      return res.status(status).json({
        error: response.status === 429
          ? "Search quota exceeded."
          : `Search request failed (${reason}).`,
      });
    }
    if (body?.error) {
      console.error("SerpApi image search returned an error response.");
      return res.status(502).json({ error: "SerpApi could not complete the search." });
    }

    const rawResults = provider === "google"
      ? body?.organic_results ?? []
      : provider === "tiktok_discover"
        ? body?.video_results ?? []
        : provider === "yandex_reverse"
          ? body?.visual_matches ?? body?.image_results ?? body?.images_results ?? []
          : body?.images_results ?? [];
    const items = rawResults.slice(offset, offset + pageSize).flatMap((item) => {
      const imageResult = item as {
        title?: string;
        original?: string;
        thumbnail?: string;
        source?: string;
        original_width?: number;
        original_height?: number;
        link?: string;
        snippet?: string;
        description?: string;
        author?: { username?: string; name?: string; link?: string };
      };
      const link = (imageResult.original ?? imageResult.link)?.trim();
      if (!link || !link.startsWith("https://")) return [];
      const previewUrl = imageResult.thumbnail?.trim();
      return [{
        title: imageResult.title ?? imageResult.description ?? imageResult.author?.name ?? "",
        link,
        ...(previewUrl?.startsWith("https://") ? { previewUrl } : {}),
        displayLink: imageResult.source ?? imageResult.author?.username ?? provider,
        description: imageResult.snippet ?? imageResult.description ?? "",
        image: {
          width: imageResult.original_width ?? 0,
          height: imageResult.original_height ?? 0,
        },
      }];
    });
    const nextStartIndex = isImageSearch && body?.serpapi_pagination?.next
      ? start - offset + pageSize
      : undefined;

    res.setHeader(
      "Cache-Control",
      provider === "yandex_reverse"
        ? "private, no-store"
        : "s-maxage=60, stale-while-revalidate=300"
    );
    return res.status(200).json({
      items,
      queries: { nextPage: nextStartIndex ? [{ startIndex: nextStartIndex }] : [] },
    });
  } catch {
    return res.status(502).json({ error: "Image search is temporarily unavailable." });
  }
}
