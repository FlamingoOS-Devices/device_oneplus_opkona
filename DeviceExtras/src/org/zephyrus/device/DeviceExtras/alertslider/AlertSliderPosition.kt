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

package org.zephyrus.device.DeviceExtras.alertslider

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

import org.zephyrus.device.DeviceExtras.R

private const val KEY_BOTTOM_POSITION_MODE = "alertslider_bottom_position_mode"
private const val KEY_MIDDLE_POSITION_MODE = "alertslider_middle_position_mode"
private const val KEY_TOP_POSITION_MODE = "alertslider_top_position_mode"

enum class Mode(
    @DrawableRes val icon: Int,
    @StringRes val title: Int
) {
    NORMAL(R.drawable.ic_volume_ringer, R.string.mode_normal),
    VIBRATE(R.drawable.ic_volume_ringer_vibrate, R.string.mode_vibrate),
    PRIORITY(R.drawable.ic_dnd, R.string.mode_priority),
    SILENT(R.drawable.ic_volume_ringer_mute, R.string.mode_silent),
    DND(R.drawable.ic_dnd, R.string.mode_dnd)
}

sealed class AlertSliderPosition(
    @StringRes val title: Int,
    val modeKey: String,
    val defaultMode: Mode
) {
    object Bottom : AlertSliderPosition(
        R.string.alert_slider_bottom_title,
        KEY_BOTTOM_POSITION_MODE,
        Mode.NORMAL
    )
    object Middle : AlertSliderPosition(
        R.string.alert_slider_middle_title,
        KEY_MIDDLE_POSITION_MODE,
        Mode.VIBRATE
    )
    object Top : AlertSliderPosition(
        R.string.alert_slider_top_title,
        KEY_TOP_POSITION_MODE,
        Mode.SILENT
    )
}
