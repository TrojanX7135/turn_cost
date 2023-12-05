/*
 *  Licensed to GraphHopper GmbH under one or more contributor
 *  license agreements. See the NOTICE file distributed with this work for
 *  additional information regarding copyright ownership.
 *
 *  GraphHopper GmbH licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except in
 *  compliance with the License. You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.graphhopper.reader.osm.conditional;

import com.graphhopper.util.Helper;

import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.graphhopper.util.Helper.createFormatter;

/**
 * Parses a DateRange from OpenStreetMap. Currently only DateRanges that last at least one day are
 * supported. The Syntax is allowed inputs is described here:
 * http://wiki.openstreetmap.org/wiki/Key:opening_hours.
 * <p>
 *
 * @author Robin Boldt
 */
public class DateTimeRangeParser implements ConditionalValueParser {
    private static final SimpleDateFormat HOUR_MINUTE = createTimeFormatter("HH:mm");
    private static final DateFormat YEAR_MONTH_DAY_DF = create3CharMonthFormatter("yyyy MMM dd");
    private static final DateFormat MONTH_DAY_DF = create3CharMonthFormatter("MMM dd");
    private static final DateFormat MONTH_DAY2_DF = createFormatter("dd.MM");
    private static final DateFormat YEAR_MONTH_DF = create3CharMonthFormatter("yyyy MMM");
    private static final DateFormat MONTH_DF = create3CharMonthFormatter("MMM");
    private static final List<String> DAY_NAMES = Arrays.asList("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa");

    private Calendar date;
    private Calendar time;

    DateTimeRangeParser() {
        this(createCalendar());
    }

    DateTimeRangeParser(Calendar date, Calendar time)
    {
        this.date = date;
        this.time = time;
    }

    DateTimeRangeParser(Calendar date)
    {
        this.date = date;
    }

    static Calendar createCalendar() {
        // Use locale US as exception here (instead of UK) to match week order "Su-Sa" used in Calendar for day_of_week.
        // Inconsistent but we should not use US for other date handling stuff like strange default formatting, related to #647.
        return Calendar.getInstance(Helper.UTC, Locale.US);
    }

    static ParsedCalendar parseDateString(String dateString) throws ParseException {
        // Replace occurrences of public holidays
        dateString = dateString.replaceAll("(,( )*)?(PH|SH)", "");
        dateString = dateString.trim();
        Calendar calendar = createCalendar();
        ParsedCalendar parsedCalendar;
        try {
            calendar.setTime(YEAR_MONTH_DAY_DF.parse(dateString));
            parsedCalendar = new ParsedCalendar(ParsedCalendar.ParseType.YEAR_MONTH_DAY, calendar);
        } catch (ParseException e1) {
            try {
                Date a = MONTH_DAY_DF.parse(dateString);
                calendar.setTime(MONTH_DAY_DF.parse(dateString));
                parsedCalendar = new ParsedCalendar(ParsedCalendar.ParseType.MONTH_DAY, calendar);
            } catch (ParseException e2) {
                try {
                    calendar.setTime(MONTH_DAY2_DF.parse(dateString));
                    parsedCalendar = new ParsedCalendar(ParsedCalendar.ParseType.MONTH_DAY, calendar);
                } catch (ParseException e3) {
                    try {
                        calendar.setTime(YEAR_MONTH_DF.parse(dateString));
                        parsedCalendar = new ParsedCalendar(ParsedCalendar.ParseType.YEAR_MONTH, calendar);
                    } catch (ParseException e4) {
                        try {
                            calendar.setTime(MONTH_DF.parse(dateString));
                            parsedCalendar = new ParsedCalendar(ParsedCalendar.ParseType.MONTH, calendar);
                        } catch (ParseException e5) {
                            int index = DAY_NAMES.indexOf(dateString);
                            if (index < 0)
                                throw new ParseException("Unparsable date: \"" + dateString + "\"", 0);

                            // Ranges from 1-7
                            calendar.set(Calendar.DAY_OF_WEEK, index + 1);
                            parsedCalendar = new ParsedCalendar(ParsedCalendar.ParseType.DAY, calendar);
                        }

                    }
                }
            }
        }
        return parsedCalendar;
    }

