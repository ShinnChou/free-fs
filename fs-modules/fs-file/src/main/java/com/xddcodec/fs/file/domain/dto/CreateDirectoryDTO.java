package com.xddcodec.fs.file.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 创建目录DTO
 *
 * @Author: hao.ding@insentek.com
 * @Date: 2025/5/13 8:41
 */
@Data
public class CreateDirectoryDTO {

    @Schema(title = "目录名称")
    @NotBlank(message = "目录名称不能为空")
    private String folderName;

    @Schema(title = "父目录ID")
    private String parentId;
}
