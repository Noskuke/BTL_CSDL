package com.hotel.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "SuDungDichVu")
public class SuDungDichVu {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaLuotDung")
    private Integer maLuotDung;

    @ManyToOne
    @JoinColumn(name = "MaLuotThue", nullable = false)
    private ThuePhong thuePhong;

    @ManyToOne
    @JoinColumn(name = "MaDichVu", nullable = false)
    private DichVu dichVu;

    @ManyToOne
    @JoinColumn(name = "MaNhanVienPhucVu")
    private NhanVien nhanVienPhucVu;

    @Column(name = "SoLuong", nullable = false)
    private Integer soLuong;

    @Column(name = "DonGiaThoiDiem", nullable = false)
    private BigDecimal donGiaThoiDiem;

    @Column(name = "ThoiGianSuDung", nullable = false)
    private LocalDateTime thoiGianSuDung;

    @Column(name = "ThanhTien", insertable = false, updatable = false)
    private BigDecimal thanhTien;

    public Integer getMaLuotDung() { return maLuotDung; }
    public void setMaLuotDung(Integer maLuotDung) { this.maLuotDung = maLuotDung; }
    public ThuePhong getThuePhong() { return thuePhong; }
    public void setThuePhong(ThuePhong thuePhong) { this.thuePhong = thuePhong; }
    public DichVu getDichVu() { return dichVu; }
    public void setDichVu(DichVu dichVu) { this.dichVu = dichVu; }
    public NhanVien getNhanVienPhucVu() { return nhanVienPhucVu; }
    public void setNhanVienPhucVu(NhanVien nhanVienPhucVu) { this.nhanVienPhucVu = nhanVienPhucVu; }
    public Integer getSoLuong() { return soLuong; }
    public void setSoLuong(Integer soLuong) { this.soLuong = soLuong; }
    public BigDecimal getDonGiaThoiDiem() { return donGiaThoiDiem; }
    public void setDonGiaThoiDiem(BigDecimal donGiaThoiDiem) { this.donGiaThoiDiem = donGiaThoiDiem; }
    public LocalDateTime getThoiGianSuDung() { return thoiGianSuDung; }
    public void setThoiGianSuDung(LocalDateTime thoiGianSuDung) { this.thoiGianSuDung = thoiGianSuDung; }
    public BigDecimal getThanhTien() { return thanhTien; }
}