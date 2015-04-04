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
package com.broadcom.app.wicedsense;

import com.broadcom.util.MathUtils;

/**
 * Helper class to parse the sensor data packets
 *
 * @author fredc
 *
 */
public class SensorDataParser {
    public static final String TAG = Settings.TAG_PREFIX + "SensorDataParser";

    public static final int SENSOR_FLAG_HUMIDITY = (0x1 << 2);
    public static final int SENSOR_FLAG_PRESSURE = (0x1 << 4);
    public static final int SENSOR_FLAG_TEMP = (0x1 << 5);

    public static final int SENSOR_HUMIDITY_MIN = 0;
    public static final int SENSOR_HUMIDITY_MAX = 100;

    public static final int SENSOR_PRESSURE_MIN = 800;
    public static final int SENSOR_PRESSURE_MAX = 1200;

    public static final float SENSOR_TEMP_MIN_F = -40;
    public static final float SENSOR_TEMP_MAX_F = 120;

    public static final float SENSOR_TEMP_MIN_C = -40;
    public static final float SENSOR_TEMP_MAX_C = 60;

    public static final int SENSOR_TEMP_DATA_SIZE = 2;
    public static final int SENSOR_PRES_DATA_SIZE = 2;
    public static final int SENSOR_HUMD_DATA_SIZE = 2;

    private static int getTwoByteValue(byte[] bytes, int offset) {
        return (bytes[offset + 1] << 8) + (bytes[offset] & 0xFF);
    }

    public static boolean humidityHasChanged(int mask) {
        return (SENSOR_FLAG_HUMIDITY & mask) > 0;
    }

    public static boolean temperatureHasChanged(int mask) {
        return (SENSOR_FLAG_TEMP & mask) > 0;
    }

    public static boolean pressureHasChanged(int mask) {
        return (SENSOR_FLAG_PRESSURE & mask) > 0;
    }

    public static float getHumidityPercent(byte[] sensorData, int offset) {
        return ((float) getTwoByteValue(sensorData, offset)) / 10;
    }

    public static float getPressureMBar(byte[] sensorData, int offset) {
        return ((float) getTwoByteValue(sensorData, offset)) / 10;
    }

    public static float getTemperatureC(byte[] sensorData, int offset) {
        return ((float) getTwoByteValue(sensorData, offset)) / 10;
    }

    public static float getTemperatureF(byte[] sensorData, int offset) {
        return getTemperatureC(sensorData, offset) * 9 / 5 + 32;
    }

    public static float tempCtoF(float c) {
        return c * 9 / 5 + 32;
    }

    public static float tempFtoC(float f) {
        return (f - 32) * 5 / 9;
    }

}