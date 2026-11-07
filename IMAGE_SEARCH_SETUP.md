# Premium image search setup

The first live provider is Google Images through SerpApi. Premium Android searches call this project's Vercel function; SerpApi credentials stay on Vercel and are never included in the APK. Reverse-image providers (Google Lens/Yandex) are separate integrations and are not enabled by this first implementation.

1. Create a SerpApi account and choose a plan. The current public pricing lists 250 searches/month free and a paid plan from US$25/month for 1,000 searches; confirm current terms and billing in your account.
2. In this Vercel project's **Settings → Environment Variables**, set the SerpApi key as `SERPAPI_API_KEY` or `SERP_API` for Production. This implementation accepts either name; never place the key in this repository, an Android `.env`, or the APK. Redeploy after changing variables.
3. Test `https://tiktoklens-premium-api.vercel.app/api/search?q=test&num=100`. A JSON response with `items` means the provider call succeeded. HTTP 503 means the server key is missing; HTTP 502 may mean the provider rejected the key or the account; HTTP 429 means the account quota/rate limit was reached.
4. In the app, open Premium → **Configurar**, enter `https://tiktoklens-premium-api.vercel.app` (base URL only, no `/api/search`), then select **Aplicar Configuração**.
5. To set a different build-time default, copy `app/.env.example` to `app/.env`, set `TIKTOKLENS_SEARCH_API_URL`, then rebuild. `app/.env` is ignored by Git.

The function validates query and pagination, requests Google Images through SerpApi, asks for large images when selected, and maps only provider-returned image URLs. Results are paged in batches of up to 100. API failures and empty results are never replaced with sample images.

**Important:** the Vercel route is publicly reachable by anyone who knows its URL. The SerpApi key stays server-side, but public requests can consume its quota and paid credits. Monitor usage and billing; add rate limiting or access controls before distributing widely.
