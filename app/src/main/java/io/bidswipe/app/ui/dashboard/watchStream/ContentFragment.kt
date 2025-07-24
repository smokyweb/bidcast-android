package io.bidswipe.app.ui.dashboard.watchStream

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.databinding.FragmentContentBinding
import io.bidswipe.app.network.Resource
import io.bidswipe.app.ui.dashboard.DashViewModel

class ContentFragment : BaseFragment<DashViewModel, FragmentContentBinding>() {

    override fun getModel() = DashViewModel::class.java

    override fun getBind(
        inflater: LayoutInflater,
        view: ViewGroup?,
    ) = FragmentContentBinding.inflate(inflater, view, false)

    private var slug: String? = null


    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        slug = activity?.intent?.getStringExtra("slug")
        val title = activity?.intent?.getStringExtra("title") ?: "Content"


        bind.webView.settings.apply {
            layoutAlgorithm = WebSettings.LayoutAlgorithm.NORMAL
            cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
            loadWithOverviewMode = true
            javaScriptEnabled = true
            domStorageEnabled = true
        }

        bind.webView.webViewClient = object : WebViewClient() {
            @Deprecated("Deprecated in Java")
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                view.loadUrl(url)
                return true
            }

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?,
            ): Boolean {
                val url = request?.url.toString()
                view?.loadUrl(url)
                return true
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                bind.loader.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                bind.loader.visibility = View.GONE
            }
        }

        bind.header.setHeaderText(title)
        bind.header.onBackClick {
            if (!findNavController().popBackStack()) {
                requireActivity().onBackPressed()
            }

        }
        loadWebContent()
    }

    private fun loadWebContent() {
        bind.loader.isVisible = true

        viewModel.getPageUrl(slug ?: "")
        viewModel.pageUrlRepo.observe(viewLifecycleOwner) { it ->
            when (it) {
                is Resource.Success -> {
                    bind.loader.isVisible = false
                    it.value.data?.url?.let { url ->
                        Log.d(TAG, "loadWebContent: $url")
                        bind.webView.loadUrl(url)
                    }
                }

                is Resource.Error -> {
                    bind.loader.isVisible = false
                }

                else -> {}
            }
        }

    }


}





