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

/**
 * Helper math class
 *
 *
 */
public class MathUtils {

    public static double degreesToRadians(double value) {
        return 0.0174532925 * value;
    }

    public static double radiansToDegrees(double value) {
        return 57.295779578 * value;
    }

    public static double getMagnitude(double x, double y) {
        return Math.sqrt(x * x + y * y);
    }

    public static double getDegrees(double y, double x) {

        double mag = getMagnitude(x, y);
        double nY = y / mag;
        double nX = x / mag;
        return radiansToDegrees(Math.atan2(nY, nX));
    }
}
