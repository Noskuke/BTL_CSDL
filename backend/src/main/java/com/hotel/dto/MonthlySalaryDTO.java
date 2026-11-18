package com.hotel.dto;

import java.math.BigDecimal;

public interface MonthlySalaryDTO {
    Integer getMaNhanVien();
    String getHoTen();
    String getChucVu();
    BigDecimal getLuongCoBan();
    Long getSoLanNhanTraPhong();
    Long getSoLanHoTro();
    BigDecimal getTongLuong();
}