package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.local.FtpConnectionEntity

@Composable
fun CreateFolderDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إنشاء مجلد جديد", fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("اسم المجلد") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_folder_name")
            )
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name.trim()) },
                modifier = Modifier.testTag("btn_confirm_create_folder")
            ) {
                Text("إنشاء")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

@Composable
fun CreateFileDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("EXCEL") } // "EXCEL" or "TEXT"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إنشاء ملف جديد", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    FilterChip(
                        selected = selectedType == "EXCEL",
                        onClick = { selectedType = "EXCEL" },
                        label = { Text("جدول بيانات (Excel/CSV)") }
                    )
                    FilterChip(
                        selected = selectedType == "TEXT",
                        onClick = { selectedType = "TEXT" },
                        label = { Text("ملف نصي (.txt)") }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(if (selectedType == "EXCEL") "اسم الملف (مثال: جدول_المبيعات.csv)" else "اسم الملف (مثال: ملاحظات.txt)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        var finalName = name.trim()
                        if (selectedType == "EXCEL" && !finalName.endsWith(".csv") && !finalName.endsWith(".xlsx")) {
                            finalName += ".csv"
                        } else if (selectedType == "TEXT" && !finalName.contains(".")) {
                            finalName += ".txt"
                        }
                        onConfirm(finalName, selectedType)
                    }
                }
            ) {
                Text("إنشاء")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

@Composable
fun EncryptDialog(
    fileName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تشفير آمن AES-256", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("تشفير الملف '$fileName' باستخدام خوارزمية AES-256 وكلمة مرور سرية.")
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("كلمة المرور") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("تأكيد كلمة المرور") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )

                errorText?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (password.length < 4) {
                        errorText = "كلمة المرور يجب أن تكون 4 أحرف على الأقل"
                    } else if (password != confirmPassword) {
                        errorText = "كلمتا المرور غير متطابقتين"
                    } else {
                        onConfirm(password)
                    }
                }
            ) {
                Text("تشفير")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

@Composable
fun DecryptDialog(
    fileName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("فك تشفير الملف", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("أدخل كلمة المرور السرية لفك تشفير '$fileName':")
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("كلمة المرور السرية") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (password.isNotBlank()) onConfirm(password) }
            ) {
                Text("فك التشفير")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

@Composable
fun AddFtpDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, Int, String, String, Boolean) -> Unit
) {
    var name by remember { mutableStateOf("سيرفر الشركة") }
    var host by remember { mutableStateOf("ftp.example.com") }
    var portText by remember { mutableStateOf("21") }
    var username by remember { mutableStateOf("admin") }
    var password by remember { mutableStateOf("") }
    var isPassive by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة سيرفر FTP جديد", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم الاتصال") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = host,
                        onValueChange = { host = it },
                        label = { Text("عنوان الهوست (IP / Domain)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    OutlinedTextField(
                        value = portText,
                        onValueChange = { portText = it },
                        label = { Text("البورت") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(80.dp)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("اسم المستخدم") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("كلمة المرور") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isPassive,
                        onCheckedChange = { isPassive = it }
                    )
                    Text("وضع الاتصال السلبي (Passive Mode)")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (host.isNotBlank() && username.isNotBlank()) {
                        val port = portText.toIntOrNull() ?: 21
                        onConfirm(name.ifBlank { host }, host.trim(), port, username.trim(), password, isPassive)
                    }
                }
            ) {
                Text("حفظ الاتصال")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

@Composable
fun AddSyncJobDialog(
    connections: List<FtpConnectionEntity>,
    localCurrentPath: String,
    onDismiss: () -> Unit,
    onConfirm: (Long, String, String, String, Boolean) -> Unit
) {
    if (connections.isEmpty()) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("لا يوجد سيرفر FTP") },
            text = { Text("يرجى إضافة سيرفر FTP أولاً لإنشاء مهمة مزامنة تلقائية.") },
            confirmButton = { TextButton(onClick = onDismiss) { Text("حسناً") } }
        )
        return
    }

    var selectedConnId by remember { mutableStateOf(connections.first().id) }
    var localPath by remember { mutableStateOf(localCurrentPath) }
    var remotePath by remember { mutableStateOf("/sync_backup") }
    var direction by remember { mutableStateOf("BIDIRECTIONAL") } // "UPLOAD", "DOWNLOAD", "BIDIRECTIONAL"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إنشاء مهمة مزامنة سحابية", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("سيرفر FTP المستهدف:")
                connections.forEach { conn ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RadioButton(
                            selected = conn.id == selectedConnId,
                            onClick = { selectedConnId = conn.id }
                        )
                        Text("${conn.name} (${conn.host})")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = localPath,
                    onValueChange = { localPath = it },
                    label = { Text("المجلد المحلي بالجهاز") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = remotePath,
                    onValueChange = { remotePath = it },
                    label = { Text("المجلد بالسيرفر Remote FTP Path") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text("اتجاه المزامنة:")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    FilterChip(
                        selected = direction == "BIDIRECTIONAL",
                        onClick = { direction = "BIDIRECTIONAL" },
                        label = { Text("ثنائية ⇄") }
                    )
                    FilterChip(
                        selected = direction == "UPLOAD",
                        onClick = { direction = "UPLOAD" },
                        label = { Text("رفع فقط ↑") }
                    )
                    FilterChip(
                        selected = direction == "DOWNLOAD",
                        onClick = { direction = "DOWNLOAD" },
                        label = { Text("تنزيل فقط ↓") }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(selectedConnId, localPath.trim(), remotePath.trim(), direction, true)
                }
            ) {
                Text("إضافة المهمة")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

@Composable
fun CreateZipDialog(
    selectedCount: Int,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var zipName by remember { mutableStateOf("أرشيف_جديد.zip") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ضغط الملفات ($selectedCount)", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("سيتم ضغط $selectedCount ملف/مجلد في أرشيف ZIP مضغوط.")
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = zipName,
                    onValueChange = { zipName = it },
                    label = { Text("اسم ملف ZIP") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (zipName.isNotBlank()) onConfirm(zipName.trim()) }
            ) {
                Text("ضغط ZIP")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

@Composable
fun FileDetailsDialog(
    fileItem: com.example.data.file.FileItem,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تفاصيل الملف والمعلومات", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("اسم الملف: ${fileItem.name}", fontWeight = FontWeight.SemiBold)
                Text("المسار: ${fileItem.path}", style = MaterialTheme.typography.bodySmall)
                Text("الحجم: ${if (fileItem.isDirectory) "مجلد" else fileItem.formattedSize}")
                Text("تاريخ التعديل: ${fileItem.formattedDate}")
                Text("نوع الملف: ${fileItem.fileType}")
                Text("حالة التشفير: ${if (fileItem.isEncrypted) "مشفر بـ AES-256 🔒" else "غير مشفر 🔓"}")
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("إغلاق") }
        }
    )
}
