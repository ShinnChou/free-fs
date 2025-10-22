package com.xddcodec.fs.file.domain.qry;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "文件列表查询参数")
public class FileQry {

    @Schema(description = "父目录ID（空表示根目录）", example = "123")
    private String parentId;

    @Schema(description = "文件名关键词（搜索用）", example = "工作文档")
    private String keyword;

    @Schema(description = "文件类型过滤", example = "IMAGE",
            allowableValues = {"ALL", "DOCUMENT", "IMAGE", "VIDEO", "AUDIO", "OTHER"})
    private String fileType;

    @Schema(description = "排序字段", example = "updateTime")
    private String orderBy;

    @Schema(description = "排序方向", example = "DESC", allowableValues = {"ASC", "DESC"})
    private String orderDirection = "DESC";
}
