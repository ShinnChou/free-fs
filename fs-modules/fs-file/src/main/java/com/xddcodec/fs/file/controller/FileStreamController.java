package com.xddcodec.fs.file.controller;

import com.aliyun.oss.internal.Mimetypes;
import com.xddcodec.fs.file.domain.FileInfo;
import com.xddcodec.fs.file.service.FileInfoService;
import com.xddcodec.fs.framework.common.enums.FileTypeEnum;
import com.xddcodec.fs.framework.preview.config.FilePreviewConfig;
import com.xddcodec.fs.storage.facade.StorageServiceFacade;
import com.xddcodec.fs.storage.plugin.core.IStorageOperationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
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
    private final FilePreviewConfig previewConfig;

    @GetMapping("/preview/{fileId}")
    public ResponseEntity<?> preview(
            @PathVariable String fileId,
            @RequestHeader(value = "Range", required = false) String rangeHeader) {

        FileInfo file = fileInfoService.getById(fileId);
        if (file == null) {
            return ResponseEntity.notFound().build();
        }

        // 检查文件大小限制
        if (file.getSize() > previewConfig.getMaxFileSize()) {
            log.warn("文件过大，拒绝预览: fileId={}, size={}MB", fileId, file.getSize() / 1024 / 1024);
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                    .body("文件过大，无法预览");
        }

        if (rangeHeader != null && isMediaFile(file.getSuffix())) {
            return streamWithRange(file, rangeHeader);
        } else if (file.getSize() > previewConfig.getSmallFileSize()) {
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

    private ResponseEntity<Resource> streamFullFile(FileInfo file) {
        InputStream inputStream = null;
        try {
            IStorageOperationService storage = storageServiceFacade
                    .getStorageService(file.getStoragePlatformSettingId());

            // 获取输入流
            inputStream = storage.getFileStream(file.getObjectKey());
            if (inputStream == null) {
                log.error("无法获取文件流: {}", file.getDisplayName());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
            
            // 包装为 InputStreamResource（Spring会在响应完成后自动关闭）
            InputStreamResource resource = new InputStreamResource(inputStream) {
                // 确保资源可以被正确关闭
                @Override
                public String getFilename() {
                    return file.getDisplayName();
                }
            };

            HttpHeaders headers = buildHeaders(file, file.getSize());
            return ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(file.getSize())
                    .body(resource);
        } catch (Exception e) {
            log.error("流式传输失败: {}", file.getDisplayName(), e);
            // 异常情况下手动关闭流
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException ex) {
                    log.error("关闭输入流失败", ex);
                }
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private ResponseEntity<Resource> streamWithRange(
            FileInfo file, String rangeHeader) {

        long fileSize = file.getSize();
        long[] range = parseRange(rangeHeader, fileSize);
        long start = range[0];
        long end = range[1];
        long contentLength = end - start + 1;

        // 限制单次Range请求大小，防止内存溢出
        if (contentLength > previewConfig.getMaxRangeSize()) {
            log.warn("Range请求过大: fileId={}, requestSize={}MB", 
                    file.getId(), contentLength / 1024 / 1024);
            // 自动调整end，限制在maxRangeSize内
            end = start + previewConfig.getMaxRangeSize() - 1;
            contentLength = previewConfig.getMaxRangeSize();
        }

        try {
            IStorageOperationService storage = storageServiceFacade
                    .getStorageService(file.getStoragePlatformSettingId());

            // 读取指定范围的数据
            byte[] rangeData = new byte[(int) contentLength];
            try (InputStream in = storage.getFileStream(file.getObjectKey())) {
                skipBytes(in, start);
                int totalRead = 0;
                while (totalRead < contentLength) {
                    int bytesRead = in.read(rangeData, totalRead, (int) (contentLength - totalRead));
                    if (bytesRead == -1) break;
                    totalRead += bytesRead;
                }
            }

            // 包装为 InputStreamResource
            InputStreamResource resource = new InputStreamResource(new ByteArrayInputStream(rangeData));

            HttpHeaders headers = buildRangeHeaders(file, start, end, fileSize);
            return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                    .headers(headers)
                    .contentLength(contentLength)
                    .body(resource);
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
                byte[] buffer = new byte[previewConfig.getBufferSize()];
                int read = in.read(buffer, 0, (int) Math.min(count - skipped, buffer.length));
                if (read <= 0) break;
                skipped += read;
            }
        }
    }

    private boolean isMediaFile(String ext) {
        return FileTypeEnum.isMediaFile(ext);
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

        // 获取MIME类型
        String mimeType = Mimetypes.getInstance().getMimetype(file.getDisplayName());

        // 特殊处理PDF文件，确保使用正确的MIME类型
        if (file.getSuffix() != null && file.getSuffix().equalsIgnoreCase("pdf")) {
            mimeType = "application/pdf";
        }

        headers.set(HttpHeaders.CONTENT_TYPE, mimeType);
        headers.setContentLength(contentLength);

        // 对于预览场景，使用inline且不带filename参数，避免浏览器触发下载
        // 只设置inline，不添加filename参数
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline");

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

        // 添加缓存控制，允许浏览器缓存预览内容
        headers.add(HttpHeaders.CACHE_CONTROL, "public, max-age=3600");

        // 明确告诉浏览器不要将响应作为下载处理
        headers.add("X-Content-Type-Options", "nosniff");

        // 防止IDM等下载工具拦截的关键头部
        //  设置为document类型，让下载工具认为这是网页内容而不是文件
        headers.add("X-Frame-Options", "SAMEORIGIN");

        // 添加CSP头，限制资源加载方式
        headers.add("Content-Security-Policy", "default-src 'self' 'unsafe-inline' 'unsafe-eval' data: blob:;");

        // 明确标识这是预览内容，不是下载
        headers.add("X-Content-Purpose", "preview");

        // 禁用下载提示
        headers.add("X-Download-Options", "noopen");
    }

    private String encodeFileName(String fileName) {
        try {
            return java.net.URLEncoder.encode(fileName, "UTF-8").replace("+", "%20");
        } catch (Exception e) {
            return fileName;
        }
    }
}
