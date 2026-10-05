package com.recibosaldo.app

import java.time.LocalDate
import java.util.Locale

data class ParsedReceipt(
    val amount: Double?,
    val merchant: String?,
    val merchantDetected: Boolean,
    val date: LocalDate?,
    val category: String,
    val raw: String
)

object ReceiptParser {
    private val known = listOf(
        "Mercadona", "Carrefour", "Lidl", "Aldi", "Dia", "Eroski", "Consum",
        "Alcampo", "Ahorramas", "Bonpreu", "Amazon", "El Corte Ingles",
        "El Corte Inglés", "Zara", "Bershka", "Pull&Bear", "Primark", "Mango",
        "Starbucks", "McDonald", "Burger King", "Telepizza", "Glovo",
        "Just Eat", "Uber Eats", "Uber", "Cabify", "Repsol", "Cepsa", "BP",
        "Ikea", "MediaMarkt", "Media Markt", "Fnac", "Decathlon", "Renfe",
        "Walmart", "Target", "Costco", "Tesco", "Sainsbury", "Aldi",
        "Leroy Merlin", "Apple", "Google", "Netflix", "Spotify"
    )

    fun parse(text: String): ParsedReceipt {
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val merchant = findMerchant(text, lines)
        val amount = findAmount(lines)
        val date = findDate(text)
        return ParsedReceipt(
            amount = amount,
            merchant = merchant,
            merchantDetected = merchant != null,
            date = date,
            category = guessCategory(merchant, text),
            raw = text
        )
    }

    private fun findMerchant(text: String, lines: List<String>): String? {
        val folded = text.lowercase(Locale.ROOT)
        return known.firstOrNull { folded.contains(it.lowercase(Locale.ROOT)) }
    }

    private fun findAmount(lines: List<String>): Double? {
        val money = Regex("""(\d{1,6})[.,](\d{2})""")
        fun parse(raw: String): Double? {
            val m = money.find(raw) ?: return null
            return "${m.groupValues[1]}.${m.groupValues[2]}".toDoubleOrNull()
        }
        val totalLine = lines.lastOrNull { line ->
            val l = line.lowercase(Locale.ROOT)
            l.contains("total") || l.contains("importe") || l.contains("amount") || l.contains("suma")
        }
        if (totalLine != null) {
            val values = money.findAll(totalLine).map { parse(it.value) }.filterNotNull().toList()
            if (values.isNotEmpty()) return values.last()
        }
        val all = lines.flatMap { line -> money.findAll(line).map { parse(it.value) }.filterNotNull() }
        return all.maxOrNull()
    }

    private fun findDate(text: String): LocalDate? {
        val m = Regex("""(\d{1,2})[/-](\d{1,2})[/-](\d{2,4})""").find(text) ?: return null
        val day = m.groupValues[1].toIntOrNull() ?: return null
        val month = m.groupValues[2].toIntOrNull() ?: return null
        var year = m.groupValues[3].toIntOrNull() ?: return null
        if (year < 100) year += 2000
        return runCatching { LocalDate.of(year, month, day) }.getOrNull()
    }

    private fun guessCategory(merchant: String?, text: String): String {
        val blob = "${merchant.orEmpty()} $text".lowercase(Locale.ROOT)
        return when {
            listOf("mercadona", "carrefour", "lidl", "aldi", "dia", "eroski", "consum", "super", "market").any { blob.contains(it) } -> "food"
            listOf("repsol", "cepsa", "renfe", "uber", "cabify", "gasolin", "metro").any { blob.contains(it) } -> "transport"
            listOf("ikea", "leroy", "luz", "agua", "endesa", "iberdrola").any { blob.contains(it) } -> "home"
            listOf("netflix", "spotify", "cine", "zara", "primark").any { blob.contains(it) } -> "leisure"
            listOf("farmac", "clinic", "hospital", "sanitas").any { blob.contains(it) } -> "health"
            listOf("zara", "mango", "primark", "h&m").any { blob.contains(it) } -> "clothes"
            listOf("colegio", "school", "universidad", "librer").any { blob.contains(it) } -> "education"
            else -> "other"
        }
    }
}
