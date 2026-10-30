# Hệ thống quản lý khách sạn

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

## 3. Tạo dữ liệu demo
Trong thư mục gốc dự án, cài thư viện Python cần thiết:

```bash
pip install mysql-connector-python
```

Sau khi đã chạy SQL tạo bảng, tạo dữ liệu demo lần đầu bằng:

```bash
python scripts/generate_data.py --reset
```


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
