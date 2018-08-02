package cn.redcdn.hvs.im.fileTask;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.os.Message;
import android.widget.Toast;

import com.butel.connectevent.base.CommonConstant;
import com.butel.connectevent.utils.LogUtil;

import cn.redcdn.datacenter.collectcenter.DataBodyInfo;
import cn.redcdn.datacenter.enterprisecenter.data.AccountInfo;
import cn.redcdn.datacenter.medicalcenter.data.MDSAccountInfo;
import cn.redcdn.hvs.AccountManager;
import cn.redcdn.hvs.MedicalApplication;
import cn.redcdn.hvs.im.IMConstant;
import cn.redcdn.hvs.im.UrlConstant;
import cn.redcdn.hvs.im.agent.AppP2PAgentManager;
import cn.redcdn.hvs.im.bean.BookMeetingExInfo;
import cn.redcdn.hvs.im.bean.CollectionEntity;
import cn.redcdn.hvs.im.bean.FileTaskBean;
import cn.redcdn.hvs.im.bean.GroupMemberBean;
import cn.redcdn.hvs.im.bean.NoticesBean;
import cn.redcdn.hvs.im.bean.ShowNameUtil;
import cn.redcdn.hvs.im.column.CollectionTable;
import cn.redcdn.hvs.im.column.NoticesTable;
import cn.redcdn.hvs.im.dao.CollectionDao;
import cn.redcdn.hvs.im.dao.GroupDao;
import cn.redcdn.hvs.im.dao.NoticesDao;
import cn.redcdn.hvs.im.dao.ThreadsDao;
import cn.redcdn.hvs.im.manager.CollectionManager;
import cn.redcdn.hvs.im.util.CompressUtil;
import cn.redcdn.hvs.im.util.IMCommonUtil;
import cn.redcdn.hvs.im.util.xutils.http.HttpHandler;
import cn.redcdn.hvs.im.util.xutils.http.HttpUtils;
import cn.redcdn.hvs.im.util.xutils.http.client.HttpRequest;
import cn.redcdn.hvs.im.util.xutils.http.client.RequestParams;
import cn.redcdn.hvs.im.work.BizConstant;
import cn.redcdn.hvs.util.StringUtil;
import cn.redcdn.log.CustomLog;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import cn.redcdn.hvs.im.preference.DaoPreference.PrefType;

import static cn.redcdn.hvs.im.IMConstant.APP_ROOT_FOLDER;
import static cn.redcdn.hvs.im.manager.CollectionManager.isValidFilePath;

/***
 * 负责文件的上传、下载，并维护t_notices表的状态，
 * 若是上传文件，会发送消息至消息服务器，而且发送SIP通知，通知对方及时获取消息
 * 利用线程池，多记录并发执行；单个记录中的多个文件是顺序下载或上传
 *
 * @author miaolikui
 */

public class FileTaskManager {

    private static final String TAG = "FileTaskManager";
    private Context mcontext = null;
    private Map<String, List<FileTaskBean>> fileTaskMap
        = new ConcurrentHashMap<String, List<FileTaskBean>>();
        private Map<String, HttpHandler> runTaskQueue = new ConcurrentHashMap<String, HttpHandler>();
    private NoticesDao noticedao;

    // handler消息
    public static final int MSG_SUCCESS = 2001;
    public static final int MSG_FAILURE = 2002;
    //(废弃2003值的使用)
    public static final int MSG_VCARD_SAVE = 2003;
    public static final int MSG_IMAGE_COMPRESSED = 2004;
    //(废弃2005值的使用)
    public static final int MSG_VIDEO_THUMB_SEND = 2005;
    public static final int NO_SDCARD = 2006;
    //(废弃2007值的使用)
    public static final int SAVE_SDCARD = 2007;

    // 下载文件保存的文件夹
    //    public static String FILE_DIR = CommonConstant.SAVE_DIR_NAME;
    // 压缩文件保存的文件夹
    public static String FILE_COMPRESS_DIR = "compress";
    // 接收到的文件保存的文件夹
    public static String FILE_DOWNLOAD_DIR = "download";
    // 本地录制文件保存的文件夹（视频和声音）
    public static String FILE_RECORD_DIR = "record";
    // 调用系统相机拍照保存的路径
    public static String FILE_TAKE_PHOTO_DIR = "takePhoto";
    // 名片分享产生的vcf文件保存的路径
    public static String FILE_VCARD_DIR = "vcard";

    // 每条消息（FileTaskBean）的状态
    public static final int TASK_STATUS_READY = 0;
    public static final int TASK_STATUS_RUNNING = 1;
    public static final int TASK_STATUS_SUCCESS = 2;
    public static final int TASK_STATUS_FAIL = 3;
    // 图片压缩中 (READY->COMPRESSING->RUNNING->SUCCESS/FAIL)
    // 仅是内存中的状态，不会修改数据库中的值
    public static final int TASK_STATUS_COMPRESSING = 4;

    // 发送的类型
    // 好友邀请（添加好友发送认证、通话‘禁止陌生人来电’发送认证）
    public static final int NOTICE_TYPE_FRIEND_SEND = 1;
    // 图片文件分享
    public static final int NOTICE_TYPE_PHOTO_SEND = 2;
    // 视频文件分享
    public static final int NOTICE_TYPE_VEDIO_SEND = 3;
    // 名片分享
    public static final int NOTICE_TYPE_VCARD_SEND = 4;
    // 未接来电提醒
    public static final int NOTICE_TYPE_IPCALL_SEND = 5;
    // 通过回执（新朋友中点‘加为好友’、平板‘加好友’操作）
    public static final int NOTICE_TYPE_FEEEDBACK_SEND = 6;
    // 声音分享
    public static final int NOTICE_TYPE_AUDIO_SEND = 7;
    // 文字分享
    public static final int NOTICE_TYPE_TXT_SEND = 8;
    // 添加好友信息
    public static final int NOTICE_TYPE_DESCRIPTION = 9;

    //========================极会议集成添加===========================
    // 通话记录
    public static final int NOTICE_TYPE_RECORD = 10;
    // 会议邀请
    public static final int NOTICE_TYPE_MEETING_INVITE = 11;
    // 预约会议
    public static final int NOTICE_TYPE_MEETING_BOOK = 12;

    //附件  类型
    public static final int NOTICE_TYPE_URL = 20;//20160512 URL格式，目前只在收藏时使用，转消息发送时，需要转换成文本

    public static final int NOTICE_TYPE_FILE = 21;//20160512附件格式，目前只在收藏时使用

    private MDSAccountInfo userAccountInfo = null;

    private CollectionDao mCollectionDao = null;


