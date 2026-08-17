package com.example.pocketpos_lite.data.repository

import com.example.pocketpos_lite.core.common.Resource
import com.example.pocketpos_lite.domain.model.Business
import com.example.pocketpos_lite.domain.model.Profile
import com.example.pocketpos_lite.domain.repository.AuthRepository
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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
            auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }
            val user = auth.currentUserOrNull() ?: return Resource.Error("User creation failed")

            // Create profile
            val profile = Profile(id = user.id, full_name = fullName)
            postgrest.from("profiles").insert(profile)

            // Create business
            val business = Business(name = businessName, owner_id = user.id, phone = phone)
            val insertedBusiness = postgrest.from("businesses").insert(business) {
                select()
            }.decodeSingle<Business>()

            // Create business user membership
            val businessUser = mapOf(
                "business_id" to insertedBusiness.id,
                "user_id" to user.id,
                "role" to "owner"
            )
            postgrest.from("business_users").insert(businessUser)

            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Unknown error occurred")
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
            Resource.Error(e.message ?: "Login failed")
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
