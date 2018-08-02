package cn.redcdn.hvs.contacts.contact;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.redcdn.keyeventwrite.KeyEventConfig;
import com.redcdn.keyeventwrite.KeyEventWrite;
import com.umeng.analytics.MobclickAgent;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import cn.redcdn.datacenter.medicalcenter.MDSAppSearchUsers;
import cn.redcdn.datacenter.medicalcenter.data.MDSDetailInfo;
import cn.redcdn.datacenter.meetingmanage.CreateMeeting;
import cn.redcdn.hvs.AccountManager;
import cn.redcdn.hvs.MedicalApplication;
import cn.redcdn.hvs.R;
import cn.redcdn.hvs.base.BaseActivity;
import cn.redcdn.hvs.contacts.contact.ContactDeleteDialog.NoClickListener;
import cn.redcdn.hvs.contacts.contact.ContactDeleteDialog.OkClickListener;
import cn.redcdn.hvs.contacts.contact.diaplayImageListener.DisplayImageListener;
import cn.redcdn.hvs.contacts.contact.interfaces.Contact;
import cn.redcdn.hvs.contacts.contact.interfaces.ContactCallback;
import cn.redcdn.hvs.contacts.contact.interfaces.ResponseEntry;
import cn.redcdn.hvs.contacts.contact.manager.ContactManager;
import cn.redcdn.hvs.contacts.contact.manager.RecommendManager;
import cn.redcdn.hvs.im.activity.ChatActivity;
import cn.redcdn.hvs.meeting.meetingManage.MedicalMeetingManage;
import cn.redcdn.hvs.util.CommonUtil;
import cn.redcdn.hvs.util.CustomToast;
import cn.redcdn.hvs.util.OpenBigImageActivity;
import cn.redcdn.hvs.util.youmeng.AnalysisConfig;
import cn.redcdn.log.CustomLog;

import static cn.redcdn.datacenter.medicalcenter.MDSErrorCode.MDS_TOKEN_DISABLE;
import static cn.redcdn.hvs.R.id.iamgehead;
import static cn.redcdn.hvs.contacts.contact.ContactTransmitConfig.REQUEST_CONTACT_CODE;



public class ContactCardActivity extends BaseActivity implements Serializable {

