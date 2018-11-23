package org.btider.dediapp.util;

import android.app.Activity;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import org.btider.dediapp.database.Address;
import org.btider.dediapp.database.DatabaseFactory;
import org.btider.dediapp.recipients.Recipient;
import org.btider.dediapp.GroupCreateActivity;
import org.btider.dediapp.GroupMembersDialog;
import org.btider.dediapp.R;
import org.btider.dediapp.database.Address;
import org.btider.dediapp.database.DatabaseFactory;
import org.btider.dediapp.recipients.Recipient;
import org.whispersystems.libsignal.util.guava.Optional;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class SelectedRecipientsAdapter extends BaseAdapter {
  @NonNull  private Activity                    context;
  @Nullable private OnRecipientDeletedListener onRecipientDeletedListener;
  @NonNull  private List<RecipientWrapper>     recipients;
  private String groupID;

  public SelectedRecipientsAdapter(@NonNull Activity context) {
    this(context, Collections.<Recipient>emptyList(),null);
  }

  public SelectedRecipientsAdapter(@NonNull Activity context,
                                   @NonNull Collection<Recipient> existingRecipients,
                                   String groupID)
  {
    this.context    = context;
    this.recipients = wrapExistingMembers(existingRecipients);
    this.groupID = groupID;
  }

  public void add(@NonNull Recipient recipient, boolean isPush) {
    if (!find(recipient).isPresent()) {
      RecipientWrapper wrapper = new RecipientWrapper(recipient, true, isPush);
      this.recipients.add(0, wrapper);
      notifyDataSetChanged();
    }
  }

  public Optional<RecipientWrapper> find(@NonNull Recipient recipient) {
    RecipientWrapper found = null;
    for (RecipientWrapper wrapper : recipients) {
      if (wrapper.getRecipient().equals(recipient)) found = wrapper;
    }
    return Optional.fromNullable(found);
  }

  public void remove(@NonNull Recipient recipient) {
    Optional<RecipientWrapper> match = find(recipient);
    if (match.isPresent()) {
      recipients.remove(match.get());
      notifyDataSetChanged();
    }
  }

  public Set<Recipient> getRecipients() {
    final Set<Recipient> recipientSet = new HashSet<>(recipients.size());
    for (RecipientWrapper wrapper : recipients) {
      recipientSet.add(wrapper.getRecipient());
    }
    return recipientSet;
  }

  @Override
  public int getCount() {
    return recipients.size();
  }

  public boolean hasNonPushMembers() {
    for (RecipientWrapper wrapper : recipients) {
      if (!wrapper.isPush()) return true;
    }
    return false;
  }

  @Override
  public Object getItem(int position) {
    return recipients.get(position);
  }

  @Override
  public long getItemId(int position) {
    return position;
  }

  @Override
  public View getView(final int position, View v, final ViewGroup parent) {
    if (v == null) {
      v = LayoutInflater.from(context).inflate(R.layout.selected_recipient_list_item, parent, false);
    }

    final RecipientWrapper rw         = (RecipientWrapper)getItem(position);

    final Recipient        p          = rw.getRecipient();

    boolean admin = false;
    if(groupID != null)
      for(Address address : DatabaseFactory.getGroupDatabase(context).getAdmins(groupID)){
        if(address.equals(Address.fromSerialized(TextSecurePreferences.getLocalNumber(context)))){
          admin = true;
        }
      }

    boolean          modifiable = rw.isModifiable();//true;
    if(admin)
      modifiable = true;



    TextView    name   = (TextView)    v.findViewById(R.id.name);
    TextView    phone  = (TextView)    v.findViewById(R.id.phone);
    ImageButton delete = (ImageButton) v.findViewById(R.id.delete);

    name.setText(p.getName());
    phone.setText(p.getAddress().serialize());
    delete.setVisibility(modifiable ? View.VISIBLE : View.GONE);
    delete.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        if(!rw.isModifiable()){

          AlertDialog.Builder builder = new AlertDialog.Builder(context);
          builder.setMessage(R.string.conversation_list_batch__menu_delete_selected);
          builder.setCancelable(true);
          builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
              dialog.dismiss();
            }
          });
          builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
              ((GroupCreateActivity)context).UpdateGroupTask(recipients.get(position).getRecipient());
              if (onRecipientDeletedListener != null) {
                onRecipientDeletedListener.onRecipientDeleted(recipients.get(position).getRecipient());
              }
            }
          });
          builder.show();

          //((GroupCreateActivity)context).UpdateGroupTask(recipients.get(position).getRecipient());
        }else{

          if (onRecipientDeletedListener != null) {
            onRecipientDeletedListener.onRecipientDeleted(recipients.get(position).getRecipient());
          }
        }
      }
    });

    return v;
  }

  private static List<RecipientWrapper> wrapExistingMembers(Collection<Recipient> recipients) {
    final LinkedList<RecipientWrapper> wrapperList = new LinkedList<>();
    for (Recipient recipient : recipients) {
      wrapperList.add(new RecipientWrapper(recipient, false, true));
    }
    return wrapperList;
  }

  public void setOnRecipientDeletedListener(@Nullable OnRecipientDeletedListener listener) {
    onRecipientDeletedListener = listener;
  }

  public interface OnRecipientDeletedListener {
    void onRecipientDeleted(Recipient recipient);
  }

  public static class RecipientWrapper {
    private final Recipient recipient;
    private final boolean   modifiable;
    private final boolean   push;

    public RecipientWrapper(final @NonNull Recipient recipient,
                            final boolean modifiable,
                            final boolean push)
    {
      this.recipient  = recipient;
      this.modifiable = modifiable;
      this.push       = push;
    }

    public @NonNull Recipient getRecipient() {
      return recipient;
    }

    public boolean isModifiable() {
      return modifiable;
    }

    public boolean isPush() {
      return push;
    }
  }


}