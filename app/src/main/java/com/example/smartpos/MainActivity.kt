package com.example.smartpos

import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.example.smartpos.ui.theme.TouchPayTheme
import com.example.smartpos.ui.theme.navigation.PosNavGraph
import com.example.smartpos.viewmodel.PosViewModel

class MainActivity : ComponentActivity(), NfcAdapter.ReaderCallback {

    private var nfcAdapter: NfcAdapter? = null
    private lateinit var sharedViewModel: PosViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        setContent {
            TouchPayTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF121212)
                ) {
                    val navController = rememberNavController()
                    sharedViewModel = viewModel()

                    PosNavGraph(navController = navController, viewModel = sharedViewModel)
                }
            }
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
        if (tag != null) {

            val tagId = tag.id.joinToString("") { "%02X".format(it) }
            Log.d("NFC", "Đã phát hiện thẻ ID: $tagId")

            sharedViewModel.processNfcPayment(tagId)
        }
    }
}