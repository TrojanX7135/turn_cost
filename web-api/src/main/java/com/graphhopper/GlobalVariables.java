package com.graphhopper;

import java.time.LocalDateTime;
//import org.joda.time.LocalDateTime;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

// Biến toàn cục cho time_request
public class GlobalVariables {
    private static LocalDateTime time_request;
    private static Boolean turn_cost_status = false;
    private static PropertyChangeSupport support = new PropertyChangeSupport(GlobalVariables.class);

    //Time_request
    public static LocalDateTime getTimeRequest() {
        return time_request;
    }

    public static void setTimeRequest(LocalDateTime time_request) {
        LocalDateTime oldTime = GlobalVariables.time_request;
        GlobalVariables.time_request = time_request;
        support.firePropertyChange("time_request", oldTime, time_request);
    }
    
    //Turn_cost status
    public static Boolean getTurnCostStatus() {
    	return turn_cost_status;
    }
    
    public static void setTurnCostStatus(Boolean turn_cost_status) {
    	GlobalVariables.turn_cost_status = turn_cost_status;
    }
    
    
    // Hệ thống Observer cho time_request
    public static void addPropertyChangeListener(PropertyChangeListener listener) {
        support.addPropertyChangeListener(listener);
    }
}