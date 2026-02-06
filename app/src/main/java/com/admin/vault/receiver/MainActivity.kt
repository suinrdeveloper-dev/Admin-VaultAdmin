package com.admin.vault.receiver

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.admin.vault.receiver.data.AppDatabase
import com.admin.vault.receiver.ui.MessageAdapter // ✅ FIX: Correct Import
import kotlinx.coroutines.launch
import java.util.Timer
import java.util.TimerTask

class MainActivity : AppCompatActivity() {
    private val syncManager by lazy { SyncManager(this) }
    
    // ✅ FIX: 'VaultAdapter' ko hatakar 'MessageAdapter' kiya
    private lateinit var adapter: MessageAdapter 

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkPermissions()

        val recycler = findViewById<RecyclerView>(R.id.recyclerView)
        val search = findViewById<EditText>(R.id.searchBox)
        
        adapter = MessageAdapter() // ✅ FIX: Initialized correct adapter
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        // Database Observer
        lifecycleScope.launch {
            AppDatabase.getDatabase(this@MainActivity).messageDao().getAllMessages().collect {
                adapter.submitList(it)
            }
        }

        // Search Logic
        search.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                lifecycleScope.launch {
                    AppDatabase.getDatabase(this@MainActivity).messageDao()
                        .searchMessages(s.toString())
                        .collect { adapter.submitList(it) }
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Auto-Sync Loop (Vacuum Protocol)
        Timer().schedule(object : TimerTask() {
            override fun run() {
                syncManager.executeVacuum()
            }
        }, 0, 10000)
    }

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            }
        }
    }
}
