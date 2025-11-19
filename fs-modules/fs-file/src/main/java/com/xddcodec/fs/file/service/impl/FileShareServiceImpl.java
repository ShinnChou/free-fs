package com.xddcodec.fs.file.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.xddcodec.fs.file.domain.FileInfo;
import com.xddcodec.fs.file.domain.FileShare;
import com.xddcodec.fs.file.domain.dto.CreateShareCmd;
import com.xddcodec.fs.file.domain.dto.VerifyShareCodeCmd;
import com.xddcodec.fs.file.domain.qry.FileQry;
import com.xddcodec.fs.file.domain.qry.FileSharePageQry;
import com.xddcodec.fs.file.domain.qry.FileShareQry;
import com.xddcodec.fs.file.domain.vo.FileShareThinVO;
import com.xddcodec.fs.file.domain.vo.FileShareVO;
import com.xddcodec.fs.file.domain.vo.FileVO;
import com.xddcodec.fs.file.mapper.FileShareMapper;
import com.xddcodec.fs.file.service.FileInfoService;
import com.xddcodec.fs.file.service.FileShareItemService;
import com.xddcodec.fs.file.service.FileShareService;
import com.xddcodec.fs.framework.common.domain.PageResult;
import com.xddcodec.fs.framework.common.exception.BusinessException;
import com.xddcodec.fs.framework.common.utils.StringUtils;
import io.github.linpeilie.Converter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static com.xddcodec.fs.file.domain.table.FileShareTableDef.FILE_SHARE;

/**
 * 文件分享服务实现类
 *
 * @Author: xddcode
 * @Date: 2025/10/30 10:02
 */
@Service
@RequiredArgsConstructor
public class FileShareServiceImpl extends ServiceImpl<FileShareMapper, FileShare> implements FileShareService {

    private final FileInfoService fileInfoService;

    private final FileShareItemService fileShareItemService;

    private final Converter converter;

    @Value("${fs.share.domain:http://localhost:3000}")
    private String shareDomain;

