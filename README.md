# Vault Admin (Receiver App)

This is the Admin/Receiver application built following "The Vacuum Protocol".

## Features
- **Fetch**: Automatically pulls data from Supabase every 10 seconds.
- **Store & Export**: Saves data to a local Room database and exports individual CSV files to `Documents/SecureVault`.
- **Delete (Vacuum)**: Deletes the data from Supabase once successfully saved locally to keep the server clean.
- **Search**: Offline search capability through the local database.

## Setup Instructions
1. Open this project in Android Studio.
2. Open `app/src/main/java/com/admin/vault/receiver/Config.kt`.
3. Replace `YOUR_SUPABASE_URL` and `YOUR_SUPABASE_KEY` with your actual Supabase credentials.
4. Build and Run the app.

## Permissions
The app requires the following permissions:
- Internet access for Supabase sync.
- Storage permissions for saving CSV files.
- Note: On Android 11+, you may need to manually grant "All Files Access" for the app to write to the Documents folder.

## Architecture
- **Data Layer**: Room Database (`MessageEntity`, `MessageDao`, `AppDatabase`).
- **Sync Logic**: `SyncManager` handles Supabase interaction and CSV generation.
- **UI**: `MainActivity` with `RecyclerView` and `MessageAdapter` for live updates and searching.
