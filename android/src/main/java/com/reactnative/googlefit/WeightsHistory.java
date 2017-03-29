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
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.data.Bucket;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataSource;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.request.DataDeleteRequest;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.result.DataReadResult;

import java.text.DateFormat;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;


public class WeightsHistory {

    private ReactContext mReactContext;
    private GoogleFitManager googleFitManager;
    private DataSet WeightsDataset;

    private static final String TAG = "Weights History";

    public WeightsHistory(ReactContext reactContext, GoogleFitManager googleFitManager){
        this.mReactContext = reactContext;
        this.googleFitManager = googleFitManager;
    }

    public ReadableArray displayLastWeeksData(long startTime, long endTime) {
        DateFormat dateFormat = DateFormat.getDateInstance();
        //Log.i(TAG, "Range Start: " + dateFormat.format(startTime));
        //Log.i(TAG, "Range End: " + dateFormat.format(endTime));

        //Check how many steps were walked and recorded in the last 7 days
        DataReadRequest readRequest = new DataReadRequest.Builder()
                .aggregate(DataType.TYPE_WEIGHT, DataType.AGGREGATE_WEIGHT_SUMMARY)
                .bucketByTime(1, TimeUnit.DAYS)
                .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                .build();


        DataReadResult dataReadResult = Fitness.HistoryApi.readData(googleFitManager.getGoogleApiClient(), readRequest).await(1, TimeUnit.MINUTES);


        WritableArray map = Arguments.createArray();

        //Used for aggregated data
        if (dataReadResult.getBuckets().size() > 0) {
            //Log.i(TAG, "Number of buckets: " + dataReadResult.getBuckets().size());
            for (Bucket bucket : dataReadResult.getBuckets()) {
                List<DataSet> dataSets = bucket.getDataSets();
                for (DataSet dataSet : dataSets) {
                    processDataSet(dataSet, map);
                }
            }
        }
        //Used for non-aggregated data
        else if (dataReadResult.getDataSets().size() > 0) {
            //Log.i(TAG, "Number of returned DataSets: " + dataReadResult.getDataSets().size());
            for (DataSet dataSet : dataReadResult.getDataSets()) {
                processDataSet(dataSet, map);
            }
        }

        //Log.i("Returnable", map.toString());

        return map;
    }

    public boolean saveWeight(ReadableMap weightSample) {
        this.WeightsDataset = createDataForRequest(
                DataType.TYPE_WEIGHT,    // for height, it would be DataType.TYPE_HEIGHT
                DataSource.TYPE_RAW,
                weightSample.getDouble("value"),                  // weight in kgs
                (long)weightSample.getDouble("date"),              // start time
                (long)weightSample.getDouble("date"),                // end time
                TimeUnit.MILLISECONDS                // Time Unit, for example, TimeUnit.MILLISECONDS
        );
        new InsertAndVerifyDataTask(this.WeightsDataset).execute();

        return true;
    }

    public boolean deleteWeight(ReadableMap weightSample) {

        DateFormat dateFormat = DateFormat.getDateInstance();

        long endTime = (long) weightSample.getDouble("endTime");
        long startTime = (long) weightSample.getDouble("startTime");

        new DeleteDataTask(startTime, endTime).execute();

        return true;
    }

    //Async fit data delete
    private class DeleteDataTask extends AsyncTask<Void, Void, Void> {

        long startTime;
        long endTime;

        DeleteDataTask(long startTime, long endTime) {
            this.startTime = startTime;
            this.endTime = endTime;
        }

        @Override
        protected Void doInBackground(Void... params) {

            DataDeleteRequest request = new DataDeleteRequest.Builder()
                    .setTimeInterval(startTime, endTime, TimeUnit.MILLISECONDS)
                    .addDataType(DataType.TYPE_WEIGHT)
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


    //Async fit data insert
    private class InsertAndVerifyDataTask extends AsyncTask<Void, Void, Void> {

        private DataSet WeightsDataset;

        InsertAndVerifyDataTask(DataSet dataset) {
            this.WeightsDataset = dataset;
        }

        protected Void doInBackground(Void... params) {
            // Create a new dataset and insertion request.
            DataSet dataSet = this.WeightsDataset;

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

        WritableMap stepMap = Arguments.createMap();


        for (DataPoint dp : dataSet.getDataPoints()) {
            String day = formatter.format(new Date(dp.getStartTime(TimeUnit.MILLISECONDS)));

            int i = 0;

            for (Field field : dp.getDataType().getFields()) {
                i++;
                if (i > 1) continue; //Get only average instance

                stepMap.putString("day", day);
                stepMap.putDouble("startDate", dp.getStartTime(TimeUnit.MILLISECONDS));
                stepMap.putDouble("endDate", dp.getEndTime(TimeUnit.MILLISECONDS));
                stepMap.putDouble("value", dp.getValue(field).asFloat());
            }
        }
        map.pushMap(stepMap);
    }

}
