# Quick Start Guide - NFC Payment Integration

## 🚀 Luồng hoạt động mới

```
┌─────────┐     ┌──────┐     ┌────────────┐     ┌─────────┐     ┌──────────────┐     ┌────────┐
│  SALE   │ --> │ TIP  │ --> │ PROCESSING │ --> │ PAYMENT │ --> │ CARD DETAILS │ --> │ RESULT │
└─────────┘     └──────┘     └────────────┘     └─────────┘     └──────────────┘     └────────┘
                                    │                  │                  │
                                    │                  │                  └─> DECLINE -> ERROR
                                    │                  │
                                    │                  └─> TIMEOUT (30s) -> HOME
                                    │
                                    └─> ERROR -> HOME
```

## 📡 TCP Messages

### 1️⃣ Khi kết nối (Tự động)
```json
{"msgType": 0, "trmId": "TRM1736179200000"}
```

### 2️⃣ Khi bắt đầu Payment
```json
{
  "msgType": 2,
  "trmId": "TRM1736179200000",
  "status": "STARTED",
  "amount": "100.00"
}
```

### 3️⃣ Khi Approve (Success)
```json
{
  "msgType": 2,
  "trmId": "TRM1736179200000",
  "status": "COMPLETED",
  "transactionId": "uuid-here",
  "cardData": "JOHN DOE|**** **** **** 1234|12/25|VISA"
}
```

### 4️⃣ Khi Decline (Failure)
```json
{
  "msgType": 2,
  "trmId": "TRM1736179200000",
  "status": "FAILED",
  "cardData": "Transaction declined"
}
```

### 5️⃣ Khi Timeout
```json
{
  "msgType": 2,
  "trmId": "TRM1736179200000",
  "status": "TIMEOUT"
}
```

## 💳 NFC Data Format

**Format:**
```
cardHolderName|maskedCardNumber|expiryDate|cardScheme
```

**Ví dụ:**
```
JOHN DOE|**** **** **** 1234|12/25|VISA
JANE SMITH|**** **** **** 5678|03/26|MASTERCARD
ALEX NGUYEN|**** **** **** 9012|08/27|AMEX
```

## 🔧 Integrate NFC Reader thực

### Bước 1: Thay thế mock trong PosViewModel

**Tìm:**
```kotlin
fun startNfcReading() {
    _paymentState.value = PaymentState.Processing
    
    viewModelScope.launch {
        delay(5000)  // <-- XÓA DÒNG NÀY
        val mockNfcData = "JOHN DOE|**** **** **** 1234|12/25|VISA"  // <-- XÓA DÒNG NÀY
        simulateNfcRead(mockNfcData)  // <-- THAY BẰNG CODE DƯỚI
    }
}
```

**Thay bằng:**
```kotlin
fun startNfcReading() {
    _paymentState.value = PaymentState.Processing
    
    // TODO: Integrate your NFC reader here
    // Ví dụ:
    // nfcReader.startReading { nfcRawData ->
    //     simulateNfcRead(nfcRawData)
    // }
}
```

### Bước 2: Callback từ NFC Reader

Khi NFC reader đọc xong thẻ, gọi:
```kotlin
viewModel.simulateNfcRead(nfcRawData)
```

Với `nfcRawData` theo format: `name|card|expiry|scheme`

## 📱 Test Flow

### Test Case 1: Success Flow
1. Vào **Sale** → nhập $100
2. Chọn **Tip** → 10%
3. **Processing** → kết nối TCP
4. **Payment** → đợi 5s (mock NFC)
5. **CardDetails** → hiển thị thẻ VISA
6. Click **Approve** → Result screen

### Test Case 2: Decline Flow
1-5. Giống Test Case 1
6. Click **Decline** → Error screen
7. Click **Back to Home** → Home screen

### Test Case 3: Timeout Flow
1. Vào **Sale** → nhập $100
2. Chọn **Tip** → 10%
3. **Processing** → kết nối TCP
4. **Payment** → đợi 30s (không có NFC)
5. Timeout dialog hiện ra
6. Click **Go to Home** → Home screen

## 🎯 Key Points

### ViewModel Methods
```kotlin
// Bắt đầu đọc NFC
viewModel.startNfcReading()

// Simulate NFC read (call từ NFC reader callback)
viewModel.simulateNfcRead("JOHN DOE|1234|12/25|VISA")

// Get card data hiện tại
val card = viewModel.getCurrentCardData()

// Xử lý thành công
viewModel.onTransactionSuccess()

// Xử lý lỗi
viewModel.onTransactionError("Card declined")
```

