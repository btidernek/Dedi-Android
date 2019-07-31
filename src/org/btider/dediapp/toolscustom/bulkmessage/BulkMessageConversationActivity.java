/*
 * Copyright (C) 2011 Whisper Systems
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
package org.btider.dediapp.toolscustom.bulkmessage;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.provider.Browser;
import android.provider.ContactsContract;
import android.provider.Telephony;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.WindowCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnKeyListener;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.places.ui.PlacePicker;

import org.btider.dediapp.ApplicationContext;
import org.btider.dediapp.ConversationFragment;
import org.btider.dediapp.GroupCreateActivity;
import org.btider.dediapp.PassphraseRequiredActionBarActivity;
import org.btider.dediapp.PromptMmsActivity;
import org.btider.dediapp.R;
import org.btider.dediapp.RecipientPreferenceActivity;
import org.btider.dediapp.RegistrationActivity;
import org.btider.dediapp.TransportOption;
import org.btider.dediapp.VerifyIdentityActivity;
import org.btider.dediapp.audio.AudioRecorder;
import org.btider.dediapp.audio.AudioSlidePlayer;
import org.btider.dediapp.color.MaterialColor;
import org.btider.dediapp.components.AnimatingToggle;
import org.btider.dediapp.components.AttachmentTypeSelector;
import org.btider.dediapp.components.ComposeText;
import org.btider.dediapp.components.HidingLinearLayout;
import org.btider.dediapp.components.InputAwareLayout;
import org.btider.dediapp.components.InputPanel;
import org.btider.dediapp.components.KeyboardAwareLinearLayout;
import org.btider.dediapp.components.SendButton;
import org.btider.dediapp.components.camera.QuickAttachmentDrawer;
import org.btider.dediapp.components.emoji.EmojiDrawer;
import org.btider.dediapp.components.emoji.EmojiStrings;
import org.btider.dediapp.components.identity.UntrustedSendDialog;
import org.btider.dediapp.components.identity.UnverifiedBannerView;
import org.btider.dediapp.components.identity.UnverifiedSendDialog;
import org.btider.dediapp.components.location.SignalPlace;
import org.btider.dediapp.components.reminder.ExpiredBuildReminder;
import org.btider.dediapp.components.reminder.InviteReminder;
import org.btider.dediapp.components.reminder.ReminderView;
import org.btider.dediapp.components.reminder.UnauthorizedReminder;
import org.btider.dediapp.contacts.ContactAccessor;
import org.btider.dediapp.contactshare.Contact;
import org.btider.dediapp.contactshare.ContactShareEditActivity;
import org.btider.dediapp.contactshare.ContactUtil;
import org.btider.dediapp.crypto.IdentityKeyParcelable;
import org.btider.dediapp.crypto.SecurityEvent;
import org.btider.dediapp.database.Address;
import org.btider.dediapp.database.DatabaseFactory;
import org.btider.dediapp.database.DraftDatabase;
import org.btider.dediapp.database.GroupDatabase;
import org.btider.dediapp.database.IdentityDatabase;
import org.btider.dediapp.database.MessagingDatabase;
import org.btider.dediapp.database.MmsSmsColumns;
import org.btider.dediapp.database.RecipientDatabase;
import org.btider.dediapp.database.ThreadDatabase;
import org.btider.dediapp.database.identity.IdentityRecordList;
import org.btider.dediapp.database.model.MessageRecord;
import org.btider.dediapp.database.model.MmsMessageRecord;
import org.btider.dediapp.events.ReminderUpdateEvent;
import org.btider.dediapp.giph.ui.GiphyActivity;
import org.btider.dediapp.jobs.MultiDeviceBlockedUpdateJob;
import org.btider.dediapp.jobs.RetrieveProfileJob;
import org.btider.dediapp.mms.AttachmentManager;
import org.btider.dediapp.mms.AudioSlide;
import org.btider.dediapp.mms.GlideApp;
import org.btider.dediapp.mms.GlideRequests;
import org.btider.dediapp.mms.LocationSlide;
import org.btider.dediapp.mms.MediaConstraints;
import org.btider.dediapp.mms.OutgoingMediaMessage;
import org.btider.dediapp.mms.OutgoingSecureMediaMessage;
import org.btider.dediapp.mms.QuoteId;
import org.btider.dediapp.mms.QuoteModel;
import org.btider.dediapp.mms.Slide;
import org.btider.dediapp.mms.SlideDeck;
import org.btider.dediapp.notifications.MarkReadReceiver;
import org.btider.dediapp.notifications.MessageNotifier;
import org.btider.dediapp.permissions.Permissions;
import org.btider.dediapp.profiles.GroupShareProfileView;
import org.btider.dediapp.profiles.InfoView;
import org.btider.dediapp.profiles.InformationSecurityInfoView;
import org.btider.dediapp.providers.PersistentBlobProvider;
import org.btider.dediapp.recipients.Recipient;
import org.btider.dediapp.recipients.RecipientFormattingException;
import org.btider.dediapp.recipients.RecipientModifiedListener;
import org.btider.dediapp.scribbles.ScribbleActivity;
import org.btider.dediapp.service.KeyCachingService;
import org.btider.dediapp.sms.MessageSender;
import org.btider.dediapp.sms.OutgoingEncryptedMessage;
import org.btider.dediapp.sms.OutgoingTextMessage;
import org.btider.dediapp.util.CharacterCalculator;
import org.btider.dediapp.util.DirectoryHelper;
import org.btider.dediapp.util.DynamicLanguage;
import org.btider.dediapp.util.DynamicTheme;
import org.btider.dediapp.util.IdentityUtil;
import org.btider.dediapp.util.MediaUtil;
import org.btider.dediapp.util.ServiceUtil;
import org.btider.dediapp.util.TextSecurePreferences;
import org.btider.dediapp.util.Util;
import org.btider.dediapp.util.ViewUtil;
import org.btider.dediapp.util.concurrent.AssertedSuccessListener;
import org.btider.dediapp.util.concurrent.ListenableFuture;
import org.btider.dediapp.util.concurrent.SettableFuture;
import org.btider.dediapp.util.views.Stub;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.whispersystems.libsignal.InvalidMessageException;
import org.whispersystems.libsignal.util.guava.Optional;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;

import gov.btider.multiselectgallary.GalleryActivity;

import static org.btider.dediapp.TransportOption.Type;
import static org.btider.dediapp.util.TextSecurePreferences.PREF_MEDIA_SIZE;
import static org.btider.dediapp.util.TextSecurePreferences.getBooleanPreference;
import static org.whispersystems.libsignal.SessionCipher.SESSION_LOCK;

//import me.nereo.multi_image_selector.MultiImageSelectorActivity;

/**
 * Activity for displaying a message thread, as well as
 * composing/sending a new message into that thread.
 *
 * @author Moxie Marlinspike
 */
