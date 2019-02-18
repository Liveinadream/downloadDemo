package com.example.zhangtianning.download.service;

import android.app.DownloadManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.widget.Toast;

import com.example.zhangtianning.download.UpdateActivity2;
import com.example.zhangtianning.download.dao.DownloadFileInfo;
import com.example.zhangtianning.download.daoutils.DBDaoImpl;
import com.example.zhangtianning.download.utils.LogUtils;
import com.example.zhangtianning.download.utils.OpenApk;

import java.io.File;

/**
 * Created by 冒险者ztn on 2017/10/16.
 */

public class DownloadService extends Service implements DownloadInterface {
    private static final String TAG = DownloadService.class.getSimpleName();
    /**
     * 文件下载目录
     */
    private static final String FILE_DOWNLOAD_DIR = "/knrt/download/";
    private DownloadManager downloadManager;
    private long requestId;

    private ContentResolver resolver;
    private Context context;
    private static String URL = "url";
    private static String FILENAME = "fileName";
    private String url = "";
    private String fileName = "";
    private Binder serviceBinder = new DownLoadServiceBinder();
    DownloadFileInfo downloadFileInfo;//文件信息
    private int downloadNum = 0;
    UpdateActivity2 updataActivity;
    private int state = 0;


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            url = intent.getStringExtra(URL);
            fileName = intent.getStringExtra(FILENAME);
        } else {
            LogUtils.E(TAG, "没有传递内容");
            return Service.START_NOT_STICKY;
        }
        context = this;
        try {
            updataActivity = (UpdateActivity2) context;
            updataActivity.downloadService = this;
        } catch (ClassCastException ignore) {

        }
        downloadFileInfo = DBDaoImpl.getInstance(context).getDownloadFileInfoWithUrl(url);

        startDownload();
        registerReceiver(receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));


        return Service.START_STICKY;
    }

    private void startDownload() {
//创建下载任务
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        //移动网络情况下是否允许漫游
        request.setAllowedOverRoaming(false);

        //在通知栏中显示，默认就是显示的
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
        request.setTitle("新版本Apk");
        request.setDescription("Apk Downloading");
        request.setVisibleInDownloadsUi(true);
        request.addRequestHeader("Accept-Language", "zh-cn");
        request.addRequestHeader("UA-CPU", "x86");
        request.addRequestHeader("AAccept-Encoding", "gzip");
        request.addRequestHeader("Connection", "close");
        request.addRequestHeader("User-Agent", "Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 5.1");

        long downloadSize = downloadFileInfo.getHadDownloadSize();
        long fileLength = 0;

        //设置下载的路径与文件名
        request.setDestinationInExternalPublicDir(FILE_DOWNLOAD_DIR, fileName);

        //获取DownloadManager
        downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        //将下载请求加入下载队列，加入下载队列后会给该任务返回一个long型的id，通过该id可以取消任务，重启任务、获取下载的文件等等
        requestId = downloadManager.enqueue(request);

    }

    @Override
    public String getProgress() {
        return getBytesAndStatus(requestId)[0] / 1024 + "kb/ " + getBytesAndStatus(requestId)[1] / 1024 + "kb";
    }

    @Override
    public long getFileSize() {
        return getBytesAndStatus(requestId)[1];
    }

    @Override
    public long getDownloadedSize() {
        return getBytesAndStatus(requestId)[0];
    }

    @Override
    public long getDownloadId() {
        return requestId;
    }

    @Override
    public Cursor getCursor(long requestId) {
        DownloadManager.Query query = new DownloadManager.Query();
        return downloadManager.query(query.setFilterById(requestId));
    }

    public int getDownloadState() {
        return state;
    }

    /**
     * 通过query查询下载状态，包括已下载数据大小，总大小，下载状态
     *
     * @param downloadId
     * @return
     */
    private int[] getBytesAndStatus(long downloadId) {
        int[] bytesAndStatus = new int[]{
                -1, -1, 0
        };
        DownloadManager.Query query = new DownloadManager.Query().setFilterById(downloadId);
        Cursor cursor = null;
        try {
            cursor = downloadManager.query(query);
            if (cursor != null && cursor.moveToFirst()) {
                //已经下载文件大小
                bytesAndStatus[0] = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                //下载文件的总大小
                bytesAndStatus[1] = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                //下载状态
                bytesAndStatus[2] = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return bytesAndStatus;
    }


    //广播监听下载的各个状态
    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            checkStatus();
        }
    };


    //检查下载状态
    private void checkStatus() {
        DownloadManager.Query query = new DownloadManager.Query();
        //通过下载的id查找
        query.setFilterById(requestId);
        Cursor c = downloadManager.query(query);
        if (c.moveToFirst()) {
            int status = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS));
            switch (status) {
                //下载暂停
                case DownloadManager.STATUS_PAUSED:
                    state = DownloadManager.STATUS_PAUSED;

                    break;
                //下载延迟
                case DownloadManager.STATUS_PENDING:
                    state = DownloadManager.STATUS_PENDING;

                    break;
                //正在下载
                case DownloadManager.STATUS_RUNNING:

                    state = DownloadManager.STATUS_RUNNING;
                    downloadFileInfo.setHadDownloadSize((long) getBytesAndStatus(requestId)[0]);
                    break;
                //下载完成
                case DownloadManager.STATUS_SUCCESSFUL:
                    state = DownloadManager.STATUS_SUCCESSFUL;
                    //下载完成安装APK
                    downloadFileInfo.setHadDownloadSize((long) getBytesAndStatus(requestId)[1]);
                    OpenApk.getInstance().openFile(context, new File(downloadFileInfo.getFilePath()));
                    break;
                //下载失败
                case DownloadManager.STATUS_FAILED:
                    state = DownloadManager.STATUS_FAILED;
                    Toast.makeText(context, "下载失败", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
        c.close();
    }


    public class DownLoadServiceBinder extends Binder {
        public DownloadService getService() {
            return DownloadService.this;
        }
    }
}
