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
import com.google.android.gms.fitness.data.HealthDataTypes;
import com.google.android.gms.fitness.data.HealthFields;
import com.google.android.gms.fitness.data.Value;
import com.google.android.gms.fitness.request.DataDeleteRequest;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.result.DataReadResult;

import org.json.JSONObject;

import java.io.IOException;
import java.text.DateFormat;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Copyright (c) 2019-present, Juangui Jordán - juangui@gmail.com All rights
 * reserved.
 *
 * This source code is licensed under the MIT-style license found in the LICENSE
 * file in the root directory of this source tree.
 *
 **/
public class NutritionHistory {
    private ReactContext mReactContext;
    private GoogleFitManager googleFitManager;
    private DataSet FoodDataSet;

    private static final String TAG = "NutritionHistory";

    public NutritionHistory(ReactContext reactContext, GoogleFitManager googleFitManager) {
        this.mReactContext = reactContext;
        this.googleFitManager = googleFitManager;
    }

    public ReadableArray aggregateDataByDate(long startTime, long endTime) {

        DateFormat dateFormat = DateFormat.getDateInstance();
        Log.i(TAG, "Range Start: " + dateFormat.format(startTime));
        Log.i(TAG, "Range End: " + dateFormat.format(endTime));

        DataReadRequest readRequest = new DataReadRequest.Builder().read(DataType.TYPE_NUTRITION)
                .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS).build();

        DataReadResult dataReadResult = Fitness.HistoryApi.readData(googleFitManager.getGoogleApiClient(), readRequest)
                .await(1, TimeUnit.MINUTES);

        WritableArray map = Arguments.createArray();

        // Used for aggregated data
        if (dataReadResult.getBuckets().size() > 0) {
            Log.i(TAG, "Number of buckets: " + dataReadResult.getBuckets().size());
            for (Bucket bucket : dataReadResult.getBuckets()) {
                List<DataSet> dataSets = bucket.getDataSets();
                for (DataSet dataSet : dataSets) {
                    processDataSet(dataSet, map);
                }
            }
        }
        // Used for non-aggregated data
        else if (dataReadResult.getDataSets().size() > 0) {
            Log.i(TAG, "Number of returned DataSets: " + dataReadResult.getDataSets().size());
            for (DataSet dataSet : dataReadResult.getDataSets()) {
                processDataSet(dataSet, map);
            }
        }

        return map;
    }

    private void processDataSet(DataSet dataSet, WritableArray map) {
        Log.i(TAG, "Data returned for Data type: " + dataSet.getDataType().getName());
        DateFormat dateFormat = DateFormat.getDateInstance();
        DateFormat timeFormat = DateFormat.getTimeInstance();
        Format formatter = new SimpleDateFormat("EEE");

        for (DataPoint dp : dataSet.getDataPoints()) {
            Log.i(TAG, "Data point:");
            Log.i(TAG, "\tType: " + dp.getDataType().getName());
            Log.i(TAG, "\tStart: " + dateFormat.format(dp.getStartTime(TimeUnit.MILLISECONDS)) + " "
                    + timeFormat.format(dp.getStartTime(TimeUnit.MILLISECONDS)));
            Log.i(TAG, "\tEnd: " + dateFormat.format(dp.getEndTime(TimeUnit.MILLISECONDS)) + " "
                    + timeFormat.format(dp.getStartTime(TimeUnit.MILLISECONDS)));

            String foodItem = dp.getValue((Field.FIELD_FOOD_ITEM)).asString();
            Integer mealType = dp.getValue((Field.FIELD_MEAL_TYPE)).asInt();
            String nutrientsJson = dp.getValue((Field.FIELD_NUTRIENTS)).asString();
            ObjectMapper mapper = new ObjectMapper();

            try {

                // convert JSON string to Map
                Map<String, String> nutrientsMap = mapper.readValue(nutrientsJson, Map.class);

                // it works
                // Map<String, String> map = mapper.readValue(json, new
                // TypeReference<Map<String, String>>() {});

                Log.i(TAG, "\tNutrients map: " + nutrientsMap);

            } catch (IOException e) {
                e.printStackTrace();
            }

            WritableMap nutritionMap = Arguments.createMap();

            nutritionMap.putDouble("startDate", dp.getStartTime(TimeUnit.MILLISECONDS));
            nutritionMap.putDouble("endDate", dp.getEndTime(TimeUnit.MILLISECONDS));
            nutritionMap.putString("foodItem", foodItem);
            nutritionMap.putInt("mealType", mealType);

            map.pushMap(nutritionMap);
        }
    }

    public boolean saveFood(ReadableMap foodSample) {
        this.FoodDataSet = createDataForRequest(DataType.TYPE_NUTRITION, // for height, it would be DataType.TYPE_HEIGHT
                DataSource.TYPE_RAW, foodSample.getMap("nutrients").toHashMap(), foodSample.getInt("mealType"), // meal
                                                                                                                // type
                foodSample.getString("foodName"), // food name
                (long) foodSample.getDouble("date"), // start time
                (long) foodSample.getDouble("date"), // end time
                TimeUnit.MILLISECONDS // Time Unit, for example, TimeUnit.MILLISECONDS
        );
        new NutritionHistory.InsertAndVerifyDataTask(this.FoodDataSet).execute();

        return true;
    }

    // Async fit data insert
    private class InsertAndVerifyDataTask extends AsyncTask<Void, Void, Void> {

        private DataSet FoodDataset;

        InsertAndVerifyDataTask(DataSet dataset) {
            this.FoodDataset = dataset;
        }

        protected Void doInBackground(Void... params) {
            // Create a new dataset and insertion request.
            DataSet dataSet = this.FoodDataset;

            // [START insert_dataset]
            // Then, invoke the History API to insert the data and await the result, which
            // is
            // possible here because of the {@link AsyncTask}. Always include a timeout when
            // calling
            // await() to prevent hanging that can occur from the service being shutdown
            // because
            // of low memory or other conditions.
            // Log.i(TAG, "Inserting the dataset in the History API.");
            com.google.android.gms.common.api.Status insertStatus = Fitness.HistoryApi
                    .insertData(googleFitManager.getGoogleApiClient(), dataSet).await(1, TimeUnit.MINUTES);

            // Before querying the data, check to see if the insertion succeeded.
            if (!insertStatus.isSuccess()) {
                // Log.i(TAG, "There was a problem inserting the dataset.");
                return null;
            }

            // Log.i(TAG, "Data insert was successful!");

            return null;
        }
    }

    /**
     * This method creates a dataset object to be able to insert data in google fit
     *
     * @param dataType       DataType Fitness Data Type object
     * @param dataSourceType int Data Source Id. For example, DataSource.TYPE_RAW
     * @param values         Object Values for the fitness data. They must be
     *                       HashMap
     * @param mealType       int Value of enum. For example Field.MEAL_TYPE_SNACK
     * @param name           String Dish name. For example "banana"
     * @param startTime      long Time when the fitness activity started
     * @param endTime        long Time when the fitness activity finished
     * @param timeUnit       TimeUnit Time unit in which period is expressed
     * @return
     */
    private DataSet createDataForRequest(DataType dataType, int dataSourceType, HashMap<String, Object> values,
            int mealType, String name, long startTime, long endTime, TimeUnit timeUnit) {

        DataSource dataSource = new DataSource.Builder().setAppPackageName(GoogleFitPackage.PACKAGE_NAME)
                .setDataType(dataType).setType(dataSourceType).build();

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
