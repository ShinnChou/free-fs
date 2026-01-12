package com.xddcodec.fs.storage.plugin.rustfs;

import com.xddcodec.fs.storage.plugin.core.annotation.StoragePlugin;
import com.xddcodec.fs.storage.plugin.core.config.StorageConfig;
import com.xddcodec.fs.storage.plugin.core.s3.AbstractS3CompatibleStorageService;
import com.xddcodec.fs.storage.plugin.rustfs.config.RustFsConfig;

/**
 * RustFS 存储插件实现
 * 基于S3兼容协议的对象存储服务
 *
 * @Author: xddcode
 * @Date: 2026/01/12 22:06
 */
@StoragePlugin(
        identifier = "RustFS",
        name = "RustFS对象存储",
        description = "RustFS是一个高性能的S3兼容对象存储服务",
        icon = "icon-bendicunchu1",
        link = "https://github.com/rustfs/rustfs",
        configSchema = """
                {
                    "type": "object",
                    "properties": {
                        "endpoint": {"type": "string", "title": "Endpoint", "description": "RustFS服务端点"},
                        "accessKey": {"type": "string", "title": "AccessKey", "description": "访问密钥ID"},
                        "secretKey": {"type": "string", "title": "SecretKey", "description": "访问密钥Secret", "format": "password"},
                        "bucket": {"type": "string", "title": "Bucket名称", "description": "存储桶名称"},
                        "customDomain": {"type": "string", "title": "自定义域名", "description": "用于生成文件URL的自定义域名（可选）"}
                    },
                    "required": ["endpoint", "accessKey", "secretKey", "bucket"]
                }
                """
)
public class RustFsStorageServiceImpl extends AbstractS3CompatibleStorageService<RustFsConfig> {

    public RustFsStorageServiceImpl() {
        super();
    }

    public RustFsStorageServiceImpl(StorageConfig config) {
        super(config);
    }

    @Override
    protected Class<RustFsConfig> getS3ConfigClass() {
        return RustFsConfig.class;
    }
}
