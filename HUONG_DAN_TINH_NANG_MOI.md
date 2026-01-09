# Hướng Dẫn Các Tính Năng Mới - SmartPos

## Tổng Quan
Đã triển khai thành công 7 tính năng mới để cải thiện quản lý giao dịch, xử lý trạng thái và in hóa đơn.

---

## ✅ 1. Tạo Mã Giao Dịch Ngẫu Nhiên

**Chức năng:**
- Khi nhập số tiền bằng bàn phím (không từ server TCP), hệ thống tự động tạo mã giao dịch UUID ngẫu nhiên
- Khi nhận giao dịch từ server, sử dụng mã giao dịch do server cung cấp

**Cách hoạt động:**
```
Nhập từ bàn phím → Tạo UUID mới (ví dụ: a1b2c3d4-e5f6-...)
Nhận từ TCP server → Sử dụng transactionId từ server
```

**Kiểm tra:**
1. Nhập số tiền thủ công
2. Hoàn tất giao dịch
3. Kiểm tra logs: "Generated random transaction ID for keyboard entry"

---

## ✅ 2. Xóa Trạng Thái Khi Quay Lại

**Chức năng:**
- Khi nhấn nút "Quay lại" từ màn hình Payment hoặc QR, toàn bộ trạng thái giao dịch được xóa sạch
- Đảm bảo giao dịch tiếp theo bắt đầu với trạng thái mới hoàn toàn

**Trạng thái được xóa:**
- Số tiền (về 0)
- Tip đã chọn (về 0%)
- Mã giao dịch
- Dữ liệu EMV thẻ
- Tất cả thông tin TCP

**Kiểm tra:**
1. Bắt đầu giao dịch từ SaleScreen
2. Nhập số tiền và tiến tới Payment
3. Nhấn nút quay lại
4. Xác nhận tất cả đã được xóa sạch

---

## ✅ 3. Giao Dịch VOID Gửi Tin Nhắn EMV

**Chức năng:**
- Khi hủy (void) giao dịch SALE, hệ thống gửi tin nhắn EMV đầy đủ tới bank connector
- Tin nhắn bao gồm DE55 với transactionType = "VOID" (0x02)

**Quy trình:**
```
1. Vào màn hình VOID
2. Chọn giao dịch SALE cần hủy
3. Nhấn nút "Void"
4. Hệ thống:
   - Xây dựng tin nhắn EMV DE55
   - Gửi tới bank connector qua TCP
   - Đánh dấu giao dịch gốc là "voided"
   - Tạo bản ghi VOID mới
```

**Kiểm tra:**
1. Hoàn tất giao dịch SALE
2. Vào màn hình VOID
3. Chọn giao dịch và nhấn Void
4. Kiểm tra logs: "VOID transaction sent to bank connector"
5. Xác nhận giao dịch gốc bị đánh dấu voided
6. Xác nhận có bản ghi VOID mới

---

## ✅ 4. Giao Dịch REFUND Gửi Tin Nhắn EMV

**Chức năng:**
- Khi hoàn tiền (refund) giao dịch QR, hệ thống gửi tin nhắn EMV đầy đủ tới bank connector
- Tin nhắn bao gồm DE55 với transactionType = "REFUND" (0x20)

**Quy trình:**
```
1. Vào màn hình REFUND
2. Chọn giao dịch QR cần hoàn tiền
3. Nhấn nút "Refund"
4. Hệ thống:
   - Xây dựng tin nhắn EMV DE55
   - Gửi tới bank connector qua TCP
   - Đánh dấu giao dịch gốc là "voided"
   - Tạo bản ghi REFUND mới
```

**Kiểm tra:**
1. Hoàn tất giao dịch QR
2. Vào màn hình REFUND
3. Chọn giao dịch và nhấn Refund
4. Kiểm tra logs: "REFUND transaction sent to bank connector"
5. Xác nhận giao dịch gốc bị đánh dấu voided
6. Xác nhận có bản ghi REFUND mới

---

## ✅ 5. Sửa Đồng Bộ Tổng Số Dư

**Vấn đề trước đây:**
- Tổng số dư không khớp với danh sách giao dịch
- Tính cả giao dịch VOID/REFUND vào tổng
- Tính cả giao dịch đã bị hủy vào tổng

**Giải pháp:**
```kotlin
totalSum chỉ bao gồm:
- Giao dịch SALE và QR
- Chưa bị hủy (isVoided = false)
- Không bao gồm VOID và REFUND
```

