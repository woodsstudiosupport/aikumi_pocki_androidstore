package com.woodsstudio.android.aikumi.pocket

import android.content.Context
import org.json.JSONObject

/** No repository or network writes. Backups are excluded in the manifest. */
class ProfileStore(context: Context) {
    private val prefs = context.getSharedPreferences("personalisation", Context.MODE_PRIVATE)
    @Synchronized fun read(): JSONObject = normalise(JSONObject(prefs.getString("state", "{}") ?: "{}"))
    @Synchronized fun save(input: JSONObject): JSONObject {
        val state = normalise(input)
        check(prefs.edit().putString("state", state.toString()).commit())
        return state
    }
    private fun normalise(input: JSONObject): JSONObject = JSONObject().apply {
        put("schemaVersion", 1)
        put("onboardingComplete", input.opt("onboardingComplete") == true)
        put("onboardingStep", input.optInt("onboardingStep", 0).coerceIn(0, 2))
        put("name", (input.opt("name") as? String ?: "").trim().take(60))
        put("interests", (input.opt("interests") as? String ?: "").trim().take(300))
        put("saveHistory", input.opt("saveHistory") == true)
    }
}
