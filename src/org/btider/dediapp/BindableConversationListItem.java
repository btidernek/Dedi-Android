package org.btider.dediapp;

import android.support.annotation.NonNull;

import org.btider.dediapp.database.model.ThreadRecord;
import org.btider.dediapp.crypto.MasterSecret;
import org.btider.dediapp.database.model.ThreadRecord;
import org.btider.dediapp.mms.GlideRequests;

import java.util.Locale;
import java.util.Set;

public interface BindableConversationListItem extends Unbindable {

  public void bind(@NonNull ThreadRecord thread,
                   @NonNull GlideRequests glideRequests, @NonNull Locale locale,
                   @NonNull Set<Long> selectedThreads, boolean batchMode);
}
