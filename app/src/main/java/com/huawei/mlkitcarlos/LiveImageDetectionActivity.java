package com.huawei.mlkitcarlos;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.huawei.hms.mlsdk.MLAnalyzerFactory;
import com.huawei.hms.mlsdk.common.LensEngine;
import com.huawei.hms.mlsdk.common.MLAnalyzer;
import com.huawei.hms.mlsdk.common.MLResultTrailer;
import com.huawei.hms.mlsdk.face.MLFace;
import com.huawei.hms.mlsdk.face.MLFaceAnalyzer;
import com.huawei.hms.mlsdk.face.MLFaceAnalyzerSetting;
import com.huawei.hms.mlsdk.face.MLFaceEmotion;
import com.huawei.hms.mlsdk.face.MLMaxSizeFaceTransactor;
import com.huawei.mlkitcarlos.camera.CameraSourcePreview;
import com.huawei.mlkitcarlos.camera.GraphicOverlay;
import com.huawei.mlkitcarlos.ui.MLFaceGraphic;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class LiveImageDetectionActivity extends AppCompatActivity implements LensEngine.PhotographListener,  CompoundButton.OnCheckedChangeListener {
    private static final String TAG = "LiveImageDetection";

    private static final int CAMERA_PERMISSION_CODE = 2;
    MLFaceAnalyzer analyzer;
    private LensEngine mLensEngine;
    private CameraSourcePreview mPreview;
    private GraphicOverlay mOverlay;
    private int lensType = LensEngine.BACK_LENS;

    private boolean safeToTakePicture;
    private float smilingPossibility = 0.95f;
    private float smilingRate = 0.8f;

    private String DETECT_MODE = "detect_mode";
    private int MOST_PEOPLE = 1002;
    private int NEAREST_PEOPLE = 1003;

    private int detectMode = 0;
    private final int STOP_PREVIEW = 1;
    private final int TAKE_PHOTO = 2;
    private Handler mHandler = obtenerHandler();
    private String storePath = "/storage/emulated/0/DCIM/Camera";
    private Button restart = null;
    private String urlCreado = "url no creado";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_live_image_detection);

        detectMode = NEAREST_PEOPLE;

        this.mPreview = this.findViewById(R.id.preview);
        this.mOverlay = this.findViewById(R.id.overlay);
        restart = findViewById(R.id.restart);
        restart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("mensaje data","mensaje data");
                Log.d("mensaje data","url: " + urlCreado);
            }
        });
        this.createFaceAnalyzer();
        ToggleButton facingSwitch = this.findViewById(R.id.facingSwitch);
        facingSwitch.setOnCheckedChangeListener(this);
        // Checking Camera Permissions
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            this.createLensEngine();
        } else {
            this.requestCameraPermission();
        }
    }

    private void requestCameraPermission() {
        final String[] permissions = new String[]{Manifest.permission.CAMERA};

        if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.CAMERA)) {
            ActivityCompat.requestPermissions(this, permissions, LiveImageDetectionActivity.CAMERA_PERMISSION_CODE);
            return;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.startLensEngine();
    }

    @Override
    protected void onPause() {
        super.onPause();
        this.mPreview.stop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (this.mLensEngine != null) {
            this.mLensEngine.release();
        }
        if (this.analyzer != null) {
            this.analyzer.destroy();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode != LiveImageDetectionActivity.CAMERA_PERMISSION_CODE) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }
        if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            this.createLensEngine();
            return;
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
    }


    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (this.mLensEngine != null) {
            if (isChecked) {
                this.lensType = LensEngine.FRONT_LENS;
            } else {
                this.lensType = LensEngine.BACK_LENS;
            }
        }
        this.mLensEngine.close();
        this.createLensEngine();
        this.startLensEngine();
    }

