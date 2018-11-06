'use strict'

import { DeviceEventEmitter, NativeModules} from 'react-native';

const googleFit = NativeModules.RNGoogleFit;

class RNGoogleFit {
    eventListeners = []

    authorize = () => {
        googleFit.authorize();
    }

    disconnect = () => {
      googleFit.disconnect();
    }

    removeListeners = () => {
        this.eventListeners.forEach(eventListener => eventListener.remove())
        this.eventListeners = []
    }

    /**
     * Start recording fitness data (steps, distance)
     * This function relies on sending events to signal the RecordingAPI status
     * Simply create an event listener for the {DATA_TYPE}_RECORDING (ex. STEP_RECORDING)
     * and check for {recording: true} as the event data
     */
    startRecording = (callback) => {
        googleFit.startFitnessRecording();

        const recordingObserver = DeviceEventEmitter.addListener(
            'STEP_RECORDING',
            (steps) => callback(steps));

        const distanceObserver = DeviceEventEmitter.addListener(
            'DISTANCE_RECORDING',
            (distance) => callback(distance));

        // TODO: add mote activity listeners

        this.eventListeners.push(recordingObserver, distanceObserver)
    }

    //Will be deprecated in future releases
    getSteps(dayStart, dayEnd) {
        googleFit.getDailySteps(Date.parse(dayStart), Date.parse(dayEnd));
    }

    //Will be deprecated in future releases
    getWeeklySteps(startDate) {
        googleFit.getWeeklySteps(Date.parse(startDate), Date.now());
    }


    /**
     * Get the total steps per day over a specified date range.
     * @param {Object} options getDailyStepCountSamples accepts an options object containing required startDate: ISO8601Timestamp and endDate: ISO8601Timestamp.
     * @param {Function} callback The function will be called with an array of elements.
     */

    getDailyStepCountSamples = (options, callback) => {
        let startDate = options.startDate != undefined ? Date.parse(options.startDate) : (new Date()).setHours(0, 0, 0, 0);
        let endDate = options.endDate != undefined ? Date.parse(options.endDate) : (new Date()).valueOf();
        googleFit.getDailyStepCountSamples(startDate, endDate,
            msg => callback(msg, false),
            (res) => {
                if (res.length > 0) {
                    callback(false, res.map(function (dev) {
                            let obj = {};
                            obj.source = dev.source.appPackage + ((dev.source.stream) ? ":" + dev.source.stream : "");
                            obj.steps = this.buildDailySteps(dev.steps);
                            return obj;
                        }, this)
                    );
                } else {
                    callback("There is no any steps data for this period", false);
                }
            }
        );
    }

    buildDailySteps(steps) {
        let results = {};
        for (let step of steps) {
            if (step == undefined) continue;

            const date = new Date(step.startDate);

            const day = ("0" + date.getDate()).slice(-2);
            const month = ("0" + (date.getMonth() + 1)).slice(-2);
            const year = date.getFullYear();
            const dateFormatted = year + "-" + month + "-" + day;

            if (!(dateFormatted in results)) {
                results[dateFormatted] = 0;
            }

            results[dateFormatted] += step.steps;
        }

        let dateMap = [];
        for (let index in results) {
            dateMap.push({date: index, value: results[index]});
        }
        return dateMap;
    }

    /**
     * Get the total distance per day over a specified date range.
     * @param {Object} options getDailyDistanceSamples accepts an options object containing required startDate: ISO8601Timestamp and endDate: ISO8601Timestamp.
     * @callback callback The function will be called with an array of elements.
     */

    getDailyDistanceSamples(options, callback) {
        const startDate = options.startDate != undefined ? Date.parse(options.startDate) : (new Date()).setHours(0, 0, 0, 0);
        const endDate = options.endDate != undefined ? Date.parse(options.endDate) : (new Date()).valueOf();
        googleFit.getDailyDistanceSamples(startDate,
            endDate,
            (msg) => {
                callback(msg, false);
            },
            (res) => {
                if (res.length > 0) {
                    res = res.map((el) => {
                        if (el.distance) {
                            el.startDate = new Date(el.startDate).toISOString();
                            el.endDate = new Date(el.endDate).toISOString();
                            return el;
                        }
                    });
                    callback(false, res.filter(day => day != undefined));
                } else {
                    callback("There is no any distance data for this period", false);
                }
            });
    }

    getActivitySamples(options, callback) {
        googleFit.getActivitySamples(
            options.startDate,
            options.endDate,
            (error) => {
                callback(error, false);
            },
            (res) => {
                if (res.length>0) {
                    callback(false, res);
                } else {
                    callback("There is no any distance data for this period", false);
                }
            });
    }

