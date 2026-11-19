USE QuanLyKhachSan;

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
        ) THEN 'DANGSUDUNG'
        WHEN EXISTS (
            SELECT 1 FROM ThuePhong t
            WHERE t.MaPhong = p.MaPhong
              AND t.TrangThaiDotThue = 'DADAT'
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
        SET MESSAGE_TEXT = 'BOOKING_OVERLAP';
    END IF;
END$$

DROP TRIGGER IF EXISTS trg_CheckOverlappingBookingUpdate$$
CREATE TRIGGER trg_CheckOverlappingBookingUpdate
BEFORE UPDATE ON ThuePhong
FOR EACH ROW
BEGIN
    IF OLD.TrangThaiDotThue = 'DADAT'
       AND NEW.TrangThaiDotThue = 'HUY'
       AND NOW() > DATE_SUB(OLD.NgayNhanPhong, INTERVAL 7 DAY) THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'BOOKING_CANCEL_WINDOW_7_DAYS';
    END IF;

    IF OLD.TrangThaiDotThue = 'DADAT'
       AND NEW.TrangThaiDotThue = 'DADAT'
       AND (OLD.MaPhong <> NEW.MaPhong
            OR OLD.NgayNhanPhong <> NEW.NgayNhanPhong
            OR OLD.NgayTraDuKien <> NEW.NgayTraDuKien)
       AND NOW() > DATE_SUB(OLD.NgayNhanPhong, INTERVAL 7 DAY) THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'BOOKING_CHANGE_WINDOW_7_DAYS';
    END IF;

    IF NEW.TrangThaiDotThue IN ('DADAT', 'DANGO') AND EXISTS (
        SELECT 1 FROM ThuePhong t
        WHERE t.MaPhong = NEW.MaPhong
          AND t.MaLuotThue <> NEW.MaLuotThue
          AND t.TrangThaiDotThue <> 'HUY'
          AND NEW.NgayNhanPhong < COALESCE(t.NgayTraThucTe, t.NgayTraDuKien)
          AND NEW.NgayTraDuKien > t.NgayNhanPhong
    ) THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'BOOKING_OVERLAP';
    END IF;
END$$

DELIMITER ;
