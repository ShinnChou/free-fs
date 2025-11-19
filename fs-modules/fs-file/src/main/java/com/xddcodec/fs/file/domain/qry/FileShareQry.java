package com.xddcodec.fs.file.domain.qry;


import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 *
 * @author Yann
 * @date 2025/11/19 15:50
 */
@Data
public class FileShareQry {

    /** 分享ID */
    @NotBlank(message = "分享ID不能为空")
    private String shareId;
    /**
     * 文件父ID
     */
    private String parentId;
}
