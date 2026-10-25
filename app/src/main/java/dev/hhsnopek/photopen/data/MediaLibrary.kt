package dev.hhsnopek.photopen.data

import android.content.ContentResolver
import android.content.ContentUris
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Reads the whole photo and video library from MediaStore.
 *
 * Images and videos live in different tables with different columns (DURATION
 * only exists on the video table before API 29), so each is queried on its own
 * and the two lists are merged by date. Partial access on API 34+ needs no
 * special case: MediaStore only returns the rows the user granted.
 */
class MediaLibrary(
    private val contentResolver: ContentResolver,
) {
    suspend fun load(): List<MediaItem> =
        withContext(Dispatchers.IO) {
            val images = queryImages()
            val videos = queryVideos()
            (images + videos).sortedByDescending { it.dateMillis }
        }

    /**
     * MediaStore changes arrive asynchronously after a camera write or a
     * delete. The caller refreshes; the observer only says "something moved".
     */
    fun observe(onChanged: () -> Unit): ContentObserver {
        val observer =
            object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(
                    selfChange: Boolean,
                    uri: Uri?,
                ) {
                    onChanged()
                }
            }
        contentResolver.registerContentObserver(imagesUri, true, observer)
        contentResolver.registerContentObserver(videosUri, true, observer)
        return observer
    }

    fun stopObserving(observer: ContentObserver) {
        contentResolver.unregisterContentObserver(observer)
    }

    private fun queryImages(): List<MediaItem> {
        val projection =
            arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DATE_TAKEN,
                MediaStore.Images.Media.DATE_ADDED,
                MediaStore.Images.Media.DATE_MODIFIED,
                MediaStore.Images.Media.WIDTH,
                MediaStore.Images.Media.HEIGHT,
                MediaStore.Images.Media.BUCKET_ID,
                MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
                MediaStore.Images.Media.MIME_TYPE,
            )
        return query(
            uri = imagesUri,
            projection = projection,
            isVideo = false,
            durationColumn = null,
        )
    }

    private fun queryVideos(): List<MediaItem> {
        val projection =
            arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DATE_TAKEN,
                MediaStore.Video.Media.DATE_ADDED,
                MediaStore.Video.Media.DATE_MODIFIED,
                MediaStore.Video.Media.DURATION,
                MediaStore.Video.Media.WIDTH,
                MediaStore.Video.Media.HEIGHT,
                MediaStore.Video.Media.BUCKET_ID,
                MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
                MediaStore.Video.Media.MIME_TYPE,
            )
        return query(
            uri = videosUri,
            projection = projection,
            isVideo = true,
            durationColumn = MediaStore.Video.Media.DURATION,
        )
    }

    private fun query(
        uri: Uri,
        projection: Array<String>,
        isVideo: Boolean,
        durationColumn: String?,
    ): List<MediaItem> {
        val items = ArrayList<MediaItem>()
        val sortOrder = "${MediaStore.MediaColumns.DATE_TAKEN} DESC, ${MediaStore.MediaColumns.DATE_ADDED} DESC"

        contentResolver
            .query(uri, projection, null, null, sortOrder)
            ?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                val takenCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_TAKEN)
                val addedCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
                val modifiedCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
                val widthCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.WIDTH)
                val heightCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.HEIGHT)
                val bucketIdCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_ID)
                val bucketCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
                val durationCol = durationColumn?.let { cursor.getColumnIndex(it) } ?: -1

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val taken = cursor.getLong(takenCol)
                    val addedSeconds = cursor.getLong(addedCol)
                    val modifiedSeconds = cursor.getLong(modifiedCol)
                    // DATE_ADDED is when MediaStore indexed the row, not when the
                    // photo was taken: a file pulled in by adb or restored from a
                    // backup all share one timestamp. Prefer the capture time,
                    // then the file's own modification time, then index time.
                    val dateMillis =
                        when {
                            taken > 0 -> taken
                            modifiedSeconds > 0 -> modifiedSeconds * 1_000L
                            else -> addedSeconds * 1_000L
                        }
                    items +=
                        MediaItem(
                            id = id,
                            uri = ContentUris.withAppendedId(uri, id),
                            isVideo = isVideo,
                            dateMillis = dateMillis,
                            durationMs = if (durationCol >= 0) cursor.getLong(durationCol) else 0L,
                            width = cursor.getInt(widthCol),
                            height = cursor.getInt(heightCol),
                            bucketId = cursor.getString(bucketIdCol).orEmpty(),
                            bucketName = cursor.getString(bucketCol).orEmpty(),
                            mimeType = cursor.getString(mimeCol).orEmpty(),
                        )
                }
            }
        return items
    }

    private companion object {
        // "external" on API 29+ means every external volume, so media on a
        // removable SD card is included. Below 29 the deprecated per-table
        // external URI is the only form that exists.
        val imagesUri: Uri =
            allVolumes(
                newer = { MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL) },
                legacy = {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                },
            )
        val videosUri: Uri =
            allVolumes(
                newer = { MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL) },
                legacy = {
                    @Suppress("DEPRECATION")
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                },
            )

        private inline fun allVolumes(
            newer: () -> Uri,
            legacy: () -> Uri,
        ): Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) newer() else legacy()
    }
}
