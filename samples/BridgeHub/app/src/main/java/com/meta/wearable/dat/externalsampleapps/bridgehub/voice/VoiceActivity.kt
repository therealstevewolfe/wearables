package com.meta.wearable.dat.externalsampleapps.bridgehub.voice

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

class VoiceActivity : ComponentActivity() {
  private val permissionLauncher =
      registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op */ }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    // Best-effort: request mic permission if needed.
    if (
        ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) !=
            PackageManager.PERMISSION_GRANTED
    ) {
      permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    setContent {
      val ctx = LocalContext.current
      val defaultUrl = "wss://swimming-mlb-executed-achievements.trycloudflare.com/ws"

      Surface(color = MaterialTheme.colorScheme.background) {
        val relayUrlState = remember { mutableStateOf(defaultUrl) }
        VoiceScreen(
            relayUrlState = relayUrlState,
        )
      }
    }
  }
}
