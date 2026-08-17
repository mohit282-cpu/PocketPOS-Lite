package com.example.pocketpos_lite.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Expense(
    val id: String? = null,
    val business_id: String,
    val description: String,
    val amount: Double,
    val category: String? = null,
    val expense_date: String? = null,
    val created_at: String? = null
)
