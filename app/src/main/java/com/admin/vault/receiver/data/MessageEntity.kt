package com.admin.vault.receiver.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "vault_table",
    // ✅ SAFETY LOCK: 'supabase_id' par unique index lagaya hai.
    // Agar Supabase galti se same data dubara bhejta hai, to Database use reject kar dega ya update kar dega (duplicate nahi banega).
    indices = [Index(value = ["supabase_id"], unique = true)]
)
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0, // Local ID (Room ke liye)
    val supabase_id: String,  // ✅ Fixed: UUID String (Supabase wala ID)
    val source_app: String,
    val header: String,
    val payload: String,
    val timestamp: String,
    val file_path: String     // CSV file ka path
)
