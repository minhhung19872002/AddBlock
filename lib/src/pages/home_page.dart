import 'package:flutter/material.dart';

import '../app.dart';
import '../services/app_controller.dart';
import '../widgets/activity_list.dart';
import '../widgets/guide_card.dart';
import '../widgets/section_card.dart';
import '../widgets/stats_row.dart';
import '../widgets/status_card.dart';
import 'settings_page.dart';

class HomePage extends StatelessWidget {
  const HomePage({super.key});

  @override
  Widget build(BuildContext context) {
    final AppController controller = ControllerScope.of(context);

    if (controller.loading) {
      return const Scaffold(body: Center(child: CircularProgressIndicator()));
    }

    return Scaffold(
      appBar: AppBar(
        title: const Text(
          'Bỏ Qua Quảng Cáo',
          style: TextStyle(fontWeight: FontWeight.w700),
        ),
        actions: <Widget>[
          IconButton(
            tooltip: 'Cài đặt',
            icon: const Icon(Icons.tune_rounded),
            onPressed: () => Navigator.of(context).push(
              MaterialPageRoute<void>(
                builder: (_) => ControllerScope(
                  controller: controller,
                  child: const SettingsPage(),
                ),
              ),
            ),
          ),
        ],
      ),
      body: RefreshIndicator(
        onRefresh: controller.refresh,
        child: ListView(
          physics: const AlwaysScrollableScrollPhysics(),
          padding: const EdgeInsets.fromLTRB(16, 8, 16, 32),
          children: <Widget>[
            StatusCard(controller: controller),
            const SizedBox(height: 16),
            StatsRow(settings: controller.settings),
            const SizedBox(height: 20),
            SectionTitle(
              'Hoạt động gần đây',
              trailing: controller.log.isEmpty
                  ? null
                  : TextButton(
                      onPressed: () => _confirmReset(context, controller),
                      child: const Text('Xoá'),
                    ),
            ),
            ActivityList(events: controller.log),
            const SizedBox(height: 20),
            const SectionTitle('Hướng dẫn'),
            GuideCard(
              onOpenAccessibility: controller.openAccessibilitySettings,
              onOpenBattery: controller.openBatterySettings,
            ),
          ],
        ),
      ),
    );
  }

  Future<void> _confirmReset(BuildContext context, AppController controller) async {
    final bool? ok = await showDialog<bool>(
      context: context,
      builder: (BuildContext context) => AlertDialog(
        title: const Text('Xoá nhật ký?'),
        content: const Text('Bộ đếm và toàn bộ lịch sử hoạt động sẽ được đặt lại về 0.'),
        actions: <Widget>[
          TextButton(
            onPressed: () => Navigator.of(context).pop(false),
            child: const Text('Huỷ'),
          ),
          FilledButton(
            onPressed: () => Navigator.of(context).pop(true),
            style: FilledButton.styleFrom(minimumSize: const Size(88, 40)),
            child: const Text('Xoá'),
          ),
        ],
      ),
    );
    if (ok ?? false) await controller.resetStats();
  }
}
