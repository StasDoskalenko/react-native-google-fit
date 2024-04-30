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
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptionsExtension;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessOptions;
import com.google.android.gms.fitness.data.Bucket;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataSource;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.data.Session;
import com.google.android.gms.fitness.request.DataDeleteRequest;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.request.SessionInsertRequest;
import com.google.android.gms.fitness.request.SessionReadRequest;
import com.google.android.gms.fitness.result.DataReadResponse;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.google.android.gms.fitness.data.Device.TYPE_WATCH;

public class ActivityHistory {

    private ReactContext mReactContext;
    private GoogleFitManager googleFitManager;
    private static final String TAG = "RNGoogleFit";

    private static final String HIGH_LONGITUDE = "high_longitude";
    private static final String LOW_LONGITUDE = "low_longitude";
    private static final String HIGH_LATITUDE = "high_latitude";
    private static final String LOW_LATITUDE = "low_latitude";

    private static final int KCAL_MULTIPLIER = 1000;
    private static final int ONGOING_ACTIVITY_MIN_TIME_FROM_END = 10 * 60000;

    private static final String STEPS_FIELD_NAME = "steps";
    private static final String DISTANCE_FIELD_NAME = "distance";
    private static final String CALORIES_FIELD_NAME = "calories";
    private static final String DURATION_FIELD_NAME = "duration";
    private static final String INTENSITY_FIELD_NAME = "intensity";

    private static final DataType[] WORKOUT_FIELD_DATATYPE = new DataType[]{
            DataType.TYPE_ACTIVITY_SEGMENT,
            DataType.TYPE_MOVE_MINUTES,
            DataType.TYPE_HEART_POINTS,
            DataType.TYPE_STEP_COUNT_DELTA,
            DataType.TYPE_CALORIES_EXPENDED
    };

    public ActivityHistory(ReactContext reactContext, GoogleFitManager googleFitManager){
        this.mReactContext = reactContext;
        this.googleFitManager = googleFitManager;
    }

