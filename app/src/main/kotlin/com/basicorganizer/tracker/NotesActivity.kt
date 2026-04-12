package com.basicorganizer.tracker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.basicorganizer.tracker.adapter.NotesAdapter
import com.basicorganizer.tracker.data.Note
import com.basicorganizer.tracker.data.TrackerDatabase
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.text.SimpleDateFormat
import java.util.*

class NotesActivity : AppCompatActivity(), NotesAdapter.OnNoteClickListener {

    private lateinit var database: TrackerDatabase
    private lateinit var rvNotes: RecyclerView
    private lateinit var emptyState: View
    private lateinit var fabAddNote: FloatingActionButton
    private lateinit var notesAdapter: NotesAdapter
    
    private var itemId: Long = -1
    private var itemName: String = ""

    companion object {
        const val EXTRA_ITEM_ID = "item_id"
        const val EXTRA_ITEM_NAME = "item_name"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notes)

        itemId = intent.getLongExtra(EXTRA_ITEM_ID, -1)
        itemName = intent.getStringExtra(EXTRA_ITEM_NAME) ?: ""

        if (itemId == -1L) {
            finish()
            return
        }

        database = TrackerDatabase(this)
        initializeViews()
        loadNotes()
    }

    private fun initializeViews() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        toolbar.title = getString(R.string.notes_title, itemName)
        toolbar.setNavigationOnClickListener { finish() }
        toolbar.navigationIcon?.setTint(ContextCompat.getColor(this, R.color.white))

        rvNotes = findViewById(R.id.rv_notes)
        emptyState = findViewById(R.id.empty_state)
        fabAddNote = findViewById(R.id.fab_add_note)

        rvNotes.layoutManager = LinearLayoutManager(this)
        notesAdapter = NotesAdapter(this, emptyList(), this)
        rvNotes.adapter = notesAdapter

        fabAddNote.setOnClickListener {
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            showNoteDialog(todayStr)
        }
    }

    private fun loadNotes() {
        val notes = database.getNotesForItem(itemId)
        notesAdapter.updateNotes(notes)

        if (notes.isEmpty()) {
            rvNotes.visibility = View.GONE
            emptyState.visibility = View.VISIBLE
        } else {
            rvNotes.visibility = View.VISIBLE
            emptyState.visibility = View.GONE
        }
    }

    override fun onNoteClick(note: Note) {
        showNoteDialog(note.date)
    }

    private fun showNoteDialog(dateStr: String) {
        val existingNote = database.getNote(itemId, dateStr)
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_note, null)
        val etNote = dialogView.findViewById<EditText>(R.id.et_note)
        
        existingNote?.let {
            etNote.setText(it.text)
        }

        val dateFormat = SimpleDateFormat("d MMMM yyyy", Locale.getDefault())
        val calendar = Calendar.getInstance()
        try {
            val parsedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateStr)
            if (parsedDate != null) {
                calendar.time = parsedDate
            }
        } catch (e: Exception) {}
        val displayDate = dateFormat.format(calendar.time)

        val dialog = AlertDialog.Builder(this)
            .setTitle(getString(R.string.note_for_date, displayDate))
            .setView(dialogView)
            .setPositiveButton(R.string.save) { _, _ ->
                val noteText = etNote.text.toString().trim()
                database.saveNote(itemId, dateStr, noteText)
                loadNotes()
            }
            .setNegativeButton(R.string.cancel, null)
            .create()

        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        dialog.setOnShowListener {
            etNote.requestFocus()
            etNote.postDelayed({
                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(etNote, InputMethodManager.SHOW_FORCED)
            }, 100)
        }
        dialog.show()
    }
}
