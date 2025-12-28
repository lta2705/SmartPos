package com.example.smartpos.ui.theme.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.smartpos.ui.theme.screens.BalanceScreen
import com.example.smartpos.ui.theme.screens.HomeScreen
import com.example.smartpos.ui.theme.screens.PaymentScreen
import com.example.smartpos.ui.theme.screens.ResultScreen
import com.example.smartpos.ui.theme.screens.SaleScreen
import com.example.smartpos.ui.theme.screens.TipScreen
import com.example.smartpos.ui.theme.screens.WelcomeScreen
import com.example.smartpos.viewmodel.PosViewModel

@Composable
fun PosNavGraph(navController: NavHostController, viewModel: PosViewModel) {
    NavHost(
        navController = navController,
        startDestination = "welcome" // Hoặc "welcome" tùy ý bạn
    ) {
        composable("welcome") {
            WelcomeScreen(onEnterApp = { navController.navigate("home") })
        }
        composable("home") {
            HomeScreen(
                onSaleClick = { navController.navigate("sale") },
                onRefundClick = { navController.navigate("sale") }, // Refund có thể dùng chung màn nhập số tiền
                onBalanceClick = { navController.navigate("balance") }
            )
        }
        composable("sale") {
            SaleScreen(viewModel = viewModel, onConfirm = {
                navController.navigate("tip")
            })
        }
        // Thêm các màn hình khác tương tự tại đây...
        composable("tip") { /* TipScreen(...) */ }
        composable("payment") { /* PaymentScreen(...) */ }
        composable("tip") {
            TipScreen(
                viewModel = viewModel,
                onConfirm = {
                    navController.navigate("payment")
                }
            )
        }
        composable("payment") {
            PaymentScreen(
                viewModel = viewModel,
                onPaymentSuccess = {
                    // Sử dụng popUpTo để dọn dẹp stack, tránh quay lại màn hình Payment khi nhấn Back
                    navController.navigate("result") {
                        popUpTo("home") { inclusive = false }
                    }
                }
            )
        }
        composable("balance") {
            BalanceScreen(onBack = { navController.popBackStack() })
        }
        composable("result") {
            ResultScreen(
                viewModel = viewModel,
                onClose = {
                    viewModel.reset() // Reset dữ liệu về 0
                    navController.popBackStack("sale", inclusive = false) // Quay về màn hình đầu
                }
            )
        }
    }
}