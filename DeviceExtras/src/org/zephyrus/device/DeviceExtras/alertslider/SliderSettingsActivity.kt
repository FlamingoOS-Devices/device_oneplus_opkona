/*
 * Copyright (C) 2021 The LineageOS Project
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

package org.zephyrus.device.DeviceExtras.alertslider

import android.os.Bundle

import androidx.fragment.app.commit
import androidx.preference.ListPreference.SimpleSummaryProvider
import androidx.preference.PreferenceFragmentCompat

import com.android.settingslib.collapsingtoolbar.CollapsingToolbarBaseActivity
import com.flamingo.support.preference.SystemSettingListPreference

import org.zephyrus.device.DeviceExtras.R

private val Positions = listOf(
    AlertSliderPosition.Top,
    AlertSliderPosition.Middle,
    AlertSliderPosition.Bottom
)

class SliderSettingsActivity : CollapsingToolbarBaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                setReorderingAllowed(true)
                replace(
                    com.android.settingslib.collapsingtoolbar.R.id.content_frame,
                    SliderSettingsFragment(),
                    null
                )
            }
        }
    }

    class SliderSettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.alertslider_settings, rootKey)
            val modeEntries = Mode.values().map { resources.getString(it.title) }.toTypedArray()
            val modeEntryValues = Mode.values().map { it.toString() }.toTypedArray()
            Positions.map {
                SystemSettingListPreference(requireContext()).apply {
                    key = it.modeKey
                    title = resources.getString(it.title)
                    entries = modeEntries
                    entryValues = modeEntryValues
                    setDefaultValue(it.defaultMode.toString())
                    dialogTitle = title
                    summaryProvider = SimpleSummaryProvider.getInstance()
                }
            }.forEach {
                preferenceScreen.addPreference(it)
            }
        }
    }
}
