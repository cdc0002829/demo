package cn.redcdn.hvs.im.adapter;

/**
 * Created by guoyx on 2017/2/25.
 */

import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import cn.redcdn.hvs.MedicalApplication;
import cn.redcdn.hvs.R;
import cn.redcdn.hvs.contacts.contact.manager.ContactManager;
import cn.redcdn.hvs.im.IMConstant;
import cn.redcdn.hvs.im.activity.ChatActivity;
import cn.redcdn.hvs.im.activity.GroupChatDetailActivity;
import cn.redcdn.hvs.im.asyncTask.QueryPhoneNumberHelper;
import cn.redcdn.hvs.im.bean.GroupMemberBean;
import cn.redcdn.hvs.im.bean.ShowNameUtil;
import cn.redcdn.hvs.im.bean.ThreadsTempBean;
import cn.redcdn.hvs.im.bean.ThreadsTempTable;
import cn.redcdn.hvs.im.bean.WebpageBean;
import cn.redcdn.hvs.im.column.ThreadsTable;
import cn.redcdn.hvs.im.dao.GroupDao;
import cn.redcdn.hvs.im.dao.MedicalDaoImpl;
import cn.redcdn.hvs.im.dao.ThreadsDao;
import cn.redcdn.hvs.im.fileTask.FileTaskManager;
import cn.redcdn.hvs.im.manager.HtmlParseManager;
import cn.redcdn.hvs.im.preference.DaoPreference.PrefType;
import cn.redcdn.hvs.im.provider.ProviderConstant;
import cn.redcdn.hvs.im.util.IMCommonUtil;
import cn.redcdn.hvs.im.util.smileUtil.EmojiconTextView;
import cn.redcdn.hvs.im.view.BottomMenuWindow.MenuClickedListener;
import cn.redcdn.hvs.im.view.MedicalAlertDialog;
import cn.redcdn.hvs.im.view.SharePressableImageView;
import cn.redcdn.hvs.util.CommonUtil;
import cn.redcdn.hvs.util.DateUtil;
import cn.redcdn.hvs.util.NotificationUtil;
import cn.redcdn.log.CustomLog;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.butel.connectevent.utils.LogUtil;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;

public class MessageListCursorAdapter extends CursorAdapter {
    private static final String TAG = "MessageListCursorAdapter";
    private Context context;
    // Load ImageData
    private ViewHolder viewHolder;
    private LayoutInflater layoutInflater = null;
    // 屏幕宽度
    private int mScreentWidth;
    // 会话列表数据
    private Cursor threadsCursor = null;
    // 联系人名称
    // private Map<String, String> nubeNamesMap = null;
    // 刷新界面进度监听器
    private UIChangeListener uiChangeListener = null;
    // 数据变更监听
    private ThreadsContentObserver threadsObserver = null;
    private NoticesContentObserver noticesObserver = null;
    // 增加对群、群成员 数据库变更监听
    private GroupContentObserver groupObserver = null;
    private GroupMemberContentObserver groupMemberObserver = null;

    // 官方帐号（唯一）
    private static final String SYS_NUBE = "10000";
    // 自身视讯号
    private String selfNubeNumber = "";

    private GroupDao groupDao;
    private RelativeLayout relaLay_item;

    /** 2012-02-19 05:11 */
    public static final String FORMAT_YYYY_MM_DD_HH_MM = "yyyy-MM-dd HH:mm";

    private GroupMemberBean gbean;
    public MessageListCursorAdapter(Context context, Cursor c,
                                    int mScreenWidth) {
        super(context, c);
        this.context = context;
        this.layoutInflater = (LayoutInflater) context
            .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (layoutInflater == null) {
            CustomLog.d(TAG, "layoutInflater  null");
        }
        this.mScreentWidth = mScreenWidth;

        groupDao = new GroupDao(context);
        if (threadsObserver == null) {

            threadsObserver = new ThreadsContentObserver();
            context.getContentResolver().registerContentObserver(
                ProviderConstant.NETPHONE_THREADS_URI, true,
                threadsObserver);
        }
        if (noticesObserver == null) {
            noticesObserver = new NoticesContentObserver();
            context.getContentResolver()
                .registerContentObserver(
                    ProviderConstant.NETPHONE_NOTICE_URI, true,
                    noticesObserver);
        }
        if (groupObserver == null) {
            groupObserver = new GroupContentObserver();
            context.getContentResolver().registerContentObserver(
                ProviderConstant.NETPHONE_GROUP_URI, true, groupObserver);
        }
        if (groupMemberObserver == null) {
            groupMemberObserver = new GroupMemberContentObserver();
            context.getContentResolver().registerContentObserver(
                ProviderConstant.NETPHONE_GROUP_MEMBER_URI, true,
                groupMemberObserver);
        }

    }


