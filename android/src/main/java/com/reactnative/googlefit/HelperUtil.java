package com.reactnative.googlefit;

import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.google.android.gms.auth.api.signin.GoogleSignInOptionsExtension;
import com.google.android.gms.fitness.FitnessOptions;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Device;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.request.DataReadRequest;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

final class HelperUtil {
    public static TimeUnit processBucketUnit(String buckUnit) {
        switch (buckUnit){
            case "NANOSECOND": return TimeUnit.NANOSECONDS;
            case "MICROSECOND": return TimeUnit.MICROSECONDS;
            case "MILLISECOND": return TimeUnit.MILLISECONDS;
            case "SECOND": return TimeUnit.SECONDS;
            case "MINUTE": return TimeUnit.MINUTES;
            case "HOUR": return TimeUnit.HOURS;
            case "DAY": return TimeUnit.DAYS;
        }
        return TimeUnit.HOURS;
    }

    public static DataReadRequest createDataReadRequest(long startTime, long endTime, int bucketInterval, String bucketUnit, DataType[] fitnessDataTypes) {
        DataReadRequest.Builder readRequest = new DataReadRequest.Builder();
        for(DataType dt: fitnessDataTypes) {
            readRequest.aggregate(dt);
        }
        readRequest.bucketByTime(bucketInterval, HelperUtil.processBucketUnit(bucketUnit))
                .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS);
        return readRequest.build();
    }

    public static GoogleSignInOptionsExtension createSignInFitnessOptions(DataType fitnessDataType, Integer[] fitnessAccessOptions) {
        FitnessOptions.Builder signInOptionsExtension = FitnessOptions.builder();
        for(Integer accessOpt: fitnessAccessOptions) {
            signInOptionsExtension.addDataType(fitnessDataType, accessOpt);
        }
        return signInOptionsExtension.build();
    }

    public static void processDataSet(String TAG, DataSet dataSet, WritableArray wtArray) {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        dateFormat.setTimeZone(TimeZone.getDefault());

        Log.d(TAG, "Data returned for Data type: " + dataSet.getDataType().getName());
        for (DataPoint dp : dataSet.getDataPoints()) {
            // log debug data
            Log.d(TAG,"Data point:");
            Log.d(TAG,"\tType: "+ dp.getDataType().getName());
            Log.d(TAG,"\tStart: " + dateFormat.format(dp.getStartTime(TimeUnit.MILLISECONDS)));
            Log.d(TAG,"\tEnd: " + dateFormat.format(dp.getEndTime(TimeUnit.MILLISECONDS)));
            for (Field field : dp.getDataType().getFields()) {
                Log.d(TAG,"\tField: " + field.getName() +  " Value: " + dp.getValue(field));

                // add data
                WritableMap innerMap = Arguments.createMap();
                innerMap.putString("dataTypeName", dp.getDataType().getName());
                innerMap.putString("dataSourceId", dp.getDataSource().getStreamIdentifier());
                innerMap.putString("originDataSourceId", dp.getOriginalDataSource().getStreamIdentifier());

                Device device = dp.getOriginalDataSource().getDevice();
                if (device != null) {
                    innerMap.putString("deviceUid", device.getUid());
                    innerMap.putString("deviceManufacturer", device.getManufacturer());
                    innerMap.putString("deviceModel", device.getModel());
                    switch (device.getType()) {
                        case Device.TYPE_PHONE:  
                            innerMap.putString("deviceType", "phone");
                        break;
                        case Device.TYPE_WATCH: 
                            innerMap.putString("deviceType", "watch");
                        break;
                        case Device.TYPE_TABLET: 
                            innerMap.putString("deviceType", "tablet");
                        break;
                        case Device.TYPE_CHEST_STRAP:
                            innerMap.putString("deviceType", "chest-strap");
                        break;
                        case Device.TYPE_HEAD_MOUNTED:
                            innerMap.putString("deviceType", "head-mounted");
                        break;
                        case Device.TYPE_SCALE:
                            innerMap.putString("deviceType", "scale");
                        break;
                        case Device.TYPE_UNKNOWN:
                            innerMap.putString("deviceType", "unknown");
                        break;
                        default: 
                            innerMap.putString("deviceType", "unknown");
                        break;
                    };
                }

                innerMap.putDouble("startDate", dp.getStartTime(TimeUnit.MILLISECONDS));
                innerMap.putDouble("endDate", dp.getEndTime(TimeUnit.MILLISECONDS));
                innerMap.putDouble(field.getName(), dp.getValue(field).asInt());
                wtArray.pushMap(innerMap);
            }
        }
    }
}
