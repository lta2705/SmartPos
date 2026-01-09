package com.example.smartpos.ui.theme.screens

import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import com.example.smartpos.ui.theme.components.ActionableTransactionCard
import com.example.smartpos.ui.theme.layouts.TransactionListScreenLayout
import com.example.smartpos.viewmodel.PosViewModel

@Composable
fun VoidScreen(viewModel: PosViewModel, onBack: () -> Unit) {
    val saleTransactions by viewModel.saleTransactions.collectAsState()

    TransactionListScreenLayout(
        title = "Void Transaction",
        onBack = onBack,
        emptyMessage = "No Sale transactions available",
        isEmpty = saleTransactions.isEmpty(),
        instructionText = "Select a Sale transaction to void:"
    ) {
        items(saleTransactions) { transaction ->
            ActionableTransactionCard(
                transaction = transaction,
                actionLabel = "Void",
                actionColor = Color(0xFFFF6B6B),
                onAction = { viewModel.voidTransaction(transaction) }
            )
        }
    }
}
