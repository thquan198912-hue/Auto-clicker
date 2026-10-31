# Tự Động Chạm (AutoClicker)

Ứng dụng Android tự động lặp lại thao tác **chạm** hoặc **vuốt** trên màn hình.
Dùng Dịch vụ trợ năng (Accessibility Service) để gửi thao tác, không cần root.

- Android 7.0 (API 24) trở lên
- Thanh điều khiển nổi: ▶ Bắt đầu / ■ Dừng, đổi kiểu Chạm ↔ Vuốt, ✕ ẩn thanh
- Chấm **A** (điểm chạm hoặc điểm đầu vuốt) và chấm **B** (điểm cuối vuốt) kéo thả được
- Tuỳ chỉnh: khoảng nghỉ (ms), số lần lặp (0 = mãi mãi), thời gian mỗi lần vuốt (ms)
- Không cần cấp quyền "hiển thị trên ứng dụng khác"; không dùng Internet

## Build ra file APK

### Cách 1: GitHub Actions (không cần cài gì)
1. Tạo repo GitHub mới, tải toàn bộ thư mục này lên (nhánh `main`).
2. Vào tab **Actions** → workflow **Build APK** (tự chạy khi push, hoặc bấm **Run workflow**).
3. Khi chạy xong, tải artifact **AutoClicker-debug-apk** → giải nén được `app-debug.apk`.

### Cách 2: Android Studio
1. Cài Android Studio, chọn **Open** và trỏ vào thư mục dự án, đợi Gradle đồng bộ.
2. Menu **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
3. File nằm ở `app/build/outputs/apk/debug/app-debug.apk`.

### Cách 3: Dòng lệnh
Cần JDK 17 và Android SDK (đặt `ANDROID_HOME`):

```
./gradlew assembleDebug
```

## Cài đặt và sử dụng
1. Chép APK vào điện thoại và cài (cho phép cài từ nguồn không xác định nếu được hỏi).
2. Mở ứng dụng → **Bật dịch vụ trợ năng** → chọn **Tự Động Chạm** → bật.
   - Android 13 trở lên: nếu công tắc bị mờ, vào **Thông tin ứng dụng** → dấu ⋮ → **Cho phép cài đặt bị hạn chế**, rồi bật lại dịch vụ.
3. Thanh điều khiển nổi sẽ hiện ra. Mở ứng dụng cần thao tác, kéo chấm A (và B) tới vị trí mong muốn.
4. Nhấn **▶ Bắt đầu**. Nhấn **■ Dừng** để dừng bất cứ lúc nào.
5. Đã nhấn ✕ để ẩn thanh? Mở lại ứng dụng và nhấn **Lưu và hiện thanh điều khiển**.

## Lưu ý
- Khi đang chạy, các chấm mờ đi và "xuyên thấu" để thao tác lọt xuống ứng dụng bên dưới.
- Nếu xoay màn hình, hãy kéo lại vị trí các chấm.
- Một số ứng dụng (ngân hàng, màn hình bảo mật…) chặn thao tác từ dịch vụ trợ năng.
- Chỉ dùng cho mục đích hợp lệ; một số game và dịch vụ cấm tự động hoá trong điều khoản sử dụng.
- Đây là bản debug (ký bằng khoá debug), phù hợp để dùng cá nhân.

## Cấu trúc
```
app/src/main/java/com/example/autoclicker/
  AutoClickService.kt   dịch vụ trợ năng, thanh nổi, vòng lặp chạm/vuốt
  MainActivity.kt       màn hình cài đặt
  Prefs.kt              lưu cài đặt và vị trí chấm
app/src/main/res/        giao diện, chuỗi tiếng Việt, cấu hình dịch vụ
.github/workflows/build.yml   build APK tự động
```
