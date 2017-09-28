package com.example.zhangtianning.download;

import android.app.DownloadManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemClock;
import android.support.annotation.Nullable;

import com.example.zhangtianning.download.dao.DownloadFileInfo;
import com.example.zhangtianning.download.daoutils.DBDaoImpl;
import com.example.zhangtianning.download.utils.FileUtils;
import com.example.zhangtianning.download.utils.LogUtils;
import com.example.zhangtianning.download.utils.WeakHandler;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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
    private DownloadChangeObserver downloadObserver;
    private BroadcastReceiver downLoadBroadcast;
    private ScheduledExecutorService scheduledExecutorService;
    private long requestId;
    private int requestCode = (int) SystemClock.uptimeMillis();
    private BroadcastReceiver receiver;
    //    private String url = "http://d.koudai.com/com.koudai.weishop/1000f/weishop_1000f.apk";
    private String url = "";
    private static String DOWNLOADFILEINFO = "downloadFileInfo";
    private static String URL = "url";
    DownloadFileInfo downloadFileInfo;//文件信息
    private File tmpFile;
    Context context;
    private DownloadBinder binder;
    public static final int STATE_START_DOWNLOAD = 0; // 开始下载
    public static final int STATE_DOWNLOADING = 1;   //  下载中
    public static final int STATE_FINISH_DOWNLOAD = 2;  // 下载完成
    public static final int STATE_FAIL_DOWNLOAD = 3; // 下载失败
    public static final int STATE_FAIL_DOWNLOAD_PAUSE = 4; // 下载暂停

    public int downloadState = -1; //最初的下载状态，没有下载任务。
    public MyWeakHandler myWeakHandler;
    private OnProgressListener onProgressListener;
    public static final int HANDLE_DOWNLOAD = 0x001;



    @Nullable
    @Override
    public IBinder onBind(Intent intent) {

        return null;
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        myWeakHandler = new MyWeakHandler(this);
        if (intent != null) {
            url = intent.getStringExtra(URL);
        } else {
            LogUtils.E(TAG, "没有传递内容");
            return Service.START_NOT_STICKY;
        }
        context = this;

        downloadFileInfo = DBDaoImpl.getInstance(context).getDownloadFileInfoWithUrl(url);

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                LogUtils.D(TAG, "receiver 的情况:path " + downloadFileInfo.getFilePath());
                setDownloadInfo(requestId);
//                getDownloadQuery(requestId);

                intent = new Intent(Intent.ACTION_VIEW);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setDataAndType(Uri.fromFile(new File(downloadFileInfo.getFilePath())),
                        "application/vnd.android.package-archive");
                startActivity(intent);
                stopSelf();
            }
        };

        registerReceiver(receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        startDownload();
        return Service.START_STICKY;
    }


    private void startDownload() {
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
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

        if (fileName == null) {
            fileName = url.substring(url.lastIndexOf("/") + 1);
            downloadFileInfo.setFileName(fileName);

        }

        request.setDestinationInExternalPublicDir(FILE_DOWNLOAD_DIR,
                downloadFileInfo.getFileName());

        if (downloadSize > 0) {
            String tmpFilePath = Environment.getExternalStorageDirectory().getPath() + FILE_DOWNLOAD_TEMP_DIR + fileName;
            LogUtils.D("Tag", "临时文件的路径：" + tmpFilePath);
            request.addRequestHeader("Range",
                    "bytes=" + downloadFileInfo.getHadDownloadSize() + "-"
                            + downloadFileInfo.getFileSize());
            tmpFile = new File(tmpFilePath);
            if (!tmpFile.exists() || !tmpFile.isFile()) {
                tmpFile = FileUtils.createTempFile(tmpfileBasePath, fileName);
                downloadFileInfo.setTempFilePath(tmpFile.getAbsolutePath());
            }
            requestId = dm.enqueue(request);
        } else {
            requestId = dm.enqueue(request);

            DownloadManager.Query query = new DownloadManager.Query().setFilterById(requestId);
            Cursor cursor = null;

            try {
                cursor = dm.query(query);
                if (cursor != null && cursor.moveToFirst()) {

                    //下载文件的总大小
                    fileLength = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                    downloadFileInfo.setFileSize(fileLength);
                }


            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }

            String tmpFilePath = Environment.getExternalStorageDirectory().getPath() + FILE_DOWNLOAD_TEMP_DIR + fileName;
            tmpFile = FileUtils.createTempFile(tmpfileBasePath, fileName);
            downloadFileInfo.setTempFilePath(tmpFile.getAbsolutePath());
            DBDaoImpl.getInstance(context).Updata(downloadFileInfo);


        }

    }


    private void getDownloadQuery(long requestId) {
        DownloadManager.Query query = new DownloadManager.Query();
        Cursor cursor = dm.query(query.setFilterById(requestId));
        if (cursor != null && cursor.moveToFirst()) {
            String path = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_FILENAME)); // 获取文件路径
            LogUtils.D(TAG, "路径：" + path);
        }

    }

    private void setDownloadInfo(long requestId) {
        DownloadManager.Query query = new DownloadManager.Query();
        Cursor cursor = dm.query(query.setFilterById(requestId));
        if (cursor != null && cursor.moveToFirst()) {
            //下载的文件到本地的目录
//            String address = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
            //已经下载的字节数
            int bytes_downloaded = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));


            downloadFileInfo.setHadDownloadSize((long) bytes_downloaded);

