package org.btider.dediapp.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import org.btider.dediapp.util.TextSecurePreferences;


public class PersistentConnectionBootListener extends BroadcastReceiver {
  @Override
  public void onReceive(Context context, Intent intent) {
    if (intent != null && Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
      if (TextSecurePreferences.isFcmDisabled(context)) {
        Intent serviceIntent = new Intent(context, MessageRetrievalService.class);
        serviceIntent.setAction(MessageRetrievalService.ACTION_INITIALIZE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(serviceIntent);
        else                                                context.startService(serviceIntent);
      }
    }
  }
}
