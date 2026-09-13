/// Bản sao phía Dart của cấu hình được lưu trong SharedPreferences bên Android.
class SkipSettings {
  const SkipSettings({
    this.enabled = true,
    this.closeOverlayAds = true,
    this.muteDuringAds = true,
    this.vibrate = false,
    this.scanIntervalMs = 400,
    this.clickCooldownMs = 800,
    this.skipLabels = const <String>[],
    this.closeLabels = const <String>[],
    this.adMarkerLabels = const <String>[],
    this.packages = const <String>[],
    this.totalSkips = 0,
    this.lastSkipAt,
  });

  /// Bật/tắt việc tự động bấm mà không cần gỡ quyền Trợ năng.
  final bool enabled;

  /// Tự đóng banner quảng cáo dán dưới đáy trình phát.
  final bool closeOverlayAds;

  /// Tắt tiếng trong lúc quảng cáo chạy mà nút "Bỏ qua" chưa hiện ra.
  final bool muteDuringAds;

  /// Rung nhẹ mỗi lần bấm được.
  final bool vibrate;

  /// Chu kỳ quét lại màn hình, tính bằng mili giây.
  final int scanIntervalMs;

  /// Khoảng nghỉ tối thiểu giữa hai lần bấm, tính bằng mili giây.
  final int clickCooldownMs;

  /// Nhãn của nút "Bỏ qua" theo từng ngôn ngữ.
  final List<String> skipLabels;

  /// Nhãn của nút đóng banner quảng cáo.
  final List<String> closeLabels;

  /// Nhãn cho biết trình phát đang chạy quảng cáo ("Được tài trợ", "Sponsored").
  final List<String> adMarkerLabels;

  /// Những ứng dụng sẽ được theo dõi.
  final List<String> packages;

  final int totalSkips;
  final DateTime? lastSkipAt;

  static List<String> _strings(Object? value) =>
      value is List ? value.whereType<String>().toList(growable: false) : const <String>[];

  factory SkipSettings.fromMap(Map<Object?, Object?> map) {
    final int lastSkip = (map['lastSkipAt'] as num?)?.toInt() ?? 0;
    return SkipSettings(
      enabled: map['enabled'] as bool? ?? true,
      closeOverlayAds: map['closeOverlayAds'] as bool? ?? true,
      muteDuringAds: map['muteDuringAds'] as bool? ?? true,
      vibrate: map['vibrate'] as bool? ?? false,
      scanIntervalMs: (map['scanIntervalMs'] as num?)?.toInt() ?? 400,
      clickCooldownMs: (map['clickCooldownMs'] as num?)?.toInt() ?? 800,
      skipLabels: _strings(map['skipLabels']),
      closeLabels: _strings(map['closeLabels']),
      adMarkerLabels: _strings(map['adMarkerLabels']),
      packages: _strings(map['packages']),
      totalSkips: (map['totalSkips'] as num?)?.toInt() ?? 0,
      lastSkipAt: lastSkip > 0 ? DateTime.fromMillisecondsSinceEpoch(lastSkip) : null,
    );
  }

  /// Chỉ gửi sang Android những trường mà người dùng chỉnh được.
  Map<String, Object?> toUpdateMap() => <String, Object?>{
        'enabled': enabled,
        'closeOverlayAds': closeOverlayAds,
        'muteDuringAds': muteDuringAds,
        'vibrate': vibrate,
        'scanIntervalMs': scanIntervalMs,
        'clickCooldownMs': clickCooldownMs,
        'skipLabels': skipLabels,
        'closeLabels': closeLabels,
        'adMarkerLabels': adMarkerLabels,
        'packages': packages,
      };

  SkipSettings copyWith({
    bool? enabled,
    bool? closeOverlayAds,
    bool? muteDuringAds,
    bool? vibrate,
    int? scanIntervalMs,
    int? clickCooldownMs,
    List<String>? skipLabels,
    List<String>? closeLabels,
    List<String>? adMarkerLabels,
    List<String>? packages,
    int? totalSkips,
    DateTime? lastSkipAt,
  }) {
    return SkipSettings(
      enabled: enabled ?? this.enabled,
      closeOverlayAds: closeOverlayAds ?? this.closeOverlayAds,
      muteDuringAds: muteDuringAds ?? this.muteDuringAds,
      vibrate: vibrate ?? this.vibrate,
      scanIntervalMs: scanIntervalMs ?? this.scanIntervalMs,
      clickCooldownMs: clickCooldownMs ?? this.clickCooldownMs,
      skipLabels: skipLabels ?? this.skipLabels,
      closeLabels: closeLabels ?? this.closeLabels,
      adMarkerLabels: adMarkerLabels ?? this.adMarkerLabels,
      packages: packages ?? this.packages,
      totalSkips: totalSkips ?? this.totalSkips,
      lastSkipAt: lastSkipAt ?? this.lastSkipAt,
    );
  }
}
