package com.graphhopper.util.details;

import com.graphhopper.routing.weighting.Weighting;
import com.graphhopper.util.EdgeIteratorState;

//Lớp TurnCostsDetails kế thừa từ lớp AbstractPathDetailsBuilder
public class TurnCostsDetails extends AbstractPathDetailsBuilder {

 // Trường dữ liệu cho lớp TurnCostsDetails
 private Weighting weighting; // Trọng số được sử dụng để tính toán chi phí rẽ
 private EdgeIteratorState prevEdge; // Cạnh trước đó trong đường dẫn
 private Double costs; // Chi phí rẽ hiện tại

 // Hàm khởi tạo cho lớp TurnCostsDetails
 public TurnCostsDetails(Weighting weighting) {
     super("turn_costs"); // Gọi hàm khởi tạo của lớp cha với tham số là "turn_costs"

     this.weighting = weighting; // Gán trọng số
 }

 // Phương thức kiểm tra xem cạnh hiện tại có khác với cạnh cuối cùng hay không
 @Override
 public boolean isEdgeDifferentToLastEdge(EdgeIteratorState edge) {
     final Double currCosts; // Chi phí rẽ hiện tại
     // Nếu cạnh trước đó tồn tại và khác với cạnh hiện tại, tính toán chi phí rẽ
     if (prevEdge != null && prevEdge != edge) {
         currCosts = Math.round(weighting.calcTurnMillis(prevEdge.getEdge(), prevEdge.getAdjNode(), edge.getEdge()) / 100d) / 10d;
     } else {
         currCosts = 0d; // Nếu không, chi phí rẽ là 0
     }

     prevEdge = edge; // Cập nhật cạnh trước đó thành cạnh hiện tại
     costs = currCosts; // Cập nhật chi phí rẽ hiện tại
     return true; // Trả về true vì phương thức này luôn luôn thay đổi giá trị chi phí rẽ
 }

 // Phương thức lấy giá trị chi phí rẽ hiện tại
 @Override
 protected Object getCurrentValue() {
     return costs; // Trả về chi phí rẽ hiện tại
 }
}
