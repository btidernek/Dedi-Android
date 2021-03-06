package org.btider.dediapp.search.model;

import android.support.annotation.NonNull;

import org.btider.dediapp.recipients.Recipient;
import org.btider.dediapp.recipients.Recipient;

/**
 * Represents a search result for a message.
 */
public class MessageResult {

  public final Recipient recipient;
  public final String    bodySnippet;
  public final long      threadId;
  public final long      receivedTimestampMs;

  public MessageResult(@NonNull Recipient recipient,
                       @NonNull String bodySnippet,
                       long threadId,
                       long receivedTimestampMs)
  {
    this.recipient           = recipient;
    this.bodySnippet         = bodySnippet;
    this.threadId            = threadId;
    this.receivedTimestampMs = receivedTimestampMs;
  }
}
