package com.graphhopper;

import com.graphhopper.routing.ev.RoadAccess;
import com.graphhopper.util.EdgeIteratorState;

public interface RoadAccessService {
    RoadAccess getRoadAccess(EdgeIteratorState edgeState);
}