### Navigation
```kotlin
// From Processing (when SALE received)
navController.navigate("payment")

// From Payment (NFC read complete)
navController.navigate("cardDetails")

// From CardDetails (Approve)
navController.navigate("result")

// From CardDetails (Decline)
navController.navigate("error")

// From Payment/Error (Timeout/Cancel)
navController.navigate("home")
```

### States to observe
```kotlin
val nfcData by viewModel.nfcData.collectAsState()           // Raw NFC string
val cardData by viewModel.cardData.collectAsState()         // Parsed CardData
val paymentState by viewModel.paymentState.collectAsState() // Processing/Approved/Error
```

## 🛠️ Customization

### Thay đổi timeout (mặc định 30s)
**File:** `model/TcpModels.kt`
```kotlin
data class TerminalConfig(
    val nfcTimeout: Long = 30000L  // <-- Thay đổi ở đây (ms)
)
```

### Thêm card scheme mới
**File:** `model/TcpModels.kt`
```kotlin
fun getCardSchemeLogo(): String {
    return when (cardScheme.uppercase()) {
        "VISA" -> "visa"
        "MASTERCARD" -> "mastercard"
        "NAPAS" -> "napas"  // <-- Thêm ở đây
        else -> "card_default"
    }
}
```

### Custom error messages
**File:** `ui/theme/screens/ErrorScreen.kt` - Thay đổi text theo ý muốn

## 📊 TCP Server Mock (Python)

Để test TCP connection:

```python
import socket
import json
import time

HOST = '0.0.0.0'
PORT = 8080

def handle_client(conn):
    # 1. Nhận initial message
    data = conn.recv(1024).decode()
    print(f"Received: {data}")
    
    # 2. Gửi TransactionType = SALE
    time.sleep(2)
    response = {"TransactionType": "SALE"}
    conn.sendall(json.dumps(response).encode() + b'\n')
    print(f"Sent: {response}")
    
    # 3. Nhận transaction started
    data = conn.recv(1024).decode()
    print(f"Received: {data}")
    
    # 4. Đợi transaction completed/failed
    data = conn.recv(1024).decode()
    print(f"Received: {data}")

with socket.socket() as s:
    s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    s.bind((HOST, PORT))
    s.listen()
    print(f'Server listening on {HOST}:{PORT}')
    
    while True:
        conn, addr = s.accept()
        print(f'Connected: {addr}')
        handle_client(conn)
        conn.close()
```

## 🐛 Troubleshooting

### Lỗi: "NFC data is null"
- Check format: phải có đúng 4 phần cách nhau bởi `|`
- Ví dụ đúng: `JOHN|1234|12/25|VISA`

### Lỗi: "Transaction timeout"
- Kiểm tra NFC reader có được gọi không
- Check log: `adb logcat | grep "NFC"`

### Lỗi: "TCP connection failed"
- Kiểm tra server đang chạy
- Kiểm tra IP/Port trong `TcpConfig.kt`
- Check log: `adb logcat | grep "TcpConnectionService"`

### Card không hiển thị đúng
- Check `CardData.fromNfcData()` parsing
- Verify format của NFC data
- Check log để xem parsed data

## 📝 Checklist Integration

- [ ] Integrate NFC reader trong `PosViewModel.startNfcReading()`
- [ ] Test với thẻ thực
- [ ] Test timeout scenario
- [ ] Test decline flow
- [ ] Test với nhiều card schemes
- [ ] Verify TCP messages được gửi đúng
- [ ] Test error handling
- [ ] Add proper logging
- [ ] Test trên device thật (không phải emulator)
- [ ] Verify terminal ID persistence

## 🎉 Done!

Sau khi integrate NFC reader, hệ thống sẽ:
1. ✅ Gửi msgType=0 khi connect
2. ✅ Gửi msgType=2 STARTED khi vào Payment
3. ✅ Đọc NFC và parse data
4. ✅ Hiển thị card details
5. ✅ Gửi msgType=2 COMPLETED/FAILED dựa trên user action
6. ✅ Handle timeout với dialog
7. ✅ Route đúng tới Result/Error screens

**Support:** Xem file `NFC_PAYMENT_UPDATE.md` để biết chi tiết đầy đủ.
