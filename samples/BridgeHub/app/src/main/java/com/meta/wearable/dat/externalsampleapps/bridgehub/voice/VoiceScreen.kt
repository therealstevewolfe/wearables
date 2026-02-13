package com.meta.wearable.dat.externalsampleapps.bridgehub.voice

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
// (removed pointerInput; using toggle UI)
// (removed detectTapGestures; using toggle UI)
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.meta.wearable.dat.externalsampleapps.bridgehub.R

@Composable
fun VoiceScreen(
    relayUrlState: MutableState<String>,
    modifier: Modifier = Modifier,
) {
  var status by remember { mutableStateOf("disconnected") }
  var lastUserText by remember { mutableStateOf("") }
  var lastAssistantText by remember { mutableStateOf("") }
  var isRecording by remember { mutableStateOf(false) }
  var bytesSent by remember { mutableStateOf(0L) }
  var micLastError by remember { mutableStateOf<String?>(null) }
  var levelPeak by remember { mutableStateOf(0) }

  val audioPlayer = remember { AudioPlayer() }
  val relayClient =
      remember {
        RelayClient(
            onStatus = { status = it },
            onUserText = { lastUserText = it },
            onAssistantText = { lastAssistantText = it },
            onAudioOutputWavChunk = { wavBytes ->
              try {
                audioPlayer.playWavChunk(wavBytes)
              } catch (e: Throwable) {
                Log.w("VoiceScreen", "Audio play failed: ${e.message}")
              }
            },
        )
      }

  val micStreamer =
      remember {
        MicStreamer(
            onPcmChunk = { pcmBytes ->
              // Compute a simple peak meter (0..32767). If this stays ~0, we're sending silence.
              var peak = 0
              var i = 0
              while (i + 1 < pcmBytes.size) {
                val lo = pcmBytes[i].toInt() and 0xff
                val hi = pcmBytes[i + 1].toInt()
                val s = (hi shl 8) or lo
                val v = kotlin.math.abs(s.toShort().toInt())
                if (v > peak) peak = v
                i += 2
              }
              levelPeak = peak

              bytesSent += pcmBytes.size.toLong()
              relayClient.sendAudioInputPcm(pcmBytes)
            },
            onError = { msg -> micLastError = msg },
        )
      }

  DisposableEffect(Unit) {
    onDispose {
      micStreamer.stop()
      relayClient.close()
      audioPlayer.release()
    }
  }

  Column(
      modifier = modifier.fillMaxSize().padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Text(
        text = stringResource(R.string.voice_screen_title),
        style = MaterialTheme.typography.headlineSmall,
    )

    OutlinedTextField(
        value = relayUrlState.value,
        onValueChange = { relayUrlState.value = it },
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.voice_relay_url)) },
        singleLine = true,
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Button(
          onClick = { relayClient.connect(relayUrlState.value) },
          enabled = status != "connected",
          modifier = Modifier.weight(1f),
      ) {
        Text(stringResource(R.string.voice_connect))
      }
      Button(
          onClick = {
            isRecording = false
            micStreamer.stop()
            relayClient.close()
            audioPlayer.stop()
          },
          enabled = status == "connected",
          modifier = Modifier.weight(1f),
      ) {
        Text(stringResource(R.string.voice_disconnect))
      }
    }

    Text(text = "Status: $status")
    Text(text = "Recording: $isRecording | bytesSent: $bytesSent | levelPeak: $levelPeak")
    micLastError?.let { Text(text = "Mic error: $it") }

    Spacer(modifier = Modifier.height(8.dp))

    Text(text = "You: $lastUserText")
    Text(text = "MC: $lastAssistantText")

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Button(
          onClick = { relayClient.sendUserText("ping") },
          enabled = status == "connected" && !isRecording,
          modifier = Modifier.weight(1f),
      ) {
        Text("TEST TEXT")
      }
      Button(
          onClick = {
            // Clear UI
            lastUserText = ""
            lastAssistantText = ""
            micLastError = null
            levelPeak = 0
            bytesSent = 0
          },
          modifier = Modifier.weight(1f),
      ) {
        Text("CLEAR")
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // Toggle-to-talk
    Text(text = "Mic")

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Text(text = if (isRecording) "ON (listening)" else "OFF")

      Switch(
          checked = isRecording,
          enabled = status == "connected",
          onCheckedChange = { checked ->
            if (checked) {
              if (status != "connected") {
                isRecording = false
                return@Switch
              }
              micLastError = null
              bytesSent = 0
              isRecording = true
              audioPlayer.stop()
              relayClient.sendPause()
              relayClient.sendWavHeader(sampleRate = 48000)
              micStreamer.start()
            } else {
              // Stop capture and let the assistant speak.
              micStreamer.stop()
              relayClient.sendResume()
              isRecording = false
            }
          },
      )
    }

    // Big explicit buttons (easier than press/hold on some devices)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Button(
          onClick = {
            if (status != "connected") return@Button
            micLastError = null
            bytesSent = 0
            isRecording = true
            audioPlayer.stop()
            relayClient.sendPause()
            relayClient.sendWavHeader(sampleRate = 48000)
            micStreamer.start()
          },
          enabled = status == "connected" && !isRecording,
          modifier = Modifier.weight(1f).height(64.dp),
      ) {
        Text("START")
      }

      Button(
          onClick = {
            micStreamer.stop()
            relayClient.sendResume()
            isRecording = false
          },
          enabled = status == "connected" && isRecording,
          modifier = Modifier.weight(1f).height(64.dp),
      ) {
        Text("STOP")
      }
    }
  }
}
