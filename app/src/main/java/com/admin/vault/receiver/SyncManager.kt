package com.admin.vault.receiver

import android.content.Context
import android.os.Environment
import android.util.Log
import android.widget.Toast
import com.admin.vault.receiver.data.AppDatabase
import com.admin.vault.receiver.data.MessageEntity
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.serializer.KotlinXSerializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ✅ FIX 1: Table ke columns ke hisab se mapping ki hai
// ✅ FIX 2: 'id' ko Long se String banaya (Kyunki UUID string hota hai)
@Serializable
data class RemoteData(
    @SerialName("event_id") val id: String? = null,      // Supabase: event_id
    @SerialName("source_app") val source_app: String? = null,
    @SerialName("header") val header: String? = null,
    @SerialName("payload") val payload: String? = null,
    @SerialName("timestamp") val created_at: String? = null // Supabase: timestamp
)

class SyncManager(private val context: Context) {
    private val db = AppDatabase.getDatabase(context)
    private val scope = CoroutineScope(Dispatchers.IO)
    
    // Serializer Setup
    private val supabase = createSupabaseClient(
        supabaseUrl = Config.SUPABASE_URL,
        supabaseKey = Config.SUPABASE_KEY
    ) { 
        install(Postgrest) 
        defaultSerializer = KotlinXSerializer(Json { ignoreUnknownKeys = true })
    }

    fun executeVacuum() {
        scope.launch {
            try {
                Log.d("Vacuum", "🔍 Connecting to Supabase...")

                // 1. FETCH
                // Hum 'sys_sync_stream' table se data mang rahe hain
                val result = supabase.from("sys_sync_stream").select()
                val list = result.decodeList<RemoteData>()
                
                if (list.isNotEmpty()) {
                    val msg = "📥 Found ${list.size} messages."
                    Log.i("Vacuum", msg)
                    showToast(msg)

                    list.forEach { item ->
                        try {
                            // Validation: Agar ID ya Date gayab hai to skip karo
                            if (item.id == null || item.created_at == null) {
                                Log.e("Vacuum", "⚠️ SKIPPING: Missing event_id or timestamp. Data: $item")
                                return@forEach
                            }

                            // Step A: CSV Create
                            val path = createCsvFileOrThrow(item)
                            
                            // Step B: DB Save
                            val entity = MessageEntity(
                                supabase_id = item.id, // Ab ye String hai (UUID), perfect fit
                                source_app = item.source_app ?: "Unknown",
                                header = item.header ?: "No Header",
                                payload = item.payload ?: "",
                                timestamp = item.created_at,
                                file_path = path
                            )
                            db.messageDao().insert(entity)
                            
                            // Step C: DELETE (Important: Filter by 'event_id')
                            supabase.from("sys_sync_stream").delete {
                                filter { eq("event_id", item.id) } // ✅ Table column name 'event_id' hai
                            }
                            Log.d("Vacuum", "✅ Synced & Deleted UUID: ${item.id}")

                        } catch (e: Exception) {
                            Log.e("Vacuum", "❌ Error processing UUID ${item.id}: ${e.message}")
                            showToast("Error: ${e.message}")
                        }
                    }
                } else {
                    Log.d("Vacuum", "No new data on server.")
                }

            } catch (e: Exception) {
                Log.e("Vacuum", "🔥 CRITICAL ERROR: ${e.message}", e)
                showToast("Connection Error: ${e.message}")
            }
        }
    }

    private fun createCsvFileOrThrow(data: RemoteData): String {
        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "SecureVault")
        if (!dir.exists()) {
            val created = dir.mkdirs()
            if (!created) throw Exception("Could not create folder. Check Permissions!")
        }

        val time = SimpleDateFormat("HHmmss", Locale.getDefault()).format(Date())
        // ID string hai, isliye file name me safe rahega
        val filename = "${data.source_app}_${data.id}_$time.csv"
        val file = File(dir, filename)

        val writer = FileWriter(file)
        writer.append("Header,Message,Time\n")
        writer.append("\"${data.header}\",\"${data.payload}\",\"${data.created_at}\"")
        writer.flush()
        writer.close()
        
        return file.absolutePath
    }

    private suspend fun showToast(msg: String) {
        withContext(Dispatchers.Main) {
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }
}
