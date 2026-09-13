package com.hung.adskipper

/**
 * Mọi thứ "biết về YouTube" được gom vào đây để khi Google đổi giao diện
 * thì chỉ cần sửa một file (hoặc sửa ngay trong app qua màn hình Cài đặt).
 */
object SkipTargets {

    /** Các app sẽ được theo dõi mặc định. */
    val DEFAULT_PACKAGES = listOf(
        "com.google.android.youtube",              // YouTube
        "com.google.android.apps.youtube.music",   // YouTube Music
        "com.google.android.apps.youtube.unplugged", // YouTube TV
        "com.google.android.youtube.tv",           // YouTube trên Android TV
    )

    /**
     * Resource-id của nút "Bỏ qua". Đây là cách nhận diện chắc chắn nhất vì
     * không phụ thuộc ngôn ngữ máy. Tên package sẽ được ghép vào lúc chạy.
     */
    val SKIP_VIEW_IDS = listOf(
        "skip_ad_button",
        "ad_skip_button",
        "skip_ad_button_text",
        "player_learn_more_button_skip",
    )

    /**
     * Nhãn của nút "Bỏ qua" theo nhiều ngôn ngữ. So khớp không phân biệt hoa
     * thường, sau khi đã bỏ dấu câu ở hai đầu.
     */
    val DEFAULT_SKIP_LABELS = listOf(
        "bỏ qua",
        "bỏ qua quảng cáo",
        "skip",
        "skip ad",
        "skip ads",
        "skip advert",
        "skip advertisement",
        "anuncio",
        "omitir",
        "omitir anuncio",
        "passer",
        "passer l'annonce",
        "ignorar",
        "ignorar anúncio",
        "werbung überspringen",
        "überspringen",
        "lewati",
        "lewati iklan",
        "ข้ามโฆษณา",
        "ข้าม",
        "広告をスキップ",
        "スキップ",
        "광고 건너뛰기",
        "건너뛰기",
        "跳过广告",
        "跳过",
        "略過廣告",
        "略過",
        "пропустить",
        "пропустить рекламу",
        "تخطي الإعلان",
        "تخطي",
    )

    /**
     * Id của những thành phần chỉ xuất hiện khi TRÌNH PHÁT đang chạy quảng cáo.
     * Dùng để biết "đang trong quảng cáo" kể cả khi nút Bỏ qua chưa hiện — nhờ
     * vậy mới tắt tiếng được đoạn 5 giây đầu.
     */
    val AD_MARKER_VIEW_IDS = listOf(
        "ad_progress_text",
        "ad_countdown",
        "ad_countdown_text",
        "ad_attribution",
        "ad_badge",
        "ad_overlay",
        "ad_cta_button",
        "ad_cta_button_view",
        "player_learn_more_button",
        "skip_ad_button",
        "ad_skip_button",
    )

    /**
     * Nhãn "Được tài trợ" / "Sponsored". Chỉ được tin khi nó KHÔNG nằm trong
     * một danh sách cuộn được — bảng tin YouTube cũng có video được tài trợ, mà
     * lướt bảng tin thì không có gì để tắt tiếng cả.
     */
    val DEFAULT_AD_MARKER_LABELS = listOf(
        "được tài trợ",
        "quảng cáo",
        "sponsored",
        "ad",
        "ads",
        "advertisement",
        "anuncio",
        "publicidad",
        "publicité",
        "annonce",
        "werbung",
        "anúncio",
        "iklan",
        "реклама",
        "إعلان",
        "โฆษณา",
        "広告",
        "광고",
        "广告",
        "廣告",
    )

    /**
     * Resource-id của nút X đóng banner quảng cáo dán ở đáy trình phát.
     *
     * Chỉ giữ id có chữ "ad": "close_button" chung chung chính là nút X của
     * trình phát thu nhỏ — bấm vào là đóng luôn video người dùng đang xem.
     */
    val CLOSE_VIEW_IDS = listOf(
        "ad_close_button",
        "ad_dismiss_button",
    )

    /** Nhãn / content-description của nút đóng banner quảng cáo. */
    val DEFAULT_CLOSE_LABELS = listOf(
        "đóng",
        "đóng quảng cáo",
        "close",
        "close ad",
        "dismiss",
        "dismiss ad",
        "cerrar",
        "fermer",
        "schließen",
        "закрыть",
        "閉じる",
        "关闭",
        "닫기",
    )

    /**
     * Nhãn tuyệt đối KHÔNG được bấm. Đây là các nút dẫn tới trang quảng cáo
     * hoặc cửa hàng ứng dụng — bấm nhầm còn tệ hơn là để nguyên quảng cáo.
     */
    val BLOCKED_LABELS = listOf(
        "truy cập trang của nhà quảng cáo",
        "visit advertiser",
        "visit advertiser site",
        "visit site",
        "tìm hiểu thêm",
        "learn more",
        "cài đặt",
        "install",
        "mở ứng dụng",
        "open app",
        "đặt ngay",
        "shop now",
        "mua ngay",
        "buy now",
        "đăng ký",
        "sign up",
        "subscribe",
        "tải xuống",
        "download",
    )
}
