package com.example.zhangtianning.download;

import android.Manifest;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.zhangtianning.download.dao.DownloadFileInfo;
import com.example.zhangtianning.download.daoutils.DBDaoImpl;
import com.example.zhangtianning.download.utils.LogUtils;
import com.example.zhangtianning.download.utils.OpenApk;
import com.example.zhangtianning.download.utils.ToastUtils;
import com.example.zhangtianning.download.utils.WeakHandler;
import com.example.zhangtianning.download.service.DownloadService;
import com.tbruyelle.rxpermissions2.RxPermissions;

import java.io.File;

import io.reactivex.functions.Consumer;

/**
 * Created by 冒险者ztn on 2017/10/16.
 */

public class UpdateActivity2 extends AppCompatActivity {
    private static final String TAG = UpdateActivity2.class.getSimpleName();


    public static final int STATE_START_DOWNLOAD = 0; // 开始下载
    public static final int STATE_DOWNLOADING = 1;   //  下载中
    public static final int STATE_FINISH_DOWNLOAD = 2;  // 下载完成
    public static final int STATE_FAIL_DOWNLOAD = 3; // 下载失败
    public static final int STATE_FAIL_DOWNLOAD_PAUSE = 4; // 下载暂停
    public int downloadState = -1; //最初的下载状态，没有下载任务。
    public DownloadService downloadService;
    private Button jump, parse, start;
    TextProgressbar textProgressBar;
    private TextView state;
    UpdateActivity2 updateActivity;
    private int versionCode;
    DownloadFileInfo downloadFileInfo;//文件信息
    MyWeakHandler myWeakHandler;

    private static String URL = "url";
    private static String NAME = "name";
    private String url = "https://www.wandoujia.com/apps/com.UCMobile/download/dot?ch=detail_normal_dl";
    private String name = "";

    private static final class MyWeakHandler extends WeakHandler<UpdateActivity2> {

        MyWeakHandler(UpdateActivity2 updateActivity2) {
            super(updateActivity2);
        }

        @Override
        public void handleMessage(Message msg, UpdateActivity2 updateActivity2) {
            if (updateActivity2 == null) {
                return;
            }
            int progress = msg.arg1;
            Message message = obtainMessage();

            switch (msg.what) {
                case DownloadManager.STATUS_PENDING:
                    updateActivity2.textProgressBar.setText(progress);
                    updateActivity2.textProgressBar.setProgress(progress);
                    updateActivity2.state.setText("准备开始下载");

                    message.arg1 = updateActivity2.downloadService.getDownloadState();
                    sendMessage(message);
                    break;
                case DownloadManager.STATUS_RUNNING:
                    updateActivity2.textProgressBar.setText(updateActivity2.downloadFileInfo);
                    updateActivity2.textProgressBar.setProgress(progress);
                    updateActivity2.state.setText("下载中");

                    message.arg1 = updateActivity2.downloadService.getDownloadState();
                    sendMessage(message);
                    break;
                case DownloadManager.STATUS_SUCCESSFUL:
                    updateActivity2.textProgressBar.setText(updateActivity2.downloadFileInfo);
                    updateActivity2.textProgressBar.setProgress(progress);
                    updateActivity2.state.setText("下载完成");
                    Toast.makeText(updateActivity2, "文件：" + updateActivity2.downloadFileInfo.getFileName()
                            + "下载完成", Toast.LENGTH_SHORT).show();
                    String path = updateActivity2.downloadFileInfo.getFilePath();
                    OpenApk.getInstance().openFile(updateActivity2, new File(path));


                    break;
                case DownloadManager.STATUS_FAILED:
                    String failReson = (String) msg.obj;
                    Toast.makeText(updateActivity2, failReson, Toast.LENGTH_SHORT).show();
                    updateActivity2.state.setText("下载失败");
                    break;
                case DownloadManager.STATUS_PAUSED:
                    updateActivity2.textProgressBar.setText(updateActivity2.downloadFileInfo);
                    updateActivity2.textProgressBar.setProgress(progress);
                    updateActivity2.state.setText("下载暂停");
                    message.arg1 = updateActivity2.downloadService.getDownloadState();
                    sendMessage(message);
                    break;
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_boss);

        RxPermissions rxPermissions = new RxPermissions(this);
        rxPermissions.request(Manifest.permission.WRITE_EXTERNAL_STORAGE).subscribe(
                new Consumer<Boolean>() {
                    @Override
                    public void accept(Boolean aBoolean) {
                        if (!aBoolean) {
                            Toast.makeText(getBaseContext(), "您为授予写入权限", Toast.LENGTH_SHORT).show();
                            finish();
                        }


                    }
                }
        ).dispose();


        jump = this.findViewById(R.id.jump);
        parse = this.findViewById(R.id.parse);
        start = this.findViewById(R.id.start_service);
        textProgressBar = this.findViewById(R.id.myProgressBar);
        state = this.findViewById(R.id.tv_state);
        updateActivity = this;
        try {
            PackageManager manager = this.getPackageManager();
            PackageInfo info = manager.getPackageInfo(this.getPackageName(), 0);
            versionCode = info.versionCode;
        } catch (Exception ignored) {

        }
        name = getApplicationName();
        myWeakHandler = new MyWeakHandler(this);
        LogUtils.D(TAG, name);
        setListener();
    }

    private void setListener() {

        jump.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SceneChangeUtils.viewClick(updateActivity, MainActivity.newIntent(updateActivity));

            }
        });
        parse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ToastUtils.showToast(updateActivity, "应用包名：" + getPackageName() +
                        "\n应用版本：" + versionCode);
            }
        });

        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (downloadState == STATE_DOWNLOADING) {
                    Toast.makeText(updateActivity, "有文件正在下载", Toast.LENGTH_SHORT).show();
                } else {
                    isUrlInDB(updateActivity, url);
                }
            }
        });
    }

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
                        OpenApk.getInstance().openFile(updateActivity, file);
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
        Intent intent = new Intent(UpdateActivity2.this, DownloadService.class);
        intent.putExtra(URL, url);
        intent.putExtra(NAME, name);
        startService(intent);
        Message message = myWeakHandler.obtainMessage();
        message.arg1 = downloadService.getDownloadState();
        myWeakHandler.sendMessage(message);
    }

    public String getApplicationName() {
        PackageManager packageManager = null;
        ApplicationInfo applicationInfo;
        try {
            packageManager = getApplicationContext().getPackageManager();
            applicationInfo = packageManager.getApplicationInfo(getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            applicationInfo = null;
        }
        return (String) packageManager.getApplicationLabel(applicationInfo);
    }
}
