package com.xddcodec.fs.fs.framework.ws.core;

import lombok.Data;

@Data
public class UploadCommand {
    private UploadCommandAction action;
    private String taskId;
}
