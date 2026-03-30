package com.basicorganizer.tracker.adapter

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.basicorganizer.tracker.R
import com.basicorganizer.tracker.data.Sentiment
import com.basicorganizer.tracker.data.TrackingItem

class DrawerItemAdapter(
    private val context: Context,
    private var items: MutableList<TrackingItem>,
    private val occurrenceCounts: MutableMap<Long, Int>,
    private val listener: OnItemClickListener,
    private var itemTouchHelper: androidx.recyclerview.widget.ItemTouchHelper? = null
) : RecyclerView.Adapter<DrawerItemAdapter.ViewHolder>() {

    private var defaultItemId: Long? = null
    private var database: com.basicorganizer.tracker.data.TrackerDatabase? = null

    fun setDatabase(db: com.basicorganizer.tracker.data.TrackerDatabase) {
        database = db
    }

    fun setItemTouchHelper(helper: androidx.recyclerview.widget.ItemTouchHelper) {
        itemTouchHelper = helper
    }

    fun setDefaultItemId(itemId: Long?) {
        defaultItemId = itemId
        notifyDataSetChanged()
    }

    fun saveItemPositions() {
        database?.let { db ->
            for (i in items.indices) {
                items[i].position = i
                db.updateTrackingItem(items[i])
            }
        }
    }

    interface OnItemClickListener {
        fun onItemClick(item: TrackingItem)
        fun onItemLongClick(item: TrackingItem)
        fun onItemDelete(item: TrackingItem)
        fun onItemMoved(fromPosition: Int, toPosition: Int)
    }

    fun onItemMove(fromPosition: Int, toPosition: Int) {
        val item = items.removeAt(fromPosition)
        items.add(toPosition, item)
        notifyItemMoved(fromPosition, toPosition)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_drawer_tracking, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        
        holder.tvName.text = item.name
        
        // Set background color for default item
        if (item.id == defaultItemId) {
            holder.itemView.setBackgroundColor(ContextCompat.getColor(context, R.color.default_item_background))
        } else {
            holder.itemView.setBackgroundResource(R.drawable.drawer_item_border)
        }
        
        val sentimentColor = when (item.sentiment) {
            Sentiment.POSITIVE -> R.color.sentiment_positive
            Sentiment.NEGATIVE -> R.color.sentiment_negative
            Sentiment.NEUTRAL -> R.color.sentiment_neutral
        }
        
        val drawable = holder.sentimentIndicator.background as? GradientDrawable
        drawable?.setColor(ContextCompat.getColor(context, sentimentColor))
        
        holder.itemView.setOnClickListener {
            listener.onItemClick(item)
        }
        
        holder.itemView.setOnLongClickListener {
            listener.onItemLongClick(item)
            true
        }
        
        holder.btnDelete.setOnClickListener {
            listener.onItemDelete(item)
        }

        holder.btnDragHandle.setOnTouchListener { _, event ->
            if (event.actionMasked == android.view.MotionEvent.ACTION_DOWN) {
                itemTouchHelper?.startDrag(holder)
            }
            false
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateItems(newItems: List<TrackingItem>, newCounts: Map<Long, Int>) {
        items = newItems.toMutableList()
        occurrenceCounts.clear()
        occurrenceCounts.putAll(newCounts)
        notifyDataSetChanged()
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tv_item_name)
        val sentimentIndicator: View = itemView.findViewById(R.id.sentiment_indicator)
        val btnDelete: ImageView = itemView.findViewById(R.id.btn_delete)
        val btnDragHandle: ImageView = itemView.findViewById(R.id.btn_drag_handle)
    }
}
