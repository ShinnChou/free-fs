package com.xddcodec.fs.storage.provider;

import cn.hutool.core.util.IdUtil;
import com.xddcodec.fs.framework.common.exception.StorageOperationException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 抽象存储操作服务
 * 提供公共的方法实现和初始化状态管理
 */
public abstract class AbstractStorageOperationService implements StorageOperationService {

    /**
     * 初始化状态标识
     */
    protected volatile boolean initialized = false;

    @Override
    public String uploadFile(MultipartFile file, String objectKeyPrefix) {
        if (file == null || file.isEmpty()) {
            throw new StorageOperationException("上传的文件不能为空");
        }
        try {
            String originalFileName = file.getOriginalFilename();
            // 如果原始文件名是null (例如，通过API直接传递字节流而不是表单文件上传时可能发生)，给一个默认名
            if (originalFileName == null || originalFileName.trim().isEmpty()) {
                originalFileName = "unknown-" + IdUtil.fastSimpleUUID();
            }

            // 确保前缀以 '/' 结尾或为空，或者如果前缀不为空且不以'/'结尾则加上'/'
            String prefix = (objectKeyPrefix == null || objectKeyPrefix.isEmpty())
                    ? ""
                    : (objectKeyPrefix.endsWith("/") ? objectKeyPrefix : objectKeyPrefix + "/");

            // 构建 objectKey，可以加入UUID防止文件名冲突
            // 清理文件名中的潜在路径字符，避免安全问题
            String safeFileName = originalFileName.replace("../", "").replace("..\\", "");
            String objectKey = prefix + IdUtil.fastSimpleUUID() + "-" + safeFileName;

            // 调用的是接口中定义的、由具体子类实现的 InputStream 版本的 uploadFile
            return uploadFile(file.getInputStream(), objectKey, file.getContentType(), file.getSize());
        } catch (IOException e) {
            throw new StorageOperationException("上传文件失败: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * 检查服务是否已初始化，未初始化则抛出异常
     */
    protected void ensureInitialized() {
        if (!initialized) {
            throw new StorageOperationException("存储服务未初始化，平台: " + getPlatformIdentifier());
        }
    }
}