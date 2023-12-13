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
package com.graphhopper.routing.weighting.custom;

import com.graphhopper.routing.ev.DecimalEncodedValue;
import com.graphhopper.routing.ev.EncodedValueLookup;
import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.FetchMode;
import com.graphhopper.util.GHUtility;
import com.graphhopper.util.JsonFeature;
import com.graphhopper.util.TurnCostsConfig;
import com.graphhopper.util.shapes.BBox;
import com.graphhopper.util.shapes.Polygon;

import java.util.Map;

/**
 * This class is for internal usage only. It is subclassed by Janino, then special expressions are injected into init,
 * getSpeed and getPriority. At the end an instance is created and used in CustomWeighting.
 */
public class CustomWeightingHelper {
    protected DecimalEncodedValue avg_speed_enc;
    protected DecimalEncodedValue priority_enc;
    
    protected DecimalEncodedValue leftAffectVar_enc;
    protected DecimalEncodedValue rightAffectVar_enc;
    protected DecimalEncodedValue straightAffectVar_enc;
    
    
//    protected DecimalEncodedValue lanes;
    protected EncodedValueLookup lookup;
    TurnCostsConfig config = new TurnCostsConfig();

    protected CustomWeightingHelper() {
    }

    public void init(EncodedValueLookup lookup, DecimalEncodedValue avgSpeedEnc, DecimalEncodedValue priorityEnc, Map<String, JsonFeature> areas) {
        this.avg_speed_enc = avgSpeedEnc;
        this.priority_enc = priorityEnc;
        this.lookup = lookup;
    }

    public double getPriority(EdgeIteratorState edge, boolean reverse) {
        return 1;
    }

    public double getSpeed(EdgeIteratorState edge, boolean reverse) {
        return getRawSpeed(edge, reverse);
    }
    
    public double getLeftAffect(EdgeIteratorState edge, boolean reverse) {
        return getRawRightAffect(edge, reverse);
    }
    
    public double getRightAffect(EdgeIteratorState edge, boolean reverse) {
        return getRawRightAffect(edge, reverse);
    }

    public double getStraightAffect(EdgeIteratorState edge, boolean reverse) {
        return getRawStraightAffect(edge, reverse);
    }
    
    protected final double getRawLeftAffect(EdgeIteratorState edge, boolean reverse) {
    	if (leftAffectVar_enc == null) return 1;
        double leftAffect = reverse ? edge.getReverse(leftAffectVar_enc) : edge.get(leftAffectVar_enc);
        if (Double.isInfinite(leftAffect) || Double.isNaN(leftAffect) || leftAffect < 0)
            throw new IllegalStateException("Invalid estimated leftAffect " + leftAffect);
        return leftAffect;
    }
    
    protected final double getRawRightAffect(EdgeIteratorState edge, boolean reverse) {
    	if (rightAffectVar_enc == null) return 1;
        double rightAffect = reverse ? edge.getReverse(rightAffectVar_enc) : edge.get(rightAffectVar_enc);
        if (Double.isInfinite(rightAffect) || Double.isNaN(rightAffect) || rightAffect < 0)
            throw new IllegalStateException("Invalid estimated rightAffect " + rightAffect);
        return rightAffect;
    }
    
    protected final double getRawStraightAffect(EdgeIteratorState edge, boolean reverse) {
    	if (straightAffectVar_enc == null) return 1;
        double straightAffect = reverse ? edge.getReverse(straightAffectVar_enc) : edge.get(straightAffectVar_enc);
        if (Double.isInfinite(straightAffect) || Double.isNaN(straightAffect) || straightAffect < 0)
            throw new IllegalStateException("Invalid estimated straightAffect " + straightAffect);
        return straightAffect;
    }
    
    protected final double getRawSpeed(EdgeIteratorState edge, boolean reverse) {
        double speed = reverse ? edge.getReverse(avg_speed_enc) : edge.get(avg_speed_enc);
        if (Double.isInfinite(speed) || Double.isNaN(speed) || speed < 0)
            throw new IllegalStateException("Invalid estimated speed " + speed);
        return speed;
    }

    protected final double getRawPriority(EdgeIteratorState edge, boolean reverse) {
        if (priority_enc == null) return 1;
        double priority = reverse ? edge.getReverse(priority_enc) : edge.get(priority_enc);
        if (Double.isInfinite(priority) || Double.isNaN(priority) || priority < 0)
            throw new IllegalStateException("Invalid priority " + priority);
        return priority;
    }

    protected double getMaxPriority() {
        return 1;
    }

    protected double getMaxSpeed() {
        return 1;
    }

    public static boolean in(Polygon p, EdgeIteratorState edge) {
        BBox edgeBBox = GHUtility.createBBox(edge);
        BBox polyBBOX = p.getBounds();
        if (!polyBBOX.intersects(edgeBBox))
            return false;
        if (p.isRectangle() && polyBBOX.contains(edgeBBox))
            return true;
        return p.intersects(edge.fetchWayGeometry(FetchMode.ALL).makeImmutable()); // TODO PERF: cache bbox and edge wayGeometry for multiple area
    }
}
