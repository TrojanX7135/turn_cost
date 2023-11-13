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
import com.graphhopper.reader.osm.conditional.TimeRangeParser;
import com.graphhopper.routing.ev.EdgeIntAccess;
import com.graphhopper.storage.IntsRef;
import com.graphhopper.util.Helper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * This parser fills the different XYAccessConditional enums from the OSM conditional restrictions.
 * Node tags will be ignored.
 */
public class OSMConditionalRestrictionsParser implements TagParser {

    private static final Logger logger = LoggerFactory.getLogger(OSMConditionalRestrictionsParser.class);
    private final Collection<String> conditionals;
    private final Setter restrictionSetter;
    private TimeRangeParser parser;
    private final boolean enabledLogs = false;

    @FunctionalInterface
    public interface Setter {
        void setBoolean(int edgeId, EdgeIntAccess edgeIntAccess, boolean b);
    }

    public OSMConditionalRestrictionsParser(Collection<String> conditionals, Setter restrictionSetter, String dateRangeParserDate) {
        this.conditionals = conditionals;
        this.restrictionSetter = restrictionSetter;
        if (dateRangeParserDate.isEmpty()) {
            LocalDateTime current = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
            dateRangeParserDate = current.format(formatter);

            //dateRangeParserDate  = Helper.createFormatter("HH:mm").format(new Date().getTime());
            //dateRangeParserDate = Helper.createFormatter().format(new Date().getTime());
           // dateRangeParserDate = Helper.createFormatter("yyyy-MM-dd").format(new Date());
        }

        this.parser = TimeRangeParser.createInstance(dateRangeParserDate);
        System.out.println(dateRangeParserDate);
        System.out.println(parser.toString());
    }

    @Override
    public void handleWayTags(int edgeId, EdgeIntAccess edgeIntAccess, ReaderWay way, IntsRef relationFlags) {
        // TODO for now the node tag overhead is not worth the effort due to very few data points
        // List<Map<String, Object>> nodeTags = way.getTag("node_tags", null);

        Boolean b = getConditional(way.getTags());
        if (b != null)
            restrictionSetter.setBoolean(edgeId, edgeIntAccess, b);
    }

    Boolean getConditional(Map<String, Object> tags) {
        for (Map.Entry<String, Object> entry : tags.entrySet()) {
            if (!conditionals.contains(entry.getKey())) continue;

            String value = (String) entry.getValue();
            String[] strs = value.split("@");
            if (strs.length == 2 && isInRange(strs[1].trim())) {
                if (strs[0].trim().equals("no")) return false;
                if (strs[0].trim().equals("yes")) return true;
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
            ConditionalValueParser.ConditionState res = parser.checkCondition(conditionalValue);
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
    @Override
    public void setparset(String dateRangeParserDate)
    {
        this.parser = TimeRangeParser.createInstance(dateRangeParserDate);
    }
}
