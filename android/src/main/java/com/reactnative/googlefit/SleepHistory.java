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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.data.Bucket;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.data.DataSource;
import com.google.android.gms.fitness.request.DataSourcesRequest;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.result.DataReadResult;
import com.google.android.gms.fitness.result.DataSourcesResult;
import com.google.android.gms.fitness.data.Device;

import java.text.DateFormat;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class SleepHistory {

    private ReactContext mReactContext;
    private GoogleFitManager googleFitManager;

    private static final String TAG = "RNGoogleFit";

    public SleepHistory(ReactContext reactContext, GoogleFitManager googleFitManager){
        this.mReactContext = reactContext;
        this.googleFitManager = googleFitManager;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void getSleepData(double startDate, double endDate, final Callback errorCallback, final Callback successCallback) {
        SessionReadRequest request = new SessionReadRequest.Builder()
                .readSessionsFromAllApps()
                .read(DataType.TYPE_ACTIVITY_SEGMENT)
                .setTimeInterval((long) startDate, (long) endDate, TimeUnit.MILLISECONDS)
                .build();

        Fitness.getSessionsClient(this.mReactContext, GoogleSignIn.getLastSignedInAccount(this.mReactContext))
                .readSession(request)
                .addOnSuccessListener(new OnSuccessListener<SessionReadResponse>() {
                    @Override
                    public void onSuccess(SessionReadResponse response) {
                        List<Session> sleepSessions = response.getSessions()
                            .stream()
                            .filter(new Predicate<Session>() {
                                @Override
                                public boolean test(Session s) {
                                    return s.getActivity().equals(FitnessActivities.SLEEP);
                                }
                            })
                            .collect(Collectors.toList());

                        WritableArray sleep = Arguments.createArray();

                        for (Session session : sleepSessions) {
                            List<DataSet> dataSets = response.getDataSet(session);

                            for (DataSet dataSet : dataSets) {
                                processDataSet(dataSet, session, sleep);
                            }
                        }

                        successCallback.invoke(sleep);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        errorCallback.invoke(e.getMessage());
                    }
                });
    }

    private void processDataSet(DataSet dataSet, Session session, WritableArray map) {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        dateFormat.setTimeZone(TimeZone.getDefault());

        WritableMap sleepMap = Arguments.createMap();

        for (DataPoint dp : dataSet.getDataPoints()) {
            sleepMap.putString("value", dp.getValue(Field.FIELD_ACTIVITY).asActivity());
            sleepMap.putString("startDate", dateFormat.format(dp.getStartTime(TimeUnit.MILLISECONDS)));
            sleepMap.putString("endDate", dateFormat.format(dp.getEndTime(TimeUnit.MILLISECONDS)));
            map.pushMap(sleepMap);
        }
    }

}
