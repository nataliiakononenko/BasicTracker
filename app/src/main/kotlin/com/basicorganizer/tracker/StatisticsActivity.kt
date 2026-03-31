package com.basicorganizer.tracker

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import com.basicorganizer.tracker.data.TrackingItem
import com.basicorganizer.tracker.data.TrackerDatabase
import com.basicorganizer.tracker.view.LineGraphView
import java.text.SimpleDateFormat
import java.util.*

class StatisticsActivity : AppCompatActivity() {

    private lateinit var database: TrackerDatabase
    private lateinit var lineGraph: LineGraphView
    private lateinit var legendContainer: LinearLayout
    private lateinit var summaryContainer: LinearLayout
    private lateinit var emptyState: LinearLayout
    private lateinit var contentContainer: View

    private val itemColors = mutableMapOf<Long, Int>()
    private val itemVisibility = mutableMapOf<Long, Boolean>()
    private val itemData = mutableMapOf<Long, List<Int>>()
    private var items = listOf<TrackingItem>()

    companion object {
        val GRAPH_COLORS = listOf(
            Color.parseColor("#E91E63"),  // Pink
            Color.parseColor("#2196F3"),  // Blue
            Color.parseColor("#FF9800"),  // Orange
            Color.parseColor("#9C27B0"),  // Purple
            Color.parseColor("#009688"),  // Teal
            Color.parseColor("#F44336"),  // Red
            Color.parseColor("#4CAF50"),  // Green
            Color.parseColor("#3F51B5"),  // Indigo
            Color.parseColor("#FFEB3B"),  // Yellow
            Color.parseColor("#795548"),  // Brown
            Color.parseColor("#00BCD4"),  // Cyan
            Color.parseColor("#673AB7")   // Deep Purple
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_statistics)

        database = TrackerDatabase(this)
        initializeViews()
        loadStatistics()
    }

