package com.meta.wearable.dat.externalsampleapps.bridgehub.bridge

import android.graphics.Bitmap
import android.util.Log

interface FrameBridgeSink {
  fun onFrame(bitmap: Bitmap, timestampMs: Long)
}

class LoggingFrameBridgeSink : FrameBridgeSink {
  override fun onFrame(bitmap: Bitmap, timestampMs: Long) {
    Log.d(TAG, "Frame received ${bitmap.width}x${bitmap.height} at $timestampMs")
  }

  companion object {
    private const val TAG = "FrameBridgeSink"
  }
}

class FrameBridge(
    private val sink: FrameBridgeSink,
    private val minPublishIntervalMs: Long = 1000L,
) {
  private var lastPublishMs: Long = 0L

  fun publishIfDue(bitmap: Bitmap, timestampMs: Long) {
    if (timestampMs - lastPublishMs < minPublishIntervalMs) {
      return
    }
    sink.onFrame(bitmap, timestampMs)
    lastPublishMs = timestampMs
  }
}
