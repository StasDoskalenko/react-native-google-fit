// tslint:disable:no-default-export interface-name
declare module 'react-native-google-fit' {
  export interface GoogleFit {
    eventListeners: any[]
    isAuthorized: boolean

    authorize(options?: AuthorizeOptions): Promise<AuthorizeResponse>

    checkIsAuthorized: () => Promise<void>

    disconnect(): void

    removeListeners: () => void

    /**
     * Start recording fitness data (steps, distance)
     * This function relies on sending events to signal the RecordingAPI status
     * Simply create an event listener for the {DATA_TYPE}_RECORDING (ex. STEP_RECORDING)
     * and check for {recording: true} as the event data
     */
    startRecording: (callback: (param: any) => void, dataTypes: Array<'step' | 'distance' | 'activity'>) => void

    /**
     * A shortcut to get the total steps of a given day by using getDailyStepCountSamples
     * @param {Date} date optional param, new Date() will be used if date is not provided
     */
    getDailySteps: (date?: Date) => Promise<StepsResponse[]>

    /**
     * A shortcut to get the weekly steps of a given day by using getDailyStepCountSamples
     * @param {Date} date optional param, new Date() will be used if date is not provided
     * @param {number} adjustment, optional param, use to adjust the default start day of week, 0 = Sunday, 1 = Monday, etc.
     */
    getWeeklySteps: (date?: Date, adjustment?: number) => Promise<StepsResponse[]>

    /**
     * Get the total steps per day over a specified date range.
     * @param {Object} options getDailyStepCountSamples accepts an options object containing optional startDate: ISO8601Timestamp and endDate: ISO8601Timestamp.
     */
    getDailyStepCountSamples: (
      options: StartAndEndDate & Partial<BucketOptions>
    ) => Promise<StepsResponse[]>

    /**
     * Get the total distance per day over a specified date range.
     * @param {Object} options getDailyDistanceSamples accepts an options object containing optional startDate: ISO8601Timestamp and endDate: ISO8601Timestamp.
     */

    getDailyDistanceSamples: (
      options: StartAndEndDate & Partial<BucketOptions>
    ) => Promise<DistanceResponse[]>

    /**
     * Get the total steps per day over a specified date range.
      * @param {Object} options getUserInputSteps accepts an options object containing optional startDate: ISO8601Timestamp and endDate: ISO8601Timestamp.
      * @param {Function} callback The function will be called with an array of elements.
      */
    getUserInputSteps: (options: Partial<StartAndEndDate>, callback: (isError?: boolean, result?: number)=> void ) => void;

    /**
     * Get the total distance per day over a specified date range.
     * @param {Object} options getActivitySamples accepts an options object containing required startDate: ISO8601Timestamp and endDate: ISO8601Timestamp.
     */
    getActivitySamples: (
      options: StartAndEndDate & Partial<BucketOptions>,
    ) => Promise<ActivitySampleResponse[]>;

    /**
     * Get the total calories per day over a specified date range.
     * @param {Object} options getDailyCalorieSamples accepts an options object containing required startDate: ISO8601Timestamp and endDate: ISO8601Timestamp. optional basalCalculation - {true || false} should we substract the basal metabolic rate averaged over a week
     */
    getDailyCalorieSamples: (
      options: StartAndEndDate & { basalCalculation?: boolean } & Partial<BucketOptions>
    ) => Promise<CalorieResponse[]>;

    getDailyNutritionSamples: (
      options: StartAndEndDate & Partial<BucketOptions>,
    ) => Promise<NutrientResponse[]>;

    saveFood: (
      options: FoodIntake,
      callback: (isError: boolean, result: true) => void
    ) => void;

    /**
     * Query for weight samples. the options object is used to setup a query to retrieve relevant samples.
     * @param {Object} options  getWeightSamples accepts an options object containing unit: "pound"/"kg",
     *                          startDate: ISO8601Timestamp and endDate: ISO8601Timestamp.
     */
    getWeightSamples: (
      options: StartAndEndDate & Partial<{ unit: string, ascending: boolean } & BucketOptions>
    ) => Promise<WeightResponse[]>;

    /**
     * Query for weight samples. the options object is used to setup a query to retrieve relevant samples.
     * @param {Object} options  getHeightSamples accepts an options object containing unit: "pound"/"kg",
     *                          startDate: ISO8601Timestamp and endDate: ISO8601Timestamp.
     */
    getHeightSamples: (
      options: StartAndEndDate
    ) => Promise<HeightResponse[]>;

    getHeartRateSamples: (
      options: StartAndEndDate & Partial<BucketOptions>
    ) => Promise<HeartRateResponse[]>;

    getBloodPressureSamples: (
      options: StartAndEndDate & Partial<BucketOptions>
    ) => Promise<BloodPressureResponse[]>;

    saveWeight: (
      options: WeightData,
      callback: (isError: boolean, result: true) => void
    ) => void

