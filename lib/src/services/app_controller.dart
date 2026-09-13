import 'dart:async';

import 'package:flutter/foundation.dart';

import '../models/skip_event.dart';
import '../models/skip_settings.dart';
import 'ad_skipper_channel.dart';

/// Giữ toàn bộ trạng thái của app và đồng bộ hai chiều với phần Android.
class AppController extends ChangeNotifier {
  AppController({AdSkipperChannel channel = const AdSkipperChannel()}) : _channel = channel;

  final AdSkipperChannel _channel;
  StreamSubscription<SkipEvent>? _eventSubscription;

  SkipSettings _settings = const SkipSettings();
  List<SkipEvent> _log = const <SkipEvent>[];
  bool _accessibilityEnabled = false;
  bool _serviceRunning = false;
  bool _muted = false;
  bool _loading = true;
  bool _permissionRequested = false;

  SkipSettings get settings => _settings;
  List<SkipEvent> get log => _log;
  bool get accessibilityEnabled => _accessibilityEnabled;
  bool get serviceRunning => _serviceRunning;

  /// Đang tắt tiếng vì quảng cáo (để hiện báo cho người dùng biết vì sao mất tiếng).
  bool get muted => _muted;
  bool get loading => _loading;

  /// Đang thực sự làm việc: đã cấp quyền và công tắc trong app đang bật.
  bool get active => _accessibilityEnabled && _settings.enabled;

  /// Người dùng đã mở màn hình Trợ năng nhưng quay lại vẫn chưa bật được.
  /// Trên Android 13+ gần như chắc chắn là do "Cài đặt hạn chế" chặn, nên hiện
  /// hướng dẫn mở khoá thay vì để họ bấm tới bấm lui.
  bool get likelyBlockedByRestrictedSettings =>
      _permissionRequested && !_accessibilityEnabled;

  Future<void> start() async {
    await refresh();
    _eventSubscription ??= _channel.watchEvents().listen(_onEvent);
  }

  /// Gọi lại mỗi khi app quay về foreground, vì người dùng có thể vừa bật/tắt
  /// quyền Trợ năng ở màn hình Cài đặt của hệ thống.
  Future<void> refresh() async {
    final SkipSettings settings = await _channel.getSettings();
    final bool enabled = await _channel.isAccessibilityEnabled();
    final bool running = await _channel.isServiceRunning();
    final bool muted = await _channel.isMuted();
    final List<SkipEvent> log = await _channel.getLog();

    _settings = settings;
    if (enabled) _permissionRequested = false;
    _accessibilityEnabled = enabled;
    _serviceRunning = running;
    _muted = muted;
    _log = log;
    _loading = false;
    notifyListeners();
  }

  void _onEvent(SkipEvent event) {
    _log = <SkipEvent>[event, ..._log].take(120).toList(growable: false);
    if (event.kind == SkipEventKind.serviceOn) _serviceRunning = true;
    if (event.kind == SkipEventKind.serviceOff) {
      _serviceRunning = false;
      _muted = false;
    }
    if (event.kind == SkipEventKind.mute) _muted = true;
    if (event.kind == SkipEventKind.unmute) _muted = false;
    if (event.isClick) {
      _settings = _settings.copyWith(
        totalSkips: _settings.totalSkips + 1,
        lastSkipAt: event.time,
      );
    }
    notifyListeners();
  }

  Future<void> update(SkipSettings settings) async {
    _settings = settings; // cập nhật lạc quan để giao diện phản hồi tức thì
    notifyListeners();
    _settings = await _channel.updateSettings(settings);
    notifyListeners();
  }

  Future<void> setEnabled(bool value) => update(_settings.copyWith(enabled: value));

  Future<void> restoreDefaults() async {
    _settings = await _channel.restoreDefaults();
    notifyListeners();
  }

  Future<void> resetStats() async {
    _settings = await _channel.resetStats();
    _log = const <SkipEvent>[];
    notifyListeners();
  }

  Future<void> openAccessibilitySettings() {
    _permissionRequested = true;
    return _channel.openAccessibilitySettings();
  }

  Future<void> openBatterySettings() => _channel.openBatterySettings();

  Future<void> openAppInfo() => _channel.openAppInfo();

  @override
  void dispose() {
    _eventSubscription?.cancel();
    _eventSubscription = null;
    super.dispose();
  }
}
