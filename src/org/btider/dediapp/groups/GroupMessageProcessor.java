package org.btider.dediapp.groups;


import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.protobuf.ByteString;

import org.btider.dediapp.database.Address;
import org.btider.dediapp.database.DatabaseFactory;
import org.btider.dediapp.database.GroupDatabase;
import org.btider.dediapp.database.MessagingDatabase;
import org.btider.dediapp.database.MmsDatabase;
import org.btider.dediapp.database.SmsDatabase;
import org.btider.dediapp.notifications.MessageNotifier;
import org.btider.dediapp.recipients.Recipient;
import org.btider.dediapp.ApplicationContext;
import org.btider.dediapp.database.Address;
import org.btider.dediapp.database.DatabaseFactory;
import org.btider.dediapp.database.GroupDatabase;
import org.btider.dediapp.database.MessagingDatabase.InsertResult;
import org.btider.dediapp.database.MmsDatabase;
import org.btider.dediapp.database.SmsDatabase;
import org.btider.dediapp.jobs.AvatarDownloadJob;
import org.btider.dediapp.jobs.PushGroupUpdateJob;
import org.btider.dediapp.mms.MmsException;
import org.btider.dediapp.mms.OutgoingGroupMediaMessage;
import org.btider.dediapp.notifications.MessageNotifier;
import org.btider.dediapp.recipients.Recipient;
import org.btider.dediapp.sms.IncomingGroupMessage;
import org.btider.dediapp.sms.IncomingTextMessage;
import org.btider.dediapp.toolscustom.StaticFactoryBase;
import org.btider.dediapp.util.Base64;
import org.btider.dediapp.util.GroupUtil;
import org.btider.dediapp.util.TextSecurePreferences;

import org.whispersystems.libsignal.util.guava.Optional;
import org.whispersystems.signalservice.api.messages.SignalServiceAttachment;
import org.whispersystems.signalservice.api.messages.SignalServiceDataMessage;
import org.whispersystems.signalservice.api.messages.SignalServiceEnvelope;
import org.whispersystems.signalservice.api.messages.SignalServiceGroup;
import org.whispersystems.signalservice.api.messages.SignalServiceGroup.Type;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static org.btider.dediapp.database.GroupDatabase.GroupRecord;
import static org.whispersystems.signalservice.internal.push.SignalServiceProtos.AttachmentPointer;
import static org.whispersystems.signalservice.internal.push.SignalServiceProtos.GroupContext;

public class GroupMessageProcessor {

  private static final String TAG = GroupMessageProcessor.class.getSimpleName();

  public static @Nullable Long process(@NonNull Context context,
                                       @NonNull SignalServiceEnvelope envelope,
                                       @NonNull SignalServiceDataMessage message,
                                       boolean outgoing)
  {
    if (!message.getGroupInfo().isPresent() || message.getGroupInfo().get().getGroupId() == null) {
      Log.w(TAG, "Received group message with no id! Ignoring...");
      return null;
    }

    GroupDatabase database = DatabaseFactory.getGroupDatabase(context);
    SignalServiceGroup    group    = message.getGroupInfo().get();
    String                id       = GroupUtil.getEncodedId(group.getGroupId(), false);
    Optional<GroupDatabase.GroupRecord> record   = database.getGroup(id);

    if (record.isPresent() && group.getType() == Type.UPDATE) {
      return handleGroupUpdate(context, envelope, group, record.get(), outgoing);
    } else if (!record.isPresent() && group.getType() == Type.UPDATE) {
      return handleGroupCreate(context, envelope, group, outgoing);
    } else if (record.isPresent() && group.getType() == Type.QUIT) {
      return handleGroupLeave(context, envelope, group, record.get(), outgoing);
    } else if (record.isPresent() && group.getType() == Type.REQUEST_INFO) {
      return handleGroupInfoRequest(context, envelope, group, record.get());
    } else {
      Log.w(TAG, "Received unknown type, ignoring...");
      return null;
    }
  }