        public static long PICTURE_COMPRESSION = IMConstant.DEFAULT_IMAGE_SEND_SIZE;
    //
    //    private CollectionDao mCollectionDao = null;
    //    // 当前使用的页面(仅在弹保存联系人对话框时使用)
    //    public Activity myActivity;
    //    public Activity getMyActivity() {
    //        return myActivity;
    //    }
    //
    //    public void setMyActivity(Activity myActivity) {
    //        this.myActivity = myActivity;
    //    }
    //
        /***
         *  清除5天以外的压缩文件（独立工作线程）
         */
        public static void clearCompressedFiles(){
            if (!Environment.MEDIA_MOUNTED.equals(Environment
                    .getExternalStorageState())) {
                return ;
            }
            String dirpath = Environment.getExternalStorageDirectory()
                    + File.separator + IMConstant.APP_ROOT_FOLDER
                    + File.separator + FILE_COMPRESS_DIR;
            LogUtil.d("清除5天以外的压缩文件,dirpath=" + dirpath);
            final File file = new File(dirpath);
            if (file.exists()) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {

                        try {
                            File[] files = file.listFiles();
                            if (null != files&&files.length>0) {
                                for (File file : files) {
                                    if (file.lastModified() < (System
                                            .currentTimeMillis() - (long) 5
                                            * 24 * 60 * 60 * 1000)) {
                                        file.delete();
                                    }
                                }
                            }
                        } catch (Exception e) {
                            LogUtil.e("删除文件异常", e);
                            LogUtil.e("清除5天以外的压缩文件异常", e);
                        }
                    }
                }).start();
            }
        }

        /***
         * 压缩图片（独立工作线程），完成后（不论成功与否）handler通知主线程
         * @param uuid
         * @param filepath
         */
        private void compressImageThread(final String uuid, final String filepath){
            new Thread(new Runnable(){
                @Override
                public void run() {
                    CustomLog.d(TAG,"compressImageThread,uuid=" + uuid + "|filepath=" + filepath);

                    String outpath = getCompressFilePath(filepath);
                    boolean success = CompressUtil.compressImage(filepath, outpath);
                    CustomLog.d(TAG,"CompressUtil.compressImage,outpath=" + outpath + "|success=" + success);

                    Message msg = syncHandler.obtainMessage();
                    msg.what = MSG_IMAGE_COMPRESSED;
                    Bundle data = new Bundle();
                    data.putBoolean("compress_success", success);
                    data.putString("compress_uuid", uuid);
                    data.putString("compress_srcpath", filepath);
                    data.putString("compress_outpath", outpath);
                    msg.setData(data);
                    syncHandler.sendMessage(msg);
                }
            }
            ).start();
        }

        private String getCompressFilePath(String srcfilepath) {
            CustomLog.d(TAG,"getCompressFilePath begin,srcfilePaht" + srcfilepath);

            int index = srcfilepath.lastIndexOf(File.separator);
            String lastpart = srcfilepath.substring(index);
            String path = Environment.getExternalStorageDirectory()
                    + File.separator + IMConstant.APP_ROOT_FOLDER
                    + File.separator + FILE_COMPRESS_DIR
                    + lastpart;
            CustomLog.d(TAG,"path:" + path);
            return path;
        }


        private void updateCompressPath2Bean(String uuid, String srcpath, String compressedpath){
            CustomLog.d(TAG,"updateCompressPath2Bean begin,uuid:" + uuid + "|srcpath:" + srcpath + "|compressedpath:" + compressedpath);
            if (TextUtils.isEmpty(uuid)) {
                return ;
            }
            if (fileTaskMap == null || !fileTaskMap.containsKey(uuid)) {
                return ;
            }

            List<FileTaskBean> tempList = fileTaskMap.get(uuid);
            if(tempList==null||tempList.size()==0){
                return;
            }
            FileTaskBean bean = null;
            int length = tempList.size();
            for (int i = 0; i < length; i++) {
                bean = tempList.get(i);
                if (uuid.equals(bean.getUuid())
                        &&srcpath.equals(bean.getSrcUrl())) {
                    bean.setCompressedPath(compressedpath);
                    break;
                }
            }
        }

        private Handler syncHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {

                super.handleMessage(msg);
                switch (msg.what) {
                    case NO_SDCARD:
                        CustomLog.d(TAG,"NO_SDCARD,Toast:外部存储卡没有准备好，无法下载");
                        Toast.makeText(mcontext, "外部存储卡没有准备好，无法下载", Toast.LENGTH_SHORT).show();
                        //TODO:需要清理内存中的map数据
                        break;

                    case MSG_IMAGE_COMPRESSED:{
                        CustomLog.d(TAG,"MSG_IMAGE_COMPRESSED");
                        Bundle data = msg.getData();
                        if(data!=null){
                            boolean success = data.getBoolean("compress_success");
                            String uuid = data.getString("compress_uuid");
                            String srcpath = data.getString("compress_srcpath");
                            String outpath = data.getString("compress_outpath");
                            if(success){
                                updateCompressPath2Bean(uuid,srcpath,outpath);
                            }
                            startFirstValidFileTask(uuid);
                        }
                    }
                    break;

                    case MSG_SUCCESS: {

                        Bundle data = msg.getData();
                        String uuid = data.getString("uuid");
                        String srcUrl = data.getString("srcUrl");
                        CustomLog.d(TAG,"MSG_SUCCESS，uuid:" + uuid + "|srcUrl:" + srcUrl);
                        FileTaskBean bean = findRecentlyRunningFileTask(uuid,srcUrl);
                        if (bean != null) {
                            bean.convertSuccessStringToResultUrl();
                            // 更改压缩后文件后缀名为空
                            if (bean.getType() == NOTICE_TYPE_PHOTO_SEND
                                    && bean.getStatus() == TASK_STATUS_SUCCESS) {
                                String compressedpath = bean.getCompressedPath();
                                if (!TextUtils.isEmpty(compressedpath)) {
                                    String newpath = compressedpath.replace(".",
                                            System.currentTimeMillis() + "");
                                    File old = new File(compressedpath);
                                    if (old.exists()) {
                                        old.renameTo(new File(newpath));
                                    }
                                }
                            }

                            if (bean.getStatus() == TASK_STATUS_SUCCESS) {
                                //处理单独下载文件的情况
                                if(bean.isSingleDownload()){
                                    // updateTaskStatus(uuid, TASK_STATUS_SUCCESS, true);
                                    //TODO:20141127 在新的消息需求中，每条消息只有一张图片或视频；
                                    //此处暂可认为完全成功；（理想情况下，应该check下其他同记录中的taskbean）
                                    // 仅保存body字段，不修改消息中的状态
                                    updateBodybutTaskStatus(uuid);
                                    CustomLog.d(TAG,"fileTaskMap.remove:" + uuid);
                                    fileTaskMap.remove(uuid);
                                    runTaskQueue.remove(getKeyString(srcUrl));
                                    return;
                                }
                                if (bean.getIndex() + 1 == bean.getTotal_count()) {
                                    if (!bean.isFrom()) {
                                        //发送的消息
                                        sendMessageBody(bean.getType(), uuid, srcUrl);
                                    } else {
                                        //接收的消息
                                        updateTaskStatus(uuid, TASK_STATUS_SUCCESS,
                                                true);
                                        CustomLog.d(TAG,"fileTaskMap.remove:" + uuid);
                                        fileTaskMap.remove(uuid);
                                        runTaskQueue.remove(getKeyString(srcUrl));
                                    }
                                } else {
                                    startFirstValidFileTask(uuid);
                                }
                            } else {
                                // miaolk add 20140314
                                if(!TextUtils.isEmpty(bean.getErrorTip())){
                                    CustomLog.d(TAG,"Toast:" + bean.getErrorTip());
                                    Toast.makeText(mcontext, bean.getErrorTip(),
                                            Toast.LENGTH_SHORT).show();
                                }

                                if(bean.isSingleDownload()){
                                    updateBodybutTaskStatus(uuid);
                                }else{
                                    updateTaskStatus(uuid, TASK_STATUS_FAIL, true);
                                }

                                CustomLog.d(TAG,"fileTaskMap.remove:" + uuid);
                                fileTaskMap.remove(uuid);
                                runTaskQueue.remove(getKeyString(srcUrl));
                            }
                        }
                    }
                    break;
                    case MSG_FAILURE: {
                        Bundle data = msg.getData();
                        String uuid = data.getString("uuid");
                        String srcUrl = data.getString("srcUrl");
                        CustomLog.d(TAG,"MSG_FAILURE,uuid:" + uuid + "|srcUrl:" + srcUrl);
                        FileTaskBean bean = findRecentlyRunningFileTask(uuid,srcUrl);
                        if (bean != null) {
                            // 下载失败的场景下，主动删除temp文件
                            String destFileName = bean.getResultUrl();
                            if (bean.isFrom() && !TextUtils.isEmpty(destFileName)) {
                                // 判断文件本地文件是否存在，
                                File file = new File(destFileName);
                                if (file != null && file.exists()) {
                                    file.delete();
                                    file = null;
                                }
                                bean.setResultUrl("");
                            }

                            bean.setStatus(TASK_STATUS_FAIL);
                            if(bean.isSingleDownload()){
                                updateBodybutTaskStatus(uuid);
                            }else{
                                updateTaskStatus(uuid, TASK_STATUS_FAIL, true);
                            }
                            CustomLog.d(TAG,"fileTaskMap.remove:" + uuid);
                            fileTaskMap.remove(uuid);
                            runTaskQueue.remove(getKeyString(srcUrl));
                        }
                    }
                    break;
                }
            }
        };
    //
    public FileTaskManager(Context context) {
        noticedao = new NoticesDao(context);
        //        mCollectionDao = new CollectionDao(context);
        mcontext = context;
        initStorageDir();
        initCompressDir();
        initRecordDir();
        initTakePhotoDir();
        initVCFDir();
//                initParams();
        userAccountInfo = AccountManager.getInstance(mcontext).getAccountInfo();
    }


    //
    //    private void initParams() {
    //        // 客户端图片压缩上限
    //        String picCompr = NetPhoneApplication.getPreference().getKeyValue(
    //                PrefType.PICTURE_COMPRESSION, "");
    //        if (!TextUtils.isEmpty(picCompr)) {
    //            try {
    //                // 单位KB
    //                PICTURE_COMPRESSION = Long.parseLong(picCompr) * 1024;
    //            } catch (NumberFormatException e) {
    //                LogUtil.e("NumberFormatException", e);
    //            }
    //        }
    //    }

        /**
         * @author: zhaguitao
         * @Title: updateRunningTask2Fail
         * @Description: 打开应用时，需要将执行中的消息状态改为失败，
         *              退出则认为停止任务，以便再次进入应用时，可以重新开始任务
         * @date: 2014-2-19 上午11:38:51
         */
        public void updateRunningTask2Fail() {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    LogUtil.d("updateRunningTask2Fail");
                    Cursor cursor = null;
                    try {
                        cursor = noticedao.queryAllRunningNotices();
                        if (cursor != null && cursor.getCount() > 0) {
                            cursor.moveToFirst();
                            int idIdx = cursor
                                    .getColumnIndex(NoticesTable.NOTICE_COLUMN_ID);
                            String uuid = "";
                            do {
                                uuid = cursor.getString(idIdx);
                                // 正在执行状态的消息，状态更新为失败
                                updateTaskStatus(uuid, TASK_STATUS_FAIL, false);
                                removeMap(uuid);
                            } while (cursor.moveToNext());
                        }
                    } catch (Exception e) {
                        LogUtil.e("updateRunningTask2Fail", e);
                    } finally {
                        if (cursor != null) {
                            cursor.close();
                            cursor = null;
                        }
                    }
                }
            }).start();
        }

    //    public void startAllNoRunningSendTask() {
    //        // 准备分享或者分享失败的任务，重新开始
    //        Cursor cursor = null;
    //        try {
    //            cursor = noticedao.queryNoRunningSendNotices();
    //            if (cursor != null && cursor.getCount() > 0) {
    //                cursor.moveToFirst();
    //                int idIdx = cursor
    //                        .getColumnIndex(NoticesTable.NOTICE_COLUMN_ID);
    //                do {
    //                    final String uuid = cursor.getString(idIdx);
    //                    new Thread(new Runnable() {
    //                        @Override
    //                        public void run() {
    //                            Looper.prepare();
    //                            executeTask(uuid, null);
    //                            Looper.loop();
    //                        }
    //                    }).start();
    //                } while (cursor.moveToNext());
    //            }
    //        } catch (Exception e) {
    //            LogUtil.e("startAllNoRunningSendTask:分享任务失败",e);
    //        } finally {
    //            if (cursor != null) {
    //                cursor.close();
    //                cursor = null;
    //            }
    //        }
    //    }
    //
        /***
         * 从数据库中找出该记录，转化成FileTaskBean,并启动首个filetask
         * @param uuid
         * @param uiInterfaces
         */
        public void addTask(final String uuid, final ChangeUIInterface uiInterfaces) {

            new Thread(new Runnable() {
                @Override
                public void run() {
                    CustomLog.d("FileTaskManager","addTask");
                    Looper.prepare();
                    executeTask(uuid, uiInterfaces);
                    Looper.loop();
                }
            }).start();
        }

        /**
         * 绑定上传下载进度
         */
        public void setChgUIInterface(final String uuid,
                                      ChangeUIInterface uiInterfaces) {
            List<FileTaskBean> templist = findFileTasks(uuid);
            if (templist != null && templist.size() >= 0) {
                if (uiInterfaces != null) {
                    for (FileTaskBean item : templist) {
                        CustomLog.d(TAG,"setChangui:" + uuid);
                        item.setChangui(uiInterfaces);
                        item.setPauseUiChange(false);
                    }
                }
            }
        }

        private synchronized boolean executeTask(String uuid, ChangeUIInterface uiInterfaces) {
            CustomLog.d("FileTaskManager","executeTask begin uuid:" + uuid);

            boolean error = false;

            boolean existed = false;
            List<FileTaskBean> templist = null;
            // 是否已经开启了该任务
            templist = findFileTasks(uuid);
            if (templist == null || templist.size() == 0) {
                // 没有开启的情况下，构建file task list
                templist = createFileTaskList(uuid,false);
            } else {
                existed = true;
            }
            if (templist == null || templist.size() == 0) {
                return false;
            }

            if (uiInterfaces != null) {
                for (FileTaskBean item : templist) {
                    item.setChangui(uiInterfaces);
                    item.setPauseUiChange(false);
                }
            }
            if (existed) {
                // 已开启的情况下，只需要把uiInterfaces 设置到file task中即可
                return true;
            }
            CustomLog.d(TAG,"fileTaskMap.put:" + uuid);
            fileTaskMap.put(uuid, templist);

            updateTaskStatus(uuid, TASK_STATUS_RUNNING, false);
            // 启动任务
            if (!error) {
                if(!templist.get(0).isFrom()){
                    error = sendSCIMMsg(uuid);
                }else{
                    error = startFirstValidFileTask(uuid);
                }
            }
            // 如果启动失败，删除刚加入的filetasks
            if (!error) {
                CustomLog.d(TAG,"fileTaskMap.remove:" + uuid);
                fileTaskMap.remove(uuid);
            } else {
    //            updateTaskStatus(uuid, TASK_STATUS_RUNNING, false);
            }
            templist = null;
            return true;
        }

        /***
         * 根据UUID 和 url,启动单个文件的下载
         * @param uuid
         * @param url
         * @param uiInterfaces  进度展示回调方法
         */
        public void addSingleFileDownloadTask(final String uuid, final String url,
                                              final boolean froceDownload, final ChangeUIInterface uiInterfaces) {
            CustomLog.d(TAG,"uuid=" + uuid + "|url=" + url);

            new Thread(new Runnable() {
                @Override
                public void run() {
                    Looper.prepare();
                    executeSingleFileTask(uuid, url, froceDownload, uiInterfaces);
                    Looper.loop();
                }
            }).start();
        }

        private boolean executeSingleFileTask(String uuid, String url, boolean froceDownload,
                                              ChangeUIInterface uiInterfaces) {
            CustomLog.d(TAG,"uuid=" + uuid + "|url=" + url);

            boolean existed = false;
            List<FileTaskBean> templist = null;
            // 是否已经开启了该任务
            templist = findFileTasks(uuid);
            if (templist == null || templist.size() == 0) {
                // 没有开启的情况下，构建file task list
                templist = createFileTaskList(uuid,froceDownload);
            } else {
                existed = true;
            }
            if (templist == null || templist.size() == 0) {
                return false;
            }

            // 找到该下载的bean对象
            FileTaskBean bean = null;
            for (FileTaskBean item : templist) {
                if (item.getSrcUrl().equals(url)) {
                    bean = item;
                    if (uiInterfaces != null) {
                        item.setChangui(uiInterfaces);
                        item.setPauseUiChange(false);
                    }
                    // 如果该文件的记录不是正在下载（existed==true 内存中有其map记录）
                    // 而且该处于待下载状态，则设置单文件现在标志;
                    if (!existed && item.getStatus() == TASK_STATUS_READY) {
                        item.setSingleDownload(true);
                    }
                }
            }

            fileTaskMap.put(uuid, templist);
            templist = null;
            // 启动任务
            if (bean != null
                    && (bean.getStatus() == TASK_STATUS_READY || bean.getStatus() == TASK_STATUS_FAIL)) {
                DownFileRequestCallBack callback = new DownFileRequestCallBack(
                        bean, syncHandler);
                download(bean.getSrcUrl(), bean.getUuid(),callback);
                bean.setStatus(TASK_STATUS_RUNNING);
            }

            return true;
        }

        /**
         * @author: zhaguitao
         * @Title: delTaskAndFile
         * @Description: 停止任务，删除下载的文件，删除消息
         * @param uuid
         * @date: 2014-1-15 下午5:55:04
         */
        public void cancelTaskAndDelFile(final String uuid) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    CustomLog.d(TAG,"cancelTaskAndDelFile begin");
                    List<FileTaskBean> templist = findFileTasks(uuid);
                    if (templist == null || templist.size() == 0) {
                        templist = createFileTaskList(uuid,false);
                    } else {
                        for (FileTaskBean bean : templist) {
                            if (bean.getStatus() == TASK_STATUS_RUNNING) {
                                // 若任务已启动，则先取消任务
                                bean.setStatus(TASK_STATUS_FAIL);
                                String key = getKeyString(bean.getSrcUrl());
                                HttpHandler runhandler = runTaskQueue.get(key);
                                if (runhandler != null) {
                                    runhandler.cancel(true);
                                }
                                runTaskQueue.remove(key);
                            }
                        }
                    }

                    if (templist != null && templist.size() > 0) {
                        for (FileTaskBean bean : templist) {
                            delTaskFile(bean);
                        }
                    }

                    // 删除消息
                    int count = noticedao.deleteNotice(uuid);
                    boolean isSuccess = false;
                    if (count > 0) {
                        isSuccess = true;
                    } else {
                        isSuccess = false;
                    }
                    CustomLog.d(TAG,"cancelTaskAndDelFile,删除消息:" + uuid + "|" + isSuccess);
                }
            }).start();
        }

        /**
         * @author: zhaguitao
         * @Title: delTaskFile
         * @Description: 删除任务文件
         * @param bean
         * @date: 2014-1-15 下午7:23:13
         */
        private void delTaskFile(FileTaskBean bean) {
            String localFilePath = "";
            int type = bean.getType();
            switch (type) {
                case NOTICE_TYPE_VEDIO_SEND:
                case NOTICE_TYPE_VCARD_SEND:
                case NOTICE_TYPE_PHOTO_SEND:
                case NOTICE_TYPE_AUDIO_SEND:
                    if(!bean.isFrom()){
                        // 发送消息的场合
                        localFilePath = bean.getSrcUrl();
                    }else{
                        // 接收消息的场合
                        localFilePath = bean.getResultUrl();
                    }
                    break;
                default:
                    break;
            }

            CustomLog.d(TAG,"type:" + type + "|localFilePath:" + localFilePath);
            final File file = new File(localFilePath);
            if (file.exists()) {
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        // 文件在任务停止2s后删除，以防任务还未停止，文件还在读写，无法删除
                        boolean success = file.delete();
                        CustomLog.d(TAG,"删除文件:"
                                + file.getAbsolutePath() + "|" + success);
                    }
                }, 2000);
            }
        }

        /***
         * 在外面的列表中滚动或删除时调用
         * @param uuid
         */
        public void cancelTask(final String uuid) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Looper.prepare();
                    cancelRunningTask(uuid);
                    Looper.loop();
                }
            }).start();
        }

        /***
         * 从FileTaskBean list中找到正在运行的filetask,获得srcUrl
         * 由srcUrl转成key，从runTaskQueue中找到handler,取消任务
         * 回写已经操作的结果到本地数据库body字段中
         * @param uuid
         * @return
         */
        private boolean cancelRunningTask(String uuid) {
            CustomLog.d(TAG,"cancelRunningTask begin uuid:" + uuid);

            FileTaskBean bean = null;
            // 从FileTaskBean list中找到正在运行的filetask,
            List<FileTaskBean>  templist = findFileTasks(uuid);
            if(templist==null||templist.size()==0){
                return false;
            }
            int length = templist.size();
            int i =0;
            for(i=0;i<length;i++){
                bean = templist.get(i);
                if(bean.getStatus()!=TASK_STATUS_SUCCESS){
                    break;
                }
            }
            if(i<length){
                if(bean.getStatus()==TASK_STATUS_RUNNING){
                    bean.setStatus(TASK_STATUS_FAIL);
                    // 由srcUrl转成key，从runTaskQueue中找到handler,取消任务
                    String key = getKeyString(bean.getSrcUrl());
                    HttpHandler runhandler = runTaskQueue.get(key);
                    if (runhandler != null) {
                        runhandler.cancel(true);
                    }
                    runTaskQueue.remove(key);
                }
                bean.setStatus(TASK_STATUS_FAIL);
                // 回写已经操作的结果到本地数据库body字段中
                updateTaskStatus(uuid, TASK_STATUS_FAIL, true);
                CustomLog.d(TAG,"fileTaskMap.remove:" + uuid);
                fileTaskMap.remove(uuid);
                return true;

            }else{
                if(bean.getStatus()==TASK_STATUS_SUCCESS){
                    // 全部成功处理完成，不手动改变其为失败状态
                    return false;
                }else{
                    if(bean.getStatus()==TASK_STATUS_RUNNING){
                        bean.setStatus(TASK_STATUS_FAIL);
                        String key = getKeyString(bean.getSrcUrl());
                        HttpHandler runhandler = runTaskQueue.get(key);
                        if (runhandler != null) {
                            runhandler.cancel(true);
                        }
                        runTaskQueue.remove(key);
                    }
                    bean.setStatus(TASK_STATUS_FAIL);
                    updateTaskStatus(uuid, TASK_STATUS_FAIL, true);
                    CustomLog.d(TAG,"fileTaskMap.remove:" + uuid);
                    fileTaskMap.remove(uuid);
                    return true;
                }
            }
        }

        /***
         * 解析body中的JSON串， 把动态消息的一条记录转换为FileTaskBean list
         *
         * @param uuid
         * @param body
         * @param type
         * @param from   true:接收到的消息;
         *               false:发送的消息
         * @return
         */
        public synchronized List<FileTaskBean> getFileTaskListByBody(String uuid, String body,
                                                                     int type, boolean from) {

            CustomLog.d(TAG,"uuid:" + uuid
                    + "|body:" + body
                    + "|type:" + type
                    + "|from:" + from);

            if (!TextUtils.isEmpty(body)) {
                List<FileTaskBean> tasklist = null;
                switch (type) {

                    case NOTICE_TYPE_IPCALL_SEND:
                    case NOTICE_TYPE_FRIEND_SEND:
                        break;
                    case NOTICE_TYPE_FILE:
                    case NOTICE_TYPE_TXT_SEND:
                    case NOTICE_TYPE_MEETING_INVITE:
                    case NOTICE_TYPE_MEETING_BOOK:
                    case NOTICE_TYPE_VEDIO_SEND:
                    case NOTICE_TYPE_VCARD_SEND:
                    case NOTICE_TYPE_PHOTO_SEND:
                    case NOTICE_TYPE_AUDIO_SEND:{
                        try {
                            JSONArray array = new JSONArray(body);
                            JSONObject obj = null;
                            tasklist = new ArrayList<FileTaskBean>();
                            int length = array.length();
                            for (int i = 0; i < length; i++) {
                                FileTaskBean filetask = new FileTaskBean();
                                filetask.setFrom(from);
                                obj = array.getJSONObject(i);
                                filetask.setRawBodyItemData(obj.toString());

                                String local = obj.optString("localUrl");
                                String romote = obj.optString("remoteUrl");
                                long filesize = obj.optLong("size");
                                String thumb = obj.optString("thumbnail");
                                String compressedpath = obj.optString("compressPath");
    //						boolean oversize = obj.optBoolean("overSize");
                                filetask.setCompressedPath(compressedpath);
    //						filetask.setOverSized(oversize);
                                filetask.setFilesize(filesize);
                                if(!from){
                                    // 发送的消息，本地路径为源，服务器端的URL为结果
                                    filetask.setSrcUrl(local);
                                    filetask.setResultUrl(romote);
                                }else{
                                    // 接收的消息，服务器端的URL为源，本地路径为结果
                                    filetask.setSrcUrl(romote);
                                    filetask.setResultUrl(local);
                                }
                                filetask.setThumbnailUrl(thumb);
                                filetask.setUuid(uuid);
                                filetask.setType(type);
                                filetask.setIndex(i);
                                filetask.setTotal_count(length);
                                if(!from){
                                    // 发送的消息，如果有服务器的URL则认为成功过，设置为SUCCESS
                                    // 否则设置为READY，需要上传服务器
                                    if(NOTICE_TYPE_TXT_SEND==type||
                                            NOTICE_TYPE_MEETING_INVITE==type||
                                            NOTICE_TYPE_MEETING_BOOK==type||NOTICE_TYPE_FILE==type){
                                        filetask.setStatus(TASK_STATUS_SUCCESS);
                                    }else{
                                        if (TextUtils.isEmpty(romote)) {
                                            filetask.setStatus(TASK_STATUS_READY);
                                        } else {
                                            filetask.setStatus(TASK_STATUS_SUCCESS);
                                        }
                                    }
                                }else{
                                    // 接收到的消息
                                    if(NOTICE_TYPE_TXT_SEND==type||
                                            NOTICE_TYPE_MEETING_INVITE==type||
                                            NOTICE_TYPE_MEETING_BOOK==type||NOTICE_TYPE_FILE==type){
                                        filetask.setStatus(TASK_STATUS_SUCCESS);
                                    }else{
                                        if (!TextUtils.isEmpty(local)) {
                                            String filepath = getLocalPathFromURL(romote,
                                                    filetask.getUuid(),false);
                                            File file = new File(filepath);
                                            if (file != null && file.exists()) {
                                                filetask.setResultUrl(filepath);
                                                // 接收到的消息，本地文件路径不为空，且文件存在
                                                filetask.setStatus(TASK_STATUS_SUCCESS);
                                            } else {
                                                // 接收到的消息，本地文件路径不为空，但文件不存在
                                                // 需要重新下载
                                                String tmpfilepath = getLocalPathFromURL(
                                                        romote, filetask.getUuid(), true);
                                                filetask.setResultUrl(tmpfilepath);
                                                filetask.setStatus(TASK_STATUS_READY);
                                            }
                                        } else {
                                            // 接收到的消息,本地文件路径为空,以temp为后缀的临时文件
                                            // 进行下载
                                            filetask.setResultUrl(getLocalPathFromURL(romote,
                                                    filetask.getUuid(),true));
                                            filetask.setStatus(TASK_STATUS_READY);
                                        }
                                    }
                                }

                                tasklist.add(filetask);
                            }
                        } catch (JSONException e) {
                            CustomLog.e(TAG,"JSONException" + e.toString());
                        }
                    }
                    break;

                }
                return tasklist;
            }
            return null;
        }
    //
        /***
         * 根据UUID，把动态消息的一条记录转换为FileTaskBean list
         * @param uuid
         * @return
         */
        public synchronized List<FileTaskBean> createFileTaskList(String uuid, boolean froceDownload) {
            CustomLog.d(TAG,"createFileTaskList begin,uudi:" + uuid);
            NoticesBean bean = noticedao.getNoticeById(uuid);
            if (bean != null) {
                boolean from = false;
                String own = AccountManager.getInstance(mcontext).getAccountInfo().nube;
                if(bean.getSender().endsWith(own)){
                    from = false;
                }else{
                    from = true;
                }

                if(froceDownload){
                    from = true;
                }
                // 已发送成功的消息不再发送；
                // 但接收到的消息，可能因为已下载的图片、视频被删除;
                // 用户又再次查看，需要再次加入任务经行下载
                if(!from&&bean.getStatus() == TASK_STATUS_SUCCESS){
                    CustomLog.d(TAG,"createFileTaskList 发送任务已成功，无需再次发送");
                    return null;
                }

                return getFileTaskListByBody(uuid, bean.getBody(), bean.getType(),from);
            }
            return null;
        }

        /***
         * 启动一个需要下载或上传的FileTaskBean
         * @param uuid
         * @return
         */
        private synchronized boolean startFirstValidFileTask(String uuid) {
            CustomLog.d(TAG,"startFirstValidFileTask begin uuid:" + uuid);
            // 找到第一个需要启动的filetask, 启动http接口进行网络连接
            FileTaskBean bean = findFirstValidFileTask(uuid);
            if (bean != null) {
                // 找到的是最后一个文件，并且是成功状态（文件全下载完成或全部上传完毕，只是任务的消息状态为失败）
                if ((bean.getStatus() == TASK_STATUS_SUCCESS)
                        && (bean.getIndex() + 1 == bean.getTotal_count())) {
                    Message msg = syncHandler.obtainMessage();
                    msg.what = FileTaskManager.MSG_SUCCESS;
                    Bundle data = new Bundle();
                    data.putString("uuid", bean.getUuid());
                    data.putString("srcUrl", bean.getSrcUrl());
                    msg.setData(data);
                    syncHandler.sendMessage(msg);
                    return true;
                }

                switch (bean.getType()) {
                    case NOTICE_TYPE_FRIEND_SEND:
                    case NOTICE_TYPE_IPCALL_SEND:
                        break;
                    case NOTICE_TYPE_VEDIO_SEND: {
                        CustomLog.d(TAG,"NOTICE_TYPE_VEDIO_SEND");
                        if (bean.isFrom()) {
                            DownFileRequestCallBack callback = new DownFileRequestCallBack(
                                    bean, syncHandler);
                            download(bean.getSrcUrl(), bean.getUuid(), callback);
                            bean.setStatus(TASK_STATUS_RUNNING);
                        } else {
                            CommomFileRequestCallBack callback = new CommomFileRequestCallBack(
                                    bean, syncHandler);
                            upload(bean.getSrcUrl(), "", callback);
                            bean.setStatus(TASK_STATUS_RUNNING);
                        }
                    }
                    break;
                    case NOTICE_TYPE_VCARD_SEND:
                    case NOTICE_TYPE_AUDIO_SEND: {
                        if (bean.isFrom()) {
                            DownFileRequestCallBack callback = new DownFileRequestCallBack(
                                    bean, syncHandler);
                            download(bean.getSrcUrl(), bean.getUuid(), callback);
                            bean.setStatus(TASK_STATUS_RUNNING);
                        } else {
                            CommomFileRequestCallBack callback = new CommomFileRequestCallBack(
                                    bean, syncHandler);
                            upload(bean.getSrcUrl(), "", callback);
                            bean.setStatus(TASK_STATUS_RUNNING);
                        }
                    }
                    break;
                    case NOTICE_TYPE_PHOTO_SEND: {
                        if (bean.isFrom()) {
                            DownFileRequestCallBack callback = new DownFileRequestCallBack(
                                    bean, syncHandler);
                            download(bean.getSrcUrl(), bean.getUuid(), callback);
                            bean.setStatus(TASK_STATUS_RUNNING);
                        } else {
                            if (bean.getFilesize() > PICTURE_COMPRESSION) {
                                // 需要增加个压缩过程
                                String compressedpath = bean.getCompressedPath();
                                if (TASK_STATUS_COMPRESSING == bean.getStatus()
                                        && TextUtils.isEmpty(compressedpath)) {
                                    CommomFileRequestCallBack callback = new CommomFileRequestCallBack(
                                            bean, syncHandler);
                                    upload(bean.getSrcUrl(), "", callback);
                                    bean.setStatus(TASK_STATUS_RUNNING);
                                    return true;
                                }

                                // 先判断源文件是否存在，不存在，则不需要压缩
                                if (TextUtils.isEmpty(compressedpath)
                                        && initCompressDir() && isValidFilePath(bean.getSrcUrl())) {
                                    compressImageThread(uuid, bean.getSrcUrl());
                                    bean.setStatus(TASK_STATUS_COMPRESSING);
                                } else {
                                    File file = new File(compressedpath);
                                    if (file != null && file.exists()) {
                                        CommomFileRequestCallBack callback = new CommomFileRequestCallBack(
                                                bean, syncHandler);
                                        upload(bean.getSrcUrl(), compressedpath,
                                                callback);
                                        bean.setStatus(TASK_STATUS_RUNNING);
                                    } else {
                                        // 先判断源文件是否存在，不存在，则不需要压缩
                                        if (initCompressDir() && isValidFilePath(bean.getSrcUrl())) {
                                            compressImageThread(uuid, bean.getSrcUrl());
                                            bean.setStatus(TASK_STATUS_COMPRESSING);
                                        } else {
                                            CommomFileRequestCallBack callback = new CommomFileRequestCallBack(
                                                    bean, syncHandler);
                                            upload(bean.getSrcUrl(), compressedpath,
                                                    callback);
                                            bean.setStatus(TASK_STATUS_RUNNING);
                                        }
                                    }
                                }

                            } else {
                                CommomFileRequestCallBack callback = new CommomFileRequestCallBack(
                                        bean, syncHandler);
                                upload(bean.getSrcUrl(), "", callback);
                                bean.setStatus(TASK_STATUS_RUNNING);
                            }
                        }
                    }
                    break;
                }
                return true;
            }
            return false;
        }

        /***
         * 从FileTaskBean list数据构建动态消息的body字段
         * @param tempList
         * @return
         */
        public String createBodyStringFromList(List<FileTaskBean> tempList) {
            if (tempList == null || tempList.size() == 0) {
                return "";
            }
            FileTaskBean bean = tempList.get(0);
            int type = bean.getType();
            switch (type) {
                case NOTICE_TYPE_FILE:
                {
                    JSONArray body = new JSONArray();
                    int length = tempList.size();
                    for (int i = 0; i < length; i++) {
                        bean = tempList.get(i);
                        JSONObject object = null;
                        try {
                            object = new JSONObject(bean.getRawBodyItemData());
                        } catch (JSONException e) {
                            CustomLog.e(TAG,"JSONException" + e.toString());
                        }

                        if(object!=null){
                            body.put(object);
                        }
                    }

                    if (body.length() > 0) {
                        return body.toString();
                    }
                }
                break;
                case NOTICE_TYPE_MEETING_INVITE:
                {
                    JSONArray body = new JSONArray();
                    int length = tempList.size();
                    for (int i = 0; i < length; i++) {
                        bean = tempList.get(i);
                        JSONObject object = null;
                        try {
                            object = new JSONObject(bean.getRawBodyItemData());
                            object.put("showMeeting", false);
                        } catch (JSONException e) {
                            CustomLog.e(TAG,"JSONException" + e.toString());
                        }

                        if(object!=null){
                            body.put(object);
                        }
                    }

                    if (body.length() > 0) {
                        return body.toString();
                    }
                }
                break;
                case NOTICE_TYPE_FRIEND_SEND:
                case NOTICE_TYPE_IPCALL_SEND:
                    break;
                case NOTICE_TYPE_VEDIO_SEND:
                case NOTICE_TYPE_VCARD_SEND:
                case NOTICE_TYPE_PHOTO_SEND:
                case NOTICE_TYPE_AUDIO_SEND: {
                    JSONArray body = new JSONArray();
                    int length = tempList.size();
                    for (int i = 0; i < length; i++) {
                        bean = tempList.get(i);
                        JSONObject object = null;
                        try {
                            object = new JSONObject(bean.getRawBodyItemData());
                            if(!bean.isFrom()){
                                //发送的消息 需要更新romteUrl、compressPath和thumbnail
                                object.put("remoteUrl", bean.getResultUrl());
                                object.put("compressPath", bean.getCompressedPath());
                                object.put("thumbnail", bean.getThumbnailUrl());
                            }else{
                                //接收的消息 仅需要更新 localUrl
                                String result = bean.getResultUrl();
                                if(bean.getStatus()==TASK_STATUS_SUCCESS){
                                    if(!TextUtils.isEmpty(result)&&result.endsWith(".temp")){
                                        result.replace(".temp", "");
                                    }
                                }
                                object.put("localUrl", result);
                                object.put("size", bean.getFilesize());
                            }
                        } catch (JSONException e) {
                            CustomLog.e(TAG,"JSONException" + e.toString());
                        }

                        if(object!=null){
                            body.put(object);
                        }
                    }

                    if (body.length() > 0) {
                        return body.toString();
                    }
                }
                break;
            }

            return "";
        }

        private String createBodyString(String uuid) {
            CustomLog.d(TAG,"createBodyString begin, uuid:" + uuid);
            // 从FileTaskBean list中找到同uuid的对象， 组成JSON ARRAY
            // 再tostring(),得到需要的对象
            List<FileTaskBean> tempList = findFileTasks(uuid);
            return createBodyStringFromList(tempList);
        }

        /***
         * 找到该记录中最近运行的FileTaskBean
         * @param uuid    消息UUID
         * @param srcUrl  FileTaskBean的源地址
         * @return
         */
        private FileTaskBean findRecentlyRunningFileTask(String uuid,String srcUrl) {
            CustomLog.d(TAG,"findRecentlyRunningFileTask begin ,uuid:" + uuid + "srcUrl:" + srcUrl);
            if (TextUtils.isEmpty(uuid)) {
                return null;
            }
            if (fileTaskMap == null || !fileTaskMap.containsKey(uuid)) {
                return null;
            }
            List<FileTaskBean> templist = fileTaskMap.get(uuid);
            if(templist==null||templist.size()==0){
                return null;
            }
            FileTaskBean bean = null;
            int length = templist.size();
            for (int i = 0; i < length; i++) {
                bean = templist.get(i);
                if (uuid.equals(bean.getUuid())) {
                    if (srcUrl.equals(bean.getSrcUrl())) {
                        break;
                    }
                }
            }

            return bean;
        }


        private FileTaskBean findFirstValidFileTask(String uuid) {
            CustomLog.d(TAG,"findFirstValidFileTask begin uuid:" + uuid);
            if (TextUtils.isEmpty(uuid)) {
                return null;
            }
            if (fileTaskMap == null || !fileTaskMap.containsKey(uuid)) {
                return null;
            }
            List<FileTaskBean> templist = fileTaskMap.get(uuid);
            if(templist==null||templist.size()==0){
                return null;
            }
            FileTaskBean bean = null;
            int length = templist.size();
            for (int i = 0; i < length; i++) {
                bean = templist.get(i);
                if (uuid.endsWith(bean.getUuid())) {
                    if (bean.getStatus() != TASK_STATUS_SUCCESS
                            && (TextUtils.isEmpty(bean.getResultUrl()) || bean
                            .getResultUrl().endsWith(".temp"))) {
                        break;
                    }
                }
            }

            return bean;
        }


        public List<FileTaskBean> findFileTasks(String uuid) {
            CustomLog.d("FileTaskManager","findFileTasks begin,uuid:" + uuid);
            if (TextUtils.isEmpty(uuid)) {
                return null;
            }
            if (fileTaskMap == null || !fileTaskMap.containsKey(uuid)) {
                return null;
            }
            List<FileTaskBean> templist = fileTaskMap.get(uuid);
            if(templist==null||templist.size()==0){
                return null;
            }else{
                return templist;
            }

        }

        /***
         * 根据UUID 更新该条记录的状态
         * @param uuid
         * @param status
         * @param change_body   是否更新body字段
         * @return
         */
        private boolean updateTaskStatus(String uuid, int status,
                                         boolean change_body) {
            CustomLog.d(TAG,"updateTaskStatus begin,uuid:" + uuid + "|status:" + status + "|change_body:" + change_body);
            // 根据uuid更改记录中body 和 status 字段
            if (TextUtils.isEmpty(uuid)) {
                return false;
            }
            if (change_body) {
                String body = createBodyString(uuid);
                CustomLog.d(TAG,"body:" + body);
                if(TextUtils.isEmpty(body)){
                    int count = noticedao.updateNotice(uuid, status);
                    if (count > 0) {
                        return true;
                    }
                }else{
                    int count = noticedao.updateNotice(uuid, body, status);
                    if (count > 0) {
                        return true;
                    }
                }
            } else {
                int count = noticedao.updateNotice(uuid, status);
                if (count > 0) {
                    return true;
                }
            }
            return false;
        }

    /**
     * 根据UUID 更新该条记录的状态的body字段，不修改状态
     * @param uuid
     * @return
     */
    public boolean updateBodybutTaskStatus(String uuid) {
        CustomLog.d(TAG,"updateBodybutTaskStatus begin,uuid:" + uuid);
        // 根据uuid更改记录中body 和 status 字段
        if (TextUtils.isEmpty(uuid)) {
            return false;
        }

        String body = createBodyString(uuid);
        CustomLog.d(TAG,"body:" + body);
        if (!TextUtils.isEmpty(body)) {
            int count = noticedao.updateNotice(uuid, body);
            if (count > 0) {
                return true;
            }
        }

        return false;
    }


    public boolean updateMeetingShowFlag(String uuid) {
            CustomLog.d(TAG,"updateMeetingShowFlag uuid:" + uuid);
            // 根据uuid更改记录中body 和 status 字段
            if (TextUtils.isEmpty(uuid)) {
                return false;
            }

            NoticesBean bean = noticedao.getNoticeById(uuid);
            if(bean!=null && bean.getType() == NOTICE_TYPE_MEETING_INVITE){
                String body = createBodyString(uuid);
                CustomLog.d(TAG,"body:" + body);
                if (!TextUtils.isEmpty(body)) {
                    int count = noticedao.updateNotice(uuid, body);
                    if (count > 0) {
                        return true;
                    }
                }
            }
            return false;
        }

        /***
         * 同步下载一个文件
         * @param urlPath
         * @param outputFile
         * @return
         */
        public static boolean syncDownloadFile(String urlPath,  File outputFile) {
            CustomLog.d(TAG,"syncDownloadFile begin,urlPath" + urlPath + " outputFile:" + outputFile);
            boolean result = false;
            try {
                URL url = new URL(urlPath);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setDoInput(true);
                conn.connect();
                if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    InputStream is = conn.getInputStream();
                    FileOutputStream fos = new FileOutputStream(outputFile);
                    byte[] bt = new byte[1024];
                    int i = 0;
                    while ((i = is.read(bt)) > 0) {
                        fos.write(bt, 0, i);
                    }
                    fos.flush();
                    fos.close();
                    is.close();
                    result = true;
                } else {
                    result = false;
                }
            } catch (FileNotFoundException e) {
                CustomLog.e(TAG,"FileNotFoundException" + e.toString());
                result = false;
            } catch (IOException e) {
                CustomLog.e(TAG,"IOException" + e.toString());
                result = false;
            }
            CustomLog.d(TAG,"syncDownloadFile end result" + result);
            return result;
        }

    //    private SyncResult createNetErrorResult(){
    //        LogUtil.begin("");
    //        SyncResult result = new SyncResult();
    //        result.setOK(false);
    //        result.setErrorCode(-200);
    //        return result;
    //    }
    //
    //    public String syncSendFile(String filepath,int msgtype){
    //        LogUtil.begin("filepath:" + filepath
    //                + "|msgtype:" + msgtype);
    //        if (TextUtils.isEmpty(filepath)) {
    //            return "";
    //        }
    //        File file = new File(filepath);
    //        if (!file.exists()) {
    //            return "";
    //        }
    //        // 1、上传文件
    //
    //        String object = postFile(filepath, UrlConstant.getCommUrl(PrefType.FILE_UPLOAD_SERVER_URL));
    //        LogUtil.d("postFile:" + object);
    //
    //        String resultUrl = "";
    //        if (object != null) {
    //            JSONObject resp;
    //            try {
    //                resp = new JSONObject(object.toString());
    //                if (!resp.isNull("ok")) {
    //                    if (!resp.getString("ok").equals("1")) {
    //                        return null;
    //                    }
    //                    if (msgtype == NOTICE_TYPE_PHOTO_SEND) {
    //                        //游 要求使用源图连接 20130917
    //                        if (!resp.isNull("originalImagePath")) {
    //                            resultUrl = resp.getString("originalImagePath");
    //                        }
    //                    } else  {
    //                        if (!resp.isNull("originalFilePath")) {
    //                            resultUrl = resp.getString("originalFilePath");
    //                        }
    //                    }
    //                } else {
    //                    return null;
    //                }
    //            } catch (JSONException e) {
    //                LogUtil.e("JSONException", e);
    //            }
    //        }
    //        return resultUrl;
    //    }
    //
    //    public SyncResult syncSendFileMessage(String filepath, String receiver,
    //                                          String title, int msgtype) {
    //        LogUtil.begin("filepath:" + filepath
    //                + "|receiver:" + receiver
    //                + "|title:" + title
    //                + "|msgtype:" + msgtype);
    //        if (TextUtils.isEmpty(filepath)) {
    //            return null;
    //        }
    //        File file = new File(filepath);
    //        if (!file.exists()) {
    //            return null;
    //        }
    //        // 1、上传文件
    //
    //        String object = postFile(filepath, UrlConstant.getCommUrl(PrefType.FILE_UPLOAD_SERVER_URL));
    //        LogUtil.d("postFile:" + object);
    //
    //        if (object != null) {
    //            String resultUrl = "";
    //
    //            JSONObject resp;
    //            try {
    //                resp = new JSONObject(object.toString());
    //                if (!resp.isNull("ok")) {
    //                    if (!resp.getString("ok").equals("1")) {
    //                        return null;
    //                    }
    //                    if (msgtype == NOTICE_TYPE_PHOTO_SEND) {
    //                        //游 要求使用源图连接 20130917
    //                        if (!resp.isNull("originalImagePath")) {
    //                            resultUrl = resp.getString("originalImagePath");
    //                        }
    //                    } else  {
    //                        if (!resp.isNull("originalFilePath")) {
    //                            resultUrl = resp.getString("originalFilePath");
    //                        }
    //                    }
    //                } else {
    //                    return null;
    //                }
    //            } catch (JSONException e) {
    //                LogUtil.e("JSONException", e);
    //            }
    //
    //            if (TextUtils.isEmpty(resultUrl)) {
    //                return null;
    //            }
    //
    //            // 2、发送消息
    //            List<String> urls = new ArrayList<String>();
    //            urls.add(resultUrl);
    //            RequestParams parms = createParameter(urls,receiver,title,msgtype,null,null,null);
    //            if(parms!=null){
    //                SyncResult result = new HttpUtils().sendSync(HttpRequest.HttpMethod.POST,
    //                        UrlConstant.getCommUrl(PrefType.KEY_MESSAGE_SHARE_URL), parms,
    //                        CommonConstant.MSG_ACCESSTOKEN_INVALID);
    //
    //                if(result!=null&&result.isOK()){
    //                    LogUtil.d("发送消息成功，result:" + result.getResult());
    //                    int status = 0;
    //                    try {
    //                        JSONObject obj = new JSONObject(result.getResult());
    //                        status = obj.getInt("status");
    //                    } catch (JSONException e) {
    //                        LogUtil.e("JSONException", e);
    //                    }
    //                    if (status == 0||status == -1) {
    //                        //3、发送SIP消息
    //                        sendSIPShortMSG(receiver,msgtype,"");
    //                    }
    //                }
    //                return result;
    //            }
    //        }else{
    //            return createNetErrorResult();
    //        }
    //        return null;
    //    }
    //
    //    public SyncResult syncSendMessage(String receiver,
    //                                      String title, int msgtype){
    //        LogUtil.begin("receiver:" + receiver + "|title:" + title + "|msgtype:" + msgtype);
    //        RequestParams parms = createParameter(null,receiver,title,msgtype,null,null,null);
    //        if(parms!=null){
    //            SyncResult resultObject = new HttpUtils().sendSync(HttpRequest.HttpMethod.POST,
    //                    UrlConstant.getCommUrl(PrefType.KEY_MESSAGE_SHARE_URL), parms,
    //                    CommonConstant.MSG_ACCESSTOKEN_INVALID);
    //            return resultObject;
    //        }
    //        return null;
    //    }
    //
    //    public String postFile(String pathToOurFile, String urlServer) {
    //        LogUtil.begin("pathToOurFile:" + pathToOurFile + "|urlServer:" + urlServer);
    //
    //        String object = null;
    //        HttpClient httpclient = new DefaultHttpClient();
    //        // 设置通信协议版本
    //        httpclient.getParams().setParameter(
    //                CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
    //        // File path= Environment.getExternalStorageDirectory(); //取得SD卡的路径
    //        // String pathToOurFile = path.getPath()+File.separator+"ak.txt";
    //        // //uploadfile
    //        HttpPost httppost = new HttpPost(urlServer);
    //        File file = new File(pathToOurFile);
    //        MultipartEntity mpEntity = new MultipartEntity(); // 文件传输
    //        ContentBody cbFile = new FileBody(file);
    //        mpEntity.addPart("userfile", cbFile); // <input type="file"
    //        // name="userfile" /> 对应的
    //        httppost.setEntity(mpEntity);
    //        try {
    //            HttpResponse response = httpclient.execute(httppost);
    //            if (HttpStatus.SC_OK == response.getStatusLine().getStatusCode()){
    //                HttpEntity resEntity = response.getEntity();
    //                if (resEntity != null) {
    //                    object = EntityUtils.toString(resEntity, "utf-8");
    //                    resEntity.consumeContent();
    //                }
    //            }
    //        }catch (ClientProtocolException e) {
    //            LogUtil.e("ClientProtocolException", e);
    //        } catch (IOException e) {
    //            LogUtil.e("IOException", e);
    //        } finally {
    //            httpclient.getConnectionManager().shutdown();
    //        }
    //
    //        LogUtil.end("object:" + object);
    //        return object;
    //    }
    //
    //    private JSONObject getSendExtInfo(int msgtype,String body,String extInfo, JSONArray thumbnails){
    //        LogUtil.begin("msgtype:"+msgtype
    //                + "|body:" + body
    //                + "|extInfo:" + extInfo
    //                + "|thumbnails:" + thumbnails);
    //
    //        JSONObject obj = new JSONObject();
    //        if(NOTICE_TYPE_PHOTO_SEND==msgtype){
    //
    //            if(!TextUtils.isEmpty(body)){
    //                try {
    //                    JSONArray array = new JSONArray(body);
    //                    if(array!=null&&array.length()>0){
    //                        JSONObject item = array.getJSONObject(0);
    //                        obj.put("width", item.optString("width"));
    //                        obj.put("height", item.optString("height"));
    //                    }
    //                    array = null;
    //                } catch (JSONException e) {
    //                    e.printStackTrace();
    //                }
    //            }
    //
    //            if(!TextUtils.isEmpty(extInfo)){
    //                try {
    //                    JSONObject extObject = new JSONObject(extInfo);
    //                    obj.put("id", extObject.optString("id"));
    //                    obj.put("text", extObject.optString("text"));
    //                    obj.put("ver", extObject.optString("ver"));
    //                } catch (JSONException e) {
    //                    e.printStackTrace();
    //                    try {
    //                        obj.put("id", "");
    //                        obj.put("text", "");
    //                        obj.put("ver", BizConstant.MSG_VERSION);
    //                    } catch (JSONException e1) {
    //                        e.printStackTrace();
    //                    }
    //                }
    //            }else{
    //                try {
    //                    obj.put("id", "");
    //                    obj.put("text", "");
    //                    obj.put("ver", BizConstant.MSG_VERSION);
    //                } catch (JSONException e) {
    //                    e.printStackTrace();
    //                }
    //            }
    //
    //            if(thumbnails!=null){
    //                try {
    //                    obj.put("thumbUrls", thumbnails);
    //                } catch (JSONException e) {
    //                    e.printStackTrace();
    //                }
    //            }
    //
    //        } else if(NOTICE_TYPE_VEDIO_SEND==msgtype){
    //
    //            if(!TextUtils.isEmpty(body)){
    //                try {
    //                    JSONArray array = new JSONArray(body);
    //                    if(array!=null&&array.length()>0){
    //                        JSONObject item = array.getJSONObject(0);
    //                        obj.put("vediolen", item.optString("duration"));
    //                    }
    //                    array = null;
    //                } catch (JSONException e) {
    //                    e.printStackTrace();
    //                }
    //            }
    //
    //            if(!TextUtils.isEmpty(extInfo)){
    //                try {
    //                    JSONObject extObject = new JSONObject(extInfo);
    //                    obj.put("id", extObject.optString("id"));
    //                    obj.put("text", extObject.optString("text"));
    //                    obj.put("ver", extObject.optString("ver"));
    //                } catch (JSONException e) {
    //                    e.printStackTrace();
    //                    try {
    //                        obj.put("id", "");
    //                        obj.put("text", "");
    //                        obj.put("ver", BizConstant.MSG_VERSION);
    //                    } catch (JSONException e1) {
    //                        e.printStackTrace();
    //                    }
    //                }
    //            }else{
    //                try {
    //                    obj.put("id", "");
    //                    obj.put("text", "");
    //                    obj.put("ver", BizConstant.MSG_VERSION);
    //                } catch (JSONException e) {
    //                    e.printStackTrace();
    //                }
    //            }
    //
    //            if(thumbnails!=null){
    //                try {
    //                    obj.put("thumbUrls", thumbnails);
    //                } catch (JSONException e) {
    //                    e.printStackTrace();
    //                }
    //            }
    //
    //        } else if(NOTICE_TYPE_AUDIO_SEND==msgtype){
    //
    //            if(!TextUtils.isEmpty(body)){
    //                try {
    //                    JSONArray array = new JSONArray(body);
    //                    if(array!=null&&array.length()>0){
    //                        JSONObject item = array.getJSONObject(0);
    //                        obj.put("audiolen", item.opt("duration"));
    //                    }
    //                    array = null;
    //                } catch (JSONException e) {
    //                    e.printStackTrace();
    //                }
    //            }
    //
    //            if(!TextUtils.isEmpty(extInfo)){
    //                try {
    //                    JSONObject extObject = new JSONObject(extInfo);
    //                    obj.put("id", extObject.optString("id"));
    //                    obj.put("text", extObject.optString("text"));
    //                    obj.put("ver", extObject.optString("ver"));
    //                } catch (JSONException e) {
    //                    e.printStackTrace();
    //                    try {
    //                        obj.put("id", "");
    //                        obj.put("text", "");
    //                        obj.put("ver", BizConstant.MSG_VERSION);
    //                    } catch (JSONException e1) {
    //                        e.printStackTrace();
    //                    }
    //                }
    //            }else{
    //                try {
    //                    obj.put("id", "");
    //                    obj.put("text", "");
    //                    obj.put("ver", BizConstant.MSG_VERSION);
    //                } catch (JSONException e) {
    //                    e.printStackTrace();
    //                }
    //            }
    //
    //        } else if(NOTICE_TYPE_VCARD_SEND==msgtype){
    //
    //            if(!TextUtils.isEmpty(body)){
    //                try {
    //                    JSONArray array = new JSONArray(body);
    //                    if(array!=null&&array.length()>0){
    //                        JSONObject item = array.getJSONObject(0);
    //
    //                        JSONArray cardArray = new JSONArray();
    //                        JSONObject cardInfo = new JSONObject();
    //                        cardInfo.put("code", item.optString("code"));
    //                        cardInfo.put("name", item.optString("name"));
    //                        cardInfo.put("phone", item.optString("phone"));
    //                        cardInfo.put("url", item.optString("url"));
    //                        cardInfo.put("userid", item.optString("userid"));
    //                        cardInfo.put("sex", item.optString("sex"));
    //                        cardArray.put(cardInfo);
    //
    //                        obj.put("card", cardArray);
    //                    }
    //                    array = null;
    //                } catch (JSONException e) {
    //                    e.printStackTrace();
    //                }
    //            }
    //            try {
    //                obj.put("id", "");
    //                obj.put("text", "");
    //                obj.put("ver", BizConstant.MSG_VERSION);
    //            } catch (JSONException e) {
    //                e.printStackTrace();
    //            }
    //
    //        } else if(NOTICE_TYPE_TXT_SEND==msgtype){
    //
    //            if(!TextUtils.isEmpty(body)){
    //                try {
    //                    JSONArray array = new JSONArray(body);
    //                    if(array!=null&&array.length()>0){
    //                        JSONObject item = array.getJSONObject(0);
    //                        obj.put("text", item.optString("txt"));
    //                    }
    //                    array = null;
    //                } catch (JSONException e) {
    //                    e.printStackTrace();
    //                }
    //            }
    //
    //            if(!TextUtils.isEmpty(extInfo)){
    //                try {
    //                    JSONObject extObject = new JSONObject(extInfo);
    //                    obj.put("id", extObject.optString("id"));
    //                    obj.put("ver", extObject.optString("ver"));
    //                } catch (JSONException e) {
    //                    e.printStackTrace();
    //                    try {
    //                        obj.put("id", "");
    //                        obj.put("ver", BizConstant.MSG_VERSION);
    //                    } catch (JSONException e1) {
    //                        e.printStackTrace();
    //                    }
    //                }
    //            }else{
    //                try {
    //                    obj.put("id", "");
    //                    obj.put("ver", BizConstant.MSG_VERSION);
    //                } catch (JSONException e) {
    //                    e.printStackTrace();
    //                }
    //            }
    //
    //        } else if(NOTICE_TYPE_FILE==msgtype){
    //
    //            if(!TextUtils.isEmpty(body)){
    //                try {
    //                    JSONArray array = new JSONArray(body);
    //                    if(array!=null&&array.length()>0){
    //                        JSONObject item = array.getJSONObject(0);
    //
    //                        JSONObject cardInfo = new JSONObject();
    //                        cardInfo.put("size", item.optLong("size"));
    //                        cardInfo.put("fileName", item.optString("fileName"));
    //                        cardInfo.put("fileType", item.optString("fileType"));
    //                        cardInfo.put("localUrl", item.optString("localUrl"));
    //                        cardInfo.put("remoteUrl", item.optString("remoteUrl"));
    //                        obj.put("fileInfo", cardInfo);
    //                    }
    //                    array = null;
    //                } catch (JSONException e) {
    //                    e.printStackTrace();
    //                }
    //            }
    //            try {
    //                obj.put("id", "");
    //                obj.put("text", "");
    //                obj.put("ver", BizConstant.MSG_VERSION);
    //            } catch (JSONException e) {
    //                e.printStackTrace();
    //            }
    //        }else if(NOTICE_TYPE_MEETING_INVITE==msgtype){
    //            if(!TextUtils.isEmpty(body)){
    //                try {
    //                    JSONArray array = new JSONArray(body);
    //                    if(array!=null&&array.length()>0){
    //                        JSONObject item = array.getJSONObject(0);
    //
    //                        JSONObject cardInfo = new JSONObject();
    //                        cardInfo.put("inviterId", item.optString("inviterId"));
    //                        cardInfo.put("inviterName", item.optString("inviterName"));
    //                        cardInfo.put("inviterHeadUrl", item.optString("inviterHeadUrl"));
    //                        cardInfo.put("meetingRoom", item.optString("meetingRoom"));
    //                        cardInfo.put("meetingUrl", item.optString("meetingUrl"));
    //                        cardInfo.put("showMeeting", item.optBoolean("showMeeting"));
    //
    //                        obj.put("meetingInfo", cardInfo);
    //                    }
    //                    array = null;
    //                } catch (JSONException e) {
    //                    e.printStackTrace();
    //                }
    //            }
    //            try {
    //                obj.put("id", "");
    //                obj.put("text", "");
    //                obj.put("ver", BizConstant.MSG_VERSION);
    //            } catch (JSONException e) {
    //                e.printStackTrace();
    //            }
    //        }else {
    //
    //            try {
    //                obj.put("id", "");
    //                obj.put("text", "");
    //                obj.put("ver", BizConstant.MSG_VERSION);
    //            } catch (JSONException e) {
    //                e.printStackTrace();
    //            }
    //        }
    //        return obj;
    //    }
    //
    //    /***
    //     *
    //     * @param urls       文件的绝对地址列表
    //     * @param receiver   接收对象的视频号串 ,以分号分割（A;B;C;D）
    //     * @param titlecontext  标题字段
    //     * @param type       消息类型
    //     * @param thumb      缩略图连接列表（图片发送/转发场景）
    //     * @param body       notices表中body字段的信息;(可以为空串)
    //     * @param extinfo    notices表中extinfo字段的信息;(可以为空串)
    //     * @return
    //     */
    //    private RequestParams createParameter(List<String> urls, String receiver,
    //                                          String titlecontext, int type, List<String> thumb,
    //                                          String body,String extinfo) {
    //
    //        LogUtil.begin("receiver:" + receiver
    //                + "|titlecontext:"+ titlecontext
    //                + "|type:" + type
    //                + "|ext:" + body);
    //
    //        String own =  NetPhoneApplication.getPreference().getKeyValue(PrefType.LOGIN_NUBENUMBER, "");
    //        JSONObject request = new JSONObject();
    //        JSONObject parameter = new JSONObject();
    //        try {
    //            request.put("sender", own);
    //
    //            JSONArray j_recievers = new JSONArray();
    //            if(!TextUtils.isEmpty(receiver)){
    //                String[] reciever_list = receiver.split(";");
    //                int count = reciever_list.length;
    //                for(int i = 0;i<count;i++){
    //                    j_recievers.put(reciever_list[i]);
    //                }
    //            }else{
    //                LogUtil.d("发送对象为空");
    //                return null;
    //            }
    //            request.put("receivers", j_recievers);
    //
    //            JSONArray j_pictures = new JSONArray();
    //            if(urls==null||urls.size()==0){
    //                j_pictures.put("");
    //            }else{
    //                for(String item:urls){
    //                    j_pictures.put(item);
    //                }
    //            }
    //
    //            request.put("body", j_pictures);
    //
    //            JSONArray j_thumbs = new JSONArray();
    //            if(thumb==null||thumb.size()==0){
    //                j_thumbs.put("");
    //            }else{
    //                for(String item:thumb){
    //                    j_thumbs.put(item);
    //                }
    //            }
    //            //根据消息类型从body字段中找出部分扩展的信息+消息的版本号
    //            //放置到消息的extendedInfo中
    //            request.put("extendedInfo", getSendExtInfo(type,body,extinfo,j_thumbs));
    //
    //            // 兼容平板侧的JSON串定义
    //            if(type==NOTICE_TYPE_FRIEND_SEND||type==NOTICE_TYPE_FEEEDBACK_SEND){
    //                request.put("title", titlecontext);
    //            } else{
    //                JSONObject title = new JSONObject();
    //                title.put("sender", own);
    //                if(TextUtils.isEmpty(titlecontext)){
    //                    title.put("msgInfo", "来自Butel Android客户端");
    //                }else{
    //                    title.put("msgInfo", titlecontext);
    //                }
    //                request.put("title", title);
    //            }
    //
    //            parameter.put("msg", request);
    //
    //            LogUtil.d("parameter tostring="+ parameter.toString());
    //
    //            RequestParams params = new RequestParams();
    //            params.addBodyParameter("service", "sendMessage");
    //            params.addBodyParameter("params", parameter.toString());
    //            params.addBodyParameter("accessToken",
    //                    NetPhoneApplication.getPreference()
    //                            .getKeyValue(PrefType.LOGIN_ACCESSTOKENID,""));
    //            switch(type){
    //                case NOTICE_TYPE_PHOTO_SEND:
    //                    params.addBodyParameter("type", BizConstant.MSG_BODY_TYPE_PIC_2);
    //                    params.addBodyParameter("app", BizConstant.MSG_APP_N8_PHOTO);
    //                    break;
    //                case NOTICE_TYPE_FRIEND_SEND:
    //                    params.addBodyParameter("type", BizConstant.MSG_BODY_TYPE_VCARD);
    //                    params.addBodyParameter("app", BizConstant.MSG_APP_N8_CONTACT);
    //                    break;
    //                case NOTICE_TYPE_FEEEDBACK_SEND:
    //                    params.addBodyParameter("type", BizConstant.MSG_BODY_TYPE_MULTITRUST);
    //                    params.addBodyParameter("app", BizConstant.MSG_APP_N8_CONTACT);
    //                    break;
    //                case NOTICE_TYPE_VEDIO_SEND:
    //                    params.addBodyParameter("type", BizConstant.MSG_BODY_TYPE_VIDEO_2);
    //                    params.addBodyParameter("app", BizConstant.MSG_APP_N8_SPHONE);
    //                    break;
    //                case NOTICE_TYPE_VCARD_SEND:
    //                    params.addBodyParameter("type", BizConstant.MSG_BODY_TYPE_POSTCARD);
    //                    params.addBodyParameter("app", BizConstant.MSG_APP_NAME);
    //                    break;
    //                case NOTICE_TYPE_AUDIO_SEND:
    //                    params.addBodyParameter("type", BizConstant.MSG_BODY_TYPE_AUDIO);
    //                    params.addBodyParameter("app", BizConstant.MSG_APP_NAME);
    //                    break;
    //                case NOTICE_TYPE_IPCALL_SEND:
    //                    params.addBodyParameter("type", BizConstant.MSG_BODY_TYPE_IPCALL);
    //                    params.addBodyParameter("app", BizConstant.MSG_APP_NAME);
    //                    break;
    //                case NOTICE_TYPE_TXT_SEND:
    //                    params.addBodyParameter("type", BizConstant.MSG_BODY_TYPE_TXT);
    //                    params.addBodyParameter("app", BizConstant.MSG_APP_NAME);
    //                    break;
    //                default:
    ////					params.addQueryStringParameter("type", "");
    ////					params.addQueryStringParameter("app", BizConstant.MSG_APP_NAME);
    //                    LogUtil.d("无法识别的消息类型");
    //                    params = null;
    //                    return null;
    //            }
    //            return params;
    //        } catch (JSONException e) {
    //            LogUtil.e("JSONException", e);
    //        }
    //        return null;
    //    }
    //
        public void upload(String srcPath, String compressedpath,
                           CommomFileRequestCallBack callback) {
            CustomLog.d(TAG,"upload begin,srcPaht:" + srcPath + "|compressedpath:" + compressedpath);
            File file = null;
            if(TextUtils.isEmpty(compressedpath)){
                if (TextUtils.isEmpty(srcPath)) {
                    CustomLog.d(TAG,"download srcPath is empty");
                    //TODO:?
                    return;
                }

                file = new File(srcPath);
                if (!file.exists()) {
                    CustomLog.d(TAG,"upload srcPath is not exists:" + srcPath);
                    // TODO:在文件不存在时，上传逻辑在本处终止，还是由http发现抛fail信息终止？
                    // 暂由http处理 miaolk marked
                    // return;
                }
            }else{
                file = new File(compressedpath);
                if (file!=null&&!file.exists()) {
                    CustomLog.d(TAG,"compressedpath is not exists:" + compressedpath);
                    // TODO:在文件不存在时，上传逻辑在本处终止，还是由http发现抛fail信息终止？
                    // 暂由http处理 miaolk marked
                    // return;
                    file = new File(srcPath); // 尝试传原图至服务器
                }
            }
            RequestParams params = new RequestParams();
            params.addBodyParameter("image", file);
            HttpUtils http = new HttpUtils();
            http.configTimeout(120*1000);
//            HttpHandler handler = http.send(HttpRequest.HttpMethod.POST,
//                    UrlConstant.getCommUrl(PrefType.FILE_UPLOAD_SERVER_URL), params, callback);

            HttpHandler handler = null;

            String key = getKeyString(srcPath);
            runTaskQueue.put(key, handler);
        }
    //
    //    private String getMIMEType(int type){
    //        String content_type = "";
    //        switch (type) {
    //            case NOTICE_TYPE_PHOTO_SEND:
    //                content_type ="image";
    //                break;
    //            case NOTICE_TYPE_VEDIO_SEND:
    //                content_type ="video";
    //                break;
    //            case NOTICE_TYPE_AUDIO_SEND:
    //                content_type ="audio";
    //                break;
    //            case NOTICE_TYPE_VCARD_SEND:
    //            case NOTICE_TYPE_FRIEND_SEND:
    //                content_type ="text/x-vard";
    //                break;
    //            case NOTICE_TYPE_TXT_SEND:
    //                content_type ="text/plain";
    //                break;
    //
    //            default:
    //                break;
    //        }
    //        return content_type;
    //    }
    //
        public void download(String srcUrl, String uuid, DownFileRequestCallBack callback) {
            CustomLog.d(TAG,"download begin,srcUrl:" + srcUrl);
            if (!initStorageDir()) {
                if (syncHandler != null) {
                    syncHandler.sendEmptyMessage(NO_SDCARD);
                }
                return;
            }

            // 由下载URL 得到本地存储的路径
            String destFileName = getLocalPathFromURL(srcUrl, uuid, true);
            if (TextUtils.isEmpty(destFileName)) {
                CustomLog.d(TAG,"download create temp destFileName is empty");
                return;
            }
            // 判断文件本地文件是否存在，并返回已有文件大小
            boolean isresume = false;
            //TODO:20141125 不在支持断点续传
            File file = new File(destFileName);
            if (file!=null&&file.exists()) {
                file.delete();
                //isresume = true;
                file = null;
            }

            // 参数
            RequestParams params = null;
            HttpUtils http = new HttpUtils();
            http.configTimeout(120*1000);
            HttpHandler handler = http.download(srcUrl, params, destFileName,
                    isresume, callback);
            String key = getKeyString(srcUrl);
            runTaskQueue.put(key, handler);
        }

    public static boolean initRecordDir() {
        if (!Environment.MEDIA_MOUNTED.equals(Environment
            .getExternalStorageState())) {
            return false;
        }
        String dirpath = Environment.getExternalStorageDirectory()
            + File.separator + APP_ROOT_FOLDER
            + File.separator + FILE_RECORD_DIR;
        File file = new File(dirpath);
        if (file.exists()) {
            return true;
        } else {
            return file.mkdirs();
        }
    }
    //


    /**
     * 获得录制文件保存的路径；
     * 当没有SD卡或SDK卡没有初始化好、以及创建文件夹失败的情况下，返回空
     */
    public static String getRecordDir() {
        if (!initRecordDir()) {
            return "";
        }

        String dirpath = Environment.getExternalStorageDirectory()
            + File.separator + APP_ROOT_FOLDER
            + File.separator + FILE_RECORD_DIR;
        return dirpath;
    }


    //
    //    public static boolean initTakePhotoDir() {
    //        if (!Environment.MEDIA_MOUNTED.equals(Environment
    //                .getExternalStorageState())) {
    //            return false;
    //        }
    //        String dirpath = Environment.getExternalStorageDirectory()
    //                + File.separator + CommonConstant.APP_ROOT_FOLDER
    //                + File.separator + FILE_TAKE_PHOTO_DIR;
    //        File file = new File(dirpath);
    //        if (file.exists()) {
    //            return true;
    //        } else {
    //            return file.mkdirs();
    //        }
    //    }
    //
    //
    //    public static String  getTakePhotoDir() {
    //        if (!initTakePhotoDir()) {
    //            return "";
    //        }
    //
    //        String dirpath = Environment.getExternalStorageDirectory()
    //                + File.separator + CommonConstant.APP_ROOT_FOLDER
    //                + File.separator + FILE_TAKE_PHOTO_DIR;
    //        return dirpath;
    //    }
    //
        private boolean initCompressDir() {
            if (!Environment.MEDIA_MOUNTED.equals(Environment
                    .getExternalStorageState())) {
                return false;
            }
            String dirpath = Environment.getExternalStorageDirectory()
                    + File.separator + IMConstant.APP_ROOT_FOLDER
                    + File.separator + FILE_COMPRESS_DIR;
            File file = new File(dirpath);
            if (file.exists()) {
                return true;
            } else {
                return file.mkdirs();
            }
        }

        public static String getVCFDir(){
            if (!initVCFDir()) {
                return "";
            }

            String dirpath = Environment.getExternalStorageDirectory()
                    + File.separator + IMConstant.APP_ROOT_FOLDER
                    + File.separator + FILE_VCARD_DIR;
            return dirpath;
        }

        private static boolean initVCFDir() {
            if (!Environment.MEDIA_MOUNTED.equals(Environment
                    .getExternalStorageState())) {
                return false;
            }
            String dirpath = Environment.getExternalStorageDirectory()
                    + File.separator + IMConstant.APP_ROOT_FOLDER
                    + File.separator + FILE_VCARD_DIR;
            File file = new File(dirpath);
            if (file.exists()) {
                return true;
            } else {
                return file.mkdirs();
            }
        }

        private boolean initStorageDir() {
            if (!Environment.MEDIA_MOUNTED.equals(Environment
                    .getExternalStorageState())) {
                return false;
            }
            String dirpath = Environment.getExternalStorageDirectory()
                    + File.separator + IMConstant.APP_ROOT_FOLDER
                    + File.separator + FILE_DOWNLOAD_DIR;
            File file = new File(dirpath);
            if (file.exists()) {
                return true;
            } else {
                return file.mkdirs();
            }
        }
    //
    //    public void downAudioFileThread(final String fileUrl,final String uuid){
    //        new Thread(new Runnable() {
    //
    //            @Override
    //            public void run() {
    //                if(TextUtils.isEmpty(fileUrl)){
    //                    return;
    //                }
    //                if(!initStorageDir()){
    //                    return;
    //                }
    //                String localpath = getLocalPathFromURL(fileUrl,uuid,true);
    //                if(!TextUtils.isEmpty(localpath)){
    //                    File file = new File(localpath);
    //                    if (syncDownloadFile(fileUrl, file)) {
    //                        // 下载成功，rename
    //                        if (localpath.endsWith(".temp")) {
    //                            localpath.replace(".temp", "");
    //                            if (file.exists()) {
    //                                boolean re = file.renameTo(new File(localpath));
    //                                LogUtil.d("downAudioFileThread下载成功，rename:"
    //                                        + re + " | " + localpath);
    //                            }
    //                        }
    //                    } else {
    //                        // 下载失败，删除temp文件
    //                        LogUtil.d("downAudioFileThread下载失败，删除temp文件:" + localpath);
    //                        if (file.exists()) {
    //                            file.delete();
    //                        }
    //                    }
    //                }
    //            }
    //        }).start();
    //    }
    //
        public  String getLocalPathFromURL(String url, String uuid, boolean tempfile) {
            CustomLog.d(TAG,"getLocalPathFromURL begin,url:"+ url + "|tempfile:" + tempfile);
            if (TextUtils.isEmpty(url)) {
                return "";
            }
            int index = url.lastIndexOf("/");
            if(index==-1){
                return "";
            }
            String lastpart = url.substring(index);

            String path = Environment.getExternalStorageDirectory()
                    + File.separator + IMConstant.APP_ROOT_FOLDER
                    + File.separator + FILE_DOWNLOAD_DIR;
            String threadId = noticedao.getThreadIdById(uuid);
            if(!TextUtils.isEmpty(threadId)){
                String tmppath = path + File.separator + threadId;
                File file = new File(tmppath);
                boolean succ = false;
                if (!file.exists()) {
                    succ = file.mkdirs()||file.isDirectory();
                } else{
                    succ = true;
                }
                if(succ){
                    path = tmppath;
                }
            }
            path = path + lastpart;
            if (tempfile) {
                path = path + ".temp";
            }
            CustomLog.d(TAG,"path:" + path);

            return path;
        }

        private String getKeyString(String pathOrUrl) {
            CustomLog.d(TAG,"getKeyString begin,pathOrUrl:" + pathOrUrl);
            String key = "";
            String lastpart = "";
            if (!TextUtils.isEmpty(pathOrUrl)) {
                // 找到连接或路径的最后一段
                if (pathOrUrl.startsWith("http:")) {
                    int index = pathOrUrl.lastIndexOf("/");
                    lastpart = pathOrUrl.substring(index+1);
                } else {
                    int index = pathOrUrl.lastIndexOf(File.separator);
                    lastpart = pathOrUrl.substring(index+1);
                }
                // 去除文件名中的点号
                key = lastpart.replace(".", "");
            }
            CustomLog.d(TAG,"getKeyString end,key:" + key);
            return key;
        }

        /**
         * 转发消息：根据uuid查出要转发的本地的消息
         * 以此为基础构建出一条新的记录，保存到数据库中；
         * 再发送邮件消息，成功后，在SIP短消息通知对方立即接收
         * @param receiver 接收对象的视频号（多个视频号用分号分割）
         * @param uuid     要转发的消息的uuid
         * @return 数据库中新记录的uuid; 返回空串，则表明参数为空 或 插入数据记录失败
         */
        public String forwardMessage(String receiver, String uuid) {

            LogUtil.begin("receiver:" + receiver
                    + "|uuid:" + uuid );

            if(TextUtils.isEmpty(receiver)){
                LogUtil.d("receiver 为空");
                return "";
            }

            if(TextUtils.isEmpty(uuid)){
                LogUtil.d("uuid 为空");
                return "";
            }

            String newItemuuid = buildForwardRecord(receiver,uuid);

            if(TextUtils.isEmpty(newItemuuid)){
                LogUtil.d("newItemuuid 为空,构建记录失败");
                return "";
            }
            //加入到发送队列中
            addTask(newItemuuid, null);

            return newItemuuid;
        }


        /**
         * 构建转发消息记录，返回新记录的ID
         * @param receiver  接收对象的视频号（多个视频号用分号分割）
         * @param uuid      将要转发的消息uuid
         * @return 返回新记录的id
         */
        private String buildForwardRecord(String receiver, String uuid) {

            LogUtil.begin("receiver:" + receiver + "|uuid:" + uuid );

            NoticesBean oldbean = noticedao.getNoticeById(uuid);
            GroupDao groupDao = new GroupDao(mcontext);
            if(oldbean==null){
                LogUtil.d("buildForwardRecord getNoticeById ==null");
                return "";
            }
            // sort 接收对象的视频号
            String recipentIds = StringUtil.sortRecipentIds(receiver, ';');
            String createBody = "";
            LogUtil.d("after sorted recipentIds:"+recipentIds);

            //TODO:重新构建body字段
            JSONArray bodyArray;
            String text = "";
            try {
                bodyArray = new JSONArray(oldbean.getBody());
                if (bodyArray != null && bodyArray.length() > 0) {
                    JSONObject bodyObj = bodyArray.optJSONObject(0);
                    text = bodyObj.optString("txt");
                }
            } catch (JSONException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
            if (text.contains(IMConstant.SPECIAL_CHAR+"")) {
                ArrayList<String> result = new ArrayList<String>();
                result = IMCommonUtil.getDispList(text);
                for (int i = 0; i < result.size(); i++) {
                    GroupMemberBean gbean = groupDao.queryGroupMember(
                            oldbean.getThreadsId(), result.get(i));
                    if (gbean != null) {
                        ShowNameUtil.NameElement element = ShowNameUtil.getNameElement(
                                gbean.getName(), gbean.getNickName(),
                                gbean.getPhoneNum(), gbean.getNubeNum());
                        String MName = ShowNameUtil.getShowName(element);
                        text = text.replace("@" + result.get(i)
                                + IMConstant.SPECIAL_CHAR, "@" + MName
                                + IMConstant.SPECIAL_CHAR);
                    }
                }
                JSONArray array = new JSONArray();
                JSONObject object = null;
                object = new JSONObject();
                try {
                    object.put("txt", text);
                    array.put(object);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                createBody = array.toString();
            } else {
                createBody = oldbean.getBody();
            }
            String sender = MedicalApplication.getPreference().getKeyValue(PrefType.LOGIN_NUBENUMBER, "");
            NoticesBean bean = new NoticesBean();
            bean.setSender(sender);
            bean.setReciever(recipentIds);
            bean.setBody(createBody);
            bean.setStatus(TASK_STATUS_READY);
            bean.setType(oldbean.getType());

            bean.setIsNew(0);
            long curtime = System.currentTimeMillis();
            bean.setSendTime(curtime);
            bean.setReceivedTime(curtime);
            bean.setTitle(oldbean.getTitle());

            String newItemuuid = StringUtil.getUUID();
            bean.setId(newItemuuid);

            bean.setMsgId(newItemuuid);
            bean.setFailReplyId("");
            // 修改下extInfo中id的值
            String oldExt = oldbean.getExtInfo();
            JSONObject extObj = null;
            if (!TextUtils.isEmpty(oldExt)) {
                try {
                    extObj = new JSONObject(oldExt);
                    extObj.put("id", bean.getId());
                    extObj.put("ver", BizConstant.MSG_VERSION);
                } catch (JSONException e) {
                    LogUtil.e("JSONException", e);
                    e.printStackTrace();
                }
            }else{
                try {
                    extObj = new JSONObject();
                    extObj.put("id", bean.getId());
                    extObj.put("text", "");
                    extObj.put("ver", BizConstant.MSG_VERSION);
                } catch (JSONException e) {
                    LogUtil.e("JSONException", e);
                    e.printStackTrace();
                }
            }
            if(extObj!=null){
                bean.setExtInfo(extObj.toString());
            }else{
                bean.setExtInfo("");
            }
            //TODO 添加转发群组
            // 关联会话记录
            ThreadsDao threadsDao = new ThreadsDao(mcontext);
            if (recipentIds.length() < 12) {
                String covstid = threadsDao
                        .createThread(recipentIds, curtime, true);
                if (TextUtils.isEmpty(covstid)) {
                    LogUtil.d("createSendFileNotice createThread id==null");
                    return "";
                }
                bean.setThreadsId(covstid);
            } else {
                if (!threadsDao.isExistThread(recipentIds)) {
                    threadsDao.createThreadFromGroup(recipentIds);
                }else{
                    threadsDao.updateLastTime(recipentIds, curtime);
                }
                bean.setThreadsId(recipentIds);
            }

            newItemuuid = noticedao.insertNotice(bean);
            LogUtil.end("newItemuuid:" + newItemuuid);
            return newItemuuid;
        }

        private void sendMessageBody(int type, String uuid, String srcUrl) {
            CustomLog.d(TAG,"sendMessageBody begin type:" + type + "|uuid:" + uuid + "|srcUrl:" + srcUrl);
            switch (type) {
                case NOTICE_TYPE_PHOTO_SEND:
                case NOTICE_TYPE_VCARD_SEND:
                case NOTICE_TYPE_VEDIO_SEND:
                case NOTICE_TYPE_AUDIO_SEND:
                case NOTICE_TYPE_TXT_SEND:
                case NOTICE_TYPE_MEETING_INVITE:
                case NOTICE_TYPE_FILE:
                    // sendMessage(type,uuid,srcUrl);
                    // TODO:20150211 mlk 集成IM Connect
                    // 因跨进程的原因，本处使用广播至CallmanagerService中发送
                    // sendMessageByIMConnect(type,uuid);
    //				updateTaskStatus(uuid,TASK_STATUS_READY,true);
    //				Intent intent = new Intent();
    //				intent.setAction(CallManageConstant.IM_SEND);
    //				intent.putExtra(CallManageConstant.IM_SEND_MSGID, uuid);
    //				mcontext.sendBroadcast(intent);
                    // TODO: 20151201 集成P2P connect 删除IM Client代码
                    break;
                default:
            }
        }


    //
    //    //=======START==========IM Connect集成适配===================
    ////	public ImMessage createImMessageBean(String body, String title, String extInfo,
    ////			String[] recievers, String type, String app,String id) {
    ////		String own =  NetPhoneApplication.getPreference().getKeyValue(PrefType.LOGIN_NUBENUMBER, "");
    ////		ImMessage message = new ImMessage();
    ////		message.setBody(body);
    ////		message.setTitle(title);
    ////		message.setExtendedInfo(extInfo);
    ////		message.setReceivers(recievers);
    ////		message.setSender(own);
    ////		message.setMsgId(TextUtils.isEmpty(id)?StringUtil.getUUID():id);
    ////		if(type.equals(BizConstant.MSG_BODY_TYPE_VCARD)){
    ////			//好友认证
    ////			message.setType(type);
    ////			message.setApp(app);
    ////			message.setMsgHead(BizConstant.MSG_TYPE_VCARD_SEND_SM3);
    ////			message.setTag(ImMessage.IMMSG_TAG_ADDFRI);
    ////		} else if(type.equals(BizConstant.MSG_BODY_TYPE_MULTITRUST)){
    ////			//‘添加好友’反馈
    ////			message.setType(type);
    ////			message.setApp(app);
    ////			message.setMsgHead(BizConstant.MSG_TYPE_VCARD_RECEIVE_SM4);
    ////			message.setTag(ImMessage.IMMSG_TAG_ADDFRI_FB);
    ////		} else if(type.equals(BizConstant.MSG_BODY_TYPE_ONEKEYVISIT)){
    ////			//‘一键回家’扫一扫
    ////			message.setType(type);
    ////			message.setApp(app);
    ////			message.setMsgHead(BizConstant.MSG_TYPE_ONEKEYVISIT_SEND_SM3OK);
    ////			message.setTag(ImMessage.IMMSG_TAG_GOHOME);
    ////		} else if(type.equals(BizConstant.MSG_BODY_TYPE_PIC_2)){
    ////			//图片
    ////			message.setType(type);
    ////			message.setApp(app);
    ////			message.setMsgHead(BizConstant.MSG_TYPE_PHOTO_SM1_2);
    ////			message.setTag(ImMessage.IMMSG_TAG_NORMAL);
    ////		} else if(type.equals(BizConstant.MSG_BODY_TYPE_VIDEO_2)){
    ////			//视频
    ////			message.setType(type);
    ////			message.setApp(app);
    ////			message.setMsgHead(BizConstant.MSG_TYPE_VIDEO_SM2_2);
    ////			message.setTag(ImMessage.IMMSG_TAG_NORMAL);
    ////		} else if(type.equals(BizConstant.MSG_BODY_TYPE_AUDIO)){
    ////			//语音
    ////			message.setType(type);
    ////			message.setApp(app);
    ////			message.setMsgHead(BizConstant.MSG_TYPE_AUDIO_SEND_SM3AU);
    ////			message.setTag(ImMessage.IMMSG_TAG_NORMAL);
    ////		} else if(type.equals(BizConstant.MSG_BODY_TYPE_POSTCARD)){
    ////			//名片分享
    ////			message.setType(type);
    ////			message.setApp(app);
    ////			message.setMsgHead(BizConstant.MSG_TYPE_POSTCARD_SEND_SM3S);
    ////			message.setTag(ImMessage.IMMSG_TAG_NORMAL);
    ////		} else if(type.equals(BizConstant.MSG_BODY_TYPE_TXT)){
    ////			//文字
    ////			message.setType(type);
    ////			message.setApp(app);
    ////			message.setMsgHead(BizConstant.MSG_TYPE_TXT_SM3TXT);
    ////			message.setTag(ImMessage.IMMSG_TAG_NORMAL);
    ////		} else if(type.equals(BizConstant.MSG_BODY_TYPE_COMMON)){
    ////			//会议邀请
    ////			message.setType(type);
    ////			message.setApp(app);
    ////			message.setMsgHead(BizConstant.MSG_TYPE_TXT_SM3TXT);
    ////			message.setTag(ImMessage.IMMSG_TAG_NORMAL);
    ////		} else {
    ////			//未知
    ////			message.setType(type);
    ////			message.setApp(app);
    ////			message.setMsgHead(BizConstant.MSG_TYPE_SM0);
    ////			message.setTag(ImMessage.IMMSG_TAG_NORMAL);
    ////		}
    ////		return message;
    ////	}
    //
    ////	public ImMessage getImMessageBean(String uuid){
    ////
    ////		ImMessage message = null;
    ////		NoticesBean bean = noticedao.getNoticeById(uuid);
    ////		String tag = ImMessage.IMMSG_TAG_NORMAL;
    ////		if (bean != null) {
    ////			if(bean.getReciever().equals(bean.getThreadsId())){
    ////				tag = ImMessage.IMMSG_TAG_GROUP;
    ////			}
    ////			message = new ImMessage();
    ////			message.setTag(tag);
    ////			int type = bean.getType();
    ////			List<String> resultUrlList = new ArrayList<String>();
    ////			List<String> thumbs = new ArrayList<String>();
    ////			List<FileTaskBean> urls = createFileTaskList(uuid,false);
    ////			for (FileTaskBean url : urls) {
    ////				resultUrlList.add(url.getResultUrl());
    ////				thumbs.add(url.getThumbnailUrl());
    ////			}
    ////			String receiver = bean.getReciever();
    ////			String titlecontext = bean.getTitle();
    ////
    ////			String own =  NetPhoneApplication.getPreference().getKeyValue(PrefType.LOGIN_NUBENUMBER, "");
    ////			try {
    ////				message.setMsgId(uuid);
    ////				message.setSender(own);
    ////
    ////				if(!TextUtils.isEmpty(receiver)){
    ////					String[] reciever_list = receiver.split(";");
    ////					if(reciever_list!=null&&reciever_list.length>0){
    ////						message.setReceivers(reciever_list);
    ////					}else{
    ////						LogUtil.d("发送对象为空");
    ////						return null;
    ////					}
    ////				}else{
    ////					LogUtil.d("发送对象为空");
    ////					return null;
    ////				}
    ////
    ////				JSONArray j_pictures = new JSONArray();
    ////				if(resultUrlList==null||resultUrlList.size()==0){
    ////					j_pictures.put("");
    ////				}else{
    ////					for(String item:resultUrlList){
    ////						j_pictures.put(item);
    ////					}
    ////				}
    ////				message.setBody(j_pictures.toString());
    ////
    ////				JSONArray j_thumbs = new JSONArray();
    ////				if(thumbs==null||thumbs.size()==0){
    ////					j_thumbs.put("");
    ////				}else{
    ////					for(String item:thumbs){
    ////						j_thumbs.put(item);
    ////					}
    ////				}
    ////				//根据消息类型从body字段中找出部分扩展的信息+消息的版本号
    ////				//放置到消息的extendedInfo中
    ////				JSONObject extObj = getSendExtInfo(type,bean.getBody(),bean.getExtInfo(),j_thumbs);
    ////				message.setExtendedInfo(extObj.toString());
    ////
    ////				// 兼容平板侧的JSON串定义
    ////				if(type==NOTICE_TYPE_FRIEND_SEND||type==NOTICE_TYPE_FEEEDBACK_SEND){
    ////					message.setTitle(titlecontext);
    ////				} else{
    ////					JSONObject title = new JSONObject();
    ////					title.put("sender", own);
    ////					if(TextUtils.isEmpty(titlecontext)){
    ////						title.put("msgInfo", "来自Butel Android客户端");
    ////					}else{
    ////						title.put("msgInfo", titlecontext);
    ////					}
    ////					message.setTitle(title.toString());
    ////				}
    ////
    ////				switch(type){
    ////					case NOTICE_TYPE_PHOTO_SEND:
    ////						message.setType(BizConstant.MSG_BODY_TYPE_PIC_2);
    ////						message.setApp(BizConstant.MSG_APP_N8_PHOTO);
    ////						message.setMsgHead(BizConstant.MSG_TYPE_PHOTO_SM1_2);
    ////						break;
    ////					case NOTICE_TYPE_FRIEND_SEND:
    ////						message.setType(BizConstant.MSG_BODY_TYPE_VCARD);
    ////						message.setApp(BizConstant.MSG_APP_N8_CONTACT);
    ////						message.setMsgHead(BizConstant.MSG_TYPE_VCARD_SEND_SM3);
    ////						break;
    ////					case NOTICE_TYPE_FEEEDBACK_SEND:
    ////						message.setType(BizConstant.MSG_BODY_TYPE_MULTITRUST);
    ////						message.setApp(BizConstant.MSG_APP_N8_CONTACT);
    ////						message.setMsgHead(BizConstant.MSG_TYPE_VCARD_RECEIVE_SM4);
    ////						break;
    ////					case NOTICE_TYPE_VEDIO_SEND:
    ////						message.setType(BizConstant.MSG_BODY_TYPE_VIDEO_2);
    ////						message.setApp(BizConstant.MSG_APP_N8_SPHONE);
    ////						message.setMsgHead(BizConstant.MSG_TYPE_VIDEO_SM2_2);
    ////						break;
    ////					case NOTICE_TYPE_VCARD_SEND:
    ////						message.setType(BizConstant.MSG_BODY_TYPE_POSTCARD);
    ////						message.setApp(BizConstant.MSG_APP_NAME);
    ////						message.setMsgHead(BizConstant.MSG_TYPE_POSTCARD_SEND_SM3S);
    ////						break;
    ////					case NOTICE_TYPE_AUDIO_SEND:
    ////						message.setType(BizConstant.MSG_BODY_TYPE_AUDIO);
    ////						message.setApp(BizConstant.MSG_APP_NAME);
    ////						message.setMsgHead(BizConstant.MSG_TYPE_AUDIO_SEND_SM3AU);
    ////						break;
    ////					case NOTICE_TYPE_IPCALL_SEND:
    ////						message.setType(BizConstant.MSG_BODY_TYPE_IPCALL);
    ////						message.setApp(BizConstant.MSG_APP_NAME);
    ////						message.setMsgHead(BizConstant.MSG_TYPE_IPCALL_SEND_SM3P);
    ////						break;
    ////					case NOTICE_TYPE_TXT_SEND:
    ////						message.setType(BizConstant.MSG_BODY_TYPE_TXT);
    ////						message.setApp(BizConstant.MSG_APP_NAME);
    ////						message.setMsgHead(BizConstant.MSG_TYPE_TXT_SM3TXT);
    ////						break;
    ////					case NOTICE_TYPE_MEETING_INVITE:
    ////						message.setType(BizConstant.MSG_BODY_TYPE_COMMON);
    ////						message.setApp(BizConstant.MSG_APP_NAME);
    ////						message.setMsgHead(BizConstant.MSG_TYPE_TXT_SM3TXT);
    ////						break;
    ////					default:
    ////						LogUtil.d("无法识别的消息类型");
    ////				}
    ////			} catch (JSONException e) {
    ////			    LogUtil.e("JSONException", e);
    ////			    message = null;
    ////			}
    ////		}
    ////		return message;
    ////	}
    //
        public void updateStatusAfterIM(String uuid, boolean succ) {
            CustomLog.d(TAG,"updateStatusAfterIM uuid:" + uuid +" succ:"+succ);
            if (succ) {
                updateTaskStatus(uuid, TASK_STATUS_SUCCESS, true);
            } else {
                updateTaskStatus(uuid, TASK_STATUS_FAIL, true);
            }
        }

        // 删除消息map任务里面对应数值
        public void removeMap(String uuid) {
            if (TextUtils.isEmpty(uuid) || fileTaskMap == null
                    || fileTaskMap.size() == 0) {
                CustomLog.d(TAG,"updateStatusAfterIM uuid:" + uuid + " fileTaskMap:"
                        + fileTaskMap);
                return;
            }
            CustomLog.d(TAG,"fileTaskMap.remove:" + uuid);
            fileTaskMap.remove(uuid);
        }

        // 更新消息发送时间为服务器返回的发送时间
        public void updateTime(String uuid , long time){
            CustomLog.d(TAG,"updateStatusAfterIM uuid:" + uuid +" 发送消息成功服务器返回时间:"+time);
            if (TextUtils.isEmpty(uuid)) {
                return;
            }
            if(time != 0){
                noticedao.updateNotice(uuid, time);
            }
        }

        //=======END==========IM Connect集成适配===================


        //==================== START 集成SDK Connect=============================

        public static class SCIMBean{
            public String msgType = "";
            public String title = "";
            public String[] recvs = null;
            public int recvsLen = 0;
            public String text = "";
            // 当thumUrl不为空时，filePath可以忽略
            public String filePath = "";
            public String thumUrl = "";
            public int upLoadFilTimeOutSec = AppP2PAgentManager.UPLOADFILE_TIMEOUT;
            public int durationSec = 0;
            // 当isGroupMsg为true时，groupId有意义
            public String groupId ="";
            public boolean isGroupMsg = false;
            // 消息存储在本地表中的uuid
            public String uuid = "";
            public String extJson = "";
        }

        public boolean sendSCIMMsg(String uuid){
            SCIMBean bean = convert2SCIMBean(uuid);
            if(bean!=null){
                //TODO:
                AppP2PAgentManager.getInstance().sendIMMessage(bean);
                //更新会议弹屏状态
                updateMeetingShowFlag(bean.uuid);
                return true;
            }else{
                CustomLog.d(TAG,"convert2SCIMBean bean对象为空");
            }
            return false;
        }


        public SCIMBean convert2SCIMBean(String uuid){

            SCIMBean message = null;
            NoticesBean bean = noticedao.getNoticeById(uuid);
            boolean isGroupMsg = false;
            if (bean != null) {
                message = new SCIMBean();
                message.uuid = uuid;
                if(bean.getReciever().equals(bean.getThreadsId())){
                    isGroupMsg = true;
                    message.groupId = bean.getReciever();
                    message.recvsLen = 1;
                }
                message.isGroupMsg = isGroupMsg;
                int type = bean.getType();
                List<String> filePathList = new ArrayList<String>();
                List<String> resultUrlList = new ArrayList<String>();
                List<String> thumbs = new ArrayList<String>();
                List<FileTaskBean> urls = createFileTaskList(uuid,false);
                String temp="";
                for (FileTaskBean url : urls) {
                    temp=url.getSrcUrl();
                    if(!TextUtils.isEmpty(temp)){
                        filePathList.add(temp);
                    }
                    temp=url.getResultUrl();
                    if(!TextUtils.isEmpty(temp)){
                        resultUrlList.add(temp);
                    }
                    temp=url.getThumbnailUrl();
                    if(!TextUtils.isEmpty(temp)){
                        thumbs.add(temp);
                    }
                }
                String receiver = bean.getReciever();
                String titlecontext = bean.getTitle();

                try {
                    if(!TextUtils.isEmpty(receiver)){
                        String[] reciever_list = receiver.split(";");
                        if(reciever_list!=null&&reciever_list.length>0){
                            message.recvs = reciever_list;
                            message.recvsLen = reciever_list.length;
                        }else{
                            CustomLog.d(TAG,"发送对象为空");
                            return null;
                        }
                    }else{
                        CustomLog.d(TAG,"发送对象为空");
                        return null;
                    }


                    if(resultUrlList==null||resultUrlList.size()==0){
                        //文件上没有上传成功
                        if(filePathList.size()>0){
                            //TODO:目前SDK connect的发送消息接口，一次只能上传一个文件
                            message.filePath = filePathList.get(0);
                            message.upLoadFilTimeOutSec = AppP2PAgentManager.UPLOADFILE_TIMEOUT;
                        }else{
                            if(type == NOTICE_TYPE_PHOTO_SEND
                                    ||type == NOTICE_TYPE_VEDIO_SEND
                                    ||type == NOTICE_TYPE_AUDIO_SEND){
                                CustomLog.d(TAG,"文件上没有上传成功,且文件路径为空");
                                return null;
                            }
                        }
                    }else{
                        List<String> url = new ArrayList<String>();
                        String orgUrl = "";
                        String thumbUrl = "";
                        if(filePathList.size()>0){
                            message.filePath = filePathList.get(0);
                        }else{
                            message.filePath = "";
                        }
                        for(int i=0; i<resultUrlList.size(); i++){
                            orgUrl =  resultUrlList.get(i);
                            if(type == NOTICE_TYPE_PHOTO_SEND){
                                // bigUrl占位
                                if(!TextUtils.isEmpty(orgUrl)){
                                    url.add(orgUrl);
                                }
                            }
                            if(!TextUtils.isEmpty(orgUrl)){
                                url.add(orgUrl);
                            }
                            if(thumbs!=null&&thumbs.size()>i){
                                thumbUrl = thumbs.get(i);
                                if(!TextUtils.isEmpty(thumbUrl)){
                                    url.add(thumbUrl);
                                }
                            }
                        }
                        message.thumUrl = StringUtil.list2String(url, ',');
                    }

                    String snipText = "";
                    if(type==NOTICE_TYPE_MEETING_INVITE){
                        snipText = "邀请你加入会议";
                    }else if (type==NOTICE_TYPE_MEETING_BOOK){
                        snipText = "预约你参加会议";
                    }else if(type==NOTICE_TYPE_TXT_SEND){
                        snipText =getSnipTxt(bean);
                    }
                    //根据消息类型从body字段中找出部分扩展的信息+消息的版本号
                    //放置到SC消息的text字段
                    String text = createCSIMTxt(type, bean.getBody());
                    message.text= text;

                    message.extJson = createCSIMAppExtInfo(type, snipText,
                            bean.getBody(), bean.getExtInfo(), "");

                    // 兼容平板侧的JSON串定义
                    if(type==NOTICE_TYPE_FRIEND_SEND||type==NOTICE_TYPE_FEEEDBACK_SEND){
                        message.title=titlecontext;
                    } else{
                        JSONObject title = new JSONObject();
                        String own = AccountManager.getInstance(mcontext).getAccountInfo().nube;
                        title.put("sender", own);
                        if(TextUtils.isEmpty(titlecontext)){
                            title.put("msgInfo", "来自Butel Android客户端");
                        }else{
                            title.put("msgInfo", titlecontext);
                        }
                        message.title=title.toString();
                    }

                    switch(type){
                        case NOTICE_TYPE_PHOTO_SEND:
                            message.msgType= BizConstant.MSG_BODY_TYPE_PIC_2;
                            break;
                        case NOTICE_TYPE_FRIEND_SEND:
                            message.msgType = BizConstant.MSG_BODY_TYPE_VCARD;
                            break;
                        case NOTICE_TYPE_FEEEDBACK_SEND:
                            message.msgType=BizConstant.MSG_BODY_TYPE_MULTITRUST;
                            break;
                        case NOTICE_TYPE_VEDIO_SEND:
                            message.msgType = BizConstant.MSG_BODY_TYPE_VIDEO_2;
                            message.durationSec = getDuration(bean.getBody());
                            break;
                        case NOTICE_TYPE_VCARD_SEND:
                            message.msgType=BizConstant.MSG_BODY_TYPE_POSTCARD;
                            break;
                        case NOTICE_TYPE_AUDIO_SEND:
                            message.msgType = BizConstant.MSG_BODY_TYPE_AUDIO;
                            message.durationSec = getDuration(bean.getBody());
                            break;
                        case NOTICE_TYPE_IPCALL_SEND:
                            message.msgType= BizConstant.MSG_BODY_TYPE_IPCALL;
                            break;
                        case NOTICE_TYPE_TXT_SEND:
                            message.msgType= BizConstant.MSG_BODY_TYPE_TXT;
                            break;
                        case NOTICE_TYPE_FILE:
                        case NOTICE_TYPE_MEETING_INVITE:
                        case NOTICE_TYPE_MEETING_BOOK:
                            message.msgType=BizConstant.MSG_BODY_TYPE_COMMON;
                            break;
                        default:
                            CustomLog.d(TAG,"无法识别的消息类型");
                    }
                } catch (JSONException e) {
                    CustomLog.e(TAG,"JSONException" + e.toString());
                    message = null;
                }
            }

            return message;
        }

    //
    //    public SCIMBean createSCIMBean(String filepath, String title, String text,
    //                                   String extInfo, String[] recievers, String type,
    //                                   String id) {
    //        String own = NetPhoneApplication.getPreference().getKeyValue(
    //                PrefType.LOGIN_NUBENUMBER, "");
    //        SCIMBean message = new SCIMBean();
    //        message.filePath = filepath;
    //        message.title= title;
    //        message.text = text;
    //        message.extJson = createCSIMAppExtInfo(getlocalType(type), "", "", extInfo, type);
    //        message.durationSec =0;
    //        message.groupId = "";
    //        message.isGroupMsg = false;
    //        message.recvs = recievers;
    //        message.recvsLen = recievers.length;
    //        message.thumUrl = "";
    //        message.uuid = id;
    //        message.msgType = type;
    //
    //        return message;
    //    }
    //
    //
        private int getDuration(String body){

            int duration = 0;
            if(!TextUtils.isEmpty(body)){
                try {
                    JSONArray array = new JSONArray(body);
                    if(array!=null&&array.length()>0){
                        JSONObject item = array.getJSONObject(0);
                        duration = item.optInt("duration");
                    }
                    array = null;
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            return duration;
        }
    //
    //    private String getFilename(String path){
    //
    //        if(!TextUtils.isEmpty(path)){
    //            File file = new File(path);
    //            if(file!=null&&file.exists()){
    //                return file.getName();
    //            }
    //        }
    //        return "";
    //    }
    //
    //    private long getFilesize(String path){
    //
    //        if(!TextUtils.isEmpty(path)){
    //            File file = new File(path);
    //            if(file!=null&&file.exists()){
    //                return file.length();
    //            }
    //        }
    //        return 0;
    //    }
    //
    //
        private String createCSIMTxt(int msgtype,String body){
            CustomLog.d(TAG,"createCSIMTxt beginmsgtype:"+msgtype
                    + "|body:" + body);

            String text="";
            if(NOTICE_TYPE_PHOTO_SEND==msgtype){
                text = "[照片]";
            } else if(NOTICE_TYPE_VEDIO_SEND==msgtype){
                text = "[视频]";
            } else if(NOTICE_TYPE_AUDIO_SEND==msgtype){
                text = "[语音]";
            } else if(NOTICE_TYPE_VCARD_SEND==msgtype){
                text = "[名片]";
            } else if(NOTICE_TYPE_TXT_SEND==msgtype){

                if(!TextUtils.isEmpty(body)){
                    try {
                        JSONArray array = new JSONArray(body);
                        if(array!=null&&array.length()>0){
                            JSONObject item = array.getJSONObject(0);
                            text = item.optString("txt");
                        }
                        array = null;
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

            } else if(NOTICE_TYPE_MEETING_INVITE==msgtype){
                JSONObject obj = new JSONObject();
                if(!TextUtils.isEmpty(body)){
                    try {
                        JSONArray array = new JSONArray(body);
                        if(array!=null&&array.length()>0){
                            JSONObject item = array.getJSONObject(0);

                            JSONObject cardInfo = new JSONObject();
                            cardInfo.put("inviterId", item.optString("inviterId"));
                            cardInfo.put("inviterName", item.optString("inviterName"));
                            cardInfo.put("inviterHeadUrl", item.optString("inviterHeadUrl"));
                            cardInfo.put("meetingRoom", item.optString("meetingRoom"));
                            cardInfo.put("meetingUrl", item.optString("meetingUrl"));
                            cardInfo.put("showMeeting", item.optBoolean("showMeeting"));

                            obj.put("meetingInfo", cardInfo);
                        }
                        array = null;
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                try {
                    obj.put("subtype", BizConstant.MSG_SUB_TYPE_MEETING);
                    obj.put("text", "会议邀请");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                text = obj.toString();
            } else if (NOTICE_TYPE_MEETING_BOOK==msgtype){
                JSONObject obj = new JSONObject();
                if(!TextUtils.isEmpty(body)){
                    try {
                        JSONArray array = new JSONArray(body);
                        if(array!=null&&array.length()>0){
                            JSONObject item = array.getJSONObject(0);
                            JSONObject cardInfo = new JSONObject();
                            cardInfo.put(BookMeetingExInfo.BOOK_NUBE, item.optString(BookMeetingExInfo.BOOK_NUBE));
                            cardInfo.put(BookMeetingExInfo.BOOK_NAME, item.optString(BookMeetingExInfo.BOOK_NAME));
                            cardInfo.put(BookMeetingExInfo.MEETING_ROOM, item.optString(BookMeetingExInfo.MEETING_ROOM));
                            cardInfo.put(BookMeetingExInfo.MEETING_THEME, item.optString(BookMeetingExInfo.MEETING_THEME));
                            cardInfo.put(BookMeetingExInfo.MEETING_TIME, item.optLong(BookMeetingExInfo.MEETING_TIME));
                            cardInfo.put(BookMeetingExInfo.MEETING_URL, item.optString(BookMeetingExInfo.MEETING_URL));
                            obj.put(BookMeetingExInfo.BOOK_MEETING_INFO, cardInfo);
                        }
                        array = null;
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                try {
                    obj.put("subtype", BizConstant.MSG_SUB_TYPE_MEETING_BOOK);
                    obj.put("text", "会议预约");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                text = obj.toString();
            }else if(NOTICE_TYPE_FILE==msgtype){
                JSONObject obj = new JSONObject();
                if(!TextUtils.isEmpty(body)){
                    try {
                        JSONArray array = new JSONArray(body);
                        if(array!=null&&array.length()>0){
                            JSONObject item = array.getJSONObject(0);

                            JSONObject cardInfo = new JSONObject();
                            cardInfo.put("size", item.optLong("size"));
                            cardInfo.put("fileName", item.optString("fileName"));
                            cardInfo.put("fileType", item.optString("fileType"));
                            cardInfo.put("localUrl", item.optString("localUrl"));
                            cardInfo.put("remoteUrl", item.optString("remoteUrl"));
                            obj.put("fileInfo", cardInfo);
                        }
                        array = null;
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                try {
                    obj.put("subtype", BizConstant.MSG_SUB_TYPE_FILE);
                    obj.put("ver", BizConstant.MSG_VERSION);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                text = obj.toString();
            }

            return text;
        }


        private String createCSIMAppExtInfo(int msgtype,String snipTxt,String body,String extInfo, String bodytype){
            CustomLog.d(TAG,"createCSIMAppExtInfo begin msgtype:"+ msgtype
                    + "|snipTxt:"+ snipTxt
                    + "|body:" + body
                    + "|extInfo:" + extInfo
                    + "|bodytype:" + bodytype);;

            String msghead = getMsgHead(msgtype, snipTxt);
            if(TextUtils.isEmpty(msghead)){
                msghead = getMsgHeadByStrType(bodytype, snipTxt);
            }

            JSONObject obj = new JSONObject();
            if(NOTICE_TYPE_PHOTO_SEND==msgtype){

                if(!TextUtils.isEmpty(body)){
                    try {
                        JSONArray array = new JSONArray(body);
                        if(array!=null&&array.length()>0){
                            JSONObject item = array.getJSONObject(0);
                            obj.put("width", item.optString("width"));
                            obj.put("height", item.optString("height"));
                        }
                        array = null;
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                if(!TextUtils.isEmpty(extInfo)){
                    try {
                        JSONObject extObject = new JSONObject(extInfo);
                        obj.put("fileName", extObject.optString("fileName"));
                        obj.put("fileSize", extObject.optString("fileSize"));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

            } else if(NOTICE_TYPE_VEDIO_SEND==msgtype){

                if(!TextUtils.isEmpty(extInfo)){
                    try {
                        JSONObject extObject = new JSONObject(extInfo);
                        obj.put("fileName", extObject.optString("fileName"));
                        obj.put("fileSize", extObject.optString("fileSize"));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

            } else if(NOTICE_TYPE_AUDIO_SEND==msgtype){

                if(!TextUtils.isEmpty(extInfo)){
                    try {
                        JSONObject extObject = new JSONObject(extInfo);
                        obj.put("fileName", extObject.optString("fileName"));
                        obj.put("fileSize", extObject.optString("fileSize"));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

            }  else if(NOTICE_TYPE_VCARD_SEND==msgtype){

                if(!TextUtils.isEmpty(body)){
                    try {
                        JSONArray array = new JSONArray(body);
                        if(array!=null&&array.length()>0){
                            JSONObject item = array.getJSONObject(0);

                            JSONArray cardArray = new JSONArray();
                            JSONObject cardInfo = new JSONObject();
                            cardInfo.put("code", item.optString("code"));
                            cardInfo.put("name", item.optString("name"));
                            cardInfo.put("phone", item.optString("phone"));
                            cardInfo.put("url", item.optString("url"));
                            cardInfo.put("userid", item.optString("userid"));
                            cardInfo.put("sex", item.optString("sex"));
                            cardArray.put(cardInfo);

                            obj.put("card", cardArray.toString());
                        }
                        array = null;
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                if(!TextUtils.isEmpty(extInfo)){
                    try {
                        JSONObject extObject = new JSONObject(extInfo);
                        obj.put("fileName", extObject.optString("fileName"));
                        obj.put("fileSize", extObject.optString("fileSize"));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                try {
                    String mobile = MedicalApplication.getPreference().getKeyValue(
                            PrefType.LOGIN_MOBILE, "");
                    obj.put("caller_mobile_num", mobile);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            } else if(NOTICE_TYPE_TXT_SEND==msgtype){
                // do nothing
            } else if(NOTICE_TYPE_MEETING_INVITE==msgtype){
                // do nothing
            } else if(NOTICE_TYPE_FILE==msgtype){
                // do nothing
            }else{
                // do nothing
            }

            try {
                obj.put("ver", BizConstant.MSG_VERSION);
                obj.put("msgHead", msghead);
            } catch (JSONException e) {
                e.printStackTrace();
            }


            return obj.toString();
        }

    //    private String createSCIMExtInfo(int type, String snipTxt){
    //
    //        JSONObject obj = null;
    //        String msghead = getMsgHead(type, snipTxt);
    //        try {
    //            obj = new JSONObject();
    //            obj.put("ver", BizConstant.MSG_VERSION);
    //            obj.put("msgHead", msghead);
    //            return  obj.toString();
    //        } catch (JSONException e) {
    //            e.printStackTrace();
    //            return "";
    //        }
    //    }
    //
    //
    //
    //
    //
    //    //===================== END 集成SDK Connect============================
    //
    //
    //
    //
    //
    //
    //
    //
    //
    //
    //    @SuppressWarnings("unused")
    //    private boolean sendMessage(final int type, final String uuid,
    //                                final String srcUrl) {
    //
    //        LogUtil.begin("type:" + type
    //                + "|uuid:" + uuid
    //                + "|srcUrl:" + srcUrl);
    //
    //        boolean isSentOk = false;
    //        final NoticesBean bean = noticedao.getNoticeById(uuid);
    //        if (bean != null) {
    //            List<String> resultUrlList = new ArrayList<String>();
    //            List<String> thumbs = new ArrayList<String>();
    //            List<FileTaskBean> urls = findFileTasks(uuid);
    //            for (FileTaskBean url : urls) {
    //                resultUrlList.add(url.getResultUrl());
    //                thumbs.add(url.getThumbnailUrl());
    //            }
    //            String receiver = bean.getReciever();
    //            String title = bean.getTitle();
    //            RequestParams params = createParameter(resultUrlList, receiver,
    //                    title, type,thumbs,bean.getBody(),bean.getExtInfo());
    //            if (params != null) {
    //                HttpHandler handler = new HttpUtils()
    //                        .send(HttpRequest.HttpMethod.POST,
    //                                UrlConstant.getCommUrl(PrefType.KEY_MESSAGE_SHARE_URL),
    //                                params, new RequestCallBack<String>() {
    //
    //                                    @Override
    //                                    public void onStart() {
    //                                        super.onStart();
    //                                    }
    //
    //                                    @Override
    //                                    public void onLoading(long total,
    //                                                          long current) {
    //                                        super.onLoading(total, current);
    //                                    }
    //
    //                                    @Override
    //                                    public void onFailure(Throwable error,
    //                                                          String msg) {
    //                                        super.onFailure(error, msg);
    //                                        LogUtil.d("sendMessage onFailure:"
    //                                                + msg);
    //                                        updateTaskStatus(uuid,
    //                                                TASK_STATUS_FAIL, true);
    //                                        LogUtil.d("fileTaskMap.remove:" + uuid);
    //                                        fileTaskMap.remove(uuid);
    //                                        runTaskQueue
    //                                                .remove(getKeyString(srcUrl));
    //                                    }
    //
    //                                    @Override
    //                                    public void onSuccess(String result) {
    //                                        super.onSuccess(result);
    //                                        LogUtil.d("sendMessage onSuccess:"
    //                                                + result);
    //                                        int status = 0;
    //                                        try {
    //                                            JSONObject object = new JSONObject(
    //                                                    result);
    //                                            status = object.getInt("status");
    //                                        } catch (JSONException e) {
    //                                            LogUtil.e("JSONException", e);
    //                                        }
    //                                        if (status == 0||status == -1) {
    //                                            updateTaskStatus(uuid,
    //                                                    TASK_STATUS_SUCCESS, true);
    //                                            LogUtil.d("fileTaskMap.remove:" + uuid);
    //                                            fileTaskMap.remove(uuid);
    //                                            runTaskQueue
    //                                                    .remove(getKeyString(srcUrl));
    //
    //                                            String snipTxt = "";
    //                                            if(NOTICE_TYPE_TXT_SEND==type){
    //                                                snipTxt = getSnipTxt(bean);
    //                                            }
    //
    //                                            sendSIPShortMSG(bean.getReciever(),
    //                                                    type,snipTxt);
    //                                        } else {
    //                                            updateTaskStatus(uuid,
    //                                                    TASK_STATUS_FAIL, true);
    //                                            LogUtil.d("fileTaskMap.remove:" + uuid);
    //                                            fileTaskMap.remove(uuid);
    //                                            runTaskQueue
    //                                                    .remove(getKeyString(srcUrl));
    //                                        }
    //                                    }
    //                                },CommonConstant.MSG_ACCESSTOKEN_INVALID);
    //
    //            } else {
    //                updateTaskStatus(uuid, TASK_STATUS_FAIL, true);
    //                LogUtil.d("fileTaskMap.remove:" + uuid);
    //                fileTaskMap.remove(uuid);
    //                runTaskQueue.remove(getKeyString(srcUrl));
    //                LogUtil.d("sendMessage failure");
    //            }
    //
    //        }
    //
    //        LogUtil.end("isSentOk:" + isSentOk);
    //        return isSentOk;
    //    }
    //
    //    private void sendSIPShortMSG(String numbers, int type, String snipTxt){
    //        LogUtil.begin("numbers:" + numbers + "|type:" + type +" |snipTxt:"+snipTxt);
    //        if(TextUtils.isEmpty(numbers)){
    //            return;
    //        }
    //        String typecontext = getMsgHead(type,snipTxt);
    //        //在CallManagerServier中SIP短消息发送中已经按分号（;）循环发送了，故此处不需做处理
    //        if(!TextUtils.isEmpty(typecontext)){
    //            Intent intent = new Intent(CallManageConstant.SN_ACTION);
    //            intent.putExtra(BizConstant.SIP_KEY_NUMBERS, numbers);
    //            intent.putExtra(BizConstant.SIP_KEY_CONTENT, typecontext);
    //            mcontext.sendBroadcast(intent);
    //        }
    //    }
    //
    //    private int getlocalType(String msgtype){
    //        int localtype = -1;
    //        if(BizConstant.MSG_BODY_TYPE_VIDEO_2.equalsIgnoreCase(msgtype)){
    //            localtype = NOTICE_TYPE_VEDIO_SEND;
    //        }else if(BizConstant.MSG_BODY_TYPE_VIDEO.equalsIgnoreCase(msgtype)){
    //            localtype = NOTICE_TYPE_VEDIO_SEND;
    //        }else if(BizConstant.MSG_BODY_TYPE_PIC_2.equalsIgnoreCase(msgtype)){
    //            localtype = NOTICE_TYPE_PHOTO_SEND;
    //        }else if(BizConstant.MSG_BODY_TYPE_PIC.equalsIgnoreCase(msgtype)){
    //            localtype = NOTICE_TYPE_PHOTO_SEND;
    //        }else if(BizConstant.MSG_BODY_TYPE_AUDIO.equalsIgnoreCase(msgtype)){
    //            localtype = NOTICE_TYPE_AUDIO_SEND;
    //        }else if(BizConstant.MSG_BODY_TYPE_VCARD.equalsIgnoreCase(msgtype)){
    //            localtype = NOTICE_TYPE_FRIEND_SEND;
    //        }else if(BizConstant.MSG_BODY_TYPE_POSTCARD.equalsIgnoreCase(msgtype)){
    //            localtype = NOTICE_TYPE_VCARD_SEND;
    //        }else if(BizConstant.MSG_BODY_TYPE_MULTITRUST.equalsIgnoreCase(msgtype)){
    //            localtype = NOTICE_TYPE_FEEEDBACK_SEND;
    //        }else if(BizConstant.MSG_BODY_TYPE_TXT.equalsIgnoreCase(msgtype)){
    //            localtype = NOTICE_TYPE_TXT_SEND;
    //        }else if(BizConstant.MSG_BODY_TYPE_COMMON.equalsIgnoreCase(msgtype)){
    //            localtype = NOTICE_TYPE_MEETING_INVITE;
    //        }
    //        return localtype;
    //    }
    //

        private String getMsgHeadByStrType(String bodytype, String snipTxt){

            String offlineMsg ="";
            String typecontext = "";
            String name=ShowNameUtil.getShowName(ShowNameUtil.getNameElement("",
                    userAccountInfo.nickName,
                    userAccountInfo.mobile,
                    userAccountInfo.nube));

            if(BizConstant.MSG_BODY_TYPE_POSTCARD.equals(bodytype)){
                //typecontext = BizConstant.MSG_TYPE_POSTCARD_SEND_SM3S;
                typecontext ="[名片]";
                offlineMsg=name+":"+typecontext;
            }else if(BizConstant.MSG_BODY_TYPE_PIC_2.equals(bodytype)){
                //typecontext = BizConstant.MSG_TYPE_PHOTO_SM1_2;
                typecontext ="[图片]";
                offlineMsg=name+":"+typecontext;
            }else if(BizConstant.MSG_BODY_TYPE_VIDEO_2.equals(bodytype)){
                //typecontext = BizConstant.MSG_TYPE_VIDEO_SM2_2;
                typecontext ="[视频]";
                offlineMsg=name+":"+typecontext;
            }else if(BizConstant.MSG_BODY_TYPE_AUDIO.equals(bodytype)){
                //typecontext = BizConstant.MSG_TYPE_AUDIO_SEND_SM3AU;
                typecontext ="[语音]";
                offlineMsg=name+":"+typecontext;
            }else if(BizConstant.MSG_BODY_TYPE_TXT.equals(bodytype)){
    //			if(TextUtils.isEmpty(snipTxt)){
    //				typecontext = BizConstant.MSG_TYPE_TXT_SM3TXT;
    //			}else{
    //				typecontext = BizConstant.MSG_TYPE_TXT_SM3TXT+snipTxt;
    //			}
                typecontext =snipTxt;
                offlineMsg=name+":"+typecontext;
            }else if(BizConstant.MSG_BODY_TYPE_VCARD.equals(bodytype)){
                //typecontext = BizConstant.MSG_TYPE_VCARD_SEND_SM3;
                typecontext ="有一条好友邀请消息";
                offlineMsg=typecontext;
            }else if(BizConstant.MSG_BODY_TYPE_MULTITRUST.equals(bodytype)){
                //typecontext = BizConstant.MSG_TYPE_VCARD_RECEIVE_SM4;
                typecontext ="通过了你的好友邀请";
                offlineMsg=name+":"+typecontext;
            }else if(BizConstant.MSG_BODY_TYPE_ONEKEYVISIT.equals(bodytype)){
                //typecontext = BizConstant.MSG_TYPE_ONEKEYVISIT_SEND_SM3OK;

            } else {
    //			if(TextUtils.isEmpty(snipTxt)){
    //				typecontext = BizConstant.MSG_TYPE_TXT_SM3TXT;
    //			}else{
    //				typecontext = BizConstant.MSG_TYPE_TXT_SM3TXT+snipTxt;
    //			}
            }

            return offlineMsg;
        }

        private String getMsgHead(int type, String snipTxt){
            String offlineMsg ="";
            String typecontext = "";
            String name= ShowNameUtil.getShowName(ShowNameUtil.getNameElement("",
                    userAccountInfo.nickName,
                    userAccountInfo.mobile,
                    userAccountInfo.nube));

            switch(type){
                case NOTICE_TYPE_VCARD_SEND:
                    //typecontext = BizConstant.MSG_TYPE_POSTCARD_SEND_SM3S;
                    typecontext = "[名片]";
                    offlineMsg=name+":"+typecontext;
                    break;
                case NOTICE_TYPE_PHOTO_SEND:
                    //typecontext = BizConstant.MSG_TYPE_PHOTO_SM1_2;
                    typecontext = "[图片]";
                    offlineMsg=name+":"+typecontext;
                    break;
                case NOTICE_TYPE_VEDIO_SEND:
                    //typecontext = BizConstant.MSG_TYPE_VIDEO_SM2_2;
                    typecontext = "[视频]";
                    offlineMsg=name+":"+typecontext;
                    break;
                case NOTICE_TYPE_AUDIO_SEND:
                    //typecontext = BizConstant.MSG_TYPE_AUDIO_SEND_SM3AU;
                    typecontext = "[语音]";
                    offlineMsg=name+":"+typecontext;
                    break;
                case NOTICE_TYPE_FILE:
                    typecontext = "[文件]";
                    offlineMsg = name+":"+typecontext;
                    break;
                case NOTICE_TYPE_MEETING_INVITE:
                    typecontext = snipTxt;
                    offlineMsg=name+":"+typecontext;
                    break;
                case NOTICE_TYPE_TXT_SEND:
    //				if(TextUtils.isEmpty(snipTxt)){
    //					typecontext = BizConstant.MSG_TYPE_TXT_SM3TXT;
    //				}else{
    //					typecontext = BizConstant.MSG_TYPE_TXT_SM3TXT+snipTxt;
    //				}
                    typecontext = snipTxt;
                    offlineMsg=name+":"+typecontext;
                    break;
                case NOTICE_TYPE_FRIEND_SEND:
                    //typecontext = BizConstant.MSG_TYPE_VCARD_SEND_SM3;
                    typecontext = "有一条好友邀请消息";
                    offlineMsg=typecontext;
                    break;
                case NOTICE_TYPE_FEEEDBACK_SEND:
                    //typecontext = BizConstant.MSG_TYPE_VCARD_RECEIVE_SM4;
                    typecontext = "通过了你的好友邀请";
                    offlineMsg=name+":"+typecontext;
                    break;
                default:

            }
            return offlineMsg;
        }

        /**
         * 截取文字消息的前25个字符
         * @param bean
         * @return
         */
        private String getSnipTxt(NoticesBean bean){
            String snipTxt = "";
            if(bean!=null && NOTICE_TYPE_TXT_SEND==bean.getType()){
                String body = bean.getBody();
                if(!TextUtils.isEmpty(body)){
                    try {
                        JSONArray array = new JSONArray(body);
                        if(array!=null&&array.length()>0){
                            JSONObject item = array.getJSONObject(0);
                            snipTxt = item.optString("txt");
                        }
                        array = null;
                        if(!TextUtils.isEmpty(snipTxt)&&snipTxt.length()>25){
                            snipTxt = snipTxt.substring(0, 26)+"...";
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
            return snipTxt;
        }
    //
    //    private boolean isValidFilePath(String filePath) {
    //        if (TextUtils.isEmpty(filePath) || filePath.endsWith(".temp")) {
    //            return false;
    //        }
    //        File file = new File(filePath);
    //        if (!file.exists()) {
    //            return false;
    //        }
    //        return true;
    //    }

        /**
         *
         * @param receiver 接收者
         * @param uuid 收藏记录的ID
         * @param position ：-1，表示分享全部收藏记录。0表示分享第一个位置的记录，1表示分享第二个位置上的记录，依次类推
         * @return
         */
        public boolean forwardMessageForCollection(String receiver, String uuid,int position) {
            LogUtil.begin("receiver:" + receiver + "|uuid:" + uuid+"|position="+position);
            if (TextUtils.isEmpty(receiver)) {
                LogUtil.d("receiver 为空");
                return false;
            }
            List<String> msgIds=buildForwardMsgByCollection(receiver, mCollectionDao.getCollectionEntityById(uuid), position);
            for (String id: msgIds){
                addTask(id, null);
            }
            return msgIds.size()>0;
        }

    /**
     *
     * @param receiver 接收者
     * @param itemInfo 收藏记录的详细信息
     * @param position ：-1，表示分享全部收藏记录。0表示分享第一个位置的记录，1表示分享第二个位置上的记录，依次类推
     * @return
     */
    public boolean forwardMessageForCollectionOther(String receiver, DataBodyInfo itemInfo, int position) {
        LogUtil.begin("receiver:" + receiver + "|uuid:" + itemInfo+"|position="+position);
        if (TextUtils.isEmpty(receiver)) {
            LogUtil.d("receiver 为空");
            return false;
        }
        List<String> msgIds=buildForwardMsgByCollection(receiver, convertCollectDeteilInfoToEntity(itemInfo), position);
        for (String id: msgIds){
            addTask(id, null);
        }
        return msgIds.size()>0;
    }

    private CollectionEntity convertCollectDeteilInfoToEntity(DataBodyInfo item){
        CollectionEntity entity=new CollectionEntity();
        try {

            entity.setOperateTime(Long.parseLong(item.getCollecTime()));
            entity.setType(item.getType());
            JSONObject itemObj = new JSONObject();
            JSONArray itemArray = new JSONArray();

            if(item.getType() == 8){  //文本
                itemObj.put("txt",item.getTxt());
                itemArray.put(0,itemObj);
                entity.setBody(itemArray.toString());
            }else if(item.getType() == 3){ //视频
                itemObj.putOpt("fileName",item.getFileName().length() > 0 ? item.getFileName() : "" );
                itemObj.putOpt("remoteUrl",item.getRemoteUrl().length() > 0 ? item.getRemoteUrl() : "");
                itemObj.putOpt("duration",item.getDuration());
                itemObj.putOpt("localUrl",item.getLocalUrl().length() > 0 ? item.getLocalUrl() : "");
                itemObj.putOpt("thumbnailRemoteUrl",item.getThumbnailRemoteUrl().length() > 0 ? item.getThumbnailRemoteUrl() : "");
                itemObj.putOpt("size",item.getSize());
                itemArray.put(0,itemObj);
                entity.setBody(itemArray.toString());
            }else if(item.getType() == 2){ //图片

                itemObj.putOpt("remoteUrl",item.getRemoteUrl().length() > 0 ? item.getRemoteUrl() : "");
                itemObj.putOpt("duration",item.getDuration());
                itemObj.putOpt("localUrl",item.getLocalUrl().length() > 0 ? item.getLocalUrl() : "");
                itemObj.putOpt("thumbnailRemoteUrl",item.getThumbnailRemoteUrl().length() > 0 ? item.getThumbnailRemoteUrl() : "");
                itemObj.putOpt("size",item.getSize());
                itemObj.putOpt("height",item.getPhotoHeight());
                itemObj.putOpt("width",item.getPhotoWidh());
                itemObj.putOpt("compressPath","");
                itemArray.put(0,itemObj);
                entity.setBody(itemArray.toString());
            }else {
                CustomLog.d(TAG,"不能发送的收藏，无需解析");
            }
        }catch (Exception e){
            CustomLog.d(TAG,e.toString());
        }
        return entity;
    }

        /**
         * 构建转发消息记录，返回新记录的ID
         * @param receiver  接收对象的视频号（多个视频号用分号分割）
         * @param entity      将要转发的消息uuid
         * @param position ：-1，表示分享收藏记录。>0，表示将收藏记录拆分开，分享某个位置上的记录
         * @return 返回新记录的id
         */
        private List<String> buildForwardMsgByCollection(String receiver, CollectionEntity entity, int position) {
            List<String> ids=new ArrayList<String>();
            if(entity==null){
                LogUtil.d("entity==null");
                return ids;
            }
            //链接类型转换成文字类型进行转发
            if (NOTICE_TYPE_URL==entity.getType()){
                entity.setType(NOTICE_TYPE_TXT_SEND);
            }
            String sender= MedicalApplication.getPreference().getKeyValue(PrefType.LOGIN_NUBENUMBER, "");
            String recipentIds = StringUtil.sortRecipentIds(receiver, ';');

            List<JSONArray> newArrays=getNewBodyList(entity.getBody(), position);
            for(JSONArray newArrary:newArrays){
                NoticesBean bean = new NoticesBean();
                bean.setSender(sender);
                bean.setReciever(recipentIds);

                // added by zhaguitao on 20160622 for 转发收藏数据时，须将收藏body中的key（thumbnailRemoteUrl）转换成消息body中的key（thumbnail）
                // 本来android这边收藏也用thumbnail就好了，但ios客户端不愿意转成thumbnail，所以为了适配ios客户端，android客户端转换
                Map<String, String> keys = new HashMap<String, String>();
                keys.put("thumbnailRemoteUrl", "thumbnail");
                keys.put("photoWidth", "width");
                keys.put("photoHeight", "height");
                bean.setBody(CollectionManager.modifyBodyJsonKey(newArrary, keys));

                bean.setStatus(TASK_STATUS_READY);
                bean.setType(entity.getType());
                bean.setIsNew(0);
                long curtime = System.currentTimeMillis();
                bean.setSendTime(curtime);
                bean.setReceivedTime(curtime);
                bean.setTitle("");
                String newItemuuid = StringUtil.getUUID();
                bean.setId(newItemuuid);
                bean.setMsgId(newItemuuid);
                bean.setFailReplyId("");
                // 修改下extInfo中id的值
    //    		String oldExt = entity.getExtinfo()
    //    		JSONObject extObj = null;
    //    		if (!TextUtils.isEmpty(oldExt)) {
    //    			try {
    //    	        	extObj = new JSONObject(oldExt);
    //    	        	extObj.put("id", bean.getId());
    //    	            extObj.put("ver", BizConstant.MSG_VERSION);
    //    	        } catch (JSONException e) {
    //    	            LogUtil.e("JSONException", e);
    //    	            e.printStackTrace();
    //    	        }
    //            }else{
                JSONObject extObj = null;
                try {
                    extObj = new JSONObject();
                    extObj.put("id", bean.getId());
                    extObj.put("text", "");
                    extObj.put("ver", BizConstant.MSG_VERSION);
                } catch (JSONException e) {
                    LogUtil.e("JSONException", e);
                }
    //            }

                if(extObj!=null){
                    bean.setExtInfo(extObj.toString());
                }else{
                    bean.setExtInfo("");
                }
                //TODO 添加转发群组
                ThreadsDao threadsDao = new ThreadsDao(mcontext);
                if (recipentIds.length() < 12) {
                    String covstid = threadsDao.createThread(recipentIds, curtime, true);
                    if (TextUtils.isEmpty(covstid)) {
                        LogUtil.d("createThread id==null");
                        return ids;
                    }
                    bean.setThreadsId(covstid);
                } else {
                    if (!threadsDao.isExistThread(recipentIds)) {
                        threadsDao.createThreadFromGroup(recipentIds);
                    }else{
                        threadsDao.updateLastTime(recipentIds, curtime);
                    }
                    bean.setThreadsId(recipentIds);
                }
                newItemuuid = noticedao.insertNotice(bean);
                LogUtil.end("newItemuuid:" + newItemuuid);
                ids.add(newItemuuid);
            }
            return ids;
        }

        private  List<JSONArray> getNewBodyList(String body,int position){
            List<JSONArray> newArrays=new ArrayList<JSONArray>();
            try {
                JSONArray oldBodyArray = new JSONArray(body);
                if (position==-1){
                    for (int i=0;i<oldBodyArray.length();i++){
                        JSONArray newArray=new JSONArray();
                        newArray.put(oldBodyArray.get(i));
                        newArrays.add(newArray);
                    }
                }else {
                    JSONArray newArray=new JSONArray();
                    newArray.put(oldBodyArray.get(position));
                    newArrays.add(newArray);
                }
            } catch (JSONException e) {
                LogUtil.e("JSONException body", e);
            }
            return newArrays;
        }
    public static String getTakePhotoDir() {
        if (!initTakePhotoDir()) {
            return "";
        }

        String dirpath = Environment.getExternalStorageDirectory()
            + File.separator + IMConstant.APP_ROOT_FOLDER
            + File.separator + FILE_TAKE_PHOTO_DIR;
        return dirpath;
    }


    public static boolean initTakePhotoDir() {
        if (!Environment.MEDIA_MOUNTED.equals(Environment
            .getExternalStorageState())) {
            return false;
        }

        String dirpath = Environment.getExternalStorageDirectory()
            + File.separator + IMConstant.APP_ROOT_FOLDER
            + File.separator + FILE_TAKE_PHOTO_DIR;
        File file = new File(dirpath);

        if (file.exists()) {
            return true;
        } else {
            return file.mkdirs();
        }
    }
}
