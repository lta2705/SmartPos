# LUỒNG ĐỌC THẺ NFC VÀ GỬI SANG BANK CONNECTOR

## 📋 Tổng Quan

Hệ thống sử dụng **NDEF (NFC Data Exchange Format)** để đọc dữ liệu thẻ từ JSON format, sau đó kết hợp với thông tin giao dịch và gửi sang Bank Connector để xử lý.

---

## 🔄 Quy Trình Chi Tiết

### **BƯỚC 1: NFC Tag Discovery** 
📁 File: `MainActivity.kt` - Phương thức: `onTagDiscovered(tag: Tag?)`

```kotlin
override fun onTagDiscovered(tag: Tag?) {
    // 1.1: Kiểm tra tag hợp lệ
    if (tag == null) return
    
    // 1.2: Log thông tin tag
    Log.d("NFC", "Tag discovered: ${tag.techList.joinToString(", ")}")
    
    // 1.3: Đọc JSON từ NDEF
    val emvData = readJsonFromNdef(tag)
    
    // 1.4: Gửi dữ liệu lên ViewModel
    if (emvData != null) {
        runOnUiThread {
            sharedViewModel.onEmvCardRead(emvData)
        }
    } else {
        runOnUiThread {
            sharedViewModel.onNfcReadError("Could not read card data")
        }
    }
}
```

**Đầu vào:**
- NFC Tag được phát hiện khi khách hàng chạm thẻ

**Đầu ra:**
- `EmvCardData` object chứa tất cả EMV tags
- Hoặc error message nếu đọc thất bại

---

### **BƯỚC 2: Đọc JSON từ NDEF**
📁 File: `MainActivity.kt` - Phương thức: `readJsonFromNdef(tag: Tag)`

```kotlin
private fun readJsonFromNdef(tag: Tag): EmvCardData? {
    val ndef = Ndef.get(tag) ?: return null
    
    // 2.1: Kết nối với tag
    ndef.connect()
    
    // 2.2: Đọc NDEF message
    val ndefMessage = ndef.cachedNdefMessage ?: ndef.ndefMessage
    
    // 2.3: Lấy payload từ record đầu tiên
    val record = ndefMessage.records.firstOrNull()
    val payload = record.payload
    val jsonString = String(payload, Charset.forName("UTF-8"))
    
    // 2.4: Parse JSON
    val jsonObject = JSONObject(jsonString)
    val emvTags = jsonObject.getJSONObject("emvTags")
    
    // 2.5: Convert to Map
    val tagsMap = mutableMapOf<String, String>()
    emvTags.keys().forEach { key ->
        tagsMap[key] = emvTags.getString(key)
    }
    
    // 2.6: Tạo EmvCardData từ tags map
    val emvCardData = EmvCardData.fromTagMap(tagsMap)
    
    ndef.close()
    return emvCardData
}
```

**Format JSON trên thẻ:**
```json
{
  "emvTags": {
    "4F": "A000000003",
    "5A": "4111111111111111",
    "5F20": "4E475559454E20564F2056414E",
    "5F24": "261231",
    "5F30": "0201",
    "82": "0000",
    "9F1A": "0704",
    "9F36": "0001",
    "9F10": "06010A03000000"
  }
}
```

**Lưu ý:** Hệ thống chỉ lưu các EMV tags cần thiết (9 tags) để tối ưu dữ liệu và bảo mật.

**Đầu ra:**
- `EmvCardData` object với tất cả tags được parse

---

### **BƯỚC 3: Parse EMV Tags → EmvCardData**
📁 File: `EmvModels.kt` - Phương thức: `EmvCardData.fromTagMap(tagsMap: Map<String, String>)`

