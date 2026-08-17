package com.example.pocketpos_lite.domain.repository

import com.example.pocketpos_lite.core.common.Resource
import com.example.pocketpos_lite.domain.model.DashboardStats

interface DashboardRepository {
    suspend fun getDashboardStats(): Resource<DashboardStats>
}
