package cn.redcdn.hvs.im.work;

import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.text.TextUtils;
import android.widget.Toast;
import cn.redcdn.hvs.AccountManager;
import cn.redcdn.hvs.HomeActivity;
import cn.redcdn.hvs.MedicalApplication;
import cn.redcdn.hvs.R;
import cn.redcdn.hvs.im.IMConstant;
import cn.redcdn.hvs.im.activity.ChatActivity;
import cn.redcdn.hvs.im.agent.AppGroupManager;
import cn.redcdn.hvs.im.asyncTask.NetPhoneAsyncTask;
import cn.redcdn.hvs.im.asyncTask.QueryPhoneNumberHelper;
import cn.redcdn.hvs.im.bean.ContactFriendBean;
import cn.redcdn.hvs.im.bean.GroupMemberBean;
import cn.redcdn.hvs.im.bean.NoticesBean;
import cn.redcdn.hvs.im.bean.ShowNameUtil;
import cn.redcdn.hvs.im.dao.GroupDao;
import cn.redcdn.hvs.im.dao.MedicalDaoImpl;
import cn.redcdn.hvs.im.dao.NewFriendDao;
import cn.redcdn.hvs.im.dao.NoticesDao;
import cn.redcdn.hvs.im.preference.DaoPreference.PrefType;
import cn.redcdn.hvs.im.util.IMCommonUtil;
import cn.redcdn.hvs.im.util.xutils.http.SyncResult;
import cn.redcdn.hvs.im.work.MessageBaseParse.ExtInfo;
import cn.redcdn.hvs.meeting.meetingManage.MedicalMeetingManage;
import cn.redcdn.hvs.util.DateUtil;
import cn.redcdn.hvs.util.NotificationUtil;
import cn.redcdn.log.CustomLog;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Desc
 * Created by wangkai on 2017/2/27.
 */

public class MessageReceiveAsyncTask extends NetPhoneAsyncTask<String, SyncResult, Void> {

    private static final String TAG = "MessageReceiveAsyncTask";

    public static String DOWNLOAD_IDLIST = "MessageReceiveAsyncTask.download.idlist";

    private Context context = null;
    private GroupDao groupDao = null;
    private NoticesDao noticesDao = null;
    //    private AlarmMsgDao alarmDao = null;
    //    private DeviceDao devDao = null;
    private MedicalDaoImpl contactsDao = null;
    private NewFriendDao newFriendDao = null;

    private Intent meetingIntent = null;
    private String own = "";
    // 下面两个类变量，在跨进程使用中会出现不一致的现象；
    // 但目前就使用场景来看，到也不会出现大的问题。
    private static boolean isRunning = false;
    // 是否Toast提示异常信息
    private boolean bShowTip = false;
    // 单次消息接收过程回调监听
    private MessageReceiverListener listener = null;

    private Map<String, List<String>> folder_msgs_map = null;

    // 记录当次消息接收中的发送者
    // 普通消息（视频、图片、声音、文字、名片）的发送者《nube----消息类型+内容简介》
    private Map<String, String> msgsender = new HashMap<String, String>();
    // 好友添加消息（添加好友、同意绘制）的发送者
    private Map<String, String> frisender = new HashMap<String, String>();
    // 报警消息
    private String alarmTxt = null;
    // 群组消息（视频、图片、声音、文字、名片）《gid----消息类型+发送者+内容简介》
    private Map<String, String> groupMsgSnippet = new HashMap<String, String>();

    public static boolean FLAG_OF_NOTIFICATION_NOT_ON_LINE_MSG = true;
    // 下列定义为当前版本能识别的消息类型
    /***
     * private static String TYPE_PIC_1 = BizConstant.MSG_APP_N8_PHOTO + "_" +
     * BizConstant.MSG_BODY_TYPE_PIC; private static String TYPE_PIC_2 =
     * BizConstant.MSG_APP_N8_PHOTO + "_" + BizConstant.MSG_BODY_TYPE_PIC_2;
     * private static String TYPE_VIDEO_1 = BizConstant.MSG_APP_N8_SPHONE + "_"
     * + BizConstant.MSG_BODY_TYPE_VIDEO; private static String TYPE_VIDEO_2 =
     * BizConstant.MSG_APP_N8_SPHONE + "_" + BizConstant.MSG_BODY_TYPE_VIDEO_2;
     * private static String TYPE_AUDIO = BizConstant.MSG_APP_NAME + "_" +
     * BizConstant.MSG_BODY_TYPE_AUDIO; private static String TYPE_TXT =
     * BizConstant.MSG_APP_NAME + "_" + BizConstant.MSG_BODY_TYPE_TXT; private
     * static String TYPE_CARD = BizConstant.MSG_APP_NAME + "_" +
     * BizConstant.MSG_BODY_TYPE_POSTCARD; private static String TYPE_ADDFRI =
     * BizConstant.MSG_APP_N8_CONTACT + "_" + BizConstant.MSG_BODY_TYPE_VCARD;
     * private static String TYPE_FEEDBACK = BizConstant.MSG_APP_N8_CONTACT +
     * "_" + BizConstant.MSG_BODY_TYPE_MULTITRUST; private static String
     * TYPE_OKVISIT = BizConstant.MSG_APP_NAME + "_" +
     * BizConstant.MSG_BODY_TYPE_ONEKEYVISIT; private static String TYPE_MSGRP =
     * BizConstant.MSG_APP_NAME + "_" + BizConstant.MSG_BODY_TYPE_MSGRP; private
     * static String TYPE_HK_IMG = BizConstant.MSG_APP_NAME + "_" +
     * BizConstant.MSG_BODY_TYPE_HK_IMG; // private static String TYPE_IPCALL =
     * // BizConstant.MSG_APP_NAME+"_"+BizConstant.MSG_BODY_TYPE_IPCALL;
     **/
    private static String TYPE_PIC_1 = BizConstant.MSG_BODY_TYPE_PIC;
    private static String TYPE_PIC_2 = BizConstant.MSG_BODY_TYPE_PIC_2;
    private static String TYPE_VIDEO_1 = BizConstant.MSG_BODY_TYPE_VIDEO;
    private static String TYPE_VIDEO_2 = BizConstant.MSG_BODY_TYPE_VIDEO_2;
    private static String TYPE_AUDIO = BizConstant.MSG_BODY_TYPE_AUDIO;
    private static String TYPE_TXT = BizConstant.MSG_BODY_TYPE_TXT;
    private static String TYPE_COMMON = BizConstant.MSG_BODY_TYPE_COMMON;
    private static String TYPE_CARD = BizConstant.MSG_BODY_TYPE_POSTCARD;
    private static String TYPE_ADDFRI = BizConstant.MSG_BODY_TYPE_VCARD;
    private static String TYPE_FEEDBACK = BizConstant.MSG_BODY_TYPE_MULTITRUST;
    private static String TYPE_OKVISIT = BizConstant.MSG_BODY_TYPE_ONEKEYVISIT;
    private static String TYPE_MSGRP = BizConstant.MSG_BODY_TYPE_MSGRP;
    private static String TYPE_HK_IMG = BizConstant.MSG_BODY_TYPE_HK_IMG;
    // private static String TYPE_IPCALL = BizConstant.MSG_BODY_TYPE_IPCALL;

    public static final List<String> MSGTYPES = new ArrayList<String>();

    private PrivateMessage item;


    static {
        MSGTYPES.add(TYPE_PIC_1);
        MSGTYPES.add(TYPE_PIC_2);
        MSGTYPES.add(TYPE_VIDEO_1);
        MSGTYPES.add(TYPE_VIDEO_2);
        MSGTYPES.add(TYPE_AUDIO);
        MSGTYPES.add(TYPE_TXT);
        MSGTYPES.add(TYPE_CARD);
        MSGTYPES.add(TYPE_ADDFRI);
        MSGTYPES.add(TYPE_FEEDBACK);
        MSGTYPES.add(TYPE_OKVISIT);
        MSGTYPES.add(TYPE_MSGRP);
        MSGTYPES.add(TYPE_HK_IMG);
        MSGTYPES.add(TYPE_COMMON);
        // MSGTYPES.add(TYPE_IPCALL);
    }


    public static boolean isRunning() {
        return isRunning;
    }


    public void setReceiverListener(MessageReceiverListener listener) {
        this.listener = listener;
    }


    public void setShowErrorTip(boolean show) {
        this.bShowTip = show;
    }


