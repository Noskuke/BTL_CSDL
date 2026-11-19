package com.hotel.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Dữ liệu hiển thị một lượt đặt phòng cho giao diện quản lý. */
public class BookingDTO {
    private Integer maLuotThue;
    private Integer maKhachHang;
    private String tenKhachHang;
    private Integer maPhong;
    private String soPhong;
    private String tenLoaiPhong;
    private LocalDateTime ngayDat;
    private LocalDateTime ngayNhanPhong;
    private LocalDateTime ngayTraDuKien;
    private LocalDateTime ngayTraThucTe;
    private BigDecimal tongTienPhong;
    private String trangThaiDotThue;
    private boolean choPhepThayDoi;
    private boolean choPhepHuy;

    public Integer getMaLuotThue() { return maLuotThue; }
    public void setMaLuotThue(Integer value) { maLuotThue = value; }
    public Integer getMaKhachHang() { return maKhachHang; }
    public void setMaKhachHang(Integer value) { maKhachHang = value; }
    public String getTenKhachHang() { return tenKhachHang; }
    public void setTenKhachHang(String value) { tenKhachHang = value; }
    public Integer getMaPhong() { return maPhong; }
    public void setMaPhong(Integer value) { maPhong = value; }
    public String getSoPhong() { return soPhong; }
    public void setSoPhong(String value) { soPhong = value; }
    public String getTenLoaiPhong() { return tenLoaiPhong; }
    public void setTenLoaiPhong(String value) { tenLoaiPhong = value; }
    public LocalDateTime getNgayDat() { return ngayDat; }
    public void setNgayDat(LocalDateTime value) { ngayDat = value; }
    public LocalDateTime getNgayNhanPhong() { return ngayNhanPhong; }
    public void setNgayNhanPhong(LocalDateTime value) { ngayNhanPhong = value; }
    public LocalDateTime getNgayTraDuKien() { return ngayTraDuKien; }
    public void setNgayTraDuKien(LocalDateTime value) { ngayTraDuKien = value; }
    public LocalDateTime getNgayTraThucTe() { return ngayTraThucTe; }
    public void setNgayTraThucTe(LocalDateTime value) { ngayTraThucTe = value; }
    public BigDecimal getTongTienPhong() { return tongTienPhong; }
    public void setTongTienPhong(BigDecimal value) { tongTienPhong = value; }
    public String getTrangThaiDotThue() { return trangThaiDotThue; }
    public void setTrangThaiDotThue(String value) { trangThaiDotThue = value; }
    public boolean isChoPhepThayDoi() { return choPhepThayDoi; }
    public void setChoPhepThayDoi(boolean value) { choPhepThayDoi = value; }
    public boolean isChoPhepHuy() { return choPhepHuy; }
    public void setChoPhepHuy(boolean value) { choPhepHuy = value; }
}
