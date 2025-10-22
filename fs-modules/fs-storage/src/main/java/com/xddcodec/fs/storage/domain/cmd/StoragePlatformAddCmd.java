package com.xddcodec.fs.storage.domain.cmd;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class StoragePlatformAddCmd {

    /**
     * 存储平台名称
     */
    @NotBlank(message = "name不能为空")
    private String name;

    /**
     * 存储平台标识符
     */
    @NotBlank(message = "identifier不能为空")
    private String identifier;

    /**
     * 存储平台配置描述schema
     */
    private String configScheme;

    /**
     * 存储平台图标
     */
    private String icon;

    /**
     * 存储平台链接
     */
    private String link;

    /**
     * 存储平台描述
     */
    private String desc;
}
