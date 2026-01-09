
package com.example.smartpos.ui.theme.screens

import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import com.example.smartpos.ui.theme.components.ActionableTransactionCard
import com.example.smartpos.ui.theme.layouts.TransactionListScreenLayout
import com.example.smartpos.viewmodel.PosViewModel
import com.example.smartpos.viewmodel.Transaction

@Composable
fun RefundScreen(viewModel: PosViewModel, onBack: () -> Unit) {
    val qrTransactions by viewModel.qrTransactions.collectAsState(initial = emptyList<Transaction>())

    TransactionListScreenLayout(
        title = "Refund Transaction",
        onBack = onBack,
        emptyMessage = "No QR transactions available",
        isEmpty = qrTransactions.isEmpty(),
        instructionText = "Select a QR transaction to refund:"
    ) {
        items(qrTransactions) { transaction ->
            ActionableTransactionCard(
                transaction = transaction,
                actionLabel = "Refund",
                actionColor = Color(0xFFFF9500),
                onAction = { viewModel.refundTransaction(transaction) }
            )
        }
    }
}
