package cn.redcdn.hvs.contacts.contact;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.umeng.analytics.MobclickAgent;
import com.uuzuche.lib_zxing.activity.CodeUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cn.redcdn.datacenter.medicalcenter.MDSAppSearchUsers;
import cn.redcdn.datacenter.medicalcenter.data.MDSAccountInfo;
import cn.redcdn.datacenter.medicalcenter.data.MDSDetailInfo;
import cn.redcdn.datacenter.offaccscenter.MDSAppGetOffAccInfo;
import cn.redcdn.datacenter.offaccscenter.data.OffAccdetailInfo;
import cn.redcdn.hvs.AccountManager;
import cn.redcdn.hvs.MedicalApplication;
import cn.redcdn.hvs.R;
import cn.redcdn.hvs.base.BaseActivity;
import cn.redcdn.hvs.contacts.contact.interfaces.Contact;
import cn.redcdn.hvs.contacts.contact.manager.RecommendManager;
import cn.redcdn.hvs.im.activity.ChatActivity;
import cn.redcdn.hvs.im.activity.GroupAddActivity;
import cn.redcdn.hvs.im.bean.GroupMemberBean;
import cn.redcdn.hvs.im.dao.GroupDao;
import cn.redcdn.hvs.officialaccounts.DingYueActivity;
import cn.redcdn.hvs.profiles.activity.OutDateActivity;
import cn.redcdn.hvs.util.CommonUtil;
import cn.redcdn.hvs.util.CustomToast;
import cn.redcdn.hvs.util.ScannerActivity;
import cn.redcdn.hvs.util.youmeng.AnalysisConfig;
import cn.redcdn.log.CustomLog;

import static cn.redcdn.datacenter.medicalcenter.MDSErrorCode.MDS_TOKEN_DISABLE;
import static cn.redcdn.hvs.MedicalApplication.getContext;
import static cn.redcdn.hvs.profiles.ProfilesFragment.GROUP_TYPE;
import static cn.redcdn.hvs.profiles.ProfilesFragment.PERSON_TYPE;
import static cn.redcdn.hvs.profiles.ProfilesFragment.WE_TYPE;

public class AddContactActivity extends BaseActivity {

