# react-native-google-fit

[Gitter Group](https://gitter.im/React-native-google-fit/community) - ask questions, answer questions!

[![npm version](https://badge.fury.io/js/react-native-google-fit.svg)](https://badge.fury.io/js/react-native-google-fit) ![Downloads](https://img.shields.io/npm/dm/react-native-google-fit.svg)

A React Native bridge module for interacting with Google Fit

# Quick Links
- [Installation](/docs/INSTALLATION.md)
- [Changelog](/docs/CHANGELOG.md)
- [Example app](https://github.com/StasDoskalenko/react-native-google-fit-example)
- [F.A.Q.](/docs/FAQ.md)

### Requirement
If you didn't set `fitnessVersion` manually, you can simply skip this part.  
Note that 0.16.1 require fitness version above 20.0.0  
Please read https://developers.google.com/fit/improvements why we made the changes.

#### Android 11
For Android 11, If you want to interact with `Google Fit` App.</br>
For example, use `openFit()`,`isAvailable(callback)`. Otherwise ignore it.</br>
Add the following queries into your `AndroidManifest.xml`
```xml
<queries>
    <package android:name="com.google.android.apps.fitness" />
</queries>
```
### USAGE

1. `import GoogleFit, { Scopes } from 'react-native-google-fit'`

#### 2. Authorize:

To check whethere GoogleFit is already authorized, simply use a function, then you can refer to the static property GoogleFit.isAuthorized
```javascript
GoogleFit.checkIsAuthorized().then(() => {
    console.log(GoogleFit.isAuthorized) // Then you can simply refer to `GoogleFit.isAuthorized` boolean.
})
```

or with async/await syntax
```javascript
await checkIsAuthorized();
console.log(GoogleFit.isAuthorized);
```
```javascript
// The list of available scopes inside of src/scopes.js file
const options = {
  scopes: [
    Scopes.FITNESS_ACTIVITY_READ,
    Scopes.FITNESS_ACTIVITY_WRITE,
    Scopes.FITNESS_BODY_READ,
    Scopes.FITNESS_BODY_WRITE,
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

**Note**: If you are using the recording API for location/ distance data, you have to [request](https://developer.android.com/training/location/permissions) the `location-permission` in your app's `AndroidManifest.xml`:
`<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />`

Alternatively you can use event listeners (deprecated)
```javascript
 GoogleFit.onAuthorize(() => {
   dispatch('AUTH SUCCESS')
 })

 GoogleFit.onAuthorizeFailure(() => {
   dispatch('AUTH ERROR')
 })
```

#### 3. Retrieve Steps For Period

```javascript
const opt = {
  startDate: "2017-01-01T00:00:17.971Z", // required ISO8601Timestamp
  endDate: new Date().toISOString(), // required ISO8601Timestamp
  bucketUnit: BucketUnit.DAY, // optional - default "DAY". Valid values: "NANOSECOND" | "MICROSECOND" | "MILLISECOND" | "SECOND" | "MINUTE" | "HOUR" | "DAY"
  bucketInterval: 1, // optional - default 1. 
};

GoogleFit.getDailyStepCountSamples(opt)
 .then((res) => {
     console.log('Daily steps >>> ', res)
 })
 .catch((err) => {console.warn(err)});

// or with async/await syntax
async function fetchData() {
  const res = await GoogleFit.getDailyStepCountSamples(opt)；
  console.log(res);
}

// shortcut functions, 
// return weekly or daily steps of given date
// all params are optional, using new Date() without given date, 
// adjustment is 0 by default, determine the first day of week, 0 == Sunday, 1==Monday, etc.
GoogleFit.getDailySteps(date).then().catch()
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
// {bucketInterval: 15, bucketUnit: BucketUnit.MINUTE}
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

// {bucketInterval: 1, bucketUnit: BucketUnit.DAY}
[
    { source: "com.google.android.gms:estimated_steps",
        ...
      rawSteps: [
       {"endDate": 1594073158830, "startDate": 1594012041944, "steps": 135}
      ]
    }
]
```

#### 4. Retrieve Weights

```javascript
const opt = {
  unit: "pound", // required; default 'kg'
  startDate: "2017-01-01T00:00:17.971Z", // required
  endDate: new Date().toISOString(), // required
  bucketUnit: BucketUnit.DAY, // optional - default "DAY". Valid values: "NANOSECOND" | "MICROSECOND" | "MILLISECOND" | "SECOND" | "MINUTE" | "HOUR" | "DAY"
  bucketInterval: 1, // optional - default 1. 
  ascending: false // optional; default false
};

GoogleFit.getWeightSamples(opt).then((res)=> {
  console.log(res)
});
// or with async/await syntax
async function fetchData() {
  const res = await GoogleFit.getWeightSamples(opt)；
  console.log(res);
}
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

#### 5. Retrieve Heights

```javascript
const opt = {
  startDate: "2017-01-01T00:00:17.971Z", // required
  endDate: new Date().toISOString(), // required
};

GoogleFit.getHeightSamples(opt).then((res)=> {
  console.log(res);
});
```

**Response:**

```javascript
[
  {
    "addedBy": "app_package_name",
    "value":1.7699999809265137, // Meter
    "endDate":"2019-06-29T15:02:23.409Z",
    "startDate":"2019-06-29T15:02:23.409Z",
    "day":"Sat"
  }
]
```

#### 6. Save Weights

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
    
#### 7. Blood pressure and Heart rate methods (since version 0.8)
```javascript
Heartrate Scopes: 
    [
        Scopes.FITNESS_ACTIVITY_READ,
        Scopes.FITNESS_ACTIVITY_WRITE,
        Scopes.FITNESS_HEART_RATE_READ,
        Scopes.FITNESS_HEART_RATE_WRITE,
    ];
Blood pressure: 
    [
        FITNESS_BLOOD_PRESSURE_READ,
        FITNESS_BLOOD_PRESSURE_WRITE,
        FITNESS_BLOOD_GLUCOSE_READ,
        FITNESS_BLOOD_GLUCOSE_WRITE,
    ];
```
```javascript
const options = {
  startDate: "2017-01-01T00:00:17.971Z", // required
  endDate: new Date().toISOString(), // required
  bucketUnit: BucketUnit.DAY, // optional - default "DAY". Valid values: "NANOSECOND" | "MICROSECOND" | "MILLISECOND" | "SECOND" | "MINUTE" | "HOUR" | "DAY"
  bucketInterval: 1, // optional - default 1. 
}

async function fetchData() {
  const heartrate = await GoogleFit.getHeartRateSamples(opt)；
  console.log(heartrate);

  const restingheartrate = await GoogleFit.getRestingHeartRateSamples(opt)；
  console.log(restingheartrate);

  const bloodpressure = await GoogleFit.getBloodPressureSamples(opt)；
  console.log(bloodpressure);
}

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
    "systolic":120,
    "diastolic":80,
    "endDate":"2019-07-26T08:39:28.493Z",
    "startDate":"1970-01-01T00:00:00.000Z",
    "day":"Thu"
  }
]
```

#### 8. Get all activities
<br/>Require scopes: `Scopes.FITNESS_ACTIVITY_READ` & `Scopes.FITNESS_LOCATION_READ`
<br/>Add `<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />` to `AndroidManifest.xml`

```javascript
  let opt = {
    startDate: "2017-01-01T00:00:17.971Z", // required
    endDate: new Date().toISOString(), // required
    bucketUnit: BucketUnit.DAY, // optional - default "DAY". Valid values: "NANOSECOND" | "MICROSECOND" | "MILLISECOND" | "SECOND" | "MINUTE" | "HOUR" | "DAY"
    bucketInterval: 1, // optional - default 1. 
  };

  GoogleFit.getActivitySamples(opt).then((res)=> {
    console.log(res)
  });
  // or with async/await syntax
  async function fetchData() {
    const res = await GoogleFit.getActivitySamples(opt)；
    console.log(res);
  }
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

#### 9. Retrieve Calories For Period
```javascript
  const opt = {
    startDate: "2017-01-01T00:00:17.971Z", // required
    endDate: new Date().toISOString(), // required
    basalCalculation: true, // optional, to calculate or not basalAVG over the week
    bucketUnit: BucketUnit.DAY, // optional - default "DAY". Valid values: "NANOSECOND" | "MICROSECOND" | "MILLISECOND" | "SECOND" | "MINUTE" | "HOUR" | "DAY"
    bucketInterval: 1, // optional - default 1. 
  };

  GoogleFit.getDailyCalorieSamples(opt).then((res) => {
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

#### 10. Retrieve Distance For Period:
```javascript
  const opt = {
    startDate: "2017-01-01T00:00:17.971Z", // required
    endDate: new Date().toISOString(), // required
    bucketUnit: BucketUnit.DAY, // optional - default "DAY". Valid values: "NANOSECOND" | "MICROSECOND" | "MILLISECOND" | "SECOND" | "MINUTE" | "HOUR" | "DAY"
    bucketInterval: 1, // optional - default 1. 
  };

  GoogleFit.getDailyDistanceSamples(opt).then((res)=>{
    console.log(res);
  });

 // or with async/await syntax
  async function fetchData() {
    const res = await GoogleFit.getDailyDistanceSamples(opt)；
    console.log(res);
  }
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

#### 11. Retrieve Daily Nutrition Data for Period:
You need to add `FITNESS_NUTRITION_READ` scope to your authorization to work with nutrition.
```javascript
  const opt = {
    startDate: "2017-01-01T00:00:17.971Z", // required
    endDate: new Date().toISOString(), // required
    bucketUnit: BucketUnit.DAY, // optional - default "DAY". Valid values: "NANOSECOND" | "MICROSECOND" | "MILLISECOND" | "SECOND" | "MINUTE" | "HOUR" | "DAY"
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

#### 12. Save Food
You need to add `FITNESS_NUTRITION_WRITE` scope to your authorization to work with nutrition.
```javascript
  const opt = {
    mealType: MealType.BREAKFAST,
    foodName: "banana",
    date: moment().format(), //equals to new Date().toISOString()
    nutrients: {
        [Nutrient.TOTAL_FAT]: 0.4,
        [Nutrient.SODIUM]: 1,
        [Nutrient.SATURATED_FAT]: 0.1,
        [Nutrient.PROTEIN]: 1.3,
        [Nutrient.TOTAL_CARBS]: 27.0,
        [Nutrient.CHOLESTEROL]: 0,
        [Nutrient.CALORIES]: 105,
        [Nutrient.SUGAR]: 14,
        [Nutrient.DIETARY_FIBER]: 3.1,
        [Nutrient.POTASSIUM]: 422,
    }
} as FoodIntake;

GoogleFit.saveFood(opt, (err, res) => {
  console.log(err, res);
})
```


#### 13. Retrieve Hydration

You need to add `FITNESS_NUTRITION_WRITE` scope to your authorization to work with hydration.
```javascript
  const opt = {
    startDate: '2020-01-05T00:00:17.971Z', // required
    endDate = new Date().toISOString() // required
  };

GoogleFit.getHydrationSamples(opt).then(res) => {
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

#### 14. Save Hydration

This method can update hydration data.
An app cannot update data inserted by other apps.

```javascript
const hydrationArray = [
  {
    // recommand use moment().valueOf() or other alternatives since Date.parse() without specification can generate wrong date.
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

Delete Hydration

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
    
#### 15. Retrieve Sleep

You need to add `FITNESS_SLEEP_READ` scope to your authorization to work with sleep.
```javascript
const opt = {
  startDate: '2020-01-01T12:33:18.873Z', // required, timestamp or ISO8601 string
  endDate: new Date().toISOString(), // required, timestamp or ISO8601 string
};

GoogleFit.getSleepSamples(opt).then((res) => {
  console.log(res)
});
```
**Response:**

```javascript
[
  { 
    'addedBy': 'com.google.android.apps.fitness' 
    'endDate': '2020-11-03T07:47:00.000Z',
    'startDate': '2020-11-03T07:33:59.160Z',
    // To understand what is granularity: https://developers.google.com/fit/scenarios/read-sleep-data
    'granularity': [
      {
        'startDate': {
          'sleepStage': 2,
          'endDate': '2020-11-03T07:47:00.000Z',
          'startDate': '2020-11-03T07:33:59.160Z',
        }
      }
    ],
  },
  { 
    'addedBy': 'com.google.android.apps.fitness',
    'endDate': '2020-11-02T17:41:00.000Z',
    'startDate': '2020-11-02T10:41:00.000Z',
    'granularity': [],
  },
]
```

Save Sleep

You need to add `FITNESS_SLEEP_READ` and `FITNESS_SLEEP_WRITE` scope to your authorization to save sleep.
To reduce the complexity of converting data type internally, 
`startDate` and `endDate` must be `number` in Epoch/Unix timestamp

Note: `identifier` must be a unique string
Read https://developers.google.com/fit/sessions, https://developer.android.com/training/articles/user-data-ids for more infos.

```javascript
  const opts: SleepSample = {
    startDate: 1604052985000, 
    endDate: 1604063785000, // or more general example parseInt(new Date().valueOf())
    sessionName: "1604052985000-1604063785000:sleep-session",
    identifier: "1604052985000-1604063785000:sleep-identifier", // warning: just an example, the string is probably not unique enough
    description: "some description",
    granularity: [
      {
        startDate: 1604052985000,
        endDate: 1604056585000,
        sleepStage: 4,
      },
      {
        startDate: 1604056585000,
        endDate: 1604060185000,
        sleepStage: 5,
      }
    ]
  }
  const result = await GoogleFit.saveSleep(opts);
  console.log(result); //either be true or error
```
#### 16. Move Minutes:

Require `Scopes.FITNESS_ACTIVITY_READ`
```javascript
const opt = {
  startDate: '2020-01-01T12:33:18.873Z', // required, timestamp or ISO8601 string
  endDate: new Date().toISOString(), // required, timestamp or ISO8601 string
  //bucket unit...
};

GoogleFit.getMoveMinutes(opt).then((res) => {
  console.log(res)
});
```
**Response:**
    
```javascript
[
   {
      "dataSourceId":"derived:com.google.active_minutes:com.google.android.gms:aggregated",
      "dataTypeName":"com.google.active_minutes",
      "duration":73,
      "endDate":1622594700000,
      "originDataSourceId":"derived:com.google.step_count.delta:com.google.android.gms:estimated_steps",
      "startDate":1622574300000
   },
   {
      "dataSourceId":"derived:com.google.active_minutes:com.google.android.gms:aggregated",
      "dataTypeName":"com.google.active_minutes",
      "duration":0,
      "endDate":1622675040000,
      "originDataSourceId":"raw:com.google.active_minutes:com.google.android.apps.fitness:user_input",
      "startDate":1622671440000
   },
   {
      "dataSourceId":"derived:com.google.active_minutes:com.google.android.gms:aggregated",
      "dataTypeName":"com.google.active_minutes",
      "duration":17,
      "endDate":1622854200000,
      "originDataSourceId":"derived:com.google.step_count.delta:com.google.android.gms:estimated_steps",
      "startDate":1622852220000
   }
]
```
#### 17. Workout [Experimental]:
Fields may be **inconsistent** and **overwrite** by Google Fit App, it could be `bugs` or some internal processes that we are not aware of.
<br/>**That's why it's experimental. Use at your own risk.**
<br/>[List of ActivityType](https://github.com/StasDoskalenko/react-native-google-fit/blob/db0d2f8cf090e5f62e8115cfba2fc0cc454fb922/index.android.d.ts#L509)
<br/>[List of Session Activities from official Doc](https://developers.google.com/android/reference/com/google/android/gms/fitness/FitnessActivities#ERGOMETER)

**Add:**
```javascript
GoogleFit.saveWorkout({
    startDate: startDate,
    endDate: endDate,
    sessionName: `session name`,
    // below example is probably not unique enough, must be **UUID**
    identifier: `session:${Date.now()}-${dataType}:${startDate}-${endDate}`,
    activityType: ActivityType.Meditation, //dataType
    description: `some description`,
    // options field
    calories: 233, // most consistent field across all activities
    steps: 600, // may not working, for example: ActivityType.Meditation doesn't have step by default
    // experimental field
    // this may not work or overwrite by GoogleFit App, it works in ActivityType.Other_unclassified_fitness_activity
    intensity: 1 // display as heart points in Google Fit app
});
```
**Deletion:**
<br />Warning: Deletion is an async actions, Oftentimes the deletion would not happen immediately.
```javascript
await GoogleFit.deleteAllWorkout({
   startDate: startDate,
   endDate: endDate,
})
```
**Get Workout:**
<br/>This is **complemental** method to `getActivitySamples()`, For some unknown reasons, both were missing some data or have incorrect data.
<br/>Try to use both to create a full picture if neccessary
```javascript
await GoogleFit.getWorkoutSession({
   startDate: startDate,
   endDate: endDate,
})
```

func | session Id | session name |  session identifier | description | duration | intensity | calories, steps. etc. 
------- | ------------- | ------------ | ------------- | ------------ | ------------- | ------------ | ------------- |
getActivitySamples()  | ❌ | ❌ | ❌ | ❌ | ✔️ | ✔️ | ✔️ 
getWorkoutSession() | ✔️ | ✔️ | ✔️ | ✔️ | ❌ | ❌ | ✔️ 

---

There is not such a update workout method if the workout exists in a session based on current investigation.
<br/>So if you want to do an update, you can try to do delete then create based on the old session timestamp

```javascript
// startDate & endDate are from the existing session you want to modify
const options = {
    startDate: startDate,
    endDate: endDate,
    sessionName: `new session name`,
    identifier: `session:${Date.now()}-${dataType}:${startDate}-${endDate}`, // UUID
    activityType: ActivityType.Meditation, //dataType
    description: `new description`,
    .....
    newData
};

const del = await GoogleFit.deleteAllWorkout(options);

if(del) {
    const result = await GoogleFit.saveWorkout(options);
    console.log(result)
}
```

#### Other methods:

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
