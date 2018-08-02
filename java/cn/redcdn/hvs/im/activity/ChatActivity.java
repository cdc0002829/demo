package cn.redcdn.hvs.im.activity;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ImageSpan;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import cn.redcdn.datacenter.medicalcenter.MDSAppSearchUsers;
import cn.redcdn.datacenter.medicalcenter.data.MDSDetailInfo;
import cn.redcdn.hvs.AccountManager;
import cn.redcdn.hvs.HomeActivity;
import cn.redcdn.hvs.MedicalApplication;
import cn.redcdn.hvs.R;
import cn.redcdn.hvs.base.BaseActivity;
import cn.redcdn.hvs.contacts.contact.StringHelper;
import cn.redcdn.hvs.contacts.contact.interfaces.Contact;
import cn.redcdn.hvs.contacts.contact.interfaces.ContactCallback;
import cn.redcdn.hvs.contacts.contact.interfaces.ResponseEntry;
import cn.redcdn.hvs.contacts.contact.manager.ContactManager;
import cn.redcdn.hvs.im.IMConstant;
import cn.redcdn.hvs.im.adapter.ChatListAdapter;
import cn.redcdn.hvs.im.bean.ButelPAVExInfo;
import cn.redcdn.hvs.im.bean.ButelVcardBean;
import cn.redcdn.hvs.im.bean.ContactFriendBean;
import cn.redcdn.hvs.im.bean.GroupMemberBean;
import cn.redcdn.hvs.im.bean.NoticesBean;
import cn.redcdn.hvs.im.bean.ShowNameUtil;
import cn.redcdn.hvs.im.bean.ShowNameUtil.NameElement;
import cn.redcdn.hvs.im.bean.ThreadsBean;
import cn.redcdn.hvs.im.column.GroupMemberTable;
import cn.redcdn.hvs.im.column.NoticesTable;
import cn.redcdn.hvs.im.common.CommonWaitDialog;
import cn.redcdn.hvs.im.common.ThreadPoolManger;
import cn.redcdn.hvs.im.dao.GroupDao;
import cn.redcdn.hvs.im.dao.NetPhoneDaoImpl;
import cn.redcdn.hvs.im.dao.NoticesDao;
import cn.redcdn.hvs.im.dao.ThreadsDao;
import cn.redcdn.hvs.im.fileTask.FileTaskManager;
import cn.redcdn.hvs.im.manager.CollectionManager;
import cn.redcdn.hvs.im.manager.GroupChatInterfaceManager;
import cn.redcdn.hvs.im.preference.DaoPreference;
import cn.redcdn.hvs.im.preference.DaoPreference.PrefType;
import cn.redcdn.hvs.im.provider.ProviderConstant;
import cn.redcdn.hvs.im.task.AsyncTasks;
import cn.redcdn.hvs.im.task.QueryConvstNoticeAsyncTask;
import cn.redcdn.hvs.im.util.AudioManagerHelper;
import cn.redcdn.hvs.im.util.IMCommonUtil;
import cn.redcdn.hvs.im.util.ListSort;
import cn.redcdn.hvs.im.util.PinyinUtil;
import cn.redcdn.hvs.im.util.SendCIVMUtil;
import cn.redcdn.hvs.im.util.WakeLockHelper;
import cn.redcdn.hvs.im.view.BottomMenuWindow;
import cn.redcdn.hvs.im.view.CommonDialog;
import cn.redcdn.hvs.im.view.MedicalAlertDialog;
import cn.redcdn.hvs.profiles.activity.CollectionActivity;
import cn.redcdn.hvs.util.CommonUtil;
import cn.redcdn.hvs.util.CustomToast;
import cn.redcdn.hvs.util.NotificationUtil;
import cn.redcdn.hvs.util.StringUtil;
import cn.redcdn.hvs.util.TitleBar;
import cn.redcdn.log.CustomLog;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import org.json.JSONObject;

import static android.hardware.Sensor.TYPE_PROXIMITY;
import static android.view.View.GONE;
import static cn.redcdn.hvs.MedicalApplication.getFileTaskManager;
import static cn.redcdn.hvs.im.activity.GroupChatDetailActivity.KEY_NUMBER;

/**
 * Desc    聊天页面  单聊、群聊
 * Created by wangkai on 2017/2/24.
 */

public class ChatActivity extends BaseActivity implements ChatListAdapter.CallbackInterface,
        GroupChatInterfaceManager.GroupInterfaceListener,
        SensorEventListener,
        ChatListAdapter.AudioMsgStateListener {
    // 群聊ID
    public static final String KEY_GROUP_ID = "KEY_GROUP_ID";
    // 群聊名称
    public static final String KEY_DROUP_NAME = "KEY_DROUP_NAME";
    // 消息转发，改走 本地照片分享逻辑
    public static final int ACTION_FORWARD_NOTICE = 1105;

    //用户来源是陌生人，用户来源 0:视讯号搜索，1:手机通讯录好友推荐，2:手机号搜索， 3:邮箱搜索,
    //  4:二维码扫描,  5:群内添加,  6:陌生人聊天添加
    private static final int ADD_USER_TYPE = 6;

    /**
     * 消息界面类型
     */
    public static final String KEY_NOTICE_FRAME_TYPE = "key_notice_frame_type";
    /**
     * 新消息界面
     */
    public static final int VALUE_NOTICE_FRAME_TYPE_NEW = 1;
    /**
     * 聊天列表界面
     */
    public static final int VALUE_NOTICE_FRAME_TYPE_LIST = 2;
    /**
     * 通过nube好友发送消息
     */
    public static final int VALUE_NOTICE_FRAME_TYPE_NUBE = 3;

    /**
     * 聊天类型
     */
    public static final String KEY_CONVERSATION_TYPE = "key_conversation_type";
    /**
     * 单人聊天
     */
    public static final int VALUE_CONVERSATION_TYPE_SINGLE = 1;
    /**
     * 群发
     */
    public static final int VALUE_CONVERSATION_TYPE_MULTI = 2;

    /**
     * 会话ID
     */
    public static final String KEY_CONVERSATION_ID = "key_conversation_id";
    /**
     * 会话扩展信息
     */
    public static final String KEY_CONVERSATION_EXT = "key_conversation_ext";
    /**
     * 会话对象视讯号
     */
    public static final String KEY_CONVERSATION_NUBES = "key_conversation_nubes";
    /**
     * 聊天对象名称
     */
    public static final String KEY_CONVERSATION_SHORTNAME = "key_conversation_shortname";
    public static final String KEY_ADD_FRIEND_TYPE = "key_add_friend_type";
    /**
     * 聊天页面返回标记
     */
    public static final String KEY_CHAT_BACK_FLAG = "key_chat_back_flag";
    private static final String VOICE_PREFS_NAME = "VoicePrefsFile";
    /**
     * 添加为好友
     */

    // 界面类型(单聊，群聊)
    private int frameType = VALUE_NOTICE_FRAME_TYPE_NEW;
    // 当前会话ID(单聊,群聊)
    private String convstId = "";
    // 聊天类型(单聊，群聊)
    private int conversationType = VALUE_CONVERSATION_TYPE_SINGLE;

    //系统 Nube
    private static final String SYS_NUBE = "10000";
    // 聊天对象视讯号
    private String targetNubeNumber = "";
    // 聊天对象名称(单聊)
    private String targetShortName = "";
    // 会话扩展信息(单聊，群聊)
    private String convstExtInfo = "";

    // 列表listview
    private ListView noticeListView = null;
    //    // 发送人显示框
//    private EditText receiverInput = null;
//    // 发送人显示框
//    private TextView receiverInputFocus = null;
//    // 收件人区域
//    private RelativeLayout receiversLine = null;
    // 列表适配器
    private ChatListAdapter chatAdapter = null;
    // Load ImageData
    private View headerLoadingView = null;
    private View headerRoot = null;

    // // 上一次查询的时间
    // private long lastQueryTime = 0l;

    private Handler mHandler = new Handler();
    // 自身视讯号
    private String selfNubeNumber = "";
    // 数据变更监听
    private MyContentObserver observer = null;
    // 系统camera拍照或拍视频文件路径
    private String cameraFilePath = "";
    // 待转发的消息ID
    private String forwardNoticeId = null;
    // 官方帐号
//    private String butelPubNubeNum = "";
    // 添加好友的nube号码
    private String addFriendNube = "";
    // 添加好友task
//    private AsyncTasks addLinkmanTask = null;

    // 输入区域
    private ChatInputFragment inputFragment = new ChatInputFragment();

    // 收件人视频号码
    public ArrayList<String> receiverNumberLst = new ArrayList<String>();
    // 收件人名称
    public Map<String, String> receiverNameMap = new HashMap<String, String>();

    // 是否选择联系人
    private boolean isSelectReceiver = false;
    // 创建群界面是否要保留界面
    private boolean isSaveDraft = true;

    // 全部消息的起始时间
    private long recvTimeBegin = 0l;
    private Object recvTimeLock = new Object();
    // 群组id
    private String groupId = "";
    // 群组表监听器
    private GroupMemberObserver observeGroupMember;
    // 群成员表监听器
    private GroupObserver groupObserve;
    // 存放群成员人数
    private int groupMemberSize = 0;
    // 是否是群成员
    private boolean isGroupMember = false;
    private ArrayList<String> selectNubeList = new ArrayList<String>();
    // 收件人名称
    public ArrayList<String> selectNameList = new ArrayList<String>();
    private LinkedHashMap<String, GroupMemberBean> dateList
            = new LinkedHashMap<String, GroupMemberBean>();// 显示数据
    private static final String TAG = ChatActivity.class.getName();
    protected CommonWaitDialog waitDialog;
    // 页面返回标记
    private boolean back_flag = true;
    // 软件盘是否弹起的判断标记
    private boolean SoftInput = false;
    // 第一次进入页面标记
    private boolean firstFlag = true;

    private NoticesDao noticeDao = null;
    private ThreadsDao threadDao = null;
    private GroupDao groupDao = null;

    //多选操作模式下的相关widget和数据对象
    private RelativeLayout moreOpLayout = null;
    private ImageButton forwardBtn = null;
    private ImageButton collectBtn = null;
    private ImageButton delBtn = null;

    private Context mContext = this;
    private MDSDetailInfo currentInfo;
    private TextView newNoticeNum;

    private TitleBar titleBar;
    private boolean titlebackbtn = false;
    private Button backbtn;
    private TextView backtext;
    private LinearLayout chatlayout;

    //修改后的群名称
    // private String nameForModify = "";
    private Boolean newNoticeNumflag = false;

    private SensorManager sensorManager;
    private Sensor proximitySensor;
    private boolean isPlaying = false;

    private WakeLockHelper helper;
    private AudioManagerHelper audioHelper;
    private String audioPath;
    SharedPreferences voiceMsgSettings;
    private boolean gidExist = true;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chat_layout);

        noticeDao = new NoticesDao(this);
        threadDao = new ThreadsDao(this);
        groupDao = new GroupDao(this);
        headerLoadingView = getLayoutInflater().inflate(
                R.layout.page_load_header, null);
        headerRoot = headerLoadingView.findViewById(R.id.header_root);
        //        butelPubNubeNum = NetPhoneApplication.getPreference().getKeyValue(
        //                PrefType.KEY_BUTEL_PUBLIC_NO, "");
        selfNubeNumber = AccountManager.getInstance(this).getAccountInfo().nube;
        //        initReceive();
        helper = new WakeLockHelper();
        audioHelper = new AudioManagerHelper();
        // 消息界面类型
        if (savedInstanceState == null) {
            frameType = getIntent().getIntExtra(KEY_NOTICE_FRAME_TYPE,
                    VALUE_NOTICE_FRAME_TYPE_NEW);
            back_flag = getIntent().getBooleanExtra(KEY_CHAT_BACK_FLAG, true);
            if (frameType == VALUE_NOTICE_FRAME_TYPE_LIST) {
                convstId = getIntent().getStringExtra(KEY_CONVERSATION_ID);
                conversationType = getIntent().getIntExtra(
                        KEY_CONVERSATION_TYPE, VALUE_CONVERSATION_TYPE_SINGLE);
                targetNubeNumber = getIntent().getStringExtra(
                        KEY_CONVERSATION_NUBES);
                groupId = convstId;
                targetShortName = getIntent().getStringExtra(
                        KEY_CONVERSATION_SHORTNAME);
                convstExtInfo = getIntent()
                        .getStringExtra(KEY_CONVERSATION_EXT);
            } else if (frameType == VALUE_NOTICE_FRAME_TYPE_NUBE) {
                targetNubeNumber = getIntent().getStringExtra(
                        KEY_CONVERSATION_NUBES);
                targetShortName = getIntent().getStringExtra(
                        KEY_CONVERSATION_SHORTNAME);
            }
        } else {
            Bundle paramsBundle = savedInstanceState.getBundle("params");
            if (paramsBundle != null) {
                cameraFilePath = paramsBundle.getString("cameraFilePath");
                forwardNoticeId = paramsBundle.getString("forwardNoticeId");
                addFriendNube = paramsBundle.getString("addFriendNube");
                receiverNumberLst = paramsBundle
                        .getStringArrayList("receiverNumberLst");
                receiverNameMap = (Map<String, String>) paramsBundle
                        .getSerializable("receiverNameMap");
                selectNameList = paramsBundle
                        .getStringArrayList("selectNameList");
                selectNubeList = paramsBundle
                        .getStringArrayList("selectNubeList");
                groupId = paramsBundle.getString(KEY_GROUP_ID);
                frameType = paramsBundle.getInt(KEY_NOTICE_FRAME_TYPE,
                        VALUE_NOTICE_FRAME_TYPE_NEW);
                if (frameType == VALUE_NOTICE_FRAME_TYPE_LIST) {
                    convstId = paramsBundle.getString(KEY_CONVERSATION_ID);
                    conversationType = paramsBundle.getInt(
                            KEY_CONVERSATION_TYPE,
                            VALUE_CONVERSATION_TYPE_SINGLE);
                    groupId = paramsBundle.getString(KEY_GROUP_ID);
                    targetNubeNumber = paramsBundle
                            .getString(KEY_CONVERSATION_NUBES);
                    targetShortName = paramsBundle
                            .getString(KEY_CONVERSATION_SHORTNAME);
                    convstExtInfo = paramsBundle
                            .getString(KEY_CONVERSATION_EXT);
                } else if (frameType == VALUE_NOTICE_FRAME_TYPE_NUBE) {
                    targetNubeNumber = paramsBundle
                            .getString(KEY_CONVERSATION_NUBES);
                    targetShortName = paramsBundle
                            .getString(KEY_CONVERSATION_SHORTNAME);
                }
            }
        }
        initWidget();
        initMoreOpWidget();

        initView();

    }


    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        CustomLog.d(TAG, "onNewIntent begin");
        // 消息界面类型
        frameType = intent.getIntExtra(KEY_NOTICE_FRAME_TYPE,
                VALUE_NOTICE_FRAME_TYPE_NEW);
        if (frameType == VALUE_NOTICE_FRAME_TYPE_LIST) {
            convstId = intent.getStringExtra(KEY_CONVERSATION_ID);
            conversationType = intent.getIntExtra(KEY_CONVERSATION_TYPE,
                    VALUE_CONVERSATION_TYPE_SINGLE);
            targetNubeNumber = intent.getStringExtra(KEY_CONVERSATION_NUBES);
            CustomLog.d(TAG, "onNewIntent groupId:" + groupId + frameType
                    + conversationType);
            groupId = targetNubeNumber;
            if (inputFragment != null) {
                CustomLog.d(TAG, "onNewIntent groupId:" + groupId);
                inputFragment.changedata(groupId);
            }
            targetShortName = intent.getStringExtra(KEY_CONVERSATION_SHORTNAME);
            convstExtInfo = intent.getStringExtra(KEY_CONVERSATION_EXT);
        } else if (frameType == VALUE_NOTICE_FRAME_TYPE_NUBE) {
            conversationType = intent.getIntExtra(KEY_CONVERSATION_TYPE,
                    VALUE_CONVERSATION_TYPE_SINGLE);
            targetNubeNumber = intent.getStringExtra(KEY_CONVERSATION_NUBES);
            targetShortName = intent.getStringExtra(KEY_CONVERSATION_SHORTNAME);
            groupId = targetNubeNumber;
            if (inputFragment != null) {
                CustomLog.d(TAG, "onNewIntent groupId:" + targetNubeNumber);
                inputFragment.changedata(targetNubeNumber);
            }
        }

