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
 * màn hình YouTube và làm hai việc:
 *
 *  1. Thấy nút "Bỏ qua" thì bấm ngay.
 *  2. Trong lúc quảng cáo chạy mà nút đó chưa hiện, tắt tiếng cho đỡ nhức đầu.
 */
class AdSkipperService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private val settings: SkipSettings by lazy { SkipSettings(this) }
    private val muter: AdMuter by lazy { AdMuter(this) }

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

    override fun onInterrupt() {
        releaseMute()
    }

    override fun onDestroy() {
        isRunning = false
        handler.removeCallbacksAndMessages(null)
        // Tuyệt đối không để người dùng ở lại với cái máy câm.
        releaseMute()
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
            releaseMute()
            return
        }

        // Cửa sổ đang hiển thị mới là nguồn tin đáng tin cậy nhất — nhờ vậy app
        // vẫn bắt được quảng cáo khi người dùng bật quyền lúc YouTube đã mở sẵn.
        val root = runCatching { rootInActiveWindow }.getOrNull()
        val pkg = root?.packageName?.toString().orEmpty()
        if (root == null || !isWatched(pkg)) {
            if (idleMisses < IDLE_AFTER_MISSES) idleMisses++
            // Rời YouTube giữa chừng quảng cáo thì phải trả lại tiếng ngay.
            releaseMute()
            return
        }
        idleMisses = 0
        foregroundPackage = pkg

        val found = inspect(root, pkg)

        // Tắt tiếng được xử lý trước, và không bị chặn bởi thời gian nghỉ giữa
        // hai lần bấm — vì đây chính là đoạn nút "Bỏ qua" chưa hiện ra.
        updateMute(found.adVisible)

        if (SystemClock.uptimeMillis() - lastClickAt < settings.clickCooldownMs) return

        val skipNode = found.skipNode
        if (skipNode != null && tryClick(skipNode, SkipLog.KIND_SKIP, found.skipLabel, pkg)) {
            // Bỏ qua xong là video thật chạy ngay — trả tiếng lại luôn, không
            // chờ tới lượt quét sau.
            releaseMute()
            return
        }

        val closeNode = found.closeNode
        if (settings.closeOverlayAds && closeNode != null) {
            tryClick(closeNode, SkipLog.KIND_CLOSE, found.closeLabel, pkg)
        }
    }

    private fun isWatched(pkg: String): Boolean =
        pkg.isNotEmpty() && settings.packages.contains(pkg)

    // ------------------------------------------------------------ tắt tiếng

    private fun updateMute(adVisible: Boolean) {
        if (!settings.muteDuringAds) {
            releaseMute()
            return
        }
        // Chốt chặn cuối: nhận diện có sai thì cũng không câm quá lâu.
        if (muter.isStuck(MAX_MUTE_MS)) {
            Log.w(TAG, "Tắt tiếng quá lâu, tự mở lại")
            releaseMute()
            return
        }
        if (adVisible) {
            if (muter.mute()) {
                isMuted = true
                SkipLog.add(SkipLog.KIND_MUTE, "", foregroundPackage)
            }
        } else {
            releaseMute()
        }
    }

    private fun releaseMute() {
        if (muter.unmute()) SkipLog.add(SkipLog.KIND_UNMUTE, "", foregroundPackage)
        isMuted = false
    }

    // -------------------------------------------------------------- tìm nút

    /** Kết quả của một lượt soi màn hình. */
    private class Inspection {
        var adVisible = false
        var skipNode: AccessibilityNodeInfo? = null
        var skipLabel: String = ""
        var closeNode: AccessibilityNodeInfo? = null
        var closeLabel: String = ""
    }

    /**
     * Duyệt cây giao diện đúng MỘT lần cho cả ba việc: tìm nút "Bỏ qua", tìm
     * nút đóng banner, và nhận biết trình phát có đang chạy quảng cáo hay không.
     *
     * Mỗi node được soi theo hai hướng:
     *  - resource-id (không phụ thuộc ngôn ngữ, ưu tiên hơn)
     *  - nhãn chữ / content-description, khớp chính xác
     */
    private fun inspect(root: AccessibilityNodeInfo, pkg: String): Inspection {
        val result = Inspection()
        val skipLabels = settings.skipLabels.mapTo(HashSet()) { it.lowercase() }
        val closeLabels = if (settings.closeOverlayAds) {
            settings.closeLabels.mapTo(HashSet()) { it.lowercase() }
        } else {
            emptySet()
        }
        val adLabels = if (settings.muteDuringAds) {
            settings.adMarkerLabels.mapTo(HashSet()) { it.lowercase() }
        } else {
            emptySet()
        }

        val queue = ArrayDeque<Pair<AccessibilityNodeInfo, Int>>()
        queue.add(root to 0)
        var visited = 0

        while (queue.isNotEmpty() && visited < MAX_NODES) {
            val (node, depth) = queue.removeFirst()
            visited++

            if (node.isVisibleToUser) {
                val viewId = node.viewIdResourceName?.substringAfterLast('/')?.lowercase()
                val text = normalize(node.text)
                val desc = normalize(node.contentDescription)
                val blocked = isBlocked(text) || isBlocked(desc)

                if (viewId != null) {
                    if (result.skipNode == null && viewId in SKIP_IDS) {
                        result.skipNode = node
                        result.skipLabel = viewId
                        result.adVisible = true
                    }
                    if (result.closeNode == null && settings.closeOverlayAds && viewId in CLOSE_IDS) {
                        result.closeNode = node
                        result.closeLabel = viewId
                    }
                    if (!result.adVisible && settings.muteDuringAds && viewId in AD_MARKER_IDS) {
                        result.adVisible = true
                    }
                }

                if (!blocked) {
                    val label = listOfNotNull(text, desc)
                    if (result.skipNode == null) {
                        val hit = label.firstOrNull { it in skipLabels }
                        if (hit != null) {
                            result.skipNode = node
                            result.skipLabel = hit
                            result.adVisible = true
                        }
                    }
                    // Nút đóng nhận theo nhãn thì bắt buộc phải nằm trong một
                    // view có id liên quan quảng cáo, để không lỡ tay đóng
                    // bảng bình luận.
                    if (result.closeNode == null && closeLabels.isNotEmpty() &&
                        looksLikeAdViewId(node.viewIdResourceName)
                    ) {
                        val hit = label.firstOrNull { it in closeLabels }
                        if (hit != null) {
                            result.closeNode = node
                            result.closeLabel = hit
                        }
                    }
                    // Nhãn "Được tài trợ" chỉ đáng tin khi nó KHÔNG nằm trong
                    // danh sách cuộn được — bảng tin cũng đầy video tài trợ.
                    if (!result.adVisible && adLabels.isNotEmpty() &&
                        label.any { it in adLabels } && !isInsideScrollable(node)
                    ) {
                        result.adVisible = true
                    }
                }
            }

            if (result.skipNode != null && result.adVisible &&
                (result.closeNode != null || closeLabels.isEmpty())
            ) {
                break // đã đủ thông tin, không cần duyệt tiếp
            }

            if (depth >= MAX_DEPTH) continue
            for (i in 0 until node.childCount) {
                val child = runCatching { node.getChild(i) }.getOrNull() ?: continue
                queue.add(child to depth + 1)
            }
        }
        return result
    }

    /**
     * Node có nằm trong một danh sách cuộn được hay không. Overlay của trình
     * phát thì không, còn thẻ video trong bảng tin thì có.
     */
    private fun isInsideScrollable(node: AccessibilityNodeInfo): Boolean {
        var current: AccessibilityNodeInfo? = node
        var depth = 0
        while (depth <= MAX_SCROLL_LOOKUP) {
            val candidate = current ?: return false
            if (candidate.isScrollable) return true
            current = runCatching { candidate.parent }.getOrNull()
            depth++
        }
        return false
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
        val id = viewId?.substringAfterLast('/')?.lowercase() ?: return false
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
        private const val MAX_NODES = 1500
        private const val MAX_DEPTH = 30
        private const val MAX_CLICKABLE_LOOKUP = 5
        private const val MAX_SCROLL_LOOKUP = 8
        private const val MAX_LABEL_LENGTH = 40
        private const val MIN_EVENT_SCAN_GAP_MS = 120L
        private const val TAP_DURATION_MS = 60L
        private const val IDLE_AFTER_MISSES = 5
        private const val IDLE_SCAN_INTERVAL_MS = 3000L

        /** Không bao giờ để máy câm quá lâu, kể cả khi nhận diện sai. */
        private const val MAX_MUTE_MS = 90_000L

        private val SKIP_IDS = SkipTargets.SKIP_VIEW_IDS.toSet()
        private val CLOSE_IDS = SkipTargets.CLOSE_VIEW_IDS.toSet()
        private val AD_MARKER_IDS = SkipTargets.AD_MARKER_VIEW_IDS.toSet()

        private val TRIM_CHARS = charArrayOf(
            '.', ',', ':', ';', '!', '?', '·', '•', '›', '»', '>', '▸', '▶', '→', '…', '-', '–',
        ).toSet()
        private val WHITESPACE = Regex("\\s+")
        private val AD_ID_PATTERN = Regex("(^|_)(ad|ads|advert|advertisement)(_|\$)")

        /** Cho lớp UI biết service có đang sống hay không. */
        @Volatile
        var isRunning: Boolean = false
            private set

        /** Có đang tắt tiếng vì quảng cáo hay không. */
        @Volatile
        var isMuted: Boolean = false
            private set
    }
}
