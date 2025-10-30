package com.xddcodec.fs.file.controller;

import com.xddcodec.fs.file.domain.FileInfo;
import com.xddcodec.fs.file.domain.dto.CreateDirectoryCmd;
import com.xddcodec.fs.file.domain.dto.RenameFileCmd;
import com.xddcodec.fs.file.domain.qry.FileQry;
import com.xddcodec.fs.file.domain.vo.FileRecycleVO;
import com.xddcodec.fs.file.domain.vo.FileVO;
import com.xddcodec.fs.file.service.FileInfoService;
import com.xddcodec.fs.file.service.FileUserFavoritesService;
import com.xddcodec.fs.framework.common.domain.Result;
import com.xddcodec.fs.framework.common.exception.StorageOperationException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 文件资源控制器
 *
 * @Author: xddcode
 * @Date: 2025/5/8 10:00
 */
@Validated
@Slf4j
@RestController
@RequestMapping("/apis/file")
@Tag(name = "文件管理", description = "文件上传、下载、管理等接口")
public class FileController {

    @Autowired
    private FileInfoService fileInfoService;

    @Autowired
    private FileUserFavoritesService fileUserFavoritesService;

    @GetMapping("/list")
    @Operation(summary = "查询文件列表", description = "支持关键词搜索和文件类型筛选的列表查询")
    public Result<List<FileVO>> getList(FileQry qry) {
        List<FileVO> list = fileInfoService.getList(qry);
        return Result.ok(list);
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "上传文件", description = "上传文件到指定目录")
    @Parameters(value = {@Parameter(name = "parentId", description = "父目录ID，如果为空则上传到根目录")})
    public Result<FileInfo> uploadFile(@RequestPart("file") MultipartFile file, @RequestParam(value = "parentId", required = false) String parentId) {
        FileInfo fileInfo = fileInfoService.uploadFile(file, parentId);
        return Result.ok(fileInfo);
    }

    @GetMapping("/download/{fileId}")
    @Operation(summary = "下载文件", description = "根据文件ID下载文件")
    @Parameter(name = "fileId", description = "文件ID", in = ParameterIn.PATH, required = true)
    public ResponseEntity<InputStreamResource> downloadFile(@Parameter(description = "文件ID") @PathVariable("fileId") String fileId) {

        try {
            FileInfo fileInfo = fileInfoService.getById(fileId);
            if (fileInfo == null || fileInfo.getIsDir() || fileInfo.getIsDeleted()) {
                return ResponseEntity.notFound().build();
            }

            InputStream inputStream = fileInfoService.downloadFile(fileId);
            InputStreamResource resource = new InputStreamResource(inputStream);

            String encodedFileName = URLEncoder.encode(fileInfo.getOriginalName(), StandardCharsets.UTF_8).replaceAll("\\+", "%20");

            return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFileName).contentType(MediaType.parseMediaType(fileInfo.getMimeType())).contentLength(fileInfo.getSize()).body(resource);
        } catch (StorageOperationException e) {
            log.error("下载文件失败: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/url/{fileId}")
    @Operation(summary = "获取文件URL", description = "获取文件的访问URL")
    @Parameters(value = {@Parameter(name = "fileId", description = "文件ID", required = true), @Parameter(name = "expireSeconds", description = "URL有效时间（秒），如果不支持或永久有效可为null")})
    public Result<String> getFileUrl(@PathVariable("fileId") String fileId, @RequestParam(value = "expireSeconds", required = false) Integer expireSeconds) {

        String url = fileInfoService.getFileUrl(fileId, expireSeconds);
        return Result.ok(url);
    }

    @DeleteMapping()
    @Operation(summary = "移到回收站", description = "将文件移动到回收站")
    public Result<?> deleteFiles(@RequestBody List<String> fileIds) {
        fileInfoService.deleteFiles(fileIds);
        return Result.ok();
    }

    @PostMapping("/directory")
    @Operation(summary = "创建目录", description = "在指定目录下创建新目录")
    public Result<?> createDirectory(@RequestBody @Validated CreateDirectoryCmd cmd) {
        fileInfoService.createDirectory(cmd);
        return Result.ok();
    }

    @PutMapping("/{fileId}/rename")
    @Operation(summary = "文件重命名", description = "文件重命名")
    public Result<?> createDirectory(@PathVariable String fileId, @RequestBody @Validated RenameFileCmd cmd) {
        fileInfoService.renameFile(fileId, cmd);
        return Result.ok();
    }

    @GetMapping("/directory/{dirId}/path")
    @Operation(summary = "获取目录层级", description = "根据目录ID获取目录层级")
    public Result<List<FileVO>> createDirectory(@PathVariable String dirId) {
        List<FileVO> fileVOS = fileInfoService.getDirectoryTreePath(dirId);
        return Result.ok(fileVOS);
    }


    @GetMapping("/recycles")
    @Operation(summary = "获取回收站列表", description = "获取回收站列表")
    public Result<?> getRecycles(String keyword) {
        List<FileRecycleVO> list = fileInfoService.getRecycles(keyword);
        return Result.ok(list);
    }

    @PutMapping("/recycles")
    @Operation(summary = "恢复文件", description = "从回收站批量恢复文件")
    public Result<?> restoreFile(@RequestBody List<String> fileIds) {
        fileInfoService.restoreFiles(fileIds);
        return Result.ok();
    }

    @DeleteMapping("/recycles")
    @Operation(summary = "永久删除文件", description = "永久删除文件，不可恢复")
    public Result<?> permanentlyDeleteFiles(@RequestBody List<String> fileIds) {
        fileInfoService.permanentlyDeleteFiles(fileIds);
        return Result.ok();
    }

    @DeleteMapping("/recycles/clear")
    @Operation(summary = "清空回收站", description = "清空回收站，永久删除所有文件")
    public Result<?> clearRecycles() {
        fileInfoService.clearRecycles();
        return Result.ok();
    }

    @PostMapping("/favorites")
    @Operation(summary = "收藏文件", description = "收藏文件")
    public Result<?> favoritesFile(@RequestBody List<String> fileIds) {
        fileUserFavoritesService.favoritesFile(fileIds);
        return Result.ok();
    }

    @DeleteMapping("/favorites")
    @Operation(summary = "取消收藏文件", description = "取消收藏文件")
    public Result<?> unFavoritesFile(@RequestBody List<String> fileIds) {
        fileUserFavoritesService.unFavoritesFile(fileIds);
        return Result.ok();
    }
}