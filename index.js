'use strict'

import {
    NativeModules,
    DeviceEventEmitter
} from 'react-native';

const googleFit = NativeModules.RNGoogleFit;

class RNGoogleFit {
    constructor() {
    }

    authorizeFit() {
        googleFit.authorize();
    }

    getSteps(dayStart,dayEnd) { //TODO: refactor method as in react-native-apple-healthkit
        googleFit.getDailySteps(Date.parse(dayStart), Date.parse(dayEnd));
    }

    getWeeklySteps(startDate) { //TODO: refactor method as in react-native-apple-healthkit
        googleFit.getWeeklySteps(Date.parse(startDate), Date.now());
    }

    getWeightSamples(options,callback) {
        let startDate = Date.parse(options.startDate);
        let endDate = Date.parse(options.endDate);
        googleFit.getWeightSamples( startDate,
            endDate,
            (msg) => {
                callback(msg, false);
            },
            (res) => {
                if (res.length>0) {
                    res = res.map((el) => {
                        if (el.value) {
                            if (options.unit == 'pound') {
                                el.value = this.KgToLbs(el.value); //convert back to pounds
                            }
                            el.startDate = new Date(el.startDate).toISOString();
                            el.endDate = new Date(el.endDate).toISOString();
                            return el;
                        }
                    });
                    callback(false, res.filter(day => day != undefined));
                } else {
                    callback("There is no any fit data for this period", false);
                }
            });
    }

    saveWeight(options, callback) {
        if (options.unit == 'pound') {
            options.value = this.lbsAndOzToK({ pounds: options.value, ounces: 0 }); //convert pounds and ounces to kg
        }
        options.date = Date.parse(options.date);
        googleFit.saveWeight( options,
            (msg) => {
                callback(msg,false);
            },
            (res) => {
                callback(false,res);

            });
    }

    observeSteps(callback) {
        DeviceEventEmitter.addListener(
            'StepChangedEvent',
            (steps) => callback(steps)
        );

        googleFit.observeSteps();
    }

    observeHistory(callback) {
        DeviceEventEmitter.addListener(
            'StepHistoryChangedEvent',
            (steps) => callback(steps)
        );
    }

    onAuthorize(callback) {
        DeviceEventEmitter.addListener(
            'AuthorizeEvent',
            (authorized) => callback(authorized)
        );
    }

    usubscribeListeners() {
        DeviceEventEmitter.removeAllListeners();
    }

    lbsAndOzToK(imperial) {
        let pounds = imperial.pounds + imperial.ounces / 16;
        return pounds * 0.45359237;
    }

    KgToLbs(metric) {
        return metric * 2.2046;
    }

}

export default new RNGoogleFit();