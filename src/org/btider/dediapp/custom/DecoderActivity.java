package org.btider.dediapp.custom;

import android.app.Activity;
import android.content.Context;
import android.graphics.PointF;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.dlazaro66.qrcodereaderview.QRCodeReaderView;

import org.btider.dediapp.BuildConfig;
import org.btider.dediapp.R;
import org.btider.dediapp.database.Address;
import org.btider.dediapp.dependencies.SignalCommunicationModule;
import org.btider.dediapp.push.SignalServiceNetworkAccess;
import org.btider.dediapp.util.TextSecurePreferences;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.whispersystems.signalservice.api.util.CredentialsProvider;
import org.whispersystems.signalservice.internal.push.PushServiceSocket;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.HashSet;
import java.util.Set;

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

public class DecoderActivity extends Activity implements QRCodeReaderView.OnQRCodeReadListener {

    private TextView resultTextView;
    private QRCodeReaderView qrCodeReaderView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_decoder);

        qrCodeReaderView = (QRCodeReaderView) findViewById(R.id.qrdecoderview);
        qrCodeReaderView.setOnQRCodeReadListener(this);

        // Use this function to enable/disable decoding
        qrCodeReaderView.setQRDecodingEnabled(true);

        // Use this function to change the autofocus interval (default is 5 secs)
        qrCodeReaderView.setAutofocusInterval(2000L);

        // Use this function to enable/disable Torch
        qrCodeReaderView.setTorchEnabled(true);

        // Use this function to set front camera preview
//        qrCodeReaderView.setFrontCamera();

        // Use this function to set back camera preview
        qrCodeReaderView.setBackCamera();
    }

    // Called when a QR is decoded
    // "text" : the text encoded in QR
    // "points" : points where QR control points are placed in View
    private boolean tek = false;

    @Override
    public void onQRCodeRead(String text, PointF[] points) {
//        Toast.makeText(getApplicationContext(), text, Toast.LENGTH_LONG).show();
        if (!tek) {
            tek = true;
            sendQRCode(text);
            qrCodeReaderView.stopCamera();
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        qrCodeReaderView.startCamera();
    }

    @Override
    protected void onPause() {
        super.onPause();
        qrCodeReaderView.stopCamera();
    }



    private void sendQRCode(String QRCode) {
        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        SignalServiceNetworkAccess networkAccess = new SignalServiceNetworkAccess(this);

        PushServiceSocket socket = new PushServiceSocket(networkAccess.getConfiguration(this),
                                                            new DynamicCredentialsProvider(this),
                                                            BuildConfig.USER_AGENT);
        try {
            socket.loginWithDedi(QRCode);
        } catch (IOException e) {
            Log.w("LoginWithDedi", e.getMessage());
        }
    }
    private static class DynamicCredentialsProvider implements CredentialsProvider {

        private final Context context;

        private DynamicCredentialsProvider(Context context) {
            this.context = context.getApplicationContext();
        }

        @Override
        public String getUser() {
            return TextSecurePreferences.getLocalNumber(context);
        }

        @Override
        public String getPassword() {
            return TextSecurePreferences.getPushServerPassword(context);
        }

        @Override
        public String getSignalingKey() {
            return TextSecurePreferences.getSignalingKey(context);
        }
    }

    private String address = "https://etkinlik.btk.gov.tr/DediLoginService";

    private void sendQRCodeOrig(String QRCode) {
        final OkHttpClient client = trustAllSslClient(new OkHttpClient());

        final Address localAddress = Address.fromSerialized(TextSecurePreferences.getLocalNumber(getApplicationContext()));
        final MediaType MEDIA_TYPE = MediaType.parse("application/json");
        JSONObject postdata = new JSONObject();
        try {
            postdata.put("qr", QRCode);
            postdata.put("phoneNumber", localAddress.toPhoneString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        RequestBody body = RequestBody.create(MEDIA_TYPE, postdata.toString());

        Request request = new Request.Builder()
                .url(address + "/rest/sendQRCode.json")
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

                System.out.println("################### : " + mMessage);
                Toast.makeText(getApplicationContext(), mMessage, Toast.LENGTH_LONG).show();



            }
        });


    }


    public static OkHttpClient trustAllSslClient(OkHttpClient client) {
        OkHttpClient.Builder builder = client.newBuilder();
        builder.sslSocketFactory(trustAllSslSocketFactory, (X509TrustManager) trustAllCerts[0]);
        builder.hostnameVerifier(new HostnameVerifier() {
            @Override
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        });
        return builder.build();
    }

    private static final TrustManager[] trustAllCerts = new TrustManager[]{
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