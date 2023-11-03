package com.graphhopper.util;

import com.fasterxml.jackson.annotation.JsonProperty;
import javax.script.ScriptException;

public class TurnCostRule {
    @JsonProperty("if")
    private String condition;
    @JsonProperty("straight")
    private String straight;
    @JsonProperty("left")
    private String left;
    @JsonProperty("right")
    private String right;

    // Getter and Setter for condition
    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    // Getter and Setter for straight
    public double getStraight() throws ScriptException {
        return evaluateExpression(straight);
    }

    public void setStraight(String straight) {
        this.straight = straight;
    }

    // Getter and Setter for left
    public double getLeft() throws ScriptException {
        return evaluateExpression(left);
    }

    public void setLeft(String left) {
        this.left = left;
    }

    // Getter and Setter for right
    public double getRight() throws ScriptException {
        return evaluateExpression(right);
    }

    public void setRight(String right) {
        this.right = right;
    }

    // Method to evaluate a mathematical expression in a string
    private double evaluateExpression(String expression) {
        String[] parts = expression.split(" ");
        if (parts.length % 2 == 0) {
            throw new IllegalArgumentException("Invalid expression: " + expression);
        }
        double result = Double.parseDouble(parts[0]);
        for (int i = 1; i < parts.length; i += 2) {
            String operator = parts[i];
            double value = Double.parseDouble(parts[i + 1]);
            switch (operator) {
                case "+":
                    result += value;
                    break;
                case "-":
                    result -= value;
                    break;
                case "*":
                    result *= value;
                    break;
                case "/":
                    if (value != 0) {
                        result /= value;
                    } else {
                        throw new IllegalArgumentException("Division by zero is not allowed.");
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Unknown operator: " + operator);
            }
        }
        return result;
    }


}
