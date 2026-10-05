package com.recibosaldo.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDate

data class Movement(
    val id: String,
    val epochDay: Long,
    val type: String,
    val amount: Double,
    val category: String,
    val merchant: String,
    val note: String,
    val source: String,
    val imagePath: String = ""
) {
    fun isExpense() = type == TYPE_EXPENSE

    companion object {
        const val TYPE_EXPENSE = "expense"
        const val TYPE_INCOME = "income"
        const val SOURCE_MANUAL = "manual"
        const val SOURCE_PHOTO = "photo"
    }
}

object Categories {
    val expenses = listOf(
        "food", "transport", "home", "leisure", "health",
        "clothes", "education", "bills", "other"
    )
    val incomes = listOf("salary", "gift", "other_income")
}

class Repository(context: Context) {
    private val prefs = context.getSharedPreferences(ReciboApp.PREFS, Context.MODE_PRIVATE)
    private val file = File(context.filesDir, "movements.json")

    fun isReady(): Boolean = prefs.getBoolean(KEY_READY, false)

    fun initialBalance(): Double =
        prefs.getString(KEY_BALANCE, "0")?.toDoubleOrNull() ?: 0.0

    fun currency(): String = prefs.getString(KEY_CURRENCY, "€") ?: "€"

    fun language(): String = prefs.getString(ReciboApp.KEY_LANG, "") ?: ""

    fun saveSetup(balance: Double, currency: String) {
        prefs.edit()
            .putString(KEY_BALANCE, balance.toString())
            .putString(KEY_CURRENCY, currency.ifBlank { "€" })
            .putBoolean(KEY_READY, true)
            .apply()
    }

    fun saveCurrency(currency: String) {
        prefs.edit().putString(KEY_CURRENCY, currency.ifBlank { "€" }).apply()
    }

    fun saveLanguage(tag: String) {
        prefs.edit().putString(ReciboApp.KEY_LANG, tag).apply()
    }

    fun load(): List<Movement> {
        if (!file.exists()) return emptyList()
        return runCatching {
            val array = JSONArray(file.readText())
            buildList {
                for (i in 0 until array.length()) {
                    val o = array.getJSONObject(i)
                    add(
                        Movement(
                            id = o.getString("id"),
                            epochDay = o.getLong("epochDay"),
                            type = o.getString("type"),
                            amount = o.getDouble("amount"),
                            category = o.getString("category"),
                            merchant = o.optString("merchant"),
                            note = o.optString("note"),
                            source = o.optString("source", Movement.SOURCE_MANUAL),
                            imagePath = o.optString("imagePath")
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun saveAll(items: List<Movement>) {
        val array = JSONArray()
        items.forEach { m ->
            array.put(
                JSONObject()
                    .put("id", m.id)
                    .put("epochDay", m.epochDay)
                    .put("type", m.type)
                    .put("amount", m.amount)
                    .put("category", m.category)
                    .put("merchant", m.merchant)
                    .put("note", m.note)
                    .put("source", m.source)
                    .put("imagePath", m.imagePath)
            )
        }
        file.writeText(array.toString())
    }

    fun clear() {
        file.delete()
        File(file.parentFile, "receipts").deleteRecursively()
        prefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_READY = "ready"
        private const val KEY_BALANCE = "initial"
        private const val KEY_CURRENCY = "currency"
    }
}

fun Movement.date(): LocalDate = LocalDate.ofEpochDay(epochDay)
