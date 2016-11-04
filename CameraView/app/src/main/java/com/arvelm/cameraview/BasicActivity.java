package com.arvelm.cameraview;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.InputType;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.View;
import android.view.Window;
import android.view.TextureView;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Toast;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import org.apache.log4j.Logger;

/**
 * Created by lidh on 16-10-27.
 */

public class BasicActivity extends Activity implements View.OnClickListener,CompoundButton.OnCheckedChangeListener{

    private TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };

    private HandlerThread mBackgroundThread;

    private Handler mBackgroundHandler;

    private String mCameraId ;

    private static final String CAMERA_FRONT = "1";

    private static final String CAMERA_BACK = "0";

    private TextureView mTextureView;

    private CameraDevice mCameraDevice;

    private CameraCaptureSession mCameraCaptureSession;

    private CaptureRequest.Builder mPreviewRequestBuilder;

    private CaptureRequest mPreviewRequest;

    private final CameraDevice.StateCallback mStateCallBack = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            mCameraDevice = camera;
            createCameraPreviewSession();
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            camera.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            camera.close();
            mCameraDevice = null;
        }
    };

    private ImageReader mImageReader;

    private File mFile;

    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener
            = new ImageReader.OnImageAvailableListener() {

        @Override
        public void onImageAvailable(ImageReader reader) {
            mBackgroundHandler.post(new ImageSaver(reader.acquireNextImage(), mFile));

        }

    };

    private Button switchButton;
    private CheckBox rebootCheckBox;
    private EditText editText;
    private EditText editText2;
    private Button infoButton;

    private static final String SWITCH_CAPTURE = "switch";
    private static final String START_BUTTON_DISPLAY = "START";
    private static final String STOP_BUTTON_DISPLAY = "STOP";

    private  Timer actionTimer;
    private  Boolean timerState = false;

    private  CamCase camCase;

    private  Logger myLog ;

    private long timeTag = 0;
    private String timeOpenCamera = null;
    private String timeCaptureCamera = null;
    private String timeSwitchCamera = null;

    private Boolean grantPermission = false;

    private  Handler actionHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case 1:
                    savedFilePath();
                    captureStillPicture();
                    if (!rebootCheckBox.isChecked()) {
                        final int num = Integer.valueOf(editText.getText().toString()) - msg.what;
                        editText.setText(String.valueOf(num));
                    }
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            galleryAddPic();
                        }
                    }).start();
                    break;
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.basic_layout);
        /*Certain apps need to keep the screen turned on, such as games or movie apps.
         *The best way to do this is to use the FLAG_KEEP_SCREEN_ON in your activity
         *(and only in an activity, never in a service or other app component)
         */
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        showManualInfo(getApplicationContext());

        grantStoragePermission();

        mTextureView = (TextureView) findViewById(R.id.textureView);
        editText = (EditText) findViewById(R.id.editText);
        switchButton = (Button) findViewById(R.id.switchButton);
        switchButton.setOnClickListener(this);
        rebootCheckBox = (CheckBox)findViewById(R.id.rebootCheckBox);
        rebootCheckBox.setOnCheckedChangeListener(this);
        editText2 = (EditText)findViewById(R.id.editText_2);
        infoButton = (Button)findViewById(R.id.infoButton);
        infoButton.setOnClickListener(this);

        mCameraId = CAMERA_BACK;

        getSavedFolder();

        camCase = new CamCase(getApplicationContext());

        if (grantPermission) {
            myLog = Logging.configureLogger(BasicActivity.class);
            myLog.debug("----------------CREATE-----------------");
            myLog.debug("Main_Camera: " + ImgSensorInfo.getImgSensorInfo()[0]);
            myLog.debug("Sub_Camera: " + ImgSensorInfo.getImgSensorInfo()[1]);
        }


        restoreByCamCase();
        startActivityFirstAction();

        super.onCreate(savedInstanceState);
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
    }

    @Override
    protected void onResume() {
        super.onPause();
        if (grantPermission){
            checkLogFile();
        }
        startBackgroundThread();
        reOpenCamera();
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveByCamCase();
        if (timerState){
            actionTimer.cancel();
            timerState = false;
        }
        stopBackgroundThread();
        closeCamera();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (!FirstStartUp.showManualInfo) finish();
    }

    @Override
    protected void onDestroy() {
        saveByCamCase();
        if (timerState){
            actionTimer.cancel();
            timerState = false;
        }
        closeCamera();
        if (grantPermission){
            myLog.debug("---------------DESTORYED---------------");
            myLog.debug("");
        }
        super.onDestroy();
    }

    private void reOpenCamera(){
        if(mTextureView.isAvailable()){
            openCamera();
        }else{
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }
    }

    /**
     * Starts a background thread and its {@link Handler}.
     */
    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    /**
     * Stops the background thread and its {@link Handler}.
     */
    private void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void openCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        setupCameraOutput();

        CameraManager mCameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            mCameraManager.openCamera(mCameraId, mStateCallBack, mBackgroundHandler);
            if (!rebootCheckBox.isChecked()) {
            }
            if (timeTag == 0.0) {
                timeTag = System.currentTimeMillis();
                timeSwitchCamera = null;
//                if (mCameraId.equals(CAMERA_BACK)) myLog.debug("0" + "ms: BACK_Camera: Open");
//                if (mCameraId.equals(CAMERA_FRONT)) myLog.debug("0" + "ms: FRONT_Camera: Open");
            }else{
                timeSwitchCamera = getPeriodTimeTag(timeTag);
//                if (mCameraId.equals(CAMERA_BACK)) myLog.debug(strTime + "ms: BACK_Camera: Open");
//                if (mCameraId.equals(CAMERA_FRONT)) myLog.debug(strTime + "ms: FRONT_Camera: Open");
                }



        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    static class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }

    }
    private void setupCameraOutput(){

        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try{
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(mCameraId);

            StreamConfigurationMap map = characteristics.get(
                            CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

                    // For still image captures, we use the largest available size.
                    Size largest = Collections.max(
                            Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)),
                            new CompareSizesByArea());
                    mImageReader = ImageReader.newInstance(largest.getWidth(), largest.getHeight(),
                            ImageFormat.JPEG, /*maxImages*/2);
                    mImageReader.setOnImageAvailableListener(
                            mOnImageAvailableListener, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    private void createCameraPreviewSession() {
        try {
            SurfaceTexture texture = mTextureView.getSurfaceTexture();

            texture.setDefaultBufferSize(mTextureView.getWidth(), mTextureView.getHeight());

            Surface surface = new Surface(texture);

            mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);

            mPreviewRequestBuilder.addTarget(surface);

            mCameraDevice.createCaptureSession(Arrays.asList(surface,mImageReader.getSurface()),

                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                            // The camera is already closed
                            if (null == mCameraDevice) {
                                return;
                            }
                            // When the session is ready, we start displaying the preview.
                            mCameraCaptureSession = cameraCaptureSession;
                            try {
                                // Auto focus should be continuous for camera preview.
                                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                                // Flash is automatically enabled when necessary.
                                //setAutoFlash(mPreviewRequestBuilder);

                                // Finally, we start displaying the camera preview.
                                mPreviewRequest = mPreviewRequestBuilder.build();
                                mCameraCaptureSession.setRepeatingRequest(mPreviewRequest,
                                        new CameraCaptureSession.CaptureCallback() {
                                            int i = 1;
                                            @Override
                                            public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                                                super.onCaptureCompleted(session, request, result);
                                                while(i > 0){
                                                    String stringTime = getPeriodTimeTag(timeTag);
                                                    timeOpenCamera = stringTime;
//                                                    if (mCameraId.equals(CAMERA_BACK)) myLog.debug(stringTime + "ms: BACK_Camera Open Complete");
//                                                    if (mCameraId.equals(CAMERA_FRONT)) myLog.debug(stringTime + "ms: FRONT_Camera: Open Complete");
                                                    i--;
                                                }
                                            }
                                        }, null);

                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }
                        @Override
                        public void onConfigureFailed(
                                @NonNull CameraCaptureSession cameraCaptureSession) {
                        }
                    }, null
            );
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public void captureStillPicture() {
        try {
            if (null == mCameraDevice) {
                return;
            }
            // This is the CaptureRequest.Builder that we use to take a picture.
            final CaptureRequest.Builder captureBuilder =
                    mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(mImageReader.getSurface());

            // Use the same AE and AF modes as the preview.
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            //setAutoFlash(captureBuilder);

            mCameraCaptureSession.stopRepeating();

            timeTag = System.currentTimeMillis();
            mCameraCaptureSession.capture(captureBuilder.build(), new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            String stringTime = getPeriodTimeTag(timeTag);
                            timeCaptureCamera = stringTime;
//                            if (mCameraId.equals(CAMERA_BACK)) myLog.debug(stringTime + "ms: BACK_Camera: Capture Complete");
//                            if (mCameraId.equals(CAMERA_FRONT)) myLog.debug(stringTime + "ms: FRONT_Camera: Capture Complete");
                        }
                    }).start();

                    if(timerState || (Integer.valueOf(editText.getText().toString().trim())==0)) switchCamera();
                }
            }, null);



        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.switchButton:
                saveByCamCase();
                buttonClicked();
                break;
            case R.id.infoButton:
                if (timerState){
                    buttonClicked();
                }
                Intent intent = new Intent(this,ManualActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                FirstStartUp.showManualInfo = true;
                break;
            default:
                break;
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (isChecked){
            editText2.setVisibility(View.VISIBLE);
        }else{
            editText2.setVisibility(View.INVISIBLE);
        }

    }

    public void switchCamera(){
        closeCamera();
        if (mCameraId.equals(CAMERA_BACK)){
            mCameraId = CAMERA_FRONT;
        }else{
            mCameraId = CAMERA_BACK;
        }
       openCamera();
    }

    private void closeCamera(){
        if (mCameraCaptureSession != null){
            mCameraCaptureSession.close();
            mCameraCaptureSession = null;
        }
        if (mCameraDevice != null){
            mCameraDevice.close();
            mCameraDevice = null;
        }
        timeTag = System.currentTimeMillis();
//        if (mCameraId.equals(CAMERA_BACK)) myLog.debug("BACK_Camera: Close");
//        if (mCameraId.equals(CAMERA_FRONT)) myLog.debug("FRONT_Camera: Close");
        if (timerState){
            if(grantPermission)
                myLog.debug(timeOpenCamera + "      " + timeCaptureCamera + "         " + timeSwitchCamera);
        }

    }

    /**
     * Saves a JPEG {@link Image} into the specified {@link File}.
     */
    private static class ImageSaver implements Runnable {

        /**
         * The JPEG image
         */
        private final Image mImage;
        /**
         * The file we save the image into.
         */
        private final File mFile;

        public ImageSaver(Image image, File file) {
            mImage = image;
            mFile = file;
        }

        @Override
        public void run() {
            ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            FileOutputStream output = null;
            try {
                output = new FileOutputStream(mFile);
                output.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                mImage.close();
                if (null != output) {
                    try {
                        output.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }


    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        Uri contentUri = Uri.fromFile(mFile);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
    }

    private File getSavedFolder(){
        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        String createFolder = path + "/CameraView";
        File file = new File(createFolder);
        if (!file.exists()){
            file.mkdir();
        }
        return file;
    }

    private void savedFilePath(){
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
        String dateStr = format.format(new Date());
        String fileName = "i_" + dateStr + ".jpg";

        mFile = new File(getSavedFolder(),fileName);
        Log.d("mFile:PATH-----",mFile.toString());
    }


    private class LoopTask extends TimerTask {
        private String actionStr;
        public LoopTask(String actionStr){
            this.actionStr = actionStr;
        }

        @Override
        public void run() {
            switch (actionStr){
                case SWITCH_CAPTURE:
                    int num = Integer.valueOf(editText.getText().toString().trim());
                    if (num <= 0) {
                        if(timerState){
                            actionTimer.cancel();
                            timerState = false;
                        }
                    }
                    else{
                        switchAndCapture();
                    }
                    break;
        }
    }

    private void switchAndCapture(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep( 3 * 1000 );
                    Message msg = new Message();
                    msg.what = 1;
                    actionHandler.sendMessage(msg);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }}

    private void buttonClicked(){
        String buttonText = switchButton.getText().toString().trim();
        switch (buttonText){
            case START_BUTTON_DISPLAY:
                String textStr = editText.getText().toString().trim();
                if (!("".equals(textStr)) || textStr.length() > 0 ) {
                    int value = Integer.valueOf(textStr);
                    if (value > 0) {
                        actionTimer = new Timer();
                        actionTimer.schedule(new LoopTask(SWITCH_CAPTURE), 0, 6 * 1000);
                        timerState = true;
                        editText.setInputType(InputType.TYPE_NULL);
                        if (editText2.getVisibility() == View.VISIBLE) {
                            editText2.setInputType(InputType.TYPE_NULL);
                        }
                        rebootCheckBox.setEnabled(false);
                        switchButton.setText(STOP_BUTTON_DISPLAY);
                        switchButton.setBackgroundResource(R.drawable.button_red);
                        checkBoxIsChecked(rebootCheckBox.isChecked());
                        if(mTextureView.isAvailable()){
                            createCameraPreviewSession();
                        }
                        if(grantPermission)
                            myLog.debug("OpenTime" + "    " + "CaptureTime" + "   " + "SwitchTime" + "(ms)");
                    }
                }
                break;
            case STOP_BUTTON_DISPLAY:
                if(timerState){
                    actionTimer.cancel();
                    timerState = false;
                }
                editText.setInputType(InputType.TYPE_CLASS_NUMBER);
                if(editText2.getVisibility() == View.VISIBLE){
                    editText2.setInputType(InputType.TYPE_CLASS_NUMBER);
                }
                rebootCheckBox.setEnabled(true);
                switchButton.setText(START_BUTTON_DISPLAY);
                switchButton.setBackgroundResource(R.drawable.button_green);
                CamCase.setRebootServiceTag(false);
                break;
        }
    }


    private void checkBoxIsChecked(Boolean isChecked){
        if (isChecked){
            String textStr = editText2.getText().toString().trim();
            if (!("".equals(textStr)) || textStr.length() > 0) {
                long delayTime = Long.valueOf(textStr);
                RebootService.DELAY_TIME = delayTime * 60 * 1000;
                Intent intent = new Intent(this, RebootService.class);
                startService(intent);
                CamCase.setRebootServiceTag(true);
                String toastText = "REBOOT AFTER " + textStr + " MINUTES";
                Toast.makeText(this, toastText, Toast.LENGTH_LONG).show();
            }else{
                rebootCheckBox.setChecked(false);
                editText2.setInputType(InputType.TYPE_CLASS_NUMBER);
            }
        }
    }

    private void saveByCamCase(){
        String textStr = editText.getText().toString().trim();
        int times = 0;
        int rebootTime =0 ;
        Boolean isReboot = rebootCheckBox.isChecked();

        if (!("".equals(textStr)) || textStr.length() > 0) {
            times = Integer.valueOf(textStr);
        }

        if (editText2.getVisibility() == View.VISIBLE){
            textStr = editText2.getText().toString().trim();
            if (!("".equals(textStr)) || textStr.length() > 0) {
                rebootTime = Integer.valueOf(textStr);
                camCase.saveData(times,isReboot,rebootTime);
            }
            Log.d("TIMES-----",String.valueOf(times));
            Log.d("REBOOTTIME-----",String.valueOf(rebootTime));
            Log.d("ISREBOOT-----",isReboot.toString());
            return;
        }
        camCase.saveData(times,isReboot,0);
    }

    private void restoreByCamCase(){
        int times = camCase.getTimes();
        Boolean isReboot = camCase.getIsReboot();
        rebootCheckBox.setChecked(isReboot);
        editText.setText(String.valueOf(times));
        if (editText2.getVisibility() == View.VISIBLE){
            int rebootTime = camCase.getRebootTime();
            editText2.setText(String.valueOf(rebootTime));
        }
        Log.d("TIMES-----R",String.valueOf(camCase.getTimes()));
        Log.d("REBOOTTIME-----R",String.valueOf(camCase.getRebootTime()));
        Log.d("ISREBOOT-----R",camCase.getIsReboot().toString());
    }

    private void startActivityFirstAction() {
        Boolean rebootBool = rebootCheckBox.isChecked();
        int times = Integer.valueOf(editText.getText().toString().trim());
        if (rebootBool) {
            final int num = Integer.valueOf(editText.getText().toString()) - 1;
            editText.setText(String.valueOf(num));
        }
        if (times > 0){
            if (rebootBool){
                buttonClicked();
            }
        }
    }

    private String getPeriodTimeTag(Long timeTag){
        long lastTime = System.currentTimeMillis();
        DecimalFormat decimalFormat = new DecimalFormat("#.0");
        return decimalFormat.format(lastTime - timeTag);
    }

    private void checkLogFile(){
        File picturePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        String fileName = "CameraView.log";
        File file = new File(picturePath + File.separator + fileName);
        if (!file.exists()){
            file.mkdir();
        }
    }


    private void showManualInfo(Context context){
        if (FirstStartUp.isFirstStartup(context)){
            Intent intent = new Intent(this,ManualActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            FirstStartUp.firstStartSupFalse(context);
        }
    }

    // Get The Camera Permission from here
     private void grantStoragePermission(){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED){
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)){

            }else{
                ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
            }
        }else{
            grantPermission = true;
        }
    }

    //System CallBack---Detect the result if User "Accept" or "Deny"
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    grantPermission = true;
                    Log.d("STORAGE_PERMISSION","Granted");
                } else {
                    Log.d("STORAGE_PERMISSION","Deny");
                }
                break;
            default:
                break;
        }
    }
}
