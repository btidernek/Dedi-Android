package org.btider.dediapp;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.http.SslCertificate;
import android.net.http.SslError;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.SslErrorHandler;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import org.btider.dediapp.database.Address;
import org.btider.dediapp.permissions.Permissions;
import org.btider.dediapp.util.TextSecurePreferences;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static android.net.http.SslError.SSL_UNTRUSTED;
import static org.spongycastle.crypto.tls.HandshakeType.certificate;


public class ServiceWebAppFragment extends Fragment {

    @SuppressWarnings("unused")
    private static final String TAG = ServiceWebAppFragment.class.getSimpleName();

    private ProgressDialog progressDialog;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
    }

    private static WebView webview;
    private static Activity activity;

    private static String address = "https://dediservicesweb.btk.gov.tr/DediServiceWeb";
//    private static String address = "http://test.dedi.com.tr/DediServiceWeb";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle bundle) {
        View layout = inflater.inflate(R.layout.activity_comment_inapp, container, false);
        webview = layout.findViewById(R.id.webview);
        webview.getSettings().setJavaScriptEnabled(true);
        final Address localAddress = Address.fromSerialized(TextSecurePreferences.getLocalNumber(getContext()));

        activity = getActivity();

        getProductInfo();

        String[] languageEntryValues = getResources().getStringArray(R.array.language_values);
        int langIndex = Arrays.asList(languageEntryValues).indexOf(TextSecurePreferences.getLanguage(getContext()));
        String lang = "tr";
        if (langIndex == 0) {
            if (!Locale.getDefault().getLanguage().equals("tr")) {
                lang = "en";
            }
        } else if (langIndex == 1) {
            lang = "en";
        }

        webview.setVisibility(View.GONE);

//        webview.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
        webview.clearCache(true);
        webview.getSettings().setAppCacheEnabled(false);
        webview.getSettings().setDomStorageEnabled(true);
        webview.addJavascriptInterface(new WebAppInterface(getContext()), "Android");

        Map<String, String> extraHeaders = new HashMap<String, String>();
        extraHeaders.put("code", "879+zqw&e134*M00O08552BTider*&a");

        //webview.loadUrl(address+"/dashboard?phone=" + localAddress.toPhoneString(), extraHeaders);//TEST
        webview.loadUrl(address+"/dashboard?phone=" + localAddress.toPhoneString() + "&os=android", extraHeaders);//PROD

        webview.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                SslCertificate serverCertificate = error.getCertificate();
                if (error.hasError(SSL_UNTRUSTED)) {
                    // Check if Cert-Domain equals the Uri-Domain
                    String certDomain = serverCertificate.getIssuedTo().getCName();
                    certDomain = certDomain.replace("*","dediservicesweb");
                    try {
                        if(certDomain.equals(new URL(error.getUrl()).getHost())) {
                            handler.proceed();
                        }
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                        handler.cancel();
                    }
                }
                else {
                    super.onReceivedSslError(view, handler, error);
                    handler.cancel();
                    Toast.makeText(getContext(),"Error",Toast.LENGTH_SHORT).show();
                    activity.finish();
                }

//                handler.proceed(); // Ignore SSL certificate errors
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                webview.setVisibility(View.GONE);
            }

            @Override
            public void onLoadResource(WebView view, String url) {
                super.onLoadResource(view, url);
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                //super.onReceivedError(view, errorCode, description, failingUrl);
                Toast.makeText(getActivity(), description, Toast.LENGTH_LONG).show();
                getActivity().finish();
            }

            public void onPageFinished(WebView view, String url) {
                // do your stuff here
                updateDisplay();

                if (url.contains("close")) {
                    getActivity().finish();
                }
            }
        });

        return layout;
    }

    public void updateDisplay() {
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                webview.setVisibility(View.VISIBLE);
            }
        }, 500);
    }


    public class WebAppInterface {
        Context mContext;

        WebAppInterface(Context c) {
            mContext = c;
        }

        @JavascriptInterface
        public void dediSender(String message) {
            handleForwardMessage(message);
        }
    }

    private void handleForwardMessage(String message) {
        Intent composeIntent = new Intent(getActivity(), ShareActivity.class);
        composeIntent.putExtra(Intent.EXTRA_TEXT, message.toString());
        startActivity(composeIntent);
    }


    public static void onBackPressed() {
        if (webview.canGoBack()) {
            webview.goBack();
        } else {
            activity.finish();
        }
    }

    private void getProductInfo() {
        final OkHttpClient client = trustAllSslClient(new OkHttpClient());

        final MediaType MEDIA_TYPE = MediaType.parse("application/json");
        JSONObject postdata = new JSONObject();
        try {
            postdata.put("data", "879+zqw&e134*M00O08552BTider*&a");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        RequestBody body = RequestBody.create(MEDIA_TYPE, postdata.toString());

        Request request = new Request.Builder()
                .url(address+"/rest/getRemoteProduct")
                .post(body)
                .addHeader("Content-Type", "application/json")
                .addHeader("cache-control", "no-cache")
                .build();



        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                String mMessage = e.getMessage().toString();
                Log.w("failure Response", mMessage);
                //call.cancel();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                String mMessage = response.body().string();

                if (response.isSuccessful()) {
                    try {

                        JSONArray json = new JSONArray(mMessage);
                        for (int i = 0; i < json.length(); i++) {
                            JSONObject object = json.getJSONObject(i);

                            String number = object.getString("number");
                            JSONObject product = object.getJSONObject("product");
                            Set<String> strings = new HashSet<>();
                            String data = product.getString("name") + "," + product.getString("color");
                            if (number != null && number.length() > 0) {
                                TextSecurePreferences.setServiceProductPreference(getContext(), number, data);
                            }
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }
            }
        });


    }


    @Override
    public void onDestroy() {
        super.onDestroy();

        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
            progressDialog = null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Permissions.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
    }


    public static OkHttpClient trustAllSslClient(OkHttpClient client) {
        OkHttpClient.Builder builder = client.newBuilder();
        builder.sslSocketFactory(trustAllSslSocketFactory, (X509TrustManager)trustAllCerts[0]);
        builder.hostnameVerifier(new HostnameVerifier() {
            @Override
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        });
        return builder.build();
    }

    private static final TrustManager[] trustAllCerts = new TrustManager[] {
            new X509TrustManager() {
                @Override
                public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                }

                @Override
                public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                }

                @Override
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return new java.security.cert.X509Certificate[]{};
                }
            }
    };
    private static final SSLContext trustAllSslContext;
    static {
        try {
            trustAllSslContext = SSLContext.getInstance("SSL");
            trustAllSslContext.init(null, trustAllCerts, new java.security.SecureRandom());
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new RuntimeException(e);
        }
    }
    private static final SSLSocketFactory trustAllSslSocketFactory = trustAllSslContext.getSocketFactory();
}