### Changelog:

```
0.7.1   - Fix for disconnect() (@dmitriys-lits thanks for the PR)
0.7     - Retrieve Heights, open fit activity, unified body method (@EJohnF thanks for the PR!)

0.6     - RN 0.56+ support (@skb1129 thanks for the PR)
        - nutrition scenario (@13thdeus thanks for the PR)
        
0.5     - New auth process (@priezz thanks for PR)
        - Fix unsubscribe listeners
        - Readme refactoring
        
0.4.0-beta
        - Recording API implemetation (@reboss thanks for PR)
        - Just use startRecording(callback) function which listens
        to STEPS and DISTANCE (for now) activities from Google Fitness
        API (no need to install Google fit app)

0.3.5   - Fix Error: Fragments should be static
        - Updated readme

0.3.4   - Burned Calories History (getDailyCalorieSamples)

0.3.2
        - React Native 0.46 Support

0.3.1-beta
        - better cancel/deny support

0.3.0-beta (@firodj thanks for this PR!)
        - steps adapter to avoid errors;
        - authorize: allow cancel;
        - authorize: using callback instead event;
        - strict dataSource;
        - xiaomi support;

0.2.0   - getDailyDistanceSamples();
        - isAvailable();
        - isEnabled();
        - deleteWeight();
0.1.1-beta
        - getDailyStepCountSamples method compatible with Apple Healthkit module
        - started to implement JSDoc documentation

0.1.0
        - getting activity within module itself
        - fixed package name dependency
        - provided more detailed documentation

0.0.9   - Weights Save Support
        - Refactor methods to be compatible with react-native-apple-healthkit module
        - Remove 'moment.js' dependency

0.0.8   - Weights Samples support

0.0.1   - 0.0.7 Initial builds
```
