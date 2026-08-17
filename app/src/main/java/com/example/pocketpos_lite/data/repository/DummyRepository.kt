package com.example.pocketpos_lite.data.repository

import com.example.pocketpos_lite.data.local.AppDatabase
import io.github.jan.supabase.SupabaseClient
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DummyRepository @Inject constructor(
    private val database: AppDatabase,
    private val supabaseClient: SupabaseClient
) {
    fun getProjectName() = "PocketPOS Lite from Repository"
}
