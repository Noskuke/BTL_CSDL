package com.hotel.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ThuePhong")
public class ThuePhong {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaLuotThue")
    private Integer maLuotThue;

    @ManyToOne
    @JoinColumn(name = "MaKhachHang", nullable = false)
    private KhachHang khachHang;

    @ManyToOne
    @JoinColumn(name = "MaPhong", nullable = false)
    private Phong phong;

    @Column(name = "NgayDat", nullable = false)
    private LocalDateTime ngayDat;

    @Column(name = "NgayNhanPhong", nullable = false)
    private LocalDateTime ngayNhanPhong;

    @Column(name = "NgayTraDuKien", nullable = false)
    private LocalDateTime ngayTraDuKien;

    @Column(name = "NgayTraThucTe")
    private LocalDateTime ngayTraThucTe;

    @Column(name = "TongTienPhong")
    private BigDecimal tongTienPhong;

    @Column(name = "TrangThaiDotThue")
    private String trangThaiDotThue;

    public Integer getMaLuotThue() { return maLuotThue; }
    public void setMaLuotThue(Integer maLuotThue) { this.maLuotThue = maLuotThue; }
    public KhachHang getKhachHang() { return khachHang; }
    public void setKhachHang(KhachHang khachHang) { this.khachHang = khachHang; }
    public Phong getPhong() { return phong; }
    public void setPhong(Phong phong) { this.phong = phong; }
    public LocalDateTime getNgayDat() { return ngayDat; }
    public void setNgayDat(LocalDateTime ngayDat) { this.ngayDat = ngayDat; }
    public LocalDateTime getNgayNhanPhong() { return ngayNhanPhong; }
    public void setNgayNhanPhong(LocalDateTime ngayNhanPhong) { this.ngayNhanPhong = ngayNhanPhong; }
    public LocalDateTime getNgayTraDuKien() { return ngayTraDuKien; }
    public void setNgayTraDuKien(LocalDateTime ngayTraDuKien) { this.ngayTraDuKien = ngayTraDuKien; }
    public LocalDateTime getNgayTraThucTe() { return ngayTraThucTe; }
    public void setNgayTraThucTe(LocalDateTime ngayTraThucTe) { this.ngayTraThucTe = ngayTraThucTe; }
    public BigDecimal getTongTienPhong() { return tongTienPhong; }
    public void setTongTienPhong(BigDecimal tongTienPhong) { this.tongTienPhong = tongTienPhong; }
    public String getTrangThaiDotThue() { return trangThaiDotThue; }
    public void setTrangThaiDotThue(String trangThaiDotThue) { this.trangThaiDotThue = trangThaiDotThue; }
}