package org.btider.dediapp.toolscustom;

import android.Manifest;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import org.btider.dediapp.ConversationActivity;
import org.btider.dediapp.R;
import org.btider.dediapp.database.DatabaseFactory;
import org.btider.dediapp.database.MmsSmsDatabase;
import org.btider.dediapp.database.model.MessageRecord;
import org.btider.dediapp.permissions.Permissions;
import org.btider.dediapp.recipients.Recipient;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;

import static org.btider.dediapp.util.TextSecurePreferences.getProfileName;

public class MessageEmailSender {

    private ConversationActivity activity;
    private boolean isGroupConversation;
    private Recipient recipient;
    private long threadId;
    private String fileName = "";

    public MessageEmailSender(ConversationActivity activity, boolean isGroupConversation, Recipient recipient, long threadId) {
        this.activity = activity;
        this.isGroupConversation = isGroupConversation;
        this.recipient = recipient;
        this.threadId = threadId;
    }

    private void initializePermissions(String title, String text) {
        Permissions.with(activity)
                .request(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)
                .ifNecessary()
                //.withRationaleDialog(activity.getString(R.string.RegistrationActivity_signal_needs_access_to_your_contacts_and_media_in_order_to_connect_with_friends),R.drawable.ic_contacts_white_48dp, R.drawable.ic_folder_white_48dp)
                .onSomeGranted(permissions -> {
                    if (permissions.contains(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                        //initializeBackupDetection();
                        textWrite(title,text);
                    }
                })
                .execute();
    }

    private void textWrite(String title,String text){
        try
        {
            fileName = activity.getString(R.string.chat_send_mail_title,title);
            File myFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/Dedi/sender/"+fileName);
            File folder = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+ "/Dedi/sender");
            if (!folder.exists()) {
                folder.mkdir();
            }

            if(!myFile.exists())
            {
                myFile.createNewFile();
            }

            FileOutputStream fOut = new FileOutputStream(myFile);
            OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
            myOutWriter.append(text);
            myOutWriter.close();
            fOut.close();
            sendMail(title,myFile);
        }
        catch (Exception e)
        {
            Log.e("error", e.getMessage());
            if(e.getMessage().contains("Permission denied")){
                initializePermissions(title,text);
            }
        }
    }

    Timer timer;
    private void fileDelete(File file){

        TimerTask myTimerTask = new TimerTask() {

            @Override
            public void run() {
                boolean deleted = file.delete();
                timer.cancel();
            }

        };
        timer = new Timer();
        timer.schedule(myTimerTask, 2000);


    }

    private void sendMail(String title,File myFile){
        Intent i = new Intent(Intent.ACTION_SENDTO);
        i.setType("message/rfc822");
        i.setData(Uri.parse("mailto:"));
        i.putExtra(Intent.EXTRA_SUBJECT, activity.getString(R.string.chat_send_mail_title, title));
        i.putExtra(Intent.EXTRA_TEXT   , activity.getString(R.string.chat_send_mail_text, title));
        Uri uri = Uri.fromFile(myFile);
        i.putExtra(Intent.EXTRA_STREAM, uri);
        try {
            activity.startActivity(Intent.createChooser(i, activity.getString(R.string.conversation__menu_conversation_mail_send_all)));
            //fileDelete(myFile);
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(activity, activity.getString(R.string.chat_send_mail_problem_app_not_install), Toast.LENGTH_SHORT).show();
        }
    }

    //TODO: Konuşmaları mail ile gönderme işi
    public void fullTextSendMail(){
        String groupName = "";
        if(isGroupConversation) {
            String groupId = recipient.getAddress().toGroupString();
            groupName = DatabaseFactory.getGroupDatabase(activity).getGroup(groupId).get().getTitle();
        }

        String sendTexts = "";
        String title = "";
        LinkedList<String> lineText = new LinkedList<>();
        try (Cursor cursor = DatabaseFactory.getMmsSmsDatabase(activity).getConversation(threadId)) {
            while (cursor != null && cursor.moveToNext()) {
                MmsSmsDatabase smsDatabase = DatabaseFactory.getMmsSmsDatabase(activity);
                MmsSmsDatabase.Reader reader      = smsDatabase.readerFor(cursor);
                MessageRecord rec = reader.getCurrent();
                String sendText = "";
                DateFormat df = new SimpleDateFormat("dd.MM.yyyy HH:mm");

                if(!rec.getBody().equals("")){
                    if(!rec.isGroupAction()){
                        if(rec.isOutgoing()){
                            sendText = sendText + df.format(rec.getDateReceived()) +" - " +getProfileName(activity) +": "+rec.getBody()+"\n";
                        }else{
                            sendText = sendText + df.format(rec.getDateReceived()) +" - "+rec.getRecipient().getName()+": "+rec.getBody()+"\n";
                        }
                        lineText.add(sendText);
                    }
                }
                if(isGroupConversation){
                    if(!groupName.equals(""))
                        title = groupName;
                }else{
                    title = rec.getRecipient().getName();
                }
            }
        }
        LinkedList<String> newlineText = new LinkedList<>();
        for(int a = 0;a<lineText.size();a++){
            newlineText.add(lineText.get((lineText.size()-1)-a));
        }
        for(String st : newlineText){
            sendTexts = sendTexts + st;
        }

        textWrite(title,sendTexts);

    }
}
