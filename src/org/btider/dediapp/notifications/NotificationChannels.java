package org.btider.dediapp.notifications;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import org.btider.dediapp.BuildConfig;
import org.btider.dediapp.R;
import org.btider.dediapp.database.RecipientDatabase;
import org.btider.dediapp.util.TextSecurePreferences;

import java.util.Arrays;

public class NotificationChannels {

  private static final String CATEGORY_MESSAGES = "messages";

//  public static final String CALLS         = "calls_v2";
//  public static final String FAILURES      = "failures";
//  public static final String APP_UPDATES   = "app_updates";
//  public static final String BACKUPS       = "backups_v2";
//  public static final String LOCKED_STATUS = "locked_status_v2";

  public static final String OTHER         = "other_v2";

  public static boolean supported() {
    return Build.VERSION.SDK_INT >= 26;
  }

  public static void create(@NonNull Context context) {
    if (!supported()) {
      return;
    }

    NotificationManager notificationManager = getNotificationManager(context);

    onCreate(context, notificationManager);
  }

  @TargetApi(26)
  private static @NonNull NotificationManager getNotificationManager(@NonNull Context context) {
    NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    assert  notificationManager != null;
      return notificationManager;
  }

  @TargetApi(26)
  private static void onCreate(@NonNull Context context, @NonNull NotificationManager notificationManager) {
    System.out.println("GELDIIIIIIII");
    NotificationChannelGroup messagesGroup = new NotificationChannelGroup(CATEGORY_MESSAGES, context.getResources().getString(R.string.NotificationChannel_group_messages));
    notificationManager.createNotificationChannelGroup(messagesGroup);

    //NotificationChannel messages     = new NotificationChannel(getMessagesChannel(context), context.getString(R.string.NotificationChannel_messages), NotificationManager.IMPORTANCE_HIGH);
//    NotificationChannel calls        = new NotificationChannel(CALLS, context.getString(R.string.NotificationChannel_calls), NotificationManager.IMPORTANCE_LOW);
//    NotificationChannel failures     = new NotificationChannel(FAILURES, context.getString(R.string.NotificationChannel_failures), NotificationManager.IMPORTANCE_HIGH);
//    NotificationChannel appUpdates   = new NotificationChannel(APP_UPDATES, context.getString(R.string.NotificationChannel_app_updates), NotificationManager.IMPORTANCE_LOW);
//    NotificationChannel backups      = new NotificationChannel(BACKUPS, context.getString(R.string.NotificationChannel_backups), NotificationManager.IMPORTANCE_LOW);
//    NotificationChannel lockedStatus = new NotificationChannel(LOCKED_STATUS, context.getString(R.string.NotificationChannel_locked_status), NotificationManager.IMPORTANCE_LOW);
    NotificationChannel other        = new NotificationChannel(OTHER, context.getString(R.string.NotificationChannel_other), NotificationManager.IMPORTANCE_HIGH);

    other.setGroup(CATEGORY_MESSAGES);
    other.setLightColor(Color.parseColor(TextSecurePreferences.getNotificationLedColor(context)));
    setLedPreference(other, TextSecurePreferences.getNotificationLedColor(context));

    Uri defaultRingtone = TextSecurePreferences.getNotificationRingtone(context);
    boolean defaultVibrate  = TextSecurePreferences.isNotificationVibrateEnabled(context);
    other.enableVibration(defaultVibrate);
    AudioAttributes audioAttributes = new AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
            .build();
    other.setSound(defaultRingtone,audioAttributes);
    other.setVibrationPattern(new long[]{0, 400, 200, 400});

    other.setShowBadge(true);

    notificationManager.createNotificationChannels(Arrays.asList(other));

//    if (BuildConfig.PLAY_STORE_DISABLED) {
//      NotificationChannel appUpdates = new NotificationChannel(APP_UPDATES, context.getString(R.string.NotificationChannel_app_updates), NotificationManager.IMPORTANCE_HIGH);
//      notificationManager.createNotificationChannel(appUpdates);
//    } else {
//      notificationManager.deleteNotificationChannel(APP_UPDATES);
//    }
  }

  @TargetApi(26)
  private static void setLedPreference(@NonNull NotificationChannel channel, @NonNull String ledColor) {
    if ("none".equals(ledColor)) {
      channel.enableLights(false);
    } else {
      channel.enableLights(true);
      channel.setLightColor(Color.parseColor(ledColor));
    }
  }

  @TargetApi(21)
  private static AudioAttributes getRingtoneAudioAttributes() {
    return new AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_UNKNOWN)
            .setUsage(AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_INSTANT)
            .build();
  }
}
