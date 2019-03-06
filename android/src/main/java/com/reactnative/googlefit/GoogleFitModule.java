/**
 * Copyright (c) 2017-present, Stanislav Doskalenko - doskalenko.s@gmail.com
 * All rights reserved.
 * <p>
 * This source code is licensed under the MIT-style license found in the
 * LICENSE file in the root directory of this source tree.
 * <p>
 * Based on Asim Malik android source code, copyright (c) 2015
 **/

package com.reactnative.googlefit;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.util.Log;
import android.content.Intent;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.uimanager.IllegalViewOperationException;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.HealthDataTypes;


public class GoogleFitModule extends ReactContextBaseJavaModule implements LifecycleEventListener {

    private static final String REACT_MODULE = "RNGoogleFit";
    private ReactContext mReactContext;
    private GoogleFitManager mGoogleFitManager = null;
    private String GOOGLE_FIT_APP_URI = "com.google.android.apps.fitness";

    public GoogleFitModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.mReactContext = reactContext;
    }


    @Override
    public String getName() {
        return REACT_MODULE;
    }

    @Override
    public void initialize() {
        super.initialize();

        getReactApplicationContext().addLifecycleEventListener(this);
    }

    @Override
    public void onHostResume() {
        super.onHostResume();
        if (mGoogleFitManager != null) {
            mGoogleFitManager.resetAuthInProgress();
        }
    }

    @Override
    public void onHostPause() {
        super.onHostPause();
    }

    @Override
    public void onHostDestroy() {
        // todo disconnect from Google Fit
    }

    @ReactMethod
    public void authorize() {
        final Activity activity = getCurrentActivity();

        if (mGoogleFitManager == null) {
            mGoogleFitManager = new GoogleFitManager(mReactContext, activity);
        }

        if (mGoogleFitManager.isAuthorized()) {
            return;
        }
        mGoogleFitManager.authorize();
    }

    @ReactMethod
    public boolean isAuthorized (final Promise promise) {
        boolean isAuthorized = false;
        if (mGoogleFitManager != null && mGoogleFitManager.isAuthorized() ) {
            isAuthorized = true;
        }
        WritableMap map = Arguments.createMap();
        map.putBoolean("isAuthorized", isAuthorized);
        promise.resolve(map);
    }

    @ReactMethod
    public void disconnect() {
        if (mGoogleFitManager != null) {
            mGoogleFitManager.disconnect();
        }
    }

    @ReactMethod
    public void startFitnessRecording(ReadableArray dataTypes) {
        mGoogleFitManager.getRecordingApi().subscribe(dataTypes);
    }

    @ReactMethod
    public void observeSteps() {
        mGoogleFitManager.getStepCounter().findFitnessDataSources();
    }

    @ReactMethod
    public void getDailySteps(double startDay, double endDay) {
        mGoogleFitManager.getStepHistory().displayLastWeeksData((long) startDay, (long) endDay);
    }

    @ReactMethod
    public void getWeeklySteps(double startDate, double endDate) {
        mGoogleFitManager.getStepHistory().displayLastWeeksData((long) startDate, (long) endDate);
    }

    @ReactMethod
    public void getDailyStepCountSamples(double startDate,
                                         double endDate,
                                         Callback errorCallback,
                                         Callback successCallback) {

        try {
            mGoogleFitManager.getStepHistory().aggregateDataByDate((long) startDate, (long) endDate, successCallback);
        } catch (IllegalViewOperationException e) {
            errorCallback.invoke(e.getMessage());
        }
    }

    @ReactMethod
    public void getActivitySamples(double startDate,
                                   double endDate,
                                   Callback errorCallback,
                                   Callback successCallback) {

        try {
            successCallback.invoke(mGoogleFitManager.getActivityHistory().getActivitySamples((long)startDate, (long)endDate));
        } catch (IllegalViewOperationException e) {
            errorCallback.invoke(e.getMessage());
        }
    }

    @ReactMethod
    public void getDailyDistanceSamples(double startDate,
                                        double endDate,
                                        Callback errorCallback,
                                        Callback successCallback) {

        try {
            successCallback.invoke(mGoogleFitManager.getDistanceHistory().aggregateDataByDate((long) startDate, (long) endDate));
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
            BodyHistory bodyHistory = mGoogleFitManager.getBodyHistory();
            bodyHistory.setDataType(DataType.TYPE_WEIGHT);
            successCallback.invoke(bodyHistory.getHistory((long)startDate, (long)endDate));
        } catch (IllegalViewOperationException e) {
            errorCallback.invoke(e.getMessage());
        }
    }

    @ReactMethod
    public void getHeightSamples(double startDate,
                                 double endDate,
                                 Callback errorCallback,
                                 Callback successCallback) {

        try {
            BodyHistory bodyHistory = mGoogleFitManager.getBodyHistory();
            bodyHistory.setDataType(DataType.TYPE_HEIGHT);
            successCallback.invoke(bodyHistory.getHistory((long)startDate, (long)endDate));
        } catch (IllegalViewOperationException e) {
            errorCallback.invoke(e.getMessage());
        }
    }

    @ReactMethod
    public void saveHeight(ReadableMap heightSample,
                           Callback errorCallback,
                           Callback successCallback) {

        try {
            BodyHistory bodyHistory = mGoogleFitManager.getBodyHistory();
            bodyHistory.setDataType(DataType.TYPE_HEIGHT);
            successCallback.invoke(bodyHistory.save(heightSample));
        } catch (IllegalViewOperationException e) {
            errorCallback.invoke(e.getMessage());
        }
    }


    @ReactMethod
    public void getDailyCalorieSamples(double startDate,
                                       double endDate,
                                       Callback errorCallback,
                                       Callback successCallback) {

        try {
            successCallback.invoke(mGoogleFitManager.getCalorieHistory().aggregateDataByDate((long) startDate, (long) endDate));
        } catch (IllegalViewOperationException e) {
            errorCallback.invoke(e.getMessage());
        }
    }

    @ReactMethod
    public void saveFood(ReadableMap foodSample,
                         Callback errorCallback,
                         Callback successCallback) {
        try {
            successCallback.invoke(mGoogleFitManager.getCalorieHistory().saveFood(foodSample));
        } catch (IllegalViewOperationException e) {
            errorCallback.invoke(e.getMessage());
        }
    }

    @ReactMethod
    public void saveWeight(ReadableMap weightSample,
                           Callback errorCallback,
                           Callback successCallback) {

        try {
            BodyHistory bodyHistory = mGoogleFitManager.getBodyHistory();
            bodyHistory.setDataType(DataType.TYPE_WEIGHT);
            successCallback.invoke(bodyHistory.save(weightSample));
        } catch (IllegalViewOperationException e) {
            errorCallback.invoke(e.getMessage());
        }
    }

    @ReactMethod
    public void deleteWeight(ReadableMap options, Callback errorCallback, Callback successCallback) {
        try {
            BodyHistory bodyHistory = mGoogleFitManager.getBodyHistory();
            bodyHistory.setDataType(DataType.TYPE_WEIGHT);
            successCallback.invoke(bodyHistory.delete(options));
        } catch (IllegalViewOperationException e) {
            errorCallback.invoke(e.getMessage());
        }
    }

    @ReactMethod
    public void deleteHeight(ReadableMap options, Callback errorCallback, Callback successCallback) {
        try {
            BodyHistory bodyHistory = mGoogleFitManager.getBodyHistory();
            bodyHistory.setDataType(DataType.TYPE_HEIGHT);
            successCallback.invoke(bodyHistory.delete(options));
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

    @ReactMethod
    public void openFit() {
        PackageManager pm = mReactContext.getPackageManager();
        try {
            Intent launchIntent = pm.getLaunchIntentForPackage(GOOGLE_FIT_APP_URI);
            mReactContext.startActivity(launchIntent);
        } catch (Exception e) {
            Log.i(REACT_MODULE, e.toString());
        }
    }

    private boolean isAvailableCheck() {
        PackageManager pm = mReactContext.getPackageManager();
        try {
            pm.getPackageInfo(GOOGLE_FIT_APP_URI, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (Exception e) {
            Log.i(REACT_MODULE, e.toString());
            return false;
        }
    }

    private boolean isEnabledCheck() {
        if (mGoogleFitManager == null) {
            mGoogleFitManager = new GoogleFitManager(mReactContext, getCurrentActivity());
        }
        return mGoogleFitManager.isAuthorized();
    }

    @ReactMethod
    public void getBloodPressureSamples(double startDate,
                                 double endDate,
                                 Callback errorCallback,
                                 Callback successCallback) {
        try {
            HeartrateHistory heartrateHistory = mGoogleFitManager.getHeartrateHistory();
            heartrateHistory.setDataType(HealthDataTypes.TYPE_BLOOD_PRESSURE);
            successCallback.invoke(heartrateHistory.getHistory((long)startDate, (long)endDate));
        } catch (IllegalViewOperationException e) {
            errorCallback.invoke(e.getMessage());
        }
    }

    @ReactMethod
    public void getHeartRateSamples(double startDate,
                                 double endDate,
                                 Callback errorCallback,
                                 Callback successCallback) {

        try {
            HeartrateHistory heartrateHistory = mGoogleFitManager.getHeartrateHistory();
            heartrateHistory.setDataType(DataType.TYPE_HEART_RATE_BPM);
            successCallback.invoke(heartrateHistory.getHistory((long)startDate, (long)endDate));
        } catch (IllegalViewOperationException e) {
            errorCallback.invoke(e.getMessage());
        }
    }
}
