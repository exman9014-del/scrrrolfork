package com.example.model

enum class ImageQuality(
    val code: String,
    val title: String,
    val subtitle: String,
    val tbsParam: String
) {
    LOW(
        code = "LOW",
        title = "Baixa",
        subtitle = "Carregamento ultra-rápido, economia de dados",
        tbsParam = "&tbs=isz:s"
    ),
    MEDIUM(
        code = "MEDIUM",
        title = "Média",
        subtitle = "Equilíbrio padrão entre velocidade e nitidez",
        tbsParam = "&tbs=isz:m"
    ),
    HIGH(
        code = "HIGH",
        title = "Alta",
        subtitle = "Alta definição e detalhes nítidos (HD)",
        tbsParam = "&tbs=isz:l"
    ),
    ULTRA(
        code = "ULTRA",
        title = "Altíssima",
        subtitle = "Resolução máxima e detalhes completos",
        tbsParam = "&tbs=isz:lt,islt:qsv"
    );

    companion object {
        fun fromCode(code: String): ImageQuality {
            return entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: HIGH
        }
    }
}
