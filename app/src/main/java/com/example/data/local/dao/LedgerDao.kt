package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entities.LedgerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LedgerDao {
    @Query("SELECT * FROM ledgers ORDER BY isArchived ASC, updatedAt DESC, name COLLATE NOCASE ASC")
    fun getAllLedgersFlow(): Flow<List<LedgerEntity>>

    @Query("SELECT * FROM ledgers WHERE isArchived = 0 ORDER BY updatedAt DESC, name COLLATE NOCASE ASC")
    fun getActiveLedgersFlow(): Flow<List<LedgerEntity>>

    @Query("SELECT * FROM ledgers WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): LedgerEntity?

    @Query("SELECT * FROM ledgers WHERE id = :id LIMIT 1")
    fun getByIdFlow(id: String): Flow<LedgerEntity?>

    @Query("SELECT * FROM ledgers ORDER BY createdAt ASC")
    suspend fun getAll(): List<LedgerEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(ledger: LedgerEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(ledgers: List<LedgerEntity>)

    @Update
    suspend fun update(ledger: LedgerEntity)

    @Query("UPDATE ledgers SET isArchived = :archived, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setArchived(id: String, archived: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM ledgers WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM ledgers")
    suspend fun clear()
}
