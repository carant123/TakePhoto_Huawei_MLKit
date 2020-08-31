package com.huawei.mlkitcarlos.face

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.huawei.hms.mlsdk.MLAnalyzerFactory
import com.huawei.hms.mlsdk.common.LensEngine
import com.huawei.hms.mlsdk.common.LensEngine.PhotographListener
import com.huawei.hms.mlsdk.common.MLAnalyzer
import com.huawei.hms.mlsdk.common.MLAnalyzer.MLTransactor
import com.huawei.hms.mlsdk.common.MLResultTrailer
import com.huawei.hms.mlsdk.face.MLFace
import com.huawei.hms.mlsdk.face.MLFaceAnalyzer
import com.huawei.hms.mlsdk.face.MLFaceAnalyzerSetting
import com.huawei.hms.mlsdk.face.MLMaxSizeFaceTransactor
import com.huawei.mlkitcarlos.R
import com.huawei.mlkitcarlos.camera.LensEnginePreview
import com.huawei.mlkitcarlos.overlay.GraphicOverlay

import com.huawei.mlkitcarlos.overlay.LocalFaceGraphic
import com.huawei.mlkitcarlos.utils.Constant
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


class LiveFaceAnalyseActivity : AppCompatActivity(), View.OnClickListener {

    private val CAMERA_REQUEST_CODE = 101
    private var analyzer: MLFaceAnalyzer? = null
    private var mLensEngine: LensEngine? = null
    private var mPreview: LensEnginePreview? = null
    private var overlay: GraphicOverlay? = null
    private var lensType = LensEngine.BACK_LENS
    private var isFront = false
    private val smilingRate = 0.8f
    private val smilingPossibility = 0.95f
    private var safeToTakePicture = false
    private val storePath = "/storage/emulated/0/DCIM/Camera"
    private var restart: Button? = null
    private var detectMode = 0
    private var mensajeData = "No integrado"

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.setContentView(R.layout.activity_live_face_analyse)
        if (savedInstanceState != null) {
            lensType = savedInstanceState.getInt("lensType")
        }
        mPreview = findViewById(R.id.preview)
        val intent = this.intent
        try {
            detectMode = intent.getIntExtra(Constant.DETECT_MODE, -1)
        } catch (e: RuntimeException) {
            Log.e(TAG, "Get intent value failed:" + e.message)
        }
        createFaceAnalyzer()
        overlay = findViewById(R.id.face_overlay)
        findViewById<View>(R.id.facingSwitch).setOnClickListener(this)
        restart = findViewById(R.id.restart)
        restart?.setOnClickListener {
            val returnIntent = Intent()
            returnIntent.putExtra("url", mensajeData)
            setResult(Activity.RESULT_OK, returnIntent)
            finish()
        }
        createLensEngine()
        setupPermissions()
    }

    override fun onResume() {
        super.onResume()
        startLensEngine()
    }

    private fun setupPermissions() {
        val permission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        )

        if (permission != PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "Permission to record denied")
            makeRequest()
        }
    }

    private fun makeRequest() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE),
            CAMERA_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
        when (requestCode) {
            CAMERA_REQUEST_CODE -> {
                if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Log.i(TAG, "Permission has been denied by user")
                } else {
                    startLensEngine()
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        mPreview!!.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mLensEngine != null) {
            mLensEngine!!.release()
        }
        if (analyzer != null) {
            try {
                analyzer!!.stop()
            } catch (e: IOException) {
                Log.e(TAG, "Stop failed: " + e.message)
            }
        }
    }

    public override fun onSaveInstanceState(savedInstanceState: Bundle) {
        savedInstanceState.putInt("lensType", lensType)
        super.onSaveInstanceState(savedInstanceState)
    }

    override fun onClick(v: View?) {
        isFront = !isFront
        if (isFront) {
            lensType = LensEngine.FRONT_LENS
        } else {
            lensType = LensEngine.BACK_LENS
        }
        if (mLensEngine != null) {
            mLensEngine!!.close()
        }
        startPreview(v)
    }

    private val mHandler: Handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            when (msg.what) {
                STOP_PREVIEW -> stopPreview()
                TAKE_PHOTO -> takePhoto()
                else -> {
                }
            }
        }
    }

    private fun createFaceAnalyzer() {
        // Create a face analyzer. You can create an analyzer using the provided customized face detection parameter
        // MLFaceAnalyzerSetting
        val setting = MLFaceAnalyzerSetting.Factory()
            .setFeatureType(MLFaceAnalyzerSetting.TYPE_FEATURES)
            .setKeyPointType(MLFaceAnalyzerSetting.TYPE_UNSUPPORT_KEYPOINTS)
            .setMinFaceProportion(0.1f)
            .setTracingAllowed(true)
            .create()
        analyzer = MLAnalyzerFactory.getInstance().getFaceAnalyzer(setting)
        if (detectMode == Constant.NEAREST_PEOPLE) {
            val transactor =
                MLMaxSizeFaceTransactor.Creator(analyzer, object : MLResultTrailer<MLFace?>() {
                    override fun objectCreateCallback(
                        itemId: Int,
                        obj: MLFace?
                    ) {
                        overlay!!.clear()
                        if (obj == null) {
                            return
                        }
                        val faceGraphic = LocalFaceGraphic(
                            overlay!!,
                            obj,
                            this@LiveFaceAnalyseActivity
                        )
                        overlay!!.addGraphic(faceGraphic)
                        val emotion = obj.emotions
                        if (emotion.smilingProbability > smilingPossibility) {
                            safeToTakePicture = false
                            mHandler.sendEmptyMessage(TAKE_PHOTO)
                        }
                    }

                    override fun objectUpdateCallback(
                        var1: MLAnalyzer.Result<MLFace?>,
                        obj: MLFace?
                    ) {
                        overlay!!.clear()
                        if (obj == null) {
                            return
                        }
                        val faceGraphic = LocalFaceGraphic(
                            overlay!!,
                            obj,
                            this@LiveFaceAnalyseActivity
                        )
                        overlay!!.addGraphic(faceGraphic)
                        val emotion = obj.emotions
                        if (emotion.smilingProbability > smilingPossibility && safeToTakePicture) {
                            safeToTakePicture = false
                            mHandler.sendEmptyMessage(TAKE_PHOTO)
                        }
                    }

                    override fun lostCallback(result: MLAnalyzer.Result<MLFace?>) {
                        overlay!!.clear()
                    }

                    override fun completeCallback() {
                        overlay!!.clear()
                    }
                }).create()
            analyzer!!.setTransactor(transactor)
        } else {
            analyzer!!.setTransactor(object : MLTransactor<MLFace> {
                override fun destroy() {}
                override fun transactResult(result: MLAnalyzer.Result<MLFace>) {
                    val faceSparseArray = result.analyseList
                    var flag = 0
                    for (i in 0 until faceSparseArray.size()) {
                        val emotion = faceSparseArray.valueAt(i).emotions
                        if (emotion.smilingProbability > smilingPossibility) {
                            flag++
                        }
                    }
                    if (flag > faceSparseArray.size() * smilingRate && safeToTakePicture) {
                        safeToTakePicture = false
                        mHandler.sendEmptyMessage(TAKE_PHOTO)
                    }
                }
            })
        }
    }

    private fun createLensEngine() {
        val context: Context = this.applicationContext
        // Create LensEngine
        mLensEngine = LensEngine.Creator(context, analyzer).setLensType(lensType)
            .applyDisplayDimension(640, 480)
            .applyFps(25.0f)
            .enableAutomaticFocus(true)
            .create()
    }

    private fun startLensEngine() {
        restart!!.setVisibility(View.GONE)
        if (mLensEngine != null) {
            try {
                if (detectMode == Constant.NEAREST_PEOPLE) {
                    mPreview!!.start(mLensEngine, overlay)
                } else {
                    mPreview!!.start(mLensEngine)
                }
                safeToTakePicture = true
            } catch (e: IOException) {
                Log.e(TAG, "Failed to start lens engine.", e)
                mLensEngine!!.release()
                mLensEngine = null
            }
        }
    }

    private fun takePhoto() {
        mLensEngine!!.photograph(null,
            PhotographListener { bytes ->
                mHandler.sendEmptyMessage(STOP_PREVIEW)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                saveBitmapToDisk(bitmap)
            })
    }

    fun startPreview(view: View?) {
        Log.d("mens: ","" + mensajeData);
        createFaceAnalyzer()
        mPreview!!.release()
        createLensEngine()
        startLensEngine()
    }

    private fun stopPreview() {
        restart!!.setVisibility(View.VISIBLE)
        if (mLensEngine != null) {
            mLensEngine!!.release()
            safeToTakePicture = false
        }
        if (analyzer != null) {
            try {
                analyzer!!.stop()
            } catch (e: IOException) {
                Log.e(TAG, "Stop failed: " + e.message)
            }
        }
    }

    private fun saveBitmapToDisk(bitmap: Bitmap): String {
        val appDir = File(storePath)
        if (!appDir.exists()) {
            val res: Boolean = appDir.mkdir()
            if (!res) {
                Log.e(TAG, "saveBitmapToDisk failed")
                return ""
            }
        }
        val fileName = "SmileDemo" + System.currentTimeMillis() + ".jpg"
        val file = File(appDir, fileName)
        try {
            val fos = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
            fos.flush()
            fos.close()
            val uri: Uri = Uri.fromFile(file)
            this.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri))
        } catch (e: IOException) {
            e.printStackTrace()
        }
        Log.d("absoluteFilePath: ", file.absolutePath)
        mensajeData = file.absolutePath
        return file.absolutePath
    }

    companion object {
        private const val TAG = "LiveFaceAnalyseActivity"
        private const val STOP_PREVIEW = 1
        private const val TAKE_PHOTO = 2
    }
}