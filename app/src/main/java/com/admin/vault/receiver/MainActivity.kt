package com.admin.vault.receiver

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.admin.vault.receiver.data.AppDatabase
import com.admin.vault.receiver.ui.MessageAdapter
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    // Lazy initialization for SyncManager
    private val syncManager by lazy { SyncManager(this) }
    private lateinit var adapter: MessageAdapter
    
    // Job to handle search/data flow cancellation (Memory Leak prevention)
    private var dataJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1. Permission Check (User ko force karna zaruri hai)
        checkPermissions()

        // 2. Setup UI
        val recycler = findViewById<RecyclerView>(R.id.recyclerView)
        val search = findViewById<EditText>(R.id.searchBox)
        
        adapter = MessageAdapter()
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        // 3. Start Data Observation (Pehle saara data dikhao)
        observeData("")

        // 4. Search Listener
        search.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim()
                observeData(query) // Query badalte hi naya data mango
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // 5. Start Auto-Sync (Coroutine way - Safer than Timer)
        startAutoSyncLoop()
    }

    private fun observeData(query: String) {
        // Purana job cancel karo taaki purana search result naye ko overwrite na kare
        dataJob?.cancel()
        
        dataJob = lifecycleScope.launch {
            try {
                val dao = AppDatabase.getDatabase(this@MainActivity).messageDao()
                
                // Agar query khali hai to sab dikhao, warna search karo
                val flow = if (query.isEmpty()) dao.getAllMessages() else dao.searchMessages(query)

                flow.collectLatest { list ->
                    // 🔥 LOUD LOGGING: Data UI tak pahuncha ya nahi
                    Log.d("VaultUI", "📊 UI Update: Received ${list.size} messages")
                    
                    if (list.isEmpty()) {
                        Log.w("VaultUI", "⚠️ List is empty. Either DB is empty or Search failed.")
                    }
                    
                    adapter.submitList(list)
                }
            } catch (e: Exception) {
                Log.e("VaultUI", "❌ Database Error: ${e.message}")
            }
        }
    }

    private fun startAutoSyncLoop() {
        lifecycleScope.launch {
            // 'isActive' check karta hai ki App khula hai ya nahi (Crash Proof)
            while (isActive) {
                Log.d("VaultUI", "🔄 Auto-Sync Cycle Starting...")
                try {
                    syncManager.executeVacuum()
                } catch (e: Exception) {
                    Log.e("VaultUI", "❌ Sync Manager Crash: ${e.message}")
                }
                // 10 Second ka break (Timer ki jagah Delay use kiya hai)
                delay(10000)
            }
        }
    }

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Toast.makeText(this, "🔴 ACTIONS REQUIRED: Grant 'All Files Access' to save backups!", Toast.LENGTH_LONG).show()
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    intent.data = Uri.parse("package:$packageName")
                    startActivity(intent)
                } catch (e: Exception) {
                    Log.e("VaultUI", "Permission Intent Error: ${e.message}")
                }
            }
        }
    }
}
