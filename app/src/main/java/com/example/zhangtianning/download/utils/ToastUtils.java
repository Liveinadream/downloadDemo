package com.example.zhangtianning.download.utils;

import android.app.Activity;
import android.widget.Toast;

/**
 * Created by 冒险者ztn on 2017/9/14.
 */

public class ToastUtils {
    /**
     * 显示toast
     * @param ctx
     * @param msg
     */
    public static void showToast(final Activity ctx, final String msg){
        // 判断是在子线程，还是主线程
        if("main".equals(Thread.currentThread().getName())){
            Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show();
        }else{
            // 子线程
            ctx.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show();
                }
            });
        }


    }
}
