/**
 * Copyright (c) 2017-present, Stanislav Doskalenko - doskalenko.s@gmail.com
 * All rights reserved.
 * <p>
 * This source code is licensed under the MIT-style license found in the
 * LICENSE file in the root directory of this source tree.
 * <p>
 * Based on Asim Malik android source code, copyright (c) 2015
 **/

package com.reactnative.googlefit;

import android.os.AsyncTask;
import android.service.autofill.Dataset;
import android.util.Log;

import com.facebook.common.internal.Supplier;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.Promise;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.data.*;
import com.google.android.gms.fitness.request.DataDeleteRequest;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.request.DataUpdateRequest;
import com.google.android.gms.fitness.result.DataReadResult;

import org.json.JSONObject;

import java.text.DateFormat;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;


public class CalorieHistory {
    private ReactContext mReactContext;
    private GoogleFitManager googleFitManager;

    private static final String TAG = "CalorieHistory";

    public CalorieHistory(ReactContext reactContext, GoogleFitManager googleFitManager) {
        this.mReactContext = reactContext;
        this.googleFitManager = googleFitManager;
    }

    public ReadableArray aggregateDataByDate(long startTime, long endTime) {

        DateFormat dateFormat = DateFormat.getDateInstance();
        Log.i(TAG, "Range Start: " + dateFormat.format(startTime));
        Log.i(TAG, "Range End: " + dateFormat.format(endTime));

        //Check how much calories were expended in specific days.
        DataReadRequest readRequest = new DataReadRequest.Builder()
                .aggregate(DataType.TYPE_CALORIES_EXPENDED, DataType.AGGREGATE_CALORIES_EXPENDED)
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

        return map;
    }

