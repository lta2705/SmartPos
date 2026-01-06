package com.example.smartpos.ui.theme.screens

sealed class Screen(val route: String) {
    object Welcome : Screen("welcome")
    object Home : Screen("home")
    object Sale : Screen("sale")
    object Tip : Screen("tip")
    object Payment : Screen("payment")
    object Result : Screen("result")
    object Balance : Screen("balance")
    object QR : Screen("qr")
    object Void : Screen("void")
    object Refund : Screen("refund")
    object Settlement : Screen("settlement")
}