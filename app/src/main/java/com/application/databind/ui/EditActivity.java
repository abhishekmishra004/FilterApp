package com.application.databind.ui;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Toast;

import com.application.databind.R;
import com.application.databind.databinding.ActivityEditBinding;
import com.application.databind.model.MainActivityModel;

import java.io.File;

import static com.application.databind.util.Constants.DEVICE_PREF;
import static com.application.databind.util.Constants.FILE_NAME;

public class EditActivity extends AppCompatActivity {

    Context context;
    ActivityEditBinding binding;
    static boolean isSaved = false;
    String fileName = null;
    Bitmap clickedImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_edit);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_edit);
        context = this;

        binding.ivBack.setOnClickListener(v -> {
            onBackPressed();
        });

        binding.ivSave.setOnClickListener(v -> {
            isSaved = true;
            Toast.makeText(context, "Image saved.", Toast.LENGTH_SHORT).show();
            finish();
        });
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
        Bitmap bOutput;
        Matrix matrix = new Matrix();
        matrix.setRotate(0);
        bOutput = Bitmap.createBitmap(myBitmap, 0, 0, myBitmap.getWidth(), myBitmap.getHeight(), matrix, true);
        binding.image.setImageBitmap(bOutput);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if(!isSaved &&  null != fileName){
            File file = new File(Environment.getExternalStorageDirectory() + "/" + Environment.DIRECTORY_PICTURES + "/Images", fileName);
            if(file.exists()) file.delete();
            getSharedPreferences(DEVICE_PREF,MODE_PRIVATE).edit().putString(FILE_NAME,null).apply();
        }
    }
}