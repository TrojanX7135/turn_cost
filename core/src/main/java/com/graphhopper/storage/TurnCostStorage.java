/*
 *  Licensed to GraphHopper GmbH under one or more contributor
 *  license agreements. See the NOTICE file distributed with this work for
 *  additional information regarding copyright ownership.
 *
 *  GraphHopper GmbH licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except in
 *  compliance with the License. You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.graphhopper.storage;

import com.graphhopper.routing.ev.BooleanEncodedValue;
import com.graphhopper.routing.ev.DecimalEncodedValue;
import com.graphhopper.routing.ev.EdgeIntAccess;
import com.graphhopper.routing.ev.IntsRefEdgeIntAccess;
import com.graphhopper.util.Constants;
import com.graphhopper.util.EdgeIterator;
import com.graphhopper.util.GHUtility;

/**
 * A key/value store, where the unique keys are triples (fromEdge, viaNode, toEdge) and the values
 * are integers that can be used to store encoded values.
 *
 * @author Karl Hübner
 * @author Peter Karich
 * @author Michael Zilske
 */
public class TurnCostStorage {
    // Không có mục nhập quay
    static final int NO_TURN_ENTRY = -1;
    // chúng tôi lưu trữ mỗi mục nhập chi phí quay theo định dạng |from_edge|to_edge|flags|next|. mỗi mục nhập có 4 byte -> tổng cộng 16 byte
    private static final int TC_FROM = 0;
    private static final int TC_TO = 4;
    private static final int TC_FLAGS = 8;
    private static final int TC_NEXT = 12;
    private static final int BYTES_PER_ENTRY = 16;

    // Đồ thị cơ sở
    private final BaseGraph baseGraph;
    // Chi phí quay
    private final DataAccess turnCosts;
    // Số lượng chi phí quay
    private int turnCostsCount;

    // Hàm khởi tạo
    public TurnCostStorage(BaseGraph baseGraph, DataAccess turnCosts) {
        this.baseGraph = baseGraph;
        this.turnCosts = turnCosts;
    }

    // Tạo bộ nhớ cho chi phí quay
    public TurnCostStorage create(long initBytes) {
        turnCosts.create(initBytes);
        return this;
    }

    // Đẩy dữ liệu vào bộ nhớ
    public void flush() {
        turnCosts.setHeader(0, Constants.VERSION_TURN_COSTS);
        turnCosts.setHeader(4, BYTES_PER_ENTRY);
        turnCosts.setHeader(2 * 4, turnCostsCount);
        turnCosts.flush();
    }

    // Đóng bộ nhớ
    public void close() {
        turnCosts.close();
    }

    // Lấy dung lượng bộ nhớ
    public long getCapacity() {
        return turnCosts.getCapacity();
    }

    // Tải dữ liệu đã tồn tại
    public boolean loadExisting() {
        if (!turnCosts.loadExisting())
            return false;

        GHUtility.checkDAVersion(turnCosts.getName(), Constants.VERSION_TURN_COSTS, turnCosts.getHeader(0));
        if (turnCosts.getHeader(4) != BYTES_PER_ENTRY) {
            throw new IllegalStateException("Number of bytes per turn cost entry does not match the current configuration: " + turnCosts.getHeader(0) + " vs. " + BYTES_PER_ENTRY);
        }
        turnCostsCount = turnCosts.getHeader(8);
        return true;
    }


    // Đặt giá trị cho BooleanEncodedValue
    public void set(BooleanEncodedValue bev, int fromEdge, int viaNode, int toEdge, boolean value) {
        long pointer = findOrCreateTurnCostEntry(fromEdge, viaNode, toEdge);
        if (pointer < 0)
            throw new IllegalStateException("Invalid pointer: " + pointer + " at (" + fromEdge + ", " + viaNode + ", " + toEdge + ")");
        bev.setBool(false, -1, createIntAccess(pointer), value);
    }

    // Đặt chi phí quay tại viaNode khi đi từ "fromEdge" đến "toEdge"
    public void set(DecimalEncodedValue turnCostEnc, int fromEdge, int viaNode, int toEdge, double cost) {
        long pointer = findOrCreateTurnCostEntry(fromEdge, viaNode, toEdge);
        if (pointer < 0)
            throw new IllegalStateException("Invalid pointer: " + pointer + " at (" + fromEdge + ", " + viaNode + ", " + toEdge + ")");
        turnCostEnc.setDecimal(false, -1, createIntAccess(pointer), cost);
    }

    // Tìm hoặc tạo mục nhập chi phí quay
    private long findOrCreateTurnCostEntry(int fromEdge, int viaNode, int toEdge) {
        long pointer = findPointer(fromEdge, viaNode, toEdge);
        if (pointer < 0) {
            // tạo một mục nhập mới
            ensureTurnCostIndex(turnCostsCount);
            int prevIndex = baseGraph.getNodeAccess().getTurnCostIndex(viaNode);
            baseGraph.getNodeAccess().setTurnCostIndex(viaNode, turnCostsCount);
            pointer = (long) turnCostsCount * BYTES_PER_ENTRY;
            turnCosts.setInt(pointer + TC_FROM, fromEdge);
            turnCosts.setInt(pointer + TC_TO, toEdge);
            turnCosts.setInt(pointer + TC_NEXT, prevIndex);
            turnCostsCount++;
        }
        return pointer;
    }

    // Lấy giá trị DecimalEncodedValue
    public double get(DecimalEncodedValue dev, int fromEdge, int viaNode, int toEdge) {
        return dev.getDecimal(false, -1, createIntAccess(findPointer(fromEdge, viaNode, toEdge)));
    }

