package com.gtbluesky.mediamatrix.player.util;

import java.text.SimpleDateFormat;
import java.util.TimeZone;

public class TimeUtil {

    public static String getFormatTime(int time) {
        return getFormatTime(time, "mm:ss");
    }

    public static String getFormatTime(int time, String pattern) {
        try {
            SimpleDateFormat formatter = new SimpleDateFormat(pattern);
            formatter.setTimeZone(TimeZone.getTimeZone("GMT+00:00"));
            return formatter.format(time);
        } catch (Exception e) {
            LogUtil.e(e.getMessage());
            return "00:00";
        }
    }

}