```kotlin
fun fromTagMap(tagsMap: Map<String, String>): EmvCardData {
    return EmvCardData(
        rawTlvData = tagsMap,
        pan = tagsMap[EmvTags.TAG_PAN]?.let { bcdToString(hexToBytes(it)) },
        cardholderName = tagsMap[EmvTags.TAG_CARDHOLDER_NAME]?.let { hexToAscii(it) },
        expiryDate = tagsMap[EmvTags.TAG_EXPIRY_DATE],
        panSequence = tagsMap[EmvTags.TAG_PAN_SEQUENCE],
        aid = tagsMap[EmvTags.TAG_AID],
        applicationLabel = tagsMap[EmvTags.TAG_APP_LABEL]?.let { hexToAscii(it) },
        cryptogram = tagsMap[EmvTags.TAG_APP_CRYPTOGRAM],
        atc = tagsMap[EmvTags.TAG_APP_TRANSACTION_COUNTER],
        aip = tagsMap[EmvTags.TAG_APPLICATION_INTERCHANGE_PROFILE],
        tvr = tagsMap[EmvTags.TAG_TERMINAL_VERIFICATION],
        tsi = tagsMap[EmvTags.TAG_TSI],
        cvmResults = tagsMap[EmvTags.TAG_CVM_RESULTS],
        iad = tagsMap[EmvTags.TAG_ISSUER_APP_DATA]
    )
}
```

**Xử lý dữ liệu:**
- **BCD Conversion**: PAN được convert từ BCD (Binary Coded Decimal) sang string
- **Hex to ASCII**: Tên chủ thẻ được convert từ hex sang ASCII
- **Raw Tags**: Tất cả tags gốc được lưu trong `rawTlvData` map

**Kết quả:**
```kotlin
EmvCardData(
    pan = "4111111111111111",
    cardholderName = "NGUYEN VO VAN",
    expiryDate = "261231",
    cryptogram = "A1B2C3D4E5F6G7H8",
    atc = "0001",
    // ... các fields khác
)
```

---

### **BƯỚC 4: ViewModel Xử Lý EMV Data**
📁 File: `PosViewModel.kt` - Phương thức: `onEmvCardRead(emvData: EmvCardData)`

```kotlin
fun onEmvCardRead(emvData: EmvCardData) {
    if (!_isWaitingForNfc.value) return
    
    // 4.1: Cập nhật state
    _isWaitingForNfc.value = false
    _emvCardData.value = emvData
    
    // 4.2: Convert EmvCardData → CardData (cho UI)
    _cardData.value = CardData.fromEmvData(emvData)
    
    // 4.3: Convert to JSON string (cho transmission)
    _nfcData.value = emvData.toJson()
    
    // 4.4: Trigger navigation success
    onTransactionSuccess()
}
```

**Data Transformation:**

**EmvCardData → CardData:**
```kotlin
data class CardData(
    val cardHolderName: String,      // "NGUYEN VO VAN"
    val maskedCardNumber: String,    // "**** **** **** 1111"
    val expiryDate: String,          // "12/26"
    val cardScheme: String,          // "VISA" / "MASTERCARD" / "AMEX"
    val emvData: EmvCardData?        // Full EMV data reference
)
```

**EmvCardData → JSON String:**
```json
{
  "parsed": {
    "tlv": {
      "5A": "4111111111111111",
      "5F20": "4E475559454E20564F2056414E",
      "5F24": "261231",
      // ... all tags
    },
    "pan": "4111111111111111",
    "cardholderName": "NGUYEN VO VAN",
    "expiryDate": "12/26",
    "aid": "A0000000031010",
    "applicationLabel": "VISA CREDIT"
  }
}
```

---

### **BƯỚC 5: Navigation → CardDetailsScreen**
📁 File: `NavGraph.kt`

Sau khi đọc thẻ thành công:
```kotlin
composable("payment") {
    PaymentScreen(
        onSuccess = { 
            navController.navigate("cardDetails") 
        }
    )
}

composable("cardDetails") {
    val cardData = viewModel.cardData.collectAsState().value
    if (cardData != null) {
        CardDetailsScreen(
            cardData = cardData,
            viewModel = viewModel,
            onSuccess = { navController.navigate("result/success") },
            onError = { navController.navigate("result/error") }
        )
    }
}
```

