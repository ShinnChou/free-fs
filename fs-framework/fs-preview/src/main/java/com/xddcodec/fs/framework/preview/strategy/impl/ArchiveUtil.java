//package com.xddcodec.fs.framework.preview.strategy.impl;
//
//import com.github.junrar.Archive;
//import com.github.junrar.rarfile.FileHeader;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.commons.compress.archivers.ArchiveEntry;
//import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
//import org.apache.commons.compress.archivers.sevenz.SevenZFile;
//import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
//import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
//import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
//import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
//import java.io.IOException;
//import java.io.InputStream;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.nio.file.StandardCopyOption;
//import java.util.*;
///**
// * 压缩包解析工具类
// *
// * @Author: xddcode
// * @Date: 2024/12/26
// */
//@Slf4j
//public class ArchiveUtil {
//
//    /**
//     * 解析压缩包
//     */
//    public static List<ArchiveFileInfo> parseArchive(InputStream inputStream, String fileName) throws Exception {
//        String lowerFileName = fileName.toLowerCase();
//
//        if (lowerFileName.endsWith(".zip")) {
//            return parseZip(inputStream);
//        } else if (lowerFileName.endsWith(".rar")) {
//            return parseRar(inputStream);
//        } else if (lowerFileName.endsWith(".7z")) {
//            return parse7z(inputStream);
//        } else if (lowerFileName.endsWith(".tar")) {
//            return parseTar(inputStream);
//        } else if (lowerFileName.endsWith(".tar.gz") || lowerFileName.endsWith(".tgz")) {
//            return parseTarGz(inputStream);
//        } else if (lowerFileName.endsWith(".tar.bz2")) {
//            return parseTarBz2(inputStream);
//        } else {
//            throw new UnsupportedOperationException("不支持的压缩格式: " + fileName);
//        }
//    }
//    /**
//     * 解析 ZIP
//     */
//    private static List<ArchiveFileInfo> parseZip(InputStream inputStream) throws IOException {
//        List<ArchiveFileInfo> files = new ArrayList<>();
//
//        try (ZipArchiveInputStream zipInput = new ZipArchiveInputStream(inputStream, "UTF-8", true, true)) {
//            ArchiveEntry entry;
//            while ((entry = zipInput.getNextEntry()) != null) {
//                files.add(buildFileInfo(entry));
//            }
//        }
//
//        return files;
//    }
//    /**
//     * 解析 RAR
//     */
//    private static List<ArchiveFileInfo> parseRar(InputStream inputStream) throws Exception {
//        List<ArchiveFileInfo> files = new ArrayList<>();
//
//        // RAR 需要先保存到临时文件
//        Path tempFile = Files.createTempFile("archive_", ".rar");
//        try {
//            Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
//
//            try (Archive archive = new Archive(tempFile.toFile())) {
//                FileHeader fileHeader;
//                while ((fileHeader = archive.nextFileHeader()) != null) {
//                    files.add(buildRarFileInfo(fileHeader));
//                }
//            }
//        } finally {
//            Files.deleteIfExists(tempFile);
//        }
//
//        return files;
//    }
//    /**
//     * 解析 7Z
//     */
//    private static List<ArchiveFileInfo> parse7z(InputStream inputStream) throws Exception {
//        List<ArchiveFileInfo> files = new ArrayList<>();
//
//        // 7z 需要先保存到临时文件
//        Path tempFile = Files.createTempFile("archive_", ".7z");
//        try {
//            Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
//
//            try (SevenZFile sevenZFile = new SevenZFile(tempFile.toFile())) {
//                SevenZArchiveEntry entry;
//                while ((entry = sevenZFile.getNextEntry()) != null) {
//                    files.add(build7zFileInfo(entry));
//                }
//            }
//        } finally {
//            Files.deleteIfExists(tempFile);
//        }
//
//        return files;
//    }
//    /**
//     * 解析 TAR
//     */
//    private static List<ArchiveFileInfo> parseTar(InputStream inputStream) throws IOException {
//        List<ArchiveFileInfo> files = new ArrayList<>();
//
//        try (TarArchiveInputStream tarInput = new TarArchiveInputStream(inputStream)) {
//            ArchiveEntry entry;
//            while ((entry = tarInput.getNextEntry()) != null) {
//                files.add(buildFileInfo(entry));
//            }
//        }
//
//        return files;
//    }
//    /**
//     * 解析 TAR.GZ
//     */
//    private static List<ArchiveFileInfo> parseTarGz(InputStream inputStream) throws IOException {
//        try (GzipCompressorInputStream gzipInput = new GzipCompressorInputStream(inputStream)) {
//            return parseTar(gzipInput);
//        }
//    }
//    /**
//     * 解析 TAR.BZ2
//     */
//    private static List<ArchiveFileInfo> parseTarBz2(InputStream inputStream) throws IOException {
//        try (BZip2CompressorInputStream bz2Input = new BZip2CompressorInputStream(inputStream)) {
//            return parseTar(bz2Input);
//        }
//    }
//    /**
//     * 构建文件信息（通用 ArchiveEntry）
//     */
//    private static ArchiveFileInfo buildFileInfo(ArchiveEntry entry) {
//        String path = entry.getName();
//        String name = getFileName(path);
//        String extension = getExtension(name);
//
//        return ArchiveFileInfo.builder()
//                .name(name)
//                .path(path)
//                .isDirectory(entry.isDirectory())
//                .size(entry.getSize())
//                .compressedSize(null)
//                .modifyTime(entry.getLastModifiedDate() != null ? entry.getLastModifiedDate().getTime() : null)
//                .extension(extension)
//                .compressionRatio(null)
//                .build();
//    }
//    /**
//     * 构建 RAR 文件信息
//     */
//    private static ArchiveFileInfo buildRarFileInfo(FileHeader header) {
//        String path = header.getFileName();
//        String name = getFileName(path);
//        String extension = getExtension(name);
//        long size = header.getUnpSize();
//        long compressedSize = header.getPackSize();
//
//        return ArchiveFileInfo.builder()
//                .name(name)
//                .path(path)
//                .isDirectory(header.isDirectory())
//                .size(size)
//                .compressedSize(compressedSize)
//                .modifyTime(header.getMTime() != null ? header.getMTime().getTime() : null)
//                .extension(extension)
//                .compressionRatio(size > 0 ? (1 - (double) compressedSize / size) * 100 : null)
//                .build();
//    }
//    /**
//     * 构建 7Z 文件信息
//     */
//    private static ArchiveFileInfo build7zFileInfo(SevenZArchiveEntry entry) {
//        String path = entry.getName();
//        String name = getFileName(path);
//        String extension = getExtension(name);
//        long size = entry.getSize();
//        long compressedSize = entry.getCompressedSize();
//
//        return ArchiveFileInfo.builder()
//                .name(name)
//                .path(path)
//                .isDirectory(entry.isDirectory())
//                .size(size)
//                .compressedSize(compressedSize)
//                .modifyTime(entry.getLastModifiedDate() != null ? entry.getLastModifiedDate().getTime() : null)
//                .extension(extension)
//                .compressionRatio(size > 0 ? (1 - (double) compressedSize / size) * 100 : null)
//                .build();
//    }
//    /**
//     * 获取统计信息
//     */
//    public static Map<String, Object> getArchiveStats(List<ArchiveFileInfo> files) {
//        long fileCount = files.stream().filter(f -> !f.getIsDirectory()).count();
//        long folderCount = files.stream().filter(ArchiveFileInfo::getIsDirectory).count();
//        long totalSize = files.stream()
//                .filter(f -> !f.getIsDirectory())
//                .mapToLong(f -> f.getSize() != null ? f.getSize() : 0)
//                .sum();
//
//        Map<String, Object> stats = new HashMap<>();
//        stats.put("fileCount", fileCount);
//        stats.put("folderCount", folderCount);
//        stats.put("totalSize", totalSize);
//        stats.put("totalCount", files.size());
//
//        return stats;
//    }
//    /**
//     * 检测压缩包类型
//     */
//    public static String detectArchiveType(String fileName) {
//        String lower = fileName.toLowerCase();
//        if (lower.endsWith(".zip")) return "ZIP";
//        if (lower.endsWith(".rar")) return "RAR";
//        if (lower.endsWith(".7z")) return "7Z";
//        if (lower.endsWith(".tar")) return "TAR";
//        if (lower.endsWith(".tar.gz") || lower.endsWith(".tgz")) return "TAR.GZ";
//        if (lower.endsWith(".tar.bz2")) return "TAR.BZ2";
//        return "UNKNOWN";
//    }
//    /**
//     * 获取文件名
//     */
//    private static String getFileName(String path) {
//        if (path == null || path.isEmpty()) {
//            return "";
//        }
//        int lastSlash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
//        return lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
//    }
//    /**
//     * 获取扩展名
//     */
//    private static String getExtension(String fileName) {
//        if (fileName == null || fileName.isEmpty()) {
//            return "";
//        }
//        int lastDot = fileName.lastIndexOf('.');
//        return lastDot > 0 ? fileName.substring(lastDot + 1).toLowerCase() : "";
//    }
//}
