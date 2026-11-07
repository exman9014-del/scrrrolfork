package com.example.model

enum class SearchType(
    val id: String,
    val displayName: String,
    val googleParam: String
) {
    IMAGES(
        id = "images",
        displayName = "Imagens",
        googleParam = "&udm=2"
    ),
    VIDEOS(
        id = "videos",
        displayName = "Vídeos",
        googleParam = "&tbm=vid"
    ),
    ALL_WEB(
        id = "web",
        displayName = "Sites",
        googleParam = ""
    ),
    NEWS(
        id = "news",
        displayName = "Notícias",
        googleParam = "&tbm=nws"
    )
}
