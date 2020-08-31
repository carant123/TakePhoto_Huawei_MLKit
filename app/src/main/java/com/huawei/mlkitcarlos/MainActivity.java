package com.huawei.mlkitcarlos;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.huawei.mlkitcarlos.face.LiveFaceAnalyseActivity;
import com.huawei.mlkitcarlos.utils.Constant;

import java.io.File;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    Button btCamara;
    ImageView ivFoto;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btCamara = findViewById(R.id.button_white);
        btCamara.setOnClickListener(this);
        ivFoto = findViewById(R.id.iv_foto);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.button_white:
                activarCamaraOPermisos();
                break;
            default:
        }
    }

    private void activarCamaraOPermisos() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            activarCamara();
        } else {
            this.requestCameraPermission();
        }
    }

    private void activarCamara() {
        Intent intent = new Intent(MainActivity.this, LiveFaceAnalyseActivity.class);
        intent.putExtra(Constant.DETECT_MODE, Constant.MOST_PEOPLE);
        startActivityForResult(intent, Constant.COD_CAMARA);
    }

    private void requestCameraPermission() {
        final String[] permissions = new String[]{Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE};

        if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA) ||
                !ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE) ||
                !ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

            ActivityCompat.requestPermissions(this, permissions, Constant.COD_PERMISSION_CAMARA);
            return;

        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode != Constant.COD_PERMISSION_CAMARA) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }
        if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED &&
             grantResults[1] == PackageManager.PERMISSION_GRANTED &&
                grantResults[2] == PackageManager.PERMISSION_GRANTED) {
            activarCamara();
            return;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
            if (resultCode == RESULT_OK) {
                switch (requestCode){
                    case Constant.COD_CAMARA:
                        String url = data.getStringExtra("url");
                        File file = new File(url);
                        Log.d("UrlFinal: ","" + url);
                        if(file.exists()){
                            ivFoto.setImageURI(Uri.parse(file.toString()));
                        } else {
                            Log.d("UrlFinal: "," No existe el archivo");
                        }
                        break;
                    default:
                }
            }
    }

}