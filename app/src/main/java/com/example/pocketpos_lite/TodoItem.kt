package com.example.pocketpos_lite

import kotlinx.serialization.Serializable

@Serializable
data class TodoItem(val id: Int, val name: String)
