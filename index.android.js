'use strict'
import { DeviceEventEmitter, NativeModules, PermissionsAndroid } from 'react-native';
import moment from 'moment';

import PossibleScopes from './src/scopes';
import {
  buildDailySteps,
  isNil,
  KgToLbs,
  lbsAndOzToK,
  prepareDailyResponse,
  prepareResponse,
  prepareHydrationResponse,
  prepareDeleteOptions,
  getWeekBoundary,
} from './src/utils';

const googleFit = NativeModules.RNGoogleFit

class RNGoogleFit {
  eventListeners = []
  isAuthorized = false

  authorize = async (options = {}) => {
    const successResponse = { success: true }
    try {
      await this.checkIsAuthorized()
      if (this.isAuthorized) {
        return successResponse
      }
      const authResult = await new Promise((resolve, reject) => {
        this.onAuthorize(() => {
          this.isAuthorized = true
          resolve(successResponse)
        })
        this.onAuthorizeFailure(error => {
          this.isAuthorized = false
          reject({ success: false, message: error.message })
        })

        const defaultScopes = [
          Scopes.FITNESS_ACTIVITY_READ,
          Scopes.FITNESS_BODY_READ_WRITE,
          Scopes.FITNESS_LOCATION_READ,
        ]

        googleFit.authorize({
          scopes: (options && options.scopes) || defaultScopes,
        })
      })
      return authResult
    } catch (error) {
      return { success: false, message: error.message }
    }
  }

  checkIsAuthorized = async () => {
    const { isAuthorized } = await googleFit.isAuthorized()
    this.isAuthorized = isAuthorized
  }

  disconnect = () => {
    this.isAuthorized = false
    googleFit.disconnect()
    this.removeListeners()
  }

  removeListeners = () => {
    this.eventListeners.forEach(eventListener => eventListener.remove())
    this.eventListeners = []
  }


  // recommend to refactor both permission to allow other permission options besides PERMISSONS.ACCESS_FINE_LOCATION
  // check permissions
  checkPermissionAndroid = async () => {
    const response = await PermissionsAndroid.check(PermissionsAndroid.PERMISSIONS.ACCESS_FINE_LOCATION);
    return response === true;
  }

  // request permissions
  requestPermissionAndroid = async (dataTypes) => {

    const check = await this.checkPermissionAndroid();

    if (dataTypes.includes('distance') && !check) {
      try {
        const granted = await PermissionsAndroid.request(
          PermissionsAndroid.PERMISSIONS.ACCESS_FINE_LOCATION,
          {
            title: "Access Location Permisson",
            message:
              "Enable location access for Google Fit Api. " +
              "Cancel may cause inaccuray result",
            buttonNegative: "Cancel",
            buttonPositive: "OK"
          }
        );

        // this need to be changed in the future if we want to use RecordingAPI for more sensitive permissions
        if( granted === PermissionsAndroid.RESULTS.GRANTED ) {
          // we don't do anything here since the permissons are granted
        } else {
          // remove distance from array to avoid crash,
          return dataTypes.filter(data => data !== 'distance');
        }
      } catch (err) {
        console.warn(err);
      };
    }
    return dataTypes;
  }

  /**
   * Start recording fitness data
   *
   * You could specify data by array dataTypes. Possible values - step, distance, activity which corresponds
   * DataTypes.TYPE_STEP_CUMULATIVE, DataType.TYPE_DISTANCE_DELTA and DataType.TYPE_ACTIVITIES_SAMPLES
   *
   * Default value for dataTypes is steps and distance data
   *
   * This function relies on sending events to signal the RecordingAPI status
   * Simply create an event listener for the {DATA_TYPE}_RECORDING (ex. STEP_RECORDING)
   * and check for {recording: true} as the event data
   */
  startRecording = (callback, dataTypes = ['step']) => {

    this.requestPermissionAndroid(dataTypes).then((dataTypes) => {
      googleFit.startFitnessRecording(dataTypes)

      const eventListeners = dataTypes.map(dataTypeName => {
        const eventName = `${dataTypeName.toUpperCase()}_RECORDING`

        return DeviceEventEmitter.addListener(eventName, event => callback(event))
      })

      this.eventListeners.push(...eventListeners)
    })
  }


