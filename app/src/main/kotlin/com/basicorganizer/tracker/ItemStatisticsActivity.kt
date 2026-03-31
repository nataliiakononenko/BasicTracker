package com.basicorganizer.tracker

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import com.basicorganizer.tracker.data.Sentiment
import com.basicorganizer.tracker.data.TrackerDatabase
import java.text.SimpleDateFormat
import java.util.*

class ItemStatisticsActivity : AppCompatActivity() {

    private lateinit var database: TrackerDatabase
    private lateinit var statsGraphContainer: LinearLayout
    private lateinit var tvYAxisMax: TextView
    private lateinit var tvStatsThisMonth: TextView
    private lateinit var tvStatsTotal: TextView
    private lateinit var tvStatsAvgWeekMonth: TextView
    private lateinit var tvStatsAvgWeekOverall: TextView
    private lateinit var tvStatsStreak: TextView
    private lateinit var tvStatsLongestStreak: TextView

    private var itemId: Long = -1
    private var itemName: String = ""

    companion object {
        const val EXTRA_ITEM_ID = "item_id"
        const val EXTRA_ITEM_NAME = "item_name"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_item_statistics)

        itemId = intent.getLongExtra(EXTRA_ITEM_ID, -1)
        itemName = intent.getStringExtra(EXTRA_ITEM_NAME) ?: ""

        if (itemId == -1L) {
            finish()
            return
        }

