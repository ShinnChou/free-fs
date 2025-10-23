package com.xddcodec.fs.storage.plugin.aliyunoss;

import lombok.Data;

@Data
public class AliyunOssConfig {

    private String endpoint;

    private String accessKey;

    private String secretKey;

    private String bucket;
}