  /**
   * A shortcut to get the total steps of a given day by using getDailyStepCountSamples
   * @param {Date} date optional param, new moment() will be used if date is not provided
   */
  getDailySteps(date = moment()) {
    const options = {
      startDate: moment(date).startOf('day'),
      endDate: moment(date).endOf('day'),
    };
    return this.getDailyStepCountSamples(options);
  }

  /**
   * A shortcut to get the weekly steps of a given day by using getDailyStepCountSamples
   * @param {Date} date optional param, new Date() will be used if date is not provided
   * @param {number} adjustment, use to adjust the default start day of week, 0 = Sunday, 1 = Monday, etc.
   */
  getWeeklySteps(date=new Date(), adjustment=0) {
    const [startDate, endDate] = getWeekBoundary(date, adjustment);
    const options = {
      startDate: startDate,
      endDate: endDate,
    }
    return this.getDailyStepCountSamples(options);
  }

  isConfigsEmpty(obj) {
    for(var prop in obj) return false;
    return true;
  }

  _retrieveDailyStepCountSamples = (startDate, endDate, options, callback) => {
    googleFit.getDailyStepCountSamples(
      startDate,
      endDate,
      options.configs,
      msg => callback(msg, false),
      res => {
        if (res.length > 0) {
          callback(
            false,
            res.map(function(dev) {
              const obj = {}
              obj.source =
                dev.source.appPackage +
                (dev.source.stream ? ':' + dev.source.stream : '')
              obj.steps = buildDailySteps(dev.steps)
              obj.rawSteps = this.isConfigsEmpty(options.configs) ? [] : dev.steps
              return obj
            }, this)
          )
        } else {
          callback('There is no any steps data for this period', false)
        }
      }
    )
  }

  /**
   * Get the total steps per day over a specified date range.
   * @param {Object} options getDailyStepCountSamples accepts an options object containing required startDate: ISO8601Timestamp and endDate: ISO8601Timestamp.
   * @param {Function} callback The function will be called with an array of elements.
   */

  getDailyStepCountSamples = (options, callback) => {
    const startDate = !isNil(options.startDate)
      ? Date.parse(options.startDate)
      : new Date().setHours(0, 0, 0, 0)
    const endDate = !isNil(options.endDate)
      ? Date.parse(options.endDate)
      : new Date().valueOf()
    if (!callback || typeof callback !== 'function') {
      return new Promise((resolve, reject) => {
        this._retrieveDailyStepCountSamples(
          startDate,
          endDate,
          options,
          (error, result) => {
            if (!error) {
              resolve(result)
            } else {
              reject(error)
            }
          }
        )
      })
    }
    this._retrieveDailyStepCountSamples(startDate, endDate, options, callback)
  }

  /**
   * Get the total steps per day over a specified date range.
   * @param {Object} options getUserInputSteps accepts an options object containing required startDate: ISO8601Timestamp and endDate: ISO8601Timestamp.
   * @param {Function} callback The function will be called with an array of elements.
   */

  getUserInputSteps = (options, callback) => {
    const startDate = !isNil(options.startDate) ? Date.parse(options.startDate) : (new Date()).setHours(0, 0, 0, 0)
    const endDate = !isNil(options.endDate) ? Date.parse(options.endDate) : (new Date()).valueOf()
    googleFit.getUserInputSteps(startDate, endDate,
      (msg) => callback(msg, false),
      (res) => {
        callback(null, res);
      }
    )
  }

  /**
   * Get the total distance per day over a specified date range.
   * @param {Object} options getDailyDistanceSamples accepts an options object containing required startDate: ISO8601Timestamp and endDate: ISO8601Timestamp.
   * @param {function} callback The function will be called with an array of elements.
   */

  getDailyDistanceSamples(options, callback) {
    const startDate = !isNil(options.startDate)
      ? Date.parse(options.startDate)
      : new Date().setHours(0, 0, 0, 0)
    const endDate = !isNil(options.endDate)
      ? Date.parse(options.endDate)
      : new Date().valueOf();
    const bucketInterval = options.bucketInterval || 1;
    const bucketUnit = options.bucketUnit || "DAY";

    googleFit.getDailyDistanceSamples(
      startDate,
      endDate,
      bucketInterval,
      bucketUnit,
      msg => {
        callback(msg, false)
      },
      res => {
        if (res.length > 0) {
          callback(false, prepareResponse(res, 'distance'))
        } else {
          callback('There is no any distance data for this period', false)
        }
      }
    )
  }

