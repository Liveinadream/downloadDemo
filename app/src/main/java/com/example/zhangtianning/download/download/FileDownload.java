package com.example.zhangtianning.download.download;

import android.app.Activity;
import android.content.Context;
import android.os.Message;
import android.text.TextUtils;
import android.widget.Toast;

import com.example.zhangtianning.download.dao.DownloadFileInfo;
import com.example.zhangtianning.download.daoutils.DBDaoImpl;
import com.example.zhangtianning.download.listener.FileDownloadListener;
import com.example.zhangtianning.download.utils.LogUtils;
import com.example.zhangtianning.download.utils.OpenApk;
import com.example.zhangtianning.download.utils.ToastUtils;

import java.io.File;

/**
 * 文件下载类
 */

public class FileDownload {

    /**
     * 文件下载监听
     */
    private FileDownloadListener mFileDownloadListener;

    private DownloadThread downloadThread;
    /**
     * 是否是debug模式
     */
    private static boolean mIsDebugModel;

    /**
     * 开始下载
     *
     * @param downloadurl 文件url地址
     */
    public void start(Activity activity, String downloadurl) {
        start(activity, DBDaoImpl.getInstance(activity).initBaseDownloadFileInfo(downloadurl));
    }

    /**
     * 已经有记录开始下载
     *
     * @param downloadFileInfo 下载信息
     */
    public void start(Activity activity, DownloadFileInfo downloadFileInfo) {
        downloadThread = new DownloadThread(downloadFileInfo, mFileDownloadListener, activity);
        downloadThread.start();
    }

    public void stop() {
        if (downloadThread != null) {
            downloadThread.stopDownload();
        }
    }

    public DownloadFileInfo downloadFileInfo() {
        return downloadThread.getDownloadFileInfo();
    }

    public void setFileDownloadListener(FileDownloadListener fileDownloadListener) {
        mFileDownloadListener = fileDownloadListener;
    }

    public static void setDebugModel(boolean isDebugModel) {
        mIsDebugModel = isDebugModel;
    }

    public static boolean getDebugModel() {
        return mIsDebugModel;
    }
}
