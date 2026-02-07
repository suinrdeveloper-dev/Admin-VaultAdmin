package com.admin.vault.receiver.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    // ✅ Conflict Strategy: REPLACE
    // Agar same 'supabase_id' (UUID) wala data wapas aata hai, to purane ko hata kar naya update karega.
    // Ye duplicate entries rokne ke liye best hai.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(msg: MessageEntity)

    // ✅ Search Logic (Improved): App Name, Header, ya Message content me dhunde
    @Query("SELECT * FROM vault_table WHERE source_app LIKE '%' || :query || '%' OR payload LIKE '%' || :query || '%' OR header LIKE '%' || :query || '%' ORDER BY id DESC")
    fun searchMessages(query: String): Flow<List<MessageEntity>>

    // ✅ Live Data Stream
    @Query("SELECT * FROM vault_table ORDER BY id DESC")
    fun getAllMessages(): Flow<List<MessageEntity>>

    // ✅ NEW ADDITION: Database saaf karne ke liye (Testing me kaam aayega)
    @Query("DELETE FROM vault_table")
    suspend fun deleteAll()
}
