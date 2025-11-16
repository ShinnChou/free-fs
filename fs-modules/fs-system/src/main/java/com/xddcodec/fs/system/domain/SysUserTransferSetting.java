package com.xddcodec.fs.system.domain;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.xddcodec.fs.framework.orm.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 用户传输设置实体类
 *
 * @Author: xddcode
 * @Date: 2025/11/11 14:35
 */
@Data
@Table("sys_user_transfer_setting")
@EqualsAndHashCode(callSuper = true)
public class SysUserTransferSetting extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 自增id
     */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 文件下载位置
     */
    private String downloadLocation;

    /**
     * 是否默认该路径为下载路径，如果否则每次下载询问保存地址
     */
    private Integer isDefaultDownloadLocation;

    /**
     * 下载速率限制 单位：MB/S
     */
    private Integer downloadSpeedLimit;

    /**
     * 并发上传数量
     */
    private Integer concurrentUploadQuantity;

    /**
     * 并发下载数量
     */
    private Integer concurrentDownloadQuantity;

}
