package com.example.pocketpos_lite.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class TodoItem(val id: Int, val name: String)
