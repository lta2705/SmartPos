# Cập nhật hệ thống - NFC Payment Flow với TCP Integration

## 🎯 Tóm tắt thay đổi

Hệ thống đã được cập nhật để xử lý luồng thanh toán NFC hoàn chỉnh với TCP communication:

### Luồng mới:
```
Sale → Tip → Processing (TCP) → Payment (NFC) → Card Details → Result/Error
                                      ↓
                                  Timeout (30s)
                                      ↓
                                    Home
```

## 📋 Files đã tạo mới

### 1. **TcpModels.kt** - Data models
📁 `app/src/main/java/com/example/smartpos/model/TcpModels.kt`

**Bao gồm:**
- `TcpMessage`: Model cho TCP messages với msgType, status, trmId
- `CardData`: Parse NFC data (pipe-delimited format)
- `TerminalConfig`: Cấu hình terminal

**Message Types:**
```kotlin
MSG_TYPE_INIT = 0         // Initial connection (gửi khi kết nối)
MSG_TYPE_HEARTBEAT = 1     // Keep-alive
MSG_TYPE_TRANSACTION = 2   // Transaction messages
MSG_TYPE_RESPONSE = 3      // Response from server
```

**Transaction Status:**
```kotlin
STATUS_STARTED = "STARTED"       // Gửi khi bắt đầu transaction
STATUS_PROCESSING = "PROCESSING"
STATUS_COMPLETED = "COMPLETED"   // Gửi khi thành công
STATUS_FAILED = "FAILED"         // Gửi khi thất bại
STATUS_TIMEOUT = "TIMEOUT"       // Gửi khi timeout
```

### 2. **CardDetailsScreen.kt** - Màn hình hiển thị thẻ
📁 `app/src/main/java/com/example/smartpos/ui/theme/screens/CardDetailsScreen.kt`

**Features:**
- Hiển thị card visual (gradient background)
- Card holder name
- Masked card number (**** **** **** 1234)
- Expiry date (MM/YY)
- Card scheme (VISA, MASTERCARD, etc.)
- Validation status
- Approve/Decline buttons

### 3. **ErrorScreen.kt** - Màn hình lỗi
📁 `app/src/main/java/com/example/smartpos/ui/theme/screens/ErrorScreen.kt`

**Features:**
- Hiển thị error message
- Error details card
- Back to Home button

## 🔄 Files đã cập nhật

### 1. **TcpConnectionService.kt**
**Thay đổi:**
- ✅ Gửi initial message (msgType=0, trmId) khi connect
- ✅ Method `sendTransactionMessage()` để gửi structured messages
- ✅ Method `getTerminalId()` để lấy terminal ID
- ✅ Method `getTerminalConfig()` để lấy config

**Usage:**
```kotlin
// Gửi initial message (tự động khi connect)
val initMsg = TcpMessage.createInitMessage(trmId)
tcpService.sendTransactionMessage(initMsg)

// Gửi transaction started
val startMsg = TcpMessage.createTransactionStarted(trmId, amount)
tcpService.sendTransactionMessage(startMsg)
```

### 2. **PaymentScreen.kt**
**Thay đổi hoàn toàn:**
- ✅ Gửi msgType=2, status=STARTED khi bắt đầu
- ✅ Chờ đọc NFC (timeout 30 giây)
- ✅ Timeout dialog với button về Home
- ✅ Navigate tới CardDetails khi đọc xong NFC
- ✅ State management cho timeout

**Parameters mới:**
```kotlin
PaymentScreen(
    viewModel: PosViewModel,
    onCardRead: () -> Unit,      // Navigate to CardDetails
    onTimeout: () -> Unit         // Navigate to Home
)
```

### 3. **PosViewModel.kt**
**Thêm mới:**

**States:**
```kotlin
val nfcData: StateFlow<String?>      // Raw NFC data
val cardData: StateFlow<CardData?>    // Parsed card data
```

**Methods:**
```kotlin
// TCP & Transaction
fun sendTransactionStarted(amount: Double)
fun sendTransactionMessage(message: TcpMessage)

// NFC Processing
fun startNfcReading()
fun simulateNfcRead(nfcRawData: String)
fun onNfcTimeout()

// Transaction Results
fun onTransactionSuccess()
fun onTransactionError(reason: String)
fun getCurrentCardData(): CardData?
```

### 4. **NavGraph.kt**
**Routing mới:**
```kotlin
// Processing -> SALE -> Payment (thay vì Result)
onNavigateToSale = {
    navController.navigate("payment")
}

// Payment -> CardDetails
composable("payment") {
    PaymentScreen(
        onCardRead = { navController.navigate("cardDetails") },
        onTimeout = { navController.navigate("home") }
    )
}

// CardDetails -> Result/Error
composable("cardDetails") {
    CardDetailsScreen(
        onSuccess = { navController.navigate("result") },
        onError = { navController.navigate("error") }
    )
}

// Error screen
composable("error") {
    ErrorScreen(onClose = { navController.navigate("home") })
}
```

## 🔧 TCP Message Flow

### 1. Initial Connection
```json
{
  "msgType": 0,
  "trmId": "TRM1234567890"
}
```

