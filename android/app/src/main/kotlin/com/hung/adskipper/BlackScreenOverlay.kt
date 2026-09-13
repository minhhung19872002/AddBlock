package com.hung.adskipper

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView

/**
 * "Tắt màn hình" giả để vẫn nghe được YouTube.
 *
 * YouTube bản thường tự dừng phát ngay khi màn hình tắt thật, và app ngoài không
 * chặn được việc đó. Cách làm ở đây: phủ một lớp đen kín màn hình (lớp phủ trợ
 * năng, không cần quyền "hiển thị trên ứng dụng khác") và hạ độ sáng xuống thấp
 * nhất. Trên màn AMOLED điểm ảnh đen gần như tắt hẳn, còn YouTube vẫn nghĩ mình
 * đang hiển thị nên phát tiếp — và service vẫn bỏ qua quảng cáo bên dưới.
 *
 * Lớp phủ không nhận focus nên phím âm lượng vẫn đi xuống YouTube. Chạm hai lần
 * để thoát.
 */
class BlackScreenOverlay(private val service: AccessibilityService) {

    private val handler = Handler(Looper.getMainLooper())
    private val windowManager =
        service.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var root: FrameLayout? = null

    val isShowing: Boolean get() = root != null

    fun show() {
        if (root != null) return

        val hint = TextView(service).apply {
            text = "Màn hình đang tắt — YouTube vẫn phát\nChạm 2 lần để mở lại"
            setTextColor(Color.argb(140, 255, 255, 255))
            textSize = 15f
            gravity = Gravity.CENTER
        }
        val layout = FrameLayout(service).apply {
            setBackgroundColor(Color.BLACK)
            addView(
                hint,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER,
                ),
            )
        }

        val detector = GestureDetector(
            service,
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onDown(e: MotionEvent): Boolean = true

                override fun onDoubleTap(e: MotionEvent): Boolean {
                    hide()
                    return true
                }

                override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                    // Chạm một lần thì hiện lại dòng hướng dẫn một lúc.
                    flashHint(hint)
                    return true
                }
            },
        )
        // Nuốt mọi thao tác chạm để không lỡ tay bấm vào YouTube bên dưới.
        layout.setOnTouchListener { _, event -> detector.onTouchEvent(event) }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            PixelFormat.OPAQUE,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            screenBrightness = MIN_BRIGHTNESS
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        val added = runCatching { windowManager.addView(layout, params) }
        if (added.isFailure) {
            Log.w(TAG, "Không phủ được màn hình đen", added.exceptionOrNull())
            return
        }
        root = layout
        isActive = true
        flashHint(hint)
        Log.i(TAG, "Bật màn hình đen")
    }

    fun hide() {
        val view = root ?: return
        handler.removeCallbacksAndMessages(null)
        runCatching { windowManager.removeView(view) }
        root = null
        isActive = false
        Log.i(TAG, "Tắt màn hình đen")
    }

    fun toggle() = if (isShowing) hide() else show()

    /** Dòng chữ chỉ hiện vài giây rồi tắt hẳn, để màn hình đen tuyền. */
    private fun flashHint(hint: View) {
        handler.removeCallbacksAndMessages(null)
        hint.visibility = View.VISIBLE
        handler.postDelayed({ hint.visibility = View.INVISIBLE }, HINT_VISIBLE_MS)
    }

    companion object {
        private const val TAG = "AdSkipper"
        private const val HINT_VISIBLE_MS = 3000L

        /** 0 bị một số máy hiểu là "tắt đèn nền"; để một mức thấp nhất khác 0. */
        private const val MIN_BRIGHTNESS = 0.01f

        /** Cho nút trong thanh cài đặt nhanh biết đang bật hay tắt. */
        @Volatile
        var isActive: Boolean = false
            private set
    }
}
