import 'package:flutter/material.dart';

import '../app.dart';
import '../models/skip_settings.dart';
import '../services/app_controller.dart';
import '../utils/formatting.dart';
import '../widgets/list_editor.dart';
import '../widgets/section_card.dart';

/// Các tuỳ chọn nâng cao: tốc độ quét, nhãn nút, ứng dụng được theo dõi.
class SettingsPage extends StatelessWidget {
  const SettingsPage({super.key});

  /// Phải trùng với SkipTargets.DEFAULT_PACKAGES bên Android.
  static const List<String> knownPackages = <String>[
    'com.google.android.youtube',
    'com.google.android.apps.youtube.music',
    'com.google.android.apps.youtube.unplugged',
    'com.google.android.youtube.tv',
  ];

  @override
  Widget build(BuildContext context) {
    final AppController controller = ControllerScope.of(context);
    final SkipSettings settings = controller.settings;

    return Scaffold(
      appBar: AppBar(title: const Text('Cài đặt')),
      body: ListView(
        padding: const EdgeInsets.fromLTRB(16, 8, 16, 32),
        children: <Widget>[
          const SectionTitle('Hành vi'),
          SectionCard(
            padding: const EdgeInsets.symmetric(vertical: 4),
            child: Column(
              children: <Widget>[
                SwitchListTile.adaptive(
                  value: settings.muteDuringAds,
                  onChanged: (bool value) =>
                      controller.update(settings.copyWith(muteDuringAds: value)),
                  title: const Text('Tắt tiếng khi quảng cáo đang chạy'),
                  subtitle: const Text(
                    'Chỉ tắt tiếng quảng cáo VIDEO trong trình phát (đoạn 5 giây '
                    'đầu chưa cho bỏ qua). Không đụng tới quảng cáo cài app hay '
                    'banner tài trợ. Tiếng tự bật lại khi hết, khi rời YouTube, '
                    'hoặc sau 90 giây.',
                  ),
                ),
                const Divider(height: 1),
                SwitchListTile.adaptive(
                  value: settings.closeOverlayAds,
                  onChanged: (bool value) =>
                      controller.update(settings.copyWith(closeOverlayAds: value)),
                  title: const Text('Đóng banner quảng cáo'),
                  subtitle: const Text('Tự bấm nút X của dải quảng cáo dưới trình phát'),
                ),
                const Divider(height: 1),
                SwitchListTile.adaptive(
                  value: settings.vibrate,
                  onChanged: (bool value) =>
                      controller.update(settings.copyWith(vibrate: value)),
                  title: const Text('Rung khi bấm'),
                  subtitle: const Text('Rung nhẹ mỗi lần bỏ qua thành công'),
                ),
              ],
            ),
          ),
          const SizedBox(height: 20),
          const SectionTitle('Tốc độ phản ứng'),
          SectionCard(
            child: Column(
              children: <Widget>[
                _SliderRow(
                  label: 'Chu kỳ quét màn hình',
                  value: settings.scanIntervalMs.toDouble(),
                  min: 150,
                  max: 2000,
                  divisions: 37,
                  suffix: 'ms',
                  help: 'Số nhỏ thì bấm nhanh hơn nhưng tốn pin hơn.',
                  onChanged: (double value) =>
                      controller.update(settings.copyWith(scanIntervalMs: value.round())),
                ),
                const SizedBox(height: 8),
                _SliderRow(
                  label: 'Nghỉ giữa hai lần bấm',
                  value: settings.clickCooldownMs.toDouble(),
                  min: 200,
                  max: 3000,
                  divisions: 28,
                  suffix: 'ms',
                  help: 'Tránh bấm liên tiếp vào cùng một nút.',
                  onChanged: (double value) =>
                      controller.update(settings.copyWith(clickCooldownMs: value.round())),
                ),
              ],
            ),
          ),
          const SizedBox(height: 20),
          const SectionTitle('Ứng dụng được theo dõi'),
          SectionCard(
            padding: const EdgeInsets.symmetric(vertical: 4),
            child: Column(
              children: <Widget>[
                for (int i = 0; i < knownPackages.length; i++) ...<Widget>[
                  if (i > 0) const Divider(height: 1),
                  CheckboxListTile(
                    value: settings.packages.contains(knownPackages[i]),
                    onChanged: (bool? value) => controller.update(
                      settings.copyWith(
                        packages: _togglePackage(
                          settings.packages,
                          knownPackages[i],
                          value ?? false,
                        ),
                      ),
                    ),
                    title: Text(friendlyPackageName(knownPackages[i])),
                    subtitle: Text(
                      knownPackages[i],
                      style: Theme.of(context).textTheme.bodySmall,
                    ),
                  ),
                ],
              ],
            ),
          ),
          const SizedBox(height: 20),
          const SectionTitle('Nhận diện nút'),
          SectionCard(
            padding: const EdgeInsets.symmetric(vertical: 4),
            child: Column(
              children: <Widget>[
                ListEditor(
                  title: 'Nhãn nút "Bỏ qua"',
                  values: settings.skipLabels,
                  hint: 'App ưu tiên nhận diện theo mã của nút nên thường không cần sửa. '
                      'Chỉ thêm nhãn khi YouTube đổi chữ hoặc bạn dùng ngôn ngữ khác.',
                  onChanged: (List<String> value) =>
                      controller.update(settings.copyWith(skipLabels: value)),
                ),
                const Divider(height: 1),
                ListEditor(
                  title: 'Nhãn nút đóng quảng cáo',
                  values: settings.closeLabels,
                  onChanged: (List<String> value) =>
                      controller.update(settings.copyWith(closeLabels: value)),
                ),
              ],
            ),
          ),
          const SizedBox(height: 24),
          OutlinedButton.icon(
            onPressed: () => _confirmRestore(context, controller),
            icon: const Icon(Icons.restart_alt),
            label: const Text('Khôi phục cài đặt gốc'),
          ),
        ],
      ),
    );
  }

