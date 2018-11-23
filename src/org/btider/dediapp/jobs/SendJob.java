package org.btider.dediapp.jobs;

import android.content.Context;
import android.support.annotation.NonNull;

import org.btider.dediapp.attachments.Attachment;
import org.btider.dediapp.crypto.MasterSecret;
import org.btider.dediapp.database.AttachmentDatabase;
import org.btider.dediapp.database.DatabaseFactory;
import org.btider.dediapp.transport.UndeliverableMessageException;
import org.btider.dediapp.util.MediaUtil;
import org.btider.dediapp.util.Util;
import org.btider.dediapp.BuildConfig;
import org.btider.dediapp.TextSecureExpiredException;
import org.btider.dediapp.attachments.Attachment;
import org.btider.dediapp.crypto.MasterSecret;
import org.btider.dediapp.database.AttachmentDatabase;
import org.btider.dediapp.database.DatabaseFactory;
import org.btider.dediapp.mms.MediaConstraints;
import org.btider.dediapp.mms.MediaStream;
import org.btider.dediapp.mms.MmsException;
import org.btider.dediapp.transport.UndeliverableMessageException;
import org.btider.dediapp.util.MediaUtil;
import org.btider.dediapp.util.Util;
import org.whispersystems.jobqueue.JobParameters;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public abstract class SendJob extends MasterSecretJob {

  @SuppressWarnings("unused")
  private final static String TAG = SendJob.class.getSimpleName();

  public SendJob(Context context, JobParameters parameters) {
    super(context, parameters);
  }

  @Override
  public final void onRun(MasterSecret masterSecret) throws Exception {
    if (Util.getDaysTillBuildExpiry() <= 0) {
      throw new TextSecureExpiredException(String.format("TextSecure expired (build %d, now %d)",
                                                         BuildConfig.BUILD_TIMESTAMP,
                                                         System.currentTimeMillis()));
    }

    onSend(masterSecret);
  }

  protected abstract void onSend(MasterSecret masterSecret) throws Exception;

  protected void markAttachmentsUploaded(long messageId, @NonNull List<Attachment> attachments) {
    AttachmentDatabase database = DatabaseFactory.getAttachmentDatabase(context);

    for (Attachment attachment : attachments) {
      database.markAttachmentUploaded(messageId, attachment);
    }
  }

  protected List<Attachment> scaleAndStripExifFromAttachments(@NonNull MediaConstraints constraints,
                                                              @NonNull List<Attachment> attachments)
      throws UndeliverableMessageException
  {
    AttachmentDatabase attachmentDatabase = DatabaseFactory.getAttachmentDatabase(context);
    List<Attachment>   results            = new LinkedList<>();

    for (Attachment attachment : attachments) {
      try {
        if (constraints.isSatisfied(context, attachment)) {
          if (MediaUtil.isJpeg(attachment)) {
            MediaStream stripped = constraints.getResizedMedia(context, attachment);
            results.add(attachmentDatabase.updateAttachmentData(attachment, stripped));
          } else {
            results.add(attachment);
          }
        } else if (constraints.canResize(attachment)) {
          MediaStream resized = constraints.getResizedMedia(context, attachment);
          results.add(attachmentDatabase.updateAttachmentData(attachment, resized));
        } else {
          throw new UndeliverableMessageException("Size constraints could not be met!");
        }
      } catch (IOException | MmsException e) {
        throw new UndeliverableMessageException(e);
      }
    }

    return results;
  }
}
