package com.example.pocketpos_lite.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dummy_table")
data class DummyEntity(
    @PrimaryKey val id: Int = 1,
    val name: String = "PocketPOS"
)
