/**
 * Copyright (C) 2014 Open Whisper Systems
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.btider.dediapp.jobs;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import org.btider.dediapp.PlayServicesProblemActivity;
import org.btider.dediapp.R;
import org.btider.dediapp.dependencies.InjectableType;
import org.btider.dediapp.gcm.FcmUtil;
import org.btider.dediapp.notifications.NotificationChannels;
import org.btider.dediapp.transport.RetryLaterException;
import org.btider.dediapp.util.TextSecurePreferences;
import org.whispersystems.jobqueue.JobParameters;
import org.whispersystems.jobqueue.requirements.NetworkRequirement;
import org.whispersystems.libsignal.util.guava.Optional;
import org.whispersystems.signalservice.api.SignalServiceAccountManager;
import org.whispersystems.signalservice.api.push.exceptions.NonSuccessfulResponseCodeException;

import java.io.IOException;

import javax.inject.Inject;

public class FcmRefreshJob extends ContextJob implements InjectableType {

  private static final String TAG = FcmRefreshJob.class.getSimpleName();

  public static final String REGISTRATION_ID = "853247069301";

  @Inject transient SignalServiceAccountManager textSecureAccountManager;

  public FcmRefreshJob(Context context) {
    super(context, JobParameters.newBuilder()
                                .withRequirement(new NetworkRequirement(context))
                                .withRetryCount(1)
                                .create());
  }

  @Override
  public void onAdded() {}

  @Override
  public void onRun() throws Exception {
    if (TextSecurePreferences.isFcmDisabled(context)) return;

    Log.i(TAG, "Reregistering FCM...");

    int result = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context);

    if (result != ConnectionResult.SUCCESS) {
      notifyFcmFailure();
    } else {
      Optional<String> token = FcmUtil.getToken();

      if (token.isPresent()) {
        textSecureAccountManager.setGcmId(token);
        TextSecurePreferences.setFcmToken(context, token.get());
        TextSecurePreferences.setFcmTokenLastSetTime(context, System.currentTimeMillis());
        TextSecurePreferences.setWebsocketRegistered(context, true);
      } else {
        throw new RetryLaterException(new IOException("Failed to retrieve a token."));
      }
    }
  }

  @Override
  public void onCanceled() {
    Log.w(TAG, "GCM reregistration failed after retry attempt exhaustion!");
  }

  @Override
  public boolean onShouldRetry(Exception throwable) {
    if (throwable instanceof NonSuccessfulResponseCodeException) return false;
    return true;
  }

  private void notifyFcmFailure() {
    Intent                     intent        = new Intent(context, PlayServicesProblemActivity.class);
    PendingIntent              pendingIntent = PendingIntent.getActivity(context, 1122, intent, PendingIntent.FLAG_CANCEL_CURRENT);
    NotificationCompat.Builder builder       = new NotificationCompat.Builder(context, NotificationChannels.OTHER);

    builder.setSmallIcon(R.drawable.icon_notification);
    builder.setLargeIcon(BitmapFactory.decodeResource(context.getResources(),
            R.drawable.ic_action_warning_red));
    builder.setContentTitle(context.getString(R.string.GcmRefreshJob_Permanent_Signal_communication_failure));
    builder.setContentText(context.getString(R.string.GcmRefreshJob_Signal_was_unable_to_register_with_Google_Play_Services));
    builder.setTicker(context.getString(R.string.GcmRefreshJob_Permanent_Signal_communication_failure));
    builder.setVibrate(new long[] {0, 1000});
    builder.setContentIntent(pendingIntent);

    ((NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE))
            .notify(12, builder.build());
  }

}
