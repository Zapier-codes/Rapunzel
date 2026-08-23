package io.aatricks.easyreader.data.repository.epub

import android.util.Log
import io.aatricks.easyreader.data.local.AppDatabase
import io.aatricks.easyreader.data.local.ChapterEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lightweight EPUB exporter.
 * Generates EPUB 3.2 from selected chapters without external libraries.
 */
@Singleton
class EpubExporter @Inject constructor(
    private val database: AppDatabase,
) {
    private val tag = "EpubExporter"

    data class ExportResult(
        val file: File,
        val chapterCount: Int,
    )

    /**
     * Export selected chapters of a book to EPUB.
     * @param bookId Local book ID
     * @param chapterIds Specific chapters to export, or null for all
     * @param outputFile Destination file
     */
    suspend fun exportBook(
        bookId: String,
        chapterIds: List<Long>? = null,
        outputFile: File,
    ): Result<ExportResult> = withContext(Dispatchers.IO) {
        try {
            val book = database.bookDao().getBookById(bookId)
                ?: return@withContext Result.failure(Exception("Book not found"))
            val chapters = if (chapterIds != null) {
                database.chapterDao().getChaptersForBook(bookId).filter { it.id in chapterIds }
            } else {
                database.chapterDao().getChaptersForBook(bookId)
            }

            if (chapters.isEmpty()) {
                return@withContext Result.failure(Exception("No chapters to export"))
            }

            writeEpub(outputFile, book.title, book.author, chapters)
            Result.success(ExportResult(outputFile, chapters.size))
        } catch (e: RuntimeException) {
            Log.e(tag, "Export failed", e)
            Result.failure(e)
        }
    }

    private fun writeEpub(output: File, title: String, author: String?, chapters: List<ChapterEntity>) {
        ZipOutputStream(FileOutputStream(output)).use { zos ->
            writeMimetype(zos)
            writeContainerXml(zos)
            writeContentOpf(zos, title, author, chapters)
            writeTocNcx(zos, title, chapters)
            writeChapterFiles(zos, chapters)
        }
    }

    private fun writeMimetype(zos: ZipOutputStream) {
        // mimetype must be first and uncompressed
        zos.putNextEntry(ZipEntry("mimetype").apply { method = ZipEntry.STORED })
        zos.write("application/epub+zip".toByteArray())
        zos.closeEntry()
    }

    private fun writeContainerXml(zos: ZipOutputStream) {
        zos.putNextEntry(ZipEntry("META-INF/container.xml"))
        zos.write(
            """<?xml version="1.0"?>
<container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
    <rootfiles>
        <rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/>
    </rootfiles>
</container>""".trimIndent().toByteArray()
        )
        zos.closeEntry()
    }

    private fun writeContentOpf(
        zos: ZipOutputStream,
        title: String,
        author: String?,
        chapters: List<ChapterEntity>,
    ) {
        val chapterItems = chapters.mapIndexed { i, ch ->
            """<item id="ch${ch.id}" href="chapter${i + 1}.xhtml" media-type="application/xhtml+xml"/>"""
        }.joinToString("\n        ")
        val spineItems = chapters.mapIndexed { i, ch ->
            """<itemref idref="ch${ch.id}"/>"""
        }.joinToString("\n        ")

        zos.putNextEntry(ZipEntry("OEBPS/content.opf"))
        zos.write(
            """<?xml version="1.0" encoding="UTF-8"?>
<package version="3.0" xmlns="http://www.idpf.org/2007/opf">
    <metadata xmlns:dc="http://purl.org/dc/elements/1.1/">
        <dc:title>$title</dc:title>
        <dc:creator>${author ?: "Unknown"}</dc:creator>
        <dc:identifier id="bookid">urn:uuid:${java.util.UUID.randomUUID()}</dc:identifier>
        <dc:language>en</dc:language>
    </metadata>
    <manifest>
        <item id="toc" href="toc.ncx" media-type="application/x-dtbncx+xml"/>
        $chapterItems
    </manifest>
    <spine toc="toc">
        $spineItems
    </spine>
</package>""".toByteArray()
        )
        zos.closeEntry()
    }

    private fun writeTocNcx(zos: ZipOutputStream, title: String, chapters: List<ChapterEntity>) {
        val navPoints = chapters.mapIndexed { i, ch ->
            """<navPoint id="navPoint-${i + 1}" playOrder="${i + 1}">
            <navLabel><text>${escapeXml(ch.title ?: "Chapter ${i + 1}")}</text></navLabel>
            <content src="chapter${i + 1}.xhtml"/>
        </navPoint>"""
        }.joinToString("\n        ")

        zos.putNextEntry(ZipEntry("OEBPS/toc.ncx"))
        zos.write(
            """<?xml version="1.0" encoding="UTF-8"?>
<ncx version="2005-1" xmlns="http://www.daisy.org/z3986/2005/ncx/">
    <head>
        <meta name="dtb:uid" content="urn:uuid:${java.util.UUID.randomUUID()}"/>
        <meta name="dtb:depth" content="1"/>
        <meta name="dtb:totalPageCount" content="0"/>
        <meta name="dtb:maxPageNumber" content="0"/>
    </head>
    <docTitle><text>${escapeXml(title)}</text></docTitle>
    <navMap>
        $navPoints
    </navMap>
</ncx>""".toByteArray()
        )
        zos.closeEntry()
    }

    private fun writeChapterFiles(zos: ZipOutputStream, chapters: List<ChapterEntity>) {
        chapters.forEachIndexed { i, ch ->
            val chapterTitle = ch.title ?: "Chapter ${i + 1}"
            val body = ch.content?.let { escapeXml(it).replace("\n", "</p>\n<p>") } ?: ""
            val html = """<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
    <title>${escapeXml(chapterTitle)}</title>
    <meta charset="UTF-8"/>
</head>
<body>
    <h1>${escapeXml(chapterTitle)}</h1>
    $body
</body>
</html>"""
            zos.putNextEntry(ZipEntry("OEBPS/chapter${i + 1}.xhtml"))
            zos.write(html.toByteArray(Charsets.UTF_8))
            zos.closeEntry()
        }
    }

    private fun escapeXml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}
