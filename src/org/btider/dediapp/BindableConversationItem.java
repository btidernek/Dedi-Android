package org.btider.dediapp;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;

import org.btider.dediapp.contactshare.Contact;
import org.btider.dediapp.database.model.MessageRecord;
import org.btider.dediapp.database.model.MmsMessageRecord;
import org.btider.dediapp.recipients.Recipient;
import org.btider.dediapp.contactshare.Contact;
import org.btider.dediapp.database.model.MessageRecord;
import org.btider.dediapp.database.model.MmsMessageRecord;
import org.btider.dediapp.mms.GlideRequests;
import org.btider.dediapp.recipients.Recipient;

import java.util.List;
import java.util.Locale;
import java.util.Set;

public interface BindableConversationItem extends Unbindable {
  void bind(@NonNull MessageRecord messageRecord,
            @NonNull GlideRequests      glideRequests,
            @NonNull Locale             locale,
            @NonNull Set<MessageRecord> batchSelected,
            @NonNull Recipient recipients,
                     boolean            pulseHighlight);

  MessageRecord getMessageRecord();

  void setEventListener(@Nullable EventListener listener);

  interface EventListener {
    void onQuoteClicked(MmsMessageRecord messageRecord);
    void onSharedContactDetailsClicked(@NonNull Contact contact, @NonNull View avatarTransitionView);
    void onAddToContactsClicked(@NonNull Contact contact);
    void onMessageSharedContactClicked(@NonNull List<Recipient> choices);
    void onInviteSharedContactClicked(@NonNull List<Recipient> choices);
  }
}
