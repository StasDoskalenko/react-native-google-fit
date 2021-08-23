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

import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptionsExtension;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessActivities;
import com.google.android.gms.fitness.FitnessOptions;
import com.google.android.gms.fitness.data.Bucket;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataSource;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.data.Session;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.request.SessionInsertRequest;
import com.google.android.gms.fitness.result.DataReadResponse;
import com.google.android.gms.fitness.result.DataReadResult;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.google.android.gms.fitness.data.Device.TYPE_WATCH;

public class ActivityHistory {

    private ReactContext mReactContext;
    private GoogleFitManager googleFitManager;

    private static final String STEPS_FIELD_NAME = "steps";
    private static final String DISTANCE_FIELD_NAME = "distance";
    private static final String HIGH_LONGITUDE = "high_longitude";
    private static final String LOW_LONGITUDE = "low_longitude";
    private static final String HIGH_LATITUDE = "high_latitude";
    private static final String LOW_LATITUDE = "low_latitude";


    private static final int KCAL_MULTIPLIER = 1000;
    private static final int ONGOING_ACTIVITY_MIN_TIME_FROM_END = 10 * 60000;
    private static final String CALORIES_FIELD_NAME = "calories";

    private static final String TAG = "RNGoogleFit";

    public ActivityHistory(ReactContext reactContext, GoogleFitManager googleFitManager){
        this.mReactContext = reactContext;
        this.googleFitManager = googleFitManager;
    }

    public ReadableArray getActivitySamples(long startTime, long endTime, int bucketInterval, String bucketUnit) {
        WritableArray results = Arguments.createArray();
        DataReadRequest readRequest = new DataReadRequest.Builder()
                .aggregate(DataType.TYPE_STEP_COUNT_DELTA, DataType.AGGREGATE_STEP_COUNT_DELTA)
                .aggregate(DataType.TYPE_CALORIES_EXPENDED, DataType.AGGREGATE_CALORIES_EXPENDED)
                .aggregate(DataType.TYPE_DISTANCE_DELTA, DataType.AGGREGATE_DISTANCE_DELTA)
                .bucketByActivitySegment(bucketInterval, HelperUtil.processBucketUnit(bucketUnit))
                .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                .build();

        DataReadResult dataReadResult = Fitness.HistoryApi.readData(googleFitManager.getGoogleApiClient(), readRequest).await(1, TimeUnit.MINUTES);

        List<Bucket> buckets = dataReadResult.getBuckets();
        for (Bucket bucket : buckets) {
            String activityName = bucket.getActivity();
            int activityType = bucket.getBucketType();
            if (!bucket.getDataSets().isEmpty()) {
                long start = bucket.getStartTime(TimeUnit.MILLISECONDS);
                long end = bucket.getEndTime(TimeUnit.MILLISECONDS);
                Date startDate = new Date(start);
                Date endDate = new Date(end);
                WritableMap map = Arguments.createMap();
                map.putDouble("start",start);
                map.putDouble("end",end);
                map.putString("activityName", activityName);
                String deviceName = "";
                String sourceId = "";
                boolean isTracked = true;
                for (DataSet dataSet : bucket.getDataSets()) {
                    for (DataPoint dataPoint : dataSet.getDataPoints()) {
                        try {
                            int deviceType = dataPoint.getOriginalDataSource().getDevice().getType();
                            if (deviceType == TYPE_WATCH) {
                                deviceName = "Android Wear";
                            } else {
                                deviceName = "Android";
                            }
                        } catch (Exception e) {
                        }
                        sourceId = dataPoint.getOriginalDataSource().getAppPackageName();
                        if (startDate.getTime() % 1000 == 0 && endDate.getTime() % 1000 == 0) {
                            isTracked = false;
                        }
                        for (Field field : dataPoint.getDataType().getFields()) {
                            String fieldName = field.getName();
                            switch (fieldName) {
                                case STEPS_FIELD_NAME:
                                    map.putInt("quantity", dataPoint.getValue(field).asInt());
                                    break;
                                case DISTANCE_FIELD_NAME:
                                    map.putDouble(fieldName, dataPoint.getValue(field).asFloat());
                                    break;
                                case CALORIES_FIELD_NAME:
                                    map.putDouble(fieldName, dataPoint.getValue(field).asFloat());
                                default:
                                    Log.w(TAG, "don't specified and handled: " + fieldName);
                            }
                        }
                    }
                }
                map.putString("device", deviceName);
                map.putString("sourceName", deviceName);
                map.putString("sourceId", sourceId);
                map.putBoolean("tracked", isTracked);
                results.pushMap(map);
            }
        }

        return results;
    }

