package com.thruxion.app.ui.healthy

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
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
import com.thruxion.app.utils.LocaleManager
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

import android.Manifest
import android.content.pm.PackageManager
import android.webkit.PermissionRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.thruxion.app.utils.HealthManager

class HealthyFragment : Fragment()
{
    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var healthManager: HealthManager

    private val cameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted)
            pendingPermissionRequest?.grant(arrayOf(PermissionRequest.RESOURCE_VIDEO_CAPTURE))
        else
        {
            android.widget.Toast.makeText(context, R.string.camera_permission_denied, android.widget.Toast.LENGTH_SHORT).show()
            pendingPermissionRequest?.deny()
        }
        pendingPermissionRequest = null
    }

    private var pendingPermissionRequest: PermissionRequest? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_healthy, container, false)
        webView = root.findViewById(R.id.webViewHealthy)
        progressBar = root.findViewById(R.id.progressBarHealthy)
        healthManager = HealthManager(requireContext())

        setupWebView()
        performHandoff()

        return root
    }

    private fun performHandoff() {
        val email = TokenManager.getUserEmail() ?: ""
        val password = TokenManager.getUserPassword() ?: ""
        val lang = LocaleManager.getLanguage()

        // Set the language cookie globally for the domain
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setCookie(ApiRegistry.HEALTHY_BASE_URL, "app_lang=$lang; Path=/; Secure; SameSite=Lax")
        cookieManager.flush()

        if (email.isEmpty() || password.isEmpty()) {
            webView.loadUrl("${ApiRegistry.HEALTHY_LOGIN_URL}?lang=$lang")
            return
        }

        // 1. Check if we already have a URL loaded. If so, just update the lang cookie/JS and don't reload.
        if (webView.url != null && webView.url!!.contains(ApiRegistry.HEALTHY_BASE_URL)) {
            val currentLang = LocaleManager.getLanguage()
            webView.evaluateJavascript("window.appPreferredLanguage = '$currentLang';", null)
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                progressBar.visibility = View.VISIBLE
                val timestamp = System.currentTimeMillis()
                val secret = ApiRegistry.HEALTHY_HANDOFF_SECRET
                
                val bodyString = "{\"email\":\"$email\",\"password\":\"$password\"}"
                val signatureData = "$timestamp.$bodyString"
                val signature = hmacSha256(secret, signatureData)

                val requestBody = bodyString.toRequestBody("application/json".toMediaType())

                val response = ApiRegistry.healthyApi.issueHandoff(
                    timestamp = timestamp,
                    signature = signature,
                    language = lang,
                    request = requestBody
                )

                if (response.isSuccessful && response.body()?.success == true)
                {
                    val handoffUrl = response.body()?.handoffUrl
                    if (!handoffUrl.isNullOrEmpty())
                        webView.loadUrl(handoffUrl, mapOf("x-app-language" to lang))
                    else
                        webView.loadUrl("${ApiRegistry.HEALTHY_LOGIN_URL}?lang=$lang")

                }
                else
                    webView.loadUrl("${ApiRegistry.HEALTHY_LOGIN_URL}?lang=$lang")
            }
            catch (e: Exception) {
                android.util.Log.e("HealthyFragment", "Handoff error", e)
                webView.loadUrl("${ApiRegistry.HEALTHY_LOGIN_URL}?lang=$lang")
            }
            finally {
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun hmacSha256(secret: String, data: String): String
    {
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
                // Inject language on every page load as final fallback
                val currentLang = LocaleManager.getLanguage()
                view?.evaluateJavascript("window.appPreferredLanguage = '$currentLang';", null)
            }
        }

        webView.webChromeClient = object : WebChromeClient()
        {
            override fun onProgressChanged(view: WebView?, newProgress: Int)
            {
                if (newProgress == 100)
                    progressBar.visibility = View.GONE
                else
                {
                    progressBar.visibility = View.VISIBLE
                    progressBar.progress = newProgress
                }
            }

            override fun onPermissionRequest(request: PermissionRequest?)
            {
                if (request == null) return
                val resources = request.resources
                if (resources.contains(PermissionRequest.RESOURCE_VIDEO_CAPTURE)) {
                    if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
                        request.grant(arrayOf(PermissionRequest.RESOURCE_VIDEO_CAPTURE))
                    else
                    {
                        pendingPermissionRequest = request
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                }
                else
                    // For other resources, grant if requested
                    request.grant(resources)
            }
        }

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            // 1. Enable Caching
            cacheMode = WebSettings.LOAD_DEFAULT
            // 2. Performance optimizations
            useWideViewPort = true
            loadWithOverviewMode = true
            loadsImagesAutomatically = true
            // 3. Security (Already updated)
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            // 4. Media Capture (Necessary for barcode scanning in WebView)
            mediaPlaybackRequiresUserGesture = false
            userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
        }
        // Bridge for health data
        webView.addJavascriptInterface(HealthyJavascriptInterface(healthManager), "NativeHealthy")
        // 4. Enable hardware acceleration for smoother rendering
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner)
        {
            if (webView.canGoBack())
                webView.goBack()
            else
            {
                isEnabled = false
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }
    }

}
