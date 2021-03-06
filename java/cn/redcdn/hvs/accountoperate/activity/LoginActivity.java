package cn.redcdn.hvs.accountoperate.activity;

import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import cn.redcdn.commonutil.NetConnectHelper;
import cn.redcdn.datacenter.medicalcenter.data.MDSAccountInfo;
import cn.redcdn.hvs.AccountManager;
import cn.redcdn.hvs.AccountManager.LoginListener;
import cn.redcdn.hvs.HomeActivity;
import cn.redcdn.hvs.MedicalApplication;
import cn.redcdn.hvs.R;
import cn.redcdn.hvs.base.BaseActivity;
import cn.redcdn.hvs.im.preference.DaoPreference;
import cn.redcdn.hvs.im.preference.DaoPreference.PrefType;
import cn.redcdn.hvs.util.CustomToast;
import cn.redcdn.hvs.util.TitleBar;
import cn.redcdn.log.CustomLog;
import cn.redcdn.network.httprequest.HttpErrorCode;

public class LoginActivity extends BaseActivity {
    private EditText userNameEdit = null;
    private EditText pwdEdit = null;
    private Button registerBtn = null;
    private Button loginBtn = null;
    private Button loginForgetBtn = null;
    private String tag = LoginActivity.class.getName();
    private String mToken;
    private TitleBar titleBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        CustomLog.d(TAG, "onCreate:" + this.toString());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        allowTwiceToExit();
        titleBar = getTitleBar();
        titleBar.setTitle("登录");
        titleBar.setTitleTextColor(Color.BLACK);
        titleBar.enableRightBtn("注册", 0,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent i = new Intent();
                        i.setClass(LoginActivity.this, PhoneRegisterActivity.class);
                        startActivity(i);
                        return;
                    }
                });
        titleBar.getSubRightBtn().setTextColor(Color.BLACK);
        initWidget();
    }

    private void initWidget() {
        userNameEdit = (EditText) this.findViewById(R.id.Login_numdemo_edit);
        userNameEdit.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before,
                                      int count) {

                if (s.toString().contains(" ")) {
                    String[] str = s.toString().split(" ");
                    String str1 = "";
                    for (int i = 0; i < str.length; i++) {
                        str1 += str[i];
                    }
                    userNameEdit.setText(str1);

                    userNameEdit.setSelection(start);

                }

            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                                          int after) {

            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        pwdEdit = (EditText) this.findViewById(R.id.Login_pwddemo_edit);
        loginBtn = (Button) this.findViewById(R.id.login_login_btn);
        loginBtn.setOnClickListener(mbtnHandleEventListener);
        loginForgetBtn = (Button) this.findViewById(R.id.login_forgetpwd_btn);
        loginForgetBtn.setOnClickListener(mbtnHandleEventListener);
//		userNameEdit.setHint("请输入视讯号或手机号");
        userNameEdit.addTextChangedListener(new TextWatcher() {

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
                if ((pwdEdit.getText() != null && !pwdEdit.getText().toString()
                        .equalsIgnoreCase(""))
                        && (userNameEdit.getText() != null && !userNameEdit
                        .getText().toString().equalsIgnoreCase(""))) {
                    loginBtn.setBackgroundResource(R.drawable.button_selector);
                    loginBtn.setClickable(true);
                } else {
                    loginBtn.setClickable(false);
                    loginBtn.setBackgroundResource(R.drawable.button_btn_notclick);
                }
            }
        });
        pwdEdit.addTextChangedListener(new TextWatcher() {

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
                if ((pwdEdit.getText() != null && !pwdEdit.getText().toString()
                        .equalsIgnoreCase(""))
                        && (userNameEdit.getText() != null && !userNameEdit
                        .getText().toString().equalsIgnoreCase(""))) {
                    loginBtn.setBackgroundResource(R.drawable.button_selector);
                    loginBtn.setClickable(true);
                } else {
                    loginBtn.setClickable(false);
                    loginBtn.setBackgroundResource(R.drawable.button_btn_notclick);
                }
            }
        });

        loginBtn.setBackgroundResource(R.drawable.button_btn_notclick);
        loginBtn.setClickable(false);
    }

    @Override
    public void todoClick(int i) {
        switch (i) {

            case R.id.login_forgetpwd_btn:
                loginForgetBtn.setTextColor(Color.parseColor("#1c8dad"));
                gotoResetPwd();
                break;
            case R.id.login_login_btn:

                AccountManager.getInstance(MedicalApplication.shareInstance())
                        .registerLoginCallback(new LoginListener() {

                            @Override
                            public void onLoginFailed(int errorCode, String msg) {
                                LoginActivity.this.removeLoadingView();
                                if (HttpErrorCode.checkNetworkError(errorCode)) {
                                    CustomToast.show(LoginActivity.this,
                                            R.string.login_checkNetworkError, Toast.LENGTH_LONG);
                                    return;
                                }

                                if (NetConnectHelper.getNetWorkType(MedicalApplication.shareInstance()) == NetConnectHelper.NETWORKTYPE_INVALID) {
                                    CustomToast.show(LoginActivity.this,
                                            R.string.login_checkNetworkError, Toast.LENGTH_LONG);
                                    return;
                                }
                                if (errorCode == -909) { //未提交审核资料
                                    Intent intent = new Intent(LoginActivity.this, CardTypeActivity.class);
                                    startActivity(intent);

                                    return;
                                }

                                CustomToast.show(LoginActivity.this, msg, Toast.LENGTH_LONG);
                                if (!msg.equals("")) {
                                    if (msg.contains("账号或密码有误")) {
                                        pwdEdit.setText("");
                                    }
                                }
                            }

                            @Override
                            public void onLoginSuccess(
                                    MDSAccountInfo account) {


                                int state = AccountManager.getInstance(MedicalApplication.shareInstance()).getAccountInfo().getState();
                                //审核状态 (1：待审核、2：审核通过、3：未通过)
                                if (state == 1) {
                                    LoginActivity.this.removeLoadingView();
                                    CustomLog.e(TAG, "未审核");
                                    Intent intent = new Intent(LoginActivity.this, AuditingActivity.class);
                                    startActivity(intent);

                                } else if (state == 2) {
                                    LoginActivity.this.removeLoadingView();
                                    CustomLog.e(TAG, "登录成功");
                                    Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                                    intent.putExtra("", "");
                                    startActivity(intent);
                                    finish();
                                } else if (state == 3) {
                                    LoginActivity.this.removeLoadingView();
                                    CustomLog.e(TAG, "审核未通过");
                                    Intent intent = new Intent(LoginActivity.this, AuditErrorActivity.class);
                                    startActivity(intent);

                                }


                                DaoPreference daoPre = MedicalApplication.getPreference();

                                daoPre.setKeyValue(PrefType.LOGIN_NUMBER_CHANGE, account.getNube());
                                daoPre.setKeyValue(PrefType.BEFORE_LOGIN_NUMBER, account.getNube());
                                daoPre.setKeyValue(PrefType.USER_NICKNAME, account.getNickName());
                                daoPre.setKeyValue(PrefType.LOGIN_NUBENUMBER, account.getNube());
                                daoPre.setKeyValue(PrefType.LOGIN_MOBILE, account.getMobile());
                                ;
                                daoPre.setKeyValue(PrefType.LOGIN_ACCESSTOKENID, account.getAccessToken());


                            }

                        });
                LoginActivity.this.showLoadingView("登录中", new OnCancelListener() {

                    @Override
                    public void onCancel(DialogInterface dialog) {
                        dialog.dismiss();
                        AccountManager.getInstance(
                                MedicalApplication.shareInstance()).cancelLogin();
                        CustomToast.show(LoginActivity.this, R.string.login_cancel,
                                Toast.LENGTH_LONG);

                    }
                });
                AccountManager.getInstance(MedicalApplication.shareInstance())
                        .login(userNameEdit.getText().toString(),
                                pwdEdit.getText().toString());

                break;

        }

    }

    private void gotoResetPwd() {

        Intent iw = new Intent();
        iw.setClass(LoginActivity.this, SetNewPwdFirstActivity.class);
        startActivity(iw);


    }

}