  private static @Nullable Long handleGroupCreate(@NonNull Context context,
                                                  @NonNull SignalServiceEnvelope envelope,
                                                  @NonNull SignalServiceGroup group,
                                                  boolean outgoing)
  {
    GroupDatabase        database = DatabaseFactory.getGroupDatabase(context);
    String               id       = GroupUtil.getEncodedId(group.getGroupId(), false);
    GroupContext.Builder builder  = createGroupContext(group);
    builder.setType(GroupContext.Type.UPDATE);

    SignalServiceAttachment avatar  = group.getAvatar().orNull();

    List<Address>           members = group.getMembers().isPresent() ? new LinkedList<Address>() : null;
    if (group.getMembers().isPresent()) {
      for (String member : group.getMembers().get()) {
        members.add(Address.fromExternal(context, member));
      }
    }
    //eklendi.
    List<Address>           admins = group.getAdmins().isPresent() ? new LinkedList<Address>() : null;
    if (group.getAdmins().isPresent()) {
      for (String admin : group.getAdmins().get()) {
        admins.add(Address.fromExternal(context, admin));
      }
    }
    //son eklendi.

    database.create(id, group.getName().orNull(), members, admins, null,
                    avatar != null && avatar.isPointer() ? avatar.asPointer() : null,
                    envelope.getRelay());

    return storeMessage(context, envelope, group, builder.build(), outgoing,"0");
  }

