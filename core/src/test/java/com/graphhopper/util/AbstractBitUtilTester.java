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
package com.graphhopper.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Karich
 */
public abstract class AbstractBitUtilTester {
    protected BitUtil bitUtil = getBitUtil();

    abstract BitUtil getBitUtil();

    @Test
    public void testToFloat() {
        byte[] bytes = bitUtil.fromFloat(Float.MAX_VALUE);
        assertEquals(Float.MAX_VALUE, bitUtil.toFloat(bytes), 1e-9);

        bytes = bitUtil.fromFloat(Float.MAX_VALUE / 3);
        assertEquals(Float.MAX_VALUE / 3, bitUtil.toFloat(bytes), 1e-9);
    }

    @Test
    public void testToDouble() {
        byte[] bytes = bitUtil.fromDouble(Double.MAX_VALUE);
        assertEquals(Double.MAX_VALUE, bitUtil.toDouble(bytes), 1e-9);

        bytes = bitUtil.fromDouble(Double.MAX_VALUE / 3);
        assertEquals(Double.MAX_VALUE / 3, bitUtil.toDouble(bytes), 1e-9);
    }

    @Test
    public void testToInt() {
        byte[] bytes = bitUtil.fromInt(Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, bitUtil.toInt(bytes));

        bytes = bitUtil.fromInt(Integer.MAX_VALUE / 3);
        assertEquals(Integer.MAX_VALUE / 3, bitUtil.toInt(bytes));
    }

    @Test
    public void testToShort() {
        byte[] bytes = bitUtil.fromShort(Short.MAX_VALUE);
        assertEquals(Short.MAX_VALUE, bitUtil.toShort(bytes));

        bytes = bitUtil.fromShort((short) (Short.MAX_VALUE / 3));
        assertEquals(Short.MAX_VALUE / 3, bitUtil.toShort(bytes));

        bytes = bitUtil.fromShort((short) -123);
        assertEquals(-123, bitUtil.toShort(bytes));

        bytes = bitUtil.fromShort((short) (0xFF | 0xFF));
        assertEquals(0xFF | 0xFF, bitUtil.toShort(bytes));
    }

    @Test
    public void testToLong() {
        byte[] bytes = bitUtil.fromLong(Long.MAX_VALUE);
        assertEquals(Long.MAX_VALUE, bitUtil.toLong(bytes));

        bytes = bitUtil.fromLong(Long.MAX_VALUE / 7);
        assertEquals(Long.MAX_VALUE / 7, bitUtil.toLong(bytes));
    }

    @Test
    public void testIntsToLong() {
        int high = 2565;
        int low = 9421;
        long l = bitUtil.toLong(low, high);
        assertEquals(high, bitUtil.getIntHigh(l));
        assertEquals(low, bitUtil.getIntLow(l));
    }

    @Test
    public void testToLastBitString() {
        assertEquals("1", bitUtil.toLastBitString(1L, 1));
        assertEquals("01", bitUtil.toLastBitString(1L, 2));
        assertEquals("001", bitUtil.toLastBitString(1L, 3));
        assertEquals("010", bitUtil.toLastBitString(2L, 3));
        assertEquals("011", bitUtil.toLastBitString(3L, 3));
    }

}
