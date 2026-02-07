package com.admin.vault.receiver.ui

import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.admin.vault.receiver.R
import com.admin.vault.receiver.data.MessageEntity

class MessageAdapter : ListAdapter<MessageEntity, MessageAdapter.MessageViewHolder>(MessageDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_message, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        // 🔥 LOUD LOGGING: Agar ye print hua, iska matlab Database aur SyncManager pass hain!
        // Agar ye print nahi hua, to problem DAO ya Database mein hai.
        val item = getItem(position)
        Log.d("VaultUI", "🎨 Binding Row $position: ${item.header}")
        
        holder.bind(item)
    }

    class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvAppName: TextView = itemView.findViewById(R.id.tvAppName)
        private val tvHeader: TextView = itemView.findViewById(R.id.tvHeader)
        private val tvPayload: TextView = itemView.findViewById(R.id.tvPayload)
        private val tvTime: TextView = itemView.findViewById(R.id.tvTime)

        fun bind(message: MessageEntity) {
            // 1. Data Set Karo (Empty string safety ke sath)
            tvAppName.text = if (message.source_app.isBlank()) "(Unknown App)" else message.source_app
            tvHeader.text = if (message.header.isBlank()) "(No Header)" else message.header
            tvPayload.text = if (message.payload.isBlank()) "(Empty Message)" else message.payload
            tvTime.text = message.timestamp

            // 2. 🛡️ VISIBILITY FORCE CHECK (Debugging ke liye)
            // Agar XML mein galti se White text hai, to yahan Black force kar rahe hain
            // Jab sab fix ho jaye to ye lines hata dena
            tvAppName.setTextColor(Color.BLACK)
            tvHeader.setTextColor(Color.BLACK)
            tvPayload.setTextColor(Color.DKGRAY)
            tvTime.setTextColor(Color.GRAY)
        }
    }

    class MessageDiffCallback : DiffUtil.ItemCallback<MessageEntity>() {
        override fun areItemsTheSame(oldItem: MessageEntity, newItem: MessageEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: MessageEntity, newItem: MessageEntity): Boolean {
            return oldItem == newItem
        }
    }
}