    private FrameLayout flAddContact;
    private LinearLayout addContactLayout;
    private LinearLayout contactAddNameView;
    private EditText addContact = null;
    private Button addContactBtn = null;
    private TextView contactaddName = null;
    public static int recommendCount = 0;
    private TextView tvRightTop = null;
    private RelativeLayout rlScan = null;
    private Button btnAddContactBack = null;
    private MDSAppSearchUsers searchUsers = null;
    private String[] addcontact = new String[1];
    private int iFromOtherPage = 0;
    private int isDataChanged = 0;
    public static final int SCAN_CODE = 222;
    private LinkedHashMap<String, GroupMemberBean> memberDateList = new LinkedHashMap<String, GroupMemberBean>();//显示数据
    private MDSAccountInfo loginUserInfo = null;
    private GroupDao mGroupDao;
    private String mGroupId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_addcontact);
        recommendCount = RecommendManager.getInstance(AddContactActivity.this)
                .getNewRecommentCount();
        initAddContactPage();
    }

    private void initAddContactPage() {
        addContactLayout = (LinearLayout) findViewById(R.id.ll_add_contact);
        tvRightTop = (TextView) findViewById(R.id.tvrighttop);
        btnAddContactBack = (Button) findViewById(R.id.btnaddcontactback);
        addContact = (EditText) findViewById(R.id.contactadd_edit);
        addContactBtn = (Button) findViewById(R.id.addcontact_btn);
        rlScan = (RelativeLayout) findViewById(R.id.rl_scan);
        contactaddName = (TextView) findViewById(R.id.contactadd_name);
        contactAddNameView = (LinearLayout) findViewById(R.id.ll_contactadd_name);
        flAddContact = (FrameLayout) findViewById(R.id.fl_contact_addcontact);
        flAddContact.setVisibility(View.VISIBLE);
        addContactBtn.setClickable(false);
        addContactLayout.setOnClickListener(mbtnHandleEventListener);
        contactAddNameView.setOnClickListener(mbtnHandleEventListener);
        btnAddContactBack.setOnClickListener(mbtnHandleEventListener);

        addContact.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (addContact.length() != 0) {
                    addContactBtn.setClickable(true);
                    addContactBtn.setBackgroundDrawable(getResources().getDrawable(
                            R.drawable.contact_search_btn));
                } else {
                    addContactBtn.setClickable(false);
                    addContactBtn.setBackgroundDrawable(getResources().getDrawable(
                            R.drawable.contact_search_btn));
                }
                addcontact[0] = addContact.getText().toString();
                CustomLog.d(TAG, "addContact.length" + addcontact.length);
                if (addContact.length() == 8 || addContact.length() == 11) {
                } else {
                    contactaddName.setVisibility(View.INVISIBLE);
                    contactaddName.setText("");
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                                          int after) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                addContact.setTextColor(Color.parseColor("#000000"));

            }
        });
        addContactBtn.setOnClickListener(mbtnHandleEventListener);

        rlScan.setOnClickListener(mbtnHandleEventListener);

        if (recommendCount != 0) {
            // 刷新
            if (tvRightTop == null) {
                tvRightTop = (TextView) findViewById(R.id.tvrighttop);
            }
            tvRightTop.setVisibility(View.VISIBLE);
            if(recommendCount>99){
                tvRightTop.setText("99+");
            }else{
                tvRightTop.setText(String.valueOf(recommendCount));
            }
        } else {
            if (tvRightTop == null) {
                tvRightTop = (TextView) findViewById(R.id.tvrighttop);
            }
            tvRightTop.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void todoClick(int id) {
        super.todoClick(id);
        switch (id) {
            case R.id.ll_add_contact:
                CustomLog.d(TAG, "ll_add_contact click");
                boolean result = CommonUtil.selfPermissionGranted(AddContactActivity.this, android.Manifest.permission.READ_CONTACTS);
                if(!result){
                    CustomToast.show(AddContactActivity.this,R.string.open_contact_premission,CustomToast.LENGTH_SHORT);
                }else {
                    MobclickAgent.onEvent(MedicalApplication.shareInstance().getApplicationContext(), AnalysisConfig.ACCESS_CONTACT_RECOMMEND);
                    Intent i = new Intent();
                    i.setClass(AddContactActivity.this, RecommendActivity.class);
                    startActivityForResult(i, 0);
                }
                break;
            case R.id.ll_contactadd_name:
                addContact.requestFocus();
                InputMethodManager imm = (InputMethodManager) addContact.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.toggleSoftInput(0, InputMethodManager.SHOW_FORCED);
                break;

            case R.id.btnaddcontactback:
                if (searchUsers != null) {
                    searchUsers.cancel();
                }
                AddContactActivity.this.removeLoadingView();
                Intent intent = getIntent();
                intent.putExtra("isDataChanged", isDataChanged);
                setResult(ContactTransmitConfig.RESULT_ADD_CODE, intent);
                finish();
                break;
            case R.id.addcontact_btn:
                String[] arraylist = {addContact.getText().toString()};

                if (addContact.getText().toString() != null && !addContact.getText().toString().isEmpty()) {

                    if (isNubeNumber(arraylist[0]) == true) {
                        searchUser(3, arraylist);
                    } else if (isPhoneNumber(arraylist[0]) == true) {
                        searchUser(1, arraylist);
                    } else if (isEmail(arraylist[0]) == true) {
                        searchUser(2, arraylist);
                    } else {
                        CustomToast.show(AddContactActivity.this, "账号格式不正确！", 1);
                    }
                } else {
                    CustomToast.show(AddContactActivity.this, "请输入搜索内容！", 1);
                }
                break;

            case R.id.rl_scan:
                CustomLog.d(TAG, "rlScan click");
                //扫一扫
                Intent intentScan = new Intent();
                intentScan.setClass(AddContactActivity.this, ScannerActivity.class);
                startActivityForResult(intentScan, SCAN_CODE);
                break;
            default:
                break;
        }
    }


    private boolean isNubeNumber(String num) {
        boolean is = false;
        Pattern p = Pattern
                .compile("^([0-9])\\d{7}$");
        Matcher m = p.matcher(num);
        if (m.matches())
            is = true;
        return is;
    }

    private boolean isPhoneNumber(String num) {
        boolean is = false;
        Pattern p = Pattern
                .compile("^((1))\\d{10}$");
        Matcher m = p.matcher(num);
        if (m.matches())
            is = true;
        return is;
    }

    private boolean isEmail(String num) {
        boolean is = false;
        Pattern p = Pattern
                .compile("^\\s*\\w+(?:\\.{0,1}[\\w-]+)*@[a-zA-Z0-9]+(?:[-.][a-zA-Z0-9]+)*\\.[a-zA-Z]+\\s*$");
        Matcher m = p.matcher(num);
        if (m.matches())
            is = true;
        return is;
    }


    private void searchUser(final int searchType, String[] content) {
         searchUsers = new MDSAppSearchUsers() {
            @Override
            protected void onSuccess(List<MDSDetailInfo> responseContent) {
                AddContactActivity.this.removeLoadingView();
                List<MDSDetailInfo> list = responseContent;
                Contact contact = new Contact();
                if (list != null && list.size() > 0) {
                    contact.setContactId(list.get(0).getUid());
                    contact.setHeadUrl(list.get(0).getHeadThumUrl());
                    contact.setNickname(list.get(0).getNickName());
                    contact.setName(list.get(0).getNickName());
                    contact.setNubeNumber(list.get(0).getNubeNumber());

                    contact.setWorkUnit(list.get(0).getWorkUnit());
                    contact.setWorkUnitType(Integer.valueOf(list.get(0).getWorkUnitType()));
                    contact.setDepartment(list.get(0).getDepartment());
                    contact.setProfessional(list.get(0).getProfessional());
                    contact.setOfficeTel(list.get(0).getOfficTel());

                    if (null != list.get(0).getMobile() && !list.get(0).getMobile().isEmpty()) {//手机号
                        contact.setNumber(list.get(0).getMobile());
                    } else if (null != list.get(0).getMail() && !list.get(0).getMail().isEmpty()) {//邮箱号
                        contact.setEmail(list.get(0).getMail());
                    }

                }

                if (list != null && list.size() > 0) {

                    if (null != contact.getNubeNumber() && contact.getNubeNumber()
                            .equals(AccountManager.getInstance(AddContactActivity.this).getNube())) {
                        CustomToast.show(AddContactActivity.this, "不能添加自己为好友", 1);
                    } else {
                        Intent intent = new Intent();
                        intent.setClass(AddContactActivity.this, ContactCardActivity.class);
                        intent.putExtra("searchType", String.valueOf(searchType));
                        intent.putExtra("contact", contact);
                        intent.putExtra("REQUEST_CODE", ContactTransmitConfig.REQUEST_CONTACT_CODE);
                        startActivity(intent);
                    }

                } else {
                    CustomToast.show(AddContactActivity.this, "该用户不存在", Toast.LENGTH_LONG);
                }

            }

            @Override
            protected void onFail(int statusCode, String statusInfo) {
                AddContactActivity.this.removeLoadingView();
                CustomLog.e(TAG, "onFail" + "statusCode:" + statusCode + " statusInfo:" + statusInfo);
                if (statusCode == MDS_TOKEN_DISABLE) {
                    AccountManager.getInstance(AddContactActivity.this).tokenAuthFail(statusCode);
                } else {
                    CustomToast.show(AddContactActivity.this, statusInfo, Toast.LENGTH_LONG);
                }
            }

        };

        AddContactActivity.this.showLoadingView("加载中", new DialogInterface.OnCancelListener() {

            @Override
            public void onCancel(DialogInterface dialog) {
                dialog.dismiss();
                CustomToast.show(AddContactActivity.this, "加载取消",
                        Toast.LENGTH_LONG);

            }
        });

        searchUsers.appSearchUsers(AccountManager.getInstance(this).getToken(), searchType, content);

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
    protected void onResume() {
        super.onResume();
        addContact.setHint("手机号/视讯号/邮箱");
        addContact.setText("");
        recommendCount = RecommendManager.getInstance(AddContactActivity.this)
                .getNewRecommentCount();

        CustomLog.d(TAG, "onResume recommendCount=" + recommendCount);
        if (contactaddName != null) {
            contactaddName.setVisibility(View.INVISIBLE);
        }
        if (recommendCount != 0) {
            // 刷新
            if (tvRightTop == null) {
                tvRightTop = (TextView) findViewById(R.id.tvrighttop);
            }
            tvRightTop.setVisibility(View.VISIBLE);
            if(recommendCount>99){
                tvRightTop.setText("99+");
            }else{
                tvRightTop.setText(String.valueOf(recommendCount));
            }
        } else {
            if (tvRightTop == null) {
                tvRightTop = (TextView) findViewById(R.id.tvrighttop);
            }
            tvRightTop.setVisibility(View.INVISIBLE);
        }

        if (iFromOtherPage == 0) {
            iFromOtherPage = 1;
        } else {
        }
    }

    @Override
    public void onBackPressed() {
        if (searchUsers != null) {
            searchUsers.cancel();
        }
        AddContactActivity.this.removeLoadingView();
        Intent intent = new Intent();
        intent.putExtra("isDataChanged", isDataChanged);
        setResult(ContactTransmitConfig.RESULT_ADD_CODE, intent);
        super.onBackPressed();

        this.finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        CustomLog.d(TAG, "resultfrom" + resultCode);
        CustomLog.d(TAG, "isDataChaged" + isDataChanged);
        switch (resultCode) {
            case ContactTransmitConfig.RESULT_CARD_CODE:

                isDataChanged = (data.getExtras().getInt("isDataChanged"));
                break;
            case ContactTransmitConfig.RESULT_RECOMMEND_CODE:

                isDataChanged = (data.getExtras().getInt("isDataChanged"));
                break;
        }

        if (requestCode == SCAN_CODE) {
            if (data != null) {
                Bundle bundle = data.getExtras();
                if (bundle == null) {
                    return;
                }
                if (bundle.getInt(CodeUtils.RESULT_TYPE) == CodeUtils.RESULT_SUCCESS) {
                    String result = bundle.getString(CodeUtils.RESULT_STRING);
                    CustomLog.d(TAG, "解析的二维码字符串为:" + result);
                    String[] split = result.split("\\?");
                    if (split.length < 2) {
                        CustomToast.show(getContext(), "亲，这不是本公司的二维码哦", 8000);
                        return;
                    }
                    String[] split1 = split[1].split("=");
                    if (split1.length < 2) {
                        CustomToast.show(getContext(), "亲，这不是本公司的二维码哦", 8000);
                        return;
                    }
                    String s = split1[1];
                    String[] split2 = s.split("_");
                    if (split2.length < 3) {
                        CustomToast.show(getContext(), "亲，这不是本公司的二维码哦", 8000);
                        return;
                    }
                    mGroupId = split2[1];
                    CustomLog.e("TAG", split2[0]);
                    CustomLog.e("TAG", split2[1]);
                    CustomLog.e("TAG", split2[2]);
                    if (split2[0].equals(PERSON_TYPE)) {
                        Intent intent = new Intent();
                        intent.setClass(AddContactActivity.this, ContactCardActivity.class);
                        intent.putExtra("nubeNumber", split2[1]);
                        intent.putExtra("searchType", "4");
                        startActivity(intent);

                    } else if (split2[0].equals(GROUP_TYPE)) {
                        long nowTime = System.currentTimeMillis();
                        long startTime = Long.parseLong(split2[2]);
                        long time = nowTime - startTime;
                        long days = time / (1000 * 60 * 60 * 24);
                        if (days >= 7) {
                            Intent outDateIntent = new Intent();
                            outDateIntent.setClass(getContext(), OutDateActivity.class);
                            startActivity(outDateIntent);
                        } else {
                            mGroupDao = new GroupDao(AddContactActivity.this);
                            loginUserInfo = AccountManager.getInstance(AddContactActivity.this).getAccountInfo();
                            ;
                            memberDateList = mGroupDao.queryGroupMembers(mGroupId);
                            if (memberDateList.containsKey(loginUserInfo.getNube())) {
                                CustomLog.d(TAG, "用户属于该群，直接进入");
                                enterChatActivity();
                            } else {
                                Intent personIntent = new Intent();
                                personIntent.putExtra(GroupAddActivity.GROUP_ID, split2[1]);
                                personIntent.putExtra(GroupAddActivity.GROUP_ID_FROM, GroupAddActivity.GROUP_ID_FROM);
                                personIntent.setClass(getContext(), GroupAddActivity.class);
                                startActivity(personIntent);
                            }
                        }

                    } else if (split2[0].equals(WE_TYPE)) {
                        MDSAppGetOffAccInfo mdsAppGetOffAccInfo = new MDSAppGetOffAccInfo() {
                            @Override
                            protected void onSuccess(OffAccdetailInfo responseContent) {
                                super.onSuccess(responseContent);
                                String id = responseContent.getId();
                                Intent intentWeChat = new Intent();
                                intentWeChat.putExtra("officialAccountId", id);
                                intentWeChat.setClass(AddContactActivity.this, DingYueActivity.class);
                                startActivity(intentWeChat);
                            }

                            @Override
                            protected void onFail(int statusCode, String statusInfo) {
                                super.onFail(statusCode, statusInfo);
                                CustomToast.show(getContext(), "亲，此公众号不存在哦", 8000);
                                return;
                            }
                        };
                        mdsAppGetOffAccInfo.appGetOffAccInfo(AccountManager.getInstance(AddContactActivity.this)
                                .getAccountInfo().getAccessToken(), split2[1]);
                    } else {
                        CustomToast.show(getContext(), "亲，这不是本公司的二维码哦", 8000);
                        return;
                    }

                } else if (bundle.getInt(CodeUtils.RESULT_TYPE) == CodeUtils.RESULT_FAILED) {
                    Toast.makeText(AddContactActivity.this, "解析二维码失败", Toast.LENGTH_LONG).show();
                    CustomToast.show(getContext(), "解析二维码失败哦", 8000);
                }
            }
        }
    }

    private void enterChatActivity() {
        Intent i = new Intent(AddContactActivity.this, ChatActivity.class);
        i.putExtra(ChatActivity.KEY_NOTICE_FRAME_TYPE,
                ChatActivity.VALUE_NOTICE_FRAME_TYPE_LIST);
        i.putExtra(ChatActivity.KEY_CONVERSATION_ID, mGroupId);
        i.putExtra(ChatActivity.KEY_CONVERSATION_TYPE, ChatActivity.VALUE_CONVERSATION_TYPE_MULTI);
        i.putExtra(ChatActivity.KEY_CONVERSATION_NUBES, mGroupId);
        startActivity(i);
        AddContactActivity.this.finish();
    }

}

