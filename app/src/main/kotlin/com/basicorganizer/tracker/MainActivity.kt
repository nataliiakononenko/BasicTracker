package com.basicorganizer.tracker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.basicorganizer.tracker.adapter.DrawerItemAdapter
import com.basicorganizer.tracker.adapter.TrackingItemAdapter
import com.basicorganizer.tracker.data.Sentiment
import com.basicorganizer.tracker.data.TrackerDatabase
import com.basicorganizer.tracker.data.TrackingItem
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity(), TrackingItemAdapter.OnItemInteractionListener, DrawerItemAdapter.OnItemClickListener {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var toolbar: androidx.appcompat.widget.Toolbar
    private lateinit var rvTrackingItems: RecyclerView
    private lateinit var emptyState: View
    private lateinit var fabAddItem: FloatingActionButton
    private lateinit var tvDayName: TextView
    private lateinit var tvDate: TextView
    private lateinit var btnDayView: MaterialButton
    private lateinit var btnWeekView: MaterialButton
    private lateinit var btnMonthView: MaterialButton
    private lateinit var weekHeader: LinearLayout
    private lateinit var monthViewContainer: View

    private lateinit var database: TrackerDatabase
    private lateinit var itemAdapter: TrackingItemAdapter
    private lateinit var drawerAdapter: DrawerItemAdapter

    private var currentDate: Calendar = Calendar.getInstance()
    private var currentViewMode: ViewMode = ViewMode.DAY
    private var selectedItemId: Long? = null

    private val markedItems = mutableMapOf<Long, Boolean>()
    private val occurrenceCounts = mutableMapOf<Long, Int>()

    enum class ViewMode { DAY, WEEK, MONTH }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        database = TrackerDatabase(this)
        initializeViews()
        setupToolbar()
        setupNavigationDrawer()
        setupViewSwitcher()
        loadData()
    }

    private fun initializeViews() {
        drawerLayout = findViewById(R.id.drawer_layout)
        toolbar = findViewById(R.id.toolbar)
        rvTrackingItems = findViewById(R.id.rv_tracking_items)
        emptyState = findViewById(R.id.empty_state)
        fabAddItem = findViewById(R.id.fab_add_item)
        tvDayName = findViewById(R.id.tv_day_name)
        tvDate = findViewById(R.id.tv_date)
        btnDayView = findViewById(R.id.btn_day_view)
        btnWeekView = findViewById(R.id.btn_week_view)
        btnMonthView = findViewById(R.id.btn_month_view)
        weekHeader = findViewById(R.id.week_header)
        monthViewContainer = findViewById(R.id.month_view_container)

        rvTrackingItems.layoutManager = LinearLayoutManager(this)

        val navView = findViewById<NavigationView>(R.id.nav_view)
        val drawerRecyclerView = navView.findViewById<RecyclerView>(R.id.rv_drawer_items)
        drawerRecyclerView.layoutManager = LinearLayoutManager(this)

        drawerAdapter = DrawerItemAdapter(this, emptyList(), occurrenceCounts, this)
        drawerRecyclerView.adapter = drawerAdapter

        navView.findViewById<View>(R.id.drawer_statistics).setOnClickListener {
            showStatisticsDialog()
            drawerLayout.closeDrawer(GravityCompat.START)
        }

        fabAddItem.setOnClickListener { showAddItemDialog() }
        findViewById<MaterialButton>(R.id.btn_add_item_empty).setOnClickListener { showAddItemDialog() }
    }

    private fun setupToolbar() {
        toolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.white))
        setSupportActionBar(toolbar)
        supportActionBar?.title = getString(R.string.app_name)
    }

    private fun setupNavigationDrawer() {
        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        toggle.drawerArrowDrawable.color = ContextCompat.getColor(this, R.color.white)
    }

    private fun setupViewSwitcher() {
        btnDayView.setOnClickListener { switchToView(ViewMode.DAY) }
        btnWeekView.setOnClickListener { switchToView(ViewMode.WEEK) }
        btnMonthView.setOnClickListener { switchToView(ViewMode.MONTH) }
        updateViewButtons()
    }

    private fun switchToView(mode: ViewMode) {
        currentViewMode = mode
        updateViewButtons()
        loadData()
    }

    private fun updateViewButtons() {
        val selectedColor = ContextCompat.getColor(this, R.color.colorPrimary)
        val defaultColor = ContextCompat.getColor(this, R.color.text_secondary)

        btnDayView.setTextColor(if (currentViewMode == ViewMode.DAY) selectedColor else defaultColor)
        btnWeekView.setTextColor(if (currentViewMode == ViewMode.WEEK) selectedColor else defaultColor)
        btnMonthView.setTextColor(if (currentViewMode == ViewMode.MONTH) selectedColor else defaultColor)

        btnDayView.strokeColor = android.content.res.ColorStateList.valueOf(
            if (currentViewMode == ViewMode.DAY) selectedColor else defaultColor
        )
        btnWeekView.strokeColor = android.content.res.ColorStateList.valueOf(
            if (currentViewMode == ViewMode.WEEK) selectedColor else defaultColor
        )
        btnMonthView.strokeColor = android.content.res.ColorStateList.valueOf(
            if (currentViewMode == ViewMode.MONTH) selectedColor else defaultColor
        )
    }

    private fun loadData() {
        updateDateDisplay()
        loadTrackingItems()
        loadDrawerItems()
    }

    private fun updateDateDisplay() {
        val dayFormat = SimpleDateFormat("EEEE", Locale.getDefault())
        val dateFormat = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())

        tvDayName.text = dayFormat.format(currentDate.time)
        tvDate.text = dateFormat.format(currentDate.time)

        when (currentViewMode) {
            ViewMode.DAY -> {
                weekHeader.visibility = View.GONE
                monthViewContainer.visibility = View.GONE
                rvTrackingItems.visibility = View.VISIBLE
            }
            ViewMode.WEEK -> {
                setupWeekHeader()
                weekHeader.visibility = View.VISIBLE
                monthViewContainer.visibility = View.GONE
                rvTrackingItems.visibility = View.VISIBLE
            }
            ViewMode.MONTH -> {
                weekHeader.visibility = View.GONE
                monthViewContainer.visibility = View.VISIBLE
                rvTrackingItems.visibility = View.GONE
                setupMonthView()
            }
        }
    }

    private fun setupWeekHeader() {
        weekHeader.removeAllViews()
        val calendar = currentDate.clone() as Calendar
        calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)

        val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val dateFormat = SimpleDateFormat("d", Locale.getDefault())
        val currentDateStr = getDateString(currentDate)

        for (i in 0..6) {
            val dayView = LayoutInflater.from(this).inflate(R.layout.item_week_day, weekHeader, false)
            val tvDayShort = dayView.findViewById<TextView>(R.id.tv_day_short)
            val tvDayNum = dayView.findViewById<TextView>(R.id.tv_day_num)

            tvDayShort.text = dayFormat.format(calendar.time)
            tvDayNum.text = dateFormat.format(calendar.time)

            val dateStr = getDateString(calendar)
            if (dateStr == currentDateStr) {
                dayView.setBackgroundResource(R.drawable.selected_day_background)
            }

            dayView.setOnClickListener {
                currentDate.time = calendar.time
                loadData()
            }

            val params = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            dayView.layoutParams = params
            weekHeader.addView(dayView)

            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
    }

    private fun setupMonthView() {
        val monthGrid = findViewById<android.widget.GridLayout>(R.id.month_grid)
        monthGrid.removeAllViews()

        val calendar = currentDate.clone() as Calendar
        calendar.set(Calendar.DAY_OF_MONTH, 1)

        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1

        val items = database.getAllTrackingItems()
        val currentDateStr = getDateString(currentDate)

        for (i in 0 until firstDayOfWeek) {
            val emptyView = View(this)
            val params = android.widget.GridLayout.LayoutParams()
            params.width = 0
            params.height = 100
            params.columnSpec = android.widget.GridLayout.spec(android.widget.GridLayout.UNDEFINED, 1f)
            emptyView.layoutParams = params
            monthGrid.addView(emptyView)
        }

        for (day in 1..daysInMonth) {
            calendar.set(Calendar.DAY_OF_MONTH, day)
            val dateStr = getDateString(calendar)

            val dayView = LayoutInflater.from(this).inflate(R.layout.item_month_day, monthGrid, false)
            val tvDay = dayView.findViewById<TextView>(R.id.tv_day)
            val dotsContainer = dayView.findViewById<LinearLayout>(R.id.dots_container)

            tvDay.text = day.toString()

            if (dateStr == currentDateStr) {
                dayView.setBackgroundResource(R.drawable.selected_day_background)
            }

            val entries = database.getEntriesForDate(dateStr)
            for (entry in entries.take(4)) {
                if (entry.occurred) {
                    val item = items.find { it.id == entry.trackingItemId }
                    if (item != null) {
                        val dot = View(this)
                        val dotParams = LinearLayout.LayoutParams(8, 8)
                        dotParams.setMargins(2, 0, 2, 0)
                        dot.layoutParams = dotParams
                        dot.setBackgroundResource(R.drawable.circle_indicator)
                        val color = when (item.sentiment) {
                            Sentiment.POSITIVE -> R.color.sentiment_positive
                            Sentiment.NEGATIVE -> R.color.sentiment_negative
                            Sentiment.NEUTRAL -> R.color.sentiment_neutral
                        }
                        dot.background.setTint(ContextCompat.getColor(this, color))
                        dotsContainer.addView(dot)
                    }
                }
            }

            dayView.setOnClickListener {
                currentDate.set(Calendar.DAY_OF_MONTH, day)
                switchToView(ViewMode.DAY)
            }

            dayView.setOnLongClickListener {
                currentDate.set(Calendar.DAY_OF_MONTH, day)
                showAddItemDialog()
                true
            }

            val params = android.widget.GridLayout.LayoutParams()
            params.width = 0
            params.height = android.widget.GridLayout.LayoutParams.WRAP_CONTENT
            params.columnSpec = android.widget.GridLayout.spec(android.widget.GridLayout.UNDEFINED, 1f)
            dayView.layoutParams = params
            monthGrid.addView(dayView)
        }
    }

    private fun loadTrackingItems() {
        val items = if (selectedItemId != null) {
            listOfNotNull(database.getTrackingItem(selectedItemId!!))
        } else {
            database.getAllTrackingItems()
        }

        markedItems.clear()
        val dateStr = getDateString(currentDate)
        for (item in items) {
            val entry = database.getEntry(item.id, dateStr)
            if (entry != null) {
                markedItems[item.id] = entry.occurred
            }
        }

        if (items.isEmpty()) {
            emptyState.visibility = View.VISIBLE
            rvTrackingItems.visibility = View.GONE
            fabAddItem.visibility = View.GONE
        } else {
            emptyState.visibility = View.GONE
            rvTrackingItems.visibility = if (currentViewMode != ViewMode.MONTH) View.VISIBLE else View.GONE
            fabAddItem.visibility = View.VISIBLE

            if (!::itemAdapter.isInitialized) {
                itemAdapter = TrackingItemAdapter(this, items, markedItems, this)
                rvTrackingItems.adapter = itemAdapter
            } else {
                itemAdapter.updateItems(items)
                itemAdapter.updateMarkedItems(markedItems)
            }
        }
    }

    private fun loadDrawerItems() {
        val items = database.getAllTrackingItems()
        occurrenceCounts.clear()
        for (item in items) {
            occurrenceCounts[item.id] = database.countOccurrencesForItem(item.id)
        }
        drawerAdapter.updateItems(items, occurrenceCounts)
    }

    private fun showAddItemDialog(editItem: TrackingItem? = null) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_item, null)
        val etName = dialogView.findViewById<EditText>(R.id.et_item_name)
        val rgSentiment = dialogView.findViewById<RadioGroup>(R.id.rg_sentiment)

        if (editItem != null) {
            etName.setText(editItem.name)
            when (editItem.sentiment) {
                Sentiment.POSITIVE -> rgSentiment.check(R.id.rb_positive)
                Sentiment.NEGATIVE -> rgSentiment.check(R.id.rb_negative)
                Sentiment.NEUTRAL -> rgSentiment.check(R.id.rb_neutral)
            }
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle(if (editItem == null) R.string.add_item else R.string.edit)
            .setView(dialogView)
            .setPositiveButton(R.string.save) { _, _ ->
                val name = etName.text.toString().trim()
                if (name.isNotEmpty()) {
                    val sentiment = when (rgSentiment.checkedRadioButtonId) {
                        R.id.rb_positive -> Sentiment.POSITIVE
                        R.id.rb_negative -> Sentiment.NEGATIVE
                        else -> Sentiment.NEUTRAL
                    }

                    if (editItem == null) {
                        val item = TrackingItem(
                            name = name,
                            sentiment = sentiment,
                            position = database.getAllTrackingItems().size,
                            createdAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                        )
                        database.addTrackingItem(item)
                        Toast.makeText(this, "Item added", Toast.LENGTH_SHORT).show()
                    } else {
                        editItem.name = name
                        editItem.sentiment = sentiment
                        database.updateTrackingItem(editItem)
                        Toast.makeText(this, "Item updated", Toast.LENGTH_SHORT).show()
                    }
                    loadData()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .create()

        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        dialog.setOnShowListener {
            etName.requestFocus()
            etName.postDelayed({
                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(etName, InputMethodManager.SHOW_FORCED)
            }, 100)
        }
        dialog.show()
    }

    private fun showStatisticsDialog() {
        val items = database.getAllTrackingItems()
        if (items.isEmpty()) {
            Toast.makeText(this, "No items to show statistics for", Toast.LENGTH_SHORT).show()
            return
        }

        val message = StringBuilder()
        val calendar = Calendar.getInstance()
        val endDate = getDateString(calendar)
        calendar.add(Calendar.DAY_OF_MONTH, -30)
        val startDate = getDateString(calendar)

        for (item in items) {
            val totalCount = database.countOccurrencesForItem(item.id)
            val monthCount = database.countOccurrencesForItemInRange(item.id, startDate, endDate)
            message.append("${item.name}\n")
            message.append("  Total: $totalCount occurrences\n")
            message.append("  Last 30 days: $monthCount occurrences\n\n")
        }

        AlertDialog.Builder(this)
            .setTitle(R.string.statistics)
            .setMessage(message.toString())
            .setPositiveButton("OK", null)
            .show()
    }

    private fun getDateString(calendar: Calendar): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
    }

    override fun onMarkYes(item: TrackingItem) {
        val dateStr = getDateString(currentDate)
        val currentEntry = database.getEntry(item.id, dateStr)
        
        if (currentEntry?.occurred == true) {
            database.deleteEntry(item.id, dateStr)
        } else {
            database.setEntry(item.id, dateStr, true)
        }
        loadData()
    }

    override fun onMarkNo(item: TrackingItem) {
        val dateStr = getDateString(currentDate)
        val currentEntry = database.getEntry(item.id, dateStr)
        
        if (currentEntry?.occurred == false) {
            database.deleteEntry(item.id, dateStr)
        } else {
            database.setEntry(item.id, dateStr, false)
        }
        loadData()
    }

    override fun onItemLongClick(item: TrackingItem) {
        showItemOptionsDialog(item)
    }

    override fun onItemClick(item: TrackingItem) {
        selectedItemId = item.id
        supportActionBar?.title = item.name
        drawerLayout.closeDrawer(GravityCompat.START)
        loadData()
    }

    private fun showItemOptionsDialog(item: TrackingItem) {
        val options = arrayOf(getString(R.string.edit), getString(R.string.delete))
        AlertDialog.Builder(this)
            .setTitle(item.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showAddItemDialog(item)
                    1 -> confirmDeleteItem(item)
                }
            }
            .show()
    }

    private fun confirmDeleteItem(item: TrackingItem) {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete)
            .setMessage("Delete \"${item.name}\" and all its tracking data?")
            .setPositiveButton(R.string.delete) { _, _ ->
                database.deleteTrackingItem(item.id)
                if (selectedItemId == item.id) {
                    selectedItemId = null
                    supportActionBar?.title = getString(R.string.app_name)
                }
                loadData()
                Toast.makeText(this, "Item deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun onBackPressed() {
        when {
            drawerLayout.isDrawerOpen(GravityCompat.START) -> {
                drawerLayout.closeDrawer(GravityCompat.START)
            }
            selectedItemId != null -> {
                selectedItemId = null
                supportActionBar?.title = getString(R.string.app_name)
                loadData()
            }
            else -> {
                super.onBackPressed()
            }
        }
    }
}