---

### **BƯỚC 6: Hiển thị Card Details & Confirm**
📁 File: `CardDetailsScreen.kt`

```kotlin
@Composable
fun CardDetailsScreen(
    cardData: CardData,
    viewModel: PosViewModel,
    onSuccess: () -> Unit,
    onError: () -> Unit
) {
    // 6.1: Hiển thị thông tin thẻ
    CardDisplay(cardData)
    CardInformation(cardData)
    
    // 6.2: Xử lý nút Continue
    val handleContinue = {
        viewModel.sendTransactionToBankConnector(
            cardData = cardData,
            onSuccess = { response ->
                if (response.status == "APPROVED") {
                    onSuccess()
                } else {
                    onError()
                }
            },
            onError = { error ->
                onError()
            }
        )
    }
    
    // Button Continue
    Button(onClick = handleContinue) {
        Text("Continue")
    }
}
```

**UI Display:**
- Thông tin thẻ được mask an toàn
- Hiển thị card scheme logo (Visa, Mastercard, etc.)
- Button "Continue" để gửi lên Bank

---

### **BƯỚC 7: Gửi Transaction Tới Bank Connector**
📁 File: `PosViewModel.kt` - Phương thức: `sendTransactionToBankConnector()`

```kotlin
fun sendTransactionToBankConnector(
    cardData: CardData,
    onSuccess: (TransactionResponse) -> Unit,
    onError: (String) -> Unit
) {
    viewModelScope.launch {
        try {
            val totalAmount = getTotalAmount()
            val transactionId = UUID.randomUUID().toString()
            val emvData = _emvCardData.value
            
            if (emvData == null) {
                onError("No EMV data available")
                return@launch
            }
            
            // 7.1: Build DE55 EMV message
            val de55 = EmvMessageBuilder.buildDE55(
                emvData = emvData,
                amount = totalAmount,
                transactionDate = getCurrentDate(),
                transactionTime = getCurrentTime()
            )
            
            // 7.2: Tạo TCP Message với DE55
            val message = TcpMessage(
                msgType = TcpMessage.MSG_TYPE_TRANSACTION,
                trmId = tcpService.getTerminalId(),
                transactionType = tcpService.
                status = TcpMessage.STATUS_PROCESSING,
                amount = String.format("%.2f", totalAmount),
                transactionId = transactionId,
                cardData = de55
            )
            
            // 7.3: Kiểm tra kết nối bank connector
            val currentState = tcpConnectionState.value
            if (currentState !is TcpConnectionState.Connected) {
                Log.e(TAG, "Bank connector not connected")
                onError("Bank connector unavailable")
                return@launch
            }
            
            // 7.4: Gửi tới bank connector qua TCP
            tcpService.sendToBankConnector(message)
            
            // 7.5: Giả lập response từ bank (TODO: implement real listener)
            delay(2000)
            
            val mockResponse = TransactionResponse(
                transactionType = "SALE",
                amount = String.format("%.2f", totalAmount),
                status = "APPROVED",
                message = "Transaction approved",
                transactionId = transactionId
            )
            
            onSuccess(mockResponse)
            
        } catch (e: Exception) {
            onError("Failed: ${e.message ?: "Connection error"}")
        }
    }
}
```

**DE55 Message Format gửi tới Bank:**
```json
{
  "emvData": {
    "de55": "4F0AA0000000031010...9F36020001...",
    "de55Length": 128,
    "parsed": {
      "pan": "4111111111111111",
      "cardholderName": "NGUYEN VO VAN",
      "expiryDate": "261231",
      "amount": "000000010000",
      "transactionDate": "260108",
      "transactionTime": "143022",
      "currency": "0704",
      "aid": "A0000000031010",
      "atc": "0001"
    },
    "tags": {
      "4F": "A0000000031010",
      "5A": "4111111111111111",
      "5F20": "4E475559454E20564F2056414E",
      "5F24": "261231",
      "5F30": "0201",
      "82": "0000",
      "9A": "260108",
      "9C": "00",
      "9F02": "000000010000",
      "9F1A": "0704",
      "9F21": "143022",
      "9F36": "0001",
      "9F10": "06010A03000000"
    }
  }
}
```

