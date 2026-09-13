package com.hung.adskipper

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray

/**
 * Nơi lưu toàn bộ tuỳ chọn. Dùng SharedPreferences thuần để cả tiến trình
 * Flutter lẫn AccessibilityService cùng đọc được một nguồn dữ liệu duy nhất.
 */
class SkipSettings(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var enabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_ENABLED, value).apply()

    /** Tự bấm nút X để đóng banner quảng cáo dán dưới đáy trình phát. */
    var closeOverlayAds: Boolean
        get() = prefs.getBoolean(KEY_CLOSE_OVERLAY, true)
        set(value) = prefs.edit().putBoolean(KEY_CLOSE_OVERLAY, value).apply()

    /**
     * Tắt tiếng trong lúc quảng cáo chạy mà nút "Bỏ qua" chưa hiện ra.
     * Không chặn được quảng cáo thì ít nhất cũng không phải nghe nó.
     */
    var muteDuringAds: Boolean
        get() = prefs.getBoolean(KEY_MUTE_DURING_ADS, true)
        set(value) = prefs.edit().putBoolean(KEY_MUTE_DURING_ADS, value).apply()

    /** Rung nhẹ mỗi lần bấm được, để biết app có đang chạy hay không. */
    var vibrate: Boolean
        get() = prefs.getBoolean(KEY_VIBRATE, false)
        set(value) = prefs.edit().putBoolean(KEY_VIBRATE, value).apply()

    /** Chu kỳ quét lại màn hình (ms). Nhỏ = nhạy hơn nhưng tốn pin hơn. */
    var scanIntervalMs: Int
        get() = prefs.getInt(KEY_SCAN_INTERVAL, DEFAULT_SCAN_INTERVAL).coerceIn(150, 3000)
        set(value) = prefs.edit().putInt(KEY_SCAN_INTERVAL, value.coerceIn(150, 3000)).apply()

    /** Khoảng nghỉ tối thiểu giữa hai lần bấm (ms), tránh bấm chồng lên nhau. */
    var clickCooldownMs: Int
        get() = prefs.getInt(KEY_CLICK_COOLDOWN, DEFAULT_CLICK_COOLDOWN).coerceIn(200, 5000)
        set(value) = prefs.edit().putInt(KEY_CLICK_COOLDOWN, value.coerceIn(200, 5000)).apply()

    var skipLabels: List<String>
        get() = readList(KEY_SKIP_LABELS, SkipTargets.DEFAULT_SKIP_LABELS)
        set(value) = writeList(KEY_SKIP_LABELS, value)

    var closeLabels: List<String>
        get() = readList(KEY_CLOSE_LABELS, SkipTargets.DEFAULT_CLOSE_LABELS)
        set(value) = writeList(KEY_CLOSE_LABELS, value)

    var adMarkerLabels: List<String>
        get() = readList(KEY_AD_MARKER_LABELS, SkipTargets.DEFAULT_AD_MARKER_LABELS)
        set(value) = writeList(KEY_AD_MARKER_LABELS, value)

    var packages: List<String>
        get() = readList(KEY_PACKAGES, SkipTargets.DEFAULT_PACKAGES)
        set(value) = writeList(KEY_PACKAGES, value)

    var totalSkips: Int
        get() = prefs.getInt(KEY_TOTAL_SKIPS, 0)
        set(value) = prefs.edit().putInt(KEY_TOTAL_SKIPS, value).apply()

    var lastSkipAt: Long
        get() = prefs.getLong(KEY_LAST_SKIP_AT, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_SKIP_AT, value).apply()

    fun recordSkip(at: Long) {
        prefs.edit()
            .putInt(KEY_TOTAL_SKIPS, totalSkips + 1)
            .putLong(KEY_LAST_SKIP_AT, at)
            .apply()
    }

    fun resetStats() {
        prefs.edit().putInt(KEY_TOTAL_SKIPS, 0).putLong(KEY_LAST_SKIP_AT, 0L).apply()
    }

    fun restoreDefaults() {
        prefs.edit()
            .remove(KEY_SKIP_LABELS)
            .remove(KEY_CLOSE_LABELS)
            .remove(KEY_AD_MARKER_LABELS)
            .remove(KEY_MUTE_DURING_ADS)
            .remove(KEY_PACKAGES)
            .remove(KEY_SCAN_INTERVAL)
            .remove(KEY_CLICK_COOLDOWN)
            .remove(KEY_CLOSE_OVERLAY)
            .remove(KEY_VIBRATE)
            .apply()
    }

    fun toMap(): Map<String, Any?> = mapOf(
        "enabled" to enabled,
        "closeOverlayAds" to closeOverlayAds,
        "muteDuringAds" to muteDuringAds,
        "vibrate" to vibrate,
        "scanIntervalMs" to scanIntervalMs,
        "clickCooldownMs" to clickCooldownMs,
        "skipLabels" to skipLabels,
        "closeLabels" to closeLabels,
        "adMarkerLabels" to adMarkerLabels,
        "packages" to packages,
        "totalSkips" to totalSkips,
        "lastSkipAt" to lastSkipAt,
    )

    @Suppress("UNCHECKED_CAST")
    fun applyMap(map: Map<*, *>) {
        (map["enabled"] as? Boolean)?.let { enabled = it }
        (map["closeOverlayAds"] as? Boolean)?.let { closeOverlayAds = it }
        (map["muteDuringAds"] as? Boolean)?.let { muteDuringAds = it }
        (map["vibrate"] as? Boolean)?.let { vibrate = it }
        (map["scanIntervalMs"] as? Number)?.let { scanIntervalMs = it.toInt() }
        (map["clickCooldownMs"] as? Number)?.let { clickCooldownMs = it.toInt() }
        (map["skipLabels"] as? List<*>)?.let { skipLabels = it.filterIsInstance<String>() }
        (map["closeLabels"] as? List<*>)?.let { closeLabels = it.filterIsInstance<String>() }
        (map["adMarkerLabels"] as? List<*>)?.let { adMarkerLabels = it.filterIsInstance<String>() }
        (map["packages"] as? List<*>)?.let { packages = it.filterIsInstance<String>() }
    }

    private fun readList(key: String, fallback: List<String>): List<String> {
        val raw = prefs.getString(key, null) ?: return fallback
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length())
                .map { array.optString(it).trim() }
                .filter { it.isNotEmpty() }
        }.getOrDefault(fallback)
    }

    private fun writeList(key: String, values: List<String>) {
        val cleaned = values.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        prefs.edit().putString(key, JSONArray(cleaned).toString()).apply()
    }

    companion object {
        const val PREFS_NAME = "ad_skipper_prefs"
        const val DEFAULT_SCAN_INTERVAL = 400
        const val DEFAULT_CLICK_COOLDOWN = 800

        private const val KEY_ENABLED = "enabled"
        private const val KEY_CLOSE_OVERLAY = "close_overlay_ads"
        private const val KEY_VIBRATE = "vibrate"
        private const val KEY_SCAN_INTERVAL = "scan_interval_ms"
        private const val KEY_CLICK_COOLDOWN = "click_cooldown_ms"
        private const val KEY_SKIP_LABELS = "skip_labels"
        private const val KEY_CLOSE_LABELS = "close_labels"
        private const val KEY_AD_MARKER_LABELS = "ad_marker_labels"
        private const val KEY_MUTE_DURING_ADS = "mute_during_ads"
        private const val KEY_PACKAGES = "packages"
        private const val KEY_TOTAL_SKIPS = "total_skips"
        private const val KEY_LAST_SKIP_AT = "last_skip_at"
    }
}
