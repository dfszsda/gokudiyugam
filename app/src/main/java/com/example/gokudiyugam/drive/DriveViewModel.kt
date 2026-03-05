@file:Suppress("DEPRECATION")

package com.example.gokudiyugam.drive

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gokudiyugam.model.MediaItem
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File
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
    var driveHelper by mutableStateOf<DriveHelper?>(null)
    var isFetching by mutableStateOf(false)
    var isUploading by mutableStateOf(false)
    
    val currentCategoryItems = mutableStateListOf<MediaItem>()
    val driveFiles = mutableStateListOf<File>()
    
    private val db = FirebaseFirestore.getInstance()
    private val rtdb = FirebaseDatabase.getInstance().getReference("mediadata")
    private val auth = FirebaseAuth.getInstance()
    
    private var categoryListener: ListenerRegistration? = null

    private val FOLDER_IDS = mapOf(
        "photo" to "1_nFilCWknua9FaoDTGQHcHldzDMvBgez",
        "video" to "1JYyyW1TGKiXqo3XDiIMKY-hMMzLpNMo7",
        "audio" to "1LgShVKMO1r98aP2z_y8AVUnAkcmyUXXA",
        "doc"   to "1D-GKrMOs-tT_4hFKEmhJS6nCNC-fvjAb",
        "sabhatimetable"  to "1eRn1W9htFNTR-Dcc5kLHsseUj2fPG_hl"
    )

    fun checkExistingSignIn(context: Context) {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        if (account != null) {
            setupDriveService(context, account)
        }
    }

    private fun setupDriveService(context: Context, account: GoogleSignInAccount) {
        val service: Drive = DriveHelper.getDriveService(context, account)
        driveHelper = DriveHelper(service)
        authorizeFolders(context)
        fetchFiles()
    }

    fun handleSignInResult(context: Context, result: ActivityResult) {
        val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        if (task.isSuccessful) {
            task.result?.let { setupDriveService(context, it) }
        }
    }

    fun authorizeFolders(context: Context) {
        val helper = driveHelper ?: return
        viewModelScope.launch {
            try {
                FOLDER_IDS.values.forEach { folderId ->
                    helper.makeFolderWritable(folderId)
                }
            } catch (e: Exception) {
                Log.e("DriveViewModel", "Auth folder error: ${e.message}")
            }
        }
    }

    fun uploadToCategory(context: Context, uri: Uri, title: String, driveType: String, category: String) {
        val helper = driveHelper ?: return
        val userId = auth.currentUser?.uid ?: "anonymous"
        isUploading = true

        viewModelScope.launch {
            try {
                val folderId = FOLDER_IDS[driveType.lowercase()] ?: FOLDER_IDS["doc"]

                val driveFileId = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
                        val fileName = "${category.lowercase()}_${System.currentTimeMillis()}"
                        helper.createFile(fileName, mimeType, inputStream, folderId)
                    }
                }

                if (driveFileId != null) {
                    helper.makeFilePublic(driveFileId)
                    val finalUrl = "https://drive.google.com/uc?id=$driveFileId"
                    val itemId = UUID.randomUUID().toString()

                    // Firestore Data
                    val firestoreMap = hashMapOf(
                        "id" to itemId,
                        "title" to title,
                        "url" to finalUrl,
                        "type" to category.lowercase(),
                        "uploadedBy" to userId,
                        "timestamp" to FieldValue.serverTimestamp()
                    )
                    
                    // RTDB Data
                    val rtdbMap = hashMapOf(
                        "id" to itemId,
                        "title" to title,
                        "url" to finalUrl,
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
                }
            } catch (e: Exception) {
                Log.e("DriveViewModel", "Drive Error: ${e.message}")
            } finally {
                isUploading = false
            }
        }
    }

    fun fetchFiles() {
        val helper = driveHelper ?: return
        isFetching = true
        viewModelScope.launch {
            try {
                val files: List<File> = helper.queryFiles()
                driveFiles.clear()
                driveFiles.addAll(files)
            } catch (e: Exception) {
                Log.e("DriveViewModel", "Fetch error: ${e.message}")
            } finally {
                isFetching = false
            }
        }
    }

    fun fetchCategoryItems(category: String) {
        categoryListener?.remove()
        isFetching = true
        
        categoryListener = db.collection("mediadata")
            .whereEqualTo("type", category.lowercase())
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("DriveViewModel", "Listen failed: ${e.message}")
                    isFetching = false
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val items = snapshot.toObjects(MediaItem::class.java)
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
