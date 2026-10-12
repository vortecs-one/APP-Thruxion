package com.thruxion.app.rt.auth

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import com.thruxion.app.BuildConfig
import com.thruxion.app.rt.service.RtLocationService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.openid.appauth.AuthState
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.EndSessionRequest
import net.openid.appauth.ResponseTypeValues
import net.openid.appauth.TokenRequest
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class RtAuthManager private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val tokenStore = AuthTokenStore(appContext)
    private val refreshMutex = Mutex()
    private val okHttpClient = OkHttpClient()

    private var _authState: AuthState? = tokenStore.loadAuthState()
    val authState: AuthState? get() = _authState

    private var authServiceConfig: AuthorizationServiceConfiguration? = _authState?.authorizationServiceConfiguration

    private val _sessionEndedEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val sessionEndedEvents: SharedFlow<Unit> = _sessionEndedEvents.asSharedFlow()

    fun isLoggedIn(): Boolean {
        return _authState?.isAuthorized == true
    }

    fun getPreferredUsername(): String {
        val state = _authState ?: return "Signed in"
        val idTokenStr = state.idToken
        val extracted = parsePreferredUsername(idTokenStr)
        return extracted ?: "Signed in"
    }

    private fun parsePreferredUsername(idToken: String?): String? {
        if (idToken.isNullOrEmpty()) return null
        return try {
            val parts = idToken.split(".")
            if (parts.size >= 2) {
                val decoded = Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
                val jsonStr = String(decoded, Charsets.UTF_8)
                val jsonObj = Json.parseToJsonElement(jsonStr) as? JsonObject
                val pref = (jsonObj?.get("preferred_username") as? JsonPrimitive)?.content
                val name = (jsonObj?.get("name") as? JsonPrimitive)?.content
                pref ?: name
            } else null
        } catch (e: Exception) {
            null
        }
    }

    fun getSub(): String? {
        return _authState?.parsedIdToken?.subject
    }

    suspend fun discoverServiceConfiguration(): AuthorizationServiceConfiguration {
        authServiceConfig?.let { return it }
        return withContext(Dispatchers.IO) {
            suspendCoroutine { continuation ->
                AuthorizationServiceConfiguration.fetchFromIssuer(Uri.parse(BuildConfig.ISSUER)) { config, ex ->
                    if (config != null) {
                        authServiceConfig = config
                        continuation.resume(config)
                    } else {
                        throw ex ?: RuntimeException("Failed to discover issuer configuration")
                    }
                }
            }
        }
    }

    suspend fun createAuthIntent(): Intent {
        val config = discoverServiceConfiguration()
        val authRequest = AuthorizationRequest.Builder(
            config,
            BuildConfig.CLIENT_ID,
            ResponseTypeValues.CODE,
            Uri.parse(BuildConfig.REDIRECT_URI)
        )
            .setScope(BuildConfig.SCOPES)
            .build()

        val authService = AuthorizationService(appContext)
        return authService.getAuthorizationRequestIntent(authRequest)
    }

    suspend fun handleAuthResponse(response: AuthorizationResponse?, ex: AuthorizationException?): Boolean {
        if (response == null) {
            return false
        }
        val tokenRequest = response.createTokenExchangeRequest()
        val authService = AuthorizationService(appContext)

        return withContext(Dispatchers.IO) {
            suspendCoroutine { continuation ->
                authService.performTokenRequest(tokenRequest) { tokenResponse, tokenEx ->
                    if (tokenResponse != null) {
                        val newAuthState = AuthState(response, tokenEx)
                        newAuthState.update(tokenResponse, tokenEx)
                        _authState = newAuthState
                        tokenStore.saveAuthState(newAuthState)
                        continuation.resume(true)
                    } else {
                        continuation.resume(false)
                    }
                }
            }
        }
    }

    suspend fun getFreshAccessToken(forceRefresh: Boolean = false): String? {
        return refreshMutex.withLock {
            val state = _authState ?: return@withLock null
            if (!state.isAuthorized) {
                return@withLock null
            }

            val expirationTime = state.accessTokenExpirationTime
            val needsRefresh = forceRefresh || expirationTime == null || (expirationTime - System.currentTimeMillis() < 60_000)

            if (!needsRefresh) {
                return@withLock state.accessToken
            }

            val refreshToken = state.refreshToken
            if (refreshToken.isNullOrEmpty()) {
                handleSessionExpired()
                return@withLock null
            }

            val config = discoverServiceConfiguration()
            val tokenRequest = TokenRequest.Builder(config, BuildConfig.CLIENT_ID)
                .setGrantType("refresh_token")
                .setRefreshToken(refreshToken)
                .setScope(BuildConfig.SCOPES)
                .build()

            val authService = AuthorizationService(appContext)
            val refreshResult: Pair<Boolean, Boolean> = withContext(Dispatchers.IO) {
                suspendCoroutine { continuation ->
                    authService.performTokenRequest(tokenRequest) { response, ex ->
                        if (response != null) {
                            state.update(response, ex)
                            _authState = state
                            tokenStore.saveAuthState(state)
                            continuation.resume(Pair(true, false))
                        } else {
                            val isInvalidGrant = ex?.code == AuthorizationException.TokenRequestErrors.INVALID_GRANT.code ||
                                    ex?.error == AuthorizationException.TokenRequestErrors.INVALID_GRANT.error ||
                                    ex?.errorDescription?.contains("invalid_grant", ignoreCase = true) == true
                            continuation.resume(Pair(false, isInvalidGrant))
                        }
                    }
                }
            }

            if (refreshResult.first) {
                return@withLock _authState?.accessToken
            } else {
                if (refreshResult.second) {
                    handleSessionExpired()
                }
                return@withLock null
            }
        }
    }

    private fun handleSessionExpired() {
        RtLocationService.stopService(appContext)
        clearAuthState()
        _sessionEndedEvents.tryEmit(Unit)
    }

    fun clearAuthState() {
        _authState = null
        tokenStore.clearAuthState()
    }

    suspend fun logout(): Intent? {
        RtLocationService.stopService(appContext)

        val refreshToken = _authState?.refreshToken
        if (!refreshToken.isNullOrEmpty()) {
            withContext(Dispatchers.IO) {
                try {
                    val revokeUrl = "${BuildConfig.ISSUER}/protocol/openid-connect/revoke"
                    val formBody = FormBody.Builder()
                        .add("client_id", BuildConfig.CLIENT_ID)
                        .add("token", refreshToken)
                        .add("token_type_hint", "refresh_token")
                        .build()
                    val request = Request.Builder()
                        .url(revokeUrl)
                        .post(formBody)
                        .build()
                    okHttpClient.newCall(request).execute().close()
                } catch (e: Exception) {
                    // Ignore revoke network failure on logout
                }
            }
        }

        val idToken = _authState?.idToken
        val config = authServiceConfig
        val endSessionIntent = if (idToken != null && config != null) {
            val endSessionRequest = EndSessionRequest.Builder(config)
                .setIdTokenHint(idToken)
                .setPostLogoutRedirectUri(Uri.parse(BuildConfig.REDIRECT_URI))
                .build()
            val authService = AuthorizationService(appContext)
            authService.getEndSessionRequestIntent(endSessionRequest)
        } else null

        clearAuthState()
        return endSessionIntent
    }

    companion object {
        @Volatile
        private var INSTANCE: RtAuthManager? = null

        fun getInstance(context: Context): RtAuthManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: RtAuthManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
