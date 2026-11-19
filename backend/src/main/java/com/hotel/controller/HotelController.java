package com.hotel.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hotel.dto.BookingDTO;
import com.hotel.dto.BookingRequest;
import com.hotel.dto.CustomerFullProfileDTO;
import com.hotel.dto.MonthlySalaryDTO;
import com.hotel.dto.RevenueByRoomTypeDTO;
import com.hotel.dto.RevenueByServiceTypeDTO;
import com.hotel.dto.RevenueReportDTO;
import com.hotel.dto.RoomOptionDTO;
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

    @GetMapping("/rooms/options")
    public List<RoomOptionDTO> getRoomOptions() {
        return jdbcTemplate.query("""
            SELECT p.MaPhong, p.SoPhong, lp.TenLoaiPhong, lp.DonGia,
                   lp.MoTaChatLuong, lp.ThietKe
            FROM Phong p JOIN LoaiPhong lp ON lp.MaLoaiPhong = p.MaLoaiPhong
            ORDER BY p.SoPhong
        """, (rs, rowNum) -> new RoomOptionDTO(
            rs.getInt("MaPhong"), rs.getString("SoPhong"),
            rs.getString("TenLoaiPhong"), rs.getBigDecimal("DonGia"),
            rs.getString("MoTaChatLuong"), rs.getString("ThietKe")));
    }

    @GetMapping("/rooms/{maPhong}/booking-info")
    public List<Map<String, Object>> getRoomBookingInfo(@PathVariable Integer maPhong) {
        return jdbcTemplate.queryForList("""
            SELECT t.MaLuotThue AS maLuotThue, t.MaKhachHang AS maKhachHang,
                   kh.HoTen AS hoTen, kh.CCCD AS cccd, kh.SoDienThoai AS soDienThoai,
                   t.NgayDat AS ngayDat, t.NgayNhanPhong AS ngayNhanPhong,
                   t.NgayTraDuKien AS ngayTraDuKien, t.TrangThaiDotThue AS trangThaiDotThue
            FROM ThuePhong t
            JOIN KhachHang kh ON kh.MaKhachHang = t.MaKhachHang
            WHERE t.MaPhong = ? AND t.TrangThaiDotThue IN ('DANGO', 'DADAT')
            ORDER BY CASE WHEN t.TrangThaiDotThue = 'DANGO' THEN 0 ELSE 1 END,
                     t.NgayNhanPhong ASC
        """, maPhong);
    }

    @GetMapping("/bookings")
    public List<BookingDTO> getBookings(
            @RequestParam(required = false) Integer maKhachHang,
            @RequestParam(required = false) String trangThai) {
        StringBuilder sql = new StringBuilder("""
            SELECT t.MaLuotThue, t.MaKhachHang, kh.HoTen AS TenKhachHang,
                   t.MaPhong, p.SoPhong, lp.TenLoaiPhong, t.NgayDat,
                   t.NgayNhanPhong, t.NgayTraDuKien, t.NgayTraThucTe,
                   t.TongTienPhong, t.TrangThaiDotThue
            FROM ThuePhong t
            JOIN KhachHang kh ON kh.MaKhachHang = t.MaKhachHang
            JOIN Phong p ON p.MaPhong = t.MaPhong
            JOIN LoaiPhong lp ON lp.MaLoaiPhong = p.MaLoaiPhong
            WHERE 1 = 1
        """);
        List<Object> params = new ArrayList<>();
        if (maKhachHang != null) {
            sql.append(" AND t.MaKhachHang = ?");
            params.add(maKhachHang);
        }
        if (trangThai != null && !trangThai.isBlank()) {
            sql.append(" AND t.TrangThaiDotThue = ?");
            params.add(trangThai.trim().toUpperCase());
        }
        sql.append(" ORDER BY ")
           .append("DADAT".equalsIgnoreCase(trangThai)
               ? "t.NgayNhanPhong ASC"
               : "t.NgayNhanPhong DESC");
        return jdbcTemplate.query(sql.toString(), this::mapBooking, params.toArray());
    }

    @PostMapping("/bookings")
    public ResponseEntity<?> createBooking(@RequestBody BookingRequest request) {
        String validation = validateBookingRequest(request, true);
        if (validation != null) return ResponseEntity.badRequest().body(Map.of("message", validation));

        BigDecimal donGia = getRoomPrice(request.getMaPhong());
        if (donGia == null) return ResponseEntity.badRequest().body(Map.of("message", "Không tìm thấy phòng được chọn."));
        if (!exists("SELECT 1 FROM KhachHang WHERE MaKhachHang = ?", request.getMaKhachHang())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Không tìm thấy khách hàng."));
        }

        BigDecimal tongTien = donGia.multiply(BigDecimal.valueOf(
            Math.max(1, ChronoUnit.DAYS.between(request.getNgayNhanPhong().toLocalDate(), request.getNgayTraDuKien().toLocalDate()))));
        try {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement("""
                    INSERT INTO ThuePhong
                        (MaKhachHang, MaPhong, NgayDat, NgayNhanPhong, NgayTraDuKien, TongTienPhong, TrangThaiDotThue)
                    VALUES (?, ?, NOW(), ?, ?, ?, 'DADAT')
                """, Statement.RETURN_GENERATED_KEYS);
                ps.setInt(1, request.getMaKhachHang());
                ps.setInt(2, request.getMaPhong());
                ps.setObject(3, request.getNgayNhanPhong());
                ps.setObject(4, request.getNgayTraDuKien());
                ps.setBigDecimal(5, tongTien);
                return ps;
            }, keyHolder);
            Number id = keyHolder.getKey();
            return ResponseEntity.ok(getBooking(id.intValue()));
        } catch (DataAccessException ex) {
            return bookingConflict(ex);
        }
    }

    @PutMapping("/bookings/{id}")
    public ResponseEntity<?> updateBooking(@PathVariable Integer id, @RequestBody BookingRequest request) {
        BookingDTO current = getBookingOrNull(id);
        if (current == null) return ResponseEntity.notFound().build();
        if (!"DADAT".equals(current.getTrangThaiDotThue())) {
            return ResponseEntity.status(409).body(Map.of("message", "Chỉ lượt đặt phòng đang ở trạng thái DADAT mới được đổi lịch."));
        }
        if (LocalDateTime.now().isAfter(current.getNgayNhanPhong().minusDays(7))) {
            return ResponseEntity.status(409).body(Map.of("message", "Chỉ được thay đổi lịch đặt trước ngày nhận phòng ít nhất 7 ngày."));
        }
        if (request == null) request = new BookingRequest();
        if (request.getNgayNhanPhong() == null) request.setNgayNhanPhong(current.getNgayNhanPhong());
        if (request.getNgayTraDuKien() == null) request.setNgayTraDuKien(current.getNgayTraDuKien());
        if (request.getMaPhong() == null) request.setMaPhong(current.getMaPhong());
        String validation = validateBookingRequest(request, true);
        if (validation != null) return ResponseEntity.badRequest().body(Map.of("message", validation));

        BigDecimal donGia = getRoomPrice(request.getMaPhong());
        if (donGia == null) return ResponseEntity.badRequest().body(Map.of("message", "Không tìm thấy phòng được chọn."));
        BigDecimal tongTien = donGia.multiply(BigDecimal.valueOf(
            Math.max(1, ChronoUnit.DAYS.between(request.getNgayNhanPhong().toLocalDate(), request.getNgayTraDuKien().toLocalDate()))));
        try {
            jdbcTemplate.update("""
                UPDATE ThuePhong
                SET MaPhong = ?, NgayNhanPhong = ?, NgayTraDuKien = ?, TongTienPhong = ?
                WHERE MaLuotThue = ? AND TrangThaiDotThue = 'DADAT'
            """, request.getMaPhong(), request.getNgayNhanPhong(), request.getNgayTraDuKien(), tongTien, id);
            return ResponseEntity.ok(getBooking(id));
        } catch (DataAccessException ex) {
            return bookingConflict(ex);
        }
    }

    @DeleteMapping("/bookings/{id}")
    public ResponseEntity<?> cancelBooking(@PathVariable Integer id) {
        BookingDTO current = getBookingOrNull(id);
        if (current == null) return ResponseEntity.notFound().build();
        if (!"DADAT".equals(current.getTrangThaiDotThue())) {
            return ResponseEntity.status(409).body(Map.of("message", "Chỉ lượt đặt phòng chưa nhận phòng mới được hủy."));
        }
        if (LocalDateTime.now().isAfter(current.getNgayNhanPhong().minusDays(7))) {
            return ResponseEntity.status(409).body(Map.of("message", "Chỉ được hủy lịch đặt trước ngày nhận phòng ít nhất 7 ngày."));
        }
        try {
            jdbcTemplate.update("UPDATE ThuePhong SET TrangThaiDotThue = 'HUY' WHERE MaLuotThue = ?", id);
            return ResponseEntity.ok(getBooking(id));
        } catch (DataAccessException ex) {
            return bookingConflict(ex);
        }
    }

    private String validateBookingRequest(BookingRequest request, boolean requireFuture) {
        if (request == null || request.getMaKhachHang() == null || request.getMaPhong() == null
                || request.getNgayNhanPhong() == null || request.getNgayTraDuKien() == null) {
            return "Vui lòng nhập đầy đủ khách hàng, phòng, ngày nhận và ngày trả.";
        }
        if (!request.getNgayTraDuKien().isAfter(request.getNgayNhanPhong())) {
            return "Ngày trả dự kiến phải sau ngày nhận phòng.";
        }
        if (requireFuture && !request.getNgayNhanPhong().isAfter(LocalDateTime.now())) {
            return "Ngày nhận phòng phải ở tương lai.";
        }
        return null;
    }

    private boolean exists(String sql, Integer value) {
        return !jdbcTemplate.queryForList(sql, Integer.class, value).isEmpty();
    }

    private BigDecimal getRoomPrice(Integer maPhong) {
        List<BigDecimal> prices = jdbcTemplate.queryForList("""
            SELECT lp.DonGia FROM Phong p JOIN LoaiPhong lp ON lp.MaLoaiPhong = p.MaLoaiPhong
            WHERE p.MaPhong = ?
        """, BigDecimal.class, maPhong);
        return prices.isEmpty() ? null : prices.get(0);
    }

    private BookingDTO getBooking(Integer id) {
        BookingDTO booking = getBookingOrNull(id);
        if (booking == null) throw new IllegalStateException("Không tìm thấy lượt đặt phòng sau khi cập nhật.");
        return booking;
    }

    private BookingDTO getBookingOrNull(Integer id) {
        List<BookingDTO> bookings = jdbcTemplate.query("""
            SELECT t.MaLuotThue, t.MaKhachHang, kh.HoTen AS TenKhachHang,
                   t.MaPhong, p.SoPhong, lp.TenLoaiPhong, t.NgayDat,
                   t.NgayNhanPhong, t.NgayTraDuKien, t.NgayTraThucTe,
                   t.TongTienPhong, t.TrangThaiDotThue
            FROM ThuePhong t JOIN KhachHang kh ON kh.MaKhachHang = t.MaKhachHang
            JOIN Phong p ON p.MaPhong = t.MaPhong JOIN LoaiPhong lp ON lp.MaLoaiPhong = p.MaLoaiPhong
            WHERE t.MaLuotThue = ?
        """, this::mapBooking, id);
        return bookings.isEmpty() ? null : bookings.get(0);
    }

    private BookingDTO mapBooking(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        BookingDTO b = new BookingDTO();
        b.setMaLuotThue(rs.getInt("MaLuotThue"));
        b.setMaKhachHang(rs.getInt("MaKhachHang"));
        b.setTenKhachHang(rs.getString("TenKhachHang"));
        b.setMaPhong(rs.getInt("MaPhong"));
        b.setSoPhong(rs.getString("SoPhong"));
        b.setTenLoaiPhong(rs.getString("TenLoaiPhong"));
        b.setNgayDat(rs.getObject("NgayDat", LocalDateTime.class));
        b.setNgayNhanPhong(rs.getObject("NgayNhanPhong", LocalDateTime.class));
        b.setNgayTraDuKien(rs.getObject("NgayTraDuKien", LocalDateTime.class));
        b.setNgayTraThucTe(rs.getObject("NgayTraThucTe", LocalDateTime.class));
        b.setTongTienPhong(rs.getBigDecimal("TongTienPhong"));
        b.setTrangThaiDotThue(rs.getString("TrangThaiDotThue"));
        b.setChoPhepThayDoi("DADAT".equals(b.getTrangThaiDotThue())
            && !LocalDateTime.now().isAfter(b.getNgayNhanPhong().minusDays(7)));
        b.setChoPhepHuy("DADAT".equals(b.getTrangThaiDotThue())
            && !LocalDateTime.now().isAfter(b.getNgayNhanPhong().minusDays(7)));
        return b;
    }

    private ResponseEntity<Map<String, String>> bookingConflict(DataAccessException ex) {
        String message = ex.getMostSpecificCause() == null ? ex.getMessage() : ex.getMostSpecificCause().getMessage();
        if (message == null || message.isBlank()) message = "Không thể thực hiện thao tác đặt phòng.";
        if (message.contains("BOOKING_OVERLAP")) {
            message = "Lỗi nghiệp vụ: Phòng đã có khách đặt/ở trong khoảng thời gian này!";
        } else if (message.contains("BOOKING_CHANGE_WINDOW_7_DAYS")) {
            message = "Lỗi nghiệp vụ: Chỉ được thay đổi lịch đặt trước ngày nhận phòng ít nhất 7 ngày!";
        } else if (message.contains("BOOKING_CANCEL_WINDOW_7_DAYS")) {
            message = "Lỗi nghiệp vụ: Chỉ được hủy lịch đặt trước ngày nhận phòng ít nhất 7 ngày!";
        }
        return ResponseEntity.status(409).body(Map.of("message", message));
    }

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
            time = time.replace('T', ' ');
            if (time.length() == 16) time += ":00";
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
    public List<MonthlySalaryDTO> getMonthlySalary(@RequestParam(required = false) String month,
                                                    @RequestParam(required = false) String until) {
        if (month == null || month.isBlank()) month = YearMonth.now().toString();
        return hotelReportRepository.calculateMonthlySalary(month, normalizeSalaryUntil(month, until));
    }

    @GetMapping("/salary/{id}")
    public MonthlySalaryDTO getEmployeeSalaryById(@PathVariable Integer id,
                                                  @RequestParam(required = false) String month,
                                                  @RequestParam(required = false) String until) {
        if (month == null || month.isBlank()) month = YearMonth.now().toString();
        return hotelReportRepository.getEmployeeSalaryById(id, month, normalizeSalaryUntil(month, until));
    }

    private String normalizeSalaryUntil(String month, String until) {
        YearMonth selectedMonth = YearMonth.parse(month);
        LocalDate today = LocalDate.now();
        LocalDate date;
        if (until == null || until.isBlank()) {
            date = selectedMonth.equals(YearMonth.from(today)) ? today : selectedMonth.atEndOfMonth();
        } else {
            date = LocalDate.parse(until);
        }
        if (date.isAfter(today)) date = today;
        return date.toString();
    }
}
