//package com.xddcodec.fs.storage.provider.impl;
//
//import cn.hutool.core.util.StrUtil;
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.xddcodec.fs.framework.common.enums.StoragePlatformIdentifierEnum;
//import com.xddcodec.fs.framework.common.exception.StorageOperationException;
//import com.xddcodec.fs.storage.provider.AbstractStorageOperationService;
//import io.minio.*;
//import io.minio.http.Method;
//import io.minio.messages.Part;
//import lombok.extern.slf4j.Slf4j;
//
//import java.io.InputStream;
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.concurrent.TimeUnit;
//
///**
// * MinIO 存储实现
// *
// * @Author: xddcode
// * @Date: 2024/10/26 15:00
// */
//@Slf4j
//public class MinioStorageServiceImpl extends AbstractStorageOperationService {
//
//    private MinioClient minioClient;
//    private String bucketName;
//    private String endpoint;
//    private final ObjectMapper objectMapper = new ObjectMapper();
//
//    @Override
//    public String getPlatformIdentifier() {
//        return StoragePlatformIdentifierEnum.MINIO.getIdentifier();
//    }
//
//    @Override
//    public void init(String configData) {
//        if (StrUtil.isBlank(configData)) {
//            throw new StorageOperationException("MinIO存储配置数据不能为空");
//        }
//
//        try {
//            JsonNode config = objectMapper.readTree(configData);
//            this.endpoint = config.path("endpoint").asText(null);
//            String accessKey = config.path("accessKey").asText(null);
//            String secretKey = config.path("secretKey").asText(null);
//            this.bucketName = config.path("bucket").asText(null);
//
//            if (StrUtil.isBlank(endpoint) || StrUtil.isBlank(accessKey)
//                    || StrUtil.isBlank(secretKey) || StrUtil.isBlank(bucketName)) {
//                throw new StorageOperationException("MinIO存储配置不完整");
//            }
//
//            // 初始化 MinIO 客户端
//            this.minioClient = MinioClient.builder()
//                    .endpoint(endpoint)
//                    .credentials(accessKey, secretKey)
//                    .build();
//
//            // 检查bucket是否存在，不存在则创建
//            boolean bucketExists = minioClient.bucketExists(
//                    BucketExistsArgs.builder().bucket(bucketName).build()
//            );
//            if (!bucketExists) {
//                minioClient.makeBucket(
//                        MakeBucketArgs.builder().bucket(bucketName).build()
//                );
//                log.info("MinIO存储桶不存在，已自动创建: {}", bucketName);
//            }
//
//            this.initialized = true;
//            log.info("MinIO存储服务初始化完成. Endpoint: {}, Bucket: {}", endpoint, bucketName);
//        } catch (Exception e) {
//            log.error("初始化MinIO存储失败: {}", e.getMessage(), e);
//            throw new StorageOperationException("初始化MinIO存储失败: " + e.getMessage(), e);
//        }
//    }
//
//    @Override
//    public String uploadFile(InputStream inputStream, String objectKey, String contentType, long size) {
//        ensureInitialized();
//        try {
//            minioClient.putObject(
//                    PutObjectArgs.builder()
//                            .bucket(bucketName)
//                            .object(objectKey)
//                            .stream(inputStream, size, -1)
//                            .contentType(contentType)
//                            .build()
//            );
//            log.debug("文件上传成功: bucket={}, objectKey={}", bucketName, objectKey);
//            return getFileUrl(objectKey, null);
//        } catch (Exception e) {
//            log.error("上传文件到MinIO失败, objectKey={}: {}", objectKey, e.getMessage(), e);
//            throw new StorageOperationException("上传文件到MinIO失败: " + e.getMessage(), e);
//        }
//    }
//
//    @Override
//    public InputStream downloadFile(String objectKey) {
//        ensureInitialized();
//        try {
//            return minioClient.getObject(
//                    GetObjectArgs.builder()
//                            .bucket(bucketName)
//                            .object(objectKey)
//                            .build()
//            );
//        } catch (Exception e) {
//            log.error("从MinIO下载文件失败, objectKey={}: {}", objectKey, e.getMessage(), e);
//            throw new StorageOperationException("从MinIO下载文件失败: " + e.getMessage(), e);
//        }
//    }
//
//    @Override
//    public boolean deleteFile(String objectKey) {
//        ensureInitialized();
//        try {
//            minioClient.removeObject(
//                    RemoveObjectArgs.builder()
//                            .bucket(bucketName)
//                            .object(objectKey)
//                            .build()
//            );
//            log.debug("文件删除成功: bucket={}, objectKey={}", bucketName, objectKey);
//            return true;
//        } catch (Exception e) {
//            log.error("从MinIO删除文件失败, objectKey={}: {}", objectKey, e.getMessage(), e);
//            throw new StorageOperationException("从MinIO删除文件失败: " + e.getMessage(), e);
//        }
//    }
//
//    @Override
//    public String getFileUrl(String objectKey, Integer expireSeconds) {
//        ensureInitialized();
//        try {
//            if (expireSeconds == null || expireSeconds <= 0) {
//                expireSeconds = 7 * 24 * 3600; // 默认7天
//            }
//            return minioClient.getPresignedObjectUrl(
//                    GetPresignedObjectUrlArgs.builder()
//                            .method(Method.GET)
//                            .bucket(bucketName)
//                            .object(objectKey)
//                            .expiry(expireSeconds, TimeUnit.SECONDS)
//                            .build()
//            );
//        } catch (Exception e) {
//            log.error("获取MinIO文件URL失败, objectKey={}: {}", objectKey, e.getMessage(), e);
//            throw new StorageOperationException("获取MinIO文件URL失败: " + e.getMessage(), e);
//        }
//    }
//
//    @Override
//    public boolean isFileExist(String objectKey) {
//        ensureInitialized();
//        try {
//            minioClient.statObject(
//                    StatObjectArgs.builder()
//                            .bucket(bucketName)
//                            .object(objectKey)
//                            .build()
//            );
//            return true;
//        } catch (Exception e) {
//            return false;
//        }
//    }
//
//    @Override
//    public String initiateMultipartUpload(String objectKey, String mimeType, String fileIdentifier) {
//        ensureInitialized();
//        try {
//            // MinIO 的分片上传通过 createMultipartUpload 初始化
//            // 注意：这需要 MinIO Java SDK 8.5.0+ 版本
//            String uploadId = minioClient.createMultipartUpload(
//                    bucketName,
//                    null,
//                    objectKey,
//                    new HashMap<>(),
//                    null
//            );
//            log.info("MinIO分片上传初始化成功: objectKey={}, uploadId={}", objectKey, uploadId);
//            return uploadId;
//        } catch (Exception e) {
//            log.error("初始化MinIO分片上传失败, objectKey={}: {}", objectKey, e.getMessage(), e);
//            throw new StorageOperationException("初始化MinIO分片上传失败: " + e.getMessage(), e);
//        }
//    }
//
//    @Override
//    public String uploadPart(String objectKey, String uploadId, int partNumber,
//                            long partSize, InputStream partInputStream, String partIdentifierForLocal) {
//        ensureInitialized();
//        try {
//            String etag = minioClient.uploadPart(
//                    bucketName,
//                    null,
//                    objectKey,
//                    partInputStream,
//                    partSize,
//                    uploadId,
//                    partNumber,
//                    new HashMap<>()
//            );
//            log.debug("MinIO分片上传成功: objectKey={}, partNumber={}, etag={}", objectKey, partNumber, etag);
//            return etag;
//        } catch (Exception e) {
//            log.error("上传MinIO分片失败, objectKey={}, partNumber={}: {}", objectKey, partNumber, e.getMessage(), e);
//            throw new StorageOperationException("上传MinIO分片失败: " + e.getMessage(), e);
//        }
//    }
//
//    @Override
//    public String completeMultipartUpload(String objectKey, String uploadId, List<Map<String, Object>> partETags) {
//        ensureInitialized();
//        try {
//            // 构建Part数组
//            Part[] parts = new Part[partETags.size()];
//            for (int i = 0; i < partETags.size(); i++) {
//                Map<String, Object> partInfo = partETags.get(i);
//                int partNumber = (int) partInfo.get("partNumber");
//                String etag = (String) partInfo.get("eTag");
//                parts[i] = new Part(partNumber, etag);
//            }
//
//            minioClient.completeMultipartUpload(
//                    bucketName,
//                    null,
//                    objectKey,
//                    uploadId,
//                    parts,
//                    new HashMap<>(),
//                    new HashMap<>()
//            );
//            log.info("MinIO分片上传完成: objectKey={}, uploadId={}", objectKey, uploadId);
//            return getFileUrl(objectKey, null);
//        } catch (Exception e) {
//            log.error("完成MinIO分片上传失败, objectKey={}: {}", objectKey, e.getMessage(), e);
//            throw new StorageOperationException("完成MinIO分片上传失败: " + e.getMessage(), e);
//        }
//    }
//
//    @Override
//    public void abortMultipartUpload(String objectKey, String uploadId) {
//        ensureInitialized();
//        try {
//            minioClient.abortMultipartUpload(
//                    bucketName,
//                    null,
//                    objectKey,
//                    uploadId,
//                    new HashMap<>(),
//                    new HashMap<>()
//            );
//            log.info("MinIO分片上传已中止: objectKey={}, uploadId={}", objectKey, uploadId);
//        } catch (Exception e) {
//            log.error("中止MinIO分片上传失败, objectKey={}: {}", objectKey, e.getMessage(), e);
//            throw new StorageOperationException("中止MinIO分片上传失败: " + e.getMessage(), e);
//        }
//    }
//
//    @Override
//    public List<Map<String, Object>> listParts(String objectKey, String uploadId) {
//        ensureInitialized();
//        try {
//            ListPartsResponse response = minioClient.listParts(
//                    bucketName,
//                    null,
//                    objectKey,
//                    null,
//                    null,
//                    uploadId,
//                    new HashMap<>(),
//                    new HashMap<>()
//            );
//
//            List<Map<String, Object>> partList = new ArrayList<>();
//            for (Part part : response.result().partList()) {
//                Map<String, Object> partInfo = new HashMap<>();
//                partInfo.put("partNumber", part.partNumber());
//                partInfo.put("eTag", part.etag());
//                partInfo.put("size", part.partSize());
//                partList.add(partInfo);
//            }
//            return partList;
//        } catch (Exception e) {
//            log.error("查询MinIO已上传分片失败, objectKey={}: {}", objectKey, e.getMessage(), e);
//            throw new StorageOperationException("查询MinIO已上传分片失败: " + e.getMessage(), e);
//        }
//    }
//}
//
