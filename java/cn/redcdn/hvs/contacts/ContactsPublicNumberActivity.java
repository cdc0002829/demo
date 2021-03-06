package cn.redcdn.hvs.contacts;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
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
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import cn.redcdn.datacenter.offaccscenter.MDSAppGetSubscribeOffAccs;
import cn.redcdn.datacenter.offaccscenter.data.OffAccdetailInfo;
import cn.redcdn.hvs.AccountManager;
import cn.redcdn.hvs.MedicalApplication;
import cn.redcdn.hvs.R;
import cn.redcdn.hvs.base.BaseActivity;
import cn.redcdn.hvs.boot.SplashActivity;
import cn.redcdn.hvs.contacts.contact.ContactTransmitConfig;
import cn.redcdn.hvs.contacts.contact.manager.ContactManager;
import cn.redcdn.hvs.contacts.contact.manager.IContactListChanged;
import cn.redcdn.hvs.contacts.contact.manager.IRecommendListChanged;
import cn.redcdn.hvs.contacts.contact.manager.RecommendManager;
import cn.redcdn.hvs.officialaccounts.DingYueActivity;
import cn.redcdn.hvs.util.CommonUtil;
import cn.redcdn.hvs.util.CustomToast;
import cn.redcdn.hvs.util.SideBar;
import cn.redcdn.log.CustomLog;

import static cn.redcdn.datacenter.medicalcenter.MDSErrorCode.MDS_TOKEN_DISABLE;

public class ContactsPublicNumberActivity extends BaseActivity {

