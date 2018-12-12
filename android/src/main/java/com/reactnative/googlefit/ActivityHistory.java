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

import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessActivities;
import com.google.android.gms.fitness.data.Bucket;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.data.DataSource;
import com.google.android.gms.fitness.data.Session;
import com.google.android.gms.fitness.request.DataSourcesRequest;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.request.SessionInsertRequest;
import com.google.android.gms.fitness.result.DataReadResult;
import com.google.android.gms.fitness.result.DataSourcesResult;
import com.google.android.gms.fitness.data.Device;

import java.text.DateFormat;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.ArrayList;


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

    public ReadableArray getActivitySamples(long startTime, long endTime) {
        WritableArray results = Arguments.createArray();
        DataReadRequest readRequest = new DataReadRequest.Builder()
                .aggregate(DataType.TYPE_STEP_COUNT_DELTA, DataType.AGGREGATE_STEP_COUNT_DELTA)
                .aggregate(DataType.TYPE_CALORIES_EXPENDED, DataType.AGGREGATE_CALORIES_EXPENDED)
                .aggregate(DataType.TYPE_DISTANCE_DELTA, DataType.AGGREGATE_DISTANCE_DELTA)
                .bucketByActivitySegment(1, TimeUnit.SECONDS)
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

    public ReadableArray getWorkoutSamples(long startTime, long endTime) {
        WritableArray results = Arguments.createArray();
        DataReadRequest readRequest = new DataReadRequest.Builder()
                .aggregate(DataType.TYPE_CALORIES_EXPENDED, DataType.AGGREGATE_CALORIES_EXPENDED)
                .bucketByActivitySegment(1, TimeUnit.SECONDS)
                .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                .build();

        DataReadResult dataReadResult = Fitness.HistoryApi.readData(googleFitManager.getGoogleApiClient(), readRequest).await(1, TimeUnit.MINUTES);

        List<Bucket> buckets = dataReadResult.getBuckets();
        for (Bucket bucket : buckets) {
            String activity = bucket.getActivity();

            if (
                !activity.equalsIgnoreCase(FitnessActivities.UNKNOWN) &&
                !activity.contains(FitnessActivities.SLEEP) &&
                !activity.equalsIgnoreCase(FitnessActivities.IN_VEHICLE) &&
                !activity.equalsIgnoreCase(FitnessActivities.STILL)
            ) {
                WritableMap map = Arguments.createMap();
                map.putDouble("start", bucket.getStartTime(TimeUnit.MILLISECONDS));
                map.putDouble("end", bucket.getEndTime(TimeUnit.MILLISECONDS));

                if (activity.contains(FitnessActivities.WALKING)) {
                    map.putString("workoutType", "walk");
                } else if (activity.contains(FitnessActivities.RUNNING)) {
                    map.putString("workoutType", "run");
                } else if (activity.equalsIgnoreCase(FitnessActivities.YOGA)) {
                    map.putString("workoutType", "yoga");
                } else if (activity.equalsIgnoreCase(FitnessActivities.STRENGTH_TRAINING)) {
                    map.putString("workoutType", "strengthTraining");
                } else if (activity.contains(FitnessActivities.SWIMMING)) {
                    map.putString("workoutType", "swimming");
                } else if (activity.contains(FitnessActivities.BIKING)) {
                    map.putString("workoutType", "cycling");
                } else {
                    map.putString("workoutType", "other");
                }

                for (DataSet dataSet : bucket.getDataSets()) {
                    // Each bucket should realistically have one dataset since we are querying by one single datatype
                    for (DataPoint dataPoint : dataSet.getDataPoints()) {
                        // There should be only one datapoint in each dataset
                        map.putDouble("calories", dataPoint.getValue(Field.FIELD_CALORIES).asFloat());
                    }
                }

                results.pushMap(map);
            }
        }

        return results;
    }

    public void submitWorkout(String workoutType, long startTime, long endTime, float calories) throws Exception {
        // Create calories
        DataSource caloriesDataSource = new DataSource.Builder()
                .setAppPackageName(GoogleFitPackage.PACKAGE_NAME)
                .setDataType(DataType.TYPE_CALORIES_EXPENDED)
                .setType(DataSource.TYPE_RAW)
                .build();

        DataSet caloriesDataSet = DataSet.create(caloriesDataSource);
        DataPoint caloriesDataPoint = caloriesDataSet.createDataPoint().setTimeInterval(startTime, endTime, TimeUnit.MILLISECONDS);
        caloriesDataPoint.getValue(Field.FIELD_CALORIES).setFloat(calories);
        caloriesDataSet.add(caloriesDataPoint);

        // Persist everything in google store
        Session session = new Session.Builder()
                .setActivity(getActivityType(workoutType))
                .setIdentifier(UUID.randomUUID().toString())
                .setStartTime(startTime, TimeUnit.MILLISECONDS)
                .setEndTime(endTime, TimeUnit.MILLISECONDS)
                .build();

        SessionInsertRequest insertRequest = new SessionInsertRequest.Builder()
                .setSession(session)
                .addDataSet(caloriesDataSet)
                .build();

        Status status = Fitness.SessionsApi.insertSession(googleFitManager.getGoogleApiClient(), insertRequest).await(1, TimeUnit.MINUTES);
        if (!status.isSuccess()) {
            throw new Exception(status.getStatusMessage());
        }
    }

    private String getActivityType(String workoutType) {
        String activityType;

        switch (workoutType) {
            case "walk":
                activityType = FitnessActivities.WALKING;
                break;
            case "run":
                activityType = FitnessActivities.RUNNING;
                break;
            case "yoga":
                activityType = FitnessActivities.YOGA;
                break;
            case "strengthTraining":
                activityType = FitnessActivities.STRENGTH_TRAINING;
                break;
            case "swimming":
                activityType = FitnessActivities.SWIMMING;
                break;
            case "cycling":
                activityType = FitnessActivities.BIKING;
                break;
            default:
                activityType = FitnessActivities.OTHER;
                break;
        }

        return activityType;
    }
}
