package com.example.smartpos.ui.theme.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartpos.ui.theme.components.ActionCard
import com.example.smartpos.ui.theme.PurplePrimary
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun HomeScreen(
    onSaleClick: () -> Unit,
    onQRClick: () -> Unit,
    onRefundClick: () -> Unit,
    onBalanceClick: () -> Unit,
    onVoidClick: () -> Unit,
    onSettlementClick: () -> Unit
) {
    var currentTime by remember { mutableStateOf(LocalDateTime.now()) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = LocalDateTime.now()
            delay(1000)
        }
    }

    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Phần Header trắng
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 40.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("TouchPay", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Spacer(modifier = Modifier.height(20.dp))

            // Giả lập hình ảnh tay quẹt thẻ
            Icon(
                imageVector = Icons.Default.Nfc,
                contentDescription = null,
                modifier = Modifier.size(60.dp),
                tint = Color.LightGray
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 2. Hiển thị thời gian thực tế thay vì text tĩnh
            Text(
                text = currentTime.format(timeFormatter),
                fontSize = 48.sp,
                fontWeight = FontWeight.Light
            )
            Text(
                text = currentTime.format(dateFormatter),
                color = Color.Gray,
                fontSize = 16.sp
            )
        }

        // Phần Menu màu tím
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = PurplePrimary,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.padding(16.dp),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { ActionCard("Sale", Icons.Default.ShoppingCart, onSaleClick) }
                item { ActionCard("QR", Icons.Default.QrCode, onQRClick) }
                item { ActionCard("Void", Icons.Default.RemoveCircle, onVoidClick) }
                item { ActionCard("Refund", Icons.Default.History, onRefundClick) }
                item { ActionCard("Balance", Icons.Default.AccountBalanceWallet, onBalanceClick) }
                item { ActionCard("Settlement", Icons.Default.Assessment, onSettlementClick) }
            }
        }
    }
}