package com.example.smartpos.ui.theme.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartpos.network.TcpConnectionState
import com.example.smartpos.viewmodel.PosViewModel

@Composable
fun ProcessingScreen(
    viewModel: PosViewModel,
    onNavigateToSale: () -> Unit,
    onNavigateToVoid: () -> Unit,
    onNavigateToQr: () -> Unit,
    onNavigateToRefund: () -> Unit,
    onError: (String) -> Unit
) {
    val connectionState by viewModel.tcpConnectionState.collectAsState()

    LaunchedEffect(connectionState) {
        when (val state = connectionState) {
            is TcpConnectionState.DataReceived -> {
                // Parse TransactionType và navigate tới màn hình tương ứng
                when (state.response.transactionType.uppercase()) {
                    "SALE" -> onNavigateToSale()
                    "VOID" -> onNavigateToVoid()
                    "QR" -> onNavigateToQr()
                    "REFUND" -> onNavigateToRefund()
                    else -> onError("Unknown transaction type: ${state.response.transactionType}")
                }
            }
            is TcpConnectionState.Error -> {
                onError(state.message)
            }
            else -> { /* Do nothing for Idle, Connecting, Connected states */ }
        }
    }

    // Bắt đầu kết nối TCP khi màn hình được hiển thị
    LaunchedEffect(Unit) {
        viewModel.startTcpConnection()
    }

    // Cleanup khi màn hình bị destroy
    DisposableEffect(Unit) {
        onDispose {
            viewModel.disconnectTcp()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when (val state = connectionState) {
            is TcpConnectionState.Idle -> {
                Text(
                    text = "Initializing...",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            is TcpConnectionState.Connecting -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(64.dp),
                    color = Color(0xFF6C5CE7),
                    strokeWidth = 4.dp
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Đang kết nối tới server...",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Vui lòng đợi",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
            is TcpConnectionState.Connected -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(64.dp),
                    color = Color(0xFF6C5CE7),
                    strokeWidth = 4.dp
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Đã kết nối!",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF6C5CE7)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Đang chờ phản hồi từ server...",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
            is TcpConnectionState.DataReceived -> {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.CheckCircle,
                    contentDescription = "Success",
                    modifier = Modifier.size(64.dp),
                    tint = Color(0xFF6C5CE7)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Đã nhận dữ liệu!",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF6C5CE7)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Transaction Type: ${state.response.transactionType}",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
            is TcpConnectionState.Error -> {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.Error,
                    contentDescription = "Error",
                    modifier = Modifier.size(64.dp),
                    tint = Color(0xFFFF6B6B)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Lỗi kết nối",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFFF6B6B)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = state.message,
                    fontSize = 14.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { viewModel.retryTcpConnection() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C5CE7))
                ) {
                    Text(text = "Thử lại", color = Color.White)
                }
            }
        }
    }
}
