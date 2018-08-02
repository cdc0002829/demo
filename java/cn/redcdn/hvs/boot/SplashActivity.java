package cn.redcdn.hvs.boot;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.umeng.analytics.MobclickAgent;

import java.io.File;

import cn.redcdn.datacenter.enterprisecenter.data.AccountInfo;
import cn.redcdn.datacenter.medicalcenter.data.MDSAccountInfo;
import cn.redcdn.hvs.AccountManager;
import cn.redcdn.hvs.AccountManager.LoginListener;
import cn.redcdn.hvs.AccountManager.LoginState;
import cn.redcdn.hvs.HomeActivity;
import cn.redcdn.hvs.MedicalApplication;
import cn.redcdn.hvs.R;
import cn.redcdn.hvs.accountoperate.activity.LoginActivity;
import cn.redcdn.hvs.accountoperate.activity.PhoneRegisterActivity;
import cn.redcdn.hvs.base.BaseActivity;
import cn.redcdn.hvs.config.SettingData;
import cn.redcdn.hvs.util.CommonUtil;
import cn.redcdn.hvs.util.CustomDialog;
import cn.redcdn.hvs.util.CustomDialog.OKBtnOnClickListener;
import cn.redcdn.hvs.util.CustomToast;
import cn.redcdn.hvs.util.switchLayout.OnViewChangeListener;
import cn.redcdn.hvs.util.switchLayout.SwitchLayout;
import cn.redcdn.hvs.util.youmeng.AnalysisConfig;
import cn.redcdn.log.CustomLog;

public class SplashActivity extends BaseActivity {
    private BootManager mBootManager;

    public final static String EXTRA_FROM_MESSAGE_LINK = "EXTRA_FROM_MESSAGE_LINK";

    private SwitchLayout switchLayout;// 自定义的控件
    private LinearLayout linearLayout;
    private int mViewCount;// 自定义控件中子控件的个数
    private ImageView mImageView[];// 底部的imageView
    private int mCurSel;// 当前选中的imageView
    private Button loginBtn;
    private Button registerBtn;
    private String urlMeetingId;//短信链接内容中获取的meetingId
    private RelativeLayout rlWelcome;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //解决：点击home键后再点击应用图标，统一进入主页的问题
//        if((getIntent().getFlags() & Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT) != 0){
//            finish();
//            return;
//        }
        setContentView(R.layout.activity_splash);
        rlWelcome = (RelativeLayout) findViewById(R.id.welcome_container);
        allowTwiceToExit();
        processData();
        Boolean resultwrite = true;
        Boolean resultread=true;
        resultwrite = CommonUtil.selfPermissionGranted(SplashActivity.this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
        resultread = CommonUtil.selfPermissionGranted(SplashActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE);
        if (!(resultwrite&&resultread)) {
            final CustomDialog cd = new CustomDialog(SplashActivity.this);
            cd.setTip("请给予存储权限");
            cd.removeCancelBtn();
            cd.setOkBtnText("知道了");
            cd.setOkBtnOnClickListener(new CustomDialog.OKBtnOnClickListener() {

                @Override
                public void onClick(CustomDialog customDialog) {
                    MedicalApplication.shareInstance().exit();
                }
            });
            cd.show();
        } else{
            // 如果程序未启动，执行启动逻辑
            if (!MedicalApplication.shareInstance().getInitStatus()) {
                mBootManager = new BootManager(this) {
                    @Override
                    public void onBootSuccess() {
                        SplashActivity.this.onBootSuccess();
                    }

                    @Override
                    public void onBootFailed(int step, int errorCode, String errorMsg) {
                        SplashActivity.this.onBootFailed(step, errorCode, errorMsg);
                    }
                };
                mBootManager.start();
            } else {
                SplashActivity.this.onBootSuccess();
            }
    }
    }

    private void initGuideUI() {

        switchLayout = (SwitchLayout) findViewById(R.id.switchLayoutID);
        linearLayout = (LinearLayout) findViewById(R.id.linerLayoutID);

        // 得到子控件的个数
        mViewCount = switchLayout.getChildCount();
        mImageView = new ImageView[mViewCount];
        // 设置imageView
        for (int i = 0; i < mViewCount; i++) {
            // 得到LinearLayout中的子控件
            mImageView[i] = (ImageView) linearLayout.getChildAt(i);
            mImageView[i].setEnabled(false);// 控件激活
            mImageView[i].setOnClickListener(new MOnClickListener());
            mImageView[i].setTag(i);// 设置与view相关的标签
        }
        // 设置第一个imageView不被激活
        mCurSel = 0;
        mImageView[mCurSel].setEnabled(true);
        switchLayout.setOnViewChangeListener(new MOnViewChangeListener());

        loginBtn = (Button) findViewById(R.id.login_btn);
        registerBtn = (Button) findViewById(R.id.register_btn);

        loginBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                MedicalApplication.shareInstance().setFirstRun(false);
                login();
                findViewById(R.id.guide_container).setVisibility(View.GONE);
                findViewById(R.id.welcome_container).setVisibility(View.VISIBLE);
            }
        });

        registerBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                MobclickAgent.onEvent(SplashActivity.this, AnalysisConfig.ACCESS_REGIST);
                MedicalApplication.shareInstance().setFirstRun(false);
