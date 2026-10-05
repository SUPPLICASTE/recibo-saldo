@file:OptIn(ExperimentalMaterial3Api::class)

package com.recibosaldo.app

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

private val Green = Color(0xFF0F6E56)
private val Ink = Color(0xFF1C2B27)
private val Paper = Color(0xFFF4F7F5)
private val CardBg = Color(0xFFFFFFFF)
private val ExpenseRed = Color(0xFFB42318)
private val IncomeGreen = Color(0xFF067647)

private val sliceColors = listOf(
    Color(0xFF0F6E56), Color(0xFF1D4E89), Color(0xFFB45309), Color(0xFF7C3AED),
    Color(0xFFBE123C), Color(0xFF0E7490), Color(0xFF4D7C0F), Color(0xFF9A3412),
    Color(0xFF334155)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReciboScaffold(vm: ReciboViewModel) {
    var tab by remember { mutableStateOf(0) }
    if (!vm.ready) {
        SetupDialog(vm)
    }
    Scaffold(
        containerColor = Paper,
        bottomBar = {
            NavigationBar(containerColor = CardBg) {
                NavigationBarItem(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    icon = { Icon(Icons.Default.Home, null) },
                    label = { Text(stringResource(R.string.tab_home)) }
                )
                NavigationBarItem(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    icon = { Icon(Icons.Default.Add, null) },
                    label = { Text(stringResource(R.string.tab_add)) }
                )
                NavigationBarItem(
                    selected = tab == 2,
                    onClick = { tab = 2 },
                    icon = { Icon(Icons.Default.PieChart, null) },
                    label = { Text(stringResource(R.string.tab_stats)) }
                )
                NavigationBarItem(
                    selected = tab == 3,
                    onClick = { tab = 3 },
                    icon = { Icon(Icons.Default.Settings, null) },
                    label = { Text(stringResource(R.string.tab_settings)) }
                )
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (tab) {
                0 -> HomeScreen(vm)
                1 -> AddScreen(vm)
                2 -> StatsScreen(vm)
                else -> SettingsScreen(vm)
            }
        }
    }
}

@Composable
private fun SetupDialog(vm: ReciboViewModel) {
    var amount by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("€") }
    AlertDialog(
        onDismissRequest = {},
        title = { Text(stringResource(R.string.setup_title)) },
        text = {
            Column {
                Text(stringResource(R.string.setup_body))
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text(stringResource(R.string.initial_balance)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = currency,
                    onValueChange = { currency = it },
                    label = { Text(stringResource(R.string.currency)) },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                vm.finishSetup(amount.replace(',', '.').toDoubleOrNull() ?: 0.0, currency)
            }) { Text(stringResource(R.string.continue_btn)) }
        }
    )
}

@Composable
fun HomeScreen(vm: ReciboViewModel) {
    val days = vm.dayRows()
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Green),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(18.dp)) {
                Text(stringResource(R.string.balance), color = Color.White.copy(alpha = 0.85f))
                Text(
                    vm.money(vm.balance()),
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(stringResource(R.string.welcome_hint), color = Color.White.copy(alpha = 0.85f))
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .background(Color(0xFFE7EEF0), RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(stringResource(R.string.day), fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), color = Ink)
            Text(stringResource(R.string.total), fontWeight = FontWeight.Bold, color = Ink)
        }
        Spacer(Modifier.height(8.dp))
        if (days.isEmpty()) {
            Text(stringResource(R.string.empty_days), color = Ink, modifier = Modifier.padding(8.dp))
        } else {
            Text(stringResource(R.string.tap_day), color = Ink, modifier = Modifier.padding(bottom = 8.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(days, key = { it.day.toString() }) { row ->
                    DayRow(row, vm)
                }
            }
        }
    }
}

