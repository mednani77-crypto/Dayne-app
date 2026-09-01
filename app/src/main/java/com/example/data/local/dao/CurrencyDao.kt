package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entities.CurrencyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CurrencyDao {
    @Query("SELECT * FROM currencies ORDER BY isEnabled DESC, code ASC")
    fun getAllCurrenciesFlow(): Flow<List<CurrencyEntity>>

    @Query("SELECT * FROM currencies WHERE isEnabled = 1 ORDER BY code ASC")
    fun getEnabledCurrenciesFlow(): Flow<List<CurrencyEntity>>

    @Query("SELECT * FROM currencies WHERE isEnabled = 1 ORDER BY code ASC")
    suspend fun getEnabledCurrencies(): List<CurrencyEntity>

    @Query("SELECT * FROM currencies")
    suspend fun getAllCurrencies(): List<CurrencyEntity>

    @Query("SELECT * FROM currencies WHERE code = :code LIMIT 1")
    suspend fun getCurrencyByCode(code: String): CurrencyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCurrencies(currencies: List<CurrencyEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCurrency(currency: CurrencyEntity)

    @Update
    suspend fun updateCurrency(currency: CurrencyEntity)

    @Query("UPDATE currencies SET isEnabled = :isEnabled WHERE code = :code")
    suspend fun setCurrencyEnabled(code: String, isEnabled: Boolean)

    @Query("DELETE FROM currencies")
    suspend fun clearCurrencies()
}
