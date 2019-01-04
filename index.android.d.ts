// tslint:disable:no-default-export interface-name
declare module "react-native-google-fit" {
    export interface GoogleFit {
        eventListeners: any [];

        authorize(): void;

        disconnect(): void;

        removeListeners: () => void;

        /**
         * Start recording fitness data (steps, distance)
         * This function relies on sending events to signal the RecordingAPI status
         * Simply create an event listener for the {DATA_TYPE}_RECORDING (ex. STEP_RECORDING)
         * and check for {recording: true} as the event data
         */
        startRecording: (callback: ((param: any) => void)) => void;

        getSteps(dayStart: Date | string, dayEnd: Date | string): any;

        getWeeklySteps(startDate: Date | string): any;

        /**
         * Get the total steps per day over a specified date range.
         * @param {Object} options getDailyStepCountSamples accepts an options object containing required startDate: ISO8601Timestamp and endDate: ISO8601Timestamp.
         * @param {Function} callback The function will be called with an array of elements.
         */
        getDailyStepCountSamples: (options: any, callback: ((isError: boolean, result: any) => void)) => void;

        buildDailySteps(steps: any): { date: any, value: any }[];

        /**
         * Get the total distance per day over a specified date range.
         * @param {Object} options getDailyDistanceSamples accepts an options object containing required startDate: ISO8601Timestamp and endDate: ISO8601Timestamp.
         * @callback {Function} callback The function will be called with an array of elements.
         */
        getDailyDistanceSamples(options: any, callback: ((isError: boolean, result: any) => void)): void;

        /**
        * Get the total distance per day over a specified date range.
        * @param {Object} options getActivitySamples accepts an options object containing required startDate: ISO8601Timestamp and endDate: ISO8601Timestamp.
        * @callback {Function} callback The function will be called with an array of elements.
        */
         getActivitySamples(options: any, callback: ((isError: boolean, result: any) => void)): void;

        /**
         * Get the total calories per day over a specified date range.
         * @param {Object} options getDailyCalorieSamples accepts an options object containing required startDate: ISO8601Timestamp and endDate: ISO8601Timestamp.
         * @callback {Function} callback The function will be called with an array of elements.
         */
        getDailyCalorieSamples(options: any, callback: ((isError: boolean, result: any) => void)): void;

        saveFood(options: FoodIntake, callback: ((isError: boolean, result: any) => void)): void;
        updateFood(options: FoodIntake, callback: ((isError: boolean, result: any) => void)): void;
        deleteFood(options: FoodDeletion, callback: ((isError: boolean, result: any) => void)): void;

        /**
         * Query for weight samples. the options object is used to setup a query to retrieve relevant samples.
         * @param {Object} options  getWeightSamples accepts an options object containing unit: "pound"/"kg",
         *                          startDate: ISO8601Timestamp and endDate: ISO8601Timestamp.
         * @callback callback The function will be called with an array of elements.
         */
        getWeightSamples: (options: any, callback: ((isError: boolean, result: WeightSample[]) => void)) => void;

        /**
         * Query for weight samples. the options object is used to setup a query to retrieve relevant samples.
         * @param {Object} options  getHeightSamples accepts an options object containing unit: "pound"/"kg",
         *                          startDate: ISO8601Timestamp and endDate: ISO8601Timestamp.
         * @callback callback The function will be called with an array of elements.
         */
        getHeightSamples: (options: any, callback: ((isError: boolean, result: WeightSample[]) => void)) => void;

        saveWeight: (options: any, callback: ((isError: boolean, result: any) => void)) => void;

        saveHeight: (options: any, callback: ((isError: boolean, result: any) => void)) => void;

        deleteWeight: (options: any, callback: ((isError: boolean, result: any) => void)) => void;

        isAvailable(callback: ((isError: boolean, result: boolean) => void)): void;

        isEnabled(callback: ((isError: boolean, result: boolean) => void)): void;

        openFit(): void;

        observeSteps: (callback: ((isError: boolean, result: any) => void)) => void;

        observeHistory: (callback: ((isError: boolean, result: any) => void)) => void;

        onAuthorize: (callback: ((isError: boolean, result: any) => void)) => void;

        onAuthorizeFailure: (callback: ((isError: boolean, result: any) => void)) => void;

        unsubscribeListeners: () => void;

        lbsAndOzToK(imperial: any): any;

        KgToLbs(metric: any): any;
    }

    export interface WeightSample {
        day: string;
        value: number;
        startDate: string;
        endDate: string;
    }

    export interface FoodIntake {
        mealType: MealType;
        foodName: string;
        nutrients: Object;
        date: string;
    }

    export interface FoodDeletion {
        date: string;
    }

    export enum MealType {
        UNKNOWN = 0,
        BREAKFAST = 1,
        LUNCH = 2,
        DINNER = 3,
        SNACK = 4
    }

    export enum Nutrient {
        /**
         * Calories in kcal
         * @type {string}
         */
        CALORIES = "calories",
        /**
         * Total fat in grams.
         * @type {string}
         */
        TOTAL_FAT = "fat.total",
        /**
         * Saturated fat in grams.
         * @type {string}
         */
        SATURATED_FAT = "fat.saturated",
        /**
         * Unsaturated fat in grams.
         * @type {string}
         */
        UNSATURATED_FAT = "fat.unsaturated",
        /**
         * Polyunsaturated fat in grams.
         * @type {string}
         */
        POLYUNSATURATED_FAT = "fat.polyunsaturated",
        /**
         * Monounsaturated fat in grams.
         * @type {string}
         */
        MONOUNSATURATED_FAT = "fat.monounsaturated",
        /**
         * Trans fat in grams.
         * @type {string}
         */
        TRANS_FAT = "fat.trans",
        /**
         * Cholesterol in milligrams.
         * @type {string}
         */
        CHOLESTEROL = "cholesterol",
        /**
         * Sodium in milligrams.
         * @type {string}
         */
        SODIUM = "sodium",
        /**
         * Potassium in milligrams.
         * @type {string}
         */
        POTASSIUM = "potassium",
        /**
         * Total carbohydrates in grams.
         * @type {string}
         */
        TOTAL_CARBS = "carbs.total",
        /**
         * Dietary fiber in grams
         * @type {string}
         */
        DIETARY_FIBER = "dietary_fiber",
        /**
         * Sugar amount in grams.
         * @type {string}
         */
        SUGAR = "sugar",
        /**
         * Protein amount in grams.
         * @type {string}
         */
        PROTEIN = "protein",
        /**
         * Vitamin A amount in International Units (IU).
         * @type {string}
         */
        VITAMIN_A = "vitamin_a",
        /**
         * Vitamin C amount in milligrams.
         * @type {string}
         */
        VITAMIN_C = "vitamin_c",
        /**
         * Calcium amount in milligrams.
         * @type {string}
         */
        CALCIUM = "calcium",
        /**
         * Iron amount in milligrams
         * @type {string}
         */
        IRON = "iron"
    }

    const googleFit: GoogleFit;
    export default googleFit;
}