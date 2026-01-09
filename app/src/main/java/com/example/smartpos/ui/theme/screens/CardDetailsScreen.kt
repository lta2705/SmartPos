package com.example.smartpos.ui.theme.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartpos.model.CardData
import com.example.smartpos.network.TransactionResponse
import com.example.smartpos.viewmodel.PosViewModel
import kotlinx.coroutines.delay

@Composable
fun CardDetailsScreen(
    cardData: CardData,
    viewModel: PosViewModel,
    onSuccess: () -> Unit,
    onError: () -> Unit
) {
    var isProcessing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // Xử lý nút Continue - Gửi tới bank connector
    val handleContinue = {
        isProcessing = true
        viewModel.sendTransactionToBankConnector(
            onSuccess = { response ->
                isProcessing = false
                if (response.status == "APPROVED" || response.status == "SUCCESS") {
                    onSuccess()
                } else {
                    errorMessage = response.message ?: "Transaction declined"
                    onError()
                }
            },
            onError = { error ->
                isProcessing = false
                errorMessage = error
                onError()
            }
        )
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Card Details",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // Card Display
        CardDisplay(cardData = cardData)

        Spacer(modifier = Modifier.height(32.dp))

        // Card Information
        CardInformation(cardData = cardData)

        Spacer(modifier = Modifier.weight(1f))

        // Error message if any
        if (errorMessage != null) {
            Text(
                text = errorMessage!!,
                color = Color(0xFFFF6B6B),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Cancel Button
            OutlinedButton(
                onClick = { if (!isProcessing) onError() },
                enabled = !isProcessing,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(2.dp, Color(0xFFFF6B6B)),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFFFF6B6B)
                )
            ) {
                Text(
                    text = "Cancel",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Continue Button (sends to bank connector)
            Button(
                onClick = handleContinue,
                enabled = !isProcessing,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFC4FB6D)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.Black,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Continue",
                        color = Color.Black,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun CardDisplay(cardData: CardData) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF667eea),
                            Color(0xFF764ba2)
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Card Scheme Logo
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Surface(
                        modifier = Modifier.size(48.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White.copy(alpha = 0.9f)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Text(
                                text = cardData.cardScheme,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF667eea)
                            )
                        }
                    }
                }

                // Card Number
                Text(
                    text = cardData.maskedCardNumber,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 2.sp
                )

                // Cardholder Name and Expiry
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = "CARD HOLDER",
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.7f),
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = cardData.cardHolderName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "VALID THRU",
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.7f),
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = cardData.expiryDate,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CardInformation(cardData: CardData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2D2D2D)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Transaction Details",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f))

            // Card Holder Name
            InfoRow(label = "Card Holder", value = cardData.cardHolderName)

            // Card Number
            InfoRow(label = "Card Number", value = cardData.maskedCardNumber)

            // Expiry Date
            InfoRow(label = "Expiry Date", value = cardData.expiryDate)

            // Card Scheme
            InfoRow(label = "Card Type", value = cardData.cardScheme)

            // Validation Status
            val isValid = cardData.isExpiryValid()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Card Status",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isValid) Color(0xFF4CAF50).copy(alpha = 0.2f) else Color(0xFFFF6B6B).copy(alpha = 0.2f)
                ) {
                    Text(
                        text = if (isValid) "Valid" else "Check Expiry",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isValid) Color(0xFF4CAF50) else Color(0xFFFF6B6B),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
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
            fontWeight = FontWeight.Medium,
            color = Color.White
        )
    }
}
