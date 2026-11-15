package com.rynekryz.celestite.service

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.roundToInt

internal fun Context.windowManager(): WindowManager =
    getSystemService(WindowManager::class.java)

internal fun canShowOverlay(context: Context): Boolean =
    Settings.canDrawOverlays(context)

internal fun overlayFlags(): Int =
    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS

internal class CountdownOverlay(private val context: Context) {
    private var attached: View? = null
    private var text: TextView? = null

    fun show() {
        if (text != null) return
        val tv = TextView(context).apply {
            setTextColor(Color.WHITE)
            textSize = 140f
            gravity = Gravity.CENTER
            setShadowLayer(20f, 0f, 4f, Color.argb(120, 0, 0, 0))
        }
        val root = FrameLayout(context).apply {
            setBackgroundColor(Color.argb(70, 0, 0, 0))
            addView(
                tv,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER,
                ),
            )
        }
        text = tv
        attached = root
        context.windowManager().addView(
            root,
            WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                overlayFlags() or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT,
            ),
        )
    }

    fun update(value: String) {
        text?.text = value
    }

    fun hide() {
        attached?.let { runCatching { context.windowManager().removeView(it) } }
        attached = null
        text = null
    }
}

@SuppressLint("ClickableViewAccessibility")
internal class FloatingControls(
    private val context: Context,
    private val onPause: () -> Unit,
    private val onResume: () -> Unit,
    private val onStop: () -> Unit,
) {
    private var attached: LinearLayout? = null
    private var toggleIcon: ImageView? = null
    private var paused = false
    private var supportsPause = true

    fun show(withPause: Boolean) {
        if (attached != null) return
        supportsPause = withPause
        val density = context.resources.displayMetrics.density
        val iconSize = (44 * density).toInt()
        val padding = (10 * density).toInt()

        fun actionIcon(res: Int): ImageView = ImageView(context).apply {
            setImageResource(res)
            setColorFilter(Color.WHITE)
            setPadding(padding, padding, padding, padding)
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }

        val toggle = actionIcon(R.drawable.ic_pause).apply {
            toggleIcon = this
            setOnClickListener {
                if (paused) {
                    paused = false
                    toggleIcon?.setImageResource(R.drawable.ic_pause)
                    onResume()
                } else {
                    paused = true
                    toggleIcon?.setImageResource(R.drawable.ic_play)
                    onPause()
                }
            }
        }
        val stop = actionIcon(R.drawable.ic_stop).apply {
            setOnClickListener { onStop() }
        }

        val bar = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundResource(R.drawable.floating_pill)
            addView(toggle, LinearLayout.LayoutParams(iconSize, iconSize))
            addView(stop, LinearLayout.LayoutParams(iconSize, iconSize))
            elevation = 6 * density
        }
        toggle.visibility = if (withPause) View.VISIBLE else View.GONE

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            overlayFlags(),
            PixelFormat.TRANSLUCENT,
        ).apply {
            flags = flags or WindowManager.LayoutParams.FLAG_SECURE
            gravity = Gravity.TOP or Gravity.START
            x = context.resources.displayMetrics.widthPixels - (iconSize * 2 + 32 * density).roundToInt()
            y = (100 * density).toInt()
        }

        var downRawX = 0f
        var downRawY = 0f
        var startX = 0
        var startY = 0
        var dragging = false
        bar.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    downRawX = event.rawX
                    downRawY = event.rawY
                    startX = params.x
                    startY = params.y
                    dragging = false
                    false
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downRawX
                    val dy = event.rawY - downRawY
                    if (dragging || dx * dx + dy * dy > 40 * density * density) {
                        dragging = true
                        params.x = startX + dx.roundToInt()
                        params.y = startY + dy.roundToInt()
                        context.windowManager().updateViewLayout(v, params)
                    }
                    dragging
                }
                else -> dragging
            }
        }

        attached = bar
        context.windowManager().addView(bar, params)
    }

    fun setPaused(value: Boolean) {
        paused = value
        toggleIcon?.setImageResource(if (paused) R.drawable.ic_play else R.drawable.ic_pause)
    }

    fun hide() {
        attached?.let { runCatching { context.windowManager().removeView(it) } }
        attached = null
        toggleIcon = null
        paused = false
    }
}
