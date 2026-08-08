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
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.methods.request.Transaction
import org.web3j.protocol.http.HttpService
import org.web3j.utils.Convert
import org.web3j.abi.FunctionEncoder
import org.web3j.abi.FunctionReturnDecoder
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.Function
import org.web3j.abi.datatypes.generated.Uint256
import java.util.concurrent.CompletableFuture
import java.math.BigDecimal
import java.math.BigInteger

/**
 * Manages the MetaMask Embedded Wallet (Web3Auth) integration.
 * Handles authentication, session management, and key retrieval.
 */
object MetaMaskManager {
    private const val TAG = "MetaMaskManager"
    
    private const val CLIENT_ID = "BGiQzOQVw_kEBI4c66cDKrYwTDsnWr2iQOLiZnojd7PTGssirKtDasVsNV3V0UtvMdtYXof8Z9122N1V4Vl6GPw" 
    
    // Default RPC for balance checks (Sepolia Testnet)
    private const val DEFAULT_RPC = "https://rpc.ankr.com/eth_sepolia"

    data class Token(
        val symbol: String,
        val name: String,
        val contractAddress: String,
        val decimals: Int = 18,
        var balance: String = "0.00"
    )

    val SEPOLIA_TEST_TOKENS = listOf(
        Token("USDT", "Tether USD", "0xaA8E23Fb1079EA71e0a56F48a2aA51851D8433D0", 6),
        Token("USDC", "USD Coin", "0x94a9D9AC8a22534E3FaCa9F4e7F2E2cf85d5E4C8", 6),
        Token("DAI", "Dai Stablecoin", "0xFF34B3d4Aee8ddCd6F9AFFFB6Fe49bD371b8a357", 18)
    )

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
     * Fetches the wallet balance from the blockchain.
     */
    fun getBalance(rpcUrl: String = DEFAULT_RPC): CompletableFuture<String> {
        val address = getPublicAddress()
        if (address.isEmpty()) return CompletableFuture.completedFuture("0.00")
        
        return CompletableFuture.supplyAsync {
            try {
                val web3j = Web3j.build(HttpService(rpcUrl))
                val balanceResponse = web3j.ethGetBalance(address, DefaultBlockParameterName.LATEST).send()
                val wei = balanceResponse.balance
                val eth = Convert.fromWei(wei.toString(), Convert.Unit.ETHER)
                String.format("%.4f", eth.toDouble())
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching balance", e)
                "N/A"
            }
        }
    }

    /**
     * Fetches the balance of an ERC-20 token.
     */
    fun getTokenBalance(token: Token, rpcUrl: String = DEFAULT_RPC): CompletableFuture<String> {
        val userAddress = getPublicAddress()
        if (userAddress.isEmpty()) return CompletableFuture.completedFuture("0.00")

        return CompletableFuture.supplyAsync {
            try {
                val web3j = Web3j.build(HttpService(rpcUrl))
                val function = Function(
                    "balanceOf",
                    listOf(Address(userAddress)),
                    listOf(object : TypeReference<Uint256>() {})
                )
                val encodedFunction = FunctionEncoder.encode(function)
                val response = web3j.ethCall(
                    Transaction.createEthCallTransaction(userAddress, token.contractAddress, encodedFunction),
                    DefaultBlockParameterName.LATEST
                ).send()

                val results = FunctionReturnDecoder.decode(response.value, function.outputParameters)
                val balanceWei = results[0].value as BigInteger
                val balanceEth = BigDecimal(balanceWei).divide(BigDecimal.TEN.pow(token.decimals))
                String.format("%.2f", balanceEth.toDouble())
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching token balance for ${token.symbol}", e)
                "0.00"
            }
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
