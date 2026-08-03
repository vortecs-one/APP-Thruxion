package com.thruxion.app.utils

import android.content.Context
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import java.nio.charset.StandardCharsets

/**
 * Handles ChaCha20-Poly1305 encryption using Google Tink.
 * The keyset is stored securely using Android Keystore.
 */
class CryptoManager(context: Context) {

    private val aead: Aead

    init {
        AeadConfig.register()
        
        // We use ChaCha20Poly1305 which is what Oversec uses
        val keysetManager = AndroidKeysetManager.Builder()
            .withSharedPref(context, KEYSET_NAME, PREF_FILE_NAME)
            .withKeyTemplate(KeyTemplates.get("CHACHA20_POLY1305"))
            .withMasterKeyUri(MASTER_KEY_URI)
            .build()
        
        aead = keysetManager.keysetHandle.getPrimitive(Aead::class.java)
    }

    /**
     * Encrypts the plaintext using ChaCha20-Poly1305.
     */
    fun encrypt(plaintext: String): ByteArray {
        return encrypt(plaintext.toByteArray(StandardCharsets.UTF_8))
    }

    /**
     * Encrypts raw bytes.
     */
    fun encrypt(data: ByteArray): ByteArray {
        return aead.encrypt(data, null)
    }

    /**
     * Decrypts the ciphertext using ChaCha20-Poly1305.
     */
    fun decrypt(ciphertext: ByteArray): String {
        return String(decryptToBytes(ciphertext), StandardCharsets.UTF_8)
    }

    /**
     * Decrypts to raw bytes.
     */
    fun decryptToBytes(ciphertext: ByteArray): ByteArray {
        return aead.decrypt(ciphertext, null)
    }

    companion object {
        private const val KEYSET_NAME = "oversec_keyset"
        private const val PREF_FILE_NAME = "oversec_crypto_prefs"
        private const val MASTER_KEY_URI = "android-keystore://oversec_master_key"
    }
}
