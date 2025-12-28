package com.example.smartpos.ui.theme.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartpos.ui.theme.components.ActionCard
import com.example.smartpos.ui.theme.PurplePrimary

@Composable
fun HomeScreen(
    onSaleClick: () -> Unit,
    onRefundClick: () -> Unit,
    onBalanceClick: () -> Unit
) {
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
            Icon(Icons.Default.Nfc, contentDescription = null, modifier = Modifier.size(60.dp), tint = Color.LightGray)

            Spacer(modifier = Modifier.height(20.dp))
            Text("10:03", fontSize = 48.sp, fontWeight = FontWeight.Light)
            Text("06.04.2023", color = Color.Gray, fontSize = 16.sp)
        }

        // Phần Menu màu tím (giống ảnh mẫu)
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = PurplePrimary,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.padding(16.dp),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { ActionCard("Sale", Icons.Default.ShoppingCart, onSaleClick) }
                item { ActionCard("Cancellation", Icons.Default.Close, {}) }
                item { ActionCard("Refund", Icons.Default.History, onRefundClick) }
                item { ActionCard("Balance", Icons.Default.AccountBalanceWallet, onBalanceClick) }
            }
        }
    }
}