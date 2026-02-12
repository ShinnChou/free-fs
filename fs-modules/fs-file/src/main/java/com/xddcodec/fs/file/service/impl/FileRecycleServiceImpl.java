package com.xddcodec.fs.file.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.update.UpdateChain;
import com.xddcodec.fs.file.domain.FileInfo;
import com.xddcodec.fs.file.domain.vo.FileRecycleVO;
import com.xddcodec.fs.file.service.FileInfoService;
import com.xddcodec.fs.file.service.FileRecycleService;
import com.xddcodec.fs.file.service.FileUserFavoritesService;
import com.xddcodec.fs.framework.common.exception.BusinessException;
import com.xddcodec.fs.storage.facade.StorageServiceFacade;
import com.xddcodec.fs.storage.plugin.core.IStorageOperationService;
import com.xddcodec.fs.storage.plugin.core.context.StoragePlatformContextHolder;
import io.github.linpeilie.Converter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.xddcodec.fs.file.domain.table.FileInfoTableDef.FILE_INFO;

/**
 * 回收站服务接口实现
 *
 * @Author: xddcode
 * @Date: 2025/5/8 9:35
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileRecycleServiceImpl implements FileRecycleService {

    private final Converter converter;

    private final FileInfoService fileInfoService;

    private final FileUserFavoritesService fileUserFavoritesService;

    private final StorageServiceFacade storageServiceFacade;

    @Override
    public List<FileRecycleVO> getRecycles(String keyword) {
        String userId = StpUtil.getLoginIdAsString();
        String storagePlatformSettingId = StoragePlatformContextHolder.getConfigId();

        // 先查询所有已删除的文件
        QueryWrapper wrapper = new QueryWrapper();
        wrapper.where(FILE_INFO.USER_ID.eq(userId))
                .and(FILE_INFO.IS_DELETED.eq(true))
                .and(FILE_INFO.STORAGE_PLATFORM_SETTING_ID.eq(storagePlatformSettingId));

        if (StrUtil.isNotBlank(keyword)) {
            keyword = "%" + keyword.trim() + "%";
            wrapper.and(
                    FILE_INFO.ORIGINAL_NAME.like(keyword)
                            .or(FILE_INFO.DISPLAY_NAME.like(keyword))
            );
        }

        wrapper.orderBy(FILE_INFO.DELETED_TIME.desc());

        List<FileInfo> allDeletedFiles = fileInfoService.list(wrapper);

        if (CollUtil.isEmpty(allDeletedFiles)) {
            return Collections.emptyList();
        }

        // 构建已删除文件的ID集合
        Set<String> deletedFileIds = allDeletedFiles.stream()
                .map(FileInfo::getId)
                .collect(Collectors.toSet());

        // 过滤出顶层删除项（父目录为空或父目录未被删除）
        List<FileInfo> topLevelDeletedFiles = allDeletedFiles.stream()
                .filter(file -> {
                    String parentId = file.getParentId();
                    // 父目录为空，或者父目录未在已删除列表中
                    return StrUtil.isBlank(parentId) || !deletedFileIds.contains(parentId);
                })
                .collect(Collectors.toList());

        return converter.convert(topLevelDeletedFiles, FileRecycleVO.class);
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void restoreFiles(List<String> fileIds) {
        if (CollUtil.isEmpty(fileIds)) {
            return;
        }

        String userId = StpUtil.getLoginIdAsString();

        Set<String> allFileIds = collectFileIdsRecursively(
                fileIds,
                userId,
                wrapper -> wrapper.and(FILE_INFO.IS_DELETED.eq(true)) // 只收集已删除的
        );

        if (CollUtil.isEmpty(allFileIds)) {
            throw new BusinessException("未找到要恢复的文件或文件夹");
        }

        // 批量恢复
        UpdateChain.of(FileInfo.class)
                .set(FileInfo::getIsDeleted, false)
                .set(FileInfo::getDeletedTime, null)
                .where(FILE_INFO.ID.in(allFileIds))
                .and(FILE_INFO.USER_ID.eq(userId))
                .update();

        log.info("用户 {} 恢复文件/文件夹，共 {} 项", userId, allFileIds.size());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void permanentlyDeleteFiles(List<String> fileIds) {
        if (CollUtil.isEmpty(fileIds)) {
            return;
        }
        String userId = StpUtil.getLoginIdAsString();
        Set<String> allFileIds = collectFileIdsRecursively(
                fileIds,
                userId,
                wrapper -> wrapper.and(FILE_INFO.IS_DELETED.eq(true)) // 只能删除回收站中的
        );
        if (CollUtil.isEmpty(allFileIds)) {
            throw new BusinessException("未找到要删除的文件或文件夹");
        }
        List<FileInfo> allFiles = fileInfoService.listByIds(allFileIds);
        // 找出需要删除物理文件的（没有其他引用的）
        List<FileInfo> physicalFilesToDelete = new ArrayList<>();
        for (FileInfo file : allFiles) {
            if (StrUtil.isBlank(file.getObjectKey())) {
                continue;
            }

            // 查询除了本次要删除的文件外，还有没有其他文件引用这个objectKey
            long count = fileInfoService.count(new QueryWrapper()
                    .where(FILE_INFO.OBJECT_KEY.eq(file.getObjectKey())
                            .and(FILE_INFO.ID.notIn(allFileIds))));

            if (count == 0) {
                physicalFilesToDelete.add(file);
            }
        }

        // 删除文件信息记录
        fileInfoService.removeByIds(allFileIds);

        // 删除用户收藏记录
        fileUserFavoritesService.removeByFileIds(allFileIds, userId);

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        for (FileInfo file : physicalFilesToDelete) {
                            try {
                                deletePhysicalFile(file);
                            } catch (Exception e) {
                                log.error("删除物理文件失败: {}", file.getObjectKey(), e);
                            }
                        }
                    }
                }
        );
    }

    /**
     * 删除物理文件
     *
     * @param file 文件信息
     */
    private void deletePhysicalFile(FileInfo file) {
        IStorageOperationService storageService = storageServiceFacade.getStorageService(file.getStoragePlatformSettingId());
        storageService.deleteFile(file.getObjectKey());
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void clearRecycles() {
        String userId = StpUtil.getLoginIdAsString();
        String storagePlatformSettingId = StoragePlatformContextHolder.getConfigId();

        // 查询所有已删除的文件
        List<FileInfo> allDeletedFiles = fileInfoService.list(new QueryWrapper()
                .where(FILE_INFO.USER_ID.eq(userId))
                .and(FILE_INFO.IS_DELETED.eq(true))
                .and(FILE_INFO.STORAGE_PLATFORM_SETTING_ID.eq(storagePlatformSettingId))
        );

        if (CollUtil.isEmpty(allDeletedFiles)) {
            return;
        }

        // 构建已删除文件的ID集合
        Set<String> deletedFileIds = allDeletedFiles.stream()
                .map(FileInfo::getId)
                .collect(Collectors.toSet());

        // 过滤出顶层删除项
        List<String> topLevelDeletedFileIds = allDeletedFiles.stream()
                .filter(file -> {
                    String parentId = file.getParentId();
                    return StrUtil.isBlank(parentId) || !deletedFileIds.contains(parentId);
                })
                .map(FileInfo::getId)
                .collect(Collectors.toList());

        // 递归删除顶层项（会自动处理子项）
        this.permanentlyDeleteFiles(topLevelDeletedFileIds);
    }


    /**
     * 递归收集文件ID（通用方法）
     *
     * @param fileIds 初始文件ID列表
     * @param userId  用户ID
     * @param filter  过滤条件（可选）
     * @return 收集到的所有文件ID集合
     */
    private Set<String> collectFileIdsRecursively(
            List<String> fileIds,
            String userId,
            Consumer<QueryWrapper> filter) {

        // 查询初始文件列表
        QueryWrapper initialWrapper = new QueryWrapper()
                .where(FILE_INFO.ID.in(fileIds))
                .and(FILE_INFO.USER_ID.eq(userId));

        // 应用额外过滤条件
        if (filter != null) {
            filter.accept(initialWrapper);
        }

        List<FileInfo> files = fileInfoService.list(initialWrapper);

        if (CollUtil.isEmpty(files)) {
            return Collections.emptySet();
        }

        // 递归收集
        Set<String> allFileIds = new HashSet<>();
        files.forEach(file -> {
            collectFileIdsRecursive(file, allFileIds, userId, filter);
        });

        return allFileIds;
    }

    /**
     * 递归收集单个文件及其子文件的ID
     *
     * @param file       文件信息
     * @param allFileIds 收集的文件ID集合
     * @param userId     用户ID
     * @param filter     过滤条件（可选）
     */
    private void collectFileIdsRecursive(
            FileInfo file,
            Set<String> allFileIds,
            String userId,
            Consumer<QueryWrapper> filter) {

        // 添加当前文件ID
        allFileIds.add(file.getId());

        // 如果是文件夹，递归处理子项
        if (file.getIsDir()) {
            log.debug("收集文件夹 {} 的子项", file.getDisplayName());

            // 构建查询条件
            QueryWrapper wrapper = new QueryWrapper()
                    .where(FILE_INFO.PARENT_ID.eq(file.getId()))
                    .and(FILE_INFO.USER_ID.eq(userId));

            // 应用额外过滤条件
            if (filter != null) {
                filter.accept(wrapper);
            }

            // 查询所有子文件
            List<FileInfo> children = fileInfoService.list(wrapper);

            // 递归收集子项ID
            children.forEach(child -> collectFileIdsRecursive(child, allFileIds, userId, filter));
        }
    }
}
