package com.xddcodec.fs.storage.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.xddcodec.fs.framework.common.domain.Result;
import com.xddcodec.fs.storage.domain.StoragePlatform;
import com.xddcodec.fs.storage.domain.StorageSetting;
import com.xddcodec.fs.storage.domain.dto.StorageSettingEditCmd;
import com.xddcodec.fs.storage.domain.vo.StoragePlatformVO;
import com.xddcodec.fs.storage.service.StoragePlatformService;
import com.xddcodec.fs.storage.service.StorageSettingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@RequestMapping("/apis/storage")
@RequiredArgsConstructor
@Tag(name = "存储平台管理")
public class StorageController {

    private final StoragePlatformService storagePlatformService;

    private final StorageSettingService storageSettingService;

    @Operation(summary = "获取存储平台列表")
    @GetMapping("/platforms")
    public Result<List<StoragePlatformVO>> getPlatforms(@RequestParam(value = "keywords", required = false) String keywords) {
        List<StoragePlatformVO> result = storagePlatformService.listStoragePlatformsByUser(keywords);
        return Result.ok(result);
    }

    @Operation(summary = "根据标识符获取存储平台详情")
    @GetMapping("/platform/{identifier}")
    public Result<StoragePlatform> getStoragePlatformByIdentifier(@PathVariable("identifier") String identifier) {
        StoragePlatform detail = storagePlatformService.getStoragePlatformByIdentifier(identifier);
        return Result.ok(detail);
    }

    @Operation(summary = "开通或取消开通存储平台")
    @PostMapping("/platform/{identifier}/{action}")
    public Result<StorageSetting> openOrCancelStoragePlatform(@PathVariable("identifier") String identifier, @PathVariable("action") Integer action) {
        storageSettingService.openOrCancelStoragePlatform(identifier, action);
        return Result.ok();
    }

    @Operation(summary = "根据标识符获取用户存储设置信息")
    @GetMapping("/settings/{identifier}")
    public Result<StorageSetting> getStorageSettingByPlatform(@PathVariable("identifier") String identifier) {
        String userId = StpUtil.getLoginIdAsString();
        StorageSetting setting = storageSettingService.getStorageSettingByPlatform(identifier, userId);
        return Result.ok(setting);
    }

    @Operation(summary = "新增或更新存储平台配置")
    @PostMapping("/settings")
    public Result<StorageSetting> saveOrUpdateStorageSetting(@Validated @RequestBody StorageSettingEditCmd cmd) {
        storageSettingService.saveOrUpdateStorageSetting(cmd);
        return Result.ok();
    }

    @Operation(summary = "获取用户已开通已配置的存储平台列表")
    @GetMapping("/active-platforms")
    public Result<List<StoragePlatformVO>> getEnabledStorageSettingByUser() {
        String userId = StpUtil.getLoginIdAsString();
        List<StoragePlatformVO> settings = storagePlatformService.listEnabledStorageSettingByUser(userId);
        return Result.ok(settings);
    }


//    @Operation(summary = "新增存储平台")
//    @PostMapping("/platform")
//    public Result<?> createStoragePlatform(@Validated @RequestBody StoragePlatformAddCmd cmd) {
//        storagePlatformService.saveStoragePlatform(cmd);
//        return Result.ok();
//    }

//    @Operation(summary = "编辑存储平台")
//    @PutMapping("/platform")
//    public Result<?> editStoragePlatform(@Validated @RequestBody StoragePlatformEditCmd cmd) {
//        storagePlatformService.editStoragePlatform(cmd);
//        return Result.ok();
//    }

//    @Operation(summary = "删除存储平台")
//    @DeleteMapping("/platform/{id}")
//    public Result<?> saveStoragePlatform(@PathVariable Long id) {
//        storagePlatformService.deleteStoragePlatformById(id);
//        return Result.ok();
//    }


}
