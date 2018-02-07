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
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
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
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.ArrayList;


import static com.google.android.gms.fitness.data.Device.TYPE_WATCH;

public class ActivityHistory {

    private ReactContext mReactContext;
    private GoogleFitManager googleFitManager;

    private static final String ACTIVITY_TYPE_UNKNOWN = "unknown";
    private static final String ACTIVITY_TYPE_STILL = "still";
    private static final String ACTIVITY_TYPE_WALKING = "walking";
    private static final String ACTIVITY_IN_VEHICLE = "in_vehicle";
    private static final String STEPS_FIELD_NAME = "steps";
    private static final String DISTANCE_FIELD_NAME = "distance";
    private static final String STRAVA = "strava";

    private static final int KCAL_MULTIPLIER = 1000;
    private static final int ONGOING_ACTIVITY_MIN_TIME_FROM_END = 10 * 60000;
    private static final String CALORIES_FIELD_NAME = "calories";

    private enum ActivityDatasetType {

        CALORIES("com.google.calories.expended"),
        STEP_COUNT("com.google.step_count.delta"),
        DISTANCE("com.google.distance.delta");
        private final String id;

        ActivityDatasetType(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }

        public static ActivityDatasetType getTypeById(String id) {
            for (ActivityDatasetType type : ActivityDatasetType.values()) {
                if (type.getId().equals(id)) return type;
            }
            return null;
        }
    }

    private static final String TAG = "RNGoogleFit";

    public ActivityHistory(ReactContext reactContext, GoogleFitManager googleFitManager){
        this.mReactContext = reactContext;
        this.googleFitManager = googleFitManager;
    }

