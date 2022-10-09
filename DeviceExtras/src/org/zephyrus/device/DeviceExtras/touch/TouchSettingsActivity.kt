/*
 * Copyright (C) 2022 FlamingoOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.zephyrus.device.DeviceExtras.touch

import android.os.Bundle

import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import androidx.preference.ListPreference.SimpleSummaryProvider
import androidx.preference.PreferenceFragmentCompat

import com.android.internal.lineage.hardware.LineageHardwareManager
import com.android.internal.lineage.hardware.LineageHardwareManager.FEATURE_TOUCHSCREEN_GESTURES
import com.android.internal.lineage.hardware.TouchscreenGesture
import com.android.settingslib.collapsingtoolbar.CollapsingToolbarBaseActivity
import com.flamingo.support.preference.SystemSettingListPreference

import org.zephyrus.device.DeviceExtras.R

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TouchSettingsActivity : CollapsingToolbarBaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                setReorderingAllowed(true)
                replace(
                    com.android.settingslib.collapsingtoolbar.R.id.content_frame,
                    TouchSettingsFragment(),
                    null
                )
            }
        }
    }

    class TouchSettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            lifecycleScope.launch(Dispatchers.Default) {
                val lhm = LineageHardwareManager.getInstance(requireContext())
                if (!lhm.isSupported(FEATURE_TOUCHSCREEN_GESTURES)) return@launch
                withContext(Dispatchers.Main) {
                    setPreferencesFromResource(R.xml.touch_settings, rootKey)
                }
                val gestureEntries = Action.values().map { resources.getString(it.title) }.toTypedArray()
                val gestureEntryValues = Action.values().map { it.toString() }.toTypedArray()
                lhm.touchscreenGestures.forEach { gesture: TouchscreenGesture ->
                    val listPreference = SystemSettingListPreference(requireContext()).apply {
                        key = gesture.settingKey
                        title = ScanCodeTitleMap[gesture.keycode]?.let { resources.getString(it) } ?: gesture.name
                        entries = gestureEntries
                        entryValues = gestureEntryValues
                        setDialogTitle(R.string.touchscreen_gesture_action_dialog_title)
                        setDefaultValue(getDefaultActionForScanCode(gesture.keycode).toString())
                        summaryProvider = SimpleSummaryProvider.getInstance()
                    }.also {
                        it.setOnPreferenceChangeListener { _, newValue ->
                            val action = Action.valueOf(newValue as String)
                            lifecycleScope.launch(Dispatchers.Default) {
                                lhm.setTouchscreenGestureEnabled(gesture, action != Action.NONE)
                            }
                            return@setOnPreferenceChangeListener true
                        }
                    }
                    withContext(Dispatchers.Main) {
                        preferenceScreen.addPreference(listPreference)
                    }
                }
            }
        }
    }
}
