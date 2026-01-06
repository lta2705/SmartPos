package com.example.smartpos.ui.theme.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.smartpos.ui.theme.screens.*
import com.example.smartpos.viewmodel.PosViewModel

@Composable
fun PosNavGraph(navController: NavHostController, viewModel: PosViewModel) {
    NavHost(
        navController = navController,
        startDestination = "welcome"
    ) {
        // 1. Màn hình chào mừng
        composable("welcome") {
            WelcomeScreen(onEnterApp = { navController.navigate("home") })
        }

        // 2. Menu chính
        composable("home") {
            HomeScreen(
                onSaleClick = { navController.navigate("sale") },
                onRefundClick = { navController.navigate("refund") },
                onBalanceClick = { navController.navigate("balance") },
                onQRClick = { navController.navigate("qr") },
                onVoidClick = { navController.navigate("void") },
                onSettlementClick = { navController.navigate("settlement") }
            )
        }

        // 3. Nhập số tiền bán hàng
        composable("sale") {
            SaleScreen(viewModel = viewModel
                , onConfirm = {
                navController.navigate("tip")
            },
                onReturn ={
                    navController.navigate("home")
                })
        }

        // 4. Chọn tiền Tip
        composable("tip") {
            TipScreen(
                viewModel = viewModel,
                onConfirm = { navController.navigate("payment") }
            )
        }

        // 5. Xử lý thanh toán
        composable("payment") {
            PaymentScreen(
                viewModel = viewModel,
                onPaymentSuccess = {
                    navController.navigate("result") {
                        // Xóa các màn hình trung gian để không quay lại được bước thanh toán
                        popUpTo("home") { inclusive = false }
                    }
                }
            )
        }

        composable("balance") {
            BalanceScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        // 6. Màn hình QR Payment
        composable("qr") {
            QRScreen(
                viewModel = viewModel,
                onConfirm = {
                    viewModel.reset()
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }
                },
                onReturn = { navController.navigate("home") }
            )
        }

        // 7. Màn hình Void (hiển thị giao dịch Sale)
        composable("void") {
            VoidScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        // 8. Màn hình Refund (hiển thị giao dịch QR)
        composable("refund") {
            RefundScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        // 9. Màn hình Settlement
        composable("settlement") {
            SettlementScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        // 10. Kết quả giao dịch
        composable("result") {
            ResultScreen(
                viewModel = viewModel,
                onClose = {
                    viewModel.reset() // Xóa dữ liệu cũ trên màn hình nhập
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }
                }
            )
        }
    }
}