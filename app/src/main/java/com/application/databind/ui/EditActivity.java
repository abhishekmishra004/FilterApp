package com.application.databind.ui;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Toast;

import com.application.databind.MainActivity;
import com.application.databind.R;
import com.application.databind.databinding.ActivityEditBinding;
import com.application.databind.model.MainActivityModel;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import static com.application.databind.util.Constants.DEVICE_PREF;
import static com.application.databind.util.Constants.FILE_NAME;

public class EditActivity extends AppCompatActivity {

    Context context;
    ActivityEditBinding binding;
    static boolean isSaved = false;
    String fileName = null;
    Bitmap clickedImage,prevImage;

    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_edit);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_edit);
        context = this;
        progressDialog = new ProgressDialog(context);

        binding.ivBack.setOnClickListener(v -> {
            onBackPressed();
        });

        binding.ivSave.setOnClickListener(v -> {
            isSaved = true;
            progressDialog.setMessage("saving image ....");
            progressDialog.show();
            File file = new File(Environment.getExternalStorageDirectory() + "/" + Environment.DIRECTORY_PICTURES + "/Images", fileName);
            try (FileOutputStream out = new FileOutputStream(file)) {
                clickedImage.compress(Bitmap.CompressFormat.JPEG, 100, out);
                new Handler(Looper.getMainLooper()).postDelayed((Runnable) () -> {
                    Toast.makeText(context, "Image saved.", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                    progressDialog.cancel();
                    onBackPressed();
                },1500);
            } catch (IOException e) {
                Toast.makeText(context, "error-"+e.getMessage(), Toast.LENGTH_SHORT).show();
                progressDialog.cancel();
                e.printStackTrace();
                onBackPressed();
            }
        });

        binding.tvRotate.setOnClickListener(v -> {
            rotateImage();
        });

        binding.tvUndo.setOnClickListener(v -> {
            binding.image.setImageBitmap(prevImage);
            clickedImage = prevImage;
        });
    }

    private void rotateImage() {
        prevImage = clickedImage;
        Matrix matrix = new Matrix();
        matrix.setRotate(90);
        clickedImage = Bitmap.createBitmap(clickedImage, 0, 0, clickedImage.getWidth(), clickedImage.getHeight(), matrix, true);
        binding.image.setImageBitmap(clickedImage);
    }

    @Override
    protected void onStart() {
        super.onStart();
        fileName = getSharedPreferences(DEVICE_PREF, MODE_PRIVATE).getString(FILE_NAME, null);
        if (fileName != null){
            setImage(fileName);
            isSaved = false;
        }
    }

    private void setImage(String fileName) {
        File file = new File(Environment.getExternalStorageDirectory() + "/" + Environment.DIRECTORY_PICTURES + "/Images", fileName);
        Bitmap myBitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
        if (!file.exists() || null == myBitmap) return;
        clickedImage = myBitmap;
        prevImage = myBitmap;
        Bitmap bOutput;
        Matrix matrix = new Matrix();
        matrix.setRotate(0);
        bOutput = Bitmap.createBitmap(myBitmap, 0, 0, myBitmap.getWidth(), myBitmap.getHeight(), matrix, true);
        binding.image.setImageBitmap(bOutput);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if(!isSaved && null != fileName){
            File file = new File(Environment.getExternalStorageDirectory() + "/" + Environment.DIRECTORY_PICTURES + "/Images", fileName);
            if(file.exists()) file.delete();
            getSharedPreferences(DEVICE_PREF,MODE_PRIVATE).edit().putString(FILE_NAME,null).apply();
        }
    }
}