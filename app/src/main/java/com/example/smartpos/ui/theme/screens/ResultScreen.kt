package com.example.smartpos.ui.theme.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.smartpos.ui.theme.DarkBackground
import com.example.smartpos.ui.theme.PurplePrimary
import com.example.smartpos.viewmodel.PosViewModel

// ui/screens/ResultScreen.kt
@Composable
fun ResultScreen(viewModel: PosViewModel, onClose: () -> Unit) {
    val amount by viewModel.amount.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().background(DarkBackground),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(100.dp).background(PurplePrimary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(60.dp))
        }
        Spacer(Modifier.height(24.dp))
        Text("Approved!", fontSize = 28.sp, color = PurplePrimary)
        Text("$${amount} USD", fontSize = 24.sp, color = Color.White)

        Spacer(Modifier.height(48.dp))
        Row(Modifier.fillMaxWidth().padding(16.dp)) {
            OutlinedButton(onClick = { /* Print */ }, Modifier.weight(1f)) { Text("Print Copy") }
            Spacer(Modifier.width(8.dp))
            Button(onClick = onClose, Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = LimeGreen)) {
                Text("Close", color = Color.Black)
            }
        }
    }
}