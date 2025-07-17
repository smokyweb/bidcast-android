package io.bidswipe.app.ui.dashboard.sellerHub

import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.LAYER_TYPE_HARDWARE
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.core.view.isVisible
import io.bidswipe.app.R
import io.bidswipe.app.base.BaseFragment
import io.bidswipe.app.databinding.FragmentKYCBinding
import io.bidswipe.app.interfaces.AlertClicks
import io.bidswipe.app.network.Resource
import io.bidswipe.app.ui.custom.AppBottomSheet
import io.bidswipe.app.utils.finish
import io.bidswipe.app.utils.parse
import io.bidswipe.app.utils.runSafe

class KYCFragment : BaseFragment<SellerHubViewModel, FragmentKYCBinding>() {

    override fun getModel(): Class<SellerHubViewModel> = SellerHubViewModel::class.java

    override fun getBind(
        inflater: LayoutInflater,
        view: ViewGroup?
    ) = FragmentKYCBinding.inflate(inflater,view,false)

    private var url = ""

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        url = activity?.intent?.getStringExtra("url").toString()

        bind.webView.setLayerType(LAYER_TYPE_HARDWARE, null)

        bind.header.onBackClick{
            goBack()
        }

        val headerMap = mutableMapOf<String, String>(
            "access-control-allow-origin" to "*"
        )

        bind.loader.isVisible = true
        viewModel.checkKyc()

        viewModel.checkKycRepo.observe(viewLifecycleOwner) {
            when (it) {
                is Resource.Success -> {
                    bind.loader.isVisible = false
                    val mData = it.value.data

//                    val customTab = CustomTabsIntent.Builder().build()
//                    customTab.launchUrl(mCtx , mData?.url.toString().toUri())

                    bind.webView.webViewClient = WebClient()
                    bind.webView.loadUrl(mData?.url.toString(),headerMap)
                    bind.webView.settings.apply {
                        layoutAlgorithm = WebSettings.LayoutAlgorithm.NORMAL
                        cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
                        allowUniversalAccessFromFileURLs = true
                        loadWithOverviewMode = true
                        javaScriptEnabled = true
                        domStorageEnabled = true
                    }
                }

                is Resource.Error -> {
                    bind.loader.isVisible = false
                    if (it.isNetworkError) {
                        errorToast(getString(R.string.no_internet))
                    } else {
                        it.parse(mCtx, TAG, object : AlertClicks {
                            override fun primaryClick(dialog: AppBottomSheet) {
                                dialog.dismiss()
                            }

                            override fun secondaryClick(dialog: AppBottomSheet) {
                                dialog.dismiss()
                            }
                        })
                    }
                }

                else -> {}

            }
        }

        activity?.onBackPressedDispatcher?.addCallback(this,object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                runSafe {
                    goBack()
                }
            }
        })
    }

    private fun goBack() {
        if (bind.webView.canGoBack()) {
            bind.webView.goBack()
        }
        else {
            finish()

        }
    }

    internal inner class WebClient : WebViewClient() {
        @Deprecated("Deprecated in Java")
        override fun shouldOverrideUrlLoading(view : WebView, url : String) : Boolean {
            handleUrl(url)
            view.loadUrl(url)
            return true
        }

        override fun shouldOverrideUrlLoading(view : WebView?, request : WebResourceRequest?) : Boolean {
            val url = request?.url.toString()
            handleUrl(url)
            view?.loadUrl(url)
            return true
        }

        override fun onPageStarted(view : WebView?, url : String?, favicon : Bitmap?) {
            super.onPageStarted(view , url , favicon)
            log("LOAD START URL : $url")
            bind.loader.visibility = View.VISIBLE
        }

        override fun onPageFinished(view : WebView, url : String) {
            super.onPageFinished(view , url)
            log("LOAD FINISH URL : $url")
            bind.loader.visibility = View.GONE
        }

        private fun handleUrl(url : String){
            log("REDIRECT URL : $url")
            runSafe {
                if (url.contains("?")) {
                    val arr = url.split("/?/".toRegex()).toTypedArray()
                    if (arr[arr.size - 1].contains("account")) {
                        val param = arr[arr.size - 1].split("=".toRegex()).toTypedArray()
                        val id = param[param.size - 1]
                        log("STRIPE ID : $id")
                        finish()
                    }
                }
            }
        }
    }
}