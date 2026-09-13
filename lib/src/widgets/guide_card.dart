import 'package:flutter/material.dart';

import 'section_card.dart';

/// Hướng dẫn 3 bước, hiện ngay trên màn hình chính vì phần lớn người dùng sẽ
/// khựng lại ở bước cấp quyền Trợ năng.
class GuideCard extends StatelessWidget {
  const GuideCard({super.key, required this.onOpenAccessibility, required this.onOpenBattery});

  final VoidCallback onOpenAccessibility;
  final VoidCallback onOpenBattery;

  @override
  Widget build(BuildContext context) {
    final ThemeData theme = Theme.of(context);
    return SectionCard(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          const _Step(
            number: '1',
            title: 'Bật quyền Trợ năng',
            detail: 'Cài đặt > Trợ năng > Ứng dụng đã tải > "Bỏ Qua Quảng Cáo" > Bật.',
          ),
          const SizedBox(height: 14),
          const _Step(
            number: '2',
            title: 'Cho phép chạy nền',
            detail: 'Tắt tối ưu hoá pin cho app, nếu không hệ thống có thể tự dừng dịch vụ.',
          ),
          const SizedBox(height: 14),
          const _Step(
            number: '3',
            title: 'Mở YouTube và xem bình thường',
            detail: 'Không cần mở lại app này. Nút "Bỏ qua" hiện ra là được bấm ngay.',
          ),
          const SizedBox(height: 18),
          Row(
            children: <Widget>[
              Expanded(
                child: OutlinedButton(
                  onPressed: onOpenAccessibility,
                  child: const Text('Trợ năng'),
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: OutlinedButton(
                  onPressed: onOpenBattery,
                  child: const Text('Tối ưu pin'),
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
          Text(
            'App chỉ đọc giao diện để tìm nút "Bỏ qua". Không có nội dung nào được '
            'lưu lại hay gửi đi, và app không bao giờ bấm vào nút dẫn tới trang quảng cáo.',
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
  const _Step({required this.number, required this.title, required this.detail});

  final String number;
  final String title;
  final String detail;

  @override
  Widget build(BuildContext context) {
    final ThemeData theme = Theme.of(context);
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: <Widget>[
        Container(
          width: 26,
          height: 26,
          alignment: Alignment.center,
          decoration: BoxDecoration(
            color: theme.colorScheme.primary.withOpacity(0.12),
            shape: BoxShape.circle,
          ),
          child: Text(
            number,
            style: theme.textTheme.labelMedium?.copyWith(
              color: theme.colorScheme.primary,
              fontWeight: FontWeight.w700,
            ),
          ),
        ),
        const SizedBox(width: 12),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: <Widget>[
              Text(
                title,
                style: theme.textTheme.bodyLarge?.copyWith(fontWeight: FontWeight.w600),
              ),
              const SizedBox(height: 2),
              Text(
                detail,
                style: theme.textTheme.bodySmall?.copyWith(
                  color: theme.colorScheme.onSurfaceVariant,
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }
}
