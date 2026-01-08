# SEQUENCE DIAGRAM - CARD READING FLOW

## Luồng Đọc Thẻ NFC và Gửi Tới Bank Connector

```
┌─────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐
│ NFC Tag │    │ MainActivity │ │ EmvModels│  │PosViewModel│  │TcpService│  │   Bank   │
│         │    │              │ │          │    │          │    │          │    │Connector │
└────┬────┘    └──────┬───────┘ └────┬─────┘    └────┬─────┘    └────┬─────┘    └────┬─────┘
     │                │               │               │               │               │
     │ 1. Tap Card    │               │               │               │               │
     │───────────────>│               │               │               │               │
     │                │               │               │               │               │
     │                │ 2. onTagDiscovered()          │               │               │
     │                │───────┐       │               │               │               │
     │                │       │       │               │               │               │
     │                │<──────┘       │               │               │               │
     │                │               │               │               │               │
     │                │ 3. readJsonFromNdef()         │               │               │
     │                │───────────┐   │               │               │               │
     │                │           │   │               │               │               │
     │                │  NDEF.connect()               │               │               │
     │                │  Read payload │               │               │               │
     │                │  Parse JSON   │               │               │               │
     │                │<──────────┘   │               │               │               │
     │                │               │               │               │               │
     │                │ 4. fromTagMap(tagsMap)        │               │               │
     │                │──────────────>│               │               │               │
     │                │               │               │               │               │
     │                │               │ Parse EMV Tags│               │               │
     │                │               │ - BCD → String│               │               │
     │                │               │ - Hex → ASCII │               │               │
     │                │               │<──────┐       │               │               │
     │                │               │       │       │               │               │
     │                │  EmvCardData  │       │       │               │               │
     │                │<──────────────│       │       │               │               │
     │                │               │       │       │               │               │
     │                │ 5. onEmvCardRead(emvData)     │               │               │
     │                │──────────────────────────────>│               │               │
     │                │               │               │               │               │
     │                │               │               │ Update States │               │
     │                │               │               │ - emvCardData │               │
     │                │               │               │ - cardData    │               │
     │                │               │               │ - nfcData     │               │
     │                │               │               │<─────┐        │               │
     │                │               │               │      │        │               │
     │                │               │               │<─────┘        │               │
     │                │               │               │               │               │
     │                │               │               │ 6. Navigate to│               │
     │                │               │               │ CardDetails   │               │
     │                │               │               │               │               │
     │                ╔═══════════════════════════════════════════════════════════════════╗
     │                ║           CardDetailsScreen - User Reviews Card Info             ║
     │                ╚═══════════════════════════════════════════════════════════════════╝
     │                │               │               │               │               │
     │                │               │               │ 7. User Click │               │
     │                │               │               │    Continue   │               │
     │                │               │               │               │               │
     │                │               │               │ sendTransactionToBankConnector()
     │                │               │               │<───────┐      │               │
     │                │               │               │        │      │               │
     │                │               │               │ Create │      │               │
     │                │               │               │ TcpMessage    │               │
     │                │               │               │ {             │               │
     │                │               │               │  msgType: "2" │               │
     │                │               │               │  status: PROC │               │
     │                │               │               │  amount       │               │
     │                │               │               │  cardData     │               │
     │                │               │               │ }             │               │
     │                │               │               │<───────┘      │               │
     │                │               │               │               │               │
     │                │               │               │ 8. sendToBankConnector(msg)   │
     │                │               │               │──────────────>│               │
     │                │               │               │               │               │
     │                │               │               │               │ message.toJson()
     │                │               │               │               │<────┐         │
     │                │               │               │               │     │         │
     │                │               │               │               │<────┘         │
     │                │               │               │               │               │
     │                │               │               │               │ 9. Send TCP   │
     │                │               │               │               │──────────────>│
     │                │               │               │               │               │
     │                │               │               │               │               │ Process
     │                │               │               │               │               │ Transaction
     │                │               │               │               │               │<────┐
     │                │               │               │               │               │     │
     │                │               │               │               │               │     │
     │                │               │               │               │               │     │
     │                │               │               │               │               │<────┘
     │                │               │               │               │               │
     │                │               │               │               │10. Response   │
     │                │               │               │               │<──────────────│
     │                │               │               │               │               │
     │                │               │               │ TransactionResponse           │
     │                │               │               │<──────────────│               │
     │                │               │               │ {             │               │
     │                │               │               │  status: APPR │               │
     │                │               │               │  approvalCode │               │
     │                │               │               │  rrn          │               │
     │                │               │               │ }             │               │
     │                │               │               │               │               │
     │                │               │               │ 11. Navigate  │               │
     │                │               │               │ to Result     │               │
     │                │               │               │               │               │
     │                ╔═══════════════════════════════════════════════════════════════════╗
     │                ║             ResultScreen - Show Success/Error                    ║
     │                ╚═══════════════════════════════════════════════════════════════════╝
     │                │               │               │               │               │
```

