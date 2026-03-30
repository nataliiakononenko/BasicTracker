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
    private var items: List<TrackingItem>,
    private val occurrenceCounts: MutableMap<Long, Int>,
    private val listener: OnItemClickListener
) : RecyclerView.Adapter<DrawerItemAdapter.ViewHolder>() {

    interface OnItemClickListener {
        fun onItemClick(item: TrackingItem)
        fun onItemLongClick(item: TrackingItem)
        fun onItemDelete(item: TrackingItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_drawer_tracking, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        
        holder.tvName.text = item.name
        
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
    }

    override fun getItemCount(): Int = items.size

    fun updateItems(newItems: List<TrackingItem>, newCounts: Map<Long, Int>) {
        items = newItems
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
