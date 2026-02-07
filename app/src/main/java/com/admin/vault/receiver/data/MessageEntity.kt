package com.admin.vault.receiver.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "vault_table",
    // ✅ FIX: 'indices' add kiya gaya hai.
    // Iska matlab: Agar same 'supabase_id' dubara aayi, to Room use duplicate nahi manega, balki handle karega.
    indices = [Index(value = ["supabase_id"], unique = true)]
)
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val supabase_id: String, // Ab ye Unique hai via Index
    val source_app: String,
    val header: String,
    val payload: String,
    val timestamp: String,
    val file_path: String
)
