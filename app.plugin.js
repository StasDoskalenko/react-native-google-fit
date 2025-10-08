const {
  AndroidConfig,
  withAndroidManifest,
  createRunOncePlugin,
} = require('expo/config-plugins')

let pkg = { name: 'react-native-google-fit' }
try {
  pkg = require('./package.json')
} catch {
  // empty catch block
}

/**
 * Expo config plugin for react-native-google-fit
 * Adds necessary Android configurations for Google Fit API
 */
const withGoogleFit = (config) => {
  return withAndroidManifest(config, (config) => {
    const androidManifest = config.modResults

    // Ensure queries element exists
    if (!androidManifest.manifest.queries) {
      androidManifest.manifest.queries = []
    }

    // Add Google Fit package query for Android 11+
    const queries = androidManifest.manifest.queries[0]
    if (!queries) {
      androidManifest.manifest.queries[0] = { package: [] }
    }

    const packages = androidManifest.manifest.queries[0].package || []

    // Check if Google Fit query already exists
    const googleFitQuery = {
      $: { 'android:name': 'com.google.android.apps.fitness' },
    }

    const hasGoogleFitQuery = packages.some(
      (pkg) => pkg.$?.['android:name'] === 'com.google.android.apps.fitness'
    )

    if (!hasGoogleFitQuery) {
      packages.push(googleFitQuery)
    }

    androidManifest.manifest.queries[0].package = packages

    return config
  })
}

module.exports = createRunOncePlugin(withGoogleFit, pkg.name, pkg.version)
