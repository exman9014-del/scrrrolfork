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
  const start = Number(first(req.query.start) || "1");
  const num = Number(first(req.query.num) || "100");
  const highQuality = first(req.query.highQuality) !== "false";
  const safe = first(req.query.safe) === "active" ? "active" : "off";

  if (query.length < 2 || query.length > 150) {
    return res.status(400).json({ error: "Query must be between 2 and 150 characters." });
  }
  if (!Number.isInteger(start) || start < 1 || start > 9901 ||
      !Number.isInteger(num) || num !== 100) {
    return res.status(400).json({ error: "Invalid pagination values." });
  }

  const pageSize = num;
  const page = Math.floor((start - 1) / pageSize);
  const offset = (start - 1) % pageSize;
  const searchUrl = new URL("https://serpapi.com/search.json");
  searchUrl.searchParams.set("engine", "google_images");
  searchUrl.searchParams.set("api_key", apiKey);
  searchUrl.searchParams.set("q", query);
  searchUrl.searchParams.set("ijn", String(page));
  searchUrl.searchParams.set("hl", "pt-br");
  searchUrl.searchParams.set("gl", "br");
  searchUrl.searchParams.set("safe", safe);
  if (highQuality) searchUrl.searchParams.set("imgsz", "l");

  try {
    const response = await fetch(searchUrl, { signal: AbortSignal.timeout(15000) });
    const body = await response.json().catch(() => null) as {
      images_results?: Array<{
        title?: string;
        original?: string;
        source?: string;
        original_width?: number;
        original_height?: number;
      }>;
      serpapi_pagination?: { next?: string };
      error?: string;
    } | null;
    if (!response.ok) {
      const status = response.status === 429 ? 429 : 502;
      const reason = response.status === 401 ? "unauthorized" :
        response.status === 403 ? "forbidden" :
        response.status === 429 ? "quota_exceeded" : "upstream_error";
      console.error("SerpApi image search failed", { status: response.status, reason });
      return res.status(status).json({
        error: response.status === 429
          ? "Image search quota exceeded."
          : `Image search request failed (${reason}).`,
      });
    }
    if (body?.error) {
      console.error("SerpApi image search returned an error response.");
      return res.status(502).json({ error: "SerpApi could not complete the image search." });
    }

    const results = Array.isArray(body?.images_results) ? body.images_results : [];
    const items = results.slice(offset, offset + pageSize).flatMap((item) => {
      const link = item.original?.trim();
      if (!link || !link.startsWith("https://")) return [];
      return [{
        title: item.title ?? "",
        link,
        displayLink: item.source ?? "Google Images",
        image: {
          width: item.original_width ?? 0,
          height: item.original_height ?? 0,
        },
      }];
    });
    const nextStartIndex = body?.serpapi_pagination?.next
      ? start - offset + pageSize
      : undefined;

    res.setHeader("Cache-Control", "s-maxage=60, stale-while-revalidate=300");
    return res.status(200).json({
      items,
      queries: { nextPage: nextStartIndex ? [{ startIndex: nextStartIndex }] : [] },
    });
  } catch {
    return res.status(502).json({ error: "Image search is temporarily unavailable." });
  }
}