**Error Handling:**
- Kiểm tra EMV data có tồn tại
- Kiểm tra bank connector connection status
- Nếu không kết nối → return "Bank connector unavailable"
- Catch exceptions → return "Failed: {error}"

---

### **BƯỚC 8: TCP Service Gửi Message**
📁 File: `TcpConnectionService.kt` - Phương thức: `sendToBankConnector()`

```kotlin
suspend fun sendToBankConnector(message: TcpMessage) = withContext(Dispatchers.IO) {
    try {
        // 8.1: Convert message to JSON
        val jsonString = message.toJson()
        
        // 8.2: Gửi qua TCP socket
        writer?.println(jsonString)
        writer?.flush()
        
        Log.d(TAG, "Sent to bank connector: $jsonString")
        
    } catch (e: Exception) {
        Log.e(TAG, "Error sending to bank connector", e)
        _connectionState.value = TcpConnectionState.Error("Failed to send to bank: ${e.message}")
    }
}
```

**Socket Configuration:**
- Host: `TcpConfig.BANK_CONNECTOR_HOST` (TODO: cần config)
- Port: `TcpConfig.BANK_CONNECTOR_PORT` (TODO: cần config)
- Protocol: TCP socket với PrintWriter/BufferedReader
- Format: JSON string, mỗi message 1 dòng (line-delimited)

---

### **BƯỚC 9: Bank Connector Response (TODO)**
📝 **Hiện tại đang mock response, cần implement:**

```kotlin
// TODO: Implement bank connector response listener
private suspend fun listenForBankResponse() {
    val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
    
    while (isActive) {
        val response = reader.readLine() ?: break
        val jsonResponse = JSONObject(response)
        
        // Parse response
        val transactionResponse = TransactionResponse(
            transactionType = jsonResponse.getString("transactionType"),
            amount = jsonResponse.getString("amount"),
            status = jsonResponse.getString("status"),
            message = jsonResponse.getString("message"),
            transactionId = jsonResponse.getString("transactionId"),
            approvalCode = jsonResponse.optString("approvalCode"),
            rrn = jsonResponse.optString("rrn")
        )
        
        // Emit response to ViewModel
        _bankResponse.emit(transactionResponse)
    }
}
```

**Expected Response Format:**
```json
{
  "transactionType": "SALE",
  "transactionId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "status": "APPROVED",
  "message": "Transaction approved",
  "amount": "100.00",
  "approvalCode": "123456",
  "rrn": "000000123456"
}
```

---

### **BƯỚC 10: Result Screen**
📁 File: `NavGraph.kt` → `ResultScreen.kt`

```kotlin
composable("result/{status}") { backStackEntry ->
    val status = backStackEntry.arguments?.getString("status")
    ResultScreen(
        success = status == "success",
        onDone = {
            viewModel.reset()
            navController.navigate("home") {
                popUpTo("home") { inclusive = true }
            }
        }
    )
}
```

---

## 📊 Sơ Đồ Luồng Dữ Liệu

