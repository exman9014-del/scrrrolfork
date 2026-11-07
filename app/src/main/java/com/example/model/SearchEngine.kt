package com.example.model

enum class SearchEngine(
    val id: String,
    val displayName: String,
    val shortName: String,
    val domain: String
) {
    GOOGLE("google", "Google Imagens", "Google", "google.com"),
    BING("bing", "Bing Imagens", "Bing", "bing.com"),
    DUCKDUCKGO("duck", "DuckDuckGo (Sem Filtro)", "DuckDuckGo", "duckduckgo.com"),
    YANDEX("yandex", "Yandex Images", "Yandex", "yandex.com"),
    YAHOO("yahoo", "Yahoo Images", "Yahoo", "yahoo.com");

    companion object {
        fun fromId(id: String): SearchEngine {
            return entries.find { it.id.equals(id, ignoreCase = true) } ?: GOOGLE
        }
    }
}
