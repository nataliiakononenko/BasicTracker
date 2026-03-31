package com.basicorganizer.tracker.adapter

import android.content.Context
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
        fun onItemClick(item: TrackingItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_tracking, parent, false)
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
        holder.sentimentIndicator.background = ContextCompat.getDrawable(context, R.drawable.circle_indicator)!!.mutate()
        holder.sentimentIndicator.background.setTint(ContextCompat.getColor(context, sentimentColor))
        
        val isMarked = markedItems[item.id] == true
        updateCheckState(holder, isMarked)
        
        holder.btnCheck.setOnClickListener {
            listener.onToggleMark(item)
        }
        
        holder.itemView.setOnClickListener {
            listener.onItemClick(item)
        }
        
        holder.itemView.setOnLongClickListener {
            listener.onItemLongClick(item)
            true
        }
    }

    private fun updateCheckState(holder: ViewHolder, isChecked: Boolean) {
        if (isChecked) {
            holder.btnCheck.setBackgroundResource(R.drawable.check_box_checked)
            holder.checkIcon.visibility = View.VISIBLE
            holder.checkIcon.setColorFilter(ContextCompat.getColor(context, R.color.text_secondary))
        } else {
            holder.btnCheck.setBackgroundResource(R.drawable.check_box_border)
            holder.checkIcon.visibility = View.GONE
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
        val sentimentIndicator: View = itemView.findViewById(R.id.sentiment_indicator)
        val btnCheck: View = itemView.findViewById(R.id.btn_mark_check)
        val checkIcon: ImageView = itemView.findViewById(R.id.check_icon)
    }
}
