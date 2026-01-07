# TCP Connection Configuration Guide

## Tổng quan
Ứng dụng SmartPos đã được cập nhật để kết nối tới TCP endpoint sau khi người dùng chọn tip. Hệ thống sẽ nhận dữ liệu JSON từ server và tự động chuyển hướng tới màn hình tương ứng dựa trên `TransactionType`.

## Luồng hoạt động mới

```
Sale Screen → Tip Screen → Processing Screen → [Route dựa trên TransactionType]
                                               ├─ SALE → Result Screen
                                               ├─ VOID → Void Screen
                                               ├─ QR → QR Screen
                                               └─ REFUND → Refund Screen
```

## Cấu hình TCP Endpoint

Để thay đổi endpoint TCP, chỉnh sửa file:
`app/src/main/java/com/example/smartpos/network/TcpConnectionService.kt`

```kotlin
companion object {
    // Thay đổi các giá trị này theo server của bạn
    private const val SERVER_HOST = "192.168.1.100" // IP của server
    private const val SERVER_PORT = 8080            // Port của server
    private const val SOCKET_TIMEOUT = 30000        // Timeout (ms)
}
```

## Định dạng JSON Response

Server cần trả về JSON với format sau:

```json
{
  "TransactionType": "SALE",
  "amount": "100.00",
  "status": "SUCCESS",
  "message": "Transaction completed",
  "transactionId": "TXN12345"
}
```

### Các TransactionType được hỗ trợ:
- **SALE**: Chuyển tới Result Screen
- **VOID**: Chuyển tới Void Screen
- **QR**: Chuyển tới QR Screen
- **REFUND**: Chuyển tới Refund Screen

## Tính năng chính

### 1. TcpConnectionService
- **Kết nối TCP**: Tự động kết nối tới endpoint khi vào Processing Screen
- **Keep-alive**: Giữ sống connection để nhận data
- **Auto-parse JSON**: Tự động parse response thành TransactionResponse object
- **Error handling**: Xử lý timeout, connection errors, invalid JSON

### 2. ProcessingScreen
- Hiển thị trạng thái kết nối (Connecting, Connected, Waiting)
- Loading indicator trong khi chờ response
- Auto-navigation dựa trên TransactionType
- Retry button khi có lỗi

### 3. PosViewModel Integration
- Quản lý TCP connection state
- Methods: `startTcpConnection()`, `disconnectTcp()`, `retryTcpConnection()`
- StateFlow để observe connection state

## Testing

### Test với Mock Server (Python)

Tạo file `mock_server.py`:

```python
import socket
import json
import time

HOST = '0.0.0.0'
PORT = 8080

def start_server():
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        s.bind((HOST, PORT))
        s.listen()
        print(f'Mock TCP Server listening on {HOST}:{PORT}')
        
        while True:
            conn, addr = s.accept()
            with conn:
                print(f'Connected by {addr}')
                time.sleep(2)  # Simulate processing
                
                # Send response
                response = {
                    "TransactionType": "SALE",
                    "amount": "100.00",
                    "status": "SUCCESS",
                    "message": "Transaction completed",
                    "transactionId": "TXN12345"
                }
                
                conn.sendall(json.dumps(response).encode() + b'\n')
                print(f'Sent response: {response}')

if __name__ == '__main__':
    start_server()
```

Chạy server:
```bash
python mock_server.py
```

### Test với netcat

```bash
# Lắng nghe trên port 8080
nc -l 8080

# Khi app kết nối, gửi JSON:
{"TransactionType":"SALE","amount":"100.00","status":"SUCCESS"}
```

## Permissions

App yêu cầu các permissions sau (đã được thêm vào AndroidManifest.xml):

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

## Troubleshooting

### Lỗi kết nối
- **"Connection refused"**: Kiểm tra server có đang chạy không
- **"Connection timeout"**: Kiểm tra IP/Port có đúng không, firewall có block không
- **"Network unreachable"**: Kiểm tra thiết bị có kết nối mạng không

### Lỗi parse JSON
- **"Invalid JSON"**: Đảm bảo server trả về JSON hợp lệ
- **"Unknown TransactionType"**: TransactionType phải là: SALE, VOID, QR, hoặc REFUND

### Debug logs
Xem logs trong Android Studio Logcat với tag `TcpConnectionService`:

```
adb logcat | grep TcpConnectionService
```

## Customization

### Thay đổi timeout
```kotlin
private const val SOCKET_TIMEOUT = 30000 // 30 seconds
```

### Thay đổi keep-alive interval
```kotlin
private const val KEEP_ALIVE_INTERVAL = 5000L // 5 seconds
```

### Thêm trường mới vào TransactionResponse
Sửa file `TcpConnectionService.kt`:

```kotlin
data class TransactionResponse(
    val transactionType: String,
    val amount: String? = null,
    val status: String? = null,
    val message: String? = null,
    val transactionId: String? = null,
    val customField: String? = null // Thêm trường mới
)
```

## Production Checklist

- [ ] Cập nhật SERVER_HOST và SERVER_PORT với endpoint production
- [ ] Test kết nối với production server
- [ ] Implement SSL/TLS nếu cần (thay Socket bằng SSLSocket)
- [ ] Add retry logic và backoff strategy
- [ ] Implement proper error logging/monitoring
- [ ] Test các edge cases (slow network, connection drops, etc.)
- [ ] Add timeout handling cho các scenarios khác nhau

## Security Notes

**⚠️ Quan trọng**: 
- Connection hiện tại là plain TCP (không mã hóa)
- Để bảo mật, nên implement SSL/TLS trong production
- Không gửi thông tin nhạy cảm qua plain TCP
- Xem xét sử dụng HTTPS/WebSocket cho production environment
