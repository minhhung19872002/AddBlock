import 'package:flutter/material.dart';

import '../models/skip_settings.dart';
import '../utils/formatting.dart';
import 'section_card.dart';

/// Hai ô thống kê: tổng số lần đã bỏ qua và thời điểm gần nhất.
class StatsRow extends StatelessWidget {
  const StatsRow({super.key, required this.settings});

  final SkipSettings settings;

  @override
  Widget build(BuildContext context) {
    return Row(
      children: <Widget>[
        Expanded(
          child: _StatTile(
            icon: Icons.fast_forward_rounded,
            value: '${settings.totalSkips}',
            label: 'quảng cáo đã bỏ qua',
          ),
        ),
        const SizedBox(width: 12),
        Expanded(
          child: _StatTile(
            icon: Icons.schedule_rounded,
            value: formatRelative(settings.lastSkipAt),
            label: 'lần bỏ qua gần nhất',
          ),
        ),
      ],
    );
  }
}

class _StatTile extends StatelessWidget {
  const _StatTile({required this.icon, required this.value, required this.label});

  final IconData icon;
  final String value;
  final String label;

  @override
  Widget build(BuildContext context) {
    final ThemeData theme = Theme.of(context);
    return SectionCard(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          Icon(icon, size: 20, color: theme.colorScheme.primary),
          const SizedBox(height: 10),
          FittedBox(
            fit: BoxFit.scaleDown,
            alignment: Alignment.centerLeft,
            child: Text(
              value,
              maxLines: 1,
              style: theme.textTheme.headlineSmall?.copyWith(fontWeight: FontWeight.w700),
            ),
          ),
          const SizedBox(height: 2),
          Text(
            label,
            style: theme.textTheme.bodySmall?.copyWith(
              color: theme.colorScheme.onSurfaceVariant,
            ),
          ),
        ],
      ),
    );
  }
}
