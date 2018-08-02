package cn.redcdn.hvs.im.adapter;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.MergeCursor;
import android.graphics.drawable.AnimationDrawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CursorAdapter;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import cn.redcdn.commonutil.NetConnectHelper;
import cn.redcdn.hvs.AccountManager;
import cn.redcdn.hvs.MedicalApplication;
import cn.redcdn.hvs.R;
import cn.redcdn.hvs.contacts.contact.ContactCardActivity;
import cn.redcdn.hvs.im.IMConstant;
import cn.redcdn.hvs.im.activity.ChatActivity;
import cn.redcdn.hvs.im.activity.EmbedWebViewActivity;
import cn.redcdn.hvs.im.activity.PlayVideoActivity;
import cn.redcdn.hvs.im.activity.RecordedVideoActivity;
import cn.redcdn.hvs.im.activity.ViewPhotosActivity;
import cn.redcdn.hvs.im.bean.BookMeetingExInfo;
import cn.redcdn.hvs.im.bean.ButelFileInfo;
import cn.redcdn.hvs.im.bean.FileTaskBean;
import cn.redcdn.hvs.im.bean.GroupMemberBean;
import cn.redcdn.hvs.im.bean.NoticesBean;
import cn.redcdn.hvs.im.bean.PhotoBean;
import cn.redcdn.hvs.im.bean.ShowNameUtil;
import cn.redcdn.hvs.im.bean.ShowNameUtil.NameElement;
import cn.redcdn.hvs.im.bean.WebpageBean;
import cn.redcdn.hvs.im.collection.CollectionFileManager;
import cn.redcdn.hvs.im.column.NoticesTable;
import cn.redcdn.hvs.im.common.CommonWaitDialog;
import cn.redcdn.hvs.im.dao.GroupDao;
import cn.redcdn.hvs.im.dao.NoticesDao;
import cn.redcdn.hvs.im.fileTask.ChangeUIInterface;
import cn.redcdn.hvs.im.fileTask.FileTaskManager;
import cn.redcdn.hvs.im.manager.CollectionManager;
import cn.redcdn.hvs.im.manager.HtmlParseManager;
import cn.redcdn.hvs.im.manager.HtmlParseManager.OnClickBack;
import cn.redcdn.hvs.im.meeting.MeetingBroadcastSender;
import cn.redcdn.hvs.im.util.AudioManagerHelper;
import cn.redcdn.hvs.im.util.IMCommonUtil;
import cn.redcdn.hvs.im.util.MediaFile;
import cn.redcdn.hvs.im.util.smileUtil.EmojiconTextView;
import cn.redcdn.hvs.im.view.BottomMenuWindow;
import cn.redcdn.hvs.im.view.BottomMenuWindow.MenuClickedListener;
import cn.redcdn.hvs.im.view.MedicalAlertDialog;
import cn.redcdn.hvs.im.view.SharePressableImageView;
import cn.redcdn.hvs.im.view.XCRoundImageViewByXfermode;
import cn.redcdn.hvs.meeting.activity.ReserveSuccessActivity;
import cn.redcdn.hvs.meeting.meetingManage.MedicalMeetingManage;
import cn.redcdn.hvs.util.CommonUtil;
import cn.redcdn.hvs.util.CustomToast;
import cn.redcdn.hvs.util.DateUtil;
import cn.redcdn.log.CustomLog;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.butel.connectevent.utils.LogUtil;
import java.io.File;
import java.lang.ref.WeakReference;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import static android.view.View.GONE;
import static cn.redcdn.hvs.util.CommonUtil.getString;

/**
 * Desc
 * Created by wangkai on 2017/2/24.
 */

public class ChatListAdapter extends CursorAdapter {
    public static final String ACTIVITY_FLAG = "ChatListAdapter";

    /**
     * 1.type的值必须从0开始，否侧会报ArrayIndexOutOfBoundsException 2.用于多布局
     */
    private static final int NOTICE_TYPE_RECV_TXT = 0;
    private static final int NOTICE_TYPE_SENT_TXT = 1;
    private static final int NOTICE_TYPE_RECV_IMAGE = 2;
    private static final int NOTICE_TYPE_SENT_IMAGE = 3;
    private static final int NOTICE_TYPE_RECV_VIDEO = 4;
    private static final int NOTICE_TYPE_SENT_VIDEO = 5;
    private static final int NOTICE_TYPE_RECV_VOICE = 6;
    private static final int NOTICE_TYPE_SENT_VOICE = 7;
    private static final int NOTICE_TYPE_RECV_VCARD = 8;
    private static final int NOTICE_TYPE_SENT_VCARD = 9;
    private static final int NOTICE_TYPE_SENT_MEETING = 10;
    private static final int NOTICE_TYPE_RECV_MEETING = 11;
    //    private static final int NOTICE_TYPE_SENT_FILE = 12;
    //    private static final int NOTICE_TYPE_RECV_FILE = 13;

    // 上传下载任务管理器
    private FileTaskManager fileTaskMgr = null;

    private LayoutInflater layoutInflater = null;
    private CallbackInterface callbackIf = null;
    private NoticesDao noticeDao = null;
    private GroupDao groupDao = null;
    private Context mContext = null;

    // 刷新界面上传下载进度任务ID
    private List<String> changUIProgressTaskIds = new ArrayList<String>();
    // 上传文件进度值
    private Map<String, Float> mTaskFileProgressMap = new HashMap<String, Float>();
    // 当前收听的音频的消息ID
    private String curPlayingAuMsgId = "";
    // 当前收听的音频的View
    private WeakReference<View> currentPlayVoiceView = null;
    // 音频播放
    private MediaPlayer mMediaPlayer = null;

    // 自身视讯号
    private String selfNubeNumber = "";
    // 数据游标
    private Cursor dataCursor = null;
    // 图片最大尺寸
    private int picMaxSize = 450;
    // 图片最小尺寸
    private int picMinSize = 256;
    // 图片默认尺寸
    private int picDefaultSize = 150;
    // 音频短尺寸
    private int audioWidthS = 100;
    // 音频长尺寸
    private int audioWidthL = 300;
    // 消息类型
    private int noticeType;
    // 群id
    private String groupId = "";
    private String headUrl = "";
    private int userDefaultHeadUrl;
    private String targetNumber = "";
    private String targetShortName = "";
    private String butelPubNubeNum = "10000";
    private static String dateday = "";

    // 是否是多选模式
    private boolean bMultiCheckMode = false;

    // 加载框视图
    private CommonWaitDialog mWaitDialog;

    //当前是否在播放音频
    private boolean isPlayingAuMsg = false;

    //音频消息播放状态监听器
    AudioMsgStateListener audioMsgStateListener;

    private SharedPreferences voiceMsgSettings;


