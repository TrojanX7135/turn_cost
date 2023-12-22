package com.graphhopper.util;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.graphhopper.GlobalVariables;
import com.graphhopper.json.Statement;
//import com.graphhopper.matching.Affect;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TurnCostsConfig {

	@JsonProperty("left")
	private double left;
	
	@JsonProperty("right")
	private double right;
	
	@JsonProperty("straight")
	private double straight;
	
	@JsonProperty("time_request")
	private String timeRequest;
	
	@JsonProperty("left_affect")
	private List<Statement> leftAffect = new ArrayList<>();
	
	@JsonProperty("right_affect")
	private List<Statement> rightAffect = new ArrayList<>();
	
	@JsonProperty("straight_affect")
	private List<Statement> straightAffect = new ArrayList<>();
	
	private double calcLeft;
	private double calcRight;
	private double calcStraight;
	
    private double leftCost = 3; // in seconds
    private double rightCost = 0.5;
    private double straightCost = 0;
    private double minLeftAngle = 45, maxLeftAngle = 175;
    private double minRightAngle = -45, maxRightAngle = -175;
    

    public TurnCostsConfig() {
    	this.left = 0;
    	this.right = 0;
    	this.straight = 0;
    	this.timeRequest = "0";
    }
    
    public TurnCostsConfig(TurnCostsConfig copy) {
		leftCost = copy.leftCost;
        rightCost = copy.rightCost;
        straightCost = copy.straightCost;
        minLeftAngle = copy.minLeftAngle;
        maxLeftAngle = copy.maxLeftAngle;
        minRightAngle = copy.minRightAngle;
        maxRightAngle = copy.maxRightAngle;
    }   
    
    public List<Statement> TCgetLeftAffect() {
        return leftAffect;
    }

    public List<Statement> TCgetRightAffect() {
        return rightAffect;
    }
    
    public List<Statement> TCgetStraightAffect() {
        return straightAffect;
    }

    
    
    //timeRequest
    public void setTimeRequest (String timeRequest) {
    	this.timeRequest = timeRequest;
    	evaluateExpression(timeRequest);
    	   	
    	this.leftCost = this.left * this.calcLeft;
    	this.rightCost = this.right * this.calcRight;
    	this.straightCost = this.straight * this.calcStraight; 	
    }

    // Left
    public TurnCostsConfig setLeftCost(double leftCost) {
        this.leftCost = leftCost;
        return this;
    }

    public double getLeftCost() {
    	setTimeRequest(this.timeRequest);
        return leftCost;
    }

    
    // Right
    public TurnCostsConfig setRightCost(double rightCost) {
        this.rightCost = rightCost;
        return this;
    }
    
    public double getRightCost() {
    	setTimeRequest(this.timeRequest);
        return rightCost;
    }

    
    // Straight
    public TurnCostsConfig setStraightCost(double straightCost) {
        this.straightCost = straightCost;
        return this;
    }

    public double getStraightCost() {
    	setTimeRequest(this.timeRequest);
        return straightCost;
    }

    
    public void setMinLeftAngle(double minLeftAngle) {
        this.minLeftAngle = minLeftAngle;
    }

    public double getMinLeftAngle() {
        return minLeftAngle;
    }

    public void setMaxLeftAngle(double maxLeftAngle) {
        this.maxLeftAngle = maxLeftAngle;
    }

    public double getMaxLeftAngle() {
        return maxLeftAngle;
    }

    public void setMinRightAngle(double minRightAngle) {
        this.minRightAngle = minRightAngle;
    }

    public double getMinRightAngle() {
        return minRightAngle;
    }

    public void setMaxRightAngle(double maxRightAngle) {
        this.maxRightAngle = maxRightAngle;
    }

    public double getMaxRightAngle() {
        return maxRightAngle;
    }
    
    
    private void evaluateExpression(String expression) {
    	String[] parts = expression.split(" ");
        if (expression.contains("time_request")) {
            int hour;
            if (GlobalVariables.getTimeRequest() != null) {
                hour = GlobalVariables.getTimeRequest().getHour();
            } else {
                hour = 12;
            }
//            System.out.println("HOUR: " + hour);
            if (parts[1].equals("<=") && parts[3].equals("<=")) {
                if (Integer.parseInt(parts[0]) <= hour && hour <= Integer.parseInt(parts[4])) {
                	this.calcStraight = Double.parseDouble(parts[5]);
                	this.calcLeft = Double.parseDouble(parts[6]);
                	this.calcRight = Double.parseDouble(parts[7]);
                } else {
                	this.calcStraight = 1;
                	this.calcLeft = 1;
                	this.calcRight = 1;
                }    
            } else {
            	this.calcStraight = 1;
            	this.calcLeft = 1;
            	this.calcRight = 1;
            }
        } else {
        	this.calcStraight = 1;
        	this.calcLeft = 1;
        	this.calcRight = 1;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TurnCostsConfig that = (TurnCostsConfig) o;
        return Double.compare(that.leftCost, leftCost) == 0 && Double.compare(that.rightCost, rightCost) == 0
                && Double.compare(that.straightCost, straightCost) == 0 && Double.compare(that.minLeftAngle, minLeftAngle) == 0
                && Double.compare(that.maxLeftAngle, maxLeftAngle) == 0 && Double.compare(that.minRightAngle, minRightAngle) == 0
                && Double.compare(that.maxRightAngle, maxRightAngle) == 0;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(left, right, straight, timeRequest, leftAffect, rightAffect, straightAffect, minLeftAngle, maxLeftAngle, minRightAngle, maxRightAngle);
    }

    @Override
    public String toString() {
        return "left=" + left + ", right=" + right + ", straight=" + straight 
        		+ ", timeRequest=" + timeRequest 
        		+ ", leftAffect="+ leftAffect + ", rightAffect=" + rightAffect + ", straightAffect=" + straightAffect
                + ", minLeftAngle=" + minLeftAngle + ", maxLeftAngle=" + maxLeftAngle
                + ", minRightAngle=" + minRightAngle + ", maxRightAngle=" + maxRightAngle;
    }
    
//	@JsonProperty("left")
//	private double left;
//	
//	@JsonProperty("right")
//	private double right;
//	
//	@JsonProperty("straight")
//	private double straight;
//	
//	@JsonProperty("time_request")
//	private String left;
//	
//	@JsonProperty("left_affect")
//	private List<Statement> leftAffect = new ArrayList<>();
//	
//	@JsonProperty("right_affect")
//	private List<Statement> rightAffect = new ArrayList<>();
//	
//	@JsonProperty("straight_affect")
//	private List<Statement> straightAffect = new ArrayList<>();
    
}
