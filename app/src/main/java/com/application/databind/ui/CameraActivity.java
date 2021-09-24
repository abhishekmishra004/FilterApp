package com.application.databind.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Surface;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.application.databind.Adapter.FilterAdapter;
import com.application.databind.R;
import com.application.databind.util.YuvToRgbConverter;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import jp.co.cyberagent.android.gpuimage.GPUImageView;
import jp.co.cyberagent.android.gpuimage.filter.GPUImageColorInvertFilter;
import jp.co.cyberagent.android.gpuimage.filter.GPUImageContrastFilter;
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter;
import jp.co.cyberagent.android.gpuimage.filter.GPUImageGammaFilter;
import jp.co.cyberagent.android.gpuimage.filter.GPUImageGrayscaleFilter;
import jp.co.cyberagent.android.gpuimage.filter.GPUImageSketchFilter;

import static com.application.databind.util.Constants.DEVICE_PREF;
import static com.application.databind.util.Constants.FILE_NAME;
import static com.application.databind.util.FileUtils.moveFile;

public class CameraActivity extends AppCompatActivity {

    File outputDirectory;
    ExecutorService cameraExecutor;

    String TAG = "CameraXBasic";
    String FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS";
    int REQUEST_CODE_PERMISSIONS = 10;
    String[] REQUIRED_PERMISSIONS = new String[]{Manifest.permission.CAMERA,Manifest.permission.WRITE_EXTERNAL_STORAGE};

    Button captureCamera;
    GPUImageView gpuImageView;
    Context context;

    ImageCapture imageCapture;
    
    private ProcessCameraProvider cameraProvider;
    private YuvToRgbConverter converter;
    private Bitmap bitmap;
    RecyclerView rvFilter;
    List<String> filters = new ArrayList<>();
    FilterAdapter adapter;

    Bitmap finalImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        context = this;
        rvFilter = findViewById(R.id.rv_filter);
        captureCamera = findViewById(R.id.camera_capture_button);
        gpuImageView = findViewById(R.id.viewFinder);
        converter = new YuvToRgbConverter(this);
        converter.init();
        filters.add("No filter");
        filters.add("GrayScale");
        filters.add("Contrast");
        filters.add("Gamma");
        filters.add("Color Invert");
        adapter = new FilterAdapter(context,filters);
        LinearLayoutManager layoutManager = new LinearLayoutManager(context);
        layoutManager.setOrientation(RecyclerView.HORIZONTAL);
        layoutManager.scrollToPositionWithOffset(2, 20);
        rvFilter.setLayoutManager(layoutManager);
        rvFilter.setAdapter(adapter);


        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }

        captureCamera.setOnClickListener(v -> takePhoto());

        outputDirectory = getOutputDirectory();

        cameraExecutor = Executors.newSingleThreadExecutor();

    }

    private File getOutputDirectory() {
        ContextWrapper cw = new ContextWrapper(context);
        File directory = cw.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if(!directory.exists()) directory.mkdir();
        return directory;
    }



    private boolean allPermissionsGranted() {
        int result = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA);
        int result1 = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE);
        return result == PackageManager.PERMISSION_GRANTED && result1 == PackageManager.PERMISSION_GRANTED;
    }

    private void takePhoto() {
        String fileName = "DataBind"+ System.currentTimeMillis()  + ".jpg";
        gpuImageView.saveToPictures("Images", fileName, uri -> {
            String msg = "Photo capture succeeded ";
            getSharedPreferences(DEVICE_PREF,MODE_PRIVATE).edit().putString(FILE_NAME,fileName).apply();
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this,EditActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        });
    }

    
    private void startCamera() {

        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                imageCapture = new ImageCapture.Builder().build();
                CameraSelector cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;
                ImageAnalysis imageAnalysis = buildImageAnalysis();
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector,imageCapture,imageAnalysis);
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        },ContextCompat.getMainExecutor(this));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }

    @SuppressLint("UnsafeOptInUsageError")
    ImageAnalysis buildImageAnalysis() {
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build();
        imageAnalysis.setAnalyzer(cameraExecutor, image -> {
            Bitmap bitmap = allocateBitmapIfNecessary(image.getWidth(), image.getHeight());
            converter.yuvToRgb(image.getImage(), bitmap);
            image.close();
            gpuImageView.post(new Runnable() {
                @Override
                public void run() {
                    Matrix matrix = new Matrix();
                    matrix.setRotate(90);
                    matrix.preScale(-1, 1);
                    finalImage = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                    gpuImageView.setImage(finalImage);
                }
            });
        });
        return imageAnalysis;

    }

    private Bitmap allocateBitmapIfNecessary(int width, int height)  {
        if (bitmap == null || bitmap.getWidth() != width || bitmap.getHeight() != height) {
            bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        }
        return bitmap;
    }

    public void clickedIndex(int pos){
        GPUImageFilter filter;
        if(pos == 0)filter = new GPUImageFilter();
        else if(pos == 1) filter = new GPUImageGrayscaleFilter();
        else if(pos == 2) filter = new GPUImageContrastFilter(2f);
        else if(pos == 3) filter = new GPUImageGammaFilter(2f);
        else filter = new  GPUImageColorInvertFilter();
        gpuImageView.setFilter(filter);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
    }
}