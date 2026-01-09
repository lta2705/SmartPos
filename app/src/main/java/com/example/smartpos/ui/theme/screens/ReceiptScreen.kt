package com.example.smartpos.ui.theme.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartpos.utils.DateUtils
import com.example.smartpos.viewmodel.PosViewModel
import com.example.smartpos.viewmodel.Transaction
import java.text.SimpleDateFormat
import java.util.*

/**
 * Receipt screen for single transaction
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptScreen(
    viewModel: PosViewModel,
    transactionId: String?,
    onBack: () -> Unit,
    onPrint: () -> Unit
) {
    val transactions by viewModel.transactionHistory.collectAsState()
    val transaction = transactions.find { it.id == transactionId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transaction Receipt", color = Color.White, fontWeight = FontWeight.Bold) },
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
            Button(
                onClick = onPrint,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC4FB6D)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Print Receipt",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    ) { paddingValues ->
        if (transaction == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Transaction not found",
                    color = Color.Gray,
                    fontSize = 16.sp
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ReceiptContent(transaction)
            }
        }
    }
}

/**
 * Balance receipt screen for all transactions
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BalanceReceiptScreen(
    viewModel: PosViewModel,
    onBack: () -> Unit,
    onPrint: () -> Unit
) {
    val transactions by viewModel.transactionHistory.collectAsState()
    val totalSum by viewModel.totalSum.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Balance Receipt", color = Color.White, fontWeight = FontWeight.Bold) },
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
            Button(
                onClick = onPrint,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC4FB6D)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Print Balance Report",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            BalanceReceiptContent(transactions, totalSum)
        }
    }
}

@Composable
private fun ReceiptContent(transaction: Transaction) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "SMART POS",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Transaction Receipt",
                fontSize = 16.sp,
                color = Color.Gray
            )
            
            Divider(
                modifier = Modifier.padding(vertical = 16.dp),
                color = Color.Gray,
                thickness = 1.dp
            )
            
            // Transaction details
            ReceiptRow("Transaction ID:", transaction.id.take(8))
            ReceiptRow("Type:", transaction.type.name)
            ReceiptRow("Description:", transaction.name)
            ReceiptRow("Date:", DateUtils.formatTimestamp(transaction.timestamp))
            ReceiptRow("Status:", if (transaction.isVoided) "VOIDED" else "APPROVED")
            
            Divider(
                modifier = Modifier.padding(vertical = 16.dp),
                color = Color.Gray,
                thickness = 1.dp
            )
            
            // Amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "TOTAL AMOUNT:",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Text(
                    text = transaction.amount,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Thank you for your business!",
                fontSize = 14.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun BalanceReceiptContent(transactions: List<Transaction>, totalSum: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "SMART POS",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Balance Report",
                fontSize = 16.sp,
                color = Color.Gray
            )
            
            Divider(
                modifier = Modifier.padding(vertical = 16.dp),
                color = Color.Gray,
                thickness = 1.dp
            )
            
            // Report date
            ReceiptRow("Report Date:", DateUtils.formatTimestamp(System.currentTimeMillis()))
            ReceiptRow("Total Transactions:", "${transactions.filter { !it.isVoided }.size}")
            
            Divider(
                modifier = Modifier.padding(vertical = 16.dp),
                color = Color.Gray,
                thickness = 1.dp
            )
            
            // Transaction breakdown
            Text(
                text = "Transaction Summary",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            )
            
            transactions.filter { !it.isVoided }.forEach { transaction ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = transaction.name,
                            fontSize = 12.sp,
                            color = Color.Black
                        )
                        Text(
                            text = "ID: ${transaction.id.take(8)}",
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                    }
                    Text(
                        text = transaction.amount,
                        fontSize = 12.sp,
                        color = Color.Black,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            Divider(
                modifier = Modifier.padding(vertical = 16.dp),
                color = Color.Gray,
                thickness = 2.dp
            )
            
            // Total
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "GRAND TOTAL:",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Text(
                    text = String.format("%.2f VND", totalSum),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "End of Report",
                fontSize = 14.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ReceiptRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = Color.Gray
        )
        Text(
            text = value,
            fontSize = 14.sp,
            color = Color.Black,
            fontWeight = FontWeight.Medium
        )
    }
}

// Removed duplicate formatTimestamp - now using DateUtils.formatTimestamp()
