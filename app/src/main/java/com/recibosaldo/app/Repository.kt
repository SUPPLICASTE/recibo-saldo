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

data class Recurring(
    val id: String,
    val merchant: String,
    val amount: Double,
    val category: String,
    val dayOfMonth: Int
)

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
    private val recurringFile = File(context.filesDir, "recurring.json")

    fun isReady(): Boolean = prefs.getBoolean(KEY_READY, false)

    fun initialBalance(): Double =
        prefs.getString(KEY_BALANCE, "0")?.toDoubleOrNull() ?: 0.0

    fun currency(): String = prefs.getString(KEY_CURRENCY, "€") ?: "€"

    fun language(): String = prefs.getString(ReciboApp.KEY_LANG, "") ?: ""

    fun budget(): Double = prefs.getString(KEY_BUDGET, "0")?.toDoubleOrNull() ?: 0.0

    fun lastRecurringMonth(): Long = prefs.getLong(KEY_LAST_RECURRING, -1L)

    fun setLastRecurringMonth(value: Long) {
        prefs.edit().putLong(KEY_LAST_RECURRING, value).apply()
    }

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

    fun saveBudget(value: Double) {
        prefs.edit().putString(KEY_BUDGET, value.toString()).apply()
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

    fun recurring(): List<Recurring> {
        if (!recurringFile.exists()) return emptyList()
        return runCatching {
            val array = JSONArray(recurringFile.readText())
            buildList {
                for (i in 0 until array.length()) {
                    val o = array.getJSONObject(i)
                    add(
                        Recurring(
                            id = o.getString("id"),
                            merchant = o.optString("merchant"),
                            amount = o.getDouble("amount"),
                            category = o.optString("category", "bills"),
                            dayOfMonth = o.optInt("dayOfMonth", 1)
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun saveRecurring(items: List<Recurring>) {
        val array = JSONArray()
        items.forEach { r ->
            array.put(
                JSONObject()
                    .put("id", r.id)
                    .put("merchant", r.merchant)
                    .put("amount", r.amount)
                    .put("category", r.category)
                    .put("dayOfMonth", r.dayOfMonth)
            )
        }
        recurringFile.writeText(array.toString())
    }

    fun addRecurring(item: Recurring) {
        saveRecurring(recurring() + item)
    }

    fun removeRecurring(id: String) {
        saveRecurring(recurring().filterNot { it.id == id })
    }

    fun exportBackup(): String {
        val root = JSONObject()
            .put("version", 2)
            .put("ready", isReady())
            .put("balance", initialBalance())
            .put("currency", currency())
            .put("language", language())
            .put("budget", budget())

        val movements = JSONArray()
        load().forEach { m ->
            movements.put(
                JSONObject()
                    .put("id", m.id)
                    .put("epochDay", m.epochDay)
                    .put("type", m.type)
                    .put("amount", m.amount)
                    .put("category", m.category)
                    .put("merchant", m.merchant)
                    .put("note", m.note)
                    .put("source", m.source)
            )
        }
        root.put("movements", movements)

        val recurringItems = JSONArray()
        recurring().forEach { r ->
            recurringItems.put(
                JSONObject()
                    .put("id", r.id)
                    .put("merchant", r.merchant)
                    .put("amount", r.amount)
                    .put("category", r.category)
                    .put("dayOfMonth", r.dayOfMonth)
            )
        }
        root.put("recurring", recurringItems)
        return root.toString()
    }

    fun importBackup(json: String): Boolean = runCatching {
        val root = JSONObject(json)
        val movements = mutableListOf<Movement>()
        val array = root.optJSONArray("movements") ?: JSONArray()
        for (i in 0 until array.length()) {
            val o = array.getJSONObject(i)
            movements.add(
                Movement(
                    id = o.getString("id"),
                    epochDay = o.getLong("epochDay"),
                    type = o.getString("type"),
                    amount = o.getDouble("amount"),
                    category = o.optString("category", "other"),
                    merchant = o.optString("merchant"),
                    note = o.optString("note"),
                    source = o.optString("source", Movement.SOURCE_MANUAL)
                )
            )
        }
        val recurringItems = mutableListOf<Recurring>()
        val recArray = root.optJSONArray("recurring") ?: JSONArray()
        for (i in 0 until recArray.length()) {
            val o = recArray.getJSONObject(i)
            recurringItems.add(
                Recurring(
                    id = o.getString("id"),
                    merchant = o.optString("merchant"),
                    amount = o.getDouble("amount"),
                    category = o.optString("category", "bills"),
                    dayOfMonth = o.optInt("dayOfMonth", 1)
                )
            )
        }
        saveAll(movements)
        saveRecurring(recurringItems)
        prefs.edit()
            .putString(KEY_BALANCE, root.optString("balance", "0"))
            .putString(KEY_CURRENCY, root.optString("currency", "€"))
            .putString(ReciboApp.KEY_LANG, root.optString("language", ""))
            .putString(KEY_BUDGET, root.optString("budget", "0"))
            .putBoolean(KEY_READY, root.optBoolean("ready", true))
            .putLong(KEY_LAST_RECURRING, -1L)
            .apply()
        true
    }.getOrDefault(false)

    fun clear() {
        file.delete()
        recurringFile.delete()
        File(file.parentFile, "receipts").deleteRecursively()
        prefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_READY = "ready"
        private const val KEY_BALANCE = "initial"
        private const val KEY_CURRENCY = "currency"
        private const val KEY_BUDGET = "budget"
        private const val KEY_LAST_RECURRING = "lastRecurring"
    }
}

fun Movement.date(): LocalDate = LocalDate.ofEpochDay(epochDay)
