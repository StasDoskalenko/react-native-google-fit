### Getting started

`$ npm install react-native-google-fit --save`

### Note: If your React Native version > 0.60, You can skip the below section and scroll down to Demo Walkthrough Section to check how we config the authentication API with an existing project.
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

After that, when you have the client_secrets.json file from OAuth 2.0 Client IDs, you must rename and copy this file to path:
**app/android/app/src/main/resources/client_secrets.json**


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


 **Note**: If you are using the recording API for location/ distance data, you have to [request](https://developer.android.com/training/location/permissions) the `location-permission` in your app's `AndroidManifest.xml`:
 `<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />`
 
 
 
## Demo Walkthrough (Development Setup)
 
1. `npx react-native init AuthExampleTS --template react-native-template-typescript` to create a fresh RN project then go into the project folder
2. `npm install react-native-google-fit --save` install the library
3. `cd .\android\app` in the root 
   there is a `debug.keystore` file
   
   if you don't have one, then run **(You need to enable keytool command in your machine)**
   <br/>Sometimes you might want to delete the existing one from template becuase it's outdated, so it's recommanded to create your own fresh one.
```
   keytool -genkey -v -keystore debug.keystore -storepass android -alias androiddebugkey -keypass android -keyalg RSA -keysize 2048 -validity 10000
```
   You can generate the file anywhere, just copy/paste it into .\android\app folder after creation

   then run 
```
   keytool -list -v -keystore debug.keystore -alias androiddebugkey -storepass android -keypass android
```
Copy your `SHA1: XX:XX:XX:XX:XX...` for later use. 

4. https://developers.google.com/fit/android/get-api-key#request_an_oauth_20_client_id_in_the

  Click `Get a Client ID` button , 
  then Create a new Project for demonstration purpose.

  Create credentials
  * 1). Credential Type
        <br/>Fitness API, check userdata
  * 2). OAuth Consent Screen
        </br>Add your App name (it can be any but it will show up the name when asking authentication in your app)
        </br>Add email
  * 3). Scopes (optional)
        </br>Skip this part since we can ask permission in App.
  * 4). OAuth Client ID
     * Applicatin type: `Android`, it can be vary depends on your own app.
     * Name: `Android client 1` for demo purpose
     * Package name: 
       </br>You can find your package name in `AndroidManifest.xml` under the android folder
       ![image](https://user-images.githubusercontent.com/35160613/123344320-9972b280-d521-11eb-9661-6d5f3dedb481.png)
       </br>in this demo it's `com.authexamplets`
     * SHA-1: `SHA1: XX:XX:XX:XX:XX...` you obtained previously
  * 5). Your Credentials 
        </br>Nothing you need to do.
       
After that, Go to your `OAuth consent screen`, there are two ways you can do 
   * add test user email so your developed app can only be accessed by the test user only.
   ![image](https://user-images.githubusercontent.com/35160613/123345598-6da4fc00-d524-11eb-93b5-887be9613ab5.png)

Now add basic code into your app. You should be good to go.

   ![image](https://user-images.githubusercontent.com/35160613/123345665-93320580-d524-11eb-8c92-f9d2694e6eb3.png)

