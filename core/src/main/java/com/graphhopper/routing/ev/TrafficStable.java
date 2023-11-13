package com.graphhopper.routing.ev;
import com.graphhopper.util.Helper;
public enum TrafficStable {
    MISSING, YES, NO;

    public static final String KEY = "traffic_stable";

    public static EnumEncodedValue<TrafficStable> create() {
        return new EnumEncodedValue<>(TrafficStable.KEY, TrafficStable.class);
    }

    @Override
    public String toString() {
        return Helper.toLowerCase(super.toString());
    }

    public static TrafficStable find(String name) {
        if (name == null || name.isEmpty())
            return MISSING;
        try {
            return TrafficStable.valueOf(Helper.toUpperCase(name));
        } catch (IllegalArgumentException ex) {
            return MISSING;
        }
    }
}
