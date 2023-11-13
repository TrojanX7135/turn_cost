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
import java.util.Calendar;

/**
 * This class represents a time range and is able to determine if a given time is in that range.
 *
 * @author ttung6618
 */
public class TimeRange {
    private final Calendar from;
    private final Calendar to;

    public TimeRange(ParsedTime from, ParsedTime to) {
        this.from = from.getMin();
        this.to = to.getMax();
    }

    public boolean isInRange(Calendar date) {
        return date.getTime().compareTo(this.from.getTime()) >= 0 && date.getTime().compareTo(this.to.getTime()) <= 0;
    }
}
