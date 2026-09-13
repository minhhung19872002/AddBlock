/// Một dòng nhật ký do AccessibilityService gửi lên.
enum SkipEventKind { skip, close, serviceOn, serviceOff, unknown }

class SkipEvent {
  const SkipEvent({
    required this.time,
    required this.kind,
    required this.label,
    required this.packageName,
  });

  final DateTime time;
  final SkipEventKind kind;

  /// Nhãn hoặc resource-id của nút đã bấm.
  final String label;
  final String packageName;

  factory SkipEvent.fromMap(Map<Object?, Object?> map) {
    return SkipEvent(
      time: DateTime.fromMillisecondsSinceEpoch(
        (map['time'] as num?)?.toInt() ?? DateTime.now().millisecondsSinceEpoch,
      ),
      kind: _kindFrom(map['kind'] as String?),
      label: map['label'] as String? ?? '',
      packageName: map['package'] as String? ?? '',
    );
  }

  static SkipEventKind _kindFrom(String? raw) {
    switch (raw) {
      case 'skip':
        return SkipEventKind.skip;
      case 'close':
        return SkipEventKind.close;
      case 'service_on':
        return SkipEventKind.serviceOn;
      case 'service_off':
        return SkipEventKind.serviceOff;
      default:
        return SkipEventKind.unknown;
    }
  }

  /// Có làm tăng bộ đếm "đã bỏ qua" hay không.
  bool get isClick => kind == SkipEventKind.skip || kind == SkipEventKind.close;

  String get description {
    switch (kind) {
      case SkipEventKind.skip:
        return 'Đã bấm "Bỏ qua" ($label)';
      case SkipEventKind.close:
        return 'Đã đóng banner quảng cáo ($label)';
      case SkipEventKind.serviceOn:
        return 'Dịch vụ trợ năng bắt đầu chạy';
      case SkipEventKind.serviceOff:
        return 'Dịch vụ trợ năng đã dừng';
      case SkipEventKind.unknown:
        return label;
    }
  }
}
