package com.xddcodec.fs.file.domain.vo;

import com.xddcodec.fs.file.domain.FileInfo;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
@AutoMapper(target = FileInfo.class)
public class FileDetailVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}
