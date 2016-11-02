package com.arvelm.cameraview;

import android.app.Service;
import android.app.admin.SystemUpdatePolicy;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.util.Log;

/**
 * Created by lidh on 16-10-31.
 */

public class RebootService extends Service {
    private PowerManager powerManager;
    private long  startTime;
    public static long DELAY_TIME = 60 * 1000;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        powerManager = (PowerManager)getSystemService(POWER_SERVICE);
        startTime = 0;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        rebootAction();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopSelf();
    }

    private void rebootAction(){
        startTime = System.currentTimeMillis();
        new Thread(new Runnable() {
            @Override
            public void run() {
                while(true){
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - startTime > DELAY_TIME ){
                        if (CamCase.getRebootServiceTag()){
                            powerManager.reboot(null);
                        }else{
                            stopSelf();
                        }
                        break;
                    }
                }
            }
        }).start();
    }
}
