/// Định dạng thời gian kiểu "3 phút trước" cho gọn màn hình.
String formatRelative(DateTime? time) {
  if (time == null) return 'Chưa có';
  final Duration diff = DateTime.now().difference(time);
  if (diff.inSeconds < 60) return 'Vừa xong';
  if (diff.inMinutes < 60) return '${diff.inMinutes} phút trước';
  if (diff.inHours < 24) return '${diff.inHours} giờ trước';
  if (diff.inDays < 7) return '${diff.inDays} ngày trước';
  return formatDate(time);
}

String formatDate(DateTime time) =>
    '${_two(time.day)}/${_two(time.month)}/${time.year}';

String formatClock(DateTime time) =>
    '${_two(time.hour)}:${_two(time.minute)}:${_two(time.second)}';

String _two(int value) => value.toString().padLeft(2, '0');

/// Rút gọn tên package cho dễ đọc: com.google.android.youtube -> YouTube.
String friendlyPackageName(String packageName) {
  switch (packageName) {
    case 'com.google.android.youtube':
      return 'YouTube';
    case 'com.google.android.apps.youtube.music':
      return 'YouTube Music';
    case 'com.google.android.apps.youtube.unplugged':
      return 'YouTube TV';
    case 'com.google.android.youtube.tv':
      return 'YouTube (Android TV)';
    case '':
      return 'Hệ thống';
    default:
      return packageName;
  }
}
