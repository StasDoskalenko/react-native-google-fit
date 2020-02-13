export function buildDailySteps(steps) {
  const results = {}
  for (const step of steps) {
    if (step == undefined) {
      continue
    }

    const dateFormatted = getFormattedDate(new Date(step.startDate))

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

export function prepareResponse(response, byKey = 'value') {
  return response
    .map(el => {
      if (!isNil(el[byKey])) {
        el.startDate = new Date(el.startDate).toISOString()
        el.endDate = new Date(el.endDate).toISOString()
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

export function prepareDeleteOptions(options) {
  return {
    ...options,
    startDate: typeof options.startDate !== 'number' ? Date.parse(options.startDate) : options.startDate,
    endDate: typeof options.endDate !== 'number' ? Date.parse(options.endDate) : options.endDate,
  }
}