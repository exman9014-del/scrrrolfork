package com.example.model

enum class UserPlan(
    val id: String,
    val title: String,
    val subtitle: String,
    val isPremium: Boolean
) {
    BASIC(
        id = "basic",
        title = "Plano Básico",
        subtitle = "Free",
        isPremium = false
    ),
    PREMIUM(
        id = "premium",
        title = "Plano Premium",
        subtitle = "Mais rápido, estável e poderoso",
        isPremium = true
    )
}
