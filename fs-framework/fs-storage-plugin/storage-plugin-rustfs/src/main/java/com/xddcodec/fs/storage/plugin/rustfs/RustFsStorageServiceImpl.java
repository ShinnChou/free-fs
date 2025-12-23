package com.xddcodec.fs.storage.plugin.rustfs;

import com.xddcodec.fs.framework.common.enums.StoragePlatformIdentifierEnum;
import com.xddcodec.fs.storage.plugin.core.config.StorageConfig;
import com.xddcodec.fs.storage.plugin.core.s3.AbstractS3CompatibleStorageService;
import com.xddcodec.fs.storage.plugin.rustfs.config.RustFsConfig;

public class RustFsStorageServiceImpl extends AbstractS3CompatibleStorageService<RustFsConfig> {

    public RustFsStorageServiceImpl() {
        super();
    }

    public RustFsStorageServiceImpl(StorageConfig config) {
        super(config);
    }

    @Override
    public String getPlatformIdentifier() {
        return StoragePlatformIdentifierEnum.RUSTFS.getIdentifier();
    }

    @Override
    protected Class<RustFsConfig> getS3ConfigClass() {
        return RustFsConfig.class;
    }
}
