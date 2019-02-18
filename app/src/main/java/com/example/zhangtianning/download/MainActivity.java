package com.example.zhangtianning.download;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.NotificationCompat;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.zhangtianning.download.dao.DownloadFileInfo;
import com.example.zhangtianning.download.daoutils.DBDaoImpl;
import com.example.zhangtianning.download.download.FileDownload;
import com.example.zhangtianning.download.listener.FileDownloadListener;
import com.example.zhangtianning.download.utils.LogUtils;
import com.example.zhangtianning.download.utils.OpenApk;
import com.example.zhangtianning.download.utils.WeakHandler;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by zhangtianning on 2017/9/11
 */

public class MainActivity extends AppCompatActivity implements FileDownloadListener, View.OnClickListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    public static final int STATE_START_DOWNLOAD = 0; // 开始下载
    public static final int STATE_DOWNLOADING = 1;   //  下载中
    public static final int STATE_FINISH_DOWNLOAD = 2;  // 下载完成
    public static final int STATE_FAIL_DOWNLOAD = 3; // 下载失败
    public static final int STATE_FAIL_DOWNLOAD_PAUSE = 4; // 下载暂停
    public int downloadState = -1; //最初的下载状态，没有下载任务。

    public boolean isPuse;
    Button buttonStrart, buttonPause, buttonService;
    Button buttonSwitch, buttonStartService;
    TextView changeUrl;
    EditText etUrl;
    TextView state;
    TextProgressbar textProgressBar;
    MainActivity mainActivity;
    private FileDownload fileDownload;
    private List<String> urls = new ArrayList<>();


    private MyWeakHandler myWeakHandler;
    Random random = new Random();
    DownloadFileInfo downloadFileInfo;//文件信息


    private static final class MyWeakHandler extends WeakHandler<MainActivity> {

        MyWeakHandler(MainActivity mainActivity) {
            super(mainActivity);
        }

        @Override
        public void handleMessage(Message msg, MainActivity mainActivity) {
            if (mainActivity == null) {
                return;
            }
            int progress = msg.arg1;

            switch (msg.what) {
                case STATE_START_DOWNLOAD:
                    mainActivity.textProgressBar.setText(progress);
                    mainActivity.textProgressBar.setProgress(progress);
                    mainActivity.state.setText("开始下载");
                    break;
                case STATE_DOWNLOADING:
                    mainActivity.textProgressBar.setText(mainActivity.downloadFileInfo);
                    mainActivity.textProgressBar.setProgress(progress);
                    mainActivity.state.setText("下载中");
                    break;
                case STATE_FINISH_DOWNLOAD:
                    mainActivity.textProgressBar.setText(mainActivity.downloadFileInfo);
                    mainActivity.textProgressBar.setProgress(progress);
                    mainActivity.state.setText("下载完成");
                    Toast.makeText(mainActivity, "文件：" + mainActivity.downloadFileInfo.getFileName()
                            + "下载完成", Toast.LENGTH_SHORT).show();
                    String path = mainActivity.downloadFileInfo.getFilePath();
                    OpenApk.getInstance().openFile(mainActivity, new File(path));

                    break;
                case STATE_FAIL_DOWNLOAD:
                    String failReason = (String) msg.obj;
                    Toast.makeText(mainActivity, failReason, Toast.LENGTH_SHORT).show();
                    mainActivity.state.setText("下载失败");
                    break;
                case STATE_FAIL_DOWNLOAD_PAUSE:
                    mainActivity.textProgressBar.setText(mainActivity.downloadFileInfo);
                    mainActivity.textProgressBar.setProgress(progress);
                    mainActivity.state.setText("下载暂停");
                    break;
            }
        }
    }


    public static Intent newIntent(Context context) {
        return new Intent(context, MainActivity.class);
    }


    @Override
    public void onCreate(Bundle savedInstanceStatee) {
        super.onCreate(savedInstanceStatee);
        setContentView(R.layout.activity_main);
        myWeakHandler = new MyWeakHandler(this);

        urls.add("https://www.wandoujia.com/apps/com.UCMobile/download/dot?ch=detail_normal_dl");

        LogUtils.D(TAG, "获取应用包名" + getPackageName());

        mainActivity = this;
        buttonStrart = this.findViewById(R.id.button_start);
        buttonPause = this.findViewById(R.id.button_pause);
        buttonService = this.findViewById(R.id.button_service);
        etUrl = this.findViewById(R.id.et_url);
        buttonSwitch = this.findViewById(R.id.button_switch);
        buttonStartService = this.findViewById(R.id.button_start_a_service);

        state = this.findViewById(R.id.tv_state);
        textProgressBar = this.findViewById(R.id.myProgressBar);
        changeUrl = this.findViewById(R.id.change_url);

        etUrl.setText(urls.get(0));

        buttonStrart.setOnClickListener(this);
        buttonPause.setOnClickListener(this);
        buttonService.setOnClickListener(this);
        changeUrl.setOnClickListener(this);
        buttonSwitch.setOnClickListener(this);
        buttonStartService.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_start:
                if (etUrl.getText().toString().isEmpty()) {
                    Toast.makeText(mainActivity, "请输入url", Toast.LENGTH_SHORT).show();
                } else {
                    if (downloadState == STATE_DOWNLOADING) {
                        Toast.makeText(mainActivity, "有文件正在下载", Toast.LENGTH_SHORT).show();
                    } else {
                        LogUtils.D(TAG, "downloadState=" + downloadState);
                        startDownloadFile();
                    }
                }

                break;
            case R.id.button_pause:
                if (downloadState != -1 && downloadState != 4) {
                    stopDownloadFile();
                    isPuse = true;
                    Toast.makeText(mainActivity, "文件下载暂停", Toast.LENGTH_SHORT).show();
                } else if (downloadState == 4) {
                    Toast.makeText(mainActivity, "文件已下载暂停", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(mainActivity, "暂无下载任务", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.button_service:
                break;
            case R.id.change_url:
                etUrl.setText(urls.get(random.nextInt(urls.size())));
                break;
            case R.id.button_switch:
                textProgressBar.setIsSize(!textProgressBar.getIsSize());
                if (textProgressBar.getIsSize()) {
                    buttonSwitch.setText("进度条显示状态：文件大小");
                } else {
                    buttonSwitch.setText("进度条显示状态：百分比");
                }
                break;
//            case R.id.button_start_a_service:
//                Toast.makeText(mainActivity, "暂未实现该功能", Toast.LENGTH_SHORT).show();
//                break;
        }
    }

    /**
     * 停止下载文件
     */
    public void stopDownloadFile() {
        downloadState = STATE_FAIL_DOWNLOAD_PAUSE;
        if (fileDownload != null) {
            fileDownload.stop();
        }
    }

    /**
     * 开始下载文件
     */
    public void startDownloadFile() {
        LogUtils.D(TAG, "onFileStartDownload=");
        downloadState = 0;
        fileDownload = new FileDownload();
        fileDownload.setFileDownloadListener(this);
        String url = etUrl.getText().toString().trim();

        urlIsInDB(mainActivity, url);


    }

    /**
     * 判断url是否合理与是否在数据库中
     *
     * @param context
     * @param url
     */

    private void urlIsInDB(Context context, String url) {

        if (url.indexOf("http") == 0
                || url.indexOf("https") == 0) {
            downloadFileInfo = DBDaoImpl.getInstance(context).getDownloadFileInfoWithUrl(url);

            if (downloadFileInfo != null) {
                if (TextUtils.equals(downloadFileInfo.getHadDownloadSize().toString(), downloadFileInfo.getFileSize().toString()) &&
                        !TextUtils.equals("-1", downloadFileInfo.getFileSize().toString())) {
                    File file = new File(downloadFileInfo.getFilePath());
                    if (file.exists()) {
                        downloadState = STATE_FINISH_DOWNLOAD;
                        OpenApk.getInstance().openFile(mainActivity, file);
                    } else {
                        downloadFileInfo.setHadDownloadSize((long) 0);
                        DBDaoImpl.getInstance(context).Updata(downloadFileInfo);
                    }

                } else {
                    fileDownload.start(mainActivity, downloadFileInfo);
                    Message message = myWeakHandler.obtainMessage();
                    downloadState = message.what = STATE_START_DOWNLOAD;
                    message.obj = "下载失败";
                    myWeakHandler.sendMessage(message);
                }
            } else {
                LogUtils.D(TAG, "没有文件信息！");
                fileDownload.start(mainActivity, etUrl.getText().toString());
            }

        } else {
            Toast.makeText(mainActivity, "请输入一个正确的url", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onFileDownloading(DownloadFileInfo downloadFileInfo) {
//        LogUtils.D(TAG, "onFileDownloading=" + downloadFileInfo.toString());
        this.downloadFileInfo = downloadFileInfo;
        Message message = myWeakHandler.obtainMessage();
        downloadState = message.what = STATE_DOWNLOADING;
        message.arg1 = downloadFileInfo.getDownloadProgress();
        myWeakHandler.sendMessage(message);
    }

    @Override
    public void onFileDownloadFail(DownloadFileInfo downloadFileInfo) {
        if (downloadFileInfo != null) {
            LogUtils.D(TAG, "onFileDownloadFail=" + downloadFileInfo.toString());
            Message message = myWeakHandler.obtainMessage();
            downloadState = message.what = STATE_FAIL_DOWNLOAD;
            message.arg1 = downloadFileInfo.getDownloadProgress();
            message.obj = "下载失败";
            myWeakHandler.sendMessage(message);
        } else {
            Message message = myWeakHandler.obtainMessage();
            downloadState = message.what = STATE_FAIL_DOWNLOAD;
            message.arg1 = 0;
            message.obj = "下载失败";
            myWeakHandler.sendMessage(message);
        }
    }

    @Override
    public void onFileDownloadCompleted(DownloadFileInfo downloadFileInfo) {
        LogUtils.D(TAG, "onFileDownloadCompleted=" + downloadFileInfo.toString());
        Message message = myWeakHandler.obtainMessage();
        downloadState = message.what = STATE_FINISH_DOWNLOAD;
        message.arg1 = downloadFileInfo.getDownloadProgress();
        mainActivity.downloadFileInfo = downloadFileInfo;
        myWeakHandler.sendMessage(message);

    }

    @Override
    public void onFileDownloadPaused(DownloadFileInfo downloadFileInfo) {
        LogUtils.D(TAG, "onFileDownloadPaused=" + downloadFileInfo.toString());
        Message message = myWeakHandler.obtainMessage();
        downloadState = message.what = STATE_FAIL_DOWNLOAD_PAUSE;
        message.arg1 = downloadFileInfo.getDownloadProgress();
        myWeakHandler.sendMessage(message);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopDownloadFile();
    }
}
