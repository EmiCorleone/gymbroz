package com.meta.wearable.dat.externalsampleapps.cameraaccess.stream

import android.graphics.Bitmap
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import android.view.Surface
import java.io.File
import java.io.IOException

class BackgroundVideoEncoder(private val outputFile: File, private val width: Int, private val height: Int) {
    private val TAG = "BackgroundVideoEncoder"
    
    private var mediaCodec: MediaCodec? = null
    private var mediaMuxer: MediaMuxer? = null
    private var inputSurface: Surface? = null
    
    private var trackIndex = -1
    private var isMuxerStarted = false
    private val bufferInfo = MediaCodec.BufferInfo()
    private var frameCount = 0L

    init {
        try {
            val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height)
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            format.setInteger(MediaFormat.KEY_BIT_RATE, 2000000) // 2 Mbps
            format.setInteger(MediaFormat.KEY_FRAME_RATE, 24)
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)

            mediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            mediaCodec?.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            inputSurface = mediaCodec?.createInputSurface()
            mediaCodec?.start()

            mediaMuxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            Log.d(TAG, "Initialized Video Encoder at ${outputFile.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize BackgroundVideoEncoder", e)
            release()
        }
    }

    fun encodeFrame(bitmap: Bitmap) {
        if (mediaCodec == null || inputSurface == null || mediaMuxer == null) return

        try {
            // Draw the bitmap onto the input surface
            val canvas = inputSurface?.lockHardwareCanvas()
            if (canvas != null) {
                // Scale bitmap to fit the encoder dimensions if necessary
                if (bitmap.width != width || bitmap.height != height) {
                    val scaled = Bitmap.createScaledBitmap(bitmap, width, height, false)
                    canvas.drawBitmap(scaled, 0f, 0f, null)
                } else {
                    canvas.drawBitmap(bitmap, 0f, 0f, null)
                }
                inputSurface?.unlockCanvasAndPost(canvas)
            }

            drainEncoder(false)
            
            frameCount++
        } catch (e: Exception) {
            Log.e(TAG, "Error encoding frame", e)
        }
    }

    private fun drainEncoder(endOfStream: Boolean) {
        if (endOfStream) {
            try {
                mediaCodec?.signalEndOfInputStream()
            } catch (e: Exception) {
                Log.e(TAG, "Error signaling end of stream", e)
            }
        }

        var encoderOutputBuffers = mediaCodec?.outputBuffers
        while (true) {
            val encoderStatus = mediaCodec?.dequeueOutputBuffer(bufferInfo, 10000) ?: break
            
            if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                if (!endOfStream) break // Output is empty, but we aren't done yet
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                // Ignore, as it's deprecated
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                if (isMuxerStarted) {
                    Log.e(TAG, "Format changed twice")
                }
                val newFormat = mediaCodec?.outputFormat
                if (newFormat != null) {
                    trackIndex = mediaMuxer?.addTrack(newFormat) ?: -1
                    mediaMuxer?.start()
                    isMuxerStarted = true
                }
            } else if (encoderStatus >= 0) {
                val encodedData = mediaCodec?.getOutputBuffer(encoderStatus)
                if (encodedData != null) {
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                        bufferInfo.size = 0
                    }

                    if (bufferInfo.size != 0) {
                        if (!isMuxerStarted) {
                            Log.e(TAG, "Muxer hasn't started")
                        }
                        
                        // Fake a presentation time
                        // Assuming 24fps -> ~41ms per frame
                        bufferInfo.presentationTimeUs = frameCount * 41666L
                        
                        encodedData.position(bufferInfo.offset)
                        encodedData.limit(bufferInfo.offset + bufferInfo.size)
                        
                        try {
                            mediaMuxer?.writeSampleData(trackIndex, encodedData, bufferInfo)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error writing to muxer", e)
                        }
                    }

                    mediaCodec?.releaseOutputBuffer(encoderStatus, false)

                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        break
                    }
                }
            }
        }
    }

    fun finish(): File? {
        Log.d(TAG, "Finishing video encoder. Total frames: $frameCount")
        try {
            drainEncoder(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error draining during finish", e)
        }
        release()
        
        return if (frameCount > 0 && outputFile.exists() && outputFile.length() > 0) {
            outputFile
        } else {
            null
        }
    }

    private fun release() {
        try {
            mediaCodec?.stop()
            mediaCodec?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing codec", e)
        }
        mediaCodec = null

        inputSurface?.release()
        inputSurface = null

        try {
            if (isMuxerStarted) {
                mediaMuxer?.stop()
            }
            mediaMuxer?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing muxer", e)
        }
        mediaMuxer = null
        isMuxerStarted = false
    }
}
