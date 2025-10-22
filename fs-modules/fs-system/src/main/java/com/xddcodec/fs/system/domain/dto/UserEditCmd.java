package com.xddcodec.fs.system.domain.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * @Author: hao.ding@insentek.com
 * @Date: 2024/12/4 9:25
 */
@Data
public class UserEditCmd{

    @NotBlank(message = "id不能为空")
    private String id;

    @NotBlank(message = "username不能为空")
    private String username;

    @NotBlank(message = "email不能为空")
    private String email;

    @NotBlank(message = "roleCode不能为空")
    private String roleCode;

    @NotBlank(message = "nickname不能为空")
    private String nickname;
}
