package com.reactnative.googlefit;

import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.data.Bucket;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.data.Value;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.result.DataReadResult;

import java.text.DateFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

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

    private static final String TAG = "NutritionHistory";
    private static final String[] NUTRIENTS_ARRAY = new String[] {
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
    public static final Set<String> NUTRIENTS_SET = new HashSet<>(Arrays.asList(NUTRIENTS_ARRAY));

    public NutritionHistory(ReactContext reactContext, GoogleFitManager googleFitManager) {
        this.mReactContext = reactContext;
        this.googleFitManager = googleFitManager;
    }

    public ReadableArray aggregateDataByDate(long startTime, long endTime, int bucketInterval, String bucketUnit) {

        DateFormat dateFormat = DateFormat.getDateInstance();
        Log.i(TAG, "Range Start: " + dateFormat.format(startTime));
        Log.i(TAG, "Range End: " + dateFormat.format(endTime));

        DataReadRequest readRequest = new DataReadRequest.Builder()
                .aggregate(DataType.TYPE_NUTRITION, DataType.AGGREGATE_NUTRITION_SUMMARY)
                .bucketByTime(bucketInterval, HelperUtil.processBucketUnit(bucketUnit))
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

        for (DataPoint dp : dataSet.getDataPoints()) {
            Log.i(TAG, "Data point:");
            Log.i(TAG, "\tType: " + dp.getDataType().getName());
            Log.i(TAG, "\tStart: " + dateFormat.format(dp.getStartTime(TimeUnit.MILLISECONDS)) + " "
                    + timeFormat.format(dp.getStartTime(TimeUnit.MILLISECONDS)));
            Log.i(TAG, "\tEnd: " + dateFormat.format(dp.getEndTime(TimeUnit.MILLISECONDS)) + " "
                    + timeFormat.format(dp.getStartTime(TimeUnit.MILLISECONDS)));

            WritableMap nutritionMap = Arguments.createMap();
            Value nutrients = dp.getValue((Field.FIELD_NUTRIENTS));

            nutritionMap.putDouble("date", dp.getStartTime(TimeUnit.MILLISECONDS));
            nutritionMap.putMap("nutrients", getNutrientsAsMap(nutrients));

            map.pushMap(nutritionMap);
        }
    }

    private WritableMap getNutrientsAsMap(Value nutrients) {
        WritableMap nutrientsMap = Arguments.createMap();

        for (String nutrientKey : NUTRIENTS_SET) {
            try {
                Float nutrientVal = nutrients.getKeyValue(nutrientKey);
                nutrientsMap.putDouble(nutrientKey, nutrientVal);
            } catch (Exception e) {
            }
        }

        return nutrientsMap;
    }
}
