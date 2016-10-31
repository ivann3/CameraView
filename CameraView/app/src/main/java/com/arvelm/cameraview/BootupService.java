package com.arvelm.cameraview;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;

/**
 * Created by lidh on 16-10-31.
 */

public class BootupService extends Service {

    private long  startTime;
    private static final long DELAY_TIME = 60 * 1000;

    private  Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if(msg.what == 1){
                Intent activityIntent = new Intent(BootupService.this,BasicActivity.class);
                activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(activityIntent);
            }
        }
    };

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        startTime = 0;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        startBasicActivity();
        return super.onStartCommand(intent, flags, startId);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        stopSelf();
    }


    private void startBasicActivity(){
        startTime = System.currentTimeMillis();

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true){
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - startTime > DELAY_TIME){
                        Message msg = new Message();
                        msg.what = 1;
                        mHandler.sendMessage(msg);
                        break;
                    }
                }
            }
        }).start();
    }
}



