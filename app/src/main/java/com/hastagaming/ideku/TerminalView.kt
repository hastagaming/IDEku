// Lokasi: app/src/main/java/com/hastagaming/ideku/TerminalView.kt
package com.hastagaming.ideku

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View

class TerminalView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val paint = Paint().apply {
        color = Color.GREEN
        textSize = 34f
        typeface = Typeface.MONOSPACE
        isAntiAlias = true
    }

    private val outputBuffer = mutableListOf<String>()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.BLACK)

        var yPos = 50f
        // Ambil 30 baris terakhir agar tidak membebani memori
        val displayLines = if (outputBuffer.size > 30) outputBuffer.takeLast(30) else outputBuffer
        for (line in displayLines) {
            canvas.drawText(line, 20f, yPos, paint)
            yPos += paint.fontSpacing
        }
    }

    // Fungsi 'write' agar sinkron dengan kode MainActivity kamu
    fun write(text: String) {
        // Bersihkan karakter \r\n jika ada dan pisahkan per baris
        val cleanText = text.replace("\r", "")
        cleanText.split("\n").forEach {
            if (it.isNotEmpty()) outputBuffer.add(it)
        }
        invalidate() // Gambar ulang
    }
}