    private fun initializeViews() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }
        toolbar.navigationIcon?.setTint(ContextCompat.getColor(this, R.color.white))

        lineGraph = findViewById(R.id.line_graph)
        legendContainer = findViewById(R.id.legend_container)
        summaryContainer = findViewById(R.id.summary_container)
        emptyState = findViewById(R.id.empty_state)
        contentContainer = findViewById(R.id.content_container)
    }

    private fun loadStatistics() {
        items = database.getAllTrackingItems()
        
        // Check for empty state
        if (items.isEmpty()) {
            emptyState.visibility = View.VISIBLE
            contentContainer.visibility = View.GONE
            return
        }

        // Check if there's any data at all
        var hasAnyData = false
        for (item in items) {
            if (database.countOccurrencesForItem(item.id) > 0) {
                hasAnyData = true
                break
            }
        }

        if (!hasAnyData) {
            emptyState.visibility = View.VISIBLE
            contentContainer.visibility = View.GONE
            return
        }

        emptyState.visibility = View.GONE
        contentContainer.visibility = View.VISIBLE

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        
        // Generate list of last 30 days
        val dates = mutableListOf<String>()
        val tempCal = Calendar.getInstance()
        tempCal.add(Calendar.DAY_OF_MONTH, -29)
        for (i in 0 until 30) {
            dates.add(dateFormat.format(tempCal.time))
            tempCal.add(Calendar.DAY_OF_MONTH, 1)
        }

        // Assign unique colors and calculate data for each item
        legendContainer.removeAllViews()
        summaryContainer.removeAllViews()

        for ((index, item) in items.withIndex()) {
            val color = GRAPH_COLORS[index % GRAPH_COLORS.size]
            itemColors[item.id] = color
            itemVisibility[item.id] = true

            // Get daily counts (using rolling 7-day window for smoother lines)
            val dailyCounts = mutableListOf<Int>()
            val entries = database.getEntriesForItem(item.id).filter { it.occurred }
            val entryDates = entries.map { it.date }.toSet()

            for (i in dates.indices) {
                var count = 0
                val checkCal = Calendar.getInstance()
                checkCal.add(Calendar.DAY_OF_MONTH, -29 + i)
                
                for (j in 0 until 7) {
                    val checkDate = dateFormat.format(checkCal.time)
                    if (entryDates.contains(checkDate)) {
                        count++
                    }
                    checkCal.add(Calendar.DAY_OF_MONTH, -1)
                }
                dailyCounts.add(count)
            }
            itemData[item.id] = dailyCounts

            // Add legend item with checkbox
            val legendItem = LinearLayout(this)
            legendItem.orientation = LinearLayout.HORIZONTAL
            legendItem.gravity = android.view.Gravity.CENTER_VERTICAL
            legendItem.setPadding(0, 4, 0, 4)

            val checkBox = CheckBox(this)
            checkBox.isChecked = true
            checkBox.buttonTintList = android.content.res.ColorStateList.valueOf(color)
            checkBox.setOnCheckedChangeListener { _, isChecked ->
                itemVisibility[item.id] = isChecked
                updateGraph()
            }

            val colorDot = View(this)
            val dotSize = (12 * resources.displayMetrics.density).toInt()
            val dotParams = LinearLayout.LayoutParams(dotSize, dotSize)
            dotParams.setMargins(0, 0, (8 * resources.displayMetrics.density).toInt(), 0)
            colorDot.layoutParams = dotParams
            colorDot.setBackgroundColor(color)

            val nameText = TextView(this)
            nameText.text = item.name
            nameText.textSize = 14f
            nameText.setTextColor(ContextCompat.getColor(this, R.color.text_primary))

            legendItem.addView(checkBox)
            legendItem.addView(colorDot)
            legendItem.addView(nameText)
            legendContainer.addView(legendItem)

            // Add summary item
            val calendar = Calendar.getInstance()
            val endDate = calendar.time
            calendar.add(Calendar.DAY_OF_MONTH, -29)
            val startDate = calendar.time
            val startDateStr = dateFormat.format(startDate)
            val endDateStr = dateFormat.format(endDate)
            val count30Days = database.countOccurrencesForItemInRange(item.id, startDateStr, endDateStr)
            val totalCount = database.countOccurrencesForItem(item.id)

            val summaryItem = LinearLayout(this)
            summaryItem.orientation = LinearLayout.HORIZONTAL
            summaryItem.setPadding(0, 12, 0, 12)
            summaryItem.setBackgroundResource(R.drawable.tracking_item_border)
            val summaryParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            summaryParams.setMargins(0, 0, 0, (8 * resources.displayMetrics.density).toInt())
            summaryItem.layoutParams = summaryParams

            val summaryColorBar = View(this)
            val barWidth = (4 * resources.displayMetrics.density).toInt()
            val barParams = LinearLayout.LayoutParams(barWidth, LinearLayout.LayoutParams.MATCH_PARENT)
            summaryColorBar.layoutParams = barParams
            summaryColorBar.setBackgroundColor(color)

            val summaryContent = LinearLayout(this)
            summaryContent.orientation = LinearLayout.VERTICAL
            summaryContent.setPadding((12 * resources.displayMetrics.density).toInt(), 0, 0, 0)

            val summaryName = TextView(this)
            summaryName.text = item.name
            summaryName.textSize = 14f
            summaryName.setTextColor(ContextCompat.getColor(this, R.color.text_primary))

            val summaryStats = TextView(this)
            summaryStats.text = "Last 30 days: $count30Days  •  Total: $totalCount"
            summaryStats.textSize = 12f
            summaryStats.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))

            summaryContent.addView(summaryName)
            summaryContent.addView(summaryStats)
            summaryItem.addView(summaryColorBar)
            summaryItem.addView(summaryContent)
            summaryContainer.addView(summaryItem)
        }

        updateGraph()
    }

    private fun updateGraph() {
        val graphData = mutableListOf<Pair<List<Int>, Int>>()
        
        for (item in items) {
            if (itemVisibility[item.id] == true) {
                val data = itemData[item.id] ?: continue
                val color = itemColors[item.id] ?: continue
                graphData.add(Pair(data, color))
            }
        }
        
        lineGraph.setData(graphData)
    }
}
