package com.reactnative.googlefit;

import java.util.concurrent.TimeUnit;

final class HelperUtil {
    public static TimeUnit processBucketUnit(String buckUnit) {
        switch (buckUnit){
            case "NANOSECONDS": return TimeUnit.NANOSECONDS;
            case "MICROSECONDS": return TimeUnit.MICROSECONDS;
            case "MILLISECONDS": return TimeUnit.MILLISECONDS;
            case "SECOND": return TimeUnit.SECONDS;
            case "MINUTE": return TimeUnit.MINUTES;
            case "HOUR": return TimeUnit.HOURS;
            case "DAY": return TimeUnit.DAYS;
        }
        return TimeUnit.HOURS;
    }
}
