package com.example.qhagoapp.ui.flecha

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import com.example.qhagoapp.R

class FlechaFragment : Fragment()
{
    private lateinit var webView: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(inflater: LayoutInflater,container: ViewGroup?,savedInstanceState: Bundle?): View
    {
        val view = inflater.inflate(R.layout.fragment_flecha, container, false)
        webView = view.findViewById(R.id.webView)
        webView.webViewClient = object : WebViewClient()
        {
            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?)
            {
                // show loader if needed
            }
            override fun onPageFinished(view: WebView?, url: String?)
            {
                // hide loader if needed
            }
        }

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            useWideViewPort = true
            loadWithOverviewMode = true
        }
        webView.loadUrl("https://qapta-odoo-odoov19.odoo.com/web/login")
        return view
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
                requireActivity().onBackPressed()
            }
        }
    }

    override fun onDestroyView()
    {
        webView.destroy()
        super.onDestroyView()
    }

}