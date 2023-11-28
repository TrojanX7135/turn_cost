package com.graphhopper;

import java.time.LocalDateTime;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

// Biến toàn cục cho time_request
public class GlobalVariables {
    private static LocalDateTime time_request;
    private static PropertyChangeSupport support = new PropertyChangeSupport(GlobalVariables.class);

    public static LocalDateTime getTimeRequest() {
        return time_request;
    }

    public static void setTimeRequest(LocalDateTime time_request) {
        LocalDateTime oldTime = GlobalVariables.time_request;
        GlobalVariables.time_request = time_request;
        support.firePropertyChange("time_request", oldTime, time_request);
    }
    
    public static void addPropertyChangeListener(PropertyChangeListener listener) {
        support.addPropertyChangeListener(listener);
    }
}