import 'package:flutter/material.dart';

import 'section_card.dart';

/// Hiện ra khi người dùng đã vào màn hình Trợ năng mà vẫn không bật được.
///
/// Từ Android 13, hệ thống chặn mọi app cài ngoài Play Store không cho bật
/// quyền Trợ năng ("Cài đặt hạn chế"). Đây là rào của hệ điều hành chứ không
/// phải lỗi của app, và phải tự tay mở khoá trong phần Thông tin ứng dụng.
class RestrictedSettingsCard extends StatelessWidget {
  const RestrictedSettingsCard({
    super.key,
    required this.onOpenAppInfo,
    required this.onOpenAccessibility,
  });

  final VoidCallback onOpenAppInfo;
  final VoidCallback onOpenAccessibility;

  @override
  Widget build(BuildContext context) {
    final ThemeData theme = Theme.of(context);
    final Color accent = theme.colorScheme.tertiary;

    return SectionCard(
      color: accent.withValues(alpha: 0.10),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          Row(
            children: <Widget>[
              Icon(Icons.shield_outlined, color: accent),
              const SizedBox(width: 10),
              Expanded(
                child: Text(
                  'Máy báo "Cài đặt hạn chế"?',
                  style: theme.textTheme.titleMedium?.copyWith(
                    fontWeight: FontWeight.w700,
                    color: accent,
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 10),
          Text(
            'Từ Android 13, hệ thống chặn app cài ngoài Play Store bật quyền Trợ '
            'năng. Đây là rào của Android, app nào tải bằng file APK cũng dính. '
            'Mở khoá một lần là xong:',
            style: theme.textTheme.bodyMedium,
          ),
          const SizedBox(height: 14),
          const _Step(
            number: '1',
            text: 'Máy Samsung: Cài đặt > Bảo mật và quyền riêng tư > tắt '
                '"Chặn tự động" (Auto Blocker). Máy khác bỏ qua bước này.',
          ),
          const _Step(
            number: '2',
            text: 'Vào Thông tin ứng dụng (nút bên dưới) > bấm dấu ba chấm ở góc '
                'trên bên phải > chọn "Cho phép cài đặt bị hạn chế".',
          ),
          const _Step(
            number: '3',
            text: 'Quay lại Cài đặt > Trợ năng và bật app lên như bình thường.',
          ),
          const SizedBox(height: 14),
          Row(
            children: <Widget>[
              Expanded(
                child: FilledButton(
                  onPressed: onOpenAppInfo,
                  style: FilledButton.styleFrom(
                    backgroundColor: accent,
                    minimumSize: const Size.fromHeight(46),
                  ),
                  child: const Text('Thông tin ứng dụng'),
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: OutlinedButton(
                  onPressed: onOpenAccessibility,
                  style: OutlinedButton.styleFrom(
                    minimumSize: const Size.fromHeight(46),
                  ),
                  child: const Text('Trợ năng'),
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
          Text(
            'Không thấy mục "Cho phép cài đặt bị hạn chế"? Cắm máy vào máy tính '
            'rồi chạy:\n'
            'adb shell appops set com.hung.adskipper ACCESS_RESTRICTED_SETTINGS allow',
            style: theme.textTheme.bodySmall?.copyWith(
              color: theme.colorScheme.onSurfaceVariant,
            ),
          ),
        ],
      ),
    );
  }
}

class _Step extends StatelessWidget {
  const _Step({required this.number, required this.text});

  final String number;
  final String text;

  @override
  Widget build(BuildContext context) {
    final ThemeData theme = Theme.of(context);
    return Padding(
      padding: const EdgeInsets.only(bottom: 10),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          Container(
            width: 22,
            height: 22,
            alignment: Alignment.center,
            decoration: BoxDecoration(
              color: theme.colorScheme.tertiary.withValues(alpha: 0.16),
              shape: BoxShape.circle,
            ),
            child: Text(
              number,
              style: theme.textTheme.labelSmall?.copyWith(
                fontWeight: FontWeight.w700,
                color: theme.colorScheme.tertiary,
              ),
            ),
          ),
          const SizedBox(width: 10),
          Expanded(child: Text(text, style: theme.textTheme.bodySmall)),
        ],
      ),
    );
  }
}
