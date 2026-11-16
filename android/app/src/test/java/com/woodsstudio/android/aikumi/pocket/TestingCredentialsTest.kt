package com.woodsstudio.android.aikumi.pocket

import org.junit.Assert.*
import org.junit.Test

class TestingCredentialsTest {
    @Test fun credentialIsOnlyAvailableInClosedTesting() {
        if (BuildConfig.BUILD_TYPE == "closedTesting") {
            // Avoid printing a credential in assertion diagnostics.
            assertTrue(!TestingCredentials.openAiKey().isNullOrBlank())
        } else {
            assertNull(TestingCredentials.openAiKey())
            assertTrue(BuildConfig.OPENAI_API_KEY.isEmpty())
        }
    }
}
