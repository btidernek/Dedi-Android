package org.btider.dediapp.mms;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.btider.dediapp.attachments.Attachment;
import org.btider.dediapp.contactshare.Contact;
import org.btider.dediapp.database.ThreadDatabase;
import org.btider.dediapp.recipients.Recipient;
import org.btider.dediapp.util.Base64;
import org.btider.dediapp.attachments.Attachment;
import org.btider.dediapp.contactshare.Contact;
import org.btider.dediapp.database.ThreadDatabase;
import org.btider.dediapp.recipients.Recipient;
import org.btider.dediapp.util.Base64;
import org.whispersystems.signalservice.internal.push.SignalServiceProtos.GroupContext;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class OutgoingGroupMediaMessage extends OutgoingSecureMediaMessage {

  private final GroupContext group;

  public OutgoingGroupMediaMessage(@NonNull Recipient recipient,
                                   @NonNull String encodedGroupContext,
                                   @NonNull List<Attachment> avatar,
                                   long sentTimeMillis,
                                   long expiresIn,
                                   @Nullable QuoteModel quote,
                                   @NonNull List<Contact> contacts)
      throws IOException
  {
    super(recipient, encodedGroupContext, avatar, sentTimeMillis,
          ThreadDatabase.DistributionTypes.CONVERSATION, expiresIn, quote, contacts);

    this.group = GroupContext.parseFrom(Base64.decode(encodedGroupContext));
  }

  public OutgoingGroupMediaMessage(@NonNull Recipient recipient,
                                   @NonNull GroupContext group,
                                   @Nullable final Attachment avatar,
                                   long sentTimeMillis,
                                   long expireIn,
                                   @Nullable QuoteModel quote,
                                   @NonNull List<Contact> contacts)
  {
    super(recipient, Base64.encodeBytes(group.toByteArray()),
          new LinkedList<Attachment>() {{if (avatar != null) add(avatar);}},
          System.currentTimeMillis(),
          ThreadDatabase.DistributionTypes.CONVERSATION, expireIn, quote, contacts);

    this.group = group;
  }

  @Override
  public boolean isGroup() {
    return true;
  }

  public boolean isGroupUpdate() {
    return group.getType().getNumber() == GroupContext.Type.UPDATE_VALUE;
  }

  public boolean isGroupQuit() {
    return group.getType().getNumber() == GroupContext.Type.QUIT_VALUE;
  }

  public GroupContext getGroupContext() {
    return group;
  }
}
