/**
 * Copyright (c) 2017-present, Stanislav Doskalenko - doskalenko.s@gmail.com
 * All rights reserved.
 *
 * This source code is licensed under the MIT-style license found in the
 * LICENSE file in the root directory of this source tree.
 *
 **/

package com.reactnative.googlefit;

import android.os.AsyncTask;
import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.data.Bucket;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataSource;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.HealthDataTypes;
import com.google.android.gms.fitness.request.DataDeleteRequest;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.result.DataReadResult;

import java.text.DateFormat;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.google.android.gms.fitness.data.HealthFields.FIELD_BLOOD_PRESSURE_DIASTOLIC;
import static com.google.android.gms.fitness.data.HealthFields.FIELD_BLOOD_PRESSURE_SYSTOLIC;


public class BloodPressureHistory {

    private ReactContext mReactContext;
    private GoogleFitManager googleFitManager;
    private DataSet Dataset;
    private final DataType dataType = HealthDataTypes.TYPE_BLOOD_PRESSURE;

    private static final String TAG = "Weights History";

    public BloodPressureHistory(ReactContext reactContext, GoogleFitManager googleFitManager){
        this.mReactContext = reactContext;
        this.googleFitManager = googleFitManager;
    }


    public ReadableArray getHistory(long startTime, long endTime) {
        DataReadRequest.Builder readRequestBuilder = new DataReadRequest.Builder()
                .read(this.dataType)
                .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS);

        readRequestBuilder.bucketByTime(1, TimeUnit.DAYS);

        DataReadRequest readRequest = readRequestBuilder.build();

        DataReadResult dataReadResult = Fitness.HistoryApi.readData(googleFitManager.getGoogleApiClient(), readRequest).await(1, TimeUnit.MINUTES);

        WritableArray map = Arguments.createArray();

        //Used for aggregated data
        if (dataReadResult.getBuckets().size() > 0) {
            for (Bucket bucket : dataReadResult.getBuckets()) {
                List<DataSet> dataSets = bucket.getDataSets();
                for (DataSet dataSet : dataSets) {
                    processDataSet(dataSet, map);
                }
            }
        }
        //Used for non-aggregated data
        else if (dataReadResult.getDataSets().size() > 0) {
            for (DataSet dataSet : dataReadResult.getDataSets()) {
                processDataSet(dataSet, map);
            }
        }
        return map;
    }

    public boolean save(ReadableMap sample) {
        this.Dataset = createDataForRequest(
                this.dataType,
                DataSource.TYPE_RAW,
                (float) sample.getDouble("systolic"),
                (float)sample.getDouble("diastolic"),
                (long)sample.getDouble("date"),
                TimeUnit.MILLISECONDS
        );
        new InsertAndVerifyDataTask(this.Dataset).execute();

        return true;
    }

    public boolean delete(ReadableMap sample) {
        long endTime = (long) sample.getDouble("endTime");
        long startTime = (long) sample.getDouble("startTime");
        new DeleteDataTask(startTime, endTime, this.dataType).execute();
        return true;
    }

    //Async fit data delete
    private class DeleteDataTask extends AsyncTask<Void, Void, Void> {

        long startTime;
        long endTime;
        DataType dataType;

        DeleteDataTask(long startTime, long endTime, DataType dataType) {
            this.startTime = startTime;
            this.endTime = endTime;
        }

        @Override
        protected Void doInBackground(Void... params) {

            DataDeleteRequest request = new DataDeleteRequest.Builder()
                    .setTimeInterval(startTime, endTime, TimeUnit.MILLISECONDS)
                    .addDataType(this.dataType)
                    .build();

            com.google.android.gms.common.api.Status insertStatus =
                    Fitness.HistoryApi.deleteData(googleFitManager.getGoogleApiClient(), request)
                            .await(1, TimeUnit.MINUTES);

            if (insertStatus.isSuccess()) {
                Log.w("myLog", "+Successfully deleted data.");
            } else {
                Log.w("myLog", "+Failed to delete data.");
            }

            return null;
        }
    }

    private class InsertAndVerifyDataTask extends AsyncTask<Void, Void, Void> {

        private DataSet Dataset;

        InsertAndVerifyDataTask(DataSet dataset) {
            this.Dataset = dataset;
        }

        protected Void doInBackground(Void... params) {
            DataSet dataSet = this.Dataset;
            com.google.android.gms.common.api.Status insertStatus =
                    Fitness.HistoryApi.insertData(googleFitManager.getGoogleApiClient(), dataSet)
                            .await(1, TimeUnit.MINUTES);

            if (!insertStatus.isSuccess()) {
                return null;
            }
            return null;
        }
    }

    private DataSet createDataForRequest(DataType dataType, int dataSourceType, float systolic,
                                         float diastolic, long date, TimeUnit timeUnit) {
        DataSource dataSource = new DataSource.Builder()
                .setAppPackageName(GoogleFitPackage.PACKAGE_NAME)
                .setDataType(dataType)
                .setType(dataSourceType)
                .build();

        DataSet dataSet = DataSet.create(dataSource);
        DataPoint bloodPressure = DataPoint.create(dataSource);
        bloodPressure.setTimestamp(date, timeUnit);
        bloodPressure.getValue(FIELD_BLOOD_PRESSURE_SYSTOLIC).setFloat(systolic);
        bloodPressure.getValue(FIELD_BLOOD_PRESSURE_DIASTOLIC).setFloat(diastolic);

        dataSet.add(bloodPressure);

        return dataSet;
    }

    private void processDataSet(DataSet dataSet, WritableArray map) {
        for (DataPoint dp : dataSet.getDataPoints()) {
            WritableMap stepMap = Arguments.createMap();

            stepMap.putDouble("startDate", dp.getStartTime(TimeUnit.MILLISECONDS));
            stepMap.putDouble("endDate", dp.getEndTime(TimeUnit.MILLISECONDS));


            WritableArray measurement = Arguments.createArray();
            measurement.pushDouble(dp.getValue(FIELD_BLOOD_PRESSURE_SYSTOLIC).asFloat());
            measurement.pushDouble(dp.getValue(FIELD_BLOOD_PRESSURE_DIASTOLIC).asFloat());

            stepMap.putArray("value", measurement);

            map.pushMap(stepMap);
        }
    }

}