```
┌─────────────┐
│  NFC Tag    │ (JSON in NDEF format)
└──────┬──────┘
       │
       │ onTagDiscovered()
       ▼
┌─────────────────────────┐
│  readJsonFromNdef()     │
│  - Đọc NDEF message     │
│  - Parse JSON           │
│  - Convert to Map       │
└──────────┬──────────────┘
           │
           │ EmvCardData.fromTagMap()
           ▼
┌─────────────────────────┐
│  EmvCardData            │
│  - pan: String          │
│  - cardholderName       │
│  - expiryDate           │
│  - cryptogram           │
│  - rawTlvData: Map      │
└──────────┬──────────────┘
           │
           │ onEmvCardRead()
           ▼
┌─────────────────────────┐
│  PosViewModel           │
│  - _emvCardData         │
│  - _cardData (for UI)   │
│  - _nfcData (JSON)      │
└──────────┬──────────────┘
           │
           │ Navigation
           ▼
┌─────────────────────────┐
│  CardDetailsScreen      │
│  - Display card info    │
│  - Button: Continue     │
└──────────┬──────────────┘
           │
           │ sendTransactionToBankConnector()
           ▼
┌─────────────────────────┐
│  TcpMessage             │
│  - msgType: "2"         │
│  - status: PROCESSING   │
│  - amount               │
│  - transactionId        │
│  - cardData (JSON)      │
└──────────┬──────────────┘
           │
           │ sendToBankConnector()
           ▼
┌─────────────────────────┐
│  TCP Socket             │
│  → Bank Connector       │
└──────────┬──────────────┘
           │
           │ Response (TODO)
           ▼
┌─────────────────────────┐
│  TransactionResponse    │
│  - status: APPROVED     │
│  - approvalCode         │
│  - rrn                  │
└──────────┬──────────────┘
           │
           │ Navigation
           ▼
┌─────────────────────────┐
│  ResultScreen           │
│  - Success / Error      │
└─────────────────────────┘
```

---

## 📁 Danh Sách Files Liên Quan

### **1. MainActivity.kt**
- **Chức năng**: NFC tag discovery & NDEF r, build DE55 và gửi tới bank

### **7. NavGraph.kt**
- **Chức năng**: Navigation routing
- **Routes liên quan**:
  - `payment`: Màn hình chờ tap thẻ
  - `cardDetails`: Màn hình xác nhận thẻ
  - `result/{status}`: Màn hình kết quả

### **8. EmvMessageBuilder.kt** ⭐ **NEW**
- **Chức năng**: Build DE55 EMV messages cho ISO8583
- **Methods chính**:
  - `buildDE55()`: Build complete DE55 with transaction data
  - `buildTLVHex()`: Encode tags to TLV hex format
  - `buildISO8583Message()`: Build full ISO8583 structure (reference)
  - `parseDE55()`: Parse DE55 for testing/debugginging
- **Classes**:
  - `EmvCardData`: Chứa tất cả EMV tags và parsed data
  - `EmvTags`: Định nghĩa constants cho EMV tag IDs
- **Methods chính**:
  - `fromTagMap(tagsMap)`: Parse tags map → EmvCardData
  - `toJson()`: Convert EmvCardData → JSON string
  - `bcdToString()`: Convert BCD → readable string
  - `hexToAscii()`: Convert hex → ASCII text

### **3. PosViewModel.kt**
- **Chức năng**: Business logic & state management
- **State Fields**:
  - `_emvCardData`: Full EMV data
  - `_cardData`: UI-friendly card data
  - `_nfcData`: JSON string for transmission
- **Methods chính**:
  - `onEmvCardRead(emvData)`: Xử lý EMV data từ NFC
  - `sendTransactionToBankConnector()`: Gửi transaction tới bank
  - `onTransactionSuccess()` (với xử lý language code prefix)
2. ✅ Parse EMV tags thành EmvCardData (chỉ lưu 9 tags cần thiết)
3. ✅ Convert EMV data sang CardData cho UI
4. ✅ Build DE55 EMV message với EmvMessageBuilder utility
5. ✅ Gửi transaction message qua TCP với DE55 format
6. ✅ Navigation flow hoàn chỉnh
7. ✅ UI hiển thị card details
8. ✅ Amount hiển thị đúng từ handleIncomingTransaction
9. ✅ Error handling khi bank connector unavailablemat cho TCP communication
  - `CardData`: UI-friendly card data model
  - `TransactionResponse`: Response từ bank
- **Methods chính**:
  - `toJson()`: Convert message → JSON string
  - `CardData.fromEmvData()`: EmvCardData → CardData

### **5. TcpConnectionService.kt**
- **Chức năng**: TCP socket connection management
- **Methods chính**:
  - `sendToBankConnector(message)`: Gửi message qua TCP
  - `connect()`: Kết nối TCP với retry logic
  - `sendTransactionMessage()`: Gửi transaction message

