package com.hung.adskipper

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Trái tim của ứng dụng.
 *
 * Android không cho một app bấm hộ vào app khác, trừ khi người dùng tự tay bật
 * Accessibility Service cho app đó. Service này lắng nghe những thay đổi trên
 * màn hình YouTube, tìm nút "Bỏ qua" rồi bấm giúp người dùng.
 */
class AdSkipperService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private val settings: SkipSettings by lazy { SkipSettings(this) }

    /** Gói ứng dụng đang hiển thị, cập nhật từ sự kiện WINDOW_STATE_CHANGED. */
    private var foregroundPackage: String = ""

    private var lastClickAt = 0L
    private var lastScanAt = 0L
    private var scanScheduled = false

    /** Số lần quét liên tiếp không thấy app nào đang được theo dõi. */
    private var idleMisses = 0

    private val scanRunnable = Runnable {
        scanScheduled = false
        scanNow()
        scheduleNextScan()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        isRunning = true
        SkipLog.add(SkipLog.KIND_SERVICE_ON, "", "")
        Log.i(TAG, "AdSkipperService đã kết nối")
        scheduleNextScan()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val pkg = event.packageName?.toString().orEmpty()

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && pkg.isNotEmpty()) {
            foregroundPackage = pkg
        }

        if (!settings.enabled) return
        if (pkg.isEmpty() || !isWatched(pkg)) return
        foregroundPackage = pkg
        idleMisses = 0

        // Trong lúc phát video, YouTube bắn ra rất nhiều sự kiện. Gom lại để
        // không quét cây giao diện liên tục.
        val now = SystemClock.uptimeMillis()
        if (now - lastScanAt < MIN_EVENT_SCAN_GAP_MS) return
        scanNow()
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        isRunning = false
        handler.removeCallbacksAndMessages(null)
        SkipLog.add(SkipLog.KIND_SERVICE_OFF, "", "")
        super.onDestroy()
    }

    // ---------------------------------------------------------------- quét

    /**
     * Khi không có app nào được theo dõi ở tiền cảnh, giãn chu kỳ quét ra cho
     * đỡ tốn pin; thấy YouTube trở lại thì lập tức quét dày trở lại.
     */
    private fun scheduleNextScan() {
        if (scanScheduled) return
        scanScheduled = true
        val delay = if (idleMisses >= IDLE_AFTER_MISSES) {
            IDLE_SCAN_INTERVAL_MS
        } else {
            settings.scanIntervalMs.toLong()
        }
        handler.postDelayed(scanRunnable, delay)
    }

    private fun scanNow() {
        lastScanAt = SystemClock.uptimeMillis()
        if (!settings.enabled) {
            idleMisses = IDLE_AFTER_MISSES
            return
        }

        // Cửa sổ đang hiển thị mới là nguồn tin đáng tin cậy nhất — nhờ vậy app
        // vẫn bắt được quảng cáo khi người dùng bật quyền lúc YouTube đã mở sẵn.
        val root = runCatching { rootInActiveWindow }.getOrNull()
        val pkg = root?.packageName?.toString().orEmpty()
        if (root == null || !isWatched(pkg)) {
            if (idleMisses < IDLE_AFTER_MISSES) idleMisses++
            return
        }
        idleMisses = 0
        foregroundPackage = pkg

        if (SystemClock.uptimeMillis() - lastClickAt < settings.clickCooldownMs) return

        // 1. Ưu tiên resource-id: chính xác nhất, không phụ thuộc ngôn ngữ.
        for (id in SkipTargets.SKIP_VIEW_IDS) {
            val node = findByViewId(root, "$pkg:id/$id") ?: continue
            if (tryClick(node, SkipLog.KIND_SKIP, id, pkg)) return
        }

        // 2. Rồi mới tới so khớp nhãn "Bỏ qua" / "Skip Ad" / ...
        val skipLabels = settings.skipLabels.map { it.lowercase() }.toSet()
        val matched = findByLabels(root, skipLabels)
        if (matched != null && tryClick(matched.first, SkipLog.KIND_SKIP, matched.second, pkg)) return

        // 3. Cuối cùng là nút X của banner quảng cáo dán dưới trình phát.
        if (settings.closeOverlayAds) {
            for (id in SkipTargets.CLOSE_VIEW_IDS) {
                val node = findByViewId(root, "$pkg:id/$id") ?: continue
                if (tryClick(node, SkipLog.KIND_CLOSE, id, pkg)) return
            }
            // Nút đóng nhận theo nhãn thì bắt buộc phải nằm trong một view có
            // id liên quan tới quảng cáo, để không lỡ tay đóng bảng bình luận.
            val closeLabels = settings.closeLabels.map { it.lowercase() }.toSet()
            val closeNode = findByLabels(root, closeLabels, requireAdViewId = true)
            if (closeNode != null) {
                tryClick(closeNode.first, SkipLog.KIND_CLOSE, closeNode.second, pkg)
            }
        }
    }

    private fun isWatched(pkg: String): Boolean =
        pkg.isNotEmpty() && settings.packages.contains(pkg)

    // -------------------------------------------------------------- tìm nút

    private fun findByViewId(root: AccessibilityNodeInfo, viewId: String): AccessibilityNodeInfo? {
        val found = runCatching { root.findAccessibilityNodeInfosByViewId(viewId) }
            .getOrNull()
            .orEmpty()
        return found.firstOrNull { it.isVisibleToUser }
    }

    /**
     * Duyệt cây giao diện tìm node có text hoặc content-description trùng khớp
     * một nhãn đã khai báo. Trả về node kèm nhãn đã khớp.
     */
    private fun findByLabels(
        root: AccessibilityNodeInfo,
        labels: Set<String>,
        requireAdViewId: Boolean = false,
    ): Pair<AccessibilityNodeInfo, String>? {
        if (labels.isEmpty()) return null
        val queue = ArrayDeque<Pair<AccessibilityNodeInfo, Int>>()
        queue.add(root to 0)
        var visited = 0

        while (queue.isNotEmpty() && visited < MAX_NODES) {
            val (node, depth) = queue.removeFirst()
            visited++

            if (node.isVisibleToUser) {
                val text = normalize(node.text)
                val desc = normalize(node.contentDescription)
                val hit = listOfNotNull(text, desc).firstOrNull { it in labels }
                val idAllows = !requireAdViewId || looksLikeAdViewId(node.viewIdResourceName)
                if (hit != null && idAllows && !isBlocked(text) && !isBlocked(desc)) {
                    return node to hit
                }
            }

            if (depth >= MAX_DEPTH) continue
            for (i in 0 until node.childCount) {
                val child = runCatching { node.getChild(i) }.getOrNull() ?: continue
                queue.add(child to depth + 1)
            }
        }
        return null
    }

    /**
     * Chuẩn hoá chuỗi trước khi so khớp: bỏ khoảng trắng thừa, đưa về chữ
     * thường và cắt các ký tự trang trí mà YouTube hay gắn kèm ("Bỏ qua ▸").
     */
    private fun normalize(value: CharSequence?): String? {
        val raw = value?.toString()?.trim() ?: return null
        if (raw.isEmpty() || raw.length > MAX_LABEL_LENGTH) return null
        val cleaned = raw
            .lowercase()
            .trim { it.isWhitespace() || it in TRIM_CHARS }
            .replace(WHITESPACE, " ")
        return cleaned.ifEmpty { null }
    }

    /**
     * Id của view có thuộc về khối quảng cáo hay không, ví dụ
     * "com.google.android.youtube:id/ad_close_button" -> true, còn
     * "…:id/thread_header" -> false (không nhầm "ad" nằm giữa một từ khác).
     */
    private fun looksLikeAdViewId(viewId: String?): Boolean {
        val id = viewId?.substringAfterLast("/")?.lowercase() ?: return false
        return AD_ID_PATTERN.containsMatchIn(id)
    }

    /** Chặn tuyệt đối các nút dẫn sang trang quảng cáo / cửa hàng ứng dụng. */
    private fun isBlocked(value: String?): Boolean {
        val text = value ?: return false
        return SkipTargets.BLOCKED_LABELS.any { text == it || text.startsWith("$it ") }
    }

    // -------------------------------------------------------------- bấm nút

    private fun tryClick(
        node: AccessibilityNodeInfo,
        kind: String,
        label: String,
        pkg: String,
    ): Boolean {
        if (!node.isVisibleToUser) return false
        if (SystemClock.uptimeMillis() - lastClickAt < settings.clickCooldownMs) return false

        val clicked = performClick(node)
        if (!clicked) return false

        lastClickAt = SystemClock.uptimeMillis()
        settings.recordSkip(System.currentTimeMillis())
        SkipLog.add(kind, label, pkg)
        if (settings.vibrate) vibrateOnce()
        Log.i(TAG, "Đã bấm '$label' trong $pkg")
        return true
    }

    /**
     * YouTube thường đặt chữ "Bỏ qua" trong một TextView không click được, nút
     * thật là view cha. Vì vậy: leo dần lên cha để tìm view click được, nếu
     * vẫn không có thì chạm trực tiếp vào toạ độ của nút.
     */
    private fun performClick(node: AccessibilityNodeInfo): Boolean {
        var current: AccessibilityNodeInfo? = node
        var depth = 0
        while (depth <= MAX_CLICKABLE_LOOKUP) {
            val candidate = current ?: break
            if (candidate.isClickable && candidate.isEnabled &&
                candidate.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            ) {
                return true
            }
            current = runCatching { candidate.parent }.getOrNull()
            depth++
        }
        return tapOnScreen(node)
    }

    private fun tapOnScreen(node: AccessibilityNodeInfo): Boolean {
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        if (bounds.width() <= 0 || bounds.height() <= 0) return false

        val path = Path().apply { moveTo(bounds.exactCenterX(), bounds.exactCenterY()) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0L, TAP_DURATION_MS))
            .build()
        return runCatching { dispatchGesture(gesture, null, null) }.getOrDefault(false)
    }

    private fun vibrateOnce() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        } ?: return
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(
                    VibrationEffect.createOneShot(30L, VibrationEffect.DEFAULT_AMPLITUDE),
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(30L)
            }
        }
    }

    companion object {
        private const val TAG = "AdSkipper"
        private const val MAX_NODES = 900
        private const val MAX_DEPTH = 28
        private const val MAX_CLICKABLE_LOOKUP = 5
        private const val MAX_LABEL_LENGTH = 40
        private const val MIN_EVENT_SCAN_GAP_MS = 120L
        private const val TAP_DURATION_MS = 60L
        private const val IDLE_AFTER_MISSES = 5
        private const val IDLE_SCAN_INTERVAL_MS = 3000L

        private val TRIM_CHARS = charArrayOf(
            '.', ',', ':', ';', '!', '?', '·', '•', '›', '»', '>', '▸', '▶', '→', '…', '-', '–',
        ).toSet()
        private val WHITESPACE = Regex("\\s+")
        private val AD_ID_PATTERN = Regex("(^|_)(ad|ads|advert|advertisement)(_|\$)")

        /** Cho lớp UI biết service có đang sống hay không. */
        @Volatile
        var isRunning: Boolean = false
            private set
    }
}
