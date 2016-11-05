package com.arvelm.cameraview;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Window;
import android.widget.Toast;

/**
 * Created by lidh on 16-11-4.
 */

public class ManualActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.manual_layout);
    }

    @Override
    protected void onResume() {
        super.onResume();
        grantCameraPermission();
    }

    @Override
    protected void onStop() {
        super.onStop();

        FirstStartUp.showManualInfo = false;
        finish();
    }

   // Get The Camera Permission from here
    private void grantCameraPermission(){
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED ){
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.CAMERA)) {

            } else {
                ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.CAMERA},1);
            }
        }
    }

    //Detect the result if User "Accept" or "Deny"
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch(requestCode){
            case 1:
                if (grantResults.length>0 &&grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("CAMERA_PERMISSION","Granted");
                } else {
                    Log.d("CAMERA_PERMISSION","Deny");
                }
                break;
        }
    }
}



