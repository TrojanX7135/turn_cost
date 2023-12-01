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

package com.graphhopper.routing.util.parsers;

import com.graphhopper.reader.ReaderWay;
import com.graphhopper.reader.osm.conditional.ConditionalValueParser;
import com.graphhopper.reader.osm.conditional.DateTimeRangeParser;
import com.graphhopper.routing.ev.EdgeIntAccess;
import com.graphhopper.storage.IntsRef;
import com.graphhopper.util.Helper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.util.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import com.graphhopper.reader.ReaderWay;

/**
 * This parser fills the different XYAccessConditional enums from the OSM conditional restrictions.
 * Node tags will be ignored.
 */
public class OSMConditionalRestrictionsTrafficParser implements TagParser{
    private static final Logger logger = LoggerFactory.getLogger(OSMConditionalRestrictionsTrafficParser.class);
    private final Collection<String> conditionals;
    private final SetterTraffic restrictionSetter;
    private DateTimeRangeParser parser;
    private final boolean enabledLogs = false;

    @FunctionalInterface
    public interface SetterTraffic {
        void setBoolean(int edgeId, EdgeIntAccess edgeIntAccess, boolean b);
    }

    public OSMConditionalRestrictionsTrafficParser(Collection<String> conditionals, SetterTraffic restrictionSetter, String dateRangeParserDate) {
        this.conditionals = conditionals;
        this.restrictionSetter = restrictionSetter;
        LocalDateTime current = LocalDateTime.now();
        DateTimeFormatter formatter_date = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter formatter_time = DateTimeFormatter.ofPattern("HH:mm");
        dateRangeParserDate = current.format(formatter_date);
        String dateRangeParserTime = current.format(formatter_time);
        this.parser = DateTimeRangeParser.createInstance(dateRangeParserDate,dateRangeParserTime);
        System.out.println(dateRangeParserTime);
        System.out.println(parser.toString());
    }

    @Override
    public void handleWayTags(int edgeId, EdgeIntAccess edgeIntAccess, ReaderWay way, IntsRef relationFlags) {
        // TODO for now the node tag overhead is not worth the effort due to very few data points
        // List<Map<String, Object>> nodeTags = way.getTag("node_tags", null);

        Boolean b = getTrafficConditional(way.getTags());
        if (b != null)
        {
            //this.readerConditionalWays_list.add(way);
            restrictionSetter.setBoolean(edgeId, edgeIntAccess, b);
        }
    }

    Boolean getTrafficConditional(Map<String, Object> tags) {
        for (Map.Entry<String, Object> entry : tags.entrySet()) {
            if (!conditionals.contains(entry.getKey())) continue;

            String value = (String) entry.getValue();
            String[] strs = value.split("@");
            if (strs.length == 2 && isInRange(strs[1].trim())) {
                if (strs[0].trim().equals("no")) return false;
                if (strs[0].trim().equals("yes")) return true;
            }
            // Thêm vào
            else if(strs.length == 2 && !isInRange(strs[1].trim()))
            {
                if (strs[0].trim().equals("no")) return true;
                if (strs[0].trim().equals("yes")) return false;
            }
        }
        return null;
    }

    private boolean isInRange(final String value) {
        if (value.isEmpty())
            return false;

        if (value.contains(";")) {
            if (enabledLogs)
                logger.warn("We do not support multiple conditions yet: " + value);
            return false;
        }

        String conditionalValue = value.replace('(', ' ').replace(')', ' ').trim();
        try {
            LocalDateTime current = LocalDateTime.now();
            DateTimeFormatter formatter_date = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            DateTimeFormatter formatter_time = DateTimeFormatter.ofPattern("HH:mm");
            String dateRangeParserDate = current.format(formatter_date);
            String dateRangeParserTime = current.format(formatter_time);
            this.parser = DateTimeRangeParser.createInstance(dateRangeParserDate, dateRangeParserTime);
            String [] DateTimeArr = conditionalValue.split(" ");

            String [] a = parser.getTimeRangeCount(conditionalValue);
            ConditionalValueParser.ConditionState res = parser.checkCondition(DateTimeArr[0] + "_" + a[0]);
            for (int i = 1; i < a.length;i++) {
                ConditionalValueParser.ConditionState resTemp = parser.checkCondition(DateTimeArr[0] + "_" +a[i]);
                if(res == ConditionalValueParser.ConditionState.TRUE && resTemp == ConditionalValueParser.ConditionState.TRUE)
                    res = ConditionalValueParser.ConditionState.TRUE;
                else if(res == ConditionalValueParser.ConditionState.TRUE && resTemp == ConditionalValueParser.ConditionState.FALSE)
                    res = ConditionalValueParser.ConditionState.TRUE;
                else if(res == ConditionalValueParser.ConditionState.FALSE && resTemp == ConditionalValueParser.ConditionState.FALSE)
                    res = ConditionalValueParser.ConditionState.FALSE;
                else res = ConditionalValueParser.ConditionState.TRUE;
                //res = parser.checkCondition(a[i]);
            }
            //ConditionalValueParser.ConditionState res = parser.checkCondition(conditionalValue);
            if (res.isValid())
                return res.isCheckPassed();
            if (enabledLogs)
                logger.warn("Invalid date to parse " + conditionalValue);
        } catch (ParseException ex) {
            if (enabledLogs)
                logger.warn("Cannot parse " + conditionalValue);
        }
        return false;
    }
}