//        if (addLinkmanTask != null) {
//            addLinkmanTask.dismissDialog();
//        }

        if (conversationType == VALUE_CONVERSATION_TYPE_SINGLE) {
            if (observeGroupMember != null) {
                getContentResolver().unregisterContentObserver(observeGroupMember);
                observeGroupMember = null;
            }

            if (groupObserve != null) {
                getContentResolver().unregisterContentObserver(groupObserve);
                groupObserve = null;
            }
        }

        initView();
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        CustomLog.d(TAG, "onSaveInstanceState begin");
        Bundle bundle = new Bundle();
        bundle.putString("cameraFilePath", cameraFilePath);
        bundle.putString("forwardNoticeId", forwardNoticeId);
        bundle.putString("addFriendNube", addFriendNube);
        bundle.putStringArrayList("receiverNumberLst", receiverNumberLst);
        bundle.putStringArrayList("selectNubeList", selectNubeList);
        bundle.putStringArrayList("selectNameList", selectNameList);
        bundle.putSerializable("receiverNameMap",
                (Serializable) receiverNameMap);

        bundle.putInt(KEY_NOTICE_FRAME_TYPE, frameType);
        if (frameType == VALUE_NOTICE_FRAME_TYPE_LIST) {
            bundle.putString(KEY_CONVERSATION_ID, convstId);
            bundle.putInt(KEY_CONVERSATION_TYPE, conversationType);
            bundle.putString(KEY_CONVERSATION_NUBES, targetNubeNumber);
            bundle.putString(KEY_GROUP_ID, groupId);
            bundle.putString(KEY_CONVERSATION_SHORTNAME, targetShortName);
            bundle.putString(KEY_CONVERSATION_EXT, convstExtInfo);
        } else if (frameType == VALUE_NOTICE_FRAME_TYPE_NUBE) {
            bundle.putString(KEY_CONVERSATION_NUBES, targetNubeNumber);
            bundle.putString(KEY_CONVERSATION_SHORTNAME, targetShortName);
        }

        outState.putBundle("params", bundle);

        super.onSaveInstanceState(outState);
    }


    @Override
    protected void onResume() {
        super.onResume();

        setNotDisturbMode();
        CommonUtil.hideSoftInputFromWindow(ChatActivity.this);
        CustomLog.d(TAG, "onResume begin");
        if (conversationType == VALUE_CONVERSATION_TYPE_MULTI) {
            dateList.clear();
            dateList.putAll(groupDao.queryGroupMembers(groupId));
            String value = MedicalApplication.getPreference()
                    .getKeyValue(PrefType.KEY_CHAT_REMIND_LIST, "");
            if (value != null && value.contains(groupId)) {
                value = value.replace(groupId + ";", "");
                MedicalApplication.getPreference().setKeyValue(
                        DaoPreference.PrefType.KEY_CHAT_REMIND_LIST, value);
            }
            if (groupDao.isGroupMember(groupId, selfNubeNumber)) {
                getTitleBar().enableRightBtn(null,
                        R.drawable.multi_send_btn_selector,
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                // 跳转到群发收件人界面
                                if (CommonUtil.isFastDoubleClick()) {
                                    return;
                                }

                                if (groupDao.isGroupMember(groupId,
                                        selfNubeNumber)) {
                                    // 是群成员，跳转

                                    // 群发，跳到群发联系人页面
                                    Intent intent = new Intent(
                                            ChatActivity.this,
                                            GroupChatDetailActivity.class);
                                    intent.putExtra(
                                            GroupChatDetailActivity.KEY_CHAT_TYPE,
                                            GroupChatDetailActivity.VALUE_GROUP);
                                    intent.putExtra(
                                            KEY_NUMBER,
                                            groupId);
                                    ChatActivity.this.startActivity(intent);

                                    CustomLog.d(TAG, "点击右上角群聊图标，跳转到群聊信息页");
                                }
                            }
                        });
            } else {
                getTitleBar().setRightBtnVisibility(View.GONE);
            }

            if (!gidExist) {
                CustomLog.d(TAG,"onResume 群组未在数据库中，需在查询后，重新生成");
                new GroupChatInterfaceManager(MedicalApplication.getContext())
                        .queryGroupDetail(groupId);
                this.finish();
            }
        }
        isSelectReceiver = false;
        if (frameType == VALUE_NOTICE_FRAME_TYPE_NUBE
                || frameType == VALUE_NOTICE_FRAME_TYPE_LIST) {
            if (conversationType == VALUE_CONVERSATION_TYPE_MULTI) {
                findViewById(R.id.add_friend_line).setVisibility(View.GONE);
            }
            initAddFriendLine();
        }

        // 第一次进入聊天界面，还没有聊天记录的场合，进入界面后，需要自动打开选择面板
        if (!firstFlag) {
            if (SoftInput) {
                inputFragment.showSelectlayout();
            }
        }
        cancelNotifacation();
        //更新未读消息
        //        updateNoticesInfo();

        // add by zzwang:添加客户端被唤醒时，需要重新拉起会控页面（如果存在的话）
        //        ButelMeetingManager.getInstance().resumeMeeting();

        //注册传感器监听器
        sensorManager.registerListener(this, proximitySensor,
                SensorManager.SENSOR_DELAY_NORMAL);
    }



    @Override
    public void onPause() {
        super.onPause();

        //取消注册传感器监听器
        sensorManager.unregisterListener(this);
    }


    @Override
    protected void onStop() {
        super.onStop();
        //        hideWaitDialog();
        hideMoreOpLayout();
        cleanCheckData();
        removeLoadingView();
        //		inputFragment.onStop();

        if (chatAdapter != null) {
            chatAdapter.onStop();
        }

        if (isSelectReceiver) {
            // 选择收件人的场合，不需要保存草稿
            isSelectReceiver = false;
        } else {
            saveDraftTxt();
        }
    }


    @Override
    public void onDestroy() {
        CustomLog.d(TAG, "onDestory begin");
        super.onDestroy();
        cleanCheckData();
        if (observer != null) {
            getContentResolver().unregisterContentObserver(observer);
            observer = null;
        }

        if (conversationType == VALUE_CONVERSATION_TYPE_MULTI) {
            String value = MedicalApplication.getPreference().getKeyValue(
                    PrefType.KEY_CHAT_REMIND_LIST, "");
            if (value != null && value.contains(groupId)) {
                value = value.replace(groupId + ";", "");
                MedicalApplication.getPreference().setKeyValue(
                        PrefType.KEY_CHAT_REMIND_LIST, value);
            }
        }

        if (observeGroupMember != null) {
            getContentResolver().unregisterContentObserver(observeGroupMember);
            observeGroupMember = null;
        }

        if (groupObserve != null) {
            getContentResolver().unregisterContentObserver(groupObserve);
            groupObserve = null;
        }

        if (chatAdapter != null) {
            chatAdapter.onDestroy();
            chatAdapter = null;
        }
        //		if (inputFragment != null) {
        //			inputFragment.onDestroy();
        //			inputFragment = null;
        //		}
        //        this.unregisterReceiver(mReceiver);
        CustomLog.d(TAG, "onDestory end");
    }


    private void saveDraftTxt() {
        if (!isSaveDraft) {
            return;
        }
        // 保存草稿
        String draftTxt = inputFragment.obtainInputTxt();
        if (conversationType == VALUE_CONVERSATION_TYPE_MULTI) {
            ArrayList<String> resList = new ArrayList<String>();
            resList = IMCommonUtil.getList(draftTxt);
            for (int i = 0; i < resList.size(); i++) {

                for (int j = 0; j < selectNameList.size(); j++) {
                    if (resList.get(i).equals(selectNameList.get(j))) {
                        draftTxt = draftTxt.replace(resList.get(i),
                                selectNubeList.get(j));
                        break;
                    }
                }
            }
        }
        if (TextUtils.isEmpty(convstId)) {
            if (TextUtils.isEmpty(draftTxt)) {
                // 还没有会话的场合，没有草稿信息，无需保存
                return;
            } else {
                threadDao.saveDraft(getReceivers(), draftTxt);
            }
        } else {
            // 有会话的场合，退出时，需更新草稿信息
            threadDao.saveDraftById(convstId, draftTxt);
        }
    }


    private void queryNoticeData(int queryType) {
        // 分页查询，必须一次查询完成后才能开始下次查询，所以队列中只能保存一个类型
        // 而范围查询，只要起始时间改变了，就需要重新查询
        String queryKey = queryType + "_" + getRecvTimeBegin();
        if (queryList.contains(queryKey)) {
            // 查询队列里有相同的查询请求，则放弃查询
            return;
        }

        // 队列为空，加入队列并启动线程；队列不为空，则加入队列，等待线程执行完成后启动下一个线程
        synchronized (queryList) {
            if (queryList.isEmpty()) {
                queryList.add(queryKey);
                queryRunnable = new QueryRunnable();
                queryRunnable.queryType = queryType;
                queryRunnable.recvTimeBg = getRecvTimeBegin();
                mHandler.postDelayed(queryRunnable, 100);
            } else {
                queryList.add(queryKey);
            }
        }
    }


    private long getRecvTimeBegin() {
        synchronized (recvTimeLock) {
            return recvTimeBegin;
        }
    }


    private void setRecvTimeBegin(long rTb) {
        synchronized (recvTimeLock) {
            recvTimeBegin = rTb;
        }
    }


    // 线程安全的并发查询队列
    private List<String> queryList = new CopyOnWriteArrayList<String>();
    // 查询数据Runnable
    private QueryRunnable queryRunnable = null;


    @Override
    public void onResult(String _interfaceName, boolean isSuccess, String result) {

    }


    /**
     * 当有新的数据时 android framework 层调用
     */

    @Override public void onSensorChanged(final SensorEvent event) {


        if (isPlaying) {


            float distance = event.values[0];

            // 用户远离听筒，音频外放，亮屏
            if (distance >= proximitySensor.getMaximumRange()) {

                audioHelper.enableSpeaker();
                helper.setScreenOn();

                //make Toast
                final RelativeLayout playModeViewGroup
                        = (RelativeLayout) ((Activity) mContext)
                        .findViewById(R.id.container_toast);
                TextView textView = (TextView) playModeViewGroup.findViewById(R.id.slogan);
                textView.setText("已从听筒切换回扬声器播放");
                playModeViewGroup.setVisibility(View.VISIBLE);
                new Handler().postDelayed(new Runnable() {
                    @Override public void run() {
                        playModeViewGroup.setVisibility(GONE);
                    }
                }, 2000);


            } else {              // 用户贴近听筒，切换音频到听筒输出，并且熄屏防误触
                audioHelper.enableReceiver(audioPath);
                helper.setScreenOff();
            }
        }

    }


    /**
     * 当距离传感器精度发生变化时 android framework 层调用
     */
    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {}


    @Override public void AudioMsgState(boolean isPlaying,String audioPath) {
        this.isPlaying = isPlaying;
        this.audioPath = audioPath;
    }


    class QueryRunnable implements Runnable {

        public int queryType = 0;
        public long recvTimeBg = 0l;


        public void run() {
            CustomLog.d(TAG, "查询会话消息:" + convstId);
            if (conversationType == VALUE_CONVERSATION_TYPE_MULTI) {
                convstId = groupId;
            }
            // 查询动态数据
            QueryConvstNoticeAsyncTask task = new QueryConvstNoticeAsyncTask(
                    ChatActivity.this, convstId, queryType, getRecvTimeBegin(),
                    IMConstant.NOTICE_PAGE_CNT);
            task.setQueryTaskListener(new QueryConvstNoticeAsyncTask.QueryTaskPostListener() {
                @Override
                public void onQuerySuccess(Cursor cursor) {
                    CustomLog.d(TAG, "QueryConvstNoticeAsyncTask onQuerySuccess");
                    updateNoticesInfo();
                    if (chatAdapter != null) {

                        if (queryType == QueryConvstNoticeAsyncTask.QUERY_TYPE_PAGE) {
                            // 分页查询的场合

                            if (cursor == null) {
                                // 没有查询到数据
                                headerRoot.setPadding(0,
                                        -headerRoot.getHeight(), 0, 0);
                                headerRoot.setVisibility(View.INVISIBLE);
                            } else {
                                int pageCursorCnt = cursor.getCount();
                                CustomLog.d(TAG, "QueryConvstNoticeAsyncTask:"
                                        + pageCursorCnt);
                                if (pageCursorCnt < IMConstant.NOTICE_PAGE_CNT) {
                                    // 数据量小于一页数据的场合，没有上一页数据了
                                    headerRoot.setPadding(0,
                                            -headerRoot.getHeight(), 0, 0);
                                    headerRoot.setVisibility(View.INVISIBLE);
                                } else {
                                    headerRoot.setPadding(0, 0, 0, 0);
                                    headerRoot.setVisibility(View.VISIBLE);
                                }

                                if (cursor.moveToFirst()) {
                                    setRecvTimeBegin(cursor.getLong(cursor
                                            .getColumnIndex(NoticesTable.NOTICE_COLUMN_SENDTIME)));
                                }

                                chatAdapter.mergeLastPageCursor(cursor);

                                if (pageCursorCnt > 0) {
                                    // 定位到最下面一条
                                    noticeListView.setSelection(pageCursorCnt);
                                }
                            }

                        } else if (queryType == QueryConvstNoticeAsyncTask.QUERY_TYPE_COND) {
                            // 范围查询的场合

                            if (cursor != null && cursor.moveToFirst()) {
                                setRecvTimeBegin(cursor.getLong(cursor
                                        .getColumnIndex(NoticesTable.NOTICE_COLUMN_SENDTIME)));
                            }

                            int oldCnt = chatAdapter.getCount();
                            chatAdapter.changeCursor(cursor);
                            int newCnt = chatAdapter.getCount();
                            // CustomLog("QueryConvstNoticeAsyncTask:" + newCnt
                            // + cursor.getCount());
                            if (newCnt > oldCnt) {
                                CustomLog.d(TAG, "消息查询结果：oldCnt=" + oldCnt
                                        + " | newCnt=" + newCnt);
                                // 有新消息的场合，定位到最下面一条
                                noticeListView.setSelection(newCnt - 1);
                            }
                        }
                    }
                    // 初始化im面板
                    if (firstFlag) {
                        firstFlag = false;
                        if ((cursor == null || cursor.getCount() == 0)) {
                            inputFragment.showSelectlayout();
                            SoftInput = true;
                        } else {
                            SoftInput = false;
                        }
                    } else {
                        if (cursor.getCount() > 0) {
                            SoftInput = false;
                        }
                    }
                    afterQuery(queryType, recvTimeBg);
                }


                @Override
                public void onQueryFailure() {
                    CustomLog.d(TAG, "QueryConvstNoticeAsyncTask onQueryFailure");
                    updateNoticesInfo();
                    Toast.makeText(ChatActivity.this, R.string.load_msg_fail,
                            Toast.LENGTH_SHORT).show();
                    afterQuery(queryType, recvTimeBg);
                }
            });
            task.executeOnExecutor(ThreadPoolManger.THREAD_POOL_EXECUTOR, "");
        }
    }


    ;


    private void afterQuery(int queryType, long recvTb) {
        String queryKey = queryType + "_" + recvTb;
        queryList.remove(queryKey);

        synchronized (queryList) {
            if (!queryList.isEmpty()) {
                // 查询队列
                String key = queryList.get(0);
                String[] keys = key.split("_");

                // 继续下一个查询
                queryRunnable = new QueryRunnable();
                queryRunnable.queryType = Integer.parseInt(keys[0]);
                queryRunnable.recvTimeBg = Long.parseLong(keys[1]);
                mHandler.postDelayed(queryRunnable, 100);
            }
        }
    }


    private void initWidget() {
        setNotDisturbMode();

        // commentEditText =
        // (EmojiconEditText) findViewById(R.id.notice_comment_text);
        // commentEditText.setEmojiconSize(38);
        noticeListView = (ListView) findViewById(R.id.notice_listview);
        chatlayout = (LinearLayout) findViewById(R.id.chat_linearlayout);
        backbtn = (Button) chatlayout.findViewById(R.id.back_btn);
        backtext = (TextView) chatlayout.findViewById(R.id.back_str);
        // receiversLine = (RelativeLayout) findViewById(R.id.receivers_line);
        // receiverInput = (EditText) findViewById(R.id.receiver_input);
        // receiverInputFocus = (TextView) findViewById(R.id.on_receiver_input);

        //初始化 语音消息播放模式 SharedPreferenced
        voiceMsgSettings = getSharedPreferences(VOICE_PREFS_NAME,0);

        chatAdapter = new ChatListAdapter(ChatActivity.this, null, noticeDao,
                targetNubeNumber, targetShortName);
        chatAdapter.setSelfNubeNumber(selfNubeNumber);
        chatAdapter.setCallbackInterface((ChatListAdapter.CallbackInterface) this);
        chatAdapter.setAudioMsgStateListener(this);
        chatAdapter.setSharedPreferences(voiceMsgSettings);


        noticeListView.addHeaderView(headerLoadingView);
        noticeListView.setAdapter(chatAdapter);

        newNoticeNum = (TextView) this.findViewById(R.id.total_new_notice_num);

        //更新消息未读数
        //        updateNoticesInfo();

        String nube = "";
        if (conversationType == VALUE_CONVERSATION_TYPE_SINGLE) {
            nube = targetNubeNumber;
        } else {
            nube = groupId;
        }
        CustomLog.d(TAG, nube + "");

        inputFragment.chatActivity = ChatActivity.this;
        inputFragment.setNubeNum(nube);

        inputFragment.callback = new ChatInputFragment.SendCallbackInterface() {

            @Override
            public boolean onSendTxtMsg(final String txtMsg) {
                // 发送文字
                // MobclickAgent.onEvent(ChatActivity.this,
                //         UmengEventConstant.EVENT_SEND_TEXT);
                new Thread(new Runnable() {

                    @Override
                    public void run() {
                        String uuid = "";
                        if (conversationType == VALUE_CONVERSATION_TYPE_MULTI) {
                            // 插入发送记录
                            ArrayList<String> resList = new ArrayList<String>();
                            ArrayList<String> lastList = new ArrayList<String>();
                            resList = IMCommonUtil.getList(txtMsg);
                            lastList.addAll(resList);
                            String str = "";
                            str = new String(txtMsg);
                            boolean repalceflg = true;
                            for (int i = 0; i < resList.size(); i++) {

                                for (int j = 0; j < selectNameList
                                        .size(); j++) {
                                    if (resList.get(i).equals(
                                            selectNameList.get(j))) {
                                        str = str.replace(
                                                resList.get(i),
                                                selectNubeList.get(j));
                                        repalceflg = false;
                                        lastList.remove(0);
                                        break;
                                    }
                                }
                            }
                            if (lastList != null
                                    && lastList.size() != 0) {
                                for (int i = 0; i < lastList.size(); i++) {
                                    ArrayList<GroupMemberBean> beanList = groupDao
                                            .queryAllGroupMembers(
                                                    groupId,
                                                    selfNubeNumber);
                                    for (int j = 0; j < beanList.size(); j++) {
                                        NameElement element = ShowNameUtil
                                                .getNameElement(
                                                        beanList.get(j)
                                                                .getName(),
                                                        beanList.get(j)
                                                                .getNickName(),
                                                        beanList.get(j)
                                                                .getPhoneNum(),
                                                        beanList.get(j)
                                                                .getNubeNum());
                                        String MName = ShowNameUtil
                                                .getShowName(element);
                                        if (lastList
                                                .get(i)
                                                .equals("@"
                                                        + MName
                                                        + IMConstant.SPECIAL_CHAR)) {
                                            str = str.replace(
                                                    lastList.get(i),
                                                    "@"
                                                            + beanList
                                                            .get(j)
                                                            .getNubeNum()
                                                            + IMConstant.SPECIAL_CHAR);
                                            repalceflg = false;
                                            break;
                                        }
                                    }
                                }
                            }
                            if (resList.size() == 0 && repalceflg) {
                                str = txtMsg;
                            }
                            uuid = noticeDao
                                    .createSendFileNotice(
                                            selfNubeNumber,
                                            groupId,
                                            null,
                                            "",
                                            FileTaskManager.NOTICE_TYPE_TXT_SEND,
                                            str, groupId, null);

                            selectNameList.clear();
                            selectNubeList.clear();
                        } else {
                            // 插入发送记录
                            uuid = noticeDao
                                    .createSendFileNotice(
                                            selfNubeNumber,
                                            getReceivers(),
                                            null,
                                            "",
                                            FileTaskManager.NOTICE_TYPE_TXT_SEND,
                                            txtMsg, convstId, null);
                        }
                        getFileTaskManager()
                                .addTask(uuid, null);
                    }
                }).start();
                return true;
            }


            @Override
            public void onSendPic() {
                SendCIVMUtil.sendPic(ChatActivity.this);
            }


            @Override
            public void onSendPicFromCamera() {
                SendCIVMUtil.sendPicFromCamera(ChatActivity.this);
            }


            @Override
            public void onSendVideo() {
                SendCIVMUtil.sendVideo(ChatActivity.this);
            }


            @Override
            public void onSendVcard() {
                SendCIVMUtil.sendVcard(ChatActivity.this);
            }


            @Override
            public boolean doPreSendCheck() {

                // if (frameType == VALUE_NOTICE_FRAME_TYPE_NEW) {
                // // 点击发送或者素材加号按钮，收件人变为收缩状态
                // receiverInputFocus.setVisibility(View.VISIBLE);
                // receiverInput.setVisibility(View.GONE);
                //
                // // 新消息的场合，判断是否有收件人
                // if (receiverNumberLst == null
                // || receiverNumberLst.size() == 0) {
                // Toast.makeText(ChatActivity.this,
                // R.string.select_receiver_toast,
                // Toast.LENGTH_SHORT).show();
                // CustomLog.d("Toast:请选择收件人");
                // return false;
                // }
                // }
                return true;
            }


            @Override
            public void onSendAudio(final String rcdFilePah,
                                    final int rcdLenth) {

                //                MobclickAgent.onEvent(ChatActivity.this,
                //                        UmengEventConstant.EVENT_SEND_AUDIO);
                // 发送音频
                new Thread(new Runnable() {

                    @Override
                    public void run() {

                        ButelPAVExInfo extInfo = new ButelPAVExInfo();
                        extInfo.setDuration(rcdLenth);

                        List<String> localFiles = new ArrayList<String>();
                        localFiles.add(rcdFilePah);

                        String uuid = "";
                        if (conversationType == VALUE_CONVERSATION_TYPE_MULTI) {
                            // 插入发送记录
                            uuid = noticeDao
                                    .createSendFileNotice(
                                            selfNubeNumber,
                                            groupId,
                                            localFiles,
                                            "",
                                            FileTaskManager.NOTICE_TYPE_AUDIO_SEND,
                                            "", groupId, extInfo);
                        } else {
                            // 插入发送记录
                            uuid = noticeDao
                                    .createSendFileNotice(
                                            selfNubeNumber,
                                            getReceivers(),
                                            localFiles,
                                            "",
                                            FileTaskManager.NOTICE_TYPE_AUDIO_SEND,
                                            "", convstId, extInfo);
                        }

                        MedicalApplication.getFileTaskManager()
                                .addTask(uuid, null);
                    }
                }).start();
            }


            @Override
            public void onSelectGroupMemeber() {
                CustomLog.d(TAG, "onSelectGroupMemeber 选择回复的人");
                // 选择回复的人
                if (conversationType == VALUE_CONVERSATION_TYPE_MULTI) {
                    Intent intent = new Intent(ChatActivity.this,
                            SelectLinkManActivity.class);
                    intent.putExtra(
                            SelectLinkManActivity.OPT_FLAG,
                            SelectLinkManActivity.OPT_HAND_OVER_START_FOR_RESULT);
                    intent.putExtra(
                            SelectLinkManActivity.AVITVITY_TITLE,
                            "选择回复的人");
                    intent.putExtra(
                            SelectLinkManActivity.ACTIVITY_FLAG,
                            SelectLinkManActivity.AVITVITY_START_FOR_RESULT);
                    intent.putExtra(
                            SelectLinkManActivity.KEY_IS_SIGNAL_SELECT,
                            false);
                    intent.putExtra(
                            SelectLinkManActivity.KEY_SINGLE_CLICK_BACK,
                            true);
                    intent.putExtra(
                            SelectLinkManActivity.HAND_OVER_MASTER_LIST,
                            GroupMemberToContactsBean());
                    intent.putExtra(
                            SelectGroupMemeberActivity.SELECT_GROUPID,
                            groupId);
                    startActivityForResult(intent,
                            ACTION_FORWARD_NOTICE);
                }
            }


            @Override
            public void onAudioCall() {
                // 语音电话
                //                MobclickAgent.onEvent(ChatActivity.this,
                //                        UmengEventConstant.EVENT_SEND_AUDIOCALL);
                //                OutCallUtil.makeNormalCall(ChatActivity.this,
                //                        getReceivers(), OutCallUtil.CT_SIP_AUDIO,"","");
            }


            @Override
            public void onVedioCall() {
                // 视频电话
                //                MobclickAgent.onEvent(ChatActivity.this,
                //                        UmengEventConstant.EVENT_SEND_VEDIOCALL);
                //                OutCallUtil.makeNormalCall(ChatActivity.this,
                //                        getReceivers(), OutCallUtil.CT_SIP_AV,"","");

            }


            @Override
            public void onMeetingCall() {
                showMeetingAlertDialog();
            }


            @Override
            public void onAudioRecStart() {
                // 重新播放
                if (chatAdapter != null) {
                    chatAdapter.stopCurAuPlaying();
                }
            }


            @Override
            public void onShareCollection() {
                String receiver = "";
                if (conversationType == VALUE_CONVERSATION_TYPE_MULTI) {
                    receiver = groupId;
                } else {
                    receiver = getReceivers();
                }
                //                CollectionManager.getInstance().goToSharedCollectionActivity(ChatActivity.this,receiver);
                if (receiver.length() == 0) {
                    CustomLog.d(TAG, "收藏的received 为空字符串");
                    return;
                }
                Intent intent = new Intent(mContext, CollectionActivity.class);
                intent.putExtra(CollectionActivity.KEY_RECEIVER, receiver);
                mContext.startActivity(intent);
            }
        };

        noticeListView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_FLING) {
                    CustomLog.d(TAG, "列表正在滚动...");
                    // list列表滚动过程中，暂停图片上传下载
                } else {
                }
                if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE) {
                    // 滚动停止的场合，加载更多数据
                    int firstVP = noticeListView.getFirstVisiblePosition();
                    CustomLog.d(TAG, "列表停止滚动...FirstVisiblePosition:" + firstVP);
                    if (firstVP == 0
                            && headerRoot.getVisibility() == View.VISIBLE) {
                        queryNoticeData(QueryConvstNoticeAsyncTask.QUERY_TYPE_PAGE);
                    }

                }
            }


            @Override
            public void onScroll(AbsListView view, int firstVisibleItem,
                                 int visibleItemCount, int totalItemCount) {
            }
        });

        noticeListView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // 隐藏输入法及素材面板
                CommonUtil.hideSoftInputFromWindow(ChatActivity.this);
                inputFragment.setHide();
                return false;
            }
        });
        getTitleBar().setBack(null, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (CommonUtil.isFastDoubleClick()) {
                    return;
                }
                if (waitDialog != null && waitDialog.isShowing()) {
                    //                    hideWaitDialog();
                    removeLoadingView();
                    // ButelMeetingManager.getInstance().cancelCreateMeeting(
                    //     ButelContactDetailActivity.class.getName());
                }
                exitActivity();
            }
        });

        //确认传感器是否存在
        identifySensor();
    }


    private void identifySensor() {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        proximitySensor = sensorManager.getDefaultSensor(TYPE_PROXIMITY);

        if (proximitySensor == null) {
            CustomToast.show(this, "该手机没有距离传感器，无法使用部分功能", 1);
            CustomLog.d(TAG, "该手机没有距离传感器，无法使用播放语音消息听筒模式部分功能");
        } else {

        }

    }


    protected void showMeetingAlertDialog() {
        CustomLog.d(TAG, "显示 立即召开/预约会议室 对话框");
        final MedicalAlertDialog menuDlg = new MedicalAlertDialog(this);
        menuDlg.addButtonFirst(new BottomMenuWindow.MenuClickedListener() {
            @Override
            public void onMenuClicked() {
                conveneMeeting();
            }
        }, "立即召开");

        menuDlg.addButtonSecond(new BottomMenuWindow.MenuClickedListener() {
            @Override
            public void onMenuClicked() {
                bookMeeting();
            }
        }, "预约会诊室");
        menuDlg.addButtonThird(new BottomMenuWindow.MenuClickedListener() {
            @Override
            public void onMenuClicked() {
                menuDlg.dismiss();
            }
        }, "取消");
        menuDlg.show();
    }


    protected void conveneMeeting() {
        CustomLog.d(TAG, "conveneMeeting begin,点击 立即召开");
        String targetId = targetNubeNumber;
        if (conversationType == VALUE_CONVERSATION_TYPE_MULTI) {
            targetId = groupId;
        }
        if (SendCIVMUtil.conveneMeeting(ChatActivity.this, TAG,
                conversationType, targetId, selfNubeNumber)) {
            CustomLog.d(TAG, "准备创建会议");

        }
        CustomLog.d(TAG, "conveneMeeting end");
    }


    private void bookMeeting() {
        CustomLog.d(TAG, "bookMeeting begin,点击 预约会议室");
        String targetId = targetNubeNumber;
        if (conversationType == VALUE_CONVERSATION_TYPE_MULTI) {
            targetId = groupId;
        }
        SendCIVMUtil.bookMeeting(ChatActivity.this, targetId,
                conversationType == VALUE_CONVERSATION_TYPE_MULTI);
        CustomLog.d(TAG, "bookMeeting end");
    }


    private void exitActivity() {
        // 2015-01-29如果这个会话界面处于栈中的根节点，即为栈中唯一节点时，此时点击返回键，跳转到消息列表界面；否则直接finish该界面，返回跳转前界面
        // 2015-12-08 消息页面点击 返回，都回到消息列表页面
        // if (this.isTaskRoot()) {
        if (back_flag) {
            HomeActivity.isFromChatActivity = true;
            Intent tempintent = new Intent(this, HomeActivity.class);
            tempintent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            tempintent.putExtra(HomeActivity.TAB_INDICATOR_INDEX,
                    HomeActivity.TAB_INDEX_MESSAGE);
            startActivity(tempintent);

        }
        finish();
    }

    //	private void onRecvInptFocusOut() {
    //		receiverInput.setVisibility(View.GONE);
    //		receiverInputFocus.setVisibility(View.VISIBLE);
    //		if (receiverNumberLst.size() == 0) {
    //			receiverInputFocus.setText("");
    //		} else if (receiverNumberLst.size() == 1) {
    //			receiverInputFocus.setText(receiverNameMap.get(receiverNumberLst
    //					.get(0)));
    //		} else {
    //			receiverInputFocus.setText(getString(R.string.receiver_tip,
    //					getReceiverDispName(receiverNameMap.get(receiverNumberLst
    //							.get(0))), receiverNumberLst.size() - 1));
    //		}
    //	}


    private void initView() {

        switch (frameType) {
            case VALUE_NOTICE_FRAME_TYPE_NUBE: {
                // 单聊
                conversationType = VALUE_CONVERSATION_TYPE_SINGLE;

                // 根据nube号查询会话信息
                // 已产生会话，则并入已有会话
                // 未产生会话，则继续监听数据库后，直到产生会话
                if (mergeThreads() == 0) {
                    convstId = "";
                    convstExtInfo = "";
                    // 清空列表数据
                    if (chatAdapter != null) {
                        chatAdapter.changeCursor(null);
                    }
                }

            }
            case VALUE_NOTICE_FRAME_TYPE_LIST: {
                // 聊天列表的场合，隐藏收件人区域

                if (SYS_NUBE.equals(targetNubeNumber)) {
                    // 官方帐号，隐藏输入框，禁止回复
                    inputFragment.isShowing = false;
                    getSupportFragmentManager().beginTransaction()
                            .remove(inputFragment).commit();
                    targetShortName = getString(R.string.str_butel_name);
                    convstExtInfo = "";
                    getTitleBar().setSubTitle(null);
                    getTitleBar().setRightBtnVisibility(View.INVISIBLE);

                } else if (conversationType == VALUE_CONVERSATION_TYPE_SINGLE) {
                    // 单人聊天
                    inputFragment.isShowing = true;
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.input_line, inputFragment).commit();
                    getTitleBar().setSubTitle(null);

                    //如果是系统 Nube 不显示个人详情页按钮
                    if (!targetNubeNumber.equals(SYS_NUBE)) {

                        getTitleBar().enableRightBtn(null,
                                R.drawable.single_convst_btn_selector,
                                new OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        // 跳转到个人名片界面
                                        if (CommonUtil.isFastDoubleClick()) {
                                            return;
                                        }

                                        Intent intent = new Intent(ChatActivity.this,
                                                GroupChatDetailActivity.class);
                                        intent.putExtra(
                                                GroupChatDetailActivity.KEY_CHAT_TYPE,
                                                GroupChatDetailActivity.VALUE_SINGLE);
                                        intent.putExtra(
                                                KEY_NUMBER,
                                                targetNubeNumber);
                                        startActivity(intent);

                                        CustomLog.d(TAG, "点击单人聊天图标，跳转到个人 聊天信息页");
                                    }
                                });
                    }

                } else if (conversationType == VALUE_CONVERSATION_TYPE_MULTI) {
                    if (!groupDao.existGroup(groupId)) {
                        CustomLog.d(TAG,"initView 群组未在数据库中，需在查询后，重新生成");
                        CustomToast.show(mContext,"群信息未初始化，请稍后重试",1);
                        gidExist = false;
                    }else{
                        gidExist = true;
                    }
                    // 群发
                    inputFragment.isShowing = true;
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.input_line, inputFragment).commit();
                    // 群发名称
                    chatAdapter.setNoticeType(conversationType, groupId);
                    targetShortName = getGroupNameTitle();
                    if (observeGroupMember == null) {
                        observeGroupMember = new GroupMemberObserver();
                        getContentResolver().registerContentObserver(
                                ProviderConstant.NETPHONE_GROUP_URI, true,
                                observeGroupMember);
                    }

                    if (groupObserve == null) {
                        groupObserve = new GroupObserver();
                        getContentResolver().registerContentObserver(
                                ProviderConstant.NETPHONE_GROUP_MEMBER_URI, true,
                                groupObserve);
                    }
                    getTitleBar().setSubTitle("(" + groupMemberSize + ")");
                    //				findViewById(R.id.input_layout).setVisibility(View.VISIBLE);
                    // String[] receivers = targetNubeNumber.split(";");
                    if (!isGroupMember) {
                        getTitleBar().setRightBtnVisibility(View.GONE);
                    } else {
                        getTitleBar().enableRightBtn(null,
                                R.drawable.multi_send_btn_selector,
                                new OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        // 跳转到群发收件人界面
                                        if (CommonUtil.isFastDoubleClick()) {
                                            return;
                                        }
                                        if (groupDao.isGroupMember(groupId,
                                                selfNubeNumber)) {
                                            // 是群成员，跳转

                                            // 群发，跳到群发联系人页面
                                            Intent intent = new Intent(
                                                    ChatActivity.this,
                                                    GroupChatDetailActivity.class);
                                            intent.putExtra(
                                                    GroupChatDetailActivity.KEY_CHAT_TYPE,
                                                    GroupChatDetailActivity.VALUE_GROUP);
                                            intent.putExtra(
                                                    KEY_NUMBER,
                                                    groupId);
                                            ChatActivity.this.startActivity(intent);

                                            CustomLog.d(TAG, "点击群发图标，跳转到群发收件人界面");
                                        }
                                    }
                                });
                    }
                }
                if (ContactManager.getInstance(this).checkNubeIsCustomService(targetNubeNumber)) {
                    getTitleBar().setTitle("视频客服");
                } else {
                    getTitleBar().setTitle(targetShortName);
                }

                // 草稿文字填充
                initDraftText();

                if (observer == null) {
                    observer = new MyContentObserver();
                    getContentResolver().registerContentObserver(
                            ProviderConstant.NETPHONE_NOTICE_URI, true, observer);
                }

                if (frameType == VALUE_NOTICE_FRAME_TYPE_LIST) {
                    // 初始化
                    SoftInput = false;
                    firstFlag = true;
                    setRecvTimeBegin(0);
                    if (chatAdapter != null) {
                        chatAdapter.clearData();
                    }
                    queryNoticeData(QueryConvstNoticeAsyncTask.QUERY_TYPE_PAGE);
                } else {
                    // 隐藏分页加载等待view
                    headerRoot.setPadding(0, -headerRoot.getHeight(), 0, 0);
                    headerRoot.setVisibility(View.INVISIBLE);
                    SoftInput = true;
                    firstFlag = false;
                }

                // 去除状态栏通知
                cancelNotifacation();
            }
            break;
        }
    }


    private void setNotDisturbMode() {
        if (conversationType == VALUE_CONVERSATION_TYPE_SINGLE) {
            setNotDisturbViewMainTitle();
        } else if (conversationType == VALUE_CONVERSATION_TYPE_MULTI) {
            setNotDisturbViewSubTitle();
        }
    }


    private void setNotDisturbViewMainTitle() {

        ImageView bell = (ImageView) findViewById(R.id.not_disturb);
        if (MedicalApplication.getPreference()
                .getKeyValue(PrefType.KEY_CHAT_DONT_DISTURB_LIST, "")
                .indexOf(";" + targetNubeNumber + ";") >= 0) {
            bell.setVisibility(View.VISIBLE);
        } else {
            bell.setVisibility(View.GONE);
        }
    }


    private void setNotDisturbViewSubTitle() {
        ImageView bell = (ImageView) findViewById(R.id.not_disturb_sub);
        if (MedicalApplication.getPreference()
                .getKeyValue(PrefType.KEY_CHAT_DONT_DISTURB_LIST, "")
                .indexOf(";" + targetNubeNumber + ";") >= 0) {
            bell.setVisibility(View.VISIBLE);
        } else {
            bell.setVisibility(View.GONE);
        }
    }


    private void cancelNotifacation() {
        if (conversationType == VALUE_CONVERSATION_TYPE_SINGLE) {
            NotificationUtil.cancelNewMsgNotifacation(targetNubeNumber);
            // 清除未接来电通知
            NotificationUtil.cancelNotifacationById(targetNubeNumber);
        } else if (conversationType == VALUE_CONVERSATION_TYPE_MULTI) {
            int notifyId = NotificationUtil.getGroupNotifyID(groupId);
            NotificationUtil.cancelNewMsgNotifacation(notifyId + "");
        }
    }


    private void initDraftText() {
        // 草稿文字填充
        String draftTxt = "";
        try {
            if (!TextUtils.isEmpty(convstExtInfo)) {
                JSONObject extObj = new JSONObject(convstExtInfo);
                draftTxt = extObj.optString("draftText");
            }
        } catch (Exception e) {
            CustomLog.d(TAG, "草稿信息解析失败" + e.toString());
        }

        if (conversationType == VALUE_CONVERSATION_TYPE_MULTI) {
            ArrayList<String> dispNubeList = new ArrayList<String>();
            dispNubeList = CommonUtil.getDispList(draftTxt);
            for (int i = 0; i < dispNubeList.size(); i++) {
                GroupMemberBean gbean = groupDao.queryGroupMember(groupId,
                        dispNubeList.get(i));
                NameElement element = ShowNameUtil.getNameElement(
                        gbean.getName(), gbean.getNickName(),
                        gbean.getPhoneNum(), gbean.getNubeNum());
                String MName = ShowNameUtil.getShowName(element);
                selectNameList.add("@" + MName + IMConstant.SPECIAL_CHAR);
                selectNubeList.add("@" + dispNubeList.get(i)
                        + IMConstant.SPECIAL_CHAR);
                draftTxt = draftTxt.replace("@" + dispNubeList.get(i)
                        + IMConstant.SPECIAL_CHAR, "@" + MName
                        + IMConstant.SPECIAL_CHAR);
            }
        }

        if (!inputFragment.obtainInputTxt().equals(draftTxt)) {
            inputFragment.setDraftTxt(draftTxt);
        }
    }


    private void initAddFriendLine() {
        if (ContactManager.getInstance(this).checkNubeIsCustomService(targetNubeNumber) ||
                targetNubeNumber.equals(SYS_NUBE)) {
            // 官方帐号,不显示 添加联系人条目
            findViewById(R.id.add_friend_line)
                    .setVisibility(View.GONE);
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                // 判断单聊联系人是否为好友
                if (conversationType == VALUE_CONVERSATION_TYPE_SINGLE) {
                    // 根据视讯号查询好友信息
                    final ContactFriendBean friendPo = new NetPhoneDaoImpl(
                            ChatActivity.this)
                            .queryFriendInfoByNube(targetNubeNumber);
                    if (friendPo != null
                            && !TextUtils.isEmpty(friendPo.getNubeNumber())) {
                        // 是本地好友，隐藏加为好友
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                findViewById(R.id.add_friend_line)
                                        .setVisibility(View.GONE);

                                // edit by zzwang
                                // 标题栏展现条目信息已经从上层传递过来，此处不需要再处理
                                // targetShortName = friendPo.getName();
                                // if (TextUtils.isEmpty(targetShortName)) {
                                // targetShortName = friendPo.getNickname();
                                // if (TextUtils.isEmpty(targetShortName)) {
                                // targetShortName = targetNubeNumber;
                                // }
                                // }
                                //
                                // getTitleBar().setTitle(targetShortName);
                            }
                        });
                    } else {
                        // 不是本地好友，显示加为好友
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                findViewById(R.id.add_friend_line)
                                        .setVisibility(View.VISIBLE);
                                findViewById(R.id.add_friend_btn)
                                        .setOnClickListener(
                                                new OnClickListener() {
                                                    @Override
                                                    public void onClick(View v) {
                                                        // 20160114 产品刘莹要求
                                                        // 400开头的号码，不能添加为好友
                                                        if (targetNubeNumber
                                                                .startsWith(IMConstant.BAN_ADD_FRIEND)) {
                                                            tipToast("不能添加为好友");
                                                            return;
                                                        }
                                                        addFriendNube = targetNubeNumber;
                                                        // 加为好友
                                                        addLinkmanBySearch(targetNubeNumber);
                                                    }
                                                });
                                // edit by zzwang
                                // 标题栏展现条目信息已经从上层传递过来，此处不需要再处理
                                // targetShortName = targetNubeNumber;
                                // getTitleBar().setTitle(targetShortName);
                            }
                        });
                    }
                } else if (conversationType == VALUE_CONVERSATION_TYPE_MULTI) {
                    // 查询多个人的名称
                    List<String> receivers = Arrays.asList(targetNubeNumber
                            .split(";"));
                    if (receivers != null && receivers.size() > 4) {
                        // 因为只显示前三个名称，因此此处只查前4个人
                        receivers = receivers.subList(0, 4);
                    }
                    // NetPhoneDaoImpl netPhDao = new NetPhoneDaoImpl(
                    // ChatActivity.this);
                    targetShortName = getGroupNameTitle();
                    runOnUiThread(new Runnable() {
                        public void run() {
                            getTitleBar().setTitle(targetShortName);
                        }
                    });
                }
            }
        }).start();
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // 点击back键时，如果底部面板弹出，则隐藏，否则退出程序
        if (keyCode == KeyEvent.KEYCODE_BACK) {

            if (chatAdapter != null && chatAdapter.isMultiCheckMode()) {
                hideMoreOpLayout();
                cleanCheckData();
                return true;
            }

            if (waitDialog != null && waitDialog.isShowing()) {
                //                hideWaitDialog();
                //                ButelMeetingManager.getInstance().cancelCreateMeeting(
                //                        ButelContactDetailActivity.class.getName());
            }

            if (inputFragment.isPanelVisible()) {
                inputFragment.setHide();
                return true;
            } else {
                exitActivity();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }


    class MyImageSpan extends ImageSpan {

        private String tag;


        public MyImageSpan(Drawable d) {
            super(d);
        }


        public void setTag(String position) {
            this.tag = position;
        }


        public String getTag() {
            return this.tag;
        }
    }


    private class MyContentObserver extends ContentObserver {
        public MyContentObserver() {
            super(new Handler());
        }


        @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            CustomLog.d(TAG, "消息数据库数据发生变更1:" + selfChange + "|" + uri.toString());
        }


        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            CustomLog.d(TAG, "消息数据库数据发生变更2:" + selfChange);

            int mergeInt = mergeThreads();
            // 刷新界面显示
            if (mergeInt == 1) {
                queryNoticeData(QueryConvstNoticeAsyncTask.QUERY_TYPE_COND);
            } else if (mergeInt == 2) {
                queryNoticeData(QueryConvstNoticeAsyncTask.QUERY_TYPE_PAGE);
            }
        }
    }


    private synchronized int mergeThreads() {
        if (frameType == VALUE_NOTICE_FRAME_TYPE_NUBE) {
            ThreadsBean th = threadDao.getThreadByRecipentIds(targetNubeNumber);
            if (th != null) {
                CustomLog.d("TAG", "已产生会话，则并入已有会话");
                // 已产生会话，则并入已有会话
                convstId = th.getId();
                convstExtInfo = th.getExtendInfo();
                frameType = VALUE_NOTICE_FRAME_TYPE_LIST;
                return 2;
            } else {
                return 0;
            }
        } else {
            return 1;
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (Activity.RESULT_CANCELED == resultCode) {
            return;
        }

        switch (requestCode) {
            case SendCIVMUtil.ACTION_SHARE_PIC_FROM_CAMERA:
                CustomLog.d(TAG, "拍摄图片 返回");
                if (conversationType == VALUE_CONVERSATION_TYPE_MULTI) {
                    SendCIVMUtil.onSendPicFromCameraBack(this, selfNubeNumber,
                            groupId, groupId);
                } else {
                    SendCIVMUtil.onSendPicFromCameraBack(this, selfNubeNumber,
                            getReceivers(), convstId);
                }
                //                MobclickAgent.onEvent(ChatActivity.this,
                //                        UmengEventConstant.EVENT_SEND_PIC);
                break;
            case SendCIVMUtil.ACTION_SHARE_VIDEO_FROM_CAMERA:
                CustomLog.d(TAG, "拍摄视频 返回");
                if (conversationType == VALUE_CONVERSATION_TYPE_MULTI) {
                    SendCIVMUtil.onSendVideoFromCameraBack(this, data,
                            selfNubeNumber, groupId, groupId);
                } else {
                    SendCIVMUtil.onSendVideoFromCameraBack(this, data,
                            selfNubeNumber, getReceivers(), convstId);
                }
                //                MobclickAgent.onEvent(ChatActivity.this,
                //                        UmengEventConstant.EVENT_SEND_VEDIO);
                break;
            case SendCIVMUtil.ACTION_SHARE_PIC_FROM_NATIVE:
                CustomLog.d(TAG, "选择图片 返回");
                if (conversationType == VALUE_CONVERSATION_TYPE_MULTI) {
                    SendCIVMUtil.onSendPicFromNativeBack(this, data,
                            selfNubeNumber, groupId, groupId);
                } else {
                    SendCIVMUtil.onSendPicFromNativeBack(this, data,
                            selfNubeNumber, getReceivers(), convstId);
                }
                //                MobclickAgent.onEvent(ChatActivity.this,
                //                        UmengEventConstant.EVENT_SEND_PIC);
                break;
            case SendCIVMUtil.ACTION_SHARE_VIDEO_FROM_NATIVE:
                CustomLog.d(TAG, "选择视频 返回");
                if (conversationType == VALUE_CONVERSATION_TYPE_MULTI) {
                    SendCIVMUtil.onSendVideoFromNativeBack(this, data,
                            selfNubeNumber, groupId, groupId);
                } else {
                    SendCIVMUtil.onSendVideoFromNativeBack(this, data,
                            selfNubeNumber, getReceivers(), convstId);
                }
                //                MobclickAgent.onEvent(ChatActivity.this,
                //                        UmengEventConstant.EVENT_SEND_VEDIO);
                break;
            case SendCIVMUtil.ACTION_SHARE_VCARD:
                sendVcardBack(data);
                //                MobclickAgent.onEvent(ChatActivity.this,
                //                        UmengEventConstant.EVENT_SEND_VCARD);
                break;
            case SendCIVMUtil.ACTION_FOR_RESERVE_MEETING:
                CustomLog.d(TAG, "预约会议结束后，返回到chat页面");
                break;
            //		case REQUEST_CODE_SELECT_RECEIVER:
            //			// 选择收件人
            //			if (data == null) {
            //				return;
            //			}
            //			Bundle selRes = data.getExtras();
            //			if (selRes != null) {
            //				// // 选择收件人返回，收件人变为展开状态
            //				// receiverInputFocus.setVisibility(View.GONE);
            //				// receiverInput.setVisibility(View.VISIBLE);
            //
            //				// 初始化收件人相关数据
            //				receiverNameMap.clear();
            //
            //				final ArrayList<String> selectNickNames = selRes
            //						.getStringArrayList(SelectLinkManActivity.START_RESULT_NICKNAME);
            //				final ArrayList<String> selectName = selRes
            //						.getStringArrayList(SelectLinkManActivity.START_RESULT_NAME);
            //				final ArrayList<String> selectNumber = selRes
            //						.getStringArrayList(SelectLinkManActivity.START_RESULT_NUMBER);
            //				receiverNumberLst = selRes
            //						.getStringArrayList(SelectLinkManActivity.START_RESULT_NUBE);
            //
            //				if (selectName == null
            //						|| selectName.size() != receiverNumberLst.size()) {
            //					LogUtil.d("选择收件人返回数据不整合");
            //					return;
            //				}
            //				if (selectNickNames == null
            //						|| selectNickNames.size() != receiverNumberLst.size()) {
            //					LogUtil.d("选择收件人返回数据不整合");
            //					return;
            //				}
            //				if (selectNumber == null
            //						|| selectNumber.size() != receiverNumberLst.size()) {
            //					LogUtil.d("选择收件人返回数据不整合");
            //					return;
            //				}
            //
            //				for (int i = 0; i < receiverNumberLst.size(); i++) {
            //					// 收件人名称
            //					String nubeNum = receiverNumberLst.get(i);
            //					String name = selectName.get(i);// 备注名
            //					if (TextUtils.isEmpty(name)) {
            //						name = selectNickNames.get(i);// 昵称
            //						if (TextUtils.isEmpty(name)) {
            //							name = selectNumber.get(i);// 手机号
            //							if (TextUtils.isEmpty(name)) {
            //								name = nubeNum;
            //							}
            //						}
            //					}
            //					receiverNameMap.put(nubeNum, name);
            //				}
            //
            //				setReceiverDisp();
            //			}
            //			// MobclickAgent.onEvent(ChatActivity.this,
            //			// CommonConstant.UMENG_KEY_W_NewMsg_SelectContacts_End);
            //			break;
            // 修改转发逻辑：走本地分享的交互设计
            case ACTION_FORWARD_NOTICE:
                if (data == null) {
                    return;
                }
                Bundle bundle = data.getExtras();
                String nubeNumb = bundle.getStringArrayList(
                        SelectLinkManActivity.START_RESULT_NUBE).get(0);
                String name = bundle.getStringArrayList(
                        SelectLinkManActivity.START_RESULT_NAME).get(0);
                String niName = bundle.getStringArrayList(
                        SelectLinkManActivity.START_RESULT_NICKNAME).get(0);
                String nuber = bundle.getStringArrayList(
                        SelectLinkManActivity.START_RESULT_NUMBER).get(0);
                NameElement element = ShowNameUtil.getNameElement(name, niName,
                        nuber, nubeNumb);
                String nicName = ShowNameUtil.getShowName(element);
                selectNubeList.add("@" + nubeNumb + IMConstant.SPECIAL_CHAR);
                selectNameList.add("@" + nicName + IMConstant.SPECIAL_CHAR);
                inputFragment.setSpecialtxt(nicName);
                // String nubNumber = bundle
                // .getString(SelectGroupMemeberActivity.START_RESULT_NUMBER);
                // String nicName = bundle
                // .getString(SelectGroupMemeberActivity.START_RESULT_NAME);
                // selectNubeList.add("@" + nubNumber + ComConstant.SPECIAL_CHAR);
                // selectNameList.add("@" + nicName + ComConstant.SPECIAL_CHAR);
                // noticeCommentMenu.setSpecialtxt(nicName);
                break;
            // if (data == null) {
            // return;
            // }
            // // 转发消息
            // if (TextUtils.isEmpty(forwardNoticeId)) {
            // LogUtil.d("待转发消息数据丢失");
            // return;
            // }
            //
            // final ArrayList<String> nNames = data
            // .getExtras()
            // .getStringArrayList(SelectLinkManActivity.START_RESULT_NAME);
            // final ArrayList<String> nicNames = data.getExtras()
            // .getStringArrayList(
            // SelectLinkManActivity.START_RESULT_NICKNAME);
            // final ArrayList<String> pNumbers = data.getExtras()
            // .getStringArrayList(
            // SelectLinkManActivity.START_RESULT_NUMBER);
            // final ArrayList<String> nNumebrs = data
            // .getExtras()
            // .getStringArrayList(SelectLinkManActivity.START_RESULT_NUBE);
            // String nNumber = "";
            // if (nNumebrs != null && nNumebrs.size() > 0) {
            // nNumber = nNumebrs.get(0);
            // } else {
            // Toast.makeText(ChatActivity.this, R.string.toast_no_vcard,
            // Toast.LENGTH_SHORT).show();
            // LogUtil.d("转发消息时，未选中任何转发联系人");
            // return;
            // }
            // String nName = "";
            // if (nNames != null && nNames.size() > 0) {
            // nName = nNames.get(0);
            // }
            // String pNumber = "";
            // if (pNumbers != null && pNumbers.size() > 0) {
            // pNumber = pNumbers.get(0);
            // }
            // String nicName = "";
            // if (nicNames != null && nicNames.size() > 0) {
            // nicName = nicNames.get(0);
            // }
            // CommonDialog conDlg = new CommonDialog(ChatActivity.this,
            // getLocalClassName(), 12346);
            // conDlg.setCancelable(false);
            // NameElement nameElement = ShowNameUtil.getNameElement(nName,
            // nicName, pNumber, nNumber);
            // String nickNam = ShowNameUtil.getShowName(nameElement);
            // conDlg.setTitle(R.string.commomdialog_title);
            // conDlg.setMessage(getString(R.string.confirm_forward_notice,
            // nickNam));
            // conDlg.setCancleButton(null, R.string.btn_cancle);
            // conDlg.setPositiveButton(new CommonDialog.BtnClickedListener() {
            //
            // @Override
            // public void onBtnClicked() {
            // NetPhoneApplication.getFileTaskManager().forwardMessage(
            // nNumebrs.get(0), forwardNoticeId);
            // Toast.makeText(ChatActivity.this, R.string.toast_sent,
            // Toast.LENGTH_SHORT).show();
            // }
            // }, R.string.foaword_yes);
            // conDlg.showDialog();
            // // ArrayList<String> nubes = data.getExtras().getStringArrayList(
            // // SelectLinkManActivity.START_RESULT_NUBE);
            // // if (nubes != null && nubes.size() > 0) {
            // // for (int i = 0; i < nubes.size(); i++) {
            // // NetPhoneApplication.getFileTaskManager().forwardMessage(
            // // nubes.get(i), forwardNoticeId);
            // // }
            // // Toast.makeText(ChatActivity.this, R.string.toast_sent,
            // // Toast.LENGTH_SHORT).show();
            // // } else {
            // // LogUtil.d("未选择接收人，无法转发");
            // // }
            // break;
        }
    }


    /**
     * 选择完名片，返回到本页的操作
     */
    private void sendVcardBack(Intent data) {
        // CustomLog.begin("");
        if (data == null) {
            CustomLog.d(TAG, "data == null");
            return;
        }

        ArrayList<String> nubeNumebrs = data.getExtras().getStringArrayList(
                SelectLinkManActivity.START_RESULT_NUBE);
        String nubeNumber = "";
        if (nubeNumebrs != null && nubeNumebrs.size() > 0) {
            nubeNumber = nubeNumebrs.get(0);
        }
        if (TextUtils.isEmpty(nubeNumber)) {
            tipToast("数据不合法");
            return;
        }

        ContactFriendBean Info = new NetPhoneDaoImpl(this)
                .queryFriendInfoByNube(nubeNumber);
        ButelVcardBean extInfo = new ButelVcardBean();
        extInfo.setUserId(Info.getContactId());
        extInfo.setNubeNumber(Info.getNubeNumber());
        extInfo.setHeadUrl(Info.getHeadUrl());
        extInfo.setNickname(Info.getNickname());
        extInfo.setPhoneNumber(Info.getNumber());
        extInfo.setSex(Info.getSex());
        showSendVcardDialog(extInfo);

    }


    /**
     * 显示二次 确认对话框
     */
    private void showSendVcardDialog(final ButelVcardBean bean) {
        CustomLog.d(TAG, "showSendVcardDialog begin");
        CommonDialog confDlg = new CommonDialog(ChatActivity.this,
                getLocalClassName(), 12346);
        confDlg.setCancelable(false);
        String nickN = ShowNameUtil
                .getShowName(ShowNameUtil.getNameElement("",
                        bean.getNickname(), bean.getPhoneNumber(),
                        bean.getNubeNumber()));
        confDlg.setTitle(R.string.send_Vcard_dialog_title);
        confDlg.setMessage(getString(R.string.confirm_send_vcard, nickN));
        confDlg.setCancleButton(null, R.string.btn_cancle);
        confDlg.setPositiveButton(new CommonDialog.BtnClickedListener() {
            @Override
            public void onBtnClicked() {
                CustomLog.d(TAG, "点击确定");
                if (conversationType == VALUE_CONVERSATION_TYPE_MULTI) {
                    SendCIVMUtil.onSendVcardBack(ChatActivity.this, bean,
                            selfNubeNumber, groupId, groupId);
                } else {
                    SendCIVMUtil.onSendVcardBack(ChatActivity.this, bean,
                            selfNubeNumber, getReceivers(), convstId);
                }
                CustomToast.show(mContext, "已发送", 1);
            }
        }, R.string.btn_send);

        if (!isFinishing()) {
            confDlg.showDialog();
        }
        // CustomLog.end("");
    }


    private String getReceivers() {
        if (frameType == VALUE_NOTICE_FRAME_TYPE_LIST
                || frameType == VALUE_NOTICE_FRAME_TYPE_NUBE) {
            return targetNubeNumber;
        } else {
            // 新建消息的场合，接收者为收件人输入框数据
            if (receiverNumberLst != null && receiverNumberLst.size() > 0) {
                String nubes = "";
                for (String nubeNum : receiverNumberLst) {
                    nubes = nubes + nubeNum + ";";
                }
                return nubes.substring(0, nubes.length() - 1);
            } else {
                return "";
            }
        }
    }

    //	private String getReceiverDispName(String name) {
    //		if (TextUtils.isEmpty(name)) {
    //			return "";
    //		} else if (name.length() > 10) {
    //			return name.substring(0, 10) + "...";
    //		} else {
    //			return name;
    //		}
    //	}


    @Override
    public void onMsgDelete(String uuid, long receivedTime, int dataCnt) {
        CustomLog.d(TAG, "删除消息:" + uuid);
        MedicalApplication.getFileTaskManager().cancelTask(uuid);
        if (dataCnt == 1) {
            // 最后一条消息删除时，需要更新会话表lastTime
            noticeDao.deleteLastNotice(uuid, convstId, receivedTime);
        } else {
            noticeDao.deleteNotice(uuid);
        }
    }


    @Override
    public void onMsgForward(String uuid, String sender, int msgType,
                             int msgStatus, String localPath) {
        CustomLog.d(TAG, "转发消息:" + uuid);

        forwardNoticeId = uuid;
        Intent i = new Intent(this, ShareLocalActivity.class);
        i.putExtra(ShareLocalActivity.KEY_ACTION_FORWARD, true);
        i.putExtra(ShareLocalActivity.MSG_ID, forwardNoticeId);
        startActivity(i);
        // 修改转发结束逻辑：消息转发后，界面退回到当前聊天界面，成功发送并toast显示：已发送
    }


    @Override
    public void onMoreClick(String uuid, int msgType, int msgStatus, boolean checked) {
        showMoreOpLayout();
        if (chatAdapter != null && chatAdapter.hasCheckedData()) {
            enableMoreOp(true);
            titlebackbtn = true;
            newNoticeNumflag = true;
            if (titlebackbtn) {
                //                getTitleBar().setBackText("取消");
                backbtn.setVisibility(View.GONE);
                newNoticeNum.setVisibility(View.GONE);
                backtext.setVisibility(View.VISIBLE);
                backtext.setText("取消");
                backtext.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (chatAdapter != null && chatAdapter.isMultiCheckMode()) {
                            hideMoreOpLayout();
                            cleanCheckData();
                        }
                        titlebackbtn = false;
                        newNoticeNumflag = false;
                        backbtn.setVisibility(View.VISIBLE);
                        //                        updateNoticesInfo();
                        backtext.setVisibility(View.GONE);
                        getTitleBar().setBack(null, new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                if (CommonUtil.isFastDoubleClick()) {
                                    return;
                                }
                                if (waitDialog != null && waitDialog.isShowing()) {
                                    //                    hideWaitDialog();
                                    removeLoadingView();
                                    // ButelMeetingManager.getInstance().cancelCreateMeeting(
                                    //     ButelContactDetailActivity.class.getName());
                                }
                                exitActivity();
                            }
                        });
                    }
                });
            }

        } else {
            enableMoreOp(false);
        }
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return inputFragment.handleRecordLayoutTouchEvent(event);
    }


    private void preCheckDataforForward() {
        LinkedHashMap<String, NoticesBean> uidMap = null;
        if (chatAdapter != null) {
            uidMap = chatAdapter.getCheckedDataMap();
        }
        if (uidMap != null) {
            boolean hasInvalidData = false;
            List<String> validList = new ArrayList<String>();
            Iterator<Map.Entry<String, NoticesBean>> entries = uidMap.entrySet()
                    .iterator();
            while (entries.hasNext()) {
                Map.Entry<String, NoticesBean> entry = entries.next();
                String uid = entry.getKey();
                NoticesBean bean = entry.getValue();

                CustomLog.d(TAG, "uid = " + uid);
                boolean valid = true;
                if (bean != null) {
                    int type = bean.getType();
                    int status = bean.getStatus();
                    CustomLog.d(TAG, "type = " + type + " status=" + status);
                    if (type == FileTaskManager.NOTICE_TYPE_AUDIO_SEND
                            || type == FileTaskManager.NOTICE_TYPE_RECORD) {
                        hasInvalidData = true;
                        valid = false;
                    }
                    if (status != FileTaskManager.TASK_STATUS_SUCCESS) {
                        hasInvalidData = true;
                        valid = false;
                    }
                    if (valid) {
                        validList.add(uid);
                    }
                } else {
                    continue;
                }

            }

            if (hasInvalidData) {
                //弹提醒对话框
                invalidForwardDialog(validList);
            } else {
                //直接转发
                doForwardWork(validList);
            }
        }
    }


    private void preCheckDataforCollection() {
        Map<String, NoticesBean> uidMap = null;
        if (chatAdapter != null) {
            uidMap = chatAdapter.getCheckedDataMap();
        }
        if (uidMap != null) {
            boolean hasInvalidData = false;
            List<NoticesBean> validList = new ArrayList<NoticesBean>();
            Iterator<Map.Entry<String, NoticesBean>> entries = uidMap.entrySet()
                    .iterator();
            while (entries.hasNext()) {
                Map.Entry<String, NoticesBean> entry = entries.next();
                String uid = entry.getKey();
                NoticesBean bean = entry.getValue();

                CustomLog.d(TAG, "uid = " + uid);
                boolean valid = true;

                if (bean != null) {
                    int type = bean.getType();
                    int status = bean.getStatus();
                    CustomLog.d(TAG, "type = " + type + " status=" + status);
                    if (type == FileTaskManager.NOTICE_TYPE_VCARD_SEND
                            || type == FileTaskManager.NOTICE_TYPE_RECORD
                            || type == FileTaskManager.NOTICE_TYPE_MEETING_INVITE
                            || type == FileTaskManager.NOTICE_TYPE_MEETING_BOOK) {
                        hasInvalidData = true;
                        valid = false;
                    }

                    if (valid) {
                        validList.add(bean);
                    }
                } else {
                    continue;
                }

            }

            if (hasInvalidData) {
                //弹提醒对话框
                invalidCollectDialog(validList);
            } else {
                //直接收藏
                doCollectWork(validList);
            }
        }
    }


    private void preCheckDataforDel() {
        Map<String, NoticesBean> uidMap = null;
        if (chatAdapter != null) {
            uidMap = chatAdapter.getCheckedDataMap();
        }
        if (uidMap != null) {
            //boolean hasInvalidData = false;
            List<String> validList = new ArrayList<String>();
            for (String key : uidMap.keySet()) {
                validList.add(key);
                CustomLog.d(TAG, "uid = " + key);
            }

            delDialog(validList, chatAdapter.getCount());
        }
    }


    private void invalidForwardDialog(final List<String> uidList) {
        // CustomLog.begin("");
        CommonDialog confDlg = new CommonDialog(ChatActivity.this,
                getLocalClassName(), 12350);
        confDlg.setCancelable(false);
        confDlg.setTitle(R.string.send_Vcard_dialog_title);
        confDlg.setMessage("语音、通话记录、未发送消息与其他特殊类消息不能转发");
        confDlg.setCancleButton(null, R.string.btn_cancle);
        confDlg.setPositiveButton(new CommonDialog.BtnClickedListener() {
            @Override
            public void onBtnClicked() {
                CustomLog.d(TAG, "点击转发");
                doForwardWork(uidList);
            }
        }, "转发");

        if (!isFinishing()) {
            confDlg.showDialog();
        }
        // CustomLog.end("");
    }


    private void invalidCollectDialog(final List<NoticesBean> dataList) {
        // CustomLog.begin("");
        CommonDialog confDlg = new CommonDialog(ChatActivity.this,
                getLocalClassName(), 12351);
        confDlg.setCancelable(false);
        confDlg.setTitle(R.string.send_Vcard_dialog_title);
        confDlg.setMessage("名片、通话记录、会议预约记录、会议召开记录与其他特殊类消息不能收藏");
        confDlg.setCancleButton(null, R.string.btn_cancle);
        confDlg.setPositiveButton(new CommonDialog.BtnClickedListener() {
            @Override
            public void onBtnClicked() {
                CustomLog.d(TAG, "点击收藏");
                doCollectWork(dataList);
            }
        }, "收藏");

        if (!isFinishing()) {
            confDlg.showDialog();
        }
        // CustomLog.end("");
    }


    private void delDialog(final List<String> uidList, int listCount) {
        // CustomLog.begin("");
        CommonDialog confDlg = new CommonDialog(ChatActivity.this,
                getLocalClassName(), 12352);
        confDlg.setCancelable(false);
        confDlg.setTitle(R.string.send_Vcard_dialog_title);
        confDlg.setMessage("确定删除已选消息?");
        confDlg.setCancleButton(null, R.string.btn_cancle);
        confDlg.setPositiveButton(new CommonDialog.BtnClickedListener() {
            @Override
            public void onBtnClicked() {
                CustomLog.d(TAG, "点击确定");
                noticeDao.deleteNotices(uidList);
                hideMoreOpLayout();
                cleanCheckData();
            }
        }, R.string.btn_ok);

        if (!isFinishing()) {
            confDlg.showDialog();
        }
        // CustomLog.end("");
    }


    private void doCollectWork(List<NoticesBean> dataList) {
        if (dataList != null && dataList.size() > 0) {
            CollectionManager.getInstance().addCollectionByNoticesBeans(dataList);
            Toast.makeText(ChatActivity.this, "已收藏",
                    Toast.LENGTH_SHORT).show();
        }
        hideMoreOpLayout();
        cleanCheckData();
    }


    private void doForwardWork(List<String> uidList) {
        if (uidList != null && uidList.size() > 0) {
            Intent i = new Intent(this, ShareLocalActivity.class);
            i.putExtra(ShareLocalActivity.KEY_ACTION_FORWARD, true);
            i.putExtra(ShareLocalActivity.MSG_ID,
                    StringUtil.list2String(uidList, ','));
            startActivity(i);
        }
        //        hideMoreOpLayout();
        //        cleanCheckData();
    }


    private void cleanCheckData() {
        if (chatAdapter != null) {
            chatAdapter.cleanCheckedData();
        }
    }


    private void showMoreOpLayout() {
        if (moreOpLayout.getVisibility() == View.VISIBLE) {
            return;
        }
        if (chatAdapter != null && !chatAdapter.isMultiCheckMode()) {
            chatAdapter.setMultiCheckMode(true);
            if (inputFragment != null) {
                inputFragment.setHide();
                //				getSupportFragmentManager().beginTransaction()
                //                .hide(inputFragment).commit();
            }
            moreOpLayout.setVisibility(View.VISIBLE);
        }
    }


    private void hideMoreOpLayout() {
        if (chatAdapter != null && chatAdapter.isMultiCheckMode()) {
            moreOpLayout.setVisibility(View.GONE);
            chatAdapter.setMultiCheckMode(false);
            //			if(inputFragment!=null){
            //				getSupportFragmentManager().beginTransaction()
            //                .show(inputFragment).commit();
            //				inputFragment.setHide();
            //			}
            backbtn.setVisibility(View.VISIBLE);
            updateNoticesInfo();
            backtext.setVisibility(View.GONE);
            getTitleBar().setBack(null, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (CommonUtil.isFastDoubleClick()) {
                        return;
                    }
                    if (waitDialog != null && waitDialog.isShowing()) {
                        //                    hideWaitDialog();
                        removeLoadingView();
                        // ButelMeetingManager.getInstance().cancelCreateMeeting(
                        //     ButelContactDetailActivity.class.getName());
                    }
                    exitActivity();
                }
            });

        }
    }


    private void initMoreOpWidget() {

        moreOpLayout = (RelativeLayout) this.findViewById(R.id.more_op_layout);
        forwardBtn = (ImageButton) this.findViewById(R.id.chat_more_forward_btn);
        forwardBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                //Toast.makeText(getBaseContext(), "转发", Toast.LENGTH_SHORT).show();
                preCheckDataforForward();
            }
        });
        collectBtn = (ImageButton) this.findViewById(R.id.chat_more_collect_btn);
        collectBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                //Toast.makeText(getBaseContext(), "收藏", Toast.LENGTH_SHORT).show();
                preCheckDataforCollection();
            }
        });
        delBtn = (ImageButton) this.findViewById(R.id.chat_more_del_btn);
        delBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                //Toast.makeText(getBaseContext(), "删除", Toast.LENGTH_SHORT).show();
                preCheckDataforDel();
            }
        });
    }


    private void enableMoreOp(boolean enable) {
        forwardBtn.setEnabled(enable);
        collectBtn.setEnabled(enable);
        delBtn.setEnabled(enable);
    }


    /**
     * 添加对方为好友，先调searchAccount查询该好友的详细信息，然后插入好友表
     */
    private void addLinkmanBySearch(final String nubeNumber) {
        MDSAppSearchUsers searchUsers = new MDSAppSearchUsers() {
            @Override
            protected void onSuccess(List<MDSDetailInfo> responseContent) {

                if (cn.redcdn.hvs.util.CommonUtil.isFastDoubleClick()) {
                    return;
                }
                if (responseContent == null) {
                    CustomLog.d(TAG, "responseContent is null");
                    CustomToast.show(ChatActivity.this, "responseContent is null", 1);
                    return;
                } else if (responseContent.size() == 0) {
                    CustomLog.d(TAG, "responseContent size is 0");
                    CustomToast.show(ChatActivity.this, "responseContent size is 0", 1);
                    return;
                }

                // CustomToast.show(ChatActivity.this,
                //     "onSuccess" + " list size:" + responseContent.size(), 1);
                currentInfo = responseContent.get(0);

                CustomLog.d(TAG, "已添加成功");
                addSuccess(currentInfo.getNickName(),
                        currentInfo.getNickName(),
                        currentInfo.getNubeNumber());
                // IMCommonUtil.addFriendTxt(ChatActivity.this,
                //     targetShortName, currentInfo);

                addtoLocalContact(null, 0);
            }


            @Override
            protected void onFail(int statusCode, String statusInfo) {
                CustomToast.show(ChatActivity.this,
                        "onFail" + "statusCode:" + statusCode + " statusInfo:" + statusInfo, 1);
            }

        };

        //设置searchType 为 Nube
        int searchType = 3;
        searchUsers.appSearchUsers(AccountManager.getInstance(this)
                .getToken(), searchType, new String[] { nubeNumber });

    }


    private void addtoLocalContact(String contactId, int isAddFrom) {
        final Contact newContact = new Contact();
        CustomLog.d(TAG, "nickname=" + currentInfo.nickName + "serviceType=");
        if (currentInfo.nickName != null && !currentInfo.nickName.isEmpty()) {
            newContact.setNickname(currentInfo.nickName);
            newContact.setName(currentInfo.nickName);
            newContact.setNubeNumber(currentInfo.nubeNumber);
            newContact.setContactId(currentInfo.uid);
            newContact.setFirstName(StringHelper.getHeadChar(currentInfo.nickName));
        } else {
            CustomLog.d(TAG,
                    "addtoLocalContact name 未命名=" + StringHelper.getHeadChar("未命名"));
            newContact.setNickname("未命名");
            newContact.setName("未命名");
            newContact.setNubeNumber(currentInfo.nubeNumber);
            newContact.setContactId(CommonUtil.getUUID());
            newContact.setFirstName(StringHelper.getHeadChar("未命名"));
        }

        // 設置usertype
        newContact.setAppType("mobile");
        newContact.setPicUrl(currentInfo.headThumUrl);
        newContact.setUserType(1);
        newContact.setNumber(currentInfo.officTel);
        newContact.setUserFrom(ADD_USER_TYPE);
        newContact.setPicUrl(currentInfo.headThumUrl);
        newContact.setContactId(CommonUtil.getUUID());

        //添加医疗云平台字段
        newContact.setWorkUnit(currentInfo.getWorkUnit());
        newContact.setWorkUnitType(Integer.valueOf(currentInfo.getWorkUnitType()));
        newContact.setDepartment(currentInfo.getDepartment());
        newContact.setProfessional(currentInfo.getProfessional());
        newContact.setOfficeTel(currentInfo.getOfficTel());

        CustomLog.d(TAG, "addtoLocalContact start");
        ContactManager.getInstance(ChatActivity.this).
                addContact(newContact,
                        new ContactCallback() {
                            @Override
                            public void onFinished(ResponseEntry result) {
                                CustomLog.i(TAG, "onFinish! status: " + result.status
                                        + " | content: " + result.content);
                                if (result.status == -100) {
                                    CustomToast.show(mContext, "不能添加自己", 1);
                                }

                            }

                        });

    }


    private void addSuccess(String name, String nickName, String number) {
        Toast.makeText(ChatActivity.this, R.string.toast_add_linkman_ok,
                Toast.LENGTH_SHORT).show();
        if (conversationType == VALUE_CONVERSATION_TYPE_SINGLE) {
            if (targetNubeNumber.equals(addFriendNube)) {
                // 隐藏加为好友行
                findViewById(R.id.add_friend_line).setVisibility(View.GONE);
                // 显示新的名称
                // targetShortName = name;
                // if (TextUtils.isEmpty(targetShortName)) {
                // targetShortName = nickName;
                // if (TextUtils.isEmpty(targetShortName)) {
                // targetShortName = number;
                // if(TextUtils.isEmpty(targetShortName)){
                // targetShortName = targetNubeNumber;
                // }
                // }
                // }

                NameElement element = ShowNameUtil.getNameElement(name,
                        nickName, number, addFriendNube);
                targetShortName = ShowNameUtil.getShowName(element);

                getTitleBar().setTitle(targetShortName);
            }
        }
    }


    private void addFailure(int type) {
        CustomLog.d(TAG, "type:" + type);

        if (conversationType == VALUE_CONVERSATION_TYPE_SINGLE) {
            if (targetNubeNumber.equals(addFriendNube)) {
                if (type == -1) {
                    Toast.makeText(ChatActivity.this,
                            R.string.toast_add_linkman_nok, Toast.LENGTH_SHORT)
                            .show();
                } else if (type == -2) {
                    // 用户不存在
                    Toast.makeText(ChatActivity.this,
                            R.string.toast_add_linkman_nouser,
                            Toast.LENGTH_SHORT).show();
                }
            }
        }
    }


    /*
     * 监听群信息和群成员表
     */
    private class GroupMemberObserver extends ContentObserver {

        public GroupMemberObserver() {
            super(new Handler());
        }


        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            CustomLog.d(TAG, "t_multi_chat_groups 群组数据库数据发生变更");
            String gruopName = groupDao.getGroupNameByGidTitle(groupId);
            getTitleBar().setTitle(gruopName);
            dateList.clear();
            dateList.putAll(groupDao.queryGroupMembers(groupId));
        }
    }


    private class GroupObserver extends ContentObserver {

        public GroupObserver() {
            super(new Handler());
        }


        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            CustomLog.d(TAG, "t_multi_chat_users 群成员表数据库数据发生变更");
            String gruopName = groupDao.getGroupNameByGidTitle(groupId);
            int groupSize;
            groupSize = groupDao.queryGroupMemberCnt(groupId);
            getTitleBar().setTitle(gruopName);
            getTitleBar().setSubTitle("(" + groupSize + ")");
            if (groupDao.isGroupMember(groupId, selfNubeNumber)) {
                getTitleBar().enableRightBtn(null,
                        R.drawable.multi_send_btn_selector,
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                // MobclickAgent
                                // .onEvent(
                                // ChatActivity.this,
                                // CommonConstant.UMENG_KEY_W_MsgPage_Recipients);
                                // 跳转到群发收件人界面
                                if (CommonUtil.isFastDoubleClick()) {
                                    return;
                                }

                                if (groupDao.isGroupMember(groupId,
                                        selfNubeNumber)) {
                                    // 是群成员，跳转

                                    // 群发，跳到群发联系人页面
                                    Intent intent = new Intent(
                                            ChatActivity.this,
                                            GroupChatDetailActivity.class);
                                    intent.putExtra(
                                            GroupChatDetailActivity.KEY_CHAT_TYPE,
                                            GroupChatDetailActivity.VALUE_GROUP);
                                    intent.putExtra(
                                            KEY_NUMBER,
                                            groupId);
                                    ChatActivity.this.startActivity(intent);

                                    CustomLog.d(TAG, "点击群发图标，跳转到群发收件人界面");
                                }
                            }
                        });
            } else {
                getTitleBar().setRightBtnVisibility(View.GONE);
            }
        }
    }


    class myString extends SpannableStringBuilder {

    }


    /*
     * 获取群名称表
     */
    private String getGroupName() {
        String gruopName = groupDao.getGroupNameByGid(groupId);
        //如果群名称超过15字，截取，以便显示免打扰铃铛
        gruopName = checkGroupNameLength(gruopName);
        isGroupMember = groupDao.isGroupMember(groupId, selfNubeNumber);
        groupMemberSize = groupDao.queryGroupMemberCnt(groupId);
        return gruopName;
    }


    private String getGroupNameTitle() {
        String gruopName = groupDao.getGroupNameByGidTitle(groupId);
        //如果群名称超过15字，截取，以便显示免打扰铃铛
        gruopName = checkGroupNameLength(gruopName);
        isGroupMember = groupDao.isGroupMember(groupId, selfNubeNumber);
        groupMemberSize = groupDao.queryGroupMemberCnt(groupId);
        return gruopName;
    }


    private String checkGroupNameLength(String groupName) {
        if (groupName.length() > 8) {
            groupName = groupName.substring(0, 8) + "...";
            return groupName;
        }
        return groupName;
    }


    @Override
    public void onSetSelectMemeber(String name, String nube) {
        // 选择@回復的成員
        selectNubeList.add("@" + nube + IMConstant.SPECIAL_CHAR);
        selectNameList.add("@" + name + IMConstant.SPECIAL_CHAR);
        inputFragment.setSpecialtxt("@" + name);
    }


    private ArrayList<ContactFriendBean> GroupMemberToContactsBean() {
        ArrayList<ContactFriendBean> List = new ArrayList<ContactFriendBean>();
        ContactFriendBean data;
        Iterator<Map.Entry<String, GroupMemberBean>> iter = dateList.entrySet()
                .iterator();
        while (iter.hasNext()) {
            GroupMemberBean bean = iter.next().getValue();
            if (!bean.getNubeNum().equals(selfNubeNumber)) {
                data = new ContactFriendBean();
                data.setHeadUrl(bean.getHeadUrl());
                data.setName(bean.getDispName());
                data.setNickname(bean.getNickName());
                data.setNumber(bean.getPhoneNum());
                data.setNubeNumber(bean.getNubeNum());
                data.setSex(
                        (bean.getGender() == GroupMemberTable.GENDER_MALE ? GroupMemberTable.GENDER_MALE
                                : GroupMemberTable.GENDER_FEMALE)
                                + "");
                data.setPym(PinyinUtil.getPinYin(bean.getDispName())
                        .toUpperCase());
                List.add(data);
            }
        }
        ListSort<ContactFriendBean> listSort = new ListSort<ContactFriendBean>();
        listSort.Sort(List, "getPym", null);
        return List;
    }

    // @Override
    // public void modifyClipText() {
    // if (conversationType == VALUE_CONVERSATION_TYPE_MULTI) {
    // ArrayList<String> dispNubeList = new ArrayList<String>();
    // ClipboardManager clip = (ClipboardManager) this
    // .getSystemService(Context.CLIPBOARD_SERVICE);
    // String clipTxt = clip.getText().toString();
    // dispNubeList = CommonUtil.getDispList(clipTxt);
    // for (int i = 0; i < dispNubeList.size(); i++) {
    // char ch = 3;
    // GroupMemberBean gbean = groupDao.queryGroupMember(groupId,
    // dispNubeList.get(i));
    // NameElement element = ShowNameUtil.getNameElement(
    // gbean.getName(), gbean.getNickName(),
    // gbean.getPhoneNum(), gbean.getNubeNum());
    // String MName = ShowNameUtil.getShowName(element);
    // selectNameList.add("@" + MName + ch);
    // selectNubeList.add("@" + dispNubeList.get(i) + ch);
    // clipTxt = clipTxt.replace("@" + dispNubeList.get(i) + ch, "@"
    // + MName + ch);
    // clip.setText(clipTxt);
    // }
    // }else{
    // return;
    // }
    // }

    //    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
    //
    //        @Override
    //        public void onReceive(Context context, Intent intent) {
    //            String action = intent.getAction();
    //            if (MeetingBroadcastSender.getOnCreateMeetingActionName(context)
    //                .equals(action)) {
    //                CustomLog.d(TAG, "创建会议,加入会议成功后的回调" + action);
    //                removeLoadingView();
    //                String targetId = targetNubeNumber;
    //                if (conversationType == VALUE_CONVERSATION_TYPE_MULTI) {
    //                    targetId = groupId;
    //                }
    //                if (SendCIVMUtil.onConveneMeetingBack(TAG, conversationType,
    //                    targetId, intent)) {
    //                    removeLoadingView();
    //                }
    //            }
    //        }
    //    };

    //    private void initReceive() {
    //        IntentFilter filter = new IntentFilter();
    //        filter.addAction(MeetingBroadcastSender
    //            .getOnCreateMeetingActionName(this));
    //        this.registerReceiver(mReceiver, filter);
    //    }


    private static void tipToast(String txt) {
        CommonUtil.showToast(txt);
        CustomLog.d(TAG, txt);
    }

    // protected void showMeetingAlertDialog() {
    //     LogUtil.d("显示 立即召开/预约会议室 对话框");
    //     CommonDialog menuDlg = new CommonDialog(this);
    //     menuDlg.addButtonFirst(new MenuClickedListener() {
    //         @Override
    //         public void onMenuClicked() {
    //             conveneMeeting();
    //         }
    //     }, "立即召开");
    //
    //     menuDlg.addButtonSecond(new MenuClickedListener() {
    //         @Override
    //         public void onMenuClicked() {
    //             bookMeeting();
    //         }
    //     }, "预约会议室");
    //     menuDlg.show();
    // }


    private void updateNoticesInfo() {
        if (newNoticeNumflag) {
            newNoticeNum.setVisibility(View.GONE);
        } else {
            int count = noticeDao.getNewNoticeCount();
            if (count == 0) {
                newNoticeNum.setVisibility(View.INVISIBLE);
            } else {
                if (count > 99) {
                    newNoticeNum
                            .setBackgroundResource(R.drawable.butel_new_msg_flag);
                    newNoticeNum.setText(R.string.main_bottom_count_99);
                } else {
                    newNoticeNum
                            .setBackgroundResource(R.drawable.chat_unread_count);
                    newNoticeNum.setText(String.valueOf(count));
                }
                newNoticeNum.setVisibility(View.VISIBLE);
            }
        }
    }

}