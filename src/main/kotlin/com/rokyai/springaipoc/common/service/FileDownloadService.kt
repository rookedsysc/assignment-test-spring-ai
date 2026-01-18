package com.rokyai.springaipoc.common.service

import org.springframework.core.io.ByteArrayResource
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * 파일 다운로드 기능을 제공하는 공통 서비스
 *
 * 다양한 포맷의 파일을 생성하고 다운로드할 수 있는 ResponseEntity를 생성합니다.
 */
@Service
class FileDownloadService {

    /**
     * 일반 파일 다운로드를 위한 ResponseEntity를 생성합니다.
     *
     * @param content 파일 바이트 배열
     * @param fileName 다운로드될 파일명 (확장자 포함)
     * @param contentType MIME 타입
     * @return 파일 다운로드를 위한 ResponseEntity
     */
    fun createFileResponse(
        content: ByteArray,
        fileName: String,
        contentType: String
    ): ResponseEntity<ByteArrayResource> {
        val resource = ByteArrayResource(content)

        val headers = HttpHeaders().apply {
            add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"$fileName\"")
            add(HttpHeaders.CONTENT_TYPE, contentType)
        }

        return ResponseEntity
            .ok()
            .headers(headers)
            .contentLength(content.size.toLong())
            .body(resource)
    }

    /**
     * 여러 파일을 zip으로 압축하여 다운로드를 위한 ResponseEntity를 생성합니다.
     *
     * @param files 압축할 파일들의 Map (파일명 -> 파일 내용)
     * @param zipFileNamePrefix zip 파일명 접두사 (예: "anki_package"). 타임스탬프가 자동으로 추가됩니다.
     * @return zip 파일 다운로드를 위한 ResponseEntity
     */
    fun createZipFileResponse(
        files: Map<String, String>,
        zipFileNamePrefix: String
    ): ResponseEntity<ByteArrayResource> {
        val zipFileName = generateFileName(zipFileNamePrefix, "zip")
        val zipBytes = createZipBytes(files)

        return createFileResponse(
            content = zipBytes,
            fileName = zipFileName,
            contentType = "application/zip"
        )
    }

    /**
     * 여러 파일을 zip 바이트 배열로 압축합니다.
     *
     * @param files 압축할 파일들의 Map (파일명 -> 파일 내용)
     * @return zip 파일의 바이트 배열
     */
    private fun createZipBytes(files: Map<String, String>): ByteArray {
        val byteArrayOutputStream = ByteArrayOutputStream()

        ZipOutputStream(byteArrayOutputStream).use { zipOut ->
            files.forEach { (fileName, content) ->
                val zipEntry = ZipEntry(fileName)
                zipOut.putNextEntry(zipEntry)
                zipOut.write(content.toByteArray(Charsets.UTF_8))
                zipOut.closeEntry()
            }
        }

        return byteArrayOutputStream.toByteArray()
    }

    /**
     * 타임스탬프가 포함된 고유한 파일명을 생성합니다.
     *
     * @param prefix 파일명 앞에 붙을 접두사 (예: "anki", "search")
     * @param extension 파일 확장자 (점 제외, 예: "md", "txt")
     * @return 타임스탬프가 포함된 파일명 (예: "anki_20240115_143022.md")
     */
    fun generateFileName(prefix: String, extension: String): String {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        return "${prefix}_${timestamp}.${extension}"
    }
}
