package com.example.zhangtianning.download.daoutils;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.widget.Toast;

import com.example.zhangtianning.download.dao.DaoMaster;
import com.example.zhangtianning.download.dao.DaoSession;
import com.example.zhangtianning.download.dao.DownloadFileInfo;
import com.example.zhangtianning.download.dao.DownloadFileInfoDao;
import com.example.zhangtianning.download.utils.LogUtils;

import java.io.File;
import java.util.List;

/**
 * 数据库控制器
 * Created by 冒险者ztn on 2017/9/11.
 */

public class DBDaoImpl implements DBInfo, DBInstances {
    private static volatile DBDaoImpl dbDataUtils;
    private Context ctx;
    private SQLiteDatabase database;
    private DaoMaster daoMaster;
    private DaoSession daoSession;


    private DBDaoImpl(Context ctx) {
        this.ctx = ctx.getApplicationContext();
        DaoMaster.DevOpenHelper helper = new DaoMaster.DevOpenHelper(ctx, DB_NAME, null);
        database = helper.getWritableDatabase();
        daoMaster = new DaoMaster(database);
        daoSession = daoMaster.newSession();

    }

    public static DBDaoImpl getInstance(Context ctx) {
        if (dbDataUtils == null) {
            synchronized (DBDaoImpl.class) {
                if (dbDataUtils == null) {
                    dbDataUtils = new DBDaoImpl(ctx);
                }
            }
        }
        return dbDataUtils;
    }

    private DownloadFileInfoDao getDownloadFileInfoDao() {
        return daoSession.getDownloadFileInfoDao();
    }

    @Override
    public List<DownloadFileInfo> getDownloadFileInfo() {
        return getDownloadFileInfoDao().queryBuilder().orderDesc(DownloadFileInfoDao.Properties.Id).list();
    }

    @Override
    public DownloadFileInfo getDownloadFileInfoWithUrl(String url) {
        List<DownloadFileInfo> downloadFileInfos = getDownloadFileInfo();
//        LogUtils.D("数据库中有几条数据：", downloadFileInfos.size() + "");

        for (int i = 0; i < downloadFileInfos.size(); i++) {
//            LogUtils.D("数据库url:", downloadFileInfos.get(i).getDownloadUrl());
//            LogUtils.D("数据库输入的url:", url);
            if (TextUtils.equals(downloadFileInfos.get(i).getDownloadUrl(), url)) {
//                LogUtils.D("数据库", "有对应的下载信息");
//                LogUtils.D("数据库的条目信息：", DBDaoImpl.getInstance(ctx).
//                        DownloadFileInfot2String(downloadFileInfos.get(i)));
                return downloadFileInfos.get(i);
            }
        }
//        LogUtils.D("数据库", "没有对应的下载信息");
        return null;
    }

    @Override
    public void setDownloadFileInfo(DownloadFileInfo downloadFileInfo) {
//        List<DownloadFileInfo> downloadFileInfos = getDownloadFileInfoDao().queryBuilder().list();
//        for (DownloadFileInfo downFile : downloadFileInfos) {
//            if (TextUtils.equals(downFile.getDownloadUrl(), downloadFileInfo.getDownloadUrl())) {
//                getDownloadFileInfoDao().deleteInTx(downFile);
//            }
//        }
        getDownloadFileInfoDao().insertInTx(downloadFileInfo);
    }

    @Override
    public void clearDownloadFileInfo() {
        //Todo 还需要删除对应文件，这样就可以达到删除已下载文件的目的
        getDownloadFileInfoDao().deleteAll();
    }

    /**
     * long类型的数据比较应该用equals
     *
     * @param downloadFileInfo
     * @return
     */
    @Override
    public Boolean hasCompleteFile(DownloadFileInfo downloadFileInfo) {

        return TextUtils.equals(downloadFileInfo.getHadDownloadSize().toString(),
                downloadFileInfo.getFileSize().toString()) &&
                (new File(downloadFileInfo.getFilePath()).exists());

    }

    /**
     * 设置开始下载文件时的基本信息
     */
    @Override
    public DownloadFileInfo initBaseDownloadFileInfo(String url) {

        DownloadFileInfo mDownloadFileInfo = new DownloadFileInfo();
        mDownloadFileInfo.setFileSize((long) -1);
//        mDownloadFileInfo.setFilePath(FILE_DOWNLOAD_DIR);
//        mDownloadFileInfo.setTempFilePath(FILE_DOWNLOAD_TEMP_DIR);
        mDownloadFileInfo.setDownloadUrl(url);
        mDownloadFileInfo.setHadDownloadSize((long) -1);
        mDownloadFileInfo.setDownloadProgress(0);
        mDownloadFileInfo.setVersion(0);

        //向数据库插入这条基本数据
        DBDaoImpl.getInstance(ctx).setDownloadFileInfo(mDownloadFileInfo);

        return getDownloadFileInfoWithUrl(url);
    }

    @Override
    public void Updata(DownloadFileInfo downloadFileInfo) {
        getDownloadFileInfoDao().update(downloadFileInfo);
    }

    public String DownloadFileInfot2String(DownloadFileInfo downloadFileInfo) {
        final StringBuffer sb = new StringBuffer("DownloadFileInfo{");
        sb.append("fileSize=").append(downloadFileInfo.getFileSize());
        sb.append(", fileName='").append(downloadFileInfo.getFileName()).append('\'');
        sb.append(", filePath='").append(downloadFileInfo.getFilePath()).append('\'');

        sb.append(", tempFilePath='").append(downloadFileInfo.getTempFilePath()).append('\'');
//        sb.append(", tempFileName='").append(downloadFileInfo.getTempFileName()).append('\'');

        sb.append(", downloadUrl='").append(downloadFileInfo.getDownloadUrl()).append('\'');

        sb.append(", hadDownloadSize='").append(downloadFileInfo.getHadDownloadSize()).append('\'');
        sb.append(", downloadProgress=").append(downloadFileInfo.getDownloadProgress());

        sb.append(", version='").append(downloadFileInfo.getVersion()).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
