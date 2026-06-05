package com.example.gokudiyugam.network

import android.util.Log
import com.example.gokudiyugam.model.MediaItem
import com.google.firebase.Timestamp
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

object YouTubeApiService {
    private val gson = Gson()

    suspend fun fetchLatestVideos(apiKey: String, channelId: String, eventType: String? = null): List<MediaItem> {
        if (apiKey.isBlank() || channelId.isBlank()) return emptyList()
        return withContext(Dispatchers.IO) {
            try {
                var urlString = "https://www.googleapis.com/youtube/v3/search?key=$apiKey&channelId=$channelId&part=snippet,id&order=date&maxResults=20&type=video"
                if (eventType != null) {
                    urlString += "&eventType=$eventType"
                }
                val response = URL(urlString).readText()
                val json = gson.fromJson(response, YouTubeSearchResponse::class.java)
                
                json.items.map { item ->
                    MediaItem(
                        id = item.id.videoId ?: "",
                        title = item.snippet.title,
                        url = "https://www.youtube.com/watch?v=${item.id.videoId}",
                        type = "youtube",
                        mediaType = "video",
                        timestamp = Timestamp.now()
                    )
                }
            } catch (e: Exception) {
                Log.e("YouTubeApi", "Error fetching channel videos: ${e.message}")
                emptyList()
            }
        }
    }

    suspend fun fetchLiveAndUpcomingVideos(apiKey: String, channelId: String): List<MediaItem> {
        val live = fetchLatestVideos(apiKey, channelId, "live")
        val upcoming = fetchLatestVideos(apiKey, channelId, "upcoming")
        return (live + upcoming).distinctBy { it.id }
    }

    suspend fun fetchPlaylistVideos(apiKey: String, playlistId: String): List<MediaItem> {
        if (apiKey.isBlank() || playlistId.isBlank()) return emptyList()
        return withContext(Dispatchers.IO) {
            try {
                val urlString = "https://www.googleapis.com/youtube/v3/playlistItems?key=$apiKey&playlistId=$playlistId&part=snippet,contentDetails&maxResults=20"
                val response = URL(urlString).readText()
                val json = gson.fromJson(response, YouTubePlaylistResponse::class.java)
                
                json.items.map { item ->
                    MediaItem(
                        id = item.contentDetails.videoId,
                        title = item.snippet.title,
                        url = "https://www.youtube.com/watch?v=${item.contentDetails.videoId}",
                        type = "youtube",
                        mediaType = "video",
                        timestamp = Timestamp.now()
                    )
                }
            } catch (e: Exception) {
                Log.e("YouTubeApi", "Error fetching playlist videos: ${e.message}")
                emptyList()
            }
        }
    }
}

data class YouTubeSearchResponse(val items: List<YouTubeSearchItem> = emptyList())
data class YouTubeSearchItem(val id: YouTubeId, val snippet: YouTubeSnippet)
data class YouTubeId(val videoId: String? = null)
data class YouTubeSnippet(val title: String, val publishedAt: String)

data class YouTubePlaylistResponse(val items: List<YouTubePlaylistItem> = emptyList())
data class YouTubePlaylistItem(val snippet: YouTubeSnippet, val contentDetails: YouTubeContentDetails)
data class YouTubeContentDetails(val videoId: String)
