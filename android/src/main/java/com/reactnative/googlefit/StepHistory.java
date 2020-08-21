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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptionsExtension;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessOptions;
import com.google.android.gms.fitness.data.Bucket;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.data.DataSource;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.result.DataReadResponse;
import com.google.android.gms.fitness.result.DataReadResult;
import com.google.android.gms.fitness.data.Device;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.text.DateFormat;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class StepHistory {

    private ReactContext mReactContext;
    private GoogleFitManager googleFitManager;

    private static final String TAG = "RNGoogleFit";

    public StepHistory(ReactContext reactContext, GoogleFitManager googleFitManager){
        this.mReactContext = reactContext;
        this.googleFitManager = googleFitManager;
    }

    public int getBucketTime(ReadableMap configs) {
        int bucketTime = 12;
        if (null != configs && configs.hasKey("bucketTime")) {
            bucketTime = configs.getInt("bucketTime");
        }
        return bucketTime;
    }

    public TimeUnit getBucketUnit(ReadableMap configs) {
        TimeUnit bucketUnit = TimeUnit.HOURS;
        if(null != configs && configs.hasKey("bucketUnit")) {
            bucketUnit = HelperUtil.processBucketUnit(configs.getString("bucketUnit"));
        }
        return bucketUnit;
    }

    public void getUserInputSteps(long startTime, long endTime, final Callback successCallback) {

        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        dateFormat.setTimeZone(TimeZone.getDefault());

        Log.i(TAG, "Range Start: " + dateFormat.format(startTime));
        Log.i(TAG, "Range End: " + dateFormat.format(endTime));

        final DataReadRequest readRequest = new DataReadRequest.Builder()
            .read(DataType.TYPE_STEP_COUNT_DELTA)
            .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
            .build();

        DataReadResult dataReadResult =
            Fitness.HistoryApi.readData(googleFitManager.getGoogleApiClient(), readRequest).await(1, TimeUnit.MINUTES);

        DataSet stepData = dataReadResult.getDataSet(DataType.TYPE_STEP_COUNT_DELTA);

        int userInputSteps = 0;

        for (DataPoint dp : stepData.getDataPoints()) {
            for(Field field : dp.getDataType().getFields()) {
                if("user_input".equals(dp.getOriginalDataSource().getStreamName())){
                    int steps = dp.getValue(field).asInt();
                    userInputSteps += steps;
                }
            }
        }

        successCallback.invoke(userInputSteps);
    }

    public void aggregateDataByDate(long startTime, long endTime, ReadableMap configs, final Callback successCallback) {

        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        dateFormat.setTimeZone(TimeZone.getDefault());

        //default bucket configs,
        // Half-day resolution
        int bucketTime = 12;
        TimeUnit bucketUnit = TimeUnit.HOURS;

        if(null != configs) {
            bucketTime = this.getBucketTime(configs);
            bucketUnit = this.getBucketUnit(configs);
        }

        Log.i(TAG, "Range Start: " + dateFormat.format(startTime));
        Log.i(TAG, "Range End: " + dateFormat.format(endTime));

        final WritableArray results = Arguments.createArray();

        List<DataSource> dataSources = new ArrayList<>();

        // GoogleFit Apps
        dataSources.add(
            new DataSource.Builder()
                .setAppPackageName("com.google.android.gms")
                .setDataType(DataType.TYPE_STEP_COUNT_DELTA)
                .setType(DataSource.TYPE_DERIVED)
                .setStreamName("estimated_steps")
                .build()
        );

        // GoogleFit Apps
        dataSources.add(
            new DataSource.Builder()
                .setAppPackageName("com.google.android.gms")
                .setDataType(DataType.TYPE_STEP_COUNT_DELTA)
                .setType(DataSource.TYPE_DERIVED)
                .setStreamName("merge_step_deltas")
                .build()
        );

        // Mi Fit
        dataSources.add(
            new DataSource.Builder()
                .setAppPackageName("com.xiaomi.hm.health")
                .setDataType(DataType.TYPE_STEP_COUNT_DELTA)
                .setType(DataSource.TYPE_RAW)
                .setStreamName("")
                .build()
        );

        /*
        DataSourcesRequest sourceRequest = new DataSourcesRequest.Builder()
                .setDataTypes(DataType.TYPE_STEP_COUNT_DELTA,
                    DataType.TYPE_STEP_COUNT_CUMULATIVE,
                    DataType.AGGREGATE_STEP_COUNT_DELTA
                    )
                //.setDataSourceTypes(DataSource.TYPE_DERIVED)
                .build();
        DataSourcesResult dataSourcesResult =
           Fitness.SensorsApi.findDataSources(googleFitManager.getGoogleApiClient(), sourceRequest).await(1, TimeUnit.MINUTES);

        dataSources.addAll( dataSourcesResult.getDataSources() );
        */

        final AtomicInteger dataSourcesToLoad = new AtomicInteger(dataSources.size());

        for (DataSource dataSource : dataSources) {
            final WritableMap source = Arguments.createMap();

            DataType type = dataSource.getDataType();
            Device device = dataSource.getDevice();

            Log.i(TAG, "DataSource:");

            Log.i(TAG, "  + StreamID  : " + dataSource.getStreamIdentifier());
            source.putString("id", dataSource.getStreamIdentifier());

            if (dataSource.getAppPackageName() != null) {
                source.putString("appPackage", dataSource.getAppPackageName());
            } else {
                source.putNull("appPackage");
            }

            if (dataSource.getStreamName() != null) {
                source.putString("stream", dataSource.getStreamName());
            } else {
                source.putNull("stream");
            }

            Log.i(TAG, "  + Type      : " + type);
            source.putString("type", type.getName());

            Log.i(TAG, "  + Device    : " + device);
            if (device != null) {
                source.putString("deviceManufacturer", device.getManufacturer());
                source.putString("deviceModel", device.getModel());
                switch(device.getType()) {
                    case Device.TYPE_CHEST_STRAP:
                        source.putString("deviceType", "chestStrap"); break;
                }
            } else {
                source.putNull("deviceManufacturer");
                source.putNull("deviceModel");
                source.putNull("deviceType");
            }

            //if (!DataType.TYPE_STEP_COUNT_DELTA.equals(type)) continue;
            DataReadRequest readRequest;

            List<DataType> aggregateDataTypeList = DataType.getAggregatesForInput(type);
            if (aggregateDataTypeList.size() > 0) {
                DataType aggregateType = aggregateDataTypeList.get(0);
                Log.i(TAG, "  + Aggregate : " + aggregateType);

                //Check how many steps were walked and recorded in specified days
                readRequest = new DataReadRequest.Builder()
                        .aggregate(dataSource
                            //DataType.TYPE_STEP_COUNT_DELTA
                            ,
                            //DataType.AGGREGATE_STEP_COUNT_DELTA
                            aggregateType)
                        .bucketByTime(bucketTime, bucketUnit)
                        .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                        .build();
            } else {
                readRequest = new DataReadRequest.Builder()
                        .read(dataSource)
                        //.bucketByTime(12, TimeUnit.HOURS) // Half-day resolution
                        .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                        .build();
            }

            GoogleSignInOptionsExtension fitnessOptions =
                    FitnessOptions.builder()
                            .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
                            .build();

            GoogleSignInAccount googleSignInAccount =
                    GoogleSignIn.getAccountForExtension(this.mReactContext, fitnessOptions);

            Fitness.getHistoryClient(this.mReactContext, googleSignInAccount)
                .readData(readRequest)
                    .addOnSuccessListener(new OnSuccessListener<DataReadResponse>() {
                        @Override
                        public void onSuccess(DataReadResponse dataReadResponse) {
                            Log.i(TAG, "onSuccess()");
                            WritableArray steps = Arguments.createArray();

                            //Used for aggregated data
                            if (dataReadResponse.getBuckets().size() > 0) {
                                Log.i(TAG, "  +++ Number of buckets: " + dataReadResponse.getBuckets().size());
                                for (Bucket bucket : dataReadResponse.getBuckets()) {
                                    List<DataSet> dataSets = bucket.getDataSets();
                                    for (DataSet dataSet : dataSets) {
                                        processDataSet(dataSet, steps);
                                    }
                                }
                            }

                            //Used for non-aggregated data
                            if (dataReadResponse.getDataSets().size() > 0) {
                                Log.i(TAG, "  +++ Number of returned DataSets: " + dataReadResponse.getDataSets().size());
                                for (DataSet dataSet : dataReadResponse.getDataSets()) {
                                    processDataSet(dataSet, steps);
                                }
                            }

                            WritableMap map = Arguments.createMap();
                            map.putMap("source", source);
                            map.putArray("steps", steps);
                            results.pushMap(map);

                            if (dataSourcesToLoad.decrementAndGet() <= 0) {
                                successCallback.invoke(results);
                            }
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.i(TAG, "onFailure()");
                            Log.i(TAG, "Error" + e);
                    }
            });

        }
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
