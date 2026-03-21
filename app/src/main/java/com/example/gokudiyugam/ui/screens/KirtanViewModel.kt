package com.example.gokudiyugam.ui.screens

import android.content.ComponentName
import android.content.Context
import android.util.Log
import android.widget.Toast
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
import com.example.gokudiyugam.data.KirtanRepository
import com.example.gokudiyugam.model.Kirtan
import com.example.gokudiyugam.service.KirtanAudioService
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
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
    
    val favoriteKirtans = mutableStateListOf<Kirtan>()
    val sharedKirtans = mutableStateListOf<Kirtan>()
    val dynamicCategories = mutableStateListOf<String>()
    
    private var sharedKirtansListener: ListenerRegistration? = null
    private var categoriesListener: ListenerRegistration? = null

    fun isControllerInitialized(): Boolean = mediaController != null

    fun initController(context: Context) {
        if (isControllerInitialized()) return
        
        val sessionToken = SessionToken(context, ComponentName(context, KirtanAudioService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            try {
                mediaController = controllerFuture?.get()
                mediaController?.addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(playing: Boolean) {
                        isPlaying = playing
                    }

                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                        errorMessage = "Playback Error: ${error.localizedMessage}"
                        Log.e("KirtanViewModel", "Player error", error)
                    }

                    override fun onPlaybackStateChanged(state: Int) {
                        if (state == Player.STATE_READY) {
                            errorMessage = null
                        }
                    }
                })
                startPositionUpdater()
            } catch (e: Exception) {
                Log.e("KirtanViewModel", "Error initializing controller: ${e.message}")
            }
        }, MoreExecutors.directExecutor())
        
        loadFavorites(context)
        fetchDynamicCategories()
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
        val db = FirebaseFirestore.getInstance("mediadata")
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

    fun addCategory(name: String) {
        viewModelScope.launch {
            val db = FirebaseFirestore.getInstance("mediadata")
            db.collection("kirtan_categories").document(name.lowercase()).set(mapOf("name" to name)).await()
        }
    }

    fun removeCategory(name: String) {
        viewModelScope.launch {
            val db = FirebaseFirestore.getInstance("mediadata")
            db.collection("kirtan_categories").document(name.lowercase()).delete().await()
        }
    }

    fun fetchKirtansByCategory(category: String) {
        sharedKirtansListener?.remove()
        sharedKirtans.clear()

        // If category is "Favorite", we don't fetch from Firestore here
        // as Favorites are handled locally.
        if (category == "Favorite" || category == "ફેવરિટ" || category == "पસંદગીના") {
            return
        }
        
        val db = FirebaseFirestore.getInstance("mediadata")
        var query = db.collection("mediadata")
            .whereEqualTo("mediaType", "audio")

        if (category != "All Kirtans" && category != "બધા કીર્તનો" && category != "audio") {
            query = query.whereEqualTo("type", category.lowercase())
        } else if (category == "audio") {
             query = query.whereEqualTo("type", "audio")
        }

        sharedKirtansListener = query
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("KirtanViewModel", "Error fetching kirtans: ${e.message}")
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    val items = snapshot.toObjects(com.example.gokudiyugam.model.MediaItem::class.java)
                    val kirtans = items.map { item ->
                        Kirtan(
                            id = item.id, 
                            title = item.title, 
                            category = category, 
                            fileUri = item.url
                        )
                    }
                    sharedKirtans.clear()
                    sharedKirtans.addAll(kirtans)
                }
            }
    }

    fun playKirtan(context: Context, kirtan: Kirtan, playlist: List<Kirtan> = emptyList()) {
        errorMessage = null
        currentKirtan = kirtan
        mediaController?.let { controller ->
            controller.clearMediaItems()
            if (playlist.isNotEmpty()) {
                val mediaItems = playlist.map { item ->
                    MediaItem.Builder()
                        .setMediaId(item.id)
                        .setUri(if (!item.fileUri.isNullOrEmpty()) android.net.Uri.parse(item.fileUri) 
                                else android.net.Uri.parse("android.resource://${context.packageName}/${item.resourceId}"))
                        .build()
                }
                controller.setMediaItems(mediaItems)
                val index = playlist.indexOfFirst { it.id == kirtan.id }
                if (index != -1) controller.seekTo(index, 0L)
            } else {
                val mediaItem = MediaItem.Builder()
                    .setMediaId(kirtan.id)
                    .setUri(if (!kirtan.fileUri.isNullOrEmpty()) android.net.Uri.parse(kirtan.fileUri)
                            else android.net.Uri.parse("android.resource://${context.packageName}/${kirtan.resourceId}"))
                    .build()
                controller.setMediaItem(mediaItem)
            }
            controller.prepare()
            controller.play()
        } ?: run {
            Toast.makeText(context, "Player not initialized yet. Please try again.", Toast.LENGTH_SHORT).show()
            initController(context)
        }
    }

    fun togglePlayPause() {
        mediaController?.let { if (it.isPlaying) it.pause() else it.play() }
    }

    fun seekTo(position: Long) {
        mediaController?.seekTo(position)
    }

    fun skipForward() {
        mediaController?.let { it.seekTo((it.currentPosition + 10000).coerceAtMost(it.duration)) }
    }

    fun skipBackward() {
        mediaController?.let { it.seekTo((it.currentPosition - 5000).coerceAtLeast(0L)) }
    }

    fun playNext() = mediaController?.seekToNextMediaItem()
    fun playPrevious() = mediaController?.seekToPreviousMediaItem()

    fun isFavorite(kirtan: Kirtan): Boolean = favoriteKirtans.any { it.id == kirtan.id }

    fun toggleFavorite(context: Context, kirtan: Kirtan) {
        if (isFavorite(kirtan)) favoriteKirtans.removeAll { it.id == kirtan.id }
        else favoriteKirtans.add(kirtan)
        KirtanRepository.saveFavoriteKirtans(context, favoriteKirtans.toList())
    }

    private fun loadFavorites(context: Context) {
        favoriteKirtans.clear()
        favoriteKirtans.addAll(KirtanRepository.getFavoriteKirtans(context))
    }

    override fun onCleared() {
        super.onCleared()
        controllerFuture?.let { MediaController.releaseFuture(it) }
        sharedKirtansListener?.remove()
        categoriesListener?.remove()
    }
}
