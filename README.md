
# react-native-google-fit

A React Native bridge module for interacting with Google Fit

Changelog:

```
0.0.9   - Weights Save Support
        - Refactor methods to be compatible with react-native-apple-healthkit module
        - Remove 'moment.js' dependency

0.0.8   - Weights Samples support

0.0.1   - 0.0.7 Initial builds

```

## Getting started

`$ npm install react-native-google-fit --save`

### Mostly automatic installation

`$ react-native link react-native-google-fit`

then, in MainApplication.java, you need to pass MainActivity.activity to the module:

`new GoogleFitPackage(MainActivity.activity),`

### Manual installation

1. Open up `android/app/src/main/java/[...]/MainApplication.java`
  - Add `import com.reactnative.googlefit.GoogleFitPackage;` to the imports at the top of the file
  - Add `new GoogleFitPackage(MainActivity.activity),` to the list returned by the `getPackages()` method
2. Append the following lines to `android/settings.gradle`:
  	```
  	 include ':react-native-google-fit'
    project(':react-native-google-fit').projectDir = new File(rootProject.projectDir, '../node_modules/react-native-google-fit/android')
  	```
3. Insert the following lines inside the dependencies block in `android/app/build.gradle`:
  	```
      compile project(':react-native-google-fit')
  	```
  	
  	
### USAGE

1. `import GoogleFit from 'react-native-google-fit';`

2. Authorize:

```      
        GoogleFit.authorizeFit();
        GoogleFit.onAuthorize((result) => {
             //console.log(result);
             dispatch('AUTH SUCCESS');
        });
 ```
 
3. Retrieve Steps For Period
 
 GoogleFit.getSteps(dayStart, dayEnd);
 
 REDUX example
 
 ```
    
    let retrieveDailySteps = () => {
        return async (dispatch) => {
            let todayStart = "2017-01-01T00:00:17.971Z"; //ISO Time String
            let dayEnd = "2017-01-01T23:59:17.971Z"; //ISO Time String
            await GoogleFit.getSteps(todayStart, dayEnd);
            await GoogleFit.observeHistory((results) => {
                if (results.length > 0) {
                    console.log(results[0].steps);
                    dispatch('SUCCESSFULLY GOT DAILY STEPS!');
                } 
            });
        }
    }
 
 ```

4. Retrieve Weights

 ```

 let opt =   {
                unit: 'pound',										// required; default 'kg'
                startDate: "2017-01-01T00:00:17.971Z",		        // required
                endDate: (new Date()).toISOString(),				// required
                ascending: false									// optional; default false
             };
 GoogleFit.getWeightSamples(opt, (err,res) => {
        console.log(res);
 });

 ```


5. Save Weights

 ```

    let opt =   {
                    value: 200,
                    date: (new Date().toISOString()),
                    unit: "pound"
                };
    GoogleFit.saveWeight(opt, (err, res)=> {
         if (err) throw 'Cant save data to the Google Fit';
    });

 ```


6. Other methods:
 
 ``` 
 GoogleFit.observeSteps(callback); //On Step Changed Event
 
 GoogleFit.unsucscribeListeners(); //Put into componentWillUnmount() method to prevent leaks
 
 
 ```
 
### PLANS / TODO
 
 * support of all Google Fit activity types
 * code refactoring
 * optimization 
 
 Copyright (c) 2017-present, Stanislav Doskalenko
 doskalenko.s@gmail.com
 
 Based on Asim Malik android source code, copyright (c) 2015, thanks mate!