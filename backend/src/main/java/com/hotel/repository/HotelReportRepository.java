package com.hotel.repository;

import com.hotel.dto.*;
import com.hotel.entity.KhachHang;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HotelReportRepository extends JpaRepository<KhachHang, Integer> {

    @Query(value = """
        SELECT kh.MaKhachHang FROM KhachHang kh
        WHERE (:type = 'id' AND CAST(kh.MaKhachHang AS CHAR) = :keyword)
           OR (:type = 'name' AND LOWER(kh.HoTen) LIKE LOWER(CONCAT('%', :keyword, '%')))
           OR (:type = 'phone' AND kh.SoDienThoai LIKE CONCAT('%', :keyword, '%'))
           OR (:type = 'cccd' AND kh.CCCD LIKE CONCAT('%', :keyword, '%'))
        LIMIT 1
    """, nativeQuery = true)
    List<Integer> findCustomerIdByCriteria(@Param("type") String type, @Param("keyword") String keyword);

    @Query(value = """
        SELECT p.SoPhong AS soPhong, lp.TenLoaiPhong AS tenLoaiPhong,
               CASE
                 WHEN EXISTS (SELECT 1 FROM ThuePhong t WHERE t.MaPhong = p.MaPhong AND t.TrangThaiDotThue IN ('DANGO', 'DATRA') AND t.NgayNhanPhong <= :thoiDiem AND :thoiDiem < COALESCE(t.NgayTraThucTe, t.NgayTraDuKien)) THEN 'Đang sử dụng'
                 WHEN EXISTS (SELECT 1 FROM ThuePhong t WHERE t.MaPhong = p.MaPhong AND t.TrangThaiDotThue = 'DADAT' AND t.NgayNhanPhong > :thoiDiem AND :thoiDiem < t.NgayTraDuKien) THEN 'Đã đặt trước'
                 ELSE 'Đang trống'
               END AS trangThai
        FROM Phong p JOIN LoaiPhong lp ON lp.MaLoaiPhong = p.MaLoaiPhong ORDER BY p.SoPhong
    """, nativeQuery = true)
    List<RoomStatusDTO> getRoomStatusAtTime(@Param("thoiDiem") String thoiDiem);

    @Query(value = """
        SELECT Thang AS thang, SUM(TienPhong) AS tienPhong, SUM(TienDichVu) AS tienDichVu, SUM(TienPhong) + SUM(TienDichVu) AS tongDoanhThu
        FROM (
            SELECT DATE_FORMAT(NgayTraThucTe, '%Y-%m') AS Thang, TongTienPhong AS TienPhong, 0 AS TienDichVu FROM ThuePhong WHERE TrangThaiDotThue = 'DATRA' AND NgayTraThucTe >= :tuNgay AND NgayTraThucTe < DATE_ADD(:denNgay, INTERVAL 1 DAY)
            UNION ALL
            SELECT DATE_FORMAT(ThoiGianSuDung, '%Y-%m'), 0, ThanhTien FROM SuDungDichVu WHERE ThoiGianSuDung >= :tuNgay AND ThoiGianSuDung < DATE_ADD(:denNgay, INTERVAL 1 DAY)
        ) x GROUP BY Thang ORDER BY Thang
    """, nativeQuery = true)
    List<RevenueReportDTO> getRevenueReport(@Param("tuNgay") String tuNgay, @Param("denNgay") String denNgay);

    @Query(value = """
        SELECT lp.TenLoaiPhong AS tenLoaiPhong, SUM(t.TongTienPhong) AS doanhThuPhong
        FROM ThuePhong t
        JOIN Phong p ON p.MaPhong = t.MaPhong
        JOIN LoaiPhong lp ON lp.MaLoaiPhong = p.MaLoaiPhong
        WHERE t.TrangThaiDotThue = 'DATRA'
          AND t.NgayTraThucTe >= :tuNgay AND t.NgayTraThucTe < DATE_ADD(:denNgay, INTERVAL 1 DAY)
        GROUP BY lp.MaLoaiPhong, lp.TenLoaiPhong
        ORDER BY doanhThuPhong DESC
    """, nativeQuery = true)
    List<RevenueByRoomTypeDTO> getRevenueByRoomType(@Param("tuNgay") String tuNgay, @Param("denNgay") String denNgay);

    @Query(value = """
        SELECT d.LoaiDichVu AS loaiDichVu, SUM(s.ThanhTien) AS doanhThuDichVu
        FROM SuDungDichVu s
        JOIN DichVu d ON d.MaDichVu = s.MaDichVu
        WHERE s.ThoiGianSuDung >= :tuNgay AND s.ThoiGianSuDung < DATE_ADD(:denNgay, INTERVAL 1 DAY)
        GROUP BY d.LoaiDichVu
        ORDER BY doanhThuDichVu DESC
    """, nativeQuery = true)
    List<RevenueByServiceTypeDTO> getRevenueByServiceType(@Param("tuNgay") String tuNgay, @Param("denNgay") String denNgay);

    @Query(value = """
        SELECT lp.TenLoaiPhong AS tenLoaiPhong, COUNT(t.MaLuotThue) AS soLuotThue
        FROM LoaiPhong lp
        LEFT JOIN Phong p ON p.MaLoaiPhong = lp.MaLoaiPhong
        LEFT JOIN ThuePhong t ON t.MaPhong = p.MaPhong AND t.TrangThaiDotThue <> 'HUY' AND DATE_FORMAT(t.NgayNhanPhong, '%Y-%m') = :thang
        GROUP BY lp.MaLoaiPhong, lp.TenLoaiPhong ORDER BY soLuotThue DESC
    """, nativeQuery = true)
    List<RoomFrequencyDTO> getRoomFrequencyMonthly(@Param("thang") String thang);

    @Query(value = """
        SELECT d.TenDichVu AS tenDichVu,
               d.LoaiDichVu AS loaiDichVu,
               COUNT(s.MaLuotDung) AS soLanDung,
               COALESCE(SUM(s.SoLuong), 0) AS tongSoLuong
        FROM DichVu d
        LEFT JOIN SuDungDichVu s
          ON s.MaDichVu = d.MaDichVu
         AND s.ThoiGianSuDung >= STR_TO_DATE(CONCAT(:thang, '-01'), '%Y-%m-%d')
         AND s.ThoiGianSuDung < DATE_ADD(
               STR_TO_DATE(CONCAT(:thang, '-01'), '%Y-%m-%d'),
               INTERVAL 1 MONTH
         )
        GROUP BY d.MaDichVu, d.TenDichVu, d.LoaiDichVu
        ORDER BY soLanDung DESC, d.TenDichVu
    """, nativeQuery = true)
    List<ServiceFrequencyDTO> getServiceFrequency(@Param("thang") String thang);

    @Query(value = """
        WITH thue AS (
            SELECT MaKhachHang, COUNT(*) AS SoLanThue, SUM(COALESCE(TongTienPhong, 0)) AS TienPhong
            FROM ThuePhong WHERE TrangThaiDotThue <> 'HUY' GROUP BY MaKhachHang
        ),
        dv AS (
            SELECT t.MaKhachHang, SUM(COALESCE(s.ThanhTien, 0)) AS TienDichVu
            FROM SuDungDichVu s JOIN ThuePhong t ON t.MaLuotThue = s.MaLuotThue
            WHERE t.TrangThaiDotThue <> 'HUY' GROUP BY t.MaKhachHang
        ),
        phong_thich AS (
            SELECT MaKhachHang, TenLoaiPhong FROM (
                SELECT t.MaKhachHang, lp.TenLoaiPhong, ROW_NUMBER() OVER (PARTITION BY t.MaKhachHang ORDER BY COUNT(*) DESC, lp.MaLoaiPhong) AS rn
                FROM ThuePhong t JOIN Phong p ON p.MaPhong = t.MaPhong JOIN LoaiPhong lp ON lp.MaLoaiPhong = p.MaLoaiPhong
                WHERE t.TrangThaiDotThue <> 'HUY' GROUP BY t.MaKhachHang, lp.MaLoaiPhong, lp.TenLoaiPhong
            ) a WHERE rn = 1
        ),
        dv_thich AS (
            SELECT MaKhachHang, TenDichVu FROM (
                SELECT t.MaKhachHang, d.TenDichVu, ROW_NUMBER() OVER (PARTITION BY t.MaKhachHang ORDER BY COUNT(*) DESC, d.MaDichVu) AS rn
                FROM SuDungDichVu s JOIN ThuePhong t ON t.MaLuotThue = s.MaLuotThue JOIN DichVu d ON d.MaDichVu = s.MaDichVu
                GROUP BY t.MaKhachHang, d.MaDichVu, d.TenDichVu
            ) b WHERE rn = 1
        )
        SELECT kh.MaKhachHang AS maKhachHang, kh.HoTen AS hoTen, th.SoLanThue AS soLanThue,
               COALESCE(th.TienPhong, 0) + COALESCE(dv.TienDichVu, 0) AS tongThanhToan,
               COALESCE(pt.TenLoaiPhong, 'Chưa có') AS loaiPhongUaChuong,
               COALESCE(dt.TenDichVu, 'Chưa sử dụng') AS dichVuUaChuong
        FROM KhachHang kh
        JOIN thue th ON th.MaKhachHang = kh.MaKhachHang
        LEFT JOIN dv ON dv.MaKhachHang = kh.MaKhachHang
        LEFT JOIN phong_thich pt ON pt.MaKhachHang = kh.MaKhachHang
        LEFT JOIN dv_thich dt ON dt.MaKhachHang = kh.MaKhachHang
        WHERE th.SoLanThue > 3 OR (COALESCE(th.TienPhong, 0) + COALESCE(dv.TienDichVu, 0)) > 20000000
        ORDER BY tongThanhToan DESC
    """, nativeQuery = true)
    List<VIPCustomerDTO> getVIPCustomers();

    @Query(value = """
        SELECT nv.MaNhanVien AS maNhanVien, nv.HoTen AS hoTen, nv.ChucVu AS chucVu, nv.LuongCoBan AS luongCoBan,
               COUNT(CASE WHEN pv.LoaiCongViec IN ('CHECKIN', 'CHECKOUT') THEN 1 END) AS soLanNhanTraPhong,
               COUNT(CASE WHEN pv.LoaiCongViec = 'HO_TRO' THEN 1 END) AS soLanHoTro,
               CASE WHEN :thang < DATE_FORMAT(CURDATE(), '%Y-%m') THEN nv.LuongCoBan + 100000 * COUNT(CASE WHEN pv.LoaiCongViec IN ('CHECKIN', 'CHECKOUT') THEN 1 END) + 200000 * COUNT(CASE WHEN pv.LoaiCongViec = 'HO_TRO' THEN 1 END) ELSE 0 END AS tongLuong
        FROM NhanVien nv LEFT JOIN NhanVienPhucVu pv ON pv.MaNhanVien = nv.MaNhanVien AND DATE_FORMAT(pv.ThoiGianThucHien, '%Y-%m') = :thang
        GROUP BY nv.MaNhanVien, nv.HoTen, nv.ChucVu, nv.LuongCoBan ORDER BY tongLuong DESC
    """, nativeQuery = true)
    List<MonthlySalaryDTO> calculateMonthlySalary(@Param("thang") String thang);

    @Query(value = """
        SELECT nv.MaNhanVien AS maNhanVien, nv.HoTen AS hoTen, nv.ChucVu AS chucVu, nv.LuongCoBan AS luongCoBan,
               COUNT(CASE WHEN pv.LoaiCongViec IN ('CHECKIN', 'CHECKOUT') THEN 1 END) AS soLanNhanTraPhong,
               COUNT(CASE WHEN pv.LoaiCongViec = 'HO_TRO' THEN 1 END) AS soLanHoTro,
               CASE WHEN :thang < DATE_FORMAT(CURDATE(), '%Y-%m') THEN nv.LuongCoBan + 100000 * COUNT(CASE WHEN pv.LoaiCongViec IN ('CHECKIN', 'CHECKOUT') THEN 1 END) + 200000 * COUNT(CASE WHEN pv.LoaiCongViec = 'HO_TRO' THEN 1 END) ELSE 0 END AS tongLuong
        FROM NhanVien nv LEFT JOIN NhanVienPhucVu pv ON pv.MaNhanVien = nv.MaNhanVien AND DATE_FORMAT(pv.ThoiGianThucHien, '%Y-%m') = :thang
        WHERE nv.MaNhanVien = :maNv
        GROUP BY nv.MaNhanVien, nv.HoTen, nv.ChucVu, nv.LuongCoBan
    """, nativeQuery = true)
    MonthlySalaryDTO getEmployeeSalaryById(@Param("maNv") Integer maNv, @Param("thang") String thang);
}