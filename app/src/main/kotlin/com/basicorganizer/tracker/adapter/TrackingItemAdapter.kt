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

class TrackingItemAdapter(
    private val context: Context,
    private var items: List<TrackingItem>,
    private val markedItems: MutableMap<Long, Boolean>,
    private val listener: OnItemInteractionListener
) : RecyclerView.Adapter<TrackingItemAdapter.ViewHolder>() {

    interface OnItemInteractionListener {
        fun onToggleMark(item: TrackingItem)
        fun onItemLongClick(item: TrackingItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_tracking, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        
        holder.tvName.text = item.name
        
        val bgColor = when (item.sentiment) {
            Sentiment.POSITIVE -> R.color.sentiment_positive_light
            Sentiment.NEGATIVE -> R.color.sentiment_negative_light
            Sentiment.NEUTRAL -> R.color.sentiment_neutral_light
        }
        holder.itemLayout.setBackgroundColor(ContextCompat.getColor(context, bgColor))
        
        val sentimentColor = when (item.sentiment) {
            Sentiment.POSITIVE -> R.color.sentiment_positive
            Sentiment.NEGATIVE -> R.color.sentiment_negative
            Sentiment.NEUTRAL -> R.color.sentiment_neutral
        }
        val drawable = holder.sentimentIndicator.background.mutate() as? GradientDrawable
        drawable?.setColor(ContextCompat.getColor(context, sentimentColor))
        
        val isMarked = markedItems[item.id] == true
        updateCheckState(holder, isMarked)
        
        holder.btnCheck.setOnClickListener {
            listener.onToggleMark(item)
        }
        
        holder.itemView.setOnLongClickListener {
            listener.onItemLongClick(item)
            true
        }
    }

    private fun updateCheckState(holder: ViewHolder, isChecked: Boolean) {
        if (isChecked) {
            holder.btnCheck.setBackgroundResource(R.drawable.circle_checked_background)
            holder.btnCheck.setColorFilter(ContextCompat.getColor(context, R.color.white))
        } else {
            holder.btnCheck.setBackgroundResource(R.drawable.check_unchecked_background)
            holder.btnCheck.setColorFilter(ContextCompat.getColor(context, R.color.text_secondary))
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<TrackingItem>, newMarkedItems: Map<Long, Boolean>) {
        items = newItems
        markedItems.clear()
        markedItems.putAll(newMarkedItems)
        notifyDataSetChanged()
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tv_item_name)
        val itemLayout: View = itemView.findViewById(R.id.item_layout)
        val sentimentIndicator: View = itemView.findViewById(R.id.sentiment_indicator)
        val btnCheck: ImageView = itemView.findViewById(R.id.btn_mark_check)
    }
}
