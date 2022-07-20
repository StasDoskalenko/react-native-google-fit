package com.reactnative.googlefit;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactContext;
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

    public static String getAppName(PackageManager pm, String packageName) {
        ApplicationInfo ai = null;
        try {
            ai = pm.getApplicationInfo(packageName, 0);
        } catch (final NameNotFoundException e) {
            return null;
        }
        return (String) pm.getApplicationLabel(ai);
    }

    public static String getDeviceType(Device device) {
        switch (device.getType()) {
            case Device.TYPE_PHONE: return "phone";
            case Device.TYPE_WATCH: return "watch";
            case Device.TYPE_TABLET: return "tablet";
            case Device.TYPE_CHEST_STRAP: return "chest-strap";
            case Device.TYPE_HEAD_MOUNTED: return "head-mounted";
            case Device.TYPE_SCALE: return "scale";
            case Device.TYPE_UNKNOWN: return "unknown";
        }; 
        return "unknown";
    }

    public static void processDataSet(ReactContext reactContext, String TAG, DataSet dataSet, WritableArray wtArray) {
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

                String appPackageName = dp.getOriginalDataSource().getAppPackageName();
                if (appPackageName != null) {
                    innerMap.putString("appPackageName", appPackageName);
                    PackageManager pm = reactContext.getPackageManager();
                    String appName = getAppName(pm, appPackageName);
                    if (appName != null) {
                        innerMap.putString("appName", appName);
                    }
                }

                Device device = dp.getOriginalDataSource().getDevice();
                if (device != null) {
                    innerMap.putString("deviceUid", device.getUid());
                    innerMap.putString("deviceManufacturer", device.getManufacturer());
                    innerMap.putString("deviceModel", device.getModel());
                    innerMap.putString("deviceType", getDeviceType(device));
                }

                innerMap.putDouble("startDate", dp.getStartTime(TimeUnit.MILLISECONDS));
                innerMap.putDouble("endDate", dp.getEndTime(TimeUnit.MILLISECONDS));
                innerMap.putDouble(field.getName(), dp.getValue(field).asInt());
                wtArray.pushMap(innerMap);
            }
        }
    }
}
