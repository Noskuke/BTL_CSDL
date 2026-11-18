package com.hotel.dto;

import java.util.List;
import java.util.Map;

public class CustomerFullProfileDTO {
    private Integer maKhachHang;
    private String hoTen;
    private List<Map<String, Object>> lichSuThuePhong;
    private List<Map<String, Object>> lichSuSuDungDichVu;
    private List<Map<String, Object>> phongDangO;
    private List<Map<String, Object>> dichVuDangDung;

    public Integer getMaKhachHang() { return maKhachHang; }
    public void setMaKhachHang(Integer maKhachHang) { this.maKhachHang = maKhachHang; }
    public String getHoTen() { return hoTen; }
    public void setHoTen(String hoTen) { this.hoTen = hoTen; }
    public List<Map<String, Object>> getLichSuThuePhong() { return lichSuThuePhong; }
    public void setLichSuThuePhong(List<Map<String, Object>> lichSuThuePhong) { this.lichSuThuePhong = lichSuThuePhong; }
    public List<Map<String, Object>> getLichSuSuDungDichVu() { return lichSuSuDungDichVu; }
    public void setLichSuSuDungDichVu(List<Map<String, Object>> lichSuSuDungDichVu) { this.lichSuSuDungDichVu = lichSuSuDungDichVu; }
    public List<Map<String, Object>> getPhongDangO() { return phongDangO; }
    public void setPhongDangO(List<Map<String, Object>> phongDangO) { this.phongDangO = phongDangO; }
    public List<Map<String, Object>> getDichVuDangDung() { return dichVuDangDung; }
    public void setDichVuDangDung(List<Map<String, Object>> dichVuDangDung) { this.dichVuDangDung = dichVuDangDung; }
}