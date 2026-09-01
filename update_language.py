import re

file_path = "app/src/main/java/com/example/core/localization/Language.kt"
with open(file_path, "r", encoding="utf-8") as f:
    content = f.read()

props = """    val quickActionSupplierPayment: String,
    val txSubtitleCustomerDebt: String,
    val txSubtitleCustomerPayment: String,
    val txSubtitleSupplierDebt: String,
    val txSubtitleSupplierPayment: String,"""

content = re.sub(r'    val quickActionSupplierPayment: String,', props, content, count=1)

with open(file_path, "w", encoding="utf-8") as f:
    f.write(content)

print("Updated Language.kt")
