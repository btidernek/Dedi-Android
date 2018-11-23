package org.btider.dediapp;

import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.preference.ListPreference;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import org.btider.dediapp.database.Address;
import org.btider.dediapp.permissions.Permissions;
import org.btider.dediapp.util.DynamicLanguage;
import org.btider.dediapp.util.TextSecurePreferences;

import java.util.Arrays;
import java.util.Locale;


public class CommentInAppFragment extends Fragment {

  @SuppressWarnings("unused")
  private static final String TAG = CommentInAppFragment.class.getSimpleName();

  private ProgressDialog progressDialog;

  @Override
  public void onCreate(Bundle bundle) {
    super.onCreate(bundle);
  }

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle bundle) {
    View layout              = inflater.inflate(R.layout.activity_comment_inapp, container, false);
    WebView webview = layout.findViewById(R.id.webview);
    webview.getSettings().setJavaScriptEnabled(true);
    final Address localAddress = Address.fromSerialized(TextSecurePreferences.getLocalNumber(getContext()));


    String[] languageEntryValues = getResources().getStringArray(R.array.language_values);
    int langIndex  = Arrays.asList(languageEntryValues).indexOf(TextSecurePreferences.getLanguage(getContext()));
    String lang = "tr";
    if(langIndex == 0){
      if(!Locale.getDefault().getLanguage().equals("tr")){
        lang = "en";
      }
    }else if(langIndex == 1){
      lang = "en";
    }

    webview.loadUrl("http://etkinlik.btk.gov.tr/DediSoyle/r="+localAddress+"?language="+lang);
    webview.setVisibility(View.GONE);
    webview.setWebViewClient(new WebViewClient() {

      @Override
      public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
        //super.onReceivedError(view, errorCode, description, failingUrl);
        Toast.makeText(getActivity(), description, Toast.LENGTH_LONG).show();
        getActivity().finish();
      }

      public void onPageFinished(WebView view, String url) {
        // do your stuff here
        webview.setVisibility(View.VISIBLE);
        if(url.contains("close")){
          getActivity().finish();
        }
      }
    });

    return layout;
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


}