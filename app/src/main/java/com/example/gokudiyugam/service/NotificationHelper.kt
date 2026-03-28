package com.example.gokudiyugam.service

import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

object NotificationHelper {
    private const val FCM_API_URL = "https://fcm.googleapis.com/fcm/send"
    // IMPORTANT: Use your FCM Server Key here.
    private const val SERVER_KEY = "AAAAq7-x0yA:APA91bFp7M..." // Replace with actual key from Firebase Console

    fun sendNotificationToTopic(topic: String, title: String, body: String) {
        val client = OkHttpClient()
        val json = JSONObject()
        val notification = JSONObject()
        notification.put("title", title)
        notification.put("body", body)
        notification.put("sound", "default")

        val data = JSONObject()
        data.put("title", title)
        data.put("body", body)

        json.put("to", "/topics/$topic")
        json.put("notification", notification)
        json.put("data", data)
        json.put("priority", "high")

        val requestBody = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url(FCM_API_URL)
            .post(requestBody)
            .addHeader("Authorization", "key=$SERVER_KEY")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("NotificationHelper", "Failed to send notification: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                Log.d("NotificationHelper", "Notification sent status: ${response.code}")
                response.close()
            }
        })
    }
}
