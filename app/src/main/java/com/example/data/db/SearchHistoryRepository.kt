package com.example.data.db

import kotlinx.coroutines.flow.Flow

class SearchHistoryRepository(private val dao: SearchHistoryDao) {

    val allHistory: Flow<List<SearchHistoryItem>> = dao.getAllHistory()

    fun getRecentHistory(limit: Int = 30): Flow<List<SearchHistoryItem>> = dao.getRecentHistory(limit)

    suspend fun saveSearch(
        query: String,
        cleanQuery: String,
        engine: String,
        searchType: String,
        creatorUsername: String?
    ) {
        if (cleanQuery.isBlank()) return
        // Remove older duplicate entry for the same clean query so the newest one floats to top
        dao.deleteByCleanQuery(cleanQuery)
        val item = SearchHistoryItem(
            query = query,
            cleanQuery = cleanQuery,
            searchEngine = engine,
            searchType = searchType,
            creatorUsername = creatorUsername,
            timestamp = System.currentTimeMillis()
        )
        dao.insertItem(item)
    }

    suspend fun deleteById(id: Long) {
        dao.deleteById(id)
    }

    suspend fun clearAll() {
        dao.clearAll()
    }
}
