import 'package:flutter/material.dart';

import '../services/app_controller.dart';
import 'section_card.dart';

/// Thẻ lớn trên cùng: cho biết app đang canh quảng cáo hay đang nằm im.
class StatusCard extends StatelessWidget {
  const StatusCard({super.key, required this.controller});

  final AppController controller;

  @override
  Widget build(BuildContext context) {
    final ThemeData theme = Theme.of(context);
    final ColorScheme scheme = theme.colorScheme;

    final bool granted = controller.accessibilityEnabled;
    final bool active = controller.active;

    final Color accent = !granted
        ? scheme.error
        : active
            ? const Color(0xFF1B873F)
            : scheme.tertiary;
    final IconData icon = !granted
        ? Icons.lock_outline
        : active
            ? Icons.verified_outlined
            : Icons.pause_circle_outline;
    final String title = !granted
        ? 'Chưa được cấp quyền'
        : active
            ? 'Đang canh quảng cáo'
            : 'Đang tạm dừng';
    final String activeSubtitle = controller.settings.muteDuringAds
        ? 'Cứ mở YouTube như bình thường. Lúc quảng cáo chưa cho bỏ qua thì app '
            'tắt tiếng, nút "Bỏ qua" hiện ra là bấm ngay.'
        : 'Cứ mở YouTube như bình thường. Khi nút "Bỏ qua" hiện ra, app sẽ bấm ngay.';
    final String subtitle = !granted
        ? 'Bật quyền Trợ năng để app có thể bấm nút "Bỏ qua" giúp bạn.'
        : active
            ? activeSubtitle
            : 'Quyền đã được cấp. Bật công tắc bên dưới để tiếp tục tự động bỏ qua.';

    return SectionCard(
      color: accent.withOpacity(0.10),
      padding: const EdgeInsets.all(20),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          Row(
            children: <Widget>[
              Container(
                width: 48,
                height: 48,
                decoration: BoxDecoration(
                  color: accent.withOpacity(0.18),
                  shape: BoxShape.circle,
                ),
                child: Icon(icon, color: accent, size: 26),
              ),
              const SizedBox(width: 14),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: <Widget>[
                    Text(
                      title,
                      style: theme.textTheme.titleMedium?.copyWith(
                        fontWeight: FontWeight.w700,
                        color: accent,
                      ),
                    ),
                    const SizedBox(height: 2),
                    Text(
                      granted
                          ? (controller.serviceRunning
                              ? 'Dịch vụ trợ năng đang chạy'
                              : 'Dịch vụ sẽ khởi động khi mở YouTube')
                          : 'Cài đặt > Trợ năng > Bỏ Qua Quảng Cáo',
                      style: theme.textTheme.bodySmall?.copyWith(
                        color: scheme.onSurfaceVariant,
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),
          const SizedBox(height: 14),
          Text(subtitle, style: theme.textTheme.bodyMedium),
          const SizedBox(height: 16),
          if (!granted)
            FilledButton.icon(
              onPressed: controller.openAccessibilitySettings,
              icon: const Icon(Icons.settings_accessibility),
              label: const Text('Mở cài đặt Trợ năng'),
              style: FilledButton.styleFrom(backgroundColor: accent),
            )
          else
            SwitchListTile.adaptive(
              value: controller.settings.enabled,
              onChanged: controller.setEnabled,
              contentPadding: EdgeInsets.zero,
              title: const Text(
                'Tự động bỏ qua quảng cáo',
                style: TextStyle(fontWeight: FontWeight.w600),
              ),
              subtitle: const Text('Tắt tạm thời mà không cần gỡ quyền Trợ năng'),
            ),
          // Mất tiếng đột ngột dễ làm người dùng hoảng — nói rõ là do app.
          if (controller.muted) ...<Widget>[
            const SizedBox(height: 4),
            Row(
              children: <Widget>[
                Icon(Icons.volume_off_rounded, size: 18, color: accent),
                const SizedBox(width: 8),
                Expanded(
                  child: Text(
                    'Đang tắt tiếng vì quảng cáo — sẽ tự bật lại khi hết.',
                    style: theme.textTheme.bodySmall?.copyWith(
                      color: accent,
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                ),
              ],
            ),
          ],
        ],
      ),
    );
  }
}
