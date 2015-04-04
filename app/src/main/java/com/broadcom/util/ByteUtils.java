/******************************************************************************
 *
 *  Copyright (C) 2014 Broadcom Corporation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 ******************************************************************************/
package com.broadcom.util;

import android.bluetooth.BluetoothDevice;

/**
 * Helper class to convert bytes to ints and ints to bytes
 *
 */
public class ByteUtils {
    static final int BD_ADDR_LEN = 6; // bytes

    public static void printBytes(byte[] bytes, StringBuilder builder, int bytesPerLine) {
        for (int i = 0; i < bytes.length; i++) {
            if (i != 0 && i % bytesPerLine == 0) {
                builder.append("\n");
            }
            builder.append(String.format("%02x  ", bytes[i]));
        }
    }

    public static int bytesToUInt16LI(byte[] bytes, int startIndex, int defaultValue) {
        if (bytes == null || startIndex + 1 >= bytes.length) {
            return defaultValue;
        }
        int value = bytes[startIndex] + (((int) bytes[startIndex + 1]) << 8);
        return value;
    }

    public static String printBytes(byte[] bytes, int bytesPerLine) {
        StringBuilder builder = new StringBuilder();
        printBytes(bytes, builder, bytesPerLine);
        return builder.toString();
    }

    public static void uInt32ToBytesLI(long value, byte[] buffer, int startIndex) {
        buffer[startIndex++] = (byte) (value >>> 0);
        buffer[startIndex++] = (byte) (value >>> 8);
        buffer[startIndex++] = (byte) (value >>> 16);
        buffer[startIndex++] = (byte) (value >>> 24);
    }

    public static byte[] uInt32ToBytesLI(long value) {
        byte b[] = new byte[4];
        uInt32ToBytesLI(value, b, 0);
        return b;
    }

    public static void uInt32ToBytesBI(long value, byte[] buffer, int startIndex) {
        buffer[startIndex++] = (byte) (value >>> 24);
        buffer[startIndex++] = (byte) (value >>> 16);
        buffer[startIndex++] = (byte) (value >>> 8);
        buffer[startIndex++] = (byte) (value >>> 0);
    }

    public static byte[] uInt32ToBytesBI(long value) {
        byte b[] = new byte[4];
        uInt32ToBytesBI(value, b, 0);
        return b;
    }

    public static void uInt16ToBytesLI(int value, byte[] buffer, int startIndex) {
        buffer[startIndex++] = (byte) (value >>> 0);
        buffer[startIndex++] = (byte) (value >>> 8);
    }

    public static void uInt16ToBytesBI(int value, byte[] buffer, int startIndex) {
        buffer[startIndex++] = (byte) (value >>> 8);
        buffer[startIndex++] = (byte) (value >>> 0);
    }

    public static byte[] uInt16ToBytesLI(int value) {
        byte b[] = new byte[2];
        uInt16ToBytesLI(value, b, 0);
        return b;
    }

    public static byte[] uInt16ToBytesBI(int value) {
        byte b[] = new byte[2];
        uInt16ToBytesBI(value, b, 0);
        return b;
    }

    public static byte[] getByteAddress(BluetoothDevice device) {
        return getBytesFromAddress(device.getAddress());
    }

    public static byte[] getBytesFromAddress(String address) {
        int i, j = 0;
        byte[] output = new byte[BD_ADDR_LEN];
        if (address != null) {
            for (i = 0; i < address.length(); i++) {
                if (address.charAt(i) != ':') {
                    output[j] = (byte) Integer.parseInt(address.substring(i, i + 2), 16);
                    j++;
                    i++;
                }
            }
        }
        return output;
    }

    public static String getAddressStringFromByte(byte[] address, boolean includeSeparator) {
        if (address == null || address.length != 6) {
            return null;
        }
        if (includeSeparator) {
            return String.format("%02X:%02X:%02X:%02X:%02X:%02X", address[0], address[1],
                    address[2], address[3], address[4], address[5]);
        } else {
            return String.format("%02X%02X%02X%02X%02X%02X", address[0], address[1], address[2],
                    address[3], address[4], address[5]);
        }
    }

    public static String getAddressStringFromByte(byte[] address) {
        return getAddressStringFromByte(address, true);
    }
}
