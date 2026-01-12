package com.example.smartpos.ui.theme.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
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
import com.example.smartpos.viewmodel.PosViewModel
import com.example.smartpos.viewmodel.Transaction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BalanceScreen(viewModel: PosViewModel, onBack: () -> Unit, onPrintBalance: () -> Unit) {
    val transactions by viewModel.transactionHistory.collectAsState()
    val totalSum by viewModel.totalSum.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Balance", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF121212)
                )
            )
        },
        containerColor = Color(0xFF121212),
        bottomBar = {
            // Nút Print Current Balance
            Button(
                onClick = onPrintBalance,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC4FB6D)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Print Current Balance",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .fillMaxSize()
        ) {
            // 1. Summary Cards Section (Thông tin tổng hợp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SummaryCard(
                    modifier = Modifier.weight(1f),
                    label1 = "Opening time",
                    value1 = "10:00",
                    label2 = "Closing time",
                    value2 = "N/A"
                )
                SummaryCard(
                    modifier = Modifier.weight(1f),
                    label1 = "Total transactions",
                    value1 = "${transactions.size}",
                    label2 = "Transactions sum",
                    value2 = String.format("%.0f VND", totalSum)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Recent Transactions",
                color = Color.Gray,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (transactions.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No transactions yet", color = Color.DarkGray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(transactions.reversed()) { transaction ->
                        TransactionItem(transaction)
                    }
                }
            }
        }
    }
}

@Composable
fun SummaryCard(
    modifier: Modifier,
    label1: String,
    value1: String,
    label2: String,
    value2: String
) {
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.name,
                    color = Color.White,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "ID: ${transaction.id.take(8)}",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
            Text(
                text = transaction.amount,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}