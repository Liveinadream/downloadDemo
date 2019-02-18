package com.example.zhangtianning.download.download;

import android.app.Activity;
import android.os.Environment;
import android.text.TextUtils;

import com.example.zhangtianning.download.MainActivity;
import com.example.zhangtianning.download.dao.DownloadFileInfo;
import com.example.zhangtianning.download.daoutils.DBDaoImpl;
import com.example.zhangtianning.download.listener.FileDownloadListener;
import com.example.zhangtianning.download.utils.DownloadUtils;
import com.example.zhangtianning.download.utils.FileUtils;
import com.example.zhangtianning.download.utils.LogUtils;
import com.example.zhangtianning.download.utils.OpenApk;
import com.example.zhangtianning.download.utils.ToastUtils;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;


/**
 * 下载线程
 */

public class DownloadThread extends Thread {

    private static final String TAG = DownloadThread.class.getSimpleName();
//    private List<DownloadFileInfo> downloadFileInfos;

    /**
     * 文件下载目录
     */
    private static final String FILE_DOWNLOAD_DIR = "/knrt/download/";

    /**
     * 文件下载时的临时文件目录
     */
    private static final String FILE_DOWNLOAD_TEMP_DIR = "/knrt/temp/";

    private String mDownloadUrl;
    /**
     * 文件下载详情
     */
    private static DownloadFileInfo mDownloadFileInfo;

    /**
     * 文件下载监听
     */
    private FileDownloadListener mFileDownloadListener;


    private boolean isStopDownload = false;
    private OutputStream mOutputStream;
    private ByteArrayOutputStream mByteOutput;
    private File tmpFile;

    private MainActivity ctx;

    public DownloadThread(DownloadFileInfo downloadFileInfo, FileDownloadListener fileDownloadListener, Activity context) {
        mDownloadFileInfo = downloadFileInfo;
        mDownloadUrl = downloadFileInfo.getDownloadUrl();
        this.mFileDownloadListener = fileDownloadListener;
        this.ctx = (MainActivity) context;
    }

