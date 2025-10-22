package com.xddcodec.fs.storage.provider.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.xddcodec.fs.framework.common.enums.StoragePlatformIdentifierEnum;
import com.xddcodec.fs.framework.common.exception.StorageOperationException;
import com.xddcodec.fs.storage.provider.AbstractStorageOperationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;

/**
 * 默认本地存储实现
 *
 * @Author: xddcode
 * @Date: 2024/10/26 10:10
 */
@Slf4j
@Service
public class DefaultStorageServiceImpl extends AbstractStorageOperationService {

    private String basePath;
    private String baseUrl;

    @Override
    public String getPlatformIdentifier() {
        return StoragePlatformIdentifierEnum.LOCAL.getIdentifier();
    }

    @Override
    public void init(String configData) {
        if (StrUtil.isBlank(configData)) {
            throw new StorageOperationException("本地存储配置数据不能为空");
        }

        JSONObject config = JSONUtil.parseObj(configData);
        this.basePath = config.getStr("basePath");
        this.baseUrl = config.getStr("baseUrl");

        if (StrUtil.isBlank(basePath) || StrUtil.isBlank(baseUrl)) {
            log.error("本地存储配置不完整: basePath={}, baseUrl={}", basePath, baseUrl);
            throw new StorageOperationException("本地存储配置数据不完整: basePath 和 baseUrl 不能为空");
        }

        // 标准化路径：统一使用系统分隔符
        this.basePath = new File(basePath).getAbsolutePath();

        // 确保基础路径以分隔符结尾
        if (!this.basePath.endsWith(File.separator)) {
            this.basePath += File.separator;
        }

        // 确保URL以 '/' 结尾
        if (!this.baseUrl.endsWith("/")) {
            this.baseUrl += "/";
        }

        // 创建基础目录
        File baseDir = new File(this.basePath);
        if (!baseDir.exists()) {
            if (!baseDir.mkdirs()) {
                log.error("无法创建本地存储基础目录: {}", this.basePath);
                throw new StorageOperationException("无法创建本地存储基础目录: " + this.basePath);
            }
            log.info("已创建本地存储基础目录: {}", this.basePath);
        }

        // 标记为已初始化
        this.initialized = true;
        log.info("本地存储服务初始化完成. BasePath: {}, BaseUrl: {}", this.basePath, this.baseUrl);
    }

    @Override
    public String uploadFile(InputStream inputStream, String objectKey, String contentType, long size) {
        ensureInitialized();

        if (StrUtil.isBlank(objectKey)) {
            throw new StorageOperationException("ObjectKey 不能为空");
        }

        if (inputStream == null) {
            throw new StorageOperationException("文件输入流不能为空");
        }

        try {
            // 获取物理路径
            Path targetPath = getPhysicalPath(objectKey);

            // 确保父目录存在
            File parentDir = targetPath.getParent().toFile();
            if (!parentDir.exists()) {
                if (!parentDir.mkdirs()) {
                    throw new StorageOperationException("无法创建目录: " + parentDir.getAbsolutePath());
                }
                log.debug("已创建目录: {}", parentDir.getAbsolutePath());
            }

            // 保存文件
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("文件上传成功: objectKey={}, size={} bytes, path={}", objectKey, size, targetPath);

            // 返回可访问的URL（统一使用 '/' 分隔符）
            return baseUrl + objectKey.replace(File.separatorChar, '/');
        } catch (IOException e) {
            log.error("上传文件到本地存储失败, objectKey={}: {}", objectKey, e.getMessage(), e);
            throw new StorageOperationException("上传文件到本地存储失败: " + e.getMessage(), e);
        } finally {
            IoUtil.close(inputStream);
        }
    }

