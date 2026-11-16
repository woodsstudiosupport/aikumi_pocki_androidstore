package com.woodsstudio.android.aikumi.pocket

import android.app.Activity
import android.app.AlertDialog
import android.graphics.*
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import android.view.accessibility.AccessibilityManager
import android.webkit.*
import android.widget.*
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.File
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private val executor = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var loader: ReleaseLoader
    private lateinit var profile: ProfileStore
    private lateinit var frame: FrameLayout
    private var web: WebView? = null
    private var loading = false
    private var destroyed = false
    private var ready = false
    private var healthCheck: Runnable? = null
    private val origin = "https://appassets.androidplatform.net"
    private var currentDirectory: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loader = ReleaseLoader(this); profile = ProfileStore(this)
        frame = FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
            fitsSystemWindows = true
            setOnApplyWindowInsetsListener { view, insets ->
                view.setPadding(insets.systemWindowInsetLeft, insets.systemWindowInsetTop, insets.systemWindowInsetRight, insets.systemWindowInsetBottom)
                insets
            }
        }
        setContentView(frame)
        sync()
    }
    private fun sync() {
        if (loading) return
        loading = true
        val status = startup()
        executor.execute {
            try {
                val result = loader.sync { message -> runOnUiThread { if (!destroyed) status.text = message } }
                runOnUiThread {
                    loading = false
                    if (!destroyed) {
                        showRelease(result.directory)
                        if (result.cached) Toast.makeText(this, "Using your saved version. Updates will be checked next time.", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (_: Exception) {
                runOnUiThread {
                    loading = false
                    if (!destroyed) showFailure("Couldn’t prepare Aikumi. Check your connection and try again.")
                }
            }
        }
    }
    private fun startup(): TextView {
        frame.removeAllViews()
        val column = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER }
        val logo = ImageView(this)
        try { assets.open("woods-logo.png").use { logo.setImageBitmap(BitmapFactory.decodeStream(it)) } }
        catch (_: Exception) { /* Wordmark remains visible if asset is unavailable. */ }
        logo.contentDescription = "Woods Studio"
        column.addView(logo, LinearLayout.LayoutParams(dp(180), dp(100)))
        val brand = TextView(this).apply { text = "WOODS STUDIO"; textSize = 13f; setTextColor(Color.WHITE); letterSpacing = .18f; gravity = Gravity.CENTER }
        column.addView(brand)
        column.addView(GradientSpinner(this), LinearLayout.LayoutParams(dp(40), dp(40)).apply { topMargin = dp(38) })
        val status = TextView(this).apply { text = "Preparing Aikumi…"; textSize = 14f; setTextColor(Color.rgb(192,174,207)); gravity = Gravity.CENTER; accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE }
        column.addView(status, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(24) })
        frame.addView(column, FrameLayout.LayoutParams(-1, -1))
        return status
    }
    private fun showFailure(message: String) {
        web?.destroy(); web = null
        healthCheck?.let { handler.removeCallbacks(it) }
        frame.removeAllViews()
        val column = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(dp(28),dp(28),dp(28),dp(28)) }
        column.addView(TextView(this).apply { text = message; textSize = 17f; setTextColor(Color.WHITE); gravity = Gravity.CENTER })
        column.addView(Button(this).apply { text = "Try again"; setOnClickListener { sync() } })
        frame.addView(column, FrameLayout.LayoutParams(-1,-1))
    }
    private fun showRelease(directory: File) {
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) {
            showFailure("Please update Android System WebView to open Aikumi."); return
        }
        healthCheck?.let { handler.removeCallbacks(it) }
        web?.destroy(); ready = false; currentDirectory = directory
        val allowedPaths = ReleaseManifest.parse(File(directory,"manifest.json").readText(),1).files.map { it.path }.toSet()
        val assetLoader = WebViewAssetLoader.Builder().addPathHandler("/release/", WebViewAssetLoader.PathHandler { path ->
            if (path !in allowedPaths) return@PathHandler blocked()
            val target = File(directory, path)
            if (!target.isFile || !target.canonicalPath.startsWith(directory.canonicalPath + File.separator)) return@PathHandler blocked()
            val mime = when (target.extension) {
                "html" -> "text/html"; "js" -> "application/javascript"; "css" -> "text/css"; "json" -> "application/json"
                "png" -> "image/png"; "webp" -> "image/webp"; "jpg" -> "image/jpeg"; "woff2" -> "font/woff2"
                else -> "application/octet-stream"
            }
            WebResourceResponse(mime, "UTF-8", 200,"OK", mapOf(
                "Content-Security-Policy" to "default-src 'none'; script-src 'self'; style-src 'self'; img-src 'self'; connect-src 'none'; font-src 'self'; frame-src 'none'; object-src 'none'; base-uri 'none'; form-action 'none'; frame-ancestors 'none'",
                "X-Content-Type-Options" to "nosniff", "Cache-Control" to "no-store"
            ), target.inputStream())
        }).build()
        val view = WebView(this)
        web = view
        view.setBackgroundColor(Color.rgb(16,13,21))
        view.settings.apply {
            javaScriptEnabled = true; domStorageEnabled = false
            allowFileAccess = false; allowContentAccess = false
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            javaScriptCanOpenWindowsAutomatically = false
            setSupportMultipleWindows(false)
            mediaPlaybackRequiresUserGesture = true
        }
        // The bridge is available only to the fixed local origin and only the top frame.
        WebViewCompat.addWebMessageListener(view,"AikumiNative",setOf(origin)) { _, message, source, mainFrame, reply ->
            if (!mainFrame || source.toString().trimEnd('/') != origin) return@addWebMessageListener
            try {
                val text = message.data ?: throw IllegalArgumentException()
                require(text.length <= 8192)
                val request = JSONObject(text)
                val result = when (request.getString("type")) {
                    "read", "ready" -> profile.read()
                    "save" -> profile.save(request.getJSONObject("payload"))
                    else -> throw IllegalArgumentException()
                }
                if (request.getString("type") == "ready") {
                    ready = true
                    executor.execute { loader.confirmHealthy(directory.name) }
                }
                reply.postMessage(JSONObject().put("ok",true).put("state",result).toString())
            } catch (_: Exception) { reply.postMessage("{\"ok\":false,\"error\":\"Local settings could not be saved\"}") }
        }
        view.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(v: WebView, request: WebResourceRequest): WebResourceResponse = assetLoader.shouldInterceptRequest(request.url) ?: blocked()
            override fun shouldOverrideUrlLoading(v: WebView, request: WebResourceRequest): Boolean = true
            override fun onReceivedError(v: WebView, request: WebResourceRequest, error: WebResourceError) { if (request.isForMainFrame) failRelease(directory) }
            override fun onRenderProcessGone(v: WebView, detail: RenderProcessGoneDetail): Boolean { failRelease(directory); return true }
        }
        view.webChromeClient = object : WebChromeClient() {
            override fun onPermissionRequest(request: PermissionRequest) { request.deny() }
            override fun onJsConfirm(v: WebView, url: String, message: String, result: JsResult): Boolean {
                AlertDialog.Builder(this@MainActivity).setMessage(message).setPositiveButton("Reset") { _,_ -> result.confirm() }
                    .setNegativeButton("Cancel") { _,_ -> result.cancel() }.setOnCancelListener { result.cancel() }.show()
                return true
            }
        }
        frame.removeAllViews(); frame.addView(view,FrameLayout.LayoutParams(-1,-1))
        view.loadUrl("$origin/release/ui/index.html")
        healthCheck = Runnable { if (!ready && web === view && !destroyed) failRelease(directory) }.also { handler.postDelayed(it,15000) }
    }
    private fun failRelease(directory: File) {
        if (destroyed || currentDirectory != directory) return
        currentDirectory = null
        executor.execute {
            val previous = loader.rollback(directory.name)
            runOnUiThread { if (!destroyed) {
                if (previous != null) showRelease(previous)
                else showFailure("This version couldn’t open. Please try again later.")
            } }
        }
    }
    private fun blocked() = WebResourceResponse("text/plain","UTF-8",403,"Blocked",emptyMap(),ByteArrayInputStream(ByteArray(0)))
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    override fun onDestroy() {
        destroyed = true; handler.removeCallbacksAndMessages(null); web?.destroy(); executor.shutdownNow(); super.onDestroy()
    }
}

private class GradientSpinner(context: android.content.Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 4 * resources.displayMetrics.density; strokeCap = Paint.Cap.ROUND }
    private val reduceMotion = (context.getSystemService(android.content.Context.ACCESSIBILITY_SERVICE) as AccessibilityManager).isTouchExplorationEnabled || !android.animation.ValueAnimator.areAnimatorsEnabled()
    init { contentDescription = "Loading" }
    override fun onDraw(canvas: Canvas) {
        val x = width / 2f; val y = height / 2f
        paint.shader = SweepGradient(x,y,intArrayOf(Color.rgb(100,54,158),Color.rgb(219,174,255)),null)
        val turn = if (reduceMotion) 0f else (android.os.SystemClock.uptimeMillis() % 1300) / 1300f * 360f
        canvas.save(); canvas.rotate(turn,x,y)
        val inset = paint.strokeWidth
        canvas.drawArc(inset,inset,width-inset,height-inset,15f,295f,false,paint)
        canvas.restore()
        if (!reduceMotion && isAttachedToWindow) postInvalidateOnAnimation()
    }
}
