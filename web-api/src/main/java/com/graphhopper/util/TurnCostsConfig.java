package com.graphhopper.util;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.graphhopper.GlobalVariables;

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
	
	private double calcLeft;
	private double calcRight;
	private double calcStraight;
	
    private double leftCost = 3; // in seconds
    private double rightCost = 0.5;
    private double straightCost = 0;
    private double minLeftAngle = 25, maxLeftAngle = 180;
    private double minRightAngle = -25, maxRightAngle = -180;
    

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
    
    //Evn
    public void setTimeRequest (String timeRequest) {
//    	System.out.println("timeRequest: " + timeRequest);
    	this.timeRequest = timeRequest;
    	evaluateExpression(timeRequest);
    	this.leftCost = this.left + this.calcLeft;
    	this.rightCost = this.right + this.calcRight;
    	this.straightCost = this.straight + this.calcStraight; 	
    }

    // Left
    public TurnCostsConfig setLeftCost(double leftCost) {
        this.leftCost = leftCost;
        return this;
    }

    public double getLeftCost() {
    	setTimeRequest(this.timeRequest);
//    	System.out.println("left: " + leftCost);
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
//        	System.out.println("evaluateExpression khởi chạy");
            // Tách biểu thức thành các phần
            int hour;
            if (GlobalVariables.getTimeRequest() != null) {
                hour = GlobalVariables.getTimeRequest().getHour();
            } else {
                hour = 12;
            }
            System.out.println("HOUR" + hour);
            if (parts[1].equals("<=") && parts[3].equals("<=")) {
                if (Integer.parseInt(parts[0]) <= hour && hour <= Integer.parseInt(parts[4])) {
                	this.calcStraight = Double.parseDouble(parts[5]);
                	this.calcLeft = Double.parseDouble(parts[6]);
                	this.calcRight = Double.parseDouble(parts[7]);
                } else {
                	this.calcStraight = 0;
                	this.calcLeft = 0;
                	this.calcRight = 0;
                }    
            } else {
            	this.calcStraight = 0;
            	this.calcLeft = 0;
            	this.calcRight = 0;
            }
        } else {
        	this.calcStraight = 0;
        	this.calcLeft = 0;
        	this.calcRight = 0;
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
        return Objects.hash(leftCost, rightCost, straightCost, minLeftAngle, maxLeftAngle, minRightAngle, maxRightAngle);
    }

    @Override
    public String toString() {
        return "leftCost=" + leftCost + ", rightCost=" + rightCost + ", straightCost=" + straightCost
                + ", minLeftAngle=" + minLeftAngle + ", maxLeftAngle=" + maxLeftAngle
                + ", minRightAngle=" + minRightAngle + ", maxRightAngle=" + maxRightAngle;
    }
}
