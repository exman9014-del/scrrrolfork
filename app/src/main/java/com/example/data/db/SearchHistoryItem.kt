package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "search_history")
data class SearchHistoryItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val query: String,
    val cleanQuery: String,
    val searchEngine: String,
    val searchType: String,
    val creatorUsername: String?,
    val timestamp: Long = System.currentTimeMillis()
)
