package org.btider.dediapp.service;


import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;

import org.btider.dediapp.ConversationListActivity;
import org.btider.dediapp.R;
import org.btider.dediapp.notifications.NotificationChannels;

import java.util.concurrent.atomic.AtomicInteger;

public class GenericForegroundService extends Service {

  private static final int    NOTIFICATION_ID = 827353982;
  private static final String EXTRA_TITLE     = "extra_title";
  private static final String EXTRA_CHANNEL_ID = "extra_channel_id";
  private static final String EXTRA_ICON_RES   = "extra_icon_res";

  private static final String ACTION_START = "start";
  private static final String ACTION_STOP  = "stop";

  private final AtomicInteger foregroundCount = new AtomicInteger(0);

  @Override
  public void onCreate() {

  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    if      (intent != null && ACTION_START.equals(intent.getAction())) handleStart(intent);
    else if (intent != null && ACTION_STOP.equals(intent.getAction()))  handleStop();

    return START_NOT_STICKY;
  }


  private void handleStart(@NonNull Intent intent) {
    String title = intent.getStringExtra(EXTRA_TITLE);
    assert title != null;

    if (foregroundCount.getAndIncrement() == 0) {
      startForeground(NOTIFICATION_ID, new NotificationCompat.Builder(this, NotificationChannels.OTHER)
          .setSmallIcon(R.drawable.ic_signal_grey_24dp)
          .setContentTitle(title)
          .setContentIntent(PendingIntent.getActivity(this, 0, new Intent(this, ConversationListActivity.class), 0))
          .build());
    }
  }

  private void handleStop() {
    if (foregroundCount.decrementAndGet() == 0) {
      stopForeground(true);
      stopSelf();
    }
  }

  @Nullable
  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

  public static void startForegroundTask(@NonNull Context context, @NonNull String task) {
    startForegroundTask(context, task, NotificationChannels.OTHER);
  }

  public static void startForegroundTask(@NonNull Context context, @NonNull String task, @NonNull String channelId) {
    startForegroundTask(context, task, channelId, R.drawable.ic_signal_grey_24dp);
  }

  public static void startForegroundTask(@NonNull Context context, @NonNull String task, @NonNull String channelId, @DrawableRes int iconRes) {
    Intent intent = new Intent(context, GenericForegroundService.class);
    intent.setAction(ACTION_START);
    intent.putExtra(EXTRA_TITLE, task);
    intent.putExtra(EXTRA_CHANNEL_ID, channelId);
    intent.putExtra(EXTRA_ICON_RES, iconRes);

    ContextCompat.startForegroundService(context, intent);
  }

  public static void stopForegroundTask(@NonNull Context context) {
    Intent intent = new Intent(context, GenericForegroundService.class);
    intent.setAction(ACTION_STOP);

    ContextCompat.startForegroundService(context, intent);
  }
}