    getWorkoutSamples(options, callback) {
        googleFit.getWorkoutSamples(
            options.startDate,
            options.endDate,
            (error) => {
                callback(error, false);
            },
            (res) => {
                callback(false, res);
            });
    }

    submitWorkout(options, callback) {
        googleFit.submitWorkout(
            options.startDate,
            options.endDate,
            options.workoutType,
            (error) => {
                callback(error, false);
            },
            (res) => {
                callback(false, res);
            });
    }

    /**
     * Get the total calories per day over a specified date range.
     * @param {Object} options getDailyCalorieSamples accepts an options object containing required startDate: ISO8601Timestamp and endDate: ISO8601Timestamp.
     * @callback callback The function will be called with an array of elements.
     */

    getDailyCalorieSamples(options, callback) {
        const startDate = Date.parse(options.startDate);
        const endDate = Date.parse(options.endDate);
        googleFit.getDailyCalorieSamples(
            startDate,
            endDate,
            (msg) => {
                callback(msg, false);
            },
            (res) => {
                if (res.length > 0) {
                    res = res.map((el) => {
                        if (el.calorie) {
                            el.startDate = new Date(el.startDate).toISOString();
                            el.endDate = new Date(el.endDate).toISOString();
                            return el;
                        }
                    });
                    callback(false, res.filter(day => day != undefined));
                } else {
                    callback("There is no any calorie data for this period", false);
                }
            });
    }

    saveFood(options, callback) {
        options.date = Date.parse(options.date);
        googleFit.saveFood(options,
            (msg) => {
                callback(msg, false);
            },
            (res) => {
                callback(false, res);
            });
    }

    /**
     * Query for weight samples. the options object is used to setup a query to retrieve relevant samples.
     * @param {Object} options  getDailyStepCountSamples accepts an options object containing unit: "pound"/"kg",
     *                          startDate: ISO8601Timestamp and endDate: ISO8601Timestamp.
     * @callback callback The function will be called with an array of elements.
     */

    getWeightSamples = (options, callback) => {
        const startDate = options.startDate != undefined ? Date.parse(options.startDate) : (new Date()).setHours(0, 0, 0, 0);
        const endDate = options.endDate != undefined ? Date.parse(options.endDate) : (new Date()).valueOf();
        googleFit.getWeightSamples(startDate,
            endDate,
            (msg) => {
                callback(msg, false);
            },
            (res) => {
                if (res.length > 0) {
                    res = res.map((el) => {
                        if (el.value) {
                            if (options.unit === 'pound') {
                                el.value = this.KgToLbs(el.value); //convert back to pounds
                            }
                            el.startDate = new Date(el.startDate).toISOString();
                            el.endDate = new Date(el.endDate).toISOString();
                            return el;
                        }
                    });
                    callback(false, res.filter(day => day != undefined));
                } else {
                    callback("There is no any weight data for this period", false);
                }
            });
    }

    getHeightSamples(options, callback) {
      let startDate = Date.parse(options.startDate);
      let endDate = Date.parse(options.endDate);
      googleFit.getHeightSamples( startDate,
        endDate,
        (msg) => {
          callback(msg, false);
        },
        (res) => {
          if (res.length>0) {
            res = res.map((el) => {
              if (el.value) {
                el.startDate = new Date(el.startDate).toISOString();
                el.endDate = new Date(el.endDate).toISOString();
                return el;
              }
            });
            callback(false, res.filter(day => day != undefined));
          } else {
            callback("There is no any height data for this period", false);
          }
        });
    }

    saveHeight(options, callback) {
      options.date = Date.parse(options.date);
      googleFit.saveHeight(options,
        (msg) => {
          callback(msg, false);
        },
        (res) => {
          callback(false, res);

        });
    }

    saveWeight(options, callback) {
        if (options.unit == 'pound') {
            options.value = this.lbsAndOzToK({ pounds: options.value, ounces: 0 }); //convert pounds and ounces to kg
        }
        options.date = Date.parse(options.date);
        googleFit.saveWeight(options,
            (msg) => {
                callback(msg, false);
            },
            (res) => {
                callback(false, res);
            });
    }

    deleteWeight = (options, callback) => {
        if (options.unit === 'pound') {
            options.value = this.lbsAndOzToK({pounds: options.value, ounces: 0}); //convert pounds and ounces to kg
        }
        options.date = Date.parse(options.date);
        googleFit.deleteWeight(options,
            (msg) => {
                callback(msg, false);
            },
            (res) => {
                callback(false, res);
            });
    }

