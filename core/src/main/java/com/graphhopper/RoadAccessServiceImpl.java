package com.graphhopper;

import com.graphhopper.routing.ev.EncodedValueLookup;
import com.graphhopper.routing.ev.EnumEncodedValue;
import com.graphhopper.routing.ev.RoadAccess;
import com.graphhopper.util.EdgeIteratorState;

public class RoadAccessServiceImpl implements RoadAccessService {
    private final EncodedValueLookup lookup;

    // Constructor-based Dependency Injection
    public RoadAccessServiceImpl(EncodedValueLookup lookup) {
        this.lookup = lookup;
    }

    @Override
    public RoadAccess getRoadAccess(EdgeIteratorState edgeState) {
        EnumEncodedValue<RoadAccess> roadAccessEnc = lookup.getEnumEncodedValue("road_access", RoadAccess.class);
        return edgeState.get(roadAccessEnc);
    }
}
