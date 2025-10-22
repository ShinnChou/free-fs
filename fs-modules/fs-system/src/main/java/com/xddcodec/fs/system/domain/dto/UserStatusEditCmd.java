package com.xddcodec.fs.system.domain.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @Author: xddcode
 * @Date: 2024/11/7 15:47
 */
@Data
public class UserStatusEditCmd {

    @NotEmpty(message = "userId不能为空")
    private String userId;

    @NotNull(message = "status不能为空")
    @Min(value = 0)
    @Max(value = 1)
    private Integer status;
}
