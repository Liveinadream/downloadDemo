package com.example.zhangtianning.download;

import android.app.DownloadManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Parcelable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.zhangtianning.download.dao.DownloadFileInfo;
import com.example.zhangtianning.download.daoutils.DBDaoImpl;
import com.example.zhangtianning.download.listener.FileDownloadListener;
import com.example.zhangtianning.download.utils.LogUtils;
import com.example.zhangtianning.download.utils.OpenApk;
import com.example.zhangtianning.download.utils.ToastUtils;
import com.example.zhangtianning.download.utils.WeakHandler;

import java.io.File;
import java.io.Serializable;
import java.util.logging.Logger;


/**
 * Created by 冒险者ztn on 2017/9/15.
 */

public class UpdataActivity extends AppCompatActivity implements FileDownloadListener {
    private static final String TAG = UpdataActivity.class.getSimpleName();


    public static final int STATE_START_DOWNLOAD = 0; // 开始下载
    public static final int STATE_DOWNLOADING = 1;   //  下载中
    public static final int STATE_FINISH_DOWNLOAD = 2;  // 下载完成
    public static final int STATE_FAIL_DOWNLOAD = 3; // 下载失败
    public static final int STATE_FAIL_DOWNLOAD_PAUSE = 4; // 下载暂停

    public int downloadState = -1; //最初的下载状态，没有下载任务。

    //目前可能需要的参数，用户名，token，

    //    private String userId;
//    private String token;
    private Button jump, parse, start;
    UpdataActivity updataActivity;
    private int versionCode;
    private long requestId = -1;

    private static final String DOWNLOADFILEINFO = "downloadFileInfo";
    DownloadFileInfo downloadFileInfo;//文件信息
    private String downloadUrl = "https://www.wandoujia.com/apps/com.UCMobile/download/dot?ch=detail_normal_dl";
    private static String URL = "url";
    DownloadManager downloadManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_boss);
        jump = (Button) this.findViewById(R.id.jump);
        parse = (Button) this.findViewById(R.id.parse);
        start = (Button) this.findViewById(R.id.start_service);
        updataActivity = this;
        try {
            PackageManager manager = this.getPackageManager();
            PackageInfo info = manager.getPackageInfo(this.getPackageName(), 0);
            versionCode = info.versionCode;
        } catch (Exception ignored) {

        }
        setListener();
    }

    private void setListener() {
        jump.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SceneChangeUtils.viewClick(updataActivity, MainActivity.newIntent(updataActivity));

            }
        });
        parse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ToastUtils.showToast(updataActivity, "应用包名：" + getPackageName() +
                        "\n应用版本：" + versionCode);
            }
        });
        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (downloadState == STATE_DOWNLOADING) {
                    Toast.makeText(updataActivity, "有文件正在下载", Toast.LENGTH_SHORT).show();
                } else {
                    isUrlInDB(updataActivity, downloadUrl);
                }
            }
        });
    }

    private ServiceConnection conn = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            DownloadService.DownloadBinder binder = (DownloadService.DownloadBinder) service;
            DownloadService downloadService = binder.getService();

            //接口回调，下载进度
            downloadService.setOnProgressListener(new DownloadService.OnProgressListener() {
                @Override
                public void onProgress(float fraction) {
//                    Log.i(TAG, "下载进度：" + fraction);
//                    bnp.setProgress((int)(fraction * 100));
//
//                    //判断是否真的下载完成进行安装了，以及是否注册绑定过服务
//                    if (fraction == DownloadService.UNBIND_SERVICE && isBindService) {
//                        unbindService(conn);
//                        isBindService = false;
//                        MToast.shortToast("下载完成！");
//                    }
                }
            });
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    /**
     * 这个地方由于downloadManger要求http或者https开头的所以取消了www开头的url
     *
     * @param context context
     * @param url     网址
     */

    private void isUrlInDB(Context context, String url) {

        if (url.indexOf("http") == 0
                || url.indexOf("https") == 0) {
            downloadFileInfo = DBDaoImpl.getInstance(context).getDownloadFileInfoWithUrl(url);

            if (downloadFileInfo != null) {
                if (TextUtils.equals(downloadFileInfo.getHadDownloadSize().toString(), downloadFileInfo.getFileSize().toString()) &&
                        !TextUtils.equals("-1", downloadFileInfo.getFileSize().toString())) {
                    File file = new File(downloadFileInfo.getFilePath());
                    if (file.exists()) {
                        OpenApk.getInstance().openFile(updataActivity, file);
                    } else {
                        downloadFileInfo.setHadDownloadSize((long) 0);
                        DBDaoImpl.getInstance(context).Updata(downloadFileInfo);
                    }

                } else {
                    startServiceDownload();
                }
            } else {
                LogUtils.D(TAG, "没有文件信息！");
                LogUtils.D(TAG, "初始化！");
                downloadFileInfo = DBDaoImpl.getInstance(context).initBaseDownloadFileInfo(url);
                startServiceDownload();
            }

        } else {
            Toast.makeText(this, "请输入一个正确的url", Toast.LENGTH_SHORT).show();
        }
    }

    private void startServiceDownload() {
        downloadState = STATE_START_DOWNLOAD;
        Intent intent = new Intent(UpdataActivity.this, DownloadService.class);
        intent.putExtra(URL, downloadUrl);
        startService(intent);
    }

    @Override
    public void onFileDownloading(DownloadFileInfo downloadFileInfo) {
        downloadState = STATE_DOWNLOADING;
    }

    @Override
    public void onFileDownloadFail(DownloadFileInfo downloadFileInfo) {
        downloadState = STATE_FAIL_DOWNLOAD;
    }

    @Override
    public void onFileDownloadCompleted(DownloadFileInfo downloadFileInfo) {
        downloadState = STATE_FINISH_DOWNLOAD;
    }

    @Override
    public void onFileDownloadPaused(DownloadFileInfo downloadFileInfo) {
        downloadState = STATE_FAIL_DOWNLOAD_PAUSE;
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
