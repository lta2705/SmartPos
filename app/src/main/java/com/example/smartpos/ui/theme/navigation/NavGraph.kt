package com.example.smartpos.ui.theme.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
                onConfirm = { navController.navigate("processing") }
            )
        }

        // 5. Màn hình xử lý TCP connection và chờ response
        composable("processing") {
            ProcessingScreen(
                viewModel = viewModel,
                onNavigateToSale = {
                    // SALE -> Payment (not Result directly)
                    navController.navigate("payment") {
                        popUpTo("home") { inclusive = false }
                    }
                },
                onNavigateToVoid = {
                    navController.navigate("void") {
                        popUpTo("home") { inclusive = false }
                    }
                },
                onNavigateToQr = {
                    navController.navigate("qr") {
                        popUpTo("home") { inclusive = false }
                    }
                },
                onNavigateToRefund = {
                    navController.navigate("refund") {
                        popUpTo("home") { inclusive = false }
                    }
                },
                onError = { errorMessage ->
                    // Có thể navigate tới error screen hoặc quay về home
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }
                }
            )
        }

        // 6. Xử lý thanh toán - đọc NFC và chờ
        composable("payment") {
            PaymentScreen(
                viewModel = viewModel,
                onCardRead = {
                    // Khi đọc xong NFC, navigate tới card details
                    navController.navigate("cardDetails")
                },
                onTimeout = {
                    // Timeout -> về home
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }
                }
            )
        }

        // 7. Màn hình hiển thị thông tin thẻ
        composable("cardDetails") {
            val cardData = viewModel.getCurrentCardData()
            if (cardData != null) {
                CardDetailsScreen(
                    cardData = cardData,
                    onSuccess = {
                        viewModel.onTransactionSuccess()
                        navController.navigate("result") {
                            popUpTo("home") { inclusive = false }
                        }
                    },
                    onError = {
                        viewModel.onTransactionError("Transaction declined")
                        navController.navigate("error") {
                            popUpTo("home") { inclusive = false }
                        }
                    }
                )
            } else {
                // No card data, go back
                LaunchedEffect(Unit) {
                    navController.popBackStack()
                }
            }
        }

        // 8. Màn hình error
        composable("error") {
            ErrorScreen(
                viewModel = viewModel,
                onClose = {
                    viewModel.reset()
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
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

        // 9. Màn hình QR Payment
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

        // 10. Màn hình Void (hiển thị giao dịch Sale)
        composable("void") {
            VoidScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        // 11. Màn hình Refund (hiển thị giao dịch QR)
        composable("refund") {
            RefundScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        // 12. Màn hình Settlement
        composable("settlement") {
            SettlementScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        // 13. Kết quả giao dịch
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