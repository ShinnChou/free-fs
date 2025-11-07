package com.xddcodec.fs.file.controller;

import com.xddcodec.fs.file.domain.FileInfo;
import com.xddcodec.fs.file.domain.dto.InitUploadCmd;
import com.xddcodec.fs.file.domain.dto.UploadChunkCmd;
import com.xddcodec.fs.file.domain.qry.TransferFilesQry;
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
        
        // 在HTTP请求上下文中立即读取文件内容（防止异步时临时文件被清理）
        byte[] fileBytes = file.getBytes();
        
        // 传递字节数组给异步方法
        fileTransferService.uploadChunkAsync(fileBytes, cmd);
        return Result.ok(null, "分片接收成功，正在处理");
    }

    @PostMapping("/merge/{taskId}")
    @Operation(summary = "合并分片", description = "合并分片，创建文件记录")
    public Result<FileInfo> mergeChunks(@PathVariable String taskId) {
        FileInfo fileInfo = fileTransferService.mergeChunks(taskId);
        return Result.ok(fileInfo);
    }

    @GetMapping("/chunks/{taskId}")
    @Operation(summary = "查询已上传的分片", description = "用于断点续传，返回已上传的分片索引列表")
    public Result<Set<Integer>> getUploadedChunks(@PathVariable String taskId) {
        Set<Integer> uploadedChunks = fileTransferService.getUploadedChunks(taskId);
        return Result.ok(uploadedChunks);
    }
}
