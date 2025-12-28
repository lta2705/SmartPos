package com.example.smartpos.ui.theme.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartpos.ui.theme.PurplePrimary

@Composable
fun WelcomeScreen(onEnterApp: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PurplePrimary)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "TouchPay",
            color = Color.White,
            fontSize = 42.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Receive payments easily",
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 16.sp
        )

        Spacer(modifier = Modifier.height(60.dp))

        // Giả lập hình ảnh thẻ (Bạn có thể thay bằng Image thực tế)
        Box(modifier = Modifier.size(200.dp), contentAlignment = Alignment.Center) {
            // Vẽ các hình chữ nhật giả lập thẻ ngân hàng
            Card(modifier = Modifier.size(160.dp, 100.dp).offset(y = (-20).dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {}
            Card(modifier = Modifier.size(160.dp, 100.dp).offset(x = 10.dp, y = 10.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {}
        }

        Spacer(modifier = Modifier.height(100.dp))

        // Nút tròn mũi tên
        Button(
            onClick = onEnterApp,
            shape = CircleShape,
            modifier = Modifier.size(64.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E1E)),
            contentPadding = PaddingValues(0.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Enter", tint = Color.White)
        }
    }
}