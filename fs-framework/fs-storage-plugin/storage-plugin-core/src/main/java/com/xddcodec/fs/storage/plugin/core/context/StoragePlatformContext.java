package com.xddcodec.fs.storage.plugin.core.context;

import lombok.Builder;
import lombok.Data;

/**
 * 存储平台上下文
 */
@Data
@Builder
public class StoragePlatformContext {

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 平台标识符
     */
    private String platformIdentifier;
}
