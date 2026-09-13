package com.hung.adskipper

import android.content.ComponentName
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.TextUtils
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodChannel

/**
 * Cầu nối giữa giao diện Flutter và AccessibilityService bên dưới.
 */
class MainActivity : FlutterActivity() {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var eventSink: EventChannel.EventSink? = null
    private var logListener: ((SkipLog.Entry) -> Unit)? = null

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        val settings = SkipSettings(this)

        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, METHOD_CHANNEL)
            .setMethodCallHandler { call, result ->
                when (call.method) {
                    "isAccessibilityEnabled" -> result.success(isAccessibilityEnabled())
                    "isServiceRunning" -> result.success(AdSkipperService.isRunning)
                    "openAccessibilitySettings" -> {
                        openAccessibilitySettings()
                        result.success(true)
                    }
                    "openBatterySettings" -> {
                        openBatterySettings()
                        result.success(true)
                    }
                    "getSettings" -> result.success(settings.toMap())
                    "updateSettings" -> {
                        (call.arguments as? Map<*, *>)?.let { settings.applyMap(it) }
                        result.success(settings.toMap())
                    }
                    "restoreDefaults" -> {
                        settings.restoreDefaults()
                        result.success(settings.toMap())
                    }
                    "resetStats" -> {
                        settings.resetStats()
                        SkipLog.clear()
                        result.success(settings.toMap())
                    }
                    "getLog" -> result.success(SkipLog.snapshot())
                    else -> result.notImplemented()
                }
            }

        EventChannel(flutterEngine.dartExecutor.binaryMessenger, EVENT_CHANNEL)
            .setStreamHandler(object : EventChannel.StreamHandler {
                override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
                    eventSink = events
                    val listener: (SkipLog.Entry) -> Unit = { entry ->
                        mainHandler.post { eventSink?.success(entry.toMap()) }
                    }
                    logListener = listener
                    SkipLog.addListener(listener)
                }

                override fun onCancel(arguments: Any?) {
                    logListener?.let { SkipLog.removeListener(it) }
                    logListener = null
                    eventSink = null
                }
            })
    }

    override fun onDestroy() {
        logListener?.let { SkipLog.removeListener(it) }
        logListener = null
        eventSink = null
        super.onDestroy()
    }

    /**
     * Đọc danh sách accessibility service đang bật của hệ thống để biết người
     * dùng đã cấp quyền cho app hay chưa.
     */
    private fun isAccessibilityEnabled(): Boolean {
        val expected = ComponentName(this, AdSkipperService::class.java).flattenToString()
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return false

        val splitter = TextUtils.SimpleStringSplitter(':').apply { setString(enabledServices) }
        for (service in splitter) {
            if (service.equals(expected, ignoreCase = true)) return true
            // Một số ROM lưu dạng rút gọn "package/.ServiceName".
            if (service.equals("$packageName/.${AdSkipperService::class.java.simpleName}", true)) {
                return true
            }
        }
        return false
    }

    private fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { startActivity(intent) }
    }

    /** Mở phần tối ưu hoá pin để người dùng cho phép app chạy nền. */
    private fun openBatterySettings() {
        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (runCatching { startActivity(intent) }.isFailure) {
            runCatching {
                startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        .setData(android.net.Uri.parse("package:$packageName"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }
        }
    }

    companion object {
        private const val METHOD_CHANNEL = "com.hung.adskipper/control"
        private const val EVENT_CHANNEL = "com.hung.adskipper/events"
    }
}
