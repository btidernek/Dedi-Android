package org.btider.dediapp.jobs;

import android.content.Context;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import org.btider.dediapp.R;
import org.greenrobot.eventbus.EventBus;

import org.btider.dediapp.attachments.Attachment;
import org.btider.dediapp.attachments.AttachmentId;
import org.btider.dediapp.crypto.MasterSecret;
import org.btider.dediapp.database.AttachmentDatabase;
import org.btider.dediapp.database.DatabaseFactory;
import org.btider.dediapp.dependencies.InjectableType;
import org.btider.dediapp.events.PartProgressEvent;
import org.btider.dediapp.jobs.requirements.MasterSecretRequirement;
import org.btider.dediapp.notifications.MessageNotifier;
import org.btider.dediapp.util.AttachmentUtil;
import org.btider.dediapp.util.Base64;
import org.btider.dediapp.util.Hex;
import org.btider.dediapp.util.Util;
import org.btider.dediapp.attachments.Attachment;
import org.btider.dediapp.attachments.AttachmentId;
import org.btider.dediapp.crypto.MasterSecret;
import org.btider.dediapp.database.AttachmentDatabase;
import org.btider.dediapp.database.DatabaseFactory;
import org.btider.dediapp.dependencies.InjectableType;
import org.btider.dediapp.events.PartProgressEvent;
import org.btider.dediapp.jobs.requirements.MasterSecretRequirement;
import org.btider.dediapp.mms.MmsException;
import org.btider.dediapp.notifications.MessageNotifier;
import org.btider.dediapp.util.AttachmentUtil;
import org.btider.dediapp.util.Base64;
import org.btider.dediapp.util.Hex;
import org.btider.dediapp.util.Util;
import org.whispersystems.jobqueue.JobParameters;
import org.whispersystems.jobqueue.requirements.NetworkRequirement;
import org.whispersystems.libsignal.InvalidMessageException;
import org.whispersystems.libsignal.util.guava.Optional;
import org.whispersystems.signalservice.api.SignalServiceMessageReceiver;
import org.whispersystems.signalservice.api.messages.SignalServiceAttachmentPointer;
import org.whispersystems.signalservice.api.push.exceptions.NonSuccessfulResponseCodeException;
import org.whispersystems.signalservice.api.push.exceptions.PushNetworkException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.inject.Inject;

import static org.btider.dediapp.util.TextSecurePreferences.PREF_MEDIA_SIZE;
import static org.btider.dediapp.util.TextSecurePreferences.getBooleanPreference;

public class AttachmentDownloadJob extends MasterSecretJob implements InjectableType {
  private static final long   serialVersionUID    = 2L;
  private static final int    MAX_ATTACHMENT_SIZE = 150 * 1024  * 1024;
  private static final String TAG                  = AttachmentDownloadJob.class.getSimpleName();
  private static final long   saving_max_download_size = 20971520L;

  @Inject transient SignalServiceMessageReceiver messageReceiver;

  private final long    messageId;
  private final long    partRowId;
  private final long    partUniqueId;
  private final boolean manual;
  private final long    size;

  public AttachmentDownloadJob(Context context, long messageId, AttachmentId attachmentId, boolean manual,long size) {
    super(context, JobParameters.newBuilder()
                                .withGroupId(AttachmentDownloadJob.class.getCanonicalName())
                                .withRequirement(new MasterSecretRequirement(context))
                                .withRequirement(new NetworkRequirement(context))
                                .withPersistence()
                                .create());

    this.messageId    = messageId;
    this.partRowId    = attachmentId.getRowId();
    this.partUniqueId = attachmentId.getUniqueId();
    this.manual       = manual;
    this.size         = size;
  }

  @Override
  public void onAdded() {
  }