    public ReadableArray aggregateNutritionDataByDate(long startTime, long endTime) {
        DateFormat dateFormat = DateFormat.getDateInstance();
        Log.i(TAG, "Range Start: " + dateFormat.format(startTime));
        Log.i(TAG, "Range End: " + dateFormat.format(endTime));

        //Check how much calories were expended in specific days.
        DataReadRequest readRequest = new DataReadRequest.Builder()
                .aggregate(DataType.TYPE_NUTRITION, DataType.AGGREGATE_NUTRITION_SUMMARY)
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
                    processNutritionDataSet(dataSet, map);
                }
            }
        }
        //Used for non-aggregated data
        else if (dataReadResult.getDataSets().size() > 0) {
            Log.i(TAG, "Number of returned DataSets: " + dataReadResult.getDataSets().size());
            for (DataSet dataSet : dataReadResult.getDataSets()) {
                processNutritionDataSet(dataSet, map);
            }
        }

        return map;

    }
    // utility function that gets the basal metabolic rate averaged over a week
    private float getBasalAVG(long _et) throws Exception {
        float basalAVG = 0;
        Calendar cal = java.util.Calendar.getInstance();
        cal.setTime(new Date(_et));
        //set start time to a week before end time
        cal.add(Calendar.WEEK_OF_YEAR, -1);
        long nst = cal.getTimeInMillis();

        DataReadRequest.Builder builder = new DataReadRequest.Builder();
        builder.aggregate(DataType.TYPE_BASAL_METABOLIC_RATE, DataType.AGGREGATE_BASAL_METABOLIC_RATE_SUMMARY);
        builder.bucketByTime(1, TimeUnit.DAYS);
        builder.setTimeRange(nst, _et, TimeUnit.MILLISECONDS);
        DataReadRequest readRequest = builder.build();

        DataReadResult dataReadResult = Fitness.HistoryApi.readData(googleFitManager.getGoogleApiClient(), readRequest).await();

        if (dataReadResult.getStatus().isSuccess()) {
            JSONObject obj = new JSONObject();
            int avgsN = 0;
            for (Bucket bucket : dataReadResult.getBuckets()) {
                // in the com.google.bmr.summary data type, each data point represents
                // the average, maximum and minimum basal metabolic rate, in kcal per day, over the time interval of the data point.
                DataSet ds = bucket.getDataSet(DataType.AGGREGATE_BASAL_METABOLIC_RATE_SUMMARY);
                for (DataPoint dp : ds.getDataPoints()) {
                    float avg = dp.getValue(Field.FIELD_AVERAGE).asFloat();
                    basalAVG += avg;
                    avgsN++;
                }
            }
            // do the average of the averages
            if (avgsN != 0) basalAVG /= avgsN; // this a daily average
            return basalAVG;
        } else throw new Exception(dataReadResult.getStatus().getStatusMessage());
    }


    private void processDataSet(DataSet dataSet, WritableArray map) {
        Log.i(TAG, "Data returned for Data type: " + dataSet.getDataType().getName());
        DateFormat dateFormat = DateFormat.getDateInstance();
        DateFormat timeFormat = DateFormat.getTimeInstance();
        Format formatter = new SimpleDateFormat("EEE");
        WritableMap stepMap = Arguments.createMap();


        for (DataPoint dp : dataSet.getDataPoints()) {
            Log.i(TAG, "Data point:");
            Log.i(TAG, "\tType: " + dp.getDataType().getName());
            Log.i(TAG, "\tStart: " + dateFormat.format(dp.getStartTime(TimeUnit.MILLISECONDS)) + " " + timeFormat.format(dp.getStartTime(TimeUnit.MILLISECONDS)));
            Log.i(TAG, "\tEnd: " + dateFormat.format(dp.getEndTime(TimeUnit.MILLISECONDS)) + " " + timeFormat.format(dp.getStartTime(TimeUnit.MILLISECONDS)));

            String day = formatter.format(new Date(dp.getStartTime(TimeUnit.MILLISECONDS)));
            Log.i(TAG, "Day: " + day);

            for (Field field : dp.getDataType().getFields()) {
                Log.i("History", "\tField: " + field.getName() +
                        " Value: " + dp.getValue(field));

                stepMap.putString("day", day);
                stepMap.putDouble("startDate", dp.getStartTime(TimeUnit.MILLISECONDS));
                stepMap.putDouble("endDate", dp.getEndTime(TimeUnit.MILLISECONDS));
                float basal = 0;
                try {
                    basal = getBasalAVG(dp.getEndTime(TimeUnit.MILLISECONDS));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                stepMap.putDouble("calorie", dp.getValue(field).asFloat() - basal);
                map.pushMap(stepMap);
            }
        }
    }

    final String[] NUTRIENTS = {
        Field.NUTRIENT_CALORIES,
        Field.NUTRIENT_TOTAL_FAT,
        Field.NUTRIENT_SATURATED_FAT,
        Field.NUTRIENT_UNSATURATED_FAT,
        Field.NUTRIENT_POLYUNSATURATED_FAT,
        Field.NUTRIENT_MONOUNSATURATED_FAT,
        Field.NUTRIENT_TRANS_FAT,
        Field.NUTRIENT_CHOLESTEROL,
        Field.NUTRIENT_SODIUM,
        Field.NUTRIENT_POTASSIUM,
        Field.NUTRIENT_TOTAL_CARBS,
        Field.NUTRIENT_DIETARY_FIBER,
        Field.NUTRIENT_SUGAR,
        Field.NUTRIENT_PROTEIN,
        Field.NUTRIENT_VITAMIN_A,
        Field.NUTRIENT_VITAMIN_C,
        Field.NUTRIENT_CALCIUM,
        Field.NUTRIENT_IRON
    };

    public void processNutritionDataSet(DataSet dataSet, WritableArray map) {
        Log.i(TAG, "Data returned for Data type: " + dataSet.getDataType().getName());
        DateFormat dateFormat = DateFormat.getDateInstance();
        DateFormat timeFormat = DateFormat.getTimeInstance();
        DateFormat simpleDate  = new SimpleDateFormat("yyyy-MM-dd");

        for (DataPoint dp : dataSet.getDataPoints()) {
            WritableMap dataPointMap = Arguments.createMap();

            //Log.i(TAG, "Data point:");
            //Log.i(TAG, "\tType: " + dp.getDataType().getName());
            //Log.i(TAG, "\tStart: " + dateFormat.format(dp.getStartTime(TimeUnit.MILLISECONDS)) + " " + timeFormat.format(dp.getStartTime(TimeUnit.MILLISECONDS)));
            //Log.i(TAG, "\tEnd: " + dateFormat.format(dp.getEndTime(TimeUnit.MILLISECONDS)) + " " + timeFormat.format(dp.getStartTime(TimeUnit.MILLISECONDS)));

            String day = simpleDate.format(new Date(dp.getStartTime(TimeUnit.MILLISECONDS)));
            Log.i(TAG, "Day: " + day);

            dataPointMap.putString("date", day);
            dataPointMap.putDouble("startDate", dp.getStartTime(TimeUnit.MILLISECONDS));
            dataPointMap.putDouble("endDate", dp.getEndTime(TimeUnit.MILLISECONDS));

            WritableMap nutrientsMap = Arguments.createMap();
            boolean nutrientsMapMutated  = false;
            for (Field field : dp.getDataType().getFields()) {
                final Value fieldValue = dp.getValue(field);
                //Log.i("History", "\tField: " + field.getName() + " Value: " + fieldValue);
                if (field.equals(Field.FIELD_NUTRIENTS)) {
                    for (final String nutrientName : this.NUTRIENTS) {
                        Float nutrientValue = fieldValue.getKeyValue(nutrientName);
                        if (nutrientValue != null) {
                            nutrientsMap.putDouble(nutrientName, nutrientValue.doubleValue());
                            nutrientsMapMutated = true;
                        }
                    }
                }
            }
            if (nutrientsMapMutated) {
                dataPointMap.putMap("nutrients", nutrientsMap);
                map.pushMap(dataPointMap);
            }
        }
    }

    private class ExecuteAndVerifyDataTask extends  AsyncTask<Void, Void, Void> {
        private Supplier<PendingResult<com.google.android.gms.common.api.Status>> task;
        private final Promise promise;

        ExecuteAndVerifyDataTask(
            Supplier<PendingResult<com.google.android.gms.common.api.Status>> task,
            final Promise promise) {
            this.task = task;
            this.promise = promise;
        }

        protected Void doInBackground(Void... params) {
            Log.d(TAG, "Executing the data task on the history API.");
            //Always include a timeout when calling await() to prevent hanging that can occur from
            // the service being shutdown because of low memory or other conditions.
            com.google.android.gms.common.api.Status status = this.task.get().await(
                1,
                TimeUnit.MINUTES);
            if (status.isSuccess()) {
                Log.d(TAG, "Data task completed");
                this.promise.resolve(true);
            } else {
                String statusMessage = status.getStatusMessage();
                this.promise.reject(
                    Integer.toString(status.getStatusCode()),
                    statusMessage);
                Log.e(TAG, "The data task is done with errors.");
                Log.e(TAG, statusMessage);
            }
            return null;
        }
    }

    private DataSet foodSampleToDataSet(ReadableMap foodSample) {
        return createDataForRequest(
                foodSample.getMap("nutrients").toHashMap(),
                foodSample.getInt("mealType"),                  // meal type
                foodSample.getString("foodName"),               // food name
                (long)foodSample.getDouble("date"),             // start time
                (long)foodSample.getDouble("date"),             // end time
                TimeUnit.MILLISECONDS                // Time Unit, for example, TimeUnit.MILLISECONDS
        );
    }

    public void saveFood(ReadableMap foodSample, final Promise promise) {
        final DataSet foodDataSet =  foodSampleToDataSet(foodSample);
        new ExecuteAndVerifyDataTask(
            new Supplier<PendingResult<com.google.android.gms.common.api.Status>>() {
                @Override
                public PendingResult<com.google.android.gms.common.api.Status> get() {
                    return Fitness.HistoryApi.insertData(
                        googleFitManager.getGoogleApiClient(),
                        foodDataSet);
                }
            },
            promise
        ).execute();
    }

    public void updateFood(ReadableMap foodSample, final Promise promise) {
        long time = (long)foodSample.getDouble("date");
        final DataUpdateRequest request = new DataUpdateRequest.Builder()
            .setDataSet(this.foodSampleToDataSet(foodSample))
            .setTimeInterval(time, time, TimeUnit.MILLISECONDS)
            .build();
        new ExecuteAndVerifyDataTask(
            new Supplier<PendingResult<com.google.android.gms.common.api.Status>>() {
                @Override
                public PendingResult<com.google.android.gms.common.api.Status> get() {
                    return Fitness.HistoryApi.updateData(
                        googleFitManager.getGoogleApiClient(),
                        request);
                }
            },
            promise
        ).execute();
    }

    public void deleteFood(ReadableMap options, final Promise promise) {
        long startTime = (long)options.getDouble("startDate");
        long endTime = (long)options.getDouble("endDate");

        // The start and end time must not be the same or else it will throw an error. The fix is to
        // add 1 to the endtime.
        final DataDeleteRequest request = new DataDeleteRequest.Builder()
            .addDataSource(this.createNutritionDataSource())
            .setTimeInterval(startTime, endTime, TimeUnit.MILLISECONDS)
            .build();
        new ExecuteAndVerifyDataTask(
            new Supplier<PendingResult<com.google.android.gms.common.api.Status>>() {
                @Override
                public PendingResult<com.google.android.gms.common.api.Status> get() {
                    return Fitness.HistoryApi.deleteData(
                        googleFitManager.getGoogleApiClient(),
                        request);
                }
            },
            promise).execute();
    }

    private DataSource createNutritionDataSource() {
        return new DataSource.Builder()
                .setAppPackageName(GoogleFitPackage.PACKAGE_NAME)
                .setDataType(DataType.TYPE_NUTRITION)
                .setType(DataSource.TYPE_RAW)
                .build();
    }

    /**
     * This method creates a dataset object to be able to insert data in google fit
     *
     * @param values         Object Values for the fitness data. They must be HashMap
     * @param mealType       int Value of enum. For example Field.MEAL_TYPE_SNACK
     * @param name           String Dish name. For example "banana"
     * @param startTime      long Time when the fitness activity started
     * @param endTime        long Time when the fitness activity finished
     * @param timeUnit       TimeUnit Time unit in which period is expressed
     * @return
     */
    private DataSet createDataForRequest(HashMap<String, Object> values, int mealType, String name,
                                         long startTime, long endTime, TimeUnit timeUnit) {

        DataSource dataSource = this.createNutritionDataSource();

        DataSet dataSet = DataSet.create(dataSource);
        DataPoint dataPoint = dataSet.createDataPoint().setTimeInterval(startTime, endTime, timeUnit);

        dataPoint.getValue(Field.FIELD_FOOD_ITEM).setString(name);
        dataPoint.getValue(Field.FIELD_MEAL_TYPE).setInt(mealType);
        for (String key : values.keySet()) {
            Float value = Float.valueOf(values.get(key).toString());

            if (value > 0) {
                dataPoint.getValue(Field.FIELD_NUTRIENTS).setKeyValue(key, value);
            }
        }

        dataSet.add(dataPoint);

        return dataSet;
    }
}