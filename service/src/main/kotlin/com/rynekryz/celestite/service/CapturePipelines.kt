package com.rynekryz.celestite.service

import android.content.ContentValues
import android.content.Context
import android.hardware.display.DisplayManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.view.Surface
import com.rynekryz.celestite.core.model.AudioMode
import com.rynekryz.celestite.core.model.FpsOption
import com.rynekryz.celestite.core.model.QualityOption
import com.rynekryz.celestite.core.model.ResolutionOption
import com.rynekryz.celestite.core.model.VideoFormat
import java.nio.ByteBuffer
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import android.graphics.Rect
import android.view.WindowManager

internal object CaptureMath {
    fun targetSize(bounds: Rect, option: ResolutionOption): Pair<Int, Int> {
        var w = bounds.width()
        var h = bounds.height()
        val shortSide = minOf(w, h)
        val target = when (option) {
            ResolutionOption.NATIVE -> 0
            ResolutionOption.FHD -> 1080
            ResolutionOption.HD -> 720
        }
        if (target in 1 until shortSide) {
            val scale = target.toFloat() / shortSide
            w = (w * scale).toInt()
            h = (h * scale).toInt()
        }
        return (w - (w % 2)) to (h - (h % 2))
    }

    fun fps(refreshRate: Float, option: FpsOption): Int = when (option) {
        FpsOption.AUTO -> refreshRate.toInt().coerceIn(24, 60)
        FpsOption.S60 -> 60
        FpsOption.S30 -> 30
    }

    fun bitrate(width: Int, height: Int, fps: Int, quality: QualityOption, format: VideoFormat): Long {
        val bitsPerPixel = when (quality) {
            QualityOption.HIGH -> 0.18
            QualityOption.MEDIUM, QualityOption.AUTO -> 0.10
            QualityOption.LOW -> 0.06
        } * if (format == VideoFormat.H265) 0.6 else 1.0
        val raw = width.toDouble() * height * fps * bitsPerPixel
        return raw.toLong().coerceAtLeast(1_000_000L)
    }
}

internal interface CapturePipeline {
    val supportsPause: Boolean
    fun start()
    fun pause()
    fun resume()
    fun stop(): String
    fun abort()
}

internal fun newOutputDescriptor(context: Context): Pair<Uri, ParcelFileDescriptor> {
    val resolver = context.contentResolver
    val name = "Celestite_${System.currentTimeMillis()}.mp4"
    val values = ContentValues().apply {
        put(MediaStore.Video.Media.DISPLAY_NAME, name)
        put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
        put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/Celestite")
        put(MediaStore.Video.Media.IS_PENDING, 1)
    }
    val uri = resolver.insert(
        MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
        values,
    ) ?: error("Could not create output file")
    val fd = resolver.openFileDescriptor(uri, "rw") ?: run {
        resolver.delete(uri, null, null)
        error("Could not open output file")
    }
    return uri to fd
}

internal fun finishOutput(context: Context, uri: Uri): String {
    val resolver = context.contentResolver
    val cursor = resolver.query(uri, arrayOf(MediaStore.Video.Media.DISPLAY_NAME), null, null, null)
    cursor?.use {
        if (it.moveToFirst()) {
            val name = it.getString(0)
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.IS_PENDING, 0)
            }
            resolver.update(uri, values, null, null)
            return name
        }
    }
    return "Celestite recording"
}

internal fun discardOutput(context: Context, uri: Uri) {
    runCatching { context.contentResolver.delete(uri, null, null) }
}

internal fun displayBounds(context: Context): Rect =
    (context.getSystemService(WindowManager::class.java)).maximumWindowMetrics.bounds

internal fun displayRefreshRate(context: Context): Float =
    (context.getSystemService(DisplayManager::class.java)).getDisplay(android.view.Display.DEFAULT_DISPLAY).refreshRate

internal fun displayDensity(context: Context): Int = context.resources.displayMetrics.densityDpi