**Ví dụ:**
```
3 giao dịch SALE: 100, 200, 300 VND → Tổng: 600 VND
Hủy giao dịch 200 VND → Tổng: 400 VND
(Giao dịch VOID không được tính vào tổng)
```

**Kiểm tra:**
1. Tạo 3 giao dịch SALE (100, 200, 300)
2. Xác nhận tổng = 600 VND
3. Hủy 1 giao dịch (200)
4. Xác nhận tổng = 400 VND
5. Xác nhận có bản ghi VOID nhưng không ảnh hưởng tổng

---

## ✅ 6. Hiển Thị Mã Giao Dịch Để Phân Biệt

**Chức năng:**
- Mỗi giao dịch hiển thị 8 ký tự đầu của mã UUID
- Giúp phân biệt các giao dịch dễ dàng

**Giao diện:**
```
┌──────────────────────┐
│ Sale Transaction     │
│ ID: a1b2c3d4         │ ← Mã giao dịch (màu xám)
│          100.00 VND  │
└──────────────────────┘
```

**Kiểm tra:**
1. Hoàn tất nhiều giao dịch
2. Vào màn hình Balance
3. Xác nhận mỗi giao dịch hiển thị mã ID riêng
4. Xác nhận mã ID khác nhau cho mỗi giao dịch

---

## ✅ 7. Màn Hình Hóa Đơn

### 7.1 Hóa Đơn Giao Dịch Đơn Lẻ

**Màn hình:** `ReceiptScreen`

**Hiển thị:**
- Mã giao dịch
- Loại giao dịch (SALE, QR, VOID, REFUND)
- Mô tả
- Ngày giờ
- Trạng thái (APPROVED / VOIDED)
- Tổng số tiền

**Nút chức năng:**
- Print Receipt: In hóa đơn (TODO: tích hợp máy in)

### 7.2 Báo Cáo Tổng Số Dư

**Màn hình:** `BalanceReceiptScreen`

**Hiển thị:**
- Ngày báo cáo
- Tổng số giao dịch
- Danh sách chi tiết tất cả giao dịch (có mã ID)
- Tổng cộng (Grand Total)

**Nút chức năng:**
- Print Balance Report: In báo cáo tổng (TODO: tích hợp máy in)

**Cách truy cập:**
```
Màn hình Balance → Nhấn "Print Current Balance" → Màn hình hóa đơn tổng
```

**Kiểm tra:**
1. Vào màn hình Balance
2. Nhấn nút "Print Current Balance"
3. Xác nhận màn hình hóa đơn xuất hiện
4. Xác nhận tất cả giao dịch được hiển thị đúng
5. Xác nhận tổng cộng khớp với màn hình Balance
6. Nhấn nút "Print Balance Report"
7. Kiểm tra log xuất hiện (đợi tích hợp máy in thật)

---

## Thông Tin Kỹ Thuật

### Mã Loại Giao Dịch (Transaction Type)
```
SALE   → 0x00 (Bán hàng)
CASH   → 0x01 (Rút tiền)
VOID   → 0x02 (Hủy giao dịch)
REFUND → 0x20 (Hoàn tiền)
```

### Cấu Trúc Tin Nhắn DE55 EMV
```
Tag 9F02: Số tiền giao dịch (6 bytes BCD)
Tag 9F03: Tiền tip (6 bytes BCD)
Tag 5F2A: Mã tiền tệ (2 bytes BCD)
Tag 9C:   Loại giao dịch (1 byte hex)
Tag 9F1E: Mã terminal (8 bytes ASCII)
Tag 9A:   Ngày giao dịch (3 bytes BCD)
Tag 9F21: Giờ giao dịch (3 bytes BCD)
+ Các EMV tag từ thẻ (9 tags)
```

### Quy Trình Xử Lý Trạng Thái
```
1. Nhận tin nhắn TCP → Lưu transactionId, pcPosId, amount, etc.
2. Hoặc nhập bàn phím → transactionId = null
3. Đọc thẻ NFC → Lưu dữ liệu EMV
4. Gửi tới bank → 
   - Nếu có transactionId: dùng ID đó
   - Nếu null: tạo UUID mới
   - Xây dựng DE55
   - Gửi qua TCP
5. Nhấn quay lại → Xóa toàn bộ trạng thái
```

---

