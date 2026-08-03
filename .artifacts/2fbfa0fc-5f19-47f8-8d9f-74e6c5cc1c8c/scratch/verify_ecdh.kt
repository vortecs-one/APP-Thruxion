import java.security.*
import java.security.spec.X509EncodedKeySpec
import javax.crypto.KeyAgreement

fun main() {
    // Generate two EC key pairs
    val kpg = KeyPairGenerator.getInstance("EC")
    kpg.initialize(256)
    
    val alicePair = kpg.generateKeyPair()
    val bobPair = kpg.generateKeyPair()
    
    // Alice derives shared secret using Bob's public key
    val aliceKa = KeyAgreement.getInstance("ECDH")
    aliceKa.init(alicePair.private)
    aliceKa.doPhase(bobPair.public, true)
    val aliceSecret = aliceKa.generateSecret()
    
    // Bob derives shared secret using Alice's public key
    val bobKa = KeyAgreement.getInstance("ECDH")
    bobKa.init(bobPair.private)
    bobKa.doPhase(alicePair.public, true)
    val bobSecret = bobKa.generateSecret()
    
    println("Alice Secret: ${aliceSecret.joinToString("") { "%02x".format(it) }}")
    println("Bob Secret:   ${bobSecret.joinToString("") { "%02x".format(it) }}")
    
    if (aliceSecret.contentEquals(bobSecret)) {
        println("SUCCESS: Shared secrets match!")
    } else {
        println("FAILURE: Shared secrets do NOT match!")
    }
}
