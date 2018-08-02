package cn.redcdn.hvs.database;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabaseCorruptException;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Target;

import cn.redcdn.hvs.im.column.NoticesTable;
import cn.redcdn.hvs.im.column.NubeFriendColumn;
import cn.redcdn.hvs.util.CommonUtil;
import cn.redcdn.hvs.util.NotificationUtil;
import cn.redcdn.hvs.util.StringUtil;
import cn.redcdn.log.CustomLog;

/**
 * Desc
 * Created by wangkai on 2017/2/23.
 */

public class DatabaseHelper extends SQLiteOpenHelper{

    private final String TAG = "DatabaseHelper";

    private String dbFileName = "";
    private String dbFileFolder = "";
    private SQLiteDatabase myDataBase = null;

    // 数据库文件损坏case下,拷贝次数
    private static int copyCount = 0;

    // 数据库文件损坏case下，最大拷贝次数
    private static final int MAX_COPY_COUNT = 3;
    private Context mContext;

    private static DatabaseHelper mInstance;


    public DatabaseHelper(Context context, String dbName){
        super(context,dbName,null,DBConstant.DATABASE_VERSION);
        dbFileName = dbName;
        mContext = context;
        dbFileFolder = DBConstant.SQLITE_FILE_ROM_FOLDER;
        CustomLog.d(TAG,"数据库路径为" + dbFileFolder);
        mInstance = this;
    }

    public static DatabaseHelper getInstance() {
        return mInstance;
    }

    public void release(){
        close();
        mInstance = null;
        myDataBase = null;
    }

    /**
     * 获取database实例
     * @return
     */
    public SQLiteDatabase getdatabase() {
        try {
            if (myDataBase == null || !myDataBase.isOpen()) {
                CustomLog.d(TAG,"myDataBase == null 或者 myDataBase 的场景");
                openDataBase();
            } else {
                CustomLog.d(TAG,"myDataBase打开的场景，直接返回db对象");
            }
        } catch (Exception e) {
            CustomLog.e(TAG,"SQLException" +  e.toString());
        }
        CustomLog.d(TAG,"getdatabase path:" + myDataBase != null ? myDataBase
                .getPath() : "null");
        return myDataBase;
    }

    public void openDataBase() throws SQLException {
        CustomLog.d(TAG,"打开数据库:" + dbFileFolder + dbFileName);

        try {

            File filepath = new File(dbFileFolder);
            if (!filepath.exists()) {
                filepath.mkdirs();
            }

            CustomLog.i(TAG,"getWritableDatabase exec begin");
            // 通过此方法，让SQLiteOpenHelper执行onCreate或onUpgrade，管理数据库的创建和升级
            getWritableDatabase();

            if (!StringUtil.isEmpty(dbFileFolder)) {
                File databases_dir = new File(dbFileFolder + dbFileName);
                if (!databases_dir.exists()) {
                    createDB();
                }
                if (null == myDataBase || !myDataBase.isOpen()) {
                    myDataBase = SQLiteDatabase.openDatabase(
                            dbFileFolder + '/' +  dbFileName, null,
                            SQLiteDatabase.NO_LOCALIZED_COLLATORS);
                }
            }
            boolean isOpen = myDataBase.isOpen();
            CustomLog.d(TAG,"得到的数据库,myDataBase:" + myDataBase != null ? myDataBase
                    .getPath() : "null");

            CustomLog.i(TAG,"getWritableDatabase exec end");
        } catch (SQLiteDatabaseCorruptException e) {
            CustomLog.e(TAG,"打开数据库异常1：" +  e.toString());
            // 最大拷贝次数为3
            if (copyCount >= MAX_COPY_COUNT) {
                CustomLog.d(TAG,"打开数据库SQLiteDatabaseCorruptException异常：拷贝数据库次数大于3");
            } else if (!CommonUtil.isFastDoubleClick()) {
                CustomLog.d(TAG,"打开数据库SQLiteDatabaseCorruptException异常：开始重新拷贝数据库:"
                        + copyCount);
                if (myDataBase != null) {
                    myDataBase.close();
                    myDataBase = null;
                }
                ++copyCount;
                copySqlite();
            }
        } catch (Exception ex) {
            CustomLog.e("打开数据库异常：", ex.toString());
        } finally {
            super.close();
        }
    }

    private void copySqlite() {
        File file = new File(dbFileFolder);
        if (file.exists()) {
            for (File files : file.listFiles()) {
                String fileName = files.getName();
                CustomLog.d(TAG,"fileName:" + fileName);
                if (files.isFile() && fileName.equals(dbFileName)) {
                    boolean deleteSuccess = files.delete();
                    CustomLog.d(TAG,"数据库更新前,删除【" + dbFileFolder + dbFileName
                            + "】是否成功:" + deleteSuccess);
                    break;
                }
            }
        }
        createDB();
    }

    private void createDB() {
        try {
            // 拷贝数据库到内存
            copySqlite2Rom(mContext);
        } catch (Exception e) {
            CustomLog.e(TAG,"内存空间不足，数据库拷贝失败" + e.toString());
            NotificationUtil.sendNoSpaceNotifacation();
        }
    }


    @Override
    public void onCreate(SQLiteDatabase db) {
        CustomLog.d(TAG,"onCreate begin");
        copySqlite();
        CustomLog.d(TAG,"onCreate end");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        CustomLog.i(TAG,"oldVersion=" + oldVersion + "|newVersion=" + newVersion);
        if (newVersion != oldVersion) {
            // 升级数据库
            CustomLog.d(TAG,"onUpgrade begin");
            upGradeDB(newVersion);
            CustomLog.d(TAG,"onUpgrade end");
        }
    }

    private void upGradeDB(int _version){
        if(_version == 2){
            try {
                if (myDataBase != null) {
                    myDataBase.close();
                    myDataBase = null;
                }
                copySqlite();

            } catch (Exception e) {
                CustomLog.e("copySqlite出现异常", e.toString());
            }
        }
    }

    /**
     * 复制数据库文件到手机内存
     *
     * @throws IOException
     */
    public void copySqlite2Rom(Context context)
            throws Exception {
        CustomLog.d(TAG,"copySqlite2Rom begin,");

        FileOutputStream fos = null;
        try {
            String newPath = DBConstant.SQLITE_FILE_ROM_FOLDER + dbFileName;
            File filedb = new File(newPath);
            if (filedb.exists()) {
                CustomLog.d(TAG,"手机内存中存在数据库文件:" + newPath);
            } else {
                CustomLog.d(TAG,"复制数据库文件到手机内存:" + newPath);
                fos = context.openFileOutput(dbFileName, Context.MODE_APPEND);
                byte[] b = new byte[context.getAssets()
                        .open(DBConstant.SQLITE_FILE_NAME_DEFAULT).available()];
                context.getAssets().open(DBConstant.SQLITE_FILE_NAME_DEFAULT)
                        .read(b);
                fos.write(b);
            }

        } catch (IOException e) {
            CustomLog.e(TAG,"ioexception" + e.toString());
            throw e;
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                    fos = null;
                } catch (IOException e) {
                    CustomLog.e(TAG, "copySqlite2Rom" + "fos.close() ioexception" + e.toString());
                }
            }
        }
        CustomLog.d(TAG,"copySqlite2Rom end,");
    }
}