    public MessageReceiveAsyncTask() {
        this.context = MedicalApplication.getContext();
        this.bShowTip = false;
        this.own = AccountManager.getInstance(context).getAccountInfo().nube;
        this.groupDao = new GroupDao(context);
        this.noticesDao = new NoticesDao(context);
        //        this.alarmDao = new AlarmMsgDao(context);
        //        this.devDao = new DeviceDao(context);
        this.contactsDao = new MedicalDaoImpl(context);
        this.newFriendDao = new NewFriendDao(context);
    }


    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        if (listener != null) {
            listener.onStarted();
        }
    }


    @Override
    protected Void doInBackground(String... arg0) {

        //        isRunning = true;
        //        // 获取消息的简单的索引
        //        String appname = "";
        //        String typename = "";
        //        SyncResult obj = doHttpGetAllMsgsIndex(appname, typename);
        //
        //        // 解析未读的消息索引，并按文件夹归类
        //        List<PrivateMessage> msgdetaillist = parseDetailMsgList(obj);
        //        if (msgdetaillist != null && msgdetaillist.size() > 0) {
        //
        //            // 下面按文件夹分组，方便置阅读消息状态
        //            folder_msgs_map = new HashMap<String, List<String>>();
        //            int indexLength = msgdetaillist.size();
        //            PrivateMessage item = null;
        //            List<String> msgidList = null;
        //            for (int i = 0; i < indexLength; i++) {
        //                item = msgdetaillist.get(i);
        //                if (folder_msgs_map.containsKey(item.folderId)) {
        //                    msgidList = folder_msgs_map.get(item.folderId);
        //                    msgidList.add(item.msgId);
        //                } else {
        //                    msgidList = new ArrayList<String>();
        //                    // TODO:在list的首位记录APP属性
        //                    msgidList.add(0, item.app);
        //                    msgidList.add(item.msgId);
        //                    folder_msgs_map.put(item.folderId, msgidList);
        //                }
        //            }
        //
        //            // 保存消息到本地
        //            doBatchSave(msgdetaillist);
        //            // 内存数据清理
        //            msgdetaillist.clear();
        //            msgdetaillist = null;
        //
        //            // 置消息已读状态
        //            Iterator<Map.Entry<String, List<String>>> it1 = folder_msgs_map
        //                    .entrySet().iterator();
        //            while (it1.hasNext()) {
        //                Map.Entry<String, List<String>> entry = it1.next();
        //                String folderid = entry.getKey();
        //                List<String> msgids = entry.getValue();
        //                /**
        //                 * 这个通知服务器该消息一经从服务端下载过了 1.成功了 就不会再次从服务端下载
        //                 * 2.有可能失败但是失败的时候我也继续讲该消息插入到 NoticeTable 表中 这样的话有可能会出现两条消息
        //                 * 是因为有不同的id TODO 针对动态有可能出现多个消息我应该根据消息id进行排重 TODO 注意将msgids
        //                 * list的首位移除（APP值）
        //                 */
        //                String app = msgids.get(0);
        //                msgids.remove(0);
        //                // 按文件夹和消息id, 置已读状态
        //                @SuppressWarnings("unused")
        //                Object reslut = doHttpSetStatus(app, folderid, msgids);
        //            }
        //            if (folder_msgs_map != null) {
        //                folder_msgs_map.clear();
        //                folder_msgs_map = null;
        //            }
        //
        //            // 推送通知栏消息
        //            sendNotifacation();
        //            msgsender.clear();
        //            frisender.clear();
        //            alarmTxt = null;
        //        }
        return null;
    }


    /**
     * 解决会议外呼弹屏和系统通知出现冲突问题 原因：外呼弹屏时页面拉起，此时客户端判断应用在前端故而不弹系统通知
     * 解决方法：用meetingIntent作为是否存在有会议弹屏消息的标记，在sendNotifacation调用之后，再做会议弹屏的逻辑
     */
    private void handleInviteMeetingCall() {
        if (meetingIntent != null) {
            context.sendBroadcast(meetingIntent);

            String data = meetingIntent.getStringExtra(IMCommonUtil.KEY_BROADCAST_INTENT_DATA);
            CustomLog.d(TAG, "收到邀请会议消息广播:" + data);
            String id = "";
            String name = "";
            String headurl = "";
            String room = "";
            boolean show = false;
            try {
                JSONObject object = new JSONObject(data);
                id = object.optString("inviterId");
                name = object.optString("inviterName");
                headurl = object.optString("inviterHeadUrl");
                room = object.optString("meetingRoom");
                show = object.optBoolean("showMeeting", false);
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            MedicalMeetingManage.getInstance().incomingCall(id, name, room, headurl,
                new MedicalMeetingManage.OnIncommingCallListener() {
                    @Override public void onIncommingCall(String arg0, int code) {
                        CustomLog.d(TAG, "onInCommingCall");
                    }
                });

        }
        meetingIntent = null;
    }


    @Override
    protected void onProgressUpdate(SyncResult... values) {
        super.onProgressUpdate(values);
        if (bShowTip) {
            if (values == null || values.length == 0) {
                return;
            }
            SyncResult result = (SyncResult) values[0];
            if (!result.isOK()) {
                String tip = result.getErrorMsg();
                CustomLog.d("", "Toast:" + tip);
                Toast.makeText(context, tip, Toast.LENGTH_SHORT).show();
            }
        }
    }


    @Override
    protected void onPostExecute(Void result) {
        super.onPostExecute(result);
        isRunning = false;
        if (folder_msgs_map != null) {
            folder_msgs_map.clear();
            folder_msgs_map = null;
        }
        if (listener != null) {
            listener.onFinished();
        }
    }


    @SuppressWarnings("unused")
    private String getAppNameFromType(String msgtype) {
        if (!TextUtils.isEmpty(msgtype)) {
            int index = msgtype.indexOf('_');
            if (index != -1) {
                return msgtype.substring(0, index);
            } else {
                return msgtype;
            }
        }
        return "";
    }


    private void removeMsgId(String msgId) {

        if (folder_msgs_map != null) {
            Iterator<Map.Entry<String, List<String>>> it1 = folder_msgs_map
                .entrySet().iterator();
            while (it1.hasNext()) {
                Map.Entry<String, List<String>> entry = it1.next();
                List<String> msgIds = entry.getValue();
                if (msgIds != null) {
                    msgIds.remove(msgId);
                }
            }
        }

    }


    private void doBatchSave(List<PrivateMessage> msg) {

        if (msg == null || msg.size() == 0) {
            return;
        }

        int length = msg.size();
        item = null;
        NoticesBean repeatData = null;
        msgsender.clear();
        frisender.clear();
        alarmTxt = null;

        for (int i = 0; i < length; i++) {

            item = msg.get(i);

            String gid = item.gid;
            boolean isGroupMsg = false;
            String groupPN = MessageGroupEventParse.getGroupPublicNumber();
            String extString = item.extendedInfo;
            if (!TextUtils.isEmpty(extString)) {
                try {
                    JSONObject obj = new JSONObject(extString);
                    String mobile = obj
                        .optString(IMConstant.NEW_CALL_DATA_SUB_KEY_MOBILE);
                    if (!TextUtils.isEmpty(mobile)) {
                        QueryPhoneNumberHelper.addPhoneAndNubeToCache(mobile,
                            item.sender);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    CustomLog.d(TAG, "消息扩展格式不正确");
                }
            }

            // 校验gid的有效性
            if (!TextUtils.isEmpty(gid)) {
                isGroupMsg = true;
                // 是否已存在；若没有则通过接口查询出群组的详情并保存
                MessageGroupEventParse parse = new MessageGroupEventParse(item);
                boolean exit = parse.groupExist(gid);
                if (!exit) {
                    AppGroupManager.getInstance(context)
                        .groupQueryDetailBackgroud(gid, parse);
                    if (groupPN.equals(item.sender)) {
                        continue;
                    }
                } else {
                    // 是否是群事件的公众消息
                    if (groupPN.equals(item.sender)) {
                        parse.parseMessage();
                        continue;
                    }
                }
            }

            // @lihs 2013.12.31 根据消息ID 排重，防止更新服务端消息信息失败的场合多次插入表中
            String id = item.msgId;//.replace("-", "");
            String bodyType = splitBodyType(item.type);
            if (item.type.equalsIgnoreCase(TYPE_HK_IMG)) {
                // x1报警消息排重
                CustomLog.d(TAG, "之前的报警消息，不做处理");
                //                if (alarmDao.existAlarmById(id)) {
                //                    LogUtil.d("该报警消息已经存在，不保存，重复消息下载消息ID=" + id);
                //                    continue;
                //                }
            } else {
                repeatData = noticesDao.getNoticeById(id);
                if (repeatData != null) {
                    CustomLog.d(TAG, "该消息已经存在，不再往NoticesTable表中加入数据，重复消息下载消息ID=" + id);
                    continue;
                }
            }

            // 可视极会议中不再经行该判断
            // // 普通的消息 需要 根据“接收陌生人消息”设置判断是否显示消息
            // if( !isGroupMsg && !bodyType.equalsIgnoreCase(TYPE_ADDFRI)
            // && !bodyType.equalsIgnoreCase(TYPE_FEEDBACK)
            // && !bodyType.equalsIgnoreCase(TYPE_OKVISIT)
            // && !bodyType.equalsIgnoreCase(TYPE_HK_IMG)
            // && !groupPN.equalsIgnoreCase(item.sender) //群事件消息
            // && !OutCallUtil.recieverCallAndMsg(item.sender)){
            // LogUtil.d("不接收陌生人的消息,消息ID=" + id
            // +" sender="+item.sender+" type="+bodyType);
            // continue;
            // }

            boolean succ = false;
            if (bodyType.equalsIgnoreCase(TYPE_MSGRP)) {
                // 图片、视频的回复消息
                // 将评论/回复消息变成一个txt消息记录下来
                String body = item.body;
                if (!TextUtils.isEmpty(body)) {
                    String txt = "";
                    String title = context.getString(R.string.comment);
                    try {
                        JSONObject obj = new JSONObject(body);
                        txt = obj.optString("msg");
                    } catch (JSONException e) {
                        e.printStackTrace();
                        CustomLog.d(TAG, "消息体格式不正确");
                    }
                    MessageBaseParse parse = new MessageBaseParse();
                    succ = parse.insertTxtMessage(item, txt, title);
                    if (succ) {
                        msgsender.put(item.sender, item.online + txt);
                    }
                } else {
                    succ = true;
                    CustomLog.d(TAG, "消息扩展字段为空");
                }
            } else if (bodyType.equalsIgnoreCase(TYPE_VIDEO_1)
                || bodyType.equalsIgnoreCase(TYPE_VIDEO_2)) {
                // 分享的视频消息
                MessageVedioParse parse = new MessageVedioParse(item);
                succ = parse.parseMessage();
                if (succ) {
                    String txt = "";
                    ExtInfo extinfo = parse.getExtInfoAfterParse();
                    if (extinfo != null) {
                        txt = getNickName(item.sender) + "发来一段视频";
                    }
                    if (isGroupMsg) {
                        groupMsgSnippet.put(gid, item.online
                            + getGroupMemberName(gid, item.sender)
                            + "发来一段视频");
                    } else {
                        if (!TextUtils.isEmpty(txt)) {
                            msgsender.put(item.sender, item.online + txt);
                        } else {
                            msgsender
                                .put(item.sender,
                                    item.online
                                        + "发来一段视频");
                        }
                    }
                }
            } else if (bodyType.equalsIgnoreCase(TYPE_CARD)) {
                // 分享名片的消息
                String ver = "";
                String tmpName = "";
                if (!TextUtils.isEmpty(item.extendedInfo)) {
                    try {
                        JSONObject obj = new JSONObject(item.extendedInfo);
                        ver = obj.optString("ver");
                        JSONArray bodyArray = new JSONArray(obj.optString("card"));
                        if (bodyArray != null && bodyArray.length() > 0) {
                            JSONObject bodyObj = bodyArray.optJSONObject(0);
                            tmpName = bodyObj.optString("name");
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        CustomLog.d(TAG, "消息扩展格式不正确");
                        CustomLog.d(TAG, "解析名片信息出错");
                    }
                } else {
                    CustomLog.d(TAG, "消息扩展字段为空");
                }
                MessageCardParse parse = new MessageCardParse(item);
                String txt = "";
                succ = parse.parseMessage();
                if (succ && !TextUtils.isEmpty(ver)) {

                    txt = getNickName(item.sender) + "发来了" + tmpName + "的名片";

                    if (isGroupMsg) {
                        groupMsgSnippet.put(gid, item.online
                            + getGroupMemberName(gid, item.sender)
                            + "发来了" + tmpName + "的名片");
                    } else {
                        msgsender
                            .put(item.sender,
                                item.online
                                    + txt);
                    }
                }
            } else if (bodyType.equalsIgnoreCase(TYPE_PIC_1)
                || bodyType.equalsIgnoreCase(TYPE_PIC_2)) {
                // 分享图片的消息
                MessagePicParse parse = new MessagePicParse(item);
                succ = parse.parseMessage();
                if (succ) {
                    String txt = "";
                    ExtInfo extinfo = parse.getExtInfoAfterParse();
                    if (extinfo != null) {
                        txt = getNickName(item.sender) + "发来一张图片";
                    }
                    if (isGroupMsg) {
                        groupMsgSnippet.put(gid, item.online
                            + getGroupMemberName(gid, item.sender)
                            + "发来一张图片");
                    } else {
                        if (!TextUtils.isEmpty(txt)) {
                            msgsender.put(item.sender, item.online + txt);
                        } else {
                            msgsender
                                .put(item.sender,
                                    item.online
                                        + "发来一张图片");
                        }
                    }
                }
            } else if (bodyType.equalsIgnoreCase(TYPE_TXT)) {
                // 文字信息
                String txt = "";
                String subStr = "";
                MessageTextParse parse = new MessageTextParse(item);
                succ = parse.parseMessage();
                if (succ) {
                    ExtInfo extinfo = parse.getExtInfoAfterParse();
                    if (extinfo != null) {
                        txt = extinfo.text;
                        if (txt.length() > 43) {
                            subStr = txt.substring(0, 43) + "...";
                        }else {
                            subStr = txt;
                        }
                        txt = getNickName(item.sender) + "：" + subStr;

                    }
                    if (isGroupMsg) {
                        if (txt.contains(IMConstant.SPECIAL_CHAR + "")) {
                            ArrayList<String> result = new ArrayList<String>();
                            result = IMCommonUtil.getDispList(txt);
                            for (int j = 0; j < result.size(); j++) {
                                GroupMemberBean gbean = groupDao
                                    .queryGroupMember(gid, result.get(j));
                                if (gbean != null) {
                                    ShowNameUtil.NameElement element = ShowNameUtil
                                        .getNameElement(gbean.getName(),
                                            gbean.getNickName(),
                                            gbean.getPhoneNum(),
                                            gbean.getNubeNum());
                                    String MName = ShowNameUtil
                                        .getShowName(element);
                                    txt = txt.replace("@" + result.get(j)
                                        + IMConstant.SPECIAL_CHAR, "@"
                                        + MName
                                        + IMConstant.SPECIAL_CHAR);
                                }
                            }
                        }
                        groupMsgSnippet.put(gid, item.online
                            + getGroupMemberName(gid, item.sender) + "：" + subStr);
                    } else {
                        msgsender.put(item.sender, item.online +
                            getNickName(item.sender)+"：" + subStr);
                    }
                }
            } else if (bodyType.equalsIgnoreCase(TYPE_COMMON)) {

                ExtInfo extInfo = MessageBaseParse
                    .convertExtInfo(item.extendedInfo);

                String subtype = extInfo != null ? extInfo.subtype : "";

                if (BizConstant.MSG_SUB_TYPE_MEETING.equals(subtype)) {
                    // 会议邀请信息
                    String txt = "";
                    MessageMeetingParse parse = new MessageMeetingParse(item);
                    succ = parse.parseMessage();
                    if (succ) {
                        ExtInfo extinfo = parse.getExtInfoAfterParse();
                        if (extinfo != null) {
                            txt = getNickName(item.sender) + "邀请你视频会诊";
                        }
                        if (isGroupMsg) {
                            groupMsgSnippet.put(gid, item.online
                                + getGroupMemberName(gid, item.sender)
                                + "邀请你视频会诊");
                        } else {
                            msgsender.put(item.sender, item.online +getNickName(item.sender)
                                + "邀请你视频会诊");
                        }

                        // TODO:如果是即时消息，会发送广播调用会议的来电页面

                        Date sysDate = new Date();
                        long sysTime = sysDate.getTime() / 1000;
                        long itemTime = Long.parseLong(item.time) / 1000;

                        //退出应用后，在打开应用，时间间隔小于 30s ,才可以调出外呼
                        if ((sysTime - itemTime) < 30) {

                            CustomLog.d(TAG, "item time is " + item.time);
                            if (IMConstant.isP2PConnect) {
                                meetingIntent = new Intent();
                                meetingIntent
                                    .setAction(BizConstant.JMEETING_INVITE_ACTION);
                                meetingIntent
                                    .putExtra(
                                        IMCommonUtil.KEY_BROADCAST_INTENT_DATA,
                                        extinfo.meetingInfo);
                            }
                        }

                    }
                } else if (BizConstant.MSG_SUB_TYPE_MEETING_BOOK
                    .equals(subtype)) {
                    // 会议预约信息
                    String txt = "";
                    MessageBookMeetingParse parse = new MessageBookMeetingParse(
                        item);
                    succ = parse.parseMessage();
                    if (succ) {
                        ExtInfo extinfo = parse.getExtInfoAfterParse();
                        if (extinfo != null) {
                            txt = getNickName(item.sender) + "发来了预约会诊邀请";
                        }
                        if (isGroupMsg) {
                            groupMsgSnippet.put(gid, item.online
                                + getGroupMemberName(gid, item.sender)
                                + "发来了预约会诊邀请");
                        } else {
                            msgsender.put(item.sender, item.online + getNickName(item.sender)
                                +"发来了预约会诊邀请");
                        }
                    }
                } else if (BizConstant.MSG_SUB_TYPE_FILE.equals(subtype)) {
                    String txt = "";
                    MessageFileParse parse = new MessageFileParse(item);
                    succ = parse.parseMessage();
                    if (succ) {
                        ExtInfo extinfo = parse.getExtInfoAfterParse();
                        if (extinfo != null) {
                            txt = "[文件]";
                        }
                        if (isGroupMsg) {
                            groupMsgSnippet.put(gid, item.online
                                + getGroupMemberName(gid, item.sender)
                                + txt);
                        } else {
                            msgsender.put(item.sender, item.online + txt);
                        }
                    }

                } else {
                    // TODO:其他自定义的 sub type
                }
            } else if (bodyType.equalsIgnoreCase(TYPE_AUDIO)) {
                // 语音文件接收
                String txt = "";
                MessageAudioParse parse = new MessageAudioParse(item);
                succ = parse.parseMessage();
                if (succ) {

                    txt = getNickName(item.sender) + "发来一段语音";

                    if (isGroupMsg) {
                        groupMsgSnippet.put(gid, item.online
                            + getGroupMemberName(gid, item.sender)
                            + "发来一段语音");
                    } else {
                        msgsender
                            .put(item.sender,
                                item.online + getNickName(item.sender) 
                                    + "发来一段语音");
                    }
                }
            } else if (bodyType.equalsIgnoreCase(TYPE_ADDFRI)) {
                // 隐私设置拨打陌生人电话接收方接收到的消息
                CustomLog.d(TAG, "邀请添加好友消息，直接丢弃");
                // MessageAddFriParse parse = new MessageAddFriParse(context,
                // item);
                // succ = parse.parseMessage();
                // if (succ) {
                // frisender.put(item.sender,
                // context.getString(R.string.information));
                // }
            } else if (bodyType.equalsIgnoreCase(TYPE_FEEDBACK)) {
                // 回执消息
                CustomLog.d(TAG, ",邀请添加好友的回执消息，直接丢弃");
                // MessageFeedbackParse parse = new
                // MessageFeedbackParse(context,
                // item);
                // succ = parse.parseMessage();
                // if (succ) {
                // frisender.put(item.sender,
                // context.getString(R.string.invit_friend));
                // }
            } else if (bodyType.equalsIgnoreCase(TYPE_OKVISIT)) {
                // 20150522 新需求，无需一键回家消息
                // // 一键回家的信息
                // MessageOKVParse parse = new MessageOKVParse(context, item);
                // succ = parse.parseMessage();
                //
                // if (succ && parse.isDelete()) {
                // msgsender.put(item.sender,
                // context.getString(R.string.del_onekeyfriend));
                // }
            } else if (bodyType.equalsIgnoreCase(TYPE_HK_IMG)) {
                //                MobclickAgent.onEvent(context,
                //                        UmengEventConstant.EVENT_MESSAGE_TASK_RECV_ALARM);
                //                // x1报警图片消息
                //                AlarmPicParse parse = new AlarmPicParse(item, context);
                //                succ = parse.parseAlarm();
                //                // TODO:检验下关联关系,只有关联关系正常时，才提示；否则需要删除；
                //
                //                if (devDao.getDeviceFriend(item.sender,
                //                        DeviceColumn.STATUS_NORMAL + "")) {
                //                    if (succ) {
                //                        alarmTxt = context.getString(R.string.str_pic_alarm);
                //                    }
                //                } else {
                //                    alarmDao.deleteAllAlertMsg(item.sender);
                //                }
            }
            // else if (bodyType.equalsIgnoreCase(TYPE_IPCALL)) {
            // // TODO:语音来电(漏话提醒)，
            // // 以前版本（无通话记录前版本）有实现，现在版本已取消
            // }
            else {
                continue;
            }

            if (succ) {
                // 成功消费该信息,需要置已读状态
            } else {
                // 消费失败，不需要置已读状态
                removeMsgId(item.msgId);
            }
        }
    }


    /**
     * 判断是否是 开启了面打扰设置
     */
    private boolean forbiddenNotify(String id) {
        String forbiddenList = MedicalApplication.getPreference().getKeyValue(
            PrefType.KEY_CHAT_DONT_DISTURB_LIST, "");
        if (forbiddenList.contains(id)) {
            return true;
        }
        return false;
    }


    private boolean appOnTheDesk() {
        boolean bkg1 = isApplicationBroughtToBackground(context);
        boolean scrOn = isScreenOn(context);
        boolean scrlocked = isScreenLocked(context);
        CustomLog.d(TAG, "normal msg BroughtToBackground:" + bkg1 + " | scrOn:"
            + scrOn + " | scrlocked:" + scrlocked);
        // 在消息聊天界面、正在使用引用（不在聊天界面）-->归纳为：应用在前台，仅震动提示
        // 在聊天界面锁屏，（应用仍处于前台），产品经理要求：状态栏通知+声音提醒，故排除
        return !(!bkg1 && scrOn && !scrlocked);
    }


    /**
     * 在线消息，需要响铃
     * 离线消息，只需要响铃一次
     */
    private void doSendNotifyMsg(boolean online, String titleString, String name,
                                 String notifyId, String txt, Intent pdintent) {
        CustomLog.d(TAG,
            "online=" + online + "|txt=" + txt + "|FLAG_OF_NOTIFICATION_NOT_ON_LINE_MSG=" +
                FLAG_OF_NOTIFICATION_NOT_ON_LINE_MSG);
        if (online) {
            NotificationUtil.sendNotifacationForSmallIcoMSG(
                titleString, name, notifyId + "", txt, pdintent,
                NotificationUtil.NOTIFACATION_STYLE_MSG, true);
        } else {
            if (FLAG_OF_NOTIFICATION_NOT_ON_LINE_MSG) {
                NotificationUtil.sendNotifacationForSmallIcoMSG(
                    titleString, name, notifyId + "", txt, pdintent,
                    NotificationUtil.NOTIFACATION_STYLE_MSG, true);
                FLAG_OF_NOTIFICATION_NOT_ON_LINE_MSG = false;
            } else {
                NotificationUtil.sendNotifacationForSmallIcoMSG(
                    titleString, name, notifyId + "", txt, pdintent,
                    NotificationUtil.NOTIFACATION_STYLE_MSG, false);
            }
        }
    }


    /**
     * 在线消息，需要震动
     * 离线消息，只需要震动一次
     */
    private void doVibratorMsg(boolean online) {
        CustomLog.d(TAG, "online=" + online + "|FLAG_OF_NOTIFICATION_NOT_ON_LINE_MSG=" +
            FLAG_OF_NOTIFICATION_NOT_ON_LINE_MSG);
        if (online) {
            //            OutCallUtil.vibratorWhenEndCall();
        } else {
            if (FLAG_OF_NOTIFICATION_NOT_ON_LINE_MSG) {
                //                OutCallUtil.vibratorWhenEndCall();
                FLAG_OF_NOTIFICATION_NOT_ON_LINE_MSG = false;
            }
        }
    }


    private void sendNotifacation() {
        if (groupMsgSnippet.size() > 0) {
            Iterator<Entry<String, String>> it = groupMsgSnippet.entrySet()
                .iterator();
            while (it.hasNext()) {
                Map.Entry<String, String> entry = it.next();
                String gid = entry.getKey();
                String txt = entry.getValue();// 消息类型+发送者+内容简介
                boolean online = txt.startsWith(true + "");
                if (online) {
                    txt = txt.substring(4);
                } else {
                    txt = txt.substring(5);
                }

                if (forbiddenNotify(gid)) {
                    continue;
                }

                if (appOnTheDesk()) {
                    String name = groupDao.getGroupNameByGid(gid);
                    int notifyId = NotificationUtil.getGroupNotifyID(gid);
                    Intent pdintent = new Intent(context, ChatActivity.class);
                    pdintent.putExtra(ChatActivity.KEY_NOTICE_FRAME_TYPE,
                        ChatActivity.VALUE_NOTICE_FRAME_TYPE_LIST);
                    pdintent.putExtra(ChatActivity.KEY_CONVERSATION_NUBES, gid);
                    pdintent.putExtra(ChatActivity.KEY_CONVERSATION_ID, gid);
                    pdintent.putExtra(ChatActivity.KEY_CONVERSATION_SHORTNAME, name);
                    pdintent.putExtra(ChatActivity.KEY_CONVERSATION_TYPE,
                        ChatActivity.VALUE_CONVERSATION_TYPE_MULTI);
                    String titleString = context.getString(R.string.notice_information);

                    doSendNotifyMsg(online, titleString, name, notifyId + "", txt, pdintent);

                    //                    MobclickAgent.onEvent(context,UmengEventConstant.EVENT_P2P_NOTIFICATION_COUNT);
                } else {
                    doVibratorMsg(online);
                }
            }
        }
        if (msgsender.size() > 0) {
            Iterator<Entry<String, String>> it = msgsender.entrySet()
                .iterator();
            while (it.hasNext()) {
                Map.Entry<String, String> entry = it.next();
                String number = entry.getKey();

                String txt = entry.getValue();// 消息类型+内容简介
                boolean online = txt.startsWith(true + "");
                if (online) {
                    txt = txt.substring(4);
                } else {
                    txt = txt.substring(5);
                }
                if (forbiddenNotify(number)) {
                    continue;
                }

                if (appOnTheDesk()) {
                    int count = noticesDao.getNewNoticeCountByNumber(number);
                    String name = getNickName(number);
                    if (count > 0) {
                        if (count > 1) {
                            //有多条未读通知时显示未读个数
                            // txt = "[" + count + "]" + txt;

                        }
                        // if (txt.length() )
                        // else {
                        //     txt = getNickName(item.sender) + ": " + txt;
                        // }
                    }
                    Intent pdintent = new Intent(context, ChatActivity.class);
                    pdintent.putExtra(ChatActivity.KEY_NOTICE_FRAME_TYPE,
                        ChatActivity.VALUE_NOTICE_FRAME_TYPE_NUBE);
                    pdintent.putExtra(ChatActivity.KEY_CONVERSATION_NUBES,
                        number);
                    pdintent.putExtra(ChatActivity.KEY_CONVERSATION_SHORTNAME,
                        name);
                    pdintent.putExtra(ChatActivity.KEY_CONVERSATION_TYPE,
                        ChatActivity.VALUE_CONVERSATION_TYPE_SINGLE);
                    String titleString = context
                        .getString(R.string.notice_information);
                    doSendNotifyMsg(online, titleString, name, number, txt, pdintent);
                    //                    MobclickAgent.onEvent(context,
                    //                            UmengEventConstant.EVENT_P2P_NOTIFICATION_COUNT);
                } else {
                    doVibratorMsg(online);
                }
            }
        }

        if (frisender.size() > 0) {
            Intent pdintent = new Intent(context, HomeActivity.class);
            //            pdintent.putExtra(MainFragmentActivity.TAB_INDICATOR_INDEX,
            //                    MainFragmentActivity.TAB_INDEX_CONTACT);

            Iterator<Entry<String, String>> it = frisender.entrySet()
                .iterator();
            while (it.hasNext()) {
                Map.Entry<String, String> entry = it.next();
                String number = entry.getKey();
                String txt = entry.getValue();
                // 20150115 产品经理要求：好友邀请的消息提醒都采用状态栏（带声音）的形式提醒
                String name = getNickName(number);
                String titleString = context
                    .getString(R.string.notice_information);
                NotificationUtil.sendNotifacationForSmallIcoMSG(titleString,
                    name, number, txt, pdintent,
                    NotificationUtil.NOTIFACATION_STYLE_FRI, true);
            }
        }

        if (!TextUtils.isEmpty(alarmTxt)) {
            //            if (appOnTheDesk()) {
            //                // 报警消息：[3条] 图片
            //                int count = alarmDao.getNewAlarmCount();
            //                String notifyTxt = context.getString(R.string.str_alarm_prefix);
            //                if (count > 1) {
            //                    notifyTxt = notifyTxt + "[" + count
            //                            + context.getString(R.string.str_alarm_cnt_unit)
            //                            + "]" + alarmTxt;
            //                } else {
            //                    notifyTxt = notifyTxt + alarmTxt;
            //                }
            //
            //                Intent pdintent = new Intent(context,
            //                        MainFragmentActivity.class);
            //                pdintent.putExtra(MainFragmentActivity.TAB_INDICATOR_INDEX,
            //                        MainFragmentActivity.TAB_INDEX_CONTACT);
            //                pdintent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            //                String titleStr = context
            //                        .getString(R.string.alarm_notification_title);// 报警消息中,name为"可视"
            //                NotificationUtil.sendNotifacationForSmallIcoMSG(titleStr,
            //                        titleStr, "90000000", notifyTxt, pdintent,
            //                        NotificationUtil.NOTIFACATION_STYLE_ALARM,true);
            //            } else {
            //                OutCallUtil.vibratorWhenEndCall();
            //            }
        }
    }


    private String getGroupMemberName(String gid, String number) {
        GroupMemberBean bean = groupDao.queryGroupMember(gid, number);
        String name = bean != null ? bean.getDispName() : number;
        return name;
    }


    private String getNickName(String number) {
        String name = "";

        ContactFriendBean bean = contactsDao.queryFriendInfoByNube(number);
        if (bean != null) {

            ShowNameUtil.NameElement element = ShowNameUtil.getNameElement(number);
            name = ShowNameUtil.getShowName(element);

        } else {
            //			String butelPublicNo = NetPhoneApplication.getPreference()
            //					.getKeyValue(PrefType.KEY_BUTEL_PUBLIC_NO, "");
            //			if (number.equals(butelPublicNo)) {
            //				name = context.getString(R.string.str_butel_name);
            //			} else {
            ShowNameUtil.NameElement element = ShowNameUtil.getNameElement(number);
            name = ShowNameUtil.getShowName(element);
            //			}
        }
        return name;
    }


    private String splitBodyType(String msg_type) {
        String type = "";
        if (!TextUtils.isEmpty(msg_type)) {
            int index = msg_type.indexOf('_');
            if (index != -1) {
                type = msg_type.substring(index + 1);
            } else {
                type = msg_type;
            }
        }
        return type;
    }
    //
    //    @SuppressWarnings("unused")
    //    private Intent createNoticeIntent() {
    //        int newfriend = 0;
    //        int notice = 0;
    //
    //        notice = noticesDao.getNewNoticeCount();
    //        newfriend = newFriendDao.getNewFriendUnreadCount();
    //        CustomLog.d(TAG"noticeCnt=" + notice + "|newfriendCnt=" + newfriend);
    //
    //        if (notice > 0 && newfriend > 0) {
    //            String title = context.getString(R.string.title_invit_notice,
    //                    newfriend, notice);
    //            Intent pdintent = new Intent(context, MainFragmentActivity.class);
    //            pdintent.putExtra(MainFragmentActivity.TAB_INDICATOR_INDEX,
    //                    MainFragmentActivity.TAB_INDEX_CONTACT);
    //            pdintent.putExtra("title", title);
    //            return pdintent;
    //        } else if (notice > 0 && newfriend == 0) {
    //            String title = context.getString(R.string.title_notice, notice);
    //            Intent pdintent = new Intent(context, MainFragmentActivity.class);
    //            pdintent.putExtra(MainFragmentActivity.TAB_INDICATOR_INDEX,
    //                    MainFragmentActivity.TAB_INDEX_MESSAGE);
    //            pdintent.putExtra("title", title);
    //            return pdintent;
    //        } else if (notice == 0 && newfriend > 0) {
    //            String title = context.getString(R.string.title_invitfriend,
    //                    newfriend);
    //            Intent pdintent = new Intent(context, MainFragmentActivity.class);
    //            pdintent.putExtra(MainFragmentActivity.TAB_INDICATOR_INDEX,
    //                    MainFragmentActivity.TAB_INDEX_CONTACT);
    //            pdintent.putExtra("title", title);
    //            return pdintent;
    //        } else {
    //            // nothing
    //        }
    //        return null;
    //    }
    //
    //    private SyncResult doHttpGetAllMsgsIndex(String app, String type) {
    //        CustomLog.d(TAG,"doHttpGetAllMsgsIndex begin app:" + app + "|type:" + type);
    //
    //        try {
    //            HttpUtils httpUtils = new HttpUtils();
    //            // 组装参数
    //            String accesstoken = NetPhoneApplication.getPreference()
    //                    .getKeyValue(PrefType.LOGIN_ACCESSTOKENID, "");
    //            RequestParams params = new RequestParams();
    //            // 2014118 消息V1.0.3.3中新接口，可获得未读消息全文列表 （原接口getNewMsgs）
    //            params.addBodyParameter("service", "getNewDetailMsgs");
    //            params.addBodyParameter("accessToken", accesstoken);
    //            // TODO:20141115 modify 新的后台接口，当不传app 和 type 参数时，
    //            // 获取所有未读的消息
    //            // params.addQueryStringParameter("app", app);
    //            // params.addQueryStringParameter("type", type);
    //            SyncResult result = httpUtils.sendSync(HttpMethod.POST,
    //                    UrlConstant.getCommUrl(PrefType.KEY_MESSAGE_SHARE_URL),
    //                    params, CommonConstant.MSG_ACCESSTOKEN_INVALID);
    //            return result;
    //        } catch (Exception e) {
    //            LogUtil.e("Exception", e);
    //            return null;
    //        }
    //    }


    //    @SuppressWarnings("unused")
    //    private SyncResult doHttpGetMessageBody(String app, String folderid,
    //                                            String msgid) {
    //        LogUtil.begin("app:" + app + "|folderid:" + folderid + "|msgid:"
    //                + msgid);
    //        JSONObject para = new JSONObject();
    //        try {
    //            para.put("folderId", folderid);
    //            para.put("msgId", msgid);
    //
    //            HttpUtils httpUtils = new HttpUtils();
    //            // 组装参数
    //            String accesstoken = NetPhoneApplication.getPreference()
    //                    .getKeyValue(PrefType.LOGIN_ACCESSTOKENID, "");
    //            RequestParams params = new RequestParams();
    //            params.addBodyParameter("service", "showMessage");
    //            params.addBodyParameter("accessToken", accesstoken);
    //            params.addBodyParameter("app", app);
    //            params.addBodyParameter("params", para.toString());
    //            SyncResult result = httpUtils.sendSync(HttpMethod.POST,
    //                    UrlConstant.getCommUrl(PrefType.KEY_MESSAGE_SHARE_URL),
    //                    params, CommonConstant.MSG_ACCESSTOKEN_INVALID);
    //            return result;
    //        } catch (Exception e) {
    //            LogUtil.e("Exception", e);
    //            return null;
    //        }
    //    }
    //
    //    private SyncResult doHttpSetStatus(String app, String folderid,
    //                                       List<String> idlist) {
    //        LogUtil.begin("app:" + app + "|folderid:" + folderid);
    //
    //        if (idlist == null || idlist.size() == 0) {
    //            return null;
    //        }
    //        JSONArray id = new JSONArray(idlist);
    //        JSONObject object = new JSONObject();
    //        try {
    //            object.put("folderId", folderid);
    //            object.put("msgIds", id);
    //            object.put("readStatus", "true");
    //        } catch (JSONException e) {
    //            LogUtil.e("JSONException", e);
    //            return null;
    //        }
    //
    //        try {
    //            HttpUtils httpUtils = new HttpUtils();
    //            // 组装参数
    //            String accesstoken = NetPhoneApplication.getPreference()
    //                    .getKeyValue(PrefType.LOGIN_ACCESSTOKENID, "");
    //            RequestParams params = new RequestParams();
    //            params.addBodyParameter("service", "setMsgStatus");
    //            params.addBodyParameter("accessToken", accesstoken);
    //            // 20141210服务端接口变更，app项不填写
    //            // TODO:20141210 18:12 为配合线上版本，先恢复该参数传递；
    //            // 待下个星期，线上环境升级后，再移除
    //            params.addBodyParameter("app", app);
    //            params.addBodyParameter("params", object.toString());
    //            SyncResult result = httpUtils.sendSync(HttpMethod.POST,
    //                    UrlConstant.getCommUrl(PrefType.KEY_MESSAGE_SHARE_URL),
    //                    params, CommonConstant.MSG_ACCESSTOKEN_INVALID);
    //
    //            if (result != null && !result.isOK()) {
    //                publishProgress(result);
    //            }
    //            return result;
    //        } catch (Exception e) {
    //            LogUtil.e("Exception", e);
    //            return null;
    //        }
    //    }
    //
    //    @SuppressWarnings("unused")
    //    private List<SimpleMessageIndex> parseMsgIndexList(SyncResult result) {
    //        if (result != null && result.isOK()) {
    //            CustomLog.d(TAG"object string = " + result.getResult());
    //
    //            if (TextUtils.isEmpty(result.getResult())) {
    //                return null;
    //            }
    //
    //            JSONObject object = null;
    //            int status = 0;
    //            String message = "";
    //            try {
    //                object = new JSONObject(result.getResult());
    //                status = object.optInt("status");
    //                message = object.optString("message");
    //                if (status == 0) {
    //                    JSONArray msgs = object.optJSONArray("msgs");
    //                    if (msgs != null && msgs.length() > 0) {
    //                        List<SimpleMessageIndex> privatemsgs = new ArrayList<SimpleMessageIndex>();
    //                        SimpleMessageIndex item = null;
    //                        JSONObject msgobj = null;
    //                        JSONObject msgbody = null;
    //                        int length = msgs.length();
    //                        for (int i = 0; i < length; i++) {
    //                            msgobj = msgs.getJSONObject(i);
    //                            msgbody = msgobj.getJSONObject("msg");
    //                            String folderid = msgobj.optString("folderId");
    //                            String app = msgobj.optString("app");
    //                            item = new SimpleMessageIndex();
    //                            item.folderId = folderid;
    //                            item.app = app;
    //                            item.sender = msgbody.optString("sender");
    //                            item.msgId = msgbody.optString("msgId");
    //                            item.type = msgbody.optString("type");
    //                            item.hasExtendedInfo = msgbody
    //                                    .optString("hasExtendedInfo");
    //                            item.time = msgbody.optString("createTime");
    //                            item.title = msgbody.optString("title");
    //                            // TODO:不是本应用识别的类型，一律不详细解析+置已读状态
    //                            if (MSGTYPES.contains(splitBodyType(item.type))) {
    //                                privatemsgs.add(item);
    //                            } else {
    //                                item = null;
    //                                continue;
    //                            }
    //                        }
    //                        CustomLog.d(TAG"message size= " + privatemsgs.size());
    //                        return privatemsgs;
    //                    }
    //                }
    //            } catch (JSONException e) {
    //                LogUtil.e("解析消息索引失败", e);
    //            }
    //            CustomLog.d(TAG"http message = " + message);
    //        } else {
    //            publishProgress(result);
    //        }
    //        return null;
    //    }
    //
    //    private List<PrivateMessage> parseDetailMsgList(SyncResult result) {
    //
    //        if (result != null && result.isOK()) {
    //            CustomLog.d(TAG"object string = " + result.getResult());
    //
    //            if (TextUtils.isEmpty(result.getResult())) {
    //                return null;
    //            }
    //
    //            JSONObject object = null;
    //            int status = 0;
    //            String message = "";
    //            try {
    //                object = new JSONObject(result.getResult());
    //                status = object.optInt("status");
    //                message = object.optString("message");
    //                if (status == 0) {
    //                    JSONArray msgs = object.optJSONArray("msgs");
    //                    if (msgs != null && msgs.length() > 0) {
    //                        List<PrivateMessage> privatemsgs = new ArrayList<PrivateMessage>();
    //                        PrivateMessage item = null;
    //                        JSONObject msgbody = null;
    //                        int length = msgs.length();
    //                        for (int i = 0; i < length; i++) {
    //                            msgbody = msgs.getJSONObject(i);
    //                            item = new PrivateMessage();
    //                            item.folderId = msgbody.optString("folderId");
    //                            item.app = msgbody.optString("app");
    //                            item.sender = msgbody.optString("sender");
    //                            item.msgId = msgbody.optString("msgId");
    //                            item.type = msgbody.optString("type");
    //                            item.extendedInfo = msgbody
    //                                    .optString("extendedInfo");
    //                            item.time = msgbody.optString("createTime");
    //                            item.title = msgbody.optString("title");
    //                            item.body = msgbody.optString("body");
    //                            item.receivers = own;
    //                            // TODO:不是本应用识别的类型，一律不详细解析+置已读状态
    //                            if (MSGTYPES.contains(splitBodyType(item.type))) {
    //                                privatemsgs.add(item);
    //                            } else {
    //                                item = null;
    //                                continue;
    //                            }
    //                        }
    //                        CustomLog.d(TAG"message size= " + privatemsgs.size());
    //                        return privatemsgs;
    //                    }
    //                }
    //            } catch (JSONException e) {
    //                LogUtil.e("解析消息索引失败", e);
    //            }
    //            CustomLog.d(TAG"http message = " + message);
    //        } else {
    //            publishProgress(result);
    //        }
    //        return null;
    //    }
    //
    //    @SuppressWarnings("unused")
    //    private PrivateMessage pauseMessageBody(SyncResult result, String msgid) {
    //        LogUtil.begin("msgid:" + msgid);
    //        if (result != null && result.isOK()) {
    //            CustomLog.d(TAG"object string = " + result.getResult());
    //
    //            if (TextUtils.isEmpty(result.getResult())) {
    //                return null;
    //            }
    //
    //            JSONObject object = null;
    //            int status = -1;
    //            try {
    //                object = new JSONObject(result.getResult());
    //                status = object.optInt("status");
    //                if (status == 0) {
    //                    CustomLog.d(TAG"值消息ID=" + msgid + "状态为已读");
    //                    JSONObject msgbody = object.optJSONObject("msg");
    //                    PrivateMessage item = new PrivateMessage();
    //                    item.sender = msgbody.optString("sender");
    //                    item.msgId = msgid;
    //                    item.title = msgbody.optString("title");
    //                    item.type = msgbody.optString("type");
    //                    item.body = msgbody.optString("body");
    //                    item.extendedInfo = msgbody.optString("extendedInfo");
    //                    item.readStatus = msgbody.optString("readStatus");
    //                    item.time = msgbody.optString("createTime");
    //                    item.receivers = own;
    //                    return item;
    //                } else {
    //                    CustomLog.d(TAG"值消息ID=" + msgid + "状态为阅读失败" + result.getResult());
    //                }
    //
    //            } catch (JSONException e) {
    //                LogUtil.e("解析单体消息失败", e);
    //            }
    //        } else {
    //            publishProgress(result);
    //        }
    //        return null;
    //    }
    //
    //    @SuppressWarnings("unused")
    //    private static class SimpleMessageIndex {
    //        public String folderId = "";
    //        public String sender = "";
    //        public String msgId = "";
    //        public String type = "";
    //        public String time = "";
    //        public String hasExtendedInfo = "";
    //        public String title = "";
    //        public String app = "";
    //    }
    //
    public static class PrivateMessage {
        public String folderId = "";
        public String app = "";
        public String sender = "";
        public String receivers = "";
        public String msgId = "";
        public String type = "";
        public String body = "";
        public String title = "";
        public String time = "";
        public String readStatus = "";
        public String extendedInfo = "";
        public String gid = "";
        public boolean online = false;

    }


    public static interface MessageReceiverListener {
        public void onStarted();

        public void onFinished();
    }
    //
    //    public static interface MessageSendListener {
    //        public void onCompleted(boolean succ, String uuid);
    //    }
    //


    /**
     * 判断当前应用程序处于前台还是后台
     */
    public static boolean isApplicationBroughtToBackground(final Context context) {
        ActivityManager am = (ActivityManager) context
            .getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> tasks = am.getRunningTasks(1);
        if (!tasks.isEmpty()) {
            ComponentName topActivity = tasks.get(0).topActivity;
            if (!topActivity.getPackageName().equals(context.getPackageName())) {
                return true;
            }
        }
        return false;
    }

    //    public static boolean isBackground(Context context) {
    //        CustomLog.d(TAG"pkgName:" + context.getPackageName());
    //        ActivityManager activityManager = (ActivityManager) context
    //                .getSystemService(Context.ACTIVITY_SERVICE);
    //        List<RunningAppProcessInfo> appProcesses = activityManager
    //                .getRunningAppProcesses();
    //
    //        for (RunningAppProcessInfo appProcess : appProcesses) {
    //            if (appProcess.processName.equals(context.getPackageName())) {
    //                CustomLog.d(TAG"主进程 importance:" + appProcess.importance);
    //                if (appProcess.importance > RunningAppProcessInfo.IMPORTANCE_VISIBLE) {
    //                    // 大于IMPORTANCE_VISIBLE（200），就认为是后台进程
    //                    CustomLog.d(TAG"后台" + appProcess.processName);
    //                    return true;
    //                } else {
    //                    CustomLog.d(TAG"前台" + appProcess.processName);
    //                    return false;
    //                }
    //            }
    //        }
    //        return false;
    //    }


    private boolean isScreenOn(Context context) {
        PowerManager pm = (PowerManager) context
            .getSystemService(Context.POWER_SERVICE);

        boolean isScreenOn = pm.isScreenOn();// 如果为true，则表示屏幕“亮”了，否则屏幕“暗”了。
        return isScreenOn;
    }


    public final static boolean isScreenLocked(Context context) {
        android.app.KeyguardManager mKeyguardManager = (KeyguardManager) context
            .getSystemService(Context.KEYGUARD_SERVICE);
        return mKeyguardManager.inKeyguardRestrictedInputMode();
    }


    //
    //    // ==========START========IM Connect集成===============================
    //    // private List<PrivateMessage> convertImMeassage(List<ImMessage> ImMsgs){
    //    // List<PrivateMessage> privateMsgs = null;
    //    // if(ImMsgs!=null){
    //    // privateMsgs = new ArrayList<PrivateMessage>();
    //    // PrivateMessage item = null;
    //    // for(ImMessage im:ImMsgs){
    //    // item = new PrivateMessage();
    //    // item.folderId = "";
    //    // item.app = im.getApp();
    //    // item.sender = im.getSender();
    //    // item.receivers = "";
    //    // item.msgId = im.getMsgId();
    //    // item.type = im.getType();
    //    // item.body = im.getBody();
    //    // item.title = im.getTitle();
    //    // item.time = String.valueOf(im.getCreateTime());
    //    // item.readStatus = "0";
    //    // item.extendedInfo = im.getExtendedInfo();
    //    // item.gid = im.getGid();
    //    // privateMsgs.add(item);
    //    // }
    //    // }
    //    // return privateMsgs;
    //    // }
    //    // private static final Executor executor =
    //    // Executors.newSingleThreadExecutor();
    //    // public void saveImMessageThread(final List<ImMessage> ImMsgs){
    //    // Thread thread = new Thread(new Runnable() {
    //    // @Override
    //    // public void run() {
    //    // List<PrivateMessage> privateMsgs = convertImMeassage(ImMsgs);
    //    // if(privateMsgs!=null){
    //    // // 分类保存
    //    // doBatchSave(privateMsgs);
    //    // // 推送通知栏消息
    //    // sendNotifacation();
    //    // msgsender.clear();
    //    // frisender.clear();
    //    // groupMsgSnippet.clear();
    //    // if (folder_msgs_map != null) {
    //    // folder_msgs_map.clear();
    //    // folder_msgs_map = null;
    //    // }
    //    // }
    //    // }
    //    // });
    //    // if(thread!=null){
    //    // if(executor!=null){
    //    // executor.execute(thread);
    //    // }else{
    //    // thread.start();
    //    // }
    //    // }
    //    // }
    //
    private static final Executor executor = Executors
        .newSingleThreadExecutor();


    public void saveSCImMessageThread(final List<PrivateMessage> privateMsgs) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                if (privateMsgs != null) {
                    // 分类保存
                    doBatchSave(privateMsgs);
                    // 推送通知栏消息
                    sendNotifacation();
                    handleInviteMeetingCall();
                    msgsender.clear();
                    frisender.clear();
                    groupMsgSnippet.clear();
                    if (folder_msgs_map != null) {
                        folder_msgs_map.clear();
                        folder_msgs_map = null;
                    }
                }
            }
        });


    }

    // ==========END==========IM Connect集成===============================

    // ==========START =========SDK CONNECT 集成===========================


    public static class SCIMRecBean {
        public String msgType = "";
        public String title = "";
        public String sender = "";
        public String msgId = "";
        public String text = "";
        public String thumUrl = "";
        public String nikeName = "";
        public String sendTime = "";
        public String groupId = "";
        public int durationSec = 0;
        public long serverTime = 0;
        public boolean offline = false;
        public String extJson = "";
    }


    public static PrivateMessage convertSDIMMsg4GroupEvent(String eventJson) {
        PrivateMessage item = null;
        if (!TextUtils.isEmpty(eventJson)) {

            try {
                JSONObject object = new JSONObject(eventJson);
                item = new PrivateMessage();

                item.folderId = "";
                item.app = "";
                item.sender = object.optString("sender");
                item.receivers = "";
                item.msgId = object.optString("msgId");
                item.type = object.optString("type");
                item.body = object.optString("body");
                item.title = object.optString("title");
                item.time = object.optString("createTime");
                item.readStatus = "0";
                item.extendedInfo = object.optString("extendedInfo");
                item.gid = object.optString("gid");
                item.online = false;

                if (TextUtils.isEmpty(item.gid)) {
                    JSONObject bodyobject = new JSONObject(item.body);
                    item.gid = bodyobject.optString("gid");
                }
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }
        return item;
    }


    public static PrivateMessage convertSDIMMsg(SCIMRecBean ImMsgs) {
        PrivateMessage item = null;
        if (ImMsgs != null) {
            item = new PrivateMessage();
            item.folderId = "";
            item.app = "";
            item.sender = ImMsgs.sender;
            item.receivers = "";
            item.msgId = ImMsgs.msgId;
            item.type = ImMsgs.msgType;

            item.title = ImMsgs.title;
            item.time = ImMsgs.serverTime + "";
            item.readStatus = "0";

            item.gid = ImMsgs.groupId;
            item.online = isOnlineMsg(ImMsgs.extJson) && checkOnlineBydiffTime(ImMsgs);

            String[] urls = null;
            if (!TextUtils.isEmpty(ImMsgs.thumUrl)) {
                urls = ImMsgs.thumUrl.split(",");
            }
            item.body = getBody(ImMsgs.msgType, urls);
            item.extendedInfo = getExtInfo(ImMsgs.msgType, urls, ImMsgs.text,
                ImMsgs.extJson, ImMsgs.durationSec);
        }
        return item;
    }


    private static boolean isOnlineMsg(String extJson) {
        CustomLog.d(TAG, "online extJson :" + extJson);
        JSONObject object = null;
        boolean online = false;
        try {
            object = new JSONObject(extJson);
            online = object.optBoolean("online", false);
        } catch (JSONException e) {
            CustomLog.d(TAG, "online json error:" + e.getLocalizedMessage());
        }
        return online;
    }


    private static boolean checkOnlineBydiffTime(SCIMRecBean ImMsgs) {
        if (ImMsgs != null) {
            long sendtime = DateUtil.getTimeInMillis(ImMsgs.sendTime, "yyyy-MM-dd HH-mm-ss");
            long recvtime = ImMsgs.serverTime;
            long diff = recvtime - sendtime;
            boolean online = diff < 60 * 1000 ? true : false;
            CustomLog.d(TAG,
                "check by diffTime: st_s:" + ImMsgs.sendTime + "st_l: " + sendtime + " rt_l:" +
                    recvtime + " online:" + online);
            return online;
        }
        return false;
    }


    private static String getBody(String type, String[] urls) {

        CustomLog.d(TAG, "getBody type :" + type + " url: " + urls != null ? "" : urls
            .toString());
        JSONArray array = new JSONArray();
        if (urls != null && urls.length > 0) {
            int length = urls.length;
            CustomLog.d(TAG, "getBody type :" + type + "url length=" + length);
            String romoteUrl = "";
            if (TYPE_PIC_2.equals(type)) {
                if (length == 3) {
                    romoteUrl = urls[1];
                    array.put(romoteUrl);
                } else if (length > 3) {
                    int mod = length / 3;
                    for (int i = 0; i < mod; i++) {
                        romoteUrl = urls[i * 2 + 1];
                        array.put(romoteUrl);
                    }
                } else {
                    romoteUrl = urls[0];
                    array.put(romoteUrl);
                }
            } else if (TYPE_VIDEO_2.equals(type)) {
                romoteUrl = urls[0];
                array.put(romoteUrl);
            } else if (TYPE_AUDIO.equals(type)) {
                romoteUrl = urls[0];
                array.put(romoteUrl);
            } else if (TYPE_CARD.equals(type)) {
                romoteUrl = urls[0];
                array.put(romoteUrl);
            } else {
                romoteUrl = urls[0];
                array.put(romoteUrl);
            }

        } else {
            // do nothing
        }
        return array.toString();
    }


    private static String getExtInfo(String type, String[] urls, String text,
                                     String extJson, int duration) {
        //         String thumb = "";
        //         if(TYPE_PIC_2.equals(type)&&urls!=null&&urls.length>2){
        //         thumb = urls[2];
        //         }
        //         if(TYPE_VIDEO_2.equals(type)&&urls!=null&&urls.length>=2){
        //         thumb = urls[1];
        //         }

        JSONArray array = new JSONArray();
        if (urls != null && urls.length > 0) {
            int length = urls.length;
            CustomLog.d(TAG, "getExtInfo type :" + type + "url length=" + length);
            String romoteUrl = "";
            if (TYPE_PIC_2.equals(type)) {
                if (length == 3) {
                    romoteUrl = urls[2];
                    array.put(romoteUrl);
                } else if (length > 3) {
                    int mod = length / 3;
                    for (int i = 0; i < mod; i++) {
                        romoteUrl = urls[mod * 2 + i];
                        array.put(romoteUrl);
                    }
                } else {
                    romoteUrl = urls[0];
                    array.put(romoteUrl);
                }
            } else if (TYPE_VIDEO_2.equals(type)) {

                if (length >= 2) {
                    // "thumUrl":"http:\/\/210.51.168.105\/group1\/M00\/37\/E1\/wKhlFldo826AfnPeAG267VLnhJA663.mp4,http:\/\/210.51.168.105\/group1\/M00\/37\/DF\/wKhlFVdo9ACAaYlnAAAe21QK6XM598.jpg"
                    // 第一个是视频url，第二个链接才是缩略图url
                    romoteUrl = urls[1];
                    array.put(romoteUrl);
                }
            }

        }

        JSONObject object = null;
        try {
            object = new JSONObject(extJson);

            if (duration > 0) {
                if (TYPE_VIDEO_2.equals(type)) {
                    object.put("vediolen", duration);
                }
                if (TYPE_AUDIO.equals(type)) {
                    object.put("audiolen", duration);
                }
            }

            // if(!TextUtils.isEmpty(thumb)){
            // JSONArray array = new JSONArray();
            // array.put(thumb);
            // object.put("thumbUrls", array);
            // }

            if (array != null && array.length() > 0) {
                object.put("thumbUrls", array);
            }

            if (TYPE_COMMON.equals(type)) {
                JSONObject commonObject = new JSONObject(text);
                object.put("text", commonObject.optString("text"));
                object.put("subtype", commonObject.optString("subtype"));
                if (BizConstant.MSG_SUB_TYPE_FILE.equals(commonObject.optString("subtype"))) {
                    object.put("fileInfo", commonObject.optString("fileInfo"));
                } else {
                    object.put("meetingInfo", commonObject.optString("meetingInfo"));
                }
            } else {
                object.put("text", text);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return object == null ? "" : object.toString();
    }
}