---

## Chi Tiết Từng Bước

### **Step 1-3: NFC Reading**
```
NFC Tag → MainActivity → NDEF Reader
- Tag contains JSON with EMV tags
- NDEF format: simple, no complex EMV protocol
- Read payload as UTF-8 string
```

### **Step 4: Parse EMV Tags**
```
Raw JSON → Map<String, String> → EmvCardData

Conversions:
- "5A": "4111111111111111" → pan: "4111111111111111" (BCD)
- "5F20": "4E475559454E..." → cardholderName: "NGUYEN VO VAN" (Hex→ASCII)
- "5F24": "261231" → expiryDate: "261231" (YYMMDD)
- "9F26": "A1B2C3..." → cryptogram: "A1B2C3..." (Raw hex)
```

### **Step 5-6: ViewModel Processing**
```
EmvCardData → Multiple Representations:

1. _emvCardData: Full EMV data (all tags)
2. _cardData: UI-friendly format
   {
     cardHolderName: "NGUYEN VO VAN"
     maskedCardNumber: "**** **** **** 1111"
     expiryDate: "12/26"
     cardScheme: "VISA"
   }
3. _nfcData: JSON string for transmission
   {
     "parsed": {
       "tlv": {...},
       "pan": "...",
       "cardholderName": "..."
     }
   }
```

### **Step 7-9: Send to Bank**
```
TcpMessage Creation:
{
  "msgType": "2",                    // TRANSACTION
  "trmId": "TRM001",                 // Terminal ID
  "status": "PROCESSING",            // Status
  "amount": "100.00",                // Transaction amount
  "transactionId": "uuid...",        // Unique ID
  "cardData": "{...emvData JSON...}" // Full card data
}

Transmission:
- Convert to JSON string
- Send via TCP socket (PrintWriter)
- Line-delimited JSON
```

### **Step 10-11: Bank Response**
```
Expected Response:
{
  "transactionType": "SALE",
  "transactionId": "uuid...",
  "status": "APPROVED",              // or "DECLINED"
  "message": "Transaction approved",
  "amount": "100.00",
  "approvalCode": "123456",          // Auth code
  "rrn": "000000123456"              // Reference number
}

Status Mapping:
- APPROVED → Navigate to success result
- DECLINED → Navigate to error result
- TIMEOUT → Show timeout error
```

---

## Data Flow Diagram

```
┌──────────────┐
│  JSON on Tag │ {"emvTags": {"5A": "411...", "5F20": "4E4..."}}
└──────┬───────┘
       │
       ▼ Parse
┌──────────────┐
│ Map<String>  │ {"5A" → "411...", "5F20" → "4E4..."}
└──────┬───────┘
       │
       ▼ fromTagMap()
┌──────────────┐
│ EmvCardData  │ pan="4111...", name="NGUYEN...", cryptogram="A1B2..."
└──────┬───────┘
       │
       ├──────────────────┬──────────────────┐
       │                  │                  │
       ▼                  ▼                  ▼
┌──────────┐      ┌──────────┐      ┌──────────┐
│ CardData │      │ JSON Str │      │ Display  │
│ (for UI) │      │ (for TCP)│      │ (masked) │
└──────────┘      └────┬─────┘      └──────────┘
                       │
                       ▼ TcpMessage
                ┌──────────────┐
                │ TCP Socket   │
                │ → Bank       │
                └──────┬───────┘
                       │
                       ▼ Response
                ┌──────────────┐
                │ Transaction  │
                │  Response    │
                └──────────────┘
```

---

## State Management Flow

```
┌─────────────────────────────────────────────────┐
│              PosViewModel States                │
├─────────────────────────────────────────────────┤
│                                                 │
│  _isWaitingForNfc: StateFlow<Boolean>          │
│  ├─ true: Waiting for card tap                 │
│  └─ false: Card read or cancelled              │
│                                                 │
│  _emvCardData: StateFlow<EmvCardData?>          │
│  ├─ null: No card data                         │
│  └─ EmvCardData: Full EMV tags + parsed fields │
│                                                 │
│  _cardData: StateFlow<CardData?>                │
│  ├─ null: No card                              │
│  └─ CardData: UI-friendly display data         │
│                                                 │
│  _nfcData: StateFlow<String?>                   │
│  ├─ null: No data                              │
│  └─ JSON: Transmission-ready string            │
│                                                 │
│  _paymentState: StateFlow<PaymentState>         │
│  ├─ Processing: Reading card                   │
│  ├─ Success: Card read OK                      │
│  └─ Error(msg): Failed to read                 │
│                                                 │
└─────────────────────────────────────────────────┘
```

