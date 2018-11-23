package org.btider.dediapp.dependencies;

import android.content.Context;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;

import org.btider.dediapp.crypto.storage.SignalProtocolStoreImpl;
import org.btider.dediapp.events.ReminderUpdateEvent;
import org.btider.dediapp.jobs.AttachmentDownloadJob;
import org.btider.dediapp.jobs.AvatarDownloadJob;
import org.btider.dediapp.jobs.CleanPreKeysJob;
import org.btider.dediapp.jobs.CreateSignedPreKeyJob;
import org.btider.dediapp.jobs.GcmRefreshJob;
import org.btider.dediapp.jobs.MultiDeviceBlockedUpdateJob;
import org.btider.dediapp.jobs.MultiDeviceContactUpdateJob;
import org.btider.dediapp.jobs.MultiDeviceGroupUpdateJob;
import org.btider.dediapp.jobs.MultiDeviceProfileKeyUpdateJob;
import org.btider.dediapp.jobs.MultiDeviceReadReceiptUpdateJob;
import org.btider.dediapp.jobs.MultiDeviceReadUpdateJob;
import org.btider.dediapp.jobs.MultiDeviceVerifiedUpdateJob;
import org.btider.dediapp.jobs.PushGroupSendJob;
import org.btider.dediapp.jobs.PushGroupUpdateJob;
import org.btider.dediapp.jobs.PushMediaSendJob;
import org.btider.dediapp.jobs.PushNotificationReceiveJob;
import org.btider.dediapp.jobs.PushTextSendJob;
import org.btider.dediapp.jobs.RefreshAttributesJob;
import org.btider.dediapp.jobs.RefreshPreKeysJob;
import org.btider.dediapp.jobs.RequestGroupInfoJob;
import org.btider.dediapp.jobs.RetrieveProfileAvatarJob;
import org.btider.dediapp.jobs.RetrieveProfileJob;
import org.btider.dediapp.jobs.RotateSignedPreKeyJob;
import org.btider.dediapp.jobs.SendReadReceiptJob;
import org.btider.dediapp.preferences.AppProtectionPreferenceFragment;
import org.btider.dediapp.push.SecurityEventListener;
import org.btider.dediapp.push.SignalServiceNetworkAccess;
import org.btider.dediapp.service.MessageRetrievalService;
import org.btider.dediapp.service.WebRtcCallService;
import org.btider.dediapp.util.TextSecurePreferences;
import org.btider.dediapp.BuildConfig;
import org.btider.dediapp.CreateProfileActivity;
import org.btider.dediapp.DeviceListFragment;
import org.btider.dediapp.crypto.storage.SignalProtocolStoreImpl;
import org.btider.dediapp.events.ReminderUpdateEvent;
import org.btider.dediapp.jobs.AttachmentDownloadJob;
import org.btider.dediapp.jobs.AvatarDownloadJob;
import org.btider.dediapp.jobs.CleanPreKeysJob;
import org.btider.dediapp.jobs.CreateSignedPreKeyJob;
import org.btider.dediapp.jobs.GcmRefreshJob;
import org.btider.dediapp.jobs.MultiDeviceBlockedUpdateJob;
import org.btider.dediapp.jobs.MultiDeviceContactUpdateJob;
import org.btider.dediapp.jobs.MultiDeviceGroupUpdateJob;
import org.btider.dediapp.jobs.MultiDeviceProfileKeyUpdateJob;
import org.btider.dediapp.jobs.MultiDeviceReadReceiptUpdateJob;
import org.btider.dediapp.jobs.MultiDeviceReadUpdateJob;
import org.btider.dediapp.jobs.MultiDeviceVerifiedUpdateJob;
import org.btider.dediapp.jobs.PushGroupSendJob;
import org.btider.dediapp.jobs.PushGroupUpdateJob;
import org.btider.dediapp.jobs.PushMediaSendJob;
import org.btider.dediapp.jobs.PushNotificationReceiveJob;
import org.btider.dediapp.jobs.PushTextSendJob;
import org.btider.dediapp.jobs.RefreshAttributesJob;
import org.btider.dediapp.jobs.RefreshPreKeysJob;
import org.btider.dediapp.jobs.RequestGroupInfoJob;
import org.btider.dediapp.jobs.RetrieveProfileAvatarJob;
import org.btider.dediapp.jobs.RetrieveProfileJob;
import org.btider.dediapp.jobs.RotateSignedPreKeyJob;
import org.btider.dediapp.jobs.SendReadReceiptJob;
import org.btider.dediapp.preferences.AppProtectionPreferenceFragment;
import org.btider.dediapp.preferences.SmsMmsPreferenceFragment;
import org.btider.dediapp.push.SecurityEventListener;
import org.btider.dediapp.push.SignalServiceNetworkAccess;
import org.btider.dediapp.service.MessageRetrievalService;
import org.btider.dediapp.service.WebRtcCallService;
import org.btider.dediapp.util.TextSecurePreferences;
import org.whispersystems.libsignal.util.guava.Optional;
import org.whispersystems.signalservice.api.SignalServiceAccountManager;
import org.whispersystems.signalservice.api.SignalServiceMessageReceiver;
import org.whispersystems.signalservice.api.SignalServiceMessageSender;
import org.whispersystems.signalservice.api.util.CredentialsProvider;
import org.whispersystems.signalservice.api.websocket.ConnectivityListener;

