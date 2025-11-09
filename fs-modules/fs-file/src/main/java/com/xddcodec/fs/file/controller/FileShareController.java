package com.xddcodec.fs.file.controller;

import com.xddcodec.fs.file.domain.dto.CreateShareCmd;
import com.xddcodec.fs.file.domain.qry.FileSharePageQry;
import com.xddcodec.fs.file.domain.vo.FileShareVO;
import com.xddcodec.fs.file.service.FileShareService;
import com.xddcodec.fs.framework.common.domain.PageResult;
import com.xddcodec.fs.framework.common.domain.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@Slf4j
@RestController
@RequestMapping("/apis/share")
@Tag(name = "文件分享", description = "文件分享")
public class FileShareController {

    @Autowired
    private FileShareService fileShareService;

    @GetMapping("/pages")
    @Operation(summary = "分页获取我的分享", description = "分页获取我的分享列表")
    public PageResult<FileShareVO> getMyPages(FileSharePageQry qry) {
        return fileShareService.getMyPages(qry);
    }

    @PostMapping("/create")
    @Operation(summary = "创建分享", description = "创建分享")
    public Result<FileShareVO> createDirectory(@RequestBody @Validated CreateShareCmd cmd) {
        FileShareVO fileShareVO = fileShareService.createShare(cmd);
        return Result.ok(fileShareVO);
    }

    @PutMapping("/{id}/cancel")
    @Operation(summary = "取消分享", description = "取消分享")
    public Result<FileShareVO> cancelShare(@PathVariable String id) {
        fileShareService.cancelShare(id);
        return Result.ok();
    }
}
