package com.xddcodec.fs.framework.common.exception;

import java.io.Serial;

/**
 * 存储配置异常
 *
 * @Author: xddcode
 * @Date: 2024/6/7 16:07
 */
public class StorageConfigException extends IException {

    @Serial
    private static final long serialVersionUID = 7993671808524980055L;

    public StorageConfigException() {
        super();
    }

    public StorageConfigException(String message) {
        super(message);
    }
}