  getActivitySamples(options, callback) {
    googleFit.getActivitySamples(
      options.startDate,
      options.endDate,
      error => {
        callback(error, false)
      },
      res => {
        if (res.length > 0) {
          callback(false, res)
        } else {
          callback('There is no any distance data for this period', false)
        }
      }
    )
  }

  /**
   * Get the total calories per day over a specified date range.
   * @param {Object} options getDailyCalorieSamples accepts an options object containing:
   * required startDate: ISO8601Timestamp and endDate: ISO8601Timestamp
   * optional basalCalculation - {true || false} should we substract the basal metabolic rate averaged over a week
   * @param {Function} callback The function will be called with an array of elements.
   */

  getDailyCalorieSamples(options, callback) {
    const basalCalculation = options.basalCalculation !== false
    const startDate = Date.parse(options.startDate)
    const endDate = Date.parse(options.endDate)
    const bucketInterval = options.bucketInterval || 1
    const bucketUnit = options.bucketUnit || "DAY"

    googleFit.getDailyCalorieSamples(
      startDate,
      endDate,
      basalCalculation,
      bucketInterval,
      bucketUnit,
      msg => {
        callback(msg, false)
      },
      res => {
        if (res.length > 0) {
          callback(false, prepareResponse(res, 'calorie'))
        } else {
          callback('There is no any calorie data for this period', false)
        }
      }
    )
  }

  getDailyNutritionSamples(options, callback) {
    const startDate = Date.parse(options.startDate)
    const endDate = Date.parse(options.endDate)
    const bucketInterval = options.bucketInterval || 1
    const bucketUnit = options.bucketUnit || "DAY"
    googleFit.getDailyNutritionSamples(
      startDate,
      endDate,
      bucketInterval,
      bucketUnit,
      msg => {
        callback(msg, false)
      },
      res => {
        if (res.length > 0) {
          callback(false, prepareDailyResponse(res))
        } else {
          callback('There is no any nutrition data for this period', false)
        }
      }
    )
  }

  saveFood(options, callback) {
    options.date = Date.parse(options.date)
    googleFit.saveFood(
      options,
      msg => {
        callback(msg, false)
      },
      res => {
        callback(false, res)
      }
    )
  }

  /**
   * Query for weight samples. the options object is used to setup a query to retrieve relevant samples.
   * @param {Object} options  getDailyStepCountSamples accepts an options object containing unit: "pound"/"kg",
   *                          startDate: ISO8601Timestamp and endDate: ISO8601Timestamp.
   * @callback callback The function will be called with an array of elements.
   */

  getWeightSamples = (options, callback) => {
    const startDate = !isNil(options.startDate)
      ? Date.parse(options.startDate)
      : new Date().setHours(0, 0, 0, 0)
    const endDate = !isNil(options.endDate)
      ? Date.parse(options.endDate)
      : new Date().valueOf()
    googleFit.getWeightSamples(
      startDate,
      endDate,
      msg => {
        callback(msg, false)
      },
      res => {
        if (res.length > 0) {
          res = res.map(el => {
            if (el.value) {
              if (options.unit === 'pound') {
                el.value = KgToLbs(el.value) //convert back to pounds
              }
              el.startDate = new Date(el.startDate).toISOString()
              el.endDate = new Date(el.endDate).toISOString()
              return el
            }
          })
          callback(false, res.filter(day => !isNil(day)))
        } else {
          callback('There is no any weight data for this period', false)
        }
      }
    )
  }

  getHeightSamples(options, callback) {
    const startDate = Date.parse(options.startDate)
    const endDate = Date.parse(options.endDate)
    googleFit.getHeightSamples(
      startDate,
      endDate,
      msg => {
        callback(msg, false)
      },
      res => {
        if (res.length > 0) {
          callback(false, prepareResponse(res, 'value'))
        } else {
          callback('There is no any height data for this period', false)
        }
      }
    )
  }

  saveHeight(options, callback) {
    options.date = Date.parse(options.date)
    googleFit.saveHeight(
      options,
      msg => {
        callback(msg, false)
      },
      res => {
        callback(false, res)
      }
    )
  }

  saveWeight(options, callback) {
    if (options.unit == 'pound') {
      options.value = lbsAndOzToK({ pounds: options.value, ounces: 0 }) //convert pounds and ounces to kg
    }
    options.date = Date.parse(options.date)
    googleFit.saveWeight(
      options,
      msg => {
        callback(msg, false)
      },
      res => {
        callback(false, res)
      }
    )
  }

