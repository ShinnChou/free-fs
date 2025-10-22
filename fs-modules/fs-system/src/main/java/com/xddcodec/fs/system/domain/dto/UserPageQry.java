package com.xddcodec.fs.system.domain.dto;

import lombok.Data;

/**
 * @Author: xddcode
 * @Date: 2024/10/17 9:34
 */
@Data
public class UserPageQry {

    private Integer page;

    private Integer pageSize;

    private String username;

    private String nickname;

    private String email;

    private Integer status;
}
