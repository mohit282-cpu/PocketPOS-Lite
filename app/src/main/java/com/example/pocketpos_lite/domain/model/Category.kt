package com.example.pocketpos_lite.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Category(
    val id: String? = null,
    val business_id: String,
    val name: String,
    val description: String? = null,
    val created_at: String? = null
)
