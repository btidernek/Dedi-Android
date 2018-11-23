package org.btider.dediapp.jobs;

import android.app.Activity;
import android.content.Context;
import android.telephony.SmsManager;
import android.util.Log;

import org.btider.dediapp.crypto.MasterSecret;
import org.btider.dediapp.database.DatabaseFactory;
import org.btider.dediapp.database.NoSuchMessageException;
import org.btider.dediapp.database.SmsDatabase;
import org.btider.dediapp.database.model.SmsMessageRecord;
import org.btider.dediapp.jobs.requirements.MasterSecretRequirement;
import org.btider.dediapp.notifications.MessageNotifier;
import org.btider.dediapp.service.SmsDeliveryListener;
import org.btider.dediapp.ApplicationContext;
import org.btider.dediapp.crypto.MasterSecret;
import org.btider.dediapp.database.DatabaseFactory;
import org.btider.dediapp.database.NoSuchMessageException;
import org.btider.dediapp.database.SmsDatabase;
import org.btider.dediapp.database.model.SmsMessageRecord;
import org.btider.dediapp.jobs.requirements.MasterSecretRequirement;
import org.btider.dediapp.notifications.MessageNotifier;
import org.btider.dediapp.service.SmsDeliveryListener;
import org.whispersystems.jobqueue.JobParameters;

public class SmsSentJob extends MasterSecretJob {

  private static final String TAG = SmsSentJob.class.getSimpleName();

  private final long   messageId;
  private final String action;
  private final int    result;

  public SmsSentJob(Context context, long messageId, String action, int result) {
    super(context, JobParameters.newBuilder()
                                .withPersistence()
                                .withRequirement(new MasterSecretRequirement(context))
                                .create());

    this.messageId = messageId;
    this.action    = action;
    this.result    = result;
  }

  @Override
  public void onAdded() {

  }

  @Override
  public void onRun(MasterSecret masterSecret) {
    Log.w(TAG, "Got SMS callback: " + action + " , " + result);

    switch (action) {
      case SmsDeliveryListener.SENT_SMS_ACTION:
        handleSentResult(messageId, result);
        break;
      case SmsDeliveryListener.DELIVERED_SMS_ACTION:
        handleDeliveredResult(messageId, result);
        break;
    }
  }

  @Override
  public boolean onShouldRetryThrowable(Exception throwable) {
    return false;
  }

  @Override
  public void onCanceled() {

  }

  private void handleDeliveredResult(long messageId, int result) {
    DatabaseFactory.getSmsDatabase(context).markStatus(messageId, result);
  }

  private void handleSentResult(long messageId, int result) {
    try {
      SmsDatabase database = DatabaseFactory.getSmsDatabase(context);
      SmsMessageRecord record   = database.getMessage(messageId);

      switch (result) {
        case Activity.RESULT_OK:
          database.markAsSent(messageId, false);
          break;
        case SmsManager.RESULT_ERROR_NO_SERVICE:
        case SmsManager.RESULT_ERROR_RADIO_OFF:
          Log.w(TAG, "Service connectivity problem, requeuing...");
          ApplicationContext.getInstance(context)
              .getJobManager()
              .add(new SmsSendJob(context, messageId, record.getIndividualRecipient().getAddress().serialize()));
          break;
        default:
          database.markAsSentFailed(messageId);
          MessageNotifier.notifyMessageDeliveryFailed(context, record.getRecipient(), record.getThreadId());
      }
    } catch (NoSuchMessageException e) {
      Log.w(TAG, e);
    }
  }
}
