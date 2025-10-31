package com.xddcodec.fs.storage.plugin.aliyunoss;

import com.aliyun.oss.ClientBuilderConfiguration;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.common.auth.DefaultCredentialProvider;
import com.aliyun.oss.common.comm.SignVersion;
import com.aliyun.oss.model.GeneratePresignedUrlRequest;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.PutObjectRequest;
import com.xddcodec.fs.framework.common.enums.StoragePlatformIdentifierEnum;
import com.xddcodec.fs.framework.common.exception.StorageConfigException;
import com.xddcodec.fs.framework.common.exception.StorageOperationException;
import com.xddcodec.fs.storage.plugin.aliyunoss.config.AliyunOssConfig;
import com.xddcodec.fs.storage.plugin.core.AbstractStorageOperationService;
import com.xddcodec.fs.storage.plugin.core.config.StorageConfig;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * 阿里云 OSS 存储插件实现
 * TODO: 待实现
 */
@Slf4j
public class AliyunOssStorageServiceImpl extends AbstractStorageOperationService {

    private OSS ossClient;

    private String bucketName;

    @SuppressWarnings("unused")
    public AliyunOssStorageServiceImpl() {
        super();
    }

    @SuppressWarnings("unused")
    public AliyunOssStorageServiceImpl(StorageConfig config) {
        super(config);
    }

    @Override
    public String getPlatformIdentifier() {
        return StoragePlatformIdentifierEnum.ALIYUN_OSS.getIdentifier();
    }

    @Override
    protected void validateConfig(StorageConfig config) {
        try {
            AliyunOssConfig aliyunOssConfig = config.toObject(AliyunOssConfig.class);

            if (aliyunOssConfig == null) {
                throw new StorageConfigException("存储平台配置错误：阿里云OSS配置转换失败，配置对象为空");
            }

            if (aliyunOssConfig.getEndpoint() == null || aliyunOssConfig.getEndpoint().trim().isEmpty()) {
                throw new StorageConfigException("存储平台配置错误：阿里云OSS endpoint 不能为空");
            }

            if (aliyunOssConfig.getAccessKey() == null || aliyunOssConfig.getAccessKey().trim().isEmpty()) {
                throw new StorageConfigException("存储平台配置错误：阿里云OSS accessKey 不能为空");
            }

            if (aliyunOssConfig.getSecretKey() == null || aliyunOssConfig.getSecretKey().trim().isEmpty()) {
                throw new StorageConfigException("存储平台配置错误：阿里云OSS secretKey 不能为空");
            }

            if (aliyunOssConfig.getBucket() == null || aliyunOssConfig.getBucket().trim().isEmpty()) {
                throw new StorageConfigException("存储平台配置错误：阿里云OSS bucket 不能为空");
            }
        } catch (Exception e) {
            log.error("阿里云OSS配置验证失败: {}", e.getMessage(), e);
            throw new StorageConfigException("当前存储平台配置错误：客户端校验失败");
        }
    }

    @Override
    protected void initialize(StorageConfig config) {
        try {
            AliyunOssConfig aliyunOssConfig = config.toObject(AliyunOssConfig.class);
            DefaultCredentialProvider provider = new DefaultCredentialProvider(aliyunOssConfig.getAccessKey(),
                    aliyunOssConfig.getSecretKey());
            ClientBuilderConfiguration clientBuilderConfiguration = new ClientBuilderConfiguration();
            clientBuilderConfiguration.setSignatureVersion(SignVersion.V4);
            this.ossClient = OSSClientBuilder.create()
                    .credentialsProvider(provider)
                    .clientConfiguration(clientBuilderConfiguration)
                    .region(aliyunOssConfig.getRegion())
                    .endpoint(aliyunOssConfig.getEndpoint())
                    .build();
            this.bucketName = aliyunOssConfig.getBucket();
            log.info("{} 阿里云OSS客户端初始化成功: endpoint={}, bucket={}",
                    getLogPrefix(), aliyunOssConfig.getEndpoint(), this.bucketName);
        } catch (StorageConfigException e) {
            // 重新抛出配置异常
            throw e;
        } catch (Exception e) {
            log.error("阿里云OSS初始化失败: {}", e.getMessage(), e);
            throw new StorageConfigException("当前存储平台配置错误：客户端初始化失败");
        }

    }