    // Lấy giá trị BooleanEncodedValue
    public boolean get(BooleanEncodedValue bev, int fromEdge, int viaNode, int toEdge) {
        return bev.getBool(false, -1, createIntAccess(findPointer(fromEdge, viaNode, toEdge)));
    }

    // Tạo truy cập Int cho cạnh
    private EdgeIntAccess createIntAccess(long pointer) {
        return new EdgeIntAccess() {
            @Override
            public int getInt(int edgeId, int index) {
                return pointer < 0 ? 0 : turnCosts.getInt(pointer + TC_FLAGS);
            }

            @Override
            public void setInt(int edgeId, int index, int value) {
                if (pointer < 0)
                    throw new IllegalStateException("pointer must not be negative: " + pointer);
                turnCosts.setInt(pointer + TC_FLAGS, value);
            }
        };
    }

    // Đảm bảo chỉ số chi phí quay
    private void ensureTurnCostIndex(int nodeIndex) {
        turnCosts.ensureCapacity(((long) nodeIndex + 4) * BYTES_PER_ENTRY);
    }

    // Tìm con trỏ
    private long findPointer(int fromEdge, int viaNode, int toEdge) {
        if (!EdgeIterator.Edge.isValid(fromEdge) || !EdgeIterator.Edge.isValid(toEdge))
            throw new IllegalArgumentException("from and to edge cannot be NO_EDGE");
        if (viaNode < 0)
            throw new IllegalArgumentException("via node cannot be negative");

        final int maxEntries = 1000;
        int index = baseGraph.getNodeAccess().getTurnCostIndex(viaNode);
        for (int i = 0; i < maxEntries; ++i) {
            if (index == NO_TURN_ENTRY) return -1;
            long pointer = (long) index * BYTES_PER_ENTRY;
            if (fromEdge == turnCosts.getInt(pointer + TC_FROM) && toEdge == turnCosts.getInt(pointer + TC_TO))
                return pointer;
            index = turnCosts.getInt(pointer + TC_NEXT);
        }
        throw new IllegalStateException("Turn cost list for node: " + viaNode + " is longer than expected, max: " + maxEntries);
    }

    // Kiểm tra xem bộ nhớ có đóng không
    public boolean isClosed() {
        return turnCosts.isClosed();
    }

    // Trả về chuỗi "turn_cost"
    @Override
    public String toString() {
        return "turn_cost";
    }

    // TODO: Maybe some of the stuff above could now be re-implemented in a simpler way with some of the stuff below.
    // For now, I just wanted to iterate over all entries.

    /**
     * Returns an iterator over all entries.
     *
     * @return an iterator over all entries.
     */
    
    // Trả về một iterator trên tất cả các mục nhập.
    public Iterator getAllTurnCosts() {
        return new Itr();
    }

    // Interface Iterator
    public interface Iterator {
        int getFromEdge();

        int getViaNode();

        int getToEdge();

        boolean get(BooleanEncodedValue booleanEncodedValue);

        double getCost(DecimalEncodedValue encodedValue);

        boolean next();
    }

    // Lớp Itr triển khai interface Iterator
    private class Itr implements Iterator {
        // Khởi tạo các biến
        private int viaNode = -1;
        private int turnCostIndex = -1;
        private final IntsRef intsRef = new IntsRef(1);
        private final EdgeIntAccess edgeIntAccess = new IntsRefEdgeIntAccess(intsRef);

        // Trả về con trỏ chi phí quay
        private long turnCostPtr() {
            return (long) turnCostIndex * BYTES_PER_ENTRY;
        }

        // Trả về cạnh từ
        @Override
        public int getFromEdge() {
            return turnCosts.getInt(turnCostPtr() + TC_FROM);
        }

        // Trả về nút qua
        @Override
        public int getViaNode() {
            return viaNode;
        }

        // Trả về cạnh đến
        @Override
        public int getToEdge() {
            return turnCosts.getInt(turnCostPtr() + TC_TO);
        }

        // Trả về giá trị BooleanEncodedValue
        @Override
        public boolean get(BooleanEncodedValue booleanEncodedValue) {
            intsRef.ints[0] = turnCosts.getInt(turnCostPtr() + TC_FLAGS);
            return booleanEncodedValue.getBool(false, -1, edgeIntAccess);
        }

        // Trả về chi phí DecimalEncodedValue
        @Override
        public double getCost(DecimalEncodedValue encodedValue) {
            intsRef.ints[0] = turnCosts.getInt(turnCostPtr() + TC_FLAGS);
            return encodedValue.getDecimal(false, -1, edgeIntAccess);
        }

        // Di chuyển đến mục nhập tiếp theo
        @Override
        public boolean next() {
            boolean gotNextTci = nextTci();
            if (!gotNextTci) {
                turnCostIndex = NO_TURN_ENTRY;
                boolean gotNextNode = true;
                while (turnCostIndex == NO_TURN_ENTRY && (gotNextNode = nextNode())) {

                }
                if (!gotNextNode) {
                    return false;
                }
            }
            return true;
        }

        // Di chuyển đến nút tiếp theo
        private boolean nextNode() {
            viaNode++;
            if (viaNode >= baseGraph.getNodes()) {
                return false;
            }
            turnCostIndex = baseGraph.getNodeAccess().getTurnCostIndex(viaNode);
            return true;
        }

        // Di chuyển đến chỉ số chi phí quay tiếp theo
        private boolean nextTci() {
            if (turnCostIndex == NO_TURN_ENTRY) {
                return false;
            }
            turnCostIndex = turnCosts.getInt(turnCostPtr() + TC_NEXT);
            if (turnCostIndex == NO_TURN_ENTRY) {
                return false;
            }
            return true;
        }
    }
}

