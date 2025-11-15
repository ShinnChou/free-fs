package com.xddcodec.fs.file.service;

import com.xddcodec.fs.file.domain.FileInfo;
import com.xddcodec.fs.file.domain.dto.CreateDirectoryCmd;
import com.xddcodec.fs.file.domain.dto.MoveFileCmd;
import com.xddcodec.fs.file.domain.dto.RenameFileCmd;
import com.xddcodec.fs.file.domain.qry.FileQry;
import com.mybatisflex.core.service.IService;
import com.xddcodec.fs.file.domain.vo.FileDetailVO;
import com.xddcodec.fs.file.domain.vo.FileRecycleVO;
import com.xddcodec.fs.file.domain.vo.FileVO;

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
     * @param cmd 创建目录请求参数
     * @return
     */
    void createDirectory(CreateDirectoryCmd cmd);

    /**
     * 生成唯一的文件名（处理重名冲突）
     * <p>
     * - 如果不存在重名：返回原名称
     * - 如果存在重名：自动添加 (1), (2), (3)... 后缀
     *
     * @param userId        用户ID
     * @param parentId      父目录ID
     * @param desiredName   期望的文件名
     * @param isDir         是否是文件夹
     * @param excludeFileId 排除的文件ID（可选，用于重命名场景）
     * @return 唯一的文件名
     */
    String generateUniqueName(String userId, String parentId,
                              String desiredName, Boolean isDir,
                              String excludeFileId);

    /**
     * 重命名文件
     *
     * @param fileId 文件ID
     * @param cmd    重命名请求参数
     */
    void renameFile(String fileId, RenameFileCmd cmd);

    /**
     * 移动文件到指定目录
     *
     * @param cmd 移动文件请求参数
     */
    void moveFile(MoveFileCmd cmd);

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
     * 查询文件详情
     *
     * @param fileId 文件ID
     * @return
     */
    FileDetailVO getFileDetails(String fileId);

    /**
     * 根据父目录ID查询目录列表
     *
     * @param parentId
     * @return
     */
    List<FileVO> getDirs(String parentId);

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
    List<FileRecycleVO> getRecycles(String keyword);

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