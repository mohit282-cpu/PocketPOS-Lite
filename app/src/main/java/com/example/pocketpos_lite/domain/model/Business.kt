package com.example.pocketpos_lite.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Business(
    val id: String? = null,
    val name: String,
    val owner_id: String,
    val address: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val logo_url: String? = null,
    val currency: String = "USD",
    val invoice_prefix: String = "INV",
    val created_at: String? = null
)
