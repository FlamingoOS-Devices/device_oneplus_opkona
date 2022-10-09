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

import android.animation.ValueAnimator
import android.app.Dialog
import android.content.Context
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.graphics.Point
import android.graphics.drawable.GradientDrawable
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Surface
import android.view.View
import android.view.WindowManager
import android.view.WindowManager.LayoutParams
import android.widget.ImageView
import android.widget.TextView

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.animation.addListener
import androidx.core.view.doOnLayout

import org.zephyrus.device.DeviceExtras.R

private const val APPEAR_ANIM_DURATION = 300L
private const val TRANSITION_ANIM_DURATION = 300L

class AlertSliderDialog(baseContext: Context) {

    private val windowManager = baseContext.getSystemService(WindowManager::class.java)

    private val alertSliderTopY: Int
    private val stepSize: Int
    private val positionGravity: Int

    private lateinit var view: View
    private lateinit var background: GradientDrawable
    private lateinit var icon: ImageView
    private lateinit var label: TextView

    private lateinit var currPosition: AlertSliderPosition
    private lateinit var prevPosition: AlertSliderPosition

    private var context = ContextThemeWrapper(baseContext, R.style.Theme_SubSettingsBase)

    private var appearAnimator: ValueAnimator? = null
    private var transitionAnimator: ValueAnimator? = null
    private var radiusAnimator: ValueAnimator? = null

    private var isPortrait = context.resources.configuration.orientation ==
        Configuration.ORIENTATION_PORTRAIT

    private var layoutParams = LayoutParams(
        LayoutParams.WRAP_CONTENT /** width */,
        LayoutParams.WRAP_CONTENT /** height */,
        LayoutParams.TYPE_SECURE_SYSTEM_OVERLAY /** type */,
        LayoutParams.FLAG_NOT_FOCUSABLE or
            LayoutParams.FLAG_NOT_TOUCH_MODAL or
            LayoutParams.FLAG_NOT_TOUCHABLE or
            LayoutParams.FLAG_HARDWARE_ACCELERATED /** flags */,
        PixelFormat.TRANSLUCENT /** format */
    ).apply {
        privateFlags = privateFlags or LayoutParams.SYSTEM_FLAG_SHOW_FOR_ALL_USERS
    }

    init {
        context.resources.let {
            alertSliderTopY = it.getInteger(R.integer.alertslider_top_y)
            stepSize = it.getInteger(R.integer.alertslider_width) / 2
            positionGravity = if (it.getBoolean(R.bool.config_alertSliderOnLeft)) Gravity.LEFT
                else Gravity.RIGHT
        }
        inflateLayout()
    }

    fun updateConfiguration(newConfig: Configuration) {
        radiusAnimator?.cancel()
        transitionAnimator?.cancel()
        appearAnimator?.cancel()
        if (view.parent != null) {
            windowManager.removeViewImmediate(view)
        }
        isPortrait = newConfig.orientation == Configuration.ORIENTATION_PORTRAIT
        context = ContextThemeWrapper(context.baseContext, R.style.Theme_SubSettingsBase)
        inflateLayout()
    }

    fun setIconAndLabel(@DrawableRes iconResId: Int, @StringRes labelResId: Int) {
        icon.setImageResource(iconResId)
        label.setText(labelResId)
    }

    fun show(position: AlertSliderPosition) {
        prevPosition = if (::currPosition.isInitialized) {
            currPosition
        } else {
            position
        }
        currPosition = position
        appearAnimator?.cancel()
        if (view.parent == null) {
            view.alpha = 0f
            windowManager.addView(view, layoutParams)
            // Only start the animations after view has been drawn
            // Need to get measured height to position the view correctly
            view.doOnLayout {
                layoutParams = updateLayoutParams()
                updateCornerRadii(false)
                windowManager.updateViewLayout(view, layoutParams)
                animateAppear(true)
            }
        } else {
            radiusAnimator?.end()
            transitionAnimator?.end()
            view.alpha = 1f // Make sure view is visible for transitions
            updateCornerRadii(true)
            animateTransition()
        }
    }

    fun dismiss() {
        prevPosition = currPosition
        if (view.parent != null) {
            animateAppear(false)
        }
    }

    private fun inflateLayout() {
        val layoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        view = layoutInflater.inflate(R.layout.alertslider_dialog, null, false)
        background = view.background as GradientDrawable
        icon = view.findViewById(R.id.icon)
        label = view.findViewById(R.id.label)
    }

