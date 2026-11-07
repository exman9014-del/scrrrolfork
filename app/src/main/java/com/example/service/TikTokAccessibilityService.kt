package com.example.service

import android.accessibilityservice.AccessibilityService
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.data.ScrollSearchStateBus
import com.example.model.SocialPlatform
import java.util.ArrayDeque
import java.util.Locale

open class TikTokAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "SocialLensDetector"
        private const val THROTTLE_MS = 25L

        @Volatile
        var isServiceRunning = false
            private set

        @Volatile
        var serviceInstance: TikTokAccessibilityService? = null
            private set

        // Known resource IDs across TikTok, Instagram, Facebook, YouTube Shorts, and Bluesky
        private val TARGET_VIEW_IDS = listOf(
            // TikTok
            "author_name",
            "user_name",
            "tv_author",
            "tv_user_name",
            "title_view",
            "name_view",
            "tv_title",
            "author_text",
            "feed_author",
            "profile_user_name",
            "video_author",
            "search_user_name",
            "search_item_title",
            "desc_text",
            // Instagram
            "row_feed_photo_profile_name",
            "row_feed_profile_name",
            "feed_header_username",
            "clips_author_username",
            "row_search_user_username",
            "row_search_user_fullname",
            "reel_viewer_author",
            "secondary_label",
            "username_text",
            "username",
            "action_bar_title",
            // YouTube Shorts / Feed
            "channel_name",
            "reel_player_channel_name",
            "reel_channel_name",
            "channel_title",
            // Facebook
            "story_header_text",
            "com.facebook.katana:id/author_name",
            // Bluesky
            "authorHandle",
            "displayName",
            "postHandle"
        )

        // Non-username system/UI words to filter out
        private val BLACKLIST_TERMS = hashSetOf(
            "tiktok", "musically", "fyp", "foryou", "foryoupage", "para você", "paravoce",
            "seguindo", "following", "amigos", "friends", "compartilhar", "share",
            "comentários", "comentarios", "comments", "curtir", "curtidas", "like", "likes",
            "som original", "original sound", "original audio", "som", "audio",
            "pesquisar", "search", "adicionar", "add", "início", "inicio", "home",
            "perfil", "profile", "notificações", "notificacoes", "inbox", "caixa de entrada",
            "ao vivo", "live", "login", "cadastre-se", "explore", "descubra",
            "seguir", "follow", "salvar", "save", "favorito", "favoritos", "bookmark",
            "instagram", "reels", "direct", "story", "stories", "facebook", "watch",
            "youtube", "shorts", "inscrever-se", "subscribe", "inscrito", "subscribers",
            "bluesky", "repost", "reply", "trending", "em alta", "pesquise", "procurar",
            "limpar", "cancelar", "voltar", "filtrar", "tudo", "contas", "hashtags"
        )
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        isServiceRunning = true
        serviceInstance = this
        Log.d(TAG, "TikTokAccessibilityService connected successfully")
    }

    private val debounceHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var pendingScanRunnable: Runnable? = null
    private var lastScanTime = 0L
    private var lastDetectedUser: String? = null

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val pkgName = event.packageName?.toString() ?: ""
        var platform = SocialPlatform.fromPackage(pkgName)

        // If package is keyboard/search IME or system UI, check root window package
        if (platform == null) {
            val root = rootInActiveWindow
            if (root != null) {
                val rootPkg = root.packageName?.toString() ?: ""
                platform = SocialPlatform.fromPackage(rootPkg)
            }
        }

        if (platform == null && isBrowserPackage(pkgName)) {
            platform = ScrollSearchStateBus.selectedPlatform.value
        }

        if (platform != null) {
            ScrollSearchStateBus.setTikTokForeground(true)
            ScrollSearchStateBus.setActiveDetectedPlatform(platform)
        } else {
            // Not a recognized social app event
            return
        }

        val now = SystemClock.uptimeMillis()
        val elapsed = now - lastScanTime
        val activePlatform = platform

        if (elapsed >= 200L) {
            pendingScanRunnable?.let { debounceHandler.removeCallbacks(it) }
            pendingScanRunnable = null
            lastScanTime = now
            scanActiveWindowForUsername(activePlatform)
        } else if (pendingScanRunnable == null) {
            val runnable = Runnable {
                pendingScanRunnable = null
                lastScanTime = SystemClock.uptimeMillis()
                scanActiveWindowForUsername(activePlatform)
            }
            pendingScanRunnable = runnable
            debounceHandler.postDelayed(runnable, 200L - elapsed)
        }
    }

    private fun scanActiveWindowForUsername(platform: SocialPlatform) {
        var detected: String? = null

        val rootNode = rootInActiveWindow
        if (rootNode != null) {
            try {
                detected = extractUsernameFast(rootNode, maxNodes = 400)
            } catch (e: Exception) {
                Log.w(TAG, "Error during rootNode traversal: ${e.message}")
            }
        }

        if (detected.isNullOrBlank()) {
            try {
                val windowList = windows
                if (windowList != null) {
                    for (window in windowList) {
                        val wRoot = window.root ?: continue
                        val wPkg = wRoot.packageName?.toString() ?: ""
                        if (SocialPlatform.fromPackage(wPkg) == platform) {
                            detected = extractUsernameFast(wRoot, maxNodes = 250)
                            if (!detected.isNullOrBlank()) break
                        }
                    }
                }
            } catch (_: Exception) {
            }
        }

        if (!detected.isNullOrBlank() && detected != lastDetectedUser) {
            lastDetectedUser = detected
            Log.d(TAG, "Detected post creator [${platform.displayName}]: @$detected")
            ScrollSearchStateBus.updateDetectedUsername(detected)
        }
    }

    /**
     * Fast BFS scanner designed to inspect post cards, search results, video headers,
     * captions, and author handles.
     */
    private fun extractUsernameFast(root: AccessibilityNodeInfo, maxNodes: Int): String? {
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)

        var visited = 0

        while (!queue.isEmpty() && visited < maxNodes) {
            val current = queue.poll() ?: break
            visited++

            // Strategy 1: Check known Target View ID
            val viewId = current.viewIdResourceName
            if (viewId != null) {
                for (targetId in TARGET_VIEW_IDS) {
                    if (viewId.contains(targetId, ignoreCase = true)) {
                        val text = current.text?.toString()?.trim()
                        val valid = sanitizeUsername(text)
                            ?: sanitizeUsername(current.contentDescription?.toString()?.trim())
                        if (valid != null) {
                            return valid
                        }
                    }
                }
            }

            // Strategy 2: Check Content Description pattern (e.g. "Perfil de charlidamelio", "@khaby.lame", "Canal MrBeast")
            val contentDesc = current.contentDescription?.toString()
            if (!contentDesc.isNullOrBlank()) {
                val extracted = extractFromContentDescription(contentDesc)
                if (extracted != null) {
                    return extracted
                }
            }

            // Strategy 3: Check plain text if it starts with @ or ends with .bsky.social
            val text = current.text?.toString()?.trim()
            if (!text.isNullOrBlank()) {
                if (text.startsWith("@")) {
                    val candidate = sanitizeUsername(text)
                    if (candidate != null) {
                        return candidate
                    }
                } else if (text.endsWith(".bsky.social")) {
                    val clean = text.removeSuffix(".bsky.social").trim()
                    val candidate = sanitizeUsername(clean)
                    if (candidate != null) {
                        return candidate
                    }
                }
            }

            // Enqueue child nodes
            val childCount = current.childCount
            for (i in 0 until childCount) {
                current.getChild(i)?.let { child ->
                    queue.add(child)
                }
            }
        }

        return null
    }

    private fun extractFromContentDescription(desc: String): String? {
        val lower = desc.lowercase(Locale.ROOT)
        val prefixes = listOf(
            "perfil de ",
            "profile of ",
            "vídeo de ",
            "video of ",
            "video by ",
            "ver perfil de ",
            "view profile of ",
            "canal de ",
            "canal ",
            "post de ",
            "publicação de ",
            "post by "
        )
        for (prefix in prefixes) {
            if (lower.startsWith(prefix)) {
                val candidate = desc.substring(prefix.length).trim().removePrefix("@")
                val clean = candidate.split(" ").firstOrNull() ?: candidate
                return sanitizeUsername(clean)
            }
        }
        if (desc.startsWith("@")) {
            return sanitizeUsername(desc)
        }
        return null
    }

    private fun sanitizeUsername(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        val trimmed = raw.trim().removePrefix("@").removeSuffix(":")

        // Social handles: 2-35 chars, alphanumeric with dots, dashes and underscores
        if (trimmed.length in 2..35 && trimmed.matches(Regex("^[a-zA-Z0-9._-]+$"))) {
            val lower = trimmed.lowercase(Locale.ROOT)
            if (!BLACKLIST_TERMS.contains(lower) && !lower.all { it.isDigit() }) {
                return trimmed
            }
        }
        return null
    }

    private fun isBrowserPackage(pkg: String): Boolean {
        return pkg == "com.android.chrome" ||
            pkg == "com.sec.android.app.sbrowser" ||
            pkg == "org.mozilla.firefox" ||
            pkg == "com.opera.browser" ||
            pkg == "com.brave.browser" ||
            pkg == "com.microsoft.emmx"
    }

    override fun onInterrupt() {
        Log.w(TAG, "AccessibilityService interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        serviceInstance = null
        ScrollSearchStateBus.setTikTokForeground(false)
    }
}
