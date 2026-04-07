package com.basicorganizer.tracker.data

enum class Sentiment {
    POSITIVE,
    NEGATIVE,
    NEUTRAL
}

data class TrackingItem(
    var id: Long = 0,
    var name: String = "",
    var sentiment: Sentiment = Sentiment.NEUTRAL,
    var position: Int = 0,
    var createdAt: String = "",
    var archived: Boolean = false
)