    @Override
    public InputStream downloadFile(String objectKey) {
        ensureInitialized();

        if (StrUtil.isBlank(objectKey)) {
            throw new StorageOperationException("ObjectKey 不能为空");
        }

        try {
            Path physicalPath = getPhysicalPath(objectKey);

            // 检查文件是否存在
            if (!Files.exists(physicalPath)) {
                throw new StorageOperationException("文件不存在: " + objectKey);
            }

            // 检查是否为目录
            if (Files.isDirectory(physicalPath)) {
                throw new StorageOperationException("不能下载目录: " + objectKey);
            }

            log.debug("下载文件: objectKey={}, path={}", objectKey, physicalPath);
            return Files.newInputStream(physicalPath);
        } catch (IOException e) {
            log.error("从本地存储下载文件失败, objectKey={}: {}", objectKey, e.getMessage(), e);
            throw new StorageOperationException("从本地存储下载文件失败: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean deleteFile(String objectKey) {
        ensureInitialized();

        if (StrUtil.isBlank(objectKey)) {
            throw new StorageOperationException("ObjectKey 不能为空");
        }

        try {
            Path physicalPath = getPhysicalPath(objectKey);

            // 检查文件是否存在
            if (!Files.exists(physicalPath)) {
                log.warn("文件不存在，无需删除: objectKey={}", objectKey);
                return true;
            }

            // 删除文件
            boolean deleted = FileUtil.del(physicalPath);
            if (deleted) {
                log.info("文件删除成功: objectKey={}, path={}", objectKey, physicalPath);
            } else {
                log.error("文件删除失败: objectKey={}, path={}", objectKey, physicalPath);
            }

            return deleted;
        } catch (Exception e) {
            log.error("从本地存储删除文件失败, objectKey={}: {}", objectKey, e.getMessage(), e);
            throw new StorageOperationException("从本地存储删除文件失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String getFileUrl(String objectKey, Integer expireSeconds) {
        ensureInitialized();

        if (StrUtil.isBlank(objectKey)) {
            return null;
        }

        // 本地存储直接返回拼接的URL，不涉及预签名和过期时间
        // expireSeconds 参数对本地存储无效
        return baseUrl + objectKey.replace(File.separatorChar, '/');
    }

    @Override
    public boolean isFileExist(String objectKey) {
        ensureInitialized();

        if (StrUtil.isBlank(objectKey)) {
            return false;
        }

        try {
            Path physicalPath = getPhysicalPath(objectKey);
            return Files.exists(physicalPath) && !Files.isDirectory(physicalPath);
        } catch (Exception e) {
            log.error("检查文件是否存在失败, objectKey={}: {}", objectKey, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 分片上传相关方法（暂不实现）
     */
    @Override
    public String initiateMultipartUpload(String objectKey, String mimeType, String fileIdentifier)
            throws StorageOperationException {
        throw new StorageOperationException("本地存储暂不支持分片上传，请使用普通上传");
    }

    @Override
    public String uploadPart(String objectKey, String uploadId, int partNumber,
                             long partSize, InputStream partInputStream, String partIdentifierForLocal)
            throws StorageOperationException {
        throw new StorageOperationException("本地存储暂不支持分片上传，请使用普通上传");
    }

    @Override
    public String completeMultipartUpload(String objectKey, String uploadId, List<Map<String, Object>> partETags)
            throws StorageOperationException {
        throw new StorageOperationException("本地存储暂不支持分片上传，请使用普通上传");
    }

    @Override
    public void abortMultipartUpload(String objectKey, String uploadId)
            throws StorageOperationException {
        throw new StorageOperationException("本地存储暂不支持分片上传，请使用普通上传");
    }

    @Override
    public List<Map<String, Object>> listParts(String objectKey, String uploadId)
            throws StorageOperationException {
        throw new StorageOperationException("本地存储暂不支持分片上传，请使用普通上传");
    }

    /**
     * 获取物理路径
     *
     * <p>
     * 安全检查：
     * <ul>
     * <li>防止路径遍历攻击（../ 等）</li>
     * <li>确保文件在basePath范围内</li>
     * </ul>
     *
     * @param objectKey 对象键，格式如: user/userId/fileId.suffix
     * @return 物理路径
     * @throws StorageOperationException 如果路径非法
     */
    private Path getPhysicalPath(String objectKey) {
        if (StrUtil.isBlank(objectKey)) {
            throw new StorageOperationException("ObjectKey 不能为空");
        }

        try {
            // 标准化路径，防止路径遍历漏洞
            Path normalizedPath = Paths.get(objectKey).normalize();
            String normalizedStr = normalizedPath.toString();

            // 安全检查：不允许包含 ".."
            if (normalizedStr.contains("..")) {
                throw new StorageOperationException("非法的 ObjectKey（包含路径遍历）: " + objectKey);
            }

            // 安全检查：不允许绝对路径
            if (normalizedPath.isAbsolute()) {
                throw new StorageOperationException("非法的 ObjectKey（不允许绝对路径）: " + objectKey);
            }

            // 构建完整物理路径
            Path physicalPath = Paths.get(basePath, normalizedStr);

            // 安全检查：确保文件在 basePath 范围内
            if (!physicalPath.normalize().startsWith(Paths.get(basePath).normalize())) {
                throw new StorageOperationException("非法的 ObjectKey（超出存储范围）: " + objectKey);
            }

            return physicalPath;
        } catch (Exception e) {
            if (e instanceof StorageOperationException) {
                throw e;
            }
            log.error("解析物理路径失败, objectKey={}: {}", objectKey, e.getMessage(), e);
            throw new StorageOperationException("解析物理路径失败: " + e.getMessage(), e);
        }
    }
}
