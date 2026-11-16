package com.woodsstudio.android.aikumi.pocket

/** Native-only credential access. Do not return this value through the WebView bridge. */
internal object TestingCredentials {
    fun openAiKey(): String? =
        if (BuildConfig.BUILD_TYPE == "closedTesting") {
            BuildConfig.OPENAI_API_KEY.takeIf { it.isNotBlank() }
        } else null
}
