package org.nsdev.barcodetest;

import android.content.res.AssetFileDescriptor;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.FocusingProcessor;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import java.io.IOException;
import java.lang.reflect.Field;

public class MainActivity extends AppCompatActivity {

    private CameraSource mCameraSource;
    private Camera mCamera;
    private BarcodeDetector mDetector;
    private int mBeepId;
    private SoundPool mPool;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
        try {
            AssetFileDescriptor afd = getAssets().openFd("store-scanner-beep.mp3");
            mBeepId = mPool.load(afd, 1);
        } catch (IOException ex) {

        }

        mDetector = new BarcodeDetector.Builder(this)
                .setBarcodeFormats(Barcode.ITF | Barcode.PDF417)
                .build();

        mDetector.setProcessor(new FocusingProcessor<Barcode>(mDetector, new Tracker<Barcode>() {
            @Override
            public void onNewItem(int id, Barcode item) {
                super.onNewItem(id, item);
                Log.e("NAS", "New barcode: " + item.rawValue);

                mPool.play(mBeepId, 1.0f, 1.0f, 0, 0, 1.0f);
            }
        }) {
            @Override
            public int selectFocus(Detector.Detections<Barcode> detections) {

                SparseArray<Barcode> detectedItems = detections.getDetectedItems();
                for (int i = 0; i < detectedItems.size(); i++) {
                    Log.e("NAS", detectedItems.valueAt(i).rawValue);
                    int key = detections.getDetectedItems().keyAt(i);
                    return key;
                }
                return 0;
            }
        });

        if (!mDetector.isOperational()) {
            Log.e("NAS", "Detector is not operational.");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCameraSource.release();
        mPool.release();
    }

    @Override
    protected void onResume() {
        super.onResume();
        startDetector();
    }

    private void startDetector() {

        mCameraSource = new CameraSource.Builder(this, mDetector)
                .setFacing(CameraSource.CAMERA_FACING_BACK)
                .setRequestedPreviewSize(2048, 2048)
                //.setRequestedFps(7)
                .build();

        SurfaceView surfaceView = (SurfaceView) findViewById(R.id.surface_view);

        surfaceView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCamera.autoFocus(new Camera.AutoFocusCallback() {
                    @Override
                    public void onAutoFocus(boolean b, Camera camera) {
                        Log.e("NAS", "Autofocus Callback");
                    }
                });
            }
        });

        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback2() {
            @Override
            public void surfaceRedrawNeeded(SurfaceHolder surfaceHolder) {

            }

            @Override
            public void surfaceCreated(SurfaceHolder surfaceHolder) {

                try {
                    mCameraSource.start(surfaceHolder);
                    mCameraSource.getCameraFacing();

                    Field field = mCameraSource.getClass().getDeclaredField("zzaUy");
                    field.setAccessible(true);

                    Object value = field.get(mCameraSource);

                    mCamera = (Camera)value;

                    try {
                        Camera.Parameters parameters = mCamera.getParameters();
                        parameters.setAntibanding(Camera.Parameters.ANTIBANDING_AUTO);
                        parameters.setVideoStabilization(true);
                        //parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_MACRO);
                        mCamera.setParameters(parameters);

                        mCamera.autoFocus(new Camera.AutoFocusCallback() {
                            @Override
                            public void onAutoFocus(boolean b, Camera camera) {
                                Log.e("NAS", "Autofocus Callback");
                            }
                        });
                        Log.e("NAS", "Got here.");
                    } catch (Throwable ex) {
                        Log.e("NAS", "Something bad happened: ", ex);
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (NoSuchFieldException e) {
                    e.printStackTrace();
                }

            }

            @Override
            public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

            }
        });

    }
}
