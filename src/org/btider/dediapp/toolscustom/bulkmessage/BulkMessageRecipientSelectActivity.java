/*
 * Copyright (C) 2014 Open Whisper Systems
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

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.soundcloud.android.crop.Crop;

import org.btider.dediapp.ContactSelectionListFragment;
import org.btider.dediapp.ConversationActivity;
import org.btider.dediapp.PassphraseRequiredActionBarActivity;
import org.btider.dediapp.PushContactSelectionActivity;
import org.btider.dediapp.R;
import org.btider.dediapp.components.PushRecipientsPanel;
import org.btider.dediapp.components.PushRecipientsPanel.RecipientsPanelChangedListener;
import org.btider.dediapp.contacts.ContactsCursorLoader.DisplayMode;
import org.btider.dediapp.contacts.RecipientsEditor;
import org.btider.dediapp.contacts.avatars.ContactColors;
import org.btider.dediapp.contacts.avatars.ResourceContactPhoto;
import org.btider.dediapp.database.Address;
import org.btider.dediapp.database.DatabaseFactory;
import org.btider.dediapp.database.GroupDatabase;
import org.btider.dediapp.database.GroupDatabase.GroupRecord;
import org.btider.dediapp.database.RecipientDatabase;
import org.btider.dediapp.database.ThreadDatabase;
import org.btider.dediapp.groups.GroupManager;
import org.btider.dediapp.groups.GroupManager.GroupActionResult;
import org.btider.dediapp.mms.GlideApp;
import org.btider.dediapp.recipients.Recipient;
import org.btider.dediapp.util.BitmapUtil;
import org.btider.dediapp.util.DynamicLanguage;
import org.btider.dediapp.util.DynamicTheme;
import org.btider.dediapp.util.SelectedRecipientsAdapter;
import org.btider.dediapp.util.SelectedRecipientsAdapter.OnRecipientDeletedListener;
import org.btider.dediapp.util.TextSecurePreferences;
import org.btider.dediapp.util.ViewUtil;
import org.btider.dediapp.util.task.ProgressDialogAsyncTask;
import org.whispersystems.libsignal.util.guava.Optional;
import org.whispersystems.signalservice.api.util.InvalidNumberException;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Activity to create and update groups
 *
 * @author Jake McGinty
 */