//            总需下载的字节数
            int bytes_total = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
            downloadFileInfo.setFileSize((long) bytes_total);

            //Notification 标题
            String title = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_TITLE));
            LogUtils.D(TAG, "标题:" + title);
            //描述
            String description = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_DESCRIPTION));
            LogUtils.D(TAG, "描述:" + description);

            //下载对应id
            long id = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_ID));
            //下载文件名称
            String path = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_FILENAME));

            downloadFileInfo.setFilePath(path);
            LogUtils.D(TAG, "文件名:" + path);


            //下载文件的URL链接
            String url = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_URI));
        }
        DBDaoImpl.getInstance(context).Updata(downloadFileInfo);

    }

    public class DownloadBinder extends Binder {
        /**
         * 返回当前服务的实例
         *
         * @return
         */
        public DownloadService getService() {
            return DownloadService.this;
        }

    }

    /**
     * 注销广播
     */
    private void unregisterBroadcast() {
        if (downLoadBroadcast != null) {
            unregisterReceiver(downLoadBroadcast);
            downLoadBroadcast = null;
        }
    }


    /**
     * 注销ContentObserver
     */
    private void unregisterContentObserver() {
        if (downloadObserver != null) {
            getContentResolver().unregisterContentObserver(downloadObserver);
        }
    }

    /**
     * 注册ContentObserver
     */
    private void registerContentObserver() {
        /** observer download change **/
        if (downloadObserver != null) {
            getContentResolver().registerContentObserver(Uri.parse("content://downloads/my_downloads"), false, downloadObserver);
        }
    }

    public interface OnProgressListener {
        /**
         * 下载进度
         *
         * @param fraction 已下载/总大小
         */
        void onProgress(float fraction);
    }

    public void setOnProgressListener(OnProgressListener onProgressListener) {
        this.onProgressListener = onProgressListener;
    }


    /**
     * 监听下载进度
     */
    private class DownloadChangeObserver extends ContentObserver {

        public DownloadChangeObserver() {
            super(myWeakHandler);
            scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        }

        /**
         * 当所监听的Uri发生改变时，就会回调此方法
         *
         * @param selfChange 此值意义不大, 一般情况下该回调值false
         */
        @Override
        public void onChange(boolean selfChange) {
            scheduledExecutorService.scheduleAtFixedRate(progressRunnable, 0, 2, TimeUnit.SECONDS);
        }
    }

    private static final class MyWeakHandler extends WeakHandler<DownloadService> {

        MyWeakHandler(DownloadService downloadService) {
            super(downloadService);
        }

        @Override
        public void handleMessage(Message msg, DownloadService downloadService) {
            if (downloadService == null) {
                return;
            }
            if (downloadService.onProgressListener != null && HANDLE_DOWNLOAD == msg.what) {
                //被除数可以为0，除数必须大于0
                if (msg.arg1 >= 0 && msg.arg2 > 0) {
                    downloadService.onProgressListener.onProgress(msg.arg1 / (float) msg.arg2);
                }
            }
        }
    }

    private Runnable progressRunnable = new Runnable() {
        @Override
        public void run() {
            updateProgress();
        }
    };


    /**
     * 发送Handler消息更新进度和状态
     */
    private void updateProgress() {
        int[] bytesAndStatus = getBytesAndStatus(requestId);
        myWeakHandler.sendMessage(myWeakHandler.obtainMessage(HANDLE_DOWNLOAD, bytesAndStatus[0], bytesAndStatus[1], bytesAndStatus[2]));
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
            cursor = dm.query(query);
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



    @Override
    public void onDestroy() {
        unregisterReceiver(receiver);
        unregisterBroadcast();
        unregisterContentObserver();
        super.onDestroy();
    }
}