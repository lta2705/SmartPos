package com.example.smartpos.ui.theme.screens

import androidx.compose.foundation.layout.*
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
import com.example.smartpos.ui.theme.components.NumericKeypad
import com.example.smartpos.viewmodel.PosViewModel

@Composable
fun SaleScreen(viewModel: PosViewModel, onConfirm: () -> Unit) {
    val amount by viewModel.amount.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Sale", fontSize = 20.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = "$amount USD",
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.weight(1f))

        NumericKeypad(onKeyClick = { viewModel.updateAmount(it) })

        Button(
            onClick = onConfirm,
            modifier = Modifier.fillMaxWidth().height(60.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC4FB6D)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Confirm", color = Color.Black, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}