    private fun updateLayoutParams(): LayoutParams {
        val lp = LayoutParams().apply {
            copyFrom(layoutParams)
            x = 0
            y = 0
            if (isPortrait) {
                gravity = positionGravity
                horizontalMargin = 0.025f
                verticalMargin = 0f
            } else {
                gravity = Gravity.TOP
                horizontalMargin = 0f
                verticalMargin = 0.025f
            }
        }
        val bounds = windowManager.currentWindowMetrics.bounds
        if (isPortrait) {
            lp.y = alertSliderTopY - (bounds.height() / 2) +
                getStepForPosition(currPosition) + getOffsetForPosition()
        } else {
            lp.x = alertSliderTopY - (bounds.width() / 2) + getStepForPosition(currPosition)
            if (context.display.rotation == Surface.ROTATION_270) {
                lp.x = -lp.x
            }
        }
        return lp
    }

    private fun getStepForPosition(position: AlertSliderPosition): Int {
        return when (position) {
            AlertSliderPosition.Top -> 0
            AlertSliderPosition.Middle -> 1
            AlertSliderPosition.Bottom -> 2
        } * stepSize
    }

    private fun getOffsetForPosition() =
        when (currPosition) {
            AlertSliderPosition.Bottom -> view.measuredHeight / 2
            AlertSliderPosition.Top -> -view.measuredHeight / 2
            else -> 0
        }

    private fun updateCornerRadii(animate: Boolean) {
        var radius = view.measuredHeight / 2f
        if (!isPortrait) {
            background.cornerRadius = radius
            return
        }
        if (!animate) {
            if (currPosition == AlertSliderPosition.Middle) {
                background.cornerRadius = radius
                return
            }
            background.cornerRadii = floatArrayOf(
                radius, radius, // T-L
                if (currPosition == AlertSliderPosition.Bottom) 0f else radius,
                if (currPosition == AlertSliderPosition.Bottom) 0f else radius, // T-R
                if (currPosition == AlertSliderPosition.Top) 0f else radius,
                if (currPosition == AlertSliderPosition.Top) 0f else radius, // B-R
                radius, radius, // B-L
            )
        }
        when (currPosition) {
            AlertSliderPosition.Bottom,
            AlertSliderPosition.Top -> startRadiusAnimator(radius, currPosition, radius, 0f)
            AlertSliderPosition.Middle -> startRadiusAnimator(radius, prevPosition, 0f, radius)
        }
    }

    private fun startRadiusAnimator(radius: Float, position: AlertSliderPosition, vararg values: Float) {
        radiusAnimator = ValueAnimator.ofFloat(*values).apply {
            setDuration(TRANSITION_ANIM_DURATION)
            addUpdateListener {
                val topRightRadius = if (position == AlertSliderPosition.Bottom) it.animatedValue as Float else radius
                val bottomRightRadius = if (position == AlertSliderPosition.Top) it.animatedValue as Float else radius
                background.cornerRadii = floatArrayOf(
                    radius, radius,
                    topRightRadius, topRightRadius,
                    bottomRightRadius, bottomRightRadius,
                    radius, radius,
                )
            }
            addListener(
                onCancel = { radiusAnimator = null },
                onEnd = { radiusAnimator = null },
            )
            start()
        }
    }

    private fun animateAppear(appearing: Boolean) {
        appearAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            setDuration(APPEAR_ANIM_DURATION)
            addUpdateListener {
                view.alpha = it.animatedValue as Float
            }
            addListener(
                onEnd = {
                    appearAnimator = null
                    if (!appearing && view.parent != null) {
                        windowManager.removeViewImmediate(view)
                    }
                },
                onCancel = { appearAnimator = null },
            )
            if (appearing) start()
            else reverse()
        }
    }

    private fun animateTransition() {
        val lp = updateLayoutParams()
        transitionAnimator = if (isPortrait) ValueAnimator.ofInt(layoutParams.y, lp.y)
            else ValueAnimator.ofInt(layoutParams.x, lp.x)
        transitionAnimator!!.let {
            it.setDuration(TRANSITION_ANIM_DURATION)
            it.addUpdateListener { animator ->
                if (isPortrait) {
                    lp.y = animator.animatedValue as Int
                } else {
                    lp.x = animator.animatedValue as Int
                }
                if (view.parent != null) {
                    windowManager.updateViewLayout(view, lp)
                }
            }
            it.addListener(
                onEnd = {
                    transitionAnimator = null
                    layoutParams = lp
                },
                onCancel = { transitionAnimator = null },
            )
            it.start()
        }
    }
}
