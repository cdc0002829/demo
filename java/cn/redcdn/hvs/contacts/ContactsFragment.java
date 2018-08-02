package cn.redcdn.hvs.contacts;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import cn.redcdn.hvs.MedicalApplication;
import cn.redcdn.hvs.R;
import cn.redcdn.hvs.base.BaseFragment;
import cn.redcdn.hvs.boot.SplashActivity;
import cn.redcdn.hvs.contacts.contact.AddContactActivity;
import cn.redcdn.hvs.contacts.contact.ContactCardActivity;
import cn.redcdn.hvs.contacts.contact.ContactTransmitConfig;
import cn.redcdn.hvs.contacts.contact.ListViewAdapter;
import cn.redcdn.hvs.contacts.contact.butelDataAdapter.ContactSetImp;
import cn.redcdn.hvs.contacts.contact.interfaces.Contact;
import cn.redcdn.hvs.contacts.contact.interfaces.ContactCallback;
import cn.redcdn.hvs.contacts.contact.interfaces.ResponseEntry;
import cn.redcdn.hvs.contacts.contact.manager.ContactManager;
import cn.redcdn.hvs.contacts.contact.manager.IContactListChanged;
import cn.redcdn.hvs.contacts.contact.manager.IRecommendListChanged;
import cn.redcdn.hvs.contacts.contact.manager.RecommendManager;
import cn.redcdn.hvs.util.CommonUtil;
import cn.redcdn.hvs.util.PopDialogActivity;
import cn.redcdn.hvs.util.ScannerActivity;
import cn.redcdn.hvs.util.SideBar;
import cn.redcdn.hvs.util.TitleBar;
import cn.redcdn.log.CustomLog;

import static android.content.ContentValues.TAG;


/**
 * Created by thinkpad on 2017/2/7.
 *
 */

public class ContactsFragment extends BaseFragment {

    private ListView lvContact;
    private int newRecommendCount = 0;
    private TextView tvSelect = null;
    private int refreshTime = 0;
    private String[] indexStr = { "A", "B", "C", "D", "E", "F", "G", "H",
            "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V",
            "W", "X", "Y", "Z","#" };
    private ContactSetImp mContactSetImp=null;
    private IRecommendListChanged ir = null;
    private IContactListChanged ic = null;
    private TextView tvTemp = null;
    private ListViewAdapter contactAdapter;
    /*** 定义消息类型 */
    private final int MSG_UPDATAUI = 0x66660000;
    private final int MSG_LOADINGDATA = 0x66660001;
    private final int MSG_UPDATAADAPTER = 0x66660002;
    private final int MSG_UPDATARECOMMENDCHANGE = 0x66660003;
    private final int MSG_UPDATACONTACTCHANGE = 0x66660004;
    private final int MSG_RESUMEDATA = 0x66660005;
    private final int MSG_RESUMEUI = 0x66660006;
    private final int DELAY_UPDATE_SECOND = 10000;
    private int firstTimeExecute = 0;
    // 按钮标志位
    private boolean bButtonCanClick = true;
    //用于右上角下拉菜单
    private List<PopDialogActivity.MenuInfo> moreInfo;
    public static final int SCAN_CODE = 222;
    private SideBar mSideBar;
    private List<LetterInfo> letterInfoList= null;

