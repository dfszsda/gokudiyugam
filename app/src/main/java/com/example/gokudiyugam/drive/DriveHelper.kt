@file:Suppress("DEPRECATION")

package com.example.gokudiyugam.drive

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import com.google.api.services.drive.model.FileList
import com.google.api.services.drive.model.Permission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.util.Collections

@Suppress("DEPRECATION")
class DriveHelper(private val driveService: Drive) {

    fun getDriveServiceDirect(): Drive = driveService

    suspend fun createFile(name: String, mimeType: String, content: InputStream, folderId: String? = null): String? = withContext(Dispatchers.IO) {
        val metadata = File().apply {
            this.name = name
            this.mimeType = mimeType
            if (folderId != null) {
                this.parents = listOf(folderId)
            }
        }
        val mediaContent = com.google.api.client.http.InputStreamContent(mimeType, content)
        val googleFile: File = driveService.files().create(metadata, mediaContent)
            .setFields("id")
            .execute()
        googleFile.id
    }

    suspend fun createFolder(folderName: String): String? = withContext(Dispatchers.IO) {
        val metadata = File().apply {
            name = folderName
            mimeType = "application/vnd.google-apps.folder"
        }
        val folder: File = driveService.files().create(metadata)
            .setFields("id")
            .execute()
        folder.id
    }

    suspend fun findFolder(folderName: String): String? = withContext(Dispatchers.IO) {
        val query = "name = '$folderName' and mimeType = 'application/vnd.google-apps.folder' and trashed = false"
        val result: FileList = driveService.files().list()
            .setQ(query)
            .setSpaces("drive")
            .setFields("files(id)")
            .execute()
        result.files?.firstOrNull()?.id
    }

    suspend fun makeFilePublic(fileId: String) = withContext(Dispatchers.IO) {
        val permission = Permission().apply {
            type = "anyone"
            role = "reader"
        }
        driveService.permissions().create(fileId, permission).execute()
    }

    suspend fun makeFolderWritable(folderId: String) = withContext(Dispatchers.IO) {
        val permission = Permission().apply {
            type = "anyone"
            role = "writer"
        }
        driveService.permissions().create(folderId, permission).execute()
    }

    suspend fun getFileLinks(fileId: String): Pair<String?, String?> = withContext(Dispatchers.IO) {
        val file: File = driveService.files().get(fileId)
            .setFields("webContentLink, webViewLink")
            .execute()
        Pair(file.webContentLink, file.webViewLink)
    }

    suspend fun queryFiles(): List<File> = withContext(Dispatchers.IO) {
        val result: FileList = driveService.files().list()
            .setSpaces("drive")
            .setFields("nextPageToken, files(id, name, mimeType, thumbnailLink, webContentLink)")
            .execute()
        result.files ?: emptyList()
    }

    companion object {
        fun getDriveService(context: Context, account: GoogleSignInAccount): Drive {
            val credential = GoogleAccountCredential.usingOAuth2(
                context, Collections.singleton(DriveScopes.DRIVE)
            )
            credential.selectedAccount = account.account
            return Drive.Builder(
                NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                credential
            )
                .setApplicationName("Gokudiyugam")
                .build()
        }

        fun getGoogleSignInClient(context: Context): GoogleSignInClient {
            val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("728177722635-u2gatdgb305ga6gbt6viabml4d8hv3gc.apps.googleusercontent.com")
                .requestEmail()
                .requestScopes(Scope(DriveScopes.DRIVE_FILE))
                .build()
            return GoogleSignIn.getClient(context, signInOptions)
        }
    }
}
