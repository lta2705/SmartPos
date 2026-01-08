package com.example.smartpos

import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.example.smartpos.model.EmvCardData
import com.example.smartpos.service.TcpService
import com.example.smartpos.ui.theme.TouchPayTheme
import com.example.smartpos.ui.theme.navigation.PosNavGraph
import com.example.smartpos.viewmodel.PosViewModel
import org.json.JSONObject
import java.nio.charset.Charset

class MainActivity : ComponentActivity(), NfcAdapter.ReaderCallback {

    private var nfcAdapter: NfcAdapter? = null
    private lateinit var sharedViewModel: PosViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        startTcpService()

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        setContent {
            TouchPayTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF121212)
                ) {
                    val navController = rememberNavController()
                    sharedViewModel = viewModel()

                    // Set navigation in Controller first
                    LaunchedEffect(navController) {
                        sharedViewModel.setNavController(navController)
                    }

                    // Start TCP connection after ViewModel is initialized
                    LaunchedEffect(Unit) {
                        sharedViewModel.startTcpConnection()
                    }

                    PosNavGraph(navController = navController, viewModel = sharedViewModel)
                }
            }
        }
    }

    private fun startTcpService() {
        val intent = Intent(this, TcpService::class.java)
        try {
            startForegroundService(intent)
        } catch (e: Exception) {
            Log.e("MainActivity", "Cannot initialize TCP service: ${e.message}")
        }
    }

    override fun onResume() {
        super.onResume()
        val options = Bundle()
        options.putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 250)

        nfcAdapter?.enableReaderMode(
            this,
            this,
            NfcAdapter.FLAG_READER_NFC_A or
                    NfcAdapter.FLAG_READER_NFC_B,
            options
        )
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableReaderMode(this)
    }

    override fun onTagDiscovered(tag: Tag?) {
        if (tag == null) return

        Log.d("NFC", "Tag discovered: ${tag.techList.joinToString(", ")}")

        try {
            // Read JSON from NDEF
            val emvData = readJsonFromNdef(tag)

            if (emvData != null) {
                Log.d("NFC", "EMV data read successfully")
                Log.d("NFC", "PAN: ${emvData.getMaskedPan()}")
                Log.d("NFC", "Expiry: ${emvData.getFormattedExpiry()}")
                Log.d("NFC", "Scheme: ${emvData.getCardScheme()}")

                // Send to ViewModel
                runOnUiThread {
                    sharedViewModel.onEmvCardRead(emvData)
                }
            } else {
                Log.e("NFC", "Failed to read card data from tag")
                runOnUiThread {
                    sharedViewModel.onNfcReadError("Could not read card data")
                }
            }
        } catch (e: Exception) {
            Log.e("NFC", "Error reading NFC tag: ${e.message}", e)
            runOnUiThread {
                sharedViewModel.onNfcReadError("Read failed: ${e.message}")
            }
        }
    }

    /**
     * Read JSON from NDEF tag
     * Expected format:
     * {
     *   "emvTags": {
     *     "5A": "4111111111111111",
     *     "5F20": "4E475559454E20564F2056414E",
     *     "5F24": "261231",
     *     ...
     *   }
     * }
     */
    private fun readJsonFromNdef(tag: Tag): EmvCardData? {
        val ndef = Ndef.get(tag) ?: return null

        return try {
            ndef.connect()
            val ndefMessage = ndef.cachedNdefMessage ?: ndef.ndefMessage

            if (ndefMessage == null) {
                Log.e("NFC", "No NDEF message found")
                ndef.close()
                return null
            }

            // Get first record
            val record = ndefMessage.records.firstOrNull()
            if (record == null) {
                Log.e("NFC", "No NDEF records found")
                ndef.close()
                return null
            }

            // Read payload and handle NDEF Text Record format
            // NDEF Text records have format: [status_byte][language_code][text]
            val payload = record.payload
            
            // Check if this is a Text record (TNF=1, Type="T")
            val isTextRecord = record.tnf.toByte() == 0x01.toByte() &&
                               String(record.type, Charset.forName("UTF-8")) == "T"
            
            val jsonString = if (isTextRecord && payload.isNotEmpty()) {
                // First byte is status byte (encoding + language code length)
                val statusByte = payload[0].toInt()
                val languageCodeLength = statusByte and 0x3F // Lower 6 bits = language code length
                
                // Skip status byte + language code to get actual text
                val textStart = 1 + languageCodeLength
                if (textStart < payload.size) {
                    String(payload, textStart, payload.size - textStart, Charset.forName("UTF-8"))
                } else {
                    String(payload, Charset.forName("UTF-8"))
                }
            } else {
                // Not a Text record, read as-is
                String(payload, Charset.forName("UTF-8"))
            }
            
            Log.d("NFC", "JSON from tag: $jsonString")

            // Parse JSON - handle potential double encoding
            var actualJsonString = jsonString.trim()
            
            // Check if the string is double-encoded (starts and ends with quotes)
            if (actualJsonString.startsWith("\"") && actualJsonString.endsWith("\"")) {
                // Remove outer quotes and unescape
                actualJsonString = actualJsonString.substring(1, actualJsonString.length - 1)
                actualJsonString = actualJsonString.replace("\\\"", "\"")
                actualJsonString = actualJsonString.replace("\\\\", "\\")
                Log.d("NFC", "Decoded JSON: $actualJsonString")
            }
            
            val jsonObject = JSONObject(actualJsonString)
            
            // Try to get emvTags - handle both object and string cases
            val emvTags = if (jsonObject.has("emvTags")) {
                val emvTagsValue = jsonObject.get("emvTags")
                when (emvTagsValue) {
                    is JSONObject -> emvTagsValue
                    is String -> JSONObject(emvTagsValue)
                    else -> {
                        Log.e("NFC", "emvTags is not a JSONObject or String: ${emvTagsValue.javaClass}")
                        ndef.close()
                        return null
                    }
                }
            } else {
                Log.e("NFC", "No emvTags field found in JSON")
                ndef.close()
                return null
            }

            // Convert to Map
            val tagsMap = mutableMapOf<String, String>()
            emvTags.keys().forEach { key ->
                tagsMap[key] = emvTags.getString(key)
            }

            Log.d("NFC", "Parsed ${tagsMap.size} EMV tags")

            // Create EmvCardData from tags
            val emvCardData = EmvCardData.fromTagMap(tagsMap)

            ndef.close()
            emvCardData

        } catch (e: Exception) {
            Log.e("NFC", "Error reading NDEF JSON: ${e.message}", e)
            try {
                ndef.close()
            } catch (_: Exception) {}
            null
        }
    }
}