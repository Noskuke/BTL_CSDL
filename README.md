# Hệ thống quản lý khách sạn

## Chạy nhanh bằng Docker (khuyến nghị)
Yêu cầu Docker Desktop đang chạy. Từ thư mục dự án:

```bash
docker compose -p hoteldemo up --build -d
```

Docker Compose dùng project riêng `hoteldemo`, tự tạo MySQL, chạy schema và dữ liệu mẫu trên volume mới, rồi khởi động backend và frontend:
- Frontend: `http://localhost:8081`
- Backend API: `http://localhost:8080/api/hotel`
- MySQL Workbench: `127.0.0.1:3307`, database `QuanLyKhachSan`, user `hotel_demo`, password `hotel_demo_password`
- Đăng nhập frontend: tài khoản `khang`, mật khẩu `galaxy2026`

Kiểm tra container bằng `docker compose -p hoteldemo ps`; xem log bằng `docker compose -p hoteldemo logs -f`. Dừng ứng dụng bằng `docker compose -p hoteldemo down`. Dữ liệu MySQL được giữ lại trong volume sau khi dừng. **Không dùng `docker compose -p hoteldemo down -v` nếu muốn giữ dữ liệu.** Các file SQL khởi tạo chỉ được áp dụng tự động khi volume MySQL được tạo lần đầu.

Thông tin đăng nhập mặc định chỉ dành cho chạy demo trên máy cá nhân. Có thể đặt `MYSQL_ROOT_PASSWORD`, `MYSQL_USER`, `MYSQL_PASSWORD`, `MYSQL_PORT`, `BACKEND_PORT` và `FRONTEND_PORT` trong môi trường trước khi chạy Compose.

## Công nghệ
- MySQL 8.0 trở lên
- Java 25 và Maven (theo `backend/pom.xml`)
- Spring Boot backend, cổng 8080
- Frontend HTML/JavaScript

## 1. Tạo cơ sở dữ liệu
1. Mở MySQL Workbench hoặc công cụ MySQL tương đương.
2. Mở và chạy `database/QuanLyKhachSan.sql`.
3. **Cảnh báo:** script có `DROP TABLE`; chạy lại sẽ xóa dữ liệu hiện có trong các bảng được tạo lại. Hãy sao lưu trước nếu cần giữ dữ liệu.

## 2. Cấu hình kết nối
Mở `backend/src/main/resources/application.properties` và chỉnh `spring.datasource.username` / `spring.datasource.password` theo MySQL trên máy của bạn. Mặc định hiện tại là `root` / `123456`.

## 3. Tạo dữ liệu demo (không cần kết nối MySQL)
Trong thư mục gốc dự án:

```bash
py scripts/generate_data.py --export-sql
```

Lệnh trên tạo `database/QuanLyKhachSan_data.sql` ngoại tuyến, không cần cài MySQL Connector hoặc đăng nhập MySQL. Mở file dữ liệu này trong MySQL Workbench và chạy sau khi đã chạy `database/QuanLyKhachSan.sql` trên một cơ sở dữ liệu trống. Có thể chỉ định đường dẫn khác bằng `--export-sql PATH` hoặc đổi seed bằng `--seed 123`.

Nếu muốn nạp trực tiếp vào MySQL thay vì xuất file, cài thư viện và chạy:

```bash
pip install mysql-connector-python
python scripts/generate_data.py --reset
```

Chế độ kết nối trực tiếp dùng `DB_HOST`, `DB_PORT`, `DB_USER` và `DB_PASSWORD` nếu các biến môi trường này được đặt; mặc định là `localhost:3306`, `root` / `123456`.

## 4. Chạy backend
Từ thư mục `backend`:

```bash
mvn spring-boot:run
```

API gốc: `http://localhost:8080/api/hotel`.

## 5. Mở frontend
Mở `frontend/index.html` bằng trình duyệt. Nếu trình duyệt hạn chế gọi API khi mở bằng `file://`, chạy frontend qua máy chủ web tĩnh hoặc extension Live Server trong VS Code.

## Các đồng bộ chính trong bản cập nhật
- Đã thay `database/QuanLyKhachSan.sql` bằng SQL chỉnh sửa mới.
- Mặc định tháng ở các API tần suất phòng và lương là tháng hiện tại nếu không truyền tham số.

## Lưu ý kiểm thử
Cần chạy MySQL và backend trên máy đích để xác nhận kết nối thực tế. Bản đóng gói này không kèm các file build cũ trong `backend/target` hoặc thư mục công cụ `.github/modernize` không cần thiết cho việc chạy demo, nhằm giảm dung lượng và tránh nhầm lẫn với mã nguồn hiện hành.


### Lương thực nhận

Cột `Thực nhận` chỉ hiển thị lương của tháng đã kết thúc; tháng hiện tại và các tháng tương lai hiển thị `0`.
