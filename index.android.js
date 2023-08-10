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
  prepareInput,
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
          Scopes.FITNESS_BODY_WRITE,
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

  /**
   * Get the total steps per day over a specified date range.
   * @param {Object} options getDailyStepCountSamples accepts an options object containing required startDate: ISO8601Timestamp and endDate: ISO8601Timestamp.
   */

  getDailyStepCountSamples = async (options) => {
    const { startDate, endDate, bucketInterval, bucketUnit } = prepareInput(options);

    const data = await googleFit.getDailyStepCountSamples(
      startDate,
      endDate,
      bucketInterval,
      bucketUnit,
    );

    var result;
    if(data.length > 0) {
      result = data.map(function(dev) {
        const obj = {}
        obj.source =
          dev.source.appPackage +
          (dev.source.stream ? ':' + dev.source.stream : '')
        obj.steps = buildDailySteps(dev.steps)
        obj.rawSteps = dev.steps
        return obj
      }, this);
    }else{
      //simply return raw result for better debugging;
      return data;
    }

    return result;
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
   */

  getDailyDistanceSamples = async (options) => {
    const { startDate, endDate, bucketInterval, bucketUnit } = prepareInput(options);

    const result = await googleFit.getDailyDistanceSamples(
      startDate,
      endDate,
      bucketInterval,
      bucketUnit,
    );

    //construct dataset when callback is successful
    if (result.length > 0) {
      return prepareResponse(result, 'distance');
    }
    //else either no data exists or something wrong;
    return result;
  }

  getActivitySamples = async (options) => {
    const { startDate, endDate, bucketInterval, bucketUnit } = prepareInput(options);

    const result = await googleFit.getActivitySamples(
      startDate,
      endDate,
      bucketInterval,
      bucketUnit
    );

    return result;
  }

  getMoveMinutes = async (options) => {
    const { startDate, endDate, bucketInterval, bucketUnit } = prepareInput(options);

    const result = await googleFit.getMoveMinutes(
      startDate,
      endDate,
      bucketInterval,
      bucketUnit
    );

    return result;
  }

  /**
   * Get the total calories per day over a specified date range.
   * @param {Object} options getDailyCalorieSamples accepts an options object containing:
   * required startDate: ISO8601Timestamp and endDate: ISO8601Timestamp
   * optional basalCalculation - {true || false} should we substract the basal metabolic rate averaged over a week
   */

  getDailyCalorieSamples = async (options) => {
    const basalCalculation = options.basalCalculation !== false
    const { startDate, endDate, bucketInterval, bucketUnit } = prepareInput(options);

    const result = await googleFit.getDailyCalorieSamples(
      startDate,
      endDate,
      basalCalculation,
      bucketInterval,
      bucketUnit,
    );

    //construct dataset when callback is successful
    if (result.length > 0) {
      return prepareResponse(result, 'calorie');
    }
    //else either no data exists or something wrong;
    return result;
  }

  getDailyNutritionSamples = async (options) => {
    const { startDate, endDate, bucketInterval, bucketUnit } = prepareInput(options);
    const result = await googleFit.getDailyNutritionSamples(
      startDate,
      endDate,
      bucketInterval,
      bucketUnit,
    );
    //construct dataset when callback is successful
    if (result.length > 0) {
      return prepareDailyResponse(result);
    }
    //else either no data exists or something wrong;
    return result;
  }

  getWorkoutSession = async (options) => {
    try {
      const { startDate, endDate, ...config } = options;
      const result = await googleFit.getWorkoutSession(
        Date.parse(startDate),
        Date.parse(endDate),
        config
      );
      return result;
    } catch (err) {
      return err;
    }
  }

  saveWorkout = async (options) => {
    try {
      const { startDate, endDate, ...config } = options;
      const result = await googleFit.saveWorkout(
        Date.parse(startDate),
        Date.parse(endDate),
        config
      );
      return result;
    } catch (err) {
      return err;
    }
  }

  deleteAllWorkout = async (options) => {
    try {
      const { startDate, endDate, ...config } = options;
      const result = await googleFit.deleteAllWorkout(
        Date.parse(startDate),
        Date.parse(endDate),
        config
      );
      return result;
    } catch (err) {
      return err;
    }
  }

  deleteAllSleep = async (options) => {
    try {
      const { startDate, endDate, ...config } = options;
      const result = await googleFit.deleteAllSleep(
        Date.parse(startDate),
        Date.parse(endDate),
        config
      );
      return result;
    } catch (err) {
      return err;
    }
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
   */

  getWeightSamples = async (options) => {
    const { startDate, endDate, bucketInterval, bucketUnit } = prepareInput(options);

    const raw_result = await googleFit.getWeightSamples(
      startDate,
      endDate,
      bucketInterval,
      bucketUnit
    );

    if (raw_result.length > 0) {
      //remove empty object first and then parse fitness data
      const result = raw_result
        .filter(value => Object.keys(value).length !== 0)
        .map(el => {
          if (el.value) {
            if (options.unit === 'pound') {
              el.value = KgToLbs(el.value) //convert back to pounds
            }
            el.startDate = new Date(el.startDate).toISOString()
            el.endDate = new Date(el.endDate).toISOString()
            return el
          }
        });

      return result;
    }

    return raw_result;
  }

  /**
   * Query for height samples. the options object is used to setup a query to retrieve relevant samples.
   * @param {Object} options  getDailyStepCountSamples accepts an options object containing unit: "pound"/"kg",
   *                          startDate: ISO8601Timestamp and endDate: ISO8601Timestamp.
   * Note that bucketInterval and bucketUnit have no effect at the result since GoogleFit only contains one height data.
   */

  getHeightSamples = async (options) => {
    const { startDate, endDate, bucketInterval, bucketUnit } = prepareInput(options);
    const result = await googleFit.getHeightSamples(
      startDate,
      endDate,
      bucketInterval,
      bucketUnit
    );
    if (result.length > 0) {
      return prepareResponse(result, 'value');
    }

    return result;
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

  getHeartRateSamples = async (options) => {
    const { startDate, endDate, bucketInterval, bucketUnit } = prepareInput(options);
    const result = await googleFit.getHeartRateSamples(
      startDate,
      endDate,
      bucketInterval,
      bucketUnit
    );
    if (result.length > 0) {
      return prepareResponse(result, 'value');
    }
    return result;
  }

  getAggregatedHeartRateSamples = async (options, inLocalTimeZone = false) => {
    const { startDate, endDate, bucketInterval, bucketUnit } = prepareInput(options);
    const result = await googleFit.getAggregatedHeartRateSamples(
      startDate,
      endDate,
      bucketInterval,
      bucketUnit
    );
    if (result.length > 0) {
      return prepareResponse(result, 'average', inLocalTimeZone);
    }
    return result;
  }

  getRestingHeartRateSamples = async (options) => {
    const { startDate, endDate, bucketInterval, bucketUnit } = prepareInput(options);
    const result = await googleFit.getRestingHeartRateSamples(
      startDate,
      endDate,
      bucketInterval,
      bucketUnit
    );
    if (result.length > 0) {
      return prepareResponse(result, 'value');
    }
    return result;
  }

  getBloodPressureSamples = async (options, callback) => {
    const { startDate, endDate, bucketInterval, bucketUnit } = prepareInput(options);
    const result = await googleFit.getBloodPressureSamples(
      startDate,
      endDate,
      bucketInterval,
      bucketUnit,
    );
    if (result.length > 0) {
      return prepareResponse(result, 'systolic');
    }
    return result;
  }

  getBloodGlucoseSamples = async (options, callback) => {
    const { startDate, endDate, bucketInterval, bucketUnit } = prepareInput(options);
    const result = await googleFit.getBloodGlucoseSamples(
      startDate,
      endDate,
      bucketInterval,
      bucketUnit,
    );
    if (result.length > 0) {
      return prepareResponse(result);
    }
    return result;
  }

  getBodyTemperatureSamples = async (options, callback) => {
    const { startDate, endDate, bucketInterval, bucketUnit } = prepareInput(options);
    const result = await googleFit.getBodyTemperatureSamples(
      startDate,
      endDate,
      bucketInterval,
      bucketUnit,
    );
    if (result.length > 0) {
      return prepareResponse(result);
    }
    return result;
  }

  getOxygenSaturationSamples = async (options, callback) => {
    const { startDate, endDate, bucketInterval, bucketUnit } = prepareInput(options);
    const result = await googleFit.getOxygenSaturationSamples(
      startDate,
      endDate,
      bucketInterval,
      bucketUnit,
    );
    if (result.length > 0) {
      return prepareResponse(result);
    }
    return result;
  }

  saveBloodGlucose = async (options) => {
    options.date = Date.parse(options.date)
    const result = await googleFit.saveBloodGlucose(options);
    return result;
  }

  saveBloodPressure = async (options) => {
    options.date = Date.parse(options.date)
    const result = await googleFit.saveBloodPressure(options);
    return result;
  }

  getHydrationSamples = async (options) => {
    const { startDate, endDate } = prepareInput(options);
    const result = await googleFit.getHydrationSamples(
      startDate,
      endDate
    );

    if (result.length > 0) {
      return prepareHydrationResponse(result);
    }
    return result;
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
   * @param inLocalTimeZone return start and end dates in local time zone rather than converting to UTC
   */

  getSleepSamples = async (options, inLocalTimeZone = false) => {
    const { startDate, endDate } = prepareInput(options);

    const result = await googleFit.getSleepSamples(
      startDate,
      endDate
    );

    return prepareResponse(result, "addedBy", inLocalTimeZone);
  }

  saveSleep = async (options) => {
    const result = await googleFit.saveSleep(options);
    return result;
  }
}

export default new RNGoogleFit()

// Possible Scopes
export const Scopes = Object.freeze(PossibleScopes)

export const BucketUnit = Object.freeze({
  NANOSECOND: "NANOSECOND",
  MICROSECOND: "MICROSECOND",
  MILLISECOND: "MILLISECOND",
  SECOND: "SECOND",
  MINUTE: "MINUTE",
  HOUR: "HOUR",
  DAY: "DAY"
});

//Data types for food addition
export const MealType = Object.freeze({
  UNKNOWN: 0,
  BREAKFAST: 1,
  LUNCH: 2,
  DINNER: 3,
  SNACK: 4,
})

export const SleepStage = Object.freeze({
  AWAKE: 1,
  SLEEP: 2,
  OUT_OF_BED: 3,
  LIGHT_SLEEP: 4,
  DEEP_SLEEP: 5,
  REM: 6
});

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

export const ActivityType = Object.freeze({
  Aerobics: "aerobics",
  Archery: "archery",
  Badminton: "badminton",
  Baseball: "baseball",
  Basketball: "basketball",
  Biathlon: "biathlon",
  Biking: "biking",
  Handbiking: "biking.hand",
  Mountain_biking: "biking.mountain",
  Road_biking: "biking.road",
  Spinning: "biking.spinning",
  Stationary_biking: "biking.stationary",
  Utility_biking: "biking.utility",
  Boxing: "boxing",
  Calisthenics: "calisthenics",
  Circuit_training: "circuit_training",
  Cricket: "cricket",
  Crossfit: "crossfit",
  Curling: "curling",
  Dancing: "dancing",
  Diving: "diving",
  Elevator: "elevator",
  Elliptical: "elliptical",
  Ergometer: "ergometer",
  Escalator: "escalator",
  Fencing: "fencing",
  Football_American: "football.american",
  Football_Australian: "football.australian",
  Football_Soccer: "football.soccer",
  Frisbee_Disc: "frisbee_disc",
  Gardening: "gardening",
  Golf: "golf",
  Guided_Breathing: "guided_breathing",
  Gymnastics: "gymnastics",
  Handball: "handball",
  HIIT: "interval_training.high_intensity",
  Hiking: "hiking",
  Hockey: "hockey",
  Horseback_riding: "horseback_riding",
  Housework: "housework",
  Ice_skating: "ice_skating",
  In_vehicle: "in_vehicle",
  Interval_Training: "interval_training",
  Jumping_rope: "jump_rope",
  Kayaking: "kayaking",
  Kettlebell_training: "kettlebell_training",
  Kickboxing: "kickboxing",
  Kick_Scooter: "kick_scooter",
  Kitesurfing: "kitesurfing",
  Martial_arts: "martial_arts",
  Meditation: "meditation",
  Mime_Type_Prefix: "vnd.google.fitness.activity/",
  Mixed_martial_arts: "martial_arts.mixed",
  Other_unclassified_fitness_activity: "other",
  P90X_exercises: "p90x",
  Paragliding: "paragliding",
  Pilates: "pilates",
  Polo: "polo",
  Racquetball: "racquetball",
  Rock_climbing: "rock_climbing",
  Rowing: "rowing",
  Rowing_machine: "rowing.machine",
  Rugby: "rugby",
  Running: "running",
  Jogging: "running.jogging",
  Running_on_sand: "running.sand",
  Running_treadmill: "running.treadmill",
  Sailing: "sailing",
  Scuba_diving: "scuba_diving",
  Skateboarding: "skateboarding",
  Skating: "skating",
  Skating_Cross: "skating.cross",
  Skating_Indoor: "skating.indoor",
  Skating_Inline_rollerblading: "skating.inline",
  Skiing: "skiing",
  Skiing_Back_Country: "skiing.back_country",
  Skiing_Cross_Country: "skiing.cross_country",
  Skiing_Downhill: "skiing.downhill",
  Skiing_Kite: "skiing.kite",
  Skiing_Roller: "skiing.roller",
  Sledding: "sledding",
  Snowboarding: "snowboarding",
  Snowmobile: "snowmobile",
  Snowshoeing: "snowshoeing",
  Softball: "softball",
  Squash: "squash",
  Stair_climbing: "stair_climbing",
  Stair_climbing_machine: "stair_climbing.machine",
  Stand_up_paddleboarding: "standup_paddleboarding",
  Status_Active: "ActiveActionStatus",
  Status_Completed: "CompletedActionStatus",
  Still_not_moving: "still",
  Strength_training: "strength_training",
  Surfing: "surfing",
  Swimming: "swimming",
  Swimming_open_water: "swimming.open_water",
  Swimming_swimming_pool: "swimming.pool",
  Table_tennis_ping_pong: "table_tennis",
  Team_sports: "team_sports",
  Tennis: "tennis",
  Tilting_sudden_device_gravity_change: "tilting",
  Treadmill_walking_or_running: "treadmill",
  Unknown_unable_to_detect_activity: "unknown",
  Volleyball: "volleyball",
  Volleyball_beach: "volleyball.beach",
  Volleyball_indoor: "volleyball.indoor",
  Wakeboarding: "wakeboarding",
  Walking: "walking",
  Walking_fitness: "walking.fitness",
  Walking_nording: "walking.nordic",
  Walking_treadmill: "walking.treadmill",
  Walking_stroller: "walking.stroller",
  Waterpolo: "water_polo",
  Weightlifting: "weightlifting",
  Wheelchair: "wheelchair",
  Windsurfing: "windsurfing",
  Yoga: "yoga",
  Zumba: "zumba"
})

export const ActivityTypeCode = Object.freeze({
  Aerobics: 9,
  Archery: 119,
  Badminton: 10,
  Baseball: 11,
  Basketball: 12,
  Biathlon: 13,
  Biking: 1,
  Handbiking: 14,
  Mountain_biking: 15,
  Road_biking: 16,
  Spinning: 17,
  Stationary_biking: 18,
  Utility_biking: 19,
  Boxing: 20,
  Calisthenics: 21,
  Circuit_training: 22,
  Cricket: 23,
  Crossfit: 113,
  Curling: 106,
  Dancing: 24,
  Diving: 102,
  Elevator: 117,
  Elliptical: 25,
  Ergometer: 103,
  Escalator: 118,
  Fencing: 26,
  Football_American: 27,
  Football_Australian: 28,
  Football_Soccer: 29,
  Frisbee: 30,
  Gardening: 31,
  Golf: 32,
  Guided_Breathing: 122,
  Gymnastics: 33,
  Handball: 34,
  HIIT: 114,
  Hiking: 35,
  Hockey: 36,
  Horseback_riding: 37,
  Housework: 38,
  Ice_skating: 104,
  In_vehicle: 0,
  Interval_Training: 115,
  Jumping_rope: 39,
  Kayaking: 40,
  Kettlebell_training: 41,
  Kickboxing: 42,
  Kitesurfing: 43,
  Martial_arts: 44,
  Meditation: 45,
  Mixed_martial_arts: 46,
  Other_unclassified_fitness_activity: 108,
  P90X_exercises: 47,
  Paragliding: 48,
  Pilates: 49,
  Polo: 50,
  Racquetball: 51,
  Rock_climbing: 52,
  Rowing: 53,
  Rowing_machine: 54,
  Rugby: 55,
  Running: 8,
  Jogging: 56,
  Running_on_sand: 57,
  Running_treadmill: 58,
  Sailing: 59,
  Scuba_diving: 60,
  Skateboarding: 61,
  Skating: 62,
  Cross_skating: 63,
  Indoor_skating: 105,
  Inline_skating_rollerblading: 64,
  Skiing: 65,
  Back_country_skiing: 66,
  Cross_country_skiing: 67,
  Downhill_skiing: 68,
  Kite_skiing: 69,
  Roller_skiing: 70,
  Sledding: 71,
  Snowboarding: 73,
  Snowmobile: 74,
  Snowshoeing: 75,
  Softball: 120,
  Squash: 76,
  Stair_climbing: 77,
  Stair_climbing_machine: 78,
  Stand_up_paddleboarding: 79,
  Still_not_moving: 3,
  Strength_training: 80,
  Surfing: 81,
  Swimming: 82,
  Swimming_open_water: 84,
  Swimming_swimming_pool: 83,
  Table_tennis_ping_pong: 85,
  Team_sports: 86,
  Tennis: 87,
  Tilting_sudden_device_gravity_change: 5,
  Treadmill_walking_or_running: 88,
  Unknown_unable_to_detect_activity: 4,
  Volleyball: 89,
  Volleyball_beach: 90,
  Volleyball_indoor: 91,
  Wakeboarding: 92,
  Walking: 7,
  Walking_fitness: 93,
  Nording_walking: 94,
  Walking_treadmill: 95,
  Walking_stroller: 116,
  Waterpolo: 96,
  Weightlifting: 97,
  Wheelchair: 98,
  Windsurfing: 99,
  Yoga: 100,
  Zumba: 101
})
