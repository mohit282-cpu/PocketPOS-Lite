package com.example.pocketpos_lite.domain.repository

import com.example.pocketpos_lite.core.common.Resource
import com.example.pocketpos_lite.domain.model.Profile
import io.github.jan.supabase.auth.user.UserInfo
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUser: Flow<UserInfo?>
    suspend fun signUp(email: String, password: String, fullName: String, businessName: String, phone: String): Resource<Unit>
    suspend fun login(email: String, password: String): Resource<Unit>
    suspend fun logout(): Resource<Unit>
    suspend fun getCurrentSession(): UserInfo?
    suspend fun resetPassword(email: String): Resource<Unit>
}
