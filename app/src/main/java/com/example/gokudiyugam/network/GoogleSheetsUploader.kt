package com.example.gokudiyugam.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object GoogleSheetsUploader {
    private const val WEB_APP_URL = "https://script.google.com/macros/s/AKfycbzWhvllOpg_I_45hCpuibbe7eKfRKPWFN93NCAGNup6giseDkwl7PfqsdAkq_6jAlmT/exec"

    suspend fun uploadUserData(
        firstName: String = "",
        middleName: String = "",
        lastName: String = "",
        role: String = "NORMAL",
        gender: String = "",
        email: String = "",
        mobileNumber: String = "",
        dob: String = "",
        password: String = "",
        uid: String = ""
    ) = withContext(Dispatchers.IO) {
        try {
            val url = URL(WEB_APP_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")

            val jsonObject = JSONObject().apply {
                put("firstName", firstName)
                put("middleName", middleName)
                put("lastName", lastName)
                put("role", role)
                put("gender", gender)
                put("email", email)
                put("mobileNumber", mobileNumber)
                put("dob", dob)
                put("password", password)
                put("uid", uid)
            }

            val writer = OutputStreamWriter(connection.outputStream)
            writer.write(jsonObject.toString())
            writer.flush()
            writer.close()

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_MOVED_TEMP) {
                Log.d("GoogleSheetsUploader", "Data uploaded successfully")
            } else {
                Log.e("GoogleSheetsUploader", "Upload failed with code: $responseCode")
            }
            connection.disconnect()
        } catch (e: Exception) {
            Log.e("GoogleSheetsUploader", "Error uploading to Google Sheets: ${e.message}")
        }
    }
}
