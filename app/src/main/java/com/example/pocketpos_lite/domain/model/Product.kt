package com.example.pocketpos_lite.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Product(
    val id: String? = null,
    val business_id: String,
    val category_id: String? = null,
    val name: String,
    val sku: String? = null,
    val price: Double,
    val stock_quantity: Double = 0.0,
    val is_active: Boolean = true,
    val created_at: String? = null
)
