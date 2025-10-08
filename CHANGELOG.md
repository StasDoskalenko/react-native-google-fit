## Changelog

All notable changes to this project will be documented in this file.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

### [0.22.1] - 2025-10-08

## What's Changed
* Chore: Fix expo import by @StasDoskalenko in https://github.com/StasDoskalenko/react-native-google-fit/pull/380


**Full Changelog**: https://github.com/StasDoskalenko/react-native-google-fit/compare/v0.22.0...v0.22.1



### [0.22.0] - 2025-10-08

## What's Changed
* ✨ Add Expo Support & Reorganize Documentation by @StasDoskalenko in https://github.com/StasDoskalenko/react-native-google-fit/pull/378


**Full Changelog**: https://github.com/StasDoskalenko/react-native-google-fit/compare/v0.21.4...v0.22.0



### [0.21.4] - 2025-10-08

## What's Changed
* Chore: Fix release-published.yml by @StasDoskalenko in https://github.com/StasDoskalenko/react-native-google-fit/pull/376


**Full Changelog**: https://github.com/StasDoskalenko/react-native-google-fit/compare/v0.21.3...v0.21.4



### [0.21.2] - 2025-10-08

#### Changes



### [0.21.0] - 2024-05-17

#### Added
- Distance field support in `saveWorkout()` to match workout fetching (#356)

### [0.20.0] - 2023-11-14

#### Added
- `deleteAllSleep()` method for removing sleep data (#303)
- Support for saving blood pressure data (#309)
- Flag to identify manually added data in CalorieHistory & HealthHistory (#335)
- `inLocalTimeZone` parameter to prepareResponse function (#338)

#### Changed
- Migrated to AndroidX from Android Support Library (#347)
- Bump dependencies: react-devtools-core, @babel/traverse, @sideway/formula
- Security updates: json5, decode-uri-component

### [0.19.1] - 2022-11-16

#### Added
- Resting Heart Rate support (#325, kudos to @AaronDsilva97)

### [0.19.0] - 2022-08-20

#### Changed
- Relaxed dependency version requirements (#313, kudos to @matiaskorhonen)

### [0.18.3]

#### Added
- Device info to step history data (#279, kudos to @kristerkari)

#### Changed
- Updated typescript definitions

### [0.18.2]

#### Fixed
- Temporary hotfix (#268, kudos to @nikhil-kumar-160)

### [0.18.1]

#### Added
- `getBodyTemperatureSamples()` method (#266, kudos to @mluksha)
- `getOxygenSaturationSamples()` method (#266, kudos to @mluksha)

#### Changed
- Updated typescript definitions
        
### [0.18.0]

#### Added
- Workout Support [Experimental] (#251)
- TypeScript: `BucketUnitType` and `BucketUnit` helper types
        
### [0.17.1]

#### Added
- Basic `getBloodGlucoseSamples()` implementation (kudos @ksetrin)
- Android 11 documentation note (kudos @moulie415)

#### Changed
- Updated typescript definitions

### [0.16.3]

#### Added
- `getMoveMinutes()` method

#### Changed
- Refactored and removed duplicated processData in stepHistory
- Updated typescript definitions

### [0.10.0]

#### Added
- Hydration support

#### Fixed
- Weight and height deletion

#### Changed
- Updated typescript definitions

### [0.9.17]

#### Fixed
- `observeSteps` fix (kudos to @nojas01)
- `disconnect()` method (kudos to @AylanBoscarino)

#### Changed
- Better React Native 0.60 support (kudos to @spacekadet)

### [0.9.15]

#### Added
- `getUserInputSteps()` function (kudos @HelloCore)
- Retrieve Daily Nutrition Data (kudos @jguix) 

### [0.9.13]

#### Changed
- Improved weights granularity and always use FIELD_AVERAGE (@chrisgibbs44)

### [0.9.12]

#### Changed
- Updated typescript definitions for Scoped Authorization

### [0.9.11]

#### Changed
- `getDailyCalorieSamples()` now includes basalCalculation boolean flag

### [0.9.10-beta]

#### Added
- Scope authorizations (thanks, @gaykov)
        
### [0.9.1]

#### Changed
- `getDailyStepCountSamples()` now returns promise if no callback is provided

### [0.9.0]

#### Added
- `getHeartRateSamples()` (thanks @damnnkst)
- `getBloodPressureSamples()` (thanks @damnnkst)

#### Changed
- `authorize()` is now a promise
- Non-blocking step retrieve

### [0.7.1]

#### Fixed
- `disconnect()` method (@dmitriys-lits thanks for the PR)

### [0.7]

#### Added
- Retrieve Heights
- Open fit activity
- Unified body method (@EJohnF thanks for the PR!)

### [0.6]

#### Added
- React Native 0.56+ support (@skb1129 thanks for the PR)
- Nutrition scenario (@13thdeus thanks for the PR)

### [0.5]

#### Added
- New auth process (@priezz thanks for PR)

#### Fixed
- Unsubscribe listeners

#### Changed
- README refactoring

### [0.4.0-beta]

#### Added
- Recording API implementation (@reboss thanks for PR)
- `startRecording(callback)` function which listens to STEPS and DISTANCE activities from Google Fitness API (no Google Fit app needed)

### [0.3.5]

#### Fixed
- Error: Fragments should be static
- Updated README

### [0.3.4]

#### Added
- Burned Calories History (`getDailyCalorieSamples()`)

### [0.3.2]

#### Added
- React Native 0.46 Support

### [0.3.1-beta]

#### Changed
- Better cancel/deny support

### [0.3.0-beta]

#### Added
- Steps adapter to avoid errors (@firodj thanks for this PR!)
- Authorize: allow cancel
- Authorize: using callback instead of event
- Strict dataSource
- Xiaomi support

### [0.2.0]

#### Added
- `getDailyDistanceSamples()`
- `isAvailable()`
- `isEnabled()`
- `deleteWeight()`

### [0.1.1-beta]

#### Added
- `getDailyStepCountSamples()` method compatible with Apple Healthkit module
- Started to implement JSDoc documentation

### [0.1.0]

#### Added
- Getting activity within module itself
- Fixed package name dependency
- Provided more detailed documentation

### [0.0.9]

#### Added
- Weights Save Support
- Refactor methods to be compatible with react-native-apple-healthkit module

#### Removed
- moment.js dependency

### [0.0.8]

#### Added
- Weights Samples support

### [0.0.1 - 0.0.7]

#### Added
- Initial builds
