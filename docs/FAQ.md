# F.A.Q:

### SIGN_IN_REQUIRED
(thanks, @salles-sandro )

Just to let anyone else that, like me, spent lots of precious time struggling with the authorize method and the error below:

`
"ConnectionResult{statusCode=SIGN_IN_REQUIRED, resolution=PendingIntent{4460f51: android.os.BinderProxy@6992179}, message=null}"
`

After searching the entire internet i found a comment mentioning that the client_secrets.json file generated after the https://developers.google.com/fit/android/get-api-key process should be store exactly at this location:

**app/android/app/src/main/resources/client_secrets.json**

Then... at least for me, things are finally working.

Conversation: https://github.com/StasDoskalenko/react-native-google-fit/issues/90

### Some scopes are not available

Consider to authorize the app with correct permissions, check auth
syntax and src/scopes.js or official Google Fit documentation with available scopes

```javascript
// The list of available scopes inside of src/scopes.js file
const options = {
  scopes: [
    Scopes.FITNESS_ACTIVITY_WRITE,
    Scopes.FITNESS_BODY_WRITE,
  ],
}
GoogleFit.authorize(options)
 .then(() => {
   dispatch('AUTH_SUCCESS')
 })
 .catch(() => {
   dispatch('AUTH_ERROR')
 })
```

### Calories are negative

The calories values are approx matching with that of Google Fit app apart from current day's calories value.
Subtracting basal avg value.
Consider adding endDate parameter as current DateTime to get the same value as in Google Fit


### Gradle build fails on Android X
```
...
Manifest merger failed : Attribute application@appComponentFactory value=(android.support.v4.app.CoreComponentFactory) from [com.android.support:support-compat:28.0.0] AndroidManifest.xml:22:18-91 ...
```

After version 0.9.14, you can modify your app/build.gradle, simply overriding packages google services versions:

```
rootProject.ext.set("authVersion", "16.0.1")
rootProject.ext.set("fitnessVersion", "16.0.1")
```

