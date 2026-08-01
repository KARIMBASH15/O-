package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.file.ExcelCell
import com.example.data.file.ExcelData
import com.example.ui.theme.ExcelGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExcelViewerScreen(
    data: ExcelData?,
    fileName: String,
    isCloudEdit: Boolean,
    onCellEdit: (rowIndex: Int, colIndex: Int, value: String, formula: String) -> Unit,
    onAddRow: () -> Unit,
    onAddColumn: (String) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var editingCellLocation by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var editCellValue by remember { mutableStateOf("") }
    var editCellFormula by remember { mutableStateOf("") }

    var showAddColDialog by remember { mutableStateOf(false) }
    var newColName by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.TableChart,
                                contentDescription = null,
                                tint = ExcelGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = fileName,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (isCloudEdit) {
                            Text(
                                text = "☁️ وضع التعديل السحابي الفوري (FTP Cloud)",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("btn_excel_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddColDialog = true }) {
                        Icon(Icons.Default.ViewColumn, contentDescription = "إضافة عمود", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = onAddRow) {
                        Icon(Icons.Default.TableRows, contentDescription = "إضافة صف", tint = MaterialTheme.colorScheme.primary)
                    }
                    Button(
                        onClick = onSave,
                        colors = ButtonDefaults.buttonColors(containerColor = ExcelGreen),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("btn_save_excel")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isCloudEdit) "حفظ ورفع" else "حفظ")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (data == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            val horizontalScrollState = rememberScrollState()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Formula Bar Indicator
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "fx",
                            fontWeight = FontWeight.Bold,
                            color = ExcelGreen,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        val selectedText = editingCellLocation?.let { (r, c) ->
                            if (r < data.rows.size && c < data.rows[r].cells.size) {
                                val cell = data.rows[r].cells[c]
                                if (cell.formula.isNotEmpty()) cell.formula else cell.value
                            } else ""
                        } ?: "انقر على أي خلية لتعديل قيمتها أو المعادلة (مثال: SUM= أو AVERAGE=)"

                        Text(
                            text = selectedText,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Grid Spreadsheet View
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .horizontalScroll(horizontalScrollState)
                ) {
                    LazyColumn(modifier = Modifier.fillMaxHeight()) {
                        // Header Row
                        item {
                            Row(
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                            ) {
                                // Row Index Header
                                CellItem(
                                    text = "#",
                                    isHeader = true,
                                    width = 40.dp
                                )
                                data.headers.forEach { header ->
                                    CellItem(
                                        text = header,
                                        isHeader = true,
                                        width = 110.dp
                                    )
                                }
                            }
                        }

                        // Rows List
                        itemsIndexed(data.rows) { rIdx, row ->
                            Row(
                                modifier = Modifier.border(
                                    0.5.dp,
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                )
                            ) {
                                // Row Index
                                CellItem(
                                    text = "${rIdx + 1}",
                                    isHeader = true,
                                    width = 40.dp
                                )

                                data.headers.forEachIndexed { cIdx, _ ->
                                    val cell = if (cIdx < row.cells.size) row.cells[cIdx] else ExcelCell()
                                    val displayVal = if (cell.formula.isNotEmpty()) {
                                        data.evaluateFormula(cell.formula)
                                    } else {
                                        cell.value
                                    }

                                    CellItem(
                                        text = displayVal,
                                        isHeader = false,
                                        isSelected = editingCellLocation == Pair(rIdx, cIdx),
                                        width = 110.dp,
                                        onClick = {
                                            editingCellLocation = Pair(rIdx, cIdx)
                                            editCellValue = cell.value
                                            editCellFormula = cell.formula
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Quick Add Row Floating Bar
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shadowElevation = 4.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "إجمالي الصفوف: ${data.rows.size} | الأعمدة: ${data.headers.size}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedButton(
                            onClick = onAddRow,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("صف جديد")
                        }
                    }
                }
            }
        }
    }

    // Cell Edit Dialog
    editingCellLocation?.let { (r, c) ->
        AlertDialog(
            onDismissRequest = { editingCellLocation = null },
            title = { Text("تعديل الخلية (${r + 1}, ${data?.headers?.getOrNull(c) ?: (c + 1)})") },
            text = {
                Column {
                    OutlinedTextField(
                        value = editCellValue,
                        onValueChange = { editCellValue = it },
                        label = { Text("القيمة النصية / الرقم") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_cell_value")
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editCellFormula,
                        onValueChange = { editCellFormula = it },
                        label = { Text("المعادلة (مثال: SUM(B1:B5=)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onCellEdit(r, c, editCellValue, editCellFormula)
                        editingCellLocation = null
                    },
                    modifier = Modifier.testTag("btn_confirm_cell_edit")
                ) {
                    Text("حفظ الخلية")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingCellLocation = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Add Column Dialog
    if (showAddColDialog) {
        AlertDialog(
            onDismissRequest = { showAddColDialog = false },
            title = { Text("إضافة عمود جديد") },
            text = {
                OutlinedTextField(
                    value = newColName,
                    onValueChange = { newColName = it },
                    label = { Text("اسم العمود (اختياري)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onAddColumn(newColName.trim())
                        newColName = ""
                        showAddColDialog = false
                    }
                ) {
                    Text("إضافة")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddColDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
private fun CellItem(
    text: String,
    isHeader: Boolean,
    width: androidx.compose.ui.unit.Dp,
    isSelected: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .width(width)
            .height(38.dp)
            .background(
                when {
                    isSelected -> ExcelGreen.copy(alpha = 0.25f)
                    isHeader -> MaterialTheme.colorScheme.surfaceVariant
                    else -> MaterialTheme.colorScheme.surface
                }
            )
            .border(
                0.5.dp,
                if (isSelected) ExcelGreen else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            )
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(horizontal = 6.dp),
        contentAlignment = if (isHeader) Alignment.Center else Alignment.CenterStart
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = if (isHeader || isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isHeader) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = if (isHeader) TextAlign.Center else TextAlign.Start
        )
    }
}
