package com.xddcodec.fs.framework.common.enums;

import lombok.Getter;

/**
 * 存储平台标识枚举,这里需要和数据库中的存储平台标识保持一致
 *
 * @Author: hao.ding@insentek.com
 * @Date: 2025/5/8 9:06
 */
@Getter
public enum StoragePlatformIdentifierEnum {
    LOCAL("Local", "本地存储", "icon-bendicunchu1"),
    RUSTFS("RustFS", "RustFS对象存储", "icon-bendicunchu1"),
    ALIYUN_OSS("AliyunOSS", "阿里云OSS", "icon-aliyun1"),
    QINIU_KODO("Kodo", "七牛云Kodo", "icon-normal-logo-blue");

    private final String identifier;
    private final String description;
    private final String icon;

    StoragePlatformIdentifierEnum(String identifier, String description, String icon) {
        this.identifier = identifier;
        this.description = description;
        this.icon = icon;
    }

    public static StoragePlatformIdentifierEnum fromIdentifier(String identifier) {
        for (StoragePlatformIdentifierEnum type : values()) {
            if (type.getIdentifier().equalsIgnoreCase(identifier)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown storage platform identifier: " + identifier);
    }
}