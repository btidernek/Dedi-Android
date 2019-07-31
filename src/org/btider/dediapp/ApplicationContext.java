/*
 * Copyright (C) 2013 Open Whisper Systems
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
package org.btider.dediapp;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.support.multidex.MultiDexApplication;
import android.util.Log;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.security.ProviderInstaller;

import org.btider.dediapp.crypto.PRNGFixes;
import org.btider.dediapp.dependencies.AxolotlStorageModule;
import org.btider.dediapp.dependencies.InjectableType;
import org.btider.dediapp.dependencies.SignalCommunicationModule;
import org.btider.dediapp.jobs.CreateSignedPreKeyJob;
import org.btider.dediapp.jobs.FcmRefreshJob;
import org.btider.dediapp.jobs.requirements.MasterSecretRequirementProvider;
import org.btider.dediapp.jobs.requirements.ServiceRequirementProvider;
import org.btider.dediapp.jobs.requirements.SqlCipherMigrationRequirementProvider;
import org.btider.dediapp.notifications.NotificationChannels;
import org.btider.dediapp.push.SignalServiceNetworkAccess;
import org.btider.dediapp.service.DirectoryRefreshListener;
import org.btider.dediapp.service.ExpiringMessageManager;
import org.btider.dediapp.service.LocalBackupListener;
import org.btider.dediapp.service.RotateSignedPreKeyListener;
import org.btider.dediapp.service.UpdateApkRefreshListener;
import org.btider.dediapp.util.TextSecurePreferences;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.PeerConnectionFactory.InitializationOptions;
import org.webrtc.voiceengine.WebRtcAudioManager;
import org.webrtc.voiceengine.WebRtcAudioUtils;
import org.whispersystems.jobqueue.JobManager;
import org.whispersystems.jobqueue.dependencies.DependencyInjector;
import org.whispersystems.jobqueue.persistence.JavaJobSerializer;
import org.whispersystems.jobqueue.requirements.NetworkRequirementProvider;
import org.whispersystems.libsignal.logging.SignalProtocolLoggerProvider;
import org.whispersystems.libsignal.util.AndroidSignalProtocolLogger;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import dagger.ObjectGraph;

/**
 * Will be called once when the TextSecure process is created.
 *
 * We're using this as an insertion point to patch up the Android PRNG disaster,
 * to initialize the job manager, and to check for GCM registration freshness.
 *
 * @author Moxie Marlinspike
 */
public class ApplicationContext extends MultiDexApplication implements DependencyInjector {

  private static final String TAG = ApplicationContext.class.getName();

  private ExpiringMessageManager expiringMessageManager;
  private JobManager             jobManager;
  private ObjectGraph            objectGraph;

  public static ApplicationContext getInstance(Context context) {

    return (ApplicationContext)context.getApplicationContext();
  }

  @Override
  public void onCreate() {
    super.onCreate();

    try {
      ProviderInstaller.installIfNeeded(getApplicationContext());
    } catch (GooglePlayServicesRepairableException e) {
      e.printStackTrace();
    } catch (GooglePlayServicesNotAvailableException e) {
      e.printStackTrace();
    }

    initializeRandomNumberFix();
    initializeLogging();
    initializeDependencyInjection();
    initializeJobManager();
    initializeExpiringMessageManager();
    initializeGcmCheck();
    initializeSignedPreKeyCheck();
    initializePeriodicTasks();
    initializeCircumvention();
    initializeWebRtc();
    NotificationChannels.create(this);
  }

  @Override
  public void injectDependencies(Object object) {
    if (object instanceof InjectableType) {
      objectGraph.inject(object);
    }
  }

  public JobManager getJobManager() {
    return jobManager;
  }

  public ExpiringMessageManager getExpiringMessageManager() {
    return expiringMessageManager;
  }

  private void initializeRandomNumberFix() {
    PRNGFixes.apply();
  }

  private void initializeLogging() {
    SignalProtocolLoggerProvider.setProvider(new AndroidSignalProtocolLogger());
  }

  private void initializeJobManager() {
    this.jobManager = JobManager.newBuilder(this)
                                .withName("TextSecureJobs")
                                .withDependencyInjector(this)
                                .withJobSerializer(new JavaJobSerializer())
                                .withRequirementProviders(new MasterSecretRequirementProvider(this),
                                                          new ServiceRequirementProvider(this),
                                                          new NetworkRequirementProvider(this),
                                                          new SqlCipherMigrationRequirementProvider())
                                .withConsumerThreads(5)
                                .build();
  }

  private void initializeDependencyInjection() {
    this.objectGraph = ObjectGraph.create(new SignalCommunicationModule(this, new SignalServiceNetworkAccess(this)),
                                          new AxolotlStorageModule(this));
  }

  private void initializeGcmCheck() {
    if (TextSecurePreferences.isPushRegistered(this)) {
      long nextSetTime = TextSecurePreferences.getFcmTokenLastSetTime(this) + TimeUnit.HOURS.toMillis(6);

      if (TextSecurePreferences.getFcmToken(this) == null || nextSetTime <= System.currentTimeMillis()) {
        this.jobManager.add(new FcmRefreshJob(this));
      }
    }
  }

  private void initializeSignedPreKeyCheck() {
    if (!TextSecurePreferences.isSignedPreKeyRegistered(this)) {
      jobManager.add(new CreateSignedPreKeyJob(this));
    }
  }

  private void initializeExpiringMessageManager() {
    this.expiringMessageManager = new ExpiringMessageManager(this);
  }

  private void initializePeriodicTasks() {
    RotateSignedPreKeyListener.schedule(this);
    DirectoryRefreshListener.schedule(this);
    LocalBackupListener.schedule(this);

    if (BuildConfig.PLAY_STORE_DISABLED) {
      UpdateApkRefreshListener.schedule(this);
    }
  }

  private void initializeWebRtc() {
    try {
      Set<String> HARDWARE_AEC_BLACKLIST = new HashSet<String>() {{
        add("Pixel");
        add("Pixel XL");
        add("Moto G5");
        add("Moto G (5S) Plus");
      }};

      Set<String> OPEN_SL_ES_WHITELIST = new HashSet<String>() {{
        add("Pixel");
        add("Pixel XL");
      }};

      if (HARDWARE_AEC_BLACKLIST.contains(Build.MODEL)) {
        WebRtcAudioUtils.setWebRtcBasedAcousticEchoCanceler(true);
      }

      if (!OPEN_SL_ES_WHITELIST.contains(Build.MODEL)) {
        WebRtcAudioManager.setBlacklistDeviceForOpenSLESUsage(true);
      }

      PeerConnectionFactory.initialize(InitializationOptions.builder(this)
                                                            .setEnableVideoHwAcceleration(true)
                                                            .createInitializationOptions());
    } catch (UnsatisfiedLinkError e) {
      Log.w(TAG, e);
    }
  }

  @SuppressLint("StaticFieldLeak")
  private void initializeCircumvention() {
    AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
      @Override
      protected Void doInBackground(Void... params) {
        if (new SignalServiceNetworkAccess(ApplicationContext.this).isCensored(ApplicationContext.this)) {
          try {
            ProviderInstaller.installIfNeeded(ApplicationContext.this);
          } catch (Throwable t) {
            Log.w(TAG, t);
          }
        }
        return null;
      }
    };

    task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
  }

}
