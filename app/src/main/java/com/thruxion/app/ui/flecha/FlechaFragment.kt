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
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import com.thruxion.app.R
import com.thruxion.app.network.security.TokenManager

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

        val currentUserEmail = TokenManager.getUserEmail()
        
        // SECURITY: If the current app user is not the authorized Odoo user, 
        // ensure no session data from previous users persists.
        if (currentUserEmail != AUTHORIZED_EMAIL) {
            val cookieManager = CookieManager.getInstance()
            cookieManager.removeAllCookies(null)
            cookieManager.flush()
            WebStorage.getInstance().deleteAllData()
        }

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
                
                val currentUserEmail = TokenManager.getUserEmail()
                if (currentUserEmail == AUTHORIZED_EMAIL) {
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
     * Highly aggressive injection script for Odoo.
     * Forcefully removes cookie banners, shows a "Logging in" overlay, and automates login.
     */
    private fun injectAutoLoginScript() {
        val odooEmail = "admin"
        val odooPassword = "admin123"

        val script = """
            (function() {
                if (window.autoLoginExecuting) return;
                window.autoLoginExecuting = true;

                function log(msg) { console.log("[AutoLogin] " + msg); }

                function showOverlay() {
                    if (document.getElementById('auto-login-overlay')) return;
                    const overlay = document.createElement('div');
                    overlay.id = 'auto-login-overlay';
                    overlay.style.cssText = "position:fixed;top:0;left:0;width:100%;height:100%;background:rgba(255,255,255,0.95);z-index:2147483647;display:flex;flex-direction:column;align-items:center;justify-content:center;font-family:sans-serif;pointer-events:all;";
                    overlay.innerHTML = `
                        <div style="width:60px;height:60px;border:6px solid #f3f3f3;border-top:6px solid #714B67;border-radius:50%;animation:spin 1s linear infinite;"></div>
                        <div style="margin-top:20px;color:#714B67;font-weight:bold;font-size:18px;">Autenticando...</div>
                        <style>
                            @keyframes spin { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }
                        </style>
                    `;
                    document.body.appendChild(overlay);
                }

                function cleanPage() {
                    const blockers = [
                        '.o_cookie_notice', '.o_accept_cookies', '#onetrust-banner-sdk',
                        '.cc-window', '.cc-banner', '.s_popup', '.modal-backdrop',
                        '.o_loading', '.o_blockUI', '.o_notification_manager'
                    ];
                    blockers.forEach(selector => {
                        document.querySelectorAll(selector).forEach(el => {
                            el.style.setProperty('display', 'none', 'important');
                            el.remove();
                        });
                    });
                }

                function setFieldValue(field, value) {
                    if (field.value === value) return;
                    field.focus();
                    field.value = value;
                    ['input', 'change', 'blur'].forEach(ev => {
                        field.dispatchEvent(new Event(ev, { bubbles: true }));
                    });
                }
                
                function performLogin() {
                    const loginField = document.querySelector("input[name='login']");
                    const passwordField = document.querySelector("input[name='password']");
                    const submitButton = document.querySelector("button[type='submit'], .btn-primary, .oe_login_button");
                    
                    if (loginField && passwordField && submitButton) {
                        showOverlay(); // Prevent user interaction immediately
                        cleanPage();

                        setFieldValue(loginField, '$odooEmail');
                        setFieldValue(passwordField, '$odooPassword');

                        if (loginField.value && passwordField.value) {
                            if (window.autoLoginDone) return;
                            window.autoLoginDone = true;

                            setTimeout(() => {
                                submitButton.removeAttribute('disabled');
                                submitButton.classList.remove('disabled');
                                
                                ['mousedown', 'mouseup', 'click'].forEach(ev => {
                                    submitButton.dispatchEvent(new MouseEvent(ev, {
                                        view: window, bubbles: true, cancelable: true, buttons: 1
                                    }));
                                });
                                submitButton.click();

                                setTimeout(() => {
                                    if (document.querySelector("input[name='login']")) {
                                        if (submitButton.form) submitButton.form.submit();
                                        else if (loginField.form) loginField.form.submit();
                                    }
                                }, 1000);
                            }, 400);
                        }
                    }
                }

                const interval = setInterval(() => {
                    performLogin();
                    if (!window.location.href.includes('/login') && window.autoLoginDone) {
                        clearInterval(interval);
                    }
                }, 500);
                
                performLogin();
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

    companion object {
        private const val AUTHORIZED_EMAIL = "vortecs.ink@gmail.com"
    }
}
