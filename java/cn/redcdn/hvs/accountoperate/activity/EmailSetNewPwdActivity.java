package cn.redcdn.hvs.accountoperate.activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.CountDownTimer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.redcdn.keyeventwrite.KeyEventConfig;
import com.redcdn.keyeventwrite.KeyEventWrite;

import cn.redcdn.commonutil.NetConnectHelper;
import cn.redcdn.datacenter.meetingmanage.data.ResponseEmpty;
import cn.redcdn.datacenter.usercenter.ResetPassword;
import cn.redcdn.datacenter.usercenter.SendCodeForResetPwd;
import cn.redcdn.datacenter.usercenter.data.UserInfo;
import cn.redcdn.hvs.AccountManager;
import cn.redcdn.hvs.MedicalApplication;
import cn.redcdn.hvs.R;
import cn.redcdn.hvs.base.*;
import cn.redcdn.hvs.config.SettingData;
import cn.redcdn.hvs.util.CommonUtil;
import cn.redcdn.hvs.util.CustomToast;
import cn.redcdn.hvs.util.TitleBar;
import cn.redcdn.log.CustomLog;
import cn.redcdn.network.httprequest.HttpErrorCode;


public class EmailSetNewPwdActivity extends cn.redcdn.hvs.base.BaseActivity{
    private EditText authcode = null;
    private EditText newlogincode = null;
    private Button nextBtn = null;
    private Button backBtn = null;
    private TextView resend = null;
    private TextView checkCodeTime = null;
    private String account = "";
    private String tag = EmailSetNewPwdActivity.class.getName();
    private TimeCount tc = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_email_set_new_pwd);
        TitleBar titleBar = getTitleBar();
        titleBar.setTitle("设置新密码");
        titleBar.setTitleTextColor(Color.BLACK);
        titleBar.enableBack();
        account = getIntent().getStringExtra("account");
        initWidget();
        resend.setClickable(false);
        resend.setTextColor(Color.parseColor("#8d8d8d"));
        resend.setText("重新获取");
        checkCodeTime.setVisibility(View.VISIBLE);
        tc = new TimeCount(60000, 1000);
        tc.start();

    }
    private void initWidget() {
        TitleBar titleBar = getTitleBar();
        titleBar.setTitle("设置新密码");
        titleBar.setTitleTextColor(Color.BLACK);
        titleBar.enableBack();
        authcode = (EditText) this.findViewById(R.id.email_set_new_ped_checkcode_edit);
        newlogincode = (EditText) this.findViewById(R.id.email_set_new_ped_newpwd_edit);
        resend = (TextView) this.findViewById(R.id.email_set_new_pwd_getcheckcode);
//		getCheckCodeBtn.setOnClickListener(new OnClickListener());
        backBtn = (Button) this.findViewById(R.id.back_btn);
        nextBtn = (Button) this.findViewById(R.id.setnewpwd_finish_btn);
//		finishBtn.setOnClickListener(new OnClickListener());
        checkCodeTime = (TextView) this.findViewById(R.id.email_set_new_pwd_checkcodetime);

        resend.setOnClickListener(mbtnHandleEventListener);
        backBtn.setOnClickListener(mbtnHandleEventListener);
        nextBtn.setOnClickListener(mbtnHandleEventListener);

        authcode.addTextChangedListener(new TextWatcher() {

            @Override
            public void afterTextChanged(Editable s) {

            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                                          int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before,
                                      int count) {
                if ((newlogincode.getText() != null && !newlogincode.getText().toString()
                        .equalsIgnoreCase(""))
                        && (authcode.getText() != null && !authcode
                        .getText().toString().equalsIgnoreCase(""))) {
                    nextBtn.setBackgroundResource(R.drawable.button_selector);
                    nextBtn.setClickable(true);
                } else {
                    nextBtn.setClickable(false);
                    nextBtn.setBackgroundResource(R.drawable.button_btn_notclick);
                }

            }

        });
        newlogincode.addTextChangedListener(new TextWatcher() {

            @Override
            public void afterTextChanged(Editable s) {

            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                                          int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before,
                                      int count) {
                if ((newlogincode.getText() != null && !newlogincode.getText().toString()
                        .equalsIgnoreCase(""))
                        && (authcode.getText() != null && !authcode
                        .getText().toString().equalsIgnoreCase(""))) {
                    nextBtn.setBackgroundResource(R.drawable.button_selector);
                    nextBtn.setClickable(true);
                } else {
                    nextBtn.setClickable(false);
                    nextBtn.setBackgroundResource(R.drawable.button_btn_notclick);
                }

            }

        });
        nextBtn.setClickable(false);
        nextBtn.setBackgroundResource(R.drawable.button_btn_notclick);

    }
    @Override
    public void todoClick(int id) {
        // TODO Auto-generated method stub
        super.todoClick(id);
        switch (id) {
//            case R.id.setnewpwd_back:
//                Intent i = new Intent();
//                i.setClass(NumberSetNewPwdActivity.this, LoginActivity.class);
//                startActivity(i);
//                NumberSetNewPwdActivity.this.finish();
//                break;
            case R.id.setnewpwd_finish_btn:

                resetPassword();
                break;
            case R.id.getcheckcode_text:
                sendCodeForResetPwd();
                break;

        }
    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent iw = new Intent();
        iw.setClass(EmailSetNewPwdActivity.this, SetNewPwdFirstActivity.class);
        startActivity(iw);
        EmailSetNewPwdActivity.this.finish();
    }

    private void sendCodeForResetPwd() {

        final SendCodeForResetPwd sr = new SendCodeForResetPwd() {

            @Override
            protected void onSuccess(ResponseEmpty responseContent) {
                KeyEventWrite.write(KeyEventConfig.SEND_CHECKCODE
                        + "_ok" + "_"
                        + AccountManager.getInstance(
                        MedicalApplication.shareInstance().getApplicationContext())
                        .getAccountInfo().nube);
                CustomLog.v(tag,
                        "sendCodeForResetPwd onSuccess responseContent");
                EmailSetNewPwdActivity.this.removeLoadingView();
                resend.setClickable(false);
                resend.setTextColor(Color.parseColor("#8d8d8d"));
                resend.setText("重新获取");
                checkCodeTime.setVisibility(View.VISIBLE);
                tc = new EmailSetNewPwdActivity.TimeCount(60000, 1000);
                tc.start();
            }

            @Override
            protected void onFail(int statusCode, String statusInfo) {
                KeyEventWrite.write(KeyEventConfig.SEND_CHECKCODE + "_fail" + "_"
                        + AccountManager.getInstance(
                        MedicalApplication.shareInstance().getApplicationContext())
                        .getAccountInfo().nube + "_"
                        + statusCode);
                CustomLog.v(tag, "SendCodeForResetPwd onFail statusCode="
                        + statusCode);
                EmailSetNewPwdActivity.this.removeLoadingView();
                resend.setClickable(true);
                resend.setTextColor(Color.parseColor("#35b7c6"));
                resend.setVisibility(View.GONE);
                if (HttpErrorCode.checkNetworkError(statusCode)) {
                    CustomToast.show(EmailSetNewPwdActivity.this,
                            "网络不给力，请检查网络！", Toast.LENGTH_LONG);
                    return;
                }

                if (NetConnectHelper.getNetWorkType(MedicalApplication.getContext()) == NetConnectHelper.NETWORKTYPE_INVALID) {
                    CustomToast.show(EmailSetNewPwdActivity.this,
                            "网络不给力，请检查网络！", Toast.LENGTH_LONG);
                    return;
                }
                if (statusCode == -410 || statusCode == -411) {
                    CustomToast.show(EmailSetNewPwdActivity.this, "该手机号尚未注册，请重新输入！",
                            Toast.LENGTH_LONG);
                    return;
                }
                if (statusCode == -452) {
                    CustomToast.show(EmailSetNewPwdActivity.this,
                            "手机号1小时内注册验证最多5次，请稍后再次注册",
                            Toast.LENGTH_LONG);
                    return;
                }
                if (statusCode == -93) {
                    CustomToast.show(EmailSetNewPwdActivity.this, "该手机号尚未注册，请重新输入！",
                            Toast.LENGTH_LONG);

                    return;
                }
                CustomToast.show(EmailSetNewPwdActivity.this, "验证码获取失败=" + statusCode,
                        Toast.LENGTH_LONG);
            }
        };
        EmailSetNewPwdActivity.this.showLoadingView("获取中", new DialogInterface.OnCancelListener() {

            @Override
            public void onCancel(DialogInterface dialog) {
                dialog.dismiss();
                sr.cancel();
                CustomToast.show(EmailSetNewPwdActivity.this, "验证码获取取消",
                        Toast.LENGTH_LONG);

            }
        });
        sr.sendCodeForResetPwd(account, SettingData.AUTH_PRODUCT_ID, SendCodeForResetPwd.ProductType_HVS);
    }

    private void resetPassword() {
        String checkCode = authcode.getText().toString();
        String pwdEditString = newlogincode.getText().toString();
        if (checkCode != null && !checkCode.equalsIgnoreCase("")
                && pwdEditString != null && !pwdEditString.equalsIgnoreCase("")) {
            if (checkCode.length() < 6) {
                CustomToast.show(EmailSetNewPwdActivity.this, "验证码格式不正确！",
                        Toast.LENGTH_LONG);
            } else if (pwdEditString.length() < 6) {
                CustomToast.show(EmailSetNewPwdActivity.this, "新密码格式错误！",
                        Toast.LENGTH_LONG);
            } else {

                final ResetPassword rp = new ResetPassword() {

                    @Override
                    protected void onSuccess(UserInfo responseContent) {
                        KeyEventWrite.write(KeyEventConfig.RESETPASSWORD
                                + "_ok" + "_"
                                + AccountManager.getInstance(
                                MedicalApplication.shareInstance().getApplicationContext())
                                .getAccountInfo().nube);
                        EmailSetNewPwdActivity.this.removeLoadingView();
                        CustomLog.v(tag,
                                "ResetPassword onSuccess responseContent="
                                        + responseContent);
                        CustomToast.show(EmailSetNewPwdActivity.this, "重置密码成功",
                                Toast.LENGTH_LONG);
                        Intent i = new Intent();
                        i.setClass(EmailSetNewPwdActivity.this, LoginActivity.class);
                        startActivity(i);
                        EmailSetNewPwdActivity.this.finish();
                    }

                    @Override
                    protected void onFail(int statusCode, String statusInfo) {
                        KeyEventWrite.write(KeyEventConfig.RESETPASSWORD + "_fail" + "_"
                                + AccountManager.getInstance(
                                MedicalApplication.shareInstance().getApplicationContext())
                                .getAccountInfo().nube + "_"
                                + statusCode);
                        EmailSetNewPwdActivity.this.removeLoadingView();
                        CustomLog.v(tag, "ResetPassword onFail statusCode="
                                + statusCode);
                        if (NetConnectHelper.getNetWorkType(MedicalApplication.getContext()) == NetConnectHelper.NETWORKTYPE_INVALID) {
                            CustomToast.show(EmailSetNewPwdActivity.this,
                                    "网络不给力，请检查网络！", Toast.LENGTH_LONG);
                            return;
                        }
                        if (statusCode == -406) {
                            CustomToast.show(EmailSetNewPwdActivity.this, "验证码错误，请重新获取！",
                                    Toast.LENGTH_LONG);
                        } else {
                            CustomToast.show(EmailSetNewPwdActivity.this, "重置密码失败="
                                    + statusCode, Toast.LENGTH_LONG);
                        }
                    }
                };
                EmailSetNewPwdActivity.this.showLoadingView("提交中",
                        new DialogInterface.OnCancelListener() {

                            @Override
                            public void onCancel(DialogInterface dialog) {
                                dialog.dismiss();
                                rp.cancel();
                                CustomToast.show(EmailSetNewPwdActivity.this,
                                        "重置密码取消", Toast.LENGTH_LONG);
                            }
                        });
                rp.resetPassword(account, checkCode,
                        CommonUtil.string2MD5(pwdEditString));
            }
        } else {
            CustomToast.show(EmailSetNewPwdActivity.this, "验证码或者新密码不能为空",
                    Toast.LENGTH_LONG);
        }
    }
    class TimeCount extends CountDownTimer {
        public TimeCount(long millisInFuture, long countDownInterval) {

            super(millisInFuture, countDownInterval);// 参数依次为总时长,和计时的时间间隔
            CustomLog.v(tag, "计时器");
        }

        @Override
        public void onFinish() {// 计时完毕时触发
            resend.setClickable(true);
            resend.setTextColor(Color.parseColor("#f76626"));
            resend.setText("点击重新获取");
            checkCodeTime.setVisibility(View.GONE);
        }

        @Override
        public void onTick(long millisUntilFinished) {// 计时过程显示
            checkCodeTime.setText("(" + millisUntilFinished / 1000 + ")");
        }
    }

}