@SuppressLint("StaticFieldLeak")
public class BulkMessageConversationActivity extends PassphraseRequiredActionBarActivity
        implements ConversationFragment.ConversationFragmentListener,
        AttachmentManager.AttachmentListener,
        RecipientModifiedListener,
        KeyboardAwareLinearLayout.OnKeyboardShownListener,
        QuickAttachmentDrawer.AttachmentDrawerListener,
        InputPanel.Listener,
        InputPanel.MediaListener{
    private static final String TAG = BulkMessageConversationActivity.class.getSimpleName();

    public static final String ADDRESS_EXTRA = "address";
    public static final String THREAD_ID_EXTRA = "thread_id";
    public static final String IS_ARCHIVED_EXTRA = "is_archived";
    public static final String TEXT_EXTRA = "draft_text";
    public static final String DISTRIBUTION_TYPE_EXTRA = "distribution_type";
    public static final String TIMING_EXTRA = "timing";
    public static final String LAST_SEEN_EXTRA = "last_seen";
    public static final String STARTING_POSITION_EXTRA = "starting_position";

    private static final int PICK_GALLERY = 1;
    private static final int PICK_DOCUMENT = 2;
    private static final int PICK_AUDIO = 3;
    private static final int PICK_CONTACT = 4;
    private static final int GET_CONTACT_DETAILS = 5;
    private static final int GROUP_EDIT = 6;
    private static final int TAKE_PHOTO = 7;
    private static final int ADD_CONTACT = 8;
    private static final int PICK_LOCATION = 9;
    private static final int PICK_GIF = 10;
    private static final int SMS_DEFAULT = 11;

    private GlideRequests glideRequests;
    protected ComposeText composeText;
    private AnimatingToggle buttonToggle;
    private SendButton sendButton;
    private ImageButton attachButton;
    protected BulkMessageConversationTitleView titleView;
    private TextView charactersLeft;
    private ConversationFragment fragment;
    private Button unblockButton;
    //private Button makeDefaultSmsButton;
    private Button registerButton;
    private InputAwareLayout container;
    private View composePanel;
    protected Stub<ReminderView> reminderView;
    private Stub<UnverifiedBannerView> unverifiedBannerView;
    private Stub<GroupShareProfileView> groupShareProfileView;
    private Stub<InformationSecurityInfoView> informationSecurityInfoViewStub;
    private Stub<InfoView> infoViewStub;

    private AttachmentTypeSelector attachmentTypeSelector;
    private AttachmentManager attachmentManager;
    private AudioRecorder audioRecorder;
    private BroadcastReceiver securityUpdateReceiver;
    private Stub<EmojiDrawer> emojiDrawerStub;
    protected HidingLinearLayout quickAttachmentToggle;
    private QuickAttachmentDrawer quickAttachmentDrawer;
    private InputPanel inputPanel;

    private Recipient recipient;
    private long threadId;
    private int distributionType;
    private boolean archived;
    private boolean isSecureText;
    private boolean isDefaultSms = true;
    private boolean isMmsEnabled = true;
    private boolean isSecurityInitialized = false;

    private final IdentityRecordList identityRecords = new IdentityRecordList();
    private final DynamicTheme dynamicTheme = new DynamicTheme();
    private final DynamicLanguage dynamicLanguage = new DynamicLanguage();

    @Override
    protected void onPreCreate() {
        dynamicTheme.onCreate(this);
        dynamicLanguage.onCreate(this);
    }


    private Integer scount = 0;
    private Long time = 500L;

    @Override
    protected void onCreate(Bundle state, boolean ready) {
        Log.w(TAG, "onCreate()");

        supportRequestWindowFeature(WindowCompat.FEATURE_ACTION_BAR_OVERLAY);
        setContentView(R.layout.conversation_activity);

        TypedArray typedArray = obtainStyledAttributes(new int[]{R.attr.conversation_background});
        int color = typedArray.getColor(0, Color.WHITE);
        typedArray.recycle();

        getWindow().getDecorView().setBackgroundColor(color);

        fragment = initFragment(R.id.fragment_content, new ConversationFragment(), dynamicLanguage.getCurrentLocale());

        initializeReceivers();
        initializeActionBar();
        initializeViews();
        initializeResources();
        initializeSecurity(false, isDefaultSms).addListener(new AssertedSuccessListener<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                initializeProfiles();
                initializeDraft();
            }
        });

        //fragment.setInformationSecurityInfoView(informationSecurityInfoViewStub.get());

        String address = "";
        for(Recipient recipient : recipients){
            address = address + recipient.getName()+", ";
        }
        address = address.substring(0,address.length()-2);

        infoViewStub.get().setVisibility(View.VISIBLE);
        infoViewStub.get().setMessage(getString(R.string.bulk_message_conversation_popup)+" "+address);
        infoViewStub.get().setBackground_color(R.color.bulk_notification_color);
        infoViewStub.get().setLogo(R.drawable.bulk_sender);
        infoViewStub.get().setText_color(Color.WHITE);
        infoViewStub.get().initialize();
        fragment.setInfoView(infoViewStub.get());

    }

    @Override
    protected void onNewIntent(Intent intent) {
        Log.w(TAG, "onNewIntent()");

        if (isFinishing()) {
            Log.w(TAG, "Activity is finishing...");
            return;
        }

        if (!Util.isEmpty(composeText) || attachmentManager.isAttachmentPresent()) {
            saveDraft();
            attachmentManager.clear(glideRequests, false);
            composeText.setText("");
        }

        setIntent(intent);
        initializeResources();
        initializeSecurity(false, isDefaultSms).addListener(new AssertedSuccessListener<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                initializeDraft();
            }
        });


        if (fragment != null) {
            fragment.onNewIntent();
        }

    }

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        dynamicTheme.onResume(this);
        dynamicLanguage.onResume(this);
        quickAttachmentDrawer.onResume();

        initializeEnabledCheck();
        initializeMmsEnabledCheck();
        initializeIdentityRecords();
        composeText.setTransport(sendButton.getSelectedTransport());

        titleView.setTitle(glideRequests, recipient);
        setActionBarColor(recipient.getColor());
        setBlockedUserState(recipient, isSecureText, isDefaultSms);
        setGroupShareProfileReminder(recipient);
        calculateCharactersRemaining();

        MessageNotifier.setVisibleThread(threadId);
        markThreadAsRead();

        Log.w(TAG, "onResume() Finished: " + (System.currentTimeMillis() - getIntent().getLongExtra(TIMING_EXTRA, 0)));
    }

    @Override
    protected void onPause() {
        super.onPause();
        MessageNotifier.setVisibleThread(-1L);
        if (isFinishing()) overridePendingTransition(R.anim.fade_scale_in, R.anim.slide_to_right);
        quickAttachmentDrawer.onPause();
        inputPanel.onPause();

        fragment.setLastSeen(System.currentTimeMillis());
        markLastSeen();
        AudioSlidePlayer.stopAll();
        EventBus.getDefault().unregister(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        Log.w(TAG, "onConfigurationChanged(" + newConfig.orientation + ")");
        super.onConfigurationChanged(newConfig);
        composeText.setTransport(sendButton.getSelectedTransport());
        quickAttachmentDrawer.onConfigurationChanged();

        if (emojiDrawerStub.resolved() && container.getCurrentInput() == emojiDrawerStub.get()) {
            container.hideAttachedInput(true);
        }
    }

    @Override
    protected void onDestroy() {
        saveDraft();
        if (recipient != null) recipient.removeListener(this);
        if (securityUpdateReceiver != null) unregisterReceiver(securityUpdateReceiver);
        super.onDestroy();
    }


    @Override
    public void onActivityResult(final int reqCode, int resultCode, Intent data) {
        Log.w(TAG, "onActivityResult called: " + reqCode + ", " + resultCode + " , " + data);

        super.onActivityResult(reqCode, resultCode, data);

        if ((data == null && reqCode != TAKE_PHOTO && reqCode != SMS_DEFAULT) ||
                (resultCode != RESULT_OK && reqCode != SMS_DEFAULT)) {
            return;
        }

        switch (reqCode) {
            case PICK_GALLERY:

                ArrayList<Uri> uris = new ArrayList<>();
                ArrayList<AttachmentManager.MediaType> mediaTypes = new ArrayList<>();

                if(data.getData() != null){
                    uris.add(data.getData());

                    AttachmentManager.MediaType mediaType;
                    String mimeType = MediaUtil.getMimeType(this, data.getData());
                    if (MediaUtil.isGif(mimeType)) mediaType = AttachmentManager.MediaType.GIF;
                    else if (MediaUtil.isVideo(mimeType)) mediaType = AttachmentManager.MediaType.VIDEO;
                    else mediaType = AttachmentManager.MediaType.IMAGE;

                    mediaTypes.add(mediaType);
                }else{
                    List<String> paths1 =  (List<String>) data.getSerializableExtra(GalleryActivity.PHOTOS);
                    if(paths1 == null){
                        paths1 = new ArrayList<>();
                        paths1.add((String) data.getSerializableExtra(GalleryActivity.VIDEO));
                    }
                   // List<String> paths2 =  (List<String>) data.getSerializableExtra(GalleryActivity.VIDEO);


                    int srtm = 0;
//                    List<String> path = data.getStringArrayListExtra(MultiImageSelectorActivity.EXTRA_RESULT);
                    for(String pat : paths1){
                        Uri myUri = Uri.parse("file://" +pat);
                        //Uri myUri = getUriFromPath(pat);


                        AttachmentManager.MediaType mediaType;
                        String mimeType = MediaUtil.getMimeType(this, myUri);
                        if (MediaUtil.isGif(mimeType)) mediaType = AttachmentManager.MediaType.GIF;
                        else if (MediaUtil.isVideo(mimeType)) mediaType = AttachmentManager.MediaType.VIDEO;
                        else mediaType = AttachmentManager.MediaType.IMAGE;

                        boolean stym = false;

                        if(getBooleanPreference(this, PREF_MEDIA_SIZE, false)){
                            File f = new File(myUri.getPath());
                            long size = f.length();
                            if(size >= 20971520){
                                stym = true;
                                srtm++;
                            }
                        }else{
                            File f = new File(myUri.getPath());
                            long size = f.length();
                            if(size >= 104857600){
                                stym = true;
                                srtm++;
                            }
                        }

                        if(!stym){
                            uris.add(myUri);
                            mediaTypes.add(mediaType);
                        }

                    }
                    if(srtm != 0){
                        if(getBooleanPreference(this, PREF_MEDIA_SIZE, false)){
                            Toast.makeText(this,
                                    String.format(this.getString(R.string.ConversationActivity_attachment_exceeds_size_limits_low_count),srtm+""),
                                    Toast.LENGTH_SHORT).show();
                        }else{
                            Toast.makeText(this,
                                    String.format(this.getString(R.string.ConversationActivity_attachment_exceeds_size_limits_count),srtm+""),
                                    Toast.LENGTH_SHORT).show();
                        }

                    }

                }

                setMediaInMedia(uris, mediaTypes);

                break;
            case PICK_DOCUMENT:
                setMedia(data.getData(), AttachmentManager.MediaType.DOCUMENT);
                break;
            case PICK_AUDIO:
                setMedia(data.getData(), AttachmentManager.MediaType.AUDIO);
                break;
            case PICK_CONTACT:
                // TODO(greyson): Re-enable shared contact sending after receiving has been enabled for a few releases
                //TODO: Kişi content'ini göndermek için kullanıyoruz.
                openContactShareEditor(data.getData());

                break;
            case GET_CONTACT_DETAILS:
                sendSharedContact(data.getParcelableArrayListExtra(ContactShareEditActivity.KEY_CONTACTS));
                break;
            case GROUP_EDIT:
                recipient = Recipient.from(this, data.getParcelableExtra(GroupCreateActivity.GROUP_ADDRESS_EXTRA), true);
                recipient.addListener(this);
                titleView.setTitle(glideRequests, recipient);
                setBlockedUserState(recipient, isSecureText, isDefaultSms);
                supportInvalidateOptionsMenu();
                break;
            case TAKE_PHOTO:
                if (attachmentManager.getCaptureUri() != null) {
                    setMedia(attachmentManager.getCaptureUri(), AttachmentManager.MediaType.IMAGE);
                }
                break;
            case ADD_CONTACT:
                recipient = Recipient.from(this, recipient.getAddress(), true);
                recipient.addListener(this);
                fragment.reloadList();
                break;
            case PICK_LOCATION:
                SignalPlace place = new SignalPlace(PlacePicker.getPlace(data, this));
                attachmentManager.setLocation(place, getCurrentMediaConstraints());
                break;
            case PICK_GIF:
                setMedia(data.getData(),
                        AttachmentManager.MediaType.GIF,
                        data.getIntExtra(GiphyActivity.EXTRA_WIDTH, 0),
                        data.getIntExtra(GiphyActivity.EXTRA_HEIGHT, 0));
                break;
            case ScribbleActivity.SCRIBBLE_REQUEST_CODE:
                setMedia(data.getData(), AttachmentManager.MediaType.IMAGE);
                break;
            case SMS_DEFAULT:
                initializeSecurity(isSecureText, isDefaultSms);
                break;
        }
    }

    @Override
    public void startActivity(Intent intent) {
        if (intent.getStringExtra(Browser.EXTRA_APPLICATION_ID) != null) {
            intent.removeExtra(Browser.EXTRA_APPLICATION_ID);
        }

        try {
            super.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.w(TAG, e);
            Toast.makeText(this, R.string.ConversationActivity_there_is_no_app_available_to_handle_this_link_on_your_device, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {


        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        super.onOptionsItemSelected(item);

        return false;
    }

    @Override
    public void onBackPressed() {
        Log.w(TAG, "onBackPressed()");
        if (container.isInputOpen()) container.hideCurrentInput(composeText);
        else super.onBackPressed();
    }

    @Override
    public void onKeyboardShown() {
        inputPanel.onKeyboardShown();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(ReminderUpdateEvent event) {
        updateReminders(recipient.hasSeenInviteReminder());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Permissions.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
    }

    //////// Event Handlers

    private void handleConversationSettings() {
        Intent intent = new Intent(BulkMessageConversationActivity.this, RecipientPreferenceActivity.class);
        intent.putExtra(RecipientPreferenceActivity.ADDRESS_EXTRA, recipient.getAddress());
        intent.putExtra(RecipientPreferenceActivity.CAN_HAVE_SAFETY_NUMBER_EXTRA,
                isSecureText && !isSelfConversation());

        startActivitySceneTransition(intent, titleView.findViewById(R.id.contact_photo_image), "avatar");
    }

    private void handleUnblock() {
        //noinspection CodeBlock2Expr
        new AlertDialog.Builder(this)
                .setTitle(R.string.ConversationActivity_unblock_this_contact_question)
                .setMessage(R.string.ConversationActivity_you_will_once_again_be_able_to_receive_messages_and_calls_from_this_contact)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.ConversationActivity_unblock, (dialog, which) -> {
                    new AsyncTask<Void, Void, Void>() {
                        @Override
                        protected Void doInBackground(Void... params) {
                            DatabaseFactory.getRecipientDatabase(BulkMessageConversationActivity.this)
                                    .setBlocked(recipient, false);

                            ApplicationContext.getInstance(BulkMessageConversationActivity.this)
                                    .getJobManager()
                                    .add(new MultiDeviceBlockedUpdateJob(BulkMessageConversationActivity.this));

                            return null;
                        }
                    }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                }).show();
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private void handleMakeDefaultSms() {
        Intent intent = new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
        intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, getPackageName());
        startActivityForResult(intent, SMS_DEFAULT);
    }

    private void handleRegisterForSignal() {
        Intent intent = new Intent(this, RegistrationActivity.class);
        intent.putExtra(RegistrationActivity.RE_REGISTRATION_EXTRA, true);
        startActivity(intent);
    }

    private void handleInviteLink() {
        try {
            String inviteText;

            boolean a = SecureRandom.getInstance("SHA1PRNG").nextBoolean();
            if (a)
                inviteText = getString(R.string.ConversationActivity_lets_switch_to_signal, getString(R.string.share_link));
            else
                inviteText = getString(R.string.ConversationActivity_lets_use_this_to_chat, getString(R.string.share_link));

            if (isDefaultSms) {
                composeText.appendInvite(inviteText);
            } else {
                Intent intent = new Intent(Intent.ACTION_SENDTO);
                intent.setData(Uri.parse("smsto:" + recipient.getAddress().serialize()));
                intent.putExtra("sms_body", inviteText);
                intent.putExtra(Intent.EXTRA_TEXT, inviteText);
                startActivity(intent);
            }
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError(e);
        }
    }

    private void handleAddToContacts() {
        if (recipient.getAddress().isGroup()) return;

        try {
            final Intent intent = new Intent(Intent.ACTION_INSERT_OR_EDIT);
            if (recipient.getAddress().isEmail()) {
                intent.putExtra(ContactsContract.Intents.Insert.EMAIL, recipient.getAddress().toEmailString());
            } else {
                intent.putExtra(ContactsContract.Intents.Insert.PHONE, recipient.getAddress().toPhoneString());
            }
            intent.setType(ContactsContract.Contacts.CONTENT_ITEM_TYPE);
            startActivityForResult(intent, ADD_CONTACT);
        } catch (ActivityNotFoundException e) {
            Log.w(TAG, e);
        }

    }

    private boolean handleDisplayQuickContact() {
        if (recipient.getAddress().isGroup()) return false;

        if (recipient.getContactUri() != null) {
            ContactsContract.QuickContact.showQuickContact(BulkMessageConversationActivity.this, titleView, recipient.getContactUri(), ContactsContract.QuickContact.MODE_LARGE, null);
        } else {
            handleAddToContacts();
        }

        return true;
    }

    private void handleAddAttachment() {
        if (this.isMmsEnabled || isSecureText) {
            if (attachmentTypeSelector == null) {
                attachmentTypeSelector = new AttachmentTypeSelector(this, getSupportLoaderManager(), new AttachmentTypeListener());
            }
            attachmentTypeSelector.show(this, attachButton);
        } else {
            handleManualMmsRequired();
        }
    }

    private void handleManualMmsRequired() {
        Toast.makeText(this, R.string.MmsDownloader_error_reading_mms_settings, Toast.LENGTH_LONG).show();

        Bundle extras = getIntent().getExtras();
        Intent intent = new Intent(this, PromptMmsActivity.class);
        if (extras != null) intent.putExtras(extras);
        startActivity(intent);
    }

    private void handleUnverifiedRecipients() {
        List<Recipient> unverifiedRecipients = identityRecords.getUnverifiedRecipients(this);
        List<IdentityDatabase.IdentityRecord> unverifiedRecords = identityRecords.getUnverifiedRecords();
        String message = IdentityUtil.getUnverifiedSendDialogDescription(this, unverifiedRecipients);

        if (message == null) return;

        //noinspection CodeBlock2Expr
        new UnverifiedSendDialog(this, message, unverifiedRecords, () -> {
            initializeIdentityRecords().addListener(new ListenableFuture.Listener<Boolean>() {
                @Override
                public void onSuccess(Boolean result) {
                    //sendMessage();
                }

                @Override
                public void onFailure(ExecutionException e) {
                    throw new AssertionError(e);
                }
            });
        }).show();
    }

    private void handleUntrustedRecipients() {
        List<Recipient> untrustedRecipients = identityRecords.getUntrustedRecipients(this);
        List<IdentityDatabase.IdentityRecord> untrustedRecords = identityRecords.getUntrustedRecords();
        String untrustedMessage = IdentityUtil.getUntrustedSendDialogDescription(this, untrustedRecipients);

        if (untrustedMessage == null) return;

        //noinspection CodeBlock2Expr
        new UntrustedSendDialog(this, untrustedMessage, untrustedRecords, () -> {
            initializeIdentityRecords().addListener(new ListenableFuture.Listener<Boolean>() {
                @Override
                public void onSuccess(Boolean result) {
                    //sendMessage();
                }

                @Override
                public void onFailure(ExecutionException e) {
                    throw new AssertionError(e);
                }
            });
        }).show();
    }

    private void handleSecurityChange(boolean isSecureText, boolean isDefaultSms) {
        Log.w(TAG, "handleSecurityChange(" + isSecureText + ", " + isDefaultSms + ")");
        if (isSecurityInitialized && isSecureText == this.isSecureText && isDefaultSms == this.isDefaultSms) {
            return;
        }

        this.isSecureText = isSecureText;
        this.isDefaultSms = isDefaultSms;
        this.isSecurityInitialized = true;

        boolean isMediaMessage = recipient.isMmsGroupRecipient() || attachmentManager.isAttachmentPresent();

        sendButton.resetAvailableTransports(isMediaMessage);

        if (!isSecureText && !isPushGroupConversation())
            sendButton.disableTransport(Type.TEXTSECURE);
        if (recipient.isPushGroupRecipient()) sendButton.disableTransport(Type.SMS);

        if (isSecureText || isPushGroupConversation())
            sendButton.setDefaultTransport(Type.TEXTSECURE);
        else sendButton.setDefaultTransport(Type.SMS);

        calculateCharactersRemaining();
        supportInvalidateOptionsMenu();
        setBlockedUserState(recipient, isSecureText, isDefaultSms);

    }

    ///// Initializers

    private void initializeDraft() {
        final String draftText = getIntent().getStringExtra(TEXT_EXTRA);
        if (draftText != null) composeText.setText(draftText);


        scount = getIntent().getIntExtra("image_size",0);
        if(scount >10){
            Toast.makeText(BulkMessageConversationActivity.this, R.string.ConversationActivity_message_is_send_max,
                    Toast.LENGTH_SHORT).show();
            return;
        }

        ArrayList<Uri> uris = getIntent().getParcelableArrayListExtra("media_uris");
        ArrayList<String> mineTypes = getIntent().getStringArrayListExtra("media_types");

       if(scount == 0)
            initializeDraftFromDatabase();
       else{
           setMediaOutMedia(uris, mineTypes);
           updateToggleButtonState();
       }

    }

    private void initializeEnabledCheck() {
        //alanların enable kısmı bu. Visible değişecek.
        boolean enabled = !(isPushGroupConversation() && !isActiveGroup());
        //System.out.println("------------------------------------------------------GELDIM-------------------------" + enabled + "--------------------------");
        inputPanel.setEnabled(enabled);
        sendButton.setEnabled(enabled);
        attachButton.setEnabled(enabled);

//        if (!enabled) {
//            composeText.setText("Gruptan çıkarıldınız.");
//        }

    }


    private void initializeDraftFromDatabase() {
        new AsyncTask<Void, Void, List<DraftDatabase.Draft>>() {
            @Override
            protected List<DraftDatabase.Draft> doInBackground(Void... params) {
                DraftDatabase draftDatabase = DatabaseFactory.getDraftDatabase(BulkMessageConversationActivity.this);
                List<DraftDatabase.Draft> results = draftDatabase.getDrafts(threadId);

                draftDatabase.clearDrafts(threadId);

                return results;
            }

            @Override
            protected void onPostExecute(List<DraftDatabase.Draft> drafts) {
                for (DraftDatabase.Draft draft : drafts) {
                    try {
                        switch (draft.getType()) {
                            case DraftDatabase.Draft.TEXT:
                                composeText.setText(draft.getValue());
                                break;
                            case DraftDatabase.Draft.LOCATION:
                                attachmentManager.setLocation(SignalPlace.deserialize(draft.getValue()), getCurrentMediaConstraints());
                                break;
                            case DraftDatabase.Draft.IMAGE:
                                setMedia(Uri.parse(draft.getValue()), AttachmentManager.MediaType.IMAGE);
                                break;
                            case DraftDatabase.Draft.AUDIO:
                                setMedia(Uri.parse(draft.getValue()), AttachmentManager.MediaType.AUDIO);
                                break;
                            case DraftDatabase.Draft.VIDEO:
                                setMedia(Uri.parse(draft.getValue()), AttachmentManager.MediaType.VIDEO);
                                break;
                            case DraftDatabase.Draft.QUOTE:
                                new QuoteRestorationTask(draft.getValue()).execute();
                                break;
                        }
                    } catch (IOException e) {
                        Log.w(TAG, e);
                    }
                }

                updateToggleButtonState();
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private ListenableFuture<Boolean> initializeSecurity(final boolean currentSecureText,
                                                         final boolean currentIsDefaultSms) {
        final SettableFuture<Boolean> future = new SettableFuture<>();

        handleSecurityChange(currentSecureText || isPushGroupConversation(), currentIsDefaultSms);

        new AsyncTask<Recipient, Void, boolean[]>() {
            @Override
            protected boolean[] doInBackground(Recipient... params) {
                Context context = BulkMessageConversationActivity.this;
                Recipient recipient = params[0];
                Log.w(TAG, "Resolving registered state...");
                RecipientDatabase.RegisteredState registeredState;

                if (recipient.isPushGroupRecipient()) {
                    Log.w(TAG, "Push group recipient...");
                    registeredState = RecipientDatabase.RegisteredState.REGISTERED;
                } else if (recipient.isResolving()) {
                    Log.w(TAG, "Talking to DB directly.");
                    registeredState = DatabaseFactory.getRecipientDatabase(BulkMessageConversationActivity.this).isRegistered(recipient.getAddress());
                } else {
                    Log.w(TAG, "Checking through resolved recipient");
                    registeredState = recipient.resolve().getRegistered();
                }

                Log.w(TAG, "Resolved registered state: " + registeredState);
                boolean signalEnabled = TextSecurePreferences.isPushRegistered(context);

                if (registeredState == RecipientDatabase.RegisteredState.UNKNOWN) {
                    try {
                        Log.w(TAG, "Refreshing directory for user: " + recipient.getAddress().serialize());
                        registeredState = DirectoryHelper.refreshDirectoryFor(context, recipient);
                    } catch (IOException e) {
                        Log.w(TAG, e);
                    }
                }

                Log.w(TAG, "Returning registered state...");
                return new boolean[]{registeredState == RecipientDatabase.RegisteredState.REGISTERED && signalEnabled,
                        Util.isDefaultSmsProvider(context)};


            }

            @Override
            protected void onPostExecute(boolean[] result) {
                if (result[0] != currentSecureText || result[1] != currentIsDefaultSms) {
                    Log.w(TAG, "onPostExecute() handleSecurityChange: " + result[0] + " , " + result[1]);
                    handleSecurityChange(result[0], result[1]);
                }
                future.set(true);
                onSecurityUpdated();

            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, recipient);

        return future;
    }

    private void onSecurityUpdated() {
        Log.w(TAG, "onSecurityUpdated()");
        updateReminders(recipient.hasSeenInviteReminder());
        updateDefaultSubscriptionId(recipient.getDefaultSubscriptionId());
    }

    protected void updateReminders(boolean seenInvite) {

        Log.w(TAG, "updateReminders(" + seenInvite + ")");

        if (UnauthorizedReminder.isEligible(this)) {
            reminderView.get().showReminder(new UnauthorizedReminder(this));
        } else if (ExpiredBuildReminder.isEligible()) {
            reminderView.get().showReminder(new ExpiredBuildReminder(this));
        } else if (TextSecurePreferences.isPushRegistered(this) &&
                TextSecurePreferences.isShowInviteReminders(this) &&
                !isSecureText &&
                !seenInvite &&
                !recipient.isGroupRecipient()) {
            InviteReminder reminder = new InviteReminder(this, recipient);
            reminder.setOkListener(v -> {
                handleInviteLink();
                reminderView.get().requestDismiss();
            });
            reminderView.get().showReminder(reminder);
        } else if (reminderView.resolved()) {
            reminderView.get().hide();
        }

    }

    private void updateDefaultSubscriptionId(Optional<Integer> defaultSubscriptionId) {
        Log.w(TAG, "updateDefaultSubscriptionId(" + defaultSubscriptionId.orNull() + ")");
        sendButton.setDefaultSubscriptionId(defaultSubscriptionId);
    }

    private void initializeMmsEnabledCheck() {
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... params) {
                return Util.isMmsCapable(BulkMessageConversationActivity.this);
            }

            @Override
            protected void onPostExecute(Boolean isMmsEnabled) {
                BulkMessageConversationActivity.this.isMmsEnabled = isMmsEnabled;
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private ListenableFuture<Boolean> initializeIdentityRecords() {
        final SettableFuture<Boolean> future = new SettableFuture<>();

        new AsyncTask<Recipient, Void, Pair<IdentityRecordList, String>>() {
            @Override
            protected @NonNull
            Pair<IdentityRecordList, String> doInBackground(Recipient... params) {
                IdentityDatabase identityDatabase = DatabaseFactory.getIdentityDatabase(BulkMessageConversationActivity.this);
                IdentityRecordList identityRecordList = new IdentityRecordList();
                List<Recipient> recipients = new LinkedList<>();

                if (params[0].isGroupRecipient()) {
                    recipients.addAll(DatabaseFactory.getGroupDatabase(BulkMessageConversationActivity.this)
                            .getGroupMembers(params[0].getAddress().toGroupString(), false));
                } else {
                    recipients.add(params[0]);
                }

                for (Recipient recipient : recipients) {
                    Log.w(TAG, "Loading identity for: " + recipient.getAddress());
                    identityRecordList.add(identityDatabase.getIdentity(recipient.getAddress()));
                }

                String message = null;

                if (identityRecordList.isUnverified()) {
                    message = IdentityUtil.getUnverifiedBannerDescription(BulkMessageConversationActivity.this, identityRecordList.getUnverifiedRecipients(BulkMessageConversationActivity.this));
                }

                return new Pair<>(identityRecordList, message);
            }

            @Override
            protected void onPostExecute(@NonNull Pair<IdentityRecordList, String> result) {
                Log.w(TAG, "Got identity records: " + result.first.isUnverified());
                identityRecords.replaceWith(result.first);

                if (result.second != null) {
                    Log.w(TAG, "Replacing banner...");
                    unverifiedBannerView.get().display(result.second, result.first.getUnverifiedRecords(),
                            new UnverifiedClickedListener(),
                            new UnverifiedDismissedListener());
                } else if (unverifiedBannerView.resolved()) {
                    Log.w(TAG, "Clearing banner...");
                    unverifiedBannerView.get().hide();
                }

                titleView.setVerified(isSecureText && identityRecords.isVerified());

                future.set(true);
            }

        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, recipient);

        return future;
    }

    private void initializeViews() {
        ActionBar supportActionBar = getSupportActionBar();
        if (supportActionBar == null) throw new AssertionError();

        titleView = (BulkMessageConversationTitleView) supportActionBar.getCustomView();
        buttonToggle = ViewUtil.findById(this, R.id.button_toggle);
        sendButton = ViewUtil.findById(this, R.id.send_button);
        attachButton = ViewUtil.findById(this, R.id.attach_button);
        composeText = ViewUtil.findById(this, R.id.embedded_text_editor);
        charactersLeft = ViewUtil.findById(this, R.id.space_left);
        emojiDrawerStub = ViewUtil.findStubById(this, R.id.emoji_drawer_stub);
        unblockButton = ViewUtil.findById(this, R.id.unblock_button);
//        makeDefaultSmsButton = ViewUtil.findById(this, R.id.make_default_sms_button);
        registerButton = ViewUtil.findById(this, R.id.register_button);
        composePanel = ViewUtil.findById(this, R.id.bottom_panel);
        container = ViewUtil.findById(this, R.id.layout_container);
        reminderView = ViewUtil.findStubById(this, R.id.reminder_stub);
        unverifiedBannerView = ViewUtil.findStubById(this, R.id.unverified_banner_stub);
        groupShareProfileView = ViewUtil.findStubById(this, R.id.group_share_profile_view_stub);

        informationSecurityInfoViewStub = ViewUtil.findStubById(this, R.id.information_security_info_title_stub);
        infoViewStub = ViewUtil.findStubById(this,R.id.info_title_stub);
        quickAttachmentDrawer = ViewUtil.findById(this, R.id.quick_attachment_drawer);
        quickAttachmentToggle = ViewUtil.findById(this, R.id.quick_attachment_toggle);
        inputPanel = ViewUtil.findById(this, R.id.bottom_panel);

        ImageButton quickCameraToggle = ViewUtil.findById(this, R.id.quick_camera_toggle);
        View composeBubble = ViewUtil.findById(this, R.id.compose_bubble);

        container.addOnKeyboardShownListener(this);
        inputPanel.setListener(this);
        inputPanel.setMediaListener(this);

        int[] attributes = new int[]{R.attr.conversation_item_bubble_background};
        TypedArray colors = obtainStyledAttributes(attributes);
        int defaultColor = colors.getColor(0, Color.WHITE);
        composeBubble.getBackground().setColorFilter(defaultColor, Mode.MULTIPLY);
        colors.recycle();

        attachmentTypeSelector = null;
        attachmentManager = new AttachmentManager(this, this);
        audioRecorder = new AudioRecorder(this);

        SendButtonListener sendButtonListener = new SendButtonListener();
        ComposeKeyPressedListener composeKeyPressedListener = new ComposeKeyPressedListener();

        composeText.setOnEditorActionListener(sendButtonListener);
        attachButton.setOnClickListener(new AttachButtonListener());
        attachButton.setOnLongClickListener(new AttachButtonLongClickListener());
        sendButton.setOnClickListener(sendButtonListener);
        sendButton.setEnabled(true);
        sendButton.addOnTransportChangedListener((newTransport, manuallySelected) -> {
            calculateCharactersRemaining();
            composeText.setTransport(newTransport);
            buttonToggle.getBackground().setColorFilter(newTransport.getBackgroundColor(), Mode.MULTIPLY);
            buttonToggle.getBackground().invalidateSelf();
            if (manuallySelected)
                recordSubscriptionIdPreference(newTransport.getSimSubscriptionId());
        });

        titleView.setOnClickListener(v -> handleConversationSettings());
        titleView.setOnLongClickListener(v -> handleDisplayQuickContact());
        titleView.setOnBackClickedListener(view -> super.onBackPressed());
        unblockButton.setOnClickListener(v -> handleUnblock());
        //makeDefaultSmsButton.setOnClickListener(v -> handleMakeDefaultSms());
        registerButton.setOnClickListener(v -> handleRegisterForSignal());

        composeText.setOnKeyListener(composeKeyPressedListener);
        composeText.addTextChangedListener(composeKeyPressedListener);
        composeText.setOnEditorActionListener(sendButtonListener);
        composeText.setOnClickListener(composeKeyPressedListener);
        composeText.setOnFocusChangeListener(composeKeyPressedListener);

        if (QuickAttachmentDrawer.isDeviceSupported(this)) {
            quickAttachmentDrawer.setListener(this);
            quickCameraToggle.setOnClickListener(new QuickCameraToggleListener());
        } else {
            quickCameraToggle.setVisibility(View.GONE);
            quickCameraToggle.setEnabled(false);
        }

    }

    protected void initializeActionBar() {
        ActionBar supportActionBar = getSupportActionBar();
        if (supportActionBar == null) throw new AssertionError();

        supportActionBar.setDisplayHomeAsUpEnabled(false);
        supportActionBar.setCustomView(R.layout.bulkmessage_conversation_title_view);
        supportActionBar.setDisplayShowCustomEnabled(true);
        supportActionBar.setDisplayShowTitleEnabled(false);
    }

    private ArrayList<Recipient> recipients = new ArrayList<>();
    private ArrayList<Long> threads = new ArrayList<>();
    private int sendCount = 0;
    private int recipient_size = 0;

    private void initializeResources() {
        if (recipient != null) recipient.removeListener(this);

        recipient_size = getIntent().getIntExtra("recipient_size",0);
        for(int i=0; i<recipient_size;i++){
            Address address = getIntent().getParcelableExtra("recipient_"+i);
            Long threadID = getIntent().getLongExtra("threads_"+i,0);
            threads.add(threadID);
            recipients.add( Recipient.from(this, address, true));
        }


        recipient = Recipient.from(this, getIntent().getParcelableExtra(ADDRESS_EXTRA), true);
        threadId = getIntent().getLongExtra(THREAD_ID_EXTRA, -1000);
        archived = getIntent().getBooleanExtra(IS_ARCHIVED_EXTRA, false);
        distributionType = getIntent().getIntExtra(DISTRIBUTION_TYPE_EXTRA, ThreadDatabase.DistributionTypes.DEFAULT);
        glideRequests = GlideApp.with(this);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            LinearLayout conversationContainer = ViewUtil.findById(this, R.id.conversation_container);
            conversationContainer.setClipChildren(true);
            conversationContainer.setClipToPadding(true);
        }

        recipient.addListener(this);
    }

    private void initializeProfiles() {
        if (!isSecureText) {
            Log.w(TAG, "SMS contact, no profile fetch needed.");
            return;
        }

        ApplicationContext.getInstance(this)
                .getJobManager()
                .add(new RetrieveProfileJob(this, recipient));
    }
//TODO: BURASI DEĞİŞTİRİLECEK ÜST KISIM İÇİN
    @Override
    public void onModified(final Recipient recipient) {
        Log.w(TAG, "onModified(" + recipient.getAddress().serialize() + ")");
        Util.runOnMain(() -> {
            Log.w(TAG, "onModifiedRun(): " + recipient.getRegistered());
            titleView.setTitle(glideRequests, recipient);
            titleView.setVerified(identityRecords.isVerified());
            setBlockedUserState(recipient, isSecureText, isDefaultSms);
            setActionBarColor(recipient.getColor());
            setGroupShareProfileReminder(recipient);
            updateReminders(recipient.hasSeenInviteReminder());
            updateDefaultSubscriptionId(recipient.getDefaultSubscriptionId());
            initializeSecurity(isSecureText, isDefaultSms);
            //invalidateOptionsMenu();
        });
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onIdentityRecordUpdate(final IdentityDatabase.IdentityRecord event) {
        initializeIdentityRecords();
    }

    private void initializeReceivers() {
        securityUpdateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                initializeSecurity(isSecureText, isDefaultSms);
                calculateCharactersRemaining();
            }
        };

        registerReceiver(securityUpdateReceiver,
                new IntentFilter(SecurityEvent.SECURITY_UPDATE_EVENT),
                KeyCachingService.KEY_PERMISSION, null);
    }

    //////// Helper Methods

    private void addAttachment(int type) {
        Log.w("ComposeMessageActivity", "Selected: " + type);
        switch (type) {
            case AttachmentTypeSelector.ADD_GALLERY:
                AttachmentManager.selectGallery(this, PICK_GALLERY,1);
                break;
            case AttachmentTypeSelector.ADD_DOCUMENT:
                AttachmentManager.selectDocument(this, PICK_DOCUMENT);
                break;
            case AttachmentTypeSelector.ADD_SOUND:
                AttachmentManager.selectAudio(this, PICK_AUDIO);
                break;
            case AttachmentTypeSelector.ADD_CONTACT_INFO:
                AttachmentManager.selectContactInfo(this, PICK_CONTACT);
                break;
            case AttachmentTypeSelector.ADD_LOCATION:
                AttachmentManager.selectLocation(this, PICK_LOCATION);
                break;
            case AttachmentTypeSelector.TAKE_PHOTO:
                attachmentManager.capturePhoto(this, TAKE_PHOTO);
                break;
            case AttachmentTypeSelector.ADD_GIF:
                AttachmentManager.selectGif(this, PICK_GIF, !isSecureText);
                break;
        }
    }

    private void setMedia(@Nullable Uri uri, @NonNull AttachmentManager.MediaType mediaType) {
        setMedia(uri, mediaType, 0, 0);
    }

    private void setMedia(@Nullable Uri uri, @NonNull AttachmentManager.MediaType mediaType, int width, int height) {
        if (uri == null) return;

        // TODO(greyson): Re-enable shared contact sending after receiving has been enabled for a few releases
        attachmentManager.setMedia(glideRequests, uri, mediaType, getCurrentMediaConstraints(), width, height);

       // truCopInAPP(1);
//    if (MediaType.VCARD.equals(mediaType) && isSecureText) {
//      openContactShareEditor(uri);
//    } else {
//      attachmentManager.setMedia(glideRequests, uri, mediaType, getCurrentMediaConstraints(), width, height);
//    }
    }

    private void setMediaOutMedia(@Nullable ArrayList<Uri> uris, @NonNull ArrayList<String> mediaTypes){
        ArrayList<AttachmentManager.MediaType> mediaTypem = new ArrayList<>();

        for(String mimeType : mediaTypes){
            AttachmentManager.MediaType mediaType;
            if (MediaUtil.isGif(mimeType)) mediaType = AttachmentManager.MediaType.GIF;
            else if (MediaUtil.isVideo(mimeType)) mediaType = AttachmentManager.MediaType.VIDEO;
            else mediaType = AttachmentManager.MediaType.IMAGE;
            mediaTypem.add(mediaType);
        }

        setMediaSendMedia(uris,mediaTypem,0,0);
    }

    private void setMediaInMedia(@Nullable ArrayList<Uri> uri, @NonNull ArrayList<AttachmentManager.MediaType> mediaType) {
        setMediaSendMedia(uri, mediaType, 0, 0);
    }

    private int colMultiInCount = 0;
    private int colMultiInCountTotal = 0;
    private ArrayList<Uri> uris = new ArrayList<>();
    private ArrayList<AttachmentManager.MediaType> mediaTypes = new ArrayList<>();
    private void setMediaSendMedia(@Nullable ArrayList<Uri> uri, @NonNull ArrayList<AttachmentManager.MediaType> mediaType, int width, int height) {
        if (uri == null) {
            uris = new ArrayList<>();
            colMultiInCount = 0;
            colMultiInCountTotal = 0;
            return;
        }
        if (uri.size() == 0) {
            uris = new ArrayList<>();
            colMultiInCount = 0;
            colMultiInCountTotal = 0;
            return;
        }

        uris = uri;
        mediaTypes = mediaType;
        colMultiInCountTotal = uri.size();

        colMultiIn();

    }
    private void colMultiIn(){
        attachmentManager.setMedia(glideRequests, uris.get(colMultiInCount), mediaTypes.get(colMultiInCount), getCurrentMediaConstraints(), 0, 0);
        truCopInAPP(uris.get(colMultiInCount),mediaTypes.get(colMultiInCount));
    }

    private void openContactShareEditor(Uri contactUri) {
        Intent intent = ContactShareEditActivity.getIntent(this, Collections.singletonList(contactUri));
        startActivityForResult(intent, GET_CONTACT_DETAILS);
    }

    private void addAttachmentContactInfo(Uri contactUri) {
        ContactAccessor contactDataList = ContactAccessor.getInstance();
        ContactAccessor.ContactData contactData = contactDataList.getContactData(this, contactUri);

        if (contactData.numbers.size() == 1) composeText.append(contactData.numbers.get(0).number);
        else if (contactData.numbers.size() > 1) selectContactInfo(contactData);
    }

    private void sendSharedContact(List<Contact> contacts) {
        int subscriptionId = sendButton.getSelectedTransport().getSimSubscriptionId().or(-1);
        long expiresIn = recipient.getExpireMessages() * 1000L;
        boolean initiating = threadId == -1;

        sendMediaMessage(isSmsForced(), "", attachmentManager.buildSlideDeck(), contacts, expiresIn, subscriptionId, initiating);
    }

    private void selectContactInfo(ContactAccessor.ContactData contactData) {
        final CharSequence[] numbers = new CharSequence[contactData.numbers.size()];
        final CharSequence[] numberItems = new CharSequence[contactData.numbers.size()];

        for (int i = 0; i < contactData.numbers.size(); i++) {
            numbers[i] = contactData.numbers.get(i).number;
            numberItems[i] = contactData.numbers.get(i).type + ": " + contactData.numbers.get(i).number;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setIconAttribute(R.attr.conversation_attach_contact_info);
        builder.setTitle(R.string.ConversationActivity_select_contact_info);

        builder.setItems(numberItems, (dialog, which) -> composeText.append(numbers[which]));
        builder.show();
    }

    private DraftDatabase.Drafts getDraftsForCurrentState() {
        DraftDatabase.Drafts drafts = new DraftDatabase.Drafts();

        if (!Util.isEmpty(composeText)) {
            drafts.add(new DraftDatabase.Draft(DraftDatabase.Draft.TEXT, composeText.getTextTrimmed()));
        }

        for (Slide slide : attachmentManager.buildSlideDeck().getSlides()) {
            if (slide.hasAudio() && slide.getUri() != null)
                drafts.add(new DraftDatabase.Draft(DraftDatabase.Draft.AUDIO, slide.getUri().toString()));
            else if (slide.hasVideo() && slide.getUri() != null)
                drafts.add(new DraftDatabase.Draft(DraftDatabase.Draft.VIDEO, slide.getUri().toString()));
            else if (slide.hasLocation())
                drafts.add(new DraftDatabase.Draft(DraftDatabase.Draft.LOCATION, ((LocationSlide) slide).getPlace().serialize()));
            else if (slide.hasImage() && slide.getUri() != null)
                drafts.add(new DraftDatabase.Draft(DraftDatabase.Draft.IMAGE, slide.getUri().toString()));
        }

        Optional<QuoteModel> quote = inputPanel.getQuote();

        if (quote.isPresent()) {
            drafts.add(new DraftDatabase.Draft(DraftDatabase.Draft.QUOTE, new QuoteId(quote.get().getId(), quote.get().getAuthor()).serialize()));
        }

        return drafts;
    }

    protected ListenableFuture<Long> saveDraft() {
        final SettableFuture<Long> future = new SettableFuture<>();

        if (this.recipient == null) {
            future.set(threadId);
            return future;
        }

        final DraftDatabase.Drafts drafts = getDraftsForCurrentState();
        final long thisThreadId = this.threadId;
        final int thisDistributionType = this.distributionType;

        new AsyncTask<Long, Void, Long>() {
            @Override
            protected Long doInBackground(Long... params) {
                ThreadDatabase threadDatabase = DatabaseFactory.getThreadDatabase(BulkMessageConversationActivity.this);
                DraftDatabase draftDatabase = DatabaseFactory.getDraftDatabase(BulkMessageConversationActivity.this);
                long threadId = params[0];

                if (drafts.size() > 0) {
                    if (threadId == -1)
                        threadId = threadDatabase.getThreadIdFor(getRecipient(), thisDistributionType);

                    draftDatabase.insertDrafts(threadId, drafts);
                    threadDatabase.updateSnippet(threadId, drafts.getSnippet(BulkMessageConversationActivity.this),
                            drafts.getUriSnippet(),
                            System.currentTimeMillis(), MmsSmsColumns.Types.BASE_DRAFT_TYPE, true);
                } else if (threadId > 0) {
                    threadDatabase.update(threadId, false);
                }

                return threadId;
            }

            @Override
            protected void onPostExecute(Long result) {
                future.set(result);
            }

        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, thisThreadId);

        return future;
    }

    private void setActionBarColor(MaterialColor color) {
        ActionBar supportActionBar = getSupportActionBar();
        if (supportActionBar == null) throw new AssertionError();
        supportActionBar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#2090ea")));
        setStatusBarColor(color.toStatusBarColor(this));
    }

    private void setBlockedUserState(Recipient recipient, boolean isSecureText, boolean isDefaultSms) {
        if (recipient.isBlocked()) {
            unblockButton.setVisibility(View.VISIBLE);
            composePanel.setVisibility(View.GONE);
            //makeDefaultSmsButton.setVisibility(View.GONE);
            registerButton.setVisibility(View.GONE);
        } else if (!isSecureText && isPushGroupConversation()) {
            unblockButton.setVisibility(View.GONE);
            composePanel.setVisibility(View.GONE);
//            makeDefaultSmsButton.setVisibility(View.GONE);
            registerButton.setVisibility(View.VISIBLE);
        } else if (!isSecureText && !isDefaultSms) {
            unblockButton.setVisibility(View.GONE);
            composePanel.setVisibility(View.GONE);
//            makeDefaultSmsButton.setVisibility(View.VISIBLE);
            registerButton.setVisibility(View.GONE);
        } else {
            composePanel.setVisibility(View.VISIBLE);
            unblockButton.setVisibility(View.GONE);
//            makeDefaultSmsButton.setVisibility(View.GONE);
            registerButton.setVisibility(View.GONE);
        }
    }

    private void setGroupShareProfileReminder(@NonNull Recipient recipient) {
        if (recipient.isPushGroupRecipient() && !recipient.isProfileSharing()) {
            groupShareProfileView.get().setRecipient(recipient);
            groupShareProfileView.get().setVisibility(View.VISIBLE);
        } else if (groupShareProfileView.resolved()) {
            groupShareProfileView.get().setVisibility(View.GONE);
        }
    }

    private void calculateCharactersRemaining() {
        String messageBody = composeText.getTextTrimmed();
        TransportOption transportOption = sendButton.getSelectedTransport();
        CharacterCalculator.CharacterState characterState = transportOption.calculateCharacters(messageBody);

        if (characterState.charactersRemaining <= 15 || characterState.messagesSpent > 1) {
            charactersLeft.setText(String.format(dynamicLanguage.getCurrentLocale(),
                    "%d/%d (%d)",
                    characterState.charactersRemaining,
                    characterState.maxMessageSize,
                    characterState.messagesSpent));
            charactersLeft.setVisibility(View.VISIBLE);
        } else {
            charactersLeft.setVisibility(View.GONE);
        }
    }

    private boolean isSingleConversation() {
        return getRecipient() != null && !getRecipient().isGroupRecipient();
    }

    private boolean isActiveGroup() {
        if (!isGroupConversation()) return false;

        Optional<GroupDatabase.GroupRecord> record = DatabaseFactory.getGroupDatabase(this).getGroup(getRecipient().getAddress().toGroupString());
        return record.isPresent() && record.get().isActive();
    }

    @SuppressWarnings("SimplifiableIfStatement")
    private boolean isSelfConversation() {
        if (!TextSecurePreferences.isPushRegistered(this)) return false;
        if (recipient.isGroupRecipient()) return false;

        return Util.isOwnNumber(this, recipient.getAddress());
    }

    private boolean isGroupConversation() {
        return getRecipient() != null && getRecipient().isGroupRecipient();
    }

    private boolean isPushGroupConversation() {
        return getRecipient() != null && getRecipient().isPushGroupRecipient();
    }

    private boolean isSmsForced() {
        return sendButton.isManualSelection() && sendButton.getSelectedTransport().isSms();
    }

    protected Recipient getRecipient() {
        return this.recipient;
    }

    protected long getThreadId() {
        return this.threadId;
    }

    private String getMessage() throws InvalidMessageException {
        String rawText = composeText.getTextTrimmed();

        if (rawText.length() < 1 && !attachmentManager.isAttachmentPresent())
            throw new InvalidMessageException(getString(R.string.ConversationActivity_message_is_empty_exclamation));

        return rawText;
    }

    private MediaConstraints getCurrentMediaConstraints() {
        return sendButton.getSelectedTransport().getType() == Type.TEXTSECURE
                ? MediaConstraints.getPushMediaConstraints()
                : MediaConstraints.getMmsMediaConstraints(sendButton.getSelectedTransport().getSimSubscriptionId().or(-1));
    }

    private void markThreadAsRead() {
        new AsyncTask<Long, Void, Void>() {
            @Override
            protected Void doInBackground(Long... params) {
                Context context = BulkMessageConversationActivity.this;
                List<MessagingDatabase.MarkedMessageInfo> messageIds = DatabaseFactory.getThreadDatabase(context).setRead(params[0], false);

                MessageNotifier.updateNotification(context);
                MarkReadReceiver.process(context, messageIds);

                return null;
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, threadId);
    }

    private void markLastSeen() {
        new AsyncTask<Long, Void, Void>() {
            @Override
            protected Void doInBackground(Long... params) {
                DatabaseFactory.getThreadDatabase(BulkMessageConversationActivity.this).setLastSeen(params[0]);
                return null;
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, threadId);
    }


    protected void sendComplete(long threadId) {



        sendCount++;
        if(sendCount < recipient_size){
            recipient = recipients.get(sendCount);
            this.threadId = threads.get(sendCount);
            sendMessage();
        }else{
            this.composeText.setText("");
            inputPanel.clearQuote();
            attachmentManager.clear(glideRequests, false);

            boolean refreshFragment = (threadId != this.threadId);
            this.threadId = threadId;

            if (fragment == null || !fragment.isVisible() || isFinishing()) {
                return;
            }

            fragment.setLastSeen(0);

            if (refreshFragment) {
                fragment.reload(recipient, threadId);
                MessageNotifier.setVisibleThread(threadId);
            }

            fragment.scrollToBottom();

            if(colMultiInCount == colMultiInCountTotal){
                attachmentManager.cleanup();
            }

            Toast.makeText(getApplicationContext(),getString(R.string.bulk_message_success),Toast.LENGTH_LONG).show();

            finish();
        }


    }

    public void sendMessage() {
        try {
            Recipient recipient = getRecipient();

            if (recipient == null) {
                throw new RecipientFormattingException("Badly formatted");
            }

            boolean forceSms = sendButton.isManualSelection() && sendButton.getSelectedTransport().isSms();

            int subscriptionId = sendButton.getSelectedTransport().getSimSubscriptionId().or(-1);
            long expiresIn = recipient.getExpireMessages() * 1000L;
            boolean initiating = threadId == -1;

            Log.w(TAG, "isManual Selection: " + sendButton.isManualSelection());
            Log.w(TAG, "forceSms: " + forceSms);

            if ((recipient.isMmsGroupRecipient() || recipient.getAddress().isEmail()) && !isMmsEnabled) {
                handleManualMmsRequired();
            } else if (!forceSms && identityRecords.isUnverified()) {
                handleUnverifiedRecipients();
            } else if (!forceSms && identityRecords.isUntrusted()) {
                handleUntrustedRecipients();
            } else if (attachmentManager.isAttachmentPresent() || recipient.isGroupRecipient() || recipient.getAddress().isEmail() || inputPanel.getQuote().isPresent()) {
                sendMediaMessage(forceSms, expiresIn, subscriptionId, initiating);
            } else {
                sendTextMessage(forceSms, expiresIn, subscriptionId, initiating);
            }
        } catch (RecipientFormattingException ex) {
            Toast.makeText(BulkMessageConversationActivity.this,
                    R.string.ConversationActivity_recipient_is_not_a_valid_sms_or_email_address_exclamation,
                    Toast.LENGTH_LONG).show();
            Log.w(TAG, ex);
        } catch (InvalidMessageException ex) {
            Toast.makeText(BulkMessageConversationActivity.this, R.string.ConversationActivity_message_is_empty_exclamation,
                    Toast.LENGTH_SHORT).show();
            Log.w(TAG, ex);
        }
    }

    private Timer timer;
    public static boolean conttimer = false;
    private TimerTask repeatedTask;
    private void truCopInAPP(Uri draftMedia, AttachmentManager.MediaType draftMediaType){

        if(colMultiInCount < colMultiInCountTotal){
            final String draftText = getIntent().getStringExtra(TEXT_EXTRA);

            if (draftText != null) composeText.setText(draftText);

            if (draftText == null && draftMedia == null && draftMediaType == null) {
                initializeDraftFromDatabase();
            } else {
                updateToggleButtonState();
            }

            repeatedTask = new TimerTask() {
                public void run() {

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if(conttimer){
                                conttimer = false;
                                repeatedTask.cancel();
                                timer.cancel();
                                timer.purge();

                                sendButton.performClick();

                                colMultiInCount++;

                                if(colMultiInCount < colMultiInCountTotal) {
                                    colMultiIn();
                                }else{
                                    colMultiInCount = 0;
                                    colMultiInCountTotal = 0;
                                }
                            }
                        }

                    });
                }
            };
            timer = new Timer("Timer2");
            long delay  = time;
            long period = time;
            timer.scheduleAtFixedRate(repeatedTask, delay, period);


        }else{
            colMultiInCount = 0;
            colMultiInCountTotal = 0;
        }


    }

    private void sendMediaMessage(final boolean forceSms, final long expiresIn, final int subscriptionId, boolean initiating)
            throws InvalidMessageException {
        Log.w(TAG, "Sending media message...");
        sendMediaMessage(forceSms, getMessage(), attachmentManager.buildSlideDeck(), Collections.emptyList(), expiresIn, subscriptionId, initiating);
    }

    private ListenableFuture<Void> sendMediaMessage(final boolean forceSms, String body, SlideDeck slideDeck, List<Contact> contacts, final long expiresIn, final int subscriptionId, final boolean initiating) {
        OutgoingMediaMessage outgoingMessageCandidate = new OutgoingMediaMessage(recipient, slideDeck, body, System.currentTimeMillis(), subscriptionId, expiresIn, distributionType, inputPanel.getQuote().orNull(), contacts);

        final SettableFuture<Void> future = new SettableFuture<>();
        final Context context = getApplicationContext();

        final OutgoingMediaMessage outgoingMessage;

        if (isSecureText && !forceSms) {
            outgoingMessage = new OutgoingSecureMediaMessage(outgoingMessageCandidate);
        } else {
            outgoingMessage = outgoingMessageCandidate;
        }

        final long id = fragment.stageOutgoingMessage(outgoingMessage);

        new AsyncTask<Void, Void, Long>() {
            @Override
            protected Long doInBackground(Void... param) {
                if (initiating) {
                    DatabaseFactory.getRecipientDatabase(context).setProfileSharing(recipient, true);
                }

                return MessageSender.send(context, outgoingMessage, threadId, forceSms, () -> fragment.releaseOutgoingMessage(id));
            }

            @Override
            protected void onPostExecute(Long result) {
                sendComplete(result);
                future.set(null);
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        return future;
    }
///this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    private void sendTextMessage(final boolean forceSms, final long expiresIn, final int subscriptionId, final boolean initiatingConversation)
            throws InvalidMessageException {
        final Context context = getApplicationContext();
        final String messageBody = getMessage();

        OutgoingTextMessage message;

        if (isSecureText && !forceSms) {
            message = new OutgoingEncryptedMessage(recipient, messageBody, expiresIn);
        } else {
            message = new OutgoingTextMessage(recipient, messageBody, expiresIn, subscriptionId);
        }

        final long id = fragment.stageOutgoingMessage(message);

        new AsyncTask<OutgoingTextMessage, Void, Long>() {
            @Override
            protected Long doInBackground(OutgoingTextMessage... messages) {
                if (initiatingConversation) {
                    DatabaseFactory.getRecipientDatabase(context).setProfileSharing(recipient, true);
                }

                return MessageSender.send(context, messages[0], threadId, forceSms, () -> fragment.releaseOutgoingMessage(id));
            }

            @Override
            protected void onPostExecute(Long result) {
                sendComplete(result);
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, message);
    }

    private void updateToggleButtonState() {
        if (composeText.getText().length() == 0 && !attachmentManager.isAttachmentPresent()) {
            buttonToggle.display(attachButton);
            quickAttachmentToggle.show();
        } else {
            buttonToggle.display(sendButton);
            quickAttachmentToggle.hide();
        }


    }

    private void recordSubscriptionIdPreference(final Optional<Integer> subscriptionId) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                DatabaseFactory.getRecipientDatabase(BulkMessageConversationActivity.this)
                        .setDefaultSubscriptionId(recipient, subscriptionId.or(-1));
                return null;
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    public void onAttachmentDrawerStateChanged(QuickAttachmentDrawer.DrawerState drawerState) {
        ActionBar supportActionBar = getSupportActionBar();
        if (supportActionBar == null) throw new AssertionError();

        if (drawerState == QuickAttachmentDrawer.DrawerState.FULL_EXPANDED) {
            supportActionBar.hide();
        } else {
            supportActionBar.show();
        }

        if (drawerState == QuickAttachmentDrawer.DrawerState.COLLAPSED) {
            container.hideAttachedInput(true);
            this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER);
        }else{
            this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
    }

    @Override
    public void onImageCapture(@NonNull final byte[] imageBytes) {
        setMedia(PersistentBlobProvider.getInstance(this)
                        .create(this, imageBytes, MediaUtil.IMAGE_JPEG, null),
                AttachmentManager.MediaType.IMAGE);
        quickAttachmentDrawer.hide(false);
    }

    @Override
    public void onVideoCapture(@NonNull byte[] imageBytes) {
        setMedia(PersistentBlobProvider.getInstance(this).create(this, imageBytes, MediaUtil.VIDEO_UNSPECIFIED, null),
                AttachmentManager.MediaType.VIDEO);
        quickAttachmentDrawer.hide(false);
    }

    @Override
    public void onCameraFail() {
        Toast.makeText(this, R.string.ConversationActivity_quick_camera_unavailable, Toast.LENGTH_SHORT).show();
        quickAttachmentDrawer.hide(false);
        quickAttachmentToggle.disable();
    }

    @Override
    public void onCameraStart() {
    }

    @Override
    public void onCameraStop() {
    }

    @Override
    public void onRecorderPermissionRequired() {
        Permissions.with(this)
                .request(Manifest.permission.RECORD_AUDIO)
                .ifNecessary()
                .withRationaleDialog(getString(R.string.ConversationActivity_to_send_audio_messages_allow_signal_access_to_your_microphone), R.drawable.ic_mic_white_48dp)
                .withPermanentDenialDialog(getString(R.string.ConversationActivity_signal_requires_the_microphone_permission_in_order_to_send_audio_messages))
                .execute();
    }

    @Override
    public void onRecorderStarted() {
        Vibrator vibrator = ServiceUtil.getVibrator(this);
        vibrator.vibrate(20);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        audioRecorder.startRecording();
    }

    @Override
    public void onRecorderFinished() {
        Vibrator vibrator = ServiceUtil.getVibrator(this);
        vibrator.vibrate(20);

        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        ListenableFuture<Pair<Uri, Long>> future = audioRecorder.stopRecording();
        future.addListener(new ListenableFuture.Listener<Pair<Uri, Long>>() {
            @Override
            public void onSuccess(final @NonNull Pair<Uri, Long> result) {
                boolean forceSms = sendButton.isManualSelection() && sendButton.getSelectedTransport().isSms();
                int subscriptionId = sendButton.getSelectedTransport().getSimSubscriptionId().or(-1);
                long expiresIn = recipient.getExpireMessages() * 1000L;
                boolean initiating = threadId == -1;
                AudioSlide audioSlide = new AudioSlide(BulkMessageConversationActivity.this, result.first, result.second, MediaUtil.AUDIO_AAC, true);
                SlideDeck slideDeck = new SlideDeck();
                slideDeck.addSlide(audioSlide);

                sendMediaMessage(forceSms, "", slideDeck, Collections.emptyList(), expiresIn, subscriptionId, initiating).addListener(new AssertedSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void nothing) {
                        new AsyncTask<Void, Void, Void>() {
                            @Override
                            protected Void doInBackground(Void... params) {
                                PersistentBlobProvider.getInstance(BulkMessageConversationActivity.this).delete(BulkMessageConversationActivity.this, result.first);
                                return null;
                            }
                        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    }
                });
            }

            @Override
            public void onFailure(ExecutionException e) {
                Toast.makeText(BulkMessageConversationActivity.this, R.string.ConversationActivity_unable_to_record_audio, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onRecorderCanceled() {
        Vibrator vibrator = ServiceUtil.getVibrator(this);
        vibrator.vibrate(50);

        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        ListenableFuture<Pair<Uri, Long>> future = audioRecorder.stopRecording();
        future.addListener(new ListenableFuture.Listener<Pair<Uri, Long>>() {
            @Override
            public void onSuccess(final Pair<Uri, Long> result) {
                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... params) {
                        PersistentBlobProvider.getInstance(BulkMessageConversationActivity.this).delete(BulkMessageConversationActivity.this, result.first);
                        return null;
                    }
                }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }

            @Override
            public void onFailure(ExecutionException e) {
            }
        });
    }

    @Override
    public void onEmojiToggle() {
        if (!emojiDrawerStub.resolved()) {
            inputPanel.setEmojiDrawer(emojiDrawerStub.get());
            emojiDrawerStub.get().setEmojiEventListener(inputPanel);
        }

        if (container.getCurrentInput() == emojiDrawerStub.get()) {
            container.showSoftkey(composeText);
        } else {
            container.show(composeText, emojiDrawerStub.get());
        }
    }

    @Override
    public void onMediaSelected(@NonNull Uri uri, String contentType) {
        if (!TextUtils.isEmpty(contentType) && contentType.trim().equals("image/gif")) {
            setMedia(uri, AttachmentManager.MediaType.GIF);
        } else if (MediaUtil.isImageType(contentType)) {
            setMedia(uri, AttachmentManager.MediaType.IMAGE);
        } else if (MediaUtil.isVideoType(contentType)) {
            setMedia(uri, AttachmentManager.MediaType.VIDEO);
        } else if (MediaUtil.isAudioType(contentType)) {
            setMedia(uri, AttachmentManager.MediaType.AUDIO);
        }
    }


    // Listeners
    private class AttachmentTypeListener implements AttachmentTypeSelector.AttachmentClickedListener {
        @Override
        public void onClick(int type) {
            addAttachment(type);
        }

        @Override
        public void onQuickAttachment(Uri uri) {
            Intent intent = new Intent();
            intent.setData(uri);

            onActivityResult(PICK_GALLERY, RESULT_OK, intent);
        }
    }

    private class QuickCameraToggleListener implements OnClickListener {
        @Override
        public void onClick(View v) {
            if (!quickAttachmentDrawer.isShowing()) {
                Permissions.with(BulkMessageConversationActivity.this)
                        .request(Manifest.permission.CAMERA,Manifest.permission.RECORD_AUDIO,Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)
                        .ifNecessary()
                        .withRationaleDialog(getString(R.string.ConversationActivity_to_capture_photos_and_video_allow_signal_access_to_the_camera), R.drawable.ic_photo_camera_white_48dp)
                        .withPermanentDenialDialog(getString(R.string.ConversationActivity_signal_needs_the_camera_permission_to_take_photos_or_video))
                        .onAllGranted(() -> {
                            composeText.clearFocus();
                            container.show(composeText, quickAttachmentDrawer);
                        })
                        .onAnyDenied(() -> Toast.makeText(BulkMessageConversationActivity.this, R.string.ConversationActivity_signal_needs_camera_permissions_to_take_photos_or_video, Toast.LENGTH_LONG).show())
                        .execute();
            } else {
                container.hideAttachedInput(false);
            }
        }
    }

    private class SendButtonListener implements OnClickListener, TextView.OnEditorActionListener {
        @Override
        public void onClick(View v) {

            recipient = recipients.get(0);
            threadId = threads.get(0);
            sendMessage();

        }

        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendButton.performClick();
                return true;
            }
            return false;
        }
    }

    private class AttachButtonListener implements OnClickListener {
        @Override
        public void onClick(View v) {
            handleAddAttachment();
        }
    }

    private class AttachButtonLongClickListener implements View.OnLongClickListener {
        @Override
        public boolean onLongClick(View v) {
            return sendButton.performLongClick();
        }
    }

    private class ComposeKeyPressedListener implements OnKeyListener, OnClickListener, TextWatcher, OnFocusChangeListener {

        int beforeLength;

        @Override
        public boolean onKey(View v, int keyCode, KeyEvent event) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                if (keyCode == KeyEvent.KEYCODE_ENTER) {
                    if (TextSecurePreferences.isEnterSendsEnabled(BulkMessageConversationActivity.this)) {
                        sendButton.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER));
                        sendButton.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER));
                        return true;
                    }
                }
            }
            return false;
        }

        @Override
        public void onClick(View v) {
            container.showSoftkey(composeText);
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            beforeLength = composeText.getTextTrimmed().length();
        }

        @Override
        public void afterTextChanged(Editable s) {
            calculateCharactersRemaining();

            if (composeText.getTextTrimmed().length() == 0 || beforeLength == 0) {
                composeText.postDelayed(BulkMessageConversationActivity.this::updateToggleButtonState, 50);
            }
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void onFocusChange(View v, boolean hasFocus) {
        }
    }

    @Override
    public void setThreadId(long threadId) {
        this.threadId = threadId;
    }

    @Override
    public void handleReplyMessage(MessageRecord messageRecord) {
        Recipient author;

        if (messageRecord.isOutgoing()) {
            author = Recipient.from(this, Address.fromSerialized(TextSecurePreferences.getLocalNumber(this)), true);
        } else {
            author = messageRecord.getIndividualRecipient();
        }

        if (messageRecord.isMms() && !((MmsMessageRecord) messageRecord).getSharedContacts().isEmpty()) {
            Contact contact = ((MmsMessageRecord) messageRecord).getSharedContacts().get(0);
            String displayName = ContactUtil.getDisplayName(contact);
            String body = getString(R.string.ConversationActivity_quoted_contact_message, EmojiStrings.BUST_IN_SILHOUETTE, displayName);
            SlideDeck slideDeck = new SlideDeck();

            if (contact.getAvatarAttachment() != null) {
                slideDeck.addSlide(MediaUtil.getSlideForAttachment(this, contact.getAvatarAttachment()));
            }

            inputPanel.setQuote(GlideApp.with(this),
                    messageRecord.getDateSent(),
                    author,
                    body,
                    slideDeck);
        } else {
            inputPanel.setQuote(GlideApp.with(this),
                    messageRecord.getDateSent(),
                    author,
                    messageRecord.getBody(),
                    messageRecord.isMms() ? ((MmsMessageRecord) messageRecord).getSlideDeck() : new SlideDeck());
        }
    }

    @Override
    public void onAttachmentChanged() {
        handleSecurityChange(isSecureText, isDefaultSms);
        updateToggleButtonState();
    }

    private class UnverifiedDismissedListener implements UnverifiedBannerView.DismissListener {
        @Override
        public void onDismissed(final List<IdentityDatabase.IdentityRecord> unverifiedIdentities) {
            final IdentityDatabase identityDatabase = DatabaseFactory.getIdentityDatabase(BulkMessageConversationActivity.this);

            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    synchronized (SESSION_LOCK) {
                        for (IdentityDatabase.IdentityRecord identityRecord : unverifiedIdentities) {
                            identityDatabase.setVerified(identityRecord.getAddress(),
                                    identityRecord.getIdentityKey(),
                                    IdentityDatabase.VerifiedStatus.DEFAULT);
                        }
                    }

                    return null;
                }

                @Override
                protected void onPostExecute(Void result) {
                    initializeIdentityRecords();
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    private class UnverifiedClickedListener implements UnverifiedBannerView.ClickListener {
        @Override
        public void onClicked(final List<IdentityDatabase.IdentityRecord> unverifiedIdentities) {
            Log.w(TAG, "onClicked: " + unverifiedIdentities.size());
            if (unverifiedIdentities.size() == 1) {
                Intent intent = new Intent(BulkMessageConversationActivity.this, VerifyIdentityActivity.class);
                intent.putExtra(VerifyIdentityActivity.ADDRESS_EXTRA, unverifiedIdentities.get(0).getAddress());
                intent.putExtra(VerifyIdentityActivity.IDENTITY_EXTRA, new IdentityKeyParcelable(unverifiedIdentities.get(0).getIdentityKey()));
                intent.putExtra(VerifyIdentityActivity.VERIFIED_EXTRA, false);

                startActivity(intent);
            } else {
                String[] unverifiedNames = new String[unverifiedIdentities.size()];

                for (int i = 0; i < unverifiedIdentities.size(); i++) {
                    unverifiedNames[i] = Recipient.from(BulkMessageConversationActivity.this, unverifiedIdentities.get(i).getAddress(), false).toShortString();
                }

                AlertDialog.Builder builder = new AlertDialog.Builder(BulkMessageConversationActivity.this);
                builder.setIconAttribute(R.attr.dialog_alert_icon);
                builder.setTitle("No longer verified");
                builder.setItems(unverifiedNames, (dialog, which) -> {
                    Intent intent = new Intent(BulkMessageConversationActivity.this, VerifyIdentityActivity.class);
                    intent.putExtra(VerifyIdentityActivity.ADDRESS_EXTRA, unverifiedIdentities.get(which).getAddress());
                    intent.putExtra(VerifyIdentityActivity.IDENTITY_EXTRA, new IdentityKeyParcelable(unverifiedIdentities.get(which).getIdentityKey()));
                    intent.putExtra(VerifyIdentityActivity.VERIFIED_EXTRA, false);

                    startActivity(intent);
                });
                builder.show();
            }
        }
    }

    private class QuoteRestorationTask extends AsyncTask<Void, Void, MessageRecord> {

        private final String serialized;

        QuoteRestorationTask(@NonNull String serialized) {
            this.serialized = serialized;
        }

        @Override
        protected MessageRecord doInBackground(Void... voids) {
            QuoteId quoteId = QuoteId.deserialize(serialized);

            if (quoteId != null) {
                return DatabaseFactory.getMmsSmsDatabase(getApplicationContext()).getMessageFor(quoteId.getId(), quoteId.getAuthor());
            }

            return null;
        }

        @Override
        protected void onPostExecute(MessageRecord messageRecord) {
            if (messageRecord != null) {
                handleReplyMessage(messageRecord);
            } else {
                Log.e(TAG, "Failed to restore a quote from a draft. No matching message record.");
            }
        }
    }
}
