# Bỏ Qua Quảng Cáo — tự động bấm "Bỏ qua" trên YouTube

Ứng dụng Flutter (Android) tự động bấm nút **"Bỏ qua"** mỗi khi quảng cáo YouTube
hiện ra, kèm tuỳ chọn tự đóng dải banner quảng cáo dán dưới trình phát.

Bạn cứ xem YouTube như bình thường — không cần mở app này lên.

## Cách hoạt động

Android **không cho phép** một ứng dụng bấm vào giao diện của ứng dụng khác bằng
code Dart thuần. Cơ chế hợp lệ duy nhất là **AccessibilityService** — chính là
thứ các app đọc màn hình cho người khiếm thị đang dùng — và người dùng phải tự
tay bật nó trong *Cài đặt > Trợ năng*.

Vì vậy dự án gồm hai nửa:

| Lớp | Việc của nó |
|-----|-------------|
| Kotlin (`android/app/src/main/kotlin/com/hung/adskipper/`) | Lắng nghe màn hình YouTube, tìm nút "Bỏ qua", bấm hộ |
| Flutter (`lib/`) | Giao diện: bật/tắt, thống kê, nhật ký, tuỳ chỉnh |

Hai bên nói chuyện qua `MethodChannel` (`com.hung.adskipper/control`) và
`EventChannel` (`com.hung.adskipper/events`).

### Thứ tự nhận diện nút

1. **Theo resource-id** (`skip_ad_button`, `ad_skip_button`, …) — chính xác nhất
   và không phụ thuộc ngôn ngữ của máy.
2. **Theo nhãn chữ** — so khớp chính xác với danh sách ~30 ngôn ngữ
   ("bỏ qua", "skip ad", "広告をスキップ", …). So khớp *chính xác* chứ không
   phải "chứa", để không bấm nhầm lúc nút còn đang đếm ngược ("Bỏ qua sau 3").
3. **Nút X của banner quảng cáo** — nếu bạn bật tuỳ chọn này.

Khi tìm thấy, app thử `ACTION_CLICK` trên node hoặc view cha gần nhất bấm được;
nếu YouTube không cho (chữ nằm trong TextView không click được) thì mới chạm
trực tiếp vào toạ độ nút bằng `dispatchGesture()`.

### Tắt tiếng đoạn quảng cáo chưa cho bỏ qua

YouTube thường bắt xem 5 giây rồi mới cho bấm "Bỏ qua". Đoạn đó không có gì để
bấm, nên app **tắt tiếng luồng nhạc** thay vì ngồi chịu trận:

- Nhận biết "đang trong quảng cáo" qua id của các thành phần chỉ có trên trình
  phát khi chạy quảng cáo (`ad_progress_text`, `ad_badge`, `player_learn_more_button`…),
  hoặc nhãn "Được tài trợ"/"Sponsored" **nằm ngoài danh sách cuộn được** — để
  video được tài trợ trong bảng tin không làm mất tiếng oan.
- Tắt bằng `ADJUST_MUTE` (hệ thống tự nhớ mức âm lượng cũ); máy nào chặn thì lùi
  về cách nhớ mức cũ rồi kéo về 0.
- **Luôn** bật tiếng lại: hết quảng cáo, rời YouTube, tắt công tắc, dừng dịch
  vụ — và có chốt chặn cuối cùng tự mở tiếng sau 90 giây phòng khi nhận diện sai.

Tắt được tuỳ chọn này trong *Cài đặt > Tắt tiếng khi quảng cáo đang chạy*.

### Có chặn đứt quảng cáo luôn được không?

**Không** — và không app ngoài nào làm được trên máy chưa root. Lý do: YouTube
phát quảng cáo từ cùng máy chủ và cùng luồng video với nội dung chính
(`googlevideo.com`), nên chặn DNS/hosts là vô hiệu; muốn bỏ hẳn thì phải sửa
chính app YouTube. App này đứng ngoài, chỉ "nhìn" được màn hình và bấm hộ.

Nên thực tế chỉ còn ba đường: YouTube Premium, dùng một client YouTube khác, hoặc
đúng cách app này đang làm — tắt tiếng đoạn đầu rồi bỏ qua ngay khi có thể.

### Những gì app **không bao giờ** làm

- Không bấm "Truy cập trang của nhà quảng cáo", "Cài đặt", "Mở ứng dụng",
  "Tìm hiểu thêm"… — các nhãn này nằm trong danh sách chặn cứng
  (`SkipTargets.BLOCKED_LABELS`).
- Không đọc, không lưu, không gửi đi bất kỳ nội dung nào trên màn hình. Service
  chỉ được hệ thống cho phép nhận sự kiện từ đúng các app YouTube
  (`android:packageNames` trong `accessibility_service_config.xml`).
- Không đụng tới mạng — app không xin quyền INTERNET ở bản release.

## Yêu cầu

- Flutter SDK 3.22 trở lên (Dart 3.4+)
- Android SDK 35, JDK 17
- Máy Android 7.0 (API 24) trở lên — cần API 24 cho `dispatchGesture()`

## Build

```bash
flutter pub get
flutter run                 # cắm máy thật vào rồi chạy
# hoặc
flutter build apk --release # file nằm ở build/app/outputs/flutter-apk/
```

> Bản release đang ký bằng khoá debug cho tiện thử. Trước khi phát hành thật,
> hãy tạo keystore riêng và sửa `signingConfig` trong `android/app/build.gradle`.

## Cài đặt trên máy

1. Cài APK, mở app.
2. Bấm **"Mở cài đặt Trợ năng"** → *Ứng dụng đã tải xuống* → **Bỏ Qua Quảng Cáo**
   → bật lên. (Một số máy Xiaomi/Oppo/Vivo còn hỏi thêm một lần xác nhận.)
