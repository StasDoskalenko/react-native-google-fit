package com.reactnative.googlefit;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataSource;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.data.Value;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.result.DataReadResult;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class HydrationHistory {
  private ReactContext mReactContext;
  private GoogleFitManager googleFitManager;

  private static final String TAG = "HydrationHistory";
  private static final int MAX_DATAPOINTS_PER_SINGLE_REQUEST = 900;
  private DataType dataType = DataType.TYPE_HYDRATION;

  public HydrationHistory(ReactContext reactContext, GoogleFitManager googleFitManager) {
    this.mReactContext = reactContext;
    this.googleFitManager = googleFitManager;
  }

  private DataSource getDataSource() {
    return new DataSource.Builder()
      .setAppPackageName(GoogleFitPackage.PACKAGE_NAME)
      .setDataType(this.dataType)
      .setStreamName("hydrationSource")
      .setType(DataSource.TYPE_RAW)
      .build();
  }

  public ReadableArray getHistory(long startTime, long endTime) {
    DateFormat dateFormat = DateFormat.getDateInstance();

    DataReadRequest readRequest = new DataReadRequest.Builder()
      .read(this.dataType)
      .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS).build();

    DataReadResult dataReadResult = Fitness.HistoryApi.readData(googleFitManager.getGoogleApiClient(), readRequest)
      .await(1, TimeUnit.MINUTES);

    WritableArray map = Arguments.createArray();

    if (dataReadResult.getDataSets().size() > 0) {
      for (DataSet dataSet : dataReadResult.getDataSets()) {
        processDataSet(dataSet, map);
      }
    }

    return map;
  }

  private void processDataSet(DataSet dataSet, WritableArray map) {
    for (DataPoint dp : dataSet.getDataPoints()) {
      WritableMap hydrationMap = Arguments.createMap();
      Value hydration = dp.getValue((Field.FIELD_VOLUME));

      hydrationMap.putDouble("date", dp.getEndTime(TimeUnit.MILLISECONDS));
      hydrationMap.putDouble("waterConsumed", hydration.asFloat());
      hydrationMap.putString("addedBy", dp.getOriginalDataSource().getAppPackageName());

      map.pushMap(hydrationMap);
    }
  }

  public boolean save(ReadableArray hydrationArray) {
    DataSource hydrationSource = this.getDataSource();
    ArrayList<DataPoint> dataPoints = new ArrayList<DataPoint>();
    ArrayList<DataSet> dataSets = new ArrayList<DataSet>();
    for (int index = 0 ; index < hydrationArray.size() ; index++) {
      ReadableMap hydrationSample = hydrationArray.getMap(index);
      if (hydrationSample != null) {
        dataPoints.add(DataPoint.builder(hydrationSource)
          .setTimestamp((long) hydrationSample.getDouble("date"), TimeUnit.MILLISECONDS)
          .setField(Field.FIELD_VOLUME, (float) hydrationSample.getDouble("waterConsumed"))
          .build());
      }
      if (dataPoints.size() % MAX_DATAPOINTS_PER_SINGLE_REQUEST == 0) {
        // Be sure to limit each individual request to 1000 datapoints. Exceeding this limit could result in an error.
        // https://developers.google.com/fit/android/history#insert_data
        dataSets.add(DataSet.builder(hydrationSource).addAll(dataPoints).build());
        dataPoints.clear();
      }
    }
    if (dataPoints.size() > 0) {
      dataSets.add(DataSet.builder(hydrationSource).addAll(dataPoints).build());
    }
    new SaveDataHelper(dataSets, googleFitManager).execute();

    return true;
  }

  public boolean delete(ReadableMap options) {
    long endTime = (long) options.getDouble("endDate");
    long startTime = (long) options.getDouble("startDate");
    new DeleteDataHelper(startTime, endTime, this.dataType, googleFitManager).execute();
    return true;
  }
}
