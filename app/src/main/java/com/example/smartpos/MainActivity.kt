package com.example.smartpos

import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.IsoDep
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
                    
                    // Start TCP connection right after composition
                    LaunchedEffect(Unit) {
                        sharedViewModel.startTcpConnection()
                    }
                    
                    // Set navigation in Controller
                    LaunchedEffect(navController) {
                        sharedViewModel.setNavController(navController)
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
                    NfcAdapter.FLAG_READER_NFC_B or
                    NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
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
            // Read EMV card data
            val emvData = readEmvCardData(tag)
            
            if (emvData != null) {
                Log.d("NFC", "EMV data read successfully")
                Log.d("NFC", "PAN: ${emvData.getMaskedPan()}")
                Log.d("NFC", "Expiry: ${emvData.getFormattedExpiry()}")
                Log.d("NFC", "Scheme: ${emvData.getCardScheme()}")
                Log.d("NFC", "EMV JSON: ${emvData.toJson()}")
                
                // Send to ViewModel
                runOnUiThread {
                    sharedViewModel.onEmvCardRead(emvData)
                }
            } else {
                Log.e("NFC", "Failed to read EMV data from tag")
                runOnUiThread {
                    sharedViewModel.onNfcReadError("Could not read EMV data")
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
     * Read EMV card data from NFC tag
     */
    private fun readEmvCardData(tag: Tag): EmvCardData? {
        // Try IsoDep for EMV contactless cards
        return readIsoDepEmvCard(tag)
    }
    
    /**
     * Read EMV card using IsoDep protocol
     */
    private fun readIsoDepEmvCard(tag: Tag): EmvCardData? {
        val isoDep = IsoDep.get(tag) ?: return null
        
        return try {
            isoDep.connect()
            isoDep.timeout = 5000 // 5 seconds timeout
            
            Log.d("NFC", "IsoDep connected, reading EMV data...")
            
            // 1. SELECT PPSE (Proximity Payment System Environment)
            val ppseResponse = selectPpse(isoDep)
            if (!isResponseSuccess(ppseResponse)) {
                Log.e("NFC", "PPSE selection failed")
                isoDep.close()
                return null
            }
            
            Log.d("NFC", "PPSE Response: ${bytesToHex(ppseResponse)}")
            
            // 2. Parse AID from PPSE response
            val aid = parseAidFromPpse(ppseResponse)
            if (aid == null) {
                Log.e("NFC", "Could not extract AID from PPSE")
                isoDep.close()
                return null
            }
            
            Log.d("NFC", "AID found: ${bytesToHex(aid)}")
            
            // 3. SELECT Application by AID
            val selectAppResponse = selectApplication(isoDep, aid)
            if (!isResponseSuccess(selectAppResponse)) {
                Log.e("NFC", "Application selection failed")
                isoDep.close()
                return null
            }
            
            Log.d("NFC", "Application selected successfully")
            
            // 4. GET PROCESSING OPTIONS (GPO)
            val gpoResponse = getProcessingOptions(isoDep)
            if (!isResponseSuccess(gpoResponse)) {
                Log.e("NFC", "GPO failed")
                isoDep.close()
                return null
            }
            
            Log.d("NFC", "GPO Response: ${bytesToHex(gpoResponse)}")
            
            // 5. READ RECORD to get card data
            val cardRecords = readCardRecords(isoDep)
            
            isoDep.close()
            
            // 6. Combine all EMV data
            val allEmvData = mutableListOf<Byte>()
            allEmvData.addAll(selectAppResponse.toList())
            allEmvData.addAll(gpoResponse.toList())
            cardRecords.forEach { allEmvData.addAll(it.toList()) }
            
            // 7. Parse TLV data
            val emvCardData = EmvCardData.fromTlvBytes(allEmvData.toByteArray())
            
            Log.d("NFC", "EMV parsing complete: ${emvCardData.rawTlvData.size} tags found")
            
            emvCardData
            
        } catch (e: Exception) {
            Log.e("NFC", "Error reading IsoDep EMV: ${e.message}", e)
            try { isoDep.close() } catch (_: Exception) {}
            null
        }
    }
    
    /**
     * SELECT PPSE command
     */
    private fun selectPpse(isoDep: IsoDep): ByteArray {
        val ppse = "2PAY.SYS.DDF01".toByteArray(Charsets.US_ASCII)
        val command = byteArrayOf(
            0x00.toByte(), // CLA
            0xA4.toByte(), // INS (SELECT)
            0x04.toByte(), // P1
            0x00.toByte(), // P2
            ppse.size.toByte() // Lc
        ) + ppse + byteArrayOf(0x00.toByte()) // Le
        
        return isoDep.transceive(command)
    }
    
    /**
     * SELECT Application by AID
     */
    private fun selectApplication(isoDep: IsoDep, aid: ByteArray): ByteArray {
        val command = byteArrayOf(
            0x00.toByte(), // CLA
            0xA4.toByte(), // INS (SELECT)
            0x04.toByte(), // P1
            0x00.toByte(), // P2
            aid.size.toByte() // Lc
        ) + aid + byteArrayOf(0x00.toByte()) // Le
        
        return isoDep.transceive(command)
    }
    
    /**
     * GET PROCESSING OPTIONS command
     */
    private fun getProcessingOptions(isoDep: IsoDep): ByteArray {
        // PDOL: Tag 83 with default values
        val pdol = byteArrayOf(
            0x83.toByte(), 0x00.toByte() // Empty PDOL for simplicity
        )
        
        val command = byteArrayOf(
            0x80.toByte(), // CLA
            0xA8.toByte(), // INS (GPO)
            0x00.toByte(), // P1
            0x00.toByte(), // P2
            pdol.size.toByte() // Lc
        ) + pdol + byteArrayOf(0x00.toByte()) // Le
        
        return isoDep.transceive(command)
    }
    
    /**
     * READ RECORD commands to get card data
     */
    private fun readCardRecords(isoDep: IsoDep): List<ByteArray> {
        val records = mutableListOf<ByteArray>()
        
        // Try reading common SFI (Short File Identifier) and records
        for (sfi in 1..5) {
            for (record in 1..10) {
                try {
                    val command = byteArrayOf(
                        0x00.toByte(), // CLA
                        0xB2.toByte(), // INS (READ RECORD)
                        record.toByte(), // P1 (record number)
                        ((sfi shl 3) or 0x04).toByte(), // P2 (SFI)
                        0x00.toByte() // Le
                    )
                    
                    val response = isoDep.transceive(command)
                    
                    if (isResponseSuccess(response)) {
                        Log.d("NFC", "Read SFI $sfi Record $record: ${bytesToHex(response)}")
                        records.add(response)
                    } else {
                        // No more records in this SFI
                        break
                    }
                } catch (e: Exception) {
                    // Skip failed reads
                    break
                }
            }
        }
        
        return records
    }
    
    /**
     * Parse AID from PPSE response
     */
    private fun parseAidFromPpse(response: ByteArray): ByteArray? {
        // Look for tag 4F (AID) in response
        return findTlvTag(response, byteArrayOf(0x4F))
    }
    
    /**
     * Find TLV tag in data
     */
    private fun findTlvTag(data: ByteArray, searchTag: ByteArray): ByteArray? {
        var index = 0
        
        while (index < data.size - 2) {
            // Check if current position matches search tag
            var tagMatch = true
            for (i in searchTag.indices) {
                if (index + i >= data.size || data[index + i] != searchTag[i]) {
                    tagMatch = false
                    break
                }
            }
            
            if (tagMatch) {
                // Found tag, parse length and value
                val lengthIndex = index + searchTag.size
                if (lengthIndex >= data.size) return null
                
                val length = data[lengthIndex].toInt() and 0xFF
                val valueIndex = lengthIndex + 1
                
                if (valueIndex + length <= data.size) {
                    return data.sliceArray(valueIndex until valueIndex + length)
                }
            }
            
            index++
        }
        
        return null
    }
    
    /**
     * Check if APDU response is successful (SW1SW2 = 9000)
     */
    private fun isResponseSuccess(response: ByteArray): Boolean {
        if (response.size < 2) return false
        val sw1 = response[response.size - 2].toInt() and 0xFF
        val sw2 = response[response.size - 1].toInt() and 0xFF
        return sw1 == 0x90 && sw2 == 0x00
    }
    
    /**
     * Convert bytes to hex string
     */
    private fun bytesToHex(bytes: ByteArray): String {
        return bytes.joinToString("") { "%02X".format(it) }
    }