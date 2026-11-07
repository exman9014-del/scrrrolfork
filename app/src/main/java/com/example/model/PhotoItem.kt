package com.example.model

data class PhotoItem(
    val id: String,
    val url: String,
    val title: String = "",
    val sourceDomain: String = "",
    val width: Int = 400,
    val height: Int = 500
)