## Files Đã Thay Đổi

1. **PosViewModel.kt**
   - Thêm phương thức `clearTransactionState()`
   - Cập nhật tính toán `totalSum`
   - Cập nhật `voidTransaction()` gửi tin nhắn EMV
   - Cập nhật `refundTransaction()` gửi tin nhắn EMV
   - Cải tiến tạo mã giao dịch ngẫu nhiên

2. **BalanceScreen.kt**
   - Thêm callback `onPrintBalance`
   - Hiển thị mã giao dịch trong danh sách

3. **NavGraph.kt**
   - Thêm gọi `clearTransactionState()` khi quay lại
   - Thêm route `balanceReceipt`

4. **ReceiptScreen.kt** (MỚI)
   - Màn hình hóa đơn giao dịch đơn
   - Màn hình báo cáo tổng số dư

---

## TODO - Phát Triển Tiếp Theo

1. **Tích Hợp Máy In**
   - Kết nối máy in nhiệt (thermal printer)
   - Triển khai chức năng in thực tế
   - Xử lý lỗi in

2. **Tùy Chỉnh Hóa Đơn**
   - Thêm logo cửa hàng
   - Thêm thông tin cửa hàng
   - Thêm mã QR cho hóa đơn điện tử

3. **Truy Cập Hóa Đơn Giao Dịch**
   - Thêm nút "Xem Hóa Đơn" cho mỗi giao dịch
   - Điều hướng tới hóa đơn cụ thể
   - Lịch sử hóa đơn

4. **Xử Lý Phản Hồi Ngân Hàng**
   - Loại bỏ mock response
   - Triển khai TCP listener thật
   - Xử lý trạng thái phê duyệt/từ chối

---

## Danh Sách Kiểm Tra

### Tính Năng 1: Mã Giao Dịch Ngẫu Nhiên
- [ ] Nhập thủ công tạo UUID
- [ ] ID từ server được giữ nguyên
- [ ] ID xuất hiện trong logs
- [ ] ID dùng trong tin nhắn EMV

### Tính Năng 2: Xóa Trạng Thái
- [ ] Quay lại từ SaleScreen xóa trạng thái
- [ ] Quay lại từ QRScreen xóa trạng thái
- [ ] Tất cả biến được reset
- [ ] Giao dịch tiếp theo bắt đầu sạch

### Tính Năng 3: VOID
- [ ] Nút VOID gửi tin nhắn EMV
- [ ] Giao dịch gốc đánh dấu voided
- [ ] Tạo bản ghi VOID mới
- [ ] Tổng số dư cập nhật đúng

### Tính Năng 4: REFUND
- [ ] Nút REFUND gửi tin nhắn EMV
- [ ] Giao dịch gốc đánh dấu voided
- [ ] Tạo bản ghi REFUND mới
- [ ] Tổng số dư cập nhật đúng

### Tính Năng 5: Đồng Bộ Số Dư
- [ ] Tổng chỉ tính SALE và QR
- [ ] Loại trừ giao dịch voided
- [ ] Loại trừ VOID và REFUND
- [ ] Khớp với danh sách hiển thị

### Tính Năng 6: Mã Giao Dịch
- [ ] ID hiển thị dưới tên
- [ ] ID duy nhất cho mỗi giao dịch
- [ ] ID cắt ngắn 8 ký tự
- [ ] ID màu xám, font nhỏ

### Tính Năng 7: Màn Hình Hóa Đơn
- [ ] Nút in điều hướng đúng
- [ ] Hóa đơn tổng hiển thị tất cả
- [ ] Tổng cộng đúng
- [ ] Format hóa đơn đẹp
- [ ] Nút in log thông báo

---

## Tóm Tắt

Tất cả 7 tính năng đã được triển khai thành công:
✅ Tạo mã giao dịch ngẫu nhiên cho nhập bàn phím
✅ Xóa trạng thái khi quay lại
✅ VOID gửi tin nhắn EMV tới ngân hàng
✅ REFUND gửi tin nhắn EMV tới ngân hàng
✅ Sửa đồng bộ tổng số dư
✅ Hiển thị mã giao dịch để phân biệt
✅ Màn hình hóa đơn (đơn lẻ + tổng)

Triển khai tuân thủ best practices Android, quản lý trạng thái đúng cách với StateFlow, xử lý lỗi tốt, và sử dụng US locale nhất quán cho định dạng số.
