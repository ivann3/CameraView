package com.arvelm.cameraview;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by lidh on 16-11-1.
 */

public class CamCase {

    private static final int DEFAULT_TIMES = 100;
    private static final Boolean DEFAULT_REBOOT = false;

    private static final String TIMES_KEY = "times";
    private static final String REBOOT_KEY = "isReboot";
    private static final String REBOOTTIME_KEY = "rebootTime";

    private static final String FILE_NAME = "CameraCase";

    //when user stop the timerTask,set the rebootServiceTag=false,the RebootService would not Execute
    private static Boolean rebootServiceTag = false;

    private Context context;
    private int times;
    private Boolean isReboot;
    private int rebootTime;

    private SharedPreferences casePreferences;

    public CamCase(final Context context){
        times = 0;
        isReboot = false;
        rebootTime = 0;
        this.context = context;
        casePreferences =  context.getSharedPreferences(FILE_NAME,0);
    }

    public void saveData(int times,Boolean isReboot,int rebootTime){
        SharedPreferences.Editor editor = casePreferences.edit();
        editor.putInt(TIMES_KEY,times);
        editor.putBoolean(REBOOT_KEY,isReboot);
        editor.putInt(REBOOTTIME_KEY,rebootTime);
        editor.commit();
    }

    public int getTimes(){
        return casePreferences.getInt(TIMES_KEY,DEFAULT_TIMES);
    }

    public Boolean getIsReboot(){
        return casePreferences.getBoolean(REBOOT_KEY,DEFAULT_REBOOT);
    }


    public int getRebootTime(){
        return casePreferences.getInt(REBOOTTIME_KEY,0);
    }

    public static Boolean getRebootServiceTag(){
        return rebootServiceTag;
    }

    public static void setRebootServiceTag(Boolean serviceTag) {
        rebootServiceTag = serviceTag;
    }


}
