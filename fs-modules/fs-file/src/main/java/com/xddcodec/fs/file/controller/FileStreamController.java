package com.xddcodec.fs.file.controller;

import com.aliyun.oss.internal.Mimetypes;
import com.xddcodec.fs.file.domain.FileInfo;
import com.xddcodec.fs.file.service.FileInfoService;
import com.xddcodec.fs.storage.facade.StorageServiceFacade;
import com.xddcodec.fs.storage.plugin.core.IStorageOperationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@RestController
@RequestMapping("/api/file/stream")
@RequiredArgsConstructor
public class FileStreamController {

    private final FileInfoService fileInfoService;
    private final StorageServiceFacade storageServiceFacade;

    private static final long SMALL_FILE_SIZE = 10 * 1024 * 1024;
    private static final int BUFFER_SIZE = 8192;

    @GetMapping("/preview/{fileId}")
    public ResponseEntity<?> preview(
            @PathVariable String fileId,
            @RequestHeader(value = "Range", required = false) String rangeHeader) {

        FileInfo file = fileInfoService.getById(fileId);
        if (file == null) {
            return ResponseEntity.notFound().build();
        }

        if (rangeHeader != null && isMediaFile(file.getSuffix())) {
            return streamWithRange(file, rangeHeader);
        } else if (file.getSize() > SMALL_FILE_SIZE) {
            return streamFullFile(file);
        } else {
            return directTransfer(file);
        }
    }

    private ResponseEntity<byte[]> directTransfer(FileInfo file) {
        try {
            IStorageOperationService storage = storageServiceFacade
                    .getStorageService(file.getStoragePlatformSettingId());

            byte[] fileBytes;
            try (InputStream in = storage.getFileStream(file.getObjectKey())) {
                fileBytes = in.readAllBytes();
            }

            return ResponseEntity.ok()
                    .headers(buildHeaders(file, fileBytes.length))
                    .body(fileBytes);
        } catch (Exception e) {
            log.error("直接传输失败: {}", file.getDisplayName(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private ResponseEntity<StreamingResponseBody> streamFullFile(FileInfo file) {
        try {
            IStorageOperationService storage = storageServiceFacade
                    .getStorageService(file.getStoragePlatformSettingId());

            StreamingResponseBody body = out -> {
                try (InputStream in = storage.getFileStream(file.getObjectKey())) {
                    in.transferTo(out);
                }
            };

            return ResponseEntity.ok()
                    .headers(buildHeaders(file, file.getSize()))
                    .body(body);
        } catch (Exception e) {
            log.error("流式传输失败: {}", file.getDisplayName(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private ResponseEntity<StreamingResponseBody> streamWithRange(
            FileInfo file, String rangeHeader) {

        long fileSize = file.getSize();
        long[] range = parseRange(rangeHeader, fileSize);
        long start = range[0];
        long end = range[1];
        long contentLength = end - start + 1;

        try {
            IStorageOperationService storage = storageServiceFacade
                    .getStorageService(file.getStoragePlatformSettingId());

            StreamingResponseBody body = out -> {
                try (InputStream in = storage.getFileStream(file.getObjectKey())) {
                    skipBytes(in, start);
                    transferBytes(in, out, contentLength);
                }
            };

            return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                    .headers(buildRangeHeaders(file, start, end, fileSize))
                    .body(body);
        } catch (Exception e) {
            log.error("Range传输失败: {}", file.getDisplayName(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private void skipBytes(InputStream in, long count) throws IOException {
        long skipped = 0;
        while (skipped < count) {
            long n = in.skip(count - skipped);
            if (n > 0) {
                skipped += n;
            } else {
                byte[] buffer = new byte[BUFFER_SIZE];
                int read = in.read(buffer, 0, (int) Math.min(count - skipped, buffer.length));
                if (read <= 0) break;
                skipped += read;
            }
        }
    }

    private void transferBytes(InputStream in, java.io.OutputStream out, long count) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        long remaining = count;
        while (remaining > 0) {
            int toRead = (int) Math.min(buffer.length, remaining);
            int bytesRead = in.read(buffer, 0, toRead);
            if (bytesRead == -1) break;
            out.write(buffer, 0, bytesRead);
            remaining -= bytesRead;
        }
    }

    private boolean isMediaFile(String ext) {
        return ext != null && ext.matches("(?i)mp4|webm|ogg|mp3|wav|flac|avi|mkv|mov");
    }

    private long[] parseRange(String rangeHeader, long fileSize) {
        long start = 0, end = fileSize - 1;
        if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
            Pattern pattern = Pattern.compile("bytes=(\\d*)-(\\d*)");
            Matcher matcher = pattern.matcher(rangeHeader);
            if (matcher.find()) {
                String startStr = matcher.group(1);
                String endStr = matcher.group(2);
                if (!startStr.isEmpty()) start = Long.parseLong(startStr);
                if (!endStr.isEmpty()) end = Long.parseLong(endStr);
            }
        }
        return new long[]{Math.max(0, start), Math.min(end, fileSize - 1)};
    }

    private HttpHeaders buildHeaders(FileInfo file, long contentLength) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_TYPE,
                Mimetypes.getInstance().getMimetype(file.getDisplayName()));
        headers.setContentLength(contentLength);
        headers.add(HttpHeaders.CONTENT_DISPOSITION,
                "inline; filename*=UTF-8''" + encodeFileName(file.getDisplayName()));
        headers.add(HttpHeaders.ACCEPT_RANGES, "bytes");
        addCorsHeaders(headers);
        return headers;
    }

    private HttpHeaders buildRangeHeaders(FileInfo file, long start, long end, long fileSize) {
        HttpHeaders headers = buildHeaders(file, end - start + 1);
        headers.set(HttpHeaders.CONTENT_RANGE,
                String.format("bytes %d-%d/%d", start, end, fileSize));
        return headers;
    }

    private void addCorsHeaders(HttpHeaders headers) {
        headers.add("Access-Control-Allow-Origin", "*");
        headers.add("Access-Control-Allow-Methods", "GET, HEAD, OPTIONS");
        headers.add("Access-Control-Expose-Headers", "Content-Range, Content-Length");
    }

    private String encodeFileName(String fileName) {
        try {
            return java.net.URLEncoder.encode(fileName, "UTF-8").replace("+", "%20");
        } catch (Exception e) {
            return fileName;
        }
    }
}
