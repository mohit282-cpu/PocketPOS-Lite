package com.example.pocketpos_lite.domain.repository

import com.example.pocketpos_lite.core.common.Resource
import com.example.pocketpos_lite.domain.model.DashboardStats
import com.example.pocketpos_lite.domain.model.Sale

interface DashboardRepository {
    suspend fun getDashboardStats(): Resource<DashboardStats>
    suspend fun getRecentSales(limit: Int = 5): Resource<List<Sale>>
}
