package com.landsofjail.assistant

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.landsofjail.assistant.capture.ScreenCaptureConsentActivity
import com.landsofjail.assistant.data.ForticladInventoryStore
import com.landsofjail.assistant.data.ProgressionProfileStore
import com.landsofjail.assistant.model.ForticladInventory
import com.landsofjail.assistant.model.PlayerResourceStock
import com.landsofjail.assistant.model.ProgressionPlayerProfile
import com.landsofjail.assistant.model.ProgressionProfileCodec
import kotlinx.coroutines.runBlocking
import com.landsofjail.assistant.service.cropFrameBitmap
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
    fun forticladInventoryRestoresPerProfileWithoutCrossContamination() {
        val first = ForticladInventoryStore(context, "player-one")
        val second = ForticladInventoryStore(context, "player-two")
        try {
            first.save(ForticladInventory(12_345L, 678L))
            second.save(ForticladInventory(90L, 4L))

            assertEquals(ForticladInventory(12_345L, 678L), ForticladInventoryStore(context, "player-one").load())
            assertEquals(ForticladInventory(90L, 4L), ForticladInventoryStore(context, "player-two").load())
        } finally {
            context.getSharedPreferences("forticlad_inventory_player-one", Context.MODE_PRIVATE).edit().clear().commit()
            context.getSharedPreferences("forticlad_inventory_player-two", Context.MODE_PRIVATE).edit().clear().commit()
        }
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
    fun refusesToReplaceCaptureThatIsStillPending() {
        val directory = File(context.filesDir, "captures")
        assertTrue(directory.exists() || directory.mkdirs())
        val firstFile = File(directory, "pending_first.jpg").apply { writeBytes(byteArrayOf(1, 2, 3)) }
        val secondFile = File(directory, "pending_second.jpg").apply { writeBytes(byteArrayOf(4, 5, 6)) }
        val firstBitmap = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
        val secondBitmap = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
        val first = com.landsofjail.assistant.capture.CapturedScreenPayload(
            firstBitmap, "first", 100L, Uri.fromFile(firstFile).toString()
        )
        val second = com.landsofjail.assistant.capture.CapturedScreenPayload(
            secondBitmap, "second", 200L, Uri.fromFile(secondFile).toString()
        )

        assertTrue(ScreenCaptureConsentActivity.publishCapture(context, first))
        assertFalse(ScreenCaptureConsentActivity.publishCapture(context, second))
        assertEquals(first, ScreenCaptureConsentActivity.pendingCapture.value)
        assertEquals(Uri.fromFile(firstFile).toString(), preferences.getString("pending_capture_uri", null))
        assertTrue(firstFile.exists())
        assertFalse(secondFile.exists())

        secondBitmap.recycle()
        ScreenCaptureConsentActivity.consumeCapture(context)
        assertTrue("La première capture est libérée après consommation", firstBitmap.isRecycled)
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
    fun rejectsEmptyAndOversizedPersistedCaptureFiles() {
        val directory = File(context.filesDir, "captures")
        assertTrue(directory.exists() || directory.mkdirs())

        val empty = File(directory, "empty_capture.jpg")
        empty.writeBytes(byteArrayOf())
        preferences.edit()
            .putString("pending_capture_uri", Uri.fromFile(empty).toString())
            .putLong("pending_capture_at", 1L)
            .commit()

        ScreenCaptureConsentActivity.restorePendingCapture(context)

        assertNull(ScreenCaptureConsentActivity.pendingCapture.value)
        assertFalse("Le fichier vide invalide doit être supprimé", empty.exists())
        assertNull(preferences.getString("pending_capture_uri", null))

        val oversized = File(directory, "oversized_capture.jpg")
        java.io.RandomAccessFile(oversized, "rw").use { file ->
            file.setLength(20L * 1024L * 1024L + 1L)
        }
        preferences.edit()
            .putString("pending_capture_uri", Uri.fromFile(oversized).toString())
            .putLong("pending_capture_at", 2L)
            .commit()

        ScreenCaptureConsentActivity.restorePendingCapture(context)

        assertNull(ScreenCaptureConsentActivity.pendingCapture.value)
        assertFalse("Le fichier trop volumineux invalide doit être supprimé", oversized.exists())
        assertNull(preferences.getString("pending_capture_uri", null))
    }

    @Test
    fun rejectsCorruptedPersistedCaptureImage() {
        val directory = File(context.filesDir, "captures")
        assertTrue(directory.exists() || directory.mkdirs())
        val corrupted = File(directory, "corrupted_capture.jpg")
        corrupted.writeBytes(byteArrayOf(1, 2, 3, 4, 5, 6))
        preferences.edit()
            .putString("pending_capture_uri", Uri.fromFile(corrupted).toString())
            .putLong("pending_capture_at", 3L)
            .commit()

        ScreenCaptureConsentActivity.restorePendingCapture(context)

        assertNull(ScreenCaptureConsentActivity.pendingCapture.value)
        assertFalse("La capture illisible doit être supprimée", corrupted.exists())
        assertNull(preferences.getString("pending_capture_uri", null))
    }

    @Test
    fun rejectsCapturePathTraversalOutsidePrivateDirectory() {
        val outside = File(context.filesDir, "traversal_capture.jpg")
        outside.writeBytes(byteArrayOf(1, 2, 3))
        val traversal = File(context.filesDir, "captures/../traversal_capture.jpg")
        preferences.edit()
            .putString("pending_capture_uri", Uri.fromFile(traversal).toString())
            .putLong("pending_capture_at", 1L)
            .commit()

        ScreenCaptureConsentActivity.restorePendingCapture(context)

        assertNull(ScreenCaptureConsentActivity.pendingCapture.value)
        assertTrue("Un fichier extérieur ne doit pas être supprimé", outside.exists())
        assertNull(preferences.getString("pending_capture_uri", null))
        outside.delete()
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

    @Test
    fun progressionProfileDataStoreMigratesLegacyDataPerProfile() = runBlocking {
        val firstId = "migration-player-one-${System.nanoTime()}"
        val secondId = "migration-player-two-${System.nanoTime()}"
        val firstLegacyProfile = context.getSharedPreferences("progression_profile_$firstId", Context.MODE_PRIVATE)
        val firstLegacyInventory = context.getSharedPreferences("forticlad_inventory_$firstId", Context.MODE_PRIVATE)
        val secondLegacyInventory = context.getSharedPreferences("forticlad_inventory_$secondId", Context.MODE_PRIVATE)
        try {
            val original = ProgressionPlayerProfile(
                profileId = firstId,
                resourceStocks = listOf(PlayerResourceStock("food", 4_000L, true, 10L))
            )
            firstLegacyProfile.edit().putString("profile", ProgressionProfileCodec.encode(original)).commit()
            firstLegacyInventory.edit()
                .putLong("forticlad_cores", 1_250L)
                .putLong("advanced_cores", 75L)
                .commit()
            secondLegacyInventory.edit()
                .putLong("forticlad_cores", 90L)
                .putLong("advanced_cores", 5L)
                .commit()

            val first = ProgressionProfileStore(context, firstId)
            val second = ProgressionProfileStore(context, secondId)
            val migratedFirst = first.load()
            val migratedSecond = second.load()

            assertEquals(4_000L, migratedFirst.confirmedStock("food"))
            assertEquals(1_250L, migratedFirst.confirmedStock("forticlad cores"))
            assertEquals(75L, migratedFirst.confirmedStock("advanced forticlad cores"))
            assertEquals(90L, migratedSecond.confirmedStock("forticlad cores"))
            assertEquals(5L, migratedSecond.confirmedStock("advanced forticlad cores"))

            first.save(migratedFirst.copy(updatedAtEpochMillis = 99L))
            assertEquals(99L, first.load().updatedAtEpochMillis)
            assertEquals(90L, second.load().confirmedStock("forticlad cores"))
        } finally {
            firstLegacyProfile.edit().clear().commit()
            firstLegacyInventory.edit().clear().commit()
            secondLegacyInventory.edit().clear().commit()
        }
    }

}
