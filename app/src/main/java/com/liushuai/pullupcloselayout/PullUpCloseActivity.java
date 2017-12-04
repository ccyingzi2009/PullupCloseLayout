package com.liushuai.pullupcloselayout;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class PullUpCloseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pull_up_close);
        initWebview();
        PullupCloseLayout pullupCloseLayout = findViewById(R.id.pullUpLayout);
        pullupCloseLayout.registerPullUpListener(new PullupCloseLayout.PullUpListener() {
            @Override
            public void pullUp(boolean close) {
                if (close) {
                    finish();
                    PullUpCloseActivity.this.overridePendingTransition(R.anim.base_stay_orig, android.R.anim.fade_out);
                }
            }
        });
    }

    private void initWebview() {
        WebView webView = findViewById(R.id.webview);

        webView.setWebViewClient(new WebViewClient(){
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return true;
            }
        });

        WebSettings mWebSettings = webView.getSettings();
        mWebSettings.setSupportZoom(true);
        mWebSettings.setLoadWithOverviewMode(true);
        mWebSettings.setUseWideViewPort(true);
        mWebSettings.setJavaScriptEnabled(true);
        mWebSettings.setSupportMultipleWindows(true);
        mWebSettings.setLoadsImagesAutomatically(true);
        mWebSettings.setDomStorageEnabled(true);
        mWebSettings.setDatabaseEnabled(true);
        mWebSettings.setAppCacheEnabled(true);

        webView.loadUrl("https://github.com");
    }
}