internal class MediaRecorderPipeline(
    private val context: Context,
    private val projection: MediaProjection,
    private val width: Int,
    private val height: Int,
    private val fps: Int,
    private val bitrate: Long,
    private val format: VideoFormat,
    private val audioMode: AudioMode,
) : CapturePipeline {
    private var recorder: MediaRecorder? = null
    private var display: android.hardware.display.VirtualDisplay? = null
    private var output: Uri? = null
    private var descriptor: ParcelFileDescriptor? = null

    override val supportsPause: Boolean get() = true

    override fun start() {
        val (uri, fd) = newOutputDescriptor(context)
        output = uri
        descriptor = fd
        val r = MediaRecorder(context)
        try {
            r.setVideoSource(MediaRecorder.VideoSource.SURFACE)
            if (audioMode == AudioMode.MIC) {
                r.setAudioSource(MediaRecorder.AudioSource.MIC)
            }
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            if (audioMode == AudioMode.MIC) {
                r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                r.setAudioSamplingRate(44100)
                r.setAudioChannels(2)
                r.setAudioEncodingBitRate(128000)
            }
            r.setVideoEncoder(if (format == VideoFormat.H265) MediaRecorder.VideoEncoder.HEVC else MediaRecorder.VideoEncoder.H264)
            r.setVideoSize(width, height)
            r.setVideoFrameRate(fps)
            r.setVideoEncodingBitRate(bitrate.toInt())
            r.setOutputFile(fd.fileDescriptor)
            r.prepare()
            display = projection.createVirtualDisplay(
                "celestite",
                width,
                height,
                displayDensity(context),
                android.hardware.display.DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                r.surface,
                null,
                null,
            )
            r.start()
            recorder = r
        } catch (t: Throwable) {
            r.release()
            cleanupOutput()
            throw t
        }
    }

    override fun pause() {
        recorder?.pause()
    }

    override fun resume() {
        recorder?.resume()
    }

    override fun stop(): String {
        try {
            recorder?.stop()
        } finally {
            recorder?.release()
            recorder = null
            display?.release()
            display = null
            descriptor?.close()
            descriptor = null
        }
        val uri = output ?: return "Celestite recording"
        return finishOutput(context, uri)
    }

    override fun abort() {
        runCatching { recorder?.stop() }
        recorder?.release()
        recorder = null
        display?.release()
        display = null
        descriptor?.close()
        descriptor = null
        cleanupOutput()
    }

    private fun cleanupOutput() {
        output?.let { discardOutput(context, it) }
        output = null
    }
}