    public ReadableArray getActivitySamples(long startTime, long endTime, int bucketInterval, String bucketUnit) {
        WritableArray results = Arguments.createArray();
        DataReadRequest.Builder readRequestBuilder = new DataReadRequest.Builder();

        for (DataType dt : WORKOUT_FIELD_DATATYPE) {
            readRequestBuilder.aggregate(dt);
        }
        readRequestBuilder.aggregate(DataType.TYPE_DISTANCE_DELTA);

        //bucket by activity segment is critical, not bucketByTime
        DataReadRequest readRequest = readRequestBuilder
                .bucketByActivitySegment(bucketInterval, HelperUtil.processBucketUnit(bucketUnit))
                .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                .build();

        FitnessOptions fitnessOptions = createWorkoutFitnessOptions(FitnessOptions.ACCESS_READ);
        GoogleSignInAccount googleSignInAccount =
                GoogleSignIn.getAccountForExtension(this.mReactContext, fitnessOptions);

        try {
            Task<DataReadResponse> task = Fitness.getHistoryClient(this.mReactContext, googleSignInAccount)
                    .readData(readRequest);

            DataReadResponse response = Tasks.await(task, 30, TimeUnit.SECONDS);

            if (response.getStatus().isSuccess()) {
                for (Bucket bucket : response.getBuckets()) {
                    String activityName = bucket.getActivity();
                    int activityType = bucket.getBucketType();
                    if (!bucket.getDataSets().isEmpty()) {
                        long start = bucket.getStartTime(TimeUnit.MILLISECONDS);
                        long end = bucket.getEndTime(TimeUnit.MILLISECONDS);
                        Date startDate = new Date(start);
                        Date endDate = new Date(end);
                        WritableMap map = Arguments.createMap();
                        map.putDouble("start", start); // deprecated
                        map.putDouble("startDate", start);
                        map.putDouble("end", end); // deprecated
                        map.putDouble("endDate", end);
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
                                            map.putInt(fieldName, dataPoint.getValue(field).asInt());
                                            // deprecated
                                            map.putInt("quantity", dataPoint.getValue(field).asInt());
                                            break;
                                        case DURATION_FIELD_NAME:
                                            map.putInt(fieldName, dataPoint.getValue(field).asInt());
                                            break;
                                        case DISTANCE_FIELD_NAME:
                                        case CALORIES_FIELD_NAME:
                                        case INTENSITY_FIELD_NAME:
                                            map.putDouble(fieldName, dataPoint.getValue(field).asFloat());
                                            break;
                                        default:
                                            map.putString(fieldName, dataPoint.getValue(field).toString());
                                            break;
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
            } else {
                Log.w(TAG, "There was an error reading data from Google Fit" + response.getStatus().toString());
            }

        } catch (Exception e) {
            Log.w(TAG, "Exception: " + e);
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
                        HelperUtil.processDataSet(this.mReactContext, TAG, dataSet, moveMinutes);
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

    public void getWorkoutSession(long startTime, long endTime, ReadableMap options, final Promise promise) {
        WritableArray results = Arguments.createArray();
        String readSessionFromAllAppsKey = "readSessionFromAllApps";
        boolean readSessionFromAllApps = options.hasKey(readSessionFromAllAppsKey)
                ? options.getBoolean(readSessionFromAllAppsKey)
                : false;

        SessionReadRequest.Builder readRequestBuilder = new SessionReadRequest.Builder()
                .setTimeInterval(startTime, endTime, TimeUnit.MILLISECONDS)
                .includeActivitySessions();

        if(readSessionFromAllApps) readRequestBuilder.readSessionsFromAllApps();

        for (DataType dataType : WORKOUT_FIELD_DATATYPE) {
            readRequestBuilder.read(dataType);
        }
        readRequestBuilder.read(DataType.TYPE_DISTANCE_DELTA);

        SessionReadRequest readRequest = readRequestBuilder.build();
        FitnessOptions fitnessOptions = createWorkoutFitnessOptions(FitnessOptions.ACCESS_READ);

        Fitness.getSessionsClient(this.mReactContext, GoogleSignIn.getAccountForExtension(this.mReactContext, fitnessOptions))
                .readSession(readRequest)
                .addOnSuccessListener(response -> {
                    List<Session> sessions = response.getSessions();
                    for (Session session : sessions) {
                        WritableMap map = Arguments.createMap();
                        List<DataSet> dataSets = response.getDataSet(session);
                        for (DataSet dataSet : dataSets) {
                            for (DataPoint dataPoint : dataSet.getDataPoints()) {
                                for (Field field : dataPoint.getDataType().getFields()) {
                                    String fieldName = field.getName();
                                    switch (fieldName) {
                                        case STEPS_FIELD_NAME:
                                        case DURATION_FIELD_NAME:
                                            map.putInt(fieldName, dataPoint.getValue(field).asInt());
                                            break;
                                        case DISTANCE_FIELD_NAME:
                                        case CALORIES_FIELD_NAME:
                                        case INTENSITY_FIELD_NAME:
                                            map.putDouble(fieldName, dataPoint.getValue(field).asFloat());
                                            break;
                                        default:
                                            map.putString(fieldName, dataPoint.getValue(field).toString());
                                            break;
                                    }
                                }
                            }
                        }
                        map.putString("appPackageName", session.getAppPackageName());
                        map.putString("activity", session.getActivity());
                        map.putDouble("startDate", session.getStartTime(TimeUnit.MILLISECONDS));
                        map.putDouble("endDate", session.getEndTime(TimeUnit.MILLISECONDS));
                        map.putString("sessionName", session.getName());
                        map.putString("description", session.getDescription());
                        map.putString("identifier", session.getIdentifier());
                        results.pushMap(map);
                    }
                    promise.resolve(results);
                })
                .addOnFailureListener(promise::reject);
    }

    public void saveWorkout(long startTime, long endTime, ReadableMap options, final Promise promise) {
        String sessionName = options.getString("sessionName");
        String identifier = options.getString("identifier");
        String description = options.hasKey("description") ? options.getString("description") : "";
        String activityType = options.getString("activityType");

        //create session
        Session session = new Session.Builder()
                .setName(sessionName)
                .setActivity(activityType)
                .setIdentifier(identifier)
                .setDescription(description)
                .setStartTime(startTime, TimeUnit.MILLISECONDS)
                .setEndTime(endTime, TimeUnit.MILLISECONDS)
                .build();

        //create session insert request builder
        SessionInsertRequest.Builder sessionInsertBuilder = new SessionInsertRequest.Builder()
                .setSession(session);

        //create session client builder
        FitnessOptions.Builder fitnessOptionsBuilder = FitnessOptions.builder();

        //create session activity
        DataSource activityDataSource = createWorkoutDataSource(DataType.TYPE_ACTIVITY_SEGMENT);

        DataPoint activityDataPoint = DataPoint.builder(activityDataSource)
                .setActivityField(Field.FIELD_ACTIVITY, activityType)
                .setTimeInterval(startTime, endTime, TimeUnit.MILLISECONDS)
                .build();

        DataSet activityDataSet = DataSet.builder(activityDataSource)
                .add(activityDataPoint)
                .build();

        sessionInsertBuilder.addDataSet(activityDataSet);
        fitnessOptionsBuilder.addDataType(DataType.TYPE_ACTIVITY_SEGMENT, FitnessOptions.ACCESS_WRITE);

        //create calories
        if (options.hasKey(CALORIES_FIELD_NAME)) {
            Float calories = (float) options.getDouble(CALORIES_FIELD_NAME);
            DataSource calDataSource = createWorkoutDataSource(DataType.TYPE_CALORIES_EXPENDED);

            DataPoint calDataPoint = DataPoint.builder(calDataSource)
                    .setTimeInterval(startTime, endTime, TimeUnit.MILLISECONDS)
                    .setField(Field.FIELD_CALORIES, calories)
                    .build();

            DataSet calDataSet = DataSet.builder(calDataSource)
                    .add(calDataPoint)
                    .build();

            sessionInsertBuilder.addDataSet(calDataSet);
            fitnessOptionsBuilder.addDataType(DataType.TYPE_CALORIES_EXPENDED, FitnessOptions.ACCESS_WRITE);
        }

        //create intensity
        if (options.hasKey(INTENSITY_FIELD_NAME)) {
            Float intensity = (float) options.getDouble(INTENSITY_FIELD_NAME);
            DataSource intensityDataSource = createWorkoutDataSource(DataType.TYPE_HEART_POINTS);

            DataPoint intensityDataPoint = DataPoint.builder(intensityDataSource)
                    .setTimeInterval(startTime, endTime, TimeUnit.MILLISECONDS)
                    .setField(Field.FIELD_INTENSITY, intensity)
                    .build();

            DataSet intensityDataSet = DataSet.builder(intensityDataSource)
                    .add(intensityDataPoint)
                    .build();

            sessionInsertBuilder.addDataSet(intensityDataSet);
            fitnessOptionsBuilder.addDataType(DataType.TYPE_HEART_POINTS, FitnessOptions.ACCESS_WRITE);
        }

        //create steps
        if (options.hasKey(STEPS_FIELD_NAME)) {
            int steps = options.getInt(STEPS_FIELD_NAME);
            DataSource stepsDataSource = createWorkoutDataSource(DataType.TYPE_STEP_COUNT_DELTA);

            DataPoint stepsDataPoint = DataPoint.builder(stepsDataSource)
                    .setTimeInterval(startTime, endTime, TimeUnit.MILLISECONDS)
                    .setField(Field.FIELD_STEPS, steps)
                    .build();

            DataSet stepsDataSet = DataSet.builder(stepsDataSource)
                    .add(stepsDataPoint)
                    .build();

            sessionInsertBuilder.addDataSet(stepsDataSet);
            fitnessOptionsBuilder.addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_WRITE);
        }

        if (options.hasKey(DISTANCE_FIELD_NAME)) {
            float distance = (float) options.getDouble(DISTANCE_FIELD_NAME);
            DataSource distanceDataSource = createWorkoutDataSource(DataType.TYPE_DISTANCE_DELTA);

            DataPoint distanceDataPoint = DataPoint.builder(distanceDataSource)
                    .setTimeInterval(startTime, endTime, TimeUnit.MILLISECONDS)
                    .setField(Field.FIELD_DISTANCE, distance)
                    .build();

            DataSet distanceDataSet = DataSet.builder(distanceDataSource)
                    .add(distanceDataPoint)
                    .build();

            sessionInsertBuilder.addDataSet(distanceDataSet);
            fitnessOptionsBuilder.addDataType(DataType.TYPE_DISTANCE_DELTA, FitnessOptions.ACCESS_WRITE);
        }

        // add dataSet into session
        SessionInsertRequest insertRequest = sessionInsertBuilder.build();

        // session Client
        FitnessOptions fitnessOptions = fitnessOptionsBuilder.build();
        GoogleSignInAccount account = GoogleSignIn.getAccountForExtension(this.mReactContext, fitnessOptions);

        Fitness.getSessionsClient(this.mReactContext, account)
                .insertSession(insertRequest)
                .addOnSuccessListener(unused -> promise.resolve(true))
                .addOnFailureListener(e -> promise.reject(e));
    }

    public void deleteAllWorkout(long startTime, long endTime, ReadableMap options,  final Promise promise) {
        DataDeleteRequest.Builder requestBuilder = new DataDeleteRequest.Builder()
                .setTimeInterval(startTime, endTime, TimeUnit.MILLISECONDS);


        // distance scope is missing here due to permission restriction
        // will be added in the future while distance is also available from saveWorkout()
        DataDeleteRequest request = requestBuilder
                .addDataType(DataType.TYPE_HEART_POINTS)
                .addDataType(DataType.TYPE_ACTIVITY_SEGMENT)
                .addDataType(DataType.TYPE_CALORIES_EXPENDED)
                .addDataType(DataType.TYPE_STEP_COUNT_DELTA)
                .addDataType(DataType.TYPE_MOVE_MINUTES)
                .deleteAllSessions()
                .build();

        FitnessOptions fitnessOptions = createWorkoutFitnessOptions(FitnessOptions.ACCESS_WRITE);

        Fitness.getHistoryClient(this.mReactContext, GoogleSignIn.getAccountForExtension(this.mReactContext, fitnessOptions))
                .deleteData(request)
                .addOnSuccessListener(unused -> promise.resolve(true))
                .addOnFailureListener(e -> promise.reject(e));
    }

    public void deleteAllSleep(long startTime, long endTime, ReadableMap options,  final Promise promise) {
        DataDeleteRequest.Builder requestBuilder = new DataDeleteRequest.Builder()
                .setTimeInterval(startTime, endTime, TimeUnit.MILLISECONDS);

        DataDeleteRequest request = requestBuilder
                .addDataType(DataType.TYPE_SLEEP_SEGMENT)
                .deleteAllSessions()
                .build();

        FitnessOptions fitnessOptions = createWorkoutFitnessOptions(FitnessOptions.ACCESS_WRITE);

        Fitness.getHistoryClient(this.mReactContext, GoogleSignIn.getAccountForExtension(this.mReactContext, fitnessOptions))
                .deleteData(request)
                .addOnSuccessListener(unused -> promise.resolve(true))
                .addOnFailureListener(e -> promise.reject(e));
    }

    //private helper functions
    private FitnessOptions createWorkoutFitnessOptions(int fitnessOptionsAccess) {
        FitnessOptions.Builder fitnessOptionsBuilder = FitnessOptions.builder();
        for (DataType dataType : WORKOUT_FIELD_DATATYPE) {
            fitnessOptionsBuilder.addDataType(dataType, fitnessOptionsAccess);
        }
        return fitnessOptionsBuilder.build();
    }

    private DataSource createWorkoutDataSource(DataType dataType) {
        DataSource dataSource = new DataSource.Builder()
                .setAppPackageName(this.mReactContext.getPackageName())
                .setDataType(dataType)
                .setType(DataSource.TYPE_RAW)
                .build();
        return dataSource;
    }

}
