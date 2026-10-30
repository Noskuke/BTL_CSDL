package com.hotel.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "Phong")
public class Phong {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaPhong")
    private Integer maPhong;

    @Column(name = "SoPhong", unique = true, nullable = false)
    private String soPhong;

    @ManyToOne
    @JoinColumn(name = "MaLoaiPhong", nullable = false)
    private LoaiPhong loaiPhong;

    @Column(name = "TrangThai")
    private String trangThai;

    public Integer getMaPhong() { return maPhong; }
    public void setMaPhong(Integer maPhong) { this.maPhong = maPhong; }
    public String getSoPhong() { return soPhong; }
    public void setSoPhong(String soPhong) { this.soPhong = soPhong; }
    public LoaiPhong getLoaiPhong() { return loaiPhong; }
    public void setLoaiPhong(LoaiPhong loaiPhong) { this.loaiPhong = loaiPhong; }
    public String getTrangThai() { return trangThai; }
    public void setTrangThai(String trangThai) { this.trangThai = trangThai; }
}