  private static @Nullable Long handleGroupUpdate(@NonNull Context context,
                                                  @NonNull SignalServiceEnvelope envelope,
                                                  @NonNull SignalServiceGroup group,
                                                  @NonNull GroupDatabase.GroupRecord groupRecord,
                                                  boolean outgoing)
  {

    GroupDatabase database = DatabaseFactory.getGroupDatabase(context);
    String        id       = GroupUtil.getEncodedId(group.getGroupId(), false);

    //eklendi.
    List<Address> admins = database.getAdmins(id);
    // only admins can update group.
    if(!admins.contains(Address.fromExternal(context, envelope.getSource()))) {
      return null;
    }

    Set<Address> messageAdmins = new HashSet<>();
    for(String messageAdmin : group.getAdmins().get()) {
      messageAdmins.add(Address.fromExternal(context, messageAdmin));
    }
    // update admins if changed
    if(!new HashSet<>(groupRecord.getAdmins()).equals(messageAdmins)) {
      database.updateAdmins(id, new LinkedList<>(messageAdmins));
    }
    //son eklendi.

    Set<Address> recordMembers = new HashSet<>(groupRecord.getMembers());
    Set<Address> messageMembers = new HashSet<>();




    for (String messageMember : group.getMembers().get()) {
      messageMembers.add(Address.fromExternal(context, messageMember));
    }


    //Gruptan bir kişi eklenir veya çıkarılırsa yeni bir TYPE belirliyoruz
    String status = "0";
    List<Address> removeUsers = new ArrayList<>();
    if(messageMembers.size()<recordMembers.size()){
      for(Address address : recordMembers){
        boolean stats = false;
        for(Address address1 : messageMembers){
          if(address1.toPhoneString().equals(address.toPhoneString())){
              stats = true;
          }
        }
        if(!stats){
          removeUsers.add(address);
          status = StaticFactoryBase.groupUserRemove;
        }
      }
    }else if(messageMembers.size()>recordMembers.size()){
      for(Address address : messageMembers){
        boolean stats = false;
        for(Address address1 : recordMembers){
          if(address1.toPhoneString().equals(address.toPhoneString())){
            stats = true;
          }
        }
        if(!stats){
          removeUsers.add(address);
          status = StaticFactoryBase.groupUserAdd;
        }
      }
    }
    if(removeUsers.size()>0){
      String addr = "";
      for(Address address : removeUsers){
        addr = addr+ address+", ";
      }
      addr = addr.substring(0,addr.length()-2);
      status = status + StaticFactoryBase.baseKey +addr;
    }
    //### SON - Gruptan bir kişi eklenir veya çıkarılırsa yeni bir TYPE belirliyoruz



    database.updateMembers(id, new LinkedList<>(messageMembers));



//    Set<Address> addedMembers = new HashSet<>(messageMembers);
//
//
//    addedMembers.removeAll(recordMembers);
//
//    Set<Address> missingMembers = new HashSet<>(recordMembers);
//    missingMembers.removeAll(messageMembers);

    GroupContext.Builder builder = createGroupContext(group);
    builder.clearMembers();
    builder.setType(GroupContext.Type.UPDATE);
    for (Address addedMember : messageMembers) {
      builder.addMembers(addedMember.serialize());
    }

    //if(remove)
      //builder.setType(GroupContext.Type.REQUEST_INFO_VALUE);

    //BURAYA DİKKAT

//    Set<Address> members =  messageMembers;
//    Set<Address> phoneNumbers = recordMembers;
//
//    Address deleteRec = null;
//
//    for(Address recipient : phoneNumbers){
//      boolean statu = false;
//      for(Address rec : members){
//        if(rec != null)
//          if(recipient != null)
//            if(rec.equals(recipient)){
//              statu = true;
//            }
//      }
//      if(!statu){
//        System.out.println("equals222*****************"+phoneNumbers.size());
//        if(phoneNumbers.size()>0){
//          System.out.println("GELDIIIIIIIIIIIIIIIIIII*********");
//          phoneNumbers.clear();
//          deleteRec = recipient;
//          break;
//        }
//      }
//    }
//
//    phoneNumbers.clear();
//    phoneNumbers.addAll(members);
//
//    if(deleteRec != null){
//      builder.clearMembers();
//      builder.setType(GroupContext.Type.QUIT);
//      builder.add(deleteRec.serialize());
//    }
    //#################








//    if (addedMembers.size() > 0) {
//      Set<Address> unionMembers = new HashSet<>(recordMembers);
//      unionMembers.addAll(messageMembers);
//      database.updateMembers(id, new LinkedList<>(unionMembers));
//
//      builder.clearMembers();
//
//      for (Address addedMember : addedMembers) {
//        builder.addMembers(addedMember.serialize());
//      }
//    } else {
//      builder.clearMembers();
//    }

//    if (missingMembers.size() > 0) {
//      // TODO We should tell added and missing about each-other.
//    }

    if (group.getName().isPresent() || group.getAvatar().isPresent()) {
      SignalServiceAttachment avatar = group.getAvatar().orNull();
      database.update(id, group.getName().orNull(), avatar != null ? avatar.asPointer() : null);
    }

    if (group.getName().isPresent() && group.getName().get().equals(groupRecord.getTitle())) {
      builder.clearName();
    }

    if (!groupRecord.isActive()) database.setActive(id, true);
    Log.w(TAG,TextSecurePreferences.getLocalNumber(context));
    Log.w(TAG, messageMembers.toString());

    if(!messageMembers.contains(Address.fromSerialized(TextSecurePreferences.getLocalNumber(context)))) {
      database.setActive(id, false);
      database.remove(id, Address.fromSerialized(TextSecurePreferences.getLocalNumber(context)));
    }


    return storeMessage(context, envelope, group, builder.build(), outgoing,status);
  }

  private static Long handleGroupInfoRequest(@NonNull Context context,
                                             @NonNull SignalServiceEnvelope envelope,
                                             @NonNull SignalServiceGroup group,
                                             @NonNull GroupDatabase.GroupRecord record)
  {
    if (record.getMembers().contains(Address.fromExternal(context, envelope.getSource()))) {
      ApplicationContext.getInstance(context)
                        .getJobManager()
                        .add(new PushGroupUpdateJob(context, envelope.getSource(), group.getGroupId()));
    }

    return null;
  }

