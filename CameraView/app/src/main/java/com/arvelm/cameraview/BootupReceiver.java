package com.arvelm.cameraview;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by lidh on 16-10-31.
 */

public class BootupReceiver extends BroadcastReceiver {

    private static final String BOOT_COMPLETED = "android.intent.action.BOOT_COMPLETED";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(BOOT_COMPLETED)) {
            Intent bootupIntent = new Intent(context,BootupService.class);
            context.startService(bootupIntent);
        }
    }
}