@Composable
private fun DayRow(row: DayRow, vm: ReciboViewModel) {
    var open by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Movement?>(null) }
    var viewing by remember { mutableStateOf<String?>(null) }
    val movements = vm.movementsOn(row.day)
    Card(colors = CardDefaults.cardColors(containerColor = CardBg), shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.clickable { open = !open }.padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    row.day.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                    modifier = Modifier.weight(1f),
                    fontWeight = FontWeight.SemiBold,
                    color = Ink
                )
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "${stringResource(R.string.received)}: ${vm.money(row.income)}",
                        color = IncomeGreen,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "${stringResource(R.string.spent)}: ${vm.money(row.expense)}",
                        color = ExpenseRed,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "${stringResource(R.string.net)}: ${vm.signed(row.income - row.expense)}",
                        fontWeight = FontWeight.Bold,
                        color = Ink
                    )
                }
            }
            if (open) {
                Spacer(Modifier.height(10.dp))
                movements.forEach { movement ->
                    MovementLine(movement, vm, onEdit = { editing = movement }, onView = { viewing = movement.imagePath })
                }
            }
        }
    }
    editing?.let { movement ->
        EditMovementDialog(
            movement = movement,
            onDismiss = { editing = null },
            onSave = {
                vm.update(it)
                editing = null
            },
            onDelete = {
                vm.delete(movement.id)
                editing = null
            }
        )
    }
    viewing?.let { path ->
        TicketDialog(path) { viewing = null }
    }
}

@Composable
private fun MovementLine(
    movement: Movement,
    vm: ReciboViewModel,
    onEdit: () -> Unit,
    onView: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                movement.merchant.ifBlank { stringResource(R.string.no_company) },
                fontWeight = FontWeight.SemiBold,
                color = Ink
            )
            Text(
                "${categoryLabel(movement.category)} · ${vm.signed(if (movement.isExpense()) -movement.amount else movement.amount)}",
                style = MaterialTheme.typography.bodySmall
            )
        }
        if (movement.imagePath.isNotBlank() && File(movement.imagePath).exists()) {
            TextButton(onClick = onView) { Text(stringResource(R.string.view_ticket)) }
        }
        TextButton(onClick = onEdit) { Text(stringResource(R.string.edit)) }
    }
}

