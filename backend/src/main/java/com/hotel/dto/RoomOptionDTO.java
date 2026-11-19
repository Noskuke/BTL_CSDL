package com.hotel.dto;

import java.math.BigDecimal;

/** Phòng và giá dùng trong form đặt phòng. */
public class RoomOptionDTO {
    private Integer maPhong;
    private String soPhong;
    private String tenLoaiPhong;
    private BigDecimal donGia;
    private String moTaChatLuong;
    private String thietKe;

    public RoomOptionDTO(Integer maPhong, String soPhong, String tenLoaiPhong, BigDecimal donGia,
                         String moTaChatLuong, String thietKe) {
        this.maPhong = maPhong;
        this.soPhong = soPhong;
        this.tenLoaiPhong = tenLoaiPhong;
        this.donGia = donGia;
        this.moTaChatLuong = moTaChatLuong;
        this.thietKe = thietKe;
    }

    public Integer getMaPhong() { return maPhong; }
    public String getSoPhong() { return soPhong; }
    public String getTenLoaiPhong() { return tenLoaiPhong; }
    public BigDecimal getDonGia() { return donGia; }
    public String getMoTaChatLuong() { return moTaChatLuong; }
    public String getThietKe() { return thietKe; }
}
