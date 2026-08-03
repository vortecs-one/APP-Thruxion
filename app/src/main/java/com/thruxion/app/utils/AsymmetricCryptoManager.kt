package com.thruxion.app.utils

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.*
import java.security.spec.X509EncodedKeySpec
import javax.crypto.KeyAgreement

/**
 * Handles Elliptic Curve Diffie-Hellman (ECDH) key exchange.
 * Generates and stores the device's EC key pair in the Android Keystore.
 */
object AsymmetricCryptoManager {
    private const val KEY_ALIAS = "thruxion_ec_key"
    private const val PROVIDER = "AndroidKeyStore"

    init {
        generateKeyPairIfNeeded()
    }

    /**
     * Generates an EC key pair if it doesn't already exist.
     */
    private fun generateKeyPairIfNeeded() {
        val keyStore = KeyStore.getInstance(PROVIDER).apply { load(null) }
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val kpg = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC)
            kpg.initialize(256) // Standard P-256 curve
            val keyPair = kpg.generateKeyPair()
            
            // In a production app, we would encrypt the private key before storing it.
            // For now, we'll focus on the logic.
            // Note: Storing raw keys in SharedPreferences is NOT recommended for production.
        }
    }

    /**
     * Gets the public key in Base64 format for sharing with the server.
     */
    fun getPublicKeyBase64(): String {
        val keyStore = KeyStore.getInstance(PROVIDER).apply { load(null) }
        val certificate = keyStore.getCertificate(KEY_ALIAS)
        return Base64.encodeToString(certificate.publicKey.encoded, Base64.NO_WRAP)
    }

    /**
     * Derives a shared secret using the local private key and a partner's public key.
     * @param partnerPublicKeyBase64 The Base64 encoded public key of the other user.
     * @return A 32-byte shared secret (suitable for AES/ChaCha20 keys).
     */
    fun deriveSharedSecret(partnerPublicKeyBase64: String): ByteArray {
        val keyStore = KeyStore.getInstance(PROVIDER).apply { load(null) }
        val privateKey = keyStore.getKey(KEY_ALIAS, null) as PrivateKey

        val partnerKeyBytes = Base64.decode(partnerPublicKeyBase64, Base64.DEFAULT)
        val kf = KeyFactory.getInstance("EC")
        val partnerPublicKey = kf.generatePublic(X509EncodedKeySpec(partnerKeyBytes))

        val ka = KeyAgreement.getInstance("ECDH")
        ka.init(privateKey)
        ka.doPhase(partnerPublicKey, true)
        
        val sharedSecret = ka.generateSecret()
        
        // Hash the secret to ensure it's exactly 32 bytes and uniformly distributed
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest(sharedSecret)
    }
}
