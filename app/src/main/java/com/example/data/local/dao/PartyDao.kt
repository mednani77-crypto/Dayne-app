package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entities.PartyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PartyDao {
    @Query("SELECT * FROM parties WHERE isArchived = 0 ORDER BY name COLLATE NOCASE ASC")
    fun getActivePartiesFlow(): Flow<List<PartyEntity>>

    @Query("SELECT * FROM parties ORDER BY name COLLATE NOCASE ASC")
    fun getAllPartiesFlow(): Flow<List<PartyEntity>>

    @Query("SELECT * FROM parties WHERE id = :id LIMIT 1")
    fun getPartyFlowById(id: String): Flow<PartyEntity?>

    @Query("SELECT * FROM parties WHERE id = :id LIMIT 1")
    suspend fun getPartyById(id: String): PartyEntity?

    @Query("SELECT * FROM parties")
    suspend fun getAllParties(): List<PartyEntity>

    @Query("SELECT * FROM parties WHERE normalizedName LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%'")
    suspend fun searchParties(query: String): List<PartyEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParty(party: PartyEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParties(parties: List<PartyEntity>)

    @Update
    suspend fun updateParty(party: PartyEntity)

    @Query("UPDATE parties SET isArchived = :isArchived, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setPartyArchived(id: String, isArchived: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM parties WHERE id = :id")
    suspend fun deleteParty(id: String)

    @Query("DELETE FROM parties")
    suspend fun clearParties()
}