    @Override
    protected View createView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View l1 = View.inflate(getActivity(), R.layout.contacts_fragment, null);
        mSideBar = (SideBar)l1.findViewById(R.id.sidebar_contact_fragment);
        tvTemp = (TextView) l1.findViewById(R.id.fragment_tvtemp);
        tvSelect = (TextView) l1.findViewById(R.id.fragment_tvselect);
        tvSelect.setVisibility(View.INVISIBLE);
        lvContact = (ListView) l1.findViewById(R.id.fragment_listView);
        tvTemp.setOnClickListener(mbtnHandleEventListener);
        lvContact.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position,
                                    long id) {
                if (!bButtonCanClick) {
                    CustomLog.d(TAG, "lvContact.setOnItemClickListener");
                } else {
                    if(position==0){
                        Intent intent = new Intent();
                        intent.setClass(getActivity(), ContactsGroupChatActivity.class);
                        startActivityForResult(intent, 0);
                    }else if(position==1){
                        Intent intent = new Intent();
                        intent.setClass(getActivity(), ContactsPublicNumberActivity.class);
                        startActivityForResult(intent, 0);
                    }else{
                        Intent intent = new Intent();
                        intent.setClass(getActivity(), ContactCardActivity.class);
                        intent.putExtra("contact", (Contact)mContactSetImp.getItem(position));
                        intent.putExtra("REQUEST_CODE", ContactTransmitConfig.REQUEST_CONTACT_CODE);
                        startActivityForResult(intent, 0);
                    }

                }
            }
        });

        return l1;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        TitleBar titleBar = getTitleBar();
        titleBar.setTitle(getResources().getString(R.string.titlebar_middle_contact));

        titleBar.enableRightBtn("", R.drawable.btn_meetingfragment_addmeet,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (CommonUtil.isFastDoubleClick()) {
                            return;
                        }

                        showMoreTitle();

                    }
                });

        start();

    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            //相当于Fragment的onResume
            mHandler.sendEmptyMessage(MSG_RESUMEDATA);
        } else {
            //相当于Fragment的onPause
        }
    }

    private void showMoreTitle() {

        if (moreInfo == null) {

            moreInfo = new ArrayList<PopDialogActivity.MenuInfo>();

            moreInfo.add(new PopDialogActivity.MenuInfo(R.drawable.contact_addfriend_pop, "添加好友",
                    new View.OnClickListener() {
                        @Override public void onClick(View v) {
                            Intent intent = new Intent();
                            intent.setClass(getActivity(), AddContactActivity.class);
                            startActivityForResult(intent, 0);
                        }
                    }));

            moreInfo.add(new PopDialogActivity.MenuInfo(R.drawable.temp_pop_dialog_scan, "扫一扫",
                    new View.OnClickListener() {
                        @Override public void onClick(View v) {
                            //扫一扫

                            Intent intentScan = new Intent();
                            intentScan.setClass(getActivity(), ScannerActivity.class);
                            startActivityForResult(intentScan, SCAN_CODE);
                        }
                    }));
        }

        PopDialogActivity.setMenuInfo(moreInfo);
        startActivity(new Intent(getActivity(), PopDialogActivity.class));
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
                    initContactAdapter();

                    switchLayout();

                    ir = new IRecommendListChanged() {

                        @Override
                        public void onListChange(int count) {

                            CustomLog.d(TAG, "MSG_UPDATAUI IRecommendListChanged change");
                            Message msa = new Message();
                            msa.arg1 = count;
                            msa.obj = MSG_UPDATARECOMMENDCHANGE;
                            mHandler.sendMessage(msa);
                        }
                    };
                    RecommendManager.getInstance(getActivity()).registerListener(ir);
                    ic = new IContactListChanged() {

                        @Override
                        public void onListChange(ContactSetImp set) {
                            CustomLog.d(TAG, " IContactListChanged change");
                            mContactSetImp=set;

                            mHandler.sendEmptyMessage(MSG_UPDATACONTACTCHANGE);

                        }
                    };
                    ContactManager.getInstance(getActivity())
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
                case MSG_UPDATARECOMMENDCHANGE:
                    CustomLog.d(TAG, "MSG_UPDATARECOMMENDCHANGE onListChange " + msg.arg1);
                    AddContactActivity.recommendCount = msg.arg1;
                    newRecommendCount = msg.arg1;
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

        contactAdapter = new ListViewAdapter(getActivity());
        if(null!=mContactSetImp){
            contactAdapter.addDataSet(mContactSetImp);
            lvContact.setAdapter(contactAdapter);
        }else{
            CustomLog.e(TAG, "mContactSetImp is null");
        }
        CustomLog.i(TAG, "initContactAdapter");
    }

    @Override
    public void todoClick(int id) {
        super.todoClick(id);

        if (!bButtonCanClick) {
            CustomLog.d(TAG, "todoClick test...........");
        } else {
            bButtonCanClick = false;
            switch (id) {
                case R.id.fragment_tvtemp:
                    bButtonCanClick = true;
                    break;
                default:
                    bButtonCanClick = true;
                    break;
            }
        }
    }

    private void switchLayout() {
        CustomLog.i(TAG, "switchLayout");
        // 设置需要显示的索引栏内容
        mSideBar.setLetter(indexStr);
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
                lvContact.setVisibility(View.INVISIBLE);
                mSideBar.setVisibility(View.INVISIBLE);
            } else {
                lvContact.setVisibility(View.VISIBLE);
                mSideBar.setVisibility(View.VISIBLE);
            }

        }else{
            lvContact.setVisibility(View.INVISIBLE);
            mSideBar.setVisibility(View.INVISIBLE);
        }

        if (contactAdapter != null) {
            contactAdapter.notifyDataSetChanged();
        }

    }

    private void initContactsInfo() {
        CustomLog.i(TAG, "initContactsInfo");

        ContactManager.getInstance(getActivity()).getAllContacts(
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
                },true);
    }

    private void updateContactsInfo() {
        CustomLog.i(TAG, "updateContactsInfo");

        ContactManager.getInstance(getActivity()).getAllContacts(
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
                },true);
    }

    @Override
    public void onResume() {
        CustomLog.d(TAG, "onresume");
        super.onResume();
        if (!MedicalApplication.shareInstance().getInitStatus()) {
            CustomLog.e(TAG, "onStart 应用程序未启动，重新执行启动逻辑");
            Intent intent = new Intent();
            intent.setClass(getActivity(), SplashActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } else {
            CustomLog.d(TAG, "onResume newRecommendCount=" + newRecommendCount);
            if (firstTimeExecute == 0) {
                firstTimeExecute = 1;
            } else {
                mHandler.sendEmptyMessage(MSG_RESUMEDATA);
            }
        }
    }

    @Override
    public void onDestroy() {
        CustomLog.i(TAG, "onDestroy");
        super.onDestroy();
        if (ic != null) {
            ContactManager.getInstance(getActivity())
                    .unRegisterUpdateListener(ic);
            CustomLog.d(TAG, "onStop ic" + (ic == null));
        }
        if (ir != null) {
            RecommendManager.getInstance(getActivity())
                    .unRegisterVersionListener(ir);
            CustomLog.d(TAG, "onStop ir" + (ir == null));
        }
    }

    @Override
    public void onStop() {
        CustomLog.i(TAG, "onStop");
        super.onStop();


    }

    @Override
    public void onActivityResult(int requestCode,int resultCode,Intent data) {
        CustomLog.d(TAG, "resultfrom"+resultCode);
        if(requestCode == SCAN_CODE){
            parseBarCodeResult(data);   
        }

    }

    @Override
    protected void setListener() {

    }

    @Override
    protected void initData() {

    }

}

