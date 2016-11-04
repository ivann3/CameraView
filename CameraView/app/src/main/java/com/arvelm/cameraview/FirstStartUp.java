package com.arvelm.cameraview;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by lidh on 16-11-4.
 */

public class FirstStartUp {
    private static final String IsFirstStartup = "isFirstStartup";

    private static final String FILE_NAME = "StartUp";

    private static Boolean isFirst = true;

    public static boolean showManualInfo = false;

    public static void firstStartSupFalse(final Context context){
        SharedPreferences sharedPreferences = context.getSharedPreferences(FILE_NAME,0);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        isFirst = false;
        editor.putBoolean(IsFirstStartup,isFirst);
        editor.commit();
    }

    public static Boolean isFirstStartup(final Context context){
        SharedPreferences sharedPreferences = context.getSharedPreferences(FILE_NAME,0);
        isFirst = sharedPreferences.getBoolean(IsFirstStartup,true);
        return isFirst;
    }
}