    public ReadableArray getActivitySamples(long startTime, long endTime) {
        Log.i(TAG, "go into getActivitySamples" + startTime + endTime);
        WritableArray results = Arguments.createArray();

        DataReadRequest readRequest = new DataReadRequest.Builder()
                .aggregate(DataType.TYPE_STEP_COUNT_DELTA, DataType.AGGREGATE_STEP_COUNT_DELTA)
                .aggregate(DataType.TYPE_CALORIES_EXPENDED, DataType.AGGREGATE_CALORIES_EXPENDED)
                .bucketByActivitySegment(1, TimeUnit.SECONDS)
                .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                .build();

        DataReadResult dataReadResult = Fitness.HistoryApi.readData(googleFitManager.getGoogleApiClient(), readRequest).await(1, TimeUnit.MINUTES);

        List<Bucket> buckets = dataReadResult.getBuckets();
        for (Bucket bucket : buckets) {
            String activityName = bucket.getActivity();
//            Log.i(TAG, "next activity: " + activityName);

//            switch (activityName) {
//                case ACTIVITY_TYPE_UNKNOWN:
//                case ACTIVITY_TYPE_STILL:
//                case ACTIVITY_IN_VEHICLE:
//                    activityName = ACTIVITY_TYPE_WALKING;
//                    break;
//            }

            if (!bucket.getDataSets().isEmpty()) {
                int steps = 0;
                float distance = 0f;
                float calories = 0f;
                long start = bucket.getStartTime(TimeUnit.MILLISECONDS);
                long end = bucket.getEndTime(TimeUnit.MILLISECONDS);
                for (DataSet dataSet : bucket.getDataSets()) {
                    ActivityDatasetType datasetType = ActivityDatasetType.getTypeById(dataSet.getDataType().getName());
//                    Log.i(TAG, "next dataset: " + datasetType.getId());

                    for (DataPoint dataPoint : dataSet.getDataPoints()) {
                        DataSource originalDataSource = dataPoint.getOriginalDataSource();

//                        int deviceType = dataPoint.getOriginalDataSource().getDevice().getType();

//                        if (deviceType == TYPE_WATCH) {
//                            return ANDROID_WEAR;
//                        } else {
//                            return ANDROID;
//                        }


                        //code for detecting magical activities created by google fit and user inputs
                        String streamName = dataPoint.getOriginalDataSource().getStreamName();
//                        if (!TextUtils.isEmpty(streamName)) {
//                            ActivityDataSourceStreamType dataSourceStreamType =
//                                    ActivityDataSourceStreamType.getTypeByStreamName(streamName);
//                            if (dataSourceStreamType != null) {
//                                dataSourceStreamTypes.add(dataSourceStreamType);
//                            }
//                        }
                        //end of code for detecting magical activities created by google fit and user inputs

                        for (Field field : dataPoint.getDataType().getFields()) {
                            String fieldName = field.getName();
                            switch (fieldName) {
                                case STEPS_FIELD_NAME:
                                    steps += dataPoint.getValue(field).asInt();
                                    break;
                                case DISTANCE_FIELD_NAME:
                                    distance += dataPoint.getValue(field).asFloat();
                                    break;
                                case CALORIES_FIELD_NAME:
                                    calories += dataPoint.getValue(field).asFloat();
                                    break;
                            }
                        }
                    }
                }
                Log.i(TAG, activityName +" "+ steps + "\t" + distance + "\t" + calories);
                WritableMap map = Arguments.createMap();
                map.putDouble("distance", distance);
                map.putDouble("calories", calories);
                map.putInt("steps", steps);
                map.putDouble("startDate",start);
                map.putDouble("endDate",end);
                map.putString("type", activityName);
                results.pushMap(map);
            }

//            if (TextUtils.isEmpty(activity.getDevice())) {
//                activity.setDevice(ANDROID);
//            }
//
//            //code for detecting magical activities created by google fit
//            if (dataSourceStreamTypes.isEmpty()) {
//                return null;
//            }
//
//            if (dataSourceStreamTypes.contains(FROM_ACTIVITIES) && !dataSourceStreamTypes.contains(STEP_COUNT)) {
//                return null;
//            }
            //end of code for detecting magical activities created by google fit

//            boolean isActivityTracked;
//            if (dataSourceStreamTypes.contains(USER_INPUT)) {
//                isActivityTracked = false;
//            } else {
//                if (dataSourceType == ActivityDataSourceType.STRAVA && activity.getDistance() > 0) {
//                    // Logged activities in strava have only one decimal point, so for example 223.4,
//                    // and the tracked activities have for example 522.3212
//                    isActivityTracked = (activity.getDistance() * 10) % 1 != 0;
//                } else {
//                    //Because there is no consistent way to identify if an activity is logged or tracked, we are
//                    // looking at the start and end times. If those end with 000, it means it is a logged activity.
//                    isActivityTracked = startTime.getTime() % 1000 != 0 || endTime.getTime() % 1000 != 0;
//                }
//            }
//            activity.setTracked(isActivityTracked);

            //We don't want to process tracked walking activities without stepcount
//            if (!testMode && isActivityTracked && activity.getStepCount() <= 0 &&
//                    TextUtility.equalsOneOfThem(activity.getActivityName(), ACTIVITY_TYPE_WALKING,
//                            ACTIVITY_TYPE_STILL, ACTIVITY_IN_VEHICLE, ACTIVITY_TYPE_UNKNOWN)) {
//                return null;
//            }

//            long currentTime = DateUtility.getCalendar().getTimeInMillis();
//            if (isActivityTracked && currentTime - endTime.getTime() < ONGOING_ACTIVITY_MIN_TIME_FROM_END) {
//                //if the end of the activity is less then 10 minutes from now, and it is a tracked activity,
//                // it is possibly an ongoing activity, and therefore we shouldn't sync it, because google
//                // fit will modify that activity later
//                return null;
//            }
        }


        return results;
    }

    private void processDataSet(DataSet dataSet, WritableArray map) {
        //Log.i(TAG, "Data returned for Data type: " + dataSet.getDataType().getName());

        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        dateFormat.setTimeZone(TimeZone.getDefault());

        WritableMap stepMap = Arguments.createMap();

        for (DataPoint dp : dataSet.getDataPoints()) {
            Log.i(TAG, "\tData point:");
            Log.i(TAG, "\t\tType : " + dp.getDataType().getName());
            Log.i(TAG, "\t\tStart: " + dateFormat.format(dp.getStartTime(TimeUnit.MILLISECONDS)));
            Log.i(TAG, "\t\tEnd  : " + dateFormat.format(dp.getEndTime(TimeUnit.MILLISECONDS)));

            for(Field field : dp.getDataType().getFields()) {
                Log.i(TAG, "\t\tField: " + field.getName() +
                        " Value: " + dp.getValue(field));

                stepMap.putDouble("startDate", dp.getStartTime(TimeUnit.MILLISECONDS));
                stepMap.putDouble("endDate", dp.getEndTime(TimeUnit.MILLISECONDS));
                stepMap.putDouble("steps", dp.getValue(field).asInt());
                map.pushMap(stepMap);
            }
        }
    }


    private void sendEvent(ReactContext reactContext,
                           String eventName,
                           @Nullable WritableArray params) {
        reactContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit(eventName, params);
    }

}
