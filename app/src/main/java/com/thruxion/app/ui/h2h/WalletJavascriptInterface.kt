package com.thruxion.app.ui.h2h

import android.webkit.JavascriptInterface
import com.thruxion.app.utils.MetaMaskManager
import org.web3j.crypto.Sign
import org.web3j.crypto.Credentials
import org.web3j.utils.Numeric
import android.util.Log

/**
 * Bridge between the H2H WebView and the native MetaMask wallet.
 * Exposes methods to JavaScript under the name 'NativeWallet'.
 */
class WalletJavascriptInterface {

    @JavascriptInterface
    fun getAddress(): String {
        return MetaMaskManager.getPublicAddress()
    }

    @JavascriptInterface
    fun isConnected(): Boolean {
        return MetaMaskManager.isLoggedIn()
    }

    @JavascriptInterface
    fun signMessage(message: String): String {
        val privKey = MetaMaskManager.getPrivateKey()
        if (privKey.isEmpty()) return ""
        
        return try {
            val credentials = Credentials.create(privKey)
            val msgBytes = message.toByteArray()
            val signatureData = Sign.signPrefixedMessage(msgBytes, credentials.ecKeyPair)
            
            val r = Numeric.toHexString(signatureData.r)
            val s = Numeric.toHexString(signatureData.s)
            val v = Numeric.toHexString(signatureData.v)
            
            // Format: 0x... (compact signature)
            "0x" + r.substring(2) + s.substring(2) + v.substring(2)
        } catch (e: Exception) {
            Log.e("WalletJS", "Error signing message", e)
            ""
        }
    }
}
