package ai.moechat.android.web

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewClientCompat
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import ai.moechat.android.R
import ai.moechat.android.model.SubApp
import org.json.JSONObject

/**
 * 子应用资源的 origin。与 macOS 宿主的 `moechat-app://` 对应，都指向「本地源」。
 *
 * 不能用 `file://`：Android WebView 在 file:// 下会因 CORS 拒绝 ES 模块，
 * 子应用的 `<script type="module">` 直接不执行。
 */
const val ASSET_ORIGIN = "https://appassets.androidplatform.net"

/** 入口路径 → 加载 URL。entry 是 assets 相对路径，见 SubApp.entry。 */
fun entryUrl(entry: String): String = "$ASSET_ORIGIN/assets/$entry"

/**
 * 宿主向子应用暴露的上下文。
 * 与 macOS 宿主的 HostContext.swift 字段一一对应，字段名不许各端各叫各的。
 */
data class HostContext(
    val subjectId: String,
    val subjectName: String,
    val spacetime: String,
    val theme: String,
    /** BCP-47，宿主系统语言，如 zh-Hans-CN。子应用按自己的兜底链解析。 */
    val locale: String,
) {
    /** 手写字符串拼接会漏转义，用 JSONObject。 */
    fun toJson(): String = JSONObject(
        mapOf(
            "subjectId" to subjectId,
            "subjectName" to subjectName,
            "spacetime" to spacetime,
            "theme" to theme,
            "locale" to locale,
        ),
    ).toString()
}

/**
 * 子应用 → 宿主的回调桥。
 *
 * Java 桥只能收 String，所以 JS 侧那一层的 `post(type, payload)` 由 [bootstrapScript]
 * 补上 stringify。这样四端对子应用暴露的是**同一份 JS 契约**：
 * `window.moechatHost.post(type, payloadObject)`。
 */
class Bridge(private val onMessage: (String, String) -> Unit) {
    @JavascriptInterface
    fun post(type: String, payloadJson: String) {
        onMessage(type, payloadJson)
    }
}

/**
 * 注入宿主上下文与通信函数。必须在 documentStart 执行——
 * 子应用的模块脚本是 deferred，跑起来时 `window.moechat` 必须已经在。
 */
private fun bootstrapScript(hostContext: HostContext): String = """
    (function () {
      window.moechat = ${hostContext.toJson()};
      window.moechatHost = {
        post: function (type, payload) {
          AndroidBridge.post(type, JSON.stringify(payload === undefined ? {} : payload));
        }
      };
    })();
""".trimIndent()

/**
 * 子应用的宿主容器。
 * 子应用是 Web 应用（各自独立仓库），跑在这里，与 macOS 端的 WKWebView 方案同理。
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebAppView(
    app: SubApp,
    hostContext: HostContext,
    onMessage: (String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    val assetLoader = remember(context) {
        WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(context))
            .build()
    }
    val script = remember(hostContext) { bootstrapScript(hostContext) }
    val injectable = remember { WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT) }
    val installed = remember(context, app) { isInstalled(context, app) }

    // 三种状态各自的兜底页文案。stringResource 只能在组合里调，不能进 update 闭包。
    val fallback = when {
        !injectable -> FallbackPage(
            title = stringResource(R.string.subapp_webview_too_old_title),
            hint = stringResource(R.string.subapp_webview_too_old_hint),
        )
        !installed -> FallbackPage(
            title = stringResource(R.string.subapp_missing_title, app.id),
            hint = stringResource(R.string.subapp_missing_hint, app.entry),
        )
        else -> null
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                // 让页面按 viewport meta 的 device-width 排版。缺这两项时 WebView 走默认布局，
                // 百分比高度会塌成 0。
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
                // 子应用一律走 assetloader 的 origin，不留 file:// 后门
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                setBackgroundColor(0xFF0A0B0D.toInt())

                // 注册名与 macOS 不同（那边是 webkit.messageHandlers），但暴露给 JS 的
                // window.moechatHost 是一致的，差异只留在这一层。
                addJavascriptInterface(Bridge(onMessage), "AndroidBridge")
                if (injectable) {
                    WebViewCompat.addDocumentStartJavaScript(this, script, setOf(ASSET_ORIGIN))
                }

                webViewClient = object : WebViewClientCompat() {
                    override fun shouldInterceptRequest(
                        view: WebView,
                        request: WebResourceRequest,
                    ): WebResourceResponse? = assetLoader.shouldInterceptRequest(request.url)
                }
            }
        },
        // factory 只在首次组合时执行，切换子应用必须在这里换页，否则内容不会更新。
        update = { webView ->
            val key = "${app.id}|${fallback != null}"
            if (webView.tag != key) {
                webView.tag = key
                if (fallback == null) {
                    webView.loadUrl(entryUrl(app.entry))
                } else {
                    webView.loadDataWithBaseURL(null, fallback.html(), "text/html", "utf-8", null)
                }
            }
        },
    )
}

/** 子应用加载不出来时显示的页面。宁可明确说清原因，也不要白屏。 */
private class FallbackPage(val title: String, val hint: String) {
    fun html(): String = """
        <!doctype html>
        <html><head><meta charset="utf-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <style>
          html, body {
            margin: 0; height: 100%;
            background: #0A0B0D; color: rgba(255,255,255,.92);
            font: 13px/1.6 -apple-system, "Noto Sans SC", sans-serif;
          }
          body { display: flex; align-items: center; justify-content: center; }
          .box { text-align: center; padding: 0 24px; }
          .sub { color: rgba(255,255,255,.38); font-size: 12px; margin-top: 8px; overflow-wrap: anywhere; }
          code { font-family: monospace; color: #3E9BD6; }
        </style></head>
        <body>
          <div class="box">
            <div style="font-size:15px;font-weight:500;">$title</div>
            <div class="sub"><code>$hint</code></div>
          </div>
        </body></html>
    """.trimIndent()
}

/** 子应用是否已「安装」= assets 里有没有它的入口文件。 */
private fun isInstalled(context: Context, app: SubApp): Boolean {
    val dir = app.entry.substringBeforeLast('/')
    val file = app.entry.substringAfterLast('/')
    return context.assets.list(dir)?.contains(file) == true
}
