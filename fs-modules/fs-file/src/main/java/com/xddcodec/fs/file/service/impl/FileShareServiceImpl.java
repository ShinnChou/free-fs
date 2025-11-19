package com.xddcodec.fs.file.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.xddcodec.fs.file.domain.FileInfo;
import com.xddcodec.fs.file.domain.FileShare;
import com.xddcodec.fs.file.domain.dto.CreateFileShareAccessRecordCmd;
import com.xddcodec.fs.file.domain.dto.CreateShareCmd;
import com.xddcodec.fs.file.domain.dto.VerifyShareCodeCmd;
import com.xddcodec.fs.file.domain.event.CreateFileShareAccessRecordEvent;
import com.xddcodec.fs.file.domain.qry.FileQry;
import com.xddcodec.fs.file.domain.qry.FileShareQry;
import com.xddcodec.fs.file.domain.vo.FileShareThinVO;
import com.xddcodec.fs.file.domain.vo.FileShareVO;
import com.xddcodec.fs.file.domain.vo.FileVO;
import com.xddcodec.fs.file.mapper.FileShareMapper;
import com.xddcodec.fs.file.service.FileInfoService;
import com.xddcodec.fs.file.service.FileShareItemService;
import com.xddcodec.fs.file.service.FileShareService;
import com.xddcodec.fs.framework.common.exception.BusinessException;
import com.xddcodec.fs.framework.common.utils.Ip2RegionUtils;
import com.xddcodec.fs.framework.common.utils.IpUtils;
import com.xddcodec.fs.framework.common.utils.StringUtils;
import com.xddcodec.fs.log.domain.event.CreateLoginLogEvent;
import io.github.linpeilie.Converter;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
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

    private final ApplicationEventPublisher eventPublisher;

    @Override
    public List<FileShareVO> getList(FileShareQry qry) {
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
        List<FileShare> fileShares = this.list(wrapper);
        return converter.convert(fileShares, FileShareVO.class);
    }

    @Override
    public FileShareVO getDetail(String shareId) {
        FileShare share = this.getById(shareId);
        if (share == null) {
            throw new BusinessException("该分享不存在");
        }
        return buildShareVO(share);
    }

    /**
     * 构建分享VO
     */
    private FileShareVO buildShareVO(FileShare share) {
        FileShareVO vo = converter.convert(share, FileShareVO.class);
        // 是否永久有效
        vo.setIsPermanent(share.getExpireTime() == null);
        // 查询有几个文件
        vo.setFileCount(fileShareItemService.countByShareId(share.getId()));
        // 判断是否到期
        return vo;
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
        if (cmd.getExpireType() == 4) {
            share.setExpireTime(null);
        } else if (cmd.getExpireType() == 3) {
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
     * 计算过期时间
     */
    private static LocalDateTime calculateExpireTime(Integer expireType) {
        LocalDateTime now = LocalDateTime.now();
        return switch (expireType) {
            case 1 -> now.plusDays(7);
            case 2 -> now.plusDays(30);
            default -> now.plusDays(7);
        };
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelShares(List<String> ids) {
        for (String id : ids) {
            FileShare share = this.getById(id);
            if (share == null) {
                continue;
            }
            if (share.getUserId().equals(StpUtil.getLoginIdAsString())) {
                this.removeById(id);
                fileShareItemService.removeByShareId(id);
            }
        }
    }

    @Override
    public boolean verifyShareCode(VerifyShareCodeCmd cmd) {
        // 故意延迟200ms，增加暴力破解成本
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        FileShareVO share = this.getDetail(cmd.getShareId());
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
            throw new BusinessException("该分享不存在");
        }
        FileShareThinVO vo = converter.convert(fileShare, FileShareThinVO.class);
        vo.setHasCheckCode(StringUtils.isNotEmpty(fileShare.getShareCode()));
        // 查询有几个文件
        vo.setFileCount(fileShareItemService.countByShareId(shareId));
        // 判断是否到期
        LocalDateTime expireTime = fileShare.getExpireTime();
        if (expireTime == null) {
            // 永久有效
            vo.setIsExpire(false);
        } else {
            LocalDateTime now = LocalDateTime.now();
            vo.setIsExpire(now.isAfter(expireTime));
        }
        return vo;
    }

    @Override
    public List<FileVO> getShareFileItems(String shareId, String parentId) {
        FileShare fileShare = this.getById(shareId);
        if (fileShare == null) {
            throw new BusinessException("该分享不存在或已删除");
        }
        if (StringUtils.isNotEmpty(parentId)) {
            // 若有父文件ID参数, 则需要查询子数据集
            FileQry fileQry = new FileQry();
            fileQry.setParentId(parentId);
            return fileInfoService.getList(fileQry);
        }
        // 获取分享明细
        List<String> shareFileIds = fileShareItemService.getShareFileIds(shareId);

        //记录访问日志
        recordShareAccessLog(shareId);
        //访问计数 + 1
//        incrementViewCount(qry.getShareId());

        return fileInfoService.getByFileIds(shareFileIds);
    }

    /**
     * 记录分享访问日志
     *
     * @param shareId
     */
    private void recordShareAccessLog(String shareId) {
        String ip = IpUtils.getIpAddr();
        String address = Ip2RegionUtils.search(ip);
        String browser = IpUtils.getBrowser();
        String os = IpUtils.getOs();
        CreateFileShareAccessRecordCmd cmd = new CreateFileShareAccessRecordCmd();
        cmd.setShareId(shareId);
        cmd.setAccessIp(ip);
        cmd.setAccessAddress(address);
        cmd.setBrowser(browser);
        cmd.setOs(os);
        eventPublisher.publishEvent(new CreateFileShareAccessRecordEvent(this, cmd));
    }

    /**
     * 原子递增访问次数
     */
//    private void incrementViewCount(String shareId) {
//        String key = VIEW_COUNT_KEY + shareId;
//        Long count = redisTemplate.opsForValue().increment(key);
//
//        if (count == null) {
//            return;
//        }
//        // 第一次访问时设置过期时间（与分享有效期一致，或者永不过期）
//        if (count == 1) {
//            // 选项1: 永不过期
//            // redisTemplate.persist(key);
//
//            // 选项2: 与分享有效期同步（推荐）
//            setExpireTimeByShareExpiry(shareId, key);
//        }
//    }
}