### 2. Transaction Started (at Payment screen)
```json
{
  "msgType": 2,
  "trmId": "TRM1234567890",
  "status": "STARTED",
  "amount": "100.00"
}
```

### 3. Transaction Completed (at CardDetails Approve)
```json
{
  "msgType": 2,
  "trmId": "TRM1234567890",
  "status": "COMPLETED",
  "transactionId": "TXN-UUID",
  "cardData": "JOHN DOE|**** **** **** 1234|12/25|VISA"
}
```

### 4. Transaction Failed (at CardDetails Decline)
```json
{
  "msgType": 2,
  "trmId": "TRM1234567890",
  "status": "FAILED",
  "cardData": "Transaction declined"
}
```

### 5. Transaction Timeout
```json
{
  "msgType": 2,
  "trmId": "TRM1234567890",
  "status": "TIMEOUT"
}
```

## 💳 NFC Data Format

**Input Format (Pipe-delimited):**
```
cardHolderName|maskedCardNumber|expiryDate|cardScheme
```

**Example:**
```
JOHN DOE|**** **** **** 1234|12/25|VISA
```

**Parsing:**
```kotlin
val cardData = CardData.fromNfcData(nfcRawData)
// Returns: CardData(
//   cardHolderName = "JOHN DOE",
//   maskedCardNumber = "**** **** **** 1234",
//   expiryDate = "12/25",
//   cardScheme = "VISA"
// )
```

## 🧪 Testing

### Test 1: Mock NFC Read (5 giây)
```kotlin
// Trong PosViewModel.startNfcReading()
delay(5000) // Mock delay
val mockData = "JOHN DOE|**** **** **** 1234|12/25|VISA"
simulateNfcRead(mockData)
```

### Test 2: Timeout (30 giây)
- Vào Payment screen
- Đợi 30 giây
- Timeout dialog xuất hiện
- Click "Go to Home"

### Test 3: Complete Flow
1. **Sale** → nhập amount
2. **Tip** → chọn tip
3. **Processing** → kết nối TCP, nhận TransactionType=SALE
4. **Payment** → gửi msgType=2, đợi NFC
5. **CardDetails** → hiển thị thông tin thẻ
6. Click **Approve** → Result screen
7. Hoặc click **Decline** → Error screen

## 🎨 UI Components

### CardDetailsScreen Components:
- **CardDisplay**: Card visual với gradient
- **CardInformation**: Details trong card
- **InfoRow**: Row hiển thị label-value
- Action buttons: Approve (green) / Decline (red)

### PaymentScreen Components:
- NFC icon với animation
- Amount display
- Progress indicator
- **TimeoutDialog**: Dialog khi timeout

### ErrorScreen Components:
- Large error icon
- Error title
- Error details card
- Back to Home button

## 📝 Configuration

### Terminal ID
```kotlin
// Auto-generated trong TerminalConfig
trmId = "TRM${System.currentTimeMillis()}"
```

### NFC Timeout
```kotlin
// Trong TerminalConfig
nfcTimeout = 30000L // 30 seconds
```

### Card Schemes hỗ trợ:
- VISA
- MASTERCARD / MASTER
- AMEX / AMERICAN EXPRESS
- DISCOVER
- JCB
- UNIONPAY

## 🔍 Debug

### Check NFC Data:
```kotlin
adb logcat | grep "NFC"
```

### Check TCP Messages:
```kotlin
adb logcat | grep "TcpConnectionService"
```

### Check ViewModel State:
```kotlin
viewModel.nfcData.value    // Raw NFC string
viewModel.cardData.value   // Parsed CardData object
```

## ⚠️ Important Notes

### 1. Mock NFC
Hiện tại sử dụng mock NFC data sau 5 giây. Để integrate NFC thực:
```kotlin
// Thay thế trong PosViewModel.startNfcReading()
// Remove mock delay và call real NFC reader
nfcReader.startReading { nfcData ->
    simulateNfcRead(nfcData)
}
```

### 2. Terminal ID
Terminal ID được generate tự động. Trong production, nên:
- Lưu vào SharedPreferences
- Hoặc lấy từ device configuration

### 3. TCP Connection
Đảm bảo Processing screen được gọi trước Payment để có TCP connection sẵn sàng.

### 4. Error Handling
Tất cả errors đều gửi message về server với status=FAILED hoặc TIMEOUT.

## 🚀 Production Checklist

- [ ] Replace mock NFC với real NFC reader
- [ ] Persist terminal ID
- [ ] Add proper error logging
- [ ] Test với các card schemes khác nhau
- [ ] Test timeout scenarios
- [ ] Add analytics tracking
- [ ] Implement retry logic cho failed transactions
- [ ] Add biometric authentication option
- [ ] Test với slow networks
- [ ] Validate card expiry dates properly

## 📦 Summary

**Files mới: 3**
- TcpModels.kt
- CardDetailsScreen.kt
- ErrorScreen.kt

**Files cập nhật: 4**
- TcpConnectionService.kt
- PaymentScreen.kt
- PosViewModel.kt
- NavGraph.kt

**Total changes: 7 files**

✅ **Hoàn tất!** Hệ thống sẵn sàng xử lý NFC payments với TCP integration.
