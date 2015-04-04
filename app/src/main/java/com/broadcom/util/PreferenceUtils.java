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

import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.util.Log;

public class PreferenceUtils {
    private static final String TAG = "PreferenceUtils";

    public static void setSummaryToEditTextValue(PreferenceScreen root, String prefKey) {
        EditTextPreference pref = null;
        try {
            pref = (EditTextPreference) root.findPreference(prefKey);
            if (pref == null) {
                return;
            }
            pref.setSummary(((EditTextPreference) pref).getText());
        } catch (Throwable t) {
            Log.w(TAG, "setSummaryToEditTextValue(): error", t);
        }
    }

    public static void setSummaryToListValue(PreferenceScreen root, String prefKey) {
        ListPreference pref = null;
        try {
            pref = (ListPreference) root.findPreference(prefKey);
            if (pref == null) {
                return;
            }
            pref.setSummary(((ListPreference) pref).getEntry());
        } catch (Throwable t) {
            Log.w(TAG, "setSummaryToListValue(): error", t);
        }
    }

    public static void setSummaryToValue(Preference pref) {
        if (pref.getClass() == EditTextPreference.class) {
            pref.setSummary(((EditTextPreference) pref).getText());
        } else if (pref.getClass() == ListPreference.class) {
            pref.setSummary(((ListPreference) pref).getEntry());
        } else {
            Log.w(TAG, "setSummaryToValue(): unknown preference type " + pref);
        }
    }

    public static void setSummaryToValue(PreferenceGroup pGroup) {
        int prefcount = pGroup.getPreferenceCount();
        for (int i = 0; i < prefcount; i++) {
            setSummaryToValue(pGroup.getPreference(i));
        }
    }

    public static void setSummaryToValue(PreferenceScreen root, String prefKey) {
        Preference pref = null;
        try {
            pref = root.findPreference(prefKey);
            if (pref == null) {
                Log.w(TAG, "setSummaryToValue(): preference not found " + prefKey);
                return;
            }
            setSummaryToValue(pref);
        } catch (Throwable t) {
            Log.w(TAG, "setSummaryToValue(): error", t);
        }
    }

    public static void setValue(Preference pref, String value) {
        if (pref.getClass() == EditTextPreference.class) {
            ((EditTextPreference) pref).setText(value);
        } else if (pref.getClass() == ListPreference.class) {
            ((ListPreference) pref).setValue(value);
        } else {
            Log.w(TAG, "setValue(): unknown preference type " + pref.getClass());
        }
    }

    public static void setValue(PreferenceScreen root, String prefKey, String value) {
        Preference pref = null;
        try {
            pref = root.findPreference(prefKey);
            if (pref == null) {
                Log.d(TAG, "setValue(): preference not found " + prefKey);
                return;
            }
            setValue(pref, value);
        } catch (Throwable t) {
            Log.d(TAG, "setValue(): error", t);
        }

    }

}
