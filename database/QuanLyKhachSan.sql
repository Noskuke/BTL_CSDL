CREATE DATABASE IF NOT EXISTS QuanLyKhachSan CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE QuanLyKhachSan;


-- ===== 1. TẠO BẢNG & RÀNG BUỘC =====
DROP TABLE IF EXISTS NhanVienPhucVu, SuDungDichVu, ThuePhong, DichVu, Phong, LoaiPhong, KhachHang, NhanVien;

CREATE TABLE NhanVien (
    MaNhanVien   INT PRIMARY KEY AUTO_INCREMENT,
    CCCD         VARCHAR(12) UNIQUE NOT NULL,
    HoTen        VARCHAR(100) NOT NULL,
    SoDienThoai  VARCHAR(15),
    ChucVu       VARCHAR(50),
    LuongCoBan   DECIMAL(12,2) NOT NULL,
    CONSTRAINT chk_luong_coban CHECK (LuongCoBan > 0)
);

CREATE TABLE KhachHang (
    MaKhachHang INT PRIMARY KEY AUTO_INCREMENT,
    CCCD        VARCHAR(12) UNIQUE NOT NULL,
    HoTen       VARCHAR(100) NOT NULL,
    SoDienThoai VARCHAR(15)
);

CREATE TABLE LoaiPhong (
    MaLoaiPhong  INT PRIMARY KEY AUTO_INCREMENT,
    TenLoaiPhong VARCHAR(50) NOT NULL,
    SoLuong      INT NOT NULL,
    DonGia       DECIMAL(12,2) NOT NULL,
    CONSTRAINT chk_soluong_phong CHECK (SoLuong > 0),
    CONSTRAINT chk_dongia_phong CHECK (DonGia > 0)
);

CREATE TABLE Phong (
    MaPhong      INT PRIMARY KEY AUTO_INCREMENT,
    SoPhong      VARCHAR(10) UNIQUE NOT NULL,
    MaLoaiPhong  INT NOT NULL,
    TrangThai    ENUM('TRONG', 'DANGSUDUNG', 'DATTRUOC') DEFAULT 'TRONG',
    FOREIGN KEY (MaLoaiPhong) REFERENCES LoaiPhong(MaLoaiPhong)
);

CREATE TABLE DichVu (
    MaDichVu   INT PRIMARY KEY AUTO_INCREMENT,
    TenDichVu  VARCHAR(100) NOT NULL,
    LoaiDichVu VARCHAR(50),
    DonGia     DECIMAL(12,2) NOT NULL,
    CONSTRAINT chk_dongia_dv CHECK (DonGia > 0)
);

CREATE TABLE ThuePhong (
    MaLuotThue         INT PRIMARY KEY AUTO_INCREMENT,
    MaKhachHang        INT NOT NULL,
    MaPhong            INT NOT NULL,
    NgayDat            DATETIME NOT NULL,
    NgayNhanPhong      DATETIME NOT NULL,
    NgayTraDuKien      DATETIME NOT NULL,
    NgayTraThucTe      DATETIME,
    TongTienPhong      DECIMAL(12,2) DEFAULT 0,
    TrangThaiDotThue   ENUM('DADAT', 'DANGO', 'DATRA', 'HUY') DEFAULT 'DADAT',
    FOREIGN KEY (MaKhachHang) REFERENCES KhachHang(MaKhachHang),
    FOREIGN KEY (MaPhong)     REFERENCES Phong(MaPhong),
    INDEX idx_thue_phong_thoigian (MaPhong, NgayNhanPhong, NgayTraDuKien),
    CONSTRAINT chk_ngay_nhan CHECK (NgayNhanPhong >= NgayDat),
    CONSTRAINT chk_ngay_tra_dk CHECK (NgayTraDuKien > NgayNhanPhong),
    CONSTRAINT chk_ngay_tra_tt CHECK (NgayTraThucTe IS NULL OR NgayTraThucTe > NgayNhanPhong)
);

