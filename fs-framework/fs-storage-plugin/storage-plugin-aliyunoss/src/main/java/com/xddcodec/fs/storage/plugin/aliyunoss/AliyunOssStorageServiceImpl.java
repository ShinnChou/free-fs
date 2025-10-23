//package com.xddcodec.fs.storage.plugin.aliyunoss;
//
//import cn.hutool.core.util.StrUtil;
//import com.aliyun.oss.OSS;
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.xddcodec.fs.framework.common.enums.StoragePlatformIdentifierEnum;
//import com.xddcodec.fs.framework.common.exception.StorageOperationException;
//import com.xddcodec.fs.storage.plugin.core.AbstractStorageOperationService;
//import lombok.extern.slf4j.Slf4j;
//
//import java.io.InputStream;
//import java.util.List;
//import java.util.Map;
//
///**
// * 阿里云 OSS 存储实现
// * <p>
// * 注意：需要添加阿里云OSS SDK依赖
// * <dependency>
// * <groupId>com.aliyun.oss</groupId>
// * <artifactId>aliyun-sdk-oss</artifactId>
// * <version>3.17.1</version>
// * </dependency>
// *
// * @Author: xddcode
// * @Date: 2024/10/26 15:30
// */
//@Slf4j
//public class AliyunOssStorageServiceImpl extends AbstractStorageOperationService {
//
//    private OSS ossClient;
//    private String bucketName;
//    private String endpoint;
//    private final ObjectMapper objectMapper = new ObjectMapper();
//
//    @Override
//    public String getPlatformIdentifier() {
//        return StoragePlatformIdentifierEnum.ALIYUN_OSS.getIdentifier();
//    }
//
//    @Override
//    public void init(String configData) {
//        if (StrUtil.isBlank(configData)) {
//            throw new StorageOperationException("阿里云OSS存储配置数据不能为空");
//        }
//
//        try {
//            JsonNode config = objectMapper.readTree(configData);
//            this.endpoint = config.path("endpoint").asText(null);
//            String accessKeyId = config.path("accessKey").asText(null);
//            String accessKeySecret = config.path("secretKey").asText(null);
//            this.bucketName = config.path("bucket").asText(null);
//
//            if (StrUtil.isBlank(endpoint) || StrUtil.isBlank(accessKeyId)
//                    || StrUtil.isBlank(accessKeySecret) || StrUtil.isBlank(bucketName)) {
//                throw new StorageOperationException("阿里云OSS存储配置不完整");
//            }
//
//            // 初始化 OSS 客户端
//            this.ossClient = new com.aliyun.oss.OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
//
//            // 检查bucket是否存在
//            if (!ossClient.doesBucketExist(bucketName)) {
//                throw new StorageOperationException("阿里云OSS存储桶不存在: " + bucketName);
//            }
//
//            this.initialized = true;
//            log.info("阿里云OSS存储服务初始化完成. Endpoint: {}, Bucket: {}", endpoint, bucketName);
//        } catch (Exception e) {
//            log.error("初始化阿里云OSS存储失败: {}", e.getMessage(), e);
//            throw new StorageOperationException("初始化阿里云OSS存储失败: " + e.getMessage(), e);
//        }
//    }
//
//    @Override
//    public String uploadFile(InputStream inputStream, String objectKey, String contentType, long size) {
//        ensureInitialized();
//        try {
//            com.aliyun.oss.model.ObjectMetadata metadata = new com.aliyun.oss.model.ObjectMetadata();
//            metadata.setContentLength(size);
//            metadata.setContentType(contentType);
//
//            com.aliyun.oss.model.PutObjectResult result = ossClient.putObject(bucketName, objectKey, inputStream, metadata);
//            log.debug("文件上传成功: bucket={}, objectKey={}", bucketName, objectKey);
//
//            return getFileUrl(objectKey, null);
//        } catch (Exception e) {
//            log.error("上传文件到阿里云OSS失败, objectKey={}: {}", objectKey, e.getMessage(), e);
//            throw new StorageOperationException("上传文件到阿里云OSS失败: " + e.getMessage(), e);
//        }
//    }
//
//    @Override
//    public InputStream downloadFile(String objectKey) {
//        ensureInitialized();
//        try {
//            com.aliyun.oss.model.OSSObject ossObject = ossClient.getObject(bucketName, objectKey);
//            return ossObject.getObjectContent();
//        } catch (Exception e) {
//            log.error("从阿里云OSS下载文件失败, objectKey={}: {}", objectKey, e.getMessage(), e);
//            throw new StorageOperationException("从阿里云OSS下载文件失败: " + e.getMessage(), e);
//        }
//    }
//
//    @Override
//    public boolean deleteFile(String objectKey) {
//        ensureInitialized();
//        try {
//            ossClient.deleteObject(bucketName, objectKey);
//            log.debug("文件删除成功: bucket={}, objectKey={}", bucketName, objectKey);
//            return true;
//        } catch (Exception e) {
//            log.error("从阿里云OSS删除文件失败, objectKey={}: {}", objectKey, e.getMessage(), e);
//            throw new StorageOperationException("从阿里云OSS删除文件失败: " + e.getMessage(), e);
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
//
//            java.util.Date expiration = new java.util.Date(System.currentTimeMillis() + expireSeconds * 1000L);
//            java.net.URL url = ossClient.generatePresignedUrl(bucketName, objectKey, expiration);
//            return url.toString();
//        } catch (Exception e) {
//            log.error("获取阿里云OSS文件URL失败, objectKey={}: {}", objectKey, e.getMessage(), e);
//            throw new StorageOperationException("获取阿里云OSS文件URL失败: " + e.getMessage(), e);
//        }
//    }
//
//    @Override
//    public boolean isFileExist(String objectKey) {
//        ensureInitialized();
//        try {
//            return ossClient.doesObjectExist(bucketName, objectKey);
//        } catch (Exception e) {
//            return false;
//        }
//    }
//
//    @Override
//    public String initiateMultipartUpload(String objectKey, String mimeType, String fileIdentifier) {
//        ensureInitialized();
//        try {
//            com.aliyun.oss.model.InitiateMultipartUploadRequest request = new com.aliyun.oss.model.InitiateMultipartUploadRequest(bucketName, objectKey);
//
//            com.aliyun.oss.model.ObjectMetadata metadata = new com.aliyun.oss.model.ObjectMetadata();
//            metadata.setContentType(mimeType);
//            request.setObjectMetadata(metadata);
//
//            com.aliyun.oss.model.InitiateMultipartUploadResult result = ossClient.initiateMultipartUpload(request);
//            String uploadId = result.getUploadId();
//            log.info("阿里云OSS分片上传初始化成功: objectKey={}, uploadId={}", objectKey, uploadId);
//            return uploadId;
//        } catch (Exception e) {
//            log.error("初始化阿里云OSS分片上传失败, objectKey={}: {}", objectKey, e.getMessage(), e);
//            throw new StorageOperationException("初始化阿里云OSS分片上传失败: " + e.getMessage(), e);
//        }
//    }
//
//    @Override
//    public String uploadPart(String objectKey, String uploadId, int partNumber,
//                             long partSize, InputStream partInputStream, String partIdentifierForLocal) {
//        ensureInitialized();
//        try {
//            com.aliyun.oss.model.UploadPartRequest uploadPartRequest = new com.aliyun.oss.model.UploadPartRequest();
//            uploadPartRequest.setBucketName(bucketName);
//            uploadPartRequest.setKey(objectKey);
//            uploadPartRequest.setUploadId(uploadId);
//            uploadPartRequest.setInputStream(partInputStream);
//            uploadPartRequest.setPartSize(partSize);
//            uploadPartRequest.setPartNumber(partNumber);
//
//            com.aliyun.oss.model.UploadPartResult uploadPartResult = ossClient.uploadPart(uploadPartRequest);
//            String etag = uploadPartResult.getETag();
//            log.debug("阿里云OSS分片上传成功: objectKey={}, partNumber={}, etag={}", objectKey, partNumber, etag);
//            return etag;
//        } catch (Exception e) {
//            log.error("上传阿里云OSS分片失败, objectKey={}, partNumber={}: {}", objectKey, partNumber, e.getMessage(), e);
//            throw new StorageOperationException("上传阿里云OSS分片失败: " + e.getMessage(), e);
//        }
//    }
//
//    @Override
//    public String completeMultipartUpload(String objectKey, String uploadId, List<Map<String, Object>> partETags) {
//        ensureInitialized();
//        try {
//            java.util.List<com.aliyun.oss.model.PartETag> partETagList = new java.util.ArrayList<>();
//            for (Map<String, Object> partInfo : partETags) {
//                int partNumber = (int) partInfo.get("partNumber");
//                String etag = (String) partInfo.get("eTag");
//                partETagList.add(new com.aliyun.oss.model.PartETag(partNumber, etag));
//            }
//
//            com.aliyun.oss.model.CompleteMultipartUploadRequest completeRequest =
//                new com.aliyun.oss.model.CompleteMultipartUploadRequest(bucketName, objectKey, uploadId, partETagList);
//            ossClient.completeMultipartUpload(completeRequest);
//
//            log.info("阿里云OSS分片上传完成: objectKey={}, uploadId={}", objectKey, uploadId);
//            return getFileUrl(objectKey, null);
//        } catch (Exception e) {
//            log.error("完成阿里云OSS分片上传失败, objectKey={}: {}", objectKey, e.getMessage(), e);
//            throw new StorageOperationException("完成阿里云OSS分片上传失败: " + e.getMessage(), e);
//        }
//    }
//
//    @Override
//    public void abortMultipartUpload(String objectKey, String uploadId) {
//        ensureInitialized();
//        try {
//            com.aliyun.oss.model.AbortMultipartUploadRequest abortRequest =
//                new com.aliyun.oss.model.AbortMultipartUploadRequest(bucketName, objectKey, uploadId);
//            ossClient.abortMultipartUpload(abortRequest);
//            log.info("阿里云OSS分片上传已中止: objectKey={}, uploadId={}", objectKey, uploadId);
//        } catch (Exception e) {
//            log.error("中止阿里云OSS分片上传失败, objectKey={}: {}", objectKey, e.getMessage(), e);
//            throw new StorageOperationException("中止阿里云OSS分片上传失败: " + e.getMessage(), e);
//        }
//    }
//
//    @Override
//    public List<Map<String, Object>> listParts(String objectKey, String uploadId) {
//        ensureInitialized();
//        try {
//            com.aliyun.oss.model.ListPartsRequest listPartsRequest = new com.aliyun.oss.model.ListPartsRequest(bucketName, objectKey, uploadId);
//            com.aliyun.oss.model.PartListing partListing = ossClient.listParts(listPartsRequest);
//
//            List<Map<String, Object>> partList = new java.util.ArrayList<>();
//            for (com.aliyun.oss.model.PartSummary part : partListing.getParts()) {
//                Map<String, Object> partInfo = new java.util.HashMap<>();
//                partInfo.put("partNumber", part.getPartNumber());
//                partInfo.put("eTag", part.getETag());
//                partInfo.put("size", part.getSize());
//                partList.add(partInfo);
//            }
//            return partList;
//        } catch (Exception e) {
//            log.error("查询阿里云OSS已上传分片失败, objectKey={}: {}", objectKey, e.getMessage(), e);
//            throw new StorageOperationException("查询阿里云OSS已上传分片失败: " + e.getMessage(), e);
//        }
//    }
//}
//
