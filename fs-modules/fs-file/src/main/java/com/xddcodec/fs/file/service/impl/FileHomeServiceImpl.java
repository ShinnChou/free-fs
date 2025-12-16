package com.xddcodec.fs.file.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.xddcodec.fs.file.domain.FileInfo;
import com.xddcodec.fs.file.domain.qry.FileQry;
import com.xddcodec.fs.file.domain.vo.FileHomeVO;
import com.xddcodec.fs.file.domain.vo.FileVO;
import com.xddcodec.fs.file.service.FileHomeService;
import com.xddcodec.fs.file.service.FileInfoService;
import com.xddcodec.fs.file.service.FileShareService;
import com.xddcodec.fs.file.service.FileUserFavoritesService;
import com.xddcodec.fs.storage.plugin.core.context.StoragePlatformContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.xddcodec.fs.file.domain.table.FileInfoTableDef.FILE_INFO;
import static com.xddcodec.fs.file.domain.table.FileShareTableDef.FILE_SHARE;

@Service
@RequiredArgsConstructor
public class FileHomeServiceImpl implements FileHomeService {

    private final FileInfoService fileInfoService;

    private final FileShareService fileShareService;

    private final FileUserFavoritesService fileUserFavoritesService;

    @Override
    public FileHomeVO getFileHomes() {
        String userId = StpUtil.getLoginIdAsString();
        String storagePlatformSettingId = StoragePlatformContextHolder.getConfigId();
        FileHomeVO fileHomeVO = new FileHomeVO();
        List<FileInfo> fileInfoList = fileInfoService.list(new QueryWrapper()
                .where(FILE_INFO.USER_ID.eq(userId)
                        .and(FILE_INFO.STORAGE_PLATFORM_SETTING_ID.eq(storagePlatformSettingId))
                        .and(FILE_INFO.IS_DELETED.eq(false))
                ));
        long directoryCount = fileInfoList.stream()
                .filter(FileInfo::getIsDir)
                .count();
        long fileCount = fileInfoList.size() - directoryCount;
        fileHomeVO.setFileCount(fileCount);
        fileHomeVO.setDirectoryCount(directoryCount);

        // 统计已使用容量
        Long usedStorage = fileInfoService.calculateUsedStorage();
        fileHomeVO.setUsedStorage(usedStorage);

        //查询已分享数量
        Long shareCount = fileShareService.count(
                new QueryWrapper().where(FILE_SHARE.USER_ID.eq(userId))
        );
        fileHomeVO.setShareCount(shareCount);
        //查询收藏文件数量
        Long favoriteCount = fileUserFavoritesService.getFavoritesCount();
        fileHomeVO.setFavoriteCount(favoriteCount);

        //查询用户最近使用的文件
        FileQry fileQry = new FileQry();
        fileQry.setIsRecents(Boolean.TRUE);
        List<FileVO> recentFiles = fileInfoService.getList(fileQry);
        fileHomeVO.setRecentFiles(recentFiles);
        return fileHomeVO;
    }
}
