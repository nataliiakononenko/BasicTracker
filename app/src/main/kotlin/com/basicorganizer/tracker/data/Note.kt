package com.basicorganizer.tracker.data

data class Note(
    var id: Long = 0,
    var trackingItemId: Long = 0,
    var date: String = "",
    var text: String = ""
)
