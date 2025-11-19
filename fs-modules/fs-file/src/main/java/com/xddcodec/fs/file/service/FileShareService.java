package com.xddcodec.fs.file.service;

import com.mybatisflex.core.service.IService;
import com.xddcodec.fs.file.domain.FileShare;
import com.xddcodec.fs.file.domain.dto.CreateShareCmd;
import com.xddcodec.fs.file.domain.dto.VerifyShareCodeCmd;
import com.xddcodec.fs.file.domain.qry.FileSharePageQry;
import com.xddcodec.fs.file.domain.qry.FileShareQry;
import com.xddcodec.fs.file.domain.vo.FileShareThinVO;
import com.xddcodec.fs.file.domain.vo.FileShareVO;
import com.xddcodec.fs.file.domain.vo.FileVO;
import com.xddcodec.fs.framework.common.domain.PageResult;

import java.util.List;

/**
 * 文件分享服务接口
 *
 * @Author: xddcode
 * @Date: 2025/10/30 9:35
 */
public interface FileShareService extends IService<FileShare> {

    /**
     * 分页查询我的分享
     *
     * @param qry
     * @return
     */
    PageResult<FileShareVO> getMyPages(FileSharePageQry qry);

    /**
     * 创建分享
     *
     * @param cmd 创建分享参数
     */
    FileShareVO createShare(CreateShareCmd cmd);

    /**
     * 取消分享
     *
     * @param shareId 分享ID
     */
    void cancelShare(String shareId);

    /**
     * 校验提取码
     *
     * @param cmd
     */
    boolean verifyShareCode(VerifyShareCodeCmd cmd);

    /**
     * 获取文件分享页对象
     * @param shareId 分享id
     * @return vo
     */
    FileShareThinVO getFileShareThinVO(String shareId);

    List<FileVO> getShareFileItems(FileShareQry qry);
}
