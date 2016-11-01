package com.arvelm.cameraview;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by lidh on 16-11-1.
 */

public class CamCase {

    private static final int DEFAULT_TIMES = 100;
    private static final Boolean DEFAULT_REBOOT = false;
    private static final String DEFAULT_TEXT = "START";

    private static final String TIMES_KEY = "times";
    private static final String REBOOT_KEY = "isReboot";
    private static final String TEXT_KEY = "buttonText";

    private static final String FILE_NAME = "CameraCase";

    //when user stop the timerTask,set the rebootServiceTag=false,the RebootService would not Execute
    private static Boolean rebootServiceTag = false;


    private Context context;
    public int times;
    public Boolean isReboot;
    public String buttonText;

    private SharedPreferences casePreferences;

    public CamCase(final Context context){
        times = 0;
        isReboot = false;
        buttonText = DEFAULT_TEXT;
        this.context = context;
        casePreferences =  context.getSharedPreferences(FILE_NAME,0);
    }

    public void saveData(int times,Boolean isReboot,String buttonText){
        SharedPreferences.Editor editor = casePreferences.edit();
        editor.putInt(TIMES_KEY,times);
        editor.putBoolean(REBOOT_KEY,isReboot);
        editor.putString(TEXT_KEY,buttonText);
        editor.commit();
    }

    public int getTimes(){
        return casePreferences.getInt(TIMES_KEY,DEFAULT_TIMES);
    }

    public Boolean getIsReboot(){
        return casePreferences.getBoolean(REBOOT_KEY,DEFAULT_REBOOT);
    }

    public String getButtonText() {
        return casePreferences.getString(TEXT_KEY,DEFAULT_TEXT);
    }

    public static Boolean getRebootServieTag(){
        return rebootServiceTag;
    }

    public static void setRebootServiceTag(Boolean serviceTag) {
        rebootServiceTag = serviceTag;
    }
}