    @Override
    public void run() {
        super.run();
        isStopDownload = false;

        String basePath = FileUtils.createBasePath(FILE_DOWNLOAD_DIR);
        String tmpfileBasePath = FileUtils.createBasePath(FILE_DOWNLOAD_TEMP_DIR);

        LogUtils.D(TAG, "run =" + mDownloadFileInfo.toString());
        URL url;
        HttpURLConnection httpURLConnection;
        BufferedInputStream bufferedInputStream;
        try {
            url = new URL(this.mDownloadUrl);
            httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setUseCaches(false);  // 请求时不使用缓存
            httpURLConnection.setConnectTimeout(5 * 1000); // 设置连接超时时间
            httpURLConnection.setRequestMethod("GET");
            httpURLConnection.setRequestProperty("Accept-Language", "zh-cn");
            httpURLConnection.setRequestProperty("UA-CPU", "x86");
            httpURLConnection.setRequestProperty("Accept-Encoding", "gzip");
//            httpURLConnection.setRequestProperty("Content-type", "text/html");
            httpURLConnection.setRequestProperty("Connection", "close");


//            httpURLConnection.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.0; Windows NT; DigExt)");
            httpURLConnection.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 5.1)");
            httpURLConnection.setRequestProperty("Accept",
                    "image/gif, image/x-xbitmap, image/jpeg, image/pjpeg, application/x-shockwave-flash, application/vnd.ms-powerpoint, application/vnd.ms-excel, application/msword, */*");
//            httpURLConnection.setRequestProperty("User-Agent", " Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/37.0.2062.120 Safari/537.36");

            long downloadSize = mDownloadFileInfo.getHadDownloadSize();

            String fileName = mDownloadFileInfo.getFileName();


            long fileLength;

//
            if (downloadSize == mDownloadFileInfo.getFileSize() &&
                    !TextUtils.equals("-1", mDownloadFileInfo.getFileSize().toString())) {
                OpenApk.getInstance().openFile(ctx, new File(mDownloadFileInfo.getFilePath()));
                interrupt();
            }
            if (downloadSize > 0) {
                String tmpFilePath = Environment.getExternalStorageDirectory().getPath() + FILE_DOWNLOAD_TEMP_DIR + fileName;
                LogUtils.D("Tag", "临时文件的路径：" + tmpFilePath);
                fileLength = mDownloadFileInfo.getFileSize();
                httpURLConnection.setRequestProperty("Range",
                        "bytes=" + mDownloadFileInfo.getHadDownloadSize() + "-"
                                + mDownloadFileInfo.getFileSize());
                tmpFile = new File(tmpFilePath);
                if (!tmpFile.exists() || !tmpFile.isFile()) {
                    tmpFile = FileUtils.createTempFile(tmpfileBasePath, fileName);
                    mDownloadFileInfo.setTempFilePath(tmpFile.getAbsolutePath());
                }
            } else {
                fileLength = httpURLConnection.getContentLength(); // 获取文件的大小

                mDownloadFileInfo.setFileSize(fileLength);

                if (fileName == null) {
                    fileName = httpURLConnection.getHeaderField("Content-Disposition"); // 获取文件名
                }
                if (fileName == null) {
                    fileName = mDownloadUrl.substring(mDownloadUrl.lastIndexOf("/") + 1);
                }
                mDownloadFileInfo.setFileName(fileName);
                mDownloadFileInfo.setFilePath(basePath + File.separator + fileName);
                String tmpFilePath = Environment.getExternalStorageDirectory().getPath() + FILE_DOWNLOAD_TEMP_DIR + fileName;
                LogUtils.D("Tag", "临时文件的路径：" + tmpFilePath);
                tmpFile = FileUtils.createTempFile(tmpfileBasePath, fileName);
                mDownloadFileInfo.setTempFilePath(tmpFile.getAbsolutePath());
                if (TextUtils.equals("-1", mDownloadFileInfo.getFileSize().toString())) {
                    ToastUtils.showToast(ctx, "没有获取下载信息请重试");
//                stop();
                    interrupt();
                }
            }

            LogUtils.D(TAG, "run fileLength=" + fileLength +
                    ",fileName=" + fileName + ",absolute=" + tmpFile.getAbsolutePath());

            int progress = DownloadUtils.getProgress(downloadSize, fileLength);
            mDownloadFileInfo.setDownloadProgress(progress);
            httpURLConnection.connect();
            if (mFileDownloadListener != null) {
                mFileDownloadListener.onFileDownloading(mDownloadFileInfo);
            }

            int code = httpURLConnection.getResponseCode();
            LogUtils.E(TAG, "start  code=" + code);
            LogUtils.E(TAG, "文件大小=" + mDownloadFileInfo.getFileSize());
            if (code == HttpURLConnection.HTTP_OK) {
                if (mDownloadFileInfo.getHadDownloadSize() > 0) {
                    // 子线程
                    ToastUtils.showToast(ctx, "文件不支持断点续传，已重新开始下载！");
                    mDownloadFileInfo.setHadDownloadSize((long) -1);
                }
                long currentTime = System.currentTimeMillis();
                int bufferSize = 1024;
                bufferedInputStream = new BufferedInputStream(httpURLConnection.getInputStream(), bufferSize);
                int len; //读取到的数据长度
                byte[] buffer = new byte[bufferSize];
                //写入中间文件
                mOutputStream = new FileOutputStream(tmpFile, true);//true表示向打开的文件末尾追加数据
                mByteOutput = new ByteArrayOutputStream();
                // 开始读取
                while ((len = bufferedInputStream.read(buffer)) != -1) {
                    mByteOutput.write(buffer, 0, len);
                    mDownloadFileInfo = writeCache(mDownloadFileInfo);
                    progress = DownloadUtils.getProgress(mDownloadFileInfo.getHadDownloadSize(), fileLength);
                    long nowTime = System.currentTimeMillis();
                    if (currentTime < nowTime - 500) {
                        currentTime = nowTime;

                        mDownloadFileInfo.setDownloadProgress(progress);
                        if (mFileDownloadListener != null) {
                            if (downloadSize == fileLength) {
                                mFileDownloadListener.onFileDownloadCompleted(mDownloadFileInfo);
                                break;
                            } else {
                                mFileDownloadListener.onFileDownloading(mDownloadFileInfo);
                            }
                        }
                    }
                    if (isStopDownload) {
                        if (mFileDownloadListener != null) {
                            mFileDownloadListener.onFileDownloadPaused(mDownloadFileInfo);
                        }
                        break;
                    }
                }

            } else if (code == HttpURLConnection.HTTP_PARTIAL) {
                long currentTime = System.currentTimeMillis();
                int bufferSize = 1024;
                bufferedInputStream = new BufferedInputStream(httpURLConnection.getInputStream(), bufferSize);
                int len; //读取到的数据长度
                byte[] buffer = new byte[bufferSize];
                //写入中间文件
                mOutputStream = new FileOutputStream(tmpFile, true);//true表示向打开的文件末尾追加数据
                mByteOutput = new ByteArrayOutputStream();
                // 开始读取
                while ((len = bufferedInputStream.read(buffer)) != -1) {
                    mByteOutput.write(buffer, 0, len);
                    mDownloadFileInfo = writeCache(mDownloadFileInfo);
                    progress = DownloadUtils.getProgress(mDownloadFileInfo.getHadDownloadSize(), fileLength);
                    long nowTime = System.currentTimeMillis();
                    if (currentTime < nowTime - 500) {
                        currentTime = nowTime;
                        mDownloadFileInfo.setDownloadProgress(progress);
                        if (mFileDownloadListener != null) {
                            if (downloadSize == fileLength) {
                                mFileDownloadListener.onFileDownloadCompleted(mDownloadFileInfo);
                                break;
                            } else {
                                mFileDownloadListener.onFileDownloading(mDownloadFileInfo);
                            }
                        }
                    }
                    if (isStopDownload) {
                        if (mFileDownloadListener != null) {
                            mFileDownloadListener.onFileDownloadPaused(mDownloadFileInfo);
                        }
                        break;
                    }
                }
            } else {
                mDownloadFileInfo.setDownloadProgress(0);
                if (mFileDownloadListener != null) {
                    mFileDownloadListener.onFileDownloadFail(mDownloadFileInfo);
                }
                stopDownload();
            }

        } catch (IOException e) {
//            e.printStackTrace();
            LogUtils.E(TAG, e.toString());
            if (mFileDownloadListener != null) {
                mFileDownloadListener.onFileDownloadFail(mDownloadFileInfo);
            }
        } finally {
            if (!isStopDownload) {
                DBDaoImpl.getInstance(ctx).Updata(mDownloadFileInfo);
                LogUtils.D(TAG, "文件路径" + mDownloadFileInfo.getFilePath());
                LogUtils.D(TAG, "数据库信息：" + DBDaoImpl.getInstance(ctx).DownloadFileInfot2String(mDownloadFileInfo));

            }
        }
    }

    void stopDownload() {
        isStopDownload = true;
    }

    DownloadFileInfo getDownloadFileInfo() {
        return mDownloadFileInfo;
    }

    /**
     * 写缓存
     */
    private synchronized DownloadFileInfo writeCache(DownloadFileInfo downloadFileInfo) {
        if (mByteOutput != null && mByteOutput.size() > 0 && mOutputStream != null) {
            try {
                mByteOutput.writeTo(mOutputStream);
                downloadFileInfo.setHadDownloadSize(downloadFileInfo.getHadDownloadSize() + mByteOutput.size());
                DBDaoImpl.getInstance(ctx).Updata(downloadFileInfo);

                mByteOutput.reset();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (downloadFileInfo.getHadDownloadSize() >= (downloadFileInfo.getFileSize() - 1)) {  // 下载完成后重命名
            boolean isSuccess = tmpFile.renameTo(FileUtils.deleteAndCreatFilePath(downloadFileInfo.getFilePath(), true));
            LogUtils.D(TAG, "writeCache  renameto is" + isSuccess);
            downloadFileInfo.setHadDownloadSize(downloadFileInfo.getFileSize());
            downloadFileInfo.setDownloadProgress(100);
            DBDaoImpl.getInstance(ctx).Updata(downloadFileInfo);
            mFileDownloadListener.onFileDownloadCompleted(downloadFileInfo);
        }
        return downloadFileInfo;
    }
}
