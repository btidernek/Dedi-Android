package org.btider.dediapp.mms;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.btider.dediapp.attachments.Attachment;
import org.btider.dediapp.contactshare.Contact;
import org.btider.dediapp.recipients.Recipient;
import org.btider.dediapp.attachments.Attachment;
import org.btider.dediapp.contactshare.Contact;
import org.btider.dediapp.recipients.Recipient;

import java.util.List;

public class OutgoingSecureMediaMessage extends OutgoingMediaMessage {

  public OutgoingSecureMediaMessage(Recipient recipient, String body,
                                    List<Attachment> attachments,
                                    long sentTimeMillis,
                                    int distributionType,
                                    long expiresIn,
                                    @Nullable QuoteModel quote,
                                    @NonNull List<Contact> contacts)
  {
    super(recipient, body, attachments, sentTimeMillis, -1, expiresIn, distributionType, quote, contacts);
  }

  public OutgoingSecureMediaMessage(OutgoingMediaMessage base) {
    super(base);
  }

  @Override
  public boolean isSecure() {
    return true;
  }
}