    // 会诊邀请人视讯号列表，手机号
    private ArrayList<String> phoneId = new ArrayList<String>();
    private String[] invitedPhones = new String[1];
    private ImageView iamgeHead = null;
    private Contact mContact = null;
    private ImageView iamgeVip = null;
    private Button btnContactCardBack = null;
    private Button btnContactCardDel = null;
    private RelativeLayout rlContactInfo = null;
    private TextView tvContactName = null;
    private TextView tvNubeDetail = null;
    private TextView tvPhoneDetail = null;
    private TextView tvMeetingRoom = null;
    private Button ibStartMeeting = null;
    private TextView tvPhone = null;
    private CreateMeeting create = null;
    private DisplayImageListener mDisplayImageListener = null;
    private RelativeLayout rlContactPhoneInfo = null;
    private int REQUEST_CODE;
    private RelativeLayout rlContactCardContent = null;
    private RelativeLayout rlSendMessage = null;
    private RelativeLayout rlBlank = null;
    private TextView tvUnit;
    private TextView tvDepartment;
    private TextView tvProfessional;
    private TextView tvOfficeTel;
    private TextView tvUnitType;
    private TextView tvDepartmentType;
    private TextView tvProfessionalType;
    private TextView tvOfficeTelType;
    private Button sendChatMsgBt;
    private Button btnAddFriend;
    private int searchType = -1;
    private RelativeLayout rlJoinMeeting;
    private RelativeLayout rlAddFriend;
    private RelativeLayout rlContent;
    private RelativeLayout rlDepartment;
    private RelativeLayout rlPositionalTitle;
    private RelativeLayout rlDepartmentPhoneNumber;
    private View vBelowDepartmenTel;
    private View vBelowPositionalTitle;
    private View vBelowDepartment;
    private MDSAppSearchUsers searchUsers = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_contactcard);
        mDisplayImageListener = new DisplayImageListener();

        rlContent = (RelativeLayout) findViewById(R.id.rlcontent);
        rlContent.setVisibility(View.GONE);
        btnContactCardBack = (Button) findViewById(R.id.btncontactcardback);
        btnContactCardBack.setOnClickListener(mbtnHandleEventListener);
        btnContactCardDel = (Button) findViewById(R.id.btncontactcarddel);

        if (null != getIntent().getExtras() && null != getIntent().getExtras().getString("searchType")) {
            searchType = Integer.valueOf(getIntent().getExtras().getString("searchType"));
        }

        if (null != getIntent().getExtras()
            && null != getIntent().getExtras().getString("nubeNumber")) {
            REQUEST_CODE = ContactTransmitConfig.REQUEST_CONTACT_CODE;
            String nubeNumber = getIntent().getExtras().getString("nubeNumber");
            String[] arraylist = { nubeNumber };
            if (ContactManager.getInstance(this).checkNubeIsCustomService(nubeNumber)) {
                mContact = new Contact();
                mContact.setNubeNumber(nubeNumber);
                mContact.setName("视频客服");
                initCustomServiceView(nubeNumber);
            } else if (null != (ContactManager.getInstance(ContactCardActivity.this)
                    .getContactInfoByNubeNumber(nubeNumber)).getContactId()
                    && !(ContactManager.getInstance(ContactCardActivity.this)
                    .getContactInfoByNubeNumber(nubeNumber)).getContactId().isEmpty()) {
                mContact = new Contact();
                Contact contact = (ContactManager.getInstance(ContactCardActivity.this)
                        .getContactInfoByNubeNumber(nubeNumber));

                mContact.setHeadUrl(contact.getHeadUrl());
                mContact.setNickname(contact.getNickname());
                mContact.setName(contact.getNickname());
                mContact.setNubeNumber(nubeNumber);
                mContact.setWorkUnit(contact.getWorkUnit());
                mContact.setWorkUnitType(contact.getWorkUnitType());
                mContact.setDepartment(contact.getDepartment());
                mContact.setProfessional(contact.getProfessional());
                mContact.setOfficeTel(contact.getOfficeTel());
                mContact.setNumber(contact.getNumber());
                mContact.setEmail(contact.getEmail());
                initContactCardPage(mContact);

            } else {
                searchUser(3, arraylist);
            }

        } else if (null != getIntent().getExtras()
            && null != getIntent().getExtras().getSerializable("contact")) {
            mContact = (Contact) getIntent().getExtras().getSerializable("contact");
            REQUEST_CODE = (getIntent().getExtras().getInt("REQUEST_CODE"));
            CustomLog.d(TAG, "mContact ..." + mContact.toString());
            initContactCardPage(mContact);
        }

    }


    private void searchUser(final int searchType, String[] content) {
        searchUsers = new MDSAppSearchUsers() {
            @Override
            protected void onSuccess(List<MDSDetailInfo> responseContent) {
                ContactCardActivity.this.removeLoadingView();
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

                } else {
                    CustomToast.show(ContactCardActivity.this, "该用户不存在", Toast.LENGTH_LONG);
                }

                mContact = contact;
                initContactCardPage(mContact);
            }

            @Override
            protected void onFail(int statusCode, String statusInfo) {
                ContactCardActivity.this.removeLoadingView();
                CustomLog.e(TAG, "onFail" + "statusCode:" + statusCode + " statusInfo:" + statusInfo);
                if (statusCode == MDS_TOKEN_DISABLE) {
                    AccountManager.getInstance(ContactCardActivity.this).tokenAuthFail(statusCode);
                } else {
                    CustomToast.show(ContactCardActivity.this, statusInfo, Toast.LENGTH_LONG);
                }

            }

        };

        ContactCardActivity.this.showLoadingView("加载中", new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                dialog.dismiss();
                CustomToast.show(ContactCardActivity.this, "加载取消",
                    Toast.LENGTH_LONG);
            }
        });
        searchUsers.appSearchUsers(AccountManager.getInstance(this).getToken(), searchType, content);
    }


    private void createMeeting() {
        CustomLog.i(TAG, "HomeActivity::createMeeting() 正在创建会诊！");
        String LoadingString = "";
        if (ContactManager.getInstance(this).checkNubeIsCustomService(mContact.getNubeNumber())) {
            LoadingString = "正在创建视频呼叫";
        } else {
            LoadingString = "正在创建会诊";
        }

        ContactCardActivity.this.showLoadingView(LoadingString,
            new DialogInterface.OnCancelListener() {

                @Override
                public void onCancel(DialogInterface dialog) {
                    ContactCardActivity.this.removeLoadingView();
                    if (create != null) {
                        create.cancel();
                    }
                }
            });
        newExecCreateMeeting();
    }


    private void newExecCreateMeeting() {
        int i = MedicalMeetingManage.getInstance().createMeeting(TAG, phoneId, new MedicalMeetingManage.OnCreateMeeetingListener() {
            @Override
            public void onCreateMeeting(int code, final cn.redcdn.jmeetingsdk.MeetingInfo meetingInfo) {
                if (code == 0) {
                    CustomLog.i(TAG, "meetingInfo==" + meetingInfo.meetingId);
                    removeLoadingView();
                    MedicalMeetingManage.getInstance().joinMeeting(meetingInfo.meetingId, new MedicalMeetingManage.OnJoinMeetingListener() {
                        @Override
                        public void onJoinMeeting(String valueDes, int valueCode) {
                            ArrayList<String> list = new ArrayList<String>();
                            list.add(mContact.getNubeNumber());
                            MedicalMeetingManage manager = MedicalMeetingManage.getInstance();
                            manager.inviteMeeting(list, meetingInfo.meetingId);
                        }
                    });
                }else {
                    removeLoadingView();
                    CustomToast.show(ContactCardActivity.this,"创建会诊失败",CustomToast.LENGTH_SHORT);
                }
            }
        });
        if (i == 0) {
            removeLoadingView();
            if (ContactManager.getInstance(this).checkNubeIsCustomService(mContact.getNubeNumber())) {
                showLoadingView("正在召开视频呼叫");
            } else {
                showLoadingView("正在召开会诊");
            }
        } else {
            removeLoadingView();
            if (ContactManager.getInstance(this).checkNubeIsCustomService(mContact.getNubeNumber())) {
                CustomToast.show(this, "召开视频呼叫失败", CustomToast.LENGTH_SHORT);
            } else {
                CustomToast.show(this, "召开会诊失败", CustomToast.LENGTH_SHORT);
            }

        }

    }


    private void initCustomServiceView(String nubeNumber) {
        rlContent.setVisibility(View.VISIBLE);
        btnContactCardDel.setVisibility(View.INVISIBLE);
        iamgeHead = (ImageView) findViewById(iamgehead);
        iamgeHead.setImageResource(R.drawable.contact_customservice);
        tvMeetingRoom = (TextView) findViewById(R.id.tvmeetingroom);
        tvMeetingRoom.setText("视频客服");
        tvNubeDetail = (TextView) findViewById(R.id.tv_nubenumber_number);
        tvNubeDetail.setText(nubeNumber);
        rlContactCardContent = (RelativeLayout) findViewById(R.id.rlsecondcontant);
        rlContactCardContent.setVisibility(View.INVISIBLE);
        rlBlank = (RelativeLayout) findViewById(R.id.rl_contact_blank);
        rlBlank.setVisibility(View.INVISIBLE);
        rlAddFriend = (RelativeLayout) findViewById(R.id.rl_add_friend);
        rlAddFriend.setVisibility(View.INVISIBLE);
        sendChatMsgBt = (Button) findViewById(R.id.btn_sendmessage);
        sendChatMsgBt.setOnClickListener(mbtnHandleEventListener);
        ibStartMeeting = (Button) findViewById(R.id.ibstartmeeting);
        ibStartMeeting.setText("视频呼叫");
        ibStartMeeting.setOnClickListener(mbtnHandleEventListener);
    }


    private void initContactCardPage(Contact newContact) {

        rlContent.setVisibility(View.VISIBLE);
        rlDepartment = (RelativeLayout) findViewById(R.id.rl_contact_department);
        rlPositionalTitle = (RelativeLayout) findViewById(R.id.rl_contact_positionaltitle);
        rlDepartmentPhoneNumber = (RelativeLayout) findViewById(R.id.rl_contact_departmentphonenumber);
        vBelowDepartmenTel =  findViewById(R.id.v_below_department_tel);
        vBelowPositionalTitle =  findViewById(R.id.v_line_positionaltitle_bottom);
        vBelowDepartment =  findViewById(R.id.v_line_department_bottom);
        rlAddFriend = (RelativeLayout) findViewById(R.id.rl_add_friend);
        rlJoinMeeting = (RelativeLayout) findViewById(R.id.rlendcontent);
        btnAddFriend = (Button) findViewById(R.id.btn_add_friend);
        tvUnit = (TextView) findViewById(R.id.tv_hospital_detail);
        tvDepartment = (TextView) findViewById(R.id.tv_department_detail);
        tvProfessional = (TextView) findViewById(R.id.tv_positionaltitle_detail);
        tvOfficeTel = (TextView) findViewById(R.id.tv_departmentphonenumber_detail);
        tvUnitType = (TextView) findViewById(R.id.tv_hospital);
        tvDepartmentType = (TextView) findViewById(R.id.tv_department);
        tvProfessionalType = (TextView) findViewById(R.id.tv_positionaltitle);
        tvOfficeTelType = (TextView) findViewById(R.id.tv_departmentphonenumber);
        iamgeHead = (ImageView) findViewById(iamgehead);
        iamgeVip = (ImageView) findViewById(R.id.iamgevip);
        tvPhone = (TextView) findViewById(R.id.tvphone);
        rlContactInfo = (RelativeLayout) findViewById(R.id.rlcontactinfo);
        tvContactName = (TextView) findViewById(R.id.tvcontactname);
        tvNubeDetail = (TextView) findViewById(R.id.tv_nubenumber_number);
        tvPhoneDetail = (TextView) findViewById(R.id.tvphonedetail);
        ibStartMeeting = (Button) findViewById(R.id.ibstartmeeting);
        sendChatMsgBt = (Button) findViewById(R.id.btn_sendmessage);
        tvMeetingRoom = (TextView) findViewById(R.id.tvmeetingroom);
        rlContactPhoneInfo = (RelativeLayout) findViewById(R.id.rlcontactphoneinfo);
        rlContactCardContent = (RelativeLayout) findViewById(R.id.rlsecondcontant);
        rlSendMessage = (RelativeLayout) findViewById(R.id.rl_sendmessage);
        rlBlank = (RelativeLayout) findViewById(R.id.rl_contact_blank);
        ImageLoader imageLoader = ImageLoader.getInstance();
        imageLoader.displayImage(mContact.getHeadUrl(), iamgeHead,
            MedicalApplication.shareInstance().options, mDisplayImageListener);
        btnContactCardDel.setOnClickListener(mbtnHandleEventListener);
        tvOfficeTel.setOnClickListener(mbtnHandleEventListener);
        btnAddFriend.setOnClickListener(mbtnHandleEventListener);
        tvPhoneDetail.setOnClickListener(mbtnHandleEventListener);
        ibStartMeeting.setOnClickListener(mbtnHandleEventListener);
        sendChatMsgBt.setOnClickListener(mbtnHandleEventListener);
        iamgeVip.setVisibility(View.GONE);
        CustomLog.d(TAG, "newContact.getNumber" + newContact.getNumber());
        if (newContact.getNumber() != null && !newContact.getNumber().isEmpty()
            && (searchType == 1 || searchType == 7 || newContact.getUserFrom() == 2 || newContact.getUserFrom() == 1)) {
            tvPhoneDetail.setClickable(true);
            tvPhone.setVisibility(View.VISIBLE);
            tvPhoneDetail.setVisibility(View.VISIBLE);
            tvPhoneDetail.setText(newContact.getNumber());
            if (null != RecommendManager.getInstance(getApplicationContext()).getRawIdByMobile(newContact.getNumber())) {
                String id = RecommendManager.getInstance(getApplicationContext()).getRawIdByMobile(newContact.getNumber());
                String name = RecommendManager.getNameByRawId(getApplicationContext(), id);
                if (name != null && !name.equals("")) {
                    rlContactInfo.setVisibility(View.VISIBLE);
                    tvContactName.setText(name);
                } else {
                    rlContactInfo.setVisibility(View.INVISIBLE);
                }
            }

        } else if (newContact.getEmail() != null && !newContact.getEmail().isEmpty()
            && (searchType == 2 || newContact.getUserFrom() == 3)) {
            tvPhoneDetail.setClickable(false);
            tvPhone.setText("邮箱");
            tvPhone.setVisibility(View.VISIBLE);
            tvPhoneDetail.setVisibility(View.VISIBLE);
            tvPhoneDetail.setText(newContact.getEmail());
        } else {
            tvPhoneDetail.setClickable(false);
            tvPhone.setVisibility(View.INVISIBLE);
            tvPhoneDetail.setVisibility(View.INVISIBLE);
            rlContactPhoneInfo.setVisibility(View.INVISIBLE);
            rlContactPhoneInfo.setVisibility(View.GONE);
            rlContactInfo.setVisibility(View.INVISIBLE);
            rlBlank.setVisibility(View.GONE);
        }

        if (newContact.getNickname() == null || newContact.getNickname().isEmpty()) {
            newContact.setNickname("未命名");
        }
        tvMeetingRoom.setText(newContact.getNickname());
        tvNubeDetail.setText(newContact.getNubeNumber());
        //
        phoneId.clear();

        if (newContact.getNumber() != null && !newContact.getNumber().isEmpty()) {
            invitedPhones[0] = newContact.getNumber();
        }
        phoneId.add(AccountManager.getInstance(ContactCardActivity.this)
            .getAccountInfo().nube);
        phoneId.add(tvNubeDetail.getText().toString());
        if (tvPhoneDetail.getText() != null) {
            invitedPhones[0] = tvPhoneDetail.getText().toString();
        } else {
            invitedPhones[0] = "";
        }

        if (newContact.getWorkUnit() != null && !newContact.getWorkUnit().isEmpty()) {
            tvUnit.setText(newContact.getWorkUnit());
        } else {
            tvUnit.setText("无");
        }

        if (newContact.getDepartment() != null && !newContact.getDepartment().isEmpty()) {
            tvDepartment.setText(newContact.getDepartment());
        } else {
            tvDepartment.setText("无");
        }

        if (newContact.getProfessional() != null && !newContact.getProfessional().isEmpty()) {
            tvProfessional.setText(newContact.getProfessional());
        } else {
            tvProfessional.setText("无");
        }

        if (newContact.getOfficeTel() != null && !newContact.getOfficeTel().isEmpty()) {
            tvOfficeTel.setText(newContact.getOfficeTel());
        } else {
            tvOfficeTel.setText("无");
        }

        if (newContact.getWorkUnitType() == 1) {
            tvUnitType.setText("医院");
            tvDepartmentType.setText("科室");
            tvProfessionalType.setText("职称");
            tvOfficeTelType.setText("科室电话");
        } else if (newContact.getWorkUnitType() == 2) {
            tvUnitType.setText("公司");
            tvDepartmentType.setText("部门");
            tvProfessionalType.setText("职位");
            tvOfficeTelType.setText("公司电话");
        }

        if (searchType == 1 || searchType == 7 || newContact.getUserFrom() == 2 || newContact.getUserFrom() == 1) {
            tvPhone.setText("手机号");
            if (newContact.getNumber() != null && !newContact.getNumber().isEmpty()) {
                tvPhoneDetail.setText(newContact.getNumber());
            } else {
                tvPhoneDetail.setText("无");
            }
        } else if (searchType == 2 || newContact.getUserFrom() == 3) {
            tvPhone.setText("邮箱");
            if (newContact.getEmail() != null && !newContact.getEmail().isEmpty()) {
                tvPhoneDetail.setText(newContact.getEmail());
            } else {
                tvPhoneDetail.setText("无");
            }
        } else if (searchType == 4 || searchType == 5 || searchType == 6
            || newContact.getUserFrom() == 4 || newContact.getUserFrom() == 5 || newContact.getUserFrom() == 6) {
            tvPhoneDetail.setText("");
            rlContactPhoneInfo.setVisibility(View.GONE);
            rlBlank.setVisibility(View.GONE);
        }

        if (null != (ContactManager.getInstance(ContactCardActivity.this)
            .getContactInfoByNubeNumber(mContact.getNubeNumber())).getContactId()
            && !(ContactManager.getInstance(ContactCardActivity.this)
            .getContactInfoByNubeNumber(mContact.getNubeNumber())).getContactId().isEmpty()) {
            //好友
            rlSendMessage.setVisibility(View.VISIBLE);
            rlJoinMeeting.setVisibility(View.VISIBLE);
            rlAddFriend.setVisibility(View.INVISIBLE);
            btnContactCardDel.setVisibility(View.VISIBLE);
        } else {
            //陌生人
            rlSendMessage.setVisibility(View.INVISIBLE);
            rlJoinMeeting.setVisibility(View.INVISIBLE);
            rlAddFriend.setVisibility(View.VISIBLE);
            btnContactCardDel.setVisibility(View.INVISIBLE);
            rlDepartment.setVisibility(View.INVISIBLE);
            rlPositionalTitle.setVisibility(View.INVISIBLE);
            rlDepartmentPhoneNumber.setVisibility(View.INVISIBLE);
            vBelowDepartmenTel.setVisibility(View.INVISIBLE);
            vBelowPositionalTitle.setVisibility(View.INVISIBLE);
            vBelowDepartment.setVisibility(View.INVISIBLE);

            if (!ContactManager.getInstance(this).checkNubeIsCustomService(
                newContact.getNubeNumber())) {
                tvMeetingRoom.setText(newContact.getNickname()
                    .replace(newContact.getNickname()
                        .substring(0, newContact.getNickname().length() - 1), "**"));
            }

        }

        if (ContactManager.getInstance(this).checkNubeIsCustomService(
            newContact.getNubeNumber())) {
            btnContactCardDel.setVisibility(View.GONE);
            rlContactCardContent.setVisibility(View.INVISIBLE);
            rlBlank.setVisibility(View.GONE);
            rlAddFriend.setVisibility(View.INVISIBLE);
            rlSendMessage.setVisibility(View.VISIBLE);
            rlJoinMeeting.setVisibility(View.VISIBLE);
            iamgeHead.setImageResource(R.drawable.contact_customservice);
            ibStartMeeting.setText("视频呼叫");
        } else {
            iamgeHead.setOnClickListener(mbtnHandleEventListener);
        }

    }


    @Override
    public void todoClick(int id) {
        super.todoClick(id);
        switch (id) {
            case R.id.tv_departmentphonenumber_detail:
                if (!tvOfficeTel.getText().toString().isEmpty()) {

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (ContactCardActivity.this.checkSelfPermission(Manifest.permission.CALL_PHONE) ==
                            PackageManager.PERMISSION_GRANTED) {
                            Intent i = new Intent(Intent.ACTION_CALL, Uri.parse("tel:"
                                + tvOfficeTel.getText().toString()));
                            startActivity(i);
                        } else {

                        }
                    } else {
                        Intent i = new Intent(Intent.ACTION_CALL, Uri.parse("tel:"
                            + tvOfficeTel.getText().toString()));
                        startActivity(i);
                    }

                }
                break;
            case R.id.iamgehead:
                if (null != mContact.getHeadUrl() && !mContact.getHeadUrl().equals("")) {
                    Intent intent_inputimage = new Intent(ContactCardActivity.this, OpenBigImageActivity.class);
                    intent_inputimage.putExtra(OpenBigImageActivity.DATE_TYPE, OpenBigImageActivity.DATE_TYPE_Internet);
                    intent_inputimage.putExtra(OpenBigImageActivity.DATE_URL, mContact.getHeadUrl());
                    startActivity(intent_inputimage);
                } else {
                    CustomToast.show(ContactCardActivity.this, "该图片地址为空", 1);
                }
                break;
            case R.id.btn_add_friend:
                if (searchType == 4) {
                    addtoLocalContact(4);
                } else if (searchType == 1) {
                    addtoLocalContact(2);
                } else if (searchType == 2) {
                    addtoLocalContact(3);
                } else if (searchType == 3) {
                    addtoLocalContact(0);
                } else if (searchType == 6) {
                    addtoLocalContact(6);
                } else if (searchType == 5) {
                    addtoLocalContact(5);
                } else if (searchType == 7) {
                    addtoLocalContact(1);
                }

                break;
            case R.id.tvphonedetail:
                if (!tvPhoneDetail.getText().toString().isEmpty()) {

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (ContactCardActivity.this.checkSelfPermission(Manifest.permission.CALL_PHONE) ==
                            PackageManager.PERMISSION_GRANTED) {
                            Intent i = new Intent(Intent.ACTION_CALL, Uri.parse("tel:"
                                + tvPhoneDetail.getText().toString()));
                            startActivity(i);
                        } else {

                        }
                    } else {
                        Intent i = new Intent(Intent.ACTION_CALL, Uri.parse("tel:"
                            + tvPhoneDetail.getText().toString()));
                        startActivity(i);
                    }

                }
                break;
            case R.id.ibstartmeeting:
                MobclickAgent
                    .onEvent(MedicalApplication.shareInstance().getApplicationContext(),
                        AnalysisConfig.CLICK_MEETING_IN_CONTACTCARD);
                createMeeting();
                break;
            case R.id.btn_sendmessage:
                enterChatActivity();
                break;

            case R.id.btncontactcarddel:
                if (null != mContact.getNubeNumber()) {
                    showDelContactDialog();
                }
                break;
            case R.id.btncontactcardback:
                if (create != null) {
                    create.cancel();
                }
                if (searchUsers != null) {
                    searchUsers.cancel();
                }
                ContactCardActivity.this.removeLoadingView();
                Intent intent = new Intent();
                if (REQUEST_CODE == REQUEST_CONTACT_CODE) {

                    intent.putExtra("isDataChanged", 0);

                } else if (REQUEST_CODE == ContactTransmitConfig.REQUEST_ADD_CODE) {
                    intent.putExtra("isDataChanged", 1);
                }
                setResult(ContactTransmitConfig.RESULT_CARD_CODE, intent);
                this.finish();
                break;
            default:
                break;
        }
    }


    private void addtoLocalContact(int isAddFrom) {
        final Contact newContact = new Contact();

        if (null != mContact && mContact.getNickname() != null && !mContact.getNickname().isEmpty()) {
            newContact.setFirstName(StringHelper.getHeadChar(mContact.getNickname()));
            newContact.setNickname(mContact.getNickname());
            newContact.setName(mContact.getNickname());
        } else {
            newContact.setFirstName(StringHelper.getHeadChar("未命名"));
            newContact.setNickname("未命名");
            newContact.setName("未命名");
        }
        newContact.setContactId(CommonUtil.getUUID());
        if (null != mContact) {
            newContact.setNubeNumber(mContact.getNubeNumber());
            newContact.setHeadUrl(mContact.getHeadUrl());
            newContact.setWorkUnit(mContact.getWorkUnit());
            newContact.setWorkUnitType(Integer.valueOf(mContact.getWorkUnitType()));
            newContact.setDepartment(mContact.getDepartment());
            newContact.setProfessional(mContact.getProfessional());
            newContact.setOfficeTel(mContact.getOfficeTel());
        }

        if (null != mContact && mContact.getNumber() != null && !mContact.getNumber().isEmpty()) {
            newContact.setNumber(mContact.getNumber());
        } else if (null != mContact && mContact.getEmail() != null && !mContact.getEmail().isEmpty()) {
            newContact.setEmail(mContact.getEmail());
        }

        // 設置usertype
        newContact.setAppType("mobile");
        newContact.setUserFrom(isAddFrom);
        CustomLog.d(TAG, "addtoLocalContact start");
        ContactManager.getInstance(ContactCardActivity.this).
            addContact(newContact,
                new ContactCallback() {
                    @Override
                    public void onFinished(ResponseEntry result) {
                        CustomLog.i(TAG, "onFinish! status: " + result.status
                            + " | content: " + result.content);
                        if (result.status >= 0) {
                            CustomLog.i(TAG, "addtoLocalContact success");
                            KeyEventWrite.write(KeyEventConfig.ADD_CONTACT_FROMDB
                                + "_ok" + "_"
                                + AccountManager.getInstance(
                                MedicalApplication.shareInstance().getApplicationContext())
                                .getAccountInfo().nube);
                            Intent intent = new Intent();
                            intent.setClass(ContactCardActivity.this, ContactCardActivity.class);
                            intent.putExtra("contact", newContact);
                            intent.putExtra("REQUEST_CODE", ContactTransmitConfig.REQUEST_ADD_CODE);
                            startActivityForResult(intent, 0);
                            finish();
                        } else if (result.status == -2) {
                            CustomLog.i(TAG, "onFinish! status: " + result.status
                                + " | content: " + result.content);
                            CustomToast.show(ContactCardActivity.this, "添加失败！不能添加自己为好友！", Toast.LENGTH_LONG);
                        } else {
                            CustomLog.i(TAG, "onFinish! status: " + result.status
                                + " | content: " + result.content);
                            CustomToast.show(ContactCardActivity.this, "添加失败！", 1);
                            KeyEventWrite.write(KeyEventConfig.ADD_CONTACT_FROMDB
                                + "_fail" + "_"
                                + AccountManager.getInstance(
                                MedicalApplication.shareInstance().getApplicationContext())
                                .getAccountInfo().nube + "_" + result.status + " | content: " + result.content);
                        }
                    }

                });

    }


    private void showDelContactDialog() {
        final ContactDeleteDialog cdd = new ContactDeleteDialog(
            ContactCardActivity.this, R.style.contact_del_dialog);
        cdd.setOkClickListener(new OkClickListener() {
            @Override
            public void clickListener() {
                ContactManager.getInstance(ContactCardActivity.this)
                    .logicDeleteContactById(ContactManager.getInstance(ContactCardActivity.this)
                            .getContactInfoByNubeNumber(mContact.getNubeNumber())
                            .getContactId(),
                        new ContactCallback() {
                            @Override
                            public void onFinished(ResponseEntry result) {
                                CustomLog.i(TAG, "onFinish! status: " + result.status
                                    + " | content: " + result.content);
                                if (result.status >= 0) {
                                    CustomLog.d(TAG, "删除联系人" + mContact.getNubeNumber());
                                    KeyEventWrite
                                        .write(KeyEventConfig.DELETE_CONTACT_FROMDB
                                            + "_ok"
                                            + "_"
                                            + AccountManager.getInstance(
                                            MedicalApplication.shareInstance()
                                                .getApplicationContext())
                                            .getAccountInfo().nube);
                                    CustomToast.show(ContactCardActivity.this, "删除成功！", 1);
                                    Intent intent = getIntent();
                                    intent.putExtra("isDataChanged", 1);
                                    setResult(ContactTransmitConfig.RESULT_CARD_CODE, intent);
                                    finish();
                                } else {
                                    KeyEventWrite
                                        .write(KeyEventConfig.DELETE_CONTACT_FROMDB
                                            + "_fail"
                                            + "_"
                                            + AccountManager.getInstance(
                                            MedicalApplication.shareInstance()
                                                .getApplicationContext())
                                            .getAccountInfo().nube + "_"
                                            + result.status + " | content: " + result.content);
                                    CustomLog.d(TAG, "删除联系人" + mContact.getNubeNumber());
                                    CustomToast.show(ContactCardActivity.this, "删除失败！", 1);
                                }
                            }

                        });

            }
        });
        cdd.setNoClickListener(new NoClickListener() {
            @Override
            public void clickListener() {
                cdd.dismiss();
            }
        });
        Window window = cdd.getWindow();
        window.setGravity(Gravity.BOTTOM);
        cdd.setCanceledOnTouchOutside(true);
        cdd.show();
        WindowManager windowManager = getWindowManager();
        Display display = windowManager.getDefaultDisplay();
        WindowManager.LayoutParams lp = cdd.getWindow().getAttributes();
        lp.width = (int) (display.getWidth()); // 设置宽度
        lp.height = (int) (0.22 * display.getHeight()); // 设置高度
        cdd.getWindow().setAttributes(lp);
        WindowManager.LayoutParams wlp = cdd.getWindow().getAttributes();
        wlp.dimAmount = 0.4f;
        cdd.getWindow().setAttributes(wlp);
        cdd.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
    }


    private void enterChatActivity() {
        if (mContact.getNubeNumber() == null) {
            CustomToast.show(this, "nubeNumber 视讯号不存在", 1);
            return;
        }

        if (mContact.getNubeNumber().length() == 0) {
            CustomToast.show(this, "nubeNumber 视讯号为不存在", 1);
            return;
        }

        Intent i = new Intent(ContactCardActivity.this, ChatActivity.class);
        i.putExtra(ChatActivity.KEY_NOTICE_FRAME_TYPE,
            ChatActivity.VALUE_NOTICE_FRAME_TYPE_NUBE);
        i.putExtra(ChatActivity.KEY_CONVERSATION_NUBES,
            mContact.getNubeNumber());
        i.putExtra(ChatActivity.KEY_CONVERSATION_SHORTNAME,
            mContact.getName());
        i.putExtra(ChatActivity.KEY_CONVERSATION_TYPE,
            ChatActivity.VALUE_CONVERSATION_TYPE_SINGLE);
        startActivity(i);
    }


    @Override
    protected void onResume() {
        super.onResume();
    }


    @Override
    protected void onStop() {
        super.onStop();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
    }


    @Override
    public void onBackPressed() {

        if (create != null) {
            create.cancel();
        }

        if (searchUsers != null) {
            searchUsers.cancel();
        }

        ContactCardActivity.this.removeLoadingView();
        Intent intent = new Intent();
        if (REQUEST_CODE == REQUEST_CONTACT_CODE) {

            intent.putExtra("isDataChanged", 0);

        } else if (REQUEST_CODE == ContactTransmitConfig.REQUEST_ADD_CODE) {
            intent.putExtra("isDataChanged", 1);
        }
        setResult(ContactTransmitConfig.RESULT_CARD_CODE, intent);
        super.onBackPressed();
        this.finish();
    }
}
