package org.bahmni.module.hip.utils;

import org.bahmni.module.hip.utils.DateUtil;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DateUtils {
    public static Date parseDate(String date) throws ParseException {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        return dateFormat.parse(date);
    }

    public static Date parseDateTime(String date) throws ParseException {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return dateFormat.parse(date);
    }

    public static boolean isDateBetweenDateRange(Date date, String fromDate, String toDate) throws ParseException {
        String dateString = new SimpleDateFormat("yyyy-MM-dd").format(date);
        Date inputDate = parseDate(dateString);
        return inputDate.compareTo(parseDate(fromDate)) >= 0 && inputDate.compareTo(parseDate(toDate)) <= 0;
    }

    public static Date validDate(String value) {
        try {
            return DateUtil.parseDate(value);
        } catch (RuntimeException re) {
           //log
        }
        return null;
    }
}
