# API Reference

Complete API documentation for `react-native-google-fit`.

## Table of Contents

- [Authorization](#authorization)
- [Activity & Steps](#activity--steps)
- [Body Measurements](#body-measurements)
- [Heart Rate & Blood Pressure](#heart-rate--blood-pressure)
- [Blood Glucose & Other Vitals](#blood-glucose--other-vitals)
- [Nutrition & Hydration](#nutrition--hydration)
- [Sleep](#sleep)
- [Workouts](#workouts)
- [Recording API](#recording-api)
- [Permissions](#permissions)
- [Constants & Types](#constants--types)

---

## Authorization

### Methods

| Method | Description | Returns |
|--------|-------------|---------|
| `authorize(options)` | Request access to Google Fit with specified scopes | `Promise<{success: boolean, message?: string}>` |
| `checkIsAuthorized()` | Check if already authorized | `Promise<void>` (sets `GoogleFit.isAuthorized`) |
| `disconnect()` | Revoke access and remove listeners | `void` |

### Example

```javascript
import GoogleFit, { Scopes } from 'react-native-google-fit'

const options = {
  scopes: [
    Scopes.FITNESS_ACTIVITY_READ,
    Scopes.FITNESS_ACTIVITY_WRITE,
    Scopes.FITNESS_BODY_READ,
    Scopes.FITNESS_BODY_WRITE,
  ],
}

// Authorize
const authResult = await GoogleFit.authorize(options)
if (authResult.success) {
  console.log('Authorized!')
}

// Check authorization status
await GoogleFit.checkIsAuthorized()
console.log(GoogleFit.isAuthorized) // true or false
```

### Available Scopes

See [src/scopes.js](../src/scopes.js) for all available scopes. Common ones:

- `Scopes.FITNESS_ACTIVITY_READ` / `FITNESS_ACTIVITY_WRITE`
- `Scopes.FITNESS_BODY_READ` / `FITNESS_BODY_WRITE`
- `Scopes.FITNESS_HEART_RATE_READ` / `FITNESS_HEART_RATE_WRITE`
- `Scopes.FITNESS_BLOOD_PRESSURE_READ` / `FITNESS_BLOOD_PRESSURE_WRITE`
- `Scopes.FITNESS_BLOOD_GLUCOSE_READ` / `FITNESS_BLOOD_GLUCOSE_WRITE`
- `Scopes.FITNESS_NUTRITION_READ` / `FITNESS_NUTRITION_WRITE`
- `Scopes.FITNESS_SLEEP_READ` / `FITNESS_SLEEP_WRITE`

---

## Activity & Steps

### Methods

| Method | Description | Parameters | Returns |
|--------|-------------|------------|---------|
| `getDailyStepCountSamples(options)` | Get daily step counts for a period | `{startDate, endDate, bucketUnit?, bucketInterval?}` | `Promise<Array>` |
| `getDailySteps(date?)` | Get steps for a specific day | `date?: Date` | `Promise<number>` |
| `getWeeklySteps(date?, adjustment?)` | Get steps for a week | `date?: Date, adjustment?: number` | `Promise<number>` |
| `getUserInputSteps(options)` | Get manually entered steps | `{startDate, endDate}` | Callback |
| `getDailyDistanceSamples(options)` | Get distance data | `{startDate, endDate, bucketUnit?, bucketInterval?}` | `Promise<Array>` |
| `getActivitySamples(options)` | Get activity data | `{startDate, endDate, bucketUnit?, bucketInterval?}` | `Promise<Array>` |
| `getMoveMinutes(options)` | Get move minutes (Google Fit metric) | `{startDate, endDate, bucketUnit?, bucketInterval?}` | `Promise<Array>` |
| `getDailyCalorieSamples(options)` | Get calorie expenditure | `{startDate, endDate, basalCalculation?, bucketUnit?, bucketInterval?}` | `Promise<Array>` |

### Example: Get Daily Steps

```javascript
const options = {
  startDate: "2024-01-01T00:00:00.000Z",
  endDate: new Date().toISOString(),
  bucketUnit: BucketUnit.DAY,
  bucketInterval: 1,
}

const steps = await GoogleFit.getDailyStepCountSamples(options)
console.log(steps)
```

### Response Format

```javascript
[
  {
    source: "com.google.android.gms:estimated_steps",
    steps: [
      { date: "2024-01-01", value: 5432 },
      { date: "2024-01-02", value: 8921 }
    ],
    rawSteps: [
      { startDate: 1704067200000, endDate: 1704153600000, steps: 5432 }
    ]
  }
]
```

---

## Body Measurements

### Methods

| Method | Description | Parameters | Returns |
|--------|-------------|------------|---------|
| `getWeightSamples(options)` | Get weight history | `{startDate, endDate, unit?, bucketUnit?, bucketInterval?, ascending?}` | `Promise<Array>` |
| `saveWeight(options)` | Save weight entry | `{value, date, unit?}` | Callback |
| `deleteWeight(options)` | Delete weight entry | `{value, date}` | Callback |
| `getHeightSamples(options)` | Get height history | `{startDate, endDate, bucketUnit?, bucketInterval?, ascending?}` | `Promise<Array>` |
| `saveHeight(options)` | Save height entry | `{value, date}` | Callback |
| `deleteHeight(options)` | Delete height entry | `{value, date}` | Callback |

### Example: Weight Tracking

```javascript
// Save weight
await GoogleFit.saveWeight({
  value: 75.5,
  date: new Date().toISOString(),
  unit: "kg" // or "pound"
})

// Get weight history
const weights = await GoogleFit.getWeightSamples({
  startDate: "2024-01-01T00:00:00.000Z",
  endDate: new Date().toISOString(),
  unit: "kg",
  ascending: false
})
```

### Response Format

```javascript
[
  {
    addedBy: "com.example.app",
    value: 75.5,
    startDate: "2024-01-15T10:00:00.000Z",
    endDate: "2024-01-15T10:00:00.000Z",
    day: "Mon"
  }
]
```

---

## Heart Rate & Blood Pressure

### Methods

| Method | Description | Parameters | Returns |
|--------|-------------|------------|---------|
| `getHeartRateSamples(options)` | Get heart rate data | `{startDate, endDate, bucketUnit?, bucketInterval?}` | `Promise<Array>` |
| `getAggregatedHeartRateSamples(options, inLocalTimeZone?)` | Get aggregated heart rate | `{startDate, endDate}` | `Promise<Array>` |
| `getRestingHeartRateSamples(options)` | Get resting heart rate | `{startDate, endDate}` | `Promise<Array>` |
| `saveHeartRate(options)` | Save heart rate measurement | `{value, date}` | Callback |
| `getBloodPressureSamples(options)` | Get blood pressure data | `{startDate, endDate, bucketUnit?, bucketInterval?}` | `Promise<Array>` |
| `saveBloodPressure(options)` | Save blood pressure reading | `{systolic, diastolic, date}` | `Promise<boolean>` |

### Example: Heart Rate

```javascript
// Get heart rate samples
const heartRate = await GoogleFit.getHeartRateSamples({
  startDate: "2024-01-01T00:00:00.000Z",
  endDate: new Date().toISOString()
})

// Save heart rate
GoogleFit.saveHeartRate({
  value: 72,
  date: new Date().toISOString()
}, (error, result) => {
  if (result) console.log('Saved!')
})
```

### Blood Pressure Example

```javascript
await GoogleFit.saveBloodPressure({
  systolic: 120,
  diastolic: 80,
  date: new Date().toISOString()
})
```

---

## Blood Glucose & Other Vitals

### Methods

| Method | Description | Parameters | Returns |
|--------|-------------|------------|---------|
| `getBloodGlucoseSamples(options)` | Get blood glucose data | `{startDate, endDate}` | Callback |
| `saveBloodGlucose(options)` | Save blood glucose reading | `{value, date}` | `Promise<boolean>` |
| `getBodyTemperatureSamples(options)` | Get body temperature data | `{startDate, endDate}` | Callback |
| `getOxygenSaturationSamples(options)` | Get oxygen saturation (SpO2) | `{startDate, endDate}` | Callback |

### Example

```javascript
// Save blood glucose
await GoogleFit.saveBloodGlucose({
  value: 95, // mg/dL
  date: new Date().toISOString()
})

// Get readings
GoogleFit.getBloodGlucoseSamples({
  startDate: "2024-01-01T00:00:00.000Z",
  endDate: new Date().toISOString()
}, (error, result) => {
  console.log(result)
})
```

---

## Nutrition & Hydration

### Methods

| Method | Description | Parameters | Returns |
|--------|-------------|------------|---------|
| `getDailyNutritionSamples(options)` | Get nutrition data | `{startDate, endDate, bucketUnit?, bucketInterval?}` | `Promise<Array>` |
| `saveFood(options)` | Save food/nutrition entry | `{foodName, nutrients, date, mealType?}` | Callback |
| `getHydrationSamples(options)` | Get hydration data | `{startDate, endDate}` | `Promise<Array>` |
| `saveHydration(waterArray)` | Save hydration data | `[{date, waterConsumed}]` | `Promise<boolean>` |
| `deleteHydration(options)` | Delete hydration entry | `{date}` | Callback |

### Example: Hydration

```javascript
// Save water intake
await GoogleFit.saveHydration([
  {
    date: new Date().toISOString(),
    waterConsumed: 250 // ml
  }
])

// Get hydration history
const hydration = await GoogleFit.getHydrationSamples({
  startDate: "2024-01-01T00:00:00.000Z",
  endDate: new Date().toISOString()
})
```

### Example: Nutrition

```javascript
GoogleFit.saveFood({
  foodName: "Banana",
  date: new Date().toISOString(),
  mealType: "snack", // breakfast, lunch, dinner, snack
  nutrients: {
    [Nutrient.CALORIES]: 105,
    [Nutrient.PROTEIN]: 1.3,
    [Nutrient.CARBS]: 27,
    [Nutrient.SUGAR]: 14
  }
}, (error, result) => {
  console.log('Food saved!')
})
```

---

## Sleep

### Methods

| Method | Description | Parameters | Returns |
|--------|-------------|------------|---------|
| `getSleepSamples(options, inLocalTimeZone?)` | Get sleep data | `{startDate, endDate}` | `Promise<Array>` |
| `saveSleep(options)` | Save sleep session | `{startDate, endDate, sleepStages?}` | `Promise<boolean>` |
| `deleteAllSleep(options)` | Delete all sleep data in range | `{startDate, endDate}` | `Promise<boolean>` |

### Example

```javascript
// Get sleep data
const sleep = await GoogleFit.getSleepSamples({
  startDate: "2024-01-01T00:00:00.000Z",
  endDate: new Date().toISOString()
}, true) // inLocalTimeZone = true

// Save sleep
await GoogleFit.saveSleep({
  startDate: "2024-01-15T22:00:00.000Z",
  endDate: "2024-01-16T06:00:00.000Z",
  sleepStages: [
    { startDate: "2024-01-15T22:00:00.000Z", endDate: "2024-01-15T22:30:00.000Z", stage: SleepStage.AWAKE },
    { startDate: "2024-01-15T22:30:00.000Z", endDate: "2024-01-16T02:00:00.000Z", stage: SleepStage.DEEP },
    { startDate: "2024-01-16T02:00:00.000Z", endDate: "2024-01-16T05:00:00.000Z", stage: SleepStage.LIGHT },
    { startDate: "2024-01-16T05:00:00.000Z", endDate: "2024-01-16T06:00:00.000Z", stage: SleepStage.REM }
  ]
})
```

### Sleep Stages

```javascript
import { SleepStage } from 'react-native-google-fit'

SleepStage.AWAKE      // 1
SleepStage.SLEEP      // 2
SleepStage.OUT_OF_BED // 3
SleepStage.LIGHT      // 4
SleepStage.DEEP       // 5
SleepStage.REM        // 6
```

---

## Workouts

### Methods

| Method | Description | Parameters | Returns |
|--------|-------------|------------|---------|
| `getWorkoutSession(options)` | Get workout sessions | `{startDate, endDate}` | `Promise<Array>` |
| `saveWorkout(options)` | Save workout session | `{startDate, endDate, activityType, calories?, distance?}` | `Promise<boolean>` |
| `deleteAllWorkout(options)` | Delete all workouts in range | `{startDate, endDate}` | `Promise<boolean>` |

### Example

```javascript
import { ActivityType } from 'react-native-google-fit'

// Save workout
await GoogleFit.saveWorkout({
  startDate: "2024-01-15T10:00:00.000Z",
  endDate: "2024-01-15T11:00:00.000Z",
  activityType: ActivityType.Running,
  calories: 450,
  distance: 5000 // meters
})

// Get workouts
const workouts = await GoogleFit.getWorkoutSession({
  startDate: "2024-01-01T00:00:00.000Z",
  endDate: new Date().toISOString()
})
```

### Activity Types

See `ActivityType` constant for all available activities (Running, Walking, Cycling, Swimming, etc.)

---

## Recording API

The Recording API allows background data collection without needing the Google Fit app.

### Methods

| Method | Description | Parameters | Returns |
|--------|-------------|------------|---------|
| `startRecording(callback, dataTypes?)` | Start background recording | `dataTypes?: string[]` | void |
| `isAvailable(callback)` | Check if Google Fit is available | Callback | void |
| `openFit()` | Open Google Fit app | None | void |

### Example

```javascript
// Start recording steps in background
GoogleFit.startRecording((callback) => {
  console.log('Recording started')
})

// With specific data types
GoogleFit.startRecording((callback) => {
  console.log('Recording started')
}, ['step', 'distance', 'activity'])
```

**Note:** Requires `ACCESS_FINE_LOCATION` permission for location/distance data.

---

## Permissions

### Methods

| Method | Description | Returns |
|--------|-------------|---------|
| `checkPermissionAndroid()` | Check if location permission granted | `Promise<boolean>` |
| `requestPermissionAndroid(dataTypes?)` | Request location permission | `Promise<boolean>` |

### Example

```javascript
// Check permission
const hasPermission = await GoogleFit.checkPermissionAndroid()

// Request permission
if (!hasPermission) {
  const granted = await GoogleFit.requestPermissionAndroid(['step', 'distance'])
  if (granted) {
    console.log('Permission granted!')
  }
}
```

---

## Constants & Types

### BucketUnit

Time intervals for aggregating data:

```javascript
import { BucketUnit } from 'react-native-google-fit'

BucketUnit.NANOSECOND
BucketUnit.MICROSECOND
BucketUnit.MILLISECOND
BucketUnit.SECOND
BucketUnit.MINUTE
BucketUnit.HOUR
BucketUnit.DAY
```

### Nutrient

Nutrition data types (see [Nutrient constants](#) for full list):

```javascript
import { Nutrient } from 'react-native-google-fit'

Nutrient.CALORIES
Nutrient.PROTEIN
Nutrient.TOTAL_FAT
Nutrient.TOTAL_CARBS
Nutrient.SUGAR
// ... and many more
```

### ActivityType

Physical activity types (see [ActivityType constants](#) for full list):

```javascript
import { ActivityType } from 'react-native-google-fit'

ActivityType.Running
ActivityType.Walking
ActivityType.Cycling
ActivityType.Swimming
ActivityType.Yoga
// ... 100+ activity types
```

### SleepStage

Sleep stage classifications:

```javascript
import { SleepStage } from 'react-native-google-fit'

SleepStage.AWAKE      // 1
SleepStage.SLEEP      // 2
SleepStage.OUT_OF_BED // 3
SleepStage.LIGHT      // 4
SleepStage.DEEP       // 5
SleepStage.REM        // 6
```

---

## Common Options

Most methods accept these common parameters:

| Parameter | Type | Description | Default |
|-----------|------|-------------|---------|
| `startDate` | `string` | ISO8601 timestamp | Required |
| `endDate` | `string` | ISO8601 timestamp | Required |
| `bucketUnit` | `BucketUnit` | Time unit for aggregation | `DAY` |
| `bucketInterval` | `number` | Number of units per bucket | `1` |
| `ascending` | `boolean` | Sort order | `false` |

### Date Format

All dates should be in ISO8601 format:

```javascript
new Date().toISOString() // "2024-01-15T10:30:00.000Z"
```

---

## Error Handling

All async methods return promises and can be caught:

```javascript
try {
  const steps = await GoogleFit.getDailyStepCountSamples(options)
  console.log(steps)
} catch (error) {
  console.error('Error fetching steps:', error)
}
```

For callback-based methods:

```javascript
GoogleFit.saveWeight(options, (error, result) => {
  if (error) {
    console.error('Error saving weight:', error)
  } else {
    console.log('Weight saved!', result)
  }
})
```

---

## Next Steps

- [Installation Guide](/docs/INSTALLATION.md)
- [F.A.Q.](/docs/FAQ.md)
- [Example App](https://github.com/StasDoskalenko/react-native-google-fit-example)

