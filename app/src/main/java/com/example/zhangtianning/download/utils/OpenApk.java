package com.example.zhangtianning.download.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.support.v4.content.FileProvider;
import android.widget.Toast;


import java.io.File;

/**
 * Created by 冒险者ztn on 2017/9/12.
 */

public class OpenApk {
    private static volatile OpenApk openApk;

    public OpenApk() {

    }

    public static OpenApk getInstance() {
        if (openApk == null) {
            synchronized (OpenApk.class) {
                if (openApk == null) {
                    openApk = new OpenApk();
                }
            }
        }
        return openApk;
    }


    /**
     * 打开APK程序代码
     */
    public void openFile(Context context, File file) {
        // TODO Auto-generated method stub

        if (file.getName().contains(".") &&
                file.getName().length() >= 4 &&
                file.getName().substring(file.getName().length() - 4).equals(".apk")) {
            Intent intent = new Intent();
            // 由于没有在Activity环境下启动Activity,设置下面的标签
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setAction(android.content.Intent.ACTION_VIEW);
            if (Build.VERSION.SDK_INT >= 24) { //判读版本是否在7.0以上
                //参数1 上下文, 参数2 Provider主机地址 和配置文件中保持一致   参数3  共享的文件
                Uri apkUri =
                        FileProvider.getUriForFile(context, context.getPackageName() + ".FileProvider", file);
                //添加这一句表示对目标应用临时授权该Uri所代表的文件
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
            } else {
                intent.setDataAndType(Uri.fromFile(file),
                        "application/vnd.android.package-archive");
            }
            context.startActivity(intent);
        } else {
            Toast.makeText(context, "无法打开该文件", Toast.LENGTH_SHORT).show();
        }
    }
}
