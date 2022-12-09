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

import android.content.Context
import android.content.ContentResolver
import android.content.res.Configuration
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.UserHandle
import android.provider.Settings

private const val TIMEOUT = 1000L

class AlertSliderController(context: Context) {

    private val handler = Handler(Looper.getMainLooper())
    private val powerManager = context.getSystemService(PowerManager::class.java)
    private val contentResolver = context.getContentResolver()

    private val dialog = AlertSliderDialog(context)
    private val dismissDialogRunnable = Runnable { dialog.dismiss() }

    fun updateConfiguration(newConfig: Configuration) {
        removeHandlerCalbacks()
        dialog.updateConfiguration(newConfig)
    }

    fun updateDialog(mode: Mode) {
        dialog.setIconAndLabel(mode.icon, mode.title)
    }

    fun showDialog(position: AlertSliderPosition) {
        val pulseSliderSetting = Settings.System.getIntForUser(
        contentResolver,
        PULSE_SLIDER,
        0,
        UserHandle.USER_CURRENT
        ) == 1
        removeHandlerCalbacks()
        if (powerManager.isInteractive || pulseSliderSetting) {
            dialog.show(position)
            handler.postDelayed(dismissDialogRunnable, TIMEOUT)
        }
    }

    private fun removeHandlerCalbacks() {
        if (handler.hasCallbacks(dismissDialogRunnable)) {
            handler.removeCallbacks(dismissDialogRunnable)
        }
    }

    companion object {
        private const val TAG = "AlertSliderController"

        private const val PULSE_SLIDER = "pulse_slider"
    }
}