    private ListView lvContact;
    private int newRecommendCount = 0;
    private LinearLayout llNoContactno = null;
    private TextView tvSelect = null;
    private IRecommendListChanged ir = null;
    private IContactListChanged ic = null;
    private Button btnContactBack = null;
    private TextView tvTemp = null;
    private ContactsPublicNumberListViewAdapter contactAdapter;
    /*** 定义消息类型 */
    private int firstTimeExecute = 0;
    private int isDataChanged=0;//0 数据未跟新，1 数据已跟新
    private List<OffAccdetailInfo> infolist = null;
    private final int MSG_UPDATAUI = 0x66660000;
    private MDSAppGetSubscribeOffAccs mdsappgetSub = null;
    private boolean createFlag = false;
    private SideBar mSideBar;

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_UPDATAUI:
                    switchLayout();
                    break;
            }
            }
        };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 去标题栏
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_contact_publicnumber);

        initContactPage();
        initContactAdapter();
        createFlag = true;
    }

     private void cv(){

       mdsappgetSub = new MDSAppGetSubscribeOffAccs(){

            @Override
            protected void onSuccess(List<OffAccdetailInfo> responseContent) {
                ContactsPublicNumberActivity.this.removeLoadingView();
                final List<LetterInfo> letterInfoList= new ArrayList<LetterInfo>();
                if(responseContent!=null&&responseContent.size()>0){
                    for(int i=0;i<responseContent.size();i++){
                        infolist.add(responseContent.get(i));
                        if(null!=responseContent.get(i).getNameSpell()){
                            LetterInfo letterInfo = new LetterInfo(){};
                            letterInfo.setLetter(responseContent.get(i).getNameSpell());
                            letterInfoList.add(letterInfo);
                        }
                    }
                }else{
                    llNoContactno.setVisibility(View.VISIBLE);
                }

                contactAdapter.notifyDataSetChanged();

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

                if(createFlag){
                    createFlag = false;
                    mHandler.sendEmptyMessage(MSG_UPDATAUI);
                }

            }

            @Override
            protected void onFail(int statusCode, String statusInfo) {
                ContactsPublicNumberActivity.this.removeLoadingView();
                llNoContactno.setVisibility(View.VISIBLE);
                CustomLog.e(TAG,"onFail"+"statusCode:"+statusCode+" statusInfo:"+statusInfo);
                if(statusCode==MDS_TOKEN_DISABLE){
                    AccountManager.getInstance(ContactsPublicNumberActivity.this).tokenAuthFail(statusCode);
                }else{
                    CustomToast.show(ContactsPublicNumberActivity.this,statusInfo,Toast.LENGTH_LONG);
                }
            }

        };

         ContactsPublicNumberActivity.this.showLoadingView("加载中", new DialogInterface.OnCancelListener() {

             @Override
             public void onCancel(DialogInterface dialog) {
                 dialog.dismiss();
                 CustomToast.show(ContactsPublicNumberActivity.this, "加载取消",
                         Toast.LENGTH_LONG);

             }
         });

         mdsappgetSub.appGetSubscribeOffAccs(AccountManager.getInstance(this).getToken());

    };

    private void initContactAdapter() {

        infolist = new ArrayList<OffAccdetailInfo>();

            contactAdapter = new ContactsPublicNumberListViewAdapter(this,infolist);
            lvContact.setAdapter(contactAdapter);
            CustomLog.i(TAG, "initContactAdapter");

    }

    private void initContactPage() {
        CustomLog.i(TAG, "initContactPage");
        tvTemp = (TextView) findViewById(R.id.tvtemp);
        llNoContactno = (LinearLayout) findViewById(R.id.nocontact_layout);
        tvSelect = (TextView) findViewById(R.id.tvselect);
        tvSelect.setVisibility(View.INVISIBLE);
        tvTemp.setOnClickListener(mbtnHandleEventListener);
        lvContact = (ListView) findViewById(R.id.listView);
        btnContactBack = (Button) findViewById(R.id.btncontactback);
        mSideBar = (SideBar) findViewById(R.id.sidebar_publicnumber);

        btnContactBack.setOnClickListener(mbtnHandleEventListener);
        lvContact.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position,
                                    long id) {
                    Intent intent = new Intent();
                    intent.setClass(ContactsPublicNumberActivity.this, DingYueActivity.class);

                    if(infolist.size()>0){
                        intent.putExtra("officialAccountId",infolist.get(position).getId());
                        intent.putExtra("officialName",infolist.get(position).getName());
                    }
                    startActivity(intent);
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
                    if (mdsappgetSub != null) {
                        mdsappgetSub.cancel();
                    }
                    ContactsPublicNumberActivity.this.removeLoadingView();
                    finish();
                    break;
                default:
                    break;
            }
    }

    private void switchLayout() {
        CustomLog.i(TAG, "switchLayout");
        if (infolist != null) {
            if (infolist.size() == 0) {
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

    @Override
    protected void onResume() {
        CustomLog.d(TAG, "onresume");
        super.onResume();
        infolist.clear();
        cv();
        if (!MedicalApplication.shareInstance().getInitStatus()) {
            CustomLog.e(TAG, "onStart 应用程序未启动，重新执行启动逻辑");
            Intent intent = new Intent();
            intent.setClass(this, SplashActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } else {
            CustomLog.d(TAG, "onResume newRecommendCount=" + newRecommendCount);
            if (firstTimeExecute == 0) {
                firstTimeExecute = 1;
            } else {

                CustomLog.i(TAG, "isDataChanged"+isDataChanged);
            }
        }
    }

    @Override
    protected void onDestroy() {
        CustomLog.i(TAG, "onDestroy");
        super.onDestroy();

        if (ic != null) {
            ContactManager.getInstance(ContactsPublicNumberActivity.this)
                    .unRegisterUpdateListener(ic);
            CustomLog.d(TAG, "onStop ic" + (ic == null));
        }
        if (ir != null) {
            RecommendManager.getInstance(ContactsPublicNumberActivity.this)
                    .unRegisterVersionListener(ir);
            CustomLog.d(TAG, "onStop ir" + (ir == null));
        }
    }

    @Override
    public void onBackPressed() {
        CustomLog.i(TAG, "onBackPressed");
        if (mdsappgetSub != null) {
            mdsappgetSub.cancel();
        }
        ContactsPublicNumberActivity.this.removeLoadingView();
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