package moe.nyarchlinux.catgirldownloader.storage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StorageTest {
    private fun bytes(vararg b: Int) = ByteArray(b.size) { b[it].toByte() }

    @Test
    fun detectsFormatFromMagicBytes() {
        assertEquals(ImageFormat.PNG, ImageFormat.detect(bytes(0x89, 0x50, 0x4E, 0x47, 0x0D), null, null))
        assertEquals(ImageFormat.JPEG, ImageFormat.detect(bytes(0xFF, 0xD8, 0xFF, 0xE0), "image/png", null))
        assertEquals(ImageFormat.GIF, ImageFormat.detect("GIF89a".toByteArray(), null, null))
        assertEquals(ImageFormat.WEBP, ImageFormat.detect("RIFF\u0000\u0000\u0000\u0000WEBPVP8".toByteArray(), null, null))
    }

    @Test
    fun fallsBackToContentTypeThenUrl() {
        val junk = bytes(1, 2, 3)
        assertEquals(ImageFormat.WEBP, ImageFormat.detect(junk, "image/webp; charset=binary", null))
        assertEquals(ImageFormat.JPEG, ImageFormat.detect(junk, "application/octet-stream", "https://x/y.JPEG?a=b"))
        assertNull(ImageFormat.detect(junk, "text/html", "https://x/y.mp4"))
    }

    @Test
    fun uniqueNameAppendsCounter() {
        val existing = setOf("a.png", "a-1.png", "noext")
        assertEquals("b.png", ImageSaver.uniqueName("b.png") { it in existing })
        assertEquals("a-2.png", ImageSaver.uniqueName("a.png") { it in existing })
        assertEquals("noext-1", ImageSaver.uniqueName("noext") { it in existing })
    }
}