public class BulkMessageRecipientSelectActivity extends PassphraseRequiredActionBarActivity
                                 implements OnRecipientDeletedListener,
        RecipientsPanelChangedListener
{

  private final static String TAG = BulkMessageRecipientSelectActivity.class.getSimpleName();

  public static final String GROUP_ADDRESS_EXTRA = "group_recipient";
  public static final String GROUP_THREAD_EXTRA  = "group_thread";

  private final DynamicTheme dynamicTheme    = new DynamicTheme();
  private final DynamicLanguage dynamicLanguage = new DynamicLanguage();

  private static final int PICK_CONTACT = 1;

  private ListView     lv;
  private TextView     creatingText;

  @Override
  protected void onPreCreate() {
    dynamicTheme.onCreate(this);
    dynamicLanguage.onCreate(this);
  }

  @Override
  protected void onCreate(Bundle state, boolean ready) {
    setContentView(R.layout.bulk_list_create_activity);
    //noinspection ConstantConditions
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    initializeResources();
  }

  @Override
  public void onResume() {
    super.onResume();
    dynamicTheme.onResume(this);
    dynamicLanguage.onResume(this);
    updateViewState();
  }

  private boolean isSignalGroup() {
    return TextSecurePreferences.isPushRegistered(this) && !getAdapter().hasNonPushMembers();
  }

  private void disableSignalGroupViews(int reasonResId) {
    View pushDisabled = findViewById(R.id.push_disabled);
    pushDisabled.setVisibility(View.VISIBLE);
    ((TextView) findViewById(R.id.push_disabled_reason)).setText(reasonResId);
  }

  private void enableSignalGroupViews() {
    findViewById(R.id.push_disabled).setVisibility(View.GONE);
    findViewById(R.id.relative_layout_head).setVisibility(View.GONE);
  }

  @SuppressWarnings("ConstantConditions")
  private void updateViewState() {
      enableSignalGroupViews();
      getSupportActionBar().setTitle(R.string.bulk_message_title);
  }

  private static boolean isActiveInDirectory(Recipient recipient) {
    return recipient.resolve().getRegistered() == RecipientDatabase.RegisteredState.REGISTERED;
  }

  private void addSelectedContacts(@NonNull Recipient... recipients) {
    new AddMembersTask(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, recipients);
  }

  private void addSelectedContacts(@NonNull Collection<Recipient> recipients) {
    addSelectedContacts(recipients.toArray(new Recipient[recipients.size()]));
  }

  private void initializeResources() {
    RecipientsEditor recipientsEditor = ViewUtil.findById(this, R.id.recipients_text);
    PushRecipientsPanel recipientsPanel  = ViewUtil.findById(this, R.id.recipients);
    lv           = ViewUtil.findById(this, R.id.selected_contacts_list);
    creatingText = ViewUtil.findById(this, R.id.creating_group_text);
    SelectedRecipientsAdapter adapter = new SelectedRecipientsAdapter(this);
    adapter.setOnRecipientDeletedListener(this);
    lv.setAdapter(adapter);
    recipientsEditor.setHint(R.string.bulk_message_recipent_add);
    recipientsPanel.setPanelChangeListener(this);
    findViewById(R.id.contacts_button).setOnClickListener(new AddRecipientButtonListener());

  }


  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    MenuInflater inflater = this.getMenuInflater();
    menu.clear();

    inflater.inflate(R.menu.group_create, menu);
    super.onPrepareOptionsMenu(menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    super.onOptionsItemSelected(item);
    switch (item.getItemId()) {
      case android.R.id.home:
        finish();
        return true;
      case R.id.menu_create_group:
        handleGroupCreate();
        return true;
    }

    return false;
  }

  @Override
  public void onRecipientDeleted(Recipient recipient) {
    getAdapter().remove(recipient);
    updateViewState();
  }

  @Override
  public void onRecipientsPanelUpdate(List<Recipient> recipients) {
    if (recipients != null && !recipients.isEmpty()) addSelectedContacts(recipients);
  }

  private void handleGroupCreate() {
    if (getAdapter().getCount() < 2) {
      Toast.makeText(getApplicationContext(), R.string.bulk_message_send_count, Toast.LENGTH_SHORT).show();
      return;
    }
    ArrayList<Recipient> recipients = new ArrayList<>();
    ArrayList<Long> threads = new ArrayList<>();
    long      threadId = 0l;
    for(int i=0;i<getAdapter().getRecipients().size();i++){
      Recipient recipient = (Recipient) getAdapter().getRecipients().toArray()[i];
      threadId       = DatabaseFactory.getThreadDatabase(this).getThreadIdFor(recipient, ThreadDatabase.DistributionTypes.DEFAULT);
      recipients.add(recipient);
      threads.add(threadId);
    }
    this.handleOpenConversation(threadId, recipients,threads);
  }


  private void handleOpenConversation(long threadId, ArrayList<Recipient> recipient, ArrayList<Long> threads) {
    Intent intent = new Intent(this, BulkMessageConversationActivity.class);
    intent.putExtra(ConversationActivity.THREAD_ID_EXTRA, -1000);
    intent.putExtra(ConversationActivity.DISTRIBUTION_TYPE_EXTRA, ThreadDatabase.DistributionTypes.DEFAULT);
    intent.putExtra(ConversationActivity.ADDRESS_EXTRA, recipient.get(0).getAddress());
    intent.putExtra("recipient_size", recipient.size());
    int i = 0;
    for(Recipient recipient1 : recipient) {
      intent.putExtra("recipient_"+i, recipient1.getAddress());
      intent.putExtra("threads_"+i, threads.get(i));
      i++;
    }
    startActivity(intent);
    finish();
  }

  private SelectedRecipientsAdapter getAdapter() {
    return (SelectedRecipientsAdapter)lv.getAdapter();
  }


  @Override
  public void onActivityResult(int reqCode, int resultCode, final Intent data) {
    super.onActivityResult(reqCode, resultCode, data);
    Uri outputFile = Uri.fromFile(new File(getCacheDir(), "cropped"));

    if (data == null || resultCode != Activity.RESULT_OK)
      return;

    switch (reqCode) {
      case PICK_CONTACT:
        List<String> selected = data.getStringArrayListExtra("contacts");

        for (String contact : selected) {
          Address   address   = Address.fromExternal(this, contact);
          Recipient recipient = Recipient.from(this, address, false);

          addSelectedContacts(recipient);
        }
        break;

      case Crop.REQUEST_PICK:
        new Crop(data.getData()).output(outputFile).asSquare().start(this);
        break;
    }
  }

  private class AddRecipientButtonListener implements View.OnClickListener {
    @Override
    public void onClick(View v) {
      Intent intent = new Intent(BulkMessageRecipientSelectActivity.this, PushContactSelectionActivity.class);
      intent.putExtra(ContactSelectionListFragment.DISPLAY_MODE, DisplayMode.FLAG_PUSH);
      startActivityForResult(intent, PICK_CONTACT);
    }
  }

  private static class AddMembersTask extends AsyncTask<Recipient,Void,List<AddMembersTask.Result>> {
    static class Result {
      Optional<Recipient> recipient;
      boolean             isPush;
      String              reason;

      public Result(@Nullable Recipient recipient, boolean isPush, @Nullable String reason) {
        this.recipient = Optional.fromNullable(recipient);
        this.isPush    = isPush;
        this.reason    = reason;
      }
    }

    private BulkMessageRecipientSelectActivity activity;
    private boolean             failIfNotPush;

    public AddMembersTask(@NonNull BulkMessageRecipientSelectActivity activity) {
      this.activity      = activity;
    }

    @Override
    protected List<Result> doInBackground(Recipient... recipients) {
      final List<Result> results = new LinkedList<>();

      for (Recipient recipient : recipients) {
        boolean isPush = isActiveInDirectory(recipient);

        if (failIfNotPush && !isPush) {
          //results.add(new Result(null, false, activity.getString(R.string.GroupCreateActivity_cannot_add_non_push_to_existing_group,recipient.toShortString())));
        } else if (TextUtils.equals(TextSecurePreferences.getLocalNumber(activity), recipient.getAddress().serialize())) {
          //results.add(new Result(null, false, activity.getString(R.string.GroupCreateActivity_youre_already_in_the_group)));
        } else {
          results.add(new Result(recipient, isPush, null));
        }
      }
      return results;
    }

    @Override
    protected void onPostExecute(List<Result> results) {
      if (activity.isFinishing()) return;

      for (Result result : results) {
        if (result.recipient.isPresent()) {
          activity.getAdapter().add(result.recipient.get(), result.isPush);
        } else {
          Toast.makeText(activity, result.reason, Toast.LENGTH_SHORT).show();
        }
      }
      activity.updateViewState();
    }
  }


}
