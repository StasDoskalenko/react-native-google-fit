/**
 * Copyright (c) 2017-present, Stanislav Doskalenko - doskalenko.s@gmail.com
 * All rights reserved.
 *
 * This source code is licensed under the MIT-style license found in the
 * LICENSE file in the root directory of this source tree.
 *
 * Based on Asim Malik android source code, copyright (c) 2015
 *
 **/

package com.reactnative.googlefit;

import android.app.Activity;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;


public class GoogleFitModule extends ReactContextBaseJavaModule {

    private static final String REACT_MODULE = "RNGoogleFit";
    private ReactContext mReactContext;
    private GoogleFitManager googleFitManager;
    private Activity activity;

    public GoogleFitModule(ReactApplicationContext reactContext, Activity activity) {
        super(reactContext);

        this.mReactContext = reactContext;
        this.activity = activity;
    }


    @Override
    public String getName() {
        return REACT_MODULE;
    }

    @ReactMethod
    public void authorize() {
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





}