import dagger.Module;
import dagger.Provides;

@Module(complete = false, injects = {CleanPreKeysJob.class,
                                     CreateSignedPreKeyJob.class,
                                     PushGroupSendJob.class,
                                     PushTextSendJob.class,
                                     PushMediaSendJob.class,
                                     AttachmentDownloadJob.class,
                                     RefreshPreKeysJob.class,
                                     MessageRetrievalService.class,
                                     PushNotificationReceiveJob.class,
                                     MultiDeviceContactUpdateJob.class,
                                     MultiDeviceGroupUpdateJob.class,
                                     MultiDeviceReadUpdateJob.class,
                                     MultiDeviceBlockedUpdateJob.class,
                                     DeviceListFragment.class,
                                     RefreshAttributesJob.class,
                                     GcmRefreshJob.class,
                                     RequestGroupInfoJob.class,
                                     PushGroupUpdateJob.class,
                                     AvatarDownloadJob.class,
                                     RotateSignedPreKeyJob.class,
                                     WebRtcCallService.class,
                                     RetrieveProfileJob.class,
                                     MultiDeviceVerifiedUpdateJob.class,
                                     CreateProfileActivity.class,
                                     RetrieveProfileAvatarJob.class,
                                     MultiDeviceProfileKeyUpdateJob.class,
                                     SendReadReceiptJob.class,
                                     MultiDeviceReadReceiptUpdateJob.class,
                                     AppProtectionPreferenceFragment.class})
public class SignalCommunicationModule {

  private static final String TAG = SignalCommunicationModule.class.getSimpleName();

  private final Context                      context;
  private final SignalServiceNetworkAccess networkAccess;

  private SignalServiceAccountManager  accountManager;
  private SignalServiceMessageSender   messageSender;
  private SignalServiceMessageReceiver messageReceiver;

  public SignalCommunicationModule(Context context, SignalServiceNetworkAccess networkAccess) {
    this.context       = context;
    this.networkAccess = networkAccess;
  }

  @Provides
  synchronized SignalServiceAccountManager provideSignalAccountManager() {
    if (this.accountManager == null) {
      this.accountManager = new SignalServiceAccountManager(networkAccess.getConfiguration(context),
                                                            new DynamicCredentialsProvider(context),
                                                            BuildConfig.USER_AGENT);
    }

    return this.accountManager;
  }

  @Provides
  synchronized SignalServiceMessageSender provideSignalMessageSender() {
    if (this.messageSender == null) {
      this.messageSender = new SignalServiceMessageSender(networkAccess.getConfiguration(context),
                                                          new DynamicCredentialsProvider(context),
                                                          new SignalProtocolStoreImpl(context),
                                                          BuildConfig.USER_AGENT,
                                                          Optional.fromNullable(MessageRetrievalService.getPipe()),
                                                          Optional.of(new SecurityEventListener(context)));
    } else {
      this.messageSender.setMessagePipe(MessageRetrievalService.getPipe());
    }

    return this.messageSender;
  }

  @Provides
  synchronized SignalServiceMessageReceiver provideSignalMessageReceiver() {
    if (this.messageReceiver == null) {
      this.messageReceiver = new SignalServiceMessageReceiver(networkAccess.getConfiguration(context),
                                                              new DynamicCredentialsProvider(context),
                                                              BuildConfig.USER_AGENT,
                                                              new PipeConnectivityListener());
    }

    return this.messageReceiver;
  }

  private static class DynamicCredentialsProvider implements CredentialsProvider {

    private final Context context;

    private DynamicCredentialsProvider(Context context) {
      this.context = context.getApplicationContext();
    }

    @Override
    public String getUser() {
      return TextSecurePreferences.getLocalNumber(context);
    }

    @Override
    public String getPassword() {
      return TextSecurePreferences.getPushServerPassword(context);
    }

    @Override
    public String getSignalingKey() {
      return TextSecurePreferences.getSignalingKey(context);
    }
  }

  private class PipeConnectivityListener implements ConnectivityListener {

    @Override
    public void onConnected() {
      Log.w(TAG, "onConnected()");
    }

    @Override
    public void onConnecting() {
      Log.w(TAG, "onConnecting()");
    }

    @Override
    public void onDisconnected() {
      Log.w(TAG, "onDisconnected()");
    }

    @Override
    public void onAuthenticationFailure() {
      Log.w(TAG, "onAuthenticationFailure()");
      TextSecurePreferences.setUnauthorizedReceived(context, true);
      EventBus.getDefault().post(new ReminderUpdateEvent());
    }

  }

}