    public ReadableArray getMoveMinutes(long startTime, long endTime, int bucketInterval, String bucketUnit) {
        DataType[] fitnessDataTypes = {DataType.TYPE_MOVE_MINUTES, DataType.AGGREGATE_MOVE_MINUTES};
        DataReadRequest readReq = HelperUtil.createDataReadRequest(
                startTime,
                endTime,
                bucketInterval,
                bucketUnit,
                fitnessDataTypes);
        Integer[] accessOpts = {FitnessOptions.ACCESS_READ};
        GoogleSignInOptionsExtension fitnessOptions = HelperUtil.createSignInFitnessOptions(DataType.TYPE_MOVE_MINUTES, accessOpts);

        GoogleSignInAccount googleSignInAccount =
                GoogleSignIn.getAccountForExtension(this.mReactContext, fitnessOptions);

        WritableArray moveMinutes = Arguments.createArray();

        try {
            Task<DataReadResponse> task = Fitness.getHistoryClient(this.mReactContext, googleSignInAccount)
                    .readData(readReq);
            DataReadResponse response = Tasks.await(task, 30, TimeUnit.SECONDS);
            if (response.getStatus().isSuccess()) {
                for (Bucket bucket : response.getBuckets()) {
                    for (DataSet dataSet : bucket.getDataSets()) {
                        HelperUtil.processDataSet(TAG, dataSet, moveMinutes);
                    }
                }
                return moveMinutes;
            } else {
                Log.w(TAG, "There was an error reading data from Google Fit" + response.getStatus().toString());
            }
        } catch (Exception e) {
            Log.w(TAG, "Exception: " + e);
        }
        return moveMinutes;
    }

    public void saveWorkout(String name, String id, String description, long startTime, long endTime, float calories, Promise promise) {
        Session session = new Session.Builder()
                .setName(name)
                .setIdentifier(id)
                .setDescription(description)

                .setActivity(FitnessActivities.STRENGTH_TRAINING)
                .setStartTime(startTime, TimeUnit.MILLISECONDS)
                .setEndTime(endTime, TimeUnit.MILLISECONDS)
                .build();

        DataSource sourceKcal = new DataSource.Builder()
                .setAppPackageName(this.mReactContext.getPackageName())
                .setDataType(DataType.TYPE_CALORIES_EXPENDED)
                .setType(DataSource.TYPE_RAW)
                .build();


        DataPoint kcal = DataPoint.builder(sourceKcal)
                .setTimeInterval(startTime, endTime, TimeUnit.MILLISECONDS)
                .setFloatValues(calories)
                .build();

        DataSet kcalDataset = DataSet.builder(sourceKcal)
                .add(kcal)
                .build();

        SessionInsertRequest insertRequest = new SessionInsertRequest.Builder()
                .setSession(session)
                // Optionally add DataSets for this session.
                .addDataSet(kcalDataset)
                .build();
        GoogleSignInOptionsExtension fitnessOptions =
                FitnessOptions.builder()
                        .addDataType(DataType.TYPE_CALORIES_EXPENDED, FitnessOptions.ACCESS_WRITE)
                        .build();

        Fitness.getSessionsClient(this.mReactContext, GoogleSignIn.getAccountForExtension(this.mReactContext, fitnessOptions))
                .insertSession(insertRequest)
                .addOnSuccessListener (unused -> {
                    Log.i(TAG, "Session insert was successful!");
                    promise.resolve(true);
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "There was a problem inserting the session: ", e);
                    promise.reject(e);
                });
    }
}
