package cn.redcdn.hvs.contacts.contact;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.TextView;

import com.umeng.analytics.MobclickAgent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.redcdn.buteldataadapter.DataExpand;
import cn.redcdn.datacenter.meetingmanage.CreateMeeting;
import cn.redcdn.hvs.AccountManager;
import cn.redcdn.hvs.MedicalApplication;
import cn.redcdn.hvs.R;
import cn.redcdn.hvs.base.BaseActivity;
import cn.redcdn.hvs.contacts.contact.butelDataAdapter.ContactSetImp;
import cn.redcdn.hvs.contacts.contact.interfaces.Contact;
import cn.redcdn.hvs.contacts.contact.interfaces.ContactCallback;
import cn.redcdn.hvs.contacts.contact.interfaces.ResponseEntry;
import cn.redcdn.hvs.contacts.contact.manager.ContactManager;
import cn.redcdn.hvs.meeting.meetingManage.MedicalMeetingManage;
import cn.redcdn.hvs.util.CustomToast;
import cn.redcdn.hvs.util.youmeng.AnalysisConfig;
import cn.redcdn.log.CustomLog;



public class HoldMutiMeetingActivity extends BaseActivity {
    // 会诊邀请人视讯号列表，手机号
    private ArrayList<String> phoneId = new ArrayList<String>();
    private boolean flag = false;
    private HashMap<String, Integer> selector;// 存放含有索引字母的位置
    private LinearLayout llViewPage = null;
    private final int CONTACT_LIST_COLUMN = 8;
    private TextView tvTemp = null;
    private String[] indexStr = { "A", "B", "C", "D", "E", "F", "G", "H",
        "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V",
        "W", "X", "Y", "Z", "#" };
    private ContactSetImp mContactSetImp = null;
    private Map<String, DataExpand> mExpandMap;
    private int height;// 字体高度
    private Button btnMutiPeopleMeeting = null;
    private ListView lvMutiPeopleList = null;
    private LinearLayout llMutiPeopleIndex = null;
    private Button btnMutiBack = null;
    private MutiListViewAdapter holdMeetingAdapter;
    private List<Contact> mMutiTopList = new ArrayList<Contact>();
    private int inviteCount = 0;
    /**** 定义通讯录的ContactPagerAdapterList */
    private ContactPagerAdapterList mContactPagerAdapterList = null;
    /**** 定义通讯录的mViewPagerList */
    private ViewPager mViewPagerList = null;
    private TextView tvSelect = null;
    /*** 定义消息类型 */
    private final int MSG_UPDATAUI = 0x77770000;
    private final int MSG_LOADINGDATA = 0x77770001;
    private CreateMeeting create = null;
    // 0初始状态1正常状态2destroy状态
    private int mCurrentState = 0;
    private String indexString = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 去标题栏
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        AddContactActivity.recommendCount = 0;
        setContentView(R.layout.activity_heldmutipeoplemeeting);
        start();
    }


    private void start() {
        CustomLog.i(TAG, "start");
        mHandler.sendEmptyMessage(MSG_LOADINGDATA);
    }


    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case MSG_LOADINGDATA:
                    initMutiData();
                    break;
                case MSG_UPDATAUI:
                    initMutiPeopleMeeting();

                default:
                    break;
            }

        }


        ;
    };


    private void initMutiData() {

        ContactManager.getInstance(HoldMutiMeetingActivity.this).getAllContacts(
            new ContactCallback() {

                @Override
                public void onFinished(ResponseEntry result) {
                    CustomLog.i(TAG, "onFinish! status: " + result.status
                        + " | content: " + result.content);
                    if (result.status >= 0) {
                        mContactSetImp = (ContactSetImp) result.content;
                        updataIndex(mContactSetImp);
                        updatemExpandMap();
                        mHandler.sendEmptyMessage(MSG_UPDATAUI);
                    }
                }
            }, true);

    }


    private void updataIndex(ContactSetImp contactSet) {
        CustomLog.i(TAG, "updataIndex");
        selector = new HashMap<String, Integer>();
        String headChar = new String();
        CustomLog.i(TAG, "getConunt" + contactSet.getCount());
        for (int i = 0; i < contactSet.getCount(); i++) {
            headChar = ((Contact) contactSet.getItem(i)).getFirstName();
            if (!indexString.contains(headChar)) {
                headChar = "#";
            }
            if (headChar != null) {
                if (!selector.containsKey(headChar)) {
                    selector.put(headChar, i);
                }

            }
        }

    }

    private void initMutiPeopleMeeting() {
        llViewPage = (LinearLayout) findViewById(R.id.llviewpage);
        tvTemp = (TextView) findViewById(R.id.tvtemp);
        tvTemp.bringToFront();
        tvTemp.setOnClickListener(mbtnHandleEventListener);
        tvSelect = (TextView) findViewById(R.id.tvselect);
        btnMutiBack = (Button) findViewById(R.id.btnmutiback);
        btnMutiPeopleMeeting = (Button) findViewById(R.id.btnmutipeoplemeeting);
        btnMutiPeopleMeeting.setClickable(false);
        btnMutiPeopleMeeting.setOnClickListener(mbtnHandleEventListener);
        btnMutiBack.setOnClickListener(mbtnHandleEventListener);
        llMutiPeopleIndex = (LinearLayout) this
            .findViewById(R.id.llmutipeopleindex);
        lvMutiPeopleList = (ListView) findViewById(R.id.lvmutipeoplelist);
        lvMutiPeopleList.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position,
                                    long id) {
                CustomLog.d(TAG, "ItemClick     " + position);
                ImageView ivSelected = (ImageView) view.findViewById(R.id.cbselect);
                ImageView ivUnSelected = (ImageView) view.findViewById(R.id.cbunselect);
                if (mContactSetImp != null) {
                    CustomLog.d(TAG, "newMutiContact size=" + mContactSetImp.getCount()
                        + "inviteCount=" + inviteCount);
                    if (!mExpandMap.get(((Contact) mContactSetImp.getItem(position))
                        .getNubeNumber()).isSelected) {
                        ivSelected.setVisibility(View.INVISIBLE);
                        ivUnSelected.setVisibility(View.VISIBLE);
                        if (inviteCount == 0) {
                            llViewPage.setVisibility(View.VISIBLE);
                            btnMutiPeopleMeeting.setClickable(true);
                            btnMutiPeopleMeeting.setTextColor(getResources().getColor(R.color.select_linkman_btn_ok_color));
                        }
                        btnMutiPeopleMeeting.setClickable(true);
                        Log.d("itemSelected  ", "count = " + inviteCount + "position = "
                            + position + "mMutiList.get(position)"
                            + ((Contact) mContactSetImp.getItem(position)).getNubeNumber()
                            + ((Contact) mContactSetImp.getItem(position)).getPicUrl());
                        inviteCount++;
                        mMutiTopList.add((Contact) mContactSetImp.getItem(position));
                        if (inviteCount > 99) {
                            btnMutiPeopleMeeting.setText("确定" + "(99+)");
                        } else {
                            btnMutiPeopleMeeting.setText("确定" + "(" + inviteCount + ")");
                        }
                        phoneId.add(((Contact) mContactSetImp.getItem(position))
                            .getNubeNumber());
                        mExpandMap.get(((Contact) mContactSetImp.getItem(position))
                            .getNubeNumber()).isSelected = true;
                        holdMeetingAdapter.updateExpandMap(mExpandMap);
                    } else {
                        ivSelected.setVisibility(View.VISIBLE);
                        ivUnSelected.setVisibility(View.INVISIBLE);
                        phoneId.remove(((Contact) mContactSetImp.getItem(position))
                            .getNubeNumber());
                        if (inviteCount > 0) {
                            inviteCount--;
                            mMutiTopList.remove((Contact) mContactSetImp.getItem(position));
                            if (mContactPagerAdapterList != null) {
                                mContactPagerAdapterList.notifyDataSetChanged();
                            }
                            if (inviteCount > 99) {
                                btnMutiPeopleMeeting.setText("确定" + "(99+)");
                            } else {
                                btnMutiPeopleMeeting.setText("确定" + "(" + inviteCount + ")");
                            }
                            CustomLog.i(
                                "itemdisSelected  ",
                                "count = "
                                    + inviteCount
                                    + "position = "
                                    + position
                                    + "newMutiContact.get(position)"
                                    + ((Contact) mContactSetImp.getItem(position))
                                    .getNubeNumber());

                        }

                        if (inviteCount == 0) {
                            CustomLog.d(TAG, "test gone...............");
                            btnMutiPeopleMeeting.setClickable(false);
                            llViewPage.setVisibility(View.GONE);
                            btnMutiPeopleMeeting.setClickable(false);
                            btnMutiPeopleMeeting.setTextColor(getResources().getColor(R.color.select_linkman_btn_disable_color));
                            btnMutiPeopleMeeting.setText("确定");
                        }
                        CustomLog.i(TAG, "inviteCount=" + inviteCount);
                        mExpandMap.get(((Contact) mContactSetImp.getItem(position))
                            .getNubeNumber()).isSelected = false;
                        holdMeetingAdapter.updateExpandMap(mExpandMap);
                    }
                    if (mContactPagerAdapterList != null) {
                        mContactPagerAdapterList.notifyDataSetChanged();
                    }
                }

            }
        });
        mViewPagerList = (ViewPager) findViewById(R.id.invite_list);
        if (flag == false) {
            height = llMutiPeopleIndex.getMeasuredHeight() / indexStr.length;
        }
        getIndexView(llMutiPeopleIndex, lvMutiPeopleList);
        phoneId.clear();
        holdMeetingAdapter = new MutiListViewAdapter(this);
        holdMeetingAdapter.addDataSet(mContactSetImp);
        holdMeetingAdapter.addExpandMap(mExpandMap);
        lvMutiPeopleList.setAdapter(holdMeetingAdapter);
        initListAdapter();
    }


    @Override
    public void todoClick(int id) {
        super.todoClick(id);
        switch (id) {
            case R.id.tvtemp:
                break;
            case R.id.btnmutiback:
                if (create != null) {
                    create.cancel();
                }
                finish();
                break;
            case R.id.btnmutipeoplemeeting:
                if (inviteCount > 0) {
                    MobclickAgent.onEvent(MedicalApplication.shareInstance()
                            .getApplicationContext(),
                        AnalysisConfig.MULTIPERSON_MEETING_BY_CONTACT);
                    createMeeting();
                } else {
                    CustomToast.show(HoldMutiMeetingActivity.this, "未选择联系人不能开始会诊", 1);
                }
                break;
            default:
                break;
        }
    }


    private void createMeeting() {
        CustomLog.i(TAG, "MainActivity::createMeeting() 正在创建会诊！");
        HoldMutiMeetingActivity.this.showLoadingView("正在创建会诊",
            new OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    HoldMutiMeetingActivity.this.removeLoadingView();
                    if (create != null) {
                        create.cancel();
                    }
                }
            });
        newExecCreateMeeting();
    }


    private void newExecCreateMeeting() {
        phoneId.add(AccountManager.getInstance(HoldMutiMeetingActivity.this)
            .getAccountInfo().nube);
        CustomLog.d(TAG, phoneId.toString());
        for (int i = 0; i < phoneId.size(); i++) {
            phoneId.get(i).toString();
        }

        int i = MedicalMeetingManage.getInstance().createMeeting(TAG, phoneId, new MedicalMeetingManage.OnCreateMeeetingListener() {
            @Override
            public void onCreateMeeting(int valueCode, final cn.redcdn.jmeetingsdk.MeetingInfo meetingInfo) {
                CustomLog.i(TAG, "meetingInfo==" + meetingInfo.meetingId);
                removeLoadingView();
                if (valueCode == 0) {
                    MedicalMeetingManage.getInstance().joinMeeting(meetingInfo.meetingId, new MedicalMeetingManage.OnJoinMeetingListener() {
                        @Override
                        public void onJoinMeeting(String valueDes, int valueCode) {
                            MedicalMeetingManage manager = MedicalMeetingManage.getInstance();
                            manager.inviteMeeting(phoneId, meetingInfo.meetingId);
                        }
                    });
                }else{
                    CustomToast.show(HoldMutiMeetingActivity.this,"创建会诊失败",CustomToast.LENGTH_SHORT);
                }
            }
        });
        if (i == 0) {
            removeLoadingView();
            showLoadingView("正在召开会诊");
        } else {
            removeLoadingView();
            CustomToast.show(this, "召开会诊失败", CustomToast.LENGTH_SHORT);
        }

    }


    private void initListAdapter() {
        CustomLog.i("initListAdapter", "initListAdapter");
        mContactPagerAdapterList = null;
        mContactPagerAdapterList = new ContactPagerAdapterList(this, mMutiTopList,
            CONTACT_LIST_COLUMN, 1, false);
        mViewPagerList.setAdapter(mContactPagerAdapterList);
    }


    private void updatemExpandMap() {
        mExpandMap = new HashMap<String, DataExpand>();
        for (int i = 0; i < mContactSetImp.getCount(); i++) {
            DataExpand expand = new DataExpand();
            expand.isSelected = false;
            String id = ((Contact) mContactSetImp.getItem(i)).getNubeNumber();
            mExpandMap.put(((Contact) mContactSetImp.getItem(i)).getNubeNumber(),
                expand);
        }

    }


    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        // 在oncreate里面执行下面的代码没反应，因为oncreate里面得到的getHeight=0
        if (!flag) {// 这里为什么要设置个flag进行标记，我这里不先告诉你们，请读者研究，因为这对你们以后的开发有好处

            if (llMutiPeopleIndex != null && indexStr != null) {
                height = llMutiPeopleIndex.getMeasuredHeight() / indexStr.length;

                flag = true;
            }
        }
    }


    /**
     * 绘制索引列表
     */
    public void getIndexView(LinearLayout llindex, final ListView listView) {
        LayoutParams params = new LayoutParams(
            LayoutParams.WRAP_CONTENT, height);
        for (int i = 0; i < indexStr.length; i++) {
            CustomLog.d(TAG, "getIndexView  " + i);
            final TextView tv = new TextView(this);
            tv.setTextColor(0xff646566);
            tv.setTextSize(12);
            tv.setLayoutParams(params);
            tv.setText(indexStr[i]);
            tv.setPadding(0, 0, 0, 0);
            llindex.addView(tv);
            llindex.setOnTouchListener(new OnTouchListener() {

                @Override
                public boolean onTouch(View v, MotionEvent event)

                {
                    float y = event.getY();
                    int index = (int) (y / height);
                    if (index > -1 && index < indexStr.length) {// 防止越界
                        String key = indexStr[index];
                        if (selector.containsKey(key)) {
                            int pos = selector.get(key);
                            CustomLog.d("2222222222222222", "test");
                            if (listView.getHeaderViewsCount() > 0) {// 防止ListView有标题栏，本例中没有。
                                listView.setSelectionFromTop(
                                    pos + listView.getHeaderViewsCount(), 0);
                            } else {
                                listView.setSelectionFromTop(pos, 0);// 滑动到第一项
                            }
                            tvSelect.setVisibility(View.VISIBLE);
                            tvSelect.setText(indexStr[index]);
                        }
                    }
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            CustomLog.d(TAG, "111111111111111111111");
                            llMutiPeopleIndex.setBackgroundColor(Color.parseColor("#e3e4e5"));
                            break;

                        case MotionEvent.ACTION_MOVE:

                            break;
                        case MotionEvent.ACTION_UP:
                            CustomLog.d(TAG, "2222222222222222222");
                            llMutiPeopleIndex.setBackgroundColor(Color.parseColor("#00ffffff"));
                            tvSelect.setVisibility(View.GONE);
                            break;
                    }
                    return true;
                }
            });
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
    }


    @Override
    protected void onStop() {
        super.onStop();
    }


    @Override
    public void onBackPressed() {

        super.onBackPressed();
        if (create != null) {
            create.cancel();
        }
        this.finish();
    }

}
