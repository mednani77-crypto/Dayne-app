import re

file_path = "app/src/main/java/com/example/core/localization/AppStrings.kt"
with open(file_path, "r", encoding="utf-8") as f:
    content = f.read()

# Add to interface
interface_props = """    val quickActionSupplierPayment: String
    val txSubtitleCustomerDebt: String
    val txSubtitleCustomerPayment: String
    val txSubtitleSupplierDebt: String
    val txSubtitleSupplierPayment: String"""
content = re.sub(r'    val quickActionSupplierPayment: String', interface_props, content, count=1)

# Arabic
ar_props = """        quickActionSupplierPayment = "دفع لمورد",
        txSubtitleCustomerDebt = "+ لي عنده",
        txSubtitleCustomerPayment = "- استلمت منه",
        txSubtitleSupplierDebt = "+ عليّ له",
        txSubtitleSupplierPayment = "- دفعت له","""
content = re.sub(r'        quickActionSupplierPayment = "دفع لمورد",', ar_props, content, count=1)

# Somali
so_props = """        quickActionSupplierPayment = "Bixin Ganacsade",
        txSubtitleCustomerDebt = "+ Waan ku leeyahay",
        txSubtitleCustomerPayment = "- Waan ka qabtay",
        txSubtitleSupplierDebt = "+ Wuu igu leeyahay",
        txSubtitleSupplierPayment = "- Waan siiyay","""
content = re.sub(r'        quickActionSupplierPayment = "Bixin Ganacsade",', so_props, content, count=1)

# Amharic
am_props = """        quickActionSupplierPayment = "ለአቅራቢ የተከፈለ",
        txSubtitleCustomerDebt = "+ ያለበት",
        txSubtitleCustomerPayment = "- የተቀበልኩት",
        txSubtitleSupplierDebt = "+ ያለብኝ",
        txSubtitleSupplierPayment = "- የከፈልኩት","""
content = re.sub(r'        quickActionSupplierPayment = "ለአቅራቢ የተከፈለ",', am_props, content, count=1)

# French
fr_props = """        quickActionSupplierPayment = "Paiement versé",
        txSubtitleCustomerDebt = "+ Me doit",
        txSubtitleCustomerPayment = "- J'ai reçu",
        txSubtitleSupplierDebt = "+ Je lui dois",
        txSubtitleSupplierPayment = "- J'ai payé","""
content = re.sub(r'        quickActionSupplierPayment = "Paiement versé",', fr_props, content, count=1)

# English
en_props = """        quickActionSupplierPayment = "Supplier Payment",
        txSubtitleCustomerDebt = "+ Owes Me",
        txSubtitleCustomerPayment = "- Received",
        txSubtitleSupplierDebt = "+ I Owe",
        txSubtitleSupplierPayment = "- Paid","""
content = re.sub(r'        quickActionSupplierPayment = "Supplier Payment",', en_props, content, count=1)

with open(file_path, "w", encoding="utf-8") as f:
    f.write(content)

print("Updated AppStrings.kt")
