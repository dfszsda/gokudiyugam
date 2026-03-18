package com.example.gokudiyugam.ui.screens

import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gokudiyugam.model.MediaItem
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

class MediaViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance("mediadata")
    private val rtdb = FirebaseDatabase.getInstance().getReference("mediadata")
    private val storage = FirebaseStorage.getInstance()
    private val auth = FirebaseAuth.getInstance()

    var isUploading by mutableStateOf(false)
    val mediaList = mutableStateListOf<MediaItem>()
    
    // ૪. ડેટા ડિસ્પ્લે કરવો (બધા યુઝર્સ માટે)
    fun fetchMediaData() {
        db.collection("mediadata")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("MediaViewModel", "Firestore Error: ${e.message}")
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    val items = snapshot.toObjects(MediaItem::class.java)
                    val sortedItems = items.sortedByDescending { it.timestamp?.toDate()?.time ?: 0L }
                    mediaList.clear()
                    mediaList.addAll(sortedItems)
                }
            }
    }

    // ૨. Firebase Storage માં ફાઇલ અપલોડ અને ૩. Firestore/RTDB માં ફાઇલની વિગતો સેવ કરવી
    fun uploadFile(uri: Uri, title: String, type: String) {
        val userId = auth.currentUser?.uid ?: "anonymous"
        isUploading = true

        viewModelScope.launch {
            try {
                val fileName = "${UUID.randomUUID()}_$title"
                val storagePath = "uploads/$type/$fileName"
                val storageRef = storage.reference.child(storagePath)

                storageRef.putFile(uri).await()
                val downloadUrl = storageRef.downloadUrl.await().toString()

                val mediaItem = MediaItem(
                    id = UUID.randomUUID().toString(),
                    title = title,
                    url = downloadUrl,
                    type = type,
                    uploadedBy = userId,
                    timestamp = Timestamp.now()
                )

                db.collection("mediadata").document(mediaItem.id).set(mediaItem).await()
                rtdb.child(mediaItem.id).setValue(mediaItem).await()

                isUploading = false
            } catch (e: Exception) {
                Log.e("MediaViewModel", "Upload Failed: ${e.message}")
                isUploading = false
            }
        }
    }

    // New: Post YouTube Link
    fun postYouTubeLink(title: String, url: String) {
        val userId = auth.currentUser?.uid ?: "anonymous"
        viewModelScope.launch {
            try {
                val mediaItem = MediaItem(
                    id = UUID.randomUUID().toString(),
                    title = title,
                    url = url,
                    type = "youtube",
                    uploadedBy = userId,
                    timestamp = Timestamp.now()
                )
                db.collection("mediadata").document(mediaItem.id).set(mediaItem).await()
                rtdb.child(mediaItem.id).setValue(mediaItem).await()
            } catch (e: Exception) {
                Log.e("MediaViewModel", "YouTube Link Post Failed: ${e.message}")
            }
        }
    }

    // New: Re-post an existing item from Media Library to a specific screen
    fun repostItem(item: MediaItem, newCategory: String) {
        viewModelScope.launch {
            try {
                val newItem = item.copy(
                    id = UUID.randomUUID().toString(),
                    type = newCategory,
                    timestamp = Timestamp.now()
                )
                db.collection("mediadata").document(newItem.id).set(newItem).await()
                rtdb.child(newItem.id).setValue(newItem).await()
            } catch (e: Exception) {
                Log.e("MediaViewModel", "Repost Failed: ${e.message}")
            }
        }
    }

    // New: Delete an item from Media Library and Storage
    fun deleteMediaItem(item: MediaItem) {
        viewModelScope.launch {
            try {
                // Delete from Firestore
                db.collection("mediadata").document(item.id).delete().await()
                
                // Delete from Realtime Database
                rtdb.child(item.id).removeValue().await()
                
                // Delete from Storage (Try to delete if it's a firebase storage URL)
                if (item.url.contains("firebasestorage.googleapis.com")) {
                    try {
                        val storageRef = storage.getReferenceFromUrl(item.url)
                        storageRef.delete().await()
                    } catch (e: Exception) {
                        Log.e("MediaViewModel", "Storage Delete Failed: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e("MediaViewModel", "Delete Failed: ${e.message}")
            }
        }
    }
}