@Composable
fun AddScreen(vm: ReciboViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isExpense by remember { mutableStateOf(true) }
    var amount by remember { mutableStateOf("") }
    var merchant by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("other") }
    var date by remember { mutableStateOf(LocalDate.now()) }
    var error by remember { mutableStateOf<String?>(null) }
    var reading by remember { mutableStateOf(false) }
    var review by remember { mutableStateOf<ParsedReceipt?>(null) }
    var reviewImage by remember { mutableStateOf<Uri?>(null) }
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    val invalid = stringResource(R.string.invalid_amount)
    val failed = stringResource(R.string.ocr_failed)

    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            scope.launch {
                reading = true
                reviewImage = uri
                review = readReceipt(context, uri) ?: ParsedReceipt(null, null, false, null, "other", "")
                if (review?.raw.isNullOrBlank()) error = failed
                reading = false
            }
        }
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val uri = photoUri
        if (ok && uri != null) {
            scope.launch {
                reading = true
                reviewImage = uri
                review = readReceipt(context, uri) ?: ParsedReceipt(null, null, false, null, "other", "")
                if (review?.raw.isNullOrBlank()) error = failed
                reading = false
            }
        }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            val uri = newReceiptUri(context)
            photoUri = uri
            camera.launch(uri)
        } else {
            error = context.getString(R.string.camera_denied)
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(stringResource(R.string.add_manual), style = MaterialTheme.typography.titleLarge, color = Ink)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = isExpense, onClick = {
                isExpense = true
                category = "other"
            }, label = { Text(stringResource(R.string.expense)) })
            FilterChip(selected = !isExpense, onClick = {
                isExpense = false
                category = "salary"
            }, label = { Text(stringResource(R.string.income)) })
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = amount,
            onValueChange = { amount = it },
            label = { Text(stringResource(R.string.amount)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        OutlinedTextField(
            value = merchant,
            onValueChange = { merchant = it },
            label = { Text(stringResource(R.string.company)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            label = { Text(stringResource(R.string.note)) },
            modifier = Modifier.fillMaxWidth()
        )
        Text(stringResource(R.string.category), modifier = Modifier.padding(top = 8.dp), color = Ink)
        CategoryChips(
            keys = if (isExpense) Categories.expenses else Categories.incomes,
            selected = category,
            onSelect = { category = it }
        )
        Text(
            "${stringResource(R.string.date)}: ${date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}",
            modifier = Modifier.padding(vertical = 8.dp),
            color = Ink
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { date = date.minusDays(1) }) { Text("-") }
            OutlinedButton(onClick = { date = LocalDate.now() }) { Text(stringResource(R.string.date)) }
            OutlinedButton(onClick = { date = date.plusDays(1) }) { Text("+") }
        }
        error?.let { Text(it, color = ExpenseRed, modifier = Modifier.padding(top = 8.dp)) }
        if (reading) Text(stringResource(R.string.reading_receipt), modifier = Modifier.padding(top = 8.dp))
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = {
                val value = amount.replace(',', '.').toDoubleOrNull()
                if (value == null || value <= 0.0) {
                    error = invalid
                } else {
                    vm.add(
                        Movement(
                            id = UUID.randomUUID().toString(),
                            epochDay = date.toEpochDay(),
                            type = if (isExpense) Movement.TYPE_EXPENSE else Movement.TYPE_INCOME,
                            amount = value,
                            category = category,
                            merchant = merchant.trim(),
                            note = note.trim(),
                            source = Movement.SOURCE_MANUAL
                        )
                    )
                    amount = ""
                    merchant = ""
                    note = ""
                    error = null
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text(stringResource(R.string.save)) }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = {
                val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
                if (granted) {
                    val uri = newReceiptUri(context)
                    photoUri = uri
                    camera.launch(uri)
                } else {
                    permission.launch(Manifest.permission.CAMERA)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text(stringResource(R.string.take_photo)) }
        OutlinedButton(
            onClick = { gallery.launch("image/*") },
            modifier = Modifier.fillMaxWidth()
        ) { Text(stringResource(R.string.pick_gallery)) }
    }

    review?.let { parsed ->
        ReceiptEditor(
            parsed = parsed,
            imageUri = reviewImage,
            onDismiss = {
                review = null
                reviewImage = null
            },
            onSave = { movement ->
                vm.add(movement)
                review = null
                reviewImage = null
            }
        )
    }
}

@Composable
private fun ReceiptEditor(
    parsed: ParsedReceipt,
    imageUri: Uri?,
    onDismiss: () -> Unit,
    onSave: (Movement) -> Unit
) {
    val context = LocalContext.current
    var amount by remember { mutableStateOf(parsed.amount?.toString().orEmpty()) }
    var merchant by remember { mutableStateOf(parsed.merchant.orEmpty()) }
    var category by remember { mutableStateOf(parsed.category) }
    var date by remember { mutableStateOf(parsed.date ?: LocalDate.now()) }
    var error by remember { mutableStateOf<String?>(null) }
    val invalid = stringResource(R.string.invalid_amount)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_receipt)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                imageUri?.let { TicketPreview(it.toString(), fromUri = true) }
                Text(
                    if (parsed.merchantDetected) stringResource(R.string.merchant_found)
                    else stringResource(R.string.merchant_missing),
                    color = if (parsed.merchantDetected) IncomeGreen else ExpenseRed
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text(stringResource(R.string.amount)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                OutlinedTextField(
                    value = merchant,
                    onValueChange = { merchant = it },
                    label = { Text(stringResource(R.string.company)) },
                    singleLine = true
                )
                Text(stringResource(R.string.category))
                CategoryChips(Categories.expenses, category) { category = it }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { date = date.minusDays(1) }) { Text("-") }
                    Text(date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                    TextButton(onClick = { date = date.plusDays(1) }) { Text("+") }
                }
                error?.let { Text(it, color = ExpenseRed) }
            }
        },
        confirmButton = {
            Button(onClick = {
                val value = amount.replace(',', '.').toDoubleOrNull()
                if (value == null || value <= 0.0) {
                    error = invalid
                } else {
                    val id = UUID.randomUUID().toString()
                    val path = imageUri?.let { copyReceiptImage(context, it, id) }.orEmpty()
                    onSave(
                        Movement(
                            id = id,
                            epochDay = date.toEpochDay(),
                            type = Movement.TYPE_EXPENSE,
                            amount = value,
                            category = category,
                            merchant = merchant.trim(),
                            note = "",
                            source = Movement.SOURCE_PHOTO,
                            imagePath = path
                        )
                    )
                }
            }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

@Composable
private fun EditMovementDialog(
    movement: Movement,
    onDismiss: () -> Unit,
    onSave: (Movement) -> Unit,
    onDelete: () -> Unit
) {
    var amount by remember { mutableStateOf(movement.amount.toString()) }
    var merchant by remember { mutableStateOf(movement.merchant) }
    var note by remember { mutableStateOf(movement.note) }
    var category by remember { mutableStateOf(movement.category) }
    var date by remember { mutableStateOf(LocalDate.ofEpochDay(movement.epochDay)) }
    var isExpense by remember { mutableStateOf(movement.isExpense()) }
    var error by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    var viewing by remember { mutableStateOf(false) }
    val invalid = stringResource(R.string.invalid_amount)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                if (movement.imagePath.isNotBlank() && File(movement.imagePath).exists()) {
                    TicketPreview(movement.imagePath, fromUri = false)
                    TextButton(onClick = { viewing = true }) { Text(stringResource(R.string.view_ticket)) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = isExpense, onClick = { isExpense = true }, label = { Text(stringResource(R.string.expense)) })
                    FilterChip(selected = !isExpense, onClick = { isExpense = false }, label = { Text(stringResource(R.string.income)) })
                }
                OutlinedTextField(amount, { amount = it }, label = { Text(stringResource(R.string.amount)) }, singleLine = true)
                OutlinedTextField(merchant, { merchant = it }, label = { Text(stringResource(R.string.company)) }, singleLine = true)
                OutlinedTextField(note, { note = it }, label = { Text(stringResource(R.string.note)) })
                CategoryChips(if (isExpense) Categories.expenses else Categories.incomes, category) { category = it }
                Row {
                    TextButton(onClick = { date = date.minusDays(1) }) { Text("-") }
                    Text(date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                    TextButton(onClick = { date = date.plusDays(1) }) { Text("+") }
                }
                error?.let { Text(it, color = ExpenseRed) }
                TextButton(onClick = { confirmDelete = true }) { Text(stringResource(R.string.delete)) }
            }
        },
        confirmButton = {
            Button(onClick = {
                val value = amount.replace(',', '.').toDoubleOrNull()
                if (value == null || value <= 0.0) error = invalid
                else onSave(
                    movement.copy(
                        epochDay = date.toEpochDay(),
                        type = if (isExpense) Movement.TYPE_EXPENSE else Movement.TYPE_INCOME,
                        amount = value,
                        category = category,
                        merchant = merchant.trim(),
                        note = note.trim()
                    )
                )
            }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.delete)) },
            text = { Text(stringResource(R.string.delete_confirm)) },
            confirmButton = { Button(onClick = onDelete) { Text(stringResource(R.string.delete)) } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) } }
        )
    }
    if (viewing) TicketDialog(movement.imagePath) { viewing = false }
}

