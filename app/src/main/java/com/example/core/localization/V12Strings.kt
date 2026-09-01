package com.example.core.localization

data class V12Strings(
    val ledgers: String,
    val switchLedger: String,
    val newLedger: String,
    val editLedger: String,
    val ledgerName: String,
    val ledgerProfile: String,
    val logo: String,
    val chooseLogo: String,
    val removeLogo: String,
    val footerNote: String,
    val cannotDeleteLedger: String,
    val currentLedger: String,
    val smartOverview: String,
    val overdue: String,
    val dueNext7Days: String,
    val collectedThisMonth: String,
    val paidSuppliersThisMonth: String,
    val topDebtors: String,
    val advancedSearch: String,
    val minBalance: String,
    val allBalances: String,
    val debtorsOnly: String,
    val settledOnly: String,
    val sortName: String,
    val sortLargestDebt: String,
    val sortRecent: String,
    val excelTitle: String,
    val exportExcel: String,
    val importExcel: String,
    val importTemplate: String,
    val excelImportSuccess: String,
    val excelImportFailed: String,
    val importedAccounts: String,
    val importedTransactions: String,
    val activity: String,
    val collectionActivity: String,
    val multipleAttachments: String,
    val addAttachment: String,
    val attachments: String,
    val open: String,
    val sharedToDeynBook: String,
    val chooseAccountForSharedFile: String,
    val exportAll: String,
    val dateRange: String,
    val ledgerEmpty: String,
    val save: String,
    val cancel: String,
    val delete: String,
    val archive: String,
    val unarchive: String
)