  static List<String> _togglePackage(List<String> current, String value, bool selected) {
    final List<String> next = List<String>.of(current);
    if (selected) {
      if (!next.contains(value)) next.add(value);
    } else {
      next.remove(value);
    }
    return next;
  }

  Future<void> _confirmRestore(BuildContext context, AppController controller) async {
    final bool? ok = await showDialog<bool>(
      context: context,
      builder: (BuildContext context) => AlertDialog(
        title: const Text('Khôi phục cài đặt gốc?'),
        content: const Text('Mọi tuỳ chỉnh về nhãn nút và tốc độ quét sẽ trở lại mặc định.'),
        actions: <Widget>[
          TextButton(
            onPressed: () => Navigator.of(context).pop(false),
            child: const Text('Huỷ'),
          ),
          FilledButton(
            onPressed: () => Navigator.of(context).pop(true),
            style: FilledButton.styleFrom(minimumSize: const Size(88, 40)),
            child: const Text('Khôi phục'),
          ),
        ],
      ),
    );
    if (ok ?? false) await controller.restoreDefaults();
  }
}

class _SliderRow extends StatelessWidget {
  const _SliderRow({
    required this.label,
    required this.value,
    required this.min,
    required this.max,
    required this.divisions,
    required this.suffix,
    required this.help,
    required this.onChanged,
  });

  final String label;
  final double value;
  final double min;
  final double max;
  final int divisions;
  final String suffix;
  final String help;
  final ValueChanged<double> onChanged;

  @override
  Widget build(BuildContext context) {
    final ThemeData theme = Theme.of(context);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: <Widget>[
        Row(
          children: <Widget>[
            Expanded(child: Text(label, style: theme.textTheme.bodyLarge)),
            Text(
              '${value.round()} $suffix',
              style: theme.textTheme.labelLarge?.copyWith(color: theme.colorScheme.primary),
            ),
          ],
        ),
        Slider(
          value: value.clamp(min, max),
          min: min,
          max: max,
          divisions: divisions,
          onChanged: onChanged,
        ),
        Text(
          help,
          style: theme.textTheme.bodySmall?.copyWith(
            color: theme.colorScheme.onSurfaceVariant,
          ),
        ),
      ],
    );
  }
}
