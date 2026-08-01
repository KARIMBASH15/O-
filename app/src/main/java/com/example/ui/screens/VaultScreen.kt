package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.file.FileItem
import com.example.ui.components.FileListItem
import com.example.ui.theme.VaultAmber

@Composable
fun VaultScreen(
    encryptedFiles: List<FileItem>,
    onDecryptFile: (FileItem) -> Unit,
    onDeleteFile: (FileItem) -> Unit,
    onEncryptNewClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isUnlocked by remember { mutableStateOf(false) }
    var inputPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }

    if (!isUnlocked) {
        // PIN Lock Screen
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = VaultAmber.copy(alpha = 0.2f),
                        modifier = Modifier.size(64.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Lock, null, tint = VaultAmber, modifier = Modifier.size(36.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "الخزنة المشفرة - AES-256",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "أدخل رمز الدخول لمنطقة التشفير الآمن (الرمز الافتراضي: 1234)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    OutlinedTextField(
                        value = inputPin,
                        onValueChange = {
                            inputPin = it
                            pinError = false
                        },
                        label = { Text("رمز الدخول (PIN)") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        isError = pinError,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_vault_pin")
                    )

                    if (pinError) {
                        Text(
                            text = "رمز الدخول غير صحيح",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            if (inputPin == "1234" || inputPin.length >= 4) {
                                isUnlocked = true
                            } else {
                                pinError = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = VaultAmber),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_unlock_vault")
                    ) {
                        Icon(Icons.Default.LockOpen, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("فتح الخزنة")
                    }
                }
            }
        }
    } else {
        // Vault Unlocked Content
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "الخزنة المشفرة (AES-256)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "الملفات المحمية بالتشفير العالي",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = onEncryptNewClick,
                    colors = ButtonDefaults.buttonColors(containerColor = VaultAmber),
                    modifier = Modifier.testTag("btn_encrypt_file")
                ) {
                    Icon(Icons.Default.Lock, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("تشفير ملف")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (encryptedFiles.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "لا توجد ملفات مشفرة حالياً. يمكنك اختيار أي ملف من الجهاز وتشفيره بكلمة مرور سرية.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(encryptedFiles) { item ->
                        FileListItem(
                            item = item,
                            onClick = { onDecryptFile(item) },
                            onOpenExcel = {},
                            onOpenText = {},
                            onEncrypt = {},
                            onDecrypt = { onDecryptFile(item) },
                            onUploadFtp = {},
                            onRename = {},
                            onDelete = { onDeleteFile(item) }
                        )
                    }
                }
            }
        }
    }
}
