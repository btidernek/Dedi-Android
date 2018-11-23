package org.btider.dediapp.database.model;


import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.btider.dediapp.contactshare.Contact;
import org.btider.dediapp.database.documents.IdentityKeyMismatch;
import org.btider.dediapp.database.documents.NetworkFailure;
import org.btider.dediapp.recipients.Recipient;
import org.btider.dediapp.contactshare.Contact;
import org.btider.dediapp.database.documents.IdentityKeyMismatch;
import org.btider.dediapp.database.documents.NetworkFailure;
import org.btider.dediapp.mms.Slide;
import org.btider.dediapp.mms.SlideDeck;
import org.btider.dediapp.recipients.Recipient;

import java.util.LinkedList;
import java.util.List;

public abstract class MmsMessageRecord extends MessageRecord {

  private final @NonNull  SlideDeck     slideDeck;
  private final @Nullable Quote         quote;
  private final @NonNull  List<Contact> contacts = new LinkedList<>();

  MmsMessageRecord(Context context, long id, String body, Recipient conversationRecipient,
                   Recipient individualRecipient, int recipientDeviceId, long dateSent,
                   long dateReceived, long threadId, int deliveryStatus, int deliveryReceiptCount,
                   long type, List<IdentityKeyMismatch> mismatches,
                   List<NetworkFailure> networkFailures, int subscriptionId, long expiresIn,
                   long expireStarted, @NonNull SlideDeck slideDeck, int readReceiptCount,
                   @Nullable Quote quote, @NonNull List<Contact> contacts)
  {
    super(context, id, body, conversationRecipient, individualRecipient, recipientDeviceId, dateSent, dateReceived, threadId, deliveryStatus, deliveryReceiptCount, type, mismatches, networkFailures, subscriptionId, expiresIn, expireStarted, readReceiptCount);

    this.slideDeck = slideDeck;
    this.quote     = quote;

    this.contacts.addAll(contacts);
  }

  @Override
  public boolean isMms() {
    return true;
  }

  @NonNull
  public SlideDeck getSlideDeck() {
    return slideDeck;
  }

  @Override
  public boolean isMediaPending() {
    for (Slide slide : getSlideDeck().getSlides()) {
      if (slide.isInProgress() || slide.isPendingDownload()) {
        return true;
      }
    }

    return false;
  }

  public boolean containsMediaSlide() {
    return slideDeck.containsMediaSlide();
  }

  public @Nullable Quote getQuote() {
    return quote;
  }

  public @NonNull List<Contact> getSharedContacts() {
    return contacts;
  }
}
