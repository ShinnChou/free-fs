package com.xddcodec.fs.file.service;

import com.xddcodec.fs.file.domain.FileInfo;
import com.xddcodec.fs.file.domain.dto.CreateDirectoryDTO;
import com.xddcodec.fs.file.domain.dto.RenameFileCmd;
import com.xddcodec.fs.file.domain.qry.FileQry;
import com.mybatisflex.core.service.IService;
import com.xddcodec.fs.file.domain.vo.FileRecycleVO;
import com.xddcodec.fs.file.domain.vo.FileVO;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;

/**
 * 文件资源服务接口
 *
 * @Author: xddcode
 * @Date: 2025/5/8 9:35
 */
public interface FileInfoService extends IService<FileInfo> {

    /**
     * 上传文件
     *
     * @param file     文件
     * @param parentId 父目录ID，如果为null则上传到根目录
     * @return 文件信息
     */
    FileInfo uploadFile(MultipartFile file, String parentId);

    /**
     * 上传文件
     *
     * @param inputStream               文件输入流
     * @param originalName              原始文件名
     * @param size                      文件大小
     * @param mimeType                  文件类型
     * @param userId                    用户ID
     * @param parentId                  父目录ID，如果为null则上传到根目录
     * @param storagePlatformIdentifier 存储平台标识符，如果为null则使用默认存储平台
     * @return 文件信息
     */
    FileInfo uploadFile(InputStream inputStream, String originalName, long size, String mimeType,
                        String userId, String parentId, String storagePlatformIdentifier);

    /**
     * 秒传检查
     *
     * @param md5                       文件MD5值
     * @param storagePlatformIdentifier 存储平台标识符
     * @param userId                    用户ID
     * @param originalName              原始文件名
     * @return
     */
    FileInfo checkSecondUpload(String md5, String storagePlatformIdentifier, String userId, String originalName);

    /**
     * 下载文件
     *
     * @param fileId 文件ID
     * @return 文件输入流
     */
    InputStream downloadFile(String fileId);

    /**
     * 获取文件访问URL
     *
     * @param fileId        文件ID
     * @param expireSeconds URL有效时间（秒），如果不支持或永久有效可为null或0
     * @return 文件访问URL
     */
    String getFileUrl(String fileId, Integer expireSeconds);

    /**
     * 删除文件
     *
     * @param fileIds 文件ID集合
     * @return 是否删除成功
     */
    void deleteFiles(List<String> fileIds);

    /**
     * 创建目录
     *
     * @param dto 创建目录请求参数
     * @return
     */
    void createDirectory(CreateDirectoryDTO dto);

    /**
     * 重命名文件
     *
     * @param fileId 文件ID
     * @param cmd    重命名请求参数
     */
    void renameFile(String fileId, RenameFileCmd cmd);

    /**
     * 获取目录层级
     *
     * @param dirId 目录ID
     * @return
     */
    List<FileVO> getDirectoryTreePath(String dirId);

    /**
     * 查询文件列表
     *
     * @param qry 查询参数（包含关键词、文件类型、分页参数等）
     * @return 分页结果
     */
    List<FileVO> getList(FileQry qry);

    /**
     * 根据文件ID列表查询文件信息
     *
     * @param fileIds
     * @return
     */
    List<FileVO> getByFileIds(List<String> fileIds);

    /**
     * 查询回收站文件列表
     *
     * @return
     */
    List<FileRecycleVO> getRecycles();

    /**
     * 恢复已删除的文件
     *
     * @param fileIds 文件ID集合
     * @return
     */
    void restoreFiles(List<String> fileIds);

    /**
     * 永久删除文件
     *
     * @param fileIds 文件ID集合
     * @return
     */
    void permanentlyDeleteFiles(List<String> fileIds);

    /**
     * 清空回收站
     */
    void clearRecycles();
}