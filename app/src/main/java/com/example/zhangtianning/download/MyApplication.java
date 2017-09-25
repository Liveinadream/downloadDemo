package com.example.zhangtianning.download;

import android.app.Application;
import android.content.Context;
import android.widget.Button;

/**
 * Created by zhangtianning on 2017/9/11
 */

public class MyApplication extends Application implements Thread.UncaughtExceptionHandler {

    public Context app;

    @Override
    public void onCreate() {
        super.onCreate();
        app = getApplicationContext();
        CrashHandler crashHandler = CrashHandler.getInstance();
        crashHandler.init(getApplicationContext());
    }

    @Override
    public void uncaughtException(Thread thread, Throwable throwable) {
        System.out.println("uncaughtException");
        System.exit(0);
    }
}
