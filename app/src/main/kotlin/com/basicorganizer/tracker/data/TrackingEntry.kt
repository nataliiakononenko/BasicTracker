package com.basicorganizer.tracker.data

data class TrackingEntry(
    var id: Long = 0,
    var trackingItemId: Long = 0,
    var date: String = "",
    var occurred: Boolean = false
)
