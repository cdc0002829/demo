package cn.redcdn.hvs.im.activity;

import android.content.Intent;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.UnsupportedEncodingException;

import cn.redcdn.datacenter.medicalcenter.MDSAppUploadGroupNotice;
import cn.redcdn.hvs.AccountManager;
import cn.redcdn.hvs.R;
import cn.redcdn.hvs.base.BaseActivity;
import cn.redcdn.hvs.util.CustomDialog;
import cn.redcdn.hvs.util.CustomToast;
import cn.redcdn.hvs.util.TitleBar;

import static cn.redcdn.datacenter.medicalcenter.MDSErrorCode.MDS_TOKEN_DISABLE;

public class GroupAnnouncementEditActivity extends BaseActivity {
    private EditText announcement_edit;
    private Button titlerightbtn;
    private RelativeLayout titleLayout;
    private TitleBar titleBar;
    private String mGroupId, announceContent;
    private final int resultcode = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_announcement_edit);
        initWidget();
        initData();
//        setButtonState();

    }
    private void initWidget(){
        titleBar = getTitleBar();
        titleBar.setTitle("群公告");
        titleBar.setBackText("取消");
        titleBar.enableRightBtn("完成", 0, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                submitGroupAnnouncement();
            }
        });
        announcement_edit = (EditText) findViewById(R.id.group_announcement_edit);  //群公告内容
        titleLayout = (RelativeLayout) findViewById(R.id.title);
        titlerightbtn = (Button) titleLayout.findViewById(R.id.right_btn);
    }
    private void initData(){
        announcement_edit.addTextChangedListener(textWatcher);
        Intent intent = getIntent();
        mGroupId = intent.getStringExtra(GroupAnnouncementActivity.GROUP_ID);
        announceContent = intent.getStringExtra(GroupAnnouncementActivity.ANNOUNCEMENT);
        announcement_edit.setText(announceContent);
        announcement_edit.setSelection(announcement_edit.getText().length());

    }

    private void submitGroupAnnouncement() {
        String announcement = announcement_edit.getText().toString()  ;
//        InputFilter[] filters = {new InputFilter.LengthFilter(10)};
//        announcement_edit.setFilters(filters);
        byte[] bff= new byte[0];
        try {
            bff = announcement.getBytes("GB2312");//utf-8编码一个汉子占三个字节
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        int count=bff.length;
        if (announcement.length() == 0) {
            CustomToast.show(GroupAnnouncementEditActivity.this, "群公告不能为空", CustomToast.LENGTH_SHORT);
            return;
        }
        if (count > 3000) {
            CustomToast.show(GroupAnnouncementEditActivity.this, "群公告字符不能超过3000", CustomToast.LENGTH_SHORT);
            return;
        }
        titlerightbtn.setTextColor(Color.parseColor("#cc49afcc"));
        final CustomDialog tipDlg = new CustomDialog(GroupAnnouncementEditActivity.this);
        tipDlg.setTip("该公告会通知全部群成员，是否发布？");
        tipDlg.setOkBtnText("发布");
        tipDlg.setCancelBtnText("取消");
        tipDlg.setOkBtnOnClickListener(new CustomDialog.OKBtnOnClickListener() {
            @Override
            public void onClick(CustomDialog customDialog) {
                MDSAppUploadGroupNotice appUploadGroupNotice = new MDSAppUploadGroupNotice() {
                    @Override
                    protected void onSuccess(JSONObject responseContent) {
                        super.onSuccess(responseContent);
                        CustomToast.show(GroupAnnouncementEditActivity.this, "上交成功", CustomToast.LENGTH_SHORT);
                        GroupAnnouncementEditActivity.this.removeLoadingView();
                        Intent intent = new Intent(GroupAnnouncementEditActivity.this, GroupChatDetailActivity.class);
                        String announcement=announcement_edit.getText().toString();
                        intent.putExtra(GroupAnnouncementActivity.NEW_ANNOUNCEMENT, announcement);
                        GroupAnnouncementEditActivity.this.setResult(resultcode, intent);
                        GroupAnnouncementEditActivity.this.finish();
                    }

                    @Override
                    protected void onFail(int statusCode, String statusInfo) {
                        super.onFail(statusCode, statusInfo);
                        CustomToast.show(GroupAnnouncementEditActivity.this, "上交失败,请重试", CustomToast.LENGTH_SHORT);
                        GroupAnnouncementEditActivity.this.removeLoadingView();
                        if(statusCode==MDS_TOKEN_DISABLE){
                            AccountManager.getInstance(GroupAnnouncementEditActivity.this).tokenAuthFail(statusCode);
                        }else {
                            CustomToast.show(GroupAnnouncementEditActivity.this, statusInfo , Toast.LENGTH_LONG);
                        }

                    }

                };
                String token = AccountManager.getInstance(GroupAnnouncementEditActivity.this).getToken();
                appUploadGroupNotice.appUploadGroupNotice(token, mGroupId, announcement_edit.getText().toString());
                GroupAnnouncementEditActivity.this.showLoadingView("正在上传公告");
                tipDlg.dismiss();
            }
        });
        tipDlg.setCancelBtnOnClickListener(new CustomDialog.CancelBtnOnClickListener() {
            @Override
            public void onClick(CustomDialog customDialog) {
                tipDlg.dismiss();
            }
        });
        tipDlg.show();

    }
    private TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void afterTextChanged(Editable editable) {
            setButtonState();
        }


    };

    private void setButtonState(){
        if (announcement_edit.getText().toString().equals("")) {
            titlerightbtn.setTextColor(Color.parseColor("#222625"));
        } else {
            titlerightbtn.setTextColor(Color.parseColor("#49afcc"));
        }
    }
}
