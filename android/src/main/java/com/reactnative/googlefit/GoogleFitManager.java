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
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.DialogInterface;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.Log;
import java.util.ArrayList;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.ErrorDialogFragment;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.auth.api.signin.*;


public class GoogleFitManager implements
        ActivityEventListener {

    private ReactContext mReactContext;
    private GoogleApiClient mApiClient;
    private static final int REQUEST_OAUTH = 1001;
    private static final String AUTH_PENDING = "auth_state_pending";
    private static boolean mAuthInProgress = false;
    private Activity mActivity;

    private DistanceHistory distanceHistory;
    private StepHistory stepHistory;
    private BodyHistory bodyHistory;
    private HeartrateHistory heartrateHistory;
    private CalorieHistory calorieHistory;
    private NutritionHistory nutritionHistory;
    private StepCounter mStepCounter;
    private StepSensor stepSensor;
    private RecordingApi recordingApi;
    private ActivityHistory activityHistory;
    private HydrationHistory hydrationHistory;

    private static final String TAG = "RNGoogleFit";

    public GoogleFitManager(ReactContext reactContext, Activity activity) {

        //Log.i(TAG, "Initializing GoogleFitManager" + mAuthInProgress);
        this.mReactContext = reactContext;
        this.mActivity = activity;

        mReactContext.addActivityEventListener(this);

        this.mStepCounter = new StepCounter(mReactContext, this, activity);
        this.stepHistory = new StepHistory(mReactContext, this);
        this.bodyHistory = new BodyHistory(mReactContext, this);
        this.heartrateHistory = new HeartrateHistory(mReactContext, this);
        this.distanceHistory = new DistanceHistory(mReactContext, this);
        this.calorieHistory = new CalorieHistory(mReactContext, this);
        this.nutritionHistory = new NutritionHistory(mReactContext, this);
        this.recordingApi = new RecordingApi(mReactContext, this);
        this.activityHistory = new ActivityHistory(mReactContext, this);
        this.hydrationHistory = new HydrationHistory(mReactContext, this);
        //        this.stepSensor = new StepSensor(mReactContext, activity);
    }

    public GoogleApiClient getGoogleApiClient() {
        return mApiClient;
    }

    public RecordingApi getRecordingApi() {
        return recordingApi;
    }

    public StepCounter getStepCounter() {
        return mStepCounter;
    }

    public StepHistory getStepHistory() {
        return stepHistory;
    }

    public BodyHistory getBodyHistory() {
        return bodyHistory;
    }

    public HeartrateHistory getHeartrateHistory() {
        return heartrateHistory;
    }

    public DistanceHistory getDistanceHistory() {
        return distanceHistory;
    }

    public void resetAuthInProgress()
    {
        if (!isAuthorized()) {
            mAuthInProgress = false;
        }
    }

    public CalorieHistory getCalorieHistory() { return calorieHistory; }

    public NutritionHistory getNutritionHistory() { return nutritionHistory; }

    public HydrationHistory getHydrationHistory() { return hydrationHistory; }

    public void authorize(ArrayList<String> userScopes) {
        final ReactContext mReactContext = this.mReactContext;

        GoogleApiClient.Builder apiClientBuilder = new GoogleApiClient.Builder(mReactContext.getApplicationContext())
                .addApi(Fitness.SENSORS_API)
                .addApi(Fitness.HISTORY_API)
                .addApi(Fitness.RECORDING_API);

        for (String scopeName : userScopes) {
            apiClientBuilder.addScope(new Scope(scopeName));
        }

        mApiClient = apiClientBuilder
                .addConnectionCallbacks(
                        new GoogleApiClient.ConnectionCallbacks() {
                            @Override
                            public void onConnected(@Nullable Bundle bundle) {
                                Log.i(TAG, "Authorization - Connected");
                                sendEvent(mReactContext, "GoogleFitAuthorizeSuccess", null);
                            }

                            @Override
                            public void onConnectionSuspended(int i) {
                                Log.i(TAG, "Authorization - Connection Suspended");
                                if ((mApiClient != null) && (mApiClient.isConnected())) {
                                    mApiClient.disconnect();
                                }
                            }
                        }
                )
                .addOnConnectionFailedListener(
                        new GoogleApiClient.OnConnectionFailedListener() {
                            @Override
                            public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
                                Log.i(TAG, "Authorization - Failed Authorization Mgr:" + connectionResult);
                                if (mAuthInProgress) {
                                    Log.i(TAG, "Authorization - Already attempting to resolve an error.");
                                } else if (connectionResult.hasResolution()) {
                                    try {
                                        mAuthInProgress = true;
                                        connectionResult.startResolutionForResult(mActivity, REQUEST_OAUTH);
                                    } catch (IntentSender.SendIntentException e) {
                                        Log.i(TAG, "Authorization - Failed again: " + e);
                                        mApiClient.connect();
                                    }
                                } else {
                                    Log.i(TAG, "Show dialog using GoogleApiAvailability.getErrorDialog()");
                                    showErrorDialog(connectionResult.getErrorCode());
                                    mAuthInProgress = true;
                                    WritableMap map = Arguments.createMap();
                                    map.putString("message", "" + connectionResult);
                                    sendEvent(mReactContext, "GoogleFitAuthorizeFailure", map);
                                }
                            }
                        }
                )
                .build();

        mApiClient.connect();
    }

    public void  disconnect(Context context) {
        GoogleSignInOptions options = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();
        GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(context, options);

        GoogleSignInAccount gsa = GoogleSignIn.getAccountForScopes(mReactContext, new Scope(Scopes.FITNESS_ACTIVITY_READ));
        Fitness.getConfigClient(mReactContext, gsa).disableFit();
        mApiClient.disconnect();

        googleSignInClient.signOut();
    }

    public boolean isAuthorized() {
        if (mApiClient != null && mApiClient.isConnected()) {
            return true;
        } else {
            return false;
        }
    }

    protected void stop() {
        Fitness.SensorsApi.remove(mApiClient, mStepCounter)
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
        if (requestCode == REQUEST_OAUTH) {
            mAuthInProgress = false;
            if (resultCode == Activity.RESULT_OK) {
                // Make sure the app is not already connected or attempting to connect
                if (!mApiClient.isConnecting() && !mApiClient.isConnected()) {
                    mApiClient.connect();
                }
            } else if (resultCode == Activity.RESULT_CANCELED) {
                Log.e(TAG, "Authorization - Cancel");
                WritableMap map = Arguments.createMap();
                map.putString("message", "" + "Authorization cancelled");
                sendEvent(mReactContext, "GoogleFitAuthorizeFailure", map);
            }
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
    }

    public ActivityHistory getActivityHistory() {
        return activityHistory;
    }

    public void setActivityHistory(ActivityHistory activityHistory) {
        this.activityHistory = activityHistory;
    }

    public static class GoogleFitCustomErrorDialig extends ErrorDialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Get the error code and retrieve the appropriate dialog
            int errorCode = this.getArguments().getInt(AUTH_PENDING);
            return GoogleApiAvailability.getInstance().getErrorDialog(
                    this.getActivity(), errorCode, REQUEST_OAUTH);
        }

        @Override
        public void onCancel(DialogInterface dialog) {
            mAuthInProgress = false;
        }
    }

    /* Creates a dialog for an error message */
    private void showErrorDialog(int errorCode) {
        // Create a fragment for the error dialog
        GoogleFitCustomErrorDialig dialogFragment = new GoogleFitCustomErrorDialig();
        // Pass the error that should be displayed
        Bundle args = new Bundle();
        args.putInt(AUTH_PENDING, errorCode);
        dialogFragment.setArguments(args);
        dialogFragment.show(mActivity.getFragmentManager(), "errordialog");
    }
}
