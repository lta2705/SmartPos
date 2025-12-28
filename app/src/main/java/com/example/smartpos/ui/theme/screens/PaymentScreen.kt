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
import kotlinx.coroutines.delay

@Composable
fun PaymentScreen(viewModel: PosViewModel, onPaymentSuccess: () -> Unit) {
    val amountStr by viewModel.amount.collectAsState()
    val selectedTip by viewModel.selectedTip.collectAsState()
    val paymentState by viewModel.paymentState.collectAsState()

    // Tính toán tổng tiền cuối cùng
    val baseAmount = amountStr.toDoubleOrNull() ?: 0.0
    val totalAmount = baseAmount + (baseAmount * (selectedTip / 100.0))

    // Hiệu ứng nhấp nháy cho icon NFC
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

    // Tự động giả lập quẹt thẻ khi vào màn hình này
    LaunchedEffect(Unit) {
        viewModel.processPayment()
    }

    // Theo dõi trạng thái để chuyển màn hình khi Approved
    LaunchedEffect(paymentState) {
        if (paymentState is PaymentState.Approved) {
            onPaymentSuccess()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // 1. Phần tiêu đề & Icon phía trên
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
                modifier = Modifier.size(80.dp).alpha(alpha) // Áp dụng animation alpha
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "For mobile pay,\nplace your device here",
                textAlign = TextAlign.Center,
                color = Color.Gray,
                lineHeight = 20.sp
            )
        }

        // 2. Phần hiển thị số tiền chính giữa
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Amount", color = Color.Gray, fontSize = 16.sp)
            Text(
                text = "${String.format("%.2f", totalAmount)} USD",
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        // 3. Hướng dẫn đút thẻ phía dưới
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Insert your credit card here", color = Color.Gray)
            Spacer(modifier = Modifier.height(8.dp))
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = Color(0xFF6C5CE7),
                modifier = Modifier.size(32.dp)
            )

            // Loading indicator nếu đang Processing
            if (paymentState is PaymentState.Processing) {
                Spacer(modifier = Modifier.height(16.dp))
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(4.dp),
                    color = Color(0xFFC4FB6D),
                    trackColor = Color.DarkGray
                )
            }
        }
    }
}