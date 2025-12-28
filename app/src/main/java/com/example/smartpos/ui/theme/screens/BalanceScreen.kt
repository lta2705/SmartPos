package com.example.smartpos.ui.theme.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Model cho dữ liệu giao dịch
data class Transaction(val name: String, val amount: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BalanceScreen(onBack: () -> Unit) {
    // Dữ liệu giả lập theo ảnh mẫu
    val transactions = listOf(
        Transaction("Cortado", "8 USD"),
        Transaction("Banana bread", "4 USD"),
        Transaction("Cold brew latte", "7.50"),
        Transaction("Lemon fresh", "12"),
        Transaction("Espresso +5% tip", "5 USD"),
        Transaction("Avocado sandwich", "14 USD")
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Balance", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF121212))
            )
        },
        containerColor = Color(0xFF121212),
        bottomBar = {
            // Nút Print Current Balance ở dưới cùng
            Button(
                onClick = { /* Logic in hóa đơn */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC4FB6D)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Print Current Balance", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .fillMaxSize()
        ) {
            // 1. Summary Cards Section (Màu tím)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SummaryCard(Modifier.weight(1f), "Opening time", "10:00", "Closing time", "N/A")
                SummaryCard(Modifier.weight(1f), "Total transactions", "17", "Transactions sum", "408.21 USD")
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 2. Transactions List
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(transactions) { item ->
                    TransactionItem(item)
                }
            }
        }
    }
}

@Composable
fun SummaryCard(modifier: Modifier, label1: String, value1: String, label2: String, value2: String) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF6C5CE7)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label1, color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
            Text(value1, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            Text(label2, color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
            Text(value2, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun TransactionItem(transaction: Transaction) {
    Surface(
        color = Color(0xFF1E1E1E),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(transaction.name, color = Color.White, fontSize = 16.sp)
            Text(transaction.amount, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
    }
}