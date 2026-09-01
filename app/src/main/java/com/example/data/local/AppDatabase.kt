package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.CurrencyDao
import com.example.data.local.dao.LedgerDao
import com.example.data.local.dao.LedgerTransactionDao
import com.example.data.local.dao.PartyDao
import com.example.data.local.dao.SettingsDao
import com.example.data.local.dao.TransactionAttachmentDao
import com.example.data.local.entities.CurrencyEntity
import com.example.data.local.entities.LedgerEntity
import com.example.data.local.entities.LedgerTransactionEntity
import com.example.data.local.entities.PartyEntity
import com.example.data.local.entities.SettingsEntity
import com.example.data.local.entities.TransactionAttachmentEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        SettingsEntity::class,
        CurrencyEntity::class,
        LedgerEntity::class,
        PartyEntity::class,
        LedgerTransactionEntity::class,
        TransactionAttachmentEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun settingsDao(): SettingsDao
    abstract fun currencyDao(): CurrencyDao
    abstract fun ledgerDao(): LedgerDao
    abstract fun partyDao(): PartyDao
    abstract fun ledgerTransactionDao(): LedgerTransactionDao
    abstract fun transactionAttachmentDao(): TransactionAttachmentDao

    companion object {
        const val DEFAULT_LEDGER_ID = "default-ledger"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE ledger_transactions ADD COLUMN dueAt INTEGER")
                db.execSQL("ALTER TABLE ledger_transactions ADD COLUMN attachmentPath TEXT")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_ledger_transactions_dueAt ON ledger_transactions(dueAt)")
                db.execSQL("ALTER TABLE settings ADD COLUMN biometricLockEnabled INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE settings ADD COLUMN calendarMode TEXT NOT NULL DEFAULT 'GREGORIAN'")
            }
        }

        /**
         * DeynBook 1.2 migration. Existing users keep every party/transaction in a new
         * default ledger. The old single attachment column is preserved for compatibility,
         * and also copied into the new one-to-many attachment table.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS ledgers (
                        id TEXT NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        phone TEXT,
                        address TEXT,
                        countryCode TEXT NOT NULL,
                        defaultCurrencyCode TEXT NOT NULL,
                        logoPath TEXT,
                        footerNote TEXT,
                        isArchived INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_ledgers_isArchived ON ledgers(isArchived)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_ledgers_updatedAt ON ledgers(updatedAt)")

                val now = System.currentTimeMillis()
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO ledgers(
                        id, name, phone, address, countryCode, defaultCurrencyCode,
                        logoPath, footerNote, isArchived, createdAt, updatedAt
                    )
                    SELECT ?,
                           CASE WHEN TRIM(businessName) = '' THEN 'DeynBook' ELSE businessName END,
                           businessPhone,
                           businessAddress,
                           countryCode,
                           defaultCurrencyCode,
                           NULL,
                           NULL,
                           0,
                           ?,
                           ?
                    FROM settings WHERE id = 1
                    """.trimIndent(),
                    arrayOf(DEFAULT_LEDGER_ID, now, now)
                )

                db.execSQL("ALTER TABLE parties ADD COLUMN ledgerId TEXT NOT NULL DEFAULT '$DEFAULT_LEDGER_ID'")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_parties_ledgerId ON parties(ledgerId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_parties_ledgerId_normalizedName ON parties(ledgerId, normalizedName)")

                db.execSQL("ALTER TABLE settings ADD COLUMN activeLedgerId TEXT NOT NULL DEFAULT '$DEFAULT_LEDGER_ID'")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS transaction_attachments (
                        id TEXT NOT NULL PRIMARY KEY,
                        transactionId TEXT NOT NULL,
                        filePath TEXT NOT NULL,
                        displayName TEXT NOT NULL,
                        mimeType TEXT NOT NULL,
                        sizeBytes INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        FOREIGN KEY(transactionId) REFERENCES ledger_transactions(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_transaction_attachments_transactionId ON transaction_attachments(transactionId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_transaction_attachments_createdAt ON transaction_attachments(createdAt)")

                db.execSQL(
                    """
                    INSERT OR IGNORE INTO transaction_attachments(
                        id, transactionId, filePath, displayName, mimeType, sizeBytes, createdAt
                    )
                    SELECT 'legacy-' || id,
                           id,
                           attachmentPath,
                           'Attachment',
                           'application/octet-stream',
                           0,
                           createdAt
                    FROM ledger_transactions
                    WHERE attachmentPath IS NOT NULL AND TRIM(attachmentPath) <> ''
                    """.trimIndent()
                )
            }
        }

        fun getDatabase(context: Context, scope: CoroutineScope = CoroutineScope(Dispatchers.IO)): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "deynbook_database.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .addCallback(DatabaseCallback(scope))
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
            onboardingCompleted = false,
            biometricLockEnabled = false,
            calendarMode = "GREGORIAN",
            activeLedgerId = DEFAULT_LEDGER_ID
        )

        fun defaultLedgerFromSettings(settings: SettingsEntity = DEFAULT_SETTINGS): LedgerEntity = LedgerEntity(
            id = DEFAULT_LEDGER_ID,
            name = settings.businessName.ifBlank { "DeynBook" },
            phone = settings.businessPhone,
            address = settings.businessAddress,
            countryCode = settings.countryCode,
            defaultCurrencyCode = settings.defaultCurrencyCode
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
            val ledgerDao = database.ledgerDao()

            if (currencyDao.getAllCurrencies().isEmpty()) {
                currencyDao.insertCurrencies(DEFAULT_CURRENCIES)
            }
            val settings = settingsDao.getSettings() ?: DEFAULT_SETTINGS.also { settingsDao.insertOrUpdate(it) }
            if (ledgerDao.getAll().isEmpty()) {
                ledgerDao.insert(defaultLedgerFromSettings(settings))
            }
        }
    }
}
