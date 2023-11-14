import moment from 'moment';

export function buildDailySteps(steps) {
  const results = {}
  for (const step of steps) {
    if (step == undefined) {
      continue
    }

    const dateFormatted = getFormattedDate(new Date(step.endDate))

    if (!(dateFormatted in results)) {
      results[dateFormatted] = 0
    }

    results[dateFormatted] += step.steps
  }

  const dateMap = []
  for (const index in results) {
    dateMap.push({ date: index, value: results[index] })
  }
  return dateMap
}

export function lbsAndOzToK(imperial) {
  const pounds = imperial.pounds + imperial.ounces / 16
  return pounds * 0.45359237
}

export const KgToLbs = metric => metric * 2.2046

export function isNil(value) {
  return value == null
}

// parse the default config based on user input
export function prepareInput(options) {
  const startDate = !isNil(options.startDate)
  ? Date.parse(options.startDate)
  : new Date().setHours(0, 0, 0, 0)
  const endDate = !isNil(options.endDate)
    ? Date.parse(options.endDate)
    : new Date().valueOf();
  const bucketInterval = options.bucketInterval || 1;
  const bucketUnit = options.bucketUnit || "DAY";

  return { startDate, endDate, bucketInterval, bucketUnit };
}

export function prepareResponse(response, byKey = 'value', inLocalTimeZone = false) {
  return response
    .map(el => {
      if (!isNil(el[byKey])) {
        // Android is returning a date format from Fit that new Date() can't parse, e.g.: 2020-05-21T06:06:05.871-0400
        // Note the time offset at the end rather is non-standard compared to ISO which new Date expects. This works in
        // the Chrome V8 debugger, but not on device. Using momentJS here to get around this issue.
        // el.startDate = new Date(el.startDate).toISOString()
        // el.endDate = new Date(el.endDate).toISOString()
        if (inLocalTimeZone) {
          el.startDate = moment.parseZone(el.startDate).toISOString(true)
          el.endDate = moment.parseZone(el.endDate).toISOString(true)
        } else {
          el.startDate = moment(el.startDate).toISOString()
          el.endDate = moment(el.endDate).toISOString()
        }
        return el
      }
    })
    .filter(day => !isNil(day))
}

export function prepareDailyResponse(response) {
  return response.map(el => {
    el.date = getFormattedDate(new Date(el.date))
    return el
  })
}

export function prepareHydrationResponse(response) {
  return response.map(el => {
    el.date = new Date(el.date).toISOString()
    el.waterConsumed = Number(el.waterConsumed).toFixed(3)
    return el
  })
}

function getFormattedDate(date) {
  const day = ('0' + date.getDate()).slice(-2)
  const month = ('0' + (date.getMonth() + 1)).slice(-2)
  const year = date.getFullYear()
  return year + '-' + month + '-' + day
}

/***
* avoid month boundary issue by using millisecond calculation
* TimeZone Issue
* moment.js can be used as better alternative
*/

// export function getWeekBoundary(date, adjustment) {
//   const dayMilliseconds = 24 * 60 * 60 * 1000;
//   const currentWeekDay = date.getDay() - adjustment % 7;
//   const startDate = new Date(date.setHours(0,0,0,0) - dayMilliseconds * currentWeekDay);
//   const endDate = new Date(startDate.getTime() + dayMilliseconds * 7 -1);
//   return [startDate, endDate];
// }

export function getWeekBoundary(date, adjustment) {
  const startDate = moment(date).startOf('week').add(adjustment, 'days');
  const endDate = moment(date).endOf('week').add(adjustment, 'days');
  return [startDate, endDate];
}

export function prepareDeleteOptions(options) {
  return {
    ...options,
    startDate: typeof options.startDate !== 'number' ? Date.parse(options.startDate) : options.startDate,
    endDate: typeof options.endDate !== 'number' ? Date.parse(options.endDate) : options.endDate,
  }
}