object V12StringsProvider {
    fun get(language: AppLanguage): V12Strings = when (language) {
        AppLanguage.ARABIC -> V12Strings(
            "الدفاتر", "تبديل الدفتر", "دفتر جديد", "تعديل الدفتر", "اسم الدفتر", "هوية الدفتر", "الشعار", "اختيار شعار", "إزالة الشعار", "ملاحظة أسفل كشف الحساب", "لا يمكن حذف دفتر يحتوي على حسابات أو الدفتر الحالي", "الدفتر الحالي", "نظرة ذكية", "متأخر", "مستحق خلال 7 أيام", "المحصل هذا الشهر", "المدفوع للموردين هذا الشهر", "أكبر المدينين", "بحث متقدم", "أقل رصيد", "كل الأرصدة", "المدينون فقط", "المسددون فقط", "الترتيب بالاسم", "الأكبر دينًا", "الأحدث", "Excel", "تصدير Excel", "استيراد Excel", "قالب استيراد Excel", "تم الاستيراد بنجاح", "تعذر استيراد ملف Excel", "حسابات مستوردة", "عمليات مستوردة", "النشاط", "نشاط التحصيل والتسديد", "مرفقات متعددة", "إضافة مرفق", "المرفقات", "فتح", "تمت مشاركة ملف إلى DeynBook", "اختر الحساب لإضافة الملف المشترك", "تصدير الكل", "نطاق التاريخ", "هذا الدفتر فارغ", "حفظ", "إلغاء", "حذف", "أرشفة", "إلغاء الأرشفة"
        )
        AppLanguage.SOMALI -> V12Strings(
            "Buugaagta", "Beddel buugga", "Buug cusub", "Wax ka beddel buugga", "Magaca buugga", "Aqoonsiga buugga", "Astaanta", "Dooro astaan", "Ka saar astaanta", "Qoraalka hoose ee warbixinta", "Buug leh xisaabo ama buugga hadda lama tirtiri karo", "Buugga hadda", "Dulmar caqli leh", "Daahay", "7 maalmood gudahood", "La ururiyey bishan", "Alaab-qeybiyeyaasha la siiyey bishan", "Deyn-qaatayaasha ugu waaweyn", "Raadin horumarsan", "Hadhaaga ugu yar", "Dhammaan hadhaaga", "Deyn-qaatayaasha", "La xisaabtamay", "Magac", "Deynta ugu weyn", "Ugu dambeeyay", "Excel", "Dhoofinta Excel", "Soo dejinta Excel", "Qaabka soo dejinta", "Soo dejin waa guul", "Soo dejinta Excel way fashilantay", "Xisaabaad la soo dejiyey", "Dhaqdhaqaaqyo la soo dejiyey", "Dhaqdhaqaaqa", "Ururinta iyo bixinta", "Lifaaqyo badan", "Ku dar lifaaq", "Lifaaqyada", "Fur", "Fayl ayaa lala wadaagay DeynBook", "Dooro xisaabta faylka", "Dhoofinta dhammaan", "Xadka taariikhda", "Buuggani waa madhan", "Kaydi", "Jooji", "Tirtir", "Kaydi/Archive", "Ka saar archive"
        )
        AppLanguage.AMHARIC -> V12Strings(
            "ደብተሮች", "ደብተር ቀይር", "አዲስ ደብተር", "ደብተር አርትዕ", "የደብተር ስም", "የደብተር መለያ", "አርማ", "አርማ ምረጥ", "አርማ አስወግድ", "የመግለጫ ግርጌ ማስታወሻ", "መለያዎች ያሉበትን ወይም ንቁ ደብተርን መሰረዝ አይቻልም", "ንቁ ደብተር", "ብልህ እይታ", "ያለፈ", "በ7 ቀን ውስጥ", "በዚህ ወር የተሰበሰበ", "በዚህ ወር ለአቅራቢዎች የተከፈለ", "ትልቁ ተበዳሪዎች", "የላቀ ፍለጋ", "ዝቅተኛ ቀሪ", "ሁሉም ቀሪ", "ተበዳሪዎች ብቻ", "የተከፈሉ ብቻ", "በስም", "ትልቁ ዕዳ", "የቅርብ ጊዜ", "Excel", "Excel ላክ", "Excel አስገባ", "የExcel አብነት", "ማስገባት ተሳክቷል", "Excel ማስገባት አልተሳካም", "የገቡ መለያዎች", "የገቡ ግብይቶች", "እንቅስቃሴ", "የስብስብ እና ክፍያ እንቅስቃሴ", "ብዙ አባሪዎች", "አባሪ ጨምር", "አባሪዎች", "ክፈት", "ፋይል ወደ DeynBook ተጋርቷል", "ለተጋራው ፋይል መለያ ምረጥ", "ሁሉን ላክ", "የቀን ክልል", "ይህ ደብተር ባዶ ነው", "አስቀምጥ", "ሰርዝ", "ሰርዝ", "Archive", "Archive አስወግድ"
        )
        AppLanguage.FRENCH -> V12Strings(
            "Carnets", "Changer de carnet", "Nouveau carnet", "Modifier le carnet", "Nom du carnet", "Identité du carnet", "Logo", "Choisir un logo", "Retirer le logo", "Note de pied de relevé", "Impossible de supprimer le carnet actif ou un carnet contenant des comptes", "Carnet actuel", "Vue intelligente", "En retard", "Échéance sous 7 jours", "Encaissements ce mois", "Paiements fournisseurs ce mois", "Plus gros débiteurs", "Recherche avancée", "Solde minimum", "Tous les soldes", "Débiteurs seulement", "Soldés seulement", "Trier par nom", "Dette la plus élevée", "Plus récents", "Excel", "Exporter Excel", "Importer Excel", "Modèle d’import Excel", "Import réussi", "Échec de l’import Excel", "Comptes importés", "Opérations importées", "Activité", "Encaissements et règlements", "Pièces jointes multiples", "Ajouter une pièce jointe", "Pièces jointes", "Ouvrir", "Fichier partagé vers DeynBook", "Choisissez le compte pour le fichier partagé", "Tout exporter", "Période", "Ce carnet est vide", "Enregistrer", "Annuler", "Supprimer", "Archiver", "Désarchiver"
        )
        AppLanguage.ENGLISH -> V12Strings(
            "Ledgers", "Switch ledger", "New ledger", "Edit ledger", "Ledger name", "Ledger profile", "Logo", "Choose logo", "Remove logo", "Statement footer note", "The active ledger or a ledger containing accounts cannot be deleted", "Current ledger", "Smart overview", "Overdue", "Due in next 7 days", "Collected this month", "Paid to suppliers this month", "Top debtors", "Advanced search", "Minimum balance", "All balances", "Debtors only", "Settled only", "Sort by name", "Largest debt", "Most recent", "Excel", "Export Excel", "Import Excel", "Excel import template", "Import completed", "Excel import failed", "Imported accounts", "Imported transactions", "Activity", "Collections & settlements activity", "Multiple attachments", "Add attachment", "Attachments", "Open", "File shared to DeynBook", "Choose an account for the shared file", "Export all", "Date range", "This ledger is empty", "Save", "Cancel", "Delete", "Archive", "Unarchive"
        )
    }
}
