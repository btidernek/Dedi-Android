package org.btider.dediapp.mms;

import org.btider.dediapp.attachments.Attachment;
import org.btider.dediapp.database.ThreadDatabase;
import org.btider.dediapp.recipients.Recipient;
import org.btider.dediapp.attachments.Attachment;
import org.btider.dediapp.database.ThreadDatabase;
import org.btider.dediapp.recipients.Recipient;

import java.util.Collections;
import java.util.LinkedList;

public class OutgoingExpirationUpdateMessage extends OutgoingSecureMediaMessage {

  public OutgoingExpirationUpdateMessage(Recipient recipient, long sentTimeMillis, long expiresIn) {
    super(recipient, "", new LinkedList<Attachment>(), sentTimeMillis,
          ThreadDatabase.DistributionTypes.CONVERSATION, expiresIn, null, Collections.emptyList());
  }

  @Override
  public boolean isExpirationUpdate() {
    return true;
  }

}
