package com.example.smartpos.ui.theme.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contactless
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartpos.viewmodel.PaymentState
import com.example.smartpos.viewmodel.PosViewModel

@Composable
fun PaymentScreen(viewModel: PosViewModel, onPaymentSuccess: () -> Unit) {
    val amountStr by viewModel.amount.collectAsState()
    val selectedTip by viewModel.selectedTip.collectAsState()
    val paymentState by viewModel.paymentState.collectAsState()

    // 1. Tính toán tổng tiền (Gốc + Tip)
    val baseAmount = amountStr.toDoubleOrNull() ?: 0.0
    val totalAmount = baseAmount + (baseAmount * (selectedTip / 100.0))

    // 2. Hiệu ứng nhấp nháy cho icon NFC
    val infiniteTransition = rememberInfiniteTransition(label = "NFCAnimation")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Alpha"
    )

    // 3. Tự động giả lập quẹt thẻ khi vào màn hình này
    LaunchedEffect(Unit) {
        viewModel.processPayment()
    }

    // 4. THEO DÕI TRẠNG THÁI: Lưu giao dịch khi thành công
    LaunchedEffect(paymentState) {
        if (paymentState is PaymentState.Approved) {
            // Sửa đổi: Truyền totalAmount vào String.format
            viewModel.addTransaction(
                name = "Sale Giao dịch",
                amount = String.format("%.2f VND", totalAmount)
            )
            onPaymentSuccess()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // PHẦN TRÊN: Tiêu đề & Icon NFC
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Sale", fontSize = 18.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(32.dp))
            Icon(
                imageVector = Icons.Default.KeyboardArrowUp,
                contentDescription = null,
                tint = Color(0xFF6C5CE7),
                modifier = Modifier.size(32.dp)
            )
            Icon(
                imageVector = Icons.Default.Contactless,
                contentDescription = "NFC",
                tint = Color(0xFF6C5CE7),
                modifier = Modifier
                    .size(80.dp)
                    .alpha(alpha)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "For mobile pay,\nplace your device here",
                textAlign = TextAlign.Center,
                color = Color.Gray,
                lineHeight = 20.sp
            )
        }

        // PHẦN GIỮA: Hiển thị tổng số tiền
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Amount", color = Color.Gray, fontSize = 16.sp)
            Text(
                text = "${String.format("%.2f", totalAmount)} VND",
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        // PHẦN DƯỚI: Hướng dẫn đút thẻ & Progress
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Insert your credit card here", color = Color.Gray)
            Spacer(modifier = Modifier.height(8.dp))
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = Color(0xFF6C5CE7),
                modifier = Modifier.size(32.dp)
            )

            if (paymentState is PaymentState.Processing) {
                Spacer(modifier = Modifier.height(16.dp))
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    color = Color(0xFFC4FB6D),
                    trackColor = Color.DarkGray
                )
            }
        }
    }
}