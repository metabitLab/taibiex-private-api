package com.metabitlab.taibiex.privateapi.util;

import java.time.*;

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

    public static long getStartOfDayTimestamp () {

        LocalDate currentDate = LocalDate.now();

        // 将当天的时间设置为0点
        LocalDate oneMonthAgoMidnight = currentDate.atStartOfDay().toLocalDate();

        // 获取时间戳
        long timestamp = oneMonthAgoMidnight.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli();

        return timestamp/1000;
    }

    /**
     * 从当前分钟数起，过去每5分钟获取一次时间戳
     * @param length 获取时间戳的个数
     */
    public static long[] getFiveMinuteTimestamp (int length) {
        long[] timestamps = new long[length];
        long currentMinuteTimestamp = getCurrentMinuteTimestamp();
        for (int i = 0; i < length; i++) {
            timestamps[i] = currentMinuteTimestamp - (i * 300);
        }
        return timestamps;
    }

    /**
     * 获取过去每分钟的起始时间戳
     * @param length
     * @return
     */
    public static long[] getLastMinuteTimestamp (int length) {
        long[] timestamps = new long[length];
        long currentMinuteTimestamp = getCurrentMinuteTimestamp();
        for (int i = 0; i < length; i++) {
            timestamps[i] = currentMinuteTimestamp - ( (length - i) * 60);
        }
        return timestamps;
    }

    /**
     * 从当前小时起始时间起，获取过去每小时的起始时间戳
     * @param length
     * @return
     */
    public static long[] getLastHourTimestamp (int length) {
        long[] timestamps = new long[length];
        long currentHourTimestamp = getCurrentHourTimestamp();
        for (int i = 0; i < length; i++) {
            timestamps[i] = currentHourTimestamp - ( (length - i) * 3600);
        }
        return timestamps;
    }

    /*
     * 从当前日期起始时间起，每天获取一次时间戳
     * @param length 获取时间戳的个数
     */
    public static long[] getLastDayTimestamp (int length) {
        long[] timestamps = new long[length];
        long currentDayTimestamp = getCurrentDayTimestamp();
        for (int i = 0; i < length; i++) {
            timestamps[i] = currentDayTimestamp - ((length - i) * 86400);
        }
        return timestamps;
    }

    /*
     * 从当前日期起始时间起，每7天获取一次时间戳
     * @param length 获取时间戳的个数
     */
    public static long[] getSevenDayTimestamp (int length) {
        long[] timestamps = new long[length];
        long currentDayTimestamp = getCurrentDayTimestamp();
        for (int i = 0; i < length; i++) {
            timestamps[i] = currentDayTimestamp - (i * 86400 * 7);
        }
        return timestamps;
    }


    /*
     * 从当前小时起始时间起，每6个小时获取一次时间戳
     * @param length 获取时间戳的个数
     */
    public static long[] getSixHourTimestamp (int length) {
        long[] timestamps = new long[length];
        long currentHourTimestamp = getCurrentHourTimestamp();
        for (int i = 0; i < length; i++) {
            timestamps[i] = currentHourTimestamp - (i * 3600 * 6);
        }
        return timestamps;
    }

    /*
     * 从当前小时起始时间起，每4个小时获取一次时间戳
     * @param length 获取时间戳的个数
     */
    public static long[] get4HourTimestamp (int length) {
        long[] timestamps = new long[length];
        long currentHourTimestamp = getCurrentHourTimestamp();
        for (int i = 0; i < length; i++) {
            timestamps[i] = currentHourTimestamp - (i * 3600 * 4);
        }
        return timestamps;
    }

    /*
     * 从当前小时起始时间起，获取过去24小时每个小时的起始时间戳
     */
    public static long[] get24HourTimestamp () {
        long[] timestamps = new long[24];
        long currentHourTimestamp = getCurrentHourTimestamp();
        for (int i = 0; i < 24; i++) {
            timestamps[i] = currentHourTimestamp - (i * 3600);
        }
        return timestamps;
    }

    public static long getCurrentDayTimestamp () {

        long l = System.currentTimeMillis() / 1000 - (System.currentTimeMillis() / 1000) % 86400;

        return l;
    }

    /**
     * 获取当前分钟数起始时间戳
     */
    public static long getCurrentMinuteTimestamp () {

        long l = System.currentTimeMillis() / 1000 - (System.currentTimeMillis() / 1000) % 60;

        return l;
    }

    /**
     * 获取当前小时起始时间戳
     */
    public static long getCurrentHourTimestamp () {

        long l = System.currentTimeMillis() / 1000 - (System.currentTimeMillis() / 1000) % 3600;

        return l;
    }


    public static void main(String[] args) {
        long[] longs = getLastMinuteTimestamp(60);
        for (long aLong : longs) {
            System.out.println(aLong);
        }
    }

}
