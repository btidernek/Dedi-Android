package org.btider.dediapp.gcm;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.text.TextUtils;
import android.util.Log;


import org.btider.dediapp.ApplicationContext;
import org.btider.dediapp.jobs.PushContentReceiveJob;
import org.btider.dediapp.jobs.PushNotificationReceiveJob;
import org.btider.dediapp.util.TextSecurePreferences;

public class GcmBroadcastReceiver { //extends WakefulBroadcastReceiver {

//  private static final String TAG = GcmBroadcastReceiver.class.getSimpleName();
//
//  @Override
//  public void onReceive(Context context, Intent intent) {
//    GoogleCloudMessaging gcm         = GoogleCloudMessaging.getInstance(context);
//    String               messageType = gcm.getMessageType(intent);
//
//    if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {
//      Log.w(TAG, "GCM message...");
//
//      if (!TextSecurePreferences.isPushRegistered(context)) {
//        Log.w(TAG, "Not push registered!");
//        return;
//      }
//
//      String receiptData = intent.getStringExtra("receipt");
//
//      if      (!TextUtils.isEmpty(receiptData)) handleReceivedMessage(context, receiptData);
//      else if (intent.hasExtra("notification")) handleReceivedNotification(context);
//    }
//  }
//
//  private void handleReceivedMessage(Context context, String data) {
//    ApplicationContext.getInstance(context)
//                      .getJobManager()
//                      .add(new PushContentReceiveJob(context, data));
//  }
//
//  private void handleReceivedNotification(Context context) {
//    ApplicationContext.getInstance(context)
//                      .getJobManager()
//                      .add(new PushNotificationReceiveJob(context));
//  }
}