# Premium image search setup

Premium Android searches call this project's Vercel function; SerpApi credentials stay on Vercel and are never included in the APK. The configured backend supports Google Images plus the provider cards listed below. Reverse image lookup is via Yandex Images' reverse-search parameters; Google Lens is not configured.

1. Create a SerpApi account and choose a plan. The current public pricing lists 250 searches/month free and a paid plan from US$25/month for 1,000 searches; confirm current terms and billing in your account.
2. In this Vercel project's **Settings → Environment Variables**, set the SerpApi key as `SERPAPI_API_KEY` or `SERP_API` for Production. This implementation accepts either name; never place the key in this repository, an Android `.env`, or the APK. Redeploy after changing variables.
3. Test `https://tiktoklens-premium-api.vercel.app/api/search?q=test&num=100`. A JSON response with `items` means the provider call succeeded. HTTP 503 means the server key is missing; HTTP 502 may mean the provider rejected the key or the account; HTTP 429 means the account quota/rate limit was reached.
4. In the app, open Premium → **Configurar**, enter `https://tiktoklens-premium-api.vercel.app` (base URL only, no `/api/search`), then select **Aplicar Configuração**.
5. To set a different build-time default, copy `app/.env.example` to `app/.env`, set `TIKTOKLENS_SEARCH_API_URL`, then rebuild. `app/.env` is ignored by Git.

The function validates query, pagination, and the selected image-size filter (`LOW`, `MEDIUM`, `HIGH`, or `ULTRA`), requests Google Images through SerpApi, and maps only provider-returned image URLs. Premium uses the chosen size as a search preference; actual dimensions and whether an image can be embedded are controlled by the source and provider. The grid uses the provider thumbnail when available, while fullscreen first tries the original and falls back to that thumbnail if loading fails. Results are paged in batches of up to 100. SafeSearch is off by default unless enabled in the app. API failures and empty results are never replaced with sample images.

The platform selector also includes X. With the overlay/accessibility service enabled, opening X in its app or in a browser detected by the service lets the app read visible post handles and search Google Images for that handle plus “X Twitter”. This depends on X exposing the author text to Android accessibility; posts whose author is not exposed cannot be detected automatically.

The Premium dashboard also exposes SerpApi cards for Google Search, Google Images, Yandex Images, Yandex Reverse, and TikTok Discover. Text-search cards accept a query; Yandex Reverse requires a publicly accessible HTTPS image URL. These providers use the same server-side SerpApi key. SerpApi account plan/engine access, quotas, and provider result availability still apply. The card lock is an app UI gate only; this project does not authenticate Premium entitlements at the Vercel API, so add server-side user authentication and authorization before treating it as a billing/security boundary. The API route is publicly reachable and requests may consume the configured account quota.

Run the API route tests with Node.js 24+: `node --experimental-strip-types --test api/search.test.mjs`.

**Important:** the Vercel route is publicly reachable by anyone who knows its URL. The SerpApi key stays server-side, but public requests can consume its quota and paid credits. Monitor usage and billing; add rate limiting or access controls before distributing widely.
