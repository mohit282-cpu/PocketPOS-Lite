package com.example.pocketpos_lite.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [DummyEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    // DAOs will be added here
}
