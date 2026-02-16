package com.basicorganizer.tracker.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class TrackerDatabase(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_VERSION = 1
        private const val DATABASE_NAME = "TrackerDB"

        private const val TABLE_ITEMS = "tracking_items"
        private const val TABLE_ENTRIES = "tracking_entries"

        private const val KEY_ID = "id"
        private const val KEY_NAME = "name"
        private const val KEY_SENTIMENT = "sentiment"
        private const val KEY_POSITION = "position"
        private const val KEY_CREATED_AT = "created_at"

        private const val KEY_ITEM_ID = "item_id"
        private const val KEY_DATE = "date"
        private const val KEY_OCCURRED = "occurred"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createItemsTable = """
            CREATE TABLE $TABLE_ITEMS (
                $KEY_ID INTEGER PRIMARY KEY,
                $KEY_NAME TEXT,
                $KEY_SENTIMENT TEXT,
                $KEY_POSITION INTEGER DEFAULT 0,
                $KEY_CREATED_AT TEXT
            )
        """.trimIndent()
        db.execSQL(createItemsTable)

        val createEntriesTable = """
            CREATE TABLE $TABLE_ENTRIES (
                $KEY_ID INTEGER PRIMARY KEY,
                $KEY_ITEM_ID INTEGER,
                $KEY_DATE TEXT,
                $KEY_OCCURRED INTEGER,
                UNIQUE($KEY_ITEM_ID, $KEY_DATE)
            )
        """.trimIndent()
        db.execSQL(createEntriesTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Handle future migrations here
    }

    fun addTrackingItem(item: TrackingItem): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(KEY_NAME, item.name)
            put(KEY_SENTIMENT, item.sentiment.name)
            put(KEY_POSITION, item.position)
            put(KEY_CREATED_AT, item.createdAt)
        }
        val id = db.insert(TABLE_ITEMS, null, values)
        db.close()
        return id
    }

    fun getAllTrackingItems(): List<TrackingItem> {
        val items = mutableListOf<TrackingItem>()
        val query = "SELECT * FROM $TABLE_ITEMS ORDER BY $KEY_POSITION ASC"
        val db = readableDatabase
        val cursor = db.rawQuery(query, null)

        if (cursor.moveToFirst()) {
            do {
                val item = TrackingItem(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_ID)),
                    name = cursor.getString(cursor.getColumnIndexOrThrow(KEY_NAME)),
                    sentiment = Sentiment.valueOf(cursor.getString(cursor.getColumnIndexOrThrow(KEY_SENTIMENT))),
                    position = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_POSITION)),
                    createdAt = cursor.getString(cursor.getColumnIndexOrThrow(KEY_CREATED_AT))
                )
                items.add(item)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return items
    }

    fun getTrackingItem(id: Long): TrackingItem? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_ITEMS,
            null,
            "$KEY_ID = ?",
            arrayOf(id.toString()),
            null, null, null
        )

        return if (cursor.moveToFirst()) {
            val item = TrackingItem(
                id = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_ID)),
                name = cursor.getString(cursor.getColumnIndexOrThrow(KEY_NAME)),
                sentiment = Sentiment.valueOf(cursor.getString(cursor.getColumnIndexOrThrow(KEY_SENTIMENT))),
                position = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_POSITION)),
                createdAt = cursor.getString(cursor.getColumnIndexOrThrow(KEY_CREATED_AT))
            )
            cursor.close()
            item
        } else {
            cursor.close()
            null
        }
    }

    fun updateTrackingItem(item: TrackingItem): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(KEY_NAME, item.name)
            put(KEY_SENTIMENT, item.sentiment.name)
            put(KEY_POSITION, item.position)
        }
        val result = db.update(TABLE_ITEMS, values, "$KEY_ID = ?", arrayOf(item.id.toString()))
        db.close()
        return result
    }

    fun deleteTrackingItem(id: Long) {
        val db = writableDatabase
        db.delete(TABLE_ENTRIES, "$KEY_ITEM_ID = ?", arrayOf(id.toString()))
        db.delete(TABLE_ITEMS, "$KEY_ID = ?", arrayOf(id.toString()))
        db.close()
    }

    fun setEntry(itemId: Long, date: String, occurred: Boolean) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(KEY_ITEM_ID, itemId)
            put(KEY_DATE, date)
            put(KEY_OCCURRED, if (occurred) 1 else 0)
        }
        db.insertWithOnConflict(TABLE_ENTRIES, null, values, SQLiteDatabase.CONFLICT_REPLACE)
        db.close()
    }

    fun getEntry(itemId: Long, date: String): TrackingEntry? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_ENTRIES,
            null,
            "$KEY_ITEM_ID = ? AND $KEY_DATE = ?",
            arrayOf(itemId.toString(), date),
            null, null, null
        )

        return if (cursor.moveToFirst()) {
            val entry = TrackingEntry(
                id = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_ID)),
                trackingItemId = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_ITEM_ID)),
                date = cursor.getString(cursor.getColumnIndexOrThrow(KEY_DATE)),
                occurred = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_OCCURRED)) == 1
            )
            cursor.close()
            entry
        } else {
            cursor.close()
            null
        }
    }

    fun getEntriesForItem(itemId: Long): List<TrackingEntry> {
        val entries = mutableListOf<TrackingEntry>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_ENTRIES,
            null,
            "$KEY_ITEM_ID = ?",
            arrayOf(itemId.toString()),
            null, null, "$KEY_DATE DESC"
        )

        if (cursor.moveToFirst()) {
            do {
                val entry = TrackingEntry(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_ID)),
                    trackingItemId = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_ITEM_ID)),
                    date = cursor.getString(cursor.getColumnIndexOrThrow(KEY_DATE)),
                    occurred = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_OCCURRED)) == 1
                )
                entries.add(entry)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return entries
    }

    fun getEntriesForDate(date: String): List<TrackingEntry> {
        val entries = mutableListOf<TrackingEntry>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_ENTRIES,
            null,
            "$KEY_DATE = ?",
            arrayOf(date),
            null, null, null
        )

        if (cursor.moveToFirst()) {
            do {
                val entry = TrackingEntry(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_ID)),
                    trackingItemId = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_ITEM_ID)),
                    date = cursor.getString(cursor.getColumnIndexOrThrow(KEY_DATE)),
                    occurred = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_OCCURRED)) == 1
                )
                entries.add(entry)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return entries
    }

    fun getEntriesInRange(startDate: String, endDate: String): List<TrackingEntry> {
        val entries = mutableListOf<TrackingEntry>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_ENTRIES,
            null,
            "$KEY_DATE >= ? AND $KEY_DATE <= ?",
            arrayOf(startDate, endDate),
            null, null, "$KEY_DATE ASC"
        )

        if (cursor.moveToFirst()) {
            do {
                val entry = TrackingEntry(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_ID)),
                    trackingItemId = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_ITEM_ID)),
                    date = cursor.getString(cursor.getColumnIndexOrThrow(KEY_DATE)),
                    occurred = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_OCCURRED)) == 1
                )
                entries.add(entry)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return entries
    }

    fun countOccurrencesForItem(itemId: Long): Int {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT COUNT(*) FROM $TABLE_ENTRIES WHERE $KEY_ITEM_ID = ? AND $KEY_OCCURRED = 1",
            arrayOf(itemId.toString())
        )
        val count = if (cursor.moveToFirst()) cursor.getInt(0) else 0
        cursor.close()
        return count
    }

    fun countOccurrencesForItemInRange(itemId: Long, startDate: String, endDate: String): Int {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT COUNT(*) FROM $TABLE_ENTRIES WHERE $KEY_ITEM_ID = ? AND $KEY_OCCURRED = 1 AND $KEY_DATE >= ? AND $KEY_DATE <= ?",
            arrayOf(itemId.toString(), startDate, endDate)
        )
        val count = if (cursor.moveToFirst()) cursor.getInt(0) else 0
        cursor.close()
        return count
    }

    fun deleteEntry(itemId: Long, date: String) {
        val db = writableDatabase
        db.delete(TABLE_ENTRIES, "$KEY_ITEM_ID = ? AND $KEY_DATE = ?", arrayOf(itemId.toString(), date))
        db.close()
    }
}