CREATE TABLE SuDungDichVu (
    MaLuotDung         INT PRIMARY KEY AUTO_INCREMENT,
    MaLuotThue         INT NOT NULL,
    MaDichVu           INT NOT NULL,
    MaNhanVienPhucVu   INT,
    SoLuong            INT NOT NULL DEFAULT 1,
    DonGiaThoiDiem     DECIMAL(12,2) NOT NULL,
    ThoiGianSuDung     DATETIME NOT NULL,
    ThanhTien          DECIMAL(12,2) GENERATED ALWAYS AS (SoLuong * DonGiaThoiDiem) STORED,
    FOREIGN KEY (MaLuotThue)        REFERENCES ThuePhong(MaLuotThue),
    FOREIGN KEY (MaDichVu)          REFERENCES DichVu(MaDichVu),
    FOREIGN KEY (MaNhanVienPhucVu) REFERENCES NhanVien(MaNhanVien),
    CONSTRAINT chk_soluong_dv CHECK (SoLuong > 0),
    CONSTRAINT chk_dongia_td CHECK (DonGiaThoiDiem > 0)
);

CREATE TABLE NhanVienPhucVu (
    MaLuotPhucVu       INT PRIMARY KEY AUTO_INCREMENT,
    MaLuotThue         INT NOT NULL,
    MaNhanVien         INT NOT NULL,
    LoaiCongViec       ENUM('CHECKIN', 'CHECKOUT', 'HO_TRO') NOT NULL,
    ThoiGianThucHien   DATETIME NOT NULL,
    FOREIGN KEY (MaLuotThue) REFERENCES ThuePhong(MaLuotThue),
    FOREIGN KEY (MaNhanVien) REFERENCES NhanVien(MaNhanVien)
);


-- ===== 2. TRIGGER & STORED PROCEDURE NGHIỆP VỤ =====
DELIMITER $$

DROP PROCEDURE IF EXISTS sp_CapNhatTrangThaiPhong$$
CREATE PROCEDURE sp_CapNhatTrangThaiPhong(IN p_MaPhong INT)
BEGIN
    UPDATE Phong p
    SET p.TrangThai = CASE
        WHEN EXISTS (
            SELECT 1 FROM ThuePhong t
            WHERE t.MaPhong = p.MaPhong
              AND t.TrangThaiDotThue = 'DANGO'
              AND t.NgayNhanPhong <= NOW()
              AND NOW() < COALESCE(t.NgayTraThucTe, t.NgayTraDuKien)
        ) THEN 'DANGSUDUNG'
        WHEN EXISTS (
            SELECT 1 FROM ThuePhong t
            WHERE t.MaPhong = p.MaPhong
              AND t.TrangThaiDotThue = 'DADAT'
              AND t.NgayNhanPhong > NOW()
              AND t.NgayTraDuKien > NOW()
        ) THEN 'DATTRUOC'
        ELSE 'TRONG'
    END
    WHERE p.MaPhong = p_MaPhong;
END$$

DROP TRIGGER IF EXISTS trg_CheckOverlappingBooking$$
CREATE TRIGGER trg_CheckOverlappingBooking
BEFORE INSERT ON ThuePhong
FOR EACH ROW
BEGIN
    IF NEW.TrangThaiDotThue <> 'HUY' AND EXISTS (
        SELECT 1 FROM ThuePhong t
        WHERE t.MaPhong = NEW.MaPhong
          AND t.TrangThaiDotThue <> 'HUY'
          AND NEW.NgayNhanPhong < COALESCE(t.NgayTraThucTe, t.NgayTraDuKien)
          AND NEW.NgayTraDuKien > t.NgayNhanPhong
    ) THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Lỗi nghiệp vụ: Phòng đã có khách đặt/ở trong khoảng thời gian này!';
    END IF;
END$$

DROP TRIGGER IF EXISTS trg_CheckOverlappingBookingUpdate$$
CREATE TRIGGER trg_CheckOverlappingBookingUpdate
BEFORE UPDATE ON ThuePhong
FOR EACH ROW
BEGIN
    IF NEW.TrangThaiDotThue IN ('DADAT', 'DANGO') AND EXISTS (
        SELECT 1 FROM ThuePhong t
        WHERE t.MaPhong = NEW.MaPhong
          AND t.MaLuotThue <> NEW.MaLuotThue
          AND t.TrangThaiDotThue <> 'HUY'
          AND NEW.NgayNhanPhong < COALESCE(t.NgayTraThucTe, t.NgayTraDuKien)
          AND NEW.NgayTraDuKien > t.NgayNhanPhong
    ) THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Lỗi nghiệp vụ: Phòng đã có khách đặt/ở trong khoảng thời gian này!';
    END IF;
END$$

DROP TRIGGER IF EXISTS trg_UpdateRoomStatusInsert$$
CREATE TRIGGER trg_UpdateRoomStatusInsert
AFTER INSERT ON ThuePhong
FOR EACH ROW
BEGIN
    CALL sp_CapNhatTrangThaiPhong(NEW.MaPhong);
