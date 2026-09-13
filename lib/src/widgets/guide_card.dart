import 'package:flutter/material.dart';

import 'section_card.dart';

/// Hướng dẫn 3 bước, hiện ngay trên màn hình chính vì phần lớn người dùng sẽ
/// khựng lại ở bước cấp quyền Trợ năng.
class GuideCard extends StatelessWidget {
  const GuideCard({
    super.key,
    required this.onOpenAccessibility,
    required this.onOpenBattery,
    required this.onAddBlackScreenTile,
  });

  final VoidCallback onOpenAccessibility;
  final VoidCallback onOpenBattery;

  /// Nhờ hệ thống thêm nút "Màn hình đen" vào thanh Cài đặt nhanh.
  final Future<bool> Function() onAddBlackScreenTile;

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
          const SizedBox(height: 14),
          const _Step(
            number: '4',
            title: 'Nghe YouTube khi "tắt màn hình"',
            detail: 'Bấm nút nguồn thì YouTube sẽ dừng. Thay vào đó, kéo thanh thông báo '
                'xuống và bấm nút "Màn hình đen". Màn hình tối hẳn mà video vẫn phát. '
                'Chạm 2 lần để mở lại.',
          ),
          const SizedBox(height: 12),
          // Android không tự gắn nút vào Cài đặt nhanh, người dùng phải đồng ý.
          OutlinedButton.icon(
            onPressed: () => _addTile(context),
            icon: const Icon(Icons.add_circle_outline, size: 20),
            label: const Text('Thêm nút "Màn hình đen"'),
            style: OutlinedButton.styleFrom(minimumSize: const Size.fromHeight(46)),
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

  Future<void> _addTile(BuildContext context) async {
    final ScaffoldMessengerState messenger = ScaffoldMessenger.of(context);
    if (await onAddBlackScreenTile()) return;
    // Android 12 trở xuống không có API này — chỉ còn cách chỉ đường thủ công.
    messenger.showSnackBar(
      const SnackBar(
        duration: Duration(seconds: 8),
        content: Text(
          'Máy này phải thêm tay: kéo thanh thông báo xuống hết cỡ, bấm biểu '
          'tượng bút chì (hoặc dấu ba chấm > Chỉnh sửa nút), rồi kéo nút '
          '"Màn hình đen" lên bảng.',
        ),
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
            color: theme.colorScheme.primary.withValues(alpha: 0.12),
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