3. Bấm **"Tối ưu pin"** và chọn *Không tối ưu hoá* cho app, nếu không hệ thống
   có thể tự tắt dịch vụ sau một lúc.
4. Mở YouTube và xem bình thường.

Mỗi lần bấm được, app ghi lại một dòng trong mục **Hoạt động gần đây** — đó là
cách nhanh nhất để kiểm chứng nó đang chạy thật.

## Nghe YouTube khi "tắt màn hình"

Bản YouTube miễn phí dừng phát khi màn hình tắt thật. Thay vào đó app phủ một
lớp đen (overlay trợ năng) ở độ sáng thấp nhất: YouTube tưởng màn hình vẫn bật
nên phát tiếp, quảng cáo vẫn được bỏ qua bên dưới. Chạm 2 lần để bỏ lớp phủ.

Bật bằng nút **"Màn hình đen"** trong thanh Cài đặt nhanh (kéo thanh thông báo
xuống). Android không tự gắn nút đó, nên:

- **Android 13+**: mở app > mục *Hướng dẫn* > bấm **Thêm nút "Màn hình đen"**,
  hệ thống sẽ hỏi và tự gắn.
- **Android 12 trở xuống**: kéo thanh thông báo xuống hết cỡ, bấm biểu tượng
  **bút chì** (Samsung: dấu ba chấm > *Chỉnh sửa nút*), rồi kéo nút "Màn hình
  đen" từ danh sách bên dưới lên bảng.

Nút chỉ hoạt động khi quyền Trợ năng đã bật — chưa bật thì nó mở app lên.

## Máy báo "Cài đặt hạn chế" / "Ứng dụng đã bị từ chối cấp quyền"

Từ **Android 13**, hệ thống chặn mọi app cài ngoài Play Store bật quyền Trợ năng
— vì đây đúng là quyền mà phần mềm độc hại hay lạm dụng. App nào tải bằng file
APK cũng dính, không riêng app này. Mở khoá một lần:

1. **Samsung**: *Cài đặt > Bảo mật và quyền riêng tư > Chặn tự động (Auto
   Blocker)* → tắt. Hãng khác bỏ qua bước này.
2. *Cài đặt > Ứng dụng > Bỏ Qua Quảng Cáo* → bấm **dấu ba chấm ⋮** góc trên bên
   phải → **"Cho phép cài đặt bị hạn chế"**.
3. Quay lại *Cài đặt > Trợ năng* và bật app lên.

Không thấy mục đó (hay gặp trên Android 14+)? Cắm máy vào máy tính và chạy một
trong hai lệnh:

```bash
# mở khoá trực tiếp
adb shell appops set com.hung.adskipper ACCESS_RESTRICTED_SETTINGS allow

# hoặc cài lại và khai báo nguồn cài là Play Store
adb install -i com.android.vending -r build/app/outputs/flutter-apk/app-release.apk
```

Cách chắc ăn nhất khi phát triển là chạy thẳng `flutter run` rồi dùng lệnh
`appops` ở trên — khỏi phải đụng tới Auto Blocker.

## Tuỳ chỉnh (trong màn hình Cài đặt)

- **Chu kỳ quét màn hình** (mặc định 400 ms): nhỏ hơn thì bấm nhanh hơn, tốn pin hơn.
- **Nghỉ giữa hai lần bấm** (mặc định 800 ms): tránh bấm chồng lên nhau.
- **Nhãn nút "Bỏ qua"**: nếu YouTube đổi chữ, bạn tự thêm nhãn mới ngay trong app,
  không cần chờ bản cập nhật.
- **Ứng dụng được theo dõi**: YouTube, YouTube Music, YouTube TV.
- **Tắt tiếng khi quảng cáo đang chạy** (mặc định bật) và danh sách nhãn dùng để
  nhận biết quảng cáo.

## Giới hạn cần biết

- Quảng cáo **không có nút "Bỏ qua"** (loại 6–15 giây bắt buộc xem) thì không bỏ
  qua được — nhưng vẫn được tắt tiếng cho tới lúc hết.
- Đây **không phải** trình chặn quảng cáo: quảng cáo vẫn tải về và vẫn phát vài
  giây đầu, app chỉ bấm bỏ qua giúp bạn.
- YouTube đổi giao diện là chuyện thường. Nếu một hôm app "hết ăn", vào
  `SkipTargets.kt` thêm resource-id mới, hoặc thêm nhãn ngay trong màn hình Cài đặt.
- Google Play hạn chế app dùng AccessibilityService cho mục đích không phải trợ
  năng, nên bản này hợp với việc tự cài (sideload) cho riêng mình hơn là đăng lên
  Play Store.

## Cấu trúc thư mục

```
lib/
  main.dart
  src/
    app.dart                    # khung app + ControllerScope
    theme.dart
    models/                     # SkipSettings, SkipEvent
    services/
      ad_skipper_channel.dart   # cầu nối sang Android
      app_controller.dart       # trạng thái toàn app
    pages/                      # home_page, settings_page
    widgets/                    # status_card, stats_row, activity_list, ...
    utils/formatting.dart
android/app/src/main/kotlin/com/hung/adskipper/
  AdSkipperService.kt           # AccessibilityService — phần lõi
  AdMuter.kt                    # tắt/mở tiếng lúc quảng cáo chạy
  SkipTargets.kt                # id/nhãn của nút, danh sách chặn
  SkipSettings.kt               # lưu cấu hình
  SkipLog.kt                    # nhật ký + phát sự kiện
  MainActivity.kt               # MethodChannel / EventChannel
test/ad_skipper_test.dart
```

## Kiểm thử

```bash
flutter analyze
flutter test
```
