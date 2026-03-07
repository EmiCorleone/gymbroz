package com.meta.wearable.dat.externalsampleapps.cameraaccess.openclaw

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Base64
import android.util.Log
import com.meta.wearable.dat.externalsampleapps.cameraaccess.gemini.GeminiConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class MusicStreamingService(private val scope: CoroutineScope) {
    companion object {
        private const val TAG = "MusicStream"
        private const val SAMPLE_RATE = 48000
        private const val NORMAL_VOLUME = 0.25f
        private const val DUCKED_VOLUME = 0.08f
        private const val BASE_URL = "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1alpha.GenerativeService.BidiGenerateMusic"
    }

    private var webSocket: WebSocket? = null
    private var audioTrack: AudioTrack? = null
    private val isPlaying = AtomicBoolean(false)
    private val audioQueue = ConcurrentLinkedQueue<ByteArray>()
    private var playbackThread: Thread? = null

    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    fun play(prompt: String, bpm: Int = 120) {
        stop()

        val apiKey = GeminiConfig.apiKey
        if (apiKey.isEmpty()) {
            Log.e(TAG, "No API key")
            return
        }

        // Set up AudioTrack for stereo 48kHz PCM16
        val bufferSize = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(SAMPLE_RATE)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize * 4)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        audioTrack?.play()
        audioTrack?.setVolume(NORMAL_VOLUME)
        isPlaying.set(true)

        // Start playback thread
        playbackThread = Thread {
            while (isPlaying.get()) {
                val data = audioQueue.poll()
                if (data != null) {
                    audioTrack?.write(data, 0, data.size)
                } else {
                    Thread.sleep(10)
                }
            }
        }.apply { start() }

        // Connect WebSocket to Lyria
        val url = "$BASE_URL?key=$apiKey"
        val request = Request.Builder().url(url).build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                Log.d(TAG, "Music WebSocket opened, response: ${response.code}")
                // Send setup (just model, no generationConfig for music)
                val setup = JSONObject().apply {
                    put("setup", JSONObject().apply {
                        put("model", "models/lyria-realtime-exp")
                    })
                }
                Log.d(TAG, "Sending setup: ${setup.toString()}")
                ws.send(setup.toString())
            }

            override fun onClosing(ws: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "Music WebSocket closing: code=$code reason=$reason")
            }

            override fun onMessage(ws: WebSocket, text: String) {
                try {
                    Log.d(TAG, "Music message: ${text.take(300)}")
                    val json = JSONObject(text)

                    if (json.has("setupComplete")) {
                        Log.d(TAG, "Music setup complete, sending prompt")
                        // Send weighted prompts (wrapped in clientContent)
                        val promptMsg = JSONObject().apply {
                            put("clientContent", JSONObject().apply {
                                put("weightedPrompts", JSONArray().put(JSONObject().apply {
                                    put("text", prompt)
                                    put("weight", 1.0)
                                }))
                            })
                        }
                        ws.send(promptMsg.toString())

                        // Send music config
                        val configMsg = JSONObject().apply {
                            put("musicGenerationConfig", JSONObject().apply {
                                put("bpm", bpm)
                                put("temperature", 1.1)
                            })
                        }
                        ws.send(configMsg.toString())

                        // Start playing
                        ws.send(JSONObject().put("playbackControl", "PLAY").toString())
                        return
                    }

                    // Handle audio chunks - try multiple possible response formats
                    val serverContent = json.optJSONObject("serverContent")
                    val audioChunks = serverContent?.optJSONArray("audioChunks")
                    if (audioChunks != null) {
                        for (i in 0 until audioChunks.length()) {
                            val chunk = audioChunks.getJSONObject(i)
                            val data = chunk.optString("data", "")
                            if (data.isNotEmpty()) {
                                val pcmData = Base64.decode(data, Base64.DEFAULT)
                                audioQueue.add(pcmData)
                            }
                        }
                        if (audioQueue.size == 1) Log.d(TAG, "First audio chunk received")
                    }

                    // Also check for modelTurn inline audio (standard Gemini format)
                    val modelTurn = serverContent?.optJSONObject("modelTurn")
                    val parts = modelTurn?.optJSONArray("parts")
                    if (parts != null) {
                        for (i in 0 until parts.length()) {
                            val part = parts.getJSONObject(i)
                            val inlineData = part.optJSONObject("inlineData")
                            if (inlineData != null) {
                                val data = inlineData.optString("data", "")
                                if (data.isNotEmpty()) {
                                    val pcmData = Base64.decode(data, Base64.DEFAULT)
                                    audioQueue.add(pcmData)
                                    if (audioQueue.size == 1) Log.d(TAG, "First audio chunk (inline) received")
                                }
                            }
                        }
                    }

                    // Log unhandled messages for debugging
                    if (audioChunks == null && parts == null && !json.has("setupComplete")) {
                        Log.d(TAG, "Unhandled message: ${text.take(200)}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Music message error: ${e.message}")
                }
            }

            override fun onMessage(ws: WebSocket, bytes: ByteString) {
                // Lyria sends all messages as binary frames - parse as text
                val text = bytes.utf8()
                Log.d(TAG, "Music binary message (${bytes.size} bytes): ${text.take(300)}")
                onMessage(ws, text)
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "Music WebSocket error: ${t.message}, response: ${response?.code} ${response?.message}")
                try {
                    response?.body?.string()?.let { body ->
                        Log.e(TAG, "Music WebSocket error body: ${body.take(500)}")
                    }
                } catch (_: Exception) {}
                isPlaying.set(false)
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "Music WebSocket closed: code=$code reason=$reason")
                isPlaying.set(false)
            }
        })
    }

    fun updatePrompt(prompt: String) {
        val msg = JSONObject().apply {
            put("clientContent", JSONObject().apply {
                put("weightedPrompts", JSONArray().put(JSONObject().apply {
                    put("text", prompt)
                    put("weight", 1.0)
                }))
            })
        }
        webSocket?.send(msg.toString())
    }

    fun updateBpm(bpm: Int) {
        val msg = JSONObject().apply {
            put("musicGenerationConfig", JSONObject().apply {
                put("bpm", bpm)
            })
        }
        webSocket?.send(msg.toString())
    }

    fun duck() {
        audioTrack?.setVolume(DUCKED_VOLUME)
    }

    fun unduck() {
        audioTrack?.setVolume(NORMAL_VOLUME)
    }

    fun stop() {
        isPlaying.set(false)
        webSocket?.close(1000, null)
        webSocket = null
        playbackThread?.join(1000)
        playbackThread = null
        audioTrack?.stop()
        audioTrack?.release()
        audioTrack = null
        audioQueue.clear()
    }
}
