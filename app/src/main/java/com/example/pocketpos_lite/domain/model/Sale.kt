package com.example.pocketpos_lite.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Sale(
    val id: String? = null,
    val business_id: String,
    val customer_id: String? = null,
    val total_amount: Double,
    val discount_amount: Double = 0.0,
    val tax_amount: Double = 0.0,
    val net_amount: Double,
    val status: String,
    val created_at: String? = null
)
