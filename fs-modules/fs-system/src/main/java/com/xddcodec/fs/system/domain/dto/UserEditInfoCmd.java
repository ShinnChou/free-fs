package com.xddcodec.fs.system.domain.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * @Author: hao.ding@insentek.com
 * @Date: 2024/12/4 9:25
 */
@Data
public class UserEditInfoCmd {

    @NotBlank(message = "nickname不能为空")
    private String nickname;
}
