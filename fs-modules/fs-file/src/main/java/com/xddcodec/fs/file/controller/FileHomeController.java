package com.xddcodec.fs.file.controller;

import com.xddcodec.fs.file.domain.vo.FileHomeVO;
import com.xddcodec.fs.file.service.FileHomeService;
import com.xddcodec.fs.framework.common.domain.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@Slf4j
@RestController
@RequestMapping("/apis/home")
@Tag(name = "文件首页", description = "首页")
public class FileHomeController {

    @Autowired
    private FileHomeService fileHomeService;

    @GetMapping("/info")
    @Operation(summary = "查询首页信息", description = "查询首页信息")
    public Result<FileHomeVO> getHomes() {
        FileHomeVO homeVO = fileHomeService.getFileHomes();
        return Result.ok(homeVO);
    }

}
