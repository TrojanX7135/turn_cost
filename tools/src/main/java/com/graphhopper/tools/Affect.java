package com.graphhopper.tools;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.graphhopper.routing.ev.EncodedValueLookup;
import com.graphhopper.routing.ev.EnumEncodedValue;
import com.graphhopper.routing.ev.RoadAccess;
import com.graphhopper.routing.ev.StringEncodedValue;
import com.graphhopper.util.EdgeIteratorState;


// Lớp Affect đại diện cho một câu lệnh có điều kiện
public class Affect {

	@JsonProperty("condition")
    private String condition;
    
    // Các giá trị ảnh hưởng tương ứng với các hướng: thẳng, trái, và phải
	@JsonProperty("straightAffect")
    private double straightAffect;
	
	@JsonProperty("leftAffect")
    private double leftAffect;
	
	@JsonProperty("rightAffect")
    private double rightAffect;
   
    
    public Affect() {
    	this.condition = "0";
    }
    
    // Các phương thức setter để đặt giá trị cho các thuộc tính
    public void setCondition(String Condition) {
        this.condition = Condition;
    }

    public void setStraightAffect(double straightAffect) {
        this.straightAffect = straightAffect;
    }
    
    public void setLeftAffect(double leftAffect) {
        this.leftAffect = leftAffect;
    }
    
    public void setRightAffect(double rightAffect) {
        this.rightAffect = rightAffect;
    }
    
    
    
    // Các phương thức getter để lấy các thuộc tính của đối tượng
    public String getCondition() {
        return condition;
    }

    public double getStraightAffect(EncodedValueLookup lookup, EdgeIteratorState edgeState) {
    	affectCalc(lookup, edgeState, this.condition);
        return straightAffect;
    }
    
    public double getLeftAffect(EncodedValueLookup lookup, EdgeIteratorState edgeState) {
    	affectCalc(lookup, edgeState, this.condition);
        return leftAffect;
    }
    
    public double getRightAffect(EncodedValueLookup lookup, EdgeIteratorState edgeState) {
    	affectCalc(lookup, edgeState, this.condition);
        return rightAffect;
    }
    
    
    
    // Clac
    private void affectCalc(EncodedValueLookup lookup, EdgeIteratorState edgeState, String expression) {
    	String[] parts = expression.split(" ");
        if (expression.contains("road_access")) {
            if (parts[1].equals("==")) {
            	boolean resultCheck = checkRoadAccess(lookup, edgeState, parts[2]);
            	if (resultCheck) {
                   	this.straightAffect = Double.parseDouble(parts[4]);
                	this.leftAffect = Double.parseDouble(parts[5]);
                	this.rightAffect = Double.parseDouble(parts[6]);
            	} else {
                	this.straightAffect = 0;
                	this.leftAffect = 0;
                	this.rightAffect = 0;
                }    
            } else {
            	this.straightAffect = 0;
            	this.leftAffect = 0;
            	this.rightAffect = 0;
            }
        } else {
        	this.straightAffect = 0;
        	this.leftAffect = 0;
        	this.rightAffect = 0;
        }
    }

    
    
 // Phương thức để tính toán giá trị Affect dựa trên customModel
    public boolean checkRoadAccess(EncodedValueLookup lookup, EdgeIteratorState edgeState, String value) {
        // Giả sử road_access là một EnumEncodedValue đã được định nghĩa
        EnumEncodedValue<RoadAccess> roadAccessEnc = lookup.getEnumEncodedValue("road_access", RoadAccess.class);
        RoadAccess roadAccessValue = edgeState.get(roadAccessEnc);
        
        // Chuyển đổi chuỗi String thành giá trị enum RoadAccess
        RoadAccess userProvidedAccess;
        try {
            userProvidedAccess = RoadAccess.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            // Xử lý trường hợp chuỗi không phải là giá trị hợp lệ của enum RoadAccess
            userProvidedAccess = null;
        }

        // Kiểm tra điều kiện và cập nhật giá trị Affect
        if (userProvidedAccess != null && roadAccessValue == userProvidedAccess) {
            return true;
        }
        return false;
        
        // Thêm các điều kiện khác nếu cần
    }
}

