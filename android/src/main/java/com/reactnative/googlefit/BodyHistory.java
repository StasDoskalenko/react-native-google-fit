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
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.result.DataReadResult;

import java.text.DateFormat;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;


public class BodyHistory {

    private ReactContext mReactContext;
    private GoogleFitManager googleFitManager;
    private DataSet Dataset;
    private DataType dataType;

    private static final String TAG = "Body History";

    public BodyHistory(ReactContext reactContext, GoogleFitManager googleFitManager, DataType dataType){
        this.mReactContext = reactContext;
        this.googleFitManager = googleFitManager;
        this.dataType = dataType;
    }

    public BodyHistory(ReactContext reactContext, GoogleFitManager googleFitManager){
        this(reactContext, googleFitManager, DataType.TYPE_WEIGHT);
    }

    public void setDataType(DataType dataType) {
        this.dataType = dataType;
    }

    public ReadableArray getHistory(long startTime, long endTime, int bucketInterval, String bucketUnit) {
        // for height we need to take time, since GoogleFit foundation - https://stackoverflow.com/questions/28482176/read-the-height-in-googlefit-in-android
        startTime = this.dataType == DataType.TYPE_WEIGHT ? startTime : 1401926400;
        DataReadRequest.Builder readRequestBuilder = new DataReadRequest.Builder()
                .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS);

        if (this.dataType == DataType.TYPE_WEIGHT) {

            // old method:
            // In general here we want to set the bucket size to the smallest possible allowed, in case the
            // user weighs themselves in a short interval (e.g. before and after a meal)
            //
            // The Google Fit API seems to have a limit of around 3,000 as the maximum number of buckets that
            // can be returned in an aggregated query - anything more than this and the fitness API takes
            // ages to respond and/or no response at all on both Galaxy S5 (6.0.1) and Huawei P9 Lite (7.0)
            //
            // So, divide the time range by 2,000 to be on the safe side
//            long bucketSizeMillis = (endTime - startTime) / 2000;
//
//            // We don't need any finer granularity than 1 minute, so make buckets at least this size to keep
//            // the number of buckets low if not much time has elapsed since the last query
//            bucketSizeMillis = Math.max(bucketSizeMillis, 60 * 1000);


            // new method:
            // let users to provide their own bucket size, handle the limit risk by themselves so they are able to
            // get the most accurate data for their own need.

            readRequestBuilder
                .bucketByTime(bucketInterval, HelperUtil.processBucketUnit(bucketUnit))
                .aggregate(this.dataType, DataType.AGGREGATE_WEIGHT_SUMMARY);

        } else {
            readRequestBuilder.read(this.dataType);
            readRequestBuilder.setLimit(1); // need only one height, since it's unchangable
        }

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
                this.dataType,    // for height, it would be DataType.TYPE_HEIGHT
                DataSource.TYPE_RAW,
                sample.getDouble("value"),                  // weight in kgs, height in metrs
                (long)sample.getDouble("date"),              // start time
                (long)sample.getDouble("date"),                // end time
                TimeUnit.MILLISECONDS                // Time Unit, for example, TimeUnit.MILLISECONDS
        );
        new InsertAndVerifyDataTask(this.Dataset).execute();

        return true;
    }

    public boolean delete(ReadableMap sample) {
        long endTime = (long) sample.getDouble("endDate");
        long startTime = (long) sample.getDouble("startDate");
        new DeleteDataHelper(startTime, endTime, this.dataType, googleFitManager).execute();
        return true;
    }

    //Async fit data insert
    private class InsertAndVerifyDataTask extends AsyncTask<Void, Void, Void> {

        private DataSet Dataset;

        InsertAndVerifyDataTask(DataSet dataset) {
            this.Dataset = dataset;
        }

        protected Void doInBackground(Void... params) {
            // Create a new dataset and insertion request.
            DataSet dataSet = this.Dataset;

            // [START insert_dataset]
            // Then, invoke the History API to insert the data and await the result, which is
            // possible here because of the {@link AsyncTask}. Always include a timeout when calling
            // await() to prevent hanging that can occur from the service being shutdown because
            // of low memory or other conditions.
            //Log.i(TAG, "Inserting the dataset in the History API.");
            com.google.android.gms.common.api.Status insertStatus =
                    Fitness.HistoryApi.insertData(googleFitManager.getGoogleApiClient(), dataSet)
                            .await(1, TimeUnit.MINUTES);

            // Before querying the data, check to see if the insertion succeeded.
            if (!insertStatus.isSuccess()) {
                //Log.i(TAG, "There was a problem inserting the dataset.");
                return null;
            }

            //Log.i(TAG, "Data insert was successful!");

            return null;
        }
    }

    /**
     * This method creates a dataset object to be able to insert data in google fit
     * @param dataType DataType Fitness Data Type object
     * @param dataSourceType int Data Source Id. For example, DataSource.TYPE_RAW
     * @param value Object Values for the fitness data. They must be int or float
     * @param startTime long Time when the fitness activity started
     * @param endTime long Time when the fitness activity finished
     * @param timeUnit TimeUnit Time unit in which period is expressed
     * @return
     */
    private DataSet createDataForRequest(DataType dataType, int dataSourceType, Double value,
                                         long startTime, long endTime, TimeUnit timeUnit) {
        DataSource dataSource = new DataSource.Builder()
                .setAppPackageName(GoogleFitPackage.PACKAGE_NAME)
                .setDataType(dataType)
                .setType(dataSourceType)
                .build();

        DataSet dataSet = DataSet.create(dataSource);
        DataPoint dataPoint = dataSet.createDataPoint().setTimeInterval(startTime, endTime, timeUnit);

        float f1 = Float.valueOf(value.toString());
        dataPoint = dataPoint.setFloatValues(f1);

        dataSet.add(dataPoint);

        return dataSet;
    }

    private void processDataSet(DataSet dataSet, WritableArray map) {
        //Log.i(TAG, "Data returned for Data type: " + dataSet.getDataType().getName());
        Format formatter = new SimpleDateFormat("EEE");

        WritableMap bodyMap = Arguments.createMap();

        for (DataPoint dp : dataSet.getDataPoints()) {
            String day = formatter.format(new Date(dp.getStartTime(TimeUnit.MILLISECONDS)));

            bodyMap.putString("day", day);
            bodyMap.putDouble("startDate", dp.getStartTime(TimeUnit.MILLISECONDS));
            bodyMap.putDouble("endDate", dp.getEndTime(TimeUnit.MILLISECONDS));
            bodyMap.putString("addedBy", dp.getOriginalDataSource().getAppPackageName());

            // When there is a short interval between weight readings (< 1 hour or so), some phones e.g.
            // Galaxy S5 use the average of the readings, whereas other phones e.g. Huawei P9 Lite use the
            // most recent of the bunch (this might be related to Android versions - 6.0.1 vs 7.0 in this
            // example for former and latter)
            //
            // For aggregated weight summary, only the min, max and average values are available (i.e. the
            // most recent sample is not an option), so use average value to maximise the match between values
            // returned here and values as reported by Google Fit app
            if (this.dataType == DataType.TYPE_WEIGHT) {
                bodyMap.putDouble("value", dp.getValue(Field.FIELD_AVERAGE).asFloat());
            } else {
                bodyMap.putDouble("value", dp.getValue(Field.FIELD_HEIGHT).asFloat());
            }
        }
        map.pushMap(bodyMap);
    }

}
