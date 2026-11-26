package moe.nyarchlinux.catgirldownloader.storage

/** Replaces GdkPixbuf's format sniffing: works out extension and MIME type of downloaded bytes. */
data class ImageFormat(val extension: String, val mimeType: String) {
    companion object {
        val PNG = ImageFormat("png", "image/png")
        val JPEG = ImageFormat("jpg", "image/jpeg")
        val GIF = ImageFormat("gif", "image/gif")
        val WEBP = ImageFormat("webp", "image/webp")
        val AVIF = ImageFormat("avif", "image/avif")
        private val known = listOf(PNG, JPEG, GIF, WEBP, AVIF)

        fun detect(bytes: ByteArray, contentType: String?, url: String?): ImageFormat? =
            fromMagic(bytes) ?: fromContentType(contentType) ?: fromUrl(url)

        internal fun fromMagic(b: ByteArray): ImageFormat? {
            fun at(offset: Int, vararg sig: Int) =
                b.size >= offset + sig.size && sig.indices.all { b[offset + it].toInt() and 0xFF == sig[it] }
            fun ascii(offset: Int, s: String) = at(offset, *s.map { it.code }.toIntArray())
            return when {
                at(0, 0x89, 0x50, 0x4E, 0x47) -> PNG
                at(0, 0xFF, 0xD8, 0xFF) -> JPEG
                ascii(0, "GIF8") -> GIF
                ascii(0, "RIFF") && ascii(8, "WEBP") -> WEBP
                ascii(4, "ftypavif") || ascii(4, "ftypavis") -> AVIF
                else -> null
            }
        }

        private fun fromContentType(contentType: String?): ImageFormat? {
            val mime = contentType?.substringBefore(';')?.trim()?.lowercase() ?: return null
            return known.firstOrNull { it.mimeType == mime }
        }

        private fun fromUrl(url: String?): ImageFormat? {
            val ext = url?.substringBefore('?')?.substringAfterLast('/')
                ?.substringAfterLast('.', "")?.lowercase() ?: return null
            if (ext == "jpeg") return JPEG
            return known.firstOrNull { it.extension == ext }
        }
    }
}
