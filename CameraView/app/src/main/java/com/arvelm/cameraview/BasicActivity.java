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
import android.widget.EditText;
import android.widget.Toast;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by lidh on 16-10-27.
 */

public class BasicActivity extends Activity implements View.OnClickListener{

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

    private static final String SWITCH_CAPTURE = "switch";
    private static final String START_BUTTON_DISPLAY = "START";
    private static final String STOP_BUTTON_DISPLAY = "STOP";

    private  Timer actionTimer;
    private  Boolean timerState = false;

    private  CamCase camCase;

    private  Handler actionHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case 1:
                    savedFilePath();
                    captureStillPicture();
                    final int num = Integer.valueOf(editText.getText().toString()) - msg.what;
                    editText.setText(String.valueOf(num));
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Thread.sleep(1*1000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            switchCamera();
                        }
                    }).start();
                    galleryAddPic();
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

        mTextureView = (TextureView) findViewById(R.id.textureView);
        editText = (EditText) findViewById(R.id.editText);
        switchButton = (Button) findViewById(R.id.switchButton);
        switchButton.setOnClickListener(this);
        rebootCheckBox = (CheckBox)findViewById(R.id.rebootCheckBox);

        mCameraId = CAMERA_BACK;

        getSavedFolder();

        camCase = new CamCase(getApplicationContext());

        restoreByCamCase();

        startActivityFirstAction();

        super.onCreate(savedInstanceState);
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
    }

    @Override
    protected void onResume() {
        super.onPause();
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
    protected void onDestroy() {
        saveByCamCase();
        if (timerState){
            actionTimer.cancel();
            timerState = false;
        }
        closeCamera();

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
                                        null, null);
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

            mCameraCaptureSession.capture(captureBuilder.build(), null, null);
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
        String createFolder = path + "/CameraTest";
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
        Log.d("PATH-----",mFile.getAbsolutePath());
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
                int value = Integer.valueOf(editText.getText().toString().trim());
                if (value > 0) {
                    actionTimer = new Timer();
                    actionTimer.schedule(new LoopTask(SWITCH_CAPTURE), 0, 6 * 1000);
                    timerState = true;
                    editText.setInputType(InputType.TYPE_NULL);
                    rebootCheckBox.setEnabled(false);
                    switchButton.setText(STOP_BUTTON_DISPLAY);
                    switchButton.setBackgroundResource(R.drawable.button_red);
                    checkBoxIsChecked(rebootCheckBox.isChecked());
                }
                break;
            case STOP_BUTTON_DISPLAY:
                if(timerState){
                    actionTimer.cancel();
                    timerState = false;
                }
                editText.setInputType(InputType.TYPE_CLASS_NUMBER);
                rebootCheckBox.setEnabled(true);
                switchButton.setText(START_BUTTON_DISPLAY);
                switchButton.setBackgroundResource(R.drawable.button_green);
                CamCase.setRebootServiceTag(false);
                break;
        }
    }


    private void checkBoxIsChecked(Boolean isChecked){
        if (isChecked){
            Intent intent = new Intent(this,RebootService.class);
            startService(intent);
            CamCase.setRebootServiceTag(true);
            String toastText = "REBOOT AFTER " + RebootService.DELAY_TIME/1000 + "S";
            Toast.makeText(this,toastText,Toast.LENGTH_LONG).show();
        }
    }

    private void saveByCamCase(){
        int times = Integer.valueOf(editText.getText().toString().trim());
        Boolean isReboot = rebootCheckBox.isChecked();

        camCase.saveData(times,isReboot,null);
    }

    private void restoreByCamCase(){
        int times = camCase.getTimes();
        Boolean isReboot = camCase.getIsReboot();

        editText.setText(String.valueOf(times));
        rebootCheckBox.setChecked(isReboot);
    }

    private void startActivityFirstAction() {
        Boolean rebootBool = rebootCheckBox.isChecked();
        int times = Integer.valueOf(editText.getText().toString().trim());
        if (times > 0){
            if (rebootBool){
                buttonClicked();
            }
        }
    }
}
