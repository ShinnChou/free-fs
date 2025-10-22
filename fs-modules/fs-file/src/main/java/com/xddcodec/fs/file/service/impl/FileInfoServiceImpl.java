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
import com.xddcodec.fs.file.domain.FileInfo;
import com.xddcodec.fs.file.domain.dto.CreateDirectoryDTO;
import com.xddcodec.fs.file.domain.qry.FileQry;
import com.xddcodec.fs.file.domain.vo.FileRecycleVO;
import com.xddcodec.fs.file.domain.vo.FileVO;
import com.xddcodec.fs.file.enums.FileTypeEnum;
import com.xddcodec.fs.file.mapper.FileInfoMapper;
import com.xddcodec.fs.file.service.FileInfoService;
import com.xddcodec.fs.framework.common.context.StoragePlatformContextHolder;
import com.xddcodec.fs.framework.common.exception.BusinessException;
import com.xddcodec.fs.framework.common.exception.StorageOperationException;
import com.xddcodec.fs.storage.provider.StorageOperationService;
import com.xddcodec.fs.storage.provider.StorageServiceFacade;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.xddcodec.fs.file.domain.table.FileInfoTableDef.FILE_INFO;


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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileInfo uploadFile(MultipartFile file, String parentId) {
        if (file == null || file.isEmpty()) {
            throw new StorageOperationException("上传文件不能为空");
        }
        String userId = StpUtil.getLoginIdAsString();
        String platformIdentifier = StoragePlatformContextHolder.getPlatformOrDefault();
        try {
            return uploadFile(
                    file.getInputStream(),
                    file.getOriginalFilename(),
                    file.getSize(),
                    file.getContentType(),
                    userId,
                    parentId,
                    platformIdentifier
            );
        } catch (IOException e) {
            log.error("读取上传文件流失败: {}", e.getMessage(), e);
            throw new StorageOperationException("读取上传文件流失败: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileInfo uploadFile(InputStream inputStream, String originalName, long size, String mimeType,
                               String userId, String parentId, String storagePlatformIdentifier) {
        if (inputStream == null) {
            throw new StorageOperationException("上传文件流不能为空");
        }
        if (StrUtil.isBlank(originalName)) {
            throw new StorageOperationException("原始文件名不能为空");
        }

        byte[] fileBytes = null;
        String md5 = null;
        try {
            fileBytes = IoUtil.readBytes(inputStream);

            // 计算文件 MD5
            md5 = DigestUtil.md5Hex(fileBytes);
        } catch (Exception e) {
            log.error("读取文件流失败: {}", e.getMessage(), e);
            throw new StorageOperationException("读取文件流失败: " + e.getMessage(), e);
        } finally {
            IoUtil.close(inputStream);
        }

        // 秒传检查
        FileInfo existingFile = checkSecondUpload(md5, storagePlatformIdentifier, userId, originalName);
        if (existingFile != null) {
            log.info("秒传成功，文件ID: {}, MD5: {}", existingFile.getId(), md5);
            return existingFile;
        }

        // 获取存储服务（根据平台标识）
        StorageOperationService storageService = storageServiceFacade.getService(storagePlatformIdentifier);
        String platformIdentifier = storageService.getPlatformIdentifier();

        // 生成文件ID和对象键
        String fileId = IdUtil.fastSimpleUUID();
        String suffix = FileUtil.extName(originalName);

        //TODO 后续保存的文件名需要根据用户的配置是否生成新的
        String displayName = IdUtil.fastSimpleUUID() + "." + suffix;
        String objectKey = generateObjectKey(userId, displayName, suffix);
        try {
            // 从字节数组创建新的输入流上传到存储平台
            ByteArrayInputStream uploadStream = new ByteArrayInputStream(fileBytes);
            storageService.uploadFile(uploadStream, objectKey, mimeType, size);

            // 创建文件信息记录
            FileInfo fileInfo = new FileInfo();
            fileInfo.setId(fileId);
            fileInfo.setObjectKey(objectKey);
            fileInfo.setOriginalName(originalName);
            // 默认显示名与原始名相同
            fileInfo.setDisplayName(displayName);
            fileInfo.setSuffix(suffix);
            fileInfo.setSize(size);
            fileInfo.setMimeType(mimeType);
            fileInfo.setIsDir(false);
            fileInfo.setParentId(parentId);
            fileInfo.setUserId(userId);
            fileInfo.setContentMd5(md5);
            fileInfo.setStoragePlatformIdentifier(platformIdentifier);
            fileInfo.setUploadTime(LocalDateTime.now());
            fileInfo.setUpdateTime(LocalDateTime.now());
            fileInfo.setIsDeleted(false);

            // 保存文件信息到数据库
            save(fileInfo);
            return fileInfo;
        } catch (Exception e) {
            log.error("上传文件失败: {}", e.getMessage(), e);
            throw new StorageOperationException("上传文件失败: " + e.getMessage(), e);
        }
    }

    @Override
    public FileInfo checkSecondUpload(String md5, String storagePlatformIdentifier, String userId, String originalName) {
        if (StrUtil.isBlank(md5) || StrUtil.isBlank(storagePlatformIdentifier)) {
            return null;
        }
        FileInfo existingFile = getOne(
                new QueryWrapper()
                        .where(FILE_INFO.CONTENT_MD5.eq(md5)
                                .and(FILE_INFO.STORAGE_PLATFORM_IDENTIFIER.eq(storagePlatformIdentifier))
                                .and(FILE_INFO.USER_ID.eq(userId))
                                .and(FILE_INFO.ORIGINAL_NAME.eq(originalName))
                                .and(FILE_INFO.IS_DELETED.eq(false))
                        )
        );
        return existingFile;
    }

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

        // 根据文件记录中的平台标识获取对应的存储服务
        StorageOperationService storageService = storageServiceFacade.getService(fileInfo.getStoragePlatformIdentifier());
        return storageService.downloadFile(fileInfo.getObjectKey());
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

        // 根据文件记录中的平台标识获取对应的存储服务
        StorageOperationService storageService = storageServiceFacade.getService(fileInfo.getStoragePlatformIdentifier());
        return storageService.getFileUrl(fileInfo.getObjectKey(), expireSeconds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteFile(String fileId) {
        FileInfo fileInfo = getById(fileId);
        if (fileInfo == null) {
            return false;
        }
        if (fileInfo.getIsDeleted()) {
            return true;
        }
        // 更新文件状态为已删除
        fileInfo.setIsDeleted(true);
        fileInfo.setDeletedTime(LocalDateTime.now());
        return updateById(fileInfo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createDirectory(CreateDirectoryDTO dto) {
        // 生成目录ID
        String folderId = IdUtil.fastSimpleUUID();
        String userId = StpUtil.getLoginIdAsString();
        String platformIdentifier = storageServiceFacade.getCurrentService().getPlatformIdentifier();

        String baseName = dto.getFolderName().trim();
        String finalName = baseName;
        int suffixNum = 0;
        // 查询同级目录下是否存在同名文件夹
        QueryWrapper query = new QueryWrapper();
        query.where(FILE_INFO.PARENT_ID.eq(dto.getParentId()))
                .and(FILE_INFO.USER_ID.eq(userId))
                .and(FILE_INFO.IS_DIR.eq(true))
                .and(FILE_INFO.IS_DELETED.eq(false))
                .and(FILE_INFO.ORIGINAL_NAME.like(baseName + "%"));
        List<FileInfo> existingFolders = list(query);
        if (!existingFolders.isEmpty()) {
            // 提取所有匹配 `(n)` 后缀的数字
            Set<Integer> usedSuffixes = existingFolders.stream()
                    .map(f -> {
                        String name = f.getOriginalName();
                        Matcher matcher = Pattern.compile("\\((\\d+)\\)$").matcher(name);
                        if (matcher.find()) {
                            return Integer.parseInt(matcher.group(1));
                        }
                        return name.equals(baseName) ? 0 : -1;
                    })
                    .filter(n -> n >= 0)
                    .collect(Collectors.toSet());

            // 递增生成唯一名称
            do {
                suffixNum++;
                finalName = baseName + "(" + suffixNum + ")";
            } while (usedSuffixes.contains(suffixNum));
        }
        // 创建目录信息记录
        FileInfo dirInfo = new FileInfo();
        dirInfo.setId(folderId);
        dirInfo.setOriginalName(finalName);
        dirInfo.setDisplayName(finalName);
        dirInfo.setIsDir(true);
        dirInfo.setParentId(dto.getParentId());
        dirInfo.setUserId(userId);
        dirInfo.setStoragePlatformIdentifier(platformIdentifier);
        dirInfo.setUploadTime(LocalDateTime.now());
        dirInfo.setUpdateTime(LocalDateTime.now());
        dirInfo.setIsDeleted(false);
        // 保存目录信息到数据库
        save(dirInfo);
        log.info("创建文件夹成功，folderId: {}, name: {}, parentId: {}, userId: {}", folderId, finalName, dto.getParentId(), userId);
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
        String platformIdentifier = storageServiceFacade.getCurrentService().getPlatformIdentifier();
        remove(new QueryWrapper()
                .where(FILE_INFO.USER_ID.eq(userId)
                        .and(FILE_INFO.IS_DELETED.eq(true)
                                .and(FILE_INFO.STORAGE_PLATFORM_IDENTIFIER.eq(platformIdentifier))
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

    /**
     * 生成对象键
     * <p>
     * 格式: {projectName}/{userId}/{yyyyMMdd}/{fileId}.{suffix}
     * 示例: free-fs/user001/20241226/abc123.pdf
     *
     * @param userId     用户ID
     * @param objectName 文件名
     * @param suffix     文件后缀
     * @return 对象键
     */
    private String generateObjectKey(String userId, String objectName, String suffix) {
        StringBuilder objectKey = new StringBuilder();

        // 1. 项目名称（从 spring.application.name 读取）
        objectKey.append(applicationName).append("/");

        // 2. 用户ID
        if (StrUtil.isNotBlank(userId)) {
            objectKey.append(userId).append("/");
        } else {
            objectKey.append("anonymous/");  // 匿名用户
        }

        // 3. 日期目录 (yyyyMMdd 格式)
        String dateDir = DateUtil.format(new java.util.Date(), "yyyyMMdd");
        objectKey.append(dateDir).append("/");

        // 4. 文件ID + 后缀
        objectKey.append(objectName);
        if (StrUtil.isNotBlank(suffix)) {
            objectKey.append(".").append(suffix);
        }

        return objectKey.toString();
    }

    @Override
    public List<FileVO> getList(FileQry qry) {
        String userId = StpUtil.getLoginIdAsString();
        String storagePlatformIdentifier = storageServiceFacade.getCurrentService().getPlatformIdentifier();
        // 构建查询条件
        QueryWrapper wrapper = new QueryWrapper();
        wrapper.where(FILE_INFO.USER_ID.eq(userId));
        wrapper.and(FILE_INFO.IS_DELETED.eq(false));
        wrapper.and(FILE_INFO.STORAGE_PLATFORM_IDENTIFIER.eq(storagePlatformIdentifier));
        //  父目录过滤
        if (qry.getParentId() == null) {
            wrapper.and(FILE_INFO.PARENT_ID.isNull());
        } else {
            wrapper.and(FILE_INFO.PARENT_ID.eq(qry.getParentId()));
        }

        // 关键词搜索（搜索原始文件名和显示文件名）
        if (StrUtil.isNotBlank(qry.getKeyword())) {
            String keyword = "%" + qry.getKeyword().trim() + "%";
            wrapper.and(
                    FILE_INFO.ORIGINAL_NAME.like(keyword)
                            .or(FILE_INFO.DISPLAY_NAME.like(keyword))
            );
        }
        // 文件类型过滤,全部查询特殊处理
        applyFileTypeFilter(wrapper, qry);

        String orderBy = StrUtil.toUnderlineCase(qry.getOrderBy());
        boolean isAsc = "ASC".equalsIgnoreCase(qry.getOrderDirection());

        wrapper.orderBy(FILE_INFO.IS_DIR.desc())
                .orderBy(orderBy, isAsc);

        List<FileInfo> fileInfos = this.list(wrapper);
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
    public List<FileRecycleVO> getRecycles() {
        String userId = StpUtil.getLoginIdAsString();
        String storagePlatformIdentifier = storageServiceFacade.getCurrentService().getPlatformIdentifier();
        List<FileInfo> fileInfos = this.list(
                new QueryWrapper()
                        .where(FILE_INFO.USER_ID.eq(userId)
                                .and(FILE_INFO.IS_DELETED.eq(true))
                                .and(FILE_INFO.STORAGE_PLATFORM_IDENTIFIER.eq(storagePlatformIdentifier))
                        ));
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
