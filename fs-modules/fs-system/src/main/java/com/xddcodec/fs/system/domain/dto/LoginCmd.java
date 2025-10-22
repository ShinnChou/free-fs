package com.xddcodec.fs.system.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录DTO对象
 *
 * @Author: xddcode
 * @Date: 2024/6/7 11:24
 */
@Data
public class LoginCmd {

    @NotBlank(message = "账号不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    private Boolean isRemember;
}
