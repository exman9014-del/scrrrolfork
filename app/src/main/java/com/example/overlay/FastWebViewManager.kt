package com.example.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import com.example.data.ScrollSearchStateBus
import com.example.util.GoogleSearchHelper
import com.example.util.ImageDownloader
import org.json.JSONArray
import org.json.JSONTokener

class FastWebViewManager(private val context: Context) {

    private var webView: WebView? = null
    private var currentUrl: String? = null
    private var currentQuery: String? = null
    private var lastLoadTimestamp: Long = 0L
    private val mainHandler = Handler(Looper.getMainLooper())

    private var isPureMode: Boolean = false
    private var currentQualityParam: String = ""
    private var isUsingAlternativeEngine: Boolean = false

    var onPageStartedListener: (() -> Unit)? = null
    var onPageFinishedListener: (() -> Unit)? = null
    var onErrorListener: ((String) -> Unit)? = null
    var onImagesExtractedListener: ((List<String>) -> Unit)? = null

    init {
        initWebView()
    }

    private fun captureBitmapFromWebView(left: Float, top: Float, width: Float, height: Float, dpr: Float): Bitmap? {
        val wv = webView ?: return null
        if (wv.width <= 0 || wv.height <= 0 || width <= 0 || height <= 0) return null
        return try {
            val fullBmp = Bitmap.createBitmap(wv.width, wv.height, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(fullBmp)
            wv.draw(canvas)

            val density = wv.resources.displayMetrics.density
            val cropX = (left * density).toInt().coerceIn(0, wv.width - 1)
            val cropY = (top * density).toInt().coerceIn(0, wv.height - 1)
            val cropW = (width * density).toInt().coerceIn(1, wv.width - cropX)
            val cropH = (height * density).toInt().coerceIn(1, wv.height - cropY)

            val cropped = Bitmap.createBitmap(fullBmp, cropX, cropY, cropW, cropH)
            fullBmp.recycle()
            cropped
        } catch (e: Exception) {
            Log.e("FastWebViewManager", "Failed to capture bitmap from WebView: ${e.message}")
            null
        }
    }

    private fun parseUrlsJson(allImagesJson: String?): List<String> {
        val urls = mutableListOf<String>()
        if (!allImagesJson.isNullOrBlank()) {
            try {
                val unescaped = if (allImagesJson.startsWith("\"") && allImagesJson.endsWith("\"")) {
                    JSONTokener(allImagesJson).nextValue().toString()
                } else {
                    allImagesJson
                }
                val arr = JSONArray(unescaped)
                for (i in 0 until arr.length()) {
                    val u = arr.optString(i)
                    if (u.isNotBlank()) urls.add(u)
                }
            } catch (e: Exception) {
                Log.e("FastWebViewManager", "Error parsing allImagesJson: ${e.message}")
            }
        }
        return urls
    }

    private inner class ScrollSearchBridge {
        @JavascriptInterface
        fun onReceiveImages(jsonString: String) {
            try {
                val clean = if (jsonString.startsWith("\"") && jsonString.endsWith("\"")) {
                    JSONTokener(jsonString).nextValue().toString()
                } else {
                    jsonString
                }
                val array = JSONArray(clean)
                val urls = mutableListOf<String>()
                for (i in 0 until array.length()) {
                    val url = array.optString(i)
                    if (url.isNotBlank() && (url.startsWith("http") || url.startsWith("data:image"))) {
                        urls.add(url)
                    }
                }
                if (urls.isNotEmpty()) {
                    mainHandler.post {
                        onImagesExtractedListener?.invoke(urls)
                    }
                }
            } catch (e: Exception) {
                Log.e("FastWebViewManager", "Bridge JSON error: ${e.message}")
            }
        }

        @JavascriptInterface
        fun onCaptureAndOpenFullscreenWithIndex(
            left: Float,
            top: Float,
            width: Float,
            height: Float,
            dpr: Float,
            imageUrl: String,
            allImagesJson: String? = null,
            index: Int = -1
        ) {
            mainHandler.post {
                val captured = captureBitmapFromWebView(left, top, width, height, dpr)
                val urls = parseUrlsJson(allImagesJson)
                ScrollSearchStateBus.openFullscreenWithBitmap(captured, imageUrl.trim(), urls, index)
            }
        }

        @JavascriptInterface
        fun onCaptureAndOpenFullscreen(
            left: Float,
            top: Float,
            width: Float,
            height: Float,
            dpr: Float,
            imageUrl: String,
            allImagesJson: String? = null
        ) {
            mainHandler.post {
                val captured = captureBitmapFromWebView(left, top, width, height, dpr)
                val urls = parseUrlsJson(allImagesJson)
                ScrollSearchStateBus.openFullscreenWithBitmap(captured, imageUrl.trim(), urls, -1)
            }
        }

        @JavascriptInterface
        fun onCaptureAndDownload(
            left: Float,
            top: Float,
            width: Float,
            height: Float,
            dpr: Float,
            fallbackUrl: String?,
            title: String? = null
        ) {
            mainHandler.post {
                val captured = captureBitmapFromWebView(left, top, width, height, dpr)
                if (captured != null) {
                    ImageDownloader.saveBitmapToGallery(context, captured, title ?: "ScrollSearch_GooglePhoto")
                } else if (!fallbackUrl.isNullOrBlank()) {
                    val clean = fallbackUrl.trim()
                    if (clean.startsWith("data:image")) {
                        ImageDownloader.downloadBase64(context, clean, title ?: "ScrollSearch_GooglePhoto")
                    } else {
                        ImageDownloader.downloadPhoto(context, clean, "https://www.google.com/", title ?: "ScrollSearch_GooglePhoto")
                    }
                }
            }
        }

        @JavascriptInterface
        fun onDownloadBase64(base64Data: String, title: String? = null) {
            mainHandler.post {
                val clean = base64Data.trim()
                if (clean.isNotBlank()) {
                    ImageDownloader.downloadBase64(context, clean, title ?: "ScrollSearch Photo")
                }
            }
        }

        @JavascriptInterface
        fun onDownloadImageWithReferrer(imageUrl: String, referrer: String? = null, title: String? = null) {
            mainHandler.post {
                val cleanUrl = imageUrl.trim()
                if (cleanUrl.isNotBlank()) {
                    ImageDownloader.downloadPhoto(context, cleanUrl, referrer ?: "https://www.google.com/", title ?: "ScrollSearch Photo")
                }
            }
        }

        @JavascriptInterface
        fun onDownloadImage(imageUrl: String) {
            mainHandler.post {
                val clean = imageUrl.trim()
                if (clean.isNotBlank()) {
                    ImageDownloader.downloadPhoto(context, clean, "https://www.google.com/", "ScrollSearch Photo")
                }
            }
        }

        @JavascriptInterface
        fun onOpenFullscreen(imageUrl: String, allImagesJson: String? = null) {
            mainHandler.post {
                val cleanUrl = imageUrl.trim()
                val urls = parseUrlsJson(allImagesJson)
                ScrollSearchStateBus.openFullscreenPhoto(cleanUrl, urls)
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initWebView() {
        try {
            val wv = WebView(context.applicationContext).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                setLayerType(View.LAYER_TYPE_HARDWARE, null)
                setBackgroundColor(0xFF12121E.toInt())
                isVerticalScrollBarEnabled = true
                isHorizontalScrollBarEnabled = false
                scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY
                overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS

                // 1. Enable cookies and inject Google & DuckDuckGo cookies
                val cookieManager = CookieManager.getInstance()
                cookieManager.setAcceptCookie(true)
                cookieManager.setAcceptThirdPartyCookies(this, true)
                try {
                    cookieManager.setCookie("https://www.google.com", "CONSENT=YES+cb.20230531-04-p0.pt-BR+FX+999; domain=.google.com; path=/; SameSite=None; Secure")
                    cookieManager.setCookie("https://www.google.com", "SOCS=CAISHAgBEhJnd3NfMjAyNDA2MTAtMF9SQzIaAmVuIAEaBgoFDICw_hM; domain=.google.com; path=/; SameSite=None; Secure")
                    cookieManager.setCookie("https://www.google.com", "safe=off; domain=.google.com; path=/; SameSite=None; Secure")
                    // Disable SafeSearch on DuckDuckGo and Bing by default
                    cookieManager.setCookie("https://duckduckgo.com", "kp=-2; domain=.duckduckgo.com; path=/; SameSite=None; Secure")
                    cookieManager.setCookie("https://www.bing.com", "SRCHHPGUSR=ADLT=OFF; domain=.bing.com; path=/; SameSite=None; Secure")
                    cookieManager.flush()
                } catch (e: Exception) {
                    Log.w("FastWebViewManager", "Failed to set consent cookies: ${e.message}")
                }

                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    cacheMode = WebSettings.LOAD_DEFAULT
                    useWideViewPort = true
                    loadWithOverviewMode = true
                    builtInZoomControls = true
                    displayZoomControls = false
                    setSupportZoom(true)
                    loadsImagesAutomatically = true
                    mediaPlaybackRequiresUserGesture = false
                    mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE

                    // Enable pre-rastering for 60/120fps smooth scrolling without stutter
                    if (WebViewFeature.isFeatureSupported(WebViewFeature.OFF_SCREEN_PRERASTER)) {
                        try {
                            WebSettingsCompat.setOffscreenPreRaster(this, true)
                        } catch (_: Exception) {}
                    }

                    // Clean Chrome User-Agent without WebView markers
                    userAgentString = "Mozilla/5.0 (Linux; Android 14; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"
                }

                // 2. CRITICAL ANTI-BLOCK: Disable X-Requested-With header to stop Google bot-detection
                if (WebViewFeature.isFeatureSupported(WebViewFeature.REQUESTED_WITH_HEADER_ALLOW_LIST)) {
                    try {
                        WebSettingsCompat.setRequestedWithHeaderOriginAllowList(settings, emptySet())
                    } catch (e: Exception) {
                        Log.w("FastWebViewManager", "Could not set requested with allow list: ${e.message}")
                    }
                }

                addJavascriptInterface(ScrollSearchBridge(), "ScrollSearchBridge")

                var cssInjectedForPage = false
                var buttonsInjectedForPage = false

                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        cssInjectedForPage = false
                        buttonsInjectedForPage = false
                        onPageStartedListener?.invoke()
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        applyCurrentCss(view)
                        // Inject overlay action buttons (Download & Fullscreen Zoom icons)
                        view?.evaluateJavascript(GoogleSearchHelper.getImageOverlayButtonsScript(), null)
                        onPageFinishedListener?.invoke()
                    }

                    override fun shouldInterceptRequest(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): android.webkit.WebResourceResponse? {
                        if (ScrollSearchStateBus.isAdBlockEnabled.value) {
                            val host = request?.url?.host?.lowercase() ?: ""
                            if (isAdHost(host)) {
                                return android.webkit.WebResourceResponse(
                                    "text/plain",
                                    "utf-8",
                                    java.io.ByteArrayInputStream(ByteArray(0))
                                )
                            }
                        }
                        return super.shouldInterceptRequest(view, request)
                    }

                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: WebResourceError?
                    ) {
                        super.onReceivedError(view, request, error)
                        onErrorListener?.invoke(error?.description?.toString() ?: "Erro ao carregar")
                    }
                }

                webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        if (newProgress >= 35 && !cssInjectedForPage) {
                            cssInjectedForPage = true
                            applyCurrentCss(view)
                        }
                        if (newProgress >= 65 && !buttonsInjectedForPage) {
                            buttonsInjectedForPage = true
                            view?.evaluateJavascript(GoogleSearchHelper.getImageOverlayButtonsScript(), null)
                        }
                    }
                }
            }
            webView = wv
        } catch (e: Exception) {
            Log.e("FastWebViewManager", "Failed to init WebView: ${e.message}")
        }
    }

    fun applyCurrentCss(targetView: WebView? = webView) {
        val activeVersion = ScrollSearchStateBus.activeVersion.value
        val script = if (activeVersion == com.example.model.OverlayUiVersion.F7) {
            GoogleSearchHelper.getF7SingleImageCssInjectionScript()
        } else if (isPureMode) {
            GoogleSearchHelper.getPurePhotosCssInjectionScript()
        } else {
            GoogleSearchHelper.getCleanViewCssInjectionScript()
        }
        targetView?.evaluateJavascript(script, null)
    }

    fun setPureMode(pure: Boolean) {
        if (isPureMode != pure) {
            isPureMode = pure
            applyCurrentCss()
            webView?.evaluateJavascript(GoogleSearchHelper.getImageOverlayButtonsScript(), null)
        }
    }

    fun getWebView(): WebView? {
        if (webView == null) {
            initWebView()
        }
        return webView
    }

    fun getOrCreateWebView(ctx: Context? = null): WebView {
        if (webView == null) {
            initWebView()
        }
        return webView ?: WebView(ctx ?: context).also { webView = it }
    }

    fun scrollDownFast(amountDp: Int = 450) {
        val wv = webView ?: return
        val density = wv.resources.displayMetrics.density
        val px = (amountDp * density).toInt()
        wv.evaluateJavascript("window.scrollBy({ top: $px, behavior: 'smooth' });", null)
    }

    fun scrollToTopFast() {
        val wv = webView ?: return
        wv.evaluateJavascript("window.scrollTo({ top: 0, behavior: 'smooth' });", null)
    }

    /**
     * Loads the search query using the selected engine.
     * When isPure is true (versions F), applies qualityParam to search for specific resolution/quality on Google.
     */
    fun loadSearchQuery(
        query: String,
        isPure: Boolean = false,
        qualityParam: String = "",
        engine: com.example.model.SearchEngine = ScrollSearchStateBus.selectedSearchEngine.value,
        searchType: com.example.model.SearchType = ScrollSearchStateBus.selectedSearchType.value
    ) {
        currentQuery = query
        isPureMode = isPure
        currentQualityParam = qualityParam

        val isSafeSearchDisabled = ScrollSearchStateBus.isSafeSearchDisabled.value
        try {
            val cookieManager = CookieManager.getInstance()
            if (isSafeSearchDisabled) {
                cookieManager.setCookie("https://duckduckgo.com", "kp=-2; domain=.duckduckgo.com; path=/; SameSite=None; Secure")
                cookieManager.setCookie("https://www.bing.com", "SRCHHPGUSR=ADLT=OFF; domain=.bing.com; path=/; SameSite=None; Secure")
                cookieManager.setCookie("https://www.google.com", "safe=off; domain=.google.com; path=/; SameSite=None; Secure")
                cookieManager.setCookie("https://search.yahoo.com", "sB=vm=p; domain=.yahoo.com; path=/; SameSite=None; Secure")
            } else {
                cookieManager.setCookie("https://duckduckgo.com", "kp=1; domain=.duckduckgo.com; path=/; SameSite=None; Secure")
                cookieManager.setCookie("https://www.bing.com", "SRCHHPGUSR=ADLT=STRICT; domain=.bing.com; path=/; SameSite=None; Secure")
                cookieManager.setCookie("https://www.google.com", "safe=active; domain=.google.com; path=/; SameSite=None; Secure")
                cookieManager.setCookie("https://search.yahoo.com", "sB=vm=r; domain=.yahoo.com; path=/; SameSite=None; Secure")
            }
        } catch (e: Exception) {
            Log.w("FastWebViewManager", "Failed to update cookies: ${e.message}")
        }

        val targetUrl = GoogleSearchHelper.buildSearchUrl(
            engine = engine,
            query = query,
            qualityParam = if (isPure) qualityParam else "",
            isSafeSearchDisabled = isSafeSearchDisabled,
            searchType = searchType
        )
        if (targetUrl == currentUrl) {
            applyCurrentCss()
            webView?.evaluateJavascript(GoogleSearchHelper.getImageOverlayButtonsScript(), null)
            return
        }

        currentUrl = targetUrl
        lastLoadTimestamp = SystemClock.uptimeMillis()

        webView?.let { wv ->
            wv.stopLoading()
            wv.loadUrl(targetUrl)
        } ?: run {
            initWebView()
            webView?.loadUrl(targetUrl)
        }
    }

    fun cleanRecreate() {
        try {
            webView?.let { wv ->
                (wv.parent as? ViewGroup)?.removeView(wv)
                wv.stopLoading()
                wv.clearHistory()
                wv.clearCache(false)
                wv.destroy()
            }
        } catch (e: Exception) {
            Log.e("FastWebViewManager", "Error destroying old webview: ${e.message}")
        } finally {
            webView = null
            initWebView()
            currentUrl?.let { loadSearchQuery(it, isPureMode, currentQualityParam) }
        }
    }

    private fun isAdHost(host: String): Boolean {
        if (host.isEmpty()) return false
        return host.contains("doubleclick.net") ||
            host.contains("googlesyndication.com") ||
            host.contains("googleadservices.com") ||
            host.contains("adnxs.com") ||
            host.contains("ads.yahoo.com") ||
            host.contains("an.yandex.ru") ||
            host.contains("mc.yandex.ru") ||
            host.contains("amazon-adsystem.com") ||
            host.contains("criteo.com") ||
            host.contains("taboola.com") ||
            host.contains("outbrain.com") ||
            host.contains("pubmatic.com") ||
            host.contains("rubiconproject.com") ||
            host.contains("openx.net") ||
            host.contains("popads.net") ||
            host.contains("propellerads.com") ||
            host.startsWith("ads.") ||
            host.startsWith("ad.")
    }

    fun destroy() {
        try {
            webView?.let { wv ->
                (wv.parent as? ViewGroup)?.removeView(wv)
                wv.stopLoading()
                wv.destroy()
            }
        } catch (e: Exception) {
            Log.e("FastWebViewManager", "Error on destroy: ${e.message}")
        } finally {
            webView = null
        }
    }
}
