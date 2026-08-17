package com.example.pocketpos_lite.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Customer(
    val id: String? = null,
    val business_id: String,
    val name: String,
    val email: String? = null,
    val phone: String? = null,
    val address: String? = null,
    val created_at: String? = null
)