    static ParsedTime parseTimeString(String timeString) throws ParseException {
        try {
            HOUR_MINUTE.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date date = HOUR_MINUTE.parse(timeString);
            Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            ParsedTime parsedtime;
            calendar.setTime(date);
            parsedtime = new ParsedTime(ParsedTime.ParseType.HOUR_MINUTE, calendar);
            return parsedtime;
        }catch(ParseException e)
        {
            // Xử lý ngoại lệ theo cách phù hợp với yêu cầu của GraphHopper
            e.printStackTrace(); // In stack trace để xem chi tiết lỗi
            return null; // hoặc throw một exception khác tùy vào yêu cầu
        }
    }

    DateTimeRange getRange(String dateRangeString) throws ParseException {
        if (dateRangeString == null || dateRangeString.isEmpty())
            throw new IllegalArgumentException("Passing empty Strings is not allowed");
        ParsedCalendar from_date;
        ParsedCalendar to_date;
        ParsedTime from_time;
        ParsedTime to_time;


        String[] dateTimeArr = dateRangeString.split("_");
        if (dateTimeArr.length > 2 || dateTimeArr.length < 1)
            return null;
        // throw new IllegalArgumentException("Only Strings containing two Date separated by a '-' or a single Date are allowed");

        String[] dateArr = dateTimeArr[0].split("-");
        String[] timeArr = dateTimeArr[1].split("-");
        from_date = parseDateString(dateArr[0]);
        if (dateArr.length == 2)
            to_date = parseDateString(dateArr[1]);
        else
            // faster and safe?
            // to = new ParsedDateTime(from.parseType, (Calendar) from.parsedCalendar.clone());
            to_date = parseDateString(dateArr[0]);
        from_time = parseTimeString(timeArr[0]);
        if(timeArr.length == 2)
            to_time = parseTimeString(timeArr[1]);
        else to_time = parseTimeString(timeArr[0]);

        try {
            return new DateTimeRange(from_date, to_date, from_time, to_time);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    @Override
    public ConditionState checkCondition(String dateRangeString) throws ParseException {
        DateTimeRange dr = getRange(dateRangeString);
        if (dr == null)
            return ConditionState.INVALID;

        if (dr.isInRange(date, time))
            return ConditionState.TRUE;
        else
            return ConditionState.FALSE;
    }

    public static DateTimeRangeParser createInstance(String day, String time) {
        Calendar calendar_date = createCalendar();
        Calendar calendar_time = createCalendar();
        try {
            if (!day.isEmpty()) {
                calendar_time.set(Calendar.YEAR, 1970);
                calendar_time.set(Calendar.MONTH, Calendar.JANUARY);
                calendar_time.set(Calendar.DAY_OF_MONTH, 1);
                calendar_date.setTime(Helper.createFormatter("yyyy-MM-dd").parse(day));
                calendar_time.setTime(Helper.createFormatter("HH:mm").parse(time));
            }
        } catch (ParseException e) {
            throw new IllegalArgumentException(e);
        }
        return new DateTimeRangeParser(calendar_date, calendar_time);
    }

    private static SimpleDateFormat create3CharMonthFormatter(String pattern) {
        DateFormatSymbols formatSymbols = new DateFormatSymbols(Locale.ENGLISH);
        formatSymbols.setShortMonths(new String[]{"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"});
        SimpleDateFormat df = new SimpleDateFormat(pattern, formatSymbols);
        df.setTimeZone(Helper.UTC);
        return df;
    }

    public String [] getTimeRangeCount(String timeRangeString)
    {
        String [] DateTimeArr = timeRangeString.split("_");
        String[] count = DateTimeArr[1].split(",");
//        System.out.print("So khoang thoi gian: ");
//        System.out.println(count.length);
        return count;
    }

    private static SimpleDateFormat createTimeFormatter(String format) {
        return new SimpleDateFormat(format);
    }
}
