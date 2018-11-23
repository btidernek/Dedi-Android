package org.btider.dediapp.jobs;

import android.content.Context;
import android.util.Log;

import org.btider.dediapp.database.Address;
import org.btider.dediapp.database.DatabaseFactory;
import org.btider.dediapp.database.NoSuchMessageException;
import org.btider.dediapp.database.SmsDatabase;
import org.btider.dediapp.database.model.SmsMessageRecord;
import org.btider.dediapp.dependencies.InjectableType;
import org.btider.dediapp.notifications.MessageNotifier;
import org.btider.dediapp.recipients.Recipient;
import org.btider.dediapp.service.ExpiringMessageManager;
import org.btider.dediapp.transport.InsecureFallbackApprovalException;
import org.btider.dediapp.transport.RetryLaterException;
import org.btider.dediapp.ApplicationContext;
import org.btider.dediapp.database.Address;
import org.btider.dediapp.database.DatabaseFactory;
import org.btider.dediapp.database.NoSuchMessageException;
import org.btider.dediapp.database.SmsDatabase;
import org.btider.dediapp.database.model.SmsMessageRecord;
import org.btider.dediapp.dependencies.InjectableType;
import org.btider.dediapp.notifications.MessageNotifier;
import org.btider.dediapp.recipients.Recipient;
import org.btider.dediapp.service.ExpiringMessageManager;
import org.btider.dediapp.transport.InsecureFallbackApprovalException;
import org.btider.dediapp.transport.RetryLaterException;
import org.whispersystems.libsignal.util.guava.Optional;
import org.whispersystems.signalservice.api.SignalServiceMessageSender;
import org.whispersystems.signalservice.api.crypto.UntrustedIdentityException;
import org.whispersystems.signalservice.api.messages.SignalServiceDataMessage;
import org.whispersystems.signalservice.api.push.SignalServiceAddress;
import org.whispersystems.signalservice.api.push.exceptions.UnregisteredUserException;

import java.io.IOException;

import javax.inject.Inject;

public class PushTextSendJob extends PushSendJob implements InjectableType {

  private static final long serialVersionUID = 1L;

  private static final String TAG = PushTextSendJob.class.getSimpleName();

  @Inject transient SignalServiceMessageSender messageSender;

  private final long messageId;

  public PushTextSendJob(Context context, long messageId, Address destination) {
    super(context, constructParameters(context, destination));
    this.messageId = messageId;
  }

  @Override
  public void onAdded() {}

  @Override
  public void onPushSend() throws NoSuchMessageException, RetryLaterException {
    ExpiringMessageManager expirationManager = ApplicationContext.getInstance(context).getExpiringMessageManager();
    SmsDatabase database          = DatabaseFactory.getSmsDatabase(context);
    SmsMessageRecord record            = database.getMessage(messageId);

    try {
      Log.w(TAG, "Sending message: " + messageId);

      deliver(record);
      database.markAsSent(messageId, true);

      if (record.getExpiresIn() > 0) {
        database.markExpireStarted(messageId);
        expirationManager.scheduleDeletion(record.getId(), record.isMms(), record.getExpiresIn());
      }

    } catch (InsecureFallbackApprovalException e) {
      Log.w(TAG, e);
      database.markAsPendingInsecureSmsFallback(record.getId());
      MessageNotifier.notifyMessageDeliveryFailed(context, record.getRecipient(), record.getThreadId());
      ApplicationContext.getInstance(context).getJobManager().add(new DirectoryRefreshJob(context, false));
    } catch (UntrustedIdentityException e) {
      Log.w(TAG, e);
      database.addMismatchedIdentity(record.getId(), Address.fromSerialized(e.getE164Number()), e.getIdentityKey());
      database.markAsSentFailed(record.getId());
      database.markAsPush(record.getId());
    }
  }

  @Override
  public boolean onShouldRetryThrowable(Exception exception) {
    if (exception instanceof RetryLaterException) return true;

    return false;
  }

  @Override
  public void onCanceled() {
    DatabaseFactory.getSmsDatabase(context).markAsSentFailed(messageId);

    long      threadId  = DatabaseFactory.getSmsDatabase(context).getThreadIdForMessage(messageId);
    Recipient recipient = DatabaseFactory.getThreadDatabase(context).getRecipientForThreadId(threadId);

    if (threadId != -1 && recipient != null) {
      MessageNotifier.notifyMessageDeliveryFailed(context, recipient, threadId);
    }
  }

  private void deliver(SmsMessageRecord message)
      throws UntrustedIdentityException, InsecureFallbackApprovalException, RetryLaterException
  {
    try {
      SignalServiceAddress       address           = getPushAddress(message.getIndividualRecipient().getAddress());
      Optional<byte[]>           profileKey        = getProfileKey(message.getIndividualRecipient());
      SignalServiceDataMessage   textSecureMessage = SignalServiceDataMessage.newBuilder()
                                                                             .withTimestamp(message.getDateSent())
                                                                             .withBody(message.getBody())
                                                                             .withExpiration((int)(message.getExpiresIn() / 1000))
                                                                             .withProfileKey(profileKey.orNull())
                                                                             .asEndSessionMessage(message.isEndSession())
                                                                             .build();


      messageSender.sendMessage(address, textSecureMessage);
    } catch (UnregisteredUserException e) {
      Log.w(TAG, e);
      throw new InsecureFallbackApprovalException(e);
    } catch (IOException e) {
      Log.w(TAG, e);
      throw new RetryLaterException(e);
    }
  }
}
