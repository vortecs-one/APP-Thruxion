package com.thruxion.app.rt

import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class SingleFlightRefreshTest {

    class SingleFlightTokenRefresher {
        private val mutex = Mutex()
        val networkCallCount = AtomicInteger(0)
        private var currentToken = "old_token"

        suspend fun getFreshAccessToken(): String {
            return mutex.withLock {
                if (currentToken == "old_token") {
                    // Simulate network call delay
                    delay(100)
                    networkCallCount.incrementAndGet()
                    currentToken = "new_token_123"
                }
                currentToken
            }
        }
    }

    @Test
    fun testTwoConcurrentCallersExecuteOneRefresh() = runBlocking {
        val refresher = SingleFlightTokenRefresher()

        val deferred1 = async { refresher.getFreshAccessToken() }
        val deferred2 = async { refresher.getFreshAccessToken() }

        val token1 = deferred1.await()
        val token2 = deferred2.await()

        assertEquals("new_token_123", token1)
        assertEquals("new_token_123", token2)
        assertEquals("Two concurrent callers must result in exactly 1 refresh network call", 1, refresher.networkCallCount.get())
    }
}
