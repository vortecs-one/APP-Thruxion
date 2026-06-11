package com.example.qhagoapp.data.dao

import androidx.room.*
import com.example.qhagoapp.data.model.ChatMessage
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatMessageDao {
    @Query("""
        SELECT * FROM chat_messages 
        WHERE ownerId = :currentUserId 
        AND (
            (senderId = :currentUserId AND receiverId = :partnerId) OR 
            (senderId = :partnerId AND receiverId = :currentUserId)
        )
        ORDER BY timestamp ASC
    """)
    fun getMessagesWith(currentUserId: String, partnerId: String): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage)

    @Query("""
        DELETE FROM chat_messages 
        WHERE ownerId = :currentUserId 
        AND (
            (senderId = :currentUserId AND receiverId = :partnerId) OR 
            (senderId = :partnerId AND receiverId = :currentUserId)
        )
    """)
    suspend fun clearChat(currentUserId: String, partnerId: String)

    @Query("""
        SELECT * FROM chat_messages 
        WHERE ownerId = :currentUserId 
        AND id IN (
            SELECT id FROM (
                SELECT id, MAX(timestamp) OVER (PARTITION BY CASE WHEN senderId = :currentUserId THEN receiverId ELSE senderId END) as max_ts, timestamp
                FROM chat_messages
                WHERE ownerId = :currentUserId
            ) WHERE timestamp = max_ts
        )
        ORDER BY timestamp DESC
    """)
    fun getActiveChats(currentUserId: String): Flow<List<ChatMessage>>
}