    public void changeCursor(Cursor newCursor) {
        Cursor oldCursor = this.threadsCursor;
        this.threadsCursor = newCursor;
        // this.nubeNamesMap = nubeNamesMap;
        // this.gid_gName=gid_gNames;
        // this.gid_memberNubeNames=gid_memberNubeNames;
        this.notifyDataSetChanged();
        if (oldCursor != null) {
            oldCursor.close();
        }
    }


    public void setUIChangeListener(UIChangeListener uiChangeListener) {
        this.uiChangeListener = uiChangeListener;
    }


    public interface UIChangeListener {
        public void onRefreshProgress();
    }


    @Override
    public int getCount() {
        int count = this.threadsCursor != null ? this.threadsCursor.getCount()
                                               : 0;
        return count;
    }


    @Override
    public Object getItem(int position) {
        return position;
    }


    @Override
    public long getItemId(int position) {
        return position;
    }


    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        threadsCursor.moveToPosition(position);
        // 注意：ThreadsTempBean中的name、nickName、nube、phone，只有name是从本地nube表中查询的；
        // 其他三项是从群组成员表中查询的。--主要原因：当群中最后一条消息不是本机发送时，需要显示发送者
        // --add on 2015/6/29
        final ThreadsTempBean bean = ThreadsTempTable.pureCursor(threadsCursor);
        CustomLog.d(TAG, "getView position=" + position + "headUrl" + bean.getHeadUrl());
        if (convertView == null) {
            convertView = layoutInflater.inflate(
                R.layout.message_fragment_conversation_list_item, parent, false);
            viewHolder = createViewLine(convertView, bean);
            LayoutParams lp = viewHolder.content.getLayoutParams();
            lp.width = mScreentWidth;
            convertView.setTag(viewHolder);
        } else {
            Object viewTag = convertView.getTag();
            viewHolder = (ViewHolder) viewTag;
        }
        relaLay_item = (RelativeLayout) convertView.findViewById(R.id.reLayout);
        if (bean.getTop().equals("1")) {
            relaLay_item.setBackgroundResource(R.drawable.message_top_item);
        }else{
            relaLay_item.setBackgroundResource(R.drawable.


                    dial_item_layout_bg_press);
        }

        if (position == 0) {
            viewHolder.line_top.setVisibility(View.VISIBLE);
        } else {
            viewHolder.line_top.setVisibility(View.GONE);
        }

        final String threadsId = bean.getThreadsId();

        // 有草稿，显示草稿信息；没有草稿，显示消息摘要
        String draftTxt = "";
        final String extInfo = bean.getExtInfo();
        // 草稿信息：
        try {
            if (!TextUtils.isEmpty(extInfo)) {

                JSONObject extObject = new JSONObject(extInfo);
                draftTxt = extObject.optString("draftText");
            }

        } catch (Exception e) {
            CustomLog.e("JSONArray Exception", String.valueOf(e));
        }

        if (position == this.threadsCursor.getCount() - 1) {
            viewHolder.item_divider_bottom.setVisibility(View.GONE);
            viewHolder.item_divider_bottom1.setVisibility(View.VISIBLE);
        } else {
            viewHolder.item_divider_bottom1.setVisibility(View.GONE);
            viewHolder.item_divider_bottom.setVisibility(View.VISIBLE);
        }

