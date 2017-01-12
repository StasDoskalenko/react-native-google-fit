/**
 * Copyright (c) 2017-present, Stanislav Doskalenko - doskalenko.s@gmail.com
 * All rights reserved.
 *
 * This source code is licensed under the MIT-style license found in the
 * LICENSE file in the root directory of this source tree.
 **/
package com.reactnative.googlefit;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.fitness.Fitness;


public class GoogleFitManager implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        ActivityEventListener {

    private ReactContext mReactContext;
    private GoogleApiClient mApiClient;
    private static final int REQUEST_OAUTH = 1;
    private static final String AUTH_PENDING = "auth_state_pending";
    private boolean authInProgress = false;
    private Activity activity;


    private StepHistory stepHistory;
    private StepCounter stepCounter;
    private StepSensor stepSensor;

    private static final String TAG = "GoogleFitManager";

    public GoogleFitManager(ReactContext reactContext, Activity activity) {

        Log.i(TAG, "Initializing GoogleFitManager" + authInProgress);
        this.mReactContext = reactContext;
        this.activity = activity;


        mReactContext.addActivityEventListener(this);


        this.stepCounter = new StepCounter(mReactContext, this, activity);
        this.stepHistory = new StepHistory(mReactContext, this);

//        this.stepSensor = new StepSensor(mReactContext, activity);


    }

    public GoogleApiClient getGoogleApiClient() {
        return mApiClient;
    }

    public StepCounter getStepCounter() {
        return stepCounter;
    }

    public StepHistory getStepHistory() {
        return stepHistory;
    }


    public void authorize() {

        Log.i(TAG, "Authorizing");
        mApiClient = new GoogleApiClient.Builder(mReactContext.getApplicationContext())
                .addApi(Fitness.SENSORS_API)
                .addApi(Fitness.HISTORY_API)
                .addScope(new Scope(Scopes.FITNESS_ACTIVITY_READ))
                .addScope(new Scope(Scopes.FITNESS_BODY_READ_WRITE))
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        mApiClient.connect();
    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.i(TAG, "Connected");

        //stepCounter.findFitnessDataSources();
        WritableMap map = Arguments.createMap();
        map.putBoolean("authorized", true);
        sendEvent(this.mReactContext, "AuthorizeEvent", map);

    }


    @Override
    public void onConnectionSuspended(int i) {
        Log.i("AuthorizationMgr", "Connection Suspended");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.i(TAG, "Failed AuthorizationMgr:" + connectionResult);
        if (!authInProgress) {
            try {
                authInProgress = true;
                connectionResult.startResolutionForResult(this.activity, REQUEST_OAUTH);
            } catch (IntentSender.SendIntentException e) {

            }
        } else {
            Log.i(TAG, "authInProgress");
        }

    }



    protected void stop() {
        Fitness.SensorsApi.remove(mApiClient, stepCounter)
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        if (status.isSuccess()) {
                            mApiClient.disconnect();
                        }
                    }
                });
    }


    private void sendEvent(ReactContext reactContext,
                           String eventName,
                           @Nullable WritableMap params) {
        reactContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit(eventName, params);
    }


    @Override
    public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
        Log.i(TAG, "onActivityResult" + requestCode);
        if (requestCode == REQUEST_OAUTH) {
            authInProgress = false;
            if (resultCode == Activity.RESULT_OK) {
                if (!mApiClient.isConnecting() && !mApiClient.isConnected()) {
                    mApiClient.connect();
                }
            } else if (resultCode == Activity.RESULT_CANCELED) {
                Log.e(TAG, "RESULT_CANCELED ale co tam");
                mApiClient.connect();
            }
        } else {
            Log.e(TAG, "requestCode NOT request_oauth");
        }

    }

    @Override
    public void onNewIntent(Intent intent) {

    }
}
