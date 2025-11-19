package com.xddcodec.fs.storage.plugin.cos.config;

import lombok.Data;

@Data
public class CosConfig {

    private String accessKey;

    private String secretKey;

    private String bucket;

    private String region;
}
