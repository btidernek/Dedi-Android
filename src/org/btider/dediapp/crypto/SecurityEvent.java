package org.btider.dediapp.crypto;

import android.content.Context;
import android.content.Intent;

import org.btider.dediapp.service.KeyCachingService;
import org.btider.dediapp.recipients.Recipient;
import org.btider.dediapp.service.KeyCachingService;

/**
 * This class processes key exchange interactions.
 *
 * @author Moxie Marlinspike
 */

public class SecurityEvent {

  public static final String SECURITY_UPDATE_EVENT = "org.btider.dediapp.KEY_EXCHANGE_UPDATE";

  public static void broadcastSecurityUpdateEvent(Context context) {
    Intent intent = new Intent(SECURITY_UPDATE_EVENT);
    intent.setPackage(context.getPackageName());
    context.sendBroadcast(intent, KeyCachingService.KEY_PERMISSION);
  }

}
