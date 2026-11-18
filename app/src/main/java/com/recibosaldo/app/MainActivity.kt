package com.recibosaldo.app

import android.app.Application
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import java.io.File
import java.text.NumberFormat
import java.time.LocalDate
import java.util.Locale
import java.util.UUID

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val vm: ReciboViewModel = viewModel()
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Color(0xFF0F6E56),
                    secondary = Color(0xFF1D4E89),
                    background = Color(0xFFF4F7F5)
                )
            ) {
                ReciboScaffold(vm)
            }
        }
    }
}

class ReciboViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = Repository(app)
    var ready by mutableStateOf(repo.isReady())
        private set
    var initialBalance by mutableStateOf(repo.initialBalance())
        private set
    var currency by mutableStateOf(repo.currency())
        private set
    var language by mutableStateOf(repo.language())
        private set
    var budget by mutableStateOf(repo.budget())
        private set
    var items by mutableStateOf(repo.load())
        private set
    var recurring by mutableStateOf(repo.recurring())
        private set

    init {
        val now = LocalDate.now()
        val key = monthKey(now)
        if (repo.lastRecurringMonth() != key) {
            addDueRecurring(now, key)
            repo.setLastRecurringMonth(key)
        }
    }

    fun finishSetup(balance: Double, symbol: String) {
        repo.saveSetup(balance, symbol)
        initialBalance = balance
        currency = symbol.ifBlank { "€" }
        ready = true
    }

    fun updateSettings(balance: Double, symbol: String, newBudget: Double) {
        repo.saveSetup(balance, symbol)
        repo.saveBudget(newBudget)
        initialBalance = balance
        currency = symbol.ifBlank { "€" }
        budget = newBudget
        ready = true
    }

    fun applyLanguage(tag: String) {
        repo.saveLanguage(tag)
        language = tag
        val locales = if (tag.isEmpty()) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(tag)
        }
        AppCompatDelegate.setApplicationLocales(locales)
    }

    fun add(movement: Movement) {
        items = items + movement
        repo.saveAll(items)
    }

    fun update(movement: Movement) {
        items = items.map { if (it.id == movement.id) movement else it }
        repo.saveAll(items)
    }

    fun delete(id: String) {
        items.firstOrNull { it.id == id }?.imagePath?.let { path ->
            if (path.isNotBlank()) File(path).delete()
        }
        items = items.filterNot { it.id == id }
        repo.saveAll(items)
    }

    fun movementsOn(day: java.time.LocalDate): List<Movement> {
        return items.filter { it.epochDay == day.toEpochDay() }
    }

    fun filteredMovements(query: String, month: Long?): List<Movement> {
        val text = query.trim().lowercase(Locale.getDefault())
        return items.filter { movement ->
            val inMonth = month == null || monthKey(LocalDate.ofEpochDay(movement.epochDay)) == month
            val matches = text.isBlank() || listOf(
                movement.merchant,
                movement.note,
                movement.category,
                searchLabel(movement.category),
                movement.amount.toString()
            ).any { it.contains(text, ignoreCase = true) }
            inMonth && matches
        }
    }

    fun filteredDayRows(query: String, month: Long?): List<DayRow> {
        return filteredMovements(query, month)
            .groupBy { it.epochDay }
            .map { (day, list) ->
                DayRow(
                    day = LocalDate.ofEpochDay(day),
                    income = list.filter { !it.isExpense() }.sumOf { it.amount },
                    expense = list.filter { it.isExpense() }.sumOf { it.amount }
                )
            }
            .sortedByDescending { it.day }
    }

    fun movementsOn(day: java.time.LocalDate, query: String, month: Long?): List<Movement> {
        return filteredMovements(query, month).filter { it.epochDay == day.toEpochDay() }
    }

    fun monthExpense(): Double {
        val key = monthKey(LocalDate.now())
        return items.filter {
            it.isExpense() && monthKey(LocalDate.ofEpochDay(it.epochDay)) == key
        }.sumOf { it.amount }
    }

    fun addRecurring(item: Recurring) {
        repo.addRecurring(item)
        recurring = repo.recurring()
        addDueRecurring(LocalDate.now(), monthKey(LocalDate.now()))
    }

    fun removeRecurring(id: String) {
        repo.removeRecurring(id)
        recurring = repo.recurring()
    }

    fun exportBackup(): String = repo.exportBackup()

    fun importBackup(json: String): Boolean {
        if (!repo.importBackup(json)) return false
        items = repo.load()
        recurring = repo.recurring()
        initialBalance = repo.initialBalance()
        currency = repo.currency()
        language = repo.language()
        budget = repo.budget()
        ready = repo.isReady()
        return true
    }

    fun clear() {
        repo.clear()
        items = emptyList()
        initialBalance = 0.0
        currency = "€"
        ready = false
        language = ""
        budget = 0.0
        recurring = emptyList()
    }

    fun balance(): Double {
        val income = items.filter { !it.isExpense() }.sumOf { it.amount }
        val expense = items.filter { it.isExpense() }.sumOf { it.amount }
        return initialBalance + income - expense
    }

    fun dayRows(): List<DayRow> {
        return items.groupBy { it.epochDay }
            .map { (day, list) ->
                DayRow(
                    day = LocalDate.ofEpochDay(day),
                    income = list.filter { !it.isExpense() }.sumOf { it.amount },
                    expense = list.filter { it.isExpense() }.sumOf { it.amount }
                )
            }
            .sortedByDescending { it.day }
    }

    fun expenseByCategory(): List<Pair<String, Double>> {
        return items.filter { it.isExpense() }
            .groupBy { it.category }
            .map { it.key to it.value.sumOf { m -> m.amount } }
            .sortedByDescending { it.second }
    }

    fun money(value: Double): String {
        val format = NumberFormat.getNumberInstance(Locale.getDefault())
        format.minimumFractionDigits = 2
        format.maximumFractionDigits = 2
        return "${format.format(value)} $currency"
    }

    fun signed(value: Double): String {
        val prefix = if (value > 0) "+" else ""
        return prefix + money(value)
    }

    private fun addDueRecurring(today: LocalDate, key: Long) {
        val existing = items.map { it.id }.toSet()
        val note = getApplication<Application>().getString(R.string.recurring_note)
        val additions = recurring.mapNotNull { rule ->
            val id = "rec-${rule.id}-${key}"
            if (existing.contains(id)) return@mapNotNull null
            val day = rule.dayOfMonth.coerceAtMost(today.lengthOfMonth())
            val date = LocalDate.of(today.year, today.month, day)
            Movement(
                id = id,
                epochDay = date.toEpochDay(),
                type = Movement.TYPE_EXPENSE,
                amount = rule.amount,
                category = rule.category,
                merchant = rule.merchant,
                note = note,
                source = Movement.SOURCE_MANUAL
            )
        }
        if (additions.isNotEmpty()) {
            items = items + additions
            repo.saveAll(items)
        }
    }

    companion object {
        fun monthKey(date: LocalDate): Long = date.year * 12L + date.monthValue

        private fun searchLabel(key: String): String = when (key) {
            "food" -> "food alimentacion"
            "transport" -> "transport transporte"
            "home" -> "home hogar"
            "leisure" -> "leisure ocio"
            "health" -> "health salud"
            "clothes" -> "clothes ropa"
            "education" -> "education educacion"
            "bills" -> "bills facturas"
            "salary" -> "salary salario"
            "gift" -> "gift regalo"
            "other_income" -> "other income ingresos"
            else -> "other otros"
        }
    }
}
