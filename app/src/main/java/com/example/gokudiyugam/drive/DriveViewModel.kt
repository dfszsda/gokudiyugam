@file:Suppress("DEPRECATION")

package com.example.gokudiyugam.drive

import android.accounts.Account
import android.content.Context
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gokudiyugam.PreferenceManager
import com.example.gokudiyugam.ai.AIService
import com.example.gokudiyugam.model.MediaItem
import com.example.gokudiyugam.service.NotificationHelper
import com.google.android.gms.tasks.Tasks
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.InputStreamContent
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import com.google.api.services.drive.model.Permission
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

class DriveViewModel : ViewModel() {
    var isFetching by mutableStateOf(false)
    var isUploading by mutableStateOf(false)
    
    val currentCategoryItems = mutableStateListOf<MediaItem>()
    
    private val db = FirebaseFirestore.getInstance("mediadata")
    private val rtdb = FirebaseDatabase.getInstance().getReference("mediadata")
    
    private var categoryListener: ListenerRegistration? = null

    var selectedGoogleAccount by mutableStateOf<Account?>(null)

    private fun getCurrentUser() = FirebaseAuth.getInstance().currentUser

    private fun getFileExtension(context: Context, uri: Uri): String {
        return MimeTypeMap.getSingleton().getExtensionFromMimeType(context.contentResolver.getType(uri)) ?: "file"
    }

    fun uploadToCategory(
        context: Context, 
        uri: Uri, 
        title: String, 
        driveType: String, 
        category: String,
        canDownload: Boolean = true
    ) {
        uploadFestivalItem(context, uri, title, category, null, canDownload, null)
    }

