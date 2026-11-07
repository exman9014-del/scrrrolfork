import assert from "node:assert/strict";
import { test } from "node:test";
import handler from "./search.ts";

function createResponse() {
  return {
    code: 200,
    body: undefined,
    headers: {},
    status(code) {
      this.code = code;
      return this;
    },
    setHeader(name, value) {
      this.headers[name] = value;
    },
    json(body) {
      this.body = body;
      return this;
    },
  };
}

test("SerpApi providers map their results and validate inputs", async () => {
  const previousKey = process.env.SERPAPI_API_KEY;
  const previousFetch = globalThis.fetch;
  process.env.SERPAPI_API_KEY = "test-only-key";
  let upstreamUrl;
  let upstreamBody;
  globalThis.fetch = async (input) => {
    upstreamUrl = String(input);
    return {
      ok: true,
      status: 200,
      json: async () => upstreamBody,
    };
  };

  try {
    const cases = [
      {
        provider: "google",
        query: "creator",
        engine: "google",
        parameter: "q",
        item: { title: "Google result", link: "https://site.test/a", snippet: "Web result" },
        payload: { organic_results: [{ title: "Google result", link: "https://site.test/a", snippet: "Web result" }] },
      },
      {
        provider: "google_images",
        query: "creator",
        engine: "google_images",
        parameter: "q",
        item: { title: "Google image", link: "https://img.test/a.jpg", previewUrl: "https://thumb.test/a.jpg" },
        payload: { images_results: [{ title: "Google image", original: "https://img.test/a.jpg", thumbnail: "https://thumb.test/a.jpg" }] },
      },
      {
        provider: "yandex_images",
        query: "creator",
        engine: "yandex_images",
        parameter: "text",
        item: { title: "Yandex image", link: "https://img.test/b.jpg", previewUrl: "https://thumb.test/b.jpg" },
        payload: { images_results: [{ title: "Yandex image", original: "https://img.test/b.jpg", thumbnail: "https://thumb.test/b.jpg" }] },
      },
      {
        provider: "yandex_reverse",
        query: "",
        imageUrl: "https://img.test/input.jpg",
        engine: "yandex_images",
        parameter: "url",
        item: { title: "Similar image", link: "https://site.test/match", previewUrl: "https://thumb.test/match.jpg" },
        nextStart: 31,
        payload: {
          visual_matches: [{ title: "Similar image", link: "https://site.test/match", thumbnail: "https://thumb.test/match.jpg" }],
          serpapi_pagination: { next: "https://serpapi.com/next" },
        },
      },
      {
        provider: "tiktok_discover",
        query: "dance",
        engine: "tiktok_discover",
        parameter: "q",
        item: { title: "Dance video", link: "https://www.tiktok.com/video/1", previewUrl: "https://thumb.test/video.jpg" },
        payload: { video_results: [{ description: "Dance video", link: "https://www.tiktok.com/video/1", thumbnail: "https://thumb.test/video.jpg" }] },
      },
    ];

    for (const entry of cases) {
      upstreamBody = entry.payload;
      const requestQuery = {
        provider: entry.provider,
        q: entry.query,
        num: "100",
        start: "1",
        quality: "ULTRA",
        highQuality: "true",
      };
      if (entry.imageUrl) requestQuery.imageUrl = entry.imageUrl;
      const response = createResponse();
      await handler({ method: "GET", query: requestQuery }, response);
      assert.equal(response.code, 200, entry.provider);
      const requestUrl = new URL(upstreamUrl);
      assert.equal(requestUrl.searchParams.get("engine"), entry.engine);
      assert.equal(requestUrl.searchParams.get(entry.parameter), entry.imageUrl ?? entry.query);
      if (entry.provider === "google_images") {
        assert.equal(requestUrl.searchParams.get("imgsz"), "4mp");
      }
      assert.equal(response.body.items.length, 1, entry.provider);
      assert.equal(response.body.items[0].link, entry.item.link);
      assert.equal(response.body.items[0].previewUrl, entry.item.previewUrl);
      if (entry.nextStart) assert.equal(response.body.queries.nextPage[0].startIndex, entry.nextStart);
      assert.equal(JSON.stringify(response.body).includes("test-only-key"), false);
    }

    upstreamBody = {
      images_results: [{ title: "Default image", original: "https://img.test/default.jpg" }],
      serpapi_pagination: { next: "https://serpapi.com/next" },
    };
    const legacyDefault = createResponse();
    await handler({
      method: "GET",
      query: { q: "creator", num: "100", start: "101" },
    }, legacyDefault);
    const legacyRequest = new URL(upstreamUrl);
    assert.equal(legacyRequest.searchParams.get("engine"), "google_images");
    assert.equal(legacyRequest.searchParams.get("ijn"), "1");
    assert.equal(legacyDefault.body.queries.nextPage[0].startIndex, 201);

    upstreamBody = { visual_matches: [{ title: "Similar", link: "https://site.test/match" }] };
    const reverseResult = createResponse();
    await handler({
      method: "GET",
      query: {
        provider: "yandex_reverse",
        imageUrl: "https://img.test/input.jpg",
        num: "100",
        start: "1",
      },
    }, reverseResult);
    assert.equal(reverseResult.headers["Cache-Control"], "private, no-store");

    const invalidProvider = createResponse();
    await handler({ method: "GET", query: { provider: "unknown", q: "test" } }, invalidProvider);
    assert.equal(invalidProvider.code, 400);

    const invalidReverseUrl = createResponse();
    await handler({
      method: "GET",
      query: { provider: "yandex_reverse", imageUrl: "http://images.test/photo.jpg" },
    }, invalidReverseUrl);
    assert.equal(invalidReverseUrl.code, 400);

    const invalidQuality = createResponse();
    await handler({
      method: "GET",
      query: { provider: "google_images", q: "test", quality: "unlimited" },
    }, invalidQuality);
    assert.equal(invalidQuality.code, 400);

    delete process.env.SERPAPI_API_KEY;
    const missingKey = createResponse();
    await handler({ method: "GET", query: { provider: "google", q: "test" } }, missingKey);
    assert.equal(missingKey.code, 503);
  } finally {
    globalThis.fetch = previousFetch;
    if (previousKey === undefined) delete process.env.SERPAPI_API_KEY;
    else process.env.SERPAPI_API_KEY = previousKey;
  }
});
