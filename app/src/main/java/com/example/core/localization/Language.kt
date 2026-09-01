package com.example.core.localization

enum class AppLanguage(val code: String, val nativeName: String, val isRtl: Boolean) {
    ARABIC("ar", "العربية", true),
    SOMALI("so", "Soomaali", false),
    AMHARIC("am", "አማርኛ", false),
    FRENCH("fr", "Français", false),
    ENGLISH("en", "English", false);

    companion object {
        fun fromCode(code: String): AppLanguage {
            return entries.find { it.code.equals(code, ignoreCase = true) } ?: ARABIC
        }
    }
}

data class Strings(
    // App identity
    val appName: String,
    val appTagline: String,

    // Navigation
    val navHome: String,
    val navParties: String,
    val navReports: String,
    val navMore: String,
    val addTransaction: String,

    // Onboarding
    val stepLanguageTitle: String,
    val stepLanguageDesc: String,
    val stepCountryTitle: String,
    val stepCountryDesc: String,
    val stepCurrencyTitle: String,
    val stepCurrencyDesc: String,
    val stepBusinessTitle: String,
    val stepBusinessDesc: String,
    val businessNameLabel: String,
    val businessNamePlaceholder: String,
    val businessPhoneLabel: String,
    val startUsingApp: String,
    val next: String,
    val back: String,

    // Home / Dashboard
    val receivableTitle: String, // لي عند الناس
    val payableTitle: String,    // عليّ للناس
    val quickActionCustomerDebt: String, // دين على عميل
    val quickActionCustomerPayment: String, // استلام دفعة
    val quickActionSupplierDebt: String, // دين لمورد
    val quickActionSupplierPayment: String,
    val txSubtitleCustomerDebt: String,
    val txSubtitleCustomerPayment: String,
    val txSubtitleSupplierDebt: String,
    val txSubtitleSupplierPayment: String, // دفع لمورد
    val recentTransactions: String,
    val seeAll: String,
    val noTransactionsYet: String,
    val addFirstTransaction: String,
    val searchPlaceholder: String,
    val selectCurrency: String,
    val allCurrencies: String,

    // Parties / Accounts
    val partiesTitle: String,
    val tabAll: String,
    val tabCustomers: String,
    val tabSuppliers: String,
    val tabArchived: String,
    val addParty: String,
    val editParty: String,
    val partyNameLabel: String,
    val partyPhoneLabel: String,
    val partyTypeLabel: String,
    val typeCustomer: String,
    val typeSupplier: String,
    val typeBoth: String,
    val notesLabel: String,
    val openingBalanceTitle: String,
    val openingBalanceNone: String,
    val openingBalanceHeOwesMe: String, // هو مدين لي
    val openingBalanceIOweHim: String,  // أنا مدين له
    val openingBalanceAmount: String,
    val similarNameWarning: String,
    val noPartiesFound: String,
    val addFirstParty: String,
    val archiveParty: String,
    val unarchiveParty: String,
    val deleteParty: String,
    val deletePartyWarning: String,
    val cannotDeletePartyWithTx: String,

    // Party Balances & Status
    val statusCustomerOwes: String,   // عليه
    val statusSupplierOwesUs: String, // علينا له
    val statusSettled: String,         // مسدد
    val statusCustomerCredit: String,  // له رصيد (صالح العميل)
    val statusSupplierAdvance: String, // دفعة مقدمة للمورد
    val totalDebtRecorded: String,
    val totalPaidRecorded: String,
    val netBalance: String,

    // Transactions
    val txCustomerDebtTitle: String,    // أضفت دينًا على عميل
    val txCustomerPaymentTitle: String, // استلام دفعة من عميل
    val txSupplierDebtTitle: String,    // اشتريت بالدين من مورد
    val txSupplierPaymentTitle: String, // دفعت لمورد
    val txOpeningReceivable: String,    // رصيد افتتاحي (مدين)
    val txOpeningPayable: String,       // رصيد افتتاحي (دائن)
    val amountLabel: String,
    val currencyLabel: String,
    val dateLabel: String,
    val timeLabel: String,
    val noteLabel: String,
    val selectParty: String,
    val addNewPartyOption: String,
    val save: String,
    val cancel: String,
    val delete: String,
    val edit: String,
    val confirmDeleteTxTitle: String,
    val confirmDeleteTxMessage: String,
    val undo: String,
    val overpaymentWarning: String,
    val advancePaymentWarning: String,
    val txSuccessSaved: String,
    val txSuccessDeleted: String,
    val txSuccessUpdated: String,

    // Quick note chips
    val noteChipCash: String,
    val noteChipWaafi: String,
    val noteChipGoods: String,
    val noteChipPartial: String,

    // Party Detail
    val callParty: String,
    val shareStatement: String,
    val shareTextSummary: String,
    val shareImageCard: String,
    val exportPdfStatement: String,
    val filterAll: String,
    val filterDebts: String,
    val filterPayments: String,
    val balanceAfterTx: String,

    // Reports
    val reportsTitle: String,
    val periodToday: String,
    val periodLast7Days: String,
    val periodLast30Days: String,
    val periodThisMonth: String,
    val periodCustom: String,
    val newDebtsOnCustomers: String,
    val customerPaymentsReceived: String,
    val newDebtsToSuppliers: String,
    val supplierPaymentsPaid: String,
    val customerCreditsSum: String,
    val supplierAdvancesSum: String,
    val topDebtorCustomers: String,
    val topCreditorSuppliers: String,
    val totalTxCount: String,

    // Statement / PDF
    val statementTitle: String,
    val statementFor: String,
    val statementPeriod: String,
    val statementGeneratedAt: String,
    val statementInitialBalance: String,
    val statementTotalDebts: String,
    val statementTotalPayments: String,
    val statementClosingBalance: String,
    val statementDescription: String,
    val statementDebtCol: String,
    val statementPaymentCol: String,
    val statementRunningBalanceCol: String,
    val statementFooterText: String,

    // Backup & Restore & CSV
    val backupAndRestoreTitle: String,
    val createBackup: String,
    val restoreBackup: String,
    val exportCsv: String,
    val lastBackupDate: String,
    val neverBackedUp: String,
    val backupSuccess: String,
    val backupShareDesc: String,
    val restorePreviewTitle: String,
    val restorePreviewDesc: String,
    val restoreWarningMessage: String,
    val restoreSuccess: String,
    val restoreInvalidFile: String,
    val restoreUnsupportedVersion: String,
    val csvExportSuccess: String,

    // Settings
    val settingsTitle: String,
    val businessProfileTitle: String,
    val businessAddressLabel: String,
    val countryLabel: String,
    val languageSettingTitle: String,
    val currencySettingTitle: String,
    val defaultCurrencyLabel: String,
    val enabledCurrenciesLabel: String,
    val addCustomCurrency: String,
    val currencyCodeLabel: String,
    val currencyNameLabel: String,
    val decimalPlacesLabel: String,
    val themeTitle: String,
    val themeSystem: String,
    val themeLight: String,
    val themeDark: String,
    val privacyPolicyTitle: String,
    val aboutAppTitle: String,
    val appVersionLabel: String,
    val openSourceLicenses: String,
    val dangerZoneTitle: String,
    val deleteAllData: String,
    val deleteAllDataConfirmTitle: String,
    val deleteAllDataConfirmMessage: String,
    val typeDeleteToConfirm: String,
    val deleteKeyword: String,

    // Error messages
    val errorNameRequired: String,
    val errorAmountMustBePositive: String,
    val errorSelectParty: String,
    val errorGeneric: String
)
