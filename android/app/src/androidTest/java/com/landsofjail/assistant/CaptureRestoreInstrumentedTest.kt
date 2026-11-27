package com.landsofjail.assistant

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.landsofjail.assistant.capture.ScreenCaptureConsentActivity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class CaptureRestoreInstrumentedTest {

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private val preferences
        get() = context.getSharedPreferences("loj_capture_state", Context.MODE_PRIVATE)

    @After
    fun cleanUp() {
        ScreenCaptureConsentActivity.consumeCapture(context)
        preferences.edit().clear().commit()
        File(context.filesDir, "captures").listFiles()?.forEach { it.delete() }
    }

    @Test
    fun croppedFrameIsNeverRecycledWithItsSource() {
        val exactSizeSource = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888)
        val exactSizeCrop = cropFrameBitmap(exactSizeSource, 32, 32)
        assertFalse("Le bitmap transmis doit rester utilisable", exactSizeCrop.isRecycled)
        exactSizeCrop.setPixel(0, 0, android.graphics.Color.RED)
        assertEquals(android.graphics.Color.RED, exactSizeCrop.getPixel(0, 0))
        if (exactSizeCrop !== exactSizeSource) assertTrue(exactSizeSource.isRecycled)
        else exactSizeCrop.recycle()

        val paddedSource = Bitmap.createBitmap(40, 32, Bitmap.Config.ARGB_8888)
        val paddedCrop = cropFrameBitmap(paddedSource, 32, 32)
        assertTrue("La source avec padding doit être libérée", paddedSource.isRecycled)
        assertFalse("Le crop doit rester utilisable", paddedCrop.isRecycled)
        paddedCrop.recycle()
    }

    @Test
    fun restoresPersistedCaptureAndConsumesItSafely() {
        val directory = File(context.filesDir, "captures")
        assertTrue(directory.exists() || directory.mkdirs())
        val capture = File(directory, "restore_test.jpg")
        val bitmap = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888)
        try {
            capture.outputStream().use { output ->
                assertTrue(bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output))
            }
        } finally {
            bitmap.recycle()
        }
        val capturedAt = 1_795_000_000_000L
        preferences.edit()
            .putString("pending_capture_uri", Uri.fromFile(capture).toString())
            .putLong("pending_capture_at", capturedAt)
            .commit()

        ScreenCaptureConsentActivity.restorePendingCapture(context)

        val restored = ScreenCaptureConsentActivity.pendingCapture.value
        assertNotNull(restored)
        assertEquals(capturedAt, restored!!.capturedAtMillis)
        assertFalse(restored.bitmap.isRecycled)
        assertFalse(restored.base64Jpeg.isBlank())

        ScreenCaptureConsentActivity.consumeCapture(context)

        assertNull(ScreenCaptureConsentActivity.pendingCapture.value)
        assertFalse(capture.exists())
        assertNull(preferences.getString("pending_capture_uri", null))
    }

    @Test
    fun rejectsCaptureFileOutsidePrivateCaptureDirectory() {
        val outside = File(context.filesDir, "not-a-capture.jpg")
        outside.writeBytes(byteArrayOf(1, 2, 3))
        preferences.edit()
            .putString("pending_capture_uri", Uri.fromFile(outside).toString())
            .putLong("pending_capture_at", 1L)
            .commit()

        ScreenCaptureConsentActivity.restorePendingCapture(context)

        assertNull(ScreenCaptureConsentActivity.pendingCapture.value)
        assertTrue(outside.exists())
        assertNull(preferences.getString("pending_capture_uri", null))
        outside.delete()
    }
}
