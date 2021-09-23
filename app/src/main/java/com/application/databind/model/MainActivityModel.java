package com.application.databind.model;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class MainActivityModel extends ViewModel {


    public MutableLiveData<Boolean> openCamera = new MutableLiveData<>(false);
    public MutableLiveData<Boolean> openGallery = new MutableLiveData<>(false);

    public void onSelfieClicked(){
        Log.d("Selfie", "onSelfieClicked: funtion called");
        openCamera.setValue(true);
    }

    public void onGalleryClicked(){
        openGallery.setValue(true);
    }

}
