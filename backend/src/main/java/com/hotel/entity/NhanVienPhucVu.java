package com.hotel.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "NhanVienPhucVu")
public class NhanVienPhucVu {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaLuotPhucVu")
    private Integer maLuotPhucVu;

    @ManyToOne
    @JoinColumn(name = "MaLuotThue", nullable = false)
    private ThuePhong thuePhong;

    @ManyToOne
    @JoinColumn(name = "MaNhanVien", nullable = false)
    private NhanVien nhanVien;

    @Column(name = "LoaiCongViec", nullable = false)
    private String loaiCongViec;

    @Column(name = "ThoiGianThucHien", nullable = false)
    private LocalDateTime thoiGianThucHien;

    public Integer getMaLuotPhucVu() { return maLuotPhucVu; }
    public void setMaLuotPhucVu(Integer maLuotPhucVu) { this.maLuotPhucVu = maLuotPhucVu; }
    public ThuePhong getThuePhong() { return thuePhong; }
    public void setThuePhong(ThuePhong thuePhong) { this.thuePhong = thuePhong; }
    public NhanVien getNhanVien() { return nhanVien; }
    public void setNhanVien(NhanVien nhanVien) { this.nhanVien = nhanVien; }
    public String getLoaiCongViec() { return loaiCongViec; }
    public void setLoaiCongViec(String loaiCongViec) { this.loaiCongViec = loaiCongViec; }
    public LocalDateTime getThoiGianThucHien() { return thoiGianThucHien; }
    public void setThoiGianThucHien(LocalDateTime thoiGianThucHien) { this.thoiGianThucHien = thoiGianThucHien; }
}