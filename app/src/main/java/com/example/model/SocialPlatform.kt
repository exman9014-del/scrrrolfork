package com.example.model

import androidx.compose.ui.graphics.Color

enum class SocialPlatform(
    val id: String,
    val displayName: String,
    val tagline: String,
    val packageNames: List<String>,
    val webFallbackUrl: String,
    val primaryColor: Color,
    val accentColor: Color,
    val sampleCreators: List<String>
) {
    TIKTOK(
        id = "tiktok",
        displayName = "TikTok",
        tagline = "Vídeos curtos, criadores e sons virais",
        packageNames = listOf(
            "com.zhiliaoapp.musically",
            "com.ss.android.ugc.trill",
            "com.ss.android.ugc.aweme"
        ),
        webFallbackUrl = "https://www.tiktok.com",
        primaryColor = Color(0xFFFE2C55),
        accentColor = Color(0xFF00F2FE),
        sampleCreators = listOf("khaby.lame", "charlidamelio", "mrbeast", "bellapoarch", "zachking")
    ),
    INSTAGRAM(
        id = "instagram",
        displayName = "Instagram",
        tagline = "Reels, posts, explorar e busca",
        packageNames = listOf(
            "com.instagram.android"
        ),
        webFallbackUrl = "https://www.instagram.com",
        primaryColor = Color(0xFFE1306C),
        accentColor = Color(0xFFF77737),
        sampleCreators = listOf("cristiano", "leomessi", "selenagomez", "kyliejenner", "therock")
    ),
    FACEBOOK(
        id = "facebook",
        displayName = "Facebook",
        tagline = "Watch, vídeos, feed e grupos",
        packageNames = listOf(
            "com.facebook.katana",
            "com.facebook.lite"
        ),
        webFallbackUrl = "https://www.facebook.com/watch",
        primaryColor = Color(0xFF1877F2),
        accentColor = Color(0xFF42B72A),
        sampleCreators = listOf("meta", "nasa", "natgeo", "cristiano", "eminem")
    ),
    YOUTUBE_SHORTS(
        id = "youtube_shorts",
        displayName = "YouTube Shorts",
        tagline = "Shorts, canais e vídeos em alta",
        packageNames = listOf(
            "com.google.android.youtube"
        ),
        webFallbackUrl = "https://www.youtube.com/shorts",
        primaryColor = Color(0xFFFF0000),
        accentColor = Color(0xFF282828),
        sampleCreators = listOf("MrBeast", "PewDiePie", "MarkRober", "DudePerfect", "Whindersson")
    ),
    BLUESKY(
        id = "bluesky",
        displayName = "Bluesky",
        tagline = "Posts, feeds personalizados e criadores",
        packageNames = listOf(
            "xyz.blueskyweb.app"
        ),
        webFallbackUrl = "https://bsky.app",
        primaryColor = Color(0xFF0085FF),
        accentColor = Color(0xFF1185FE),
        sampleCreators = listOf("bsky.app", "jay.bsky.team", "nytimes.com", "theverge.com")
    ),
    X(
        id = "x",
        displayName = "X",
        tagline = "Posts, perfis e criadores",
        packageNames = listOf("com.twitter.android"),
        webFallbackUrl = "https://x.com",
        primaryColor = Color(0xFF000000),
        accentColor = Color(0xFF1D9BF0),
        sampleCreators = listOf("x", "NASA", "YouTube", "TheRock", "taylorswift13")
    );

    companion object {
        fun fromPackage(pkg: String): SocialPlatform? {
            return entries.firstOrNull { platform ->
                platform.packageNames.any { pkg.startsWith(it) }
            }
        }

        fun fromId(id: String): SocialPlatform {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: TIKTOK
        }
    }
}
