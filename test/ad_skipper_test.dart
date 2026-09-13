import 'package:ad_skipper/src/app.dart';
import 'package:ad_skipper/src/models/skip_event.dart';
import 'package:ad_skipper/src/models/skip_settings.dart';
import 'package:ad_skipper/src/utils/formatting.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  group('SkipSettings', () {
    test('đọc được map do Android gửi sang', () {
      final SkipSettings settings = SkipSettings.fromMap(<Object?, Object?>{
        'enabled': false,
        'scanIntervalMs': 250,
        'skipLabels': <Object?>['bỏ qua', 'skip ad', 42],
        'totalSkips': 7,
        'lastSkipAt': 1700000000000,
      });

      expect(settings.enabled, isFalse);
      expect(settings.scanIntervalMs, 250);
      expect(settings.skipLabels, <String>['bỏ qua', 'skip ad']);
      expect(settings.totalSkips, 7);
      expect(settings.lastSkipAt, isNotNull);
    });

    test('map rỗng thì dùng giá trị mặc định', () {
      const SkipSettings defaults = SkipSettings();
      final SkipSettings settings = SkipSettings.fromMap(const <Object?, Object?>{});

      expect(settings.enabled, defaults.enabled);
      expect(settings.scanIntervalMs, defaults.scanIntervalMs);
      expect(settings.lastSkipAt, isNull);
    });

    test('tắt tiếng quảng cáo bật sẵn khi chưa có cấu hình', () {
      final SkipSettings settings = SkipSettings.fromMap(const <Object?, Object?>{});
      expect(settings.muteDuringAds, isTrue);
    });

    test('toUpdateMap mang theo tuỳ chọn tắt tiếng', () {
      const SkipSettings settings = SkipSettings(
        muteDuringAds: false,
        adMarkerLabels: <String>['được tài trợ'],
      );
      final Map<String, Object?> map = settings.toUpdateMap();

      expect(map['muteDuringAds'], isFalse);
      expect(map['adMarkerLabels'], <String>['được tài trợ']);
    });

    test('copyWith chỉ đổi trường được truyền vào', () {
      const SkipSettings settings = SkipSettings(scanIntervalMs: 400, totalSkips: 3);
      final SkipSettings updated = settings.copyWith(scanIntervalMs: 900);

      expect(updated.scanIntervalMs, 900);
      expect(updated.totalSkips, 3);
    });
  });

  group('SkipEvent', () {
    test('phân loại được sự kiện bấm nút', () {
      final SkipEvent event = SkipEvent.fromMap(<Object?, Object?>{
        'time': 1700000000000,
        'kind': 'skip',
        'label': 'bỏ qua',
        'package': 'com.google.android.youtube',
      });

      expect(event.kind, SkipEventKind.skip);
      expect(event.isClick, isTrue);
      expect(event.description, contains('Bỏ qua'));
    });

    test('sự kiện tắt tiếng không tính là một lần bấm', () {
      final SkipEvent event = SkipEvent.fromMap(<Object?, Object?>{'kind': 'mute'});

      expect(event.kind, SkipEventKind.mute);
      expect(event.isClick, isFalse);
      expect(event.description, contains('tắt tiếng'));
    });

    test('sự kiện dịch vụ không tính là một lần bấm', () {
      final SkipEvent event =
          SkipEvent.fromMap(<Object?, Object?>{'kind': 'service_on'});
      expect(event.isClick, isFalse);
    });
  });

  group('formatting', () {
    test('formatRelative', () {
      expect(formatRelative(null), 'Chưa có');
      expect(formatRelative(DateTime.now()), 'Vừa xong');
      expect(
        formatRelative(DateTime.now().subtract(const Duration(minutes: 5))),
        '5 phút trước',
      );
    });

    test('friendlyPackageName', () {
      expect(friendlyPackageName('com.google.android.youtube'), 'YouTube');
      expect(friendlyPackageName('com.vi.du'), 'com.vi.du');
    });
  });

  testWidgets('màn hình chính nhắc cấp quyền khi chưa bật Trợ năng', (
    WidgetTester tester,
  ) async {
    // Trong môi trường test không có phía Android, channel trả về mặc định:
    // coi như người dùng chưa cấp quyền Trợ năng.
    await tester.pumpWidget(const AdSkipperApp());
    await tester.pumpAndSettle();

    expect(find.text('Chưa được cấp quyền'), findsOneWidget);
    expect(find.widgetWithText(FilledButton, 'Mở cài đặt Trợ năng'), findsOneWidget);
  });
}
