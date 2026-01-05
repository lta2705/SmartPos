package com.example.smartpos.ui.theme.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartpos.viewmodel.PosViewModel
import java.util.*

@Composable
fun TipScreen(viewModel: PosViewModel, onConfirm: () -> Unit) {
    val amountStr by viewModel.amount.collectAsState()
    val selectedTip by viewModel.selectedTip.collectAsState()

    val baseAmount = amountStr.toDoubleOrNull() ?: 0.0
    val tipAmount = baseAmount * (selectedTip / 100.0)
    val totalAmount = baseAmount + tipAmount

    val tipOptions = listOf(5, 10, 20, 25)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Sale", fontSize = 18.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(8.dp))

        // Hiển thị số tiền gốc và tổng tiền
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = "Amount", color = Color.Gray, fontSize = 14.sp)
                Text(text = "$${String.format("%.2f", baseAmount)}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(text = "Total", color = Color.Gray, fontSize = 14.sp)
                Text(text = "$${String.format("%.2f", totalAmount)}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6C5CE7))
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Grid các lựa chọn Tip
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(tipOptions) { tipPercent ->
                val calculatedTip = baseAmount * (tipPercent / 100.0)
                TipCard(
                    percent = tipPercent,
                    amount = calculatedTip,
                    isSelected = selectedTip == tipPercent,
                    onClick = { viewModel.selectTip(tipPercent) }
                )
            }

            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                OutlinedButton(
                    onClick = { viewModel.selectTip(0) },
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (selectedTip == 0) Color(0xFF6C5CE7) else Color.DarkGray),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (selectedTip == 0) Color(0xFF6C5CE7).copy(alpha = 0.1f) else Color.Transparent
                    )
                ) {
                    Text(text = "No Tip", color = Color.White)
                }
            }
        }

        // Nút xác nhận chuyển sang màn hình thanh toán
        Button(
            onClick = onConfirm,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC4FB6D)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(text = "Confirm", color = Color.Black, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TipCard(percent: Int, amount: Double, isSelected: Boolean, onClick: () -> Unit) {
    val backgroundColor = if (isSelected) Color(0xFF2D2D2D) else Color(0xFF1E1E1E)
    val borderColor = if (isSelected) Color(0xFF6C5CE7) else Color.Transparent

    Card(
        onClick = onClick,
        modifier = Modifier.height(100.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(2.dp, borderColor),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "$percent%", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(text = "$${String.format("%.2f", amount)}", fontSize = 14.sp, color = Color.Gray)
        }
    }
}