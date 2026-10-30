import argparse
import math
import os
import random
from datetime import datetime, timedelta

import mysql.connector


db_config = {
    'host': 'localhost',
    'user': 'root',
    'password': os.getenv('DB_PASSWORD', '123456'),
    'database': 'QuanLyKhachSan'
}


SO_NHAN_VIEN = 25
SO_PHONG = 50
SO_KHACH_BAN_DAU = 5000
SO_KHACH_MOI_MOI_LAN = 20
SO_NGAY_LICH_SU = 1100
SO_LUOT_TOI_THIEU = 10000

# Ngưỡng định danh VIP
NGUONG_LAN_THUE_VIP = 3
NGUONG_CHI_TIEU_VIP = 20000000

# Số khách VIP mỗi lần chạy là ngẫu nhiên trong khoảng [TOI_THIEU, TOI_DA].
# Mỗi VIP chỉ được giữ tối đa SO_LUOT_TOI_DA_VIP lượt thuê (mỗi người một mức ngẫu nhiên
# từ 4 đến mức này) để không có khách thuê quá nhiều lần.
SO_VIP_TOI_THIEU = 15
SO_VIP_TOI_DA = 60
SO_LUOT_TOI_DA_VIP = 12

# Số khách đang ở (DANGO) tại thời điểm chạy: ngẫu nhiên trong khoảng này (khách khác nhau)
SO_KHACH_DANG_O_TOI_THIEU = 40
SO_KHACH_DANG_O_TOI_DA = 47


HO = [
    ("Nguyễn", 38), ("Trần", 11), ("Lê", 9), ("Phạm", 7), ("Hoàng", 5),
    ("Huỳnh", 4), ("Phan", 4), ("Vũ", 4), ("Võ", 3), ("Đặng", 3),
    ("Bùi", 3), ("Đỗ", 3), ("Hồ", 2), ("Ngô", 2), ("Dương", 1), ("Lý", 1),
]

DEM_NAM = ["Văn", "Minh", "Quốc", "Đức", "Hữu", "Anh", "Thanh", "Gia", "Xuân", "Công"]
DEM_NU = ["Thị", "Thu", "Ngọc", "Thanh", "Phương", "Kim", "Mai", "Hồng", "Bích", "Khánh"]

TEN_NAM = [
    "Hùng", "Dũng", "Nam", "Long", "Khang", "Phúc", "Tuấn", "Hải", "Sơn", "Quân",
    "Bảo", "Huy", "Hiếu", "Thành", "Trung", "Khoa", "Đạt", "Tùng", "Linh", "Việt",
]
TEN_NU = [
    "Lan", "Hoa", "Mai", "Hương", "Trang", "Linh", "Anh", "Ngọc", "Thảo", "Hà",
    "Nhung", "Yến", "Vy", "Chi", "Ly", "Phương", "Quỳnh", "Dung", "Hạnh", "My",
]


def tao_ten():
    ho = random.choices([h for h, _ in HO], weights=[w for _, w in HO])[0]

    if random.random() < 0.5:
        dem, ten = random.choice(DEM_NAM), random.choice(TEN_NAM)
    else:
        dem, ten = random.choice(DEM_NU), random.choice(TEN_NU)

    return f"{ho} {dem} {ten}"


def doi_ten_hien_co(cursor, bang, cot_ma, mau_ten):
    cursor.execute(
        f"SELECT {cot_ma} FROM {bang} WHERE HoTen LIKE %s",
        (mau_ten,)
    )
    ids = [x[0] for x in cursor.fetchall()]

    cursor.executemany(
        f"UPDATE {bang} SET HoTen = %s WHERE {cot_ma} = %s",
        [(tao_ten(), ma) for ma in ids]
    )

    return len(ids)


def chia_lo(danh_sach, kich_thuoc=5000):
    for i in range(0, len(danh_sach), kich_thuoc):
        yield danh_sach[i:i + kich_thuoc]


def xoa_sach_du_lieu(cursor):
    cursor.execute("SET FOREIGN_KEY_CHECKS = 0")

    for bang in [
        'NhanVienPhucVu', 'SuDungDichVu', 'ThuePhong', 'KhachHang',
        'Phong', 'LoaiPhong', 'DichVu', 'NhanVien'
    ]:
        cursor.execute(f"TRUNCATE TABLE {bang}")

    cursor.execute("SET FOREIGN_KEY_CHECKS = 1")



# Không sinh hoặc quản lý trạng thái bảo trì trong script dữ liệu demo.

def nap_du_lieu_goc(cursor):
    nhan_vien = []

    for i in range(1, SO_NHAN_VIEN + 1):
        if i <= 5:
            chuc_vu, luong = "Lễ Tân", 8500000
        elif i <= 15:
            chuc_vu, luong = "Phục Vụ", 6500000
        elif i <= 20:
            chuc_vu, luong = "Quản Lý", 12000000
        else:
            chuc_vu, luong = "Kỹ Thuật", 7000000

        nhan_vien.append(
            (f"001099{i:06d}", tao_ten(), f"0912345{i:03d}", chuc_vu, luong)
        )

    cursor.executemany(
        """
        INSERT INTO NhanVien
        (CCCD, HoTen, SoDienThoai, ChucVu, LuongCoBan)
        VALUES (%s, %s, %s, %s, %s)
        """,
        nhan_vien
    )

    loai_phong = [
        ('Standard Single', 20, 500000),
        ('Standard Double', 15, 800000),
        ('Deluxe Suite', 10, 1500000),
        ('President VIP', 5, 3500000)
    ]

    cursor.executemany(
        """
        INSERT INTO LoaiPhong
        (TenLoaiPhong, SoLuong, DonGia)
        VALUES (%s, %s, %s)
        """,
        loai_phong
    )

    phong = []

    for i in range(1, SO_PHONG + 1):
        if i <= 20:
            ma_loai = 1
        elif i <= 35:
            ma_loai = 2
        elif i <= 45:
            ma_loai = 3
        else:
            ma_loai = 4

        phong.append((f"P{i:03d}", ma_loai, 'TRONG'))

    cursor.executemany(
        """
        INSERT INTO Phong
        (SoPhong, MaLoaiPhong, TrangThai)
        VALUES (%s, %s, %s)
        """,
        phong
    )

    dich_vu = [
        ('Buffet Sáng High-End', 'Ẩm thực', 200000),
        ('Giặt ủi cao cấp', 'Dịch vụ phòng', 100000),
        ('Massage & Spa 60p', 'Thư giãn', 800000),
        ('Rượu Minibar', 'Minibar', 150000),
        ('Thuê Xe Máy 24h', 'Thuê xe', 200000),
        ('Đón Tiễn Sân Bay', 'Dịch vụ phòng', 300000)
    ]

    cursor.executemany(
        """
        INSERT INTO DichVu
        (TenDichVu, LoaiDichVu, DonGia)
        VALUES (%s, %s, %s)
        """,
        dich_vu
    )

    khach_hang = [
        (f"034095{i:06d}", tao_ten(), f"098{i:07d}")
        for i in range(1, SO_KHACH_BAN_DAU + 1)
    ]

    cursor.executemany(
        """
        INSERT INTO KhachHang
        (CCCD, HoTen, SoDienThoai)
        VALUES (%s, %s, %s)
        """,
        khach_hang
    )


