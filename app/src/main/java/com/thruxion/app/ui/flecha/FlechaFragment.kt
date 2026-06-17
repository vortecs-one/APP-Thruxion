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
     * Highly robust injection script for Odoo.
     * Aggressively clears cookie banners and accessibility links to ensure the login button is clickable.
     */
    private fun injectAutoLoginScript() {
        val odooEmail = "admin"
        val odooPassword = "admin123"

        val script = """
            (function() {
                if (window.autoLoginExecuting) return;
                window.autoLoginExecuting = true;

                function log(msg) { console.log("[AutoLogin] " + msg); }

                function forceClearObstacles() {
                    // 1. Target common Odoo cookie banners and buttons
                    const cookieSelectors = [
                        '.o_accept_cookies', '.o_cookie_notice', '#onetrust-accept-btn-handler', 
                        '.cc-btn.cc-dismiss', '.js-cookie-consent-agree', 'button[aria-label*="Accept"]',
                        '#cc-allow-all-lib', '.s_popup_close'
                    ];
                    cookieSelectors.forEach(s => {
                        const el = document.querySelector(s);
                        if (el && el.offsetParent !== null) {
                            log("Clearing obstacle: " + s);
                            if (el.tagName === 'BUTTON' || el.classList.contains('btn')) {
                                el.click();
                            } else {
                                el.style.setProperty('display', 'none', 'important');
                            }
                        }
                    });
                    
                    // 2. Remove "Skip to Content" and other accessibility overlays that block clicks
                    const blockers = document.querySelectorAll('a[href*="main"], .o_skip_to_content, [class*="skip-link"], .o_accessibility_links');
                    blockers.forEach(el => {
                        el.style.setProperty('display', 'none', 'important');
                        el.style.setProperty('visibility', 'hidden', 'important');
                        el.style.setProperty('pointer-events', 'none', 'important');
                    });
                }
                
                function performLogin() {
                    forceClearObstacles();

                    const loginField = document.querySelector("input[name='login']");
                    const passwordField = document.querySelector("input[name='password']");
                    const submitButton = document.querySelector(".btn-primary, button[type='submit']");
                    
                    if (loginField && passwordField && submitButton) {
                        // If fields are not visible, they might be covered by a popup
                        if (loginField.offsetParent === null) {
                             log("Fields found but not visible (possibly blocked by popup).");
                             return false;
                        }
                        
                        if (window.autoLoginDone) return true;

                        log("Login form clear and ready. Proceeding...");

                        function setFieldValue(field, value) {
                            field.focus();
                            field.value = value;
                            ['input', 'change', 'blur'].forEach(ev => {
                                field.dispatchEvent(new Event(ev, { bubbles: true }));
                                field.dispatchEvent(new InputEvent(ev, { bubbles: true, inputType: 'insertText', data: value }));
                            });
                        }

                        // Fill Fields
                        setFieldValue(loginField, '$odooEmail');
                        setFieldValue(passwordField, '$odooPassword');
                        log("Fields filled.");

                        // Wait for Framework Sync
                        setTimeout(function() {
                            log("Triggering login sequence...");
                            
                            submitButton.focus();

                            // Priority 1: Enter Key
                            const enterEv = new KeyboardEvent('keydown', {
                                bubbles: true, cancelable: true, keyCode: 13, key: 'Enter', code: 'Enter'
                            });
                            passwordField.dispatchEvent(enterEv);

                            // Priority 2: Full Click Sequence
                            ['mousedown', 'mouseup', 'click'].forEach(ev => {
                                submitButton.dispatchEvent(new MouseEvent(ev, {
                                    view: window, bubbles: true, cancelable: true, buttons: 1
                                }));
                            });
                            submitButton.click();

                            // Fallback: Form Submit
                            setTimeout(() => {
                                if (submitButton && submitButton.form) {
                                    log("Final fallback: Form submit.");
                                    submitButton.form.submit();
                                }
                                window.autoLoginDone = true;
                            }, 1000);

                        }, 1500);

                        return true; 
                    }
                    return false;
                }

                // Initial scan and periodic cleanup
                let attempts = 0;
                const interval = setInterval(function() {
                    attempts++;
                    // Keep clearing obstacles even if we haven't found the form yet
                    forceClearObstacles();

                    if (performLogin() || attempts > 60) {
                        clearInterval(interval);
                    }
                }, 1000);

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