@Composable
private fun TicketPreview(source: String, fromUri: Boolean, imageHeight: androidx.compose.ui.unit.Dp = 160.dp) {
    val context = LocalContext.current
    val bitmap = remember(source) {
        runCatching {
            val raw = if (fromUri) {
                context.contentResolver.openInputStream(Uri.parse(source))?.use { BitmapFactory.decodeStream(it) }
            } else {
                BitmapFactory.decodeFile(source)
            }
            raw?.asImageBitmap()
        }.getOrNull()
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = stringResource(R.string.view_ticket),
            modifier = Modifier.fillMaxWidth().height(imageHeight),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
private fun TicketDialog(path: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.view_ticket)) },
        text = { TicketPreview(path, fromUri = false, imageHeight = 360.dp) },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}

@Composable
private fun CategoryChips(keys: List<String>, selected: String, onSelect: (String) -> Unit) {
    Column {
        keys.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 2.dp)) {
                row.forEach { key ->
                    FilterChip(
                        selected = selected == key,
                        onClick = { onSelect(key) },
                        label = { Text(categoryLabel(key)) }
                    )
                }
            }
        }
    }
}

@Composable
fun StatsScreen(vm: ReciboViewModel) {
    val slices = vm.expenseByCategory()
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(stringResource(R.string.stats_title), style = MaterialTheme.typography.titleLarge, color = Ink)
        Spacer(Modifier.height(12.dp))
        if (slices.isEmpty()) {
            Text(stringResource(R.string.stats_empty), color = Ink)
            return@Column
        }
        val total = slices.sumOf { it.second }
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(200.dp)) {
                var start = -90f
                slices.forEachIndexed { index, slice ->
                    val sweep = (slice.second / total * 360.0).toFloat()
                    drawArc(
                        color = sliceColors[index % sliceColors.size],
                        startAngle = start,
                        sweepAngle = sweep,
                        useCenter = true
                    )
                    start += sweep
                }
                drawCircle(Color.White, radius = size.minDimension * 0.28f)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.flow_title), fontWeight = FontWeight.Bold, color = Ink)
        Text(stringResource(R.string.most_spent), color = Ink)
        Spacer(Modifier.height(8.dp))
        slices.forEachIndexed { index, (key, value) ->
            val pct = value / total
            Card(colors = CardDefaults.cardColors(containerColor = CardBg), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(14.dp)
                                .background(sliceColors[index % sliceColors.size], RoundedCornerShape(4.dp))
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(categoryLabel(key), fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        Text(vm.money(value))
                    }
                    Spacer(Modifier.height(6.dp))
                    Box(
                        Modifier
                            .fillMaxWidth(pct.toFloat().coerceAtLeast(0.05f))
                            .height(10.dp)
                            .background(sliceColors[index % sliceColors.size], RoundedCornerShape(8.dp))
                    )
                    Text("${(pct * 100).toInt()} %")
                }
            }
            if (index < slices.lastIndex) {
                Canvas(Modifier.height(22.dp).fillMaxWidth()) {
                    val x = size.width / 2f
                    drawLine(Green, Offset(x, 0f), Offset(x, size.height), strokeWidth = 4f)
                    drawCircle(Green, radius = 5f, center = Offset(x, size.height - 4f))
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(vm: ReciboViewModel) {
    var balance by remember { mutableStateOf(vm.initialBalance.toString()) }
    var currency by remember { mutableStateOf(vm.currency) }
    var confirm by remember { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(stringResource(R.string.tab_settings), style = MaterialTheme.typography.titleLarge, color = Ink)
        Text(stringResource(R.string.language), fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = vm.language == "", onClick = { vm.applyLanguage("") }, label = { Text(stringResource(R.string.system_lang)) })
            FilterChip(selected = vm.language == "es", onClick = { vm.applyLanguage("es") }, label = { Text(stringResource(R.string.spanish)) })
            FilterChip(selected = vm.language == "en", onClick = { vm.applyLanguage("en") }, label = { Text(stringResource(R.string.english)) })
        }
        OutlinedTextField(
            value = balance,
            onValueChange = { balance = it },
            label = { Text(stringResource(R.string.initial_balance)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = currency,
            onValueChange = { currency = it },
            label = { Text(stringResource(R.string.currency)) },
            modifier = Modifier.fillMaxWidth()
        )
        Button(onClick = {
            vm.updateSettings(balance.replace(',', '.').toDoubleOrNull() ?: 0.0, currency)
        }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.save_settings))
        }
        OutlinedButton(onClick = { confirm = true }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.clear_data))
        }
    }
    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text(stringResource(R.string.clear_data)) },
            text = { Text(stringResource(R.string.clear_confirm)) },
            confirmButton = {
                Button(onClick = {
                    vm.clear()
                    confirm = false
                    balance = "0.0"
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { confirm = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

@Composable
fun categoryLabel(key: String): String = when (key) {
    "food" -> stringResource(R.string.cat_food)
    "transport" -> stringResource(R.string.cat_transport)
    "home" -> stringResource(R.string.cat_home)
    "leisure" -> stringResource(R.string.cat_leisure)
    "health" -> stringResource(R.string.cat_health)
    "clothes" -> stringResource(R.string.cat_clothes)
    "education" -> stringResource(R.string.cat_education)
    "bills" -> stringResource(R.string.cat_bills)
    "salary" -> stringResource(R.string.cat_salary)
    "gift" -> stringResource(R.string.cat_gift)
    "other_income" -> stringResource(R.string.cat_other_income)
    else -> stringResource(R.string.cat_other)
}

data class DayRow(val day: LocalDate, val income: Double, val expense: Double)

private fun newReceiptUri(context: android.content.Context): Uri {
    val dir = File(context.cacheDir, "receipts").apply { mkdirs() }
    val file = File(dir, "receipt_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

private fun copyReceiptImage(context: android.content.Context, uri: Uri, id: String): String {
    val dir = File(context.filesDir, "receipts").apply { mkdirs() }
    val dest = File(dir, "$id.jpg")
    context.contentResolver.openInputStream(uri)?.use { input ->
        dest.outputStream().use { output -> input.copyTo(output) }
    }
    return dest.absolutePath
}

private suspend fun readReceipt(context: android.content.Context, uri: Uri): ParsedReceipt? {
    return runCatching {
        val image = InputImage.fromFilePath(context, uri)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val text = recognizer.process(image).await().text
        recognizer.close()
        ReceiptParser.parse(text)
    }.getOrNull()
}
