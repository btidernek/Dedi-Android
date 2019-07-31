package org.btider.dediapp.gcm;

import android.content.Context;
import android.os.PowerManager;
import android.support.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.btider.dediapp.ApplicationContext;
import org.btider.dediapp.R;
import org.btider.dediapp.dependencies.InjectableType;
import org.btider.dediapp.jobs.FcmRefreshJob;
import org.btider.dediapp.jobs.PushNotificationReceiveJob;
import org.btider.dediapp.notifications.NotificationChannels;
import org.btider.dediapp.service.GenericForegroundService;
import org.btider.dediapp.util.PowerManagerCompat;
import org.btider.dediapp.util.ServiceUtil;
import org.btider.dediapp.util.TextSecurePreferences;
import org.btider.dediapp.util.concurrent.SignalExecutors;
import org.whispersystems.jobqueue.requirements.NetworkRequirement;
import org.whispersystems.libsignal.logging.Log;
import org.whispersystems.signalservice.api.SignalServiceAccountManager;
import org.whispersystems.signalservice.api.SignalServiceMessageReceiver;
import org.whispersystems.signalservice.internal.util.Util;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;

public class FcmService extends FirebaseMessagingService implements InjectableType {

  private static final String TAG = FcmService.class.getSimpleName();

  private static final Executor MESSAGE_EXECUTOR = SignalExecutors.newCachedSingleThreadExecutor("FcmMessageProcessing");

  @Inject SignalServiceMessageReceiver messageReceiver;

  private static int activeCount;

  @Override
  public void onMessageReceived(RemoteMessage remoteMessage) {
    Log.i(TAG, "FCM message... Original Priority: " + remoteMessage.getOriginalPriority() + ", Actual Priority: " + remoteMessage.getPriority());
    ApplicationContext.getInstance(getApplicationContext()).injectDependencies(this);
    handleReceivedNotification(getApplicationContext());
  }

  @Override
  public void onNewToken(String token) {
    if (!TextSecurePreferences.isPushRegistered(getApplicationContext())) {
      Log.i(TAG, "Got a new FCM token, but the user isn't registered.");
      return;
    }

    ApplicationContext.getInstance(getApplicationContext())
                      .getJobManager()
                      .add(new FcmRefreshJob(getApplicationContext()));
  }

  private void handleReceivedNotification(Context context) {
    if (!incrementActiveGcmCount()) {
      Log.i(TAG, "Skipping FCM processing -- there's already one enqueued.");
      return;
    }

    TextSecurePreferences.setNeedsMessagePull(context, true);

    long         startTime    = System.currentTimeMillis();
    PowerManager powerManager = ServiceUtil.getPowerManager(getApplicationContext());
    boolean      doze         = PowerManagerCompat.isDeviceIdleMode(powerManager);
    boolean      network      = new NetworkRequirement(context).isPresent();

    final Object         foregroundLock    = new Object();
    final AtomicBoolean  foregroundRunning = new AtomicBoolean(false);
    final AtomicBoolean  taskCompleted     = new AtomicBoolean(false);
    final CountDownLatch latch             = new CountDownLatch(1);

    if (doze || !network) {
      Log.i(TAG, "Starting a foreground task because we may be operating in a constrained environment. Doze: " + doze + " Network: " + network);
      showForegroundNotification(context);
      foregroundRunning.set(true);
      latch.countDown();
    }

    MESSAGE_EXECUTOR.execute(() -> {
      try {
        new PushNotificationReceiveJob(context).pullAndProcessMessages(messageReceiver, TAG, startTime);
      } catch (IOException e) {
        Log.i(TAG, "Failed to retrieve the envelope. Scheduling on JobManager.", e);
        ApplicationContext.getInstance(context)
                          .getJobManager()
                          .add(new PushNotificationReceiveJob(context));
      } finally {
        synchronized (foregroundLock) {
          if (foregroundRunning.getAndSet(false)) {
            GenericForegroundService.stopForegroundTask(context);
          } else {
            latch.countDown();
          }
          taskCompleted.set(true);
        }

        decrementActiveGcmCount();
        Log.i(TAG, "Processing complete.");
      }
    });

    if (!foregroundRunning.get()) {
      new Thread("FcmForegroundServiceTimer") {
        @Override
        public void run() {
          Util.sleep(7000);
          synchronized (foregroundLock) {
            if (!taskCompleted.get() && !foregroundRunning.getAndSet(true)) {
              Log.i(TAG, "Starting a foreground task because the job is running long.");
              showForegroundNotification(context);
              latch.countDown();
            }
          }
        }
      }.start();
    }

    try {
      latch.await();
    } catch (InterruptedException e) {
      Log.w(TAG, "Latch was interrupted.", e);
    }
  }

  private void showForegroundNotification(@NonNull Context context) {
    GenericForegroundService.startForegroundTask(context,
                                                 "İndiriliyor, azıcık bekleyin.", //context.getString(R.string.GcmBroadcastReceiver_retrieving_a_message),
                                                 NotificationChannels.OTHER,
                                                 R.drawable.icon); //R.drawable.ic_signal_downloading);
  }

  private static synchronized boolean incrementActiveGcmCount() {
    if (activeCount < 2) {
      activeCount++;
      return true;
    }
    return false;
  }

  private static synchronized void decrementActiveGcmCount() {
    activeCount--;
  }
}