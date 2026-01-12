package com.example.smartpos.ui.theme.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartpos.viewmodel.PosViewModel
import com.example.smartpos.viewmodel.Transaction
import com.example.smartpos.viewmodel.TransactionType
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SettlementScreen(viewModel: PosViewModel, onBack: () -> Unit) {
    val allTransactions by viewModel.transactionHistory.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(24.dp)
        ) {
            // Header
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                Text(
                    text = "Settlement Report",
                    fontSize = 20.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Summary Statistics
            SummarySection(allTransactions)

            Spacer(modifier = Modifier.height(24.dp))

            if (allTransactions.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No transactions available",
                        color = Color.Gray,
                        fontSize = 16.sp
                    )
                }
            } else {
                Text(
                    text = "All Transactions:",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(allTransactions.sortedByDescending { it.timestamp }) { transaction ->
                        SettlementTransactionCard(transaction = transaction)
                    }
                }
            }
        }

        // Settlement Button at bottom
        Button(
            onClick = { viewModel.performSettlement() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFC4FB6D)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "Perform Settlement",
                color = Color.Black,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SummarySection(transactions: List<Transaction>) {
    val saleTotal = transactions
        .filter { it.type == TransactionType.SALE && !it.isVoided }
        .sumOf { it.amount.replace(" VND", "").toDoubleOrNull() ?: 0.0 }

    val qrTotal = transactions
        .filter { it.type == TransactionType.QR && !it.isVoided }
        .sumOf { it.amount.replace(" VND", "").toDoubleOrNull() ?: 0.0 }

    val voidTotal = transactions
        .filter { it.type == TransactionType.VOID }
        .sumOf { it.amount.replace(" VND", "").toDoubleOrNull() ?: 0.0 }

    val refundTotal = transactions
        .filter { it.type == TransactionType.REFUND }
        .sumOf { it.amount.replace(" VND", "").toDoubleOrNull() ?: 0.0 }

    val netTotal = saleTotal + qrTotal - voidTotal - refundTotal

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2D2D2D))
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Summary",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            SummaryRow("Sale Total:", saleTotal, Color(0xFFC4FB6D))
            SummaryRow("QR Total:", qrTotal, Color(0xFFC4FB6D))
            SummaryRow("Void Total:", voidTotal, Color(0xFFFF6B6B))
            SummaryRow("Refund Total:", refundTotal, Color(0xFFFF9500))

            Divider(
                color = Color.Gray,
                modifier = Modifier.padding(vertical = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Net Total:",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = String.format("%.0f VND", netTotal),
                    color = Color(0xFFC4FB6D),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, amount: Double, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = Color.Gray,
            fontSize = 14.sp
        )
        Text(
            text = String.format("%.0f VND", amount),
            color = color,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun SettlementTransactionCard(transaction: Transaction) {
    val backgroundColor = when (transaction.type) {
        TransactionType.SALE -> if (transaction.isVoided) Color(0xFF3D3D3D) else Color(0xFF2D2D2D)
        TransactionType.QR -> if (transaction.isVoided) Color(0xFF3D3D3D) else Color(0xFF2D2D2D)
        TransactionType.VOID -> Color(0xFF4D2D2D)
        TransactionType.REFUND -> Color(0xFF4D3D2D)
        TransactionType.SETTLEMENT -> Color(0xFF4D3D2D)
    }

    val typeColor = when (transaction.type) {
        TransactionType.SALE -> Color(0xFFC4FB6D)
        TransactionType.QR -> Color(0xFF00D4FF)
        TransactionType.VOID -> Color(0xFFFF6B6B)
        TransactionType.REFUND -> Color(0xFFFF9500)
        TransactionType.SETTLEMENT -> Color(0xFFFFD700)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = transaction.type.name,
                        color = typeColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (transaction.isVoided) {
                        Text(
                            text = "[VOIDED]",
                            color = Color.Gray,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = transaction.name,
                    color = if (transaction.isVoided) Color.Gray else Color.White,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatTimestamp(transaction.timestamp),
                    color = Color.Gray,
                    fontSize = 11.sp
                )
            }

            Text(
                text = transaction.amount,
                color = if (transaction.isVoided) Color.Gray else typeColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