  deleteWeight = (options, callback) => {
    googleFit.deleteWeight(
      prepareDeleteOptions(options),
      msg => {
        callback(msg, false)
      },
      res => {
        callback(false, res)
      }
    )
  }

  deleteHeight = (options, callback) => {
    googleFit.deleteHeight(
      prepareDeleteOptions(options),
      msg => {
        callback(msg, false)
      },
      res => {
        callback(false, res)
      }
    )
  }

  isAvailable(callback) {
    // true if GoogleFit installed
    googleFit.isAvailable(
      msg => {
        callback(msg, false)
      },
      res => {
        callback(false, res)
      }
    )
  }

  isEnabled(callback) {
    // true if permission granted
    googleFit.isEnabled(
      msg => {
        callback(msg, false)
      },
      res => {
        callback(false, res)
      }
    )
  }

  openFit() {
    googleFit.openFit()
  }

  observeSteps = callback => {
    const stepsObserver = DeviceEventEmitter.addListener(
      'StepChangedEvent',
      steps => callback(steps)
    )
    googleFit.observeSteps()
    this.eventListeners.push(stepsObserver)
  }

  observeHistory = callback => {
    const historyObserver = DeviceEventEmitter.addListener(
      'StepHistoryChangedEvent',
      steps => callback(steps)
    )
    this.eventListeners.push(historyObserver)
  }

  onAuthorize = callback => {
    const authObserver = DeviceEventEmitter.addListener(
      'GoogleFitAuthorizeSuccess',
      authorized => callback(authorized)
    )
    this.eventListeners.push(authObserver)
  }

  onAuthorizeFailure = callback => {
    const authFailedObserver = DeviceEventEmitter.addListener(
      'GoogleFitAuthorizeFailure',
      authorized => callback(authorized)
    )
    this.eventListeners.push(authFailedObserver)
  }

  unsubscribeListeners = () => {
    this.removeListeners()
  }

  getHeartRateSamples(options, callback) {
    const startDate = Date.parse(options.startDate)
    const endDate = Date.parse(options.endDate)
    const bucketInterval = options.bucketInterval || 1
    const bucketUnit = options.bucketUnit || "DAY"
    googleFit.getHeartRateSamples(
      startDate,
      endDate,
      bucketInterval,
      bucketUnit,
      msg => {
        callback(msg, false)
      },
      res => {
        if (res.length > 0) {
          callback(false, prepareResponse(res, 'value'))
        } else {
          callback('There is no any heart rate data for this period', false)
        }
      }
    )
  }

  getBloodPressureSamples(options, callback) {
    const startDate = Date.parse(options.startDate)
    const endDate = Date.parse(options.endDate)
    const bucketInterval = options.bucketInterval || 1
    const bucketUnit = options.bucketUnit || "DAY"
    googleFit.getBloodPressureSamples(
      startDate,
      endDate,
      bucketInterval,
      bucketUnit,
      msg => {
        callback(msg, false)
      },
      res => {
        if (res.length > 0) {
          callback(false, prepareResponse(res, 'value'))
        } else {
          callback('There is no any heart rate data for this period', false)
        }
      }
    )
  }

  getHydrationSamples = (startDate, endDate, callback) => {
    startDate = !isNil(startDate)
      ? Date.parse(startDate)
      : new Date().setHours(0, 0, 0, 0)
    endDate = !isNil(endDate)
      ? Date.parse(endDate)
      : new Date().valueOf()
    googleFit.getHydrationSamples(
      startDate,
      endDate,
      msg => callback(true, msg),
      res => {
        callback(
          false,
          prepareHydrationResponse(res)
        )
      }
    )
  }

  saveHydration(hydrationArray, callback) {
    googleFit.saveHydration(
      hydrationArray,
      msg => {
        callback(true, msg)
      },
      res => {
        callback(false, res)
      }
    )
  }

  deleteHydration = (options, callback) => {
    googleFit.deleteHydration(
      prepareDeleteOptions(options),
      msg => {
        callback(msg, false)
      },
      res => {
        callback(false, res)
      }
    )
  }

  /**
   * Get the sleep sessions over a specified date range.
   * @param {Object} options getSleepData accepts an options object containing required startDate: ISO8601Timestamp and endDate: ISO8601Timestamp.
   * @param {Function} callback The function will be called with an array of elements.
   */