        boolean repalyFlag = false;
        if (ThreadsTable.TYPE_GROUP_CHAT == bean.getThreadType()) {
            String value = MedicalApplication.getPreference().getKeyValue(
                PrefType.KEY_CHAT_REMIND_LIST, "");
            if (value != null && value.contains(bean.getRecipientIds())) {
                viewHolder.draftFlag.setText(R.string.repaly_me);
                viewHolder.draftFlag.setVisibility(View.VISIBLE);
                repalyFlag = true;
            }
        }
        if (!TextUtils.isEmpty(draftTxt) && !repalyFlag) {
            // 草稿
            viewHolder.draftFlag.setText("[草稿]");
            viewHolder.draftFlag.setVisibility(View.VISIBLE);
            if (ThreadsTable.TYPE_GROUP_CHAT == bean.getThreadType()) {
                ArrayList<String> dispNubeList = new ArrayList<String>();
                dispNubeList = CommonUtil.getDispList(draftTxt);
                for (int i = 0; i < dispNubeList.size(); i++) {
                     gbean = groupDao.queryGroupMember(
                        bean.getRecipientIds(), dispNubeList.get(i));
                    ShowNameUtil.NameElement element = ShowNameUtil.getNameElement(
                        gbean.getName(), gbean.getNickName(),
                        gbean.getPhoneNum(), gbean.getNubeNum());
                    String MName = ShowNameUtil.getShowName(element);
                    String txt = "";
                    txt = draftTxt.replace("@" + dispNubeList.get(i)
                        + IMConstant.SPECIAL_CHAR, "@" + MName
                        + IMConstant.SPECIAL_CHAR);
                    draftTxt = txt;
                }
            }
            viewHolder.msgTxt.setText(draftTxt);
            // 草稿：注意修改失败标志：GONE，以及摘要颜色：黑色
            viewHolder.failFlag.setVisibility(View.GONE);
        } else {
            if (repalyFlag) {
                viewHolder.draftFlag.setVisibility(View.VISIBLE);
            } else {
                viewHolder.draftFlag.setVisibility(View.GONE);
            }
            repalyFlag = false;
            // 消息状态：
            int status = bean.getStatus();
            // 发送出去的消息失败了才显示失败标志
            // 接收到的消息都认为是成功
            if (status == 3 && isSendNotice(bean.getSender())) {
                viewHolder.failFlag.setVisibility(View.VISIBLE);
            } else {
                viewHolder.failFlag.setVisibility(View.GONE);
            }

            if (status == 1 && isSendNotice(bean.getSender())) {
                viewHolder.chatSendBtn.setVisibility(View.VISIBLE);
            } else {
                viewHolder.chatSendBtn.setVisibility(View.GONE);
            }
            // 消息摘要：
            String msg = "";
            String member = "";
            String prefix = "";
            // TODO:add：群聊时，如果不是本人发送的，需要显示 发送者名字
            if (ThreadsTable.TYPE_GROUP_CHAT == bean.getThreadType()) {
                if (!isSendNotice(bean.getSender())) {
                    // 查询最后一个发送消息的人名字

                    // 采用ShowNameUtil中显示名字的方法---add on 2015/6/29
                    ShowNameUtil.NameElement element = ShowNameUtil.getNameElement(
                        bean.getName(), bean.getNickName(),
                        bean.getPhoneNum(), bean.getNubeNumber());

                    // String lastSendName = getGroupMemberName(gbean.getGid(),bean.getSender());

                    String showName = ShowNameUtil.getShowName(element);
                    // 如果showName为空，认为该消息是群推送消息
                    if (!TextUtils.isEmpty(showName)) {
                        prefix = showName + ":";
                    }
                }
            }

            switch (bean.getNoticeType()) {
                case FileTaskManager.NOTICE_TYPE_DESCRIPTION:
                    // 添加好友消息
                    try {
                        if (!TextUtils.isEmpty(bean.getBody())) {
                            JSONArray bodyArray = new JSONArray(bean.getBody());
                            if (bodyArray != null && bodyArray.length() > 0) {
                                JSONObject bodyObj = bodyArray.optJSONObject(0);
                                msg = bodyObj.optString("txt");
                                if (!TextUtils.isEmpty(msg) && msg.length() > 3000) {
                                    CustomLog.d(TAG, "msg.length=" + msg.length());
                                    msg = msg.substring(0, 3000);
                                    msg = msg + "...";
                                }
                            }
                        }
                    } catch (Exception e) {
                        CustomLog.e("JSONArray Exception", String.valueOf(e));
                    }
                    break;
                case FileTaskManager.NOTICE_TYPE_TXT_SEND:
                    CustomLog.d(TAG, "消息类型：txt ");
                    // 文字消息
                    try {
                        if (!TextUtils.isEmpty(bean.getBody())) {
                            JSONArray bodyArray = new JSONArray(bean.getBody());
                            String pageData = "";
                            if (bodyArray != null && bodyArray.length() > 0) {
                                JSONObject bodyObj = bodyArray.optJSONObject(0);
                                msg = bodyObj.optString("txt");
                                pageData = bodyObj.optString("webData");
                                Log.d("chencj", "selectmemeber" + member);
                                if (!TextUtils.isEmpty(msg) && msg.length() > 3000) {
                                    CustomLog.d(TAG, "msg.length=" + msg.length());
                                    msg = msg.substring(0, 3000);
                                    msg = msg + "...";
                                }
                                // 接收的消息，考虑链接的显示
                                if (!isSendNotice(bean.getSender())) {
                                    CustomLog.d(TAG, "接收的消息");
                                    if (!TextUtils.isEmpty(pageData)) {
                                        CustomLog.d("TAG", "接收的消息,且pageData不为空");
                                        // 显示链接：
                                        JSONArray pageArray = new JSONArray(
                                            pageData);
                                        List<WebpageBean> webPages = HtmlParseManager.getInstance()
                                            .convertWebpageBean(pageArray);
                                        WebpageBean wPage = webPages.get(0);
                                        if (wPage != null && wPage.isValid()) {
                                            String title = wPage.getTitle();
                                            String srcUrl = wPage.getSrcUrl();
                                            // String urlStr = srcUrl;
                                            if (!msg.contains(srcUrl)) {
                                                srcUrl = srcUrl.replace("http://",
                                                    "");
                                            }
                                            if (msg.indexOf(srcUrl) <= 0
                                                && !TextUtils.isEmpty(title)) {
                                                CustomLog.d(TAG, "以链接开头，显示msg = [链接]"
                                                    + title);
                                                msg = "[链接]" + title;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        CustomLog.e("JSONArray Exception", String.valueOf(e));
                    }

                    if (ThreadsTable.TYPE_GROUP_CHAT == bean.getThreadType()) {
                        ArrayList<String> dispNubeList = new ArrayList<String>();
                        dispNubeList = CommonUtil.getDispList(msg);
                        for (int i = 0; i < dispNubeList.size(); i++) {
                             gbean = groupDao.queryGroupMember(
                                bean.getRecipientIds(), dispNubeList.get(i));
                            if (gbean != null) {
                                ShowNameUtil.NameElement element = ShowNameUtil.getNameElement(
                                    gbean.getName(), gbean.getNickName(),
                                    gbean.getPhoneNum(), gbean.getNubeNum());
                                String MName = ShowNameUtil.getShowName(element);
                                msg = msg.replace("@" + dispNubeList.get(i)
                                    + IMConstant.SPECIAL_CHAR, "@" + MName
                                    + IMConstant.SPECIAL_CHAR);
                            }
                        }
                    }
                    break;
                case FileTaskManager.NOTICE_TYPE_PHOTO_SEND:
                    // 图片消息
                    msg = context.getResources().getString(R.string.str_pic_thread);
                    break;
                case FileTaskManager.NOTICE_TYPE_VEDIO_SEND:
                    // 视频消息
                    msg = context.getResources().getString(
                        R.string.str_video_thread);
                    break;
                case FileTaskManager.NOTICE_TYPE_VCARD_SEND:
                    // 名片消息
                    msg = context.getResources().getString(
                        R.string.str_vcard_thread);
                    break;
                case FileTaskManager.NOTICE_TYPE_AUDIO_SEND:
                    // 音频消息
                    msg = context.getResources().getString(
                        R.string.str_audio_thread);
                    break;
                case FileTaskManager.NOTICE_TYPE_RECORD:
                    try {
                        if (!TextUtils.isEmpty(bean.getBody())) {
                            JSONArray bodyArray = new JSONArray(bean.getBody());
                            if (bodyArray != null && bodyArray.length() > 0) {
                                JSONObject bodyObj = bodyArray.optJSONObject(0);
                                int type = bodyObj.optInt("calltype");
                                if (type == 0) {
                                    msg = "[语音电话]";
                                } else {
                                    msg = "[视频电话]";
                                }
                            }
                        }
                    } catch (Exception e) {
                        CustomLog.e("JSONArray Exception", String.valueOf(e));
                    }
                    break;
                case FileTaskManager.NOTICE_TYPE_MEETING_INVITE:
                    if (ThreadsTable.TYPE_GROUP_CHAT == bean.getThreadType()) {
                        ShowNameUtil.NameElement element = ShowNameUtil.getNameElement(
                            bean.getName(), bean.getNickName(),
                            bean.getPhoneNum(), bean.getNubeNumber());

                        String showName = ShowNameUtil.getShowName(element);
                        if (!isSendNotice(bean.getSender())) {
                            msg = showName + "发起了视频会议";
                        } else {
                            msg = showName + "发起了视频会议";
                        }
                    } else {
                        msg = "[视频会诊]";
                    }
                    break;
                case FileTaskManager.NOTICE_TYPE_MEETING_BOOK:
                    if (ThreadsTable.TYPE_GROUP_CHAT == bean.getThreadType()) {
                        ShowNameUtil.NameElement element = ShowNameUtil.getNameElement(
                            bean.getName(), bean.getNickName(),
                            bean.getPhoneNum(), bean.getNubeNumber());

                        String showName = ShowNameUtil.getShowName(element);
                        if (!isSendNotice(bean.getSender())) {
                            msg = showName + "发起了预约会议";
                        } else {
                            msg = showName + "发起了预约会议";
                        }
                    } else {
                        msg = "[预约会诊]";
                    }
                    break;
                case FileTaskManager.NOTICE_TYPE_FILE:
                    msg = "[文件]";
                    break;
            }
            viewHolder.msgTxt.setText(prefix + msg);
        }
        viewHolder.msgTxt.setTextColor(context.getResources().getColor(
            R.color.color_threads_black_2));
        // 设置发送时间--统一用receivedTime
        long time = bean.getSendTime();
        if (time == 0) {
            time = bean.getLastTime();
        }
        String timeStr = getDispTimestamp(time);
        viewHolder.timeTxt.setText(timeStr);

        // 显示name
        final int type;// 消息类型：单聊、群发
        final String reciever = bean.getRecipientIds();

        // 会话类型：根据ThreadTable新增字段type判断：1-单聊；2：群聊，没有群发的概念了
        int threadType = bean.getThreadType();

        // 头像点击
        // viewHolder.contactIcon.pressableTextview.setVisibility(View.VISIBLE);

        // 新需求：没有群发，增加了群聊,根据threadTable的type字段判断类型，群聊中RecipientIds为gid(群号)
        // add at 15/6/17
        if (ThreadsTable.TYPE_GROUP_CHAT == threadType) {
            // TODO:显示群聊名称：gName 或者 默认群名
            // 群聊时，reciver是gid

            String groupName = groupDao.getGroupNameByGid(bean
                .getRecipientIds());
            viewHolder.nameTxt.setText(groupName);

            // TODO:群聊时，类型与原来群发概念不同
            type = ChatActivity.VALUE_CONVERSATION_TYPE_MULTI;
        } else {
            // 单人
            // 产品要求按照ShowNameUtil中的显示规则显示名字--add on 2015/6/29
            ShowNameUtil.NameElement element = ShowNameUtil.getNameElement(reciever);
            String showName = ShowNameUtil.getShowName(element);

            // 单聊的时候需要给chatActivity传递name--add 15/6/23
            viewHolder.nameTxt.setText(showName);
            type = ChatActivity.VALUE_CONVERSATION_TYPE_SINGLE;
        }

        if (type == ChatActivity.VALUE_CONVERSATION_TYPE_SINGLE
            || type == ChatActivity.VALUE_CONVERSATION_TYPE_MULTI) {

            viewHolder.contactIcon.pressableTextview
                .setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        CustomLog.d(TAG, "点击联系人头像");

                        if (CommonUtil.isFastDoubleClick()) {
                            return;
                        }
                        if (reciever.equals(SYS_NUBE)) {
                            return;
                        }

                        if (ChatActivity.VALUE_CONVERSATION_TYPE_SINGLE == type) {
                            // 单聊，也跳到群设置页面，不存在往联系人详情跳转了---add at 15/6/18
                            String nube = reciever;
                            Intent intent = new Intent(context,
                                GroupChatDetailActivity.class);
                            intent.putExtra(
                                GroupChatDetailActivity.KEY_CHAT_TYPE,
                                GroupChatDetailActivity.VALUE_SINGLE);
                            intent.putExtra(
                                GroupChatDetailActivity.KEY_NUMBER,
                                nube);
                            context.startActivity(intent);

                        } else if (ChatActivity.VALUE_CONVERSATION_TYPE_MULTI == type) {
                            // 群聊，跳到群设置页面 --add at 15/6/18
                            String gID = reciever;// 传递群id---目前群id即会话id

                            // 如果已经被移除了，则不能再查看群设置信息
                            if (!groupDao
                                .isGroupMember(gID, selfNubeNumber)) {
                                return;
                            }
                            Intent intent = new Intent(context,
                                GroupChatDetailActivity.class);
                            intent.putExtra(
                                GroupChatDetailActivity.KEY_CHAT_TYPE,
                                GroupChatDetailActivity.VALUE_GROUP);
                            intent.putExtra(
                                GroupChatDetailActivity.KEY_NUMBER, gID);
                            context.startActivity(intent);
                        }

                    }
                });
        }

        viewHolder.content.setOnLongClickListener(new OnLongClickListener() {

            @Override
            public boolean onLongClick(View v) {
                // TODO Auto-generated method stub
                // 显示 删除 菜单
                MedicalAlertDialog menuDlg = new MedicalAlertDialog(v
                    .getContext());
                menuDlg.setShowFlag();
                menuDlg.addButtonSecond(new MenuClickedListener() {
                    @Override
                    public void onMenuClicked() {
                        // MobclickAgent
                        //     .onEvent(
                        //         context,
                        //         UmengEventConstant.EVENT_MessageList_Item_Delete);

                        new Thread(new Runnable() {

                            @Override
                            public void run() {
                                CustomLog.d(TAG, "delete threads list item <<< start...");
                                ThreadsDao dao = new ThreadsDao(context);
                                dao.deleteThread(threadsId);
                                if (ChatActivity.VALUE_CONVERSATION_TYPE_MULTI == type) {
                                    GroupDao groupDao = new GroupDao(context);
                                    String gID = reciever;
                                    if (!groupDao.isGroupMember(gID,
                                        selfNubeNumber)) {
                                        groupDao.delMembersByGid(gID);
                                        groupDao.delGroup(gID);
                                    }
                                }
                                // 去除通知栏中的消息
                                if (ChatActivity.VALUE_CONVERSATION_TYPE_SINGLE == type) {
                                    NotificationUtil
                                        .cancelNewMsgNotifacation(reciever);
                                }
                                CustomLog.d(TAG, "delete threads list item <<< end");
                            }
                        }).start();
                    }
                }, context.getString(R.string.chat_delete));
                menuDlg.show();
                return true;
            }
        });

        viewHolder.content.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (CommonUtil.isFastDoubleClick()) {
                    return;
                }

                CustomLog.d(TAG, "点击会话item,进入聊天界面");
//                try {
//                    //将该会话的消息置为已读
//                    noticesDao.updateNewStatusInConvst(threadsId);
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
                Intent i = new Intent(context, ChatActivity.class);
                i.putExtra(ChatActivity.KEY_NOTICE_FRAME_TYPE,
                    ChatActivity.VALUE_NOTICE_FRAME_TYPE_LIST);
                i.putExtra(ChatActivity.KEY_CONVERSATION_ID, threadsId);
                i.putExtra(ChatActivity.KEY_CONVERSATION_TYPE, type);
                i.putExtra(ChatActivity.KEY_CONVERSATION_NUBES, reciever);
                if (ChatActivity.VALUE_CONVERSATION_TYPE_SINGLE == type) {
                    // 产品要求按照ShowNameUtil中的显示规则显示名字--add on 2015/6/29
                    ShowNameUtil.NameElement element = ShowNameUtil.getNameElement(reciever);
                    String showName = ShowNameUtil.getShowName(element);
                    i.putExtra(ChatActivity.KEY_CONVERSATION_SHORTNAME,
                        showName);

                }
                i.putExtra(ChatActivity.KEY_CONVERSATION_EXT, extInfo);
                context.startActivity(i);

                if (reciever.equals(SYS_NUBE)) {
                    // 官方消息查看次数
                    // MobclickAgent
                    //     .onEvent(
                    //         context,
                    //            UmengEventConstant.EVENT_MessageList_ButelItem_Click);
                } else {
                    // MobclickAgent.onEvent(context,
                    //     UmengEventConstant.EVENT_SCAN_MSG);
                }
            }
        });