END$$

DROP TRIGGER IF EXISTS trg_UpdateRoomStatus$$
CREATE TRIGGER trg_UpdateRoomStatus
AFTER UPDATE ON ThuePhong
FOR EACH ROW
BEGIN
    CALL sp_CapNhatTrangThaiPhong(NEW.MaPhong);
    IF OLD.MaPhong <> NEW.MaPhong THEN
        CALL sp_CapNhatTrangThaiPhong(OLD.MaPhong);
    END IF;
END$$

-- Khi xoa mot luot thue, tinh lai trang thai phong tuong ung.
DROP TRIGGER IF EXISTS trg_UpdateRoomStatusDelete$$
CREATE TRIGGER trg_UpdateRoomStatusDelete
AFTER DELETE ON ThuePhong
FOR EACH ROW
BEGIN
    CALL sp_CapNhatTrangThaiPhong(OLD.MaPhong);
END$$

DELIMITER ;


-- ===== 3. THIẾT LẬP THAM SỐ THỜI GIAN =====
SET @ma_kh     = 1;
SET @thoi_diem = NOW();

-- Khoảng báo cáo: 12 tháng gần nhất.
-- @den_ngay là mốc kết thúc KHÔNG bao gồm, tránh mất dữ liệu trong ngày cuối.
SET @tu_ngay   = DATE_FORMAT(NOW() - INTERVAL 11 MONTH, '%Y-%m-01');
SET @den_ngay  = DATE_FORMAT(NOW() + INTERVAL 1 MONTH, '%Y-%m-01');

-- Tháng hiện tại dùng cho truy vấn tần suất và tính lương.
SET @thang     = DATE_FORMAT(NOW(), '%Y-%m');
SET @thang_tu  = STR_TO_DATE(CONCAT(@thang, '-01'), '%Y-%m-%d');
SET @thang_den = DATE_ADD(@thang_tu, INTERVAL 1 MONTH);


-- ===== 4. CÁC CÂU TRUY VẤN BÁO CÁO =====

-- 1. HỒ SƠ KHÁCH HÀNG 
SELECT t.MaLuotThue, p.SoPhong, lp.TenLoaiPhong, t.NgayNhanPhong, t.NgayTraThucTe,
       t.TrangThaiDotThue, t.TongTienPhong
FROM ThuePhong t
JOIN Phong p      ON p.MaPhong = t.MaPhong
JOIN LoaiPhong lp ON lp.MaLoaiPhong = p.MaLoaiPhong
WHERE t.MaKhachHang = @ma_kh
ORDER BY t.NgayNhanPhong DESC;

SELECT t.MaLuotThue, d.TenDichVu, d.LoaiDichVu, s.SoLuong, s.DonGiaThoiDiem,
       s.ThanhTien, s.ThoiGianSuDung
FROM SuDungDichVu s
JOIN ThuePhong t ON t.MaLuotThue = s.MaLuotThue
JOIN DichVu d    ON d.MaDichVu = s.MaDichVu
WHERE t.MaKhachHang = @ma_kh
ORDER BY s.ThoiGianSuDung DESC;

SELECT p.SoPhong, lp.TenLoaiPhong, t.NgayNhanPhong AS TuNgay, t.NgayTraDuKien
FROM ThuePhong t
JOIN Phong p      ON p.MaPhong = t.MaPhong
JOIN LoaiPhong lp ON lp.MaLoaiPhong = p.MaLoaiPhong
WHERE t.MaKhachHang = @ma_kh AND t.TrangThaiDotThue = 'DANGO';

SELECT d.TenDichVu, SUM(s.SoLuong) AS SoLuong, SUM(s.ThanhTien) AS TongTien
FROM ThuePhong t
JOIN SuDungDichVu s ON s.MaLuotThue = t.MaLuotThue
JOIN DichVu d       ON d.MaDichVu = s.MaDichVu
WHERE t.MaKhachHang = @ma_kh AND t.TrangThaiDotThue = 'DANGO'
GROUP BY d.MaDichVu, d.TenDichVu;


