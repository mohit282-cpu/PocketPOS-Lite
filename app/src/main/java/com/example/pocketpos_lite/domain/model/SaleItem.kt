package com.example.pocketpos_lite.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class SaleItem(
    val id: String? = null,
    val sale_id: String? = null,
    val product_id: String,
    val quantity: Double,
    val unit_price: Double,
    val subtotal: Double,
    val created_at: String? = null
)
