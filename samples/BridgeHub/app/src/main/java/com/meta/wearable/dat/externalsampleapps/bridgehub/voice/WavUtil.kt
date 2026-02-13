package com.meta.wearable.dat.externalsampleapps.bridgehub.voice

/** Minimal WAV wrapper for PCM16 audio. */
object WavUtil {
  /**
   * WAV header for streaming PCM16 where total data size is unknown.
   * Uses large placeholder sizes so decoders don't treat it as zero-length.
   */
  fun streamingPcm16WavHeader(
      sampleRate: Int,
      channels: Int = 1,
  ): ByteArray {
    val bitsPerSample = 16
    val byteRate = sampleRate * channels * (bitsPerSample / 8)
    val blockAlign = channels * (bitsPerSample / 8)

    // Placeholder sizes (unknown upfront). Use max positive int.
    val dataSize = 0x7fffffff
    val riffChunkSize = 36 + dataSize

    val header = ByteArray(44)

    fun putAscii(off: Int, s: String) {
      val b = s.toByteArray(Charsets.US_ASCII)
      for (i in b.indices) header[off + i] = b[i]
    }

    fun putLE16(off: Int, v: Int) {
      header[off] = (v and 0xff).toByte()
      header[off + 1] = ((v ushr 8) and 0xff).toByte()
    }

    fun putLE32(off: Int, v: Int) {
      header[off] = (v and 0xff).toByte()
      header[off + 1] = ((v ushr 8) and 0xff).toByte()
      header[off + 2] = ((v ushr 16) and 0xff).toByte()
      header[off + 3] = ((v ushr 24) and 0xff).toByte()
    }

    putAscii(0, "RIFF")
    putLE32(4, riffChunkSize)
    putAscii(8, "WAVE")

    putAscii(12, "fmt ")
    putLE32(16, 16) // PCM fmt chunk size
    putLE16(20, 1) // audio format 1=PCM
    putLE16(22, channels)
    putLE32(24, sampleRate)
    putLE32(28, byteRate)
    putLE16(32, blockAlign)
    putLE16(34, bitsPerSample)

    putAscii(36, "data")
    putLE32(40, dataSize)

    return header
  }
}
