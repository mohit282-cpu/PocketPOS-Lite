package com.example.pocketpos_lite.domain.model

data class DashboardStats(
    val todaySales: Double = 0.0,
    val todayTransactions: Int = 0,
    val totalProducts: Int = 0,
    val lowStockProducts: Int = 0,
    val todayExpenses: Double = 0.0,
    val estimatedProfit: Double = 0.0
)