        database = TrackerDatabase(this)
        initializeViews()
        loadStatistics()
    }

    private fun initializeViews() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        toolbar.title = "Statistics - $itemName"
        toolbar.setNavigationOnClickListener { finish() }
        toolbar.navigationIcon?.setTint(ContextCompat.getColor(this, R.color.white))

        statsGraphContainer = findViewById(R.id.stats_graph_container)
        tvYAxisMax = findViewById(R.id.tv_y_axis_max)
        tvStatsThisMonth = findViewById(R.id.tv_stats_this_month)
        tvStatsTotal = findViewById(R.id.tv_stats_total)
        tvStatsAvgWeekMonth = findViewById(R.id.tv_stats_avg_week_month)
        tvStatsAvgWeekOverall = findViewById(R.id.tv_stats_avg_week_overall)
        tvStatsStreak = findViewById(R.id.tv_stats_streak)
        tvStatsLongestStreak = findViewById(R.id.tv_stats_longest_streak)
    }

    private fun loadStatistics() {
        val item = database.getTrackingItem(itemId) ?: return
        val entries = database.getEntriesForItem(itemId).filter { it.occurred }

        // This month stats
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        calendar.set(year, month, 1)
        val monthStart = getDateString(calendar)
        calendar.set(year, month, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        val monthEnd = getDateString(calendar)

        val thisMonthCount = database.countOccurrencesForItemInRange(itemId, monthStart, monthEnd)
        val totalCount = database.countOccurrencesForItem(itemId)

        tvStatsThisMonth.text = "$thisMonthCount times"
        tvStatsTotal.text = "$totalCount times"

        // Calculate weeks in current month
        calendar.set(year, month, 1)
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val weeksInMonth = daysInMonth / 7.0
        val avgWeekMonth = if (weeksInMonth > 0) thisMonthCount / weeksInMonth else 0.0
        tvStatsAvgWeekMonth.text = String.format("%.1f", avgWeekMonth)

        // Calculate overall weeks since first entry
        if (entries.isNotEmpty()) {
            val firstEntryDate = entries.minByOrNull { it.date }?.date
            if (firstEntryDate != null) {
                try {
                    val parseFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val firstDate = parseFormat.parse(firstEntryDate)
                    val today = Date()
                    if (firstDate != null) {
                        val daysSinceStart = ((today.time - firstDate.time) / (1000 * 60 * 60 * 24)).toInt() + 1
                        val weeksSinceStart = daysSinceStart / 7.0
                        val avgWeekOverall = if (weeksSinceStart > 0) totalCount / weeksSinceStart else 0.0
                        tvStatsAvgWeekOverall.text = String.format("%.1f", avgWeekOverall)
                    }
                } catch (e: Exception) {
                    tvStatsAvgWeekOverall.text = "0.0"
                }
            }
        } else {
            tvStatsAvgWeekOverall.text = "0.0"
        }

        // Calculate streaks
        val sortedDates = entries.map { it.date }.sorted()
        var currentStreak = 0
        var longestStreak = 0
        var tempStreak = 0

        val todayStr = getDateString(Calendar.getInstance())
        val yesterdayCalendar = Calendar.getInstance()
        yesterdayCalendar.add(Calendar.DAY_OF_MONTH, -1)
        val yesterdayStr = getDateString(yesterdayCalendar)

        // Check if today or yesterday is marked for current streak
        val hasToday = sortedDates.contains(todayStr)
        val hasYesterday = sortedDates.contains(yesterdayStr)

        if (hasToday || hasYesterday) {
            var checkDate = Calendar.getInstance()
            if (!hasToday) checkDate.add(Calendar.DAY_OF_MONTH, -1)

            while (sortedDates.contains(getDateString(checkDate))) {
                currentStreak++
                checkDate.add(Calendar.DAY_OF_MONTH, -1)
            }
        }

        // Calculate longest streak
        if (sortedDates.isNotEmpty()) {
            val parseFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            var prevDate: Calendar? = null

            for (dateStr in sortedDates) {
                try {
                    val date = parseFormat.parse(dateStr)
                    if (date != null) {
                        val cal = Calendar.getInstance()
                        cal.time = date

                        if (prevDate == null) {
                            tempStreak = 1
                        } else {
                            val diff = ((cal.timeInMillis - prevDate.timeInMillis) / (1000 * 60 * 60 * 24)).toInt()
                            if (diff == 1) {
                                tempStreak++
                            } else {
                                longestStreak = maxOf(longestStreak, tempStreak)
                                tempStreak = 1
                            }
                        }
                        prevDate = cal
                    }
                } catch (e: Exception) {}
            }
            longestStreak = maxOf(longestStreak, tempStreak)
        }

        tvStatsStreak.text = "$currentStreak days"
        tvStatsLongestStreak.text = "$longestStreak days"

        // Draw graph
        drawMonthlyGraph(item.sentiment)
    }

    private fun drawMonthlyGraph(sentiment: Sentiment) {
        statsGraphContainer.removeAllViews()

        val monthCounts = mutableListOf<Int>()
        val monthLabels = mutableListOf<String>()
        val labelFormat = SimpleDateFormat("MMM", Locale.getDefault())

        // Get last 6 months of data
        for (i in 5 downTo 0) {
            val cal = Calendar.getInstance()
            cal.add(Calendar.MONTH, -i)
            val year = cal.get(Calendar.YEAR)
            val month = cal.get(Calendar.MONTH)

            cal.set(year, month, 1)
            val monthStart = getDateString(cal)
            cal.set(year, month, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
            val monthEnd = getDateString(cal)

            val count = database.countOccurrencesForItemInRange(itemId, monthStart, monthEnd)
            monthCounts.add(count)
            monthLabels.add(labelFormat.format(cal.time))
        }

        val maxCount = monthCounts.maxOrNull() ?: 1
        tvYAxisMax.text = maxCount.toString()

        val barColor = when (sentiment) {
            Sentiment.POSITIVE -> R.color.sentiment_positive
            Sentiment.NEGATIVE -> R.color.sentiment_negative
            Sentiment.NEUTRAL -> R.color.sentiment_neutral
        }

        for (i in monthCounts.indices) {
            val barContainer = LinearLayout(this)
            barContainer.orientation = LinearLayout.VERTICAL
            barContainer.gravity = android.view.Gravity.BOTTOM or android.view.Gravity.CENTER_HORIZONTAL
            val containerParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
            containerParams.setMargins(4, 0, 4, 0)
            barContainer.layoutParams = containerParams

            // Bar
            val bar = View(this)
            val barHeight = if (maxCount > 0) ((monthCounts[i].toFloat() / maxCount) * 60 * resources.displayMetrics.density).toInt() else 0
            val barParams = LinearLayout.LayoutParams((16 * resources.displayMetrics.density).toInt(), maxOf(barHeight, (4 * resources.displayMetrics.density).toInt()))
            bar.layoutParams = barParams
            bar.setBackgroundColor(ContextCompat.getColor(this, barColor))

            // Label
            val label = TextView(this)
            label.text = monthLabels[i]
            label.textSize = 9f
            label.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
            label.gravity = android.view.Gravity.CENTER

            barContainer.addView(bar)
            barContainer.addView(label)
            statsGraphContainer.addView(barContainer)
        }
    }

    private fun getDateString(calendar: Calendar): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
    }
}
