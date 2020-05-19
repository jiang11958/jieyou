package com.m.w.vpnservice;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.webkit.MimeTypeMap;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.m.w.vpnservice.utils.PermissionUtils;
import com.m.w.vpnservice.vpn.cert.CertificateHelper;
import com.m.w.vpnservice.vpn.util.VpnServiceHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.Set;

public class IndexActivity extends AppCompatActivity {

    private String TAG = "AdultBlock_MainActivity";

    private static final String STUDENT_URL = "http://121.196.15.118/student";
    private static final String PARENT_URL = "http://121.196.15.118/parent";
    private boolean isParent = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_index);
        initWebview();

        PermissionUtils.isGrantExternalRW(this, 1);
    }

    private WebView webview;

    private void initWebview() {
        webview = findViewById(R.id.webview);
        WebSettings settings = webview.getSettings();
        settings.setDatabaseEnabled(true);
        settings.setGeolocationEnabled(true);
        String dir = IndexActivity.this.getCacheDir() + "/data";
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
        setDownLoader();
    }

    private void setDownLoader() {
        webview.setDownloadListener(new android.webkit.DownloadListener() {
            @Override
            public void onDownloadStart(final String url, final String userAgent, final String contentDisposition, final String mimeType, long contentLength) {
                Log.d(TAG, "onDownloadStart: " + url);
                new AlertDialog.Builder(IndexActivity.this)
                        .setTitle("确定下载该文件吗？")
                        .setPositiveButton("确定",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog,
                                                        int which) {
                                        String fileName = URLUtil.guessFileName(url, contentDisposition, mimeType);
                                        String destPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                                                .getAbsolutePath() + File.separator + fileName;
                                        new DownloadTask().execute(url, destPath);
                                    }
                                })
                        .setNegativeButton("取消",
                                new DialogInterface.OnClickListener() {

                                    @Override
                                    public void onClick(DialogInterface dialog,
                                                        int which) {
                                    }
                                })
                        .setOnCancelListener(
                                new DialogInterface.OnCancelListener() {

                                    @Override
                                    public void onCancel(DialogInterface dialog) {
                                    }
                                }).show();

            }
        });
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
                    IndexActivity.IndexActivity.this.startActivityForResult(clientCertInstall, CertificateHelper.REGISTER_CLIENT_CERT);
                }catch (Exception ex){
                    Log.i(Tag, ex.toString());
                }/*/
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == FILE_CHOOESE) {
            if (resultCode == RESULT_OK) {
                switch (requestCode) {
                    case FILE_CHOOESE:
                        if (null != uploadFile) {
                            Uri result = data == null || resultCode != RESULT_OK ? null
                                    : data.getData();
                            uploadFile.onReceiveValue(result);
                            uploadFile = null;
                        }
                        if (null != uploadFiles) {
                            Uri result = data == null || resultCode != RESULT_OK ? null
                                    : data.getData();
                            uploadFiles.onReceiveValue(new Uri[]{result});
                            uploadFiles = null;
                        }
                        break;
                    default:
                        break;
                }
            } else if (resultCode == RESULT_CANCELED) {
                if (null != uploadFile) {
                    uploadFile.onReceiveValue(null);
                    uploadFile = null;
                }

            }
        } else if (requestCode == VpnServiceHelper.START_VPN_SERVICE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                VpnServiceHelper.startVpnService(this);
            } else {
                //DebugLog.d("canceled");
            }
            return;
        } else if (requestCode == CertificateHelper.REGISTER_CLIENT_CERT) {
            if (resultCode == RESULT_OK) {

            } else {

            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private ValueCallback<Uri> uploadFile;
    private ValueCallback<Uri[]> uploadFiles;

    class MyWebChromeClient extends WebChromeClient {

        // For Android 3.0+
        public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType) {
            Log.i("test", "openFileChooser 1");
            IndexActivity.this.uploadFile = uploadFile;
            openFileChooseProcess();
        }

        // For Android < 3.0
        public void openFileChooser(ValueCallback<Uri> uploadMsgs) {
            Log.i("test", "openFileChooser 2");
            IndexActivity.this.uploadFile = uploadFile;
            openFileChooseProcess();
        }

        // For Android  > 4.1.1
        public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture) {
            Log.i("test", "openFileChooser 3");
            IndexActivity.this.uploadFile = uploadFile;
            openFileChooseProcess();
        }

        // For Android  >= 5.0
        public boolean onShowFileChooser(WebView webView,
                                         ValueCallback<Uri[]> filePathCallback,
                                         FileChooserParams fileChooserParams) {
            Log.i("test", "openFileChooser 4:" + filePathCallback.toString());
            IndexActivity.this.uploadFiles = filePathCallback;
            openFileChooseProcess();
            return true;
        }

    }

    public class MyWebViewClient extends WebViewClient {
        @Override
        public void onReceivedError(WebView view, int errorCode,
                                    String description, String failingUrl) {
            Log.d(TAG, "onReceivedError: " + failingUrl);
            super.onReceivedError(view, errorCode, description, failingUrl);
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            Log.d(TAG, "onPageStarted: " + url);
            super.onPageStarted(view, url, favicon);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            Log.d(TAG, "onPageFinished: " + url);
            initVpn();
            super.onPageFinished(view, url);
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            Log.d(TAG, "shouldOverrideUrlLoading: " + url);
            if (url.startsWith("http:") || url.startsWith("https:")) {
                view.loadUrl(url);
            } else {
                return false;
            }

            //屏蔽掉错误的重定向url："baidumap://map/?src=webapp.default.all.callnaonopenwebapp?"
            return super.shouldOverrideUrlLoading(view, url);
        }

    }

    private boolean hasStart = false;

    private void initVpn() {
        //        getCA();
        if (!hasStart && !isParent) {
            //启动VPN服务
            if (!VpnServiceHelper.vpnRunningStatus()) {
                VpnServiceHelper.changeVpnRunningStatus(IndexActivity.this, true);
            }
            //启动监听
            initReceiver();
            hasStart = true;
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
                webview.loadUrl("javascript:uploadBrowsingHistory('" + domain + "')");
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

    private static final int FILE_CHOOESE = 123;

    private void openFileChooseProcess() {
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        startActivityForResult(Intent.createChooser(i, "选择文件"), FILE_CHOOESE);
    }


    private class DownloadTask extends AsyncTask<String, Void, Void> {
        // 传递两个参数：URL 和 目标路径
        private String url;
        private String destPath;

        @Override
        protected void onPreExecute() {
            Log.i(TAG, "开始下载");
        }

        @Override
        protected Void doInBackground(String... params) {
            Log.d(params[0], params[1]);
            url = params[0];
            destPath = params[1];
            OutputStream out = null;
            HttpURLConnection urlConnection = null;
            try {
                new File(destPath).createNewFile();
                URL url = new URL(params[0]);
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setConnectTimeout(15000);
                urlConnection.setReadTimeout(15000);
                InputStream in = urlConnection.getInputStream();
                out = new FileOutputStream(destPath);
                byte[] buffer = new byte[10 * 1024];
                int len;
                while ((len = in.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                }
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException e) {
                    }
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            Log.i(TAG, "完成下载");
            try {
                File file = new File(destPath);
                String mimeType = getMIMEType(url);
                Uri uri = Uri.fromFile(new File(destPath));
                Intent intent = new Intent(Intent.ACTION_VIEW);
                if (!file.exists()) {
                    return;
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    try {
                        String provider = getPackageName() + ".fileprovider";
                        Uri contentUri = FileProvider.getUriForFile(getApplicationContext(), provider, file);
                        intent.setAction(Intent.ACTION_VIEW);
                        intent.setDataAndType(contentUri, mimeType);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    intent.setDataAndType(uri, mimeType);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                }

                startActivity(intent);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private String getMIMEType(String url) {
        String type = null;
        String extension = MimeTypeMap.getFileExtensionFromUrl(url);
        Log.d("extension:{}", extension);
        if (extension != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }
        return type;
    }
}
