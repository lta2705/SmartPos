package com.example.smartpos.ui.theme.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun NumericKeypad(onKeyClick: (String) -> Unit) {
    val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", ".", "0", "del")
    Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
        keys.chunked(3).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                row.forEach { key ->
                    TextButton(
                        onClick = { onKeyClick(key) },
                        modifier = Modifier.weight(1f).height(70.dp)
                    ) {
                        if (key == "del") {
                            Icon(Icons.Default.Backspace, contentDescription = "Delete", tint = Color.White)
                        } else {
                            Text(text = key, fontSize = 26.sp, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActionCard(title: String, icon: ImageVector, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.padding(8.dp).fillMaxWidth().height(140.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2D2D2D))
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = title, color = Color.White, fontWeight = FontWeight.Medium)
        }
    }
}