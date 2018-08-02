package cn.redcdn.hvs.contacts;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import cn.redcdn.hvs.MedicalApplication;
import cn.redcdn.hvs.R;
import cn.redcdn.hvs.base.BaseActivity;
import cn.redcdn.hvs.boot.SplashActivity;
import cn.redcdn.hvs.contacts.contact.ContactTransmitConfig;
import cn.redcdn.hvs.contacts.contact.butelDataAdapter.ContactSetImp;
import cn.redcdn.hvs.contacts.contact.interfaces.Contact;
import cn.redcdn.hvs.contacts.contact.interfaces.ContactCallback;
import cn.redcdn.hvs.contacts.contact.interfaces.ResponseEntry;
import cn.redcdn.hvs.contacts.contact.manager.ContactManager;
import cn.redcdn.hvs.contacts.contact.manager.IContactListChanged;
import cn.redcdn.hvs.im.activity.ChatActivity;
import cn.redcdn.hvs.util.CommonUtil;
import cn.redcdn.hvs.util.SideBar;
import cn.redcdn.log.CustomLog;


public class ContactsGroupChatActivity extends BaseActivity {

    private ListView lvContact;
    private LinearLayout llNoContactno = null;
    private TextView tvSelect = null;
    private int refreshTime = 0;
    private ContactSetImp mContactSetImp=null;
    private IContactListChanged ic = null;
    private Button btnContactBack = null;
    private TextView tvTemp = null;
    private ContactsGroupChatListViewAdapter contactAdapter;
    /*** 定义消息类型 */
    private final int MSG_UPDATAUI = 0x66660000;
    private final int MSG_LOADINGDATA = 0x66660001;
    private final int MSG_UPDATAADAPTER = 0x66660002;
    private final int MSG_UPDATACONTACTCHANGE = 0x66660004;
    private final int MSG_RESUMEDATA = 0x66660005;
    private final int MSG_RESUMEUI = 0x66660006;
    private final int DELAY_UPDATE_SECOND = 10000;
    private int firstTimeExecute = 0;
    private int isDataChanged=0;//0 数据未跟新，1 数据已跟新
    private SideBar mSideBar;
    private List<LetterInfo> letterInfoList= null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 去标题栏
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_contact_groupchat);
        start();

    }

    private void start() {
        CustomLog.i(TAG, "start");

        mHandler.sendEmptyMessage(MSG_LOADINGDATA);
    }

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_LOADINGDATA:

                    initContactsInfo();
                    break;

                case MSG_RESUMEDATA:

                    CustomLog.d(TAG, "mHandler  MSG_RESUMEDATA 更新聯繫人數據");
                    updateContactsInfo();
                    break;
                case MSG_RESUMEUI:

                    if (contactAdapter != null) {
                        CustomLog.i(TAG, "MSG_RESUMEUI contactAdapter");
                        contactAdapter.updateDataSet(0, mContactSetImp);
                    }

                    switchLayout();
                    break;

                case MSG_UPDATAUI:
                    initContactPage();
                    initContactAdapter();

                    switchLayout();

                    ic = new IContactListChanged() {

                        @Override
                        public void onListChange(ContactSetImp set) {
                            CustomLog.d(TAG, " IContactListChanged change");
                            mContactSetImp=set;

                            mHandler.sendEmptyMessage(MSG_UPDATACONTACTCHANGE);

                        }
                    };
                    ContactManager.getInstance(ContactsGroupChatActivity.this)
                            .registerUpdateListener(ic);
                    mHandler
                            .sendEmptyMessageDelayed(MSG_UPDATAADAPTER, DELAY_UPDATE_SECOND);
                    break;
                case MSG_UPDATAADAPTER:
                    CustomLog.d(TAG, "MSG_UPDATAADAPTER");
                    if (contactAdapter != null) {
                        contactAdapter.notifyDataSetChanged();
                    }
                    if (refreshTime == 0 ) {
                        refreshTime = 1;
                        mHandler.sendEmptyMessageDelayed(MSG_UPDATAADAPTER,
                                DELAY_UPDATE_SECOND);
                    }
                    break;
                case MSG_UPDATACONTACTCHANGE:
                    if (contactAdapter != null) {
                        contactAdapter.notifyDataSetChanged();
                    }
                    switchLayout();
                    break;
                default:
                    break;
            }

        };
    };

    private void initContactAdapter() {
        contactAdapter = new ContactsGroupChatListViewAdapter(this);
        contactAdapter.addDataSet(mContactSetImp);
        lvContact.setAdapter(contactAdapter);
        CustomLog.i(TAG, "initContactAdapter");
    }

    private void initContactPage() {
        CustomLog.i(TAG, "initContactPage");
        mSideBar = (SideBar) findViewById(R.id.sidebar_groupchat);        ;
        tvTemp = (TextView) findViewById(R.id.tvtemp);
        llNoContactno = (LinearLayout) findViewById(R.id.nocontact_layout);
        tvSelect = (TextView) findViewById(R.id.tvselect);
        tvSelect.setVisibility(View.INVISIBLE);
        tvTemp.setOnClickListener(mbtnHandleEventListener);
        lvContact = (ListView) findViewById(R.id.listView);
        btnContactBack = (Button) findViewById(R.id.btncontactback);
        btnContactBack.setOnClickListener(mbtnHandleEventListener);
        lvContact.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position,
                                    long id) {
                    CustomLog.d(TAG, "lvContact itemclick     " + position
                            + "newContacts=" + mContactSetImp.getItem(position).toString());
                    Contact tmpContact = (Contact)mContactSetImp.getItem(position);
                    Intent i = new Intent(ContactsGroupChatActivity.this, ChatActivity.class);
                    i.putExtra(ChatActivity.KEY_NOTICE_FRAME_TYPE,
                            ChatActivity.VALUE_NOTICE_FRAME_TYPE_LIST);
                    i.putExtra(ChatActivity.KEY_CONVERSATION_ID, tmpContact.getNubeNumber());
                    i.putExtra(ChatActivity.KEY_CONVERSATION_TYPE,  ChatActivity.VALUE_CONVERSATION_TYPE_MULTI);
                    i.putExtra(ChatActivity.KEY_CONVERSATION_NUBES, tmpContact.getNubeNumber());
                    startActivity(i);
                    finish();

            }
        });

    }

    @Override
    public void todoClick(int id) {
        super.todoClick(id);
            switch (id) {
                case R.id.tvtemp:
                    break;
                case R.id.btncontactback:
                    finish();
                    break;
                default:
                    break;
            }
    }

    private void switchLayout() {
        CustomLog.i(TAG, "switchLayout");
        // 设置需要显示的提示框
        mSideBar.setTextView(tvSelect);
        mSideBar.setOnTouchingLetterChangedListener(new SideBar.OnTouchingLetterChangedListener() {
            @Override
            public void onTouchingLetterChanged(String s) {
                int position = CommonUtil.getLetterPosition(letterInfoList, s);
                if (position != -1) {
                    lvContact.setSelection(position);
                }
                mSideBar.setBackgroundColor(Color.parseColor("#e3e4e5"));
            }
        });
        if (mContactSetImp != null) {
            if (mContactSetImp.getCount() == 0) {
                llNoContactno.setVisibility(View.VISIBLE);
                lvContact.setVisibility(View.INVISIBLE);
                mSideBar.setVisibility(View.INVISIBLE);
            } else {
                llNoContactno.setVisibility(View.INVISIBLE);
                lvContact.setVisibility(View.VISIBLE);
                mSideBar.setVisibility(View.VISIBLE);
            }
        }else{
            llNoContactno.setVisibility(View.VISIBLE);
            lvContact.setVisibility(View.INVISIBLE);
            mSideBar.setVisibility(View.INVISIBLE);
        }
    }

    private void initContactsInfo() {
        CustomLog.i(TAG, "initContactsInfo");

        ContactManager.getInstance(this).getAllGroups(
                new ContactCallback() {

                    @Override
                    public void onFinished(ResponseEntry result) {
                        CustomLog.i(TAG, "onFinish! status: " + result.status
                                + " | content: " + result.content);
                        if (result.status >= 0) {
                            letterInfoList= new ArrayList<LetterInfo>();
                            mContactSetImp = (ContactSetImp) result.content;
                            if(null!=mContactSetImp&&mContactSetImp.getCount()>0){
                                for(int i=0;i<mContactSetImp.getCount();i++){
                                    Contact tContact = (Contact)mContactSetImp.getItem(i);
                                    if(null!=tContact.getFirstName()){
                                        LetterInfo letterInfo = new LetterInfo(){};
                                        letterInfo.setLetter(tContact.getFirstName());
                                        letterInfoList.add(letterInfo);
                                    }
                                }
                            }
                            mHandler.sendEmptyMessage(MSG_UPDATAUI);
                        }
                    }
                });
    }

    private void updateContactsInfo() {
        CustomLog.i(TAG, "updateContactsInfo");

        ContactManager.getInstance(this).getAllGroups(
                new ContactCallback() {

                    @Override
                    public void onFinished(ResponseEntry result) {
                        CustomLog.i(TAG, "onFinish! status: " + result.status
                                + " | content: " + result.content);
                        if (result.status >= 0) {
                            letterInfoList= new ArrayList<LetterInfo>();
                            mContactSetImp = (ContactSetImp) result.content;
                            if(null!=mContactSetImp&&mContactSetImp.getCount()>0){
                                for(int i=0;i<mContactSetImp.getCount();i++){
                                    Contact tContact = (Contact)mContactSetImp.getItem(i);
                                    if(null!=tContact.getFirstName()){
                                        LetterInfo letterInfo = new LetterInfo(){};
                                        letterInfo.setLetter(tContact.getFirstName());
                                        letterInfoList.add(letterInfo);
                                    }
                                }
                            }
                            mHandler.sendEmptyMessage(MSG_RESUMEUI);
                        }
                    }
                });
    }

    @Override
    protected void onResume() {
        CustomLog.d(TAG, "onresume");
        super.onResume();
        if (!MedicalApplication.shareInstance().getInitStatus()) {
            CustomLog.e(TAG, "onStart 应用程序未启动，重新执行启动逻辑");
            Intent intent = new Intent();
            intent.setClass(this, SplashActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } else {
            if (firstTimeExecute == 0) {
                firstTimeExecute = 1;
            } else {

                CustomLog.i(TAG, "isDataChanged"+isDataChanged);

                mHandler.sendEmptyMessage(MSG_RESUMEDATA);

            }
        }
    }

    @Override
    protected void onDestroy() {
        CustomLog.i(TAG, "onDestroy");
        super.onDestroy();
        if (ic != null) {
            ContactManager.getInstance(ContactsGroupChatActivity.this)
                    .unRegisterUpdateListener(ic);
            CustomLog.d(TAG, "onStop ic" + (ic == null));
        }
    }

    @Override
    public void onBackPressed() {
        CustomLog.i(TAG, "onBackPressed");
        super.onBackPressed();
        this.finish();
    }

    @Override
    protected void onStop() {
        CustomLog.i(TAG, "onStop");
        super.onStop();


    }

    @Override
    protected void onActivityResult(int requestCode,int resultCode,Intent data) {
        CustomLog.d(TAG, "resultfrom"+resultCode);
        switch(resultCode){
            case ContactTransmitConfig.RESULT_CARD_CODE:

                isDataChanged =(data.getExtras().getInt("isDataChanged"));
                break;
            case ContactTransmitConfig.RESULT_ADD_CODE:
                isDataChanged =(data.getExtras().getInt("isDataChanged"));
                break;
        }
    }

}