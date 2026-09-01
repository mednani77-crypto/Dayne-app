package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.CurrencyDao
import com.example.data.local.dao.LedgerTransactionDao
import com.example.data.local.dao.PartyDao
import com.example.data.local.dao.SettingsDao
import com.example.data.local.entities.CurrencyEntity
import com.example.data.local.entities.LedgerTransactionEntity
import com.example.data.local.entities.PartyEntity
import com.example.data.local.entities.SettingsEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        SettingsEntity::class,
        CurrencyEntity::class,
        PartyEntity::class,
        LedgerTransactionEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun settingsDao(): SettingsDao
    abstract fun currencyDao(): CurrencyDao
    abstract fun partyDao(): PartyDao
    abstract fun ledgerTransactionDao(): LedgerTransactionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope = CoroutineScope(Dispatchers.IO)): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "deynbook_database.db"
                )
                    .addCallback(DatabaseCallback(scope))
                    // Intentionally no fallbackToDestructiveMigration: future schema changes
                    // must provide explicit Room migrations so user ledger data is never wiped.
                    .build()
                INSTANCE = instance
                instance
            }
        }

        fun getInstance(context: Context): AppDatabase = getDatabase(context)

        val DEFAULT_CURRENCIES = listOf(
            CurrencyEntity(code = "DJF", name = "Djiboutian Franc", symbol = "Fdj", decimalPlaces = 0, isCustom = false, isEnabled = true),
            CurrencyEntity(code = "ETB", name = "Ethiopian Birr", symbol = "Br", decimalPlaces = 2, isCustom = false, isEnabled = false),
            CurrencyEntity(code = "SOS", name = "Somali Shilling", symbol = "Sh.So.", decimalPlaces = 0, isCustom = false, isEnabled = false),
            CurrencyEntity(code = "SLS", name = "Somaliland Shilling", symbol = "Sl.Sh.", decimalPlaces = 0, isCustom = false, isEnabled = false),
            CurrencyEntity(code = "USD", name = "US Dollar", symbol = "$", decimalPlaces = 2, isCustom = false, isEnabled = true),
            CurrencyEntity(code = "KES", name = "Kenyan Shilling", symbol = "KSh", decimalPlaces = 2, isCustom = false, isEnabled = false)
        )

        val DEFAULT_SETTINGS = SettingsEntity(
            id = 1,
            businessName = "",
            businessPhone = null,
            businessAddress = null,
            countryCode = "DJ",
            languageCode = "ar",
            defaultCurrencyCode = "DJF",
            enabledCurrenciesJson = "[\"DJF\",\"USD\"]",
            themeMode = "SYSTEM",
            onboardingCompleted = false
        )

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database)
                    }
                }
            }
        }

        suspend fun populateInitialData(database: AppDatabase) {
            val currencyDao = database.currencyDao()
            val settingsDao = database.settingsDao()

            if (currencyDao.getAllCurrencies().isEmpty()) {
                currencyDao.insertCurrencies(DEFAULT_CURRENCIES)
            }
            if (settingsDao.getSettings() == null) {
                settingsDao.insertOrUpdate(DEFAULT_SETTINGS)
            }
        }
    }
}
