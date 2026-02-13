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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
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
              relayClient.sendAudioInput(pcmBytes)
            },
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

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      Button(
          onClick = { relayClient.connect(relayUrlState.value) },
          enabled = status != "connected",
      ) {
        Text(stringResource(R.string.voice_connect))
      }
      Button(
          onClick = {
            micStreamer.stop()
            relayClient.close()
            audioPlayer.stop()
          },
          enabled = status == "connected",
      ) {
        Text(stringResource(R.string.voice_disconnect))
      }
    }

    Text(text = "Status: $status")

    Spacer(modifier = Modifier.height(8.dp))

    Text(text = "You: $lastUserText")
    Text(text = "MC: $lastAssistantText")

    Spacer(modifier = Modifier.height(8.dp))

    // Push-to-talk
    Text(text = stringResource(R.string.voice_hold_to_talk))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
      Button(
          onClick = { /* handled by pointer events */ },
          modifier =
              Modifier
                  .pointerInteropFilter { motionEvent ->
                    when (motionEvent.actionMasked) {
                      android.view.MotionEvent.ACTION_DOWN -> {
                        if (status == "connected") {
                          audioPlayer.stop()
                          relayClient.sendPause()
                          micStreamer.start()
                        }
                        true
                      }
                      android.view.MotionEvent.ACTION_UP,
                      android.view.MotionEvent.ACTION_CANCEL -> {
                        if (status == "connected") {
                          micStreamer.stop()
                          relayClient.sendResume()
                        }
                        true
                      }
                      else -> false
                    }
                  },
      ) {
        Text("PTT")
      }
    }
  }
}
