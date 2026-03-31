package com.basicorganizer.tracker.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.basicorganizer.tracker.R

class LineGraphView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val linePaints = mutableListOf<Paint>()
    private val paths = mutableListOf<Path>()
    private val dataLines = mutableListOf<List<Int>>()
    private val gridPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.divider)
        strokeWidth = 1f
        style = Paint.Style.STROKE
    }
    private val axisLabelPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.text_secondary)
        textSize = 10f * resources.displayMetrics.density
        isAntiAlias = true
    }

    private var maxValue = 1
    private var dataPointCount = 30
    private var xAxisLabels = listOf<String>()

    fun setData(lines: List<Pair<List<Int>, Int>>, xLabels: List<String> = emptyList()) {
        dataLines.clear()
        linePaints.clear()
        paths.clear()
        xAxisLabels = xLabels

        maxValue = 1
        for ((data, color) in lines) {
            dataLines.add(data)
            dataPointCount = data.size
            val paint = Paint().apply {
                this.color = color
                strokeWidth = 4f
                style = Paint.Style.STROKE
                isAntiAlias = true
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
            }
            linePaints.add(paint)
            paths.add(Path())
            
            val lineMax = data.maxOrNull() ?: 0
            if (lineMax > maxValue) maxValue = lineMax
        }
        
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        val leftPadding = 30f * resources.displayMetrics.density
        val rightPadding = 12f * resources.displayMetrics.density
        val topPadding = 16f * resources.displayMetrics.density
        val bottomPadding = 24f * resources.displayMetrics.density
        val graphWidth = w - leftPadding - rightPadding
        val graphHeight = h - topPadding - bottomPadding

        // Draw horizontal grid lines and Y-axis labels
        val ySteps = 4
        for (i in 0..ySteps) {
            val y = topPadding + (graphHeight / ySteps) * i
            canvas.drawLine(leftPadding, y, w - rightPadding, y, gridPaint)
            
            // Y-axis label (value)
            val value = maxValue - (maxValue * i / ySteps)
            val label = value.toString()
            val textWidth = axisLabelPaint.measureText(label)
            canvas.drawText(label, leftPadding - textWidth - 4f * resources.displayMetrics.density, y + 4f * resources.displayMetrics.density, axisLabelPaint)
        }

        // Draw X-axis labels
        if (xAxisLabels.isNotEmpty()) {
            val stepX = graphWidth / (xAxisLabels.size - 1).coerceAtLeast(1)
            for (i in xAxisLabels.indices) {
                if (i == 0 || i == xAxisLabels.size - 1 || i == xAxisLabels.size / 2) {
                    val x = leftPadding + stepX * i
                    val label = xAxisLabels[i]
                    val textWidth = axisLabelPaint.measureText(label)
                    val xPos = when (i) {
                        0 -> x
                        xAxisLabels.size - 1 -> x - textWidth
                        else -> x - textWidth / 2
                    }
                    canvas.drawText(label, xPos, h - 4f * resources.displayMetrics.density, axisLabelPaint)
                }
            }
        } else if (dataPointCount > 0) {
            // Default: show day numbers
            val labels = listOf("1", "${dataPointCount / 2}", "$dataPointCount")
            val positions = listOf(0, dataPointCount / 2, dataPointCount - 1)
            val stepX = graphWidth / (dataPointCount - 1).coerceAtLeast(1)
            for (i in labels.indices) {
                val x = leftPadding + stepX * positions[i]
                val label = labels[i]
                val textWidth = axisLabelPaint.measureText(label)
                val xPos = when (i) {
                    0 -> x
                    labels.size - 1 -> x - textWidth
                    else -> x - textWidth / 2
                }
                canvas.drawText(label, xPos, h - 4f * resources.displayMetrics.density, axisLabelPaint)
            }
        }

        // Draw data lines
        for (i in dataLines.indices) {
            val data = dataLines[i]
            val path = paths[i]
            path.reset()

            if (data.isEmpty()) continue

            val stepX = if (data.size > 1) graphWidth / (data.size - 1) else graphWidth
            
            for (j in data.indices) {
                val x = leftPadding + stepX * j
                val normalizedY = if (maxValue > 0) data[j].toFloat() / maxValue else 0f
                val y = topPadding + graphHeight - (normalizedY * graphHeight)

                if (j == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
            }

            canvas.drawPath(path, linePaints[i])
        }
    }
}
