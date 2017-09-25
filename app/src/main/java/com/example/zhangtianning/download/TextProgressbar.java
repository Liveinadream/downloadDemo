package com.example.zhangtianning.download;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.ProgressBar;

import com.example.zhangtianning.download.dao.DownloadFileInfo;
import com.example.zhangtianning.download.utils.DpUtils;
import com.example.zhangtianning.download.utils.LogUtils;

/**
 * Created by zhangtianning on 2017/9/11
 */

public class TextProgressbar extends ProgressBar {
    private String str = "0%";
    private Paint mPaint;
    Rect rect;
    private boolean isSize = false;

    public TextProgressbar(Context context) {
        super(context);
    }

    public TextProgressbar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initText();
    }

    public TextProgressbar(Context context, AttributeSet attrs) {
        super(context, attrs);
        initText();
    }

    @Override
    public void setProgress(int progress) {
//        setText(progress);
        super.setProgress(progress);

    }

    @Override
    protected synchronized void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (rect == null) {
            rect = new Rect();
        }
        this.mPaint.getTextBounds(this.str, 0, this.str.length(), rect);
        mPaint.setTextSize(DpUtils.sp2px(getContext(), 14));
        int x = (getWidth() / 2) - rect.centerX();// 让现实的字体处于中心位置;;
        int y = (getHeight() / 2) - rect.centerY();// 让显示的字体处于中心位置;;
        canvas.drawText(this.str, x, y, this.mPaint);
    }

    // 初始化，画笔
    private void initText() {
        this.mPaint = new Paint();
        this.mPaint.setAntiAlias(true);// 设置抗锯齿;;;;
        this.mPaint.setColor(Color.WHITE);
    }

    // 设置文字内容
    public void setText(int progress) {
        int i = (int) ((progress * 1.0f / this.getMax()) * 100);
        if (i == 100) {
            this.str = "已完成";
        } else {
            if (!isSize) {
                this.str = String.valueOf(i) + "%";
            } else {
                this.str = 0 + "";
            }
        }
    }

    public void setIsSize(boolean isSize) {
        this.isSize = isSize;
    }

    public boolean getIsSize() {
        return isSize;
    }

    // 根据数据库信息设置文字内容
    public void setText(DownloadFileInfo downloadFileInfo) {

        if (downloadFileInfo != null) {
            LogUtils.D("下载中：", "文件下载中"+downloadFileInfo.getHadDownloadSize());

            if (TextUtils.equals(downloadFileInfo.getHadDownloadSize().toString(),
                    downloadFileInfo.getFileSize().toString())) {
                this.str = "已完成";
            } else {
                if (isSize) {
                    this.str = (downloadFileInfo.getHadDownloadSize() / 1024) + "kb / "
                            + (downloadFileInfo.getFileSize() / 1024) + "kb";
                } else {
                    this.str = downloadFileInfo.getDownloadProgress().toString() + "%";
                }
            }
        } else {
            if (isSize) {
                this.str = "0";
            } else {
                this.str = "0%";
            }
        }
    }
}

