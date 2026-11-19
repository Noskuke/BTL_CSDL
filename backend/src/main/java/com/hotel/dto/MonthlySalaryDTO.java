package com.hotel.dto;

import java.math.BigDecimal;

public interface MonthlySalaryDTO {
    Integer getMaNhanVien();
    String getHoTen();
    String getCccd();
    String getSoDienThoai();
    String getChucVu();
    BigDecimal getLuongCoBan();
    Long getSoLanNhanTraPhong();
    Long getSoLanHoTro();
    BigDecimal getTamTinh();
    BigDecimal getTongLuong();
}
