import 'package:flutter/material.dart';

import 'pages/home_page.dart';
import 'services/app_controller.dart';
import 'theme.dart';

class AdSkipperApp extends StatefulWidget {
  const AdSkipperApp({super.key});

  @override
  State<AdSkipperApp> createState() => _AdSkipperAppState();
}

class _AdSkipperAppState extends State<AdSkipperApp> with WidgetsBindingObserver {
  final AppController _controller = AppController();

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _controller.start();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    // Người dùng có thể vừa bật quyền Trợ năng rồi quay lại; đọc lại trạng thái.
    if (state == AppLifecycleState.resumed) {
      _controller.refresh();
    }
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return ControllerScope(
      controller: _controller,
      child: MaterialApp(
        title: 'Bỏ Qua Quảng Cáo',
        debugShowCheckedModeBanner: false,
        theme: AppTheme.light(),
        darkTheme: AppTheme.dark(),
        home: const HomePage(),
      ),
    );
  }
}

/// Truyền [AppController] xuống cây widget mà không cần thư viện ngoài.
class ControllerScope extends InheritedNotifier<AppController> {
  const ControllerScope({
    super.key,
    required AppController controller,
    required super.child,
  }) : super(notifier: controller);

  static AppController of(BuildContext context) {
    final ControllerScope? scope =
        context.dependOnInheritedWidgetOfExactType<ControllerScope>();
    assert(scope != null, 'ControllerScope chưa được đặt phía trên widget này');
    return scope!.notifier!;
  }
}
