package top.easelink.lcg.service.web;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.net.http.SslError;
import android.os.Build;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.SslErrorHandler;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import timber.log.Timber;
import top.easelink.lcg.appinit.LCGApp;

import static top.easelink.lcg.utils.CookieUtilsKt.updateCookies;

/**
 * author : junzhang
 * date   : 2019-07-23 13:58
 * desc   : can be a back-up method in case of Jsoup doesn't work
 */
@SuppressLint({"SetJavaScriptEnabled", "StaticFieldLeak"})
public class WebViewWrapper {

    private static final String HOOK_NAME = "hook";
    private static volatile WebViewWrapper instance;
    private final WebView mWebView;

    private WebViewWrapper() {
        mWebView = new WebView(LCGApp.getContext());
        mWebView.setWebViewClient(new InnerWebViewClient());
        updateWebViewSettings();
    }

    public static void init() {
        getInstance();
    }

    public static WebViewWrapper getInstance() {
        if (instance == null) {
            synchronized (WebViewWrapper.class) {
                if (instance == null) {
                    instance = new WebViewWrapper();
                }
            }
        }
        return instance;
    }

    public void post(String url, HookInterface hookInterface) {
        mWebView.post(() -> {
            mWebView.removeJavascriptInterface(HOOK_NAME);
            mWebView.addJavascriptInterface(hookInterface, HOOK_NAME);
            mWebView.loadUrl(url);
        });
    }

    public void loadUrl(String url, HookInterface hookInterface) {
        mWebView.removeJavascriptInterface(HOOK_NAME);
        mWebView.addJavascriptInterface(hookInterface, HOOK_NAME);
        mWebView.loadUrl(url);
    }

    @SuppressWarnings("deprecation")
    public void clearCookies() {
        mWebView.clearCache(true);
        mWebView.clearHistory();
        mWebView.clearFormData();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            CookieManager.getInstance().removeAllCookies(null);
            CookieManager.getInstance().flush();
        } else {
            CookieSyncManager cookieSyncMngr = CookieSyncManager.createInstance(mWebView.getContext());
            cookieSyncMngr.startSync();
            CookieManager cookieManager = CookieManager.getInstance();
            cookieManager.removeAllCookie();
            cookieManager.removeSessionCookie();
            cookieSyncMngr.stopSync();
            cookieSyncMngr.sync();
        }
    }

    private void updateWebViewSettings() {
        WebSettings settings = mWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setBlockNetworkImage(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        settings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);
        settings.setDefaultTextEncodingName("UTF-8");
        settings.setBuiltInZoomControls(false);
        settings.setSupportZoom(false);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        }

        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
    }

    private static class InnerWebViewClient extends WebViewClient {
        @Override
        public void onPageFinished(WebView view, String url) {
            CookieManager cookieManager = CookieManager.getInstance();
            String cookieUrl = cookieManager.getCookie(url);
            Timber.i("Cookie : %s", cookieUrl);
            if (cookieUrl != null) {
                updateCookies(cookieUrl, false);
            }
            view.loadUrl("javascript:" + HOOK_NAME + ".processHtml(document.documentElement.outerHTML);");
            super.onPageFinished(view, url);
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
        }

        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
            handler.proceed();
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(url);
            return true;
        }
    }
}