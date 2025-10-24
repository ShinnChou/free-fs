package com.xddcodec.fs.storage.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class StorageActivePlatformsVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String settingId;

    private String platformIdentifier;

    private String platformName;

    private String platformIcon;
}
