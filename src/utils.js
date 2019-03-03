export function lbsAndOzToK(imperial) {
  const pounds = imperial.pounds + imperial.ounces / 16
  return pounds * 0.45359237
}

export const KgToLbs = (metric) => metric * 2.2046
