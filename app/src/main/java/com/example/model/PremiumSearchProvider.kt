package com.example.model

enum class PremiumSearchProvider(
    val id: String,
    val title: String,
    val description: String,
    val requiresImageUrl: Boolean = false
) {
    YANDEX_IMAGES("yandex_images", "Yandex Imagens", "Buscar imagens por texto"),
    YANDEX_REVERSE("yandex_reverse", "Yandex Reverse", "Encontrar imagens semelhantes", true),
    TIKTOK_DISCOVER("tiktok_discover", "TikTok Discover", "Descobrir vídeos e tendências"),
    GOOGLE_SEARCH("google", "Google Search", "Buscar páginas e resultados web"),
    GOOGLE_IMAGES("google_images", "Google Images", "Buscar imagens por texto");
}