def them_khach_moi(cursor, so_luong):
    cursor.execute("SELECT COALESCE(MAX(MaKhachHang), 0) FROM KhachHang")
    max_id = cursor.fetchone()[0]

    du_lieu = []
    for i in range(1, so_luong + 1):
        ma = max_id + i
        du_lieu.append((f"034095{ma:06d}", tao_ten(), f"098{ma:07d}"))

    cursor.executemany(
        """
        INSERT INTO KhachHang
        (CCCD, HoTen, SoDienThoai)
        VALUES (%s, %s, %s)
        """,
        du_lieu
    )

    cursor.execute(
        "SELECT MaKhachHang FROM KhachHang WHERE MaKhachHang > %s",
        (max_id,)
    )

    return [x[0] for x in cursor.fetchall()]


def chon_khach_hang(khach_ids, khach_moi_ids):
    if khach_moi_ids and random.random() < 0.20:
        return random.choice(khach_moi_ids)

    return random.choice(khach_ids)


def sinh_dich_vu(ma_luot, nhan, tra, nv_ids, dich_vu_list, dv_rows):
    if nhan >= tra:
        return

    khoang_thoi_gian = tra - nhan
    thoi_gian_1 = nhan + timedelta(hours=3)

    if thoi_gian_1 < tra:
        ma_dv, gia_dv = random.choice(dich_vu_list)
        dv_rows.append(
            (ma_luot, ma_dv, random.choice(nv_ids),
             random.randint(1, 2), gia_dv, thoi_gian_1)
        )

    so_dich_vu_them = random.randint(1, 3)

    for _ in range(so_dich_vu_them):
        if khoang_thoi_gian.total_seconds() <= 4 * 3600:
            break

        so_gio = random.randint(
            4,
            max(4, int(khoang_thoi_gian.total_seconds() // 3600) - 1)
        )

        thoi_gian = nhan + timedelta(hours=so_gio)

        if thoi_gian >= tra:
            continue

        ma_dv, gia_dv = random.choice(dich_vu_list)
        dv_rows.append(
            (ma_luot, ma_dv, random.choice(nv_ids),
             random.randint(1, 3), gia_dv, thoi_gian)
        )


def sinh_cong_viec(ma_luot, nhan, tra, trang_thai, le_tan_ids, nv_ids, nv_rows):
    nv_rows.append((ma_luot, random.choice(le_tan_ids), 'CHECKIN', nhan))

    if trang_thai == 'DATRA':
        nv_rows.append((ma_luot, random.choice(le_tan_ids), 'CHECKOUT', tra))

    so_ho_tro = random.randint(1, 3)

    for _ in range(so_ho_tro):
        thoi_gian = nhan + timedelta(
            hours=random.randint(
                3, max(3, int((tra - nhan).total_seconds() // 3600) - 1)
            )
        )

        if thoi_gian < tra:
            nv_rows.append(
                (ma_luot, random.choice(nv_ids), 'HO_TRO', thoi_gian)
            )


def cap_nhat_luot_thue_hien_co(
    cursor, now, le_tan_ids, nv_ids, dich_vu_list, dv_rows, nv_rows
):
    cursor.execute(
        """
        SELECT MaLuotThue, NgayNhanPhong, NgayTraDuKien, TrangThaiDotThue
        FROM ThuePhong
        WHERE TrangThaiDotThue IN ('DADAT', 'DANGO')
        """
    )

    cac_luot = cursor.fetchall()
    so_chuyen = 0

    for ma_luot, nhan, tra, trang_thai_cu in cac_luot:
        if tra < now:
            trang_thai_moi = 'DATRA'
        elif nhan <= now:
            trang_thai_moi = 'DANGO'
        else:
            trang_thai_moi = 'DADAT'

        if trang_thai_moi != trang_thai_cu:
            cursor.execute(
                """
                UPDATE ThuePhong
                SET
                    TrangThaiDotThue = %s,
                    NgayTraThucTe =
                        CASE
                            WHEN %s = 'DATRA'
                            THEN NgayTraDuKien
                            ELSE NgayTraThucTe
                        END
                WHERE MaLuotThue = %s
                """,
                (trang_thai_moi, trang_thai_moi, ma_luot)
            )
            so_chuyen += 1

        if trang_thai_moi == 'DADAT':
            continue

        if trang_thai_cu == 'DADAT':
            nv_rows.append(
                (ma_luot, random.choice(le_tan_ids), 'CHECKIN', nhan)
            )

        if trang_thai_moi == 'DATRA':
            nv_rows.append(
                (ma_luot, random.choice(le_tan_ids), 'CHECKOUT', tra)
            )

        sinh_dich_vu(
            ma_luot, nhan, min(tra, now), nv_ids, dich_vu_list, dv_rows
        )

    return so_chuyen


def sinh_luot_thue(
    cursor, now, gioi_han, phong_list, khach_ids, khach_moi_ids,
    le_tan_ids, nv_ids, dich_vu_list, dv_rows, nv_rows
):
    cursor.execute(
        """
        SELECT MaPhong, MAX(NgayTraDuKien)
        FROM ThuePhong
        WHERE TrangThaiDotThue <> 'HUY'
        GROUP BY MaPhong
        """
    )

    luot_cuoi = dict(cursor.fetchall())
    moc_dau = (now - timedelta(days=SO_NGAY_LICH_SU)).replace(
        hour=14, minute=0, second=0, microsecond=0
    )

    so_luot_moi = 0

    for ma_phong, don_gia in phong_list:
        if ma_phong in luot_cuoi:
            nhan = (
                luot_cuoi[ma_phong] + timedelta(days=random.randint(0, 2))
            ).replace(hour=14, minute=0, second=0)
        else:
            nhan = moc_dau + timedelta(days=random.randint(0, 2))

        while nhan < gioi_han:
            so_ngay = random.randint(1, 5)
            tra = (nhan + timedelta(days=so_ngay)).replace(
                hour=12, minute=0, second=0
            )

            ngay_dat = nhan - timedelta(
                days=random.randint(1, 30), hours=random.randint(0, 12)
            )

            if ngay_dat > now:
                ngay_dat = now

            if tra <= now:
                trang_thai = 'DATRA'
                tra_thuc_te = tra
            elif nhan <= now:
                trang_thai = 'DANGO'
                tra_thuc_te = None
            else:
                trang_thai = 'DADAT'
                tra_thuc_te = None

            ma_kh = chon_khach_hang(khach_ids, khach_moi_ids)

            cursor.execute(
                """
                INSERT INTO ThuePhong
                (
                    MaKhachHang, MaPhong, NgayDat, NgayNhanPhong,
                    NgayTraDuKien, NgayTraThucTe, TongTienPhong,
                    TrangThaiDotThue
                )
                VALUES
                (%s, %s, %s, %s, %s, %s, %s, %s)
                """,
                (
                    ma_kh, ma_phong, ngay_dat, nhan, tra,
                    tra_thuc_te, don_gia * so_ngay, trang_thai
                )
            )

            ma_luot = cursor.lastrowid
            so_luot_moi += 1

            if trang_thai in ('DANGO', 'DATRA'):
                sinh_cong_viec(
                    ma_luot, nhan, min(tra, now), trang_thai,
                    le_tan_ids, nv_ids, nv_rows
                )

                sinh_dich_vu(
                    ma_luot, nhan, min(tra, now),
                    nv_ids, dich_vu_list, dv_rows
                )

            nhan = (tra + timedelta(days=random.randint(0, 2))).replace(
                hour=14, minute=0, second=0
            )

    return so_luot_moi


def sinh_bo_du_lieu(
    cursor, now, phong_list, khach_ids, khach_moi_ids,
    le_tan_ids, nv_ids, dich_vu_list, dv_rows, nv_rows, ngay_toi
):
    gioi_han = now + timedelta(days=ngay_toi)

    return sinh_luot_thue(
        cursor, now, gioi_han, phong_list, khach_ids, khach_moi_ids,
        le_tan_ids, nv_ids, dich_vu_list, dv_rows, nv_rows
    )


def xoa_du_lieu_cu(cursor, giu_ngay):
    moc = datetime.now() - timedelta(days=giu_ngay)
    dieu_kien = "t.NgayTraDuKien < %s AND t.TrangThaiDotThue IN ('DATRA', 'HUY')"

    cursor.execute(
        f"""
        DELETE nv
        FROM NhanVienPhucVu nv
        JOIN ThuePhong t ON t.MaLuotThue = nv.MaLuotThue
        WHERE {dieu_kien}
        """,
        (moc,)
    )

    cursor.execute(
        f"""
        DELETE s
        FROM SuDungDichVu s
        JOIN ThuePhong t ON t.MaLuotThue = s.MaLuotThue
        WHERE {dieu_kien}
        """,
        (moc,)
    )

    cursor.execute(
        f"DELETE t FROM ThuePhong t WHERE {dieu_kien}",
        (moc,)
    )

    return cursor.rowcount


def them_luot_dang_o(cursor, now, ma_phong, don_gia, ma_kh, le_tan_ids):
    """
    Thêm một lượt ở (DANGO) cho phòng đang trống, khoảng thời gian được chọn sao cho
    KHÔNG trùng lịch với lượt thuê nào khác của phòng (đúng điều kiện của trigger
    trg_CheckOverlappingBooking). Trả về True nếu thêm được.
    """
    for _ in range(30):
        nhan = (now - timedelta(days=random.randint(0, 4))).replace(
            hour=14, minute=0, second=0, microsecond=0
        )

        # Phải nhận phòng đủ lâu để có thể đã dùng dịch vụ
        if nhan > now - timedelta(hours=4):
            continue

        tra = (nhan + timedelta(days=random.randint(1, 5))).replace(
            hour=12, minute=0, second=0, microsecond=0
        )

        if tra <= now + timedelta(hours=2):
            continue

        cursor.execute(
            """
            SELECT 1 FROM ThuePhong t
            WHERE t.MaPhong = %s
              AND t.TrangThaiDotThue <> 'HUY'
              AND %s < COALESCE(t.NgayTraThucTe, t.NgayTraDuKien)
              AND %s > t.NgayNhanPhong
            LIMIT 1
            """,
            (ma_phong, nhan, tra)
        )

        if cursor.fetchone() is not None:
            continue

        so_ngay = (tra.date() - nhan.date()).days
        ngay_dat = nhan - timedelta(
            days=random.randint(1, 30), hours=random.randint(0, 12)
        )

        cursor.execute(
            """
            INSERT INTO ThuePhong
            (
                MaKhachHang, MaPhong, NgayDat, NgayNhanPhong,
                NgayTraDuKien, TongTienPhong, TrangThaiDotThue
            )
            VALUES (%s, %s, %s, %s, %s, %s, 'DANGO')
            """,
            (ma_kh, ma_phong, ngay_dat, nhan, tra, don_gia * so_ngay)
        )

        cursor.execute(
            """
            INSERT INTO NhanVienPhucVu
            (MaLuotThue, MaNhanVien, LoaiCongViec, ThoiGianThucHien)
            VALUES (%s, %s, 'CHECKIN', %s)
            """,
            (cursor.lastrowid, random.choice(le_tan_ids), nhan)
        )

        return True

    return False


def dam_bao_nhieu_khach_dang_o(
    cursor, now, ma_khach_mau, le_tan_ids, nv_ids, dich_vu_list, khach_ids, muc_tieu
):
    """
    Đảm bảo có ít nhất `muc_tieu` KHÁCH KHÁC NHAU đang ở (DANGO) tại thời điểm hiện tại.
    Thiếu thì sinh thêm lượt ở cho các phòng trống (không trùng lịch) và gán cho
    các mã khách riêng biệt, đồng thời đảm bảo mọi khách đang ở đều có 2-4 dịch vụ.
    Trả về (số khách đang ở, số khách có dịch vụ, số lượt ở đã bổ sung).
    """
    if ma_khach_mau:
        cursor.execute(
            "SELECT 1 FROM KhachHang WHERE MaKhachHang = %s",
            (ma_khach_mau,)
        )
        if cursor.fetchone() is not None:
            cursor.execute(
                """
                SELECT MaLuotThue FROM ThuePhong
                WHERE MaKhachHang = %s AND TrangThaiDotThue = 'DANGO'
                """,
                (ma_khach_mau,)
            )
            if cursor.fetchone() is None:
                cursor.execute(
                    """
                    SELECT MaLuotThue FROM ThuePhong
                    WHERE TrangThaiDotThue = 'DANGO' AND NgayNhanPhong <= %s
                    LIMIT 1
                    """,
                    (now - timedelta(hours=2),)
                )
                luot_thay = cursor.fetchone()
                if luot_thay:
                    cursor.execute(
                        "UPDATE ThuePhong SET MaKhachHang = %s WHERE MaLuotThue = %s",
                        (ma_khach_mau, luot_thay[0])
                    )

    # Lấy các lượt ở DANGO hiện tại
    cursor.execute(
        """
        SELECT MaLuotThue, MaKhachHang, MaPhong, NgayNhanPhong
        FROM ThuePhong
        WHERE TrangThaiDotThue = 'DANGO'
        """
    )
    dango_list = cursor.fetchall()

    so_them = 0

    if len(dango_list) < muc_tieu:
        phong_dang_o = {row[2] for row in dango_list}
        cursor.execute(
            """
            SELECT p.MaPhong, lp.DonGia
            FROM Phong p
            JOIN LoaiPhong lp ON lp.MaLoaiPhong = p.MaLoaiPhong
            """
        )
        phong_trong = [p for p in cursor.fetchall() if p[0] not in phong_dang_o]
        random.shuffle(phong_trong)

        khach_dang_o_set = {row[1] for row in dango_list}
        khach_ranh = [k for k in khach_ids if k not in khach_dang_o_set]
        random.shuffle(khach_ranh)

        can_them = muc_tieu - len(dango_list)

        for ma_phong, don_gia in phong_trong:
            if so_them >= can_them or not khach_ranh:
                break

            if them_luot_dang_o(
                cursor, now, ma_phong, don_gia, khach_ranh[-1], le_tan_ids
            ):
                khach_ranh.pop()
                so_them += 1

        cursor.execute(
            """
            SELECT MaLuotThue, MaKhachHang, MaPhong, NgayNhanPhong
            FROM ThuePhong
            WHERE TrangThaiDotThue = 'DANGO'
            """
        )
        dango_list = cursor.fetchall()

    # Phân bổ lại mã khách hàng để đảm bảo KHÔNG TRÙNG NHAU giữa các phòng đang ở
    khach_da_o = set()
    khach_kha_dung = [k for k in khach_ids if k != ma_khach_mau]
    random.shuffle(khach_kha_dung)

    for ma_luot, ma_kh, _, nhan in dango_list:
        # Khách nào (kể cả khách mẫu) cũng chỉ giữ MỘT phòng đang ở, lượt trùng sẽ đổi sang khách khác
        if ma_kh in khach_da_o:
            while khach_kha_dung and khach_kha_dung[-1] in khach_da_o:
                khach_kha_dung.pop()

            if khach_kha_dung:
                ma_kh_moi = khach_kha_dung.pop()
                cursor.execute(
                    "UPDATE ThuePhong SET MaKhachHang = %s WHERE MaLuotThue = %s",
                    (ma_kh_moi, ma_luot)
                )
                ma_kh = ma_kh_moi

        khach_da_o.add(ma_kh)

    # Đảm bảo TẤT CẢ các khách đang ở đều có dữ liệu sử dụng dịch vụ
    so_khach_dang_o = len(dango_list)
    so_khach_co_dv = 0

    for ma_luot, _, _, nhan in dango_list:
        cursor.execute(
            "SELECT COUNT(*) FROM SuDungDichVu WHERE MaLuotThue = %s",
            (ma_luot,)
        )
        count_dv = cursor.fetchone()[0]

        if count_dv < 2:
            so_dv_can_them = random.randint(2, 4) - count_dv
            for _ in range(so_dv_can_them):
                ma_dv, gia_dv = random.choice(dich_vu_list)
                thoi_gian_dung = nhan + timedelta(
                    minutes=random.randint(30, max(31, int((now - nhan).total_seconds() // 60)))
                )
                if thoi_gian_dung > now:
                    thoi_gian_dung = now

                cursor.execute(
                    """
                    INSERT INTO SuDungDichVu
                    (
                        MaLuotThue, MaDichVu, MaNhanVienPhucVu,
                        SoLuong, DonGiaThoiDiem, ThoiGianSuDung
                    )
                    VALUES
                    (%s, %s, %s, %s, %s, %s)
                    """,
                    (ma_luot, ma_dv, random.choice(nv_ids), random.randint(1, 2), gia_dv, thoi_gian_dung)
                )
        so_khach_co_dv += 1

    return so_khach_dang_o, so_khach_co_dv, so_them


def lap_ke_hoach_vip(
    luot_cua, khach_ids, ma_khach, muc_tieu_vip, luot_toi_da_vip, ghim, tao_khach_moi
):
    """
    Lập kế hoạch chia lại lượt thuê (chỉ đổi chủ lượt thuê, không đụng tới phòng/thời gian).
    VIP = thuê trên NGUONG_LAN_THUE_VIP lần HOẶC tổng chi trên NGUONG_CHI_TIEU_VIP.

      - Giữ lại tối đa `muc_tieu_vip` khách VIP (ưu tiên khách mẫu rồi khách chi nhiều).
      - Mỗi VIP giữ lại chỉ có tối đa một mức lượt thuê riêng (4 .. luot_toi_da_vip).
      - VIP dư bị hạ về khách thường (<= 3 lượt, <= 20 triệu).
      - Lượt thuê dư được chuyển sang khách thường còn chỗ; hết chỗ thì dồn cho VIP đến
        mức giới hạn của họ; vẫn dư thì tạo thêm khách mới (không bao giờ dồn quá mức).
      - Các lượt "đang ở" được giữ ở các khách khác nhau; lượt trong `ghim` không bị chuyển.

    luot_cua: {ma_kh: [(ma_luot, tien, dang_o), ...]}
    Trả về (cap_nhat, vip_truoc, vip_sau, so_khach_moi), cap_nhat = [(ma_kh_moi, ma_luot), ...]
    """

    def la_vip(so, tong):
        return so > NGUONG_LAN_THUE_VIP or tong > NGUONG_CHI_TIEU_VIP

    tt = {kh: [0, 0.0, 0] for kh in khach_ids}  # [số lượt, tổng chi, số lượt đang ở]
    chu, chu_goc, tien_cua = {}, {}, {}
    dang_o_set, dang_o_cua = set(), {}

    for kh, ds in luot_cua.items():
        st = tt.setdefault(kh, [0, 0.0, 0])
        for ma_luot, tien, dang_o in ds:
            st[0] += 1
            st[1] += tien
            chu[ma_luot] = chu_goc[ma_luot] = kh
            tien_cua[ma_luot] = tien
            if dang_o:
                st[2] += 1
                dang_o_set.add(ma_luot)
                dang_o_cua.setdefault(kh, []).append(ma_luot)

    def bot(ma_luot):
        kh = chu[ma_luot]
        tt[kh][0] -= 1
        tt[kh][1] -= tien_cua[ma_luot]
        if ma_luot in dang_o_set:
            tt[kh][2] -= 1
            dang_o_cua[kh].remove(ma_luot)
        chu[ma_luot] = None

    def dat(ma_luot, kh):
        st = tt.setdefault(kh, [0, 0.0, 0])
        st[0] += 1
        st[1] += tien_cua[ma_luot]
        if ma_luot in dang_o_set:
            st[2] += 1
            dang_o_cua.setdefault(kh, []).append(ma_luot)
        chu[ma_luot] = kh

    vip = [kh for kh, (so, tong, _) in tt.items() if la_vip(so, tong)]
    vip_truoc = len(vip)

    vip.sort(key=lambda kh: (kh != ma_khach, -tt[kh][1]))
    giu_lai = vip[:muc_tieu_vip]
    du = vip[muc_tieu_vip:]
    tap_vip = set(vip)

    thua = []  # các lượt thuê cần chuyển đi

    # VIP dư: chỉ giữ tối đa 3 lượt rẻ nhất sao cho tổng chi <= 20 triệu (để không còn là VIP)
    for kh in du:
        so_giu, tien_giu = 0, 0.0
        ds = sorted(luot_cua[kh], key=lambda x: (x[0] not in ghim, x[1]))
        for ma_luot, tien, _ in ds:
            if ma_luot in ghim or (
                so_giu < NGUONG_LAN_THUE_VIP
                and tien_giu + tien <= NGUONG_CHI_TIEU_VIP
            ):
                so_giu += 1
                tien_giu += tien
            else:
                thua.append(ma_luot)

    for ma_luot in thua:
        bot(ma_luot)

    # VIP giữ lại: mỗi người một mức giới hạn lượt thuê riêng
    cap_cua = {}
    for kh in giu_lai:
        cap = random.randint(
            NGUONG_LAN_THUE_VIP + 1, max(NGUONG_LAN_THUE_VIP + 1, luot_toi_da_vip)
        )
        cap_cua[kh] = cap
        so_bot = tt[kh][0] - cap
        if so_bot > 0:
            co_the = [m for m, _, _ in luot_cua[kh] if m not in ghim]
            for ma_luot in random.sample(co_the, min(so_bot, len(co_the))):
                thua.append(ma_luot)
                bot(ma_luot)

    # Khách thường còn chỗ (để họ không trở thành VIP)
    pool = [
        kh for kh in tt
        if kh not in tap_vip and tt[kh][0] < NGUONG_LAN_THUE_VIP
    ]
    pool += [kh for kh in du if tt[kh][0] < NGUONG_LAN_THUE_VIP]

    def chon_tu_pool(tien, la_dang_o):
        for _ in range(60):
            if not pool:
                return None
            i = random.randrange(len(pool))
            so, tong, n_o = tt[pool[i]]
            if (
                so < NGUONG_LAN_THUE_VIP
                and tong + tien <= NGUONG_CHI_TIEU_VIP
                and not (la_dang_o and n_o)
            ):
                return i
        return None

    def dat_vao_pool(ma_luot, i):
        kh = pool[i]
        dat(ma_luot, kh)
        if tt[kh][0] >= NGUONG_LAN_THUE_VIP:
            pool[i] = pool[-1]
            pool.pop()

    random.shuffle(thua)
    thua.sort(key=lambda m: m not in dang_o_set)  # lượt đang ở được xếp trước

    chua_xep = []
    for ma_luot in thua:
        tien = tien_cua[ma_luot]
        if tien <= NGUONG_CHI_TIEU_VIP:
            i = chon_tu_pool(tien, ma_luot in dang_o_set)
            if i is not None:
                dat_vao_pool(ma_luot, i)
                continue
        chua_xep.append(ma_luot)

    # Hết chỗ ở khách thường: dồn cho VIP nhưng không vượt mức giới hạn của từng người
    con_lai = []
    for ma_luot in chua_xep:
        tien = tien_cua[ma_luot]
        la_o = ma_luot in dang_o_set

        if tien > NGUONG_CHI_TIEU_VIP and giu_lai:
            # lượt quá lớn (tự nó đã khiến khách thành VIP): dồn cho VIP đang ít lượt nhất
            khong_trung = [kh for kh in giu_lai if not (la_o and tt[kh][2])] or giu_lai
            con_cho = [kh for kh in khong_trung if tt[kh][0] < cap_cua[kh]] or khong_trung
            dat(ma_luot, min(con_cho, key=lambda kh: tt[kh][0]))
            continue

        ung_vien = [
            kh for kh in giu_lai
            if tt[kh][0] < cap_cua[kh] and not (la_o and tt[kh][2])
        ]
        if ung_vien:
            dat(ma_luot, random.choice(ung_vien))
        else:
            con_lai.append(ma_luot)

    # Vẫn còn dư: tạo thêm khách mới, mỗi người tối đa 3 lượt
    khach_moi = []

    def tim_khach_moi(tien, la_o):
        for kh in khach_moi:
            so, tong, n_o = tt[kh]
            if (
                so < NGUONG_LAN_THUE_VIP
                and (so == 0 or tong + tien <= NGUONG_CHI_TIEU_VIP)
                and not (la_o and n_o)
            ):
                return kh
        return None

    def them_khach_moi_vao_ke_hoach(so_luong):
        ids = tao_khach_moi(so_luong)
        for kh in ids:
            tt.setdefault(kh, [0, 0.0, 0])
        khach_moi.extend(ids)

    if con_lai:
        them_khach_moi_vao_ke_hoach(
            max(1, math.ceil(len(con_lai) / NGUONG_LAN_THUE_VIP))
        )

        for ma_luot in con_lai:
            tien = tien_cua[ma_luot]
            la_o = ma_luot in dang_o_set
            kh = tim_khach_moi(tien, la_o)
            while kh is None:
                them_khach_moi_vao_ke_hoach(1)
                kh = tim_khach_moi(tien, la_o)
            dat(ma_luot, kh)

    # Mỗi khách chỉ nên có một phòng đang ở: chuyển phần trùng sang khách còn chỗ
    for kh in [k for k, ds in dang_o_cua.items() if len(ds) > 1]:
        for ma_luot in list(dang_o_cua[kh])[1:]:
            if ma_luot in ghim:
                continue
            i = chon_tu_pool(tien_cua[ma_luot], True)
            if i is None:
                break
            bot(ma_luot)
            dat_vao_pool(ma_luot, i)

    vip_sau = sum(1 for so, tong, _ in tt.values() if la_vip(so, tong))
    cap_nhat = [(den, m) for m, den in chu.items() if den != chu_goc[m]]

    return cap_nhat, vip_truoc, vip_sau, len(khach_moi)


def gioi_han_khach_vip(cursor, khach_ids, ma_khach, muc_tieu_vip, luot_toi_da_vip):
    cursor.execute(
        """
        SELECT t.MaLuotThue, t.MaKhachHang, t.TrangThaiDotThue,
               COALESCE(t.TongTienPhong, 0) + COALESCE(SUM(s.ThanhTien), 0)
        FROM ThuePhong t
        LEFT JOIN SuDungDichVu s ON s.MaLuotThue = t.MaLuotThue
        WHERE t.TrangThaiDotThue <> 'HUY'
        GROUP BY t.MaLuotThue, t.MaKhachHang, t.TrangThaiDotThue, t.TongTienPhong
        """
    )

    luot_cua = {}
    ghim = set()  # lượt đang ở của khách mẫu: không được chuyển đi

    for ma_luot, ma_kh, trang_thai, tien in cursor.fetchall():
        dang_o = trang_thai == 'DANGO'
        luot_cua.setdefault(ma_kh, []).append((ma_luot, float(tien), dang_o))
        if dang_o and ma_kh == ma_khach:
            ghim.add(ma_luot)

    def tao_khach_moi(so_luong):
        ids = them_khach_moi(cursor, so_luong)
        khach_ids.extend(ids)
        return ids

    cap_nhat, vip_truoc, vip_sau, so_khach_moi = lap_ke_hoach_vip(
        luot_cua, khach_ids, ma_khach, muc_tieu_vip,
        luot_toi_da_vip, ghim, tao_khach_moi
    )

    if cap_nhat:
        cursor.executemany(
            "UPDATE ThuePhong SET MaKhachHang = %s WHERE MaLuotThue = %s",
            cap_nhat
        )

    return vip_truoc, vip_sau, len(cap_nhat), so_khach_moi


def dong_bo_trang_thai_phong(cursor):
    cursor.execute(
        """
        UPDATE Phong p
        SET p.TrangThai =
            CASE
                WHEN EXISTS (
                    SELECT 1
                    FROM ThuePhong t
                    WHERE t.MaPhong = p.MaPhong
                      AND t.TrangThaiDotThue = 'DANGO'
                      AND t.NgayNhanPhong <= NOW()
                      AND NOW() < COALESCE(t.NgayTraThucTe, t.NgayTraDuKien)
                ) THEN 'DANGSUDUNG'
                WHEN EXISTS (
                    SELECT 1
                    FROM ThuePhong t
                    WHERE t.MaPhong = p.MaPhong
                      AND t.TrangThaiDotThue = 'DADAT'
                      AND t.NgayNhanPhong > NOW()
                      AND t.NgayTraDuKien > NOW()
                ) THEN 'DATTRUOC'
                ELSE 'TRONG'
            END
        """
    )


def in_thong_ke(cursor):
    print()
    print("========== THỐNG KÊ ==========")

    cac_bang = [
        'NhanVien', 'KhachHang', 'LoaiPhong', 'Phong',
        'DichVu', 'ThuePhong', 'SuDungDichVu', 'NhanVienPhucVu'
    ]

    for bang in cac_bang:
        cursor.execute(f"SELECT COUNT(*) FROM {bang}")
        print(f"{bang:<20}: {cursor.fetchone()[0]}")

    print()
    print("----- TRẠNG THÁI LƯỢT THUÊ -----")

    cursor.execute(
        """
        SELECT TrangThaiDotThue, COUNT(*)
        FROM ThuePhong
        GROUP BY TrangThaiDotThue
        """
    )

    for trang_thai, so_luong in cursor.fetchall():
        print(f"{trang_thai:<10}: {so_luong}")

    cursor.execute(
        """
        SELECT COUNT(*) FROM (
            SELECT x.MaKhachHang
            FROM (
                SELECT t.MaKhachHang, t.MaLuotThue,
                       COALESCE(t.TongTienPhong, 0) + COALESCE(SUM(s.ThanhTien), 0) AS Tien
                FROM ThuePhong t
                LEFT JOIN SuDungDichVu s ON s.MaLuotThue = t.MaLuotThue
                WHERE t.TrangThaiDotThue <> 'HUY'
                GROUP BY t.MaLuotThue, t.MaKhachHang, t.TongTienPhong
            ) x
            GROUP BY x.MaKhachHang
            HAVING COUNT(*) > 3 OR SUM(x.Tien) > 20000000
        ) v
        """
    )

    print()
    print(f"Khách VIP thực tế (thuê >3 lần hoặc chi >20 triệu): {cursor.fetchone()[0]}")

    cursor.execute(
        """
        SELECT COUNT(*), COUNT(DISTINCT MaKhachHang)
        FROM ThuePhong
        WHERE TrangThaiDotThue = 'DANGO'
        """
    )
    so_luot_o, so_khach_o = cursor.fetchone()
    print(f"Đang ở hiện tại: {so_luot_o} phòng / {so_khach_o} khách khác nhau")

    cursor.execute(
        """
        SELECT COALESCE(MAX(c), 0) FROM (
            SELECT COUNT(*) AS c
            FROM ThuePhong
            WHERE TrangThaiDotThue <> 'HUY'
            GROUP BY MaKhachHang
        ) x
        """
    )
    print(f"Số lượt thuê nhiều nhất của một khách: {cursor.fetchone()[0]}")

    print()
    print("----- DỊCH VỤ -----")

    cursor.execute(
        """
        SELECT
            d.TenDichVu,
            COUNT(s.MaLuotDung),
            COALESCE(SUM(s.ThanhTien), 0)
        FROM DichVu d
        LEFT JOIN SuDungDichVu s
            ON d.MaDichVu = s.MaDichVu
        GROUP BY d.MaDichVu, d.TenDichVu
        ORDER BY COUNT(s.MaLuotDung) DESC
        """
    )

    for ten, so_luot, doanh_thu in cursor.fetchall():
        print(f"{ten:<25} Lượt: {so_luot:<6} Doanh thu: {doanh_thu:,.0f}")


def chay_doi_ten(chi_khach):
    conn = mysql.connector.connect(**db_config)
    cursor = conn.cursor()

    try:
        so_khach = doi_ten_hien_co(
            cursor, 'KhachHang', 'MaKhachHang', 'Khách Hàng %'
        )
        print(f"Đã đổi tên {so_khach} khách hàng.")

        if not chi_khach:
            so_nv = doi_ten_hien_co(
                cursor, 'NhanVien', 'MaNhanVien', 'Nhân Viên %'
            )
            print(f"Đã đổi tên {so_nv} nhân viên.")

        conn.commit()

    except mysql.connector.Error as err:
        conn.rollback()
        print("LỖI MYSQL:", err)

    finally:
        cursor.close()
        conn.close()


def sinh_du_lieu(
    ngay_toi, reset, ma_khach, giu_ngay, vip_toi_da=None, vip_toi_thieu=None,
    luot_toi_da_vip=SO_LUOT_TOI_DA_VIP,
    dang_o_toi_thieu=SO_KHACH_DANG_O_TOI_THIEU,
    dang_o_toi_da=SO_KHACH_DANG_O_TOI_DA,
):
    # Mỗi lần chạy, số khách VIP là ngẫu nhiên trong [vip_toi_thieu, vip_toi_da]
    if vip_toi_da is None:
        vip_toi_da = SO_VIP_TOI_DA
    if vip_toi_thieu is None:
        vip_toi_thieu = min(SO_VIP_TOI_THIEU, vip_toi_da)
    vip_toi_thieu, vip_toi_da = sorted((vip_toi_thieu, vip_toi_da))
    muc_tieu_vip = random.randint(vip_toi_thieu, vip_toi_da)
    luot_toi_da_vip = max(NGUONG_LAN_THUE_VIP + 1, luot_toi_da_vip)

    # Số khách đang ở (khác nhau) cũng ngẫu nhiên trong khoảng cho trước
    dang_o_toi_thieu, dang_o_toi_da = sorted((dang_o_toi_thieu, dang_o_toi_da))
    muc_tieu_dang_o = random.randint(dang_o_toi_thieu, dang_o_toi_da)

    conn = mysql.connector.connect(**db_config)
    cursor = conn.cursor()

    so_xoa = 0

    try:
        print("Kết nối MySQL thành công.")

        # Dọn trạng thái BAOTRI còn sót từ phiên bản script cũ.
        cursor.execute("UPDATE Phong SET TrangThai = 'TRONG' WHERE TrangThai = 'BAOTRI'")

        if reset:
            xoa_sach_du_lieu(cursor)
            conn.commit()
            print("Đã xóa sạch dữ liệu cũ.")

        cursor.execute("SELECT COUNT(*) FROM Phong")
        lan_dau = cursor.fetchone()[0] == 0

        if lan_dau:
            print("CSDL còn trống: đang tạo dữ liệu gốc...")
            nap_du_lieu_goc(cursor)
            conn.commit()
        else:
            print("CSDL đã có dữ liệu: đang cập nhật...")
            if giu_ngay > 0:
                so_xoa = xoa_du_lieu_cu(cursor, giu_ngay)
                print(f"Đã xóa {so_xoa} lượt thuê cũ hơn {giu_ngay} ngày.")

        cursor.execute("SELECT MaNhanVien FROM NhanVien")
        nv_ids = [x[0] for x in cursor.fetchall()]

        cursor.execute("SELECT MaNhanVien FROM NhanVien WHERE ChucVu = 'Lễ Tân'")
        le_tan_ids = [x[0] for x in cursor.fetchall()]

        cursor.execute(
            """
            SELECT p.MaPhong, lp.DonGia
            FROM Phong p
            JOIN LoaiPhong lp ON lp.MaLoaiPhong = p.MaLoaiPhong
            """
        )
        phong_list = cursor.fetchall()

        cursor.execute("SELECT MaDichVu, DonGia FROM DichVu")
        dich_vu_list = cursor.fetchall()

        if lan_dau:
            khach_moi_ids = []
        else:
            khach_moi_ids = them_khach_moi(cursor, SO_KHACH_MOI_MOI_LAN)

        cursor.execute("SELECT MaKhachHang FROM KhachHang")
        khach_ids = [x[0] for x in cursor.fetchall()]

        now = datetime.now()
        dv_rows = []
        nv_rows = []

        so_chuyen = cap_nhat_luot_thue_hien_co(
            cursor, now, le_tan_ids, nv_ids, dich_vu_list, dv_rows, nv_rows
        )

        so_luot_moi = sinh_bo_du_lieu(
            cursor, now, phong_list, khach_ids, khach_moi_ids,
            le_tan_ids, nv_ids, dich_vu_list, dv_rows, nv_rows, ngay_toi
        )

        cursor.execute("SELECT COUNT(*) FROM ThuePhong")
        tong_luot = cursor.fetchone()[0]

        while tong_luot < SO_LUOT_TOI_THIEU:
            gioi_han = now + timedelta(days=ngay_toi)
            them = sinh_luot_thue(
                cursor, now, gioi_han, phong_list, khach_ids, khach_moi_ids,
                le_tan_ids, nv_ids, dich_vu_list, dv_rows, nv_rows
            )
            if them == 0:
                break
            cursor.execute("SELECT COUNT(*) FROM ThuePhong")
            tong_luot = cursor.fetchone()[0]

        for lo in chia_lo(dv_rows):
            cursor.executemany(
                """
                INSERT INTO SuDungDichVu
                (
                    MaLuotThue, MaDichVu, MaNhanVienPhucVu,
                    SoLuong, DonGiaThoiDiem, ThoiGianSuDung
                )
                VALUES
                (%s, %s, %s, %s, %s, %s)
                """,
                lo
            )

        for lo in chia_lo(nv_rows):
            cursor.executemany(
                """
                INSERT INTO NhanVienPhucVu
                (MaLuotThue, MaNhanVien, LoaiCongViec, ThoiGianThucHien)
                VALUES
                (%s, %s, %s, %s)
                """,
                lo
            )

        so_khach_dango, so_khach_dung_dv, so_dang_o_them = dam_bao_nhieu_khach_dang_o(
            cursor, now, ma_khach, le_tan_ids, nv_ids, dich_vu_list,
            khach_ids, muc_tieu_dang_o
        )

        vip_truoc, vip_sau, so_luot_chuyen_vip, so_khach_them = gioi_han_khach_vip(
            cursor, khach_ids, ma_khach, muc_tieu_vip, luot_toi_da_vip
        )

        dong_bo_trang_thai_phong(cursor)

        conn.commit()

        print()
        print("===================================")
        print("      SINH DỮ LIỆU THÀNH CÔNG")
        print("===================================")
        print(f"Khách hàng mới              : {len(khach_moi_ids)}")
        print(f"Lượt thuê mới               : {so_luot_moi}")
        print(f"Lượt thuê đổi trạng thái    : {so_chuyen}")
        print(f"Lượt sử dụng dịch vụ mới    : {len(dv_rows)}")
        print(f"Khách đang ở (DANGO)        : {so_khach_dango} khách (mục tiêu lần này: {muc_tieu_dang_o}, bổ sung {so_dang_o_them})")
        print(f"Khách đang ở dùng dịch vụ   : {so_khach_dung_dv} khách")
        print(f"Khách VIP           : {vip_sau} (mục tiêu lần này: {muc_tieu_vip}, khoảng {vip_toi_thieu}-{vip_toi_da})")
        print(f"Lượt thuê đã chuyển khách   : {so_luot_chuyen_vip} (mỗi VIP tối đa {luot_toi_da_vip} lượt)")
        print(f"Khách mới tạo thêm          : {so_khach_them}")

        in_thong_ke(cursor)

    except mysql.connector.Error as err:
        conn.rollback()
        print()
        print("LỖI MYSQL:")
        print(err)

    except Exception as err:
        conn.rollback()
        print()
        print("LỖI:")
        print(err)

    finally:
        cursor.close()
        conn.close()


if __name__ == "__main__":

    parser = argparse.ArgumentParser(
        description="Sinh và cập nhật dữ liệu khách sạn"
    )

    parser.add_argument(
        '--ngay-toi', type=int, default=20,
        help="Số ngày tới sinh lịch đặt phòng"
    )

    parser.add_argument(
        '--reset', action='store_true',
        help="Xóa sạch dữ liệu rồi sinh lại"
    )

    parser.add_argument(
        '--giu-du-lieu', action='store_true',
        help="Không reset: giữ dữ liệu hiện có, chỉ cập nhật và sinh thêm"
    )

    parser.add_argument(
        '--giu-ngay', type=int, default=SO_NGAY_LICH_SU,
        help="Tự xóa lượt thuê đã trả cũ hơn số ngày này trước khi sinh thêm"
    )

    parser.add_argument(
        '--ma-khach', type=int, default=1,
        help="Mã khách luôn có phòng đang ở và dịch vụ đang dùng"
    )

    parser.add_argument(
        '--vip-toi-thieu', type=int, default=None,
        help=f"Số khách VIP tối thiểu (mặc định {SO_VIP_TOI_THIEU}); mỗi lần chạy chọn ngẫu nhiên giữa tối thiểu và tối đa"
    )

    parser.add_argument(
        '--vip-toi-da', type=int, default=None,
        help=f"Số khách VIP tối đa (mặc định {SO_VIP_TOI_DA})"
    )

    parser.add_argument(
        '--luot-toi-da-vip', type=int, default=SO_LUOT_TOI_DA_VIP,
        help="Số lượt thuê tối đa của mỗi khách VIP (mỗi VIP một mức ngẫu nhiên từ 4 đến mức này)"
    )

    parser.add_argument(
        '--dang-o-toi-thieu', type=int, default=SO_KHACH_DANG_O_TOI_THIEU,
        help="Số khách (khác nhau) đang ở tối thiểu"
    )

    parser.add_argument(
        '--dang-o-toi-da', type=int, default=SO_KHACH_DANG_O_TOI_DA,
        help="Số khách (khác nhau) đang ở tối đa"
    )

    parser.add_argument(
        '--doi-ten', action='store_true',
        help="Chỉ đổi tên 'Khách Hàng N' / 'Nhân Viên N' đang có sang tên người Việt"
    )

    parser.add_argument(
        '--chi-khach', action='store_true',
        help="Dùng với --doi-ten: chỉ đổi tên khách hàng, giữ nguyên nhân viên"
    )

    args = parser.parse_args()

    if args.doi_ten:
        chay_doi_ten(args.chi_khach)
    else:
        sinh_du_lieu(
            args.ngay_toi, not args.giu_du_lieu, args.ma_khach,
            args.giu_ngay, args.vip_toi_da, args.vip_toi_thieu,
            args.luot_toi_da_vip, args.dang_o_toi_thieu, args.dang_o_toi_da
        )