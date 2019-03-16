### Getting started

`$ npm install react-native-google-fit --save`

### Enable Google Fitness API for your application

In order for your app to communicate properly with the Google Fitness API you need to enable Google Fit API in your Google API Console.
Also you need to generate new client ID for your app and provide both debug and release SHA keys.
Another step is to configure the consent screen, etc.

More detailed info available at
https://developers.google.com/fit/android/get-api-key

```
1. In order for the library to work correctly, you'll need following SDK setups:
   
   Android Support Repository
   Android Support Library
   Google Play services
   Google Repository
   Google Play APK Expansion Library
   
2. In order for your app to communicate properly with the Google Fitness API,
   you need to provide the SHA1 sum of the certificate used for signing your
   application to Google. This will enable the GoogleFit plugin to communicate
   with the Fit application in each smartphone where the application is installed.
   https://developers.google.com/fit/android/get-api-key
```

### Mostly Automatic installation

`$ react-native link react-native-google-fit`

then pass your package name to the module in MainApplication.java (google fit requires package name to save data)

`new GoogleFitPackage(BuildConfig.APPLICATION_ID)`
_**Note**: Do not change BuildConfig.APPLICATION_ID - it's a constant value._

### Manual installation

1. Open up `android/app/src/main/java/[...]/MainApplication.java`

    * Add `import com.reactnative.googlefit.GoogleFitPackage;` to the imports at the top of the file
    * Add `new GoogleFitPackage(BuildConfig.APPLICATION_ID),` to the list returned by the `getPackages()` method.
    _**Note**: Do not change BuildConfig.APPLICATION_ID - it's a constant value._

2. Append the following lines to `android/settings.gradle`:
   ```
   include ':react-native-google-fit'
   project(':react-native-google-fit').projectDir = new File(rootProject.projectDir, '../node_modules/react-native-google-fit/android')
   ```
3. Insert the following lines inside the dependencies block in `android/app/build.gradle`:

   ```
     compile project(':react-native-google-fit')
   ```

