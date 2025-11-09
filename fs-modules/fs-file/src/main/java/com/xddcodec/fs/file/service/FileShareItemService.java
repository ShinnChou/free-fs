package com.xddcodec.fs.file.service;

import com.mybatisflex.core.service.IService;
import com.xddcodec.fs.file.domain.FileShareItem;

import java.util.List;

/**
 * 文件分享关联服务接口
 *
 * @Author: xddcode
 * @Date: 2025/10/30 9:35
 */
public interface FileShareItemService extends IService<FileShareItem> {

    /**
     * 创建分享文件关联
     *
     * @param shareId
     * @param fileIds
     */
    void saveShareItems(String shareId, List<String> fileIds);

    /**
     * 删除分享文件关联
     *
     * @param shareId
     */
    void removeByShareId(String shareId);
}