        CustomLog.d(TAG, bean.getRecipientIds());

        viewHolder.numTxt.setVisibility(View.GONE);

        int isNewSum = bean.getIsNews();

        ThreadsDao threadsDao = new ThreadsDao(context);

        if (bean.getDoNotDisturb().equals(ThreadsTable.DISTRUB_NO)) {
            //如果是免打扰,且有新消息，只显示小红点，
            if (isNewSum > 0) {
                viewHolder.newNoticeFlag.setVisibility(View.VISIBLE);
                viewHolder.new_notice_num.setVisibility(View.INVISIBLE);
            }else {
                viewHolder.new_notice_num.setVisibility(View.INVISIBLE);
                viewHolder.newNoticeFlag.setVisibility(View.INVISIBLE);
            }

        } else { //否则显示数量
            if (isNewSum > 0) {
                viewHolder.newNoticeFlag.setVisibility(View.INVISIBLE);
                viewHolder.new_notice_num.setVisibility(View.VISIBLE);
                if (isNewSum > 99) {
                    viewHolder.new_notice_num
                        .setBackgroundResource(R.drawable.chat_unread_count);
                    viewHolder.new_notice_num
                        .setText(R.string.main_bottom_count_99);
                } else {
                    viewHolder.new_notice_num
                        .setBackgroundResource(R.drawable.chat_unread_count);
                    viewHolder.new_notice_num.setText(String.valueOf(isNewSum));
                }
            } else {
                viewHolder.new_notice_num.setVisibility(View.INVISIBLE);
                viewHolder.newNoticeFlag.setVisibility(View.INVISIBLE);
            }
        }

