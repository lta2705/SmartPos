package com.example.smartpos.ui.theme.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartpos.ui.theme.DarkBackground
import com.example.smartpos.ui.theme.PurplePrimary
import com.example.smartpos.ui.theme.LimeGreen
import com.example.smartpos.viewmodel.PosViewModel

@Composable
fun ResultScreen(viewModel: PosViewModel, onClose: () -> Unit) {
    val amount by viewModel.amount.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Biểu tượng Checkmark hình tròn
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(PurplePrimary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Success",
                tint = Color.White,
                modifier = Modifier.size(60.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Approved!",
            fontSize = 28.sp,
            color = PurplePrimary
        )

        Text(
            text = "$${amount} USD",
            fontSize = 24.sp,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Hàng chứa các nút bấm hành động
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            OutlinedButton(
                onClick = { /* In hóa đơn */ },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Print Receipt")
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = onClose,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = LimeGreen),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Close", color = Color.Black)
            }
        }
    }
}