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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    
    // Supabase Client
    private val supabase = createSupabaseClient(
        supabaseUrl = Config.SUPABASE_URL,
        supabaseKey = Config.SUPABASE_KEY
    ) { install(Postgrest) }

    fun executeVacuum() {
        scope.launch {
            try {
                Log.d("Vacuum", "🔍 Connecting to Supabase...")

                // 1. FETCH: डेटा मांगो
                // अगर यहाँ क्रैश हुआ मतलब इंटरनेट या Config गलत है
                val list = supabase.from("sys_sync_stream").select().decodeList<RemoteData>()
                
                if (list.isNotEmpty()) {
                    val message = "📥 Found ${list.size} messages. Processing..."
                    Log.i("Vacuum", message)
                    showToast(message) // User ko batao ki data mila

                    list.forEach { item ->
                        try {
                            // Step A: CSV Create karo (Agar fail hua to catch me jayega)
                            val path = createCsvFileOrThrow(item)
                            
                            // Step B: DB me daalo
                            val entity = MessageEntity(
                                supabase_id = item.id.toString(),
                                source_app = item.source_app,
                                header = item.header ?: "Unknown",
                                payload = item.payload ?: "",
                                timestamp = item.created_at,
                                file_path = path
                            )
                            db.messageDao().insert(entity)
                            
                            // Step C: ✅ DELETE FROM SUPABASE (Sirf tab jab A aur B pass ho jayein)
                            supabase.from("sys_sync_stream").delete {
                                filter { eq("id", item.id) }
                            }
                            Log.d("Vacuum", "✅ Securely Synced & Deleted ID: ${item.id}")

                        } catch (e: Exception) {
                            // Agar CSV ya DB fail hua, to Supabase se delete MAT karna
                            Log.e("Vacuum", "❌ FAILED to process ID ${item.id}. Keeping on server.", e)
                            showToast("Error Saving: ${e.message}")
                        }
                    }
                    showToast("✅ Sync Complete!")
                } else {
                    Log.d("Vacuum", "No new data on server.")
                }

            } catch (e: Exception) {
                // Network Error ya Config Error
                Log.e("Vacuum", "🔥 CRITICAL FAILURE: ${e.message}", e)
                showToast("Connection Error: ${e.message}")
            }
        }
    }

    // Yeh function ab Error string return nahi karega, balki Exception phekega
    // Taki hum process rok sakein
    private fun createCsvFileOrThrow(data: RemoteData): String {
        // Folder Check
        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "SecureVault")
        if (!dir.exists()) {
            val created = dir.mkdirs()
            if (!created) throw Exception("Could not create folder. Permission Denied?")
        }

        val time = SimpleDateFormat("HHmmss", Locale.getDefault()).format(Date())
        val filename = "${data.source_app}_${data.id}_$time.csv"
        val file = File(dir, filename)

        val writer = FileWriter(file)
        writer.append("Header,Message,Time\n")
        writer.append("\"${data.header}\",\"${data.payload}\",\"${data.created_at}\"")
        writer.flush()
        writer.close()
        
        return file.absolutePath
    }

    // Helper to show Toast on UI Thread
    private suspend fun showToast(msg: String) {
        withContext(Dispatchers.Main) {
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }
}
