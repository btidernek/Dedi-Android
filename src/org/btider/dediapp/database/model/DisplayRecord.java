/*
 * Copyright (C) 2012 Moxie Marlinspike
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.btider.dediapp.database.model;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.SpannableString;

import org.btider.dediapp.database.DatabaseFactory;
import org.btider.dediapp.database.MmsSmsDatabase;
import org.btider.dediapp.recipients.Recipient;
import org.btider.dediapp.database.MmsSmsColumns;
import org.btider.dediapp.database.SmsDatabase;
import org.btider.dediapp.recipients.Recipient;
import org.btider.dediapp.util.Base64;
import org.whispersystems.signalservice.internal.push.SignalServiceProtos;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The base class for all message record models.  Encapsulates basic data
 * shared between ThreadRecord and MessageRecord.
 *
 * @author Moxie Marlinspike
 *
 */

public abstract class DisplayRecord {

  protected final Context context;
  protected final long type;

  private final Recipient recipient;
  private final long       dateSent;
  private final long       dateReceived;
  private final long       threadId;
  private final String     body;
  private final int        deliveryStatus;
  private final int        deliveryReceiptCount;
  private final int        readReceiptCount;

  DisplayRecord(Context context, String body, Recipient recipient, long dateSent,
                long dateReceived, long threadId, int deliveryStatus, int deliveryReceiptCount,
                long type, int readReceiptCount)
  {
    this.context              = context.getApplicationContext();
    this.threadId             = threadId;
    this.recipient            = recipient;
    this.dateSent             = dateSent;
    this.dateReceived         = dateReceived;
    this.type                 = type;
    this.body                 = body;
    this.deliveryReceiptCount = deliveryReceiptCount;
    this.readReceiptCount     = readReceiptCount;
    this.deliveryStatus       = deliveryStatus;
  }

  public @NonNull String getBody() {
    return body == null ? "" : body;
  }

  public boolean isFailed() {
    return
        MmsSmsColumns.Types.isFailedMessageType(type)            ||
        MmsSmsColumns.Types.isPendingSecureSmsFallbackType(type) ||
        deliveryStatus >= SmsDatabase.Status.STATUS_FAILED;
  }

  public boolean isPending() {
    return MmsSmsColumns.Types.isPendingMessageType(type) &&
           !MmsSmsColumns.Types.isIdentityVerified(type)  &&
           !MmsSmsColumns.Types.isIdentityDefault(type);
  }

  public boolean isOutgoing() {
    return MmsSmsColumns.Types.isOutgoingMessageType(type);
  }

  public abstract SpannableString getDisplayBody();

  public Recipient getRecipient() {
    return recipient;
  }

  public long getDateSent() {
    return dateSent;
  }

  public long getDateReceived() {
    return dateReceived;
  }

  public long getThreadId() {
    return threadId;
  }

  public boolean isKeyExchange() {
    return SmsDatabase.Types.isKeyExchangeType(type);
  }

  public boolean isEndSession() {
    return SmsDatabase.Types.isEndSessionType(type);
  }

  public boolean isGroupUpdate() {
    return SmsDatabase.Types.isGroupUpdate(type);
  }



  public boolean isGroupUserAdd() {
    return SmsDatabase.Types.isGroupAddUser(type);
  }

  public boolean isGroupUserRemove() {
    return SmsDatabase.Types.isGroupRemoveUser(type);
  }

  public boolean isGroupCreate(long id) {

    if(isGroupAction() && isGroupUpdate()){
      MmsSmsDatabase mmsSmsDatabase = DatabaseFactory.getMmsSmsDatabase(context);
      long count                    = mmsSmsDatabase.getConversationCount(threadId);
      if (count == 0) {
        return true;
      }
      MmsSmsDatabase.Reader reader = null;

      try {
        reader = mmsSmsDatabase.readerFor(mmsSmsDatabase.getConversationSnippetASC(threadId));
        MessageRecord record;

        if (reader != null && (record = reader.getNext()) != null) {
          System.out.println(record.getId());
          if(id==record.getId()){
            return true;
          }
        }
      } finally {
        if (reader != null)
          reader.close();
      }
    }
    return false;
  }

  public boolean isGroupQuit() {
    return SmsDatabase.Types.isGroupQuit(type);
  }

  public boolean isGroupAction() {
    return isGroupUpdate() || isGroupQuit();
  }

  public boolean isExpirationTimerUpdate() {
    return SmsDatabase.Types.isExpirationTimerUpdate(type);
  }

  public boolean isCallLog() {
    return SmsDatabase.Types.isCallLog(type);
  }

  public boolean isJoined() {
    return SmsDatabase.Types.isJoinedType(type);
  }

  public boolean isIncomingCall() {
    return SmsDatabase.Types.isIncomingCall(type);
  }

  public boolean isOutgoingCall() {
    return SmsDatabase.Types.isOutgoingCall(type);
  }

  public boolean isMissedCall() {
    return SmsDatabase.Types.isMissedCall(type);
  }

  public boolean isVerificationStatusChange() {
    return SmsDatabase.Types.isIdentityDefault(type) || SmsDatabase.Types.isIdentityVerified(type);
  }

  public int getDeliveryStatus() {
    return deliveryStatus;
  }

  public int getDeliveryReceiptCount() {
    return deliveryReceiptCount;
  }

  public int getReadReceiptCount() {
    return readReceiptCount;
  }

  public boolean isDelivered() {
    if(recipient.getParticipants().size() == 0){
      return (deliveryStatus >= SmsDatabase.Status.STATUS_COMPLETE &&
              deliveryStatus < SmsDatabase.Status.STATUS_PENDING) || deliveryReceiptCount > 0;
    }else
      return (deliveryStatus >= SmsDatabase.Status.STATUS_COMPLETE &&
              deliveryStatus < SmsDatabase.Status.STATUS_PENDING) || deliveryReceiptCount == (recipient.getParticipants().size()-1);
  }

  public boolean isRemoteRead() {
    if(recipient.getParticipants().size() == 0){
      return readReceiptCount > 0;
    }

    if((recipient.getParticipants().size()-1) == deliveryReceiptCount)
      return readReceiptCount>0 && readReceiptCount == deliveryReceiptCount;
    return false;
  }

  public boolean isPendingInsecureSmsFallback() {
    return SmsDatabase.Types.isPendingInsecureSmsFallbackType(type);
  }


}
