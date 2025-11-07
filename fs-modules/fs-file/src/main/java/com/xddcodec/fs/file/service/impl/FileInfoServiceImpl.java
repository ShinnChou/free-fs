package com.xddcodec.fs.file.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.mybatisflex.core.update.UpdateChain;
import com.mybatisflex.core.update.UpdateWrapper;
import com.mybatisflex.core.util.UpdateEntity;
import com.xddcodec.fs.file.domain.FileInfo;
import com.xddcodec.fs.file.domain.dto.CreateDirectoryCmd;
import com.xddcodec.fs.file.domain.dto.MoveFileCmd;
import com.xddcodec.fs.file.domain.dto.RenameFileCmd;
import com.xddcodec.fs.file.domain.qry.FileQry;
import com.xddcodec.fs.file.domain.vo.FileRecycleVO;
import com.xddcodec.fs.file.domain.vo.FileVO;
import com.xddcodec.fs.file.enums.FileTypeEnum;
import com.xddcodec.fs.file.mapper.FileInfoMapper;
import com.xddcodec.fs.file.service.FileInfoService;
import com.xddcodec.fs.framework.common.exception.BusinessException;
import com.xddcodec.fs.framework.common.exception.StorageOperationException;
import com.xddcodec.fs.framework.common.utils.StringUtils;
import com.xddcodec.fs.storage.plugin.core.IStorageOperationService;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.xddcodec.fs.storage.plugin.core.context.StoragePlatformContextHolder;
import com.xddcodec.fs.storage.facade.StorageServiceFacade;
import io.github.linpeilie.Converter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.xddcodec.fs.file.domain.table.FileInfoTableDef.FILE_INFO;
import static com.xddcodec.fs.file.domain.table.FileUserFavoritesTableDef.FILE_USER_FAVORITES;


/**
 * 文件资源服务实现类
 *
 * @Author: xddcode
 * @Date: 2025/5/8 9:40
 */
@Slf4j
@Service
public class FileInfoServiceImpl extends ServiceImpl<FileInfoMapper, FileInfo> implements FileInfoService {

    @Autowired
    private Converter converter;

    @Autowired
    private StorageServiceFacade storageServiceFacade;

