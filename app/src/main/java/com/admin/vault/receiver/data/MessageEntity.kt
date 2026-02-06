package com.admin.vault.receiver.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vault_table")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val supabase_id: String,
    val source_app: String,
    val header: String,
    val payload: String,
    val timestamp: String,
    val file_path: String // CSV फाइल का पता
)
