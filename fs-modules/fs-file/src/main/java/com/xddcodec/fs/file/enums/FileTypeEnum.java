package com.xddcodec.fs.file.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 文件类型枚举
 *
 * @Author: xddcode
 * @Date: 2024/12/26
 */
@Getter
public enum FileTypeEnum {

    /**
     * 图片类型
     */
    IMAGE("image", "图片", Arrays.asList(
            "jpg", "jpeg", "png", "gif", "bmp", "webp", "svg",
            "JPG", "JPEG", "PNG", "GIF", "BMP", "WEBP", "SVG"
    )),

    /**
     * 视频类型
     */
    VIDEO("video", "视频", Arrays.asList(
            "mp4", "avi", "mkv", "mov", "wmv", "flv", "webm",
            "MP4", "AVI", "MKV", "MOV", "WMV", "FLV", "WEBM"
    )),

    /**
     * 音频类型
     */
    AUDIO("audio", "音频", Arrays.asList(
            "mp3", "wav", "flac", "aac", "ogg", "m4a", "wma",
            "MP3", "WAV", "FLAC", "AAC", "OGG", "M4A", "WMA"
    )),

    /**
     * 文档类型
     */
    DOCUMENT("document", "文档", Arrays.asList(
            "doc", "docx", "pdf", "txt", "xls", "xlsx", "ppt", "pptx",
            "DOC", "DOCX", "PDF", "TXT", "XLS", "XLSX", "PPT", "PPTX"
    )),

    /**
     * 其他类型（不属于以上任何类型）
     */
    OTHER("other", "其他", null);

    /**
     * 英文标识
     */
    private final String code;

    /**
     * 中文名称
     */
    private final String name;

    /**
     * 支持的文件后缀列表（文件夹和其他类型为null）
     */
    private final List<String> suffixes;

    FileTypeEnum(String code, String name, List<String> suffixes) {
        this.code = code;
        this.name = name;
        this.suffixes = suffixes;
    }

    /**
     * 根据类型标识获取枚举
     *
     * @param type 类型标识（支持英文code或中文name）
     * @return 文件类型枚举，如果未匹配则返回null
     */
    public static FileTypeEnum fromType(String type) {
        if (type == null || type.trim().isEmpty()) {
            return null;
        }

        String trimmedType = type.trim().toLowerCase();

        for (FileTypeEnum fileType : values()) {
            if (fileType.getCode().equalsIgnoreCase(trimmedType)
                    || fileType.getName().equals(type.trim())) {
                return fileType;
            }
        }

        return null;
    }

    /**
     * 根据文件后缀判断文件类型
     *
     * @param suffix 文件后缀（带或不带点都可以）
     * @return 文件类型枚举
     */
    public static FileTypeEnum fromSuffix(String suffix) {
        if (suffix == null || suffix.trim().isEmpty()) {
            return OTHER;
        }

        // 去除前缀的点
        String normalizedSuffix = suffix.trim().replace(".", "");

        for (FileTypeEnum fileType : values()) {
            // 跳过 FOLDER 和 OTHER
            if (fileType.getSuffixes() != null
                    && fileType.getSuffixes().contains(normalizedSuffix)) {
                return fileType;
            }
        }

        return OTHER;
    }

    /**
     * 获取所有已知的文件后缀（用于排除"其他"类型）
     *
     * @return 所有已知后缀的列表
     */
    public static List<String> getAllKnownSuffixes() {
        return Stream.of(IMAGE, VIDEO, AUDIO, DOCUMENT)
                .filter(type -> type.getSuffixes() != null)
                .flatMap(type -> type.getSuffixes().stream())
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * 判断是否为其他类型
     *
     * @return true-其他类型
     */
    public boolean isOther() {
        return this == OTHER;
    }

    /**
     * 判断某个后缀是否属于当前类型
     *
     * @param suffix 文件后缀
     * @return true-属于当前类型
     */
    public boolean containsSuffix(String suffix) {
        if (suffix == null || this.suffixes == null) {
            return false;
        }
        String normalizedSuffix = suffix.trim().replace(".", "");
        return this.suffixes.contains(normalizedSuffix);
    }
}
