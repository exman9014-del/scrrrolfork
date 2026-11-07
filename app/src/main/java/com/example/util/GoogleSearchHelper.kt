package com.example.util

import android.util.Log
import com.example.model.PhotoItem
import com.example.model.ImageQuality
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class GoogleCustomSearchPage(
    val photos: List<PhotoItem>,
    val nextStartIndex: Int?,
    val errorMessage: String? = null
)

data class PremiumProviderResult(
    val title: String,
    val url: String,
    val previewUrl: String?,
    val description: String,
    val source: String
)

object GoogleSearchHelper {
    private const val TAG = "GoogleSearchHelper"

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }

    fun buildSearchUrl(
        engine: com.example.model.SearchEngine,
        query: String,
        qualityParam: String = "",
        isSafeSearchDisabled: Boolean = com.example.data.ScrollSearchStateBus.isSafeSearchDisabled.value,
        searchType: com.example.model.SearchType = com.example.data.ScrollSearchStateBus.selectedSearchType.value,
        isHighQualityFilter: Boolean = com.example.data.ScrollSearchStateBus.isHighQualityFilterEnabled.value
    ): String {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val effectiveQuality = if (qualityParam.isNotBlank()) {
            qualityParam
        } else if (isHighQualityFilter) {
            "&tbs=isz:l"
        } else {
            ""
        }
        return when (engine) {
            com.example.model.SearchEngine.GOOGLE -> {
                val safeParam = if (isSafeSearchDisabled) "&safe=off" else "&safe=active"
                when (searchType) {
                    com.example.model.SearchType.IMAGES ->
                        "https://www.google.com/search?q=$encoded&tbm=isch&hl=pt-BR&gl=br$safeParam$effectiveQuality"
                    com.example.model.SearchType.VIDEOS ->
                        "https://www.google.com/search?q=$encoded&tbm=vid&hl=pt-BR&gl=br$safeParam"
                    com.example.model.SearchType.ALL_WEB ->
                        "https://www.google.com/search?q=$encoded&hl=pt-BR&gl=br$safeParam"
                    com.example.model.SearchType.NEWS ->
                        "https://www.google.com/search?q=$encoded&tbm=nws&hl=pt-BR&gl=br$safeParam"
                }
            }
            com.example.model.SearchEngine.BING -> {
                val safeParam = if (isSafeSearchDisabled) "&adlt=off" else "&adlt=strict"
                val hqParam = if (isHighQualityFilter) "&qft=+filterui:imagesize-large" else ""
                when (searchType) {
                    com.example.model.SearchType.IMAGES ->
                        "https://www.bing.com/images/search?q=$encoded&form=HDRSC2$safeParam$hqParam"
                    com.example.model.SearchType.VIDEOS ->
                        "https://www.bing.com/videos/search?q=$encoded$safeParam"
                    com.example.model.SearchType.ALL_WEB ->
                        "https://www.bing.com/search?q=$encoded$safeParam"
                    com.example.model.SearchType.NEWS ->
                        "https://www.bing.com/news/search?q=$encoded$safeParam"
                }
            }
            com.example.model.SearchEngine.DUCKDUCKGO -> {
                val safeParam = if (isSafeSearchDisabled) "&kp=-2" else "&kp=1"
                val hqParam = if (isHighQualityFilter) "&iaf=size:Large" else ""
                when (searchType) {
                    com.example.model.SearchType.IMAGES ->
                        "https://duckduckgo.com/?q=$encoded&iax=images&ia=images$safeParam$hqParam"
                    com.example.model.SearchType.VIDEOS ->
                        "https://duckduckgo.com/?q=$encoded&iax=videos&ia=videos$safeParam"
                    com.example.model.SearchType.ALL_WEB ->
                        "https://duckduckgo.com/?q=$encoded$safeParam"
                    com.example.model.SearchType.NEWS ->
                        "https://duckduckgo.com/?q=$encoded&iar=news&ia=news$safeParam"
                }
            }
            com.example.model.SearchEngine.YANDEX -> {
                val safeParam = if (isSafeSearchDisabled) "&family=0" else "&family=1"
                val hqParam = if (isHighQualityFilter) "&isize=wallpaper" else ""
                when (searchType) {
                    com.example.model.SearchType.IMAGES ->
                        "https://yandex.com/images/search?text=$encoded$safeParam$hqParam"
                    com.example.model.SearchType.VIDEOS ->
                        "https://yandex.com/video/search?text=$encoded$safeParam"
                    com.example.model.SearchType.ALL_WEB ->
                        "https://yandex.com/search/?text=$encoded$safeParam"
                    com.example.model.SearchType.NEWS ->
                        "https://yandex.com/news/search?text=$encoded$safeParam"
                }
            }
            com.example.model.SearchEngine.YAHOO -> {
                val safeParam = if (isSafeSearchDisabled) "&vm=p" else "&vm=r"
                val hqParam = if (isHighQualityFilter) "&imgsz=large" else ""
                when (searchType) {
                    com.example.model.SearchType.IMAGES ->
                        "https://images.search.yahoo.com/search/images?p=$encoded$safeParam$hqParam"
                    com.example.model.SearchType.VIDEOS ->
                        "https://video.search.yahoo.com/search/video?p=$encoded$safeParam"
                    com.example.model.SearchType.ALL_WEB ->
                        "https://search.yahoo.com/search?p=$encoded$safeParam"
                    com.example.model.SearchType.NEWS ->
                        "https://news.search.yahoo.com/search?p=$encoded$safeParam"
                }
            }
        }
    }

    fun buildGoogleImagesUrl(query: String, qualityParam: String = ""): String {
        return buildSearchUrl(com.example.model.SearchEngine.GOOGLE, query, qualityParam)
    }

    fun buildAlternativeSearchUrl(query: String): String {
        return buildSearchUrl(com.example.model.SearchEngine.DUCKDUCKGO, query)
    }

    fun getCleanViewCssInjectionScript(): String {
        return """
            (function() {
                var style = document.getElementById('scrollsearch-custom-style');
                if (!style) {
                    style = document.createElement('style');
                    style.id = 'scrollsearch-custom-style';
                    document.head.appendChild(style);
                }
                style.innerHTML = `
                    header, #header, #searchform, .mNsI7b, .F9640c, .OqTqVd, .yg07nd,
                    .k27Ahb, footer, #footer, .NZmxZe, #swml, .fbar, #sfooter,
                    div[role="navigation"], div[role="banner"], form[role="search"],
                    div[aria-modal="true"], #lb, .VDgVie, form[action*="consent"] {
                        display: none !important;
                    }
                    body {
                        background-color: #12121E !important;
                        color: #FFFFFF !important;
                        padding-top: 0px !important;
                        margin: 0px !important;
                        -webkit-overflow-scrolling: touch !important;
                        touch-action: pan-y !important;
                        overflow-y: auto !important;
                    }
                    img {
                        border-radius: 8px !important;
                        content-visibility: auto !important;
                        contain-intrinsic-size: 200px 200px !important;
                        transform: translateZ(0) !important;
                        -webkit-transform: translateZ(0) !important;
                    }
                `;

                // Auto-accept Google Consent prompt if present to unblock search results
                try {
                    var consentBtn = document.querySelector('button#L2AGLb, button[id*="L2AGLb"], form[action*="consent"] button, button[aria-label*="Aceitar"], button[aria-label*="Accept"], div[aria-label*="Aceitar tudo"]');
                    if (consentBtn) { consentBtn.click(); }
                } catch(e) {}

                // Set SafeSearch disabled cookie on DuckDuckGo and Bing
                try {
                    if (window.location.hostname.includes('duckduckgo')) {
                        document.cookie = "kp=-2; path=/; domain=.duckduckgo.com";
                    } else if (window.location.hostname.includes('bing')) {
                        document.cookie = "SRCHHPGUSR=ADLT=OFF; path=/; domain=.bing.com";
                    }
                } catch(e) {}
            })();
        """.trimIndent()
    }

    fun getPurePhotosCssInjectionScript(): String {
        return """
            (function() {
                var style = document.getElementById('scrollsearch-custom-style');
                if (!style) {
                    style = document.createElement('style');
                    style.id = 'scrollsearch-custom-style';
                    document.head.appendChild(style);
                }
                style.innerHTML = `
                    header, #header, #searchform, .mNsI7b, .F9640c, .OqTqVd, .yg07nd,
                    .k27Ahb, footer, #footer, .NZmxZe, #swml, .fbar, #sfooter,
                    div[role="navigation"], div[role="banner"], form[role="search"],
                    #sfc, .hdtb-mbot, .t6P1ce, .NZmxZe, .Q3Abe, .f7Wdxd, .j31l1e,
                    div[jsname="GZIdke"], div[jscontroller="Csnxkd"],
                    div.crJ18e, div.L99Ud, div.e6i7be, #taw, .appbar, #botstuff,
                    .W4DClc, .g3P7Id, #rhs, div[data-sug],
                    div[aria-modal="true"], #lb, .VDgVie, form[action*="consent"], div[role="dialog"] {
                        display: none !important;
                        height: 0px !important;
                        visibility: hidden !important;
                    }
                    html, body {
                        background-color: #0A0A12 !important;
                        color: #FFFFFF !important;
                        padding: 0px !important;
                        margin: 0px !important;
                        overflow-x: hidden !important;
                        overflow-y: auto !important;
                        -webkit-overflow-scrolling: touch !important;
                        touch-action: pan-y !important;
                    }
                    img {
                        border-radius: 12px !important;
                        content-visibility: auto !important;
                        contain-intrinsic-size: 200px 250px !important;
                        transform: translateZ(0) !important;
                        -webkit-transform: translateZ(0) !important;
                    }
                    .isv-r, .tile--img, .img_cont, .iusc, .grid-item {
                        border-radius: 12px !important;
                        margin: 4px !important;
                        contain: layout paint !important;
                        transform: translateZ(0) !important;
                    }
                `;

                // Auto-accept Google Consent prompt if present to unblock search results
                try {
                    var consentBtn = document.querySelector('button#L2AGLb, button[id*="L2AGLb"], form[action*="consent"] button, button[aria-label*="Aceitar"], button[aria-label*="Accept"], div[aria-label*="Aceitar tudo"]');
                    if (consentBtn) { consentBtn.click(); }
                } catch(e) {}

                // Set SafeSearch disabled cookie on DuckDuckGo and Bing
                try {
                    if (window.location.hostname.includes('duckduckgo')) {
                        document.cookie = "kp=-2; path=/; domain=.duckduckgo.com";
                    } else if (window.location.hostname.includes('bing')) {
                        document.cookie = "SRCHHPGUSR=ADLT=OFF; path=/; domain=.bing.com";
                    }
                } catch(e) {}
            })();
        """.trimIndent()
    }

    /**
     * Mode F7: Exactly ONE image per display screen
     */
    fun getF7SingleImageCssInjectionScript(): String {
        return """
            (function() {
                var style = document.getElementById('scrollsearch-custom-style');
                if (!style) {
                    style = document.createElement('style');
                    style.id = 'scrollsearch-custom-style';
                    document.head.appendChild(style);
                }
                style.innerHTML = `
                    header, #header, #searchform, .mNsI7b, .F9640c, .OqTqVd, .yg07nd,
                    .k27Ahb, footer, #footer, .NZmxZe, #swml, .fbar, #sfooter,
                    div[role="navigation"], div[role="banner"], form[role="search"],
                    #sfc, .hdtb-mbot, .t6P1ce, .NZmxZe, .Q3Abe, .f7Wdxd, .j31l1e,
                    div[jsname="GZIdke"], div[jscontroller="Csnxkd"],
                    div.crJ18e, div.L99Ud, div.e6i7be, #taw, .appbar, #botstuff,
                    .W4DClc, .g3P7Id, #rhs, div[data-sug],
                    div[aria-modal="true"], #lb, .VDgVie, form[action*="consent"], div[role="dialog"] {
                        display: none !important;
                        height: 0px !important;
                        visibility: hidden !important;
                    }
                    html, body {
                        background-color: #0A0A12 !important;
                        color: #FFFFFF !important;
                        padding: 0px !important;
                        margin: 0px !important;
                        overflow-x: hidden !important;
                        -webkit-overflow-scrolling: touch !important;
                        scroll-behavior: smooth !important;
                    }
                    /* Force parent image containers to a single vertical column */
                    #islrg, .islrc, .z75nLe, div[jsname="GZIdke"], .results--images, .zmg, .dg_u, .b_focusTextExtra, .serp__results, .layout-images, .b_canvas, #b_content, #b_results {
                        display: block !important;
                        column-count: 1 !important;
                        -webkit-column-count: 1 !important;
                        grid-template-columns: 100vw !important;
                        width: 100vw !important;
                        max-width: 100vw !important;
                        padding: 0 !important;
                        margin: 0 !important;
                    }
                    /* Mode F7: Large image card filling the display cleanly with fluid scrolling */
                    div[data-ri], .isv-r, .grid-item, .img_cont, .serp__item, div[jsname="r5xl4"], .tile--img, .iusc, .iuscp {
                        width: 100vw !important;
                        max-width: 100vw !important;
                        min-width: 100vw !important;
                        height: 85vh !important;
                        min-height: 85vh !important;
                        max-height: 85vh !important;
                        margin: 0px 0px 16px 0px !important;
                        padding: 6px !important;
                        box-sizing: border-box !important;
                        display: flex !important;
                        align-items: center !important;
                        justify-content: center !important;
                        position: relative !important;
                        float: none !important;
                        clear: both !important;
                    }
                    div[data-ri] img, .isv-r img, .grid-item img, .img_cont img, .tile--img img, .mimg, .iusc img {
                        width: 100% !important;
                        height: 100% !important;
                        max-width: 100vw !important;
                        max-height: 82vh !important;
                        object-fit: contain !important;
                        border-radius: 16px !important;
                    }
                `;

                // Auto-accept Google Consent prompt if present to unblock search results
                try {
                    var consentBtn = document.querySelector('button#L2AGLb, button[id*="L2AGLb"], form[action*="consent"] button, button[aria-label*="Aceitar"], button[aria-label*="Accept"], div[aria-label*="Aceitar tudo"]');
                    if (consentBtn) { consentBtn.click(); }
                } catch(e) {}

                // Set SafeSearch disabled cookie on DuckDuckGo and Bing
                try {
                    if (window.location.hostname.includes('duckduckgo')) {
                        document.cookie = "kp=-2; path=/; domain=.duckduckgo.com";
                    } else if (window.location.hostname.includes('bing')) {
                        document.cookie = "SRCHHPGUSR=ADLT=OFF; path=/; domain=.bing.com";
                    }
                } catch(e) {}
            })();
        """.trimIndent()
    }

    fun getImageOverlayButtonsScript(): String {
        return """
            (function() {
                try {
                    var style = document.getElementById('scrollsearch-buttons-style');
                    if (!style) {
                        style = document.createElement('style');
                        style.id = 'scrollsearch-buttons-style';
                        style.innerHTML = `
                            .scrollsearch-btn-container {
                                position: absolute !important;
                                top: 6px !important;
                                right: 6px !important;
                                display: flex !important;
                                gap: 7px !important;
                                z-index: 2147483647 !important;
                                pointer-events: auto !important;
                                -webkit-user-select: none !important;
                                user-select: none !important;
                            }
                            .scrollsearch-btn {
                                width: 36px !important;
                                height: 36px !important;
                                min-width: 36px !important;
                                min-height: 36px !important;
                                border-radius: 50% !important;
                                background: rgba(10, 10, 22, 0.90) !important;
                                border: 1.5px solid rgba(255, 255, 255, 0.40) !important;
                                display: inline-flex !important;
                                align-items: center !important;
                                justify-content: center !important;
                                cursor: pointer !important;
                                box-shadow: 0 4px 10px rgba(0,0,0,0.65) !important;
                                padding: 0 !important;
                                outline: none !important;
                                touch-action: manipulation !important;
                                -webkit-tap-highlight-color: transparent !important;
                                transition: transform 0.1s ease !important;
                            }
                            .scrollsearch-btn:active {
                                transform: scale(0.90) !important;
                                background: rgba(30, 30, 50, 0.98) !important;
                            }
                            .scrollsearch-btn svg {
                                width: 19px !important;
                                height: 19px !important;
                                pointer-events: none !important;
                            }
                            .scrollsearch-btn.download svg {
                                fill: #FF3366 !important;
                            }
                            .scrollsearch-btn.fullscreen svg {
                                fill: #00E5FF !important;
                            }
                        `;
                        document.head.appendChild(style);
                    }

                    function captureImageAsBase64(img) {
                        try {
                            var src = img.src || img.getAttribute('data-src') || img.getAttribute('data-iurl');
                            if (src && src.startsWith('data:image')) {
                                return src;
                            }
                            var canvas = document.createElement('canvas');
                            canvas.width = img.naturalWidth || img.width || 300;
                            canvas.height = img.naturalHeight || img.height || 300;
                            var ctx = canvas.getContext('2d');
                            ctx.drawImage(img, 0, 0);
                            return canvas.toDataURL('image/jpeg', 0.95);
                        } catch(e) {
                            return null;
                        }
                    }

                    function getValidPhotoImages() {
                        var all = document.querySelectorAll('img');
                        var valid = [];
                        for (var i = 0; i < all.length; i++) {
                            var img = all[i];
                            var src = img.src || img.getAttribute('data-src') || img.getAttribute('data-iurl') || '';
                            if (!src || src.includes('googlelogo') || src.includes('cleardot') || src.includes('arrow_') || src.includes('quantum') || src.includes('favicon')) continue;
                            var w = img.naturalWidth || img.width || 0;
                            var h = img.naturalHeight || img.height || 0;
                            if (w >= 40 || h >= 40 || src.includes('gstatic.com') || src.includes('bing.com') || src.startsWith('data:image')) {
                                valid.push(img);
                            }
                        }
                        return valid;
                    }

                    function collectAllUrls(validImgs) {
                        var urls = [];
                        var seen = {};
                        for (var k = 0; k < validImgs.length; k++) {
                            var u = validImgs[k].src || validImgs[k].getAttribute('data-src') || validImgs[k].getAttribute('data-iurl') || '';
                            if (u && (u.startsWith('http') || u.startsWith('data:image')) && !seen[u]) {
                                seen[u] = true;
                                urls.push(u);
                            }
                        }
                        return urls;
                    }

                    function attachButtons() {
                        var validImgs = getValidPhotoImages();
                        for (var i = 0; i < validImgs.length; i++) {
                            var img = validImgs[i];
                            img.dataset.scrollsearchIndex = i.toString();
                            if (img.dataset.scrollsearchReady) continue;
                            img.dataset.scrollsearchReady = 'true';

                            var src = img.src || img.getAttribute('data-src') || img.getAttribute('data-iurl');
                            var parent = img.parentElement;
                            if (!parent) continue;

                            if (parent.style.position !== 'relative' && parent.style.position !== 'absolute') {
                                parent.style.position = 'relative';
                            }

                            var container = document.createElement('div');
                            container.className = 'scrollsearch-btn-container';

                            // 1. Fullscreen Zoom button
                            var fsBtn = document.createElement('button');
                            fsBtn.className = 'scrollsearch-btn fullscreen';
                            fsBtn.title = 'Visualizar Tela Cheia & Zoom';
                            fsBtn.setAttribute('aria-label', 'Visualizar Tela Cheia & Zoom');
                            fsBtn.innerHTML = '<svg viewBox="0 0 24 24"><path d="M7 14H5v5h5v-2H7v-3zm-2-4h2V7h3V5H5v5zm12 7h-3v2h5v-5h-2v3zM14 5v2h3v3h2V5h-5z"/></svg>';

                            (function(currentImg, fallbackSrc, btn) {
                                var handleOpen = function(e) {
                                    if (e) {
                                        e.stopPropagation();
                                        e.preventDefault();
                                    }
                                    var currentList = getValidPhotoImages();
                                    var idx = currentList.indexOf(currentImg);
                                    if (idx < 0) {
                                        idx = parseInt(currentImg.dataset.scrollsearchIndex, 10);
                                        if (isNaN(idx)) idx = 0;
                                    }

                                    var rect = currentImg.getBoundingClientRect();
                                    var dpr = window.devicePixelRatio || 1.0;
                                    var base64 = captureImageAsBase64(currentImg);
                                    var s = (base64 && base64.startsWith('data:image')) ? base64 : (currentImg.src || currentImg.getAttribute('data-src') || currentImg.getAttribute('data-iurl') || fallbackSrc);
                                    
                                    var allUrls = collectAllUrls(currentList);
                                    if (idx >= 0 && idx < allUrls.length && base64 && base64.startsWith('data:image')) {
                                        allUrls[idx] = base64;
                                    }
                                    var allUrlsJson = JSON.stringify(allUrls);

                                    if (window.ScrollSearchBridge && window.ScrollSearchBridge.onCaptureAndOpenFullscreenWithIndex) {
                                        window.ScrollSearchBridge.onCaptureAndOpenFullscreenWithIndex(rect.left, rect.top, rect.width, rect.height, dpr, s, allUrlsJson, idx);
                                    } else if (window.ScrollSearchBridge && window.ScrollSearchBridge.onCaptureAndOpenFullscreen) {
                                        window.ScrollSearchBridge.onCaptureAndOpenFullscreen(rect.left, rect.top, rect.width, rect.height, dpr, s, allUrlsJson);
                                    } else if (window.ScrollSearchBridge && window.ScrollSearchBridge.onOpenFullscreen) {
                                        window.ScrollSearchBridge.onOpenFullscreen(s, allUrlsJson);
                                    }
                                };
                                btn.addEventListener('click', handleOpen, true);
                                btn.addEventListener('touchend', handleOpen, true);
                            })(img, src, fsBtn);

                            // 2. Download button
                            var dlBtn = document.createElement('button');
                            dlBtn.className = 'scrollsearch-btn download';
                            dlBtn.title = 'Baixar Foto';
                            dlBtn.setAttribute('aria-label', 'Baixar Foto');
                            dlBtn.innerHTML = '<svg viewBox="0 0 24 24"><path d="M19.35 10.04C18.67 6.59 15.64 4 12 4 9.11 4 6.6 5.64 5.35 8.04 2.34 8.36 0 10.91 0 14c0 3.31 2.69 6 6 6h13c2.76 0 5-2.24 5-5 0-2.64-2.05-4.78-4.65-4.96zM17 13l-5 5-5-5h3V9h4v4h3z"/></svg>';

                            (function(currentImg, fallbackSrc, btn) {
                                var handleDownload = function(e) {
                                    if (e) {
                                        e.stopPropagation();
                                        e.preventDefault();
                                    }
                                    var rect = currentImg.getBoundingClientRect();
                                    var dpr = window.devicePixelRatio || 1.0;
                                    var base64 = captureImageAsBase64(currentImg);
                                    var s = (base64 && base64.startsWith('data:image')) ? base64 : (currentImg.src || currentImg.getAttribute('data-src') || currentImg.getAttribute('data-iurl') || fallbackSrc);

                                    // Direct Base64 capture from the already rendered DOM/canvas (zero 403)
                                    if (base64 && base64.startsWith('data:image')) {
                                        if (window.ScrollSearchBridge && window.ScrollSearchBridge.onDownloadBase64) {
                                            window.ScrollSearchBridge.onDownloadBase64(base64, 'ScrollSearch_Photo');
                                            return;
                                        }
                                    }
                                    if (window.ScrollSearchBridge && window.ScrollSearchBridge.onCaptureAndDownload) {
                                        window.ScrollSearchBridge.onCaptureAndDownload(rect.left, rect.top, rect.width, rect.height, dpr, s, 'ScrollSearch_Photo');
                                        return;
                                    }
                                    if (window.ScrollSearchBridge && window.ScrollSearchBridge.onDownloadImageWithReferrer) {
                                        window.ScrollSearchBridge.onDownloadImageWithReferrer(s, window.location.href || 'https://www.google.com/', 'ScrollSearch_Photo');
                                    } else if (window.ScrollSearchBridge && window.ScrollSearchBridge.onDownloadImage) {
                                        window.ScrollSearchBridge.onDownloadImage(s);
                                    }
                                };
                                btn.addEventListener('click', handleDownload, true);
                                btn.addEventListener('touchend', handleDownload, true);
                            })(img, src, dlBtn);

                            container.appendChild(fsBtn);
                            container.appendChild(dlBtn);
                            parent.appendChild(container);
                        }
                    }

                    var isAttaching = false;
                    function safeAttachButtons() {
                        if (isAttaching) return;
                        isAttaching = true;
                        try {
                            attachButtons();
                        } finally {
                            setTimeout(function() { isAttaching = false; }, 60);
                        }
                    }

                    safeAttachButtons();

                    if (!window.__scrollsearchObserverInstalled) {
                        window.__scrollsearchObserverInstalled = true;
                        var isQueued = false;
                        var throttledAttach = function() {
                            if (isQueued || isAttaching) return;
                            isQueued = true;
                            window.requestAnimationFrame(function() {
                                safeAttachButtons();
                                isQueued = false;
                            });
                        };

                        var observer = new MutationObserver(function(mutations) {
                            if (isAttaching) return;
                            var shouldTrigger = false;
                            for (var m = 0; m < mutations.length; m++) {
                                var target = mutations[m].target;
                                if (target && !(target.classList && target.classList.contains('scrollsearch-btn-container'))) {
                                    shouldTrigger = true;
                                    break;
                                }
                            }
                            if (shouldTrigger) throttledAttach();
                        });
                        observer.observe(document.body, { childList: true, subtree: true });

                        var scrollDebounce = null;
                        window.addEventListener('scroll', function() {
                            if (scrollDebounce) clearTimeout(scrollDebounce);
                            // Only run after scroll is completely finished to keep 60fps scrolling
                            scrollDebounce = setTimeout(safeAttachButtons, 350);
                        }, { passive: true });
                    }
                } catch(e) {}
            })();
        """.trimIndent()
    }

    fun getImageExtractorScript(): String {
        return """
            (function() {
                try {
                    var urls = [];
                    var seen = {};
                    var imgs = document.querySelectorAll('img');
                    for (var i = 0; i < imgs.length; i++) {
                        var el = imgs[i];
                        var src = el.src || el.getAttribute('data-src') || el.getAttribute('data-iurl') || el.getAttribute('src');
                        if (src && !seen[src]) {
                            if (!src.includes('googlelogo') && !src.includes('cleardot') && !src.includes('arrow_') && !src.includes('quantum') && !src.includes('favicon')) {
                                if (src.includes('gstatic.com') || src.startsWith('data:image') || src.startsWith('http') || (el.naturalWidth && el.naturalWidth > 40) || (el.width && el.width > 40)) {
                                    seen[src] = true;
                                    urls.push(src);
                                }
                            }
                        }
                    }
                    var extraElements = document.querySelectorAll('[data-src], [data-iurl]');
                    for (var j = 0; j < extraElements.length; j++) {
                        var elem = extraElements[j];
                        var dSrc = elem.getAttribute('data-src') || elem.getAttribute('data-iurl');
                        if (dSrc && !seen[dSrc] && (dSrc.startsWith('http') || dSrc.startsWith('data:image'))) {
                            seen[dSrc] = true;
                            urls.push(dSrc);
                        }
                    }
                    var jsonStr = JSON.stringify(urls);
                    if (window.ScrollSearchBridge && urls.length > 0) {
                        window.ScrollSearchBridge.onReceiveImages(jsonStr);
                    }
                    return jsonStr;
                } catch(e) {
                    return "[]";
                }
            })();
        """.trimIndent()
    }

    fun sanitizeSearchApiUrl(raw: String): String {
        val trimmed = raw.trim().removeSurrounding("\"").removeSurrounding("'").trimEnd('/')
        val uri = runCatching { java.net.URI(trimmed) }.getOrNull() ?: return ""
        if (uri.scheme != "https" || uri.host.isNullOrBlank() ||
            uri.rawUserInfo != null || uri.rawQuery != null || uri.rawFragment != null ||
            (!uri.rawPath.isNullOrBlank() && uri.rawPath != "/")
        ) return ""
        return trimmed
    }

    fun getSearchApiUrl(): String = sanitizeSearchApiUrl(com.example.BuildConfig.TIKTOKLENS_SEARCH_API_URL)

    fun isSearchApiConfigured(apiUrl: String = getSearchApiUrl()): Boolean =
        sanitizeSearchApiUrl(apiUrl).isNotBlank()

    internal fun buildPremiumSearchUrl(
        query: String,
        apiUrl: String,
        start: Int = 1,
        num: Int = 100,
        highQualityOnly: Boolean = true,
        safeSearchDisabled: Boolean = true,
        quality: ImageQuality = ImageQuality.HIGH
    ): String? {
        val baseUrl = sanitizeSearchApiUrl(apiUrl)
        val cleanQuery = query.trim()

        if (baseUrl.isBlank() || cleanQuery.isBlank() || start !in 1..9901) return null

        val effectiveNum = num.coerceIn(1, 100)
        val params = buildList {
            add("q=${URLEncoder.encode(cleanQuery, "UTF-8")}")
            add("num=$effectiveNum")
            add("start=$start")
            add("safe=${if (safeSearchDisabled) "off" else "active"}")
            add("highQuality=${if (highQualityOnly) "true" else "false"}")
            add("quality=${quality.code}")
        }.joinToString("&")
        return "$baseUrl/api/search?$params"
    }

    internal fun buildPremiumProviderSearchUrl(
        provider: com.example.model.PremiumSearchProvider,
        query: String,
        imageUrl: String = "",
        apiUrl: String = getSearchApiUrl()
    ): String? {
        val baseUrl = sanitizeSearchApiUrl(apiUrl)
        val cleanQuery = query.trim()
        val cleanImageUrl = imageUrl.trim()
        if (baseUrl.isBlank()) return null
        if (provider.requiresImageUrl) {
            if (!cleanImageUrl.startsWith("https://", ignoreCase = true)) return null
        } else if (cleanQuery.length !in 2..150) {
            return null
        }
        val params = buildList {
            add("provider=${provider.id}")
            if (cleanQuery.isNotBlank()) add("q=${URLEncoder.encode(cleanQuery, "UTF-8")}")
            if (cleanImageUrl.isNotBlank()) add("imageUrl=${URLEncoder.encode(cleanImageUrl, "UTF-8")}")
            add("safe=${if (com.example.data.ScrollSearchStateBus.isSafeSearchDisabled.value) "off" else "active"}")
            add("highQuality=${com.example.data.ScrollSearchStateBus.isHighQualityFilterEnabled.value}")
            add("quality=${com.example.data.ScrollSearchStateBus.imageQuality.value.code}")
        }.joinToString("&")
        return "$baseUrl/api/search?$params"
    }

    suspend fun fetchPremiumProviderResults(
        provider: com.example.model.PremiumSearchProvider,
        query: String,
        imageUrl: String = "",
        apiUrl: String = getSearchApiUrl()
    ): Result<List<PremiumProviderResult>> = withContext(Dispatchers.IO) {
        val url = buildPremiumProviderSearchUrl(provider, query, imageUrl, apiUrl)
            ?: return@withContext Result.failure(IOException("Confira o backend e os dados da busca."))
        try {
            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val message = runCatching { JSONObject(body).optString("error") }
                        .getOrNull()
                        ?.takeIf { it.isNotBlank() }
                        ?: "A busca falhou (HTTP ${response.code})."
                    return@withContext Result.failure(IOException(message))
                }
                val json = JSONObject(body)
                val items = json.optJSONArray("items") ?: org.json.JSONArray()
                val results = buildList {
                    for (index in 0 until items.length()) {
                        val item = items.optJSONObject(index) ?: continue
                        val resultUrl = item.optString("link").takeIf { it.startsWith("https://", true) } ?: continue
                        val preview = item.optString("previewUrl").takeIf { it.startsWith("https://", true) }
                        add(
                            PremiumProviderResult(
                                title = item.optString("title").ifBlank { "Resultado" },
                                url = resultUrl,
                                previewUrl = preview,
                                description = item.optString("description"),
                                source = item.optString("displayLink")
                            )
                        )
                    }
                }
                Result.success(results)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Result.failure(e)
        } catch (e: JSONException) {
            Result.failure(IOException("O backend retornou uma resposta inválida.", e))
        }
    }

    internal fun mapGoogleCustomSearchResponse(query: String, json: JSONObject): GoogleCustomSearchPage {
        val photos = mutableListOf<PhotoItem>()
        val seenUrls = mutableSetOf<String>()
        val items = json.optJSONArray("items")
        if (items != null) {
            for (index in 0 until items.length()) {
                val item = items.optJSONObject(index) ?: continue
                val link = item.optString("link").trim()
                if (!link.startsWith("https://", true) || !seenUrls.add(link)) continue
                val image = item.optJSONObject("image")
                photos.add(
                    PhotoItem(
                        id = "gcs_${query.hashCode()}_${link.hashCode()}",
                        url = link,
                        title = item.optString("title").ifBlank { "Imagem" },
                        sourceDomain = item.optString("displayLink").ifBlank { "Google Images" },
                        width = image?.optInt("width", 0) ?: 0,
                        height = image?.optInt("height", 0) ?: 0,
                        previewUrl = item.optString("previewUrl").takeIf { it.startsWith("https://", true) }
                    )
                )
            }
        }

        val nextStartIndex = json.optJSONObject("queries")
            ?.optJSONArray("nextPage")
            ?.optJSONObject(0)
            ?.optInt("startIndex", 0)
            ?.takeIf { it in 1..9901 }
        return GoogleCustomSearchPage(photos, nextStartIndex)
    }

    suspend fun fetchGoogleCustomSearchPhotos(
        query: String,
        apiUrl: String = getSearchApiUrl(),
        start: Int = 1,
        num: Int = 100
    ): GoogleCustomSearchPage = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim()
        val url = buildPremiumSearchUrl(
            query = cleanQuery,
            apiUrl = apiUrl,
            start = start,
            num = num,
            highQualityOnly = com.example.data.ScrollSearchStateBus.isHighQualityFilterEnabled.value,
            safeSearchDisabled = com.example.data.ScrollSearchStateBus.isSafeSearchDisabled.value,
            quality = com.example.data.ScrollSearchStateBus.imageQuality.value
        )

        if (url == null) {
            val message = when {
                sanitizeSearchApiUrl(apiUrl).isBlank() -> "Configure a URL HTTPS da API de busca do TikTok Lens."
                cleanQuery.isBlank() -> "Digite um termo para buscar imagens."
                else -> "Não há mais páginas disponíveis para esta busca."
            }
            return@withContext GoogleCustomSearchPage(emptyList(), null, message)
        }

        try {
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .header("User-Agent", "TikTokLens/1.0 (Android)")
                .build()

            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val providerMessage = try {
                        JSONObject(bodyString).optString("error").takeIf { it.isNotBlank() }
                    } catch (_: JSONException) {
                        null
                    }
                    val message = providerMessage ?: when (response.code) {
                        400 -> "A consulta ou paginação enviada ao backend é inválida."
                        401, 403 -> "O provedor de busca recusou a solicitação. Confira a chave SerpApi no Vercel."
                        429 -> "A cota de busca SerpApi foi atingida. Confira o plano da conta."
                        503 -> "A busca ainda não está configurada no servidor."
                        else -> "Falha no servidor de busca (HTTP ${response.code})."
                    }
                    Log.e(TAG, "Search API request failed with HTTP ${response.code}.")
                    return@withContext GoogleCustomSearchPage(emptyList(), null, message)
                }
                val json = try {
                    JSONObject(bodyString)
                } catch (_: JSONException) {
                    return@withContext GoogleCustomSearchPage(
                        emptyList(),
                        null,
                        "A API retornou uma resposta inválida."
                    )
                }

                val page = mapGoogleCustomSearchResponse(cleanQuery, json)
                if (page.photos.isEmpty()) {
                    page.copy(errorMessage =                     "A busca não encontrou imagens válidas para esta consulta.")
                } else {
                    page
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: IOException) {
            Log.e(TAG, "Search API network request failed.")
            GoogleCustomSearchPage(emptyList(), null, "Não foi possível conectar ao servidor de busca.")
        }
    }
}
