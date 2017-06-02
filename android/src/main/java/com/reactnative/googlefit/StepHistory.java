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
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.ArrayList;

public class StepHistory {

    private ReactContext mReactContext;
    private GoogleFitManager googleFitManager;

    private static final String TAG = "RNGoogleFit";

    public StepHistory(ReactContext reactContext, GoogleFitManager googleFitManager){
        this.mReactContext = reactContext;
        this.googleFitManager = googleFitManager;
    }

    public ReadableArray aggregateDataByDate(long startTime, long endTime) {

        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        dateFormat.setTimeZone(TimeZone.getDefault());

        Log.i(TAG, "Range Start: " + dateFormat.format(startTime));
        Log.i(TAG, "Range End: " + dateFormat.format(endTime));

        WritableArray results = Arguments.createArray();

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

        for (DataSource dataSource : dataSources) {
            WritableMap source = Arguments.createMap();

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

            if (dataSource.getName() != null) {
                source.putString("name", dataSource.getName());
            } else {
                source.putNull("name");
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
                        .bucketByTime(12, TimeUnit.HOURS) // Half-day resolution
                        .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                        .build();
            } else {
                readRequest = new DataReadRequest.Builder()
                        .read(dataSource)
                        //.bucketByTime(12, TimeUnit.HOURS) // Half-day resolution
                        .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                        .build();
            }

            DataReadResult dataReadResult = Fitness.HistoryApi.readData(googleFitManager.getGoogleApiClient(), readRequest).await(1, TimeUnit.MINUTES);

            WritableArray steps = Arguments.createArray();

            //Used for aggregated data
            if (dataReadResult.getBuckets().size() > 0) {
                Log.i(TAG, "  +++ Number of buckets: " + dataReadResult.getBuckets().size());
                for (Bucket bucket : dataReadResult.getBuckets()) {
                    List<DataSet> dataSets = bucket.getDataSets();
                    for (DataSet dataSet : dataSets) {
                        processDataSet(dataSet, steps);
                    }
                }
            }

            //Used for non-aggregated data
            if (dataReadResult.getDataSets().size() > 0) {
                Log.i(TAG, "  +++ Number of returned DataSets: " + dataReadResult.getDataSets().size());
                for (DataSet dataSet : dataReadResult.getDataSets()) {
                    processDataSet(dataSet, steps);
                }
            }

            WritableMap map = Arguments.createMap();
            map.putMap("source", source);
            map.putArray("steps", steps);
            results.pushMap(map);
        }

        return results;
    }

    //Will be deprecated in future releases
    public void displayLastWeeksData(long startTime, long endTime) {
        DateFormat dateFormat = DateFormat.getDateInstance();
        //Log.i(TAG, "Range Start: " + dateFormat.format(startTime));
        //Log.i(TAG, "Range End: " + dateFormat.format(endTime));

        //Check how many steps were walked and recorded in the last 7 days
        DataReadRequest readRequest = new DataReadRequest.Builder()
                .aggregate(DataType.TYPE_STEP_COUNT_DELTA, DataType.AGGREGATE_STEP_COUNT_DELTA)
                .bucketByTime(1, TimeUnit.DAYS)
                .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                .build();

        DataReadResult dataReadResult = Fitness.HistoryApi.readData(googleFitManager.getGoogleApiClient(), readRequest).await(1, TimeUnit.MINUTES);

        WritableArray map = Arguments.createArray();

        //Used for aggregated data
        if (dataReadResult.getBuckets().size() > 0) {
            Log.i(TAG, "Number of buckets: " + dataReadResult.getBuckets().size());
            for (Bucket bucket : dataReadResult.getBuckets()) {
                List<DataSet> dataSets = bucket.getDataSets();
                for (DataSet dataSet : dataSets) {
                    processDataSet(dataSet, map);
                }
            }
        }
        //Used for non-aggregated data
        else if (dataReadResult.getDataSets().size() > 0) {
            Log.i(TAG, "Number of returned DataSets: " + dataReadResult.getDataSets().size());
            for (DataSet dataSet : dataReadResult.getDataSets()) {
                processDataSet(dataSet, map);
            }
        }

        sendEvent(this.mReactContext, "StepHistoryChangedEvent", map);
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