---

## Error Handling Flow

```
┌──────────────┐
│ Error Point  │
└──────┬───────┘
       │
       ├─── NFC Read Failed
       │    │
       │    ├─ No NDEF message → onNfcReadError("Could not read card data")
       │    ├─ Invalid JSON → onNfcReadError("Invalid card format")
       │    └─ Connection fail → onNfcReadError("Connection failed")
       │
       ├─── Parsing Failed
       │    │
       │    ├─ Missing required tags → Show error, retry
       │    └─ Invalid format → Show error message
       │
       ├─── Bank Send Failed
       │    │
       │    ├─ Socket error → _connectionState = Error(...)
       │    ├─ Timeout → onTransactionError("Timeout")
       │    └─ Network error → Show retry dialog
       │
       └─── Bank Response
            │
            ├─ DECLINED → Navigate to error result
            ├─ TIMEOUT → Show timeout message
            └─ ERROR → Show error message
```

---

## Thread Model

```
┌─────────────────────────────────────────────────┐
│                Thread Execution                 │
├─────────────────────────────────────────────────┤
│                                                 │
│  [Main Thread]                                  │
│  ├─ UI Rendering (Compose)                     │
│  ├─ NFC Callbacks (onTagDiscovered)            │
│  └─ Navigation                                  │
│                                                 │
│  [IO Thread] (via viewModelScope)               │
│  ├─ NDEF Reading (Ndef.connect())              │
│  ├─ JSON Parsing                               │
│  ├─ TCP Socket Operations                      │
│  └─ Bank Communication                         │
│                                                 │
│  [Default Dispatcher]                           │
│  ├─ Data Transformations                       │
│  ├─ EMV Parsing                                │
│  └─ State Updates                              │
│                                                 │
└─────────────────────────────────────────────────┘

Thread Switching:
- onTagDiscovered (NFC thread) → runOnUiThread → ViewModel
- ViewModel → viewModelScope.launch(Dispatchers.IO) → TCP
- TCP response → _bankResponse.emit → StateFlow → UI (Main)
```

---

## Timing Diagram

```
Time  │ Action
──────┼─────────────────────────────────────────────
0ms   │ User taps card on device
      │
10ms  │ onTagDiscovered() triggered
      │
50ms  │ NDEF.connect() + read message
      │
80ms  │ JSON parse + EMV data creation
      │
100ms │ onEmvCardRead() → Update states
      │
150ms │ Navigate to CardDetailsScreen
      │
      │ ╔═══════════════════════════════════════╗
      │ ║ User reviews card info (variable time)║
      │ ╚═══════════════════════════════════════╝
      │
5000ms│ User clicks Continue button
      │
5010ms│ Create TcpMessage
      │
5020ms│ sendToBankConnector() → TCP write
      │
      │ ╔════════════════════════════════════════╗
      │ ║ Bank processing (1-3 seconds)          ║
      │ ╚════════════════════════════════════════╝
      │
7000ms│ Receive bank response
      │
7010ms│ Parse TransactionResponse
      │
7020ms│ Navigate to ResultScreen
      │
```

---

## Security Notes

### **Data in Transit**
```
Plain:     {"pan": "4111111111111111", ...}
           ↓
Encrypted: AES-256 or TLS 1.3
           ↓
TCP:       Binary encrypted stream
           ↓
Bank:      Decrypt + Process
```

### **Data at Rest**
```
Memory:    EmvCardData object (cleared after transaction)
Storage:   NEVER store full PAN
Log:       Only masked: "**** **** **** 1111"
```

### **PCI DSS Compliance**
- ✅ No storage of full PAN
- ✅ Masked display
- ✅ Secure transmission
- ⚠️ TODO: Implement encryption
- ⚠️ TODO: Certificate pinning
- ⚠️ TODO: Tokenization

---

## Performance Metrics

```
Operation                  │ Time (avg) │ Max
───────────────────────────┼────────────┼────────
NFC Tag Discovery          │ 10-20ms    │ 50ms
NDEF Read                  │ 30-50ms    │ 100ms
JSON Parse                 │ 5-10ms     │ 20ms
EMV Data Creation          │ 10-20ms    │ 50ms
UI State Update            │ 5ms        │ 10ms
Navigation                 │ 50ms       │ 100ms
TCP Send                   │ 10ms       │ 50ms
Bank Processing            │ 1-3s       │ 10s
Total (tap → result)       │ 5-8s       │ 15s
```
