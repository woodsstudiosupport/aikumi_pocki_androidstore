package com.woodsstudio.android.aikumi.pocket

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.URL
import javax.net.ssl.HttpsURLConnection

/** Only the loader has network access; the downloaded interface has none. */
class ReleaseLoader(context: Context) {
    private val root = File(context.noBackupFilesDir, "releases").apply { mkdirs() }
    private val prefs = context.getSharedPreferences("release", Context.MODE_PRIVATE)
    private val base = "https://raw.githubusercontent.com/woodsstudiosupport/aikumi_pocki_androidstore/main/remote/"
    private val shellVersion = 1
    data class Result(val directory: File, val cached: Boolean)

    fun sync(progress: (String) -> Unit): Result {
        try {
            progress("Checking for updates…")
            val raw = download("manifest.json", 128 * 1024).toString(Charsets.UTF_8)
            val manifest = ReleaseManifest.parse(raw, shellVersion)
            val active = verified(prefs.getString("active", null))
            if (active?.name == manifest.release) return Result(active, false)
            val stage = File(root, "staging-${java.util.UUID.randomUUID()}").apply { mkdirs() }
            try {
                manifest.files.forEachIndexed { index, entry ->
                    progress("Preparing Aikumi… ${index + 1}/${manifest.files.size}")
                    val old = active?.let { File(it, entry.path) }
                    val bytes = if (old != null && old.isFile && old.length() == entry.bytes.toLong() && sha256(old.readBytes()) == entry.sha256) old.readBytes()
                        else download(entry.path, entry.bytes)
                    require(bytes.size == entry.bytes && sha256(bytes) == entry.sha256) { "Resource verification failed" }
                    File(stage, entry.path).apply { parentFile!!.mkdirs(); writeBytes(bytes) }
                }
                // Parsing is part of validation: malformed core must not become active.
                val core = JSONObject(File(stage, "core/personality.json").readText())
                require(core.getInt("schemaVersion") == 1 && core.getString("instructions").isNotBlank())
                File(stage, "manifest.json").writeText(raw)
                if (Thread.currentThread().isInterrupted) throw IOException("Preparation interrupted")
                val destination = File(root, manifest.release)
                if (destination.exists()) destination.deleteRecursively()
                check(stage.renameTo(destination)) { "Could not activate release" }
                val previous = active?.name ?: prefs.getString("active", null)
                check(prefs.edit().putString("previous", previous).putString("active", manifest.release).commit())
                return Result(destination, false)
            } finally { stage.deleteRecursively() }
        } catch (error: Exception) {
            val cached = verified(prefs.getString("active", null)) ?: verified(prefs.getString("previous", null))
            if (cached != null) return Result(cached, true)
            throw IOException("Couldn’t prepare Aikumi. Check your connection and try again.", error)
        }
    }

    private fun verified(id: String?): File? {
        if (id == null || !Regex("[a-f0-9]{64}").matches(id)) return null
        return try {
            val directory = File(root, id)
            val meta = File(directory, "manifest.json")
            require(meta.length() in 1..128 * 1024)
            val manifest = ReleaseManifest.parse(meta.readText(), shellVersion)
            require(manifest.release == id)
            manifest.files.forEach { entry ->
                val file = File(directory, entry.path)
                require(file.isFile && file.length() == entry.bytes.toLong() && sha256(file.readBytes()) == entry.sha256)
            }
            directory
        } catch (_: Exception) { null }
    }

    fun confirmHealthy(id: String) {
        if (prefs.getString("active", null) != id) return
        val keep = setOf(id, prefs.getString("previous", null))
        root.listFiles()?.filter { it.name !in keep }?.forEach { it.deleteRecursively() }
    }

    fun rollback(failed: String): File? {
        val previous = verified(prefs.getString("previous", null)) ?: return null
        if (previous.name == failed) return null
        prefs.edit().putString("active", previous.name).remove("previous").commit()
        return previous
    }

    private fun download(path: String, limit: Int): ByteArray {
        val connection = URL(base + path).openConnection() as HttpsURLConnection
        connection.connectTimeout = 10000
        connection.readTimeout = 15000
        connection.instanceFollowRedirects = false
        connection.setRequestProperty("Cache-Control", "no-cache")
        try {
            if (connection.responseCode != 200) throw IOException("Resource unavailable")
            if (connection.contentLengthLong > limit) throw IOException("Resource too large")
            return connection.inputStream.use { input ->
                val output = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                var total = 0
                while (true) {
                    if (Thread.currentThread().isInterrupted) throw IOException("Download interrupted")
                    val count = input.read(buffer)
                    if (count == -1) break
                    total += count
                    if (total > limit) throw IOException("Resource too large")
                    output.write(buffer, 0, count)
                }
                output.toByteArray()
            }
        } finally { connection.disconnect() }
    }
}
