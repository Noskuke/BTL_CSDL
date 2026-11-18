package com.hotel.controller;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hotel.dto.CustomerFullProfileDTO;
import com.hotel.dto.MonthlySalaryDTO;
import com.hotel.dto.RevenueByRoomTypeDTO;
import com.hotel.dto.RevenueByServiceTypeDTO;
import com.hotel.dto.RevenueReportDTO;
import com.hotel.dto.RoomFrequencyDTO;
import com.hotel.dto.RoomStatusDTO;
import com.hotel.dto.ServiceFrequencyDTO;
import com.hotel.dto.VIPCustomerDTO;
import com.hotel.repository.HotelReportRepository;

@RestController
@RequestMapping("/api/hotel")
@CrossOrigin(origins = "*")
public class HotelController {

    @Autowired
    private HotelReportRepository hotelReportRepository;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @GetMapping("/customer-full-profile/{maKh}")
    public CustomerFullProfileDTO getCustomerFullProfile(@PathVariable Integer maKh) {
        CustomerFullProfileDTO dto = new CustomerFullProfileDTO();

        // Thông tin cơ bản của khách (mọi khách đều có tên, không chỉ khách VIP)
        dto.setMaKhachHang(maKh);
        List<String> dsTen = jdbcTemplate.queryForList(
            "SELECT HoTen FROM KhachHang WHERE MaKhachHang = ?", String.class, maKh);
        dto.setHoTen(dsTen.isEmpty() ? null : dsTen.get(0));

        dto.setLichSuThuePhong(jdbcTemplate.queryForList("""
            SELECT t.MaLuotThue, p.SoPhong, lp.TenLoaiPhong, t.NgayNhanPhong, t.NgayTraThucTe,
                   t.TrangThaiDotThue, t.TongTienPhong
            FROM ThuePhong t JOIN Phong p ON p.MaPhong = t.MaPhong JOIN LoaiPhong lp ON lp.MaLoaiPhong = p.MaLoaiPhong
            WHERE t.MaKhachHang = ? ORDER BY t.NgayNhanPhong DESC
        """, maKh));

        dto.setLichSuSuDungDichVu(jdbcTemplate.queryForList("""
            SELECT t.MaLuotThue, d.TenDichVu, d.LoaiDichVu, s.SoLuong, s.DonGiaThoiDiem,
                   s.ThanhTien, s.ThoiGianSuDung
            FROM SuDungDichVu s JOIN ThuePhong t ON t.MaLuotThue = s.MaLuotThue JOIN DichVu d ON d.MaDichVu = s.MaDichVu
            WHERE t.MaKhachHang = ? ORDER BY s.ThoiGianSuDung DESC
        """, maKh));

        dto.setPhongDangO(jdbcTemplate.queryForList("""
            SELECT p.SoPhong, lp.TenLoaiPhong, t.NgayNhanPhong AS TuNgay, t.NgayTraDuKien
            FROM ThuePhong t JOIN Phong p ON p.MaPhong = t.MaPhong JOIN LoaiPhong lp ON lp.MaLoaiPhong = p.MaLoaiPhong
            WHERE t.MaKhachHang = ? AND t.TrangThaiDotThue = 'DANGO'
        """, maKh));

        dto.setDichVuDangDung(jdbcTemplate.queryForList("""
            SELECT d.TenDichVu, SUM(s.SoLuong) AS SoLuong, SUM(s.ThanhTien) AS TongTien
            FROM ThuePhong t JOIN SuDungDichVu s ON s.MaLuotThue = t.MaLuotThue JOIN DichVu d ON d.MaDichVu = s.MaDichVu
            WHERE t.MaKhachHang = ? AND t.TrangThaiDotThue = 'DANGO' GROUP BY d.MaDichVu, d.TenDichVu
        """, maKh));

        return dto;
    }

    @GetMapping("/search-customer-id")
    public ResponseEntity<?> searchCustomerId(@RequestParam String type, @RequestParam String keyword) {
        List<Integer> list = hotelReportRepository.findCustomerIdByCriteria(type, keyword);
        return ResponseEntity.ok(list != null && !list.isEmpty() ? list.get(0) : -1);
    }

    @GetMapping("/room-status")
    public List<RoomStatusDTO> getRoomStatus(@RequestParam(required = false) String time) {
        if (time == null || time.isEmpty()) {
            time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } else {
            time = time.replace('T', ' ') + ":00";
        }
        return hotelReportRepository.getRoomStatusAtTime(time);
    }

    @GetMapping("/revenue")
    public List<RevenueReportDTO> getRevenue(
            @RequestParam(defaultValue = "2026-01-01") String tuNgay,
            @RequestParam(defaultValue = "2026-12-31") String denNgay) {
        return hotelReportRepository.getRevenueReport(tuNgay, denNgay);
    }

    @GetMapping("/revenue-room-type")
    public List<RevenueByRoomTypeDTO> getRevenueByRoomType(
            @RequestParam(defaultValue = "2026-01-01") String tuNgay,
            @RequestParam(defaultValue = "2026-12-31") String denNgay) {
        return hotelReportRepository.getRevenueByRoomType(tuNgay, denNgay);
    }

    @GetMapping("/revenue-service-type")
    public List<RevenueByServiceTypeDTO> getRevenueByServiceType(
            @RequestParam(defaultValue = "2026-01-01") String tuNgay,
            @RequestParam(defaultValue = "2026-12-31") String denNgay) {
        return hotelReportRepository.getRevenueByServiceType(tuNgay, denNgay);
    }

    @GetMapping("/room-frequency-monthly")
    public List<RoomFrequencyDTO> getRoomFrequencyMonthly(@RequestParam(required = false) String month) {
        if (month == null || month.isBlank()) month = YearMonth.now().toString();
        return hotelReportRepository.getRoomFrequencyMonthly(month);
    }

    @GetMapping("/service-frequency")
    public List<ServiceFrequencyDTO> getServiceFrequency(
            @RequestParam(required = false) String month) {
        // Mặc định tháng hiện tại nếu API được gọi mà không truyền tháng.
        if (month == null || month.isBlank()) {
            month = YearMonth.now().toString();
        }
        // Thống kê dịch vụ theo đúng tháng được chọn trên giao diện (yyyy-MM).
        if (!month.matches("\\d{4}-(0[1-9]|1[0-2])")) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Tháng không hợp lệ. Định dạng đúng: yyyy-MM");
        }
        return hotelReportRepository.getServiceFrequency(month);
    }

    @GetMapping("/vip-customers")
    public List<VIPCustomerDTO> getVIPCustomers() {
        return hotelReportRepository.getVIPCustomers();
    }

    @GetMapping("/salary")
    public List<MonthlySalaryDTO> getMonthlySalary(@RequestParam(required = false) String month) {
        if (month == null || month.isBlank()) month = YearMonth.now().toString();
        return hotelReportRepository.calculateMonthlySalary(month);
    }

    @GetMapping("/salary/{id}")
    public MonthlySalaryDTO getEmployeeSalaryById(@PathVariable Integer id, @RequestParam(required = false) String month) {
        if (month == null || month.isBlank()) month = YearMonth.now().toString();
        return hotelReportRepository.getEmployeeSalaryById(id, month);
    }
}