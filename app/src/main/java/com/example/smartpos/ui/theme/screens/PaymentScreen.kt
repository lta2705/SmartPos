package com.example.smartpos.ui.theme.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contactless
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.window.Dialog
import com.example.smartpos.viewmodel.PaymentState
import com.example.smartpos.viewmodel.PosViewModel
import kotlinx.coroutines.delay

@Composable
fun PaymentScreen(
    viewModel: PosViewModel,
    onCardRead: () -> Unit,
    onTimeout: () -> Unit
) {
    val amountStr by viewModel.amount.collectAsState()
    val selectedTip by viewModel.selectedTip.collectAsState()
    val paymentState by viewModel.paymentState.collectAsState()
    val nfcData by viewModel.nfcData.collectAsState()

    // State để tracking timeout
    var showTimeoutDialog by remember { mutableStateOf(false) }
    var timeoutOccurred by remember { mutableStateOf(false) }

    // 1. Tính toán tổng tiền - sử dụng amount từ TCP nếu có, không tính tip
    // Amount từ handleIncomingTransaction đã là tổng cuối cùng
    // Normalize comma to period for parsing
    val totalAmount = amountStr.replace(",", ".").toDoubleOrNull() ?: 0.0

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

    // 3. Gửi transaction started message và bắt đầu quá trình
    LaunchedEffect(Unit) {
        // Gửi msgType=2, status=STARTED
        viewModel.sendTransactionStarted(totalAmount)
        
        // Bắt đầu đọc NFC
        viewModel.startNfcReading()
    }

    // 4. Timeout timer (30 seconds)
    LaunchedEffect(Unit) {
        delay(30000) // 30 seconds
        if (nfcData == null && !timeoutOccurred) {
            timeoutOccurred = true
            showTimeoutDialog = true
            viewModel.onNfcTimeout()
        }
    }

    // 5. Xử lý khi có NFC data - Gửi STARTED message
    LaunchedEffect(nfcData) {
        if (nfcData != null && !timeoutOccurred) {
            // Gửi message msgType=2, status=STARTED khi đọc NFC thành công
            viewModel.sendTransactionStarted(totalAmount)
            onCardRead()
        }
    }

    // Timeout Dialog
    if (showTimeoutDialog) {
        TimeoutDialog(
            onDismiss = { showTimeoutDialog = false },
            onGoHome = {
                showTimeoutDialog = false
                onTimeout()
            }
        )
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
                text = "${String.format("%.0f", totalAmount)} VND",
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
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Waiting for card...",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun TimeoutDialog(
    onDismiss: () -> Unit,
    onGoHome: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF2D2D2D)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Timeout",
                    tint = Color(0xFFFF6B6B),
                    modifier = Modifier.size(64.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Transaction Timeout",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "The transaction has timed out. Please try again.",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onGoHome,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF6C5CE7)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Go to Home",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}