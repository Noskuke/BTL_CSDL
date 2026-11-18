package com.hotel.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface CustomerProfileDTO {
    Integer getMaLuotThue();
    String getSoPhong();
    String getTenLoaiPhong();
    LocalDateTime getNgayNhanPhong();
    LocalDateTime getNgayTraThucTe();
    String getTrangThaiDotThue();
    BigDecimal getTongTienPhong();
}