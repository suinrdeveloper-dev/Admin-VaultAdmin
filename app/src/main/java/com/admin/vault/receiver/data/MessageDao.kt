package com.admin.vault.receiver.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(msg: MessageEntity)

    // सर्च लॉजिक: ऐप का नाम, हेडर या मैसेज में ढूंढें
    @Query("SELECT * FROM vault_table WHERE source_app LIKE '%' || :query || '%' OR payload LIKE '%' || :query || '%' OR header LIKE '%' || :query || '%' ORDER BY id DESC")
    fun searchMessages(query: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM vault_table ORDER BY id DESC")
    fun getAllMessages(): Flow<List<MessageEntity>>
}
