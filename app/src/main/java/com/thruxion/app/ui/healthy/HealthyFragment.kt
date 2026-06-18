package com.thruxion.app.ui.healthy

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.thruxion.app.R
import com.thruxion.app.network.ApiRegistry
import com.thruxion.app.network.security.TokenManager
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class HealthyFragment : Fragment() {

    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_healthy, container, false)
        webView = root.findViewById(R.id.webViewHealthy)
        progressBar = root.findViewById(R.id.progressBarHealthy)

        setupWebView()
        performHandoff()

        return root
    }

    private fun performHandoff() {
        val email = TokenManager.getUserEmail() ?: ""
        val password = TokenManager.getUserPassword() ?: ""

        if (email.isEmpty() || password.isEmpty()) {
            webView.loadUrl("https://web-nutrition.vercel.app/login")
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                progressBar.visibility = View.VISIBLE
                val timestamp = System.currentTimeMillis()
                val secret = "thruxion"
                
                // Construct the exact JSON body for signing
                val bodyString = "{\"email\":\"$email\",\"password\":\"$password\"}"
                val signatureData = "$timestamp.$bodyString"
                val signature = hmacSha256(secret, signatureData)

                val requestBody = bodyString.toRequestBody("application/json".toMediaType())

                val response = ApiRegistry.healthyApi.issueHandoff(
                    timestamp = timestamp,
                    signature = signature,
                    request = requestBody
                )

                if (response.isSuccessful && response.body()?.success == true) {
                    val handoffUrl = response.body()?.handoffUrl
                    if (!handoffUrl.isNullOrEmpty()) {
                        webView.loadUrl(handoffUrl)
                    } else {
                        webView.loadUrl("https://web-nutrition.vercel.app/login")
                    }
                } else {
                    webView.loadUrl("https://web-nutrition.vercel.app/login")
                }
            } catch (e: Exception) {
                android.util.Log.e("HealthyFragment", "Handoff error", e)
                webView.loadUrl("https://web-nutrition.vercel.app/login")
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun hmacSha256(secret: String, data: String): String {
        val sha256HMAC = Mac.getInstance("HmacSHA256")
        val secretKey = SecretKeySpec(secret.toByteArray(), "HmacSHA256")
        sha256HMAC.init(secretKey)
        return sha256HMAC.doFinal(data.toByteArray()).joinToString("") { "%02x".format(it) }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                progressBar.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                progressBar.visibility = View.GONE
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                if (newProgress == 100) {
                    progressBar.visibility = View.GONE
                } else {
                    progressBar.visibility = View.VISIBLE
                    progressBar.progress = newProgress
                }
            }
        }

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            cacheMode = WebSettings.LOAD_DEFAULT
            useWideViewPort = true
            loadWithOverviewMode = true
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            if (webView.canGoBack()) {
                webView.goBack()
            } else {
                isEnabled = false
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }
    }
}