    saveHeight: (
      options: { date: string, value: number },
      callback: (isError: boolean, result: true) => void
    ) => void

    deleteWeight: (
      options: DeleteOptions,
      callback: (isError: boolean, result: true) => void
    ) => void

    deleteHeight: (
      options: DeleteOptions,
      callback: (isError: boolean, result: true) => void
    ) => void

    getHydrationSamples: (
      options: StartAndEndDate
    ) => Promise<HydrationResponse[]>

    saveHydration: (
      hydrationArray: Hydration[],
      callback: (isError: boolean, result: true) => void
    ) => void

    deleteHydration: (
      options: DeleteOptions,
      callback: (isError: boolean, result: true) => void
    ) => void

    /**
     * Get the sleep sessions over a specified date range.
     * @param {Object} options getSleepData accepts an options object containing required startDate: ISO8601Timestamp and endDate: ISO8601Timestamp.
     */
    getSleepSamples: (
      options: Partial<StartAndEndDate>
    ) => Promise<SleepSampleResponse[]>

    saveSleep: (
      options: SleepSample
    ) => Promise<Boolean | undefined>

    isAvailable(callback: (isError: boolean, result: boolean) => void): void

    isEnabled(callback: (isError: boolean, result: boolean) => void): void

    openFit(callback: (isError: boolean, result: boolean) => void): void

    observeSteps: (callback: (isError: boolean, result: any) => void) => void

    observeHistory: (callback: (isError: boolean, result: any) => void) => void

    onAuthorize: (callback: (isError: boolean, result: any) => void) => void

    onAuthorizeFailure: (
      callback: (isError: boolean, result: any) => void
    ) => void

    unsubscribeListeners: () => void

  }

  export type Day = "Mon" | "Tue" | "Wed" | "Thu" | "Fri" | "Sat" | "Sun";

  export type BucketUnit = "NANOSECOND" | "MICROSECOND" | "MILLISECOND" | "SECOND" | "MINUTE" | "HOUR" | "DAY";

  export type WeightResponse = {
    addedBy: string
    day: string
    value: number
    startDate: string
    endDate: string
  };

  export type HeightResponse = WeightResponse;

  export type HydrationResponse = {
    addedBy: string
    waterConsumed: number
    date: string
  };

  export type FoodIntake = {
    mealType: MealType
    foodName: string
    nutrients: Nutrients
    date: string
  };

  export type AuthorizeOptions = {
    scopes: Array<Scopes>
  };

  export type Hydration = {
    date: number
    waterConsumed: number
  };

  export type DeleteOptions = {
    startDate: string | number
    endDate: string | number
  };

  export type StartAndEndDate = {
    startDate: string,
    endDate: string,
  };


  export type NumericalStartAndEndDate = {
    startDate: number,
    endDate: number
  };

  export type BucketOptions = {
    bucketInterval: number,
    bucketUnit: BucketUnit
  };

  type rawSteps = Array<{startDate: string, endDate: string, steps: number}>;
  
  export type StepsResponse = {
    source: string,
    steps: Array<{date: string, value: number }>,
    rawSteps: rawSteps
  };

  export type CalorieResponse = {
    calorie: number,
    endDate: string,
    startDate: string,
    day: Day
  };

  export type DistanceResponse = {
    distance: number,
    endDate: string,
    startDate: string,
    day: Day
  };

  export type HeartRateResponse = {
    startDate: string,
    endDate: string,
    value: number,
    day: Day
  };

  export type BloodPressureResponse = {
    startDate: string,
    endDate: string,
    diastolic: number,
    systolic: number,
    day: Day
  }

  export type WeightData = { date: string } & ({ unit: 'pound', value: number } | {});

  export type AuthorizeResponse = { success: true} | {success: false, message: string };

  export type DateValueResponse = {
    value: number,
    endDate:string,
    startDate:string,
    day: Day
  }

  export type ActivitySampleResponse = {
    sourceName: string,
    device: string,
    sourceId: string,
    tracked: boolean,
    activityName: string,
    end: number,
    start: number 
    calories?: number,
    quantity?: number,
    distance?: number
  }

  export type NutrientResponse = {
    nutrients: Nutrients
    date: string
  };

  export type SleepSampleResponse = {
    addedBy: string,
    startDate: string,
    endDate: string,
    granularity: Granularity[]
  }

  export type SleepSample = {
    startDate: number,
    endDate: number,
    sessionName: string,
    identifier: string,
    description: string,
    granularity: Granularity[]
  }

  export type Granularity = {
    startDate: string | number,
    endDate: string | number,
    sleepStage: number,
  }

  export enum MealType {
    UNKNOWN = 0,
    BREAKFAST = 1,
    LUNCH = 2,
    DINNER = 3,
    SNACK = 4,
  }

  export type Nutrients = {
    [ key in Nutrient ]?: number
  }

