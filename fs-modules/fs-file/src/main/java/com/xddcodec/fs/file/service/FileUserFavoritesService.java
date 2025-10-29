package com.xddcodec.fs.file.service;

import com.xddcodec.fs.file.domain.FileUserFavorites;
import com.mybatisflex.core.service.IService;

import java.util.List;

/**
 * 用户收藏文件服务接口
 *
 * @Author: hao.ding@insentek.com
 * @Date: 2025/5/12 13:49
 */
public interface FileUserFavoritesService extends IService<FileUserFavorites> {

    /**
     * 收藏文件
     *
     * @param fileIds 文件ID集合
     * @return 是否收藏成功
     */
    void favoritesFile(List<String> fileIds);

    /**
     * 取消收藏文件
     *
     * @param fileIds 文件ID集合
     * @return 是否取消收藏成功
     */
    void unFavoritesFile(List<String> fileIds);
}