internal class CodecPipeline(
    private val context: Context,
    private val projection: MediaProjection,
    private val width: Int,
    private val height: Int,
    private val fps: Int,
    private val bitrate: Long,
    private val format: VideoFormat,
) : CapturePipeline {
    private var videoCodec: MediaCodec? = null
    private var audioCodec: MediaCodec? = null
    private var display: android.hardware.display.VirtualDisplay? = null
    private var inputSurface: Surface? = null
    private var audioRecord: AudioRecord? = null
    private var muxer: MediaMuxer? = null
    private var output: Uri? = null
    private var descriptor: ParcelFileDescriptor? = null
    private var videoTrack = -1
    private var audioTrack = -1
    private val muxerLock = Any()
    private val tracksReady = CountDownLatch(2)
    private var running = false
    private var videoThread: Thread? = null
    private var audioThread: Thread? = null

    override val supportsPause: Boolean get() = false

    override fun start() {
        val (uri, fd) = newOutputDescriptor(context)
        output = uri
        descriptor = fd
        muxer = MediaMuxer(fd.fileDescriptor, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

        val videoMime = if (format == VideoFormat.H265) MediaFormat.MIMETYPE_VIDEO_HEVC else MediaFormat.MIMETYPE_VIDEO_AVC
        val videoFormat = MediaFormat.createVideoFormat(videoMime, width, height).apply {
            setInteger(
                MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface,
            )
            setInteger(MediaFormat.KEY_BIT_RATE, bitrate.toInt())
            setInteger(MediaFormat.KEY_FRAME_RATE, fps)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2)
        }
        val video = MediaCodec.createEncoderByType(videoMime)
        video.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        val surface = video.createInputSurface()
        video.start()

        val pcmFormat = AudioFormat.Builder()
            .setSampleRate(48000)
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setChannelMask(AudioFormat.CHANNEL_IN_STEREO)
            .build()
        val captureConfig = AudioPlaybackCaptureConfiguration.Builder(projection)
            .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
            .addMatchingUsage(AudioAttributes.USAGE_GAME)
            .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
            .build()
        val minBuffer = AudioRecord.getMinBufferSize(48000, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT)
        val record = AudioRecord.Builder()
            .setAudioFormat(pcmFormat)
            .setBufferSizeInBytes(minBuffer * 4)
            .setAudioPlaybackCaptureConfig(captureConfig)
            .build()

        val audio = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        val aacFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, 48000, 2).apply {
            setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            setInteger(MediaFormat.KEY_BIT_RATE, 192000)
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 65536)
        }
        audio.configure(aacFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        audio.start()
        record.startRecording()

        display = projection.createVirtualDisplay(
            "celestite",
            width,
            height,
            displayDensity(context),
            android.hardware.display.DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            surface,
            null,
            null,
        )

        videoCodec = video
        audioCodec = audio
        inputSurface = surface
        audioRecord = record
        running = true
        videoThread = Thread({ drainVideo(video) }, "celestite-video").apply { start() }
        audioThread = Thread({ drainAudio(audio, record) }, "celestite-audio").apply { start() }
    }

    private fun addTrack(format: MediaFormat, audio: Boolean) {
        synchronized(muxerLock) {
            val m = muxer ?: return
            val index = m.addTrack(format)
            if (audio) audioTrack = index else videoTrack = index
            if (videoTrack >= 0 && audioTrack >= 0) m.start()
        }
        tracksReady.countDown()
    }

    private fun drainVideo(codec: MediaCodec) {
        val info = MediaCodec.BufferInfo()
        while (running) {
            try {
                val index = codec.dequeueOutputBuffer(info, 10_000)
                when {
                    index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> addTrack(codec.outputFormat, false)
                    index >= 0 -> writeSample(codec, index, info, false)
                    index == MediaCodec.INFO_TRY_AGAIN_LATER -> continue
                    else -> break
                }
                if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) break
            } catch (t: Throwable) {
                if (running) fail(t)
                return
            }
        }
    }

    private fun drainAudio(codec: MediaCodec, record: AudioRecord) {
        val pcm = ShortArray(4096)
        var presentationUs = 0L
        while (running) {
            val read = record.read(pcm, 0, pcm.size)
            if (read > 0) {
                val inputIndex = codec.dequeueInputBuffer(10_000)
                if (inputIndex >= 0) {
                    val buffer = codec.getInputBuffer(inputIndex)!!
                    buffer.clear()
                    buffer.put(byteBufferOf(pcm, read))
                    val pts = presentationUs
                    presentationUs += (read / 2).toLong() * 1_000_000L / 48000L
                    codec.queueInputBuffer(inputIndex, 0, read * 2, pts, 0)
                }
            }
            val info = MediaCodec.BufferInfo()
            while (running) {
                val index = codec.dequeueOutputBuffer(info, 0)
                when {
                    index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> addTrack(codec.outputFormat, true)
                    index >= 0 -> writeSample(codec, index, info, true)
                    else -> break
                }
                if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) break
            }
        }
    }

    private fun byteBufferOf(pcm: ShortArray, count: Int): ByteBuffer {
        val bytes = ByteArray(count * 2)
        var i = 0
        while (i < count) {
            val s = pcm[i]
            bytes[i * 2] = (s.toInt() and 0xff).toByte()
            bytes[i * 2 + 1] = (s.toInt() shr 8).toByte()
            i++
        }
        return ByteBuffer.wrap(bytes)
    }

    private fun writeSample(codec: MediaCodec, index: Int, info: MediaCodec.BufferInfo, audio: Boolean) {
        if (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
            codec.releaseOutputBuffer(index, false)
            return
        }
        while (running && !tracksReady.await(500, TimeUnit.MILLISECONDS)) {}
        val buffer = codec.getOutputBuffer(index)!!
        if (running && info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM == 0) {
            synchronized(muxerLock) {
                muxer?.writeSampleData(if (audio) audioTrack else videoTrack, buffer, info)
            }
        }
        codec.releaseOutputBuffer(index, false)
    }

    override fun pause() = Unit

    override fun resume() = Unit

    override fun stop(): String {
        running = false
        try {
            videoCodec?.signalEndOfInputStream()
        } catch (_: Throwable) {}
        try {
            audioRecord?.stop()
        } catch (_: Throwable) {}
        videoThread?.join(3000)
        audioThread?.join(3000)
        videoCodec?.stop()
        videoCodec?.release()
        audioCodec?.stop()
        audioCodec?.release()
        videoCodec = null
        audioCodec = null
        audioRecord?.release()
        audioRecord = null
        display?.release()
        display = null
        inputSurface?.release()
        inputSurface = null
        try {
            muxer?.stop()
        } catch (_: Throwable) {}
        muxer?.release()
        muxer = null
        descriptor?.close()
        descriptor = null
        val uri = output ?: return "Celestite recording"
        return finishOutput(context, uri)
    }

    override fun abort() {
        running = false
        runCatching { videoCodec?.signalEndOfInputStream() }
        videoThread?.join(1000)
        audioThread?.join(1000)
        runCatching { videoCodec?.stop() }
        videoCodec?.release()
        runCatching { audioCodec?.stop() }
        audioCodec?.release()
        videoCodec = null
        audioCodec = null
        runCatching { audioRecord?.stop() }
        audioRecord?.release()
        audioRecord = null
        display?.release()
        display = null
        inputSurface?.release()
        inputSurface = null
        runCatching { muxer?.stop() }
        muxer?.release()
        muxer = null
        descriptor?.close()
        descriptor = null
        output?.let { discardOutput(context, it) }
        output = null
    }

    var onFailed: ((String) -> Unit)? = null

    private fun fail(t: Throwable) {
        onFailed?.invoke(t.message ?: "Encoder failure")
    }
}
