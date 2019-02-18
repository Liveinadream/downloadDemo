package com.example.zhangtianning.download.service;

import android.database.Cursor;

/**
 * Created by 冒险者ztn on 2017/10/16.
 *
 * 理论上从DownloadManager的下载数据库中可以得到所有下载信息,因为现在应用中只有一个更新的任务，
 * 所以
 */

public interface DownloadInterface {


    //获取进度条进度
    String getProgress();

    //获取下载文件大小
    long getFileSize();

    //获取已下载文件大小
    long getDownloadedSize();

    //获取下载文件ID
    long getDownloadId();

    Cursor getCursor(long requestId);

}