    @Override
    public PageResult<FileShareVO> getMyPages(FileSharePageQry qry) {
        Integer pageNumber = qry.getPage() == null ? 1 : qry.getPage();
        Integer pageSize = qry.getPageSize() == null ? 10 : qry.getPageSize();
        Page<FileShare> page = Page.of(pageNumber, pageSize);

        String userId = StpUtil.getLoginIdAsString();

        QueryWrapper wrapper = new QueryWrapper();
        wrapper.where(FILE_SHARE.USER_ID.eq(userId));

        if (StringUtils.isNotEmpty(qry.getKeyword())) {
            String keyword = "%" + qry.getKeyword().trim() + "%";
            wrapper.and(FILE_SHARE.SHARE_NAME.like(keyword));
        }

        if (StringUtils.isEmpty(qry.getOrderBy()) || StringUtils.isEmpty(qry.getOrderDirection())) {
            wrapper.orderBy(FILE_SHARE.CREATED_AT.desc());
        } else {
            String orderBy = StrUtil.toUnderlineCase(qry.getOrderBy());
            boolean isAsc = "ASC".equalsIgnoreCase(qry.getOrderDirection());
            wrapper.orderBy(orderBy, isAsc);
        }

        page = this.page(page, wrapper);
        long total = page.getTotalRow();
        List<FileShare> fileShares = page.getRecords();
        List<FileShareVO> fileShareVOS = converter.convert(fileShares, FileShareVO.class);
        fileShareVOS.forEach(vo -> {
            vo.setShareUrl(shareDomain + "/s/" + vo.getId());
            vo.setIsPermanent(vo.getExpireTime() == null);
        });
        return PageResult.success(fileShareVOS, total);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileShareVO createShare(CreateShareCmd cmd) {
        String userId = StpUtil.getLoginIdAsString();
        FileShare share = new FileShare();
        share.setUserId(userId);
        share.setViewCount(0);
        share.setDownloadCount(0);

        if (StrUtil.isNotBlank(cmd.getShareName())) {
            share.setShareName(cmd.getShareName());
        } else {
            FileInfo fileInfo = fileInfoService.getById(cmd.getFileIds().get(0));
            // 默认取第一个文件名，如果是多个文件则显示第一个文件名+"等{数量}"文件
            if (cmd.getFileIds().size() > 1) {
                share.setShareName(fileInfo.getDisplayName() + "等" + cmd.getFileIds().size() + "个文件");
            } else {
                share.setShareName(fileInfo.getDisplayName());
            }
        }
        if (cmd.getExpireType() == null) {
            share.setExpireTime(null);
        } else if (cmd.getExpireType() == 4) {
            share.setExpireTime(cmd.getExpireTime());
        } else {
            share.setExpireTime(calculateExpireTime(cmd.getExpireType()));
        }

        if (cmd.getNeedShareCode()) {
            share.setShareCode(RandomUtil.randomString(4));
        }

        share.setMaxViewCount(cmd.getMaxViewCount());
        share.setMaxDownloadCount(cmd.getMaxDownloadCount());

        this.save(share);

        fileShareItemService.saveShareItems(share.getId(), cmd.getFileIds());

        return buildShareVO(share);
    }

    /**
     * 构建分享VO ✨
     */
    private FileShareVO buildShareVO(FileShare share) {
        FileShareVO vo = converter.convert(share, FileShareVO.class);
        // 分享链接
        vo.setShareUrl(shareDomain + "/s/" + share.getId());

        // 是否永久有效
        vo.setIsPermanent(share.getExpireTime() == null);

        return vo;
    }

    /**
     * 计算过期时间
     */
    private static LocalDateTime calculateExpireTime(Integer expireType) {

        LocalDateTime now = LocalDateTime.now();
        return switch (expireType) {
            case 1 -> now.plusDays(1);
            case 2 -> now.plusDays(7);
            case 3 -> now.plusDays(30);
            default -> now.plusDays(7);
        };
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelShare(String shareId) {
        String userId = StpUtil.getLoginIdAsString();

        FileShare share = this.getById(shareId);
        if (share == null) {
            throw new BusinessException("该分享不存在");
        }

        if (!share.getUserId().equals(userId)) {
            throw new BusinessException("无权取消此分享");
        }

        this.removeById(share);

        fileShareItemService.removeByShareId(shareId);
    }

    @Override
    public boolean verifyShareCode(VerifyShareCodeCmd cmd) {
        FileShare share = this.getById(cmd.getShareId());
        if (share == null) {
            throw new BusinessException("该分享不存在");
        }

        if (!share.getShareCode().equals(cmd.getShareCode())) {
            throw new BusinessException("提取码不正确");
        }
        return true;
    }

    @Override
    public FileShareThinVO getFileShareThinVO(String shareId) {
        FileShare fileShare = this.getById(shareId);
        if (fileShare == null) {
            throw new BusinessException("该分享不存在或已删除");
        }
        FileShareThinVO vo = converter.convert(fileShare, FileShareThinVO.class);
        vo.setHasCheckCode(StringUtils.isNotEmpty(fileShare.getShareCode()));
        // 查询有几个文件
        vo.setFileCount(fileShareItemService.countByShareId(shareId));
        return vo;
    }

    @Override
    public List<FileVO> getShareFileItems(FileShareQry qry) {
        FileShare fileShare = this.getById(qry.getShareId());
        if (fileShare == null) {
            throw new BusinessException("该分享不存在或已删除");
        }
        if (StringUtils.isNotEmpty(qry.getParentId())) {
            // 若有父文件ID参数, 则需要查询子数据集
            FileQry fileQry = new FileQry();
            fileQry.setParentId(qry.getParentId());
            return fileInfoService.getList(fileQry);
        }
        // 获取分享明细
        List<String> shareFileIds = fileShareItemService.getShareFileIds(qry.getShareId());
        return fileInfoService.getByFileIds(shareFileIds);
    }
}
