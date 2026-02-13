package com.meta.wearable.dat.externalsampleapps.bridgehub.voice

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import java.nio.ByteBuffer
import java.nio.ByteOrder

class AudioPlayer {
  private var track: AudioTrack? = null
  private var sampleRate: Int = 48000

  fun stop() {
    track?.pause()
    track?.flush()
  }

  fun release() {
    track?.release()
    track = null
  }

  fun playWavChunk(wavBytes: ByteArray) {
    if (wavBytes.size < 44) return
    if (!(wavBytes[0].toInt().toChar() == 'R' && wavBytes[1].toInt().toChar() == 'I')) return

    val sr = readIntLE(wavBytes, 24)
    val channels = readShortLE(wavBytes, 22)
    val bits = readShortLE(wavBytes, 34)

    if (channels != 1 || bits != 16) {
      // MVP supports mono PCM16 only.
      return
    }

    ensureTrack(sr)

    val pcm = wavBytes.copyOfRange(44, wavBytes.size)
    track?.play()
    track?.write(pcm, 0, pcm.size)
  }

  private fun ensureTrack(sr: Int) {
    if (track != null && sr == sampleRate) return

    track?.release()
    track = null

    sampleRate = sr

    val attrs =
        AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()

    val format =
        AudioFormat.Builder()
            .setSampleRate(sampleRate)
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build()

    val minBuf = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
    val bufSize = maxOf(minBuf, sampleRate / 2) // ~500ms

    track =
        AudioTrack(
            attrs,
            format,
            bufSize,
            AudioTrack.MODE_STREAM,
            AudioManager.AUDIO_SESSION_ID_GENERATE,
        )
  }

  private fun readIntLE(b: ByteArray, offset: Int): Int {
    return ByteBuffer.wrap(b, offset, 4).order(ByteOrder.LITTLE_ENDIAN).int
  }

  private fun readShortLE(b: ByteArray, offset: Int): Int {
    return ByteBuffer.wrap(b, offset, 2).order(ByteOrder.LITTLE_ENDIAN).short.toInt() and 0xFFFF
  }
}
