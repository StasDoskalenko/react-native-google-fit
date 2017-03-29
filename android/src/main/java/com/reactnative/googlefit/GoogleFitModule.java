/**
 * Copyright (c) 2017-present, Stanislav Doskalenko - doskalenko.s@gmail.com
 * All rights reserved.
 *
 * This source code is licensed under the MIT-style license found in the
 * LICENSE file in the root directory of this source tree.
 *
 * Based on Asim Malik android source code, copyright (c) 2015
 **/

package com.reactnative.googlefit;

import android.app.Activity;
import android.content.pm.PackageManager;

import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.uimanager.IllegalViewOperationException;


public class GoogleFitModule extends ReactContextBaseJavaModule {

    private static final String REACT_MODULE = "RNGoogleFit";
    private ReactContext mReactContext;
    private GoogleFitManager googleFitManager = null;
    private String GOOGLE_FIT_APP_URI = "com.google.android.apps.fitness";

    public GoogleFitModule(ReactApplicationContext reactContext) {
        super(reactContext);

        this.mReactContext = reactContext;
    }


    @Override
    public String getName() {
        return REACT_MODULE;
    }

    @ReactMethod
    public void authorize() {
        final Activity activity = getCurrentActivity();

        if(googleFitManager == null) {
            googleFitManager = new GoogleFitManager(mReactContext, activity);
        }

        googleFitManager.authorize();
    }

    @ReactMethod
    public void observeSteps() {
        googleFitManager.getStepCounter().findFitnessDataSources();
    }

    @ReactMethod
    public void getDailySteps(double startDay, double endDay) {
        googleFitManager.getStepHistory().displayLastWeeksData((long)startDay, (long)endDay);
    }

    @ReactMethod
    public void getWeeklySteps(double startDate, double endDate) {
        googleFitManager.getStepHistory().displayLastWeeksData((long)startDate, (long)endDate);
    }

    @ReactMethod
    public void getDailyStepCountSamples(double startDate,
                                 double endDate,
                                 Callback errorCallback,
                                 Callback successCallback) {

        try {
            successCallback.invoke(googleFitManager.getStepHistory().aggregateDataByDate((long)startDate, (long)endDate));
        } catch (IllegalViewOperationException e) {
            errorCallback.invoke(e.getMessage());
        }
    }

    @ReactMethod
    public void getWeightSamples(double startDate,
                                 double endDate,
                                 Callback errorCallback,
                                 Callback successCallback) {

        try {
            successCallback.invoke(googleFitManager.getWeightsHistory().displayLastWeeksData((long)startDate, (long)endDate));
        } catch (IllegalViewOperationException e) {
            errorCallback.invoke(e.getMessage());
        }
    }

    @ReactMethod
    public void saveWeight(ReadableMap weightSample,
                           Callback errorCallback,
                           Callback successCallback) {

        try {
            successCallback.invoke(googleFitManager.getWeightsHistory().saveWeight(weightSample));
        } catch (IllegalViewOperationException e) {
            errorCallback.invoke(e.getMessage());
        }
    }

    @ReactMethod
    public void deleteWeight(ReadableMap weightSample, Callback errorCallback, Callback successCallback) {
        try {
            successCallback.invoke(googleFitManager.getWeightsHistory().deleteWeight(weightSample));
        } catch (IllegalViewOperationException e) {
            errorCallback.invoke(e.getMessage());
        }
    }

    @ReactMethod
    public void isAvailable(Callback errorCallback, Callback successCallback) { // true if GoogleFit installed
        try {
            successCallback.invoke(isAvailableCheck());
        } catch (IllegalViewOperationException e) {
            errorCallback.invoke(e.getMessage());
        }
    }

    @ReactMethod
    public void isEnabled(Callback errorCallback, Callback successCallback) { // true if permission granted
        try {
            successCallback.invoke(isEnabledCheck());
        } catch (IllegalViewOperationException e) {
            errorCallback.invoke(e.getMessage());
        }
    }

    private boolean isAvailableCheck() {
        PackageManager pm = mReactContext.getPackageManager();
        try {
            pm.getPackageInfo(GOOGLE_FIT_APP_URI, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private boolean isEnabledCheck() {
        if (googleFitManager == null) {
            googleFitManager = new GoogleFitManager(mReactContext, getCurrentActivity());
        }
        return googleFitManager.isAuthorize();
    }

}
