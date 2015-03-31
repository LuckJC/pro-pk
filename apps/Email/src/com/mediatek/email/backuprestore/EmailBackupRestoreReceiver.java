package com.mediatek.email.backuprestore;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class EmailBackupRestoreReceiver extends BroadcastReceiver{

    @Override
    public void onReceive(Context context, Intent intent) {
        EmailBackupRestoreService.processEmailBackupRestoreIntent(context, intent);
    }

}