//                Intent i = new Intent();
//                i.setClass(SplashActivity.this, RegisterFirstActivity.class);
//                startActivity(i);
//                finish();
            }
        });
    }

    private class MOnClickListener implements OnClickListener {
        @Override
        public void onClick(View v) {
            int pos = (Integer) v.getTag();
            setCurPoint(pos);
            switchLayout.snapToScreen(pos);
        }
    }

    private void setCurPoint(int pos) {
        CustomLog.d(TAG, "SplashActivity::setCurPoint , pos : " + pos);
        if (pos < 0 || pos > mViewCount - 1 || mCurSel == pos)
            return;
        mImageView[mCurSel].setEnabled(false);
        mImageView[pos].setEnabled(true);
        mCurSel = pos;
    }

    private class MOnViewChangeListener implements OnViewChangeListener {
        @Override
        public void onViewChange(int view) {
            CustomLog.d(TAG, "SplashActivity::OnViewChangeListener , onViewChange : "
                    + view);

            if (view == mViewCount - 1) {
                findViewById(R.id.operate_btn_container).setVisibility(View.VISIBLE);
                linearLayout.setVisibility(View.GONE);
            } else {
                findViewById(R.id.operate_btn_container).setVisibility(View.GONE);
                linearLayout.setVisibility(View.VISIBLE);
            }

            setCurPoint(view);
        }
    }

    private void processData() {
        // 判断是否通过短信链接启动应用
        Intent intent = getIntent();
        if (intent != null) {
            String url = intent.getDataString();
            CustomLog.i(TAG, "判断是否是通过短信链接启动应用: " + url);
            if (url != null &&( url.startsWith("http")||url.startsWith("cn.redcdn.hvs"))) {

                try{
                    //短信链接内容中获取的meetingId
                    urlMeetingId = url.substring("cn.redcdn.hvs://".length(), url.length());
                    CustomLog.i(TAG, "通过短信链接获取到的 meetingId:"+urlMeetingId);
                }catch(Exception e){
                    e.printStackTrace();
                }

                MobclickAgent.onEvent(SplashActivity.this, AnalysisConfig.JOINMEETING_BY_MESSAGE);
                MedicalApplication.shareInstance().setIsFromMessageLink(true);
            }

        } else {
            CustomLog.i(TAG, "SplashActivity::processData 不是通过短信链接启动");
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        processData();
        if (MedicalApplication.shareInstance().getInitStatus()) {
            onBootSuccess();
        }
    }

    /**
     * 启动成功 获取登录状态，已登录的情况下检查是否是通过短信链接启动，如果是，则跳转到会诊列表页面
     */
    private void onBootSuccess() {
        CustomLog.i(TAG, "SplashActivity::onBootSuccess 启动成功");
        if (mBootManager != null) {
            mBootManager.release();
            mBootManager = null;
        }

        File Log_file = new File(SettingData.getInstance().CfgPath + "/Log.xml");
        File media_server_agent_file = new File(SettingData.getInstance().CfgPath + "/media_server_agent.xml");
        File n8config_file = new File(SettingData.getInstance().CfgPath+ "/n8config.txt");
        File LogFileUpdateConfig_file = new File(SettingData.getInstance().CfgPath + "/LogFileUpdateConfig.xml");
        File ShortLinkConfig_file = new File(SettingData.getInstance().CfgPath + "/ShortLinkConfig.xml");

//        if(!Log_file.exists()||
//                !media_server_agent_file.exists()||
//                !n8config_file.exists()||
//                !LogFileUpdateConfig_file.exists()||
//                !ShortLinkConfig_file.exists()
//                ){
            if(false){

            CustomLog.d(TAG,"SD卡不可用");

            final CustomDialog dialog = new CustomDialog(this);

            dialog.setOkBtnOnClickListener(new OKBtnOnClickListener() {
                @Override
                public void onClick(CustomDialog customDialog) {
                    dialog.cancel();
                    finish();
                    System.exit(0);
                }
            });
            dialog.setTip("SD卡不可用");
            dialog.removeCancelBtn();
            dialog.setCancelable(false);
            dialog.setOkBtnText("退出");
            dialog.show();

        }else{

            MedicalApplication.shareInstance().setInit(true);
//            MeetingManager.getInstance().init(getApplicationContext());
//            MeetingManager.getInstance().setConfigPath(SettingData.getInstance().CfgPath);
//            MeetingManager.getInstance().setContactOperationImp(ContactManager.getInstance(getApplicationContext()));
//            MeetingManager.getInstance().setHostAgentOperation(HostAgentClient.getInstance());
//            MeetingManager.getInstance().setLogPath(SettingData.getInstance().LogFileOutPath);
            final SharedPreferences sharedPreferences = MedicalApplication
                    .shareInstance().getSharedPreferences("shareHttpRequestConfig",
                            Context.MODE_PRIVATE);
//            if(sharedPreferences!=null)
//                MeetingManager.getInstance().setRcAddress(sharedPreferences.getString(
//                        "RC_URL", ""));
//            MeetingManager.getInstance().setProjectType(MeetingManager.PROJECT_HVS,MeetingManager.MEETING_APP_BUTEL_CONSULTATION);
//            MeetingManager.getInstance().setAccountManagerOperation(AccountManager.getInstance(getApplicationContext()));
            //设置非wifi进入会议
//            SharedPreferences SettingsharedPreferences=this.getSharedPreferences("setting", MODE_PRIVATE);
//            boolean Network = SettingsharedPreferences.getBoolean("webSetting", false);
//            MeetingManager.getInstance().setIsAllowMobileNet(Network);

    	       /* boolean isFirstBoot = MedicalApplication.shareInstance().isFirstRun();

    	        if (isFirstBoot) { // 第一次启动应用，使用引导页面
    	          findViewById(R.id.guide_container).setVisibility(View.VISIBLE);
    	          findViewById(R.id.welcome_container).setVisibility(View.GONE);
    	          initGuideUI();

    	          return;
    	        }*/

            LoginState loginState = AccountManager.getInstance(getApplicationContext())
                    .getLoginState();
            CustomLog.i(TAG, "SplashActivity::onBootSuccess 登录状态:  " + loginState);
            if (loginState == LoginState.ONLINE) {
//    	          switchToMainActivity();
                Intent i = new Intent(SplashActivity.this, HomeActivity.class);
                i.putExtra("urlMeetingId", urlMeetingId);
                startActivity(i);
                finish();
            } else if (loginState == LoginState.OFFLINE) {
                login();
            }

        }

    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mBootManager != null && mBootManager.getCurrentStep() == BootManager.MSG_CHECK_APP_VERSION) {
            CustomLog.d(TAG, "SplashActivity::onResume 继续执行检测应用版本");
            mBootManager.retry(mBootManager.getCurrentStep());
        }
    }

    private void onBootFailed(final int step, final int errorLevel,
                              final String errorMsg) {
        CustomLog.e(TAG, "启动出错!  errorMsg: " + errorMsg);
        if (MedicalApplication.shareInstance().getInitStatus()) {
            CustomLog.e(TAG, "启动已完成，不处理启动出错信息!");
        }
        CustomDialog dialog = new CustomDialog(this);
        dialog.setOkBtnOnClickListener(new OKBtnOnClickListener() {

            @Override
            public void onClick(CustomDialog customDialog) {
                MedicalApplication.shareInstance().exit();
            }
        });
        dialog.setTip("网络异常，请检查网络连接！");
        dialog.removeCancelBtn();
        dialog.setCancelable(false);
        dialog.setOkBtnText("确定");
        dialog.show();
    }

    private void login() {
        AccountManager.getInstance(MedicalApplication.shareInstance())
                .registerLoginCallback(new LoginListener() {

                    @Override
                    public void onLoginFailed(int errorCode, String msg) {
                        CustomLog.e(TAG, "自动登录出错!  errorMsg: " + msg);
                        Intent i = new Intent();
                        i.setClass(SplashActivity.this, LoginActivity.class);
                        startActivity(i);
                        finish();
                    }

                    @Override
                    public void onLoginSuccess(MDSAccountInfo account) {
                        //此处增加声音检测，防止首次登录成功后立即2次返回，未进行检测
                        SharedPreferences sharedPreferences= getSharedPreferences("VDS",Activity.MODE_PRIVATE);
                        int hasVoiceDetect = sharedPreferences.getInt("hasVoiceDetect", 0);
                        System.out.println("hasVoiceDetect = "+ hasVoiceDetect);
                        if(hasVoiceDetect == 1){
                            System.out.println("已经检测过，进入主页");
                            MobclickAgent.onEvent(SplashActivity.this, AnalysisConfig.ACCESS_HOME);
			    	 switchToMainActivity();
                            Intent i = new Intent(SplashActivity.this, HomeActivity.class);
                            i.putExtra("urlMeetingId", urlMeetingId);
                            startActivity(i);
                            finish();
                        }
                        else{

                            System.out.println("未检测过,进行检测");
                            switchToMainActivity();
//                            Intent inn = new Intent();
//
//                            inn.setClass(SplashActivity.this, FirstLoginVoiceDetectActivity.class);
//                            startActivity(inn);
//                            SplashActivity.this.finish();
                        }

                    }
                });

        AccountManager.getInstance(MedicalApplication.shareInstance()).login();
    }

}
