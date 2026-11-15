package com.rynekryz.celestite.feature.gallery

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.util.Size
import java.nio.ByteBuffer

data class VideoItem(
    val uri: Uri,
    val name: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val addedAt: Long,
    val width: Int,
    val height: Int,
)

object MediaLibrary {

    fun query(context: Context): List<VideoItem> {
        val resolver = context.contentResolver
        val collection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_MODIFIED,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
        )
        val items = mutableListOf<VideoItem>()
        try {
            resolver.query(
                collection,
                projection,
                "${MediaStore.Video.Media.RELATIVE_PATH} LIKE ?",
                arrayOf("%Movies/Celestite%"),
                "${MediaStore.Video.Media.DATE_MODIFIED} DESC",
            )?.use { cursor ->
                val id = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val name = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val duration = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val size = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val modified = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)
                val width = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
                val height = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
                while (cursor.moveToNext()) {
                    val uri = ContentUris.withAppendedId(collection, cursor.getLong(id))
                    items += VideoItem(
                        uri = uri,
                        name = cursor.getString(name),
                        durationMs = cursor.getLong(duration),
                        sizeBytes = cursor.getLong(size),
                        addedAt = cursor.getLong(modified) * 1000,
                        width = cursor.getInt(width),
                        height = cursor.getInt(height),
                    )
                }
            }
        } catch (_: SecurityException) {
        }
        return items
    }

    fun thumbnail(context: Context, uri: Uri, size: Int): Bitmap? =
        try {
            context.contentResolver.loadThumbnail(uri, Size(size, size), null)
        } catch (_: Exception) {
            null
        }

    fun delete(context: Context, uri: Uri) {
        try {
            context.contentResolver.delete(uri, null, null)
        } catch (_: SecurityException) {
        }
    }

    fun share(context: Context, uri: Uri): Intent {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "video/mp4"
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = android.content.ClipData.newRawUri(uri.toString(), uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(intent, "Share recording")
    }

    fun formatSize(bytes: Long): String = when {
        bytes >= 1 shl 30 -> "%.1f GB".format(bytes.toDouble() / (1 shl 30))
        bytes >= 1 shl 20 -> "%.1f MB".format(bytes.toDouble() / (1 shl 20))
        else -> "%.0f KB".format(bytes.toDouble() / (1 shl 10))
    }
}

internal fun newPendingOutput(context: Context): Pair<Uri, ParcelFileDescriptor> {
    val resolver = context.contentResolver
    val name = "Celestite_trim_${System.currentTimeMillis()}.mp4"
    val values = android.content.ContentValues().apply {
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

internal fun finishPendingOutput(context: Context, uri: Uri) {
    val values = android.content.ContentValues().apply {
        put(MediaStore.Video.Media.IS_PENDING, 0)
    }
    context.contentResolver.update(uri, values, null, null)
}

internal fun discardOutput(context: Context, uri: Uri) {
    runCatching { context.contentResolver.delete(uri, null, null) }
}

internal fun trim(context: Context, source: Uri, startUs: Long, endUs: Long): Uri? {
    val resolver = context.contentResolver
    val inFd = resolver.openFileDescriptor(source, "r") ?: return null
    val (outUri, outFd) = try {
        newPendingOutput(context)
    } catch (_: Exception) {
        runCatching { inFd.close() }
        return null
    }
    val extractor = MediaExtractor()
    try {
        extractor.setDataSource(inFd.fileDescriptor)
        val muxer = MediaMuxer(outFd.fileDescriptor, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        try {
            val trackMap = mutableMapOf<Int, Int>()
            var maxSampleSize = 1 shl 20
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                trackMap[i] = muxer.addTrack(format)
                if (format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                    maxSampleSize = maxOf(maxSampleSize, format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE))
                }
            }
            muxer.start()
            extractor.seekTo(startUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
            val buffer = ByteBuffer.allocateDirect(maxSampleSize)
            val info = MediaCodec.BufferInfo()
            while (true) {
                val trackIndex = extractor.sampleTrackIndex
                if (trackIndex < 0) break
                val sampleTime = extractor.sampleTime
                if (sampleTime > endUs) break
                extractor.readSampleData(buffer, 0)
                if (sampleTime >= startUs) {
                    info.size = extractor.sampleSize.toInt()
                    info.presentationTimeUs = sampleTime
                    info.flags = extractor.sampleFlags
                    muxer.writeSampleData(trackMap.getValue(trackIndex), buffer, info)
                }
                extractor.advance()
            }
            muxer.stop()
        } finally {
            muxer.release()
        }
        finishPendingOutput(context, outUri)
        return outUri
    } catch (_: Throwable) {
        discardOutput(context, outUri)
        return null
    } finally {
        extractor.release()
        runCatching { inFd.close() }
        runCatching { outFd.close() }
    }
}
