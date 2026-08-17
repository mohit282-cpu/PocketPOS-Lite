package com.example.pocketpos_lite.domain.repository

import com.example.pocketpos_lite.core.common.Resource
import com.example.pocketpos_lite.domain.model.Business

interface BusinessRepository {
    suspend fun getBusinessProfile(): Resource<Business>
    suspend fun updateBusinessProfile(business: Business): Resource<Unit>
    suspend fun uploadLogo(fileName: String, byteArray: ByteArray): Resource<String>
}
