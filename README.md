# Madomi Reader

Ứng dụng đọc truyện tranh / manhwa / manga cho Android, viết bằng **Kotlin + Jetpack Compose**,
có **nhận diện chữ (OCR) chạy ngay trên máy** và **dịch sang tiếng Việt bằng Google Gemini**,
rồi vẽ bản dịch đè lên từng bong bóng thoại trong trang truyện.

## Tính năng

- **Danh mục truyện**: lưới 2–3–4 cột, ảnh bìa, tên truyện, tác giả, nhãn trạng thái.
- **Tìm kiếm & lọc**: theo từ khoá (tên / tác giả / mô tả), theo nguồn và theo thể loại.
- **Kiến trúc nguồn cắm được (`MangaSource`)**: thêm nguồn mới chỉ cần viết một class và
  đăng ký trong `SourceRegistry`, không phải sửa UI. Kèm 2 nguồn demo:
  - `offline-demo`: trang truyện được **vẽ tại chỗ** (không cần mạng) để thử luồng OCR + dịch.
  - `online-demo`: trang truyện tải từ mạng (kiểm tra Coil, cache, thử lại khi lỗi).
- **Lịch sử đọc** (lưu vị trí trang đang đọc, đọc tiếp trong 1 chạm) và **truyện đã lưu**.
- **Danh sách chương**: đánh dấu chương đã đọc, sắp xếp tăng/giảm, nút "đọc tiếp".
- **Trình đọc**: 2 chế độ — cuộn dọc (webtoon/manhwa) và lật từng trang (manga);
  chạm giữa màn hình để ẩn/hiện thanh điều khiển, chạm 2 bên để sang trang;
  chuyển chương trước/sau ngay trong trình đọc; giữ màn hình sáng khi đang đọc.
- **OCR + dịch từng trang**: bitmap trang → ML Kit Text Recognition v2 (Latin / Trung / Nhật / Hàn,
  chữ ngang và chữ dọc) → gom các dòng thành từng khung thoại → Gemini dịch sang tiếng Việt
  (thoại tự nhiên, xưng hô đúng quan hệ nhân vật) → che chữ gốc bằng nền lấy màu từ chính ảnh
  → vẽ bản dịch vào giữa khung, tự thu nhỏ cỡ chữ cho vừa.
- **Dịch cả chương** một lượt, có thanh tiến độ và nút huỷ.
- **Cài đặt**: Gemini API key (có nút kiểm tra kết nối), model (Flash / Flash Lite),
  ngôn ngữ gốc (tự động / Anh / Trung / Nhật / Hàn), kiểu nền lớp dịch, chế độ đọc mặc định,
  số cột lưới, cỡ chữ bản dịch, tự động dịch, xoá dữ liệu đọc.

## Kiến trúc

MVVM + Clean Architecture, Coroutines/Flow, không dùng Hilt (DI thủ công qua `AppContainer`).

```
app/src/main/java/com/example/madomireader/
├── MangaReaderApp.kt            Application: AppContainer + cấu hình Coil
├── MainActivity.kt              Activity duy nhất, edge-to-edge, Compose
├── core/util/                   BitmapUtils, TaskExt (awaitTask cho ML Kit)
├── di/AppContainer.kt           Nơi tạo và giữ mọi phụ thuộc
├── domain/
│   ├── model/                   Manga, Chapter, Page, AppSettings, Translation…
│   ├── ocr/                     OcrEngine (ML Kit), TextBlockGrouper (thuần JVM, có unit test)
│   └── translate/TranslationService.kt
├── data/
│   ├── local/                   SettingsRepository, HistoryRepository (DataStore)
│   ├── remote/                  GeminiTranslator, PageBitmapLoader, PlaceholderPageRenderer
│   ├── repository/MangaRepository.kt
│   └── source/                  MangaSource + SourceRegistry + 2 nguồn demo
└── ui/
    ├── theme/                   Màu, typography, theme Material 3
    ├── components/              Thẻ truyện, dòng lịch sử, dòng chương, chip, ô tìm kiếm…
    ├── home/                    Màn hình Khám phá / Lịch sử / Đã lưu
    ├── chapters/                Danh sách chương
    ├── reader/                  Trình đọc + lớp dịch (TranslationOverlay)
    ├── settings/                Cài đặt
    └── navigation/AppNavHost.kt Route của toàn ứng dụng
```

## Cấu hình

- minSdk 26, targetSdk 35, compileSdk 35, JDK 17.
- AGP 8.7.3, Kotlin 2.0.21, Gradle 8.11.1, Compose BOM 2024.10.01.
- Thư viện: Compose Material 3, Navigation Compose, Coil 2.7, OkHttp 4.12, Gson 2.11,
  DataStore Preferences 1.1.1, ML Kit Text Recognition v2 16.0.1 (model nhúng, chạy offline).

## Lấy Gemini API key

1. Mở https://aistudio.google.com/app/apikey (cần tài khoản Google).
2. Tạo API key miễn phí rồi sao chép.
3. Trong app: **Cài đặt → Gemini AI → dán key → Kiểm tra**.
   Key chỉ được lưu trên máy (DataStore), không gửi đi đâu ngoài Google.

## Build APK bằng GitHub Actions (không cần máy tính)

1. Tạo repository trên GitHub rồi push toàn bộ thư mục này lên.
2. Workflow `.github/workflows/build-apk.yml` tự chạy ở mỗi lần push
   (JDK 17 → Gradle 8.11.1 → `./gradlew assembleDebug`).
3. Vào tab **Actions → Build APK → lần chạy mới nhất → Artifacts**,
   tải `MadomiReader-debug-apk`, giải nén được `MadomiReader-debug.apk`.
4. Cài file APK đó lên điện thoại (cho phép "cài từ nguồn không xác định").

Chạy test (thuật toán gom khung thoại) trên CI: `./gradlew testDebugUnitTest`.

## Thêm nguồn truyện mới

```kotlin
class MangaDexSource : MangaSource {
    override val id = "mangadex"
    override val name = "MangaDex"
    override val description = "Nguồn truyện quốc tế"
    override val defaultLanguage = SourceLanguage.ENGLISH
    override fun genres() = listOf("Action", "Romance")
    override suspend fun browse(page: Int, query: String?, genre: String?): List<Manga> = TODO()
    override suspend fun details(mangaId: String): MangaDetails = TODO()
    override suspend fun chapters(mangaId: String): List<Chapter> = TODO()
    override suspend fun pages(chapterId: String): List<Page> = TODO()
}
```

rồi thêm một dòng vào `AppContainer`:

```kotlin
val sourceRegistry = SourceRegistry(listOf(OfflineDemoSource(), OnlineDemoSource(), MangaDexSource()))
```

## Ghi chú

- Bản dịch chỉ giữ trong bộ nhớ trong lúc đọc (không ghi ra đĩa), tránh phình dung lượng.
- Trang ảnh được cache đĩa 256 MB qua Coil và cache HTTP 64 MB qua OkHttp.
- Ứng dụng chỉ dùng cho mục đích cá nhân; hãy tôn trọng bản quyền của nội dung bạn đọc.