-- 2. TRẠNG THÁI CÁC PHÒNG TẠI MỘT THỜI ĐIỂM
SELECT p.SoPhong AS soPhong, lp.TenLoaiPhong AS tenLoaiPhong,
       CASE
         WHEN EXISTS (
             SELECT 1 FROM ThuePhong t
             WHERE t.MaPhong = p.MaPhong
               AND t.TrangThaiDotThue IN ('DANGO', 'DATRA')
               AND t.NgayNhanPhong <= @thoi_diem
               AND @thoi_diem < COALESCE(t.NgayTraThucTe, t.NgayTraDuKien)
         ) THEN 'Đang sử dụng'
         WHEN EXISTS (
             SELECT 1 FROM ThuePhong t
             WHERE t.MaPhong = p.MaPhong
               AND t.TrangThaiDotThue = 'DADAT'
               AND t.NgayNhanPhong > @thoi_diem
               AND @thoi_diem < t.NgayTraDuKien
         ) THEN 'Đã đặt trước'
         ELSE 'Đang trống'
       END AS trangThai
FROM Phong p
JOIN LoaiPhong lp ON lp.MaLoaiPhong = p.MaLoaiPhong
ORDER BY p.SoPhong;


-- 3. DOANH THU THEO KHOẢNG THỜI GIAN 
SELECT Thang AS thang, SUM(TienPhong) AS tienPhong, SUM(TienDichVu) AS tienDichVu, SUM(TienPhong) + SUM(TienDichVu) AS tongDoanhThu
FROM (
    SELECT DATE_FORMAT(NgayTraThucTe, '%Y-%m') AS Thang, TongTienPhong AS TienPhong, 0 AS TienDichVu
    FROM ThuePhong WHERE TrangThaiDotThue = 'DATRA' AND NgayTraThucTe >= @tu_ngay AND NgayTraThucTe < @den_ngay
    UNION ALL
    SELECT DATE_FORMAT(ThoiGianSuDung, '%Y-%m'), 0, ThanhTien
    FROM SuDungDichVu WHERE ThoiGianSuDung >= @tu_ngay AND ThoiGianSuDung < @den_ngay
) x GROUP BY Thang ORDER BY Thang;

SELECT lp.TenLoaiPhong AS tenLoaiPhong, SUM(t.TongTienPhong) AS doanhThuPhong
FROM ThuePhong t
JOIN Phong p      ON p.MaPhong = t.MaPhong
JOIN LoaiPhong lp ON lp.MaLoaiPhong = p.MaLoaiPhong
WHERE t.TrangThaiDotThue = 'DATRA' AND t.NgayTraThucTe >= @tu_ngay AND t.NgayTraThucTe < @den_ngay
GROUP BY lp.MaLoaiPhong, lp.TenLoaiPhong ORDER BY doanhThuPhong DESC;

SELECT d.LoaiDichVu AS loaiDichVu, SUM(s.ThanhTien) AS doanhThuDichVu
FROM SuDungDichVu s
JOIN DichVu d ON d.MaDichVu = s.MaDichVu
WHERE s.ThoiGianSuDung >= @tu_ngay AND s.ThoiGianSuDung < @den_ngay
GROUP BY d.LoaiDichVu ORDER BY doanhThuDichVu DESC;


-- 4. TẦN SUẤT SỬ DỤNG VÀ SẮP XẾP LƯỢT THUÊ GIẢM DẦN TRONG THÁNG
SELECT lp.TenLoaiPhong AS tenLoaiPhong, COUNT(t.MaLuotThue) AS soLuotThue
FROM LoaiPhong lp
LEFT JOIN Phong p     ON p.MaLoaiPhong = lp.MaLoaiPhong
LEFT JOIN ThuePhong t ON t.MaPhong = p.MaPhong
    AND t.TrangThaiDotThue <> 'HUY'
    AND t.NgayNhanPhong >= @thang_tu
    AND t.NgayNhanPhong < @thang_den
GROUP BY lp.MaLoaiPhong, lp.TenLoaiPhong
ORDER BY soLuotThue DESC;

SELECT
    d.MaDichVu AS maDichVu,
    d.TenDichVu AS tenDichVu,
    d.LoaiDichVu AS loaiDichVu,
    COUNT(s.MaLuotDung) AS soLanDung,
    COALESCE(SUM(s.SoLuong), 0) AS tongSoLuong
FROM DichVu d
LEFT JOIN SuDungDichVu s
    ON s.MaDichVu = d.MaDichVu
    AND s.ThoiGianSuDung >= @thang_tu
    AND s.ThoiGianSuDung < @thang_den
GROUP BY d.MaDichVu, d.TenDichVu, d.LoaiDichVu
ORDER BY soLanDung DESC;