  getSleepData = (options, callback) => {
    const startDate = !isNil(options.startDate)
      ? Date.parse(options.startDate)
      : new Date().setHours(0, 0, 0, 0)
    const endDate = !isNil(options.endDate)
      ? Date.parse(options.endDate)
      : new Date().valueOf()

    googleFit.getSleepData(
      startDate,
      endDate,
      msg => callback(true, msg),
      res => {
        if (res.length > 0) {
          callback(false, prepareResponse(res, 'value'))
        } else {
          callback('There is no sleep data for this period.', false)
        }
      }
    )
  }

}

export default new RNGoogleFit()

// Possible Scopes
export const Scopes = Object.freeze(PossibleScopes)

//Data types for food addition
export const MealType = Object.freeze({
  UNKNOWN: 0,
  BREAKFAST: 1,
  LUNCH: 2,
  DINNER: 3,
  SNACK: 4,
})

export const Nutrient = Object.freeze({
  /**
   * Calories in kcal
   * @type {string}
   */
  CALORIES: 'calories',
  /**
   * Total fat in grams.
   * @type {string}
   */
  TOTAL_FAT: 'fat.total',
  /**
   * Saturated fat in grams.
   * @type {string}
   */
  SATURATED_FAT: 'fat.saturated',
  /**
   * Unsaturated fat in grams.
   * @type {string}
   */
  UNSATURATED_FAT: 'fat.unsaturated',
  /**
   * Polyunsaturated fat in grams.
   * @type {string}
   */
  POLYUNSATURATED_FAT: 'fat.polyunsaturated',
  /**
   * Monounsaturated fat in grams.
   * @type {string}
   */
  MONOUNSATURATED_FAT: 'fat.monounsaturated',
  /**
   * Trans fat in grams.
   * @type {string}
   */
  TRANS_FAT: 'fat.trans',
  /**
   * Cholesterol in milligrams.
   * @type {string}
   */
  CHOLESTEROL: 'cholesterol',
  /**
   * Sodium in milligrams.
   * @type {string}
   */
  SODIUM: 'sodium',
  /**
   * Potassium in milligrams.
   * @type {string}
   */
  POTASSIUM: 'potassium',
  /**
   * Total carbohydrates in grams.
   * @type {string}
   */
  TOTAL_CARBS: 'carbs.total',
  /**
   * Dietary fiber in grams
   * @type {string}
   */
  DIETARY_FIBER: 'dietary_fiber',
  /**
   * Sugar amount in grams.
   * @type {string}
   */
  SUGAR: 'sugar',
  /**
   * Protein amount in grams.
   * @type {string}
   */
  PROTEIN: 'protein',
  /**
   * Vitamin A amount in International Units (IU).
   * @type {string}
   */
  VITAMIN_A: 'vitamin_a',
  /**
   * Vitamin C amount in milligrams.
   * @type {string}
   */
  VITAMIN_C: 'vitamin_c',
  /**
   * Calcium amount in milligrams.
   * @type {string}
   */
  CALCIUM: 'calcium',
  /**
   * Iron amount in milligrams
   * @type {string}
   */
  IRON: 'iron',
})

/*
TODO: Add food example to readme

same as here: https://developers.google.com/fit/scenarios/add-nutrition-data

import GoogleFit, {Nutrient, MealType, FoodIntake, WeightSample} from "react-native-google-fit";
...
    addFoodExample(): Promise<void> {
        return new Promise<void>((resolve, reject): void => {
            const options = {
                mealType: MealType.BREAKFAST,
                foodName: "banana",
                date: moment().format(), //equals to new Date().toISOString()
                nutrients: {
                    [Nutrient.TOTAL_FAT]: 0.4,
                    [Nutrient.SODIUM]: 1,
                    [Nutrient.SATURATED_FAT]: 0.1,
                    [Nutrient.PROTEIN]: 1.3,
                    [Nutrient.TOTAL_CARBS]: 27.0,
                    [Nutrient.CHOLESTEROL]: 0,
                    [Nutrient.CALORIES]: 105,
                    [Nutrient.SUGAR]: 14,
                    [Nutrient.DIETARY_FIBER]: 3.1,
                    [Nutrient.POTASSIUM]: 422,
                }
            } as FoodIntake;

            GoogleFit.saveFood(options, (err: boolean) => {
                if (!err) {
                    resolve();
                } else {
                    reject();
                }
            });
        });
    }
*/