  @Override
  public void onRun(MasterSecret masterSecret) throws IOException {
    final AttachmentDatabase database     = DatabaseFactory.getAttachmentDatabase(context);
    final AttachmentId       attachmentId = new AttachmentId(partRowId, partUniqueId);
    final Attachment attachment   = database.getAttachment(attachmentId);

    if (attachment == null) {
      Log.w(TAG, "attachment no longer exists.");
      return;
    }

    if (!attachment.isInProgress()) {
      Log.w(TAG, "Attachment was already downloaded.");
      return;
    }

    if (!manual && !AttachmentUtil.isAutoDownloadPermitted(context, attachment)) {
      Log.w(TAG, "Attachment can't be auto downloaded...");
      return;
    }

    if(getBooleanPreference(context, PREF_MEDIA_SIZE, false)){
      if(size > saving_max_download_size){//20 MB'den büyük ise
        return;
      }
    }

    Log.w(TAG, "Downloading push part " + attachmentId);
    database.setTransferState(messageId, attachmentId, AttachmentDatabase.TRANSFER_PROGRESS_STARTED);

    retrieveAttachment(messageId, attachmentId, attachment);
    MessageNotifier.updateNotification(context);
  }

  @Override
  public void onCanceled() {
    final AttachmentId attachmentId = new AttachmentId(partRowId, partUniqueId);
    markFailed(messageId, attachmentId);
  }

  @Override
  public boolean onShouldRetryThrowable(Exception exception) {
    return (exception instanceof PushNetworkException);
  }

  private void retrieveAttachment(long messageId,
                                  final AttachmentId attachmentId,
                                  final Attachment attachment)
      throws IOException
  {

    AttachmentDatabase database       = DatabaseFactory.getAttachmentDatabase(context);
    File               attachmentFile = null;

    try {
      attachmentFile = createTempFile();

      SignalServiceAttachmentPointer pointer = createAttachmentPointer(attachment);
      InputStream                    stream  = messageReceiver.retrieveAttachment(pointer, attachmentFile, MAX_ATTACHMENT_SIZE, (total, progress) -> EventBus.getDefault().postSticky(new PartProgressEvent(attachment, total, progress)));

      database.insertAttachmentsForPlaceholder(messageId, attachmentId, stream);
    } catch (InvalidPartException | NonSuccessfulResponseCodeException | InvalidMessageException | MmsException e) {
      Log.w(TAG, e);
      markFailed(messageId, attachmentId);
    } finally {
      if (attachmentFile != null) {
        //noinspection ResultOfMethodCallIgnored
        attachmentFile.delete();
      }
    }
  }

  @VisibleForTesting
  SignalServiceAttachmentPointer createAttachmentPointer(Attachment attachment)
      throws InvalidPartException
  {
    if (TextUtils.isEmpty(attachment.getLocation())) {
      throw new InvalidPartException("empty content id");
    }

    if (TextUtils.isEmpty(attachment.getKey())) {
      throw new InvalidPartException("empty encrypted key");
    }

    try {
      long   id    = Long.parseLong(attachment.getLocation());
      byte[] key   = Base64.decode(attachment.getKey());
      String relay = null;

      if (TextUtils.isEmpty(attachment.getRelay())) {
        relay = attachment.getRelay();
      }

      if (attachment.getDigest() != null) {
        Log.w(TAG, "Downloading attachment with digest: " + Hex.toString(attachment.getDigest()));
      } else {
        Log.w(TAG, "Downloading attachment with no digest...");
      }

      return new SignalServiceAttachmentPointer(id, null, key, relay,
                                                Optional.of(Util.toIntExact(attachment.getSize())),
                                                Optional.absent(),
                                                0, 0,
                                                Optional.fromNullable(attachment.getDigest()),
                                                Optional.fromNullable(attachment.getFileName()),
                                                attachment.isVoiceNote());
    } catch (IOException | ArithmeticException e) {
      Log.w(TAG, e);
      throw new InvalidPartException(e);
    }
  }

  private File createTempFile() throws InvalidPartException {
    try {
      File file = File.createTempFile("push-attachment", "tmp", context.getCacheDir());
      file.deleteOnExit();

      return file;
    } catch (IOException e) {
      throw new InvalidPartException(e);
    }
  }

  private void markFailed(long messageId, AttachmentId attachmentId) {
    try {
      AttachmentDatabase database = DatabaseFactory.getAttachmentDatabase(context);
      database.setTransferProgressFailed(attachmentId, messageId);
    } catch (MmsException e) {
      Log.w(TAG, e);
    }
  }

  @VisibleForTesting static class InvalidPartException extends Exception {
    InvalidPartException(String s) {super(s);}
    InvalidPartException(Exception e) {super(e);}
  }

}
