package com.example.smartpos.ui.theme.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.smartpos.ui.theme.screens.SaleScreen
import com.example.smartpos.viewmodel.PosViewModel

@Composable
fun PosNavGraph(navController: NavHostController, viewModel: PosViewModel) {
    NavHost(
        navController = navController,
        startDestination = "sale" // Hoặc "welcome" tùy ý bạn
    ) {
        composable("sale") {
            SaleScreen(viewModel = viewModel, onConfirm = {
                navController.navigate("tip")
            })
        }
        // Thêm các màn hình khác tương tự tại đây...
        composable("tip") { /* TipScreen(...) */ }
        composable("payment") { /* PaymentScreen(...) */ }
    }
}