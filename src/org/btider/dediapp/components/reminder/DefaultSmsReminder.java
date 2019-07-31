package org.btider.dediapp.components.reminder;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build.VERSION_CODES;
import android.provider.Telephony;
import android.view.View;
import android.view.View.OnClickListener;

import org.btider.dediapp.R;
import org.btider.dediapp.permissions.Permissions;
import org.btider.dediapp.preferences.SmsMmsPreferenceFragment;
import org.btider.dediapp.util.TextSecurePreferences;
import org.btider.dediapp.util.Util;

public class DefaultSmsReminder extends Reminder {


    @TargetApi(VERSION_CODES.KITKAT)
    public DefaultSmsReminder(final Context context) {
        super(context.getString(R.string.reminder_header_sms_default_title),
                context.getString(R.string.reminder_header_sms_default_text));

        final OnClickListener okListener = new OnClickListener() {
            @Override
            public void onClick(View v) {

//                Permissions.with((Activity) context)
//                        .request(android.Manifest.permission.READ_CALL_LOG, android.Manifest.permission.PROCESS_OUTGOING_CALLS)
//                        .ifNecessary()
//                        .withRationaleDialog(context.getString(R.string.RegistrationActivity_signal_needs_access_to_your_contacts_and_media_in_order_to_connect_with_friends),
//                                R.drawable.ic_contacts_white_48dp, R.drawable.ic_folder_white_48dp)
//                        .onSomeGranted(permissions -> {
//                            if (permissions.contains(Manifest.permission.READ_CALL_LOG)) {
//
//                                TextSecurePreferences.setPromptedDefaultSmsProvider(context, true);
//                                Intent intent = new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
//                                intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, context.getPackageName());
//                                context.startActivity(intent);
//
//                            }
//                        })
//                        .execute();


                TextSecurePreferences.setPromptedDefaultSmsProvider(context, true);
                Intent intent = new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
                intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, context.getPackageName());
                context.startActivity(intent);

            }
        };
        final OnClickListener dismissListener = new OnClickListener() {
            @Override
            public void onClick(View v) {
                TextSecurePreferences.setPromptedDefaultSmsProvider(context, true);
            }
        };
        setOkListener(okListener);
        setDismissListener(dismissListener);
    }

    public static boolean isEligible(Context context) {
        final boolean isDefault = Util.isDefaultSmsProvider(context);
        if (isDefault) {
            TextSecurePreferences.setPromptedDefaultSmsProvider(context, false);
        }

        return !isDefault && !TextSecurePreferences.hasPromptedDefaultSmsProvider(context);
    }
}
