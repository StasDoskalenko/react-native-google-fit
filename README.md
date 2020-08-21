# react-native-google-fit

[Gitter Group](https://gitter.im/React-native-google-fit/community) - ask questions, answer questions!

[![npm version](https://badge.fury.io/js/react-native-google-fit.svg)](https://badge.fury.io/js/react-native-google-fit) ![Downloads](https://img.shields.io/npm/dm/react-native-google-fit.svg)

A React Native bridge module for interacting with Google Fit

# Quick Links
- [Installation](/docs/INSTALLATION.md)
- [Changelog](/docs/CHANGELOG.md)
- [Example app](https://github.com/StasDoskalenko/react-native-google-fit-example)
- [F.A.Q.](/docs/FAQ.md)


### USAGE

1. `import GoogleFit, { Scopes } from 'react-native-google-fit'`

2. Authorize:

    To check whethere GoogleFit is already authorized, simply use a function
    ```
        GoogleFit.checkIsAuthorized()
    ```
    Then you can simply refer to `GoogleFit.isAuthorized` boolean.
    
    ```javascript
    // The list of available scopes inside of src/scopes.js file
    const options = {
      scopes: [
        Scopes.FITNESS_ACTIVITY_READ_WRITE,
        Scopes.FITNESS_BODY_READ_WRITE,
      ],
    }
    GoogleFit.authorize(options)
      .then(authResult => {
        if (authResult.success) {
          dispatch("AUTH_SUCCESS");
        } else {
          dispatch("AUTH_DENIED", authResult.message);
        }
      })
      .catch(() => {
        dispatch("AUTH_ERROR");
      })
    
    // ...
    // Call when authorized
    GoogleFit.startRecording((callback) => {
      // Process data from Google Fit Recording API (no google fit app needed)
    });
    ```
    
    Alternatively you can use event listeners (deprecated)
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
      endDate: new Date().toISOString(), // required ISO8601Timestamp
      bucketUnit: "DAY", // optional - default "DAY". Valid values: "NANOSECOND" | "MICROSECOND" | "MILLISECOND" | "SECOND" | "MINUTE" | "HOUR" | "DAY"
      bucketInterval: 1, // optional - default 1. 
    };
    
    GoogleFit.getDailyStepCountSamples(options)
     .then((res) => {
         console.log('Daily steps >>> ', res)
     })
     .catch((err) => {console.warn(err)})
    
    // shortcut functions, 
    // return weekly or daily steps of given date
    // all params are optional, using new Date() without given date, 
    // adjustment is 0 by default, determine the first day of week, 0 == Sunday, 1==Monday, etc.
    GoogleFit.getDailySteps(date).then.catch()
    GoogleFit.getWeeklySteps(date, adjustment).then().catch()
     
    ```

    **Response:**
    
    ```javascript
    [
      { source: "com.google.android.gms:estimated_steps", steps: [
        {
          "date":"2019-06-29","value":2328
        },
        {
          "date":"2019-06-30","value":8010
          }
        ]
      },
      { source: "com.google.android.gms:merge_step_deltas", steps: [
        {
          "date":"2019-06-29","value":2328
        },
        {
          "date":"2019-06-30","value":8010
          }
        ]
      },
      { source: "com.xiaomi.hm.health", steps: [] }
    ];
    ```
   **Note:** bucket Config for step reflects on `rawStep` entity.
   
   **Response:**
   ```javascript
   // {bucketInterval: 15, bucketUnit: 'MINUTE'}
   [
      { source: "com.google.android.gms:estimated_steps", 
        steps: [
        {
          "date":"2019-07-06","value": 135
        },
        ],
        rawSteps: [
          {"endDate": 1594012101944, "startDate": 1594012041944, "steps": 13}, 
          {"endDate": 1594020600000, "startDate": 1594020596034, "steps": 0}, 
          {"endDate": 1594020693175, "startDate": 1594020600000, "steps": 24}, 
          {"endDate": 1594068898912, "startDate": 1594068777409, "steps": 53}, 
          {"endDate": 1594073158830, "startDate": 1594073066166, "steps": 45}
        ]
      },
    ]
    
    // {bucketInterval: 1, bucketUnit: 'DAY'}
    [
        { source: "com.google.android.gms:estimated_steps",
            ...
          rawSteps: [
           {"endDate": 1594073158830, "startDate": 1594012041944, "steps": 135}
          ]
        }
    ]
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

    **Response:**
    
    ```javascript
    [
      {
        "addedBy": "app_package_name",
        "value":72,
        "endDate":"2019-06-29T15:02:23.413Z",
        "startDate":"2019-06-29T15:02:23.413Z",
        "day":"Sat"
      },
      {
        "addedBy": "app_package_name",
        "value":72.4000015258789,
        "endDate":"2019-07-26T08:06:42.903Z",
        "startDate":"2019-07-26T08:06:42.903Z",
        "day":"Fri"
      }
    ]
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

    **Response:**

    ```javascript
    [
      {
        "addedBy": "app_package_name",
        "value":1.7699999809265137,
        "endDate":"2019-06-29T15:02:23.409Z",
        "startDate":"2019-06-29T15:02:23.409Z",
        "day":"Sat"
      }
    ]
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
      bucketUnit: "DAY", // optional - default "DAY". Valid values: "NANOSECOND" | "MICROSECOND" | "MILLISECOND" | "SECOND" | "MINUTE" | "HOUR" | "DAY"
      bucketInterval: 1, // optional - default 1. 
    }
    const callback = ((error, response) => {
      console.log(error, response)
    });

    GoogleFit.getHeartRateSamples(options, callback)
    GoogleFit.getBloodPressureSamples(options, callback)
    ```

    **Response:**

    ```javascript
    // heart rate
    [
      {
        "value":80,
        "endDate":"2019-07-26T10:19:21.348Z",
        "startDate":"2019-07-26T10:19:21.348Z",
        "day":"Fri"
      }
    ]

    // blood pressure
    [
      {
        "value":120,
        "value2":80,
        "endDate":"2019-07-26T08:39:28.493Z",
        "startDate":"1970-01-01T00:00:00.000Z",
        "day":"Thu"
      }
    ]
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

      **Response:**
    
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

9. Retrieve Calories For Period
    ```javascript
      const opt = {
        startDate: "2017-01-01T00:00:17.971Z", // required
        endDate: new Date().toISOString(), // required
        basalCalculation: true, // optional, to calculate or not basalAVG over the week
        bucketUnit: "DAY", // optional - default "DAY". Valid values: "NANOSECOND" | "MICROSECOND" | "MILLISECOND" | "SECOND" | "MINUTE" | "HOUR" | "DAY"
        bucketInterval: 1, // optional - default 1. 
      };

      GoogleFit.getDailyCalorieSamples(opt, (err, res) => {
        console.log(res);
      });
    ```

    **Response:**
    
    ```javascript
    [
      {
        "calorie":1721.948974609375,
        "endDate":"2019-06-27T15:13:27.000Z",
        "startDate":"2019-06-27T15:02:23.409Z",
        "day":"Thu"
      },
      {
        "calorie":1598.25,
        "endDate":"2019-06-28T15:13:27.000Z",
        "startDate":"2019-06-27T15:13:27.000Z",
        "day":"Thu"
      }
    ]
    ```

10. Retrieve Distance For Period:
    ```javascript
      const opt = {
        startDate: "2017-01-01T00:00:17.971Z", // required
        endDate: new Date().toISOString(), // required
        bucketUnit: "DAY", // optional - default "DAY". Valid values: "NANOSECOND" | "MICROSECOND" | "MILLISECOND" | "SECOND" | "MINUTE" | "HOUR" | "DAY"
        bucketInterval: 1, // optional - default 1. 
      };

      GoogleFit.getDailyDistanceSamples(opt, (err, res) => {
        console.log(res);
      });
    ```

    **Response:**
    
    ```javascript
    [
      {
        "distance":2254.958251953125,
        "endDate":"2019-06-30T15:45:32.987Z",
        "startDate":"2019-06-29T16:57:01.047Z",
        "day":"Sat"
      },
      {
        "distance":3020.439453125,
        "endDate":"2019-07-01T13:08:31.332Z",
        "startDate":"2019-06-30T16:58:44.818Z",
        "day":"Sun"
      }
    ]
    ```

11. Retrieve Daily Nutrition Data for Period:
    ```javascript
      const opt = {
        startDate: "2017-01-01T00:00:17.971Z", // required
        endDate: new Date().toISOString(), // required
        bucketUnit: "DAY", // optional - default "DAY". Valid values: "NANOSECOND" | "MICROSECOND" | "MILLISECOND" | "SECOND" | "MINUTE" | "HOUR" | "DAY"
        bucketInterval: 1, // optional - default 1. 
      };

      GoogleFit.getDailyNutritionSamples(opt, (err, res) => {
        console.log(res);
      });
    ```

    **Response:**
    
    ```javascript
    [
      {
        "nutrients":{"sugar":14,"sodium":1,"calories":105,"potassium":422},
        "date":"2019-07-02"
      },
      {
        "nutrients":{"sugar":36,"iron":0,"fat.saturated":3.6000001430511475,"sodium":0.13500000536441803,"fat.total":6,"calories":225,"fat.polyunsaturated":0,"carbs.total":36,"potassium":0.21000000834465027,"cholesterol":0.029999999329447746,"protein":9.299999237060547},
        "date":"2019-07-25"
      }
    ]
    ```

12. Retrieve Hydration

    You need to add `FITNESS_NUTRITION_READ_WRITE` scope to your authorization to work with hydration.
    ```javascript
    const startDate = '2020-01-05T00:00:17.971Z'; // required
    const endDate = new Date().toISOString(); // required

    oogleFit.getHydrationSamples(startDate, endDate, (err, res) => {
      console.log(res);
    });
    ```

    **Response:**

    ```javascript
    [
      {
        "addedBy": "app_package_name",
        "date": "2020-02-01T00:00:00.000Z",
        "waterConsumed": "0.225"
      },
      {
        "addedBy": "app_package_name",
        "date": "2020-02-02T00:00:00.000Z",
        "waterConsumed": "0.325"
      },
    ]
    ```

13. Save Hydration

    This method can update hydration data.
    An app cannot update data inserted by other apps.

    ```javascript
    const hydrationArray = [
      {
        date: Date.parse('2020-02-01'), // required, timestamp
        waterConsumed: 0.225, // required, hydration data for a 0.225 liter drink of water
      },
      {
        date: Date.parse('2020-02-02'),
        waterConsumed: 0.325,
      },
    ];

    GoogleFit.saveHydration(hydrationArray, (err, res) => {
      if (err) throw "Cant save data to the Google Fit";
    });
    ```

14. Delete Hydration

    An app cannot delete data inserted by other apps.
    startDate and endDate MUST not be the same.

    ```javascript
    const options = {
      startDate: '2020-01-01T12:33:18.873Z', // required, timestamp or ISO8601 string
      endDate: new Date().toISOString(), // required, timestamp or ISO8601 string
    };

    GoogleFit.deleteHydration(options, (err, res) => {
      console.log(res);
    });
    ```
    
15. Retrieve Sleep 
    ```javascript
        GoogleFit.getSleepData(options, (err, res) => {
      console.log(res)
    });
    ```

16. Other methods:

    ```javascript
    observeSteps(callback); // On Step Changed Event

    unsubscribeListeners(); // Put into componentWillUnmount() method to prevent leaks

    isAvailable(callback); // Checks is GoogleFit available for current account / installed on device
    
    isEnabled(callback); // Checks is permissions granted
    
    deleteWeight(options, callback); // method to delete weights by options (same as in delete hydration)
 
    openFit(); //method to open google fit app
    
    saveHeight(options, callback);
 
    deleteHeight(options, callback); // method to delete heights by options (same as in delete hydration)
 
    disconnect(); // Closes the connection to Google Play services.
    ```

### PLANS / TODO

* code refactoring
* optimization

Copyright (c) 2017-present, Stanislav Doskalenko
doskalenko.s@gmail.com

Based on Asim Malik android source code, copyright (c) 2015, thanks mate!
