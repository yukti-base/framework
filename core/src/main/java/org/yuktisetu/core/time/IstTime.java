package org.yuktisetu.core.time;

import java.time.ZoneId;
import java.util.TimeZone;

/**
 * The one timezone this system lives in: Indian Standard Time, Asia/Kolkata (+05:30,
 * no daylight saving). Every date and time the platform stores, logs, serialises or
 * shows is meant to be read as IST with no conversion; reference this instead of
 * writing "Asia/Kolkata" or ZoneOffset.UTC in new code.
 */
public final class IstTime {

    private IstTime() {}

    public static final String ZONE_NAME = "Asia/Kolkata";
    public static final ZoneId ZONE = ZoneId.of(ZONE_NAME);
    public static final TimeZone TIME_ZONE = TimeZone.getTimeZone(ZONE);
}
