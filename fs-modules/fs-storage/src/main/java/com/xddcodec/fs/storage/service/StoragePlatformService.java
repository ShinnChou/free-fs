package com.xddcodec.fs.storage.service;

import com.xddcodec.fs.storage.domain.StoragePlatform;
import com.xddcodec.fs.storage.domain.StorageSetting;
import com.xddcodec.fs.storage.domain.cmd.StoragePlatformAddCmd;
import com.xddcodec.fs.storage.domain.cmd.StoragePlatformEditCmd;
import com.xddcodec.fs.storage.domain.vo.StoragePlatformVO;
import com.mybatisflex.core.service.IService;

import java.util.List;

/**
 * 存储平台业务接口
 *
 * @Author: xddcode
 * @Date: 2024/10/25 14:37
 */
public interface StoragePlatformService extends IService<StoragePlatform> {

    /**
     * 查询用户的存储平台列表
     *
     * @param keywords 关键字
     * @return
     */
    List<StoragePlatformVO> listStoragePlatformsByUser(String keywords);

    /**
     * 根据标识符查询存储平台
     *
     * @param identifier
     * @return
     */
    StoragePlatform getStoragePlatformByIdentifier(String identifier);

    /**
     * 查询用户所有已开通和已配置的存储平台列表
     *
     * @param userId 用户ID
     * @return
     */
    List<StoragePlatformVO> listEnabledStorageSettingByUser(String userId);

    /**
     * 新增存储平台
     *
     * @param cmd
     */
    void saveStoragePlatform(StoragePlatformAddCmd cmd);

    /**
     * 编辑存储平台
     *
     * @param cmd
     */
    void editStoragePlatform(StoragePlatformEditCmd cmd);

    /**
     * 删除存储平台
     *
     * @param id
     */
    void deleteStoragePlatformById(Long id);
}
