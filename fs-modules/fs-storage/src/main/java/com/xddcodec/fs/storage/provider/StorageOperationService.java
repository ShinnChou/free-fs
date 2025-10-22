package com.xddcodec.fs.storage.provider;

import com.xddcodec.fs.framework.common.exception.StorageOperationException;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * 存储平台操作接口
 * 定义了存储平台的基本操作，如上传、下载、删除等
 *
 * @Author: xddcode
 * @Date: 2024/10/26 10:00
 */
public interface StorageOperationService {

    /**
     * 获取此存储服务的平台标识符。
     *
     * @return 平台标识符 (例如 "local", "minio")
     */
    String getPlatformIdentifier();

    /**
     * 上传文件，使用 MultipartFile。
     *
     * @param file            MultipartFile 对象
     * @param objectKeyPrefix 在存储中的对象键前缀 (例如 "user/123/images/")
     *                        最终的对象键可能是 prefix + [uuid-] + originalFilename
     * @return 文件的可访问URL或存储键 (objectKey)
     */
    String uploadFile(MultipartFile file, String objectKeyPrefix);

    /**
     * 上传文件核心方法，使用 InputStream。
     *
     * @param inputStream 文件输入流
     * @param objectKey   在存储中的完整对象键 (例如 "user/123/images/avatar.jpg")
     * @param contentType 文件MIME类型
     * @param size        文件大小 (字节)
     * @return 文件的可访问URL或存储键 (objectKey)
     */
    String uploadFile(InputStream inputStream, String objectKey, String contentType, long size);

    /**
     * 下载文件。
     *
     * @param objectKey 文件的对象键
     * @return 文件输入流
     */
    InputStream downloadFile(String objectKey);

    /**
     * 删除文件。
     *
     * @param objectKey 文件的对象键
     * @return 是否删除成功 (或者 void and throw exception on failure)
     */
    boolean deleteFile(String objectKey);

    /**
     * 获取文件访问URL。
     * 对于私有对象，这通常是预签名URL。
     *
     * @param objectKey     文件的对象键
     * @param expireSeconds URL有效时间（秒），如果不支持或永久有效可为null或0
     * @return 文件的可访问URL
     */
    String getFileUrl(String objectKey, Integer expireSeconds);

    /**
     * 检查文件是否存在。
     *
     * @param objectKey 文件的对象键
     * @return 是否存在
     */
    boolean isFileExist(String objectKey);

    /**
     * 使用从数据库加载的JSON配置字符串初始化服务。
     *
     * @param configData JSON格式的配置数据
     */
    void init(String configData);

    /**
     * 检查服务是否已初始化。
     *
     * @return 是否已初始化
     */
    boolean isInitialized();

    /**
     * 初始化分片上传任务。
     *
     * @param objectKey           最终文件的对象键
     * @param mimeType            文件的MIME类型
     * @param fileIdentifier      整个文件的唯一标识 (如MD5)
     * @return 分片上传任务ID (由存储平台提供，如S3的UploadId) 或内部生成的任务标识
     * @throws StorageOperationException 如果初始化失败
     */
    String initiateMultipartUpload(String objectKey, String mimeType, String fileIdentifier) throws StorageOperationException;

    /**
     * 上传一个分片。
     *
     * @param objectKey      最终文件的对象键
     * @param uploadId       分片上传任务ID (initiateMultipartUpload的返回值)
     * @param partNumber     分片序号 (从1开始)
     * @param partSize       当前分片大小 (字节)
     * @param partInputStream 当前分片的输入流
     * @param partIdentifierForLocal 可选，本地存储时分片的临时文件名或标识（如果不是由service内部生成）
     * @return 分片上传后的标识 (如S3的ETag)
     * @throws StorageOperationException 如果上传失败
     */
    String uploadPart(String objectKey, String uploadId, int partNumber, long partSize, InputStream partInputStream, String partIdentifierForLocal) throws StorageOperationException;

    /**
     * 完成分片上传，合并分片。
     *
     * @param objectKey  最终文件的对象键
     * @param uploadId   分片上传任务ID
     * @param partETags  一个列表，包含每个已上传分片的序号和其标识 (如ETag)
     * Map<Integer, String> partNumberToETagMap or List<PartInfo(number, etag)>
     * @return 合并后的文件访问URL或对象键
     * @throws StorageOperationException 如果合并失败
     */
    String completeMultipartUpload(String objectKey, String uploadId, List<Map<String, Object>> partETags) throws StorageOperationException; // Map可以包含 "partNumber" 和 "eTag"

    /**
     * 中止分片上传任务 (清理已上传的但未合并的分片)。
     *
     * @param objectKey 最终文件的对象键
     * @param uploadId  分片上传任务ID
     * @throws StorageOperationException 如果中止失败
     */
    void abortMultipartUpload(String objectKey, String uploadId) throws StorageOperationException;

    /**
     * 列出已上传的分片 (用于断点续传时查询哪些分片已完成)。
     *
     * @param objectKey 最终文件的对象键
     * @param uploadId  分片上传任务ID
     * @return 已上传分片的信息列表 (包含分片号和ETag/标识)
     * @throws StorageOperationException
     */
    List<Map<String, Object>> listParts(String objectKey, String uploadId) throws StorageOperationException;
}