package com.woodsstudio.android.aikumi.pocket

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class ReleaseManifestTest {
    private fun manifest(path: String = "ui/index.html", bytes: Int = 12, min: Int = 1): String {
        val hash = "a".repeat(64)
        val files = listOf(ReleaseFile(path,hash,bytes),ReleaseFile("core/personality.json",hash,12))
        val canonical = files.sortedBy { it.path }.joinToString("") { "${it.path}:${it.sha256}:${it.bytes}\n" }
        return JSONObject().put("schemaVersion",1).put("minShellVersion",min)
            .put("release",sha256(canonical.toByteArray())).put("files",JSONArray().apply {
                files.forEach { put(JSONObject().put("path",it.path).put("sha256",it.sha256).put("bytes",it.bytes)) }
            }).toString()
    }
    @Test fun acceptsValidRelease() { assertEquals(2,ReleaseManifest.parse(manifest(),1).files.size) }
    @Test fun rejectsTraversalAndNativeCode() {
        listOf("ui/../../profile.json","ui/%2e%2e/secret.json","ui/script.dex","ui/script.js?url=x").forEach {
            assertThrows(IllegalArgumentException::class.java) { ReleaseManifest.parse(manifest(it),1) }
        }
    }
    @Test fun rejectsOversizeAndIncompatibleRelease() {
        assertThrows(IllegalArgumentException::class.java) { ReleaseManifest.parse(manifest(bytes=ReleaseManifest.MAX_FILE+1),1) }
        assertThrows(IllegalArgumentException::class.java) { ReleaseManifest.parse(manifest(min=2),1) }
    }
    @Test fun rejectsResourceMutationUnderSameReleaseId() {
        val json=JSONObject(manifest())
        json.getJSONArray("files").getJSONObject(0).put("sha256","b".repeat(64))
        assertThrows(IllegalArgumentException::class.java) { ReleaseManifest.parse(json.toString(),1) }
    }
    @Test fun rejectsDuplicatePaths() {
        val json=JSONObject(manifest())
        val files=json.getJSONArray("files"); files.put(files.getJSONObject(0))
        assertThrows(IllegalArgumentException::class.java) { ReleaseManifest.parse(json.toString(),1) }
    }
}
