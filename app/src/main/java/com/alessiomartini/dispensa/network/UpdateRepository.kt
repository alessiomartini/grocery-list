package com.alessiomartini.dispensa.network

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.alessiomartini.dispensa.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

sealed interface UpdateCheckResult {
    data class Available(val version: String, val notes: String, val downloadUrl: String) : UpdateCheckResult
    data object UpToDate : UpdateCheckResult
    data class Error(val message: String) : UpdateCheckResult
}

/**
 * Checks GitHub Releases for a newer build of the app (there's no Play Store listing) and can
 * download + launch the installer for the APK asset attached to that release.
 *
 * CI always publishes to a single release tagged "latest" (overwritten on every push, no manual
 * tagging), so "newer" is decided by comparing [BuildConfig.VERSION_CODE] against a version.txt
 * asset in that release rather than by parsing the tag name.
 */
class UpdateRepository(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun checkForUpdate(): UpdateCheckResult = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("https://api.github.com/repos/${BuildConfig.GITHUB_REPO}/releases/latest")
                .addHeader("Accept", "application/vnd.github+json")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext if (response.code == 404) {
                        UpdateCheckResult.Error("No release has been published on GitHub yet")
                    } else {
                        UpdateCheckResult.Error("HTTP ${response.code}")
                    }
                }

                val release = json.decodeFromString(
                    GitHubRelease.serializer(),
                    response.body?.string().orEmpty()
                )
                val apkAsset = release.assets.firstOrNull { it.name.endsWith(".apk") }
                    ?: return@withContext UpdateCheckResult.Error("The latest release doesn't have an .apk file attached")
                val versionAsset = release.assets.firstOrNull { it.name == "version.txt" }
                    ?: return@withContext UpdateCheckResult.Error("The latest release is missing its version info")

                val remoteVersionCode = downloadVersionCode(versionAsset.browser_download_url)
                    ?: return@withContext UpdateCheckResult.Error("Couldn't read the remote version")

                if (remoteVersionCode > BuildConfig.VERSION_CODE) {
                    UpdateCheckResult.Available(
                        version = "1.$remoteVersionCode",
                        notes = release.body.orEmpty(),
                        downloadUrl = apkAsset.browser_download_url
                    )
                } else {
                    UpdateCheckResult.UpToDate
                }
            }
        } catch (e: IOException) {
            UpdateCheckResult.Error(e.message ?: "Network error")
        } catch (e: Exception) {
            UpdateCheckResult.Error(e.message ?: "Unexpected error")
        }
    }

    private fun downloadVersionCode(url: String): Int? {
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            return response.body?.string()?.trim()?.toIntOrNull()
        }
    }

    suspend fun downloadApk(url: String): File = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Download failed: HTTP ${response.code}")
            val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
            val apkFile = File(updatesDir, "pantry-update.apk")
            val body = response.body ?: throw IOException("Empty response body")
            body.byteStream().use { input ->
                apkFile.outputStream().use { output -> input.copyTo(output) }
            }
            apkFile
        }
    }

    fun canInstallPackages(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }

    fun openInstallPermissionSettings() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val intent = Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun installApk(apkFile: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apkFile)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