  export enum Nutrient {
    /**
     * Calories in kcal
     * @type {string}
     */
    CALORIES = 'calories',
    /**
     * Total fat in grams.
     * @type {string}
     */
    TOTAL_FAT = 'fat.total',
    /**
     * Saturated fat in grams.
     * @type {string}
     */
    SATURATED_FAT = 'fat.saturated',
    /**
     * Unsaturated fat in grams.
     * @type {string}
     */
    UNSATURATED_FAT = 'fat.unsaturated',
    /**
     * Polyunsaturated fat in grams.
     * @type {string}
     */
    POLYUNSATURATED_FAT = 'fat.polyunsaturated',
    /**
     * Monounsaturated fat in grams.
     * @type {string}
     */
    MONOUNSATURATED_FAT = 'fat.monounsaturated',
    /**
     * Trans fat in grams.
     * @type {string}
     */
    TRANS_FAT = 'fat.trans',
    /**
     * Cholesterol in milligrams.
     * @type {string}
     */
    CHOLESTEROL = 'cholesterol',
    /**
     * Sodium in milligrams.
     * @type {string}
     */
    SODIUM = 'sodium',
    /**
     * Potassium in milligrams.
     * @type {string}
     */
    POTASSIUM = 'potassium',
    /**
     * Total carbohydrates in grams.
     * @type {string}
     */
    TOTAL_CARBS = 'carbs.total',
    /**
     * Dietary fiber in grams
     * @type {string}
     */
    DIETARY_FIBER = 'dietary_fiber',
    /**
     * Sugar amount in grams.
     * @type {string}
     */
    SUGAR = 'sugar',
    /**
     * Protein amount in grams.
     * @type {string}
     */
    PROTEIN = 'protein',
    /**
     * Vitamin A amount in International Units (IU).
     * @type {string}
     */
    VITAMIN_A = 'vitamin_a',
    /**
     * Vitamin C amount in milligrams.
     * @type {string}
     */
    VITAMIN_C = 'vitamin_c',
    /**
     * Calcium amount in milligrams.
     * @type {string}
     */
    CALCIUM = 'calcium',
    /**
     * Iron amount in milligrams
     * @type {string}
     */
    IRON = 'iron',
  }

  export enum Scopes {
    FITNESS_ACTIVITY_READ = 'https://www.googleapis.com/auth/fitness.activity.read',
    FITNESS_ACTIVITY_WRITE = 'https://www.googleapis.com/auth/fitness.activity.write',
    FITNESS_LOCATION_READ = 'https://www.googleapis.com/auth/fitness.location.read',
    FITNESS_LOCATION_WRITE = 'https://www.googleapis.com/auth/fitness.location.write',
    FITNESS_BODY_READ = 'https://www.googleapis.com/auth/fitness.body.read',
    FITNESS_BODY_WRITE = 'https://www.googleapis.com/auth/fitness.body.write',
    FITNESS_NUTRITION_READ = 'https://www.googleapis.com/auth/fitness.nutrition.read',
    FITNESS_NUTRITION_WRITE = 'https://www.googleapis.com/auth/fitness.nutrition.write',
    FITNESS_BLOOD_PRESSURE_READ = 'https://www.googleapis.com/auth/fitness.blood_pressure.read',
    FITNESS_BLOOD_PRESSURE_WRITE = 'https://www.googleapis.com/auth/fitness.blood_pressure.write',
    FITNESS_BLOOD_GLUCOSE_READ = 'https://www.googleapis.com/auth/fitness.blood_glucose.read',
    FITNESS_BLOOD_GLUCOSE_WRITE = 'https://www.googleapis.com/auth/fitness.blood_glucose.write',
    FITNESS_OXYGEN_SATURATION_READ = 'https://www.googleapis.com/auth/fitness.oxygen_saturation.read',
    FITNESS_OXYGEN_SATURATION_WRITE = 'https://www.googleapis.com/auth/fitness.oxygen_saturation.write',
    FITNESS_BODY_TEMPERATURE_READ = 'https://www.googleapis.com/auth/fitness.body_temperature.read',
    FITNESS_BODY_TEMPERATURE_WRITE = 'https://www.googleapis.com/auth/fitness.body_temperature.write',
    FITNESS_REPRODUCTIVE_HEALTH_READ = 'https://www.googleapis.com/auth/fitness.reproductive_health.read',
    FITNESS_REPRODUCTIVE_HEALTH_WRITE = 'https://www.googleapis.com/auth/fitness.reproductive_health.write',
    FITNESS_SLEEP_READ =  'https://www.googleapis.com/auth/fitness.sleep.read',
    FITNESS_SLEEP_WRITE = 'https://www.googleapis.com/auth/fitness.sleep.write',
    FITNESS_HEART_RATE_READ = 'https://www.googleapis.com/auth/fitness.heart_rate.read',
    FITNESS_HEART_RATE_WRITE= 'https://www.googleapis.com/auth/fitness.heart_rate.write'
  }

  const googleFit: GoogleFit;
  export default googleFit;
}
