package com.application.databind;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;

import com.application.databind.databinding.ActivityMainBinding;
import com.application.databind.model.MainActivityModel;
import com.application.databind.ui.CameraActivity;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;

import static com.application.databind.util.Constants.DEVICE_PREF;
import static com.application.databind.util.Constants.FILE_NAME;
import static com.application.databind.util.FileUtils.getPath;
import static com.application.databind.util.FileUtils.moveFile;
import static com.application.databind.util.ImageUtil.rotateBitmap;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = "MainActivity";
    Context context;
    MainActivityModel viewModel;
    ActivityMainBinding binding;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        context = this;
        viewModel = new ViewModelProvider(this).get(MainActivityModel.class);
        binding.setViewModel(viewModel);

        viewModel.openCamera.observe((LifecycleOwner) context, aBoolean -> {
            Log.d("Selfie", "onSelfieClicked: funtion called value" + aBoolean);
            if (aBoolean) {
                startActivity(new Intent(this, CameraActivity.class));
            }
        });

        viewModel.openGallery.observe((LifecycleOwner) context, aBoolean -> {
            Log.d("Selfie", "onSelfieClicked: funtion called value" + aBoolean);
            if (aBoolean) {
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("image/*");
                startActivityResultLauncher.launch(intent);
            }
        });

    }

    ActivityResultLauncher<Intent> startActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            (ActivityResult result) -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    Uri mediaUri = data.getData();
                    try {
                        InputStream inputStream = getBaseContext().getContentResolver().openInputStream(mediaUri);
                        Bitmap myBitmap = BitmapFactory.decodeStream(inputStream);
                        int orientation = 0;
                        InputStream input = context.getContentResolver().openInputStream(mediaUri);
                        try {
                            if (input != null) {
                                ExifInterface exif = new ExifInterface(input);
                                orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                                input.close();
                                binding.image.setImageBitmap(rotateBitmap(myBitmap,orientation));
                            }else binding.image.setImageBitmap(myBitmap);
                        } catch (Exception e) {
                            binding.image.setImageBitmap(myBitmap);
                        }
                        String fileName = "DataBind" + System.currentTimeMillis() + ".jpg";
                        moveFile(getPath(context, mediaUri), fileName, false);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            });

    @Override
    protected void onResume() {
        super.onResume();
        String fileName = getSharedPreferences(DEVICE_PREF, MODE_PRIVATE).getString(FILE_NAME, null);
        if (fileName != null)
            setImage(fileName);
    }

    private void setImage(String fileName) {
        File file = new File(Environment.getExternalStorageDirectory() + "/" + Environment.DIRECTORY_PICTURES + "/Images", fileName);
        Bitmap myBitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
        if (!file.exists() || null == myBitmap) return;
        Bitmap bOutput;
        Matrix matrix = new Matrix();
        matrix.setRotate(0);
        bOutput = Bitmap.createBitmap(myBitmap, 0, 0, myBitmap.getWidth(), myBitmap.getHeight(), matrix, true);
        binding.image.setImageBitmap(bOutput);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        getSharedPreferences(DEVICE_PREF, MODE_PRIVATE).edit().putString(FILE_NAME, null).apply();
    }
}