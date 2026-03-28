package com.example.gokudiyugam.ui.screens

import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.gokudiyugam.model.Kirtan
import com.example.gokudiyugam.model.Playlist
import com.example.gokudiyugam.service.KirtanAudioService
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class KirtanViewModel : ViewModel() {
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null

    var isPlaying by mutableStateOf(false)
    var currentKirtan by mutableStateOf<Kirtan?>(null)
    var playbackPosition by mutableStateOf(0L)
    var duration by mutableStateOf(0L)
    var errorMessage by mutableStateOf<String?>(null)
    
    val sharedKirtans = mutableStateListOf<Kirtan>()
    val dynamicCategories = mutableStateListOf<String>()
    val userPlaylists = mutableStateListOf<Playlist>()
    
    private var sharedKirtansListener: ListenerRegistration? = null
    private var categoriesListener: ListenerRegistration? = null
    private var playlistsListener: ListenerRegistration? = null

    // Helper to consistently get the correct Firestore instance
    private fun getFirestore() = FirebaseFirestore.getInstance("mediadata")

    fun isControllerInitialized(): Boolean = mediaController != null

    fun initController(context: Context) {
        if (isControllerInitialized()) return
        
        try {
            val sessionToken = SessionToken(context, ComponentName(context, KirtanAudioService::class.java))
            controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
            controllerFuture?.addListener({
                try {
                    mediaController = controllerFuture?.get()
                    mediaController?.addListener(object : Player.Listener {
                        override fun onIsPlayingChanged(playing: Boolean) {
                            isPlaying = playing
                        }
                        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                            mediaItem?.let { item ->
                                sharedKirtans.find { it.id == item.mediaId }?.let {
                                    currentKirtan = it
                                }
                            }
                        }
                    })
                    startPositionUpdater()
                } catch (e: Exception) {
                    Log.e("KirtanViewModel", "Controller Future error: ${e.message}")
                }
            }, MoreExecutors.directExecutor())
        } catch (e: Exception) {
            Log.e("KirtanViewModel", "Init Controller error: ${e.message}")
        }
        
        fetchDynamicCategories()
        fetchUserPlaylists()
    }

    private fun startPositionUpdater() {
        viewModelScope.launch {
            while (true) {
                mediaController?.let {
                    playbackPosition = it.currentPosition
                    duration = it.duration
                }
                delay(1000)
            }
        }
    }

    private fun fetchDynamicCategories() {
        val db = getFirestore()
        categoriesListener = db.collection("kirtan_categories")
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                if (snapshot != null) {
                    val cats = snapshot.documents.mapNotNull { it.getString("name") }
                    dynamicCategories.clear()
                    dynamicCategories.addAll(cats)
                }
            }
    }

    private fun fetchUserPlaylists() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = getFirestore()
        playlistsListener = db.collection("playlists")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, _ ->
                val lists = snapshot?.documents?.mapNotNull { it.toObject(Playlist::class.java)?.copy(id = it.id) } ?: emptyList()
                userPlaylists.clear()
                userPlaylists.addAll(lists)
            }
    }

    fun addCategory(name: String) {
        viewModelScope.launch {
            try {
                val db = getFirestore()
                db.collection("kirtan_categories").document(name.lowercase()).set(mapOf("name" to name)).await()
            } catch (e: Exception) {
                Log.e("KirtanViewModel", "Add category error: ${e.message}")
            }
        }
    }

    fun removeCategory(name: String) {
        viewModelScope.launch {
            try {
                val db = getFirestore()
                db.collection("kirtan_categories").document(name.lowercase()).delete().await()
            } catch (e: Exception) {
                Log.e("KirtanViewModel", "Remove category error: ${e.message}")
            }
        }
    }

    // --- Playlist Management Restored ---
    fun addKirtanToPlaylist(playlistId: String, kirtanId: String) {
        viewModelScope.launch {
            try {
                val db = getFirestore()
                db.collection("playlists").document(playlistId)
                    .update("kirtanIds", FieldValue.arrayUnion(kirtanId)).await()
            } catch (e: Exception) {
                Log.e("KirtanViewModel", "Add to playlist error: ${e.message}")
            }
        }
    }

    fun createPlaylistAndAddKirtan(name: String, kirtanId: String) {
        viewModelScope.launch {
            try {
                val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
                val db = getFirestore()
                val playlist = Playlist(name = name, userId = userId, kirtanIds = listOf(kirtanId))
                db.collection("playlists").add(playlist).await()
            } catch (e: Exception) {
                Log.e("KirtanViewModel", "Create playlist error: ${e.message}")
            }
        }
    }

    fun fetchKirtansByCategory(category: String) {
        sharedKirtansListener?.remove()
        sharedKirtans.clear()
        val db = getFirestore()
        var query = db.collection("mediadata").whereEqualTo("mediaType", "audio")
        
        // Handle categories and translations
        val isAllKirtans = category == "All Kirtans" || category == "બધા કીર્તનો" || category == "audio"
        
        if (!isAllKirtans) {
            query = query.whereEqualTo("type", category.lowercase())
        }
        
        sharedKirtansListener = query
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("KirtanViewModel", "Fetch Kirtans Error: ${e.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val kirtans = snapshot.toObjects(com.example.gokudiyugam.model.MediaItem::class.java).map { item ->
                        Kirtan(id = item.id, title = item.title, category = category, fileUri = item.url, lyrics = item.lyrics)
                    }
                    sharedKirtans.clear()
                    sharedKirtans.addAll(kirtans)
                }
            }
    }

    fun playKirtan(context: Context, kirtan: Kirtan, playlist: List<Kirtan> = emptyList()) {
        if (mediaController?.currentMediaItem?.mediaId == kirtan.id) {
            mediaController?.play()
            currentKirtan = kirtan
            return
        }
        currentKirtan = kirtan
        mediaController?.let { controller ->
            controller.clearMediaItems()
            if (playlist.isNotEmpty()) {
                val mediaItems = playlist.map { item ->
                    MediaItem.Builder().setMediaId(item.id).setUri(android.net.Uri.parse(item.fileUri ?: "")).build()
                }
                controller.setMediaItems(mediaItems)
                val index = playlist.indexOfFirst { it.id == kirtan.id }
                if (index != -1) controller.seekTo(index, 0L)
            } else {
                controller.setMediaItem(MediaItem.Builder().setMediaId(kirtan.id).setUri(android.net.Uri.parse(kirtan.fileUri ?: "")).build())
            }
            controller.prepare()
            controller.play()
        }
    }

    fun togglePlayPause() {
        mediaController?.let { if (it.isPlaying) it.pause() else it.play() }
    }

    fun playNext() = mediaController?.seekToNextMediaItem()
    
    fun playPrevious() = mediaController?.seekToPreviousMediaItem()

    fun seekTo(position: Long) {
        mediaController?.seekTo(position)
    }

    fun skipForward() {
        mediaController?.let { it.seekTo((it.currentPosition + 10000).coerceAtMost(it.duration)) }
    }

    fun skipBackward() {
        mediaController?.let { it.seekTo((it.currentPosition - 5000).coerceAtLeast(0L)) }
    }

    override fun onCleared() {
        super.onCleared()
        controllerFuture?.let { MediaController.releaseFuture(it) }
        sharedKirtansListener?.remove()
        categoriesListener?.remove()
        playlistsListener?.remove()
    }
}