    fun uploadFestivalItem(
        context: Context,
        uri: Uri,
        title: String,
        category: String,
        folderId: String? = null,
        canDownload: Boolean = true,
        expiryTimestamp: Long? = null
    ) {
        val currentUser = getCurrentUser()
        val googleAccount = selectedGoogleAccount

        if (currentUser == null || currentUser.isAnonymous) {
            Toast.makeText(context, "ગેસ્ટ યુઝર અપલોડ ન કરી શકે. મહેરબાની કરીને લોગિન કરો.", Toast.LENGTH_LONG).show()
            return
        }

        if (googleAccount == null) {
            Toast.makeText(context, "Google Drive access required. Please grant permission.", Toast.LENGTH_LONG).show()
            return
        }

        isUploading = true

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Generate translations
                val preferenceManager = PreferenceManager(context)
                val translations = AIService.getTranslatedText(title, preferenceManager)

                val credential = GoogleAccountCredential.usingOAuth2(
                    context, listOf(DriveScopes.DRIVE_FILE)
                ).setSelectedAccount(googleAccount)

                val driveService = Drive.Builder(
                    AndroidHttp.newCompatibleTransport(),
                    GsonFactory.getDefaultInstance(),
                    credential
                ).setApplicationName("Gokudiyugam").build()

                val extension = getFileExtension(context, uri)
                val fileMetadata = File().apply { 
                    name = "$title.$extension"
                    if (folderId != null) {
                        parents = listOf(folderId)
                    }
                }

                val contentResolver = context.contentResolver
                val inputStream = contentResolver.openInputStream(uri) ?: throw Exception("Failed to open URI")
                val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"
                val mediaContent = InputStreamContent(mimeType, inputStream)

                val driveFile = driveService.files().create(fileMetadata, mediaContent)
                    .setFields("id, webViewLink, webContentLink")
                    .execute()

                val permission = Permission().apply {
                    type = "anyone"
                    role = "reader"
                }
                driveService.permissions().create(driveFile.id, permission).execute()

                val downloadUrl = "https://drive.google.com/uc?export=download&id=${driveFile.id}"
                
                val itemId = UUID.randomUUID().toString()
                val mediaTypeStr = when {
                    mimeType.startsWith("image/") -> "photo"
                    mimeType.startsWith("video/") -> "video"
                    mimeType.startsWith("audio/") -> "audio"
                    mimeType == "application/pdf" -> "pdf"
                    mimeType.contains("word") || mimeType.contains("officedocument") -> "doc"
                    else -> "file"
                }

                val firestoreMap = hashMapOf(
                    "id" to itemId,
                    "title" to title,
                    "titleEn" to title,
                    "titleGu" to (translations["gu"] ?: title),
                    "titleHi" to (translations["hi"] ?: title),
                    "url" to downloadUrl,
                    "type" to category.lowercase(),
                    "mediaType" to mediaTypeStr,
                    "uploadedBy" to currentUser.uid,
                    "canDownload" to canDownload,
                    "expiryTimestamp" to expiryTimestamp,
                    "timestamp" to FieldValue.serverTimestamp()
                )
                
                val rtdbMap = hashMapOf(
                    "id" to itemId,
                    "title" to title,
                    "titleEn" to title,
                    "titleGu" to (translations["gu"] ?: title),
                    "titleHi" to (translations["hi"] ?: title),
                    "url" to downloadUrl,
                    "type" to category.lowercase(),
                    "mediaType" to mediaTypeStr,
                    "uploadedBy" to currentUser.uid,
                    "canDownload" to canDownload,
                    "expiryTimestamp" to expiryTimestamp,
                    "timestamp" to System.currentTimeMillis()
                )

                val firestoreTask = db.collection("mediadata").document(itemId).set(firestoreMap)
                val rtdbTask = rtdb.child(itemId).setValue(rtdbMap)
                
                Tasks.whenAllComplete(firestoreTask, rtdbTask).await()
                
                sendAutoNotification(category, title)
                
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Uploaded Successfully!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("DriveViewModel", "Upload Error: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Upload Failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            } finally {
                withContext(Dispatchers.Main) { isUploading = false }
            }
        }
    }

    fun postYouTubeLink(context: Context, title: String, url: String, category: String, canDownload: Boolean = true) {
        val currentUser = getCurrentUser()
        if (currentUser == null || currentUser.isAnonymous) {
            Toast.makeText(context, "ગેસ્ટ યુઝર લિંક પોસ્ટ ન કરી શકે.", Toast.LENGTH_LONG).show()
            return
        }
        
        viewModelScope.launch {
            try {
                val preferenceManager = PreferenceManager(context)
                val translations = AIService.getTranslatedText(title, preferenceManager)

                val itemId = UUID.randomUUID().toString()
                val mediaItem = hashMapOf(
                    "id" to itemId,
                    "title" to title,
                    "titleEn" to title,
                    "titleGu" to (translations["gu"] ?: title),
                    "titleHi" to (translations["hi"] ?: title),
                    "url" to url,
                    "type" to category.lowercase(),
                    "mediaType" to "video",
                    "uploadedBy" to currentUser.uid,
                    "canDownload" to canDownload,
                    "timestamp" to FieldValue.serverTimestamp()
                )

                db.collection("mediadata").document(itemId).set(mediaItem).await()
                rtdb.child(itemId).setValue(mediaItem).await()
                
                sendAutoNotification(category, title)
                
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Posted Successfully!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("DriveViewModel", "Post Error: ${e.message}")
            }
        }
    }

    private fun sendAutoNotification(category: String, title: String) {
        val (notifTitle, notifBody) = when (category.lowercase()) {
            "guruhari_darshan" -> "ગુરુ હરિ દર્શન" to "નવું ગુરુ હરિ દર્શન વિડિયો મુકાયેલ છે: $title"
            "mandir_darshan" -> "મંદિર દર્શન" to "આજનું સુંદર મંદિર દર્શન ઉપલબ્ધ છે: $title"
            "festivals" -> "ઉત્સવ અપડેટ" to "ઉત્સવ સેક્શનમાં નવી અપડેટ: $title"
            "sabha_saar" -> "સભા સાર" to "નવો સભા સાર મુકાયેલ છે: $title"
            "news" -> "સત્સંગ સમાચાર" to "નવા સમાચાર: $title"
            else -> "Gokudiyugam Update" to "નવી અપડેટ: $title"
        }
        NotificationHelper.sendNotificationToTopic("all_users", notifTitle, notifBody)
    }

    fun fetchCategoryItems(category: String) {
        categoryListener?.remove()
        isFetching = true
        
        cleanupExpiredItems()

        categoryListener = db.collection("mediadata")
            .whereIn("type", listOf(category.lowercase(), "youtube"))
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    isFetching = false
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val items = snapshot.toObjects(MediaItem::class.java)
                        .filter { it.type == category.lowercase() || it.type == "youtube" }
                        .sortedByDescending { it.timestamp?.toDate()?.time ?: 0L }
                    
                    currentCategoryItems.clear()
                    currentCategoryItems.addAll(items)
                }
                isFetching = false
            }
    }

    private fun cleanupExpiredItems() {
        val currentTime = System.currentTimeMillis()
        db.collection("mediadata")
            .whereLessThan("expiryTimestamp", currentTime)
            .get()
            .addOnSuccessListener { snapshot ->
                for (doc in snapshot.documents) {
                    val id = doc.id
                    doc.reference.delete()
                    rtdb.child(id).removeValue()
                }
            }
    }

    fun deleteItem(context: Context, item: MediaItem, category: String) {
        viewModelScope.launch {
            try {
                db.collection("mediadata").document(item.id).delete().await()
                rtdb.child(item.id).removeValue().await()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Item deleted", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Delete failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        categoryListener?.remove()
    }
}
