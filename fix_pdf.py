import re

file_path = "app/src/main/java/com/example/services/PdfStatementService.kt"
with open(file_path, "r", encoding="utf-8") as f:
    content = f.read()

# Add Bitmap imports if missing
if "import android.graphics.Bitmap" not in content:
    content = content.replace("import android.graphics.Canvas", "import android.graphics.Bitmap\nimport android.graphics.Canvas\nimport android.graphics.RectF")

# Update initialization
init_old = """        val document = PdfDocument()
        val pageWidth = 595 // A4 standard width in points
        val pageHeight = 842 // A4 standard height in points
        val margin = 36f

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var page = document.startPage(pageInfo)
        var canvas = page.canvas"""

init_new = """        val document = PdfDocument()
        val pageWidth = 595 // A4 standard width in points
        val pageHeight = 842 // A4 standard height in points
        val margin = 36f

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var page = document.startPage(pageInfo)
        
        val scale = 3f
        var bitmap = Bitmap.createBitmap((pageWidth * scale).toInt(), (pageHeight * scale).toInt(), Bitmap.Config.ARGB_8888)
        var canvas = Canvas(bitmap)
        canvas.scale(scale, scale)
        canvas.drawColor(Color.WHITE)"""

content = content.replace(init_old, init_new)

# Update page break logic
break_old = """                drawFooter(canvas, pageNumber)
                document.finishPage(page)

                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                page = document.startPage(pageInfo)
                canvas = page.canvas"""

break_new = """                drawFooter(canvas, pageNumber)
                page.canvas.drawBitmap(bitmap, null, RectF(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat()), null)
                document.finishPage(page)

                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                page = document.startPage(pageInfo)
                bitmap = Bitmap.createBitmap((pageWidth * scale).toInt(), (pageHeight * scale).toInt(), Bitmap.Config.ARGB_8888)
                canvas = Canvas(bitmap)
                canvas.scale(scale, scale)
                canvas.drawColor(Color.WHITE)"""

content = content.replace(break_old, break_new)

# Update final finishPage
final_finish_old = """        drawFooter(canvas, pageNumber)
        document.finishPage(page)"""

final_finish_new = """        drawFooter(canvas, pageNumber)
        page.canvas.drawBitmap(bitmap, null, RectF(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat()), null)
        document.finishPage(page)"""

content = content.replace(final_finish_old, final_finish_new)

with open(file_path, "w", encoding="utf-8") as f:
    f.write(content)

print("Fixed PDF generation")