//    private MLFaceAnalyzer createFaceAnalyzer() {
//        // todo step 2: add on-device face analyzer
//        MLFaceAnalyzerSetting setting = new MLFaceAnalyzerSetting.Factory()
//                .setFeatureType(MLFaceAnalyzerSetting.TYPE_FEATURES)
//                .allowTracing()
//                .create();
//        analyzer = MLAnalyzerFactory.getInstance().getFaceAnalyzer(setting);
//
//        // finish
//        this.analyzer.setTransactor(new FaceAnalyzerTransactor(this.mOverlay));
//        return this.analyzer;
//    }

    private void takePhoto() {
        if(mLensEngine != null) {
            mLensEngine.photograph(null, this);
            Log.d("linea1","linea1");
        }
    }

    @Override
    public void takenPhotograph(byte[] bytes) {
        Log.d("linea2","linea2");
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        saveBitmapToDisk(bitmap);
        Log.d("linea3","linea3");
        mHandler.sendEmptyMessage(STOP_PREVIEW);
        Log.d("linea4","linea4");
    }

    private String saveBitmapToDisk(Bitmap bitmap) {
        Log.d("linea5","linea5");
        File appDir = new File(storePath);
        if (!appDir.exists()) {
            Boolean res = appDir.mkdir();
            if (!res) {
                Log.e(TAG, "saveBitmapToDisk failed");
                return "";
            }
        }

        String fileName = "SmileDemo" + System.currentTimeMillis() + ".jpg";
        File file = new File(appDir, fileName);
        try {
            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();
            Uri uri = Uri.fromFile(file);
            this.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri));
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.d("absoluteFilePath: ", file.getAbsolutePath());
        urlCreado = file.getAbsolutePath();
        return file.getAbsolutePath();

    }



    private Handler obtenerHandler() {
        return new Handler() {
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case TAKE_PHOTO: {
                        takePhoto();
                    }
                    case STOP_PREVIEW: {
                        stopPreview();
                    }
                    default: {}
                }
            }
        };
    }

    private void stopPreview() {
        restart.setVisibility(View.VISIBLE);
        if (mLensEngine != null) {
            mLensEngine.release();
            safeToTakePicture = false;
        }
        if (analyzer != null) {
            try {
                analyzer.stop();
            } catch (IOException e) {
                Log.e(TAG, "Stop failed: " + e.getMessage());
            }
        }
    }

    private MLFaceAnalyzer createFaceAnalyzer() {
        // todo step 2: add on-device face analyzer
        MLFaceAnalyzerSetting setting = new MLFaceAnalyzerSetting.Factory()
                .setFeatureType(MLFaceAnalyzerSetting.TYPE_FEATURES)
                .setKeyPointType(MLFaceAnalyzerSetting.TYPE_UNSUPPORT_KEYPOINTS)
                .setMinFaceProportion(0.1f)
                .setTracingAllowed(true)
                .create();
        analyzer = MLAnalyzerFactory.getInstance().getFaceAnalyzer(setting);

        if(detectMode == NEAREST_PEOPLE){

            MLMaxSizeFaceTransactor transactor = new MLMaxSizeFaceTransactor.Creator(analyzer, obtenerResultado()).create();
            this.analyzer.setTransactor(transactor);

        } else {
            analyzer.setTransactor(new MLAnalyzer.MLTransactor<MLFace>() {
                @Override
                public void destroy() { }

                @Override
                public void transactResult(MLAnalyzer.Result<MLFace> result) {
                    SparseArray<MLFace> faceSparseArray  = result.getAnalyseList();
                    int flag = 0;

                    for (int i = 0; i < faceSparseArray.size() ; i++){
                        MLFaceEmotion emotion = faceSparseArray.valueAt(i).getEmotions();
                        if (emotion.getSmilingProbability() > smilingPossibility) {
                            flag++;
                        }
                    }

                    if (flag > faceSparseArray.size() * smilingRate && safeToTakePicture) {
                        safeToTakePicture = false;
                        mHandler.sendEmptyMessage(TAKE_PHOTO);
                    }

                }
            });
        }

        // finish
        //this.analyzer.setTransactor(new FaceAnalyzerTransactor(this.mOverlay));
        return this.analyzer;
    }

    private MLResultTrailer<MLFace> obtenerResultado() {

        return new MLResultTrailer<MLFace>(){
            @Override
            public void objectCreateCallback(int i, MLFace obj) {
                super.objectCreateCallback(i, obj);

                mOverlay.clear();

                if (obj == null) {
                    return;
                }

                MLFaceGraphic faceGraphic = new MLFaceGraphic(mOverlay, obj);
                mOverlay.add(faceGraphic);

                MLFaceEmotion emotion = obj.getEmotions();

                if (emotion.getSmilingProbability() > smilingPossibility) {
                    safeToTakePicture = false;
                    mHandler.sendEmptyMessage(TAKE_PHOTO);
                }

            }

            @Override
            public void objectUpdateCallback(MLAnalyzer.Result<MLFace> var1, MLFace obj) {
                super.objectUpdateCallback(var1, obj);
                mOverlay.clear();
                if (obj == null) {
                    return;
                }

                MLFaceGraphic faceGraphic = new MLFaceGraphic(mOverlay, obj);
                mOverlay.add(faceGraphic);
                MLFaceEmotion emotion = obj.getEmotions();
                if (emotion.getSmilingProbability() > smilingPossibility && safeToTakePicture) {
                    safeToTakePicture = false;
                    mHandler.sendEmptyMessage(TAKE_PHOTO);
                }
            }

            @Override
            public void lostCallback(MLAnalyzer.Result<MLFace> result) {
                super.lostCallback(result);
                mOverlay.clear();
            }

            @Override
            public void completeCallback() {
                super.completeCallback();
                mOverlay.clear();
            }
        };
    }



    private void createLensEngine() {
        Context context = this.getApplicationContext();
        // todo step 3: add on-device lens engine
        mLensEngine = new LensEngine.Creator(context, analyzer)
                .setLensType(lensType)
                .applyDisplayDimension(1600, 1024)
                .applyFps(25.0f)
                .enableAutomaticFocus(true)
                .create();
        // finish
    }


    private void startLensEngine() {
        restart.setVisibility(View.GONE);
        if (this.mLensEngine != null) {
            try {
                this.mPreview.start(this.mLensEngine, this.mOverlay);
            } catch (IOException e) {
                Log.e(LiveImageDetectionActivity.TAG, "Failed to start lens engine.", e);
                this.mLensEngine.release();
                this.mLensEngine = null;
            }
        }
    }

}