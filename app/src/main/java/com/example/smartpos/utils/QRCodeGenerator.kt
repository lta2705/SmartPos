package com.example.smartpos.utils

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/**
 * QRCodeGenerator - Utility class để tạo QR code bitmap từ VietQR string
 * 
 * VietQR Format: 
 * - EMVCo QR Code standard
 * - Format: 00020101021238570010A000000727012700069704220114xxxxxxxxxxxx0208QRIBFTTV5303704540510005802VN62xxxxxx6304xxxx
 */
object QRCodeGenerator {
    
    /**
     * Generate QR code bitmap từ VietQR data string
     * 
     * @param content VietQR string (EMVCo format)
     * @param size Kích thước của QR code (width = height)
     * @return Bitmap của QR code, hoặc null nếu có lỗi
     */
    fun generateQRCode(content: String, size: Int = 512): Bitmap? {
        return try {
            val hints = hashMapOf<EncodeHintType, Any>().apply {
                put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M)
                put(EncodeHintType.CHARACTER_SET, "UTF-8")
                put(EncodeHintType.MARGIN, 1) // Margin nhỏ hơn để QR lớn hơn
            }
            
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size, hints)
            
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            
            // Vẽ QR code lên bitmap
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(
                        x, 
                        y, 
                        if (bitMatrix[x, y]) Color.BLACK else Color.WHITE
                    )
                }
            }
            
            bitmap
        } catch (e: Exception) {
            android.util.Log.e("QRCodeGenerator", "Error generating QR code: ${e.message}", e)
            null
        }
    }
    
    /**
     * Validate VietQR format
     * 
     * @param content VietQR string
     * @return true nếu format hợp lệ
     */
    fun isValidVietQR(content: String): Boolean {
        // Basic validation cho VietQR format
        // VietQR bắt đầu với "000201" (Payload Format Indicator)
        return content.startsWith("000201") && content.length >= 50
    }
    
    /**
     * Extract thông tin từ VietQR string (optional, để debug)
     * 
     * @param content VietQR string
     * @return Map chứa các thông tin được parse
     */
    fun parseVietQR(content: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        
        try {
            // VietQR format: TAG + LENGTH + VALUE
            var index = 0
            while (index < content.length - 4) {
                val tag = content.substring(index, index + 2)
                val length = content.substring(index + 2, index + 4).toIntOrNull() ?: break
                
                if (index + 4 + length > content.length) break
                
                val value = content.substring(index + 4, index + 4 + length)
                
                // Map các tag thông dụng
                when (tag) {
                    "00" -> result["Payload Format"] = value
                    "01" -> result["Point of Initiation"] = value
                    "38" -> result["Merchant Account"] = value
                    "52" -> result["Merchant Category"] = value
                    "53" -> result["Currency"] = value
                    "54" -> result["Amount"] = value
                    "58" -> result["Country Code"] = value
                    "62" -> result["Additional Data"] = value
                    "63" -> result["CRC"] = value
                }
                
                index += 4 + length
            }
        } catch (e: Exception) {
            android.util.Log.e("QRCodeGenerator", "Error parsing VietQR: ${e.message}")
        }
        
        return result
    }
}
