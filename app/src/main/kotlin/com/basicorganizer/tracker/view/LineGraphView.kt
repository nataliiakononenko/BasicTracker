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
    private val axisPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.text_secondary)
        textSize = 24f
        isAntiAlias = true
    }

    private var maxValue = 1

    fun setData(lines: List<Pair<List<Int>, Int>>) {
        dataLines.clear()
        linePaints.clear()
        paths.clear()

        maxValue = 1
        for ((data, color) in lines) {
            dataLines.add(data)
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
        val padding = 40f
        val graphWidth = w - padding * 2
        val graphHeight = h - padding * 2

        // Draw horizontal grid lines
        for (i in 0..4) {
            val y = padding + (graphHeight / 4) * i
            canvas.drawLine(padding, y, w - padding, y, gridPaint)
        }

        // Draw data lines
        for (i in dataLines.indices) {
            val data = dataLines[i]
            val path = paths[i]
            path.reset()

            if (data.isEmpty()) continue

            val stepX = if (data.size > 1) graphWidth / (data.size - 1) else graphWidth
            
            for (j in data.indices) {
                val x = padding + stepX * j
                val normalizedY = if (maxValue > 0) data[j].toFloat() / maxValue else 0f
                val y = padding + graphHeight - (normalizedY * graphHeight)

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