    @Value("${spring.application.name:free-fs}")
    private String applicationName;

//    @Override
//    @Transactional(rollbackFor = Exception.class)
//    public FileInfo uploadFile(MultipartFile file, String parentId) {
//        if (file == null || file.isEmpty()) {
//            throw new StorageOperationException("上传文件不能为空");
//        }
//        String userId = StpUtil.getLoginIdAsString();
//        String configId = StoragePlatformContextHolder.getConfigId();
//        try {
//            return uploadFile(
//                    file.getInputStream(),
//                    file.getOriginalFilename(),
//                    file.getSize(),
//                    file.getContentType(),
//                    userId,
//                    parentId,
//                    configId
//            );
//        } catch (IOException e) {
//            log.error("读取上传文件流失败: {}", e.getMessage(), e);
//            throw new StorageOperationException("读取上传文件流失败: " + e.getMessage(), e);
//        }
//    }
//
//    @Override
//    @Transactional(rollbackFor = Exception.class)
//    public FileInfo uploadFile(InputStream inputStream, String originalName, long size, String mimeType,
//                               String userId, String parentId, String storagePlatformSettingId) {
//        if (inputStream == null) {
//            throw new StorageOperationException("上传文件流不能为空");
//        }
//        if (StrUtil.isBlank(originalName)) {
//            throw new StorageOperationException("原始文件名不能为空");
//        }
//
//        byte[] fileBytes = null;
//        String md5 = null;
//        try {
//            fileBytes = IoUtil.readBytes(inputStream);
//
//            // 计算文件 MD5
//            md5 = DigestUtil.md5Hex(fileBytes);
//        } catch (Exception e) {
//            log.error("读取文件流失败: {}", e.getMessage(), e);
//            throw new StorageOperationException("读取文件流失败: " + e.getMessage(), e);
//        } finally {
//            IoUtil.close(inputStream);
//        }
//
//        // 秒传检查
//        FileInfo existingFile = checkSecondUpload(md5, storagePlatformSettingId, userId, originalName);
//        if (existingFile != null) {
//            log.info("秒传成功，文件ID: {}, MD5: {}", existingFile.getId(), md5);
//            return existingFile;
//        }
//
//        // 获取存储服务（使用文件记录中的 storagePlatformSettingId）
//        IStorageOperationService storageService = storageServiceFacade.getStorageService(storagePlatformSettingId);
//
//        // 生成文件ID和对象键
//        String fileId = IdUtil.fastSimpleUUID();
//        String suffix = FileUtil.extName(originalName);
//
//        //TODO 后续保存的文件名需要根据用户的配置是否生成新的
//        String displayName = IdUtil.fastSimpleUUID() + "." + suffix;
//        String objectKey = generateObjectKey(userId, displayName, suffix);
//
//        try {
//            ByteArrayInputStream uploadStream = new ByteArrayInputStream(fileBytes);
//            storageService.uploadFile(uploadStream, objectKey);
//        } catch (StorageOperationException e) {
//            // 统一转换为友好的业务异常消息
//            log.error("文件上传到存储平台失败: {}", e.getMessage(), e);
//            throw new StorageOperationException("文件上传失败，请检查当前存储平台配置后重试");
//        }
//
//        // 创建文件信息记录
//        FileInfo fileInfo = new FileInfo();
//        fileInfo.setId(fileId);
//        fileInfo.setObjectKey(objectKey);
//        fileInfo.setOriginalName(originalName);
//        // 默认显示名与原始名相同
//        fileInfo.setDisplayName(displayName);
//        fileInfo.setSuffix(suffix);
//        fileInfo.setSize(size);
//        fileInfo.setMimeType(mimeType);
//        fileInfo.setIsDir(false);
//        fileInfo.setParentId(parentId);
//        fileInfo.setUserId(userId);
//        fileInfo.setContentMd5(md5);
//        fileInfo.setStoragePlatformSettingId(storagePlatformSettingId);
//        fileInfo.setUploadTime(LocalDateTime.now());
//        fileInfo.setUpdateTime(LocalDateTime.now());
//        fileInfo.setIsDeleted(false);
//
//        // 保存文件信息到数据库
//        save(fileInfo);
//        return fileInfo;
//    }

//    @Override
//    public FileInfo checkSecondUpload(String md5, String storagePlatformSettingId, String userId, String originalName) {
//        if (StrUtil.isBlank(md5) || StrUtil.isBlank(storagePlatformSettingId)) {
//            return null;
//        }
//        return getOne(
//                new QueryWrapper()
//                        .where(FILE_INFO.CONTENT_MD5.eq(md5)
//                                .and(FILE_INFO.STORAGE_PLATFORM_SETTING_ID.eq(storagePlatformSettingId))
//                                .and(FILE_INFO.USER_ID.eq(userId))
//                                .and(FILE_INFO.ORIGINAL_NAME.eq(originalName))
//                                .and(FILE_INFO.IS_DELETED.eq(false))
//                        )
//        );
//    }

    @Override
    public InputStream downloadFile(String fileId) {
        FileInfo fileInfo = getById(fileId);
        if (fileInfo == null) {
            throw new StorageOperationException("文件不存在: " + fileId);
        }
        if (fileInfo.getIsDir()) {
            throw new StorageOperationException("不能下载目录: " + fileId);
        }
        if (fileInfo.getIsDeleted()) {
            throw new StorageOperationException("文件已被删除: " + fileId);
        }

        // 根据文件记录中的 storagePlatformSettingId 获取对应的存储服务
        try {
            IStorageOperationService storageService = storageServiceFacade.getStorageService(fileInfo.getStoragePlatformSettingId());
            return storageService.downloadFile(fileInfo.getObjectKey());
        } catch (StorageOperationException e) {
            // 统一转换为友好的业务异常消息
            log.error("从存储平台下载文件失败: {}", e.getMessage(), e);
            String friendlyMessage = e.getMessage().toLowerCase().contains("文件不存在") ||
                    e.getMessage().toLowerCase().contains("nosuchkey")
                    ? "文件不存在或已被删除"
                    : "文件下载失败，请重试";
            throw new StorageOperationException(friendlyMessage);
        }
    }

