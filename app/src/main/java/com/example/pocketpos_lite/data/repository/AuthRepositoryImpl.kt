package com.example.pocketpos_lite.data.repository

import com.example.pocketpos_lite.core.common.Resource
import com.example.pocketpos_lite.domain.model.Profile
import com.example.pocketpos_lite.domain.repository.AuthRepository
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val auth: Auth,
    private val postgrest: Postgrest
) : AuthRepository {

    override val currentUser: Flow<UserInfo?> = auth.sessionStatus.map { status ->
        when (status) {
            is SessionStatus.Authenticated -> status.session.user
            else -> null
        }
    }

    override suspend fun signUp(
        email: String,
        password: String,
        fullName: String,
        businessName: String,
        phone: String
    ): Resource<Unit> {
        return try {
            val signUpResult = auth.signUpWith(Email) {
                this.email = email
                this.password = password
                // Pass metadata so the database trigger can create the business
                data = buildJsonObject {
                    put("full_name", fullName)
                    put("business_name", businessName)
                    put("phone", phone)
                }
            }
            
            if (signUpResult == null && auth.currentUserOrNull() == null) {
                return Resource.Error("User creation failed")
            }

            // The profile, business, and business_users records are now handled 
            // automatically by the database trigger 'on_auth_user_created'.
            // This is more secure and works even if email confirmation is required.

            Resource.Success(Unit)
        } catch (e: Exception) {
            val cleanError = e.message?.substringBefore("\n") ?: "Unknown error occurred"
            Resource.Error(cleanError)
        }
    }

    override suspend fun login(email: String, password: String): Resource<Unit> {
        return try {
            auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            Resource.Success(Unit)
        } catch (e: Exception) {
            val cleanError = e.message?.substringBefore("\n") ?: "Login failed"
            Resource.Error(cleanError)
        }
    }

    override suspend fun logout(): Resource<Unit> {
        return try {
            auth.signOut()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Logout failed")
        }
    }

    override suspend fun getCurrentSession(): UserInfo? {
        // Wait for session to initialize if it's currently initializing
        auth.sessionStatus.first { it !is SessionStatus.Initializing }
        return auth.currentUserOrNull()
    }

    override suspend fun resetPassword(email: String): Resource<Unit> {
        return try {
            auth.resetPasswordForEmail(email)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Password reset failed")
        }
    }
}
