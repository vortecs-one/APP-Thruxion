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

        // Use /web instead of /web/login to check for session first
        webView.loadUrl("https://qapta-odoo-odoov19.odoo.com/web")
        
        return view
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, true)

        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                progressBar.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                progressBar.visibility = View.GONE
                
                // Force sync cookies to disk
                cookieManager.flush()
                
                val currentUserEmail = com.thruxion.app.network.security.TokenManager.getUserEmail()
                if (currentUserEmail == "vortecs.ink@gmail.com") {
                    if (url != null && (url.contains("/web/login") || url.endsWith("/login"))) {
                        injectAutoLoginScript()
                    }
                }
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
            allowContentAccess = true
            allowFileAccess = true
            javaScriptCanOpenWindowsAutomatically = true

            // Updated User Agent to Desktop for better compatibility
            userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
        }
        
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
    }

    /**
     * Refined injection script for Odoo with auto-press.
     */
    private fun injectAutoLoginScript() {
        val testEmail = "admin"
        val testPassword = "admin" 

        val script = """
            (function() {
                if (window.autoLoginTriggered) return;
                
                function fillField(selector, value) {
                    var el = document.querySelector(selector);
                    if (el) {
                        el.value = value;
                        el.dispatchEvent(new Event('input', { bubbles: true }));
                        el.dispatchEvent(new Event('change', { bubbles: true }));
                        el.dispatchEvent(new Event('blur', { bubbles: true }));
                        return true;
                    }
                    return false;
                }

                var filledLogin = fillField("input[name='login']", '$testEmail');
                var filledPass = fillField("input[name='password']", '$testPassword');
                
                if (filledLogin && filledPass) {
                    window.autoLoginTriggered = true;
                    setTimeout(function() {
                        var btn = document.querySelector("button[type='submit'], .btn-primary, .oe_login_button");
                        if (btn) {
                            btn.click();
                        } else {
                            var form = document.querySelector("form");
                            if (form) form.submit();
                        }
                    }, 800);
                }
            })()
        """.trimIndent()
        
        webView.evaluateJavascript(script, null)
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