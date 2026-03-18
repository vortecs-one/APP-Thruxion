package com.example.qhagoapp.ui.reflow

/*
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.qhagoapp.databinding.FragmentReflowBinding

class ReflowFragment : Fragment()
{
    private var _binding: FragmentReflowBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val reflowViewModel =
            ViewModelProvider(this).get(ReflowViewModel::class.java)

        _binding = FragmentReflowBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.textReflow
        reflowViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
*/

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

class ReflowFragment : Fragment() {

    private lateinit var webView: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_reflow, container, false)

        webView = view.findViewById(R.id.webView)

        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap) {
                // show loader
            }
            override fun onPageFinished(view: WebView?, url: String?) {
                // hide loader
            }
        }

        webView.webViewClient = WebViewClient()
        webView.settings.useWideViewPort = true
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.loadWithOverviewMode = true
        webView.loadUrl("https://www.google.com")
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
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