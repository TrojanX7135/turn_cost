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
import com.graphhopper.reader.osm.conditional.DateRangeParser;
import com.graphhopper.reader.osm.conditional.TimeRangeParser;
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
public class OSMConditionalRestrictionsParser implements TagParser {
    private static final Logger logger = LoggerFactory.getLogger(OSMConditionalRestrictionsParser.class);
    private final Collection<String> conditionals;
    private final Setter restrictionSetter;
    private TimeRangeParser parser_time;
    private DateRangeParser parser_date;
    private DateTimeRangeParser parser;
    private final boolean enabledLogs = false;

    @FunctionalInterface
    public interface Setter {
        void setBoolean(int edgeId, EdgeIntAccess edgeIntAccess, boolean b);
    }

    public OSMConditionalRestrictionsParser(Collection<String> conditionals, Setter restrictionSetter, String dateRangeParserDate) {
        this.conditionals = conditionals;
        this.restrictionSetter = restrictionSetter;
        LocalDateTime current = LocalDateTime.now();
        DateTimeFormatter formatter_time = DateTimeFormatter.ofPattern("HH:mm");
        DateTimeFormatter formatter_date = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        dateRangeParserDate = current.format(formatter_date);
        String dateRangeParserTime = current.format(formatter_time);
        this.parser_date = DateRangeParser.createInstance(dateRangeParserDate);
        this.parser_time = TimeRangeParser.createInstance(dateRangeParserTime);
        this.parser = DateTimeRangeParser.createInstance(dateRangeParserDate,dateRangeParserTime);
    }

    @Override
    public void handleWayTags(int edgeId, EdgeIntAccess edgeIntAccess, ReaderWay way, IntsRef relationFlags) {
        // TODO for now the node tag overhead is not worth the effort due to very few data points
        // List<Map<String, Object>> nodeTags = way.getTag("node_tags", null);

        Boolean b = getConditional(way.getTags());
        if (b != null)
        {
            //this.readerConditionalWays_list.add(way);
            restrictionSetter.setBoolean(edgeId, edgeIntAccess, b);
        }
    }

    Boolean getConditional(Map<String, Object> tags) {
        for (Map.Entry<String, Object> entry : tags.entrySet()) {
            if (!conditionals.contains(entry.getKey())) continue;

            String value = (String) entry.getValue();
            String [] strs_1 = value.split("@");
            if(strs_1.length > 2 ) return null;
            else
            {
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
        String processedconditionalValue = "";
        int skipValueANDIndex = conditionalValue.indexOf("AND");
        int skipValueORIndex = conditionalValue.indexOf("OR");
        if(skipValueANDIndex > 1 && skipValueORIndex == -1)
        {
            processedconditionalValue = conditionalValue.substring(0,skipValueANDIndex).trim();
        }
        else if(skipValueANDIndex == -1 && skipValueORIndex > 1){
            processedconditionalValue = conditionalValue.substring(0,skipValueORIndex).trim();
        }
        else if(skipValueANDIndex == -1 && skipValueORIndex == -1)
        {
            processedconditionalValue = conditionalValue;
        }

        try {
            LocalDateTime current = LocalDateTime.now();
            DateTimeFormatter formatter_date = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            DateTimeFormatter formatter_time = DateTimeFormatter.ofPattern("HH:mm");
            String dateRangeParserTime = current.format(formatter_time);
            String dateRangeParserDate = current.format(formatter_date);
            this.parser_date = DateRangeParser.createInstance(dateRangeParserDate);
            this.parser_time = TimeRangeParser.createInstance(dateRangeParserTime);
            this.parser = DateTimeRangeParser.createInstance(dateRangeParserDate,dateRangeParserTime);

            if(processedconditionalValue.split(" ").length > 2) return  false;

            String [] timeRangeCount = parser.getTimeRangeCount(processedconditionalValue);
            String [] dateRangeCount = parser.getDateRangeCount(processedconditionalValue);
            ConditionalValueParser.ConditionState resGlobal = ConditionalValueParser.ConditionState.INVALID;
            if(timeRangeCount.length > 0 && dateRangeCount.length > 0)
            {
                ConditionalValueParser.ConditionState res = parser.checkCondition(dateRangeCount[0] + " " + timeRangeCount[0]);
                for (String s : dateRangeCount) {
                    for (String string : timeRangeCount) {
                        ConditionalValueParser.ConditionState resTemp = parser.checkCondition(s + " " + string);
                        if (res == ConditionalValueParser.ConditionState.TRUE && resTemp == ConditionalValueParser.ConditionState.TRUE)
                            res = ConditionalValueParser.ConditionState.TRUE;
                        else if (res == ConditionalValueParser.ConditionState.TRUE && resTemp == ConditionalValueParser.ConditionState.FALSE)
                            res = ConditionalValueParser.ConditionState.TRUE;
                        else if (res == ConditionalValueParser.ConditionState.FALSE && resTemp == ConditionalValueParser.ConditionState.FALSE)
                            res = ConditionalValueParser.ConditionState.FALSE;
                        else res = ConditionalValueParser.ConditionState.TRUE;
                        resGlobal = res;
                    }
                }
            }
            else if(timeRangeCount.length < 1 && dateRangeCount.length > 0)
            {
                ConditionalValueParser.ConditionState res = parser_date.checkCondition(dateRangeCount[0]);
                for (String string : dateRangeCount) {
                    ConditionalValueParser.ConditionState resTemp = parser_date.checkCondition(string);
                    if (res == ConditionalValueParser.ConditionState.TRUE && resTemp == ConditionalValueParser.ConditionState.TRUE)
                        res = ConditionalValueParser.ConditionState.TRUE;
                    else if (res == ConditionalValueParser.ConditionState.TRUE && resTemp == ConditionalValueParser.ConditionState.FALSE)
                        res = ConditionalValueParser.ConditionState.TRUE;
                    else if (res == ConditionalValueParser.ConditionState.FALSE && resTemp == ConditionalValueParser.ConditionState.FALSE)
                        res = ConditionalValueParser.ConditionState.FALSE;
                    else res = ConditionalValueParser.ConditionState.TRUE;
                    resGlobal = res;
                }
            }
            else if(timeRangeCount.length > 0) {
                ConditionalValueParser.ConditionState res = parser_time.checkCondition(timeRangeCount[0]);
                for (String string : timeRangeCount) {
                    ConditionalValueParser.ConditionState resTemp = parser_time.checkCondition(string);
                    if (res == ConditionalValueParser.ConditionState.TRUE && resTemp == ConditionalValueParser.ConditionState.TRUE)
                        res = ConditionalValueParser.ConditionState.TRUE;
                    else if (res == ConditionalValueParser.ConditionState.TRUE && resTemp == ConditionalValueParser.ConditionState.FALSE)
                        res = ConditionalValueParser.ConditionState.TRUE;
                    else if (res == ConditionalValueParser.ConditionState.FALSE && resTemp == ConditionalValueParser.ConditionState.FALSE)
                        res = ConditionalValueParser.ConditionState.FALSE;
                    else res = ConditionalValueParser.ConditionState.TRUE;
                    resGlobal = res;
                }
            }
            if (resGlobal.isValid())
                return resGlobal.isCheckPassed();
            if (enabledLogs)
                logger.warn("Invalid date to parse " + conditionalValue);
        } catch (ParseException ex) {
            if (enabledLogs)
                logger.warn("Cannot parse " + conditionalValue);
        }
        return false;
    }
}
