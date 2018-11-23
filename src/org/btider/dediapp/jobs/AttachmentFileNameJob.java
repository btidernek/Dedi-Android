package org.btider.dediapp.jobs;


import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.btider.dediapp.attachments.Attachment;
import org.btider.dediapp.attachments.AttachmentId;
import org.btider.dediapp.attachments.DatabaseAttachment;
import org.btider.dediapp.crypto.AsymmetricMasterCipher;
import org.btider.dediapp.crypto.AsymmetricMasterSecret;
import org.btider.dediapp.crypto.MasterSecret;
import org.btider.dediapp.crypto.MasterSecretUtil;
import org.btider.dediapp.database.DatabaseFactory;
import org.btider.dediapp.jobs.requirements.MasterSecretRequirement;
import org.btider.dediapp.attachments.Attachment;
import org.btider.dediapp.attachments.AttachmentId;
import org.btider.dediapp.attachments.DatabaseAttachment;
import org.btider.dediapp.crypto.AsymmetricMasterCipher;
import org.btider.dediapp.crypto.AsymmetricMasterSecret;
import org.btider.dediapp.crypto.MasterSecret;
import org.btider.dediapp.crypto.MasterSecretUtil;
import org.btider.dediapp.database.DatabaseFactory;
import org.btider.dediapp.jobs.requirements.MasterSecretRequirement;
import org.btider.dediapp.mms.IncomingMediaMessage;
import org.whispersystems.jobqueue.JobParameters;
import org.whispersystems.libsignal.InvalidMessageException;

import java.io.IOException;
import java.util.Arrays;

public class AttachmentFileNameJob extends MasterSecretJob {

  private static final long serialVersionUID = 1L;

  private final long   attachmentRowId;
  private final long   attachmentUniqueId;
  private final String encryptedFileName;

  public AttachmentFileNameJob(@NonNull Context context, @NonNull AsymmetricMasterSecret asymmetricMasterSecret,
                               @NonNull DatabaseAttachment attachment, @NonNull IncomingMediaMessage message)
  {
    super(context, new JobParameters.Builder().withPersistence()
                                              .withRequirement(new MasterSecretRequirement(context))
                                              .create());

    this.attachmentRowId    = attachment.getAttachmentId().getRowId();
    this.attachmentUniqueId = attachment.getAttachmentId().getUniqueId();
    this.encryptedFileName  = getEncryptedFileName(asymmetricMasterSecret, attachment, message);
  }

  @Override
  public void onRun(MasterSecret masterSecret) throws IOException, InvalidMessageException {
    if (encryptedFileName == null) return;

    AttachmentId attachmentId      = new AttachmentId(attachmentRowId, attachmentUniqueId);
    String       plaintextFileName = new AsymmetricMasterCipher(MasterSecretUtil.getAsymmetricMasterSecret(context, masterSecret)).decryptBody(encryptedFileName);

    DatabaseFactory.getAttachmentDatabase(context).updateAttachmentFileName(attachmentId, plaintextFileName);
  }

  @Override
  public boolean onShouldRetryThrowable(Exception exception) {
    return false;
  }

  @Override
  public void onAdded() {

  }

  @Override
  public void onCanceled() {

  }

  private @Nullable String getEncryptedFileName(@NonNull AsymmetricMasterSecret asymmetricMasterSecret,
                                                @NonNull DatabaseAttachment attachment,
                                                @NonNull IncomingMediaMessage mediaMessage)
  {
    for (Attachment messageAttachment : mediaMessage.getAttachments()) {
      if (mediaMessage.getAttachments().size() == 1 ||
          (messageAttachment.getDigest() != null && Arrays.equals(messageAttachment.getDigest(), attachment.getDigest())))
      {
        if (messageAttachment.getFileName() == null) return null;
        else                                         return new AsymmetricMasterCipher(asymmetricMasterSecret).encryptBody(messageAttachment.getFileName());
      }
    }

    return null;
  }


}
