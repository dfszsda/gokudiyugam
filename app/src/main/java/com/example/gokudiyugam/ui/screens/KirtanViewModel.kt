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
import com.example.gokudiyugam.data.KirtanRepository
import com.example.gokudiyugam.model.Kirtan
import com.example.gokudiyugam.service.KirtanAudioService
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class KirtanViewModel : ViewModel() {
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null

    var isPlaying by mutableStateOf(false)
    var currentKirtan by mutableStateOf<Kirtan?>(null)
    var playbackPosition by mutableStateOf(0L)
    var duration by mutableStateOf(0L)
    
    val favoriteKirtans = mutableStateListOf<Kirtan>()
    val sharedKirtans = mutableStateListOf<Kirtan>()
    
    private var sharedKirtansListener: ListenerRegistration? = null

    fun isControllerInitialized(): Boolean = mediaController != null

    fun initController(context: Context) {
        if (isControllerInitialized()) return
        
        val sessionToken = SessionToken(context, ComponentName(context, KirtanAudioService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            mediaController = controllerFuture?.get()
            mediaController?.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) {
                    isPlaying = playing
                }
            })
            startPositionUpdater()
        }, MoreExecutors.directExecutor())
        
        loadFavorites(context)
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

    fun fetchSharedKirtans() {
        if (sharedKirtansListener != null) return
        
        val db = FirebaseFirestore.getInstance()
        sharedKirtansListener = db.collection("mediadata")
            .whereEqualTo("type", "audio")
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                
                if (snapshot != null) {
                    val items = snapshot.toObjects(com.example.gokudiyugam.model.MediaItem::class.java)
                    val kirtans = items.map { item ->
                        Kirtan(id = item.id, title = item.title, category = "Shared Kirtans", fileUri = item.url)
                    }
                    sharedKirtans.clear()
                    sharedKirtans.addAll(kirtans)
                }
            }
    }

    fun playKirtan(context: Context, kirtan: Kirtan, playlist: List<Kirtan> = emptyList()) {
        currentKirtan = kirtan
        mediaController?.let { controller ->
            controller.clearMediaItems()
            if (playlist.isNotEmpty()) {
                val mediaItems = playlist.map { item ->
                    MediaItem.Builder()
                        .setMediaId(item.id)
                        .setUri(if (item.fileUri != null) android.net.Uri.parse(item.fileUri) 
                                else android.net.Uri.parse("android.resource://${context.packageName}/${item.resourceId}"))
                        .build()
                }
                controller.setMediaItems(mediaItems)
                val index = playlist.indexOfFirst { it.id == kirtan.id }
                if (index != -1) controller.seekTo(index, 0L)
            } else {
                val mediaItem = MediaItem.Builder()
                    .setMediaId(kirtan.id)
                    .setUri(if (kirtan.fileUri != null) android.net.Uri.parse(kirtan.fileUri) 
                            else android.net.Uri.parse("android.resource://${context.packageName}/${kirtan.resourceId}"))
                    .build()
                controller.setMediaItem(mediaItem)
            }
            controller.prepare()
            controller.play()
        }
    }

    fun togglePlayPause() {
        mediaController?.let { if (it.isPlaying) it.pause() else it.play() }
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
    }
}
