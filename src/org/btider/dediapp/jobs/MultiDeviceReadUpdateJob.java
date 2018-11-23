package org.btider.dediapp.jobs;

import android.content.Context;
import android.util.Log;

import org.btider.dediapp.crypto.MasterSecret;
import org.btider.dediapp.database.MessagingDatabase;
import org.btider.dediapp.dependencies.InjectableType;
import org.btider.dediapp.jobs.requirements.MasterSecretRequirement;
import org.btider.dediapp.crypto.MasterSecret;
import org.btider.dediapp.database.MessagingDatabase.SyncMessageId;
import org.btider.dediapp.dependencies.InjectableType;
import org.btider.dediapp.jobs.requirements.MasterSecretRequirement;
import org.btider.dediapp.util.TextSecurePreferences;
import org.whispersystems.jobqueue.JobParameters;
import org.whispersystems.jobqueue.requirements.NetworkRequirement;
import org.whispersystems.signalservice.api.SignalServiceMessageSender;
import org.whispersystems.signalservice.api.crypto.UntrustedIdentityException;
import org.whispersystems.signalservice.api.messages.multidevice.ReadMessage;
import org.whispersystems.signalservice.api.messages.multidevice.SignalServiceSyncMessage;
import org.whispersystems.signalservice.api.push.exceptions.PushNetworkException;

import java.io.IOException;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;

public class MultiDeviceReadUpdateJob extends MasterSecretJob implements InjectableType {

  private static final long serialVersionUID = 1L;
  private static final String TAG = MultiDeviceReadUpdateJob.class.getSimpleName();

  private final List<SerializableSyncMessageId> messageIds;

  @Inject transient SignalServiceMessageSender messageSender;

  public MultiDeviceReadUpdateJob(Context context, List<MessagingDatabase.SyncMessageId> messageIds) {
    super(context, JobParameters.newBuilder()
                                .withRequirement(new NetworkRequirement(context))
                                .withRequirement(new MasterSecretRequirement(context))
                                .withPersistence()
                                .create());

    this.messageIds = new LinkedList<>();

    for (MessagingDatabase.SyncMessageId messageId : messageIds) {
      this.messageIds.add(new SerializableSyncMessageId(messageId.getAddress().toPhoneString(), messageId.getTimetamp()));
    }
  }


  @Override
  public void onRun(MasterSecret masterSecret) throws IOException, UntrustedIdentityException {
    if (!TextSecurePreferences.isMultiDevice(context)) {
      Log.w(TAG, "Not multi device...");
      return;
    }

    List<ReadMessage> readMessages = new LinkedList<>();

    for (SerializableSyncMessageId messageId : messageIds) {
      readMessages.add(new ReadMessage(messageId.sender, messageId.timestamp));
    }

    messageSender.sendMessage(SignalServiceSyncMessage.forRead(readMessages));
  }

  @Override
  public boolean onShouldRetryThrowable(Exception exception) {
    return exception instanceof PushNetworkException;
  }

  @Override
  public void onAdded() {

  }

  @Override
  public void onCanceled() {

  }

  private static class SerializableSyncMessageId implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String sender;
    private final long   timestamp;

    private SerializableSyncMessageId(String sender, long timestamp) {
      this.sender = sender;
      this.timestamp = timestamp;
    }
  }
}
