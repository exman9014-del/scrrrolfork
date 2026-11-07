package com.example.model

enum class ImageQuality(
    val code: String,
    val title: String,
    val subtitle: String,
    val tbsParam: String
) {
    ANY(
        code = "ANY",
        title = "Qualquer tamanho",
        subtitle = "Sem filtro de resolução; inclui todos os tamanhos disponíveis",
        tbsParam = ""
    ),
    LOW(
        code = "LOW",
        title = "Baixa",
        subtitle = "Filtro de menor tamanho do provedor (ícone)",
        tbsParam = "&tbs=isz:s"
    ),
    MEDIUM(
        code = "MEDIUM",
        title = "Média",
        subtitle = "Equilíbrio entre tamanho dos resultados e velocidade",
        tbsParam = "&tbs=isz:m"
    ),
    HIGH(
        code = "HIGH",
        title = "Alta",
        subtitle = "Prioriza imagens grandes; a resolução varia por resultado",
        tbsParam = "&tbs=isz:l"
    ),
    ULTRA(
        code = "ULTRA",
        title = "Altíssima",
        subtitle = "Prioriza imagens a partir de 4 MP, quando disponíveis",
        tbsParam = "&tbs=isz:lt,islt:qsv"
    );

    companion object {
        fun fromCode(code: String): ImageQuality {
            return entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: HIGH
        }
    }
}
