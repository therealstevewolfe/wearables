package com.meta.wearable.dat.externalsampleapps.bridgehub.voice

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MicStreamer(
    private val onPcmChunk: (ByteArray) -> Unit,
) {
  private var job: Job? = null
  private var recorder: AudioRecord? = null

  // Hume recommends 20ms chunks.
  private val sampleRate = 16000
  private val bytesPer20ms = 640 // 16kHz * 0.02s * 2 bytes

  fun start() {
    if (job != null) return

    val minBuf =
        AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )

    val bufSize = maxOf(minBuf, bytesPer20ms * 10)

    val r =
        AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufSize,
        )

    if (r.state != AudioRecord.STATE_INITIALIZED) {
      Log.w("MicStreamer", "AudioRecord not initialized")
      r.release()
      return
    }

    recorder = r
    r.startRecording()

    val scope = CoroutineScope(Dispatchers.IO)
    job =
        scope.launch {
          val frame = ByteArray(bytesPer20ms)
          while (isActive) {
            val n = r.read(frame, 0, frame.size)
            if (n <= 0) continue
            if (n == frame.size) {
              onPcmChunk(frame)
            } else {
              // Copy exact bytes read
              onPcmChunk(frame.copyOfRange(0, n))
            }
          }
        }
  }

  fun stop() {
    job?.cancel()
    job = null

    recorder?.let {
      try {
        it.stop()
      } catch (_: Throwable) {}
      it.release()
    }
    recorder = null
  }
}
