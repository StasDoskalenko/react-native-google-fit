# react-native-google-fit

[![npm version](https://badge.fury.io/js/react-native-google-fit.svg)](https://badge.fury.io/js/react-native-google-fit) ![Downloads](https://img.shields.io/npm/dm/react-native-google-fit.svg)

A React Native bridge module for interacting with Google Fit

# Quick Links
- [Installation](/docs/INSTALLATION.md)
- [Changelog](/docs/CHANGELOG.md)

### USAGE

1. `import GoogleFit from 'react-native-google-fit';`

2. Authorize:

    ```javascript
    
    GoogleFit.authorize()
     .then(() => {
       dispatch('AUTH_SUCCESS')
     })
     .catch(() => {
       dispatch('AUTH_ERROR')
     })
    
    // ...
    // Call when authorized
    GoogleFit.startRecording((callback) => {
      // Process data from Google Fit Recording API (no google fit app needed)
    });
    ```
    
    Alternatively you can use event listeners
    ```javascript
     GoogleFit.onAuthorize(() => {
       dispatch('AUTH SUCCESS')
     })
         
     GoogleFit.onAuthorizeFailure(() => {
       dispatch('AUTH ERROR')
     })
    ```

3. Retrieve Steps For Period

    ```javascript
    const options = {
      startDate: "2017-01-01T00:00:17.971Z", // required ISO8601Timestamp
      endDate: new Date().toISOString() // required ISO8601Timestamp
    };
    
    GoogleFit.getDailyStepCountSamples(options)
     .then((res) => {
         console.log('Daily steps >>> ', res)
     })
     .catch((err) => {console.warn(err)})
    ```

**Response:**

```javascript
[
  { source: "com.google.android.gms:estimated_steps", steps: [] },
  { source: "com.google.android.gms:merge_step_deltas", steps: [] },
  { source: "com.xiaomi.hm.health", steps: [] }
];
```

4. Retrieve Weights

    ```javascript
    const opt = {
      unit: "pound", // required; default 'kg'
      startDate: "2017-01-01T00:00:17.971Z", // required
      endDate: new Date().toISOString(), // required
      ascending: false // optional; default false
    };
    
    GoogleFit.getWeightSamples(opt, (err, res) => {
      console.log(res);
    });
    ```
    
5. Retrieve Heights

    ```javascript
    const opt = {
      startDate: "2017-01-01T00:00:17.971Z", // required
      endDate: new Date().toISOString(), // required
    };
    
    GoogleFit.getHeightSamples(opt, (err, res) => {
      console.log(res);
    });
    ```

6. Save Weights

    ```javascript
    const opt = {
      value: 200,
      date: new Date().toISOString(),
      unit: "pound"
    };
    
    GoogleFit.saveWeight(opt, (err, res) => {
      if (err) throw "Cant save data to the Google Fit";
    });
    ```
    
7. Blood pressure and Heart rate methods (since version 0.8)
    ```javascript
       const options = {
         startDate: "2017-01-01T00:00:17.971Z", // required
         endDate: new Date().toISOString(), // required
       }
       const callback = ((error, resoponse) => {
       
       })
       GoogleFit.getHeartRateSamples(options, callback)
       GoogleFit.getBloodPressureSamples(options, callback)
    ```
    
8. Get all activities
    ```javascript
      let options = {
        startDate: new Date(2018, 9, 17).valueOf(), // simply outputs the number of milliseconds since the Unix Epoch
        endDate: new Date(2018, 9, 18).valueOf()
      };
      GoogleFit.getActivitySamples(options, (err, res) => {
        console.log(err, res)
      });
    ```
    response:
    ```javascript
     [ { 
      sourceName: 'Android',
      device: 'Android',
      sourceId: 'com.google.android.gms',
      calories: 764.189208984375,
      quantity: 6,
      end: 1539774300992,
      tracked: true,
      activityName: 'still',
      start: 1539727200000 },
    { sourceName: 'Android',
      device: 'Android',
      sourceId: 'com.google.android.gms',
      calories: 10.351096153259277,
      quantity: 138,
      end: 1539774486088,
      tracked: true,
      distance: 88.09545135498047,
      activityName: 'walking',
    }]
    ```
    Where:
    ```
    sourceName = device - 'Android' or 'Android Wear' string
    sourceId - return a value of dataSource.getAppPackageName(). For more info see: https://developers.google.com/fit/android/data-attribution
    start/end - timestamps of activity in format of milliseconds since the Unix Epoch
    tracked - bool flag, is this activity was entered by user or tracked by device. Detected by checking milliseconds of start/end timestamps. Since when user log activity in googleFit they can't set milliseconds
    distance(opt) - A distance in meters.
    activityName - string, equivalent one of these https://developers.google.com/fit/rest/v1/reference/activity-types 
    calories(opt) - double value of burned Calories in kcal.
    quantity(opt) - equivalent of steps number
    ```
    Note that optional parametrs are not presented in all activities - only where google fit return some results for this field.
    Like no distance for still activity. 

9. Other methods:

    ```javascript
    observeSteps(callback); // On Step Changed Event
    
    unsubscribeListeners(); // Put into componentWillUnmount() method to prevent leaks
    
    getDailyCalorieSamples(options, callback); // method to get calories per day
    
    getDailyDistanceSamples(options, callback); // method to get daily distance
    
    isAvailable(callback); // Checks is GoogleFit available for current account / installed on device
    
    isEnabled(callback); // Checks is permissions granted
    
    deleteWeight(options, callback); // method to delete weights by options (same as in save weights)
 
    openFit(); //method to open google fit app
    
    saveHeight(options, callback);
 
    deleteHeight(options, callback);
 
    deleteWeight(options, callback);
 
    disconnect(); // Closes the connection to Google Play services.
    ```

### PLANS / TODO

* code refactoring
* optimization

Copyright (c) 2017-present, Stanislav Doskalenko
doskalenko.s@gmail.com

Based on Asim Malik android source code, copyright (c) 2015, thanks mate!
