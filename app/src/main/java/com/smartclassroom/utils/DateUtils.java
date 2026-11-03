package com.smartclassroom.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class DateUtils {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("hh:mm a", Locale.getDefault());
    private static final SimpleDateFormat DATE_TIME_FORMAT = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());

    private DateUtils() {}

    public static String formatDate(long timestamp) {
        return DATE_FORMAT.format(new Date(timestamp));
    }

    public static String formatTime(long timestamp) {
        return TIME_FORMAT.format(new Date(timestamp));
    }

    public static String formatDateTime(long timestamp) {
        return DATE_TIME_FORMAT.format(new Date(timestamp));
    }

    public static String getCurrentDate() {
        return DATE_FORMAT.format(new Date());
    }
}