    deleteHeight = (options, callback) => {
      options.date = Date.parse(options.date);
      googleFit.deleteWeight(options,
        (msg) => {
          callback(msg, false);
        },
        (res) => {
          callback(false, res);
        });
    }

    isAvailable(callback) { // true if GoogleFit installed
        googleFit.isAvailable(
            (msg) => {
                callback(msg, false);
            },
            (res) => {
                callback(false, res);
            });
    }

    isEnabled(callback) { // true if permission granted
        googleFit.isEnabled(
            (msg) => {
                callback(msg, false);
            },
            (res) => {
                callback(false, res);
            });
    }

    openFit() {
        googleFit.openFit();
    }

    observeSteps = (callback) => {
        const stepsObserver = DeviceEventEmitter.addListener(
            'StepChangedEvent',
            (steps) => callback(steps)
        );
        googleFit.observeSteps();
        this.eventListeners.push(stepsObserver)
    }

    observeHistory = (callback) => {
        const historyObserver = DeviceEventEmitter.addListener(
            'StepHistoryChangedEvent',
            (steps) => callback(steps)
        );
        this.eventListeners.push(historyObserver)
    }

    onAuthorize = (callback) => {
        const authObserver = DeviceEventEmitter.addListener(
            'GoogleFitAuthorizeSuccess',
            (authorized) => callback(authorized)
        );
        this.eventListeners.push(authObserver)
    }

    onAuthorizeFailure = (callback) => {
        const authFailedObserver = DeviceEventEmitter.addListener(
            'GoogleFitAuthorizeFailure',
            (authorized) => callback(authorized)
        );
        this.eventListeners.push(authFailedObserver)
    }

    unsubscribeListeners = () => {
        this.removeListeners()
    }

    lbsAndOzToK(imperial) {
        const pounds = imperial.pounds + imperial.ounces / 16;
        return pounds * 0.45359237;
    }

    KgToLbs(metric) {
        return metric * 2.2046;
    }

}

export default new RNGoogleFit();

//Data types for food addition
export const MealType = Object.freeze({
    UNKNOWN: 0,
    BREAKFAST: 1,
    LUNCH: 2,
    DINNER: 3,
    SNACK: 4
});

export const Nutrient = Object.freeze({
    /**
     * Calories in kcal
     * @type {string}
     */
    CALORIES: "calories",
    /**
     * Total fat in grams.
     * @type {string}
     */
    TOTAL_FAT: "fat.total",
    /**
     * Saturated fat in grams.
     * @type {string}
     */
    SATURATED_FAT: "fat.saturated",
    /**
     * Unsaturated fat in grams.
     * @type {string}
     */
    UNSATURATED_FAT: "fat.unsaturated",
    /**
     * Polyunsaturated fat in grams.
     * @type {string}
     */
    POLYUNSATURATED_FAT: "fat.polyunsaturated",
    /**
     * Monounsaturated fat in grams.
     * @type {string}
     */
    MONOUNSATURATED_FAT: "fat.monounsaturated",
    /**
     * Trans fat in grams.
     * @type {string}
     */
    TRANS_FAT: "fat.trans",
    /**
     * Cholesterol in milligrams.
     * @type {string}
     */
    CHOLESTEROL: "cholesterol",
    /**
     * Sodium in milligrams.
     * @type {string}
     */
    SODIUM: "sodium",
    /**
     * Potassium in milligrams.
     * @type {string}
     */
    POTASSIUM: "potassium",
    /**
     * Total carbohydrates in grams.
     * @type {string}
     */
    TOTAL_CARBS: "carbs.total",
    /**
     * Dietary fiber in grams
     * @type {string}
     */
    DIETARY_FIBER: "dietary_fiber",
    /**
     * Sugar amount in grams.
     * @type {string}
     */
    SUGAR: "sugar",
    /**
     * Protein amount in grams.
     * @type {string}
     */
    PROTEIN: "protein",
    /**
     * Vitamin A amount in International Units (IU).
     * @type {string}
     */
    VITAMIN_A: "vitamin_a",
    /**
     * Vitamin C amount in milligrams.
     * @type {string}
     */
    VITAMIN_C: "vitamin_c",
    /**
     * Calcium amount in milligrams.
     * @type {string}
     */
    CALCIUM: "calcium",
    /**
     * Iron amount in milligrams
     * @type {string}
     */
    IRON: "iron"
});

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
