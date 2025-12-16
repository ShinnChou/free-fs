package com.xddcodec.fs.file.service;

import com.xddcodec.fs.file.domain.vo.FileRecycleVO;

import java.util.List;

/**
 * 文件回收站服务接口
 *
 * @Author: xddcode
 * @Date: 2025/5/8 9:35
 */
public interface FileRecycleService {

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
