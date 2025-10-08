# Installation Guide

## Prerequisites

### Enable Google Fit API

Before using this library, you need to enable the Google Fit API and configure OAuth 2.0:

1. Go to [Google API Console](https://console.cloud.google.com/)
2. Create a new project or select an existing one
3. Enable the **Fitness API**
4. Create OAuth 2.0 credentials:
   - Go to **Credentials** → **Create Credentials** → **OAuth 2.0 Client ID**
   - Application type: **Android**
   - Package name: Your app's package name (found in `AndroidManifest.xml`)
   - SHA-1 certificate fingerprint: See below

### Get SHA-1 Certificate Fingerprint

#### For Debug Build

```bash
cd android/app
keytool -list -v -keystore debug.keystore -alias androiddebugkey -storepass android -keypass android
```

If you don't have a `debug.keystore`, create one:

```bash
keytool -genkey -v -keystore debug.keystore -storepass android -alias androiddebugkey -keypass android -keyalg RSA -keysize 2048 -validity 10000
```

#### For Release Build

```bash
keytool -list -v -keystore your-release-key.keystore -alias your-key-alias
```

Copy the **SHA1** fingerprint (format: `XX:XX:XX:XX:...`) and use it when creating your OAuth credentials.

### Configure OAuth Consent Screen

1. Go to **OAuth consent screen** in Google Cloud Console
2. Add your app name and developer email
3. Add test users (for development) or publish the app (for production)

---

## Installation

### Option 1: Expo (Recommended for Expo Projects)

This library includes an Expo config plugin for zero-config setup.

#### 1. Install the package

```bash
npx expo install react-native-google-fit
```

#### 2. Add the plugin to your app config

In `app.json` or `app.config.js`:

```json
{
  "expo": {
    "plugins": ["react-native-google-fit"]
  }
}
```

#### 3. Rebuild your app

```bash
npx expo prebuild
npx expo run:android
```

The plugin automatically configures:
- Android 11+ `<queries>` for Google Fit app interaction (see manual setup below)
- Auto-linking (React Native 0.60+)

**What the plugin does:**

The plugin adds the following to your `AndroidManifest.xml`:
```xml
<queries>
    <package android:name="com.google.android.apps.fitness" />
</queries>
```

This allows your app to interact with the Google Fit app on Android 11+.

---

### Option 2: React Native CLI

#### 1. Install the package

```bash
npm install react-native-google-fit --save
# or
yarn add react-native-google-fit
```

#### 2. Auto-linking (React Native 0.60+)

The library will be automatically linked. No additional steps required!

#### 3. Android 11+ Configuration (Optional)

If you want to interact with the Google Fit app (e.g., use `openFit()`, `isAvailable()`), add this to `AndroidManifest.xml`:

```xml
<queries>
    <package android:name="com.google.android.apps.fitness" />
</queries>
```

#### 4. Location Permission (Optional)

If you're using the recording API for location/distance data, add to `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
```

---

### Option 3: Manual Linking (React Native < 0.60)

<details>
<summary>Click to expand manual linking instructions</summary>

#### 1. Install the package

```bash
npm install react-native-google-fit --save
```

#### 2. Link the library

```bash
react-native link react-native-google-fit
```

#### 3. Update MainApplication.java

Open `android/app/src/main/java/[...]/MainApplication.java`:

```java
import com.reactnative.googlefit.GoogleFitPackage;

// ...

@Override
protected List<ReactPackage> getPackages() {
  return Arrays.<ReactPackage>asList(
    new MainReactPackage(),
    new GoogleFitPackage(BuildConfig.APPLICATION_ID)  // Add this line
  );
}
```

**Note:** Do not change `BuildConfig.APPLICATION_ID` - it's a constant value.

#### 4. Update settings.gradle

Add to `android/settings.gradle`:

```gradle
include ':react-native-google-fit'
project(':react-native-google-fit').projectDir = new File(rootProject.projectDir, '../node_modules/react-native-google-fit/android')
```

#### 5. Update build.gradle

Add to `android/app/build.gradle` dependencies:

```gradle
dependencies {
    implementation project(':react-native-google-fit')
}
```

</details>

---

## Configuration

### Custom Play Services Versions

If you need specific versions of Google Play Services, configure them in `android/build.gradle`:

```gradle
ext {
    compileSdkVersion = 28
    targetSdkVersion = 28
    authVersion = "20.0.0"      // Google Play Auth version
    fitnessVersion = "21.0.0"   // Google Fit version
}
```

### Minimum Requirements

- **Android SDK:** 16+
- **React Native:** 0.60.0+
- **Google Play Services:** Auth 16+ and Fitness 16+

---

## Troubleshooting

### "Google Play Services not available"

- Ensure Google Play Services is installed on the device/emulator
- Update Google Play Services to the latest version

### "Authorization Failed"

- Verify SHA-1 certificate matches the one in Google Cloud Console
- Check package name matches exactly
- Ensure OAuth consent screen is configured
- Add your test account email in Google Cloud Console (for development)

### "isAvailable() returns false"

- Add `<queries>` to `AndroidManifest.xml` (see Android 11+ Configuration above)
- Ensure Google Fit app is installed on the device

### Build Errors

- Clean and rebuild:
  ```bash
  cd android
  ./gradlew clean
  cd ..
  npm start -- --reset-cache
  ```

---

## Next Steps

- [API Reference](/docs/API_REFERENCE.md) - Complete API documentation
- [Quick Start Example](#) - Basic usage example
- [F.A.Q.](/docs/FAQ.md) - Frequently asked questions
