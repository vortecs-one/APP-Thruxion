package com.thruxion.app.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import com.web3auth.core.Web3Auth
import com.web3auth.core.types.Web3AuthOptions
import com.web3auth.core.types.LoginParams
import com.web3auth.core.types.AuthConnection
import com.web3auth.core.types.Web3AuthResponse
import org.torusresearch.fetchnodedetails.types.Web3AuthNetwork
import org.web3j.crypto.Credentials
import java.util.concurrent.CompletableFuture

/**
 * Manages the MetaMask Embedded Wallet (Web3Auth) integration.
 * Handles authentication, session management, and key retrieval.
 */
object MetaMaskManager {
    private const val TAG = "MetaMaskManager"
    
    private const val CLIENT_ID = "BGiQzOQVw_kEBI4c66cDKrYwTDsnWr2iQOLiZnojd7PTGssirKtDasVsNV3V0UtvMdtYXof8Z9122N1V4Vl6GPw" 
    
    private var web3Auth: Web3Auth? = null
    private var isInitialized = false

    /**
     * Initializes the Web3Auth SDK. Should be called in MainActivity onCreate.
     */
    fun init(context: Context): CompletableFuture<Void>? {
        if (isInitialized) return null
        
        val options = Web3AuthOptions(
            clientId = CLIENT_ID,
            redirectUrl = "com.thruxion.app://auth",
            web3AuthNetwork = Web3AuthNetwork.SAPPHIRE_DEVNET
        )
        
        web3Auth = Web3Auth(options, context)
        
        return web3Auth?.initialize()?.thenAccept {
            isInitialized = true
            Log.d(TAG, "Web3Auth Initialized. Logged in: ${isLoggedIn()}")
        }
    }

    /**
     * Sets the result URL from an intent (deep link).
     */
    fun setResultUrl(uri: Uri?) {
        web3Auth?.setResultUrl(uri)
    }

    /**
     * Checks if a user session is active.
     */
    fun isLoggedIn(): Boolean {
        return web3Auth?.getPrivateKey()?.isNotEmpty() ?: false
    }

    /**
     * Returns the user's private key if logged in.
     */
    fun getPrivateKey(): String {
        return web3Auth?.getPrivateKey() ?: ""
    }

    /**
     * Returns the Ethereum public address derived from the private key.
     */
    fun getPublicAddress(): String {
        val privKey = getPrivateKey()
        if (privKey.isEmpty()) return ""
        return try {
            val credentials = Credentials.create(privKey)
            credentials.address
        } catch (e: Exception) {
            Log.e(TAG, "Error deriving address", e)
            ""
        }
    }

    /**
     * Triggers a login flow (e.g., via Google).
     */
    fun login(connection: AuthConnection = AuthConnection.GOOGLE): CompletableFuture<Web3AuthResponse>? {
        return web3Auth?.connectTo(LoginParams(connection))
    }

    /**
     * Logs the user out.
     */
    fun logout(): CompletableFuture<Void>? {
        return web3Auth?.logout()
    }
}
