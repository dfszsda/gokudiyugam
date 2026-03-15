@file:Suppress("DEPRECATION")

package com.example.gokudiyugam.drive

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gokudiyugam.model.MediaItem
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

class DriveViewModel : ViewModel() {
    var isFetching by mutableStateOf(false)
    var isUploading by mutableStateOf(false)
    
    val currentCategoryItems = mutableStateListOf<MediaItem>()
    
    private val db = FirebaseFirestore.getInstance()
    private val rtdb = FirebaseDatabase.getInstance().getReference("mediadata")
    private val storage = FirebaseStorage.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    private var categoryListener: ListenerRegistration? = null

    fun uploadToCategory(context: Context, uri: Uri, title: String, driveType: String, category: String) {
        val userId = auth.currentUser?.uid ?: "anonymous"
        isUploading = true

        viewModelScope.launch {
            try {
                val fileName = "${category.lowercase()}_${System.currentTimeMillis()}"
                val storagePath = "uploads/$category/$fileName"
                val storageRef = storage.reference.child(storagePath)

                // Upload to Firebase Storage
                storageRef.putFile(uri).await()
                
                // Success check before getting download URL
                val downloadUrl = storageRef.downloadUrl.await().toString()

                val itemId = UUID.randomUUID().toString()

                // Firestore Data
                val firestoreMap = hashMapOf(
                    "id" to itemId,
                    "title" to title,
                    "url" to downloadUrl,
                    "type" to category.lowercase(),
                    "uploadedBy" to userId,
                    "timestamp" to FieldValue.serverTimestamp()
                )
                
                // RTDB Data
                val rtdbMap = hashMapOf(
                    "id" to itemId,
                    "title" to title,
                    "url" to downloadUrl,
                    "type" to category.lowercase(),
                    "uploadedBy" to userId,
                    "timestamp" to System.currentTimeMillis()
                )

                try {
                    val firestoreTask = db.collection("mediadata").document(itemId).set(firestoreMap)
                    val rtdbTask = rtdb.child(itemId).setValue(rtdbMap)
                    
                    Tasks.whenAllComplete(firestoreTask, rtdbTask).await()
                    
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Uploaded & Posted Successfully!", Toast.LENGTH_SHORT).show()
                    }
                } catch (dbError: Exception) {
                    Log.e("DriveViewModel", "DB Error: ${dbError.message}")
                }
            } catch (e: Exception) {
                Log.e("DriveViewModel", "Upload Error: ${e.message}")
                withContext(Dispatchers.Main) {
                    val errorMsg = e.message ?: "Unknown error"
                    if (errorMsg.contains("Object does not exist")) {
                        Toast.makeText(context, "Upload Failed: File not found after upload. Check storage rules.", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "Upload Failed: $errorMsg", Toast.LENGTH_SHORT).show()
                    }
                }
            } finally {
                isUploading = false
            }
        }
    }

    // New: Post YouTube Link directly to a category
    fun postYouTubeLink(context: Context, title: String, url: String, category: String) {
        val userId = auth.currentUser?.uid ?: "anonymous"
        viewModelScope.launch {
            try {
                val itemId = UUID.randomUUID().toString()
                
                // Firestore Data
                val firestoreMap = hashMapOf(
                    "id" to itemId,
                    "title" to title,
                    "url" to url,
                    "type" to "youtube", // Keep type as youtube for identification
                    "category" to category.lowercase(), // Optionally store category
                    "uploadedBy" to userId,
                    "timestamp" to FieldValue.serverTimestamp()
                )
                
                // For showing in specific categories, we might want to set type to "youtube" 
                // but let fetch logic include it, or set type to category and have a flag.
                // Let's stick to the MediaDataScreen logic where type defines the bucket.
                
                val finalType = if (category == "all") "youtube" else category.lowercase()

                val mediaItem = hashMapOf(
                    "id" to itemId,
                    "title" to title,
                    "url" to url,
                    "type" to "youtube",
                    "uploadedBy" to userId,
                    "timestamp" to FieldValue.serverTimestamp()
                )

                db.collection("mediadata").document(itemId).set(mediaItem).await()
                rtdb.child(itemId).setValue(mediaItem).await()
                
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "YouTube Link Posted!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("DriveViewModel", "YouTube Post Error: ${e.message}")
            }
        }
    }

    fun fetchCategoryItems(category: String) {
        categoryListener?.remove()
        isFetching = true
        
        categoryListener = db.collection("mediadata")
            .whereIn("type", listOf(category.lowercase(), "youtube")) // Allow youtube links in category views if needed, or stick to type
            .addSnapshotListener { snapshot, e ->
                // Filtering logic can be more complex if youtube links are meant for specific screens
                if (e != null) {
                    Log.e("DriveViewModel", "Listen failed: ${e.message}")
                    isFetching = false
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val items = snapshot.toObjects(MediaItem::class.java)
                        .filter { it.type == category.lowercase() || it.type == "youtube" } // Simplified filter
                        .sortedByDescending { it.timestamp?.toDate()?.time ?: 0L }
                    
                    currentCategoryItems.clear()
                    currentCategoryItems.addAll(items)
                }
                isFetching = false
            }
    }

    fun uploadPublicPost(context: Context, uri: Uri, title: String, type: String) {
        uploadToCategory(context, uri, title, type, type)
    }

    override fun onCleared() {
        super.onCleared()
        categoryListener?.remove()
    }
}
