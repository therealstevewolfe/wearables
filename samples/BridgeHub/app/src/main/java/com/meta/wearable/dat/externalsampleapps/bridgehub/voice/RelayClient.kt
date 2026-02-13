package com.meta.wearable.dat.externalsampleapps.bridgehub.voice

import android.util.Base64
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class RelayClient(
    private val onStatus: (String) -> Unit,
    private val onUserText: (String) -> Unit,
    private val onAssistantText: (String) -> Unit,
    private val onAudioOutputWavChunk: (ByteArray) -> Unit,
) {
  private val client =
      OkHttpClient.Builder()
          .pingInterval(15, TimeUnit.SECONDS)
          .build()

  private var ws: WebSocket? = null

  fun connect(url: String) {
    close()

    val request = Request.Builder().url(url).build()
    onStatus("connecting")

    ws =
        client.newWebSocket(
            request,
            object : WebSocketListener() {
              override fun onOpen(webSocket: WebSocket, response: Response) {
                onStatus("connected")
              }

              override fun onMessage(webSocket: WebSocket, text: String) {
                handleMessage(text)
              }

              override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                onStatus("disconnecting")
                webSocket.close(code, reason)
              }

              override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                onStatus("disconnected")
              }

              override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.w("RelayClient", "ws failure: ${t.message}")
                onStatus("error")
              }
            },
        )
  }

  fun close() {
    ws?.close(1000, "bye")
    ws = null
    onStatus("disconnected")
  }

  fun sendPause() {
    ws?.send(JSONObject(mapOf("type" to "pause_assistant_message")).toString())
  }

  fun sendResume() {
    ws?.send(JSONObject(mapOf("type" to "resume_assistant_message")).toString())
  }

  fun sendAudioInput(pcm16Mono16k: ByteArray) {
    val b64 = Base64.encodeToString(pcm16Mono16k, Base64.NO_WRAP)
    val obj = JSONObject()
    obj.put("type", "audio_input")
    obj.put("data", b64)
    ws?.send(obj.toString())
  }

  private fun handleMessage(text: String) {
    val obj = try {
      JSONObject(text)
    } catch (e: Throwable) {
      return
    }
    val type = obj.optString("type")

    when (type) {
      "user_message" -> {
        val message = obj.optJSONObject("message")
        val content = message?.optString("content") ?: ""
        val interim = obj.optBoolean("interim", false)
        if (!interim) onUserText(content)
      }

      "assistant_message" -> {
        val message = obj.optJSONObject("message")
        val content = message?.optString("content") ?: ""
        onAssistantText(content)
      }

      "audio_output" -> {
        val b64 = obj.optString("data")
        if (b64.isNullOrEmpty()) return
        val bytes = Base64.decode(b64, Base64.DEFAULT)
        onAudioOutputWavChunk(bytes)
      }

      "relay_error" -> {
        onStatus("error")
      }
    }
  }
}
