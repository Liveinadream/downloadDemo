package com.example.zhangtianning.download;

import android.app.DownloadManager;
import android.app.IntentService;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.Message;
import android.os.Parcelable;
import android.os.ResultReceiver;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.widget.Toast;

import com.example.zhangtianning.download.dao.DownloadFileInfo;
import com.example.zhangtianning.download.daoutils.DBDaoImpl;
import com.example.zhangtianning.download.listener.FileDownloadListener;
import com.example.zhangtianning.download.utils.FileUtils;
import com.example.zhangtianning.download.utils.LogUtils;
import com.example.zhangtianning.download.utils.OpenApk;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

/**
 * Created by zhangtianning on 2017/9/11
 */

public class DownloadService extends Service {

    private static final String TAG = DownloadService.class.getSimpleName();
    /**
     * 文件下载目录
     */
    private static final String FILE_DOWNLOAD_DIR = "/knrt/download/";

    /**
     * 文件下载时的临时文件目录
     */
    private static final String FILE_DOWNLOAD_TEMP_DIR = "/knrt/temp/";

    private DownloadManager dm;
    private long enqueue;
    private int requestCode = (int) SystemClock.uptimeMillis();
    private BroadcastReceiver receiver;
    //    private String url = "http://d.koudai.com/com.koudai.weishop/1000f/weishop_1000f.apk";
    private String url = "";
    private static String DOWNLOADFILEINFO = "downloadFileInfo";
    private static String URL = "url";
    DownloadFileInfo downloadFileInfo;//文件信息
    private File tmpFile;
    Context context;


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {

        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        url = intent.getStringExtra(URL);
        context = this;

        downloadFileInfo = DBDaoImpl.getInstance(context).getDownloadFileInfoWithUrl(url);

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                intent = new Intent(Intent.ACTION_VIEW);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setDataAndType(Uri.fromFile(new File(Environment.getExternalStorageDirectory()
                                + "//knrt/temp/myApp.apk")),
                        "application/vnd.android.package-archive");
                startActivity(intent);
                stopSelf();
            }
        };

        registerReceiver(receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

        startDownload();
        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(receiver);
        super.onDestroy();
    }

    private void startDownload() {
        String basePath = FileUtils.createBasePath(FILE_DOWNLOAD_DIR);
        String tmpfileBasePath = FileUtils.createBasePath(FILE_DOWNLOAD_TEMP_DIR);


        dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        DownloadManager.Request request = new DownloadManager.Request(
                Uri.parse(url));


        request.addRequestHeader("Accept-Language", "zh-cn");
        request.addRequestHeader("UA-CPU", "x86");
        request.addRequestHeader("AAccept-Encoding", "gzip");
        request.addRequestHeader("Connection", "close");
        request.addRequestHeader("User-Agent", "Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 5.1");
        long downloadSize = downloadFileInfo.getHadDownloadSize();
        long fileLength = 0;
        String fileName = downloadFileInfo.getFileName();

        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI);

        request.setMimeType("application/vnd.android.package-archive");
//        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "myApp.apk");


        if (downloadSize > 0) {
            String tmpFilePath = Environment.getExternalStorageDirectory().getPath() + FILE_DOWNLOAD_TEMP_DIR + fileName;
            LogUtils.D("Tag", "临时文件的路径：" + tmpFilePath);
            fileLength = downloadFileInfo.getFileSize();
            request.addRequestHeader("Range",
                    "bytes=" + downloadFileInfo.getHadDownloadSize() + "-"
                            + downloadFileInfo.getFileSize());
            tmpFile = new File(tmpFilePath);
            if (!tmpFile.exists() || !tmpFile.isFile()) {
                tmpFile = FileUtils.createTempFile(tmpfileBasePath, fileName);
                downloadFileInfo.setTempFilePath(tmpFile.getAbsolutePath());
            }
            enqueue = dm.enqueue(request);
        } else {
            enqueue = dm.enqueue(request);
            DownloadManager.Query query = new DownloadManager.Query().setFilterById(enqueue);
            Cursor cursor = null;

            try {
                cursor = dm.query(query);
                if (cursor != null && cursor.moveToFirst()) {

                    //下载文件的总大小
                    fileLength = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));

                    //已经下载文件大小
//                    bytesAndStatus[0] = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
//
//                    //下载状态
//                    bytesAndStatus[2] = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));

                    if (fileName == null) {
                        String path = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_FILENAME)); // 获取文件路径
                        fileName = path.substring(path.lastIndexOf("/") + 1, path.length());
                    }
                }


            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
            downloadFileInfo.setFileSize(fileLength);

            if (fileName == null) {
                fileName = url.substring(url.lastIndexOf("/") + 1);
            }
            downloadFileInfo.setFileName(fileName);
            downloadFileInfo.setFilePath(basePath + File.separator + fileName);
            String tmpFilePath = Environment.getExternalStorageDirectory().getPath() + FILE_DOWNLOAD_TEMP_DIR + fileName;
            LogUtils.D("Tag", "临时文件的路径：" + tmpFilePath);
            tmpFile = FileUtils.createTempFile(tmpfileBasePath, fileName);
            downloadFileInfo.setTempFilePath(tmpFile.getAbsolutePath());
            DBDaoImpl.getInstance(context).Updata(downloadFileInfo);

            request.setDestinationInExternalPublicDir(FileUtils.createBasePath(FILE_DOWNLOAD_DIR),
                    downloadFileInfo.getFileName());
        }

    }

}