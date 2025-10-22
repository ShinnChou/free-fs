package com.xddcodec.fs.storage.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * @Author: hao.ding@insentek.com
 * @Date: 2024/12/11 9:04
 */
@Data
public class StorageSettingEditCmd {

    @NotBlank(message = "identifier不能为空")
    private String identifier;

    private String configData;
}
