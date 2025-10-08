# react-native-google-fit

[![npm version](https://badge.fury.io/js/react-native-google-fit.svg)](https://badge.fury.io/js/react-native-google-fit) ![Downloads](https://img.shields.io/npm/dm/react-native-google-fit.svg)

A React Native bridge module for interacting with Google Fit on Android.

> **⚠️ Important Notice**: Google has deprecated the Google Fit API and is transitioning to Health Connect. While the Google Fit API remains available, we are committed to maintaining this library and bringing it up to the latest standards. We will continue to support this library as long as the API is accessible and will provide migration guidance when Health Connect integration becomes necessary.

## Features

- ✅ **Step counting** and activity tracking
- ✅ **Body measurements** (weight, height)
- ✅ **Heart rate & blood pressure** monitoring  
- ✅ **Blood glucose** tracking
- ✅ **Sleep** tracking with stages
- ✅ **Nutrition & hydration** logging
- ✅ **Workout sessions** with activity types
- ✅ **Background recording** without Google Fit app
- ✅ **Expo support** via config plugin

## Documentation

- 📦 **[Installation Guide](/docs/INSTALLATION.md)** - Setup for Expo and React Native CLI
- 📖 **[API Reference](/docs/API_REFERENCE.md)** - Complete API documentation
- ❓ **[F.A.Q.](/docs/FAQ.md)** - Frequently asked questions
- 📝 **[Changelog](CHANGELOG.md)** - Version history
- 💬 **[Gitter Chat](https://gitter.im/React-native-google-fit/community)** - Ask questions, get help!

## Quick Start

### Installation

```bash
# Expo
npx expo install react-native-google-fit

# React Native CLI
npm install react-native-google-fit --save
# or
yarn add react-native-google-fit
```

[→ Full installation instructions](/docs/INSTALLATION.md)

### Basic Usage

```javascript
import GoogleFit, { Scopes } from 'react-native-google-fit'

// 1. Authorize
const options = {
  scopes: [
    Scopes.FITNESS_ACTIVITY_READ,
    Scopes.FITNESS_ACTIVITY_WRITE,
    Scopes.FITNESS_BODY_READ,
    Scopes.FITNESS_BODY_WRITE,
  ],
}

const authResult = await GoogleFit.authorize(options)

// 2. Get daily steps
const steps = await GoogleFit.getDailyStepCountSamples({
  startDate: "2024-01-01T00:00:00.000Z",
  endDate: new Date().toISOString()
})

// 3. Save weight
await GoogleFit.saveWeight({
  value: 75.5,
  date: new Date().toISOString(),
  unit: "kg"
})
```

[→ See full API documentation](/docs/API_REFERENCE.md)

## Example App

Check out the [example app repository](https://github.com/StasDoskalenko/react-native-google-fit-example) for a complete working example.

## API Categories

### 🏃 Activity & Steps
Track steps, distance, move minutes, and calories burned.

[View methods →](/docs/API_REFERENCE.md#activity--steps)

### 📏 Body Measurements
Record and retrieve weight and height data.

[View methods →](/docs/API_REFERENCE.md#body-measurements)

### ❤️ Heart Rate & Blood Pressure
Monitor cardiovascular health metrics.

[View methods →](/docs/API_REFERENCE.md#heart-rate--blood-pressure)

### 🩸 Blood Glucose & Vitals
Track blood glucose, body temperature, and oxygen saturation.

[View methods →](/docs/API_REFERENCE.md#blood-glucose--other-vitals)

### 🍎 Nutrition & Hydration
Log food intake and water consumption.

[View methods →](/docs/API_REFERENCE.md#nutrition--hydration)

### 😴 Sleep
Record sleep sessions with stages (light, deep, REM).

[View methods →](/docs/API_REFERENCE.md#sleep)

### 🏋️ Workouts
Save and retrieve workout sessions with activity types.

[View methods →](/docs/API_REFERENCE.md#workouts)

## Requirements

- **Android:** SDK 16+ (React Native 0.60+)
- **Google Play Services:** Auth & Fitness APIs
- **OAuth 2.0:** Configured in Google Cloud Console

[→ Setup instructions](/docs/INSTALLATION.md#prerequisites)

## Contributing

Contributions are welcome! Please:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'feat: add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

See [CONTRIBUTING.md](CONTRIBUTING.md) for details.

## Troubleshooting

### Common Issues

**"Authorization Failed"**
- Verify SHA-1 certificate matches Google Cloud Console
- Check package name is correct
- Add test user email in OAuth consent screen

**"Google Play Services not available"**
- Update Google Play Services on device/emulator

[→ Full troubleshooting guide](/docs/INSTALLATION.md#troubleshooting)

## License

MIT © [StasDoskalenko](https://github.com/StasDoskalenko)

## Support

- 💬 [Gitter Community](https://gitter.im/React-native-google-fit/community)
- 🐛 [Report Issues](https://github.com/StasDoskalenko/react-native-google-fit/issues)
- 📧 Email: [maintainer email]

## Acknowledgements

Thanks to all [contributors](https://github.com/StasDoskalenko/react-native-google-fit/graphs/contributors) who have helped make this library better!
