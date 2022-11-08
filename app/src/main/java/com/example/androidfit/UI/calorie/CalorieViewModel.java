package com.example.androidfit.UI.calorie;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class CalorieViewModel extends ViewModel {

    private final MutableLiveData<String> mText;

    public CalorieViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is Calorie Calculator fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }
}
