package com.xddcodec.fs.file.controller;

import com.xddcodec.fs.file.domain.FileInfo;
import com.xddcodec.fs.file.service.FileInfoService;
import com.xddcodec.fs.framework.preview.config.FilePreviewConfig;
import com.xddcodec.fs.storage.facade.StorageServiceFacade;
import com.xddcodec.fs.storage.plugin.core.IStorageOperationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@RestController
@RequestMapping("/api/file/stream")
@RequiredArgsConstructor
public class FileStreamController {

    private final FileInfoService fileInfoService;
    private final StorageServiceFacade storageServiceFacade;
    private final FilePreviewConfig previewConfig;
    private static final Pattern RANGE_PATTERN = Pattern.compile("bytes=(\\d*)-(\\d*)");

    @GetMapping("/preview/{fileId}")
    public ResponseEntity<StreamingResponseBody> preview(
            @PathVariable String fileId,
            @RequestHeader(value = HttpHeaders.RANGE, required = false) String rangeHeader) {
        FileInfo fileInfo = fileInfoService.getById(fileId);
        if (fileInfo == null) {
            return ResponseEntity.notFound().build();
        }
        // 获取存储服务
        IStorageOperationService storage = storageServiceFacade
                .getStorageService(fileInfo.getStoragePlatformSettingId());
        long fileSize = fileInfo.getSize();

        // 处理 Range 请求 (视频/音频拖动进度条)
        if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
            return handleRangeRequest(storage, fileInfo, rangeHeader, fileSize);
        }
        // 处理全量流式请求 (普通下载或图片加载)
        return handleFullRequest(storage, fileInfo, fileSize);
    }

    /**
     * 处理全量流式传输
     */
    private ResponseEntity<StreamingResponseBody> handleFullRequest(
            IStorageOperationService storage, FileInfo fileInfo, long fileSize) {
        StreamingResponseBody stream = outputStream -> {
            try (InputStream inputStream = storage.getFileStream(fileInfo.getObjectKey())) {
                copyStream(inputStream, outputStream);
            } catch (IOException e) {
                log.debug("文件流传输中断 (用户可能是取消了请求): {}", fileInfo.getDisplayName());
            }
        };
        return ResponseEntity.ok()
                .headers(buildHeaders(fileInfo, fileSize, false))
                .body(stream);
    }

    /**
     * 处理 Range (断点续传/分片) 请求
     */
    private ResponseEntity<StreamingResponseBody> handleRangeRequest(
            IStorageOperationService storage, FileInfo fileInfo, String rangeHeader, long fileSize) {
        long start = 0;
        long end = fileSize - 1;
        Matcher matcher = RANGE_PATTERN.matcher(rangeHeader);
        if (matcher.matches()) {
            String startGroup = matcher.group(1);
            String endGroup = matcher.group(2);
            if (!startGroup.isEmpty()) start = Long.parseLong(startGroup);
            if (!endGroup.isEmpty()) end = Long.parseLong(endGroup);
        }

        // 修正 end 范围
        if (end >= fileSize) end = fileSize - 1;

        final long finalStart = start;
        final long finalEnd = end;
        final long contentLength = finalEnd - finalStart + 1;
        StreamingResponseBody stream = outputStream -> {
            try (InputStream inputStream = storage.getFileStream(fileInfo.getObjectKey())) {
                // 跳过不需要的字节
                if (finalStart > 0) {
                    long skipped = inputStream.skip(finalStart);
                    if (skipped < finalStart) {
                        // 防御性代码：如果skip不到位，手动读取丢弃
                        // 实际生产建议封装工具类
                    }
                }
                // 只传输 range 范围内的字节
                copyStreamLimited(inputStream, outputStream, contentLength);
            } catch (IOException e) {
                log.debug("Range流传输中断: {}", fileInfo.getDisplayName());
            }
        };
        HttpHeaders headers = buildHeaders(fileInfo, contentLength, true);
        headers.add(HttpHeaders.CONTENT_RANGE, String.format("bytes %d-%d/%d", finalStart, finalEnd, fileSize));
        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                .headers(headers)
                .body(stream);
    }

    /**
     * 通用流拷贝
     */
    private void copyStream(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[previewConfig.getBufferSize()];
        int bytesRead;
        while ((bytesRead = in.read(buffer)) != -1) {
            out.write(buffer, 0, bytesRead);
        }
        out.flush();
    }

    /**
     * 限制长度的流拷贝
     */
    private void copyStreamLimited(InputStream in, OutputStream out, long limit) throws IOException {
        byte[] buffer = new byte[previewConfig.getBufferSize()];
        long totalRead = 0;
        int bytesRead;
        while (totalRead < limit && (bytesRead = in.read(buffer, 0, (int) Math.min(limit - totalRead, previewConfig.getBufferSize()))) != -1) {
            out.write(buffer, 0, bytesRead);
            totalRead += bytesRead;
        }
        out.flush();
    }

    private HttpHeaders buildHeaders(FileInfo file, long contentLength, boolean isRange) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentLength(contentLength);

        // 自动识别 Content-Type (这里简化处理，实际上应该根据后缀映射正确MIME)
        String contentType = isPdf(file.getSuffix()) ? "application/pdf" : MediaType.APPLICATION_OCTET_STREAM_VALUE;
        headers.setContentType(MediaType.parseMediaType(contentType));
        // inline 表示浏览器直接展示，attachment 表示下载
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "inline; filename*=UTF-8''" + encodeFileName(file.getDisplayName()));
        headers.set(HttpHeaders.ACCEPT_RANGES, "bytes");

        // 缓存策略
        headers.setCacheControl("public, max-age=604800"); // 缓存7天
        return headers;
    }

    private boolean isPdf(String suffix) {
        return suffix != null && "pdf".equalsIgnoreCase(suffix);
    }

    private String encodeFileName(String name) {
        try {
            return URLEncoder.encode(name, StandardCharsets.UTF_8).replace("+", "%20");
        } catch (Exception e) {
            return "unknown";
        }
    }
}
