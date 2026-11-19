package org.nekosukuriputo.nekuva.download

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import java.io.File
import org.nekosukuriputo.nekuva.local.data.output.LocalMangaDirOutput
import org.nekosukuriputo.nekuva.parsers.model.Manga
import org.nekosukuriputo.nekuva.parsers.model.MangaChapter
import org.nekosukuriputo.nekuva.parsers.model.MangaParserSource
import org.nekosukuriputo.nekuva.parsers.model.MangaState

class DownloadFlowTest {

    @Test
    fun testDirOutputFlow() = runBlocking {
        val rootDir = File("/tmp/test_download_root")
        rootDir.mkdirs()
        val manga = Manga(
            id = 12345L,
            title = "Test Manga Title",
            altTitles = emptySet(),
            url = "/test",
            publicUrl = "https://example.com/test",
            rating = 0f,
            contentRating = null,
            coverUrl = null,
            largeCoverUrl = null,
            description = null,
            tags = emptySet(),
            state = MangaState.ONGOING,
            authors = emptySet(),
            source = MangaParserSource.SHINIGAMI,
            chapters = null,
        )
        val chapter = MangaChapter(
            id = 67890L,
            title = "Chapter 1",
            number = 1f,
            volume = 1,
            url = "/test/1",
            scanlator = null,
            uploadDate = 0L,
            branch = null,
            source = MangaParserSource.SHINIGAMI,
        )

        val output = LocalMangaDirOutput(File(rootDir, "Test Manga Title"), manga)
        try {
            // Test addCover
            val coverFile = File.createTempFile("cover", ".jpg.tmp", rootDir)
            coverFile.writeBytes(ByteArray(100) { 1 })
            output.addCover(coverFile, "image/jpeg")
            coverFile.delete()

            // Test addPage
            val pageFile = File.createTempFile("page", ".jpg.tmp", rootDir)
            pageFile.writeBytes(ByteArray(500) { 2 })
            output.addPage(IndexedValue(0, chapter), pageFile, 0, "image/jpeg")
            pageFile.delete()

            val flushed = output.flushChapter(chapter)
            assertTrue(flushed)

            output.finish()
        } finally {
            output.close()
            rootDir.deleteRecursively()
        }
    }
}