    @Override
    public String getFileUrl(String fileId, Integer expireSeconds) {
        FileInfo fileInfo = getById(fileId);
        if (fileInfo == null) {
            throw new StorageOperationException("文件不存在: " + fileId);
        }
        if (fileInfo.getIsDir()) {
            throw new StorageOperationException("目录没有访问URL: " + fileId);
        }
        if (fileInfo.getIsDeleted()) {
            throw new StorageOperationException("文件已被删除: " + fileId);
        }

        // 根据文件记录中的 storagePlatformSettingId 获取对应的存储服务
        try {
            IStorageOperationService storageService = storageServiceFacade.getStorageService(fileInfo.getStoragePlatformSettingId());
            return storageService.getFileUrl(fileInfo.getObjectKey(), expireSeconds);
        } catch (StorageOperationException e) {
            // 统一转换为友好的业务异常消息
            log.error("获取文件URL失败: {}", e.getMessage(), e);
            throw new StorageOperationException("获取文件访问地址失败，请重试");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFiles(List<String> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            return;
        }
        List<FileInfo> fileInfoList = listByIds(fileIds);
        if (fileInfoList.isEmpty()) {
            return;
        }
        List<FileInfo> toDeleteList = fileInfoList.stream()
                .filter(fileInfo -> !fileInfo.getIsDeleted())
                .collect(Collectors.toList());

        if (toDeleteList.isEmpty()) {
            return;
        }
        toDeleteList.forEach(fileInfo -> {
            fileInfo.setIsDeleted(true);
            fileInfo.setDeletedTime(LocalDateTime.now());
        });

        this.updateBatch(toDeleteList);
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createDirectory(CreateDirectoryCmd cmd) {
        // 生成目录ID
        String folderId = IdUtil.fastSimpleUUID();
        String userId = StpUtil.getLoginIdAsString();
        String platformConfigId = StoragePlatformContextHolder.getConfigId();
        String baseName = cmd.getFolderName().trim();
        String finalName = generateUniqueName(
                userId,
                cmd.getParentId(),
                baseName,
                true,
                null
        );
        // 创建目录信息记录
        FileInfo dirInfo = new FileInfo();
        dirInfo.setId(folderId);
        dirInfo.setOriginalName(finalName);
        dirInfo.setDisplayName(finalName);
        dirInfo.setIsDir(true);
        dirInfo.setParentId(cmd.getParentId());
        dirInfo.setUserId(userId);
        dirInfo.setStoragePlatformSettingId(platformConfigId);
        dirInfo.setUploadTime(LocalDateTime.now());
        dirInfo.setUpdateTime(LocalDateTime.now());
        dirInfo.setIsDeleted(false);
        save(dirInfo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void renameFile(String fileId, RenameFileCmd cmd) {
        FileInfo fileInfo = getById(fileId);
        if (fileInfo == null) {
            throw new StorageOperationException("文件不存在: " + fileId);
        }
        if (fileInfo.getDisplayName().equals(cmd.getDisplayName())) {
            return;
        }
        String newName = cmd.getDisplayName().trim();
        //判断同目录下是否有重名
        String finalName = generateUniqueName(
                fileInfo.getUserId(),
                fileInfo.getParentId(),
                newName,
                fileInfo.getIsDir(),
                fileId
        );
        fileInfo.setDisplayName(finalName);
        fileInfo.setUpdateTime(LocalDateTime.now());
        updateById(fileInfo);
    }

    @Override
    public void moveFile(MoveFileCmd cmd) {
        if (CollUtil.isEmpty(cmd.getFileIds())) {
            throw new BusinessException("文件ID列表不能为空");
        }

        // 处理空字符串，统一转为null表示根目录
        String targetDirId = StringUtils.isBlank(cmd.getDirId()) ? null : cmd.getDirId();

        // 如果dirId不为空，校验目标目录是否存在且为目录类型
        if (targetDirId != null) {
            FileInfo dirInfo = getById(targetDirId);
            if (dirInfo == null) {
                throw new BusinessException("目标目录不存在");
            }
            if (!dirInfo.getIsDir()) {
                throw new BusinessException("目标必须是目录");
            }
        }

        // 批量移动文件
        for (String fileId : cmd.getFileIds()) {
            FileInfo fileInfo = getById(fileId);
            if (fileInfo == null) {
                continue;
            }
            // 防止将目录移动到自己或自己的子目录下
            if (targetDirId != null && fileInfo.getIsDir()) {
                if (fileId.equals(targetDirId) || isSubDirectory(fileId, targetDirId)) {
                    throw new BusinessException("不能将目录移动到自身或子目录下");
                }
            }
            // 设置新的父目录ID（null表示根目录）
            fileInfo.setParentId(targetDirId);

            FileInfo updateEntity = UpdateEntity.of(FileInfo.class, fileInfo.getId());
            updateEntity.setParentId(targetDirId);
            updateById(updateEntity);
        }

    }

    // 检查targetId是否是sourceId的子目录
    private boolean isSubDirectory(String sourceId, String targetId) {
        FileInfo current = getById(targetId);
        while (current != null && current.getParentId() != null) {
            if (current.getParentId().equals(sourceId)) {
                return true;
            }
            current = getById(current.getParentId());
        }
        return false;
    }

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
    private String generateUniqueName(String userId, String parentId,
                                      String desiredName, Boolean isDir,
                                      String excludeFileId) {

        String nameWithoutExt = desiredName;
        String extension = "";
        if (!isDir && desiredName.contains(".")) {
            int lastDotIndex = desiredName.lastIndexOf(".");
            nameWithoutExt = desiredName.substring(0, lastDotIndex);
            extension = desiredName.substring(lastDotIndex); // 包含点号
        }
        QueryWrapper query = buildSameLevelQuery(
                userId,
                parentId,
                nameWithoutExt,
                isDir,
                excludeFileId
        );
        List<FileInfo> existingFiles = list(query);
        if (existingFiles.isEmpty()) {
            return desiredName;
        }
        Set<Integer> usedSuffixes = extractUsedSuffixes(existingFiles, nameWithoutExt, isDir);
        int suffixNum = 0;
        String finalName;
        do {
            suffixNum++;
            finalName = buildNameWithSuffix(nameWithoutExt, suffixNum, extension, isDir);
        } while (usedSuffixes.contains(suffixNum));
        log.info("检测到重名，自动重命名：{} -> {}", desiredName, finalName);
        return finalName;
    }

    /**
     * 构建查询同级目录下同类型文件的条件 ✨
     */
    private QueryWrapper buildSameLevelQuery(String userId, String parentId,
                                             String baseName, Boolean isDir,
                                             String excludeFileId) {
        QueryWrapper query = new QueryWrapper();

        query.where(FILE_INFO.USER_ID.eq(userId))
                .and(FILE_INFO.PARENT_ID.eq(parentId))
                .and(FILE_INFO.IS_DIR.eq(isDir))
                .and(FILE_INFO.IS_DELETED.eq(false))
                .and(FILE_INFO.DISPLAY_NAME.like(baseName + "%"));

        // 如果是重命名场景，排除当前文件
        if (StrUtil.isNotBlank(excludeFileId)) {
            query.and(FILE_INFO.ID.ne(excludeFileId));
        }

        return query;
    }

    /**
     * 提取已使用的后缀数字
     * <p>
     * 示例：
     * - photo.jpg       -> 0
     * - photo(1).jpg    -> 1
     * - photo(2).jpg    -> 2
     * - photo(abc).jpg  -> -1 (忽略)
     */
    private Set<Integer> extractUsedSuffixes(List<FileInfo> existingFiles,
                                             String nameWithoutExt,
                                             Boolean isDir) {
        return existingFiles.stream()
                .map(f -> {
                    String displayName = f.getDisplayName();

                    // 移除扩展名（如果是文件）
                    if (!isDir && displayName.contains(".")) {
                        int lastDotIndex = displayName.lastIndexOf(".");
                        displayName = displayName.substring(0, lastDotIndex);
                    }

                    // 检查是否完全匹配基础名称（表示原始文件，后缀为 0）
                    if (displayName.equals(nameWithoutExt)) {
                        return 0;
                    }

                    // 匹配 (n) 格式的后缀
                    String pattern = "^" + Pattern.quote(nameWithoutExt) + "\\((\\d+)\\)$";
                    Matcher matcher = Pattern.compile(pattern).matcher(displayName);

                    if (matcher.find()) {
                        return Integer.parseInt(matcher.group(1));
                    }

                    return -1; // 不匹配的名称（忽略）
                })
                .filter(n -> n >= 0)  // 只保留有效的后缀数字
                .collect(Collectors.toSet());
    }

    /**
     * 构建带后缀的文件名
     *
     * @param nameWithoutExt 不含扩展名的文件名
     * @param suffixNum      后缀数字
     * @param extension      扩展名（含点号）
     * @param isDir          是否是文件夹
     * @return 完整的文件名
     */
    private String buildNameWithSuffix(String nameWithoutExt, int suffixNum,
                                       String extension, Boolean isDir) {
        if (isDir) {
            // 文件夹：baseName(1)
            return nameWithoutExt + "(" + suffixNum + ")";
        } else {
            // 文件：baseName(1).ext
            return nameWithoutExt + "(" + suffixNum + ")" + extension;
        }
    }

    @Override
    public List<FileVO> getDirectoryTreePath(String dirId) {
        FileInfo fileInfo = getById(dirId);
        if (fileInfo == null) {
            return List.of();
        }

        List<FileVO> pathList = new ArrayList<>();
        FileInfo current = fileInfo;

        // 递归向上查找，直到根节点（parent_id 为 null）
        while (current != null) {
            FileVO fileVO = converter.convert(current, FileVO.class);
            pathList.add(0, fileVO);

            // 查找父节点
            if (current.getParentId() != null) {
                current = getById(current.getParentId());
            } else {
                break;
            }
        }

        return pathList;
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

        // 查询所有文件信息（用于删除物理文件）
        List<FileInfo> allFiles = listByIds(allFileIds);

        // 批量删除物理文件
        allFiles.stream()
                .filter(file -> !file.getIsDir())
                .forEach(this::deletePhysicalFile);

        // 批量删除数据库记录
        removeByIds(allFileIds);

        log.info("用户 {} 永久删除文件/文件夹，共 {} 项", userId, allFileIds.size());
    }

    @Override
    public void clearRecycles() {
        String userId = StpUtil.getLoginIdAsString();
        String storagePlatformSettingId = StoragePlatformContextHolder.getConfigId();
        remove(new QueryWrapper()
                .where(FILE_INFO.USER_ID.eq(userId)
                        .and(FILE_INFO.IS_DELETED.eq(true)
                                .and(FILE_INFO.STORAGE_PLATFORM_SETTING_ID.eq(storagePlatformSettingId))
                        ))
        );
    }

    /**
     * 删除物理文件
     *
     * @param file 文件信息
     */
    private void deletePhysicalFile(FileInfo file) {
        //TODO 后续通过用户配置，是否同步删除物理文件操作

    }

    @Override
    public List<FileVO> getList(FileQry qry) {
        String userId = StpUtil.getLoginIdAsString();
        String storagePlatformSettingId = StoragePlatformContextHolder.getConfigId();

        // 构建查询条件
        QueryWrapper wrapper = new QueryWrapper();

        wrapper.select(
                        "fi.*",
                        "CASE WHEN fuf.file_id IS NOT NULL THEN 1 ELSE 0 END AS is_favorite"
                )
                .from(FILE_INFO.as("fi"))
                .leftJoin(FILE_USER_FAVORITES.as("fuf"))
                .on(FILE_INFO.ID.eq(FILE_USER_FAVORITES.FILE_ID)
                        .and(FILE_USER_FAVORITES.USER_ID.eq(userId)))
                .where(FILE_INFO.USER_ID.eq(userId))
                .and(FILE_INFO.IS_DELETED.eq(false))
                .and(FILE_INFO.STORAGE_PLATFORM_SETTING_ID.eq(storagePlatformSettingId));

        // 收藏过滤
        if (Boolean.TRUE.equals(qry.getIsFavorite()) && qry.getParentId() == null) {
            wrapper.and("fuf.file_id IS NOT NULL");
        }

        // 父目录过滤
        if (qry.getParentId() == null) {
            wrapper.and(FILE_INFO.PARENT_ID.isNull());
        } else {
            wrapper.and(FILE_INFO.PARENT_ID.eq(qry.getParentId()));
        }
        // 关键词搜索
        if (StrUtil.isNotBlank(qry.getKeyword())) {
            String keyword = "%" + qry.getKeyword().trim() + "%";
            wrapper.and(
                    FILE_INFO.ORIGINAL_NAME.like(keyword)
                            .or(FILE_INFO.DISPLAY_NAME.like(keyword))
            );
        }

        // 文件类型过滤
        applyFileTypeFilter(wrapper, qry);
        String orderBy = StrUtil.toUnderlineCase(qry.getOrderBy());
        boolean isAsc = "ASC".equalsIgnoreCase(qry.getOrderDirection());
        wrapper.orderBy(FILE_INFO.IS_DIR.desc())
                .orderBy(FILE_INFO.UPDATE_TIME.desc())
                .orderBy(orderBy, isAsc);

        return this.listAs(wrapper, FileVO.class);
    }

    @Override
    public List<FileVO> getDirs(String parentId) {
        String userId = StpUtil.getLoginIdAsString();
        String storagePlatformSettingId = StoragePlatformContextHolder.getConfigId();
        QueryWrapper wrapper = new QueryWrapper();
        wrapper.where(FILE_INFO.USER_ID.eq(userId)
                .and(FILE_INFO.STORAGE_PLATFORM_SETTING_ID.eq(storagePlatformSettingId))
                .and(FILE_INFO.IS_DELETED.eq(false))
                .and(FILE_INFO.IS_DIR.eq(true))
        );

        if (StrUtil.isNotBlank(parentId)) {
            wrapper.and(FILE_INFO.PARENT_ID.eq(parentId));
        }
        wrapper.orderBy(FILE_INFO.UPDATE_TIME.desc());
        return this.listAs(wrapper, FileVO.class);
    }

    @Override
    public List<FileVO> getByFileIds(List<String> fileIds) {
        if (CollUtil.isEmpty(fileIds)) {
            return List.of();
        }
        List<FileInfo> fileInfos = this.list(new QueryWrapper().where(FILE_INFO.ID.in(fileIds)));
        return converter.convert(fileInfos, FileVO.class);
    }

    /**
     * 应用文件类型过滤
     */
    private void applyFileTypeFilter(QueryWrapper wrapper, FileQry qry) {
        if (qry.getFileType() == null || qry.getFileType().trim().isEmpty()) {
            return;
        }
        FileTypeEnum fileType = FileTypeEnum.fromType(qry.getFileType());

        if (fileType == null) {
            log.warn("未识别的文件类型: {}", qry.getFileType());
            return;
        }
        // 其他类型：排除所有已知后缀
        if (fileType.isOther()) {
            List<String> knownSuffixes = FileTypeEnum.getAllKnownSuffixes();
            wrapper.and(FILE_INFO.IS_DIR.eq(false))
                    .and(
                            FILE_INFO.SUFFIX.notIn(knownSuffixes)
                                    .or(FILE_INFO.SUFFIX.isNull().or(FILE_INFO.SUFFIX.eq("")))
                    );

            return;
        }
        // 常规类型：直接匹配后缀
        List<String> suffixes = fileType.getSuffixes();
        if (suffixes != null && !suffixes.isEmpty()) {
            wrapper.eq(FileInfo::getIsDir, false)
                    .in(FileInfo::getSuffix, suffixes);
        }
    }

    @Override
    public List<FileRecycleVO> getRecycles(String keyword) {
        String userId = StpUtil.getLoginIdAsString();
        String storagePlatformSettingId = StoragePlatformContextHolder.getConfigId();
        QueryWrapper wrapper = new QueryWrapper();
        wrapper.where(FILE_INFO.USER_ID.eq(userId));
        wrapper.and(FILE_INFO.IS_DELETED.eq(true));
        wrapper.and(FILE_INFO.STORAGE_PLATFORM_SETTING_ID.eq(storagePlatformSettingId));
        if (StrUtil.isNotBlank(keyword)) {
            keyword = "%" + keyword.trim() + "%";
            wrapper.and(
                    FILE_INFO.ORIGINAL_NAME.like(keyword)
                            .or(FILE_INFO.DISPLAY_NAME.like(keyword))
            );
        }
        List<FileInfo> fileInfos = this.list(wrapper);
        return converter.convert(fileInfos, FileRecycleVO.class);
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

        List<FileInfo> files = list(initialWrapper);

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
            List<FileInfo> children = list(wrapper);

            // 递归收集子项ID
            children.forEach(child -> collectFileIdsRecursive(child, allFileIds, userId, filter));
        }
    }
}
