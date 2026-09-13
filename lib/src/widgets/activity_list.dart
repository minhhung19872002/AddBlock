import 'package:flutter/material.dart';

import '../models/skip_event.dart';
import '../utils/formatting.dart';
import 'section_card.dart';

/// Nhật ký các lần app đã bấm, để kiểm chứng là nó thật sự hoạt động.
class ActivityList extends StatelessWidget {
  const ActivityList({super.key, required this.events, this.maxItems = 8});

  final List<SkipEvent> events;
  final int maxItems;

  @override
  Widget build(BuildContext context) {
    final ThemeData theme = Theme.of(context);
    if (events.isEmpty) {
      return SectionCard(
        child: Row(
          children: <Widget>[
            Icon(Icons.history_toggle_off, color: theme.colorScheme.onSurfaceVariant),
            const SizedBox(width: 12),
            Expanded(
              child: Text(
                'Chưa có hoạt động nào. Mở YouTube và chờ quảng cáo đầu tiên.',
                style: theme.textTheme.bodyMedium?.copyWith(
                  color: theme.colorScheme.onSurfaceVariant,
                ),
              ),
            ),
          ],
        ),
      );
    }

    final List<SkipEvent> visible = events.take(maxItems).toList(growable: false);
    return SectionCard(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Column(
        children: <Widget>[
          for (int i = 0; i < visible.length; i++) ...<Widget>[
            if (i > 0) const Divider(height: 1, indent: 56),
            _EventTile(event: visible[i]),
          ],
        ],
      ),
    );
  }
}

class _EventTile extends StatelessWidget {
  const _EventTile({required this.event});

  final SkipEvent event;

  @override
  Widget build(BuildContext context) {
    final ThemeData theme = Theme.of(context);
    final bool isClick = event.isClick;
    final Color color = isClick ? const Color(0xFF1B873F) : theme.colorScheme.onSurfaceVariant;

    return ListTile(
      dense: true,
      leading: CircleAvatar(
        radius: 16,
        backgroundColor: color.withOpacity(0.12),
        child: Icon(
          isClick ? Icons.check_rounded : Icons.info_outline,
          size: 18,
          color: color,
        ),
      ),
      title: Text(event.description, style: theme.textTheme.bodyMedium),
      subtitle: Text(
        '${formatClock(event.time)} · ${friendlyPackageName(event.packageName)}',
        style: theme.textTheme.bodySmall?.copyWith(
          color: theme.colorScheme.onSurfaceVariant,
        ),
      ),
    );
  }
}
