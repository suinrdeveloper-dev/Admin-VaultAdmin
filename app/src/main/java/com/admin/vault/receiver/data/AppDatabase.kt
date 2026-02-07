package com.admin.vault.receiver.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// ✅ FIX 1: Version ko '1' se '2' kiya kyunki humne MessageEntity change kiya hai
@Database(entities = [MessageEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "vault_db"
                )
                // ✅ FIX 2: Ye line sabse zaruri hai!
                // Agar purana database match nahi kiya, to ye crash hone ki jagah naya bana dega.
                .fallbackToDestructiveMigration()
                .build()
                
                INSTANCE = instance
                instance
            }
        }
    }
}
