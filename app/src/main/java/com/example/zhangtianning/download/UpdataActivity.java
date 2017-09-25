package com.example.zhangtianning.download;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.zhangtianning.download.dao.DownloadFileInfo;
import com.example.zhangtianning.download.daoutils.DBDaoImpl;
import com.example.zhangtianning.download.utils.LogUtils;
import com.example.zhangtianning.download.utils.OpenApk;
import com.example.zhangtianning.download.utils.ToastUtils;

import java.io.File;
import java.io.Serializable;


/**
 * Created by 冒险者ztn on 2017/9/15.
 */

public class UpdataActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    //目前可能需要的参数，用户名，token，

    //    private String userId;
//    private String token;
    private Button jump, parse, start;
    UpdataActivity updataActivity;
    private int versionCode;

    private static final String DOWNLOADFILEINFO = "downloadFileInfo";
    DownloadFileInfo downloadFileInfo;//文件信息
    private String downloadUrl = "http://d.koudai.com/com.koudai.weishop/1000f/weishop_1000f.apk";
    private static String URL = "url";


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
                isUrlInDB(updataActivity, downloadUrl);
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
                if (TextUtils.equals(downloadFileInfo.getHadDownloadSize().toString(), downloadFileInfo.getFileSize().toString())) {
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
        Intent intent = new Intent(UpdataActivity.this, DownloadService.class);

        intent.putExtra(URL, downloadUrl);
        startService(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
