package com.arvelm.cameraview;

import android.app.Activity;
import android.os.Bundle;
import android.view.Window;

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
    protected void onStop() {
        super.onStop();

        FirstStartUp.showManualInfo = false;

        finish();
    }
}
