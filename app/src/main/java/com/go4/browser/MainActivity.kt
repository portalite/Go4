package com.go4.browser

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.webkit.CookieManager
import android.webkit.SafeBrowsingResponse
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebResourceError
import android.net.http.SslError
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var browser: WebView
    private lateinit var addressBar: EditText
    private lateinit var progress: ProgressBar

    private val homeUrl = "https://duckduckgo.com/"

    private val blockedDomains = setOf(
        "doubleclick.net",
        "googlesyndication.com",
        "googleadservices.com",
        "adservice.google.com",
        "adnxs.com",
        "taboola.com",
        "outbrain.com",
        "scorecardresearch.com"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        browser = findViewById(R.id.browserView)
        addressBar = findViewById(R.id.addressBar)
        progress = findViewById(R.id.loadingProgress)

        setupBrowser()
        setupButtons()

        if (savedInstanceState != null) {
            browser.restoreState(savedInstanceState)
        } else {
            browser.loadUrl(homeUrl)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupBrowser() {

        browser.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = false

            allowFileAccess = false
            allowContentAccess = false
            allowFileAccessFromFileURLs = false
            allowUniversalAccessFromFileURLs = false

            mixedContentMode =
                WebSettings.MIXED_CONTENT_NEVER_ALLOW

            javaScriptCanOpenWindowsAutomatically = false
            setSupportMultipleWindows(false)

            safeBrowsingEnabled = true

            cacheMode = WebSettings.LOAD_DEFAULT
        }

        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(browser, false)
        }

        browser.webViewClient = object : WebViewClient() {

            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {

                val url = request.url

                return url.scheme != "https" &&
                       url.scheme != "http"
            }

            override fun onPageStarted(
                view: WebView,
                url: String?,
                favicon: android.graphics.Bitmap?
            ) {
                super.onPageStarted(view, url, favicon)

                progress.visibility = ProgressBar.VISIBLE
                addressBar.setText(url ?: "")
            }

            override fun onPageFinished(
                view: WebView,
                url: String?
            ) {
                super.onPageFinished(view, url)

                progress.visibility = ProgressBar.GONE
                addressBar.setText(url ?: "")
            }

            override fun shouldInterceptRequest(
                view: WebView,
                request: WebResourceRequest
            ): android.webkit.WebResourceResponse? {

                val host = request.url.host ?: return null

                val blocked = blockedDomains.any {
                    host == it || host.endsWith(".$it")
                }

                if (blocked) {
                    return android.webkit.WebResourceResponse(
                        "text/plain",
                        "UTF-8",
                        204,
                        "No Content",
                        emptyMap(),
                        java.io.ByteArrayInputStream(ByteArray(0))
                    )
                }

                return super.shouldInterceptRequest(view, request)
            }

            override fun onReceivedSslError(
                view: WebView,
                handler: SslErrorHandler,
                error: SslError
            ) {
                handler.cancel()
            }
        }

        browser.webChromeClient = object : WebChromeClient() {

            override fun onProgressChanged(
                view: WebView,
                newProgress: Int
            ) {
                progress.progress = newProgress

                progress.visibility =
                    if (newProgress >= 100) {
                        ProgressBar.GONE
                    } else {
                        ProgressBar.VISIBLE
                    }
            }

            override fun onGeolocationPermissionsShowPrompt(
                origin: String,
                callback: android.webkit.GeolocationPermissions.Callback
            ) {
                callback.invoke(origin, false, false)
            }
        }
    }

    private fun setupButtons() {

        val goButton: Button = findViewById(R.id.goButton)
        val backButton: Button = findViewById(R.id.backButton)
        val forwardButton: Button = findViewById(R.id.forwardButton)
        val homeButton: Button = findViewById(R.id.homeButton)
        val newTabButton: Button = findViewById(R.id.newTabButton)
        val menuButton: Button = findViewById(R.id.menuButton)

        goButton.setOnClickListener {
            openAddress()
        }

        addressBar.setOnEditorActionListener { _, actionId, _ ->

            if (actionId == EditorInfo.IME_ACTION_GO ||
                actionId == EditorInfo.IME_ACTION_DONE
            ) {
                openAddress()
                true
            } else {
                false
            }
        }

        backButton.setOnClickListener {
            if (browser.canGoBack()) {
                browser.goBack()
            }
        }

        forwardButton.setOnClickListener {
            if (browser.canGoForward()) {
                browser.goForward()
            }
        }

        homeButton.setOnClickListener {
            browser.loadUrl(homeUrl)
        }

        newTabButton.setOnClickListener {
            Toast.makeText(
                this,
                "Sekme yönetimi sonraki sürümde eklenecek.",
                Toast.LENGTH_SHORT
            ).show()
        }

        menuButton.setOnClickListener {
            Toast.makeText(
                this,
                "Go4 ayarları sonraki dosyalarda eklenecek.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun openAddress() {

        var input = addressBar.text.toString().trim()

        if (input.isEmpty()) return

        val url = if (
            input.startsWith("https://") ||
            input.startsWith("http://")
        ) {
            input
        } else if (
            input.contains(" ") ||
            !input.contains(".")
        ) {
            "https://duckduckgo.com/?q=" +
                android.net.Uri.encode(input)
        } else {
            "https://$input"
        }

        browser.loadUrl(url)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        browser.saveState(outState)
    }

    @Deprecated("Deprecated in Android API")
    override fun onBackPressed() {

        if (browser.canGoBack()) {
            browser.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        browser.stopLoading()
        browser.destroy()
        super.onDestroy()
    }
}