-- 5. KHÁCH HÀNG THÂN THIẾT 
WITH thue AS (
    SELECT MaKhachHang, COUNT(*) AS SoLanThue, SUM(COALESCE(TongTienPhong, 0)) AS TienPhong
    FROM ThuePhong WHERE TrangThaiDotThue <> 'HUY' GROUP BY MaKhachHang
),
dv AS (
    SELECT t.MaKhachHang, SUM(COALESCE(s.ThanhTien, 0)) AS TienDichVu
    FROM SuDungDichVu s JOIN ThuePhong t ON t.MaLuotThue = s.MaLuotThue
    WHERE t.TrangThaiDotThue <> 'HUY' GROUP BY t.MaKhachHang
),
phong_thich AS (
    SELECT MaKhachHang, TenLoaiPhong FROM (
        SELECT t.MaKhachHang, lp.TenLoaiPhong, ROW_NUMBER() OVER (PARTITION BY t.MaKhachHang ORDER BY COUNT(*) DESC, lp.MaLoaiPhong) AS rn
        FROM ThuePhong t JOIN Phong p ON p.MaPhong = t.MaPhong JOIN LoaiPhong lp ON lp.MaLoaiPhong = p.MaLoaiPhong
        WHERE t.TrangThaiDotThue <> 'HUY' GROUP BY t.MaKhachHang, lp.MaLoaiPhong, lp.TenLoaiPhong
    ) a WHERE rn = 1
),
dv_thich AS (
    SELECT MaKhachHang, TenDichVu FROM (
        SELECT t.MaKhachHang, d.TenDichVu, ROW_NUMBER() OVER (PARTITION BY t.MaKhachHang ORDER BY COUNT(*) DESC, d.MaDichVu) AS rn
        FROM SuDungDichVu s JOIN ThuePhong t ON t.MaLuotThue = s.MaLuotThue JOIN DichVu d ON d.MaDichVu = s.MaDichVu
        GROUP BY t.MaKhachHang, d.MaDichVu, d.TenDichVu
    ) b WHERE rn = 1
)
SELECT kh.MaKhachHang AS maKhachHang, kh.HoTen AS hoTen, th.SoLanThue AS soLanThue,
       COALESCE(th.TienPhong, 0) + COALESCE(dv.TienDichVu, 0) AS tongThanhToan,
       COALESCE(pt.TenLoaiPhong, 'Chưa có') AS loaiPhongUaChuong,
       COALESCE(dt.TenDichVu, 'Chưa sử dụng') AS dichVuUaChuong
FROM KhachHang kh
JOIN thue th             ON th.MaKhachHang = kh.MaKhachHang
LEFT JOIN dv             ON dv.MaKhachHang = kh.MaKhachHang
LEFT JOIN phong_thich pt ON pt.MaKhachHang = kh.MaKhachHang
LEFT JOIN dv_thich dt    ON dt.MaKhachHang = kh.MaKhachHang
WHERE th.SoLanThue > 3 OR (COALESCE(th.TienPhong, 0) + COALESCE(dv.TienDichVu, 0)) > 20000000
ORDER BY tongThanhToan DESC;


-- 6. TÍNH TỔNG LƯƠNG NHẬN TRONG THÁNG CỦA TỪNG NHÂN VIÊN
SELECT nv.MaNhanVien AS maNhanVien, nv.HoTen AS hoTen, nv.ChucVu AS chucVu, nv.LuongCoBan AS luongCoBan,
       COUNT(CASE WHEN pv.LoaiCongViec IN ('CHECKIN', 'CHECKOUT') THEN 1 END) AS soLanNhanTraPhong,
       COUNT(CASE WHEN pv.LoaiCongViec = 'HO_TRO' THEN 1 END)                 AS soLanHoTro,
       nv.LuongCoBan
         + 100000 * COUNT(CASE WHEN pv.LoaiCongViec IN ('CHECKIN', 'CHECKOUT') THEN 1 END)
         + 200000 * COUNT(CASE WHEN pv.LoaiCongViec = 'HO_TRO' THEN 1 END)     AS tongLuong
FROM NhanVien nv
LEFT JOIN NhanVienPhucVu pv ON pv.MaNhanVien = nv.MaNhanVien AND DATE_FORMAT(pv.ThoiGianThucHien, '%Y-%m') = @thang
GROUP BY nv.MaNhanVien, nv.HoTen, nv.ChucVu, nv.LuongCoBan
ORDER BY tongLuong DESC;