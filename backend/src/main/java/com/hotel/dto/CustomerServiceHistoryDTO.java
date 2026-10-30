package com.hotel.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface CustomerServiceHistoryDTO {
    Integer getMaLuotThue();
    String getTenDichVu();
    String getLoaiDichVu();
    Integer getSoLuong();
    BigDecimal getDonGiaThoiDiem();
    BigDecimal getThanhTien();
    LocalDateTime getThoiGianSuDung();
}