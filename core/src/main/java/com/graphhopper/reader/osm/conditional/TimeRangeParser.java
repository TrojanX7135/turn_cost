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
public class TimeRangeParser implements ConditionalValueParser {

    private Calendar date;

    TimeRangeParser() {
        this(createCalendar());
    }

    TimeRangeParser(Calendar date) {
        this.date = date;
    }

    static Calendar createCalendar() {
        // Use locale US as exception here (instead of UK) to match week order "Su-Sa" used in Calendar for day_of_week.
        // Inconsistent but we should not use US for other date handling stuff like strange default formatting, related to #647.
        return Calendar.getInstance(Helper.UTC, Locale.US);
    }

    static ParsedTime parseTimeString(String timeString) throws ParseException {
        Calendar calendar = createCalendar();
        calendar.set(Calendar.YEAR, 1970);
        calendar.set(Calendar.MONTH, Calendar.JANUARY);
        calendar.setTime(Helper.createFormatter("HH:mm").parse(timeString));
        return new ParsedTime(ParsedTime.ParseType.HOUR_MINUTE, calendar);
    }

    TimeRange getRange(String timeRangeString) throws ParseException {
        if (timeRangeString == null || timeRangeString.isEmpty())
            throw new IllegalArgumentException("Passing empty Strings is not allowed");

        String[] timeArr = timeRangeString.split("-");
        if (timeArr.length != 2)
            return null;
        // throw new IllegalArgumentException("Only Strings containing two Date separated by a '-' or a single Date are allowed");

        ParsedTime from = parseTimeString(timeArr[0]);
        ParsedTime to;
        to = parseTimeString(timeArr[1]);

        try {
            return new TimeRange(from, to);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    @Override
    public ConditionState checkCondition(String timeRangeString) throws ParseException {
        TimeRange dr = getRange(timeRangeString);
        if (dr == null)
            return ConditionState.INVALID;

        if (dr.isInRange(date))
            return ConditionState.TRUE;
        else
            return ConditionState.FALSE;
    }

    public static TimeRangeParser createInstance(String time) {
        Calendar calendar = createCalendar();
        try {
            if (!time.isEmpty()) {
                calendar.set(Calendar.YEAR, 1970);
                calendar.set(Calendar.MONTH, Calendar.JANUARY);
                calendar.setTime(Helper.createFormatter("HH:mm").parse(time));
            }
        } catch (ParseException e) {
            throw new IllegalArgumentException(e);
        }
        return new TimeRangeParser(calendar);
    }
}
