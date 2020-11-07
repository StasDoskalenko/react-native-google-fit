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

import android.os.Build;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import androidx.annotation.RequiresApi;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessActivities;
import com.google.android.gms.fitness.FitnessOptions;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.data.Session;
import com.google.android.gms.fitness.request.SessionReadRequest;
import com.google.android.gms.fitness.result.SessionReadResponse;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import android.util.Log;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.TimeZone;
import java.util.stream.Collectors;


public class SleepHistory {

    private ReactContext mReactContext;
    private GoogleFitManager googleFitManager;

    private static final String TAG = "RNGoogleFit-Sleep";
    private static final String sleepPermissionsError = "4: The user must be signed in to make this API call.";
    private static final int sleepErrorCode = 4;
    public SleepHistory(ReactContext reactContext, GoogleFitManager googleFitManager){
        this.mReactContext = reactContext;
        this.googleFitManager = googleFitManager;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void getSleepData(double startDate, double endDate, final Promise promise) {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        dateFormat.setTimeZone(TimeZone.getDefault());

        SessionReadRequest request = new SessionReadRequest.Builder()
                .readSessionsFromAllApps()
                .includeSleepSessions()
                .read(DataType.TYPE_SLEEP_SEGMENT)
                .setTimeInterval((long) startDate, (long) endDate, TimeUnit.MILLISECONDS)
                .build();

        final GoogleSignInAccount gsa = GoogleSignIn.getAccountForScopes(this.mReactContext, new Scope(Scopes.FITNESS_NUTRITION_READ));
        Fitness.getSessionsClient(this.mReactContext, gsa)
                .readSession(request)
                .addOnSuccessListener(new OnSuccessListener<SessionReadResponse>() {
                    @Override
                    public void onSuccess(SessionReadResponse response) {
                        List<Session> sleepSessions = response.getSessions()
                            .stream()
                            .filter(s -> s.getActivity().equals(FitnessActivities.SLEEP))
                            .collect(Collectors.toList());

                        WritableArray sleepSample = Arguments.createArray();

                        for (Session session : sleepSessions) {
                            WritableMap sleepData = Arguments.createMap();

                            sleepData.putString("addedBy", session.getAppPackageName());
                            sleepData.putString("startDate", dateFormat.format(session.getStartTime(TimeUnit.MILLISECONDS)));
                            sleepData.putString("endDate", dateFormat.format(session.getEndTime(TimeUnit.MILLISECONDS)));

                            // If the sleep session has finer granularity sub-components, extract them:
                            List<DataSet> dataSets = response.getDataSet(session);
                            WritableArray granularity = Arguments.createArray();
                            for (DataSet dataSet : dataSets) {
                                processDataSet(dataSet, granularity);
                            }
                            sleepData.putArray("granularity", granularity);

                            sleepSample.pushMap(sleepData);
                        }
                        promise.resolve(sleepSample);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.i(TAG, "Failure: " + e.getMessage());
                        if (sleepPermissionsError.equals(e.getMessage())) {
                            requestSleepPermissions(gsa);
                            promise.reject(e);
                        } else {
                            promise.reject(e);
                        }
                    }
                });
    }

    void requestSleepPermissions(GoogleSignInAccount account) {
        FitnessOptions fitnessOptions = FitnessOptions.builder()
                .addDataType(DataType.TYPE_ACTIVITY_SEGMENT, FitnessOptions.ACCESS_READ)
                .build();
        GoogleSignIn.requestPermissions(this.mReactContext.getCurrentActivity(), sleepErrorCode, account, fitnessOptions);
    }

    private void processDataSet(DataSet dataSet, WritableArray granularity) {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        dateFormat.setTimeZone(TimeZone.getDefault());
        for (DataPoint dp : dataSet.getDataPoints()) {
            WritableMap sleepStage = Arguments.createMap();

            sleepStage.putInt("sleepStage", dp.getValue(Field.FIELD_SLEEP_SEGMENT_TYPE).asInt());
            sleepStage.putString("startDate", dateFormat.format(dp.getStartTime(TimeUnit.MILLISECONDS)));
            sleepStage.putString("endDate", dateFormat.format(dp.getEndTime(TimeUnit.MILLISECONDS)));

            granularity.pushMap(sleepStage);
        }
    }

}
