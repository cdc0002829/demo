package cn.redcdn.hvs.base;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import cn.redcdn.crash.Crash;
import cn.redcdn.hvs.config.SettingData;
import cn.redcdn.hvs.meeting.meetingManage.MedicalMeetingManage;
import com.umeng.analytics.MobclickAgent;

import cn.redcdn.hvs.AccountManager;
import cn.redcdn.hvs.HomeActivity;
import cn.redcdn.hvs.MedicalApplication;
import cn.redcdn.hvs.boot.SplashActivity;
import cn.redcdn.hvs.util.CommonUtil;
import cn.redcdn.hvs.util.CustomToast;
import cn.redcdn.hvs.util.TitleBar;
import cn.redcdn.log.CustomLog;
import cn.redcdn.log.LogcatFileManager;

public class BaseActivity extends AppCompatActivity {
    protected final String TAG = getClass().getName();
    private Dialog mLoadingDialog = null;


    /* 标示退出应用，在某些页面需要点击两次直接退出应用 */
    private boolean isExit = false;

    private boolean twiceToExit = false;
    private static final int MSG_EXIT = 0x00101010;

    private boolean isHandleEvent = false;
    public View.OnClickListener mbtnHandleEventListener = null;
    private static final int IsHandleMsg = 99;

    private TitleBar titleBar;


    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_EXIT:
                    isExit = false;
                    break;
            }
        }
    };



    @SuppressWarnings("deprecation")
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getPointerCount() > 1) {
            if (ev.getAction() != MotionEvent.ACTION_POINTER_1_DOWN
                    && ev.getAction() != MotionEvent.ACTION_POINTER_1_UP
                    && ev.getAction() != MotionEvent.ACTION_DOWN
                    && ev.getAction() != MotionEvent.ACTION_UP
                    && ev.getAction() != MotionEvent.ACTION_MOVE
                    && ev.getAction() != MotionEvent.ACTION_CANCEL) {
                return true;
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        CustomLog.d(TAG, "onCreate:" + this.toString());
        super.onCreate(savedInstanceState);

        mbtnHandleEventListener = new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                if(isHandleEvent == true){
                    System.out.println("触摸过快,返回");
                    return;
                }
                else{
                    System.out.println("触摸成功,isHandleEvent = true");
                    todoClick(v.getId());
                    isHandleEvent = true;
                    Message msg = Message.obtain();
                    msg.what = IsHandleMsg;
                    msg.obj = v.getId();
                    isHandleEventhandler.sendMessageDelayed(msg, 200);
                }
            }

        };

    }

    public  void todoClick(int i) {

    }

    public TitleBar getTitleBar() {
        if (titleBar == null) {
            titleBar = new TitleBar(this,
                    ((ViewGroup) findViewById(android.R.id.content))
                            .getChildAt(0));
        }
        return titleBar;
    }

    Handler isHandleEventhandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == IsHandleMsg) {
                isHandleEvent = false;
                System.out.println("200ms到时，isHandleEvent = false");
            }
        }
    };


    @Override
    protected void onStart() {
        CustomLog.d(TAG, "onStart:" + this.toString());
        //对当前是否正在进行会议进行判断如果正在开会那么进入会议
        if (!this.toString().contains("SplashActivity")) {
            if (!MedicalApplication.shareInstance().getInitStatus()) {
                // CustomLog.e(TAG, "onStart 应用程序未启动，kill-self");
                //android.os.Process.killProcess(android.os.Process.myPid());
                LogcatFileManager.getInstance().setLogDir(SettingData.LogRootDir);
                LogcatFileManager.getInstance().start(getPackageName());

                Crash crash = new Crash();
                crash.setDir(SettingData.LogRootDir);
                crash.init(this, getPackageName());

                CustomLog.e(TAG, "onStart 应用程序未启动，重新执行启动逻辑");
                boolean readCache = AccountManager.getInstance(getApplicationContext()).readLoginCache();
                if(readCache == true) {
                    CustomLog.d(TAG, "onStart 应用程序未启动，读到登录缓存数据,使用缓存数据重建现场");
                    MedicalApplication.shareInstance().recoverApplication();
                } else {
                    CustomLog.e(TAG, "onStart 应用程序未启动，且未读到登录缓存数据");
                    Intent intent = new Intent();
                    intent.setClass(this, SplashActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                }
            }
            if (AccountManager.getInstance(this).getLoginState()==AccountManager.LoginState.ONLINE){
            MedicalMeetingManage.getInstance().resumeMeeting();}
        }

        MedicalApplication.shareInstance().insertActivity(this);
        super.onStart();
    }

    @Override
    protected void onRestart() {
        super.onRestart();

        if (!this.toString().contains("MainActivity")
                && !this.toString().contains("SplashActivity")
                && !this.toString().contains("MeetingRoomActivity")
                && !this.toString().contains("Dialog")) {
//            boolean isMeetingRoomRunning = MeetingManager.getInstance()
//                    .getMeetingRoomRunningState();
//            CustomLog.d(TAG, "onRestart isMeetingRoomRunning "+isMeetingRoomRunning);
//            if (isMeetingRoomRunning) {
//                // 切换到会诊室页面
//                CustomLog.d(TAG, "onResume: 会诊室界面运行中，切换到会诊室界面");
//                //Intent i = new Intent();
//                //i.setClass(this, MeetingRoomActivity.class);
//                //startActivity(i);
//                AccountInfo info = AccountManager.getInstance(BaseActivity.this).getAccountInfo();
//                int meetingId = getIntent().getIntExtra(
//                        ConstConfig.MEETING_ID,0);
//                if(info!=null)
//                    MeetingManager.getInstance().joinMeeting(info.accesstoken, info.nubeNumber, info.nickName, meetingId);
//            }
        }
    }

    @Override
    protected void onPause() {
        CustomLog.d(TAG, "onPause:" + this.toString());
        super.onPause();
        //隐藏软键盘
        CommonUtil.hideSoftInputFromWindow(this);
        MobclickAgent.onPause(this);
    }

    @Override
    protected void onResume() {
        CustomLog.d(TAG, "onResume:" + this.toString());
        super.onResume();
        MobclickAgent.onResume(this);
        if(getRequestedOrientation()!= ActivityInfo.SCREEN_ORIENTATION_PORTRAIT){
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
    }

    @Override
    protected void onStop() {
        CustomLog.d(TAG, "onStop:" + this.toString());
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        CustomLog.d(TAG, "onDestroy:" + this.toString());
        MedicalApplication.shareInstance().deleteActivity(this);
        super.onDestroy();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        CustomLog.d(TAG, "onNewIntent:" + this.toString());
        super.onNewIntent(intent);
    }

    @Override
    public void onBackPressed() {
        if (twiceToExit) {
            exit();
        } else {
            super.onBackPressed();
        }
    }

    public void allowTwiceToExit() {
        twiceToExit = true;
    }

    private void exit() {
        if (!isExit) {
            isExit = true;
            CustomToast.show(getApplicationContext(), "再按一次退出程序", Toast.LENGTH_SHORT);
            mHandler.sendEmptyMessageDelayed(MSG_EXIT, 2000);
        } else {
            MedicalApplication.shareInstance().exit();
        }
    }

    protected void onMenuBtnPressed() {
        CustomLog.d(TAG, "BaseActivity::onMenuBtnPressed()");
    }

    //joinMeeting(String token, String accountID, String accountName,
    //   int meetingID)
    protected void switchToMeetingRoomActivity(String token, String accountID, String accountName,
                                               int meetingId, String adminId) {
        CustomLog.d(TAG,
                "BaseActivity::switchToMeetingRoomActivity() 切换到会诊室页面. meetingId: "
                        + meetingId + " | adminId: " + adminId);
//        MeetingManager.getInstance().init(getApplicationContext());
//        MeetingManager.getInstance().joinMeeting(token, accountID, accountName, meetingId);
        //Intent i = new Intent();
        //i.setClass(this, MeetingRoomActivity.class);
        //i.putExtra(ConstConfig.MEETING_ID, meetingId);
        //i.putExtra(ConstConfig.ADMIN_PHONE_ID, adminId);
        //startActivity(i);

    }

    protected void switchToMainActivity() {
        Intent i = new Intent(BaseActivity.this, HomeActivity.class);
        startActivity(i);
    }

    protected void showLoadingView(String message) {
        CustomLog.i(TAG, "MeetingActivity::showLoadingDialog() msg: " + message);
        try {
            if (mLoadingDialog != null) {
                mLoadingDialog.dismiss();
            }
            mLoadingDialog = CommonUtil.createLoadingDialog(this, message);
            mLoadingDialog.show();
        } catch (Exception ex) {
            CustomLog.d(TAG, "BaseActivity::showLoadingView()" + ex.toString());
        }
    }

    protected void showLoadingView(String message,
                                   DialogInterface.OnCancelListener listener) {
        CustomLog.i(TAG, "MeetingActivity::showLoadingDialog() msg: " + message);
        try {
            if (mLoadingDialog != null) {
                mLoadingDialog.dismiss();
            }
            mLoadingDialog = CommonUtil.createLoadingDialog(this, message, listener);

            mLoadingDialog.show();
        } catch (Exception ex) {
            CustomLog.d(TAG, "BaseActivity::showLoadingView()" + ex.toString());
        }
    }

    protected void showLoadingView(String message,
                                   final DialogInterface.OnCancelListener listener, boolean cancelAble) {
        CustomLog.i(TAG, "MeetingActivity::showLoadingDialog() msg: " + message);
        try {
            if (mLoadingDialog != null) {
                mLoadingDialog.dismiss();
            }} catch (Exception ex) {
            CustomLog.d(TAG, "BaseActivity::showLoadingView()" + ex.toString());
        }
        mLoadingDialog = CommonUtil.createLoadingDialog(this, message, listener);
        mLoadingDialog.setCancelable(cancelAble);
        mLoadingDialog.setOnKeyListener(new OnKeyListener() {

            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    listener.onCancel(dialog);
                }
                return false;
            }
        });
        try {
            mLoadingDialog.show();
        } catch (Exception ex) {
            CustomLog.d(TAG, "BaseActivity::showLoadingView()" + ex.toString());
        }
    }

    protected void removeLoadingView() {
        CustomLog.i(TAG, "MeetingActivity::removeLoadingView()");
        if (mLoadingDialog != null && mLoadingDialog.isShowing()) {
            try {
                mLoadingDialog.dismiss();
            }catch (Exception ex) {
                ex.printStackTrace();
            }

            mLoadingDialog = null;
        }
    }
    protected String getLogTag() {
        return this.getClass().getSimpleName();
    }
}
