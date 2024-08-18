package com.metabitlab.taibiex.privateapi.util;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;

public class DateUtil {

    public static long getAneMonthAgoMidnightTimestamp () {

        LocalDate currentDate = LocalDate.now();

        // 获取一个月之前的日期
        LocalDate oneMonthAgo = currentDate.minusMonths(1);

        // 将当天的时间设置为0点
        LocalDate oneMonthAgoMidnight = oneMonthAgo.atStartOfDay().toLocalDate();

        // 获取时间戳
        long timestamp = oneMonthAgoMidnight.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli();

        return timestamp/1000;
    }

    public static long getAneDayAgoMidnightTimestamp () {

        LocalDate currentDate = LocalDate.now();

        // 获取一个月之前的日期
        LocalDate oneMonthAgo = currentDate.minusMonths(1);

        // 将当天的时间设置为0点
        LocalDate oneMonthAgoMidnight = oneMonthAgo.atStartOfDay().toLocalDate();

        // 获取时间戳
        long timestamp = oneMonthAgoMidnight.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli();

        return timestamp/1000;
    }

    public static long getAneHourAgoMidnightTimestamp () {

        LocalDate currentDate = LocalDate.now();

        // 获取一个月之前的日期
        LocalDate oneMonthAgo = currentDate.minusMonths(1);

        // 将当天的时间设置为0点
        LocalDate oneMonthAgoMidnight = oneMonthAgo.atStartOfDay().toLocalDate();

        // 获取时间戳
        long timestamp = oneMonthAgoMidnight.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli();

        return timestamp/1000;
    }

    public static long getAneYearAgoMidnightTimestamp () {

        LocalDate currentDate = LocalDate.now();

        // 获取一个月之前的日期
        LocalDate oneMonthAgo = currentDate.minusMonths(1);

        // 将当天的时间设置为0点
        LocalDate oneMonthAgoMidnight = oneMonthAgo.atStartOfDay().toLocalDate();

        // 获取时间戳
        long timestamp = oneMonthAgoMidnight.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli();

        return timestamp/1000;
    }

    public static long getAneWeekAgoMidnightTimestamp () {

        LocalDate currentDate = LocalDate.now();

        // 获取一个月之前的日期
        LocalDate oneMonthAgo = currentDate.minusMonths(1);

        // 将当天的时间设置为0点
        LocalDate oneMonthAgoMidnight = oneMonthAgo.atStartOfDay().toLocalDate();

        // 获取时间戳
        long timestamp = oneMonthAgoMidnight.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli();

        return timestamp/1000;
    }

    public static void main(String[] args) {
        long aneMonthAgoMidnightTimestamp = getAneMonthAgoMidnightTimestamp();

        System.out.println(aneMonthAgoMidnightTimestamp / 86400);
    }

    public static long getStartOfDayTimestamp () {

        LocalDate currentDate = LocalDate.now();

        // 将当天的时间设置为0点
        LocalDate oneMonthAgoMidnight = currentDate.atStartOfDay().toLocalDate();

        // 获取时间戳
        long timestamp = oneMonthAgoMidnight.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli();

        return timestamp/1000;
    }

}
