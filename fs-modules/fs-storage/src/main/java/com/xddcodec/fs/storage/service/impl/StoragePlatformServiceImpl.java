package com.xddcodec.fs.storage.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.xddcodec.fs.framework.common.constant.CommonConstant;
import com.xddcodec.fs.framework.common.enums.StoragePlatformIdentifierEnum;
import com.xddcodec.fs.framework.common.exception.BusinessException;
import com.xddcodec.fs.framework.common.utils.StringUtils;
import com.xddcodec.fs.storage.domain.StoragePlatform;
import com.xddcodec.fs.storage.domain.StorageSetting;
import com.xddcodec.fs.storage.domain.cmd.StoragePlatformAddCmd;
import com.xddcodec.fs.storage.domain.cmd.StoragePlatformEditCmd;
import com.xddcodec.fs.storage.domain.vo.StoragePlatformVO;
import com.xddcodec.fs.storage.mapper.StoragePlatformMapper;
import com.xddcodec.fs.storage.service.StoragePlatformService;
import com.xddcodec.fs.storage.service.StorageSettingService;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import io.github.linpeilie.Converter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.xddcodec.fs.storage.domain.table.StoragePlatformTableDef.STORAGE_PLATFORM;
import static com.xddcodec.fs.storage.domain.table.StorageSettingTableDef.STORAGE_SETTING;

/**
 * 存储平台业务接口实现
 *
 * @Author: xddcode
 * @Date: 2024/10/25 14:38
 */
@Service
@RequiredArgsConstructor
public class StoragePlatformServiceImpl extends ServiceImpl<StoragePlatformMapper, StoragePlatform> implements StoragePlatformService {

    private final Converter converter;

    private final StorageSettingService storageSettingService;

    @Override
    public List<StoragePlatformVO> listStoragePlatformsByUser(String keywords) {
        String userId = StpUtil.getLoginIdAsString();
        QueryWrapper queryWrapper = new QueryWrapper();
        if (StringUtils.isNotEmpty(keywords)) {
            queryWrapper.and(
                    STORAGE_PLATFORM.NAME.like("%" + keywords + "%")
                            .or(STORAGE_PLATFORM.IDENTIFIER.like("%" + keywords + "%"))
                            .or(STORAGE_PLATFORM.DESC.like("%" + keywords + "%"))
            );
        }
        List<StoragePlatform> storagePlatforms = this.list(queryWrapper);
        return storagePlatforms.parallelStream().map(soilMonitor -> {
            StoragePlatformVO vo = converter.convert(soilMonitor, StoragePlatformVO.class);
            StorageSetting storageSetting = storageSettingService.getStorageSettingByPlatform(vo.getIdentifier(), userId);
            if (storageSetting == null) {
                vo.setIsSetting(CommonConstant.N);
                vo.setIsEnabled(CommonConstant.N);
            } else {
                vo.setIsSetting(StringUtils.isEmpty(storageSetting.getConfigData()) ? CommonConstant.N : CommonConstant.Y);
                vo.setIsEnabled(storageSetting.getEnabled());
            }
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public StoragePlatform getStoragePlatformByIdentifier(String identifier) {
        return this.getOne(new QueryWrapper().where(STORAGE_PLATFORM.IDENTIFIER.eq(identifier)));
    }

    @Override
    public List<StoragePlatformVO> listEnabledStorageSettingByUser(String userId) {
        List<StoragePlatformVO> result = new ArrayList<>();
        StoragePlatformVO local = new StoragePlatformVO();
        local.setName(StoragePlatformIdentifierEnum.LOCAL.getDescription());
        local.setIdentifier(StoragePlatformIdentifierEnum.LOCAL.getIdentifier());
        local.setIcon(StoragePlatformIdentifierEnum.LOCAL.getIcon());
        result.add(local);
        List<StorageSetting> storageSettings = storageSettingService.list(
                new QueryWrapper().where(STORAGE_SETTING.USER_ID.eq(userId))
                        .and(STORAGE_SETTING.ENABLED.eq(CommonConstant.Y))
                        .and(STORAGE_SETTING.CONFIG_DATA.isNotNull())
        );

        for (StorageSetting storageSetting : storageSettings) {
            StoragePlatform storagePlatform = this.getStoragePlatformByIdentifier(storageSetting.getPlatformIdentifier());
            StoragePlatformVO storagePlatformVO = converter.convert(storagePlatform, StoragePlatformVO.class);
            result.add(storagePlatformVO);
        }
        return result;
    }

    @Override
    public void saveStoragePlatform(StoragePlatformAddCmd cmd) {
        StoragePlatform existName = this.getOne(new QueryWrapper().where(STORAGE_PLATFORM.NAME.eq(cmd.getName())));
        if (existName != null) {
            throw new BusinessException("存储平台名称已存在");
        }
        StoragePlatform existIdentifier = this.getOne(new QueryWrapper().where(STORAGE_PLATFORM.IDENTIFIER.eq(cmd.getIdentifier())));
        if (existIdentifier != null) {
            throw new BusinessException("存储平台标识已存在");
        }
        StoragePlatform storagePlatform = new StoragePlatform();
        storagePlatform.setName(cmd.getName());
        storagePlatform.setIdentifier(cmd.getIdentifier());
        storagePlatform.setDesc(cmd.getDesc());
        storagePlatform.setConfigScheme(cmd.getConfigScheme());
        storagePlatform.setIcon(cmd.getIcon());
        storagePlatform.setLink(cmd.getLink());
        this.save(storagePlatform);
    }

    @Override
    public void editStoragePlatform(StoragePlatformEditCmd cmd) {
        StoragePlatform storagePlatform = this.getById(cmd.getId());
        if (storagePlatform == null) {
            throw new BusinessException("存储平台不存在");
        }
        // 检查名称是否重复（排除自己）
        StoragePlatform existName = this.getOne(new QueryWrapper()
                .where(STORAGE_PLATFORM.NAME.eq(cmd.getName()))
                .and(STORAGE_PLATFORM.ID.ne(cmd.getId())));
        if (existName != null) {
            throw new BusinessException("存储平台名称已存在");
        }
        // 检查标识符是否重复（排除自己）
        StoragePlatform existIdentifier = this.getOne(new QueryWrapper()
                .where(STORAGE_PLATFORM.IDENTIFIER.eq(cmd.getIdentifier()))
                .and(STORAGE_PLATFORM.ID.ne(cmd.getId())));
        if (existIdentifier != null) {
            throw new BusinessException("存储平台标识已存在");
        }
        storagePlatform.setName(cmd.getName());
        storagePlatform.setIdentifier(cmd.getIdentifier());
        storagePlatform.setDesc(cmd.getDesc());
        storagePlatform.setConfigScheme(cmd.getConfigScheme());
        storagePlatform.setIcon(cmd.getIcon());
        storagePlatform.setLink(cmd.getLink());
        this.updateById(storagePlatform);
    }

    @Override
    public void deleteStoragePlatformById(Long id) {
        StoragePlatform storagePlatform = this.getById(id);
        if (storagePlatform == null) {
            throw new BusinessException("存储平台不存在");
        }
        // 检查是否有用户已开通此平台
        List<StorageSetting> enabledSettings = storageSettingService.listByPlatformIdentifier(storagePlatform.getIdentifier());
        if (!enabledSettings.isEmpty()) {
            throw new BusinessException("当前存储平台还有用户已开通，无法删除");
        }
        this.removeById(id);
    }
}
