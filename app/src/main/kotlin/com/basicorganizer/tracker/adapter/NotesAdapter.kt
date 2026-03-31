package com.basicorganizer.tracker.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.basicorganizer.tracker.R
import com.basicorganizer.tracker.data.Note
import java.text.SimpleDateFormat
import java.util.*

class NotesAdapter(
    private val context: Context,
    private var notes: List<Note>,
    private val listener: OnNoteClickListener
) : RecyclerView.Adapter<NotesAdapter.ViewHolder>() {

    interface OnNoteClickListener {
        fun onNoteClick(note: Note)
    }

    private val displayFormat = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
    private val parseFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_note, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val note = notes[position]
        
        val displayDate = try {
            val date = parseFormat.parse(note.date)
            if (date != null) displayFormat.format(date) else note.date
        } catch (e: Exception) { note.date }
        
        holder.tvDate.text = displayDate
        holder.tvText.text = note.text
        
        holder.itemView.setOnClickListener {
            listener.onNoteClick(note)
        }
    }

    override fun getItemCount(): Int = notes.size

    fun updateNotes(newNotes: List<Note>) {
        notes = newNotes
        notifyDataSetChanged()
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDate: TextView = itemView.findViewById(R.id.tv_note_date)
        val tvText: TextView = itemView.findViewById(R.id.tv_note_text)
    }
}
