package com.xddcodec.fs.file.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.xddcodec.fs.file.domain.FileInfo;
import com.xddcodec.fs.file.domain.FileUserFavorites;
import com.xddcodec.fs.file.mapper.FileUserFavoritesMapper;
import com.xddcodec.fs.file.service.FileUserFavoritesService;
import com.xddcodec.fs.framework.common.exception.BusinessException;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.xddcodec.fs.file.domain.table.FileInfoTableDef.FILE_INFO;
import static com.xddcodec.fs.file.domain.table.FileUserFavoritesTableDef.FILE_USER_FAVORITES;

/**
 * 用户收藏文件服务实现类
 *
 * @Author: hao.ding@insentek.com
 * @Date: 2025/5/12 13:50
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileUserFavoritesServiceImpl extends ServiceImpl<FileUserFavoritesMapper, FileUserFavorites> implements FileUserFavoritesService {

    private final FileInfoServiceImpl fileInfoService;

    @Override
    public List<FileInfo> getFavoritesFileList() {
        return List.of();
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void favoritesFile(List<String> fileIds) {
        // 校验输入
        if (CollUtil.isEmpty(fileIds)) {
            log.warn("收藏文件列表为空");
            throw new BusinessException("文件ID列表不能为空");
        }

        // 移除无效的 fileId
        List<String> validFileIds = fileIds.stream()
                .filter(StrUtil::isNotBlank)
                .distinct()
                .collect(Collectors.toList());
        if (validFileIds.isEmpty()) {
            log.warn("收藏文件列表无有效ID");
            throw new BusinessException("无有效的文件ID");
        }

        String userId = StpUtil.getLoginIdAsString();

        // 查询文件是否存在
        List<FileInfo> fileInfos = fileInfoService.list(
                new QueryWrapper()
                        .where(FILE_INFO.ID.in(validFileIds))
                        .and(FILE_INFO.USER_ID.eq(userId))
                        .and(FILE_INFO.IS_DELETED.eq(false))
        );

        if (CollUtil.isEmpty(fileInfos)) {
            log.warn("用户 {} 无可收藏的文件，fileIds: {}", userId, validFileIds);
            throw new BusinessException("无可收藏的文件");
        }

        // 验证查询结果与输入一致
        Set<String> foundFileIds = fileInfos.stream()
                .map(FileInfo::getId)
                .collect(Collectors.toSet());
        List<String> notFoundFileIds = validFileIds.stream()
                .filter(fileId -> !foundFileIds.contains(fileId))
                .collect(Collectors.toList());
        if (!notFoundFileIds.isEmpty()) {
            log.warn("部分文件不存在或无权限，userId: {}, fileIds: {}", userId, notFoundFileIds);
        }

        // 查询已收藏的文件
        List<FileUserFavorites> existingFavorites = list(
                new QueryWrapper()
                        .where(FILE_USER_FAVORITES.FILE_ID.in(foundFileIds))
                        .and(FILE_USER_FAVORITES.USER_ID.eq(userId))
        );
        Set<String> existingFileIds = existingFavorites.stream()
                .map(FileUserFavorites::getFileId)
                .collect(Collectors.toSet());

        // 过滤掉已收藏的文件
        List<FileUserFavorites> favoritesToAdd = fileInfos.stream()
                .filter(fileInfo -> !existingFileIds.contains(fileInfo.getId()))
                .map(fileInfo -> {
                    FileUserFavorites favoritesFile = new FileUserFavorites();
                    favoritesFile.setFileId(fileInfo.getId());
                    favoritesFile.setUserId(userId);
                    return favoritesFile;
                })
                .collect(Collectors.toList());

        if (favoritesToAdd.isEmpty()) {
            log.info("用户 {} 的文件已全部收藏，fileIds: {}", userId, validFileIds);
            return;
        }
        this.saveBatch(favoritesToAdd);
    }

    @Override
    public void unFavoritesFile(List<String> fileIds) {
        String userId = StpUtil.getLoginIdAsString();
        this.remove(
                new QueryWrapper()
                        .where(FILE_USER_FAVORITES.FILE_ID.in(fileIds))
                        .and(FILE_USER_FAVORITES.USER_ID.eq(userId))
        );
    }
}