### **6. CardDetailsScreen.kt**
- **Chức năng (chỉ 9 tags cần thiết):
```json
{
  "emvTags": {
    "4F": "A0000000031010",
    "5A": "4111111111111111",
    "5F20": "4E475559454E20564F2056414E",
    "5F24": "261231",
    "5F30": "0201",
    "82": "0000",
    "9F1A": "0704",
    "9F36": "0001",
    "9F10": "06010A03000000
## 🔧 Configuration Files

### **TcpConfig.kt** (TODO: cần tạo)
```kotlin
object TcpConfig {
    // Main server
    const val TCP_HOST = "127.0.0.1"
    const val TCP_PORT = 8089
    
    // Bank connector (TODO: configure)
    const val BANK_CONNECTOR_HOST = "192.168.1.100"
    const val BANK_CONNECTOR_PORT = 8090
    
    const val CONNECTION_TIMEOUT = 10000
    const val RETRY_DELAY = 5000L
}
```

---

## 🎯 Điểm Quan Trọng

### ✅ **Đã Hoàn Thành**
1. ✅ Đọc JSON từ NDEF format
2. ✅ Parse EMV tags thành EmvCardData
3. ✅ Convert EMV data sang CardData cho UI
4. ✅ Convert EMV data sang JSON string cho transmission
5. ✅ Gửi transaction message qua TCP
6. ✅ Navigation flow hoàn chỉnh
7. ✅ UI hiển thị card details

### ⚠️ **TODO - Cần Implement**
1. ❌ **Bank Connector Response Listener**: Hiện tại đang mock response
2. ❌ **Separate Bank Connector Socket**: Có thể cần socket riêng cho bank
3. ❌ **Error Handling**: Handle network errors, timeout
4. ❌ **Retry Logic**: Retry khi gửi bank failed
5. ❌ **Transaction Status Tracking**: Track transaction state trong DB
6. ❌ **Receipt Printing**: In biên lai sau khi approved

---

## 🚀 Cách Sử Dụng

### **Tạo Test NFC Tag:**
1. Sử dụng app **NFC Tools** (Android)
2. Chọn "Write" → "Add a record" → "Text"
3. Paste JSON:
```json
{
  "emvTags": {
    "5A": "4111111111111111",
    "5F20": "4E475559454E20564F2056414E",
    "5F24": "261231",
    "5F30": "0201",
    "82": "0000",
    "9F02": "000000001000",
    "9F1A": "0704",
    "9F37": "12345678",
    "9F36": "0001",
    "9F10": "06010A03000000",
    "9F26": "A1B2C3D4E5F6G7H8"
  }
}
```
4. Write to NFC tag
5. Tap tag vào thiết bị POS

### **Test Flow:**
1. Khởi động app
2. TCP auto-connect tới server
3. Server gửi transaction message (SALE)
4. App navigate tới Payment screen
5. Tap thẻ NFC
6. App đọc JSON, parse EMV data
7. Navigate tới CardDetailsScreen
8. Click "Continue"
9. App gửi transaction + EMV data tới Bank Connector
10. Nhận response → Navigate tới Result screen

---

## 📝 Notes

- **NDEF format** đơn giản hơn EMV protocol phức tạp (PPSE, SELECT, GPO, READ RECORD)
- **JSON format** dễ tạo test data và debug
- **All EMV tags** vẫn được giữ nguyên trong `rawTlvData` map
- **Bank Connector** hiện đang mock, cần implement real connection
- **Security**: Production cần encrypt cardData trước khi gửi

---

## 🔐 Security Considerations (TODO)

1. **Encryption**: Encrypt cardData với TLS/SSL
2. **Tokenization**: Replace PAN với token
3. **PCI Compliance**: Không log/store PAN trong plain text
4. **Certificate Pinning**: Verify bank connector certificate
5. **Secure Storage**: Use Android Keystore cho sensitive data
