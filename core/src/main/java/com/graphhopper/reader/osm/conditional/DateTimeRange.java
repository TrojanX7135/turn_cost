package com.graphhopper.reader.osm.conditional;

import com.graphhopper.util.Helper;

import java.text.DateFormat;
import java.util.Calendar;

public class DateTimeRange {
    private final Calendar from_date;
    private final Calendar to_date;

    private final Calendar from_time;
    private final Calendar to_time;
    boolean yearless = false;
    boolean dayOnly = false;
    boolean reverse = false;

    public DateTimeRange(ParsedCalendar from_date, ParsedCalendar to_date, ParsedTime from_time, ParsedTime to_time)
    {
        Calendar fromDateCal = from_date.parsedCalendar;
        Calendar toDateCal = to_date.parsedCalendar;

        // This should never happen
        if (fromDateCal.get(Calendar.ERA) != toDateCal.get(Calendar.ERA)) {
            throw new IllegalArgumentException("Different calendar eras are not allowed. From:" + from_date + " To:" + to_date);
        }

        if (from_date.isYearless() && to_date.isYearless()) {
            yearless = true;
        }

        if (from_date.isDayOnly() && to_date.isDayOnly()) {
            dayOnly = true;
        }

        if (fromDateCal.getTimeInMillis() > toDateCal.getTimeInMillis()) {
            if (!yearless && !dayOnly) {
                throw new IllegalArgumentException("'from' after 'to' not allowed, except for isYearless and isDayOnly DateRanges. From:" + from_date + " To:" + to_date);
            } else {
                reverse = true;
            }
        }

        this.from_date= from_date.getMin();
        this.to_date = to_date.getMax();
        this.from_time= from_time.getMin();
        this.to_time = to_time.getMax();
    }

    public boolean isInRange(Calendar date, Calendar time) {
        // Thời gian
        boolean a = time.getTime().compareTo(this.from_time.getTime()) >= 0 && time.getTime().compareTo(this.to_time.getTime()) <= 0;

        // có năm và k có thứ
        if (!yearless && !dayOnly)
            return (date.after(from_date) && date.before(to_date) || date.equals(from_date) || date.equals(to_date)) && a;

        // chỉ có thứ
        if (dayOnly) {
            int currentDayOfWeek = date.get(Calendar.DAY_OF_WEEK);
            if (reverse) {
                return (from_date.get(Calendar.DAY_OF_WEEK) <= currentDayOfWeek || currentDayOfWeek <= to_date.get(Calendar.DAY_OF_WEEK)) && a;
            } else {
                return (from_date.get(Calendar.DAY_OF_WEEK) <= currentDayOfWeek && currentDayOfWeek <= to_date.get(Calendar.DAY_OF_WEEK)) && a;
            }
        }

        // không có năm (chỉ có ngày và tháng)
        if (reverse)
            return isInRangeYearlessReverse(date, time);
        else
            return isInRangeYearless(date, time);

    }

    private boolean isInRangeYearless(Calendar date, Calendar time) {
        // Thời gian
        boolean a = time.getTime().compareTo(this.from_time.getTime()) >= 0 && time.getTime().compareTo(this.to_time.getTime()) <= 0;

        if (from_date.get(Calendar.MONTH) < date.get(Calendar.MONTH) && date.get(Calendar.MONTH) < to_date.get(Calendar.MONTH))
            return a;
        if (from_date.get(Calendar.MONTH) == date.get(Calendar.MONTH) && to_date.get(Calendar.MONTH) == date.get(Calendar.MONTH)) {
            if (from_date.get(Calendar.DAY_OF_MONTH) <= date.get(Calendar.DAY_OF_MONTH) && date.get(Calendar.DAY_OF_MONTH) <= to_date.get(Calendar.DAY_OF_MONTH))
                return a;
            else
                return false;
        }
        if (from_date.get(Calendar.MONTH) == date.get(Calendar.MONTH)) {
            if (from_date.get(Calendar.DAY_OF_MONTH) <= date.get(Calendar.DAY_OF_MONTH))
                return a;
            else
                return false;
        }
        if (to_date.get(Calendar.MONTH) == date.get(Calendar.MONTH)) {
            if (date.get(Calendar.DAY_OF_MONTH) <= to_date.get(Calendar.DAY_OF_MONTH))
                return a;
            else
                return false;
        }
        return false;
    }

    private boolean isInRangeYearlessReverse(Calendar date, Calendar time) {
        // Thời gian
        boolean a = time.getTime().compareTo(this.from_time.getTime()) >= 0 && time.getTime().compareTo(this.to_time.getTime()) <= 0;

        int currMonth = date.get(Calendar.MONTH);
        if (from_date.get(Calendar.MONTH) < currMonth || currMonth < to_date.get(Calendar.MONTH))
            return a;
        if (from_date.get(Calendar.MONTH) == currMonth && to_date.get(Calendar.MONTH) == currMonth) {
            if (from_date.get(Calendar.DAY_OF_MONTH) < date.get(Calendar.DAY_OF_MONTH)
                    || date.get(Calendar.DAY_OF_MONTH) < to_date.get(Calendar.DAY_OF_MONTH))
                return a;
            else
                return false;
        }
        if (from_date.get(Calendar.MONTH) == currMonth) {
            if (from_date.get(Calendar.DAY_OF_MONTH) <= date.get(Calendar.DAY_OF_MONTH))
                return a;
            else
                return false;
        }
        if (to_date.get(Calendar.MONTH) == currMonth) {
            if (date.get(Calendar.DAY_OF_MONTH) <= to_date.get(Calendar.DAY_OF_MONTH))
                return a;
            else
                return false;
        }
        return false;
    }

    @Override
    public String toString() {
        DateFormat f = Helper.createFormatter();
        return "yearless:" + yearless + ", dayOnly:" + dayOnly + ", reverse:" + reverse
                + ", from:" + f.format(from_date.getTime()) + ", to:" + f.format(to_date.getTime());
    }
}
