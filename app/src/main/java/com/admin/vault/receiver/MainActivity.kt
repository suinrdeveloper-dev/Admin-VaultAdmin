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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Timer
import java.util.TimerTask

class MainActivity : AppCompatActivity() {
    private val syncManager by lazy { SyncManager(this) }
    private lateinit var adapter: MessageAdapter
    
    // Search aur Normal data ke beech conflict rokne ke liye
    private var dataJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1. Permission Check (Loud Mode)
        checkPermissions()

        val recycler = findViewById<RecyclerView>(R.id.recyclerView)
        val search = findViewById<EditText>(R.id.searchBox)
        
        adapter = MessageAdapter()
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        // 2. Pehli baar Data Load karo
        observeData("")

        // 3. Search Logic
        search.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim()
                observeData(query) // Naya query pass karo
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // 4. Auto-Sync Loop (Vacuum)
        Timer().schedule(object : TimerTask() {
            override fun run() {
                Log.d("VaultUI", "⚡ Triggering Sync Manager...")
                syncManager.executeVacuum()
            }
        }, 0, 10000)
    }

    private fun observeData(query: String) {
        // Purana job cancel karo taki flicker na ho
        dataJob?.cancel()
        
        dataJob = lifecycleScope.launch {
            val dao = AppDatabase.getDatabase(this@MainActivity).messageDao()
            val flow = if (query.isEmpty()) dao.getAllMessages() else dao.searchMessages(query)

            flow.collectLatest { list ->
                // 🔥 LOUD LOGGING: Yahan pata chalega ki data UI tak pahuncha ya nahi
                Log.d("VaultUI", "Update received. Item count: ${list.size}")
                
                adapter.submitList(list)

                // Debugging ke liye Toast (Baad mein hata dena)
                if (list.isNotEmpty()) {
                    // Toast.makeText(this@MainActivity, "Showing ${list.size} messages", Toast.LENGTH_SHORT).show()
                } else {
                    Log.w("VaultUI", "⚠️ Database is empty or Search not found")
                }
            }
        }
    }

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Toast.makeText(this, "🔴 PERMISSION DENIED! App won't work.", Toast.LENGTH_LONG).show()
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            }
        }
    }
}
