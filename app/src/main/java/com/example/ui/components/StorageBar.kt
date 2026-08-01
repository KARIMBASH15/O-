package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.file.StorageStats
import com.example.ui.theme.ExcelGreen
import com.example.ui.theme.TextBlue
import com.example.ui.theme.VaultAmber

@Composable
fun StorageCard(
    stats: StorageStats,
    onQuickAccessClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val totalGB = String.format("%.1f", stats.totalSpaceBytes / (1024.0 * 1024.0 * 1024.0))
    val usedGB = String.format("%.1f", stats.usedSpaceBytes / (1024.0 * 1024.0 * 1024.0))
    val usedFraction = if (stats.totalSpaceBytes > 0) (stats.usedSpaceBytes.toFloat() / stats.totalSpaceBytes) else 0.2f

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "ذاكرة الجهاز المحلية",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "المستخدم: $usedGB جيجابايت / الكلي: $totalGB جيجابايت",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "${(usedFraction * 100).toInt()}%",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(usedFraction.coerceIn(0.05f, 1f))
                        .clip(RoundedCornerShape(5.dp))
                        .background(MaterialTheme.colorScheme.primary)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quick Category Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                CategoryBadge(
                    title = "Excel (${stats.excelCount})",
                    color = ExcelGreen,
                    icon = Icons.Default.TableChart,
                    onClick = { onQuickAccessClick("EXCEL") }
                )
                CategoryBadge(
                    title = "مستندات (${stats.textCount})",
                    color = TextBlue,
                    icon = Icons.Default.Description,
                    onClick = { onQuickAccessClick("TEXT") }
                )
                CategoryBadge(
                    title = "خزنة مشفرة (${stats.encryptedCount})",
                    color = VaultAmber,
                    icon = Icons.Default.Lock,
                    onClick = { onQuickAccessClick("VAULT") }
                )
            }
        }
    }
}

@Composable
private fun CategoryBadge(
    title: String,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.12f),
        contentColor = color
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = color
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = color
            )
        }
    }
}
