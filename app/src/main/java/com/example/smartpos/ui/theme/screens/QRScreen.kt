package com.example.smartpos.ui.theme.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartpos.ui.theme.components.NumericKeypad
import com.example.smartpos.viewmodel.PosViewModel
import com.example.smartpos.viewmodel.TransactionType

@Composable
fun QRScreen(viewModel: PosViewModel, onConfirm: () -> Unit, onReturn: () -> Unit) {
    val amount by viewModel.amount.collectAsState()

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
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Return", tint = Color.DarkGray)
            }

            Text(
                text = "QR Payment",
                fontSize = 20.sp,
                color = Color.Gray,
                fontWeight = FontWeight.Medium
            )
        }
        // ------------------

        Spacer(modifier = Modifier.height(60.dp))

        Text(
            text = amount + " VND",
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.weight(1f))

        NumericKeypad(onKeyClick = { viewModel.updateAmount(it) })

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                // Tạo giao dịch QR khi confirm
                val amountValue = amount.toDoubleOrNull() ?: 0.0
                if (amountValue > 0) {
                    viewModel.addTransaction(
                        type = TransactionType.QR,
                        name = "QR Giao dịch",
                        amount = String.format("%.2f VND", amountValue)
                    )
                }
                onConfirm()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC4FB6D)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Confirm", color = Color.Black, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}