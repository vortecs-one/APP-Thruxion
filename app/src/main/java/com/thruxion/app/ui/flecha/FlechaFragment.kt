package com.thruxion.app.ui.flecha

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
import com.thruxion.app.R

class FlechaFragment : Fragment() {
    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_flecha, container, false)
        webView = view.findViewById(R.id.webView)
        progressBar = view.findViewById(R.id.progressBar)

        setupWebView()

        webView.loadUrl("https://qapta-odoo-odoov19.odoo.com/web/login")
        return view
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        // Enable cookie persistence
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)

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
            // Core performance settings
            javaScriptEnabled = true
            domStorageEnabled = true
            
            // Caching
            cacheMode = WebSettings.LOAD_DEFAULT
            
            // Responsive settings
            useWideViewPort = true
            loadWithOverviewMode = true
            
            // Additional optimizations
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            
            // Security / Odoo specific
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }
        
        // Explicit hardware acceleration
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
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

    override fun onDestroyView() {
        super.onDestroyView()
    }
}