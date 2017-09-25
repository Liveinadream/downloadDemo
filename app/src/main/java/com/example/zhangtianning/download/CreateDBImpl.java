package com.example.zhangtianning.download;


import de.greenrobot.daogenerator.DaoGenerator;
import de.greenrobot.daogenerator.Entity;
import de.greenrobot.daogenerator.Schema;

/**
 * Created by 冒险者ztn on 2017/9/11.
 */

public class CreateDBImpl {

    public static void main(String args[]) throws Exception {
        Schema schema = new Schema(1, "com.example.zhangtianning.download.dao");
        addDownloadFileSize(schema);
        DaoGenerator daoGenerator = new DaoGenerator();
        String PATH = "app/src/main/java/";
        daoGenerator.generateAll(schema, PATH);
    }

    private static void addDownloadFileSize(Schema schema) {
        Entity downloadInfo = schema.addEntity("DownloadFileInfo");
        downloadInfo.addIdProperty().primaryKey();

        downloadInfo.addLongProperty("fileSize");
        downloadInfo.addStringProperty("filePath");
        downloadInfo.addStringProperty("fileName");

        downloadInfo.addStringProperty("tempFilePath");

        downloadInfo.addStringProperty("downloadUrl");

        downloadInfo.addLongProperty("hadDownloadSize");
        downloadInfo.addIntProperty("downloadProgress");

        downloadInfo.addIntProperty("version");

    }
}