    private BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (MeetingBroadcastSender.getOnJoinMeetingActionName(context)
                .equals(action)) {
                hideLoadingMsg();
            }
        }
    };


    private void initReceive() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(MeetingBroadcastSender
            .getOnJoinMeetingActionName(mContext));
        mContext.registerReceiver(mReceiver, filter);
    }


    private void showLoadingMsg() {
        if (null == mWaitDialog && mContext != null) {
            mWaitDialog = new CommonWaitDialog(mContext,
                mContext.getString(R.string.hard_loading));
        }
    }


    private void hideLoadingMsg() {
        if (null != mWaitDialog) {
            mWaitDialog.clearAnimation();
            mWaitDialog = null;
        }
    }


    public void onDestroy() {
        if (dataCursor != null) {
            dataCursor.close();
            dataCursor = null;
        }
        if (mReceiver != null) {
            mContext.unregisterReceiver(mReceiver);
            mReceiver = null;
        }
    }


    public void setSelfNubeNumber(String nubeNumber) {
        this.selfNubeNumber = nubeNumber;
    }


    public void setCallbackInterface(CallbackInterface cbIf) {
        this.callbackIf = cbIf;
    }


    public void setMultiCheckMode(boolean multicheck) {
        this.bMultiCheckMode = multicheck;
        this.notifyDataSetChanged();
    }


    public boolean isMultiCheckMode() {
        return this.bMultiCheckMode;
    }


    public void changeCursor(Cursor newCursor) {
        LogUtil.d("changeCursor");
        Cursor oldCursor = this.dataCursor;
        this.dataCursor = newCursor;
        this.notifyDataSetChanged();
        if (oldCursor != null) {
            oldCursor.close();
        }
    }


    /**
     * 将上一页数据的cursor合并到原cursor
     *
     * @param pageCursor 上一页数据cursor
     */
    public void mergeLastPageCursor(Cursor pageCursor) {
        if (pageCursor == null) {
            return;
        }
        if (dataCursor == null) {
            dataCursor = pageCursor;
        } else {
            Cursor[] cursors = new Cursor[2];
            cursors[0] = pageCursor;
            cursors[1] = dataCursor;
            dataCursor = new MergeCursor(cursors);
        }

        this.notifyDataSetChanged();
    }


    /**
     * 清空数据
     */
    public void clearData() {
        Cursor oldCursor = this.dataCursor;
        this.dataCursor = null;
        this.notifyDataSetChanged();
        if (oldCursor != null) {
            oldCursor.close();
        }
    }


    public void setNoticeType(int type, String groupId) {
        this.noticeType = type;
        this.groupId = groupId;
        this.targetNumber = groupId;
    }


    /**
     * @param context
     * @param c
     */
    public ChatListAdapter(Context context, Cursor c, NoticesDao noticeDao,
                           String number, String targetShortName) {
        super(context, c, FLAG_REGISTER_CONTENT_OBSERVER);
        this.mContext = context;
        this.dataCursor = c;
        this.targetNumber = number;
        this.targetShortName = targetShortName;
        this.layoutInflater = (LayoutInflater) context
            .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.fileTaskMgr = MedicalApplication.getFileTaskManager();
        this.noticeDao = noticeDao;
        this.groupDao = new GroupDao(mContext);
        this.headUrl = AccountManager.getInstance(MedicalApplication.getContext())
            .getAccountInfo().headThumUrl;
        this.userDefaultHeadUrl = IMCommonUtil.getHeadIdBySex("男");
        //        butelPubNubeNum = NetPhoneApplication.getPreference().getKeyValue(
        //                PrefType.KEY_BUTEL_PUBLIC_NO, "");
        picMaxSize = context.getResources().getDimensionPixelSize(
            R.dimen.x280);
        picMinSize = context.getResources().getDimensionPixelSize(
            R.dimen.x156);
        picDefaultSize = context.getResources().getDimensionPixelSize(
            R.dimen.chat_video_size);

        int screenWidth = IMCommonUtil.getScreenWidth(context);
        audioWidthS = screenWidth / 4;
        audioWidthL = screenWidth * 2 / 3;
        initReceive();
    }


    @Override
    public void bindView(View arg0, Context arg1, Cursor arg2) {
    }


    @Override
    public View newView(Context arg0, Cursor arg1, ViewGroup arg2) {
        return null;
    }


    public int getCount() {
        return dataCursor == null ? 0 : dataCursor.getCount();
    }


    public Cursor getItem(int position) {
        if (dataCursor != null) {
            dataCursor.moveToPosition(position);
            return dataCursor;
        } else {
            return null;
        }
    }


    public long getItemId(int position) {
        return position;
    }


    public int getViewTypeCount() {
        return 12;
    }


    /**
     * 获取item类型
     */
    public int getItemViewType(int position) {
        Cursor cursor = getItem(position);
        int type = (Integer) getCursorDataByCol(cursor,
            NoticesTable.NOTICE_COLUMN_TYPE, CURSOR_COL_TYPE_INT);
        String sender = (String) getCursorDataByCol(cursor,
            NoticesTable.NOTICE_COLUMN_SENDER, CURSOR_COL_TYPE_STRING);
        switch (type) {
            case FileTaskManager.NOTICE_TYPE_TXT_SEND:
                return isSendNotice(sender) ? NOTICE_TYPE_SENT_TXT
                                            : NOTICE_TYPE_RECV_TXT;
            case FileTaskManager.NOTICE_TYPE_PHOTO_SEND:
                return isSendNotice(sender) ? NOTICE_TYPE_SENT_IMAGE
                                            : NOTICE_TYPE_RECV_IMAGE;
            case FileTaskManager.NOTICE_TYPE_VEDIO_SEND:
                return isSendNotice(sender) ? NOTICE_TYPE_SENT_VIDEO
                                            : NOTICE_TYPE_RECV_VIDEO;
            case FileTaskManager.NOTICE_TYPE_VCARD_SEND:
                return isSendNotice(sender) ? NOTICE_TYPE_SENT_VCARD
                                            : NOTICE_TYPE_RECV_VCARD;
            case FileTaskManager.NOTICE_TYPE_AUDIO_SEND:
                return isSendNotice(sender) ? NOTICE_TYPE_SENT_VOICE
                                            : NOTICE_TYPE_RECV_VOICE;
            case FileTaskManager.NOTICE_TYPE_MEETING_INVITE:

            case FileTaskManager.NOTICE_TYPE_MEETING_BOOK:
                return isSendNotice(sender) ? NOTICE_TYPE_SENT_MEETING
                                            : NOTICE_TYPE_RECV_MEETING;
            default:
                // invalid
                return -1;
        }
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final NoticesBean bean = NoticesTable.pureChatCursor(getItem(position),
            noticeType);
        LogUtil.d("getView:position=" + position + "type:" + bean.getType()
            + bean.getBody());
        final ViewHolder holder;
        if (convertView == null) {
            holder = new ViewHolder();
            convertView = createViewByNotice(bean);
            switch (bean.getType()) {
                case FileTaskManager.NOTICE_TYPE_TXT_SEND:
                    holder.noticeTxt = (EmojiconTextView) convertView
                        .findViewById(R.id.tv_chatcontent);
                    // holder.noticeTxt.setEmojiconSize(mContext.getResources().getDimensionPixelSize(R.dimen.notice_item_emoji_size));
                    holder.runningPb = (ProgressBar) convertView
                        .findViewById(R.id.msg_running_pb);
                    // 链接布局：在handleTextMessage中判断，如果不需要，就隐藏
                    // 注意：layout在接收消息时！=null，对与发送消息，其xml中没有lauout。取出来为null
                    // 因此要保证layout出现的场合必须是接收的消息
                    holder.layout = (LinearLayout) convertView
                        .findViewById(R.id.message_recv);
                    break;
                case FileTaskManager.NOTICE_TYPE_VEDIO_SEND:
                    // 视频消息
                    holder.durationTxt = (TextView) convertView
                        .findViewById(R.id.duration_txt);
                case FileTaskManager.NOTICE_TYPE_PHOTO_SEND:
                    // 图片
                    holder.imgLayout = (FrameLayout) convertView
                        .findViewById(R.id.img_frame);
                    holder.imgIv = (ImageView) convertView
                        .findViewById(R.id.img_iv);
                    holder.progressLine = (LinearLayout) convertView
                        .findViewById(R.id.progress_line);
                    holder.progressTxt = (TextView) convertView
                        .findViewById(R.id.progress_txt);
                    holder.imgFrameIv = (ImageView) convertView
                        .findViewById(R.id.img_frame_iv);
                    holder.imgIvMask = (ImageView) convertView
                        .findViewById(R.id.img_iv_mask);
                    break;
                case FileTaskManager.NOTICE_TYPE_VCARD_SEND:
                    // 名片消息
                    holder.vcardBg = (RelativeLayout) convertView
                        .findViewById(R.id.vcard_bg);
                    holder.vcardHeadIv = (ImageView) convertView
                        .findViewById(R.id.vcard_head_iv);
                    holder.vcardNameTxt = (TextView) convertView
                        .findViewById(R.id.vcard_name_txt);
                    holder.vcardNubeTxt = (TextView) convertView
                        .findViewById(R.id.vcard_nube_txt);
                    holder.runningPb = (ProgressBar) convertView
                        .findViewById(R.id.msg_running_pb);
                    break;
                case FileTaskManager.NOTICE_TYPE_AUDIO_SEND:
                    // 音频消息
                    holder.audioBg = (RelativeLayout) convertView
                        .findViewById(R.id.audio_bg);
                    holder.audioIcon = (ImageView) convertView
                        .findViewById(R.id.audio_icon);
                    holder.audioDuration = (TextView) convertView
                        .findViewById(R.id.audio_duration);
                    holder.readStatus = (ImageView) convertView
                        .findViewById(R.id.read_status);
                    holder.runningPb = (ProgressBar) convertView
                        .findViewById(R.id.msg_running_pb);
                    break;
                case FileTaskManager.NOTICE_TYPE_DESCRIPTION:
                    holder.noticeAddTxt = (TextView) convertView
                        .findViewById(R.id.tv_addcontent);
                    break;
                case FileTaskManager.NOTICE_TYPE_RECORD:
                    holder.noticeTxt = (EmojiconTextView) convertView
                        .findViewById(R.id.tv_chatcontent);
                    holder.runningPb = (ProgressBar) convertView
                        .findViewById(R.id.msg_running_pb);
                    holder.layout = (LinearLayout) convertView
                        .findViewById(R.id.message_recv);
                    break;
                case FileTaskManager.NOTICE_TYPE_FILE:
                    holder.vcardBg = (RelativeLayout) convertView
                        .findViewById(R.id.vcard_bg);
                    holder.vcardNameTxt = (TextView) convertView
                        .findViewById(R.id.vcard_name_txt);
                    holder.runningPb = (ProgressBar) convertView
                        .findViewById(R.id.msg_running_pb);
                    holder.vcardNubeTxt = (TextView) convertView
                        .findViewById(R.id.vcard_nube_txt);
                    holder.vcardDetail = (TextView) convertView
                        .findViewById(R.id.meeting_detail);
                    holder.vcardHeadIv = (ImageView) convertView
                        .findViewById(R.id.vcard_head_iv);// icon两个类型显示不一样
                    holder.mProgressBar = (ProgressBar) convertView
                        .findViewById(R.id.file_upload_progress);
                    break;
                case FileTaskManager.NOTICE_TYPE_MEETING_BOOK:
                case FileTaskManager.NOTICE_TYPE_MEETING_INVITE:
                    holder.vcardBg = (RelativeLayout) convertView
                        .findViewById(R.id.vcard_bg);
                    holder.vcardNameTxt = (TextView) convertView
                        .findViewById(R.id.vcard_name_txt);
                    holder.vcardDetail = (TextView) convertView
                        .findViewById(R.id.meeting_detail);
                    holder.runningPb = (ProgressBar) convertView
                        .findViewById(R.id.msg_running_pb);
                    holder.vcardNubeTxt = (TextView) convertView
                        .findViewById(R.id.vcard_nube_txt);
                    holder.vcardHeadIv = (ImageView) convertView
                        .findViewById(R.id.vcard_head_iv);// icon两个类型显示不一样
                    break;
            }

            holder.timeTv = (TextView) convertView.findViewById(R.id.timestamp);
            holder.retryBtn = (ImageButton) convertView
                .findViewById(R.id.retry_btn);
            holder.contactIcon = (SharePressableImageView) convertView
                .findViewById(R.id.contact_icon_notice);
            holder.contactName = (TextView) convertView
                .findViewById(R.id.user_name);

            //多选操作
            holder.checkLayout = (RelativeLayout) convertView
                .findViewById(R.id.select_layout);
            holder.checkbox = (CheckBox) convertView
                .findViewById(R.id.linkman_select);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        switch (bean.getType()) {
            case FileTaskManager.NOTICE_TYPE_TXT_SEND:
                handleTextMessage(bean, holder);
                break;
            case FileTaskManager.NOTICE_TYPE_PHOTO_SEND:
                handleImageMessage(bean, holder, false);
                break;
            case FileTaskManager.NOTICE_TYPE_VEDIO_SEND:
                handleImageMessage(bean, holder, true);
                break;
            case FileTaskManager.NOTICE_TYPE_VCARD_SEND:
                handleVcardMessage(bean, holder);
                break;
            case FileTaskManager.NOTICE_TYPE_AUDIO_SEND:
                handleAudioMessage(bean, holder);
                break;
            case FileTaskManager.NOTICE_TYPE_RECORD:
                handleCallRecord(bean, holder);
                break;
            case FileTaskManager.NOTICE_TYPE_MEETING_INVITE:
                handleInviteMeeting(bean, holder);
                break;
            case FileTaskManager.NOTICE_TYPE_MEETING_BOOK:
                handleBookMeeting(bean, holder);
                break;
            case FileTaskManager.NOTICE_TYPE_FILE:
                handleFileMsg(bean, holder);
                break;
            case FileTaskManager.NOTICE_TYPE_DESCRIPTION:
                handleDescription(bean, holder);
        }
        if (position == 0) {
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
            layoutParams.setMargins(0, 20, 0, 10);
            holder.timeTv.setLayoutParams(layoutParams);
        }
        // 重发按钮
        holder.retryBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (CommonUtil.isFastDoubleClick()) {
                    return;
                }

                if (bean.getStatus() == FileTaskManager.TASK_STATUS_FAIL) {
                    // 失败状态，才有重试按钮
                    startTask(bean);
                }
            }
        });

        // 时间
        if (position == 0) {
            // 第一个消息显示
            holder.timeTv.setVisibility(View.VISIBLE);
            if (bean.getSendTime() == 1 || bean.getSendTime() == 0) {
                holder.timeTv.setText(getDispTimestamp(bean.getReceivedTime()));
                CustomLog.d(ACTIVITY_FLAG, "IM 消息接收时间" + bean.getReceivedTime());
            } else {
                holder.timeTv.setText(getDispTimestamp(bean.getSendTime()));
                CustomLog.d(ACTIVITY_FLAG, "IM 消息发送时间" + bean.getSendTime());
            }
        } else {
            // 两条消息时间离得如果稍长，显示时间
            long lastTime = (Long) getCursorDataByCol(getItem(position - 1),
                NoticesTable.NOTICE_COLUMN_SENDTIME, CURSOR_COL_TYPE_LONG);
            CustomLog.d(ACTIVITY_FLAG, lastTime + "");
            if (lastTime == 1) {
                lastTime = (Long) getCursorDataByCol(getItem(position - 1),
                    NoticesTable.NOTICE_COLUMN_RECEIVEDTIME,
                    CURSOR_COL_TYPE_LONG);
            }
            if (isCloseEnough(lastTime, bean.getSendTime())) {
                holder.timeTv.setVisibility(GONE);
            } else {
                holder.timeTv.setVisibility(View.VISIBLE);
                holder.timeTv.setText(getDispTimestamp(bean.getSendTime()));
            }
        }
        if (holder.checkLayout != null) {
            if (bMultiCheckMode) {
                holder.checkLayout.setVisibility(View.VISIBLE);
                holder.checkbox.setOnCheckedChangeListener(
                    new CompoundButton.OnCheckedChangeListener() {

                        @Override
                        public void onCheckedChanged(CompoundButton buttonView,
                                                     boolean isChecked) {
                            // TODO Auto-generated method stub
                            onCheckBoxChecked(bean, isChecked);
                        }
                    });
                if (hasChecked(bean.getId())) {
                    holder.checkbox.setChecked(true);
                } else {
                    holder.checkbox.setChecked(false);
                }
            } else {
                holder.checkLayout.setVisibility(GONE);
            }
        }
        return convertView;
    }


    private void startTask(NoticesBean bean) {

        if (NetConnectHelper.NETWORKTYPE_INVALID == NetConnectHelper.getNetWorkType(mContext)) {
            showToast(R.string.no_network_connect);
            return;
        }

        if (checkDataValid(bean.getType(), bean.getBody())) {
            // 重新开始分享
            fileTaskMgr.addTask(bean.getId(), null);
        }
    }


    /**
     * 判断数据是否有效
     */
    private boolean checkDataValid(int type, String body) {
        // remoteUrl为空的场合（还未上传成功），图片、视频、音频的数据，需判断源文件是否存在，不存在，则提示源文件不存在
        if (type == FileTaskManager.NOTICE_TYPE_PHOTO_SEND
            || type == FileTaskManager.NOTICE_TYPE_VEDIO_SEND
            || type == FileTaskManager.NOTICE_TYPE_AUDIO_SEND) {
            // 发送图片、视频、音频
            ButelFileInfo fileInfo = ButelFileInfo.parseJsonStr(body, false);
            if (TextUtils.isEmpty(fileInfo.getRemoteUrl())
                && !isValidFilePath(fileInfo.getLocalPath())) {
                // 无数据源，无法发送
                String fileTxt = mContext.getString(R.string.toast_no_pic);
                if (type == FileTaskManager.NOTICE_TYPE_VEDIO_SEND) {
                    fileTxt = mContext.getString(R.string.toast_no_video);
                } else if (type == FileTaskManager.NOTICE_TYPE_AUDIO_SEND) {
                    fileTxt = mContext.getString(R.string.toast_no_aud);
                }
                showToast(fileTxt);
                return false;
            }
        }

        return true;
    }


    private View createViewByNotice(NoticesBean bean) {
        switch (bean.getType()) {
            case FileTaskManager.NOTICE_TYPE_TXT_SEND:// 文字消息
                return isSendNotice(bean.getSender()) ? layoutInflater.inflate(
                    R.layout.chat_row_sent_message, null) : layoutInflater
                           .inflate(R.layout.chat_row_recv_message, null);
            case FileTaskManager.NOTICE_TYPE_PHOTO_SEND:// 图片消息
                return isSendNotice(bean.getSender()) ? layoutInflater.inflate(
                    R.layout.chat_row_sent_picture, null) : layoutInflater
                           .inflate(R.layout.chat_row_recv_picture, null);
            case FileTaskManager.NOTICE_TYPE_VEDIO_SEND:// 视频消息
                return isSendNotice(bean.getSender()) ? layoutInflater.inflate(
                    R.layout.chat_row_sent_video, null) : layoutInflater
                           .inflate(R.layout.chat_row_recv_video, null);
            case FileTaskManager.NOTICE_TYPE_VCARD_SEND:// 名片消息
                return isSendNotice(bean.getSender()) ? layoutInflater.inflate(
                    R.layout.chat_row_sent_vcard, null) : layoutInflater
                           .inflate(R.layout.chat_row_recv_vcard, null);
            case FileTaskManager.NOTICE_TYPE_AUDIO_SEND:// 音频消息
                return isSendNotice(bean.getSender()) ? layoutInflater.inflate(
                    R.layout.chat_row_sent_audio, null) : layoutInflater
                           .inflate(R.layout.chat_row_recv_audio, null);
            case FileTaskManager.NOTICE_TYPE_DESCRIPTION:
                return layoutInflater.inflate(R.layout.chat_row_add_massege, null);
            case FileTaskManager.NOTICE_TYPE_RECORD:// 通话记录
                return isSendNotice(bean.getSender()) ? layoutInflater.inflate(
                    R.layout.chat_row_sent_message, null) : layoutInflater
                           .inflate(R.layout.chat_row_recv_message, null);
            case FileTaskManager.NOTICE_TYPE_FILE:
                return isSendNotice(bean.getSender()) ? layoutInflater.inflate(
                    R.layout.chat_send_file_layout, null) : layoutInflater
                           .inflate(R.layout.chat_rec_file_layout, null);
            case FileTaskManager.NOTICE_TYPE_MEETING_INVITE:// 会议邀请信息
            case FileTaskManager.NOTICE_TYPE_MEETING_BOOK:// 会议预约信息,布局相同，但icon不同
                return isSendNotice(bean.getSender()) ? layoutInflater.inflate(
                    R.layout.chat_row_send_meeting, null) : layoutInflater
                           .inflate(R.layout.chat_row_recv_meeting, null);
            default:
                // invalid
                return null;
        }
    }


    private void handleDescription(final NoticesBean bean,
                                   final ViewHolder holder) {
        String text = "";
        try {
            JSONArray bodyArray = new JSONArray(bean.getBody());
            if (bodyArray != null && bodyArray.length() > 0) {
                JSONObject bodyObj = bodyArray.optJSONObject(0);
                text = bodyObj.optString("txt");
                if (!TextUtils.isEmpty(text) && text.length() > 3000) {
                    text = text.substring(0, 3000);
                    text = text + "...";
                }
            }
        } catch (Exception e) {
            LogUtil.e("JSONArray Exception", e);
        }
        holder.noticeAddTxt.setText(text);
    }


    private void handleFileMsg(final NoticesBean bean,
                               final ViewHolder holder) {

        showStatus(bean, holder);
        loadHeadImage(bean, holder);

        String fileName = "";
        long fileSize = 0;
        String typeName = "";
        String remoteUrl = "";
        try {
            JSONArray bodyArray = new JSONArray(bean.getBody());
            if (bodyArray != null && bodyArray.length() > 0) {
                JSONObject bodyObj = bodyArray.optJSONObject(0);
                fileSize = bodyObj.optLong("size");
                fileName = bodyObj.optString("fileName");
                typeName = bodyObj.optString("fileType");
                remoteUrl = bodyObj.optString("remoteUrl");
            }
        } catch (Exception e) {
            LogUtil.e("JSONArray Exception", e);
        }

        if (isSendNotice(bean.getSender())) {

            // 发送的消息才有状态
            switch (bean.getStatus()) {
                case FileTaskManager.TASK_STATUS_SUCCESS:
                    holder.mProgressBar.setVisibility(GONE);
                    detachFileUploadProgressView(holder.mProgressBar, bean.getId());
                    changUIProgressTaskIds.remove(bean.getId());
                    mTaskFileProgressMap.remove(bean.getId());
                    break;
                case FileTaskManager.TASK_STATUS_READY:
                case FileTaskManager.TASK_STATUS_RUNNING:
                case FileTaskManager.TASK_STATUS_COMPRESSING:
                    Float curPro = mTaskFileProgressMap.get(bean.getId());
                    if (curPro != null) {
                        // 初始化进度值
                        int duration = (int) (curPro * 100);
                        holder.mProgressBar.setProgress(duration);
                    } else {
                        holder.mProgressBar.setVisibility(GONE);
                    }
                    if (TextUtils.isEmpty(remoteUrl)) {
                        holder.mProgressBar.setVisibility(View.VISIBLE);
                        // 状态为进行中，需要更新界面进度
                        if (!changUIProgressTaskIds.contains(bean.getId())) {
                            // 文件上传任务
                            changUIProgressTaskIds.add(bean.getId());
                        }
                        attachFileUploadProgressView(holder.mProgressBar, bean.getId());
                    } else {
                        holder.mProgressBar.setVisibility(GONE);
                    }
                    break;
                case FileTaskManager.TASK_STATUS_FAIL:
                    // 重发按钮
                    changUIProgressTaskIds.remove(bean.getId());
                    mTaskFileProgressMap.remove(bean.getId());
                    holder.mProgressBar.setVisibility(GONE);
                    break;
            }
        }

        holder.vcardHeadIv.setBackgroundResource(CollectionFileManager
            .getInstance().getNoticeFileDrawableId(typeName));
        holder.vcardNameTxt.setText(fileName);
        holder.vcardNubeTxt.setText(CollectionFileManager.getInstance().convertStorage(fileSize));
        holder.vcardBg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (onItemClickedifMultiSeclect(bean, holder)) {
                    return;
                }
                CollectionFileManager.getInstance()
                    .gotoCollectionFileForNoticeActivity(mContext, bean);
            }
        });

        holder.vcardBg.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(final View v) {
                if (onLongClickedifMultiSeclect()) {
                    return true;
                }
                // 显示 删除 菜单
                MedicalAlertDialog menuDlg = new MedicalAlertDialog(v
                    .getContext());
                if (bean.getStatus() == FileTaskManager.TASK_STATUS_FAIL) {
                    // 失败状态，长按显示重试按钮
                    menuDlg.addButtonFirst(new BottomMenuWindow.MenuClickedListener() {
                        @Override
                        public void onMenuClicked() {
                            startTask(bean);
                        }
                    }, mContext.getString(R.string.chat_resend));
                } else {
                    menuDlg.addButtonSecond(new BottomMenuWindow.MenuClickedListener() {
                        @Override
                        public void onMenuClicked() {
                            if (preJudgment()) {
                                return;
                            }
                            if (checkDataValid(bean.getType(), bean.getBody()) &&
                                callbackIf != null) {
                                callbackIf.onMsgForward(bean.getId(),
                                    bean.getSender(), bean.getType(),
                                    bean.getStatus(), null);
                            }
                        }
                    }, mContext.getString(R.string.chat_forward));
                }
                menuDlg.addButtonThird(new BottomMenuWindow.MenuClickedListener() {
                    @Override
                    public void onMenuClicked() {
                        if (checkDataValid(bean.getType(), bean.getBody())) {
                            CollectionManager.getInstance()
                                .addCollectionByNoticesBean(mContext, bean);
                        }
                    }
                }, "收藏");
                menuDlg.addButtonForth(new BottomMenuWindow.MenuClickedListener() {
                    @Override
                    public void onMenuClicked() {
                        //                        MobclickAgent.onEvent(mContext,
                        //                                UmengEventConstant.EVENT_DELETE_MSG);
                        if (preJudgment()) {
                            return;
                        }
                        if (callbackIf != null) {
                            callbackIf.onMsgDelete(bean.getId(),
                                bean.getReceivedTime(), getCount());
                        }
                    }
                }, mContext.getString(R.string.chat_delete));
                addMoreItem(menuDlg, bean, 5);
                menuDlg.show();
                return true;
            }
        });

    }


    private void handleInviteMeeting(final NoticesBean bean,
                                     final ViewHolder holder) {
        showStatus(bean, holder);
        holder.vcardHeadIv.setImageResource(R.drawable.m_chat_meet_icon);
        loadHeadImage(bean, holder);

        String showName = "";
        String meetingRoom = "";
        String meetingUrl = "";
        try {
            JSONArray bodyArray = new JSONArray(bean.getBody());
            if (bodyArray != null && bodyArray.length() > 0) {
                JSONObject bodyObj = bodyArray.optJSONObject(0);
                meetingRoom = bodyObj.optString("meetingRoom");
                // TODO:liyun 的要求(IOS 发送的URL 仅有域名部分；要求Android保存一致)
                String tmpStr = bodyObj.optString("meetingUrl");
                if(!tmpStr.endsWith("/")){
                    tmpStr = tmpStr + "/";
                }
                meetingUrl = tmpStr + meetingRoom;
                String inviterId = bodyObj.optString("inviterId");
                showName = ShowNameUtil
                    .getShowName(ShowNameUtil.getNameElement(
                        getShowName(inviterId),
                        bodyObj.optString("inviterName"), "", inviterId));

                CustomLog.d(ACTIVITY_FLAG,
                    "立即会议邀请者ID" + inviterId + "," +
                        "预约会议会议室号: " + meetingRoom + "," +
                        "立即会议 URL: " + meetingUrl + ",");
            }
        } catch (Exception e) {
            LogUtil.e("JSONArray Exception", e);
        }

        final String mMeetingNum = meetingRoom;
        holder.vcardDetail.setVisibility(GONE);
        holder.vcardNameTxt.setText(showName + "召开了视频会诊" + meetingRoom);

        holder.vcardNubeTxt.setText(getString(R.string.contact_card_btn));
        final String meetURl = meetingUrl;

        //设置点击事件
        holder.vcardBg.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (onItemClickedifMultiSeclect(bean, holder)) {
                    return;
                }

                final MedicalMeetingManage meetingManager = MedicalMeetingManage.getInstance();

                int isSuccess = meetingManager.joinMeeting(mMeetingNum,
                    new MedicalMeetingManage.OnJoinMeetingListener() {
                        @Override
                        public void onJoinMeeting(String valueDes, int valueCode) {
                            if (valueCode < 0) {
                                CustomToast.show(mContext, "加入会诊失败！", 1);
                            }
                        }
                    });

                if (isSuccess == 0) {
                    // CustomToast.show(mContext, "加入会诊成功", 1);
                } else if (isSuccess == -9992) {
                    CustomToast.show(mContext, "网络不给力，请检查网络！", 1);
                } else {
                    CustomToast.show(mContext, "加入会诊失败！", 1);
                }
            }

        });

        holder.vcardBg.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(final View v) {
                if (onLongClickedifMultiSeclect()) {
                    return true;
                }
                // 显示 删除 菜单
                MedicalAlertDialog menuDlg = new MedicalAlertDialog(v
                    .getContext());
                if (bean.getStatus() == FileTaskManager.TASK_STATUS_FAIL) {
                    // 失败状态，长按显示重试按钮
                    menuDlg.addButtonFirst(new MenuClickedListener() {
                        @Override
                        public void onMenuClicked() {
                            startTask(bean);
                        }
                    }, mContext.getString(R.string.chat_resend));
                } else {
                    menuDlg.addButtonSecond(new MenuClickedListener() {
                        @Override
                        public void onMenuClicked() {
                            if (preJudgment()) {
                                return;
                            }
                            if (callbackIf != null) {
                                callbackIf.onMsgForward(bean.getId(),
                                    bean.getSender(), bean.getType(),
                                    bean.getStatus(), null);
                            }
                        }
                    }, mContext.getString(R.string.chat_forward));
                    menuDlg.addButtonThird(new MenuClickedListener() {
                        @Override
                        public void onMenuClicked() {
                            if (preJudgment()) {
                                return;
                            }
                            IMCommonUtil.copy2Clipboard(v.getContext(),
                                holder.vcardNameTxt.getText().toString()
                                    + meetURl);
                            showToast(R.string.toast_copy_ok);
                        }
                    }, mContext.getString(R.string.chat_copy));
                }
                menuDlg.addButtonForth(new MenuClickedListener() {
                    @Override
                    public void onMenuClicked() {
                        //                        MobclickAgent.onEvent(mContext,
                        //                                UmengEventConstant.EVENT_DELETE_MSG);
                        if (preJudgment()) {
                            return;
                        }
                        if (callbackIf != null) {
                            callbackIf.onMsgDelete(bean.getId(),
                                bean.getReceivedTime(), getCount());
                        }
                    }
                }, mContext.getString(R.string.chat_delete));
                addMoreItem(menuDlg, bean, 5);
                menuDlg.show();
                return true;
            }
        });
    }


    private void handleBookMeeting(final NoticesBean bean,
                                   final ViewHolder holder) {
        showStatus(bean, holder);
        holder.vcardHeadIv.setImageResource(R.drawable.m_chat_meet_book_icon);
        loadHeadImage(bean, holder);

        final BookMeetingExInfo exInfo = new BookMeetingExInfo();
        try {
            //解析 Json
            JSONArray bodyArray = new JSONArray(bean.getBody());

            if (bodyArray != null && bodyArray.length() > 0) {
                JSONObject bodyObj = bodyArray.optJSONObject(0);
                exInfo.setBookNube(bodyObj
                    .optString(BookMeetingExInfo.BOOK_NUBE));
                exInfo.setBookName(ShowNameUtil.getShowName(ShowNameUtil
                    .getNameElement(getShowName(exInfo.getBookNube()),
                        bodyObj.optString(BookMeetingExInfo.BOOK_NAME),
                        "", exInfo.getBookNube())));
                exInfo.setMeetingRoom(bodyObj
                    .optString(BookMeetingExInfo.MEETING_ROOM));// 88888888
                exInfo.setMeetingTheme(bodyObj
                    .optString(BookMeetingExInfo.MEETING_THEME));// 产品部会议
                // 2016/1/16 14:00 IOS要求发送消息时body体为秒
                exInfo.setMeetingTime(bodyObj
                    .optLong(BookMeetingExInfo.MEETING_TIME) * 1000);
                exInfo.setMeetingUrl(bodyObj
                    .optString(BookMeetingExInfo.MEETING_URL));// http://jihuiyi.cn/butel/
            }

            CustomLog.d(ACTIVITY_FLAG,
                "预约会议号" + exInfo.getBookNube() + "," +
                    "预约会议名称: " + exInfo.getBookName() + "," +
                    "预约会议会议室号: " + exInfo.getMeetingRoom() + "," +
                    "预约会议主题: " + exInfo.getMeetingTheme() + "," +
                    "预约会议时间: " + exInfo.getMeetingTime() + "," +
                    "预约会议 URL: " + exInfo.getMeetingUrl() + ",");

        } catch (Exception e) {
            LogUtil.e("JSONArray Exception", e);
        }
        String describe = exInfo.getBookName()
            + "预约了视频会诊"
            + exInfo.getMeetingRoom();

        String meetingDetail =
            "主题：" + exInfo.getMeetingTheme()
                + "\n"
                + "时间：" + DateUtil.formatMs2String(exInfo.getMeetingTime(),
                DateUtil.FORMAT_YYYY_MM_DD_HH_MM_N);

        holder.vcardNameTxt.setText(describe);
        // holder.vcardNubeTxt.setText(exInfo.getMeetingUrl()
        //     + exInfo.getMeetingRoom());// http://jihuiyi.cn/butel/88888888
        holder.vcardNubeTxt.setText(getString(R.string.reserve_meeting));

        holder.vcardDetail.setText(meetingDetail);
        holder.vcardBg.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (onItemClickedifMultiSeclect(bean, holder)) {
                    return;
                }
                // int dif = DateUtil.realDateIntervalDay(exInfo.getMeetingTime(),
                //     System.currentTimeMillis());
                // if (dif > 0) {
                //     LogUtil.d("预约的不是当天会议，直接跳转到预约详情页面,dif=" + dif);
                Intent i = new Intent(mContext,
                    ReserveSuccessActivity.class);
                i.putExtra(ReserveSuccessActivity.KEY_BOOK_MEETING_EXINFO,
                    exInfo);
                mContext.startActivity(i);
                // }
                // else {
                //     LogUtil.d("预约的当天会议，直接加入会议,dif=" + dif);
                //
                //     final MedicalMeetingManage meetingManager = MedicalMeetingManage.getInstance();
                //
                //     int isSuccess = meetingManager.joinMeeting(exInfo.getMeetingRoom(),
                //         new MedicalMeetingManage.OnJoinMeetingListener() {
                //             @Override
                //             public void onJoinMeeting(String valueDes, int valueCode) {
                //                 if (valueCode < 0) {
                //                     CustomToast.show(mContext, "初始化失败", 1);
                //                 }
                //             }
                //         });
                //
                //     if (isSuccess == 0) {
                //         CustomToast.show(mContext, "加入会议成功", 1);
                //     } else {
                //         CustomToast.show(mContext, "加入会议失败", 1);
                //
                //     }
                // }
            }
        });

        holder.vcardBg.setOnLongClickListener(new OnLongClickListener() {

            @Override
            public boolean onLongClick(final View v) {
                if (onLongClickedifMultiSeclect()) {
                    return true;
                }
                // 显示 删除 菜单
                MedicalAlertDialog menuDlg = new MedicalAlertDialog(v
                    .getContext());
                if (bean.getStatus() == FileTaskManager.TASK_STATUS_FAIL) {
                    // 失败状态，长按显示重试按钮
                    menuDlg.addButtonFirst(new MenuClickedListener() {
                        @Override
                        public void onMenuClicked() {
                            startTask(bean);
                        }
                    }, mContext.getString(R.string.chat_resend));
                } else {
                    menuDlg.addButtonSecond(new MenuClickedListener() {
                        @Override
                        public void onMenuClicked() {
                            if (preJudgment()) {
                                return;
                            }
                            if (callbackIf != null) {
                                callbackIf.onMsgForward(bean.getId(),
                                    bean.getSender(), bean.getType(),
                                    bean.getStatus(), null);
                            }
                        }
                    }, mContext.getString(R.string.chat_forward));
                    menuDlg.addButtonThird(new MenuClickedListener() {
                        @Override
                        public void onMenuClicked() {
                            //                            MobclickAgent.onEvent(mContext,
                            //                                    UmengEventConstant.EVENT_COPY_MSG);
                            if (preJudgment()) {
                                return;
                            }
                            String tmpStr = exInfo.getMeetingUrl();
                            if(tmpStr.endsWith("/")){
                                tmpStr = tmpStr + exInfo.getMeetingRoom();
                            }else {
                                tmpStr = tmpStr + "/" + exInfo.getMeetingRoom();
                            }
                            IMCommonUtil.copy2Clipboard(v.getContext(),
                                holder.vcardNameTxt.getText().toString()
                                    + holder.vcardNubeTxt.getText()
                                    .toString() + tmpStr);
                            showToast(R.string.toast_copy_ok);
                        }
                    }, mContext.getString(R.string.chat_copy));
                }
                menuDlg.addButtonForth(new MenuClickedListener() {
                    @Override
                    public void onMenuClicked() {
                        //                        MobclickAgent.onEvent(mContext,
                        //                                UmengEventConstant.EVENT_DELETE_MSG);
                        if (preJudgment()) {
                            return;
                        }
                        if (callbackIf != null) {
                            callbackIf.onMsgDelete(bean.getId(),
                                bean.getReceivedTime(), getCount());
                        }
                    }
                }, mContext.getString(R.string.chat_delete));
                addMoreItem(menuDlg, bean, 5);
                menuDlg.show();
                return true;
            }
        });
    }


    private String getShowName(String nube) {
        String name = "";
        if (isSendNotice(nube)) {// 是自己
            name = AccountManager.getInstance(MedicalApplication.getContext()).getName();
        } else {
            if (noticeType == ChatActivity.VALUE_CONVERSATION_TYPE_MULTI) {// 群聊
                name = groupDao.queryGroupMember(groupId, nube).getDispName();
            } else {// 单聊
                name = ShowNameUtil.getShowName(nube);
            }
        }
        return name;

    }


    private void handleCallRecord(final NoticesBean bean,
                                  final ViewHolder holder) {
        loadHeadImage(bean, holder);
        holder.retryBtn.setVisibility(GONE);
        holder.runningPb.setVisibility(GONE);
        holder.noticeTxt.setVisibility(View.VISIBLE);

        String text = "";
        int callType = 0;
        try {
            JSONArray bodyArray = new JSONArray(bean.getBody());
            if (bodyArray != null && bodyArray.length() > 0) {
                JSONObject bodyObj = bodyArray.optJSONObject(0);
                text = bodyObj.optString("txt");
                callType = bodyObj.optInt("calltype");
            }
            holder.noticeTxt.setCompoundDrawablePadding(10);
            if (callType == 0) {
                if (isSendNotice(bean.getSender())) {
                    holder.noticeTxt.setCompoundDrawablesWithIntrinsicBounds(
                        R.drawable.m_chat_audio_call_send, 0, 0, 0);
                } else {
                    holder.layout.setVisibility(GONE);
                    holder.noticeTxt.setCompoundDrawablesWithIntrinsicBounds(0,
                        0, R.drawable.m_chat_audio_call_receiver, 0);
                }
            } else {
                if (isSendNotice(bean.getSender())) {
                    holder.noticeTxt.setCompoundDrawablesWithIntrinsicBounds(
                        R.drawable.m_chat_vedio_call_send, 0, 0, 0);
                } else {
                    holder.layout.setVisibility(GONE);
                    holder.noticeTxt.setCompoundDrawablesWithIntrinsicBounds(0,
                        0, R.drawable.m_chat_vedio_call_receiver, 0);
                }
            }
            holder.noticeTxt.setText(text);

            final int type = callType;
            holder.noticeTxt.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View arg0) {
                    // TODO Auto-generated method stub
                    if (onItemClickedifMultiSeclect(bean, holder)) {
                        return;
                    }
                    if (preJudgment()) {
                        return;
                    }
                    //                    MobclickAgent.onEvent(mContext,
                    //                            UmengEventConstant.EVENT_REPLAY_CALL);
                    //                    if (1 == type) {
                    //                        OutCallUtil.makeNormalCall((Activity) mContext,
                    //                                targetNumber, OutCallUtil.CT_SIP_AV, "", "");
                    //                    } else {
                    //                        OutCallUtil.makeNormalCall((Activity) mContext,
                    //                                targetNumber, OutCallUtil.CT_SIP_AUDIO, "", "");
                    //                    }
                }
            });

            holder.noticeTxt.setOnLongClickListener(new OnLongClickListener() {

                @Override
                public boolean onLongClick(View v) {
                    if (onLongClickedifMultiSeclect()) {
                        return true;
                    }
                    // 显示 删除 菜单
                    MedicalAlertDialog menuDlg = new MedicalAlertDialog(v
                        .getContext());
                    menuDlg.addButtonFirst(new MenuClickedListener() {
                        @Override
                        public void onMenuClicked() {
                            //                            MobclickAgent.onEvent(mContext,
                            //                                    UmengEventConstant.EVENT_DELETE_MSG);
                            if (preJudgment()) {
                                return;
                            }
                            if (callbackIf != null) {
                                callbackIf.onMsgDelete(bean.getId(),
                                    bean.getReceivedTime(), getCount());
                            }
                        }
                    }, mContext.getString(R.string.chat_delete));
                    addMoreItem(menuDlg, bean, 2);
                    menuDlg.show();
                    return true;
                }
            });
        } catch (Exception e) {
            LogUtil.e("JSONArray Exception", e);
        }

    }


    private void handleTextMessage(final NoticesBean bean,
                                   final ViewHolder holder) {
        loadHeadImage(bean, holder);

        String text = "";
        String pageData = "";
        try {
            JSONArray bodyArray = new JSONArray(bean.getBody());
            if (bodyArray != null && bodyArray.length() > 0) {
                JSONObject bodyObj = bodyArray.optJSONObject(0);
                String oriText = bodyObj.optString("txt");
                text = oriText;
                pageData = bodyObj.optString("webData");
                LogUtil.d("handleTextMessage 处理文字消息 " + text);
                if (!TextUtils.isEmpty(text) && text.length() > 3000) {
                    text = text.substring(0, 3000);
                    text = text + "...";
                }
                if (noticeType == ChatActivity.VALUE_CONVERSATION_TYPE_MULTI) {
                    ArrayList<String> dispNubeList = new ArrayList<String>();
                    dispNubeList = CommonUtil.getDispList(text);
                    for (int i = 0; i < dispNubeList.size(); i++) {
                        GroupMemberBean gbean = groupDao.queryGroupMember(
                            groupId, dispNubeList.get(i));
                        if (gbean != null) {
                            ShowNameUtil.NameElement element = ShowNameUtil.getNameElement(
                                gbean.getName(), gbean.getNickName(),
                                gbean.getPhoneNum(), gbean.getNubeNum());
                            String MName = ShowNameUtil.getShowName(element);
                            text = text.replace("@" + dispNubeList.get(i)
                                + IMConstant.SPECIAL_CHAR, "@" + MName
                                + IMConstant.SPECIAL_CHAR);
                        }
                    }
                }

                holder.noticeTxt.setText(text);

                // 取出文字中的url信息
                //                List<String> urls = HtmlParseManager.getInstance().getUrl(
                //                        holder.noticeTxt.getUrls());
                List<String> urls = HtmlParseManager.getInstance().getUrls(text);

                // 1.发送的消息，有链接时，修改noticeTxt的显示规则
                // 2.接收的消息，有链接时，在解析结束前，在noticeTxt上显示，需修改其显示规则
                // 显示规则：点击url，进入内嵌的web浏览器
                if (urls.size() > 0) {
                    LogUtil.d("消息中有链接，修改其显示规则");

                    HtmlParseManager.getInstance().changeShowRules(
                        holder.noticeTxt, new OnClickBack() {
                            @Override
                            public void OnClick(String mUrl) {
                                Intent intent = new Intent(mContext,
                                    EmbedWebViewActivity.class);
                                intent.putExtra(
                                    EmbedWebViewActivity.KEY_PARAMETER_URL,
                                    mUrl);
                                intent.putExtra(
                                    EmbedWebViewActivity.KEY_PARAMETER_TITLE,
                                    ACTIVITY_FLAG);
                                mContext.startActivity(intent);
                            }
                        });
                }

                onTextLongClick(holder.noticeTxt, bean,
                    !isSendNotice(bean.getSender()) && urls.size() > 0, holder);

                LogUtil.d("urls.size=" + urls.size() + "\nurls="
                    + urls.toString());
                LogUtil.d("webData=" + pageData);

                // 接收的消息才需要考虑链接的图文显示
                if (!isSendNotice(bean.getSender())) {

                    // 在layout set visible之前remove
                    holder.layout.removeAllViews();
                    if (urls.size() > 0) {

                        // 首先显示链接文本，因为链接解析需要时间，防止短暂出现一条空白item
                        holder.noticeTxt.setVisibility(View.VISIBLE);
                        holder.layout.setVisibility(GONE);

                        if (TextUtils.isEmpty(pageData)) {
                            LogUtil.d("webData为空，将urls加入任务队列解析");
                            // parseHtmlThread()方法返回false时，表示本次没有成功解析(网络不好的时候)，直接
                            // 作为字符串显示出来
                            HtmlParseManager.getInstance()
                                .parseHtmlToNoticesThread(bean.getId(),
                                    urls, oriText);
                        } else {
                            // pageData不为空，直接取出来 显示
                            LogUtil.d("webData不为空，直接取出来显示");
                            JSONArray pageArray = new JSONArray(pageData);
                            List<WebpageBean> webPages = null;
                            webPages = HtmlParseManager.getInstance()
                                .convertWebpageBean(pageArray);

                            // 文字区域
                            View txtView;
                            TextView txt;

                            WebpageBean pageBean = null;

                            for (int i = 0; i < webPages.size(); i++) {

                                //                                // 该链接前的文字
                                //                                String txtBefore = "";

                                pageBean = webPages.get(i);

                                // 链接区域
                                View linkView;
                                View picTxt;
                                XCRoundImageViewByXfermode imgView;
                                TextView dscrView;
                                TextView titleView;
                                TextView urlView;

                                // 循环每个链接
                                //                                String title = webPages.get(i).getTitle();
                                final String srcUrl = pageBean.getSrcUrl();

                                // 显示链接前的文字
                                txtView = layoutInflater.inflate(
                                    R.layout.text_item, null);
                                txt = (TextView) txtView
                                    .findViewById(R.id.tv_chatcontent);
                                // 分隔线
                                View line1 = txtView
                                    .findViewById(R.id.line_bottom);

                                if (!TextUtils.isEmpty(pageBean.getHeaderStr())) {
                                    txt.setText(pageBean.getHeaderStr());
                                    holder.layout.addView(txtView);
                                    line1.setVisibility(View.VISIBLE);
                                }
                                // 显示链接
                                linkView = layoutInflater.inflate(
                                    R.layout.link_item, null);
                                // 如果解析不了，则直接显示链接字符串
                                urlView = (TextView) linkView
                                    .findViewById(R.id.link_addr);
                                picTxt = linkView.findViewById(R.id.pic_txt);
                                titleView = (TextView) linkView
                                    .findViewById(R.id.msg_title);
                                dscrView = (TextView) linkView
                                    .findViewById(R.id.msg_abstract);
                                imgView = (XCRoundImageViewByXfermode) linkView
                                    .findViewById(R.id.img);
                                imgView.setType(XCRoundImageViewByXfermode.TYPE_ROUND);
                                imgView.setRoundBorderRadius(10);
                                // imgView.setTag(srcUrl);

                                // 点击，跳入网页
                                // linkView.setTag(srcUrl);
                                // TODO:链接区域长按事件-
                                onTextLongClick(linkView, bean, true, holder);

                                linkView.setOnClickListener(new OnClickListener() {

                                    @Override
                                    public void onClick(View v) {
                                        LogUtil.d("linkView is click");
                                        Intent intent = new Intent(mContext,
                                            EmbedWebViewActivity.class);
                                        intent.putExtra(
                                            EmbedWebViewActivity.KEY_PARAMETER_URL,
                                            srcUrl);
                                        intent.putExtra(
                                            EmbedWebViewActivity.KEY_PARAMETER_TITLE,
                                            ACTIVITY_FLAG);
                                        mContext.startActivity(intent);
                                    }
                                });
                                if (!pageBean.isValid()) {
                                    LogUtil.d("is not valid ");
                                    // 标题摘要都为空，直接显示链接str
                                    urlView.setVisibility(View.VISIBLE);
                                    picTxt.setVisibility(GONE);

                                    urlView.setText(pageBean.getSrcUrl());

                                    HtmlParseManager.getInstance()
                                        .changeShowRules(urlView,
                                            new OnClickBack() {
                                                @Override
                                                public void OnClick(
                                                    String mUrl) {
                                                    Intent intent = new Intent(
                                                        mContext,
                                                        EmbedWebViewActivity.class);
                                                    intent.putExtra(
                                                        EmbedWebViewActivity.KEY_PARAMETER_URL,
                                                        mUrl);
                                                    intent.putExtra(
                                                        EmbedWebViewActivity.KEY_PARAMETER_TITLE,
                                                        ACTIVITY_FLAG);
                                                    mContext.startActivity(intent);
                                                }
                                            });

                                    // urlView绑定长按事件
                                    onTextLongClick(linkView, bean, true, holder);
                                } else {
                                    LogUtil.d("is valid");
                                    // 标题为空
                                    urlView.setVisibility(GONE);
                                    picTxt.setVisibility(View.VISIBLE);
                                    titleView.setText(pageBean.getTitle());
                                    dscrView.setText(pageBean.getDescription());
                                    if (TextUtils.isEmpty(pageBean.getImgUrl())) {
                                        imgView.setImageResource(R.drawable.default_link_pic);
                                    } else {
                                        // 下载时，显示一张默认图片
                                        Glide.with(mContext)
                                            .load(pageBean.getImgUrl())
                                            .placeholder(
                                                R.drawable.default_link_pic)
                                            .error(R.drawable.default_link_pic)
                                            .centerCrop()
                                            .diskCacheStrategy(
                                                DiskCacheStrategy.SOURCE)
                                            .crossFade().into(imgView);
                                    }
                                }
                                holder.layout.addView(linkView);
                                // 链接文字
                                if (i < webPages.size() - 1) {
                                    View line = linkView
                                        .findViewById(R.id.divider_line);
                                    line.setVisibility(View.VISIBLE);
                                }
                                // 内容加载完成，才显示图文信息
                                holder.noticeTxt.setVisibility(GONE);
                                holder.layout.setVisibility(View.VISIBLE);
                            }
                            //                            LogUtil.d("显示完链接之后的text=" + text);
                            if (pageBean != null && !TextUtils.isEmpty(pageBean.getFooterStr())) {
                                // 最后一个链接后面还有文字，显示出来
                                txtView = layoutInflater.inflate(
                                    R.layout.text_item, null);
                                txt = (TextView) txtView
                                    .findViewById(R.id.tv_chatcontent);
                                txt.setText(pageBean.getFooterStr());
                                holder.layout.addView(txtView);
                                View line = txtView.findViewById(R.id.line_top);
                                line.setVisibility(View.VISIBLE);
                            }
                            // TODO:链接区域长按事件---转发/复制/删除
                            onTextLongClick(holder.layout, bean, true, holder);
                        }
                    } else {
                        holder.noticeTxt.setVisibility(View.VISIBLE);
                        holder.layout.setVisibility(GONE);
                    }
                }
            }
        } catch (Exception e) {
            LogUtil.e("JSONArray Exception", e);
        }
        showStatus(bean, holder);
    }


    private void handleImageMessage(final NoticesBean bean,
                                    final ViewHolder holder, final boolean isVideo) {
        // 图片内容
        final ButelFileInfo fileInfo = ButelFileInfo.parseJsonStr(bean
            .getBody(), false);

        // 图片显示大小
        int[] imgSize = null;
        if (isVideo) {
            // 视频显示固定大小
            imgSize = new int[] { picDefaultSize, picDefaultSize };
        } else {
            // 图片的场合，保持原图尺寸比例
            imgSize = caculateImgSize(fileInfo.getWidth(), fileInfo.getHeight());
        }
        RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) holder.imgLayout
            .getLayoutParams();
        lp.width = imgSize[0];
        lp.height = imgSize[1];
        holder.imgLayout.setLayoutParams(lp);

        final boolean isSend = isSendNotice(bean.getSender());
        loadHeadImage(bean, holder);

        // 显示图片
        showImage(isSend, isVideo, fileInfo.getLocalPath(),
            fileInfo.getThumbUrl(), fileInfo.getRemoteUrl(), holder.imgIv,
            imgSize[0], imgSize[1]);

        // 视频时长
        if (isVideo) {
            holder.durationTxt.setText(getDispDuration(fileInfo.getDuration()));
        }

        // 显示状态
        if (isSend) {
            // 发送的消息才有状态
            switch (bean.getStatus()) {
                case FileTaskManager.TASK_STATUS_SUCCESS:
                    holder.progressTxt.setText("0%");
                    holder.progressLine.setVisibility(GONE);
                    holder.imgIvMask.setVisibility(GONE);
                    holder.retryBtn.setVisibility(GONE);

                    changUIProgressTaskIds.remove(bean.getId());
                    mTaskFileProgressMap.remove(bean.getId());
                    detachUploadProgressView(holder.progressTxt, bean.getId());
                    break;
                case FileTaskManager.TASK_STATUS_READY:
                case FileTaskManager.TASK_STATUS_RUNNING:
                case FileTaskManager.TASK_STATUS_COMPRESSING:
                    holder.progressLine.setVisibility(View.VISIBLE);
                    holder.imgIvMask.setVisibility(View.VISIBLE);
                    holder.retryBtn.setVisibility(GONE);

                    Float curPro = mTaskFileProgressMap.get(bean.getId());
                    if (curPro != null) {
                        // 初始化进度值
                        NumberFormat numFormat = NumberFormat.getNumberInstance();
                        numFormat.setMaximumFractionDigits(2);
                        holder.progressTxt.setText(numFormat.format(curPro
                            .floatValue() * 100) + "%");
                    } else {
                        holder.progressTxt.setText("0%");
                    }

                    // 状态为进行中，需要更新界面进度
                    if (!changUIProgressTaskIds.contains(bean.getId())) {
                        // 文件上传任务
                        changUIProgressTaskIds.add(bean.getId());
                    }
                    attachUploadProgressView(holder.progressTxt, bean.getId());
                    break;
                case FileTaskManager.TASK_STATUS_FAIL:
                    // 重发按钮
                    holder.retryBtn.setVisibility(View.VISIBLE);
                    holder.progressTxt.setText("0%");
                    holder.progressLine.setVisibility(GONE);
                    holder.imgIvMask.setVisibility(GONE);

                    changUIProgressTaskIds.remove(bean.getId());
                    mTaskFileProgressMap.remove(bean.getId());
                    detachUploadProgressView(holder.progressTxt, bean.getId());
                    break;
            }
        } else {
            holder.progressTxt.setText("0%");
            holder.progressLine.setVisibility(GONE);
            holder.imgIvMask.setVisibility(GONE);
            holder.retryBtn.setVisibility(GONE);
        }

        // 长按
        holder.imgFrameIv.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (onLongClickedifMultiSeclect()) {
                    return true;
                }
                // 显示转发/删除 菜单
                MedicalAlertDialog menuDlg = new MedicalAlertDialog(mContext);

                if (isVideo) {
                    menuDlg.addButtonFirst(new MenuClickedListener() {
                        @Override public void onMenuClicked() {
                            //视频静音播放
                            playSlientVideo(fileInfo,bean);
                        }
                    }, mContext.getString(R.string.slient_play));
                }
                if (bean.getStatus() == FileTaskManager.TASK_STATUS_FAIL) {
                    // 失败状态，长按显示重试按钮
                    menuDlg.addButtonSecond(new MenuClickedListener() {
                        @Override
                        public void onMenuClicked() {
                            if (preJudgment()) {
                                return;
                            }
                            startTask(bean);
                        }
                    }, mContext.getString(R.string.chat_resend));
                } else {
                    menuDlg.addButtonSecond(new MenuClickedListener() {
                        @Override
                        public void onMenuClicked() {
                            if (preJudgment()) {
                                return;
                            }
                            if (checkDataValid(bean.getType(), bean.getBody())) {
                                if (callbackIf != null) {
                                    callbackIf.onMsgForward(bean.getId(),
                                        bean.getSender(), bean.getType(),
                                        bean.getStatus(),
                                        fileInfo.getLocalPath());
                                }
                            }
                        }
                    }, mContext.getString(R.string.chat_forward));
                }

                menuDlg.addButtonThird(new MenuClickedListener() {
                    @Override
                    public void onMenuClicked() {
                        if (checkDataValid(bean.getType(), bean.getBody())) {
                            CollectionManager.getInstance()
                                .addCollectionByNoticesBean(mContext, bean);
                        }
                    }
                }, "收藏");

                menuDlg.addButtonForth(new MenuClickedListener() {
                    @Override
                    public void onMenuClicked() {
                        if (preJudgment()) {
                            return;
                        }
                        if (callbackIf != null) {
                            callbackIf.onMsgDelete(bean.getId(),
                                bean.getReceivedTime(), getCount());
                            changUIProgressTaskIds.remove(bean.getId());
                            mTaskFileProgressMap.remove(bean.getId());
                        }
                    }
                }, mContext.getString(R.string.chat_delete));

                //更多 按钮
                addMoreItem(menuDlg, bean, 5);
                menuDlg.show();
                return true;
            }
        });

        // 单击查看
        holder.imgFrameIv.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onItemClickedifMultiSeclect(bean, holder)) {
                    return;
                }
                // 查看图片/视频
                boolean exist = isValidFilePath(fileInfo.getLocalPath());
                if (isSend) {
                    if (isVideo) {
                        if (exist) {
                            // 本地视频存在，则直接播放
                            // 播放视频
                            playVideo(fileInfo, false);
                            return;
                        } else {
                            if (TextUtils.isEmpty(fileInfo.getRemoteUrl())) {
                                // 本地不存在，且服务端也不存在的场合，无法查看
                                CustomToast.show(mContext, R.string.toast_no_video, 1);
                                return;
                            }
                        }
                    } else {
                        // 图片的场合
                        // 已没有数据源，提示已删除
                        if (!exist
                            && TextUtils.isEmpty(fileInfo.getRemoteUrl())) {
                            showToast(R.string.toast_no_pic);
                            return;
                        }
                    }
                } else {
                    // 接收的视频，本地已存在的场合，直接播放
                    if (isVideo && exist) {

                        // 本地视频存在，则直接播放
                        playVideo(fileInfo, false);

                        return;
                    }
                }

                int type = -1;
                int index = -1;
                ArrayList<PhotoBean> photos = null;
                if (isVideo) {
                    type = FileTaskManager.NOTICE_TYPE_VEDIO_SEND;
                } else {
                    type = FileTaskManager.NOTICE_TYPE_PHOTO_SEND;
                }

                photos = getAllPAVFileList(bean.getThreadsId(), type,
                    bean.getId());
                index = getIndexFromList(photos, bean.getId());

                Intent i = new Intent(mContext, ViewPhotosActivity.class);
                i.putParcelableArrayListExtra(
                    ViewPhotosActivity.KEY_PHOTOS_LIST, photos);
                i.putExtra(ViewPhotosActivity.KEY_PHOTOS_SELECT_INDEX, index);
                i.putExtra(ViewPhotosActivity.KEY_REMOTE_FILE, true);
                i.putExtra(ViewPhotosActivity.KEY_VIDEO_FILE, isVideo);
                i.putExtra(ViewPhotosActivity.KEY_VIDEO_LEN,
                    fileInfo.getDuration());
                i.putExtra(ViewPhotosActivity.KEY_COLLECTION_SCAN, true);
                mContext.startActivity(i);
            }
        });
    }


    private void playSlientVideo(ButelFileInfo videoInfo,NoticesBean bean) {

        boolean exist = isValidFilePath(videoInfo.getLocalPath());
        if (exist) { //本地存在
            Boolean isSilent = true;
            playVideo(videoInfo, isSilent);

        } else if (TextUtils.isEmpty(videoInfo.getRemoteUrl())) {

            // 本地不存在，且服务端也不存在的场合，无法查看
            CustomToast.show(mContext, R.string.toast_no_video, 1);
            CustomLog.d(ACTIVITY_FLAG,"本地不存在，服务端也不存在");

        } else {  //本地不存在, 服务器端存在，下载视频
            int type = -1;
            int index = -1;
            ArrayList<PhotoBean> photos = null;

            type = FileTaskManager.NOTICE_TYPE_VEDIO_SEND;

            photos = getAllPAVFileList(bean.getThreadsId(), type,
                bean.getId());
            index = getIndexFromList(photos, bean.getId());

            Intent i = new Intent(mContext, ViewPhotosActivity.class);
            i.putParcelableArrayListExtra(
                ViewPhotosActivity.KEY_PHOTOS_LIST, photos);
            i.putExtra(ViewPhotosActivity.KEY_PHOTOS_SELECT_INDEX, index);
            i.putExtra(ViewPhotosActivity.KEY_REMOTE_FILE, true);
            i.putExtra(ViewPhotosActivity.KEY_VIDEO_FILE, true);
            i.putExtra(ViewPhotosActivity.KEY_VIDEO_LEN,
                videoInfo.getDuration());
            i.putExtra(ViewPhotosActivity.KEY_COLLECTION_SCAN, true);
            mContext.startActivity(i);

            CustomLog.d(ACTIVITY_FLAG,"本地不存在服务器端存在，下载视频");
        }
    }


    /**
     * 播放视频
     *
     * @param fileInfo 文件信息
     * @param isSilent 是否静音播放
     */
    private void playVideo(ButelFileInfo fileInfo, Boolean isSilent) {

        Intent i = new Intent(mContext,
            PlayVideoActivity.class);
        if (isSilent) {
            i.putExtra(PlayVideoActivity.SILENT_PLAY, isSilent);
        }
        i.putExtra(
            RecordedVideoActivity.KEY_VIDEO_FILE_PATH,
            fileInfo.getLocalPath());
        if (fileInfo.getDuration() == 0) {
            i.putExtra(
                RecordedVideoActivity.KEY_VIDEO_FILE_DURATION,
                30);
        } else {
            i.putExtra(
                RecordedVideoActivity.KEY_VIDEO_FILE_DURATION,
                fileInfo.getDuration());
        }
        mContext.startActivity(i);
    }


    /**
     * 获得会话中相同类型的所有文件信息; 如果是视频，则只获取指定的一条记录;
     *
     * @param threadId 会话的id
     * @param type 消息类型（图片、视频、声音）
     * @param vedioUuid 消息的id
     * @return 文件信息list
     */
    private ArrayList<PhotoBean> getAllPAVFileList(String threadId, int type,
                                                   String vedioUuid) {
        NoticesDao noticeDao = new NoticesDao(mContext);

        ArrayList<PhotoBean> photolist = null;
        PhotoBean bean = null;
        NoticesBean item = null;
        List<NoticesBean> rawlist = null;

        if (type == FileTaskManager.NOTICE_TYPE_VEDIO_SEND) {
            item = noticeDao.getNoticeById(vedioUuid);
            if (item != null) {
                rawlist = new ArrayList<NoticesBean>();
                rawlist.add(item);
            }
        } else {
            rawlist = noticeDao.getAllPAVInConversation(threadId, type);
        }

        if (rawlist != null) {
            photolist = new ArrayList<PhotoBean>();
            int length = rawlist.size();
            for (int i = 0; i < length; i++) {
                item = rawlist.get(i);
                String body = item.getBody();
                String thumbUrl = "";
                String localUrl = "";
                String remoteUrl = "";
                String id = item.getId();
                boolean isfrom = !isSendNotice(item.getSender());
                try {
                    JSONArray array = new JSONArray(body);
                    if (array != null && array.length() > 0) {
                        for (int j = 0; j < array.length(); j++) {
                            JSONObject obj = array.getJSONObject(j);
                            thumbUrl = obj.optString("thumbnail");
                            localUrl = obj.optString("localUrl");
                            remoteUrl = obj.optString("remoteUrl");

                            bean = new PhotoBean();
                            bean.setLittlePicUrl(thumbUrl);
                            bean.setLocalPath(localUrl);
                            bean.setRemoteUrl(remoteUrl);
                            bean.setTaskId(id);
                            bean.setType(type);
                            bean.setFrom(isfrom);

                            photolist.add(bean);
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    continue;
                }
            }
        }
        return photolist;
    }


    /**
     * 获得某个记录在同类记录中的index,从0开始
     *
     * @param photoList 上面方法中获得的同类消息的list
     * @param uuid 消息的id
     */
    private int getIndexFromList(ArrayList<PhotoBean> photoList, String uuid) {

        if (TextUtils.isEmpty(uuid)) {
            return -1;
        }

        if (photoList != null && photoList.size() > 0) {
            int length = photoList.size();
            PhotoBean item = null;
            for (int i = 0; i < length; i++) {
                item = photoList.get(i);
                if (item.getTaskId().equals(uuid)) {
                    return i;
                }
            }
        }

        return -1;
    }


    private void handleVcardMessage(final NoticesBean bean,
                                    final ViewHolder holder) {
        showStatus(bean, holder);
        // 名片内容
        try {
            JSONArray bodyArray = new JSONArray(bean.getBody());
            if (bodyArray != null && bodyArray.length() > 0) {
                JSONObject bodyObj = bodyArray.optJSONObject(0);
                holder.nubeNumber = bodyObj.optString("code");
                holder.nickName = bodyObj.optString("name");
                holder.phoneNum = bodyObj.optString("phone");
                holder.headUrl = bodyObj.optString("url");
                holder.sex = bodyObj.optString("sex");
            }
        } catch (Exception e) {
            LogUtil.e("JSONArray Exception", e);
        }
        loadHeadImage(bean, holder);

        holder.vcardHeadIv.setImageResource(IMCommonUtil
            .getHeadIdBySex(holder.sex));
        Glide.with(mContext).load(holder.headUrl)
            .placeholder(IMCommonUtil.getHeadIdBySex(holder.sex))
            .error(IMCommonUtil.getHeadIdBySex(holder.sex)).centerCrop()
            .diskCacheStrategy(DiskCacheStrategy.SOURCE).crossFade()
            .into(holder.vcardHeadIv);

        ShowNameUtil.NameElement element = ShowNameUtil.getNameElement("", holder.nickName,
            holder.phoneNum, holder.nubeNumber);
        holder.vcardNameTxt.setText(ShowNameUtil.getShowName(element));
        holder.vcardNubeTxt.setText(ShowNameUtil.getShowNumber(element));

        // 长按
        holder.vcardBg.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (onLongClickedifMultiSeclect()) {
                    return true;
                }
                // 显示转发/删除 菜单
                MedicalAlertDialog menuDlg = new MedicalAlertDialog(v
                    .getContext());
                if (bean.getStatus() == FileTaskManager.TASK_STATUS_FAIL) {
                    // 失败状态，长按显示重试按钮
                    menuDlg.addButtonFirst(new MenuClickedListener() {
                        @Override
                        public void onMenuClicked() {
                            if (preJudgment()) {
                                return;
                            }
                            startTask(bean);
                        }
                    }, mContext.getString(R.string.chat_resend));
                } else {
                    menuDlg.addButtonFirst(new MenuClickedListener() {
                        @Override
                        public void onMenuClicked() {
                            if (preJudgment()) {
                                return;
                            }
                            if (callbackIf != null) {
                                callbackIf.onMsgForward(bean.getId(),
                                    bean.getSender(), bean.getType(),
                                    bean.getStatus(), null);
                            }
                        }
                    }, mContext.getString(R.string.chat_forward));
                }
                menuDlg.addButtonSecond(new MenuClickedListener() {
                    @Override
                    public void onMenuClicked() {
                        //                        MobclickAgent.onEvent(mContext,
                        //                                UmengEventConstant.EVENT_DELETE_MSG);
                        if (preJudgment()) {
                            return;
                        }
                        if (callbackIf != null) {
                            callbackIf.onMsgDelete(bean.getId(),
                                bean.getReceivedTime(), getCount());
                        }
                    }
                }, mContext.getString(R.string.chat_delete));
                addMoreItem(menuDlg, bean, 3);
                menuDlg.show();
                return true;
            }
        });

        // 单击查看
        holder.vcardBg.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onItemClickedifMultiSeclect(bean, holder)) {
                    return;
                }
                // 跳转到个人名片界面
                //                Intent intent = new Intent(v.getContext(),
                //                                        ButelContactDetailActivity.class);
                //                                intent.putExtra(ButelContactDetailActivity.KEY_NUBE_NUMBER,
                //                                        holder.nubeNumber);
                //                                ContactFriendBean currentInfo = new ContactFriendPo();
                //                                currentInfo.setNubeNumber(holder.nubeNumber);
                //                                currentInfo.setHeadUrl(holder.headUrl);
                //                                currentInfo.setNickname(holder.nickName);
                //                                currentInfo.setNumber(holder.phoneNum);
                //                                currentInfo.setSex(holder.sex);
                //                                LogUtil.d("nubeNumber=" + holder.nubeNumber + "|headUrl="
                //                                        + holder.headUrl + "|nickName=" + holder.nickName
                //                                        + "|phoneNum=" + holder.phoneNum + "|sex=" + holder.sex);
                //                                intent.putExtra(ButelContactDetailActivity.KEY_FRIEND_INFO,
                //                                        currentInfo);
                //                                v.getContext().startActivity(intent);

                Intent intent = new Intent(v.getContext(), ContactCardActivity.class);
                intent.putExtra("nubeNumber", holder.nubeNumber);
                intent.putExtra("searchType", "5"); // 5:群内添加
                mContext.startActivity(intent);
            }
        });
    }


    private void handleAudioMessage(final NoticesBean bean,
                                    final ViewHolder holder) {
        // 音频内容
        int duration = 0;
        try {
            JSONArray bodyArray = new JSONArray(bean.getBody());
            if (bodyArray != null && bodyArray.length() > 0) {
                JSONObject bodyObj = bodyArray.optJSONObject(0);
                duration = bodyObj.optInt("duration");
            }
        } catch (Exception e) {
            LogUtil.e("JSONArray Exception", e);
        }

        holder.audioDuration.setText(duration + "''");
        RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) holder.audioBg
            .getLayoutParams();

        if (duration <= 20) {
            lp.width = audioWidthS;
        } else if (duration >= 60) {
            lp.width = audioWidthL;
        } else {
            lp.width = audioWidthS + (duration - 20)
                * ((audioWidthL - audioWidthS) / 40);
        }
        holder.audioBg.setLayoutParams(lp);

        boolean isSend = isSendNotice(bean.getSender());

        if (bean.getId().equals(curPlayingAuMsgId)) {
            // 当前音频正在播放，继续显示其播放动画
            int playRes = 0;
            if (isSend) {
                holder.audioIcon.setBackgroundResource(R.drawable.audio_right_playing);
            } else {
                holder.audioIcon.setBackgroundResource(R.drawable.audio_left_playing);
            }
            final AnimationDrawable drawable = (AnimationDrawable) holder.audioIcon
                .getBackground();
            holder.audioIcon.post(new Runnable() {
                @Override
                public void run() {
                    drawable.start();
                }
            });
        } else {
            // 不在播放，显示静态图标
            int iconRes = 0;
            if (isSend) {
                iconRes = R.drawable.audio_right_icon_3;
            } else {
                iconRes = R.drawable.audio_left_icon_3;
            }
            holder.audioIcon.setBackgroundResource(iconRes);
        }

        loadHeadImage(bean, holder);
        showStatus(bean, holder);

        if (!isSend && bean.getIsRead() > 0) {
            // 还未收听的场合，显示小圆点
            holder.readStatus.setVisibility(View.VISIBLE);
        } else {
            holder.readStatus.setVisibility(View.INVISIBLE);
        }

        // 长按
        holder.audioBg.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(final View v) {
                if (onLongClickedifMultiSeclect()) {
                    return true;
                }

                //长按时停止播放其他音频
                if (bean.getId().equals(curPlayingAuMsgId)) {
                    // 当前音频正在播放，则停止播放
                    stopCurAuPlaying();
                } else {
                    if (!TextUtils.isEmpty(curPlayingAuMsgId)) {
                        // 当前其他音频正在播放，先停止播放的音频
                        stopCurAuPlaying();
                    }
                }

                // 显示 删除 菜单
                MedicalAlertDialog menuDlg = new MedicalAlertDialog(v
                    .getContext());
                if (bean.getStatus() == FileTaskManager.TASK_STATUS_FAIL) {
                    // 失败状态，长按显示重试按钮
                    menuDlg.addButtonFirst(new MenuClickedListener() {
                        @Override
                        public void onMenuClicked() {
                            startTask(bean);
                        }
                    }, mContext.getString(R.string.chat_resend));
                }

                menuDlg.addButtonSecond(new MenuClickedListener() {
                    @Override public void onMenuClicked() {
                        //显示语言播放模式 Toast
                        final RelativeLayout playModeViewGroup
                            = (RelativeLayout) ((Activity) mContext)
                            .findViewById(R.id.container_toast);
                        TextView textView = (TextView) playModeViewGroup.findViewById(R.id.slogan);
                        textView.setText("当前为听筒播放模式");
                        playModeViewGroup.setVisibility(View.VISIBLE);

                        //写 SharedPreference 数据



                        new Handler().postDelayed(new Runnable() {
                            @Override public void run() {
                                playModeViewGroup.setVisibility(GONE);
                            }
                        }, 2000);

                    }
                }, "听筒播放");
                menuDlg.addButtonThird(new MenuClickedListener() {
                    @Override
                    public void onMenuClicked() {
                        if (checkDataValid(bean.getType(), bean.getBody())) {
                            CollectionManager.getInstance()
                                .addCollectionByNoticesBean(mContext, bean);
                        }
                    }
                }, "收藏");

                menuDlg.addButtonForth(new MenuClickedListener() {
                    @Override
                    public void onMenuClicked() {

                        if (callbackIf != null) {
                            if (bean.getId().equals(curPlayingAuMsgId)) {
                                // 当前音频正在播放，则停止播放
                                stopCurAuPlaying();
                            }
                            callbackIf.onMsgDelete(bean.getId(),
                                bean.getReceivedTime(), getCount());
                        }
                    }
                }, mContext.getString(R.string.chat_delete));
                addMoreItem(menuDlg, bean, 5);
                menuDlg.show();
                return true;
            }
        });

        // 单击收听/停止
        holder.audioBg.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (CommonUtil.isFastDoubleClick()) {
                    return;
                }

                if (onItemClickedifMultiSeclect(bean, holder)) {
                    return;
                }

                // 当前音频未收听过，更新为已收听音频
                if (bean.getIsRead() > 0) {
                    noticeDao.updateAudioIsRead(bean.getId(), true);
                }

                if (bean.getId().equals(curPlayingAuMsgId)) {
                    // 当前音频正在播放，则停止播放
                    stopCurAuPlaying();
                } else {
                    if (!TextUtils.isEmpty(curPlayingAuMsgId)) {
                        // 当前其他音频正在播放，先停止播放的音频
                        stopCurAuPlaying();
                    }
                    // 开始播放点击的音频
                    startAuPlaying(bean, holder.audioIcon);
                }
            }
        });
    }


    private void loadHeadImage(NoticesBean bean, ViewHolder holder) {
        if (isSendNotice(bean.getSender())) {
            setMyselfImage(holder, bean);
        } else {
            loadUserHeadIcon(holder, bean);
        }
    }


    private void startAuPlaying(NoticesBean bean, View voiceView) {

        String localPath = "";
        try {
            JSONArray bodyArray = new JSONArray(bean.getBody());
            if (bodyArray != null && bodyArray.length() > 0) {
                JSONObject bodyObj = bodyArray.optJSONObject(0);
                localPath = bodyObj.optString("localUrl");
            }
        } catch (Exception e) {
            LogUtil.e("JSONArray Exception", e);
        }

        boolean isSend = isSendNotice(bean.getSender());

        boolean exist = false;
        if (isSend) {
            // 因为音频没有转发功能，因此，发送的音频不存在下载的问题
            if (!isValidFilePath(localPath)) {
                // 发送的消息，若本地音频不存在，则提示
                showToast(R.string.toast_no_aud);
                return;
            } else {
                exist = true;
            }
        } else {
            // 接收的音频，若本地不存在，则重新下载
            if (TextUtils.isEmpty(localPath) || localPath.endsWith(".temp")) {
                // 还未开始下载或正在下载，开始下载
                showToast(R.string.toast_downloading_aud);
                fileTaskMgr.addTask(bean.getId(), null);
                return;
            } else {
                File audFile = new File(localPath);
                if (audFile.exists()) {
                    exist = true;
                } else {
                    // 语音文件不存在，重新开始下载
                    showToast(R.string.toast_downloading_aud);
                    fileTaskMgr.addTask(bean.getId(), null);
                    return;
                }
            }
        }

        // 保存数据
        curPlayingAuMsgId = bean.getId();
        voiceView.setTag(bean);
        currentPlayVoiceView = new WeakReference<View>(voiceView);

        // 开始动画
        int playRes = 0;
        if (isSend) {
            voiceView.setBackgroundResource(R.drawable.audio_right_playing);
        } else {
            voiceView.setBackgroundResource(R.drawable.audio_left_playing);
        }

        final AnimationDrawable drawable = (AnimationDrawable) voiceView
            .getBackground();
        voiceView.post(new Runnable() {
            @Override
            public void run() {
                drawable.start();
            }
        });

        // 开始播放音频
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
        if (exist) {
            // 音频文件已存在的场合，直接开始播放，不存在的场合，等待下载成功后开始播放
            playAudio(localPath, voiceView.getContext());
        }
    }


    private void playAudio(String audioPath, Context context) {

        isPlayingAuMsg = true;
        audioMsgStateListener.AudioMsgState(isPlayingAuMsg, audioPath);

        LogUtil.d("audioPath:" + audioPath);
        try {
            AudioManagerHelper audioManagerHelper = new AudioManagerHelper();
            audioManagerHelper.enableSpeaker();

            mMediaPlayer = MediaPlayer.create(mContext.getApplicationContext(),
                Uri.fromFile(new File(audioPath)));
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                public void onCompletion(MediaPlayer mp) {
                    // 播放完成后，停止播放动画
                    stopCurAuPlaying();

                }
            });
            mMediaPlayer.start();

        } catch (Exception e) {
            LogUtil.e("Exception", e);
            showToast(R.string.toast_aud_damaged);
            stopCurAuPlaying();
            return;
        }
    }


    public void stopCurAuPlaying() {

        isPlayingAuMsg = false;
        audioMsgStateListener.AudioMsgState(false, null);

        // 停止播放
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }

        // 停止动画
        if (currentPlayVoiceView != null) {
            View playingView = currentPlayVoiceView.get();
            if (playingView != null) {
                // 停止动画
                NoticesBean msg = (NoticesBean) playingView.getTag();
                // 重置图标
                int iconRes = 0;
                if (isSendNotice(msg.getSender())) {
                    iconRes = R.drawable.audio_right_icon_3;
                } else {
                    iconRes = R.drawable.audio_left_icon_3;
                }
                playingView.setBackgroundResource(iconRes);
            }
        }

        // 初始化数据
        curPlayingAuMsgId = null;
        if (currentPlayVoiceView != null) {
            currentPlayVoiceView.clear();
            currentPlayVoiceView = null;
        }
    }


    /**
     * 解除绑定上传进度显示
     */
    private void detachUploadProgressView(TextView view, String id) {
        FileTaskProgressListener progressListener = null;
        Object tagObj = view.getTag();
        if (tagObj != null) {
            progressListener = (FileTaskProgressListener) tagObj;
            progressListener.setId(id);
        }
    }


    /**
     * 绑定上传进度显示，并开始任务
     */
    private void attachUploadProgressView(TextView view, String id) {
        FileTaskProgressListener progressListener = null;
        Object tagObj = view.getTag();
        if (tagObj != null) {
            progressListener = (FileTaskProgressListener) tagObj;
            progressListener.setId(id);
            progressListener.setAttachedView(view);
        } else {
            progressListener = new FileTaskProgressListener(view, id);
            view.setTag(progressListener);
        }
        fileTaskMgr.setChgUIInterface(id, progressListener);
    }


    public void setSharedPreferences(SharedPreferences voiceMsgSettings) {
        this.voiceMsgSettings = voiceMsgSettings;
    }


    /**
     * @ClassName: ChatListAdapter.java
     * @Description: 文件上传下载任务类
     * @author: gtzha
     * @date: 2014年11月18日
     */
    private class FileTaskProgressListener extends ChangeUIInterface {
        private WeakReference<TextView> viewReference;

        public String id;


        public void setId(String _id) {
            id = _id;
        }


        public void setAttachedView(TextView view) {
            viewReference = new WeakReference<TextView>(view);
        }


        public FileTaskProgressListener(TextView view, String _id) {
            viewReference = new WeakReference<TextView>(view);
            id = _id;
        }


        public void onStart(FileTaskBean bean) {
            // 开始文件任务
            LogUtil.d("开始文件任务 onStart：" + bean.getUuid());
        }


        public void onProcessing(FileTaskBean bean, long current, long total) {
            // 文件任务进度
            LogUtil.d("onProcessing:" + bean.getUuid() + ":" + current + "/"
                + total);
            if (current < 0 || total <= 0) {
                LogUtil.d("onProcessing:数据不合法，不做更新");
                return;
            }
            if (!changUIProgressTaskIds.contains(bean.getUuid())) {
                // 已不需要更新界面
                LogUtil.d("onProcessing:已不需要更新界面");
                return;
            }

            final float pro = current / (total * 1.0f);
            mTaskFileProgressMap.put(bean.getUuid(), pro);

            final TextView proView = viewReference.get();
            if (id.equals(bean.getUuid()) && proView != null) {
                proView.post(new Runnable() {
                    @Override
                    public void run() {
                        NumberFormat numFormat = NumberFormat
                            .getNumberInstance();
                        numFormat.setMaximumFractionDigits(2);
                        proView.setText(numFormat.format(pro * 100) + "%");
                    }
                });
            }
        }


        public void onSuccess(FileTaskBean bean, String result) {
            // 文件任务成功完成
            LogUtil.d("onSuccess:" + bean.getUuid() + ":" + result);
        }


        public void onFailure(FileTaskBean bean, Throwable error, String msg) {
            LogUtil.d("onFailure:" + bean.getUuid() + ":" + msg);
            if (MediaFile.isWebpImageFileType(bean.getSrcUrl())) {
                showToast("不支持发送webp格式的图片");
                LogUtil.d("path=" + bean.getSrcUrl());
            }
        }
    }


    /**
     * 解除绑定上传进度显示
     */
    private void detachFileUploadProgressView(ProgressBar view, String id) {
        FileTaskProgressListener progressListener = null;
        Object tagObj = view.getTag();
        if (tagObj != null) {
            progressListener = (FileTaskProgressListener) tagObj;
            progressListener.setId(id);
        }
    }


    /**
     * 绑定上传进度显示，并开始任务
     */
    private void attachFileUploadProgressView(ProgressBar view, String id) {
        FileProgressListener progressListener = null;
        Object tagObj = view.getTag();
        if (tagObj != null) {
            progressListener = (FileProgressListener) tagObj;
            progressListener.setId(id);
            progressListener.setAttachedView(view);
        } else {
            progressListener = new FileProgressListener(view, id);
            view.setTag(progressListener);
        }
        fileTaskMgr.setChgUIInterface(id, progressListener);
    }


    /**
     * @ClassName: ChatListAdapter.java
     * @Description: 文件上传下载任务类
     * @author: gtzha
     * @date: 2014年11月18日
     */
    private class FileProgressListener extends ChangeUIInterface {
        private WeakReference<ProgressBar> viewReference;

        public String id;


        public void setId(String _id) {
            id = _id;
        }


        public void setAttachedView(ProgressBar view) {
            viewReference = new WeakReference<ProgressBar>(view);
        }


        public FileProgressListener(ProgressBar view, String _id) {
            viewReference = new WeakReference<ProgressBar>(view);
            id = _id;
        }


        public void onStart(FileTaskBean bean) {
            // 开始文件任务
            LogUtil.d("开始文件任务 onStart：" + bean.getUuid());
        }


        public void onProcessing(FileTaskBean bean, long current, long total) {
            // 文件任务进度
            LogUtil.d("onProcessing:" + bean.getUuid() + ":" + current + "/"
                + total);
            if (current < 0 || total <= 0) {
                LogUtil.d("onProcessing:数据不合法，不做更新");
                return;
            }
            if (!changUIProgressTaskIds.contains(bean.getUuid())) {
                // 已不需要更新界面
                LogUtil.d("onProcessing:已不需要更新界面");
                return;
            }

            final float pro = current / (total * 1.0f);
            mTaskFileProgressMap.put(bean.getUuid(), pro);

            final ProgressBar proView = viewReference.get();
            if (id.equals(bean.getUuid()) && proView != null) {
                proView.post(new Runnable() {
                    @Override
                    public void run() {
                        int duration = (int) (pro * 100);
                        proView.setProgress(duration);
                    }
                });
            }
        }


        public void onSuccess(FileTaskBean bean, String result) {
            // 文件任务成功完成
            LogUtil.d("onSuccess:" + bean.getUuid() + ":" + result);
        }


        public void onFailure(FileTaskBean bean, Throwable error, String msg) {
            LogUtil.d("onFailure:" + bean.getUuid() + ":" + msg);
            if (MediaFile.isWebpImageFileType(bean.getSrcUrl())) {
                showToast("不支持发送webp格式的图片");
                LogUtil.d("path=" + bean.getSrcUrl());
            }
        }
    }


    /**
     * 是否是发送的消息
     */
    public boolean isSendNotice(String noticeSender) {
        return selfNubeNumber.equals(noticeSender);
    }


    public static class ViewHolder {
        // 时间
        TextView timeTv;
        // 状态（正在发送，重发按钮）
        ImageButton retryBtn;
        ProgressBar runningPb;

        // 文字
        EmojiconTextView noticeTxt;
        // TextView noticeTxt;
        TextView noticeAddTxt;
        // 显示链接相关的view
        LinearLayout layout;
        View txtView;
        TextView txt_msg;
        View linkView;

        // 图片，视频
        FrameLayout imgLayout;
        ImageView imgIv;
        LinearLayout progressLine;
        TextView progressTxt;
        ImageView imgFrameIv;
        ImageView imgIvMask;
        TextView durationTxt;

        // 名片
        RelativeLayout vcardBg;
        ImageView vcardHeadIv;
        TextView vcardNameTxt;
        TextView vcardDetail;
        TextView vcardNubeTxt;
        // 接收端跳转到联系人页面避免再次查询
        String nubeNumber = "";
        String phoneNum = "";
        String nickName = "";
        String headUrl = "";
        String sex = "";

        // 音频
        RelativeLayout audioBg;
        ImageView audioIcon;
        TextView audioDuration;
        ImageView readStatus;

        SharePressableImageView contactIcon;
        TextView contactName;
        //文件
        ProgressBar mProgressBar;
        //多选的checkbox
        RelativeLayout checkLayout;
        CheckBox checkbox;
    }


    /**
     * 时间显示样式
     */
    private String getDispTimestamp(long dbTime) {

        String dateStr = DateUtil.formatMs2String(dbTime,
            DateUtil.FORMAT_YYYY_MM_DD_HH_MM);

        CustomLog.d(ACTIVITY_FLAG, "IM 消息时间戳：" + dateStr);

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(dbTime);

        Calendar nowCal = Calendar.getInstance();

        if (cal.get(Calendar.YEAR) < nowCal.get(Calendar.YEAR)) {
            // 跨年了，显示全部（2014-11-15 14:11）
            return dateStr;
        } else {
            int dayInterval = DateUtil.realDateIntervalDay(cal.getTime(),
                nowCal.getTime());
            if (dayInterval == 0) {
                // 当天（14:11）
                return dateStr.substring(11, 16);
            } else if (dayInterval == 1) {
                return "昨天 " + dateStr.substring(11, 16);
            } else {
                // 显示月份（11-15 14:11）
                return dateStr.substring(5, 16);
            }
        }
    }


    private static final int CURSOR_COL_TYPE_STRING = 1;
    private static final int CURSOR_COL_TYPE_INT = 2;
    private static final int CURSOR_COL_TYPE_LONG = 3;


    private Object getCursorDataByCol(Cursor cursor, String column, int type) {
        if (cursor != null && !cursor.isClosed()) {
            try {
                switch (type) {
                    case CURSOR_COL_TYPE_STRING:
                        return cursor.getString(cursor
                            .getColumnIndexOrThrow(column));
                    case CURSOR_COL_TYPE_INT:
                        return cursor.getInt(cursor.getColumnIndexOrThrow(column));
                    case CURSOR_COL_TYPE_LONG:
                        return cursor.getLong(cursor.getColumnIndexOrThrow(column));
                }
            } catch (Exception e) {
                LogUtil.e("getCursorDataByCol Exception", e);
            }
        }
        return "0";
    }


    /**
     * 判断时间间隔是否足够近（5分钟之内）
     */
    private boolean isCloseEnough(long dateFrom, long dateTo) {
        Calendar fromCal = Calendar.getInstance();
        fromCal.setTimeInMillis(dateFrom);
        fromCal.set(Calendar.SECOND, 0);
        fromCal.set(Calendar.MILLISECOND, 0);

        Calendar toCal = Calendar.getInstance();
        toCal.setTimeInMillis(dateTo);
        toCal.set(Calendar.SECOND, 0);
        toCal.set(Calendar.MILLISECOND, 0);

        fromCal.add(Calendar.MINUTE, 5);

        if (fromCal.compareTo(toCal) >= 0) {
            // 5分钟之内
            return true;
        }
        return false;
    }


    /**
     * 显示图片大小计算
     */
    private int[] caculateImgSize(int picWidth, int picHeight) {
        int targetWidth = picDefaultSize;
        int targetHeight = picDefaultSize;
        if (picWidth > 0 && picHeight > 0) {
            if (picHeight >= picWidth) {
                // 竖图，先将高度限定到最大高度
                targetHeight = picMaxSize;

                // 等比例计算宽度
                targetWidth = picWidth * targetHeight / picHeight;

                // 限定到最大最小宽度之间
                if (targetWidth > picMaxSize) {
                    targetWidth = picMaxSize;
                } else if (targetWidth < picMinSize) {
                    targetWidth = picMinSize;
                }
            } else {
                // 横图，先将宽度限定到最大宽度
                targetWidth = picMaxSize;

                // 等比例计算高度
                targetHeight = picHeight * targetWidth / picWidth;

                // 限定到最大最小高度之间
                if (targetHeight > picMaxSize) {
                    targetHeight = picMaxSize;
                } else if (targetHeight < picMinSize) {
                    targetHeight = picMinSize;
                }
            }
        }

        return new int[] { targetWidth, targetHeight };
    }


    /**
     * 显示图片，发送的消息，优先显示本地图片，其他都显示缩略图
     */
    private void showImage(boolean isSend, boolean isVideo,
                           String localPath, String thumbUrl, String remoteUrl,
                           ImageView imageView, int width, int height) {

        //        imageView.setImageBitmap(null);
        //        imageView.setBackgroundColor(Color.TRANSPARENT);

        if (isSend) {
            // 发送的消息，优先显示本地图片
            if (isValidFilePath(localPath)) {
                if (isVideo) {
                    Glide.with(mContext).load(localPath)
                        .placeholder(R.drawable.chat_empty_img)
                        .error(R.drawable.chat_empty_img).centerCrop()
                        .crossFade().into(imageView);
                } else {
                    Glide.with(mContext).load(localPath)
                        .placeholder(R.drawable.chat_empty_img)
                        .error(R.drawable.chat_empty_img).centerCrop()
                        .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                        .crossFade().into(imageView);
                }
            } else if (!TextUtils.isEmpty(thumbUrl)) {
                Glide.with(mContext).load(thumbUrl)
                    .placeholder(R.drawable.chat_empty_img)
                    .error(R.drawable.chat_empty_img).centerCrop()
                    .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                    .crossFade().into(imageView);
            } else {
                Glide.with(mContext).load(remoteUrl)
                    .placeholder(R.drawable.chat_empty_img)
                    .error(R.drawable.chat_empty_img).centerCrop().crossFade()
                    .into(imageView);
            }
        } else {
            if (!TextUtils.isEmpty(thumbUrl)) {
                Glide.with(mContext).load(thumbUrl)
                    .placeholder(R.drawable.chat_empty_img)
                    .error(R.drawable.chat_empty_img).centerCrop()
                    .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                    .crossFade().into(imageView);
            } else {
                Glide.with(mContext).load(remoteUrl)
                    .placeholder(R.drawable.chat_empty_img)
                    .error(R.drawable.chat_empty_img).centerCrop().crossFade()
                    .into(imageView);
            }
        }
    }


    /**
     * 显示时长
     */
    private String getDispDuration(int duration) {
        if (duration == 0) {
            return "";
        }

        int minute = duration / 60;
        int second = duration % 60;

        String result = "";
        if (minute < 10) {
            result += "0";
        }
        result += minute + ":";
        if (second < 10) {
            result += "0";
        }
        result += second;

        return result;
    }


    public void onStop() {
        // 停止播放音频
        stopCurAuPlaying();
    }


    public interface CallbackInterface {
        public void onMsgDelete(String uuid, long receivedTime, int dataCnt);

        public void onMsgForward(String uuid, String sender, int msgType,
                                 int msgStatus, String localPath);

        public void onSetSelectMemeber(String name, String nube);

        public void onMoreClick(String uuid, int msgType, int msgStatus, boolean checked);
    }


    private boolean isValidFilePath(String filePath) {
        if (TextUtils.isEmpty(filePath)) {
            return false;
        }
        File file = new File(filePath);
        if (!file.exists()) {
            return false;
        }
        if (file.length() == 0) {
            file.delete();
            return false;
        }
        if (filePath.endsWith(".temp")) {
            return false;
        }
        return true;
    }


    // 加载自己头像
    private void setMyselfImage(final ViewHolder holder, final NoticesBean bean) {
        LogUtil.d("setMyselfImage: noticeType" + noticeType + "  targetnumber:"
            + targetNumber + "  Body:" + bean.getBody());
        if (!TextUtils.isEmpty(headUrl)) {
            Glide.with(mContext).load(headUrl)
                .placeholder(IMCommonUtil.getHeadIdBySex(bean.getSex()))
                .error(IMCommonUtil.getHeadIdBySex(bean.getSex()))
                .centerCrop().diskCacheStrategy(DiskCacheStrategy.SOURCE)
                .crossFade().into(holder.contactIcon.shareImageview);
        } else {
            holder.contactIcon.shareImageview
                .setImageResource(userDefaultHeadUrl);
        }
    }


    // 加载聊天成员头像，群聊情况下加载名称
    private void loadUserHeadIcon(final ViewHolder holder,
                                  final NoticesBean bean) {
        LogUtil.d("loadUserHeadIcon: noticeType" + noticeType
            + "  targetnumber:" + targetNumber + "  Body:" + bean.getBody());
        if (noticeType == ChatActivity.VALUE_CONVERSATION_TYPE_MULTI
            || targetNumber.length() > 12) {
            NameElement element = ShowNameUtil.getNameElement(
                bean.getMemberName(), bean.getmNickName(),
                bean.getmPhone(), bean.getSender());
            final String MName = ShowNameUtil.getShowName(element);
            holder.contactName.setText(MName);
            holder.contactName.setVisibility(View.VISIBLE);
            holder.contactIcon.pressableTextview
                .setOnLongClickListener(new OnLongClickListener() {

                    @Override
                    public boolean onLongClick(View v) {
                        callbackIf.onSetSelectMemeber(MName,
                            bean.getSender());
                        return true;
                    }
                });
        } else {
            holder.contactName.setVisibility(GONE);
        }

        if (bean.getSender().equals(butelPubNubeNum)) {
            // 官方头像
            holder.contactIcon.shareImageview
                .setImageResource(R.drawable.system_icon);
        } else {
            String userUrl = bean.getHeadUrl();
            if (TextUtils.isEmpty(userUrl)) {
                holder.contactIcon.shareImageview.setImageResource(IMCommonUtil
                    .getHeadIdBySex(bean.getSex()));
            } else {
                Glide.with(mContext).load(userUrl)
                    .placeholder(IMCommonUtil.getHeadIdBySex(bean.getSex()))
                    .error(IMCommonUtil.getHeadIdBySex(bean.getSex()))
                    .centerCrop()
                    .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                    .crossFade().into(holder.contactIcon.shareImageview);
            }
        }

        final String nubNumber = bean.getSender();
        holder.contactIcon.pressableTextview.setVisibility(View.VISIBLE);
        holder.contactIcon.pressableTextview
            .setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    if (CommonUtil.isFastDoubleClick()
                        || bean.getSender().equals(butelPubNubeNum)) {
                        return;
                    }
                    Intent intent = new Intent(v.getContext(), ContactCardActivity.class);

                    intent.putExtra("nubeNumber", nubNumber);
                    intent.putExtra("searchType", "5"); // 5:群内添加
                    //                    intent.putExtra(ContactCardActivity.KEY_FROM_IM, true);
                    mContext.startActivity(intent);
                }
            });
    }


    /**
     * 发送文字/
     */
    private void showStatus(NoticesBean bean, ViewHolder holder) {
        if (isSendNotice(bean.getSender())) {
            // 发送的消息才有状态
            switch (bean.getStatus()) {
                case FileTaskManager.TASK_STATUS_SUCCESS:
                    holder.retryBtn.setVisibility(GONE);
                    holder.runningPb.setVisibility(GONE);
                    break;
                case FileTaskManager.TASK_STATUS_READY:
                case FileTaskManager.TASK_STATUS_RUNNING:
                case FileTaskManager.TASK_STATUS_COMPRESSING:
                    holder.retryBtn.setVisibility(GONE);
                    holder.runningPb.setVisibility(GONE);
                    break;
                case FileTaskManager.TASK_STATUS_FAIL:
                    // 重发按钮
                    holder.retryBtn.setVisibility(View.VISIBLE);
                    holder.runningPb.setVisibility(GONE);
                    break;
            }
        } else {
            holder.retryBtn.setVisibility(GONE);
            holder.runningPb.setVisibility(GONE);
        }
    }


    private boolean preJudgment() {
        // if (isFriend()&& !butelPubNubeNum.equals(targetNumber)) {
        // showToast(targetShortName + "还不是您的好友，快去添加好友吧");
        // return true;
        // }
        return false;
    }


    private void showToast(int toastId) {
        showToast(mContext.getString(toastId));
    }


    private void showToast(String toast) {
        Toast.makeText(mContext, toast, Toast.LENGTH_SHORT).show();
        LogUtil.d("toast");
    }


    private void onTextLongClick(final View v, final NoticesBean bean,
                                 final boolean hasUrl, final ViewHolder holder) {
        v.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View arg0) {
                if (onLongClickedifMultiSeclect()) {
                    return true;
                }
                MedicalAlertDialog menuDlg = new MedicalAlertDialog(v
                    .getContext());
                if (bean.getStatus() == FileTaskManager.TASK_STATUS_FAIL) {
                    // 失败状态，长按显示重试按钮
                    menuDlg.addButtonFirst(new MenuClickedListener() {
                        @Override
                        public void onMenuClicked() {
                            startTask(bean);
                        }
                    }, mContext.getString(R.string.chat_resend));

                    menuDlg.addButtonSecond(new MenuClickedListener() {
                        @Override
                        public void onMenuClicked() {
                        }
                    }, mContext.getString(R.string.chat_copy));

                    menuDlg.addButtonThird(new MenuClickedListener() {
                        @Override
                        public void onMenuClicked() {

                        }
                    }, mContext.getString(R.string.chat_forward));

                    menuDlg.addButtonForth(new MenuClickedListener() {
                        @Override
                        public void onMenuClicked() {
                            if (hasUrl) {
                                bean.setType(FileTaskManager.NOTICE_TYPE_URL);
                            }
                            CollectionManager.getInstance()
                                .addCollectionByNoticesBean(mContext, bean);
                        }
                    }, "收藏");

                    menuDlg.addButtonFive(new MenuClickedListener() {
                        @Override
                        public void onMenuClicked() {
                            //                            MobclickAgent.onEvent(mContext,
                            //                                    UmengEventConstant.EVENT_DELETE_MSG);

                            if (callbackIf != null) {
                                callbackIf.onMsgDelete(bean.getId(),
                                    bean.getReceivedTime(), getCount());
                            }
                        }
                    }, mContext.getString(R.string.chat_delete));

                    // addMoreItem(menuDlg, bean, 6);

                } else {
                    menuDlg.addButtonFirst(new MenuClickedListener() {
                        @Override
                        public void onMenuClicked() {
                            if (callbackIf != null) {
                                callbackIf.onMsgForward(bean.getId(),
                                    bean.getSender(), bean.getType(),
                                    bean.getStatus(), null);
                            }
                        }
                    }, mContext.getString(R.string.chat_forward));
                    menuDlg.addButtonSecond(new MenuClickedListener() {
                        @Override
                        public void onMenuClicked() {
                            //                            MobclickAgent.onEvent(mContext,
                            //                                    UmengEventConstant.EVENT_COPY_MSG);

                            // 取出链接所在消息的文本内容-->复制
                            String text = "";
                            try {
                                JSONArray bodyArray = new JSONArray(bean
                                    .getBody());
                                if (bodyArray != null && bodyArray.length() > 0) {
                                    JSONObject bodyObj = bodyArray
                                        .optJSONObject(0);
                                    text = bodyObj.optString("txt");
                                    if (!TextUtils.isEmpty(text)
                                        && text.length() > 3000) {
                                        text = text.substring(0, 3000);
                                        text = text + "...";
                                    }
                                }

                            } catch (Exception e) {
                                LogUtil.e("Exception:", e);
                            }
                            IMCommonUtil.copy2Clipboard(v.getContext(), text);
                            showToast(R.string.toast_copy_ok);
                        }
                    }, mContext.getString(R.string.chat_copy));

                    menuDlg.addButtonThird(new MenuClickedListener() {
                        @Override
                        public void onMenuClicked() {
                            if (hasUrl) {
                                bean.setType(FileTaskManager.NOTICE_TYPE_URL);
                            }
                            CollectionManager.getInstance()
                                .addCollectionByNoticesBean(mContext, bean);
                        }
                    }, "收藏");

                    menuDlg.addButtonForth(new MenuClickedListener() {
                        @Override
                        public void onMenuClicked() {
                            //                            MobclickAgent.onEvent(mContext,
                            //                                    UmengEventConstant.EVENT_DELETE_MSG);

                            if (callbackIf != null) {
                                callbackIf.onMsgDelete(bean.getId(),
                                    bean.getReceivedTime(), getCount());
                            }
                        }
                    }, mContext.getString(R.string.chat_delete));

                    addMoreItem(menuDlg, bean, 5);
                }
                menuDlg.show();

                return true;
            }
        });
        v.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (onItemClickedifMultiSeclect(bean, holder)) {
                    return;
                }

            }
        });
    }


    private HashSet<String> uuidList = new HashSet<String>();
    private LinkedHashMap<String, NoticesBean> uuidMap = new LinkedHashMap <String, NoticesBean>();


    private void updateCheckedData(String uuid, boolean checked, NoticesBean bean) {
        if (uuidList != null) {
            if (checked) {
                uuidList.add(uuid);
                uuidMap.put(uuid, bean);
            } else {
                uuidList.remove(uuid);
                uuidMap.remove(uuid);
            }
        }
    }


    private boolean hasChecked(String uuid) {
        if (!TextUtils.isEmpty(uuid)) {
            if (uuidList != null && uuidList.contains(uuid)) {
                return true;
            }
        }
        return false;
    }


    public HashSet<String> getCheckedData() {
        return uuidList;
    }


    public LinkedHashMap<String, NoticesBean> getCheckedDataMap() {
        return uuidMap;
    }


    public boolean hasCheckedData() {
        if (uuidList != null && uuidList.size() > 0) {
            return true;
        }
        return false;
    }


    public void cleanCheckedData() {
        if (uuidList != null) {
            uuidList.clear();
        }
        if (uuidMap != null) {
            uuidMap.clear();
        }
    }


    private boolean onLongClickedifMultiSeclect() {
        if (bMultiCheckMode) {
            return true;
        }
        return false;
    }


    private boolean onItemClickedifMultiSeclect(NoticesBean bean, ViewHolder holder) {
        if (bMultiCheckMode) {
            if (bean != null && holder != null && holder.checkbox != null) {
                boolean status = holder.checkbox.isChecked();
                holder.checkbox.setChecked(!status);
                return true;
            }
        }
        return false;
    }


    private void onCheckBoxChecked(NoticesBean bean, boolean checked) {
        if (bean != null) {
            if (callbackIf != null) {
                updateCheckedData(bean.getId(), checked, bean);
                callbackIf.onMoreClick(bean.getId(), bean.getType(), bean.getStatus(), checked);
            }
        }
    }


    private void addMoreItem(MedicalAlertDialog menuDlg, final NoticesBean bean, int index) {
        if (menuDlg != null && bean != null) {
            MenuClickedListener listener = new MenuClickedListener() {
                @Override
                public void onMenuClicked() {

                    if (callbackIf != null) {
                        updateCheckedData(bean.getId(), true, bean);
                        callbackIf.onMoreClick(bean.getId(), bean.getType(), bean.getStatus(),
                            true);

                    }
                }
            };

            switch (index) {
                case 1:
                    menuDlg.addButtonFirst(listener, "更多");
                    break;
                case 2:
                    menuDlg.addButtonSecond(listener, "更多");
                    break;
                case 3:
                    menuDlg.addButtonThird(listener, "更多");
                    break;
                case 4:
                    menuDlg.addButtonForth(listener, "更多");
                    break;
                case 5:
                    menuDlg.addButtonFive(listener, "更多");
                    break;
                case 6:
                    menuDlg.addButtonSix(listener, "更多");
                    break;
                default:

            }
        }
    }


    public void setAudioMsgStateListener(AudioMsgStateListener audioMsgStateListener) {
        this.audioMsgStateListener = audioMsgStateListener;
    }


    /**
     * 音频消息播放状态监听器
     */
    public interface AudioMsgStateListener {
        void AudioMsgState(boolean isPlaying, String path);
    }
}