    @Override
    public String uploadFile(InputStream inputStream, String objectKey) {
        ensureNotPrototype();
        try {
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, objectKey, inputStream);
            ossClient.putObject(putObjectRequest);
            log.debug("{} 文件上传成功: objectKey={}", getLogPrefix(), objectKey);
            return getFileUrl(objectKey, null);
        } catch (OSSException e) {
            log.error("{} 文件上传失败: objectKey={}, errorCode={}, errorMessage={}",
                    getLogPrefix(), objectKey, e.getErrorCode(), e.getErrorMessage(), e);
            throw new StorageOperationException(
                    String.format("阿里云OSS文件上传失败 [%s]: %s", e.getErrorCode(), e.getErrorMessage()), e);
        } catch (Exception e) {
            log.error("{} 文件上传失败: objectKey={}", getLogPrefix(), objectKey, e);
            throw new StorageOperationException("阿里云OSS文件上传失败: " + e.getMessage(), e);
        }
    }

    @Override
    public InputStream downloadFile(String objectKey) {
        ensureNotPrototype();
        try {
            OSSObject ossObject = ossClient.getObject(bucketName, objectKey);
            if (ossObject == null) {
                throw new StorageOperationException("文件不存在: " + objectKey);
            }
            log.debug("{} 文件下载成功: objectKey={}", getLogPrefix(), objectKey);
            return ossObject.getObjectContent();
        } catch (OSSException e) {
            if ("NoSuchKey".equals(e.getErrorCode())) {
                log.warn("{} 文件不存在: objectKey={}", getLogPrefix(), objectKey);
                throw new StorageOperationException("文件不存在: " + objectKey, e);
            }
            log.error("{} 文件下载失败: objectKey={}, errorCode={}, errorMessage={}",
                    getLogPrefix(), objectKey, e.getErrorCode(), e.getErrorMessage(), e);
            throw new StorageOperationException(
                    String.format("阿里云OSS文件下载失败 [%s]: %s", e.getErrorCode(), e.getErrorMessage()), e);
        } catch (Exception e) {
            log.error("{} 文件下载失败: objectKey={}", getLogPrefix(), objectKey, e);
            throw new StorageOperationException("阿里云OSS文件下载失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteFile(String objectKey) {
        ensureNotPrototype();
        try {
            ossClient.deleteObject(bucketName, objectKey);
            log.debug("{} 文件删除成功: objectKey={}", getLogPrefix(), objectKey);
        } catch (OSSException e) {
            if ("NoSuchKey".equals(e.getErrorCode())) {
                log.debug("{} 文件不存在，视为删除成功: objectKey={}", getLogPrefix(), objectKey);
            }
            log.error("{} 文件删除失败: objectKey={}, errorCode={}, errorMessage={}",
                    getLogPrefix(), objectKey, e.getErrorCode(), e.getErrorMessage(), e);
            throw new StorageOperationException(
                    String.format("阿里云OSS文件删除失败 [%s]: %s", e.getErrorCode(), e.getErrorMessage()), e);
        } catch (Exception e) {
            log.error("{} 文件删除失败: objectKey={}", getLogPrefix(), objectKey, e);
            throw new StorageOperationException("阿里云OSS文件删除失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String getFileUrl(String objectKey, Integer expireSeconds) {
        ensureNotPrototype();
        try {
            java.util.Date expiration = expireSeconds != null
                    ? new java.util.Date(System.currentTimeMillis() + expireSeconds * 1000L)
                    : null;

            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucketName, objectKey);
            if (expiration != null) {
                request.setExpiration(expiration);
            }

            java.net.URL url = ossClient.generatePresignedUrl(request);
            return url.toString();
        } catch (OSSException e) {
            log.error("{} 生成文件URL失败: objectKey={}, errorCode={}, errorMessage={}",
                    getLogPrefix(), objectKey, e.getErrorCode(), e.getErrorMessage(), e);
            throw new StorageOperationException(
                    String.format("阿里云OSS生成文件URL失败 [%s]: %s", e.getErrorCode(), e.getErrorMessage()), e);
        } catch (Exception e) {
            log.error("{} 生成文件URL失败: objectKey={}", getLogPrefix(), objectKey, e);
            throw new StorageOperationException("阿里云OSS生成文件URL失败: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isFileExist(String objectKey) {
        ensureNotPrototype();
        throw new StorageOperationException("阿里云 OSS 存储插件尚未实现");
    }

    @Override
    public String initiateMultipartUpload(String objectKey, String mimeType, String fileIdentifier) {
        ensureNotPrototype();
        throw new StorageOperationException("阿里云 OSS 存储插件尚未实现");
    }

    @Override
    public String uploadPart(String objectKey, String uploadId, int partNumber, long partSize,
                             InputStream partInputStream, String partIdentifierForLocal) {
        ensureNotPrototype();
        throw new StorageOperationException("阿里云 OSS 存储插件尚未实现");
    }

    @Override
    public String completeMultipartUpload(String objectKey, String uploadId, List<Map<String, Object>> partETags) {
        ensureNotPrototype();
        throw new StorageOperationException("阿里云 OSS 存储插件尚未实现");
    }

    @Override
    public void abortMultipartUpload(String objectKey, String uploadId) {
        ensureNotPrototype();
        throw new StorageOperationException("阿里云 OSS 存储插件尚未实现");
    }

    @Override
    public List<Map<String, Object>> listParts(String objectKey, String uploadId) {
        ensureNotPrototype();
        throw new StorageOperationException("阿里云 OSS 存储插件尚未实现");
    }

    @Override
    public void close() {
        if (ossClient != null) {
            ossClient.shutdown();
        }
    }
}