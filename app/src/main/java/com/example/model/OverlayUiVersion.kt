package com.example.model

enum class OverlayUiVersion(
    val code: String,
    val title: String,
    val subtitle: String,
    val isCleanPhotoOnly: Boolean = false
) {
    V1("V1", "Barra Inferior", "Cards e busca rápida na base", false),
    V2("V2", "Sidebar Lateral", "Gaveta vertical fixada na borda", false),
    V5("V5", "Bottom Sheet", "Painel com Google Imagens redimensionável", false),
    V6("V6", "Fotos Vertical", "Grade vertical com controles de busca", false),
    V7("V7", "Fotos Horizontal", "Carrossel deslizante com controles", false),

    F1("F1", "Barra Fotos Pura", "Só fotos grandes na base (sem header/Google)", true),
    F2("F2", "Sidebar Fotos Pura", "Só fotos verticais na lateral (sem bordas/Google)", true),
    F5("F5", "Bottom Sheet Fotos", "Só fotos em alta qualidade (redimensionável)", true),
    F6("F6", "Vertical Fotos Pura", "Grade vertical limpa só com imagens grandes", true),
    F7("F7", "Swipe Horizontal Puro", "Deslize lateral em tela cheia só com fotos", true);

    companion object {
        fun fromCode(code: String): OverlayUiVersion {
            return entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: F7
        }
    }
}
