import 'dart:async';

import 'package:flutter/services.dart';

import '../models/skip_event.dart';
import '../models/skip_settings.dart';

/// Lớp bọc quanh MethodChannel/EventChannel nối với phần Android.
///
/// Mọi lời gọi đều được bọc try/catch: khi chạy trên máy khác Android (hoặc
/// trong unit test) sẽ trả về giá trị mặc định thay vì ném lỗi.
class AdSkipperChannel {
  const AdSkipperChannel();

  static const MethodChannel _method = MethodChannel('com.hung.adskipper/control');
  static const EventChannel _events = EventChannel('com.hung.adskipper/events');

  /// Người dùng đã bật dịch vụ trong Cài đặt > Trợ năng hay chưa.
  Future<bool> isAccessibilityEnabled() async =>
      await _invoke<bool>('isAccessibilityEnabled') ?? false;

  /// Dịch vụ có đang thực sự chạy trong bộ nhớ hay không.
  Future<bool> isServiceRunning() async => await _invoke<bool>('isServiceRunning') ?? false;

  Future<void> openAccessibilitySettings() => _invoke<bool>('openAccessibilitySettings');

  Future<void> openBatterySettings() => _invoke<bool>('openBatterySettings');

  Future<SkipSettings> getSettings() => _settingsCall('getSettings');

  Future<SkipSettings> updateSettings(SkipSettings settings) =>
      _settingsCall('updateSettings', settings.toUpdateMap());

  Future<SkipSettings> restoreDefaults() => _settingsCall('restoreDefaults');

  Future<SkipSettings> resetStats() => _settingsCall('resetStats');

  Future<List<SkipEvent>> getLog() async {
    final List<Object?>? raw = await _invoke<List<Object?>>('getLog');
    if (raw == null) return const <SkipEvent>[];
    return raw
        .whereType<Map<Object?, Object?>>()
        .map(SkipEvent.fromMap)
        .toList(growable: false);
  }

  /// Luồng sự kiện realtime do AccessibilityService bắn lên.
  Stream<SkipEvent> watchEvents() {
    return _events
        .receiveBroadcastStream()
        .where((Object? event) => event is Map)
        .map((Object? event) => SkipEvent.fromMap(event! as Map<Object?, Object?>))
        .handleError((Object _) {});
  }

  Future<SkipSettings> _settingsCall(String method, [Object? arguments]) async {
    final Map<Object?, Object?>? map =
        await _invoke<Map<Object?, Object?>>(method, arguments);
    return SkipSettings.fromMap(map ?? const <Object?, Object?>{});
  }

  Future<T?> _invoke<T>(String method, [Object? arguments]) async {
    try {
      return await _method.invokeMethod<T>(method, arguments);
    } on MissingPluginException {
      return null; // Không chạy trên Android (ví dụ: trong test).
    } on PlatformException {
      return null;
    }
  }
}
