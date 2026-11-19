package com.hotel.dto;

import java.time.LocalDateTime;

/** Dữ liệu dùng để tạo hoặc đổi lịch một lượt đặt phòng. */
public class BookingRequest {
    private Integer maKhachHang;
    private Integer maPhong;
    private LocalDateTime ngayNhanPhong;
    private LocalDateTime ngayTraDuKien;

    public Integer getMaKhachHang() {
        return maKhachHang;
    }

    public void setMaKhachHang(Integer maKhachHang) {
        this.maKhachHang = maKhachHang;
    }

    public Integer getMaPhong() {
        return maPhong;
    }

    public void setMaPhong(Integer maPhong) {
        this.maPhong = maPhong;
    }

    public LocalDateTime getNgayNhanPhong() {
        return ngayNhanPhong;
    }

    public void setNgayNhanPhong(LocalDateTime ngayNhanPhong) {
        this.ngayNhanPhong = ngayNhanPhong;
    }

    public LocalDateTime getNgayTraDuKien() {
        return ngayTraDuKien;
    }

    public void setNgayTraDuKien(LocalDateTime ngayTraDuKien) {
        this.ngayTraDuKien = ngayTraDuKien;
    }
}
