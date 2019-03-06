'use strict'

import {DeviceEventEmitter, NativeModules} from 'react-native'
import {buildDailySteps, isNil, KgToLbs, lbsAndOzToK, prepareResponse} from './src/utils'

const googleFit = NativeModules.RNGoogleFit

class RNGoogleFit {
  eventListeners = []
  isAuthorized = false

  authorize = async () => {
    const successResponse = {success: true}
    try {
      await this.checkIsAuthorized()
      if (this.isAuthorized) {
        return successResponse
      }
      const authResult = await new Promise((resolve, reject) => {
        googleFit.authorize()
        this.onAuthorize(() => {
          this.isAuthorized = true
          resolve(successResponse)
        })
        this.onAuthorizeFailure((error) => {
          this.isAuthorized = false
          reject({success: false, message: error.message})
        })
      })
      return authResult
    } catch (error) {
      return {success: false, message: error.message}
    }
  }

  checkIsAuthorized = async () => {
    const {isAuthorized} = await googleFit.isAuthorized()
    this.isAuthorized = isAuthorized
  }

  disconnect = () => {
    this.isAuthorized = false
    googleFit.disconnect()
    this.removeListeners()
  }

  removeListeners = () => {
    this.eventListeners.forEach((eventListener) => eventListener.remove())
    this.eventListeners = []
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
  startRecording = (callback, dataTypes = ['step', 'distance']) => {
    googleFit.startFitnessRecording(dataTypes)

    const eventListeners = dataTypes.map((dataTypeName) => {
      const eventName = `${dataTypeName.toUpperCase()}_RECORDING`

      return DeviceEventEmitter.addListener(eventName, (event) => callback(event))
    })

    this.eventListeners.push(...eventListeners)
  }

  // Will be deprecated in future releases
  getSteps(dayStart, dayEnd) {
    googleFit.getDailySteps(Date.parse(dayStart), Date.parse(dayEnd))
  }

  // Will be deprecated in future releases
  getWeeklySteps(startDate) {
    googleFit.getWeeklySteps(Date.parse(startDate), Date.now())
  }

  _retrieveDailyStepCountSamples = (startDate, endDate, callback) => {
    googleFit.getDailyStepCountSamples(startDate, endDate,
      (msg) => callback(msg, false),
      (res) => {
        if (res.length > 0) {
          callback(false, res.map(function (dev) {
            const obj = {}
            obj.source = dev.source.appPackage + ((dev.source.stream) ? ':' + dev.source.stream : '')
            obj.steps = buildDailySteps(dev.steps)
            return obj
          }, this))
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
    const startDate = !isNil(options.startDate) ? Date.parse(options.startDate) : (new Date()).setHours(0, 0, 0, 0)
    const endDate = !isNil(options.endDate) ? Date.parse(options.endDate) : (new Date()).valueOf()
    if (!callback || typeof callback !== 'function') {
      return new Promise((resolve, reject) => {
        this._retrieveDailyStepCountSamples(startDate, endDate, (error, result) => {
          if (!error) {
            resolve(result)
          } else {
            reject(error)
          }
        })
      })
    }
    this._retrieveDailyStepCountSamples(startDate, endDate, callback)
  }

  /**
   * Get the total distance per day over a specified date range.
   * @param {Object} options getDailyDistanceSamples accepts an options object containing required startDate: ISO8601Timestamp and endDate: ISO8601Timestamp.
   * @param {function} callback The function will be called with an array of elements.
   */

  getDailyDistanceSamples(options, callback) {
    const startDate = !isNil(options.startDate) ? Date.parse(options.startDate) : (new Date()).setHours(0, 0, 0, 0)
    const endDate = !isNil(options.endDate) ? Date.parse(options.endDate) : (new Date()).valueOf()
    googleFit.getDailyDistanceSamples(startDate,
      endDate,
      (msg) => {
        callback(msg, false)
      },
      (res) => {
        if (res.length > 0) {
          callback(false, prepareResponse(res, 'distance'))
        } else {
          callback('There is no any distance data for this period', false)
        }
      })
  }

  getActivitySamples(options, callback) {
    googleFit.getActivitySamples(
      options.startDate,
      options.endDate,
      (error) => {
        callback(error, false)
      },
      (res) => {
        if (res.length > 0) {
          callback(false, res)
        } else {
          callback('There is no any distance data for this period', false)
        }
      })
  }

  /**
   * Get the total calories per day over a specified date range.
   * @param {Object} options getDailyCalorieSamples accepts an options object containing required startDate: ISO8601Timestamp and endDate: ISO8601Timestamp.
   * @callback callback The function will be called with an array of elements.
   */

  getDailyCalorieSamples(options, callback) {
    const startDate = Date.parse(options.startDate)
    const endDate = Date.parse(options.endDate)
    googleFit.getDailyCalorieSamples(
      startDate,
      endDate,
      (msg) => {
        callback(msg, false)
      },
      (res) => {
        if (res.length > 0) {
          callback(false, prepareResponse(res, 'calorie'))
        } else {
          callback('There is no any calorie data for this period', false)
        }
      })
  }

  saveFood(options, callback) {
    options.date = Date.parse(options.date)
    googleFit.saveFood(options,
      (msg) => {
        callback(msg, false)
      },
      (res) => {
        callback(false, res)
      })
  }

  /**
   * Query for weight samples. the options object is used to setup a query to retrieve relevant samples.
   * @param {Object} options  getDailyStepCountSamples accepts an options object containing unit: "pound"/"kg",
   *                          startDate: ISO8601Timestamp and endDate: ISO8601Timestamp.
   * @callback callback The function will be called with an array of elements.
   */

  getWeightSamples = (options, callback) => {
    const startDate = !isNil(options.startDate) ? Date.parse(options.startDate) : (new Date()).setHours(0, 0, 0, 0)
    const endDate = !isNil(options.endDate) ? Date.parse(options.endDate) : (new Date()).valueOf()
    googleFit.getWeightSamples(startDate,
      endDate,
      (msg) => {
        callback(msg, false)
      },
      (res) => {
        if (res.length > 0) {
          res = res.map((el) => {
            if (el.value) {
              if (options.unit === 'pound') {
                el.value = KgToLbs(el.value) //convert back to pounds
              }
              el.startDate = new Date(el.startDate).toISOString()
              el.endDate = new Date(el.endDate).toISOString()
              return el
            }
          })
          callback(false, res.filter((day) => !isNil(day)))
        } else {
          callback('There is no any weight data for this period', false)
        }
      })
  }

  getHeightSamples(options, callback) {
    const startDate = Date.parse(options.startDate)
    const endDate = Date.parse(options.endDate)
    googleFit.getHeightSamples(startDate,
      endDate,
      (msg) => {
        callback(msg, false)
      },
      (res) => {
        if (res.length > 0) {
          callback(false, prepareResponse(res, 'value'))
        } else {
          callback('There is no any height data for this period', false)
        }
      })
  }

  saveHeight(options, callback) {
    options.date = Date.parse(options.date)
    googleFit.saveHeight(options,
      (msg) => {
        callback(msg, false)
      },
      (res) => {
        callback(false, res)

      })
  }

  saveWeight(options, callback) {
    if (options.unit == 'pound') {
      options.value = lbsAndOzToK({pounds: options.value, ounces: 0}) //convert pounds and ounces to kg
    }
    options.date = Date.parse(options.date)
    googleFit.saveWeight(options,
      (msg) => {
        callback(msg, false)
      },
      (res) => {
        callback(false, res)
      })
  }

  deleteWeight = (options, callback) => {
    if (options.unit === 'pound') {
      options.value = lbsAndOzToK({pounds: options.value, ounces: 0}) //convert pounds and ounces to kg
    }
    options.date = Date.parse(options.date)
    googleFit.deleteWeight(options,
      (msg) => {
        callback(msg, false)
      },
      (res) => {
        callback(false, res)
      })
  }

  deleteHeight = (options, callback) => {
    options.date = Date.parse(options.date)
    googleFit.deleteWeight(options,
      (msg) => {
        callback(msg, false)
      },
      (res) => {
        callback(false, res)
      })
  }

  isAvailable(callback) { // true if GoogleFit installed
    googleFit.isAvailable(
      (msg) => {
        callback(msg, false)
      },
      (res) => {
        callback(false, res)
      })
  }

  isEnabled(callback) { // true if permission granted
    googleFit.isEnabled(
      (msg) => {
        callback(msg, false)
      },
      (res) => {
        callback(false, res)
      })
  }

  openFit() {
    googleFit.openFit()
  }

  observeSteps = (callback) => {
    const stepsObserver = DeviceEventEmitter.addListener(
      'StepChangedEvent',
      (steps) => callback(steps)
    )
    googleFit.observeSteps()
    this.eventListeners.push(stepsObserver)
  }

  observeHistory = (callback) => {
    const historyObserver = DeviceEventEmitter.addListener(
      'StepHistoryChangedEvent',
      (steps) => callback(steps)
    )
    this.eventListeners.push(historyObserver)
  }

  onAuthorize = (callback) => {
    const authObserver = DeviceEventEmitter.addListener(
      'GoogleFitAuthorizeSuccess',
      (authorized) => callback(authorized)
    )
    this.eventListeners.push(authObserver)
  }

  onAuthorizeFailure = (callback) => {
    const authFailedObserver = DeviceEventEmitter.addListener(
      'GoogleFitAuthorizeFailure',
      (authorized) => callback(authorized)
    )
    this.eventListeners.push(authFailedObserver)
  }

  unsubscribeListeners = () => {
    this.removeListeners()
  }

  getHeartRateSamples(options, callback) {
    const startDate = Date.parse(options.startDate)
    const endDate = Date.parse(options.endDate)
    googleFit.getHeartRateSamples(
      startDate,
      endDate,
      (msg) => {
        callback(msg, false)
      },
      (res) => {
        if (res.length > 0) {
          callback(false, prepareResponse(res, 'value'))
        } else {
          callback('There is no any heart rate data for this period', false)
        }
      })
  }

  getBloodPressureSamples(options, callback) {
    const startDate = Date.parse(options.startDate)
    const endDate = Date.parse(options.endDate)
    googleFit.getBloodPressureSamples(
      startDate,
      endDate,
      (msg) => {
        callback(msg, false)
      },
      (res) => {
        if (res.length > 0) {
          callback(false, prepareResponse(res, 'value'))
        } else {
          callback('There is no any heart rate data for this period', false)
        }
      })
  }

}

export default new RNGoogleFit()

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
