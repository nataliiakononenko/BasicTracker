package com.basicorganizer.tracker

import android.os.Bundle
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
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
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.basicorganizer.tracker.adapter.DrawerItemAdapter
import com.basicorganizer.tracker.adapter.TrackingItemAdapter
import com.basicorganizer.tracker.data.Sentiment
import com.basicorganizer.tracker.data.TrackerDatabase
import com.basicorganizer.tracker.data.TrackingItem
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
    private lateinit var tvMonthYear: TextView
    private lateinit var btnPrevMonth: android.widget.ImageButton
    private lateinit var btnNextMonth: android.widget.ImageButton
    private lateinit var weekHeader: LinearLayout
    private lateinit var monthViewContainer: View

    private lateinit var database: TrackerDatabase
    private lateinit var itemAdapter: TrackingItemAdapter
    private lateinit var drawerAdapter: DrawerItemAdapter
    private lateinit var itemTouchHelper: ItemTouchHelper

    private var currentDate: Calendar = Calendar.getInstance()  // For month navigation
    private var selectedDate: Calendar = Calendar.getInstance()  // For selected day (green frame)
    private var selectedItemId: Long? = null
    private var defaultItemId: Long? = null

    private val markedItems = mutableMapOf<Long, Boolean>()
    private val occurrenceCounts = mutableMapOf<Long, Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        database = TrackerDatabase(this)
        loadDefaultItem()
        initializeViews()
        setupToolbar()
        setupNavigationDrawer()
        setupMonthNavigation()
        loadData()
    }

    private fun loadDefaultItem() {
        val prefs = getSharedPreferences("BasicTrackerPrefs", MODE_PRIVATE)
        val savedDefaultId = prefs.getLong("defaultItemId", -1L)
        if (savedDefaultId != -1L) {
            val item = database.getTrackingItem(savedDefaultId)
            if (item != null) {
                defaultItemId = savedDefaultId
                selectedItemId = savedDefaultId
                supportActionBar?.title = item.name
            }
        } else {
            supportActionBar?.title = ""
        }
    }

    private fun initializeViews() {
        drawerLayout = findViewById(R.id.drawer_layout)
        toolbar = findViewById(R.id.toolbar)
        rvTrackingItems = findViewById(R.id.rv_tracking_items)
        emptyState = findViewById(R.id.empty_state)
        fabAddItem = findViewById(R.id.fab_add_item)
        tvMonthYear = findViewById(R.id.tv_month_year)
        btnPrevMonth = findViewById(R.id.btn_prev_month)
        btnNextMonth = findViewById(R.id.btn_next_month)
        weekHeader = findViewById(R.id.week_header)
        monthViewContainer = findViewById(R.id.month_view_container)

        rvTrackingItems.layoutManager = LinearLayoutManager(this)

        val navView = findViewById<NavigationView>(R.id.nav_view)
        val drawerRecyclerView = navView.findViewById<RecyclerView>(R.id.rv_drawer_items)
        drawerRecyclerView.layoutManager = LinearLayoutManager(this)

        drawerAdapter = DrawerItemAdapter(this, mutableListOf(), occurrenceCounts, this)
        drawerAdapter.setDatabase(database)
        drawerRecyclerView.adapter = drawerAdapter

        val callback = object : ItemTouchHelper.SimpleCallback(0, 0) {
            private var dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN
            
            override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
                // Only allow drag if explicitly started via handle
                return makeMovementFlags(dragFlags, 0)
            }
            
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val fromPosition = viewHolder.adapterPosition
                val toPosition = target.adapterPosition
                drawerAdapter.onItemMove(fromPosition, toPosition)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // Not used
            }

            override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                super.onSelectedChanged(viewHolder, actionState)
                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                    viewHolder?.itemView?.alpha = 0.5f
                }
            }

            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)
                viewHolder.itemView.alpha = 1.0f
                // Save the new order from the adapter
                drawerAdapter.saveItemPositions()
            }
        }

        itemTouchHelper = ItemTouchHelper(callback)
        itemTouchHelper.attachToRecyclerView(drawerRecyclerView)
        drawerAdapter.setItemTouchHelper(itemTouchHelper)

        navView.findViewById<View>(R.id.drawer_all_items).setOnClickListener {
            selectedItemId = null
            supportActionBar?.title = ""
            drawerLayout.closeDrawer(GravityCompat.START)
            loadData()
        }

        navView.findViewById<FloatingActionButton>(R.id.drawer_fab_add_item).setOnClickListener {
            showAddItemDialog()
        }

        navView.findViewById<View>(R.id.settings_tips).setOnClickListener {
            showTipsDialog()
            drawerLayout.closeDrawer(GravityCompat.START)
        }

        navView.findViewById<View>(R.id.settings_report_bug).setOnClickListener {
            val intent = Intent(Intent.ACTION_SENDTO)
            intent.data = Uri.parse("mailto:basicorganizer.post@gmail.com?subject=" + Uri.encode("Basic Tracker App"))
            try {
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "No email app found", Toast.LENGTH_SHORT).show()
            }
            drawerLayout.closeDrawer(GravityCompat.START)
        }

        navView.findViewById<View>(R.id.settings_more_apps).setOnClickListener {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://dev?id=BasicOrganizer")))
            } catch (e: android.content.ActivityNotFoundException) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/developer?id=BasicOrganizer")))
            }
            drawerLayout.closeDrawer(GravityCompat.START)
        }

        fabAddItem.setOnClickListener { showAddItemDialog() }
        findViewById<View>(R.id.btn_add_item_empty).setOnClickListener { showAddItemDialog() }
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

    private fun setupMonthNavigation() {
        btnPrevMonth.setOnClickListener {
            currentDate.add(Calendar.MONTH, -1)
            updateDateDisplay()  // Only update calendar display, not tracking items
        }
        btnNextMonth.setOnClickListener {
            currentDate.add(Calendar.MONTH, 1)
            updateDateDisplay()  // Only update calendar display, not tracking items
        }
    }

    private fun loadData() {
        updateDateDisplay()
        loadTrackingItems()
        loadDrawerItems()
    }

    private fun updateDateDisplay() {
        val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        tvMonthYear.text = monthYearFormat.format(currentDate.time)

        setupWeekDayHeaders()
        setupMonthView()
    }

    private fun setupWeekDayHeaders() {
        weekHeader.removeAllViews()
        val daysOfWeek = arrayOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")

        for (day in daysOfWeek) {
            val textView = TextView(this)
            textView.text = day
            textView.gravity = android.view.Gravity.CENTER
            textView.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
            textView.textSize = 12f
            val params = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            textView.layoutParams = params
            weekHeader.addView(textView)
        }
    }

    private fun setupMonthView() {
        val monthGrid = findViewById<android.widget.GridLayout>(R.id.month_grid)
        monthGrid.removeAllViews()

        val calendar = currentDate.clone() as Calendar
        calendar.set(Calendar.DAY_OF_MONTH, 1)

        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        var firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY
        if (firstDayOfWeek < 0) firstDayOfWeek += 7

        val items = database.getAllTrackingItems()
        val todayStr = getDateString(Calendar.getInstance())

        val prevMonthCal = currentDate.clone() as Calendar
        prevMonthCal.add(Calendar.MONTH, -1)
        val daysInPrevMonth = prevMonthCal.getActualMaximum(Calendar.DAY_OF_MONTH)

        for (i in 0 until firstDayOfWeek) {
            val day = daysInPrevMonth - firstDayOfWeek + i + 1
            prevMonthCal.set(Calendar.DAY_OF_MONTH, day)
            addDayView(monthGrid, day, prevMonthCal, items, todayStr, false)
        }

        for (day in 1..daysInMonth) {
            calendar.set(Calendar.DAY_OF_MONTH, day)
            addDayView(monthGrid, day, calendar, items, todayStr, true)
        }

        val nextMonthCal = currentDate.clone() as Calendar
        nextMonthCal.add(Calendar.MONTH, 1)
        val totalCells = firstDayOfWeek + daysInMonth
        val remainingCells = if (totalCells % 7 == 0) 0 else 7 - (totalCells % 7)
        
        for (day in 1..remainingCells) {
            nextMonthCal.set(Calendar.DAY_OF_MONTH, day)
            addDayView(monthGrid, day, nextMonthCal, items, todayStr, false)
        }
    }

    private fun addDayView(
        monthGrid: android.widget.GridLayout,
        day: Int,
        calendar: Calendar,
        items: List<TrackingItem>,
        todayStr: String,
        isCurrentMonth: Boolean
    ) {
        // Capture the date NOW before the calendar object gets mutated in the loop
        val capturedTime = calendar.timeInMillis
        val dateStr = getDateString(calendar)
        val selectedDateStr = getDateString(selectedDate)
        val isToday = dateStr == todayStr
        val isSelected = dateStr == selectedDateStr

        val dayView = LayoutInflater.from(this).inflate(R.layout.item_month_day, monthGrid, false)
        val tvDay = dayView.findViewById<TextView>(R.id.tv_day)
        val dotsContainer = dayView.findViewById<LinearLayout>(R.id.dots_container)
        val dayContainer = dayView.findViewById<LinearLayout>(R.id.day_container)

        tvDay.text = day.toString()
        
        if (!isCurrentMonth) {
            tvDay.setTextColor(ContextCompat.getColor(this, R.color.text_other_month))
        } else {
            tvDay.setTextColor(ContextCompat.getColor(this, R.color.text_primary))
        }

        // Set background based on date type
        when {
            isToday && isSelected -> {
                // Both today and selected: light green background + green frame
                dayContainer.setBackgroundResource(R.drawable.selected_today_background)
            }
            isSelected -> {
                // Selected date gets green frame only
                dayContainer.setBackgroundResource(R.drawable.selected_day_background)
            }
            isToday -> {
                // Today gets light green background only
                dayContainer.setBackgroundResource(R.drawable.current_day_background)
            }
            else -> {
                // Default: transparent background
                dayContainer.setBackgroundColor(android.graphics.Color.TRANSPARENT)
            }
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

        // Use captured time to avoid stale calendar reference
        dayView.setOnClickListener {
            selectedDate.timeInMillis = capturedTime
            setupMonthView()
            loadTrackingItems()
        }

        dayView.setOnLongClickListener {
            selectedDate.timeInMillis = capturedTime
            setupMonthView()
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

    private fun loadTrackingItems() {
        // Update toolbar title based on selected item
        if (selectedItemId != null) {
            val item = database.getTrackingItem(selectedItemId!!)
            supportActionBar?.title = item?.name ?: ""
        } else {
            supportActionBar?.title = ""
        }
        
        val items = if (selectedItemId != null) {
            listOfNotNull(database.getTrackingItem(selectedItemId!!))
        } else {
            database.getAllTrackingItems()
        }

        val newMarkedItems = mutableMapOf<Long, Boolean>()
        val dateStr = getDateString(selectedDate)  // Use selectedDate for tracking items
        for (item in items) {
            val entry = database.getEntry(item.id, dateStr)
            newMarkedItems[item.id] = entry?.occurred == true
        }
        markedItems.clear()
        markedItems.putAll(newMarkedItems)

        if (items.isEmpty()) {
            emptyState.visibility = View.VISIBLE
            rvTrackingItems.visibility = View.GONE
            fabAddItem.visibility = View.GONE
        } else {
            emptyState.visibility = View.GONE
            rvTrackingItems.visibility = View.VISIBLE
            fabAddItem.visibility = if (selectedItemId == null) View.VISIBLE else View.GONE

            if (!::itemAdapter.isInitialized) {
                itemAdapter = TrackingItemAdapter(this, items, markedItems, this)
                rvTrackingItems.adapter = itemAdapter
            } else {
                itemAdapter.updateData(items, newMarkedItems)
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
        drawerAdapter.setDefaultItemId(defaultItemId)
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

    private fun showTipsDialog() {
        // TODO: Implement tips dialog showing how to use the app
        Toast.makeText(this, "Tips coming soon!", Toast.LENGTH_SHORT).show()
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

    override fun onToggleMark(item: TrackingItem) {
        val dateStr = getDateString(selectedDate)
        val currentEntry = database.getEntry(item.id, dateStr)
        
        if (currentEntry?.occurred == true) {
            database.deleteEntry(item.id, dateStr)
        } else {
            database.setEntry(item.id, dateStr, true)
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

    override fun onItemDelete(item: TrackingItem) {
        confirmDeleteItem(item)
    }

    override fun onItemMoved(fromPosition: Int, toPosition: Int) {
        // Position updates are handled in ItemTouchHelper's clearView
    }

    private fun showItemOptionsDialog(item: TrackingItem) {
        val isDefault = defaultItemId == item.id
        val defaultOption = if (isDefault) "Unset as default" else "Set as default"
        val options = arrayOf(getString(R.string.edit), defaultOption)
        
        AlertDialog.Builder(this)
            .setTitle(item.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showAddItemDialog(item)
                    1 -> toggleDefaultItem(item)
                }
            }
            .show()
    }

    private fun toggleDefaultItem(item: TrackingItem) {
        val prefs = getSharedPreferences("BasicTrackerPrefs", MODE_PRIVATE)
        if (defaultItemId == item.id) {
            // Unset as default
            defaultItemId = null
            prefs.edit().remove("defaultItemId").apply()
            Toast.makeText(this, "Default view unset", Toast.LENGTH_SHORT).show()
        } else {
            // Set as default
            defaultItemId = item.id
            prefs.edit().putLong("defaultItemId", item.id).apply()
            Toast.makeText(this, "\"${item.name}\" set as default view", Toast.LENGTH_SHORT).show()
        }
        drawerAdapter.setDefaultItemId(defaultItemId)
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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_statistics -> {
                // TODO: Implement statistics view
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}
