package com.mahmoud.feelo.core.domain.repository

import com.mahmoud.feelo.core.model.ChildProfile
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    fun observeProfiles(): Flow<List<ChildProfile>>

    fun observeProfile(id: String): Flow<ChildProfile?>

    suspend fun getProfile(id: String): ChildProfile?

    suspend fun upsert(profile: ChildProfile)

    suspend fun delete(id: String)
}
