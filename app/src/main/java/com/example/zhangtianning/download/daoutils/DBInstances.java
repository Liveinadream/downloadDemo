package com.example.zhangtianning.download.daoutils;

import com.example.zhangtianning.download.dao.DownloadFileInfo;

import java.util.List;

/**
 * 接口
 * Created by 冒险者ztn on 2017/9/11.
 */

public interface DBInstances {

    /**
     * 获取所有下载内容信息
     */
    List<DownloadFileInfo> getDownloadFileInfo();

    /**
     * 根据url获取对应下载内容信息
     */
    DownloadFileInfo getDownloadFileInfoWithUrl(String url);


    /**
     * 保存下载内容信息
     */

    void setDownloadFileInfo(DownloadFileInfo downloadFileInfo);


    /**
     * 清除数据
     */

    void clearDownloadFileInfo();


    /**
     * 是否有完整文件
     *
     * @param downloadFileInfo
     * @return
     */
    Boolean hasCompleteFile(DownloadFileInfo downloadFileInfo);

    /**
     * 初始化一条数据
     *
     * @param url
     * @return
     */
    DownloadFileInfo initBaseDownloadFileInfo(String url);


    /**
     * 更新一条数据
     *
     * @param downloadFileInfo
     */
    void Updata(DownloadFileInfo downloadFileInfo);
}
