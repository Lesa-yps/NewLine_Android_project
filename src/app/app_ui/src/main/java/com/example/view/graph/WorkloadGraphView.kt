package com.example.view.graph

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import androidx.core.graphics.toColorInt

class WorkloadGraphView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private var data: List<Pair<String, Float>> = emptyList()

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#6200EE".toColorInt() // фиолетовый
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.GRAY
        textSize = 40f
        typeface = Typeface.MONOSPACE
    }
    private val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.GRAY
        strokeWidth = 2f
    }

    fun setData(data: List<Pair<String, Float>>) {
        this.data = data
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (data.isEmpty()) {
            canvas.drawText("Нет данных для отображения", 20f, height / 2f, textPaint)
            return
        }

        val paddingLeft = 60f
        val paddingBottom = 80f
        val paddingTop = 40f
        val graphHeight = height - paddingBottom - paddingTop
        val graphWidth = width - paddingLeft - 40f

        // Нарисовать ось Y
        canvas.drawLine(paddingLeft, paddingTop, paddingLeft, height - paddingBottom, axisPaint)

        // Максимальная нагрузка для нормализации
        val maxLoad = data.maxOf { it.second }.coerceAtLeast(0.1f)

        // Ширина одного бара и отступы
        val barWidth = graphWidth / (data.size * 2f)
        val space = barWidth

        data.forEachIndexed { i, (day, load) ->
            val x = paddingLeft + space + i * (barWidth + space)
            val barHeight = (load / maxLoad) * graphHeight
            val yTop = height - paddingBottom - barHeight

            // Рисование столбика
            canvas.drawRect(x, yTop, x + barWidth, height - paddingBottom, barPaint)

            // Рисование подписи дня под баром (сдвиг, чтобы центрировать)
            val dayText = day.take(3) // короткое имя дня (пн, вт, ср...)
            val textWidth = textPaint.measureText(dayText)
            canvas.drawText(dayText, x + barWidth / 2 - textWidth / 2, height - paddingBottom + 40f, textPaint)

            // Рисования значения над столбиком
            val loadText = String.format("%.2f", load)
            val loadTextWidth = textPaint.measureText(loadText)
            canvas.drawText(loadText, x + barWidth / 2 - loadTextWidth / 2, yTop - 10f, textPaint)
        }
    }
}