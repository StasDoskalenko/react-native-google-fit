'use strict'

import {
        NativeModules,
        DeviceEventEmitter
        } from 'react-native';

const googleFit = NativeModules.RNGoogleFit;

import moment from 'moment';


class RNGoogleFit {
    constructor() {
    }

    authorizeFit() {
        googleFit.authorize();
    }

    getSteps(dayStart,dayEnd) {
        googleFit.getDailySteps(dayStart.toDate().getTime(), dayEnd.toDate().getTime());
    }

    getWeeklySteps(startDate) {
        googleFit.getWeeklySteps(startDate.toDate().getTime(), moment().toDate().getTime());
    }

    getWeightSamples(startDate,endDate,callback) {
        googleFit.getWeightSamples( startDate,
                                    endDate,
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

}

export default new RNGoogleFit();

//module.exports = RNGoogleFit;
