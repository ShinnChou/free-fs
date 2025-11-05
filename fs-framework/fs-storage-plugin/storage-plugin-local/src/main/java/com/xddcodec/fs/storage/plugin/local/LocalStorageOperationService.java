package com.xddcodec.fs.storage.plugin.local;

import cn.hutool.core.io.FileUtil;
import com.xddcodec.fs.framework.common.enums.StoragePlatformIdentifierEnum;
import com.xddcodec.fs.framework.common.exception.StorageConfigException;
import com.xddcodec.fs.framework.common.exception.StorageOperationException;
import com.xddcodec.fs.storage.plugin.core.AbstractStorageOperationService;
import com.xddcodec.fs.storage.plugin.core.config.StorageConfig;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 本地存储插件实现
 *
 * @Author: xddcode
 * @Date: 2024/10/26 17:00
 */
@Slf4j
public class LocalStorageOperationService extends AbstractStorageOperationService {

    private String basePath;
    private String baseUrl;

    @SuppressWarnings("unused")
    public LocalStorageOperationService() {
        super();
    }

    @SuppressWarnings("unused")
    public LocalStorageOperationService(StorageConfig config) {
        super(config);
    }

    @Override
    public String getPlatformIdentifier() {
        return StoragePlatformIdentifierEnum.LOCAL.getIdentifier();
    }

