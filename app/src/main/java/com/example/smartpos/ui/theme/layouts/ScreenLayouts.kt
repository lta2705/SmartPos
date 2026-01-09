package com.example.smartpos.ui.theme.layouts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.smartpos.ui.theme.components.EmptyState
import com.example.smartpos.ui.theme.components.ScreenHeader
import com.example.smartpos.ui.theme.components.SectionHeader

/**
 * Reusable screen layout template
 * Reduces boilerplate for transaction list screens (Void, Refund, Settlement, etc.)
 */
@Composable
fun TransactionListScreenLayout(
    title: String,
    onBack: () -> Unit,
    emptyMessage: String,
    isEmpty: Boolean,
    instructionText: String? = null,
    content: LazyListScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(24.dp)
    ) {
        ScreenHeader(
            title = title,
            onBack = onBack
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (isEmpty) {
            EmptyState(message = emptyMessage)
        } else {
            instructionText?.let {
                SectionHeader(text = it)
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                content()
            }
        }
    }
}

/**
 * Standard screen layout with padding and background
 * Used for input screens (Sale, QR, etc.)
 */
@Composable
fun StandardScreenLayout(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(24.dp),
        content = content
    )
}