        //		} else {
        //			viewHolder.new_notice_num.setVisibility(View.INVISIBLE);
        //			viewHolder.newNoticeFlag.setVisibility(View.INVISIBLE);
        //		}

        if (reciever.equals(SYS_NUBE)) {
            // 官方头像
            viewHolder.contactIcon.shareImageview
                .setImageResource(R.drawable.system_icon);


            String butelName = context.getResources().getString(
                R.string.str_butel_name);
            viewHolder.nameTxt.setText(butelName);
        }else if (ContactManager.getInstance(context).checkNubeIsCustomService(reciever)){
            viewHolder.contactIcon.shareImageview
                .setImageResource(R.drawable.contact_customservice);
            String butelName = "视频客服";
            viewHolder.nameTxt.setText(butelName);
        }
        else {
            // 头像url
            String headerUrl = bean.getHeadUrl();
            if (ThreadsTable.TYPE_SINGLE_CHAT == threadType) {
                //				if (!TextUtils.isEmpty(headerUrl)) {
                //                 单聊
                int defaultImgRes = IMCommonUtil
                    .getHeadIdBySex(new MedicalDaoImpl(
                        context)
                        .getSexByNumber(reciever));
                Glide.with(context)
                    .load(headerUrl)
                    .placeholder(defaultImgRes)
                    .error(defaultImgRes)
                    .centerCrop()
                    .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                    .crossFade()
                    .into(viewHolder.contactIcon.shareImageview);
            } else {
                // TODO:群聊头像,服务端合成
                String headUrl = groupDao.getGroupHeadUrl(bean
                    .getRecipientIds());

                // 群组头像
                Glide.with(context).load(headUrl)
                    .placeholder(R.drawable.group_icon)
                    .error(R.drawable.group_icon).centerCrop()
                    .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                    .crossFade()
                    .into(viewHolder.contactIcon.shareImageview);
                //                				}
            }

        }

