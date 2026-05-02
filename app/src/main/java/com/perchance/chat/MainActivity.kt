package com.perchance.chat

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.webkit.*
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import android.graphics.Color
import android.graphics.drawable.ClipDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Build
import android.view.Gravity
import android.view.WindowInsetsController

class MainActivity : AppCompatActivity() {

    private var webView: WebView? = null
    private var progressBar: ProgressBar? = null
    private val TARGET_URL = "https://perchance.org/0t7o0b20gx"
    private var fileChooserCallback: ValueCallback<Array<Uri>>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        // ── Capturador global: muestra el error si la app crashea ──────────
        Thread.setDefaultUncaughtExceptionHandler { _, throwable ->
            runOnUiThread {
                try {
                    AlertDialog.Builder(this)
                        .setTitle("Error — copia este mensaje")
                        .setMessage(throwable.stackTraceToString().take(2000))
                        .setPositiveButton("Cerrar") { _, _ -> finish() }
                        .show()
                } catch (_: Exception) {
                    // Si ni el diálogo se puede mostrar, al menos un Toast
                    Toast.makeText(applicationContext,
                        "Crash: ${throwable.javaClass.simpleName}: ${throwable.message}",
                        Toast.LENGTH_LONG).show()
                }
            }
        }

        super.onCreate(savedInstanceState)

        try {
            setupWindow()
            buildUI(savedInstanceState)
        } catch (e: Exception) {
            showFatalError(e)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWindow() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.statusBarColor = Color.TRANSPARENT
            window.navigationBarColor = Color.TRANSPARENT
        }
        // insetsController requires DecorView to be attached — called after setContentView
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun buildUI(savedInstanceState: Bundle?) {
        val root = RelativeLayout(this).apply {
            setBackgroundColor(Color.parseColor("#0d0c14"))
            layoutParams = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT
            )
        }

        webView = WebView(this).apply {
            id = View.generateViewId()
            layoutParams = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT
            )
        }

        progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            id = View.generateViewId()
            layoutParams = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT, 8
            ).also { it.addRule(RelativeLayout.ALIGN_PARENT_TOP) }
            max = 100
            val accent = Color.parseColor("#7c72ff")
            val fill = ClipDrawable(ColorDrawable(accent), Gravity.START, ClipDrawable.HORIZONTAL)
            progressDrawable = LayerDrawable(arrayOf(ColorDrawable(Color.TRANSPARENT), fill)).apply {
                setId(0, android.R.id.background)
                setId(1, android.R.id.progress)
            }
        }

        root.addView(webView)
        root.addView(progressBar)
        setContentView(root)

        // DecorView ya está adjunto — ahora sí es seguro llamar insetsController
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.setSystemBarsAppearance(
                0, WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
            )
        }

        setupWebView()

        if (savedInstanceState != null) {
            webView?.restoreState(savedInstanceState)
        } else {
            webView?.loadUrl(TARGET_URL)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val wv = webView ?: return
        wv.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            javaScriptCanOpenWindowsAutomatically = true
            allowContentAccess = true
            allowFileAccess = true
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            loadWithOverviewMode = true
            useWideViewPort = true
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            cacheMode = WebSettings.LOAD_DEFAULT
            // Suplantar Chrome moderno — Perchance rechaza el UA por defecto del WebView
            userAgentString = "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
        }

        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(wv, true)
        }

        wv.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val url = request.url.toString()
                if (url.startsWith("https://perchance.org") || url.startsWith("http://perchance.org")) {
                    return false
                }
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                return true
            }

            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                CookieManager.getInstance().flush()
            }

            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                if (request.isForMainFrame) {
                    view.loadData(
                        """<html><body style="background:#0d0c14;color:#e8e6f0;font-family:sans-serif;
                            display:flex;align-items:center;justify-content:center;height:100vh;margin:0;
                            flex-direction:column;gap:1rem;text-align:center;padding:2rem;">
                          <div style="font-size:2rem;">⚠️</div>
                          <div style="font-size:1.1rem;font-weight:600;">Sin conexión</div>
                          <div style="opacity:0.6;font-size:0.9rem;">Revisa tu internet y vuelve a intentarlo.</div>
                          <button onclick="window.location='$TARGET_URL'"
                            style="background:#7c72ff;color:white;border:none;padding:0.75rem 1.5rem;
                            border-radius:12px;font-size:1rem;cursor:pointer;margin-top:0.5rem;">
                            Reintentar</button>
                        </body></html>""",
                        "text/html", "UTF-8"
                    )
                }
            }
        }

        wv.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                progressBar?.progress = newProgress
                progressBar?.visibility = if (newProgress < 100) View.VISIBLE else View.GONE
            }

            override fun onShowFileChooser(
                webView: WebView,
                filePathCallback: ValueCallback<Array<Uri>>,
                fileChooserParams: FileChooserParams
            ): Boolean {
                fileChooserCallback?.onReceiveValue(null)
                fileChooserCallback = filePathCallback
                return try {
                    startActivityForResult(fileChooserParams.createIntent(), FILE_CHOOSER_REQUEST)
                    true
                } catch (e: Exception) {
                    fileChooserCallback = null
                    false
                }
            }

            override fun onGeolocationPermissionsShowPrompt(origin: String, callback: GeolocationPermissions.Callback) {
                callback.invoke(origin, false, false)
            }
        }
    }

    private fun showFatalError(e: Exception) {
        AlertDialog.Builder(this)
            .setTitle("Error de inicio — copia este texto")
            .setMessage("${e.javaClass.name}\n\n${e.message}\n\n${e.stackTraceToString().take(1500)}")
            .setPositiveButton("Cerrar") { _, _ -> finish() }
            .show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == FILE_CHOOSER_REQUEST) {
            val results = if (resultCode == Activity.RESULT_OK && data != null)
                WebChromeClient.FileChooserParams.parseResult(resultCode, data) else null
            fileChooserCallback?.onReceiveValue(results)
            fileChooserCallback = null
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && webView?.canGoBack() == true) {
            webView?.goBack()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView?.saveState(outState)
    }

    override fun onResume() { super.onResume(); webView?.onResume() }
    override fun onPause() { super.onPause(); webView?.onPause() }
    override fun onDestroy() {
        webView?.apply { stopLoading(); destroy() }
        super.onDestroy()
    }

    companion object {
        private const val FILE_CHOOSER_REQUEST = 1001
    }
}
