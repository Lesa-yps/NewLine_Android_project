package com.example.view.graph

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import java.time.Duration
import kotlin.math.max

class CycleTimeGraphView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var cycleTimes: List<Duration> = emptyList()
    private var percentiles: Map<Double, Duration> = emptyMap()

    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLUE
        strokeWidth = 8f
        style = Paint.Style.STROKE
    }

    private val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.GRAY
        strokeWidth = 2f
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.GRAY
        textSize = 36f
        typeface = Typeface.MONOSPACE
    }

    private val percentilePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.RED
        strokeWidth = 2f
        pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
    }

    fun setData(cycleTimes: List<Duration>, percentiles: Map<Double, Duration>) {
        this.cycleTimes = cycleTimes.sorted()
        this.percentiles = percentiles
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (cycleTimes.isEmpty()) {
            drawNoDataMessage(canvas)
            return
        }

        val paddingLeft = 100f
        val paddingRight = 140f
        val paddingBottom = 340f
        val paddingTop = 40f
        val graphWidth = width - paddingLeft - paddingRight

        val allDurations = cycleTimes + percentiles.values
        val maxDurationMinutes = max(1L, allDurations.maxOf { it.toMinutes() })
        val xScale = graphWidth / maxDurationMinutes.toFloat()

        drawXAxis(canvas, paddingLeft, paddingRight, paddingBottom, 0, maxDurationMinutes)
        drawPercentiles(canvas, paddingLeft, paddingTop, paddingBottom, xScale)
        drawCycleTimeDots(canvas, paddingLeft, paddingBottom, xScale)
    }

    private fun drawNoDataMessage(canvas: Canvas) {
        canvas.drawText("Нет данных", 50f, height / 2f, textPaint)
    }

    private fun drawXAxis(
        canvas: Canvas,
        paddingLeft: Float,
        paddingRight: Float,
        paddingBottom: Float,
        startMinutes: Long,
        endMinutes: Long
    ) {
        val y = height - paddingBottom

        // Линия оси
        canvas.drawLine(
            paddingLeft,
            y,
            width - paddingRight,
            y,
            axisPaint
        )

        textPaint.textSize = 30f
        textPaint.color = Color.GRAY

        // Подпись начала (0 мин)
        val startLabel = "$startMinutes м"
        canvas.drawText(startLabel, paddingLeft, y + 40f, textPaint)

        // Подпись конца (макс. время)
        val endLabel = "$endMinutes м"
        val labelWidth = textPaint.measureText(endLabel)
        canvas.drawText(endLabel, width - paddingRight - labelWidth, y + 40f, textPaint)

        // Название оси
        textPaint.textSize = 28f
        canvas.drawText("линия времени", paddingLeft + (width - paddingLeft - paddingRight) / 2 - 60f, y + 70f, textPaint)
    }

    private fun drawPercentiles(
        canvas: Canvas,
        paddingLeft: Float,
        paddingTop: Float,
        paddingBottom: Float,
        xScale: Float
    ) {
        val sortedPercentiles = percentiles.toSortedMap(reverseOrder())

        for ((p, value) in sortedPercentiles) {
            val minutes = value.toMinutes()
            val x = paddingLeft + minutes * xScale
            if (x > width - paddingRight) continue

            val label = String.format("%.0f%% (%dм)", p * 100, minutes)
            val percentileLineTop = paddingTop + (1.0 - p) * (height - paddingTop - paddingBottom)

            canvas.drawLine(x, percentileLineTop.toFloat(), x, height - paddingBottom, percentilePaint)

            val labelWidth = textPaint.measureText(label)
            canvas.drawText(
                label,
                x - labelWidth / 2,
                percentileLineTop.toFloat() - 10f,
                textPaint
            )
        }
    }

    private fun drawCycleTimeDots(
        canvas: Canvas,
        paddingLeft: Float,
        paddingBottom: Float,
        xScale: Float
    ) {
        cycleTimes.forEach { duration ->
            val minutes = duration.toMinutes()
            val x = paddingLeft + minutes * xScale
            if (x > width - paddingRight) return@forEach

            val dotY = height - paddingBottom - 20f
            canvas.drawCircle(x, dotY, 6f, dotPaint)
        }
    }
}
