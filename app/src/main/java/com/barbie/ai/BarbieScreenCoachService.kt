package com.barbie.ai

import android.app.*
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.*
import java.io.File
import java.io.FileOutputStream

/**
 * User-consented screen coach capture.
 * Captures the device screen locally while the user keeps this mode enabled.
 * The latest frame is stored in app cache for the AI layer to inspect later.
 */
class BarbieScreenCoachService : Service() {
    private var projection: MediaProjection? = null
    private var reader: ImageReader? = null
    private var lastWidth = 0
    private var lastHeight = 0

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(3002, notification())
        val resultCode = intent?.getIntExtra("result_code", Activity.RESULT_CANCELED) ?: return START_NOT_STICKY
        val data = intent.getParcelableExtra<Intent>("projection_data") ?: return START_NOT_STICKY
        val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        projection?.stop()
        projection = manager.getMediaProjection(resultCode, data)
        startCapture()
        return START_STICKY
    }

    private fun startCapture() {
        val dm = resources.displayMetrics
        lastWidth = dm.widthPixels
        lastHeight = dm.heightPixels
        reader?.close()
        reader = ImageReader.newInstance(lastWidth, lastHeight, PixelFormat.RGBA_8888, 2)
        reader?.setOnImageAvailableListener({ r ->
            val image = r.acquireLatestImage() ?: return@setOnImageAvailableListener
            try {
                val plane = image.planes[0]
                val pixelStride = plane.pixelStride
                val rowStride = plane.rowStride
                val rowPadding = rowStride - pixelStride * lastWidth
                val paddedWidth = lastWidth + rowPadding / pixelStride
                val bitmap = Bitmap.createBitmap(paddedWidth, lastHeight, Bitmap.Config.ARGB_8888)
                bitmap.copyPixelsFromBuffer(plane.buffer)
                val cropped = if (paddedWidth == lastWidth) bitmap else Bitmap.createBitmap(bitmap, 0, 0, lastWidth, lastHeight)
                if (cropped !== bitmap) bitmap.recycle()
                val file = File(cacheDir, "barbie_screen_latest.jpg")
                FileOutputStream(file).use { cropped.compress(Bitmap.CompressFormat.JPEG, 65, it) }
                cropped.recycle()
            } finally {
                image.close()
            }
        }, Handler(Looper.getMainLooper()))
        projection?.createVirtualDisplay("BarbieScreenCoach", lastWidth, lastHeight, dm.densityDpi, 0, reader?.surface, null, null)
    }

    private fun notification(): Notification {
        val channelId = "barbie_screen_coach"
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(channelId, "Barbie Screen Coach", NotificationManager.IMPORTANCE_LOW))
        return Notification.Builder(this, channelId)
            .setContentTitle("Barbie Screen Coach ON")
            .setContentText("Barbie tumhari ijazat se screen dekh rahi hai")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        reader?.close()
        reader = null
        projection?.stop()
        projection = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?) = null
}
