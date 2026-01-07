package com.example.smartpos.ui.theme.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartpos.viewmodel.PosViewModel
import com.example.smartpos.viewmodel.TransactionType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun QRScreen(viewModel: PosViewModel, onConfirm: () -> Unit, onReturn: () -> Unit) {
    val amount by viewModel.amount.collectAsState()
    
    var qrCode by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var transactionCompleted by remember { mutableStateOf(false) }

    // Tự động gửi request tạo QR khi vào màn hình (nếu đã có amount từ TCP)
    LaunchedEffect(Unit) {
        val amountValue = amount.toDoubleOrNull() ?: 0.0
        if (amountValue > 0) {
            isLoading = true
            viewModel.sendQRTransactionToBank(
                amount = amountValue,
                onSuccess = { qrCodeData ->
                    qrCode = qrCodeData
                    isLoading = false
                    
                    // Tạo transaction ngay khi nhận được QR
                    viewModel.addTransaction(
                        type = TransactionType.QR,
                        name = "QR Payment",
                        amount = String.format("%.2f VND", amountValue)
                    )
                    
                    // Simulate waiting for bank notification
                    // TODO: Replace with actual bank notification listener
                    kotlinx.coroutines.GlobalScope.launch {
                        delay(5000) // Simulate 5s wait
                        transactionCompleted = true
                    }
                },
                onError = { error ->
                    errorMessage = error
                    isLoading = false
                }
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- Header Row ---
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                onClick = onReturn,
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Return", tint = Color.White)
            }

            Text(
                text = "QR Payment",
                fontSize = 20.sp,
                color = Color.White,
                fontWeight = FontWeight.Medium
            )
        }
        // ------------------

        Spacer(modifier = Modifier.height(40.dp))

        // Amount display
        Text(
            text = "$amount VND",
            fontSize = 42.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(40.dp))

        // QR Code or Loading
        Card(
            modifier = Modifier
                .size(300.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isLoading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(64.dp),
                            color = Color(0xFF6C5CE7)
                        )
                    }
                    errorMessage != null -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "❌",
                                fontSize = 48.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = errorMessage!!,
                                color = Color.Red,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    qrCode != null -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCode2,
                                contentDescription = "QR Code",
                                tint = Color(0xFF6C5CE7),
                                modifier = Modifier.size(200.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (transactionCompleted) "✓ Completed" else "Scan to Pay",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (transactionCompleted) Color(0xFF4CAF50) else Color(0xFF6C5CE7)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (transactionCompleted) {
            Text(
                text = "Transaction successful!",
                fontSize = 18.sp,
                color = Color(0xFF4CAF50),
                fontWeight = FontWeight.Bold
            )
        } else if (qrCode != null) {
            Text(
                text = "Waiting for payment...",
                fontSize = 16.sp,
                color = Color.Gray
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Done button
        Button(
            onClick = onConfirm,
            enabled = transactionCompleted || errorMessage != null,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFC4FB6D),
                disabledContainerColor = Color.Gray
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = if (errorMessage != null) "Go Back" else "Done",
                color = Color.Black,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
