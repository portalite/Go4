package com.go4.browser

import android.app.Application
import android.webkit.WebView

class Go4Application : Application() {

    override fun onCreate() {
        super.onCreate()

        // Üretim sürümünde WebView hata ayıklamasını kapat.
        WebView.setWebContentsDebuggingEnabled(false)
    }
}