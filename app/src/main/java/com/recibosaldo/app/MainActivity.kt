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
    var items by mutableStateOf(repo.load())
        private set

    fun finishSetup(balance: Double, symbol: String) {
        repo.saveSetup(balance, symbol)
        initialBalance = balance
        currency = symbol.ifBlank { "€" }
        ready = true
    }

    fun updateSettings(balance: Double, symbol: String) {
        repo.saveSetup(balance, symbol)
        initialBalance = balance
        currency = symbol.ifBlank { "€" }
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

    fun clear() {
        repo.clear()
        items = emptyList()
        initialBalance = 0.0
        currency = "€"
        ready = false
        language = ""
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
}
