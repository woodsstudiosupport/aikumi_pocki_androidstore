package com.woodsstudio.android.aikumi.pocket

import org.json.JSONObject

/** Bounded, data-only manifest. Transport and publisher access remain trust boundaries. */
data class ReleaseFile(val path: String, val sha256: String, val bytes: Int)
data class ReleaseManifest(val release: String, val files: List<ReleaseFile>, val raw: String) {
    companion object {
        const val MAX_FILE = 8 * 1024 * 1024
        const val MAX_TOTAL = 32 * 1024 * 1024
        private val allowed = Regex("(?:ui|core)/[A-Za-z0-9_-]+(?:/[A-Za-z0-9_-]+)*\\.(?:html|css|js|json|png|webp|jpg|woff2)")
        fun parse(raw: String, shellVersion: Int): ReleaseManifest {
            require(raw.toByteArray(Charsets.UTF_8).size <= 128 * 1024)
            val json = JSONObject(raw)
            require(json.getInt("schemaVersion") == 1) { "Unsupported release schema" }
            require(json.getInt("minShellVersion") <= shellVersion) { "App update required" }
            val release = json.getString("release")
            require(Regex("[a-f0-9]{64}").matches(release))
            val entries = json.getJSONArray("files")
            require(entries.length() in 1..100)
            val files = (0 until entries.length()).map { index ->
                val item = entries.getJSONObject(index)
                val file = ReleaseFile(item.getString("path"), item.getString("sha256"), item.getInt("bytes"))
                require(allowed.matches(file.path)) { "Invalid resource path" }
                require(Regex("[a-f0-9]{64}").matches(file.sha256))
                require(file.bytes in 1..MAX_FILE)
                file
            }
            require(files.map { it.path }.distinct().size == files.size)
            require(files.sumOf { it.bytes.toLong() } <= MAX_TOTAL)
            require(files.any { it.path == "ui/index.html" })
            require(files.any { it.path == "core/personality.json" })
            // Identifies the exact set of bytes; prevents changing a release under the same ID.
            val canonical = files.sortedBy { it.path }.joinToString("") { "${it.path}:${it.sha256}:${it.bytes}\n" }
            require(sha256(canonical.toByteArray(Charsets.UTF_8)) == release) { "Release identity mismatch" }
            return ReleaseManifest(release, files, raw)
        }
    }
}
fun sha256(bytes: ByteArray): String = java.security.MessageDigest.getInstance("SHA-256")
    .digest(bytes).joinToString("") { "%02x".format(it) }