        if (MedicalApplication.getPreference()
            .getKeyValue(PrefType.KEY_CHAT_DONT_DISTURB_LIST, "")
            .indexOf(";" + reciever + ";") >= 0) {
//            CustomLog.d(TAG, "已设置免打扰，设置开关状态为true");
            viewHolder.noDisturb.setVisibility(View.VISIBLE);
        } else {
//            CustomLog.d(TAG, "未设置免打扰，设置开关状态为false");
            viewHolder.noDisturb.setVisibility(View.GONE);
        }

        return convertView;
    }


    private String getGroupMemberName(String gid, String sender) {
        GroupMemberBean bean = groupDao.queryGroupMember(gid, sender);
        String name = bean != null ? bean.getDispName() : sender;
        return name;
    }


    @Override
    public void bindView(View arg0, Context arg1, Cursor arg2) {

    }


    @Override
    public View newView(Context arg0, Cursor arg1, ViewGroup arg2) {
        return null;
    }


    public ViewHolder createViewLine(View parent, ThreadsTempBean bean) {
        ViewHolder viewHolder = new ViewHolder();
        viewHolder.headView = (View) parent.findViewById(R.id.head_ray);
        viewHolder.line_top = (View) parent.findViewById(R.id.item_divider_top);
        viewHolder.contactIcon = (SharePressableImageView) parent
            .findViewById(R.id.contact_icon);
        viewHolder.newNoticeFlag = (ImageView) parent
            .findViewById(R.id.new_msg_flag);
        viewHolder.new_notice_num = (TextView) parent.findViewById(R.id.new_notice_num);
        viewHolder.nameTxt = (TextView) parent.findViewById(R.id.name_txt);
        viewHolder.numTxt = (TextView) parent.findViewById(R.id.recv_num_field);
        viewHolder.failFlag = (ImageView) parent.findViewById(R.id.failed_flag);
        viewHolder.draftFlag = (TextView) parent.findViewById(R.id.draft_field);
        viewHolder.msgTxt = (EmojiconTextView) parent.findViewById(R.id.msg_txt);

        viewHolder.timeTxt = (TextView) parent.findViewById(R.id.time_txt);
        viewHolder.chatSendBtn = (ImageView) parent
            .findViewById(R.id.chat_send_btn);
        // viewHolder.deleteBtn = (Button) parent
        // .findViewById(R.id.delete_msg_item);
        viewHolder.content = (View) parent.findViewById(R.id.reLayout);
        viewHolder.noDisturb = (ImageView) parent.findViewById(R.id.no_disturb);
        viewHolder.item_divider_bottom = (View) parent
            .findViewById(R.id.item_divider_bottom);
        viewHolder.item_divider_bottom1 = (View) parent
            .findViewById(R.id.item_divider_bottom_1);
        return viewHolder;
    }


    private class ThreadsContentObserver extends ContentObserver {

        public ThreadsContentObserver() {
            super(new Handler());
        }


        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);

            LogUtil.d("t_threads 动态消息数据库数据发生变更");

            // 刷新界面显示
            if (uiChangeListener != null) {
                uiChangeListener.onRefreshProgress();
            }
        }
    }


    private class GroupMemberContentObserver extends ContentObserver {

        public GroupMemberContentObserver() {
            super(new Handler());
        }


        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);

            LogUtil.d("t_multi_chat_users 群成员数据库数据发生变更");

            // 刷新界面显示
            if (uiChangeListener != null) {
                uiChangeListener.onRefreshProgress();
            }
        }
    }


    private class GroupContentObserver extends ContentObserver {

        public GroupContentObserver() {
            super(new Handler());
        }


        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);

            LogUtil.d("t_multi_chat_groups 群聊数据库数据发生变更");

            // 刷新界面显示
            if (uiChangeListener != null) {
                uiChangeListener.onRefreshProgress();
            }
        }
    }


    private class NoticesContentObserver extends ContentObserver {

        public NoticesContentObserver() {
            super(new Handler());
        }


        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);

            CustomLog.d(TAG, "t_notices 动态消息数据库数据发生变更");

            // 刷新界面显示
            if (uiChangeListener != null) {
                uiChangeListener.onRefreshProgress();
            }
        }
    }


    //会话列表 View
    private class ViewHolder {
        View headView;
        View line_top;
        //		HorizontalScrollView hSView;
        //		View action;
        View content;
        SharePressableImageView contactIcon;
        ImageView newNoticeFlag;
        TextView nameTxt;
        TextView numTxt;
        ImageView failFlag;
        ImageView chatSendBtn;
        TextView draftFlag;
        //		TextView msgTxt;
        //TODO:可以识别表情的TextView
        EmojiconTextView msgTxt;
        TextView timeTxt;
        ImageView noDisturb;
        View item_divider_bottom;
        View item_divider_bottom1;
        TextView new_notice_num;
        //		Button deleteBtn;
    }


    /**
     * 时间显示样式
     */
    private String getDispTimestamp(long dbTime) {

        String dateStr = DateUtil.formatMs2String(dbTime,
            DateUtil.FORMAT_YYYY_MM_DD_HH_MM);

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(dbTime);

        Calendar nowCal = Calendar.getInstance();

        // 此处优先级：跨年？年-月-日，当天？时-分，跨月？月-日，昨天？昨天，else:月-日
        if (cal.get(Calendar.YEAR) != nowCal.get(Calendar.YEAR)) {
            // 跨年了，此处应显示 年、月、日
            return dateStr.substring(0, 10);
        } else {
            // realDateIntervalDay函数，求的是日期间隔，可能之前，可能之后
            int dayInterval = DateUtil.realDateIntervalDay(cal.getTime(),
                nowCal.getTime());
            if (dayInterval == 0) {
                // 当天（14:11）
                return dateStr.substring(11, 16);
            } else if (cal.get(Calendar.MONTH) != nowCal.get(Calendar.MONTH)) {
                // 跨月了，显示：月-日
                return dateStr.substring(5, 10);
            } else if (dayInterval == 1) {
                if (cal.before(nowCal)) {
                    // 不跨月的情况下，昨天按“昨天”显示，否则按月-日显示
                    return context.getResources().getString(
                        R.string.date_yesterday);
                } else {
                    // 明天，显示“月-日”
                    return dateStr.substring(5, 10);
                }
            } else {
                // 本月中其他日子，显示“月-日”
                return dateStr.substring(5, 10);
            }
        }
    }


    private String getName(Map<String, String> nubeNamesMap, String nubeNum) {
        String name = "";
        if (nubeNamesMap != null) {
            name = nubeNamesMap.get(nubeNum);
        }

        // add by zzwang : 添加弱化视讯号需求
        if (TextUtils.isEmpty(name)) {
            name = QueryPhoneNumberHelper.getPhoneNumberByNubeNumer(nubeNum,
                false, null);
            if (TextUtils.isEmpty(name)) {
                return nubeNum;
            }
        }
        return name;
    }


    public void onDestoryView() {
        if (noticesObserver != null) {
            context.getContentResolver().unregisterContentObserver(
                noticesObserver);
        }
        if (threadsCursor != null) {
            context.getContentResolver().unregisterContentObserver(
                threadsObserver);
        }
        if (groupObserver != null) {
            context.getContentResolver().unregisterContentObserver(
                groupObserver);
        }
        if (groupMemberObserver != null) {
            context.getContentResolver().unregisterContentObserver(
                groupMemberObserver);
        }
        if (threadsCursor != null) {
            try {
                threadsCursor.close();
                threadsCursor = null;
            } catch (Exception e) {
                LogUtil.e("noticeCursor.close()", e);
            }

        }
    }


    /**
     * 是否是本端发送的消息
     */
    public boolean isSendNotice(String noticeSender) {
        if (selfNubeNumber.equals(noticeSender)) {
            return true;
        } else {
            return false;
        }
    }


    public void setSelfNubeNumber(String nubeNumber) {
        this.selfNubeNumber = nubeNumber;
    }

}
