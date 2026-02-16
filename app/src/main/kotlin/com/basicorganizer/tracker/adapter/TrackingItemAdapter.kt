package com.basicorganizer.tracker.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.basicorganizer.tracker.R
import com.basicorganizer.tracker.data.Sentiment
import com.basicorganizer.tracker.data.TrackingItem
import com.google.android.material.button.MaterialButton

class TrackingItemAdapter(
    private val context: Context,
    private var items: List<TrackingItem>,
    private val markedItems: MutableMap<Long, Boolean>,
    private val listener: OnItemInteractionListener
) : RecyclerView.Adapter<TrackingItemAdapter.ViewHolder>() {

    interface OnItemInteractionListener {
        fun onMarkYes(item: TrackingItem)
        fun onMarkNo(item: TrackingItem)
        fun onItemLongClick(item: TrackingItem)
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
        holder.sentimentIndicator.setBackgroundColor(ContextCompat.getColor(context, sentimentColor))
        
        val isMarked = markedItems[item.id]
        updateButtonStates(holder, isMarked)
        
        holder.btnYes.setOnClickListener {
            listener.onMarkYes(item)
        }
        
        holder.btnNo.setOnClickListener {
            listener.onMarkNo(item)
        }
        
        holder.itemView.setOnLongClickListener {
            listener.onItemLongClick(item)
            true
        }
    }

    private fun updateButtonStates(holder: ViewHolder, isMarked: Boolean?) {
        when (isMarked) {
            true -> {
                holder.btnYes.setBackgroundColor(ContextCompat.getColor(context, R.color.colorPrimary))
                holder.btnYes.setTextColor(ContextCompat.getColor(context, R.color.white))
                holder.btnNo.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent))
                holder.btnNo.setTextColor(ContextCompat.getColor(context, R.color.sentiment_negative))
            }
            false -> {
                holder.btnYes.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent))
                holder.btnYes.setTextColor(ContextCompat.getColor(context, R.color.colorPrimary))
                holder.btnNo.setBackgroundColor(ContextCompat.getColor(context, R.color.sentiment_negative))
                holder.btnNo.setTextColor(ContextCompat.getColor(context, R.color.white))
            }
            null -> {
                holder.btnYes.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent))
                holder.btnYes.setTextColor(ContextCompat.getColor(context, R.color.colorPrimary))
                holder.btnNo.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent))
                holder.btnNo.setTextColor(ContextCompat.getColor(context, R.color.sentiment_negative))
            }
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateItems(newItems: List<TrackingItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    fun updateMarkedItems(newMarkedItems: Map<Long, Boolean>) {
        markedItems.clear()
        markedItems.putAll(newMarkedItems)
        notifyDataSetChanged()
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tv_item_name)
        val sentimentIndicator: View = itemView.findViewById(R.id.sentiment_indicator)
        val btnYes: MaterialButton = itemView.findViewById(R.id.btn_mark_yes)
        val btnNo: MaterialButton = itemView.findViewById(R.id.btn_mark_no)
    }
}