  private static Long handleGroupLeave(@NonNull Context               context,
                                       @NonNull SignalServiceEnvelope envelope,
                                       @NonNull SignalServiceGroup    group,
                                       @NonNull GroupDatabase.GroupRecord record,
                                       boolean  outgoing)
  {
    GroupDatabase database = DatabaseFactory.getGroupDatabase(context);
    String        id       = GroupUtil.getEncodedId(group.getGroupId(), false);
    List<Address> members  = record.getMembers();

    GroupContext.Builder builder = createGroupContext(group);
    builder.setType(GroupContext.Type.QUIT);


    if (members.contains(Address.fromExternal(context, envelope.getSource()))) {
      database.remove(id, Address.fromExternal(context, envelope.getSource()));
      if (outgoing) database.setActive(id, false);

      return storeMessage(context, envelope, group, builder.build(), outgoing,"0");
    }

    return null;
  }


  private static @Nullable Long storeMessage(@NonNull Context context,
                                             @NonNull SignalServiceEnvelope envelope,
                                             @NonNull SignalServiceGroup group,
                                             @NonNull GroupContext storage,
                                             boolean  outgoing,
                                             String status)
  {
    if (group.getAvatar().isPresent()) {
      ApplicationContext.getInstance(context).getJobManager()
                        .add(new AvatarDownloadJob(context, group.getGroupId()));
    }

    try {
      if (outgoing) {
        MmsDatabase mmsDatabase     = DatabaseFactory.getMmsDatabase(context);
        Address                   addres          = Address.fromExternal(context, GroupUtil.getEncodedId(group.getGroupId(), false));
        Recipient recipient       = Recipient.from(context, addres, false);
        OutgoingGroupMediaMessage outgoingMessage = new OutgoingGroupMediaMessage(recipient, storage, null, envelope.getTimestamp(), 0, null, Collections.emptyList());
        long                      threadId        = DatabaseFactory.getThreadDatabase(context).getThreadIdFor(recipient);
        long                      messageId       = mmsDatabase.insertMessageOutbox(outgoingMessage, threadId, false, null);

        mmsDatabase.markAsSent(messageId, true);

        return threadId;
      } else {
        SmsDatabase smsDatabase  = DatabaseFactory.getSmsDatabase(context);
        String               body         = Base64.encodeBytes(storage.toByteArray());
        IncomingTextMessage  incoming     = new IncomingTextMessage(Address.fromExternal(context, envelope.getSource()), envelope.getSourceDevice(), envelope.getTimestamp(), body, Optional.of(group), 0);
        IncomingGroupMessage groupMessage = new IncomingGroupMessage(incoming, storage, body);

        Optional<MessagingDatabase.InsertResult> insertResult = smsDatabase.insertMessageInboxGroup(groupMessage,status);

        if (insertResult.isPresent()) {
          MessageNotifier.updateNotification(context, insertResult.get().getThreadId());
          return insertResult.get().getThreadId();
        } else {
          return null;
        }
      }
    } catch (MmsException e) {
      Log.w(TAG, e);
    }

    return null;
  }

  private static GroupContext.Builder createGroupContext(SignalServiceGroup group) {
    GroupContext.Builder builder = GroupContext.newBuilder();
    builder.setId(ByteString.copyFrom(group.getGroupId()));

    if (group.getAvatar().isPresent() && group.getAvatar().get().isPointer()) {
      builder.setAvatar(AttachmentPointer.newBuilder()
                                         .setId(group.getAvatar().get().asPointer().getId())
                                         .setKey(ByteString.copyFrom(group.getAvatar().get().asPointer().getKey()))
                                         .setContentType(group.getAvatar().get().getContentType()));
    }

    if (group.getName().isPresent()) {
      builder.setName(group.getName().get());
    }

    if (group.getMembers().isPresent()) {
      builder.addAllMembers(group.getMembers().get());
    }

    return builder;
  }

}
