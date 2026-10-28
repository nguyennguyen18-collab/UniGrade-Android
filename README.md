# UniGrade Android (Java / SQLite)

Ứng dụng quản lý điểm UEF – giao diện thiết kế lại theo mockup PDF (sinh viên + Ban Quản Trị).
Giao diện nằm hoàn toàn trong `res/` (layout, drawable, styles, strings); Java chỉ gán dữ liệu.

## Mở project
1. Giải nén ZIP → Android Studio > Open > chọn `UniGrade_Android`.
2. Gradle Sync, chạy trên emulator/thiết bị (JDK 17, AGP 8.6.1, minSdk 24).
3. **Cài lại app (hoặc xóa dữ liệu app)** vì CSDL đã nâng lên version 2 (schema mới; bản cũ sẽ bị nạp lại dữ liệu mẫu).

## Tài khoản demo (mật khẩu 123456)
- Sinh viên: `SV2024001` … `SV2024005`
- Quản trị: `GV9901`
- Nút "Đăng Nhập Nhanh Trải Nghiệm Demo" ở màn hình đăng nhập vào thẳng 2 vai trò; ở chế độ này mới hiện nút chuyển qua lại giữa giao diện Sinh viên ↔ Quản trị.

## Màn hình
**Đăng nhập**: chọn vai trò, ghi nhớ MSSV, quên mật khẩu, đăng nhập nhanh demo.

**Sinh viên** (5 tab): Trang chủ (GPA, tín chỉ, điểm mới) · Bảng điểm (tìm kiếm, lọc học kỳ, sắp xếp, xuất PDF) · Học phần (lịch học, phòng, giảng viên, chuyên cần, đề cương) · Thông báo (tất cả/chưa đọc, đánh dấu đã đọc) · Tài khoản (hồ sơ, đổi mật khẩu, bật/tắt thông báo điểm, đăng xuất).

**Quản trị** (5 tab + hồ sơ): Tổng quan (số liệu, phổ điểm) · Sinh viên (tìm kiếm, lọc khoa, thêm/sửa/xóa, xem chi tiết) · Môn học (tỷ lệ điểm CC/BT/GK/CK, thêm/sửa/xóa) · Nhập điểm (tự tính hệ 10/hệ 4/điểm chữ) · Lịch sử thay đổi điểm (audit trail). Chạm avatar để mở Hồ sơ cán bộ (đổi mật khẩu, khôi phục dữ liệu mẫu).

## Cấu trúc mã
**Giao diện (XML, thư mục `app/src/main/res`)**
- `layout/activity_login.xml`, `activity_shell.xml` – đăng nhập và khung (thanh trên + thanh điều hướng dưới)
- `layout/screen_student_*.xml` – trang chủ, bảng điểm, học phần, thông báo, tài khoản (sinh viên)
- `layout/screen_admin_*.xml` – tổng quan, sinh viên, môn học, nhập điểm, lịch sử, hồ sơ cán bộ (quản trị)
- `layout/item_*.xml` – thẻ lặp trong danh sách (điểm, học phần, thông báo, sinh viên, môn học, lịch sử, chip, dòng hành động...)
- `layout/dialog_*.xml` – hộp thoại thêm/sửa sinh viên, môn học, đổi mật khẩu
- `layout/view_segment2.xml`, `view_select.xml` – thanh 2 tab và ô chọn, dùng lại bằng `<include>`
- `values/colors.xml`, `styles.xml`, `strings.xml`, `arrays.xml` – màu, kiểu (Card, Chip, Btn, Input...), toàn bộ chữ giao diện
- `drawable/bg_*.xml` – nền bo góc, viền, gradient, ripple, selector; `drawable/ic_*.xml` – icon vector; `color/*.xml` – selector màu chữ

**Mã Java (`app/src/main/java/com/uef/unigrade`)**
- `ShellActivity` – nạp `activity_shell`, đổi tab; `Screen` – lớp cha của mỗi màn hình
- `StudentDashboardActivity` + `StudentScreens`, `AdminDashboardActivity` + `AdminScreens` – inflate layout rồi gán dữ liệu/sự kiện
- `Ui.java` – hàm dùng chung (gán avatar, màu điểm chữ, thanh tab, chip, ô chọn, định dạng giờ)
- `DatabaseHelper.java` + `Models.java` – SQLite (Student, Subject, Grade, GradeHistory, CourseClass, Enrollment, Notification, Staff, UserAccount)

Muốn đổi màu/cỡ chữ/khoảng cách: sửa `colors.xml`, `styles.xml` hoặc file layout tương ứng, không cần sửa Java.

## Lưu ý
- Điểm tổng kết tính theo tỷ lệ riêng của từng môn (mặc định 10/20/20/50), làm tròn 2 chữ số.
- Sửa điểm đã có bắt buộc nhập lý do; mỗi lần lưu ghi vào lịch sử và tạo thông báo cho sinh viên (nếu sinh viên bật nhận thông báo).
- Thêm sinh viên mới sẽ tự tạo tài khoản với mật khẩu mặc định `123456`.
- Avatar hiển thị chữ cái đầu (chưa có ảnh thật); logo UEF là chữ "UEF" vẽ bằng code – thay bằng ảnh logo thật nếu cần.
- Mật khẩu hiện lưu SHA-256 không salt (mức demo). Dữ liệu chỉ lưu cục bộ trên thiết bị.
- Chữ cố định trong dữ liệu mẫu (tên, môn, thông báo) và thông báo lỗi kiểm tra dữ liệu trong `DatabaseHelper` vẫn nằm trong Java.
- Mã đã được kiểm tra tĩnh nhưng **chưa build được trong môi trường này** (không có Android SDK); hãy Sync/Build trong Android Studio và báo lại nếu có lỗi biên dịch.
# UniGrade-Android