    @Override
    protected void validateConfig(StorageConfig config) {
        String basePath = config.getRequiredProperty("basePath", String.class);
        String baseUrl = config.getRequiredProperty("baseUrl", String.class);

        if (basePath == null || basePath.trim().isEmpty()) {
            throw new StorageConfigException("Local 存储配置错误：basePath 不能为空");
        }

        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            throw new StorageConfigException("Local 存储配置错误：baseUrl 不能为空");
        }
    }

    @Override
    protected void initialize(StorageConfig config) {
        String rawBasePath = config.getRequiredProperty("basePath", String.class);
        this.basePath = normalizePath(rawBasePath, File.separator, "basePath 不能为空");

        String rawBaseUrl = config.getRequiredProperty("baseUrl", String.class);
        this.baseUrl = normalizePath(rawBaseUrl, "/", "baseUrl 不能为空");
        // 创建存储目录
        File baseDir = new File(this.basePath);
        if (!baseDir.exists()) {
            if (!baseDir.mkdirs()) {
                throw new StorageOperationException("无法创建存储目录: " + this.basePath);
            }
            log.info("创建存储目录: {}", this.basePath);
        }

        log.debug("{} Local 存储初始化完成: {}", getLogPrefix(), this.basePath);
    }

    /**
     * 规范化路径（去除末尾分隔符）
     */
    private String normalizePath(String path, String separator, String errorMsg) {
        if (path == null || path.trim().isEmpty()) {
            throw new IllegalArgumentException(errorMsg);
        }
        String trimmed = path.trim();
        return trimmed.endsWith(separator)
                ? trimmed.substring(0, trimmed.length() - separator.length())
                : trimmed;
    }

    /**
     * 解析完整文件路径
     */
    private String resolveFullPath(String objectKey) {
        String normalizedObjectKey = objectKey.startsWith("/") || objectKey.startsWith("\\")
                ? objectKey.substring(1)
                : objectKey;
        return basePath + File.separator + normalizedObjectKey;
    }

    @Override
    public void uploadFile(InputStream inputStream, String objectKey) {
        ensureNotPrototype();
        try {
            String fullPath = resolveFullPath(objectKey);
            File targetFile = new File(fullPath);

            File parentDir = targetFile.getParentFile();
            if (parentDir != null && !parentDir.exists() && !parentDir.mkdirs()) {
                throw new StorageOperationException("无法创建目录: " + parentDir);
            }

            try (FileOutputStream fos = new FileOutputStream(targetFile)) {
                inputStream.transferTo(fos);
            }

            log.debug("{} 文件上传成功: objectKey={}", getLogPrefix(), objectKey);
        } catch (IOException e) {
            throw new StorageOperationException("文件上传失败: " + e.getMessage(), e);
        }
    }

    @Override
    public InputStream downloadFile(String objectKey) {
        ensureNotPrototype();

        try {
            String fullPath = resolveFullPath(objectKey);
            File file = new File(fullPath);

            if (!file.exists()) {
                throw new StorageOperationException("文件不存在: " + objectKey);
            }

            return new FileInputStream(file);
        } catch (IOException e) {
            throw new StorageOperationException("文件下载失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteFile(String objectKey) {
        ensureNotPrototype();

        try {
            String fullPath = resolveFullPath(objectKey);
            File file = new File(fullPath);

            if (!file.exists()) {
                log.debug("{} 文件不存在，视为删除成功: objectKey={}", getLogPrefix(), objectKey);
            }

            boolean deleted = file.delete();
            if (!deleted) {
                log.error("{} 文件删除失败: objectKey={}", getLogPrefix(), objectKey);
                throw new StorageOperationException("文件删除失败: " + objectKey);
            }

            log.debug("{} 文件删除成功: objectKey={}", getLogPrefix(), objectKey);
        } catch (SecurityException e) {
            log.error("{} 文件删除失败（权限不足）: objectKey={}", getLogPrefix(), objectKey, e);
            throw new StorageOperationException("文件删除失败（权限不足）: " + objectKey, e);
        }
    }

    @Override
    public String getFileUrl(String objectKey, Integer expireSeconds) {
        ensureNotPrototype();
        String normalizedObjectKey = objectKey.startsWith("/")
                ? objectKey.substring(1)
                : objectKey;

        return baseUrl + "/" + normalizedObjectKey;
    }

    @Override
    public boolean isFileExist(String objectKey) {
        ensureNotPrototype();
        String fullPath = resolveFullPath(objectKey);
        return new File(fullPath).exists();
    }

    @Override
    public String initiateMultipartUpload(String objectKey, String mimeType, String fileIdentifier) {
        ensureNotPrototype();

        // 本地存储的分片上传实现
        // 生成临时目录用于存储分片
        String tempDir = basePath + ".temp/" + fileIdentifier + "/";
        File tempDirFile = new File(tempDir);
        if (!tempDirFile.exists()) {
            if (!tempDirFile.mkdirs()) {
                throw new StorageOperationException("无法创建分片上传临时目录: " + tempDir);
            }
        }

        // 返回临时目录路径作为uploadId
        String uploadId = fileIdentifier;
        log.info("本地存储分片上传初始化成功: objectKey={}, uploadId={}", objectKey, uploadId);
        return uploadId;
    }

    @Override
    public String uploadPart(String objectKey, String uploadId, int partNumber, long partSize,
                             InputStream partInputStream, String partIdentifierForLocal) {
        ensureNotPrototype();

        try {
            // 构建分片文件路径
            String tempDir = basePath + ".temp/" + uploadId + "/";
            String partFileName = "part_" + partNumber;
            String partFilePath = tempDir + partFileName;

            // 写入分片文件
            try (FileOutputStream fos = new FileOutputStream(partFilePath)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = partInputStream.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                }
            }

            // 生成分片标识（使用文件大小和修改时间）
            File partFile = new File(partFilePath);
            String etag = String.valueOf(partFile.length()) + "_" + partFile.lastModified();

            log.debug("本地存储分片上传成功: objectKey={}, partNumber={}, etag={}", objectKey, partNumber, etag);
            return etag;

        } catch (IOException e) {
            log.error("本地存储分片上传失败, objectKey={}, partNumber={}: {}", objectKey, partNumber, e.getMessage(), e);
            throw new StorageOperationException("本地存储分片上传失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String completeMultipartUpload(String objectKey, String uploadId, List<Map<String, Object>> partETags) {
        ensureNotPrototype();

        try {
            // 构建最终文件路径
            String fullPath = basePath + objectKey;
            File targetFile = new File(fullPath);

            // 确保父目录存在
            File parentDir = targetFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                if (!parentDir.mkdirs()) {
                    throw new StorageOperationException("无法创建目录: " + parentDir.getAbsolutePath());
                }
            }

            // 合并分片文件
            String tempDir = basePath + ".temp/" + uploadId + "/";
            try (FileOutputStream fos = new FileOutputStream(targetFile)) {
                // 按分片号排序
                partETags.sort((a, b) -> {
                    int partNumA = (int) a.get("partNumber");
                    int partNumB = (int) b.get("partNumber");
                    return Integer.compare(partNumA, partNumB);
                });

                // 依次读取并合并分片
                for (Map<String, Object> partInfo : partETags) {
                    int partNumber = (int) partInfo.get("partNumber");
                    String partFilePath = tempDir + "part_" + partNumber;
                    File partFile = new File(partFilePath);

                    if (!partFile.exists()) {
                        throw new StorageOperationException("分片文件不存在: " + partFilePath);
                    }

                    try (FileInputStream fis = new FileInputStream(partFile)) {
                        byte[] buffer = new byte[8192];
                        int bytesRead;
                        while ((bytesRead = fis.read(buffer)) != -1) {
                            fos.write(buffer, 0, bytesRead);
                        }
                    }
                }
            }

            // 清理临时目录
            FileUtil.del(tempDir);

            log.info("本地存储分片上传完成: objectKey={}, uploadId={}", objectKey, uploadId);
            return getFileUrl(objectKey, null);

        } catch (IOException e) {
            log.error("本地存储分片上传完成失败, objectKey={}: {}", objectKey, e.getMessage(), e);
            throw new StorageOperationException("本地存储分片上传完成失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void abortMultipartUpload(String objectKey, String uploadId) {
        ensureNotPrototype();

        try {
            // 清理临时目录
            String tempDir = basePath + ".temp/" + uploadId + "/";
            FileUtil.del(tempDir);
            log.info("本地存储分片上传已中止: objectKey={}, uploadId={}", objectKey, uploadId);

        } catch (Exception e) {
            log.error("中止本地存储分片上传失败, objectKey={}: {}", objectKey, e.getMessage(), e);
            throw new StorageOperationException("中止本地存储分片上传失败: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Map<String, Object>> listParts(String objectKey, String uploadId) {
        ensureNotPrototype();

        try {
            String tempDir = basePath + ".temp/" + uploadId + "/";
            File tempDirFile = new File(tempDir);

            if (!tempDirFile.exists()) {
                return new ArrayList<>();
            }

            List<Map<String, Object>> partList = new ArrayList<>();
            File[] partFiles = tempDirFile.listFiles((dir, name) -> name.startsWith("part_"));

            if (partFiles != null) {
                for (File partFile : partFiles) {
                    String fileName = partFile.getName();
                    int partNumber = Integer.parseInt(fileName.substring(5)); // 去掉"part_"前缀

                    Map<String, Object> partInfo = new HashMap<>();
                    partInfo.put("partNumber", partNumber);
                    partInfo.put("eTag", String.valueOf(partFile.length()) + "_" + partFile.lastModified());
                    partInfo.put("size", partFile.length());
                    partList.add(partInfo);
                }
            }

            return partList;

        } catch (Exception e) {
            log.error("查询本地存储已上传分片失败, objectKey={}: {}", objectKey, e.getMessage(), e);
            throw new StorageOperationException("查询本地存储已上传分片失败: " + e.getMessage(), e);
        }
    }
}
