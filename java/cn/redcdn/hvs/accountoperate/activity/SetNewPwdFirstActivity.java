package cn.redcdn.hvs.accountoperate.activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.redcdn.keyeventwrite.KeyEventConfig;
import com.redcdn.keyeventwrite.KeyEventWrite;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cn.redcdn.commonutil.NetConnectHelper;
import cn.redcdn.datacenter.meetingmanage.data.ResponseEmpty;
import cn.redcdn.datacenter.usercenter.SendCodeForResetPwd;
import cn.redcdn.hvs.AccountManager;
import cn.redcdn.hvs.MedicalApplication;
import cn.redcdn.hvs.R;
import cn.redcdn.hvs.config.SettingData;
import cn.redcdn.hvs.util.CustomToast;
import cn.redcdn.hvs.util.TitleBar;
import cn.redcdn.log.CustomLog;
import cn.redcdn.network.httprequest.HttpErrorCode;

public class SetNewPwdFirstActivity extends cn.redcdn.hvs.base.BaseActivity {
    private EditText numEditText = null;
    private Button nextBtn = null;
    private Button backBtn = null;
    private String tag = SetNewPwdFirstActivity.class.getName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        CustomLog.d(TAG, "onCreate:" + this.toString());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_senewpwdfirst);
        TitleBar titleBar = getTitleBar();
        titleBar.setTitle("重置密码");
        titleBar.setTitleTextColor(Color.BLACK);
        titleBar.enableBack();
        numEditText = (EditText) findViewById(R.id.setnewpwdfirst_inputnum_edit);
        numEditText.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before,
                                      int count) {

            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                                          int after) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                nextBtn.setOnClickListener(mbtnHandleEventListener);
                if (numEditText.getText() != null
                        && !numEditText.getText().toString()
                        .equalsIgnoreCase("")) {
                    nextBtn.setBackgroundResource(R.drawable.button_selector);
                    nextBtn.setClickable(true);
                } else {
                    nextBtn.setClickable(false);
                    nextBtn.setBackgroundResource(R.drawable.button_btn_notclick);
                }

            }
        });
        nextBtn = (Button) findViewById(R.id.setnewpwd_next_btn);
        //nextBtn.setClickable(false);
        nextBtn.setBackgroundResource(R.drawable.button_btn_notclick);
        backBtn = (Button) findViewById(R.id.back_btn);

        backBtn.setOnClickListener(mbtnHandleEventListener);



    }


    @Override
    public void todoClick(int i) {
        // TODO Auto-generated method stub
        super.todoClick(i);
        switch (i) {
            case R.id.back_btn:
                Intent iw = new Intent();
                iw.setClass(SetNewPwdFirstActivity.this, LoginActivity.class);
                startActivity(iw);
                SetNewPwdFirstActivity.this.finish();
                break;
            case R.id.setnewpwd_next_btn:
                execNext();
                break;
        }
    }


    private void execNext() {

        if (!inValidNum(numEditText.getText().toString()) && !isEmail(numEditText.getText().toString())) {
            CustomToast.show(SetNewPwdFirstActivity.this, "手机号/邮箱号格式不正确！",
                    Toast.LENGTH_LONG);
            return;
        } else if (inValidNum(numEditText.getText().toString())) {
            CustomLog.d(tag,"该账号是手机号");
            phonesendCodeForResetPwd();
        } else if(isEmail(numEditText.getText().toString())){
            CustomLog.d(tag,"该账号是邮箱号");
            emailsendCodeForResetPwd();
        }

    }

    private boolean inValidNum(String num) {
        boolean is = false;
        if (numEditText.getText().toString().length() < 11) {
            return is;
        }

        Pattern p = Pattern
                .compile("^((1))\\d{10}$");
        Matcher m = p.matcher(num);
        if (m.matches())
            is = true;
        return is;
    }

    public boolean isEmail(String email) {
        String str = "^([a-zA-Z0-9_\\-\\.]+)@((\\[[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.)|(([a-zA-Z0-9\\-]+\\.)+))([a-zA-Z]{2,4}|[0-9]{1,3})(\\]?)$";
        Pattern p = Pattern.compile(str);
        Matcher m = p.matcher(email);

        return m.matches();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent iw = new Intent();
        iw.setClass(SetNewPwdFirstActivity.this, LoginActivity.class);
        startActivity(iw);
    }


    private void phonesendCodeForResetPwd() {

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
                SetNewPwdFirstActivity.this.removeLoadingView();
                CustomToast.show(SetNewPwdFirstActivity.this,"验证码已发往您的手机："+numEditText.getText().toString() +"请及时获取验证码进行重置密码！",7000);
                Intent iw = new Intent();
                iw.setClass(SetNewPwdFirstActivity.this, NumberSetNewPwdActivity.class);//重置密码
                iw.putExtra("type","1");
                iw.putExtra("account", numEditText.getText().toString());
                startActivity(iw);
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
                SetNewPwdFirstActivity.this.removeLoadingView();
                if (HttpErrorCode.checkNetworkError(statusCode)) {
                    CustomToast.show(SetNewPwdFirstActivity.this,
                            "网络不给力，请检查网络！", Toast.LENGTH_LONG);
                    return;
                }
                if (NetConnectHelper.getNetWorkType(MedicalApplication.getContext()) == NetConnectHelper.NETWORKTYPE_INVALID) {
                    CustomToast.show(SetNewPwdFirstActivity.this,
                            "网络不给力，请检查网络！", Toast.LENGTH_LONG);
                    return;
                }
                if (statusCode == -410 || statusCode == -411) {
                    CustomToast.show(SetNewPwdFirstActivity.this, "该手机号尚未注册，请重新输入！",
                            Toast.LENGTH_LONG);
                    return;
                }
                if (statusCode == -452) {
                    CustomToast.show(SetNewPwdFirstActivity.this,
                            "手机号一小时内最多获取5次验证码，请稍后再次获取",
                            Toast.LENGTH_LONG);
                    return;
                }
                if (statusCode == -93) {
                    CustomToast.show(SetNewPwdFirstActivity.this, "该手机号尚未注册，请重新输入！",
                            Toast.LENGTH_LONG);

                    return;
                }
                CustomToast.show(SetNewPwdFirstActivity.this, "验证码获取失败=" + statusCode,
                        Toast.LENGTH_LONG);

            }
        };
        SetNewPwdFirstActivity.this.showLoadingView("获取中", new DialogInterface.OnCancelListener() {

            @Override
            public void onCancel(DialogInterface dialog) {
                dialog.dismiss();
                sr.cancel();
                CustomToast.show(SetNewPwdFirstActivity.this, "验证码获取取消",
                        Toast.LENGTH_LONG);

            }
        });
        sr.sendCodeForResetPwd(numEditText.getText().toString(), SettingData.AUTH_PRODUCT_ID, SendCodeForResetPwd.ProductType_HVS);
    }

    private void emailsendCodeForResetPwd() {

        final SendCodeForResetPwd sr = new SendCodeForResetPwd() {

            @Override
            protected void onSuccess(ResponseEmpty responseContent) {
                KeyEventWrite.write(KeyEventConfig.SEND_CHECKCODE
                        + "_ok" + "_"
                        + AccountManager.getInstance(
                        MedicalApplication.shareInstance().getApplicationContext())
                        .getAccountInfo().nube);
                CustomLog.v(tag,
                        "phonesendCodeForResetPwd onSuccess responseContent");
                SetNewPwdFirstActivity.this.removeLoadingView();
                CustomToast.show(SetNewPwdFirstActivity.this,"验证码已发往您的电子邮箱："+numEditText.getText().toString() +"请及时获取验证码进行重置密码！",7000);
                Intent iw = new Intent();
                iw.setClass(SetNewPwdFirstActivity.this, NumberSetNewPwdActivity.class);//重置密码
                iw.putExtra("account", numEditText.getText().toString());
                iw.putExtra("type","2");
                startActivity(iw);
            }

            @Override
            protected void onFail(int statusCode, String statusInfo) {
                KeyEventWrite.write(KeyEventConfig.SEND_CHECKCODE + "_fail" + "_"
                        + AccountManager.getInstance(
                        MedicalApplication.shareInstance().getApplicationContext())
                        .getAccountInfo().nube + "_"
                        + statusCode);
                CustomLog.v(tag, "emailSendCodeForResetPwd onFail statusCode="
                        + statusCode);
                SetNewPwdFirstActivity.this.removeLoadingView();
                if (HttpErrorCode.checkNetworkError(statusCode)) {
                    CustomToast.show(SetNewPwdFirstActivity.this,
                            "网络不给力，请检查网络！", Toast.LENGTH_LONG);
                    return;
                }
                if (NetConnectHelper.getNetWorkType(MedicalApplication.getContext()) == NetConnectHelper.NETWORKTYPE_INVALID) {
                    CustomToast.show(SetNewPwdFirstActivity.this,
                            "网络不给力，请检查网络！", Toast.LENGTH_LONG);
                    return;
                }
                if (statusCode == -410 || statusCode == -411) {
                    CustomToast.show(SetNewPwdFirstActivity.this, "该邮箱号尚未注册，请重新输入！",
                            Toast.LENGTH_LONG);
                    return;
                }
                if (statusCode == -452) {
                    CustomToast.show(SetNewPwdFirstActivity.this,
                            "邮箱号1小时内注册验证最多5次，请稍后再次注册",
                            Toast.LENGTH_LONG);
                    return;
                }
                if (statusCode == -93) {
                    CustomToast.show(SetNewPwdFirstActivity.this, "该邮箱号尚未注册，请重新输入！",
                            Toast.LENGTH_LONG);

                    return;
                }
                CustomToast.show(SetNewPwdFirstActivity.this, "验证码获取失败=" + statusCode,
                        Toast.LENGTH_LONG);

            }
        };
        SetNewPwdFirstActivity.this.showLoadingView("获取中", new DialogInterface.OnCancelListener() {

            @Override
            public void onCancel(DialogInterface dialog) {
                dialog.dismiss();
                sr.cancel();
                CustomToast.show(SetNewPwdFirstActivity.this, "验证码获取取消",
                        Toast.LENGTH_LONG);

            }
        });
        sr.sendCodeForResetPwd(numEditText.getText().toString(), SettingData.AUTH_PRODUCT_ID, SendCodeForResetPwd.ProductType_HVS);
    }


}

