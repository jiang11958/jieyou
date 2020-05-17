package com.m.w.vpnservice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.m.w.vpnservice.vpn.cert.CertificateHelper;
import com.m.w.vpnservice.vpn.util.VpnServiceHelper;

import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private String TAG = "AdultBlock_MainActivity";

    private static final String STUDENT_URL = "http://121.196.15.118/student";
    private static final String PARENT_URL = "http://121.196.15.118/parent";
    private boolean isParent = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initWebview();

//        getCA();
        if (!isParent) {
            //启动VPN服务
            if (!VpnServiceHelper.vpnRunningStatus()) {
                VpnServiceHelper.changeVpnRunningStatus(MainActivity.this, true);
            }
            //启动监听
            initReceiver();
        }
    }

    private WebView webview;

    private void initWebview() {
        webview = findViewById(R.id.webview);
        WebSettings settings = webview.getSettings();
        settings.setDatabaseEnabled(true);
        settings.setGeolocationEnabled(true);
        String dir = this.getCacheDir() + "/data";
        settings.setGeolocationDatabasePath(dir);
        settings.setDomStorageEnabled(true);
        settings.setJavaScriptEnabled(true);// 允许请求JS
        settings.setBuiltInZoomControls(true);
        webview.setWebChromeClient(new MyWebChromeClient());
        webview.setWebViewClient(new MyWebViewClient());
        webview.requestFocus();
        if (isParent) {
            webview.loadUrl(PARENT_URL);
        } else {
            webview.loadUrl(STUDENT_URL);
        }
    }

    private void getCA() {
        CertificateHelper.GetCA(this, new CertificateHelper.onGetCAResponse() {
            @Override
            public void processFinish(X509Certificate cert) {
                if (cert == null) {
                    Log.i(TAG, "KeyStore == Null");
                    return;
                }
                /*/try{
                    Intent clientCertInstall = KeyChain.createInstallIntent();
                    clientCertInstall.putExtra(KeyChain.EXTRA_CERTIFICATE, cert.getEncoded());
                    clientCertInstall.putExtra(KeyChain.EXTRA_NAME, "Adult Block");
                    MainActivity.this.startActivityForResult(clientCertInstall, CertificateHelper.REGISTER_CLIENT_CERT);
                }catch (Exception ex){
                    Log.i(Tag, ex.toString());
                }/*/
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == VpnServiceHelper.START_VPN_SERVICE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                VpnServiceHelper.startVpnService(this);
            } else {
                //DebugLog.e("canceled");
            }
            return;
        } else if (requestCode == CertificateHelper.REGISTER_CLIENT_CERT) {
            if (resultCode == RESULT_OK) {

            } else {

            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }


    class MyWebChromeClient extends WebChromeClient {

    }

    public class MyWebViewClient extends WebViewClient {
        @Override
        public void onReceivedError(WebView view, int errorCode,
                                    String description, String failingUrl) {
            Log.e(TAG, "onReceivedError: " + failingUrl);
            super.onReceivedError(view, errorCode, description, failingUrl);
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            Log.e(TAG, "onPageStarted: " + url);
            super.onPageStarted(view, url, favicon);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            Log.e(TAG, "onPageFinished: " + url);
            super.onPageFinished(view, url);
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            Log.e(TAG, "shouldOverrideUrlLoading: " + url);
            if (url.startsWith("http:") || url.startsWith("https:")) {
                view.loadUrl(url);
            } else {
                return false;
            }

            //屏蔽掉错误的重定向url："baidumap://map/?src=webapp.default.all.callnaonopenwebapp?"
            return super.shouldOverrideUrlLoading(view, url);
        }

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK
                && event.getAction() == KeyEvent.ACTION_DOWN) {
            if (back(1)) {
                return super.onKeyDown(keyCode, event);
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private long exitTime = 0;

    private boolean back(int type) {
        try {
            if (webview.canGoBack()) {
                webview.goBack();
            } else if (type == 1) {
                if ((System.currentTimeMillis() - exitTime) > 2000) {
                    Toast.makeText(getApplicationContext(), "再按一次退出",
                            Toast.LENGTH_SHORT).show();
                    exitTime = System.currentTimeMillis();
                } else {
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;

    }

    Set<String> urls;

    public class LocalReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            try {
                String domain = intent.getStringExtra("DOMAIN");
                if (!TextUtils.isEmpty(domain)) {
                    urls.add(domain);
                }
                Log.d(TAG, "onReceive: domain:" + domain + " " + urls.size());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private LocalReceiver localReceiver;

    private void initReceiver() {
        try {
            urls = new HashSet<>();
            localReceiver = new LocalReceiver();
            LocalBroadcastManager.getInstance(getBaseContext())
                    .registerReceiver(localReceiver, new IntentFilter("CAPTURE_DOMAIN"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        try {
            LocalBroadcastManager.getInstance(getBaseContext()).unregisterReceiver(localReceiver);
        } catch (Exception e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }
}
