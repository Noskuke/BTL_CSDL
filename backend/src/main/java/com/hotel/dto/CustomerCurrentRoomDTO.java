package com.hotel.dto;

import java.time.LocalDateTime;

public interface CustomerCurrentRoomDTO {
    String getSoPhong();
    String getTenLoaiPhong();
    LocalDateTime getTuNgay();
    LocalDateTime getNgayTraDuKien();
}