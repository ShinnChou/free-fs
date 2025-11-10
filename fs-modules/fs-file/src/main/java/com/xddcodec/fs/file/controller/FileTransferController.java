package com.xddcodec.fs.file.controller;

import com.xddcodec.fs.file.domain.dto.CheckUploadCmd;
import com.xddcodec.fs.file.domain.dto.InitUploadCmd;
import com.xddcodec.fs.file.domain.dto.UploadChunkCmd;
import com.xddcodec.fs.file.domain.qry.TransferFilesQry;
import com.xddcodec.fs.file.domain.vo.CheckUploadResultVO;
import com.xddcodec.fs.file.domain.vo.FileUploadTaskVO;
import com.xddcodec.fs.file.service.FileTransferService;
import com.xddcodec.fs.framework.common.domain.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;

@Validated
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/apis/transfer")
@Tag(name = "文件传输", description = "文件传输")
public class FileTransferController {

    private final FileTransferService fileTransferService;

    @GetMapping("/files")
    @Operation(summary = "获取传输列表", description = "获取传输列表")
    public Result<List<FileUploadTaskVO>> getTransferFiles(TransferFilesQry qry) {
        List<FileUploadTaskVO> result = fileTransferService.getTransferFiles(qry);
        return Result.ok(result);
    }

    @PostMapping("/init")
    @Operation(summary = "初始化文件上传", description = "初始化上传环境，返回taskId用于后续分片上传")
    public Result<String> initUpload(@RequestBody @Validated InitUploadCmd cmd) {
        String taskId = fileTransferService.initUpload(cmd);
        return Result.ok(taskId, "初始化成功");
    }

    @PostMapping("/check")
    @Operation(summary = "校验文件", description = "前端计算完MD5后调用，判断是否秒传")
    public Result<CheckUploadResultVO> checkUpload(@RequestBody @Validated CheckUploadCmd cmd) {
        CheckUploadResultVO result = fileTransferService.checkUpload(cmd);
        return Result.ok(result);
    }

    @PostMapping("/chunk")
    @Operation(summary = "上传分片", description = "异步上传分片，立即返回，通过WebSocket推送进度")
    public Result<?> uploadChunk(
            @RequestParam("file") MultipartFile file,
            @RequestParam("taskId") String taskId,
            @RequestParam("chunkIndex") Integer chunkIndex,
            @RequestParam("chunkMd5") String chunkMd5
    ) throws Exception {
        UploadChunkCmd cmd = new UploadChunkCmd();
        cmd.setTaskId(taskId);
        cmd.setChunkIndex(chunkIndex);
        cmd.setChunkMd5(chunkMd5);
        byte[] fileBytes = file.getBytes();
        fileTransferService.uploadChunk(fileBytes, cmd);
        return Result.ok(null, "分片接收成功，正在处理");
    }

    @PostMapping("/pause/{taskId}")
    @Operation(summary = "暂停上传")
    public Result<Void> pauseUpload(@PathVariable String taskId) {
        fileTransferService.pauseUpload(taskId);
        return Result.ok();
    }

    @PostMapping("/resume/{taskId}")
    @Operation(summary = "继续上传")
    public Result<Void> resumeUpload(@PathVariable String taskId) {
        fileTransferService.resumeUpload(taskId);
        return Result.ok();
    }

    @PostMapping("/cancel/{taskId}")
    @Operation(summary = "取消上传")
    public Result<Void> cancelUpload(@PathVariable String taskId) {
        fileTransferService.cancelUpload(taskId);
        return Result.ok();
    }

    @GetMapping("/chunks/{taskId}")
    @Operation(summary = "查询已上传的分片", description = "用于断点续传，返回已上传的分片索引列表")
    public Result<Set<Integer>> getUploadedChunks(@PathVariable String taskId) {
        Set<Integer> uploadedChunks = fileTransferService.getUploadedChunks(taskId);
        return Result.ok(uploadedChunks);
    }
}
