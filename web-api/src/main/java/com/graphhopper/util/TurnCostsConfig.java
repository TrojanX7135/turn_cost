package com.graphhopper.util;

import com.fasterxml.jackson.annotation.JsonProperty;
import javax.script.ScriptException;

import java.util.*;


public class TurnCostsConfig {
	@JsonProperty("turn_costs_config")
    private List<TurnCostRule> turnCosts;
	private double minLeftAngle = 25, maxLeftAngle = 180;
	private double minRightAngle = -25, maxRightAngle = -180;
    
    public TurnCostsConfig() {
    	this.turnCosts = new ArrayList<>();
    }
    
    public TurnCostsConfig(TurnCostsConfig copy) {

		minLeftAngle = copy.minLeftAngle;
		maxLeftAngle = copy.maxLeftAngle;
		minRightAngle = copy.minRightAngle;
		maxRightAngle = copy.maxRightAngle;    	
    	
        if (copy.turnCosts != null) {
            this.turnCosts = new ArrayList<>();
            for (TurnCostRule rule : copy.turnCosts) {
                TurnCostRule ruleCopy = new TurnCostRule();
                ruleCopy.setCondition(rule.getCondition());
                try {
                    ruleCopy.setStraight(Double.toString(rule.getStraight()));
                    ruleCopy.setLeft(Double.toString(rule.getLeft()));
                    ruleCopy.setRight(Double.toString(rule.getRight()));
                } catch (ScriptException e) {
                    e.printStackTrace();
                }
                this.turnCosts.add(ruleCopy);
            }
        }
    }


    // Getter for turnCosts
    public List<TurnCostRule> getTurnCosts() {
        return turnCosts;
    }

    // Setter for turnCosts
    public void setTurnCosts(List<TurnCostRule> turnCosts) {
        if (turnCosts == null) {
            this.turnCosts = new ArrayList<>();
        } else {
            this.turnCosts = turnCosts;
        }
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
    
	@Override
	public boolean equals(Object o) {
	    if (this == o) return true;
	    if (o == null || getClass() != o.getClass()) return false;
	    TurnCostsConfig that = (TurnCostsConfig) o;
	    return Objects.equals(turnCosts, that.turnCosts) &&
	           Double.compare(that.minLeftAngle, minLeftAngle) == 0 &&
	           Double.compare(that.maxLeftAngle, maxLeftAngle) == 0 &&
	           Double.compare(that.minRightAngle, minRightAngle) == 0 &&
	           Double.compare(that.maxRightAngle, maxRightAngle) == 0;
	}

	@Override
	public int hashCode() {
	    return Objects.hash(turnCosts, minLeftAngle, maxLeftAngle, minRightAngle, maxRightAngle);
	}

	@Override
	public String toString() {
	    return "TurnCostsConfig{" +
	            "turnCosts=" + turnCosts +
	            ", minLeftAngle=" + minLeftAngle +
	            ", maxLeftAngle=" + maxLeftAngle +
	            ", minRightAngle=" + minRightAngle +
	            ", maxRightAngle=" + maxRightAngle +
	            '}';
	}
}


























//package com.graphhopper.util;
//
//import com.fasterxml.jackson.annotation.JsonProperty;
//
//import java.util.Objects;
//
//public class TurnCostsConfig {
//    private double leftCost = 3; // in seconds
//    private double rightCost = 0.5;
//    private double straightCost = 0;
//    private double minLeftAngle = 25, maxLeftAngle = 180;
//    private double minRightAngle = -25, maxRightAngle = -180;
//
//    public TurnCostsConfig() {
//    }
//
//    public TurnCostsConfig(TurnCostsConfig copy) {
//        leftCost = copy.leftCost;
//        rightCost = copy.rightCost;
//        straightCost = copy.straightCost;
//        minLeftAngle = copy.minLeftAngle;
//        maxLeftAngle = copy.maxLeftAngle;
//        minRightAngle = copy.minRightAngle;
//        maxRightAngle = copy.maxRightAngle;
//    }
//
//    public TurnCostsConfig setLeftCost(double leftCost) {
//        this.leftCost = leftCost;
//        return this;
//    }
//
//    @JsonProperty("left")
//    public double getLeftCost() {
//        return leftCost;
//    }
//
//    public TurnCostsConfig setRightCost(double rightCost) {
//        this.rightCost = rightCost;
//        return this;
//    }
//
//    @JsonProperty("right")
//    public double getRightCost() {
//        return rightCost;
//    }
//
//    public TurnCostsConfig setStraightCost(double straightCost) {
//        this.straightCost = straightCost;
//        return this;
//    }
//
//    @JsonProperty("straight")
//    public double getStraightCost() {
//        return straightCost;
//    }
//
//    public void setMinLeftAngle(double minLeftAngle) {
//        this.minLeftAngle = minLeftAngle;
//    }
//
//    public double getMinLeftAngle() {
//        return minLeftAngle;
//    }
//
//    public void setMaxLeftAngle(double maxLeftAngle) {
//        this.maxLeftAngle = maxLeftAngle;
//    }
//
//    public double getMaxLeftAngle() {
//        return maxLeftAngle;
//    }
//
//    public void setMinRightAngle(double minRightAngle) {
//        this.minRightAngle = minRightAngle;
//    }
//
//    public double getMinRightAngle() {
//        return minRightAngle;
//    }
//
//    public void setMaxRightAngle(double maxRightAngle) {
//        this.maxRightAngle = maxRightAngle;
//    }
//
//    public double getMaxRightAngle() {
//        return maxRightAngle;
//    }
//
//    @Override
//    public boolean equals(Object o) {
//        if (this == o) return true;
//        if (o == null || getClass() != o.getClass()) return false;
//        TurnCostsConfig that = (TurnCostsConfig) o;
//        return Double.compare(that.leftCost, leftCost) == 0 && Double.compare(that.rightCost, rightCost) == 0
//                && Double.compare(that.straightCost, straightCost) == 0 && Double.compare(that.minLeftAngle, minLeftAngle) == 0
//                && Double.compare(that.maxLeftAngle, maxLeftAngle) == 0 && Double.compare(that.minRightAngle, minRightAngle) == 0
//                && Double.compare(that.maxRightAngle, maxRightAngle) == 0;
//    }
//
//    @Override
//    public int hashCode() {
//        return Objects.hash(leftCost, rightCost, straightCost, minLeftAngle, maxLeftAngle, minRightAngle, maxRightAngle);
//    }
//
//    @Override
//    public String toString() {
//        return "leftCost=" + leftCost + ", rightCost=" + rightCost + ", straightCost=" + straightCost
//                + ", minLeftAngle=" + minLeftAngle + ", maxLeftAngle=" + maxLeftAngle
//                + ", minRightAngle=" + minRightAngle + ", maxRightAngle=" + maxRightAngle;
//    }
//}
