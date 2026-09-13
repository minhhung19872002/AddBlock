import 'package:flutter/material.dart';

/// Trình sửa danh sách chuỗi dạng chip (dùng cho các nhãn "Bỏ qua").
///
/// Hữu ích khi Google đổi chữ trên nút: người dùng tự thêm nhãn mới mà không
/// cần chờ bản cập nhật.
class ListEditor extends StatelessWidget {
  const ListEditor({
    super.key,
    required this.title,
    required this.values,
    required this.onChanged,
    this.hint,
  });

  final String title;
  final List<String> values;
  final ValueChanged<List<String>> onChanged;
  final String? hint;

  @override
  Widget build(BuildContext context) {
    final ThemeData theme = Theme.of(context);
    return ExpansionTile(
      tilePadding: const EdgeInsets.symmetric(horizontal: 16),
      childrenPadding: const EdgeInsets.fromLTRB(16, 0, 16, 16),
      title: Text(title, style: theme.textTheme.bodyLarge),
      subtitle: Text(
        '${values.length} nhãn',
        style: theme.textTheme.bodySmall?.copyWith(
          color: theme.colorScheme.onSurfaceVariant,
        ),
      ),
      children: <Widget>[
        if (hint != null)
          Padding(
            padding: const EdgeInsets.only(bottom: 12),
            child: Text(
              hint!,
              style: theme.textTheme.bodySmall?.copyWith(
                color: theme.colorScheme.onSurfaceVariant,
              ),
            ),
          ),
        Wrap(
          spacing: 8,
          runSpacing: 8,
          children: <Widget>[
            for (final String value in values)
              InputChip(
                label: Text(value),
                onDeleted: () => onChanged(
                  values.where((String item) => item != value).toList(growable: false),
                ),
              ),
            ActionChip(
              avatar: const Icon(Icons.add, size: 18),
              label: const Text('Thêm'),
              onPressed: () => _addValue(context),
            ),
          ],
        ),
      ],
    );
  }

  Future<void> _addValue(BuildContext context) async {
    final TextEditingController textController = TextEditingController();
    final String? added = await showDialog<String>(
      context: context,
      builder: (BuildContext context) => AlertDialog(
        title: Text('Thêm vào "$title"'),
        content: TextField(
          controller: textController,
          autofocus: true,
          textInputAction: TextInputAction.done,
          decoration: const InputDecoration(
            labelText: 'Nội dung chữ trên nút',
            hintText: 'ví dụ: Bỏ qua quảng cáo',
          ),
          onSubmitted: (String value) => Navigator.of(context).pop(value),
        ),
        actions: <Widget>[
          TextButton(
            onPressed: () => Navigator.of(context).pop(),
            child: const Text('Huỷ'),
          ),
          FilledButton(
            onPressed: () => Navigator.of(context).pop(textController.text),
            style: FilledButton.styleFrom(minimumSize: const Size(88, 40)),
            child: const Text('Thêm'),
          ),
        ],
      ),
    );
    textController.dispose();

    final String cleaned = (added ?? '').trim().toLowerCase();
    if (cleaned.isEmpty || values.contains(cleaned)) return;
    onChanged(<String>[...values, cleaned]);
  }
}
