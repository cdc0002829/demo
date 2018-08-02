package cn.redcdn.hvs.appinstall;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.ClipDrawable;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import cn.redcdn.commonutil.NetConnectHelper;
import cn.redcdn.log.CustomLog;
import cn.redcdn.hvs.MedicalApplication;
import cn.redcdn.hvs.R;
import cn.redcdn.hvs.util.CommonUtil;
import cn.redcdn.hvs.util.CustomToast;
import com.umeng.analytics.MobclickAgent;

public class ForcedInstallActivity extends Activity {
  protected final String TAG = getClass().getName();
  private TextView okBtn;
  private TextView exitBtn;
  private ProgressBar progressBar;
  private RelativeLayout confirmL;
  private RelativeLayout dowloadL;
  private TextView txt;
  private TextView title;
  private TextView content;
  public static final int NORMAR = 0;
  public static final int TIP = 1;
  public static final int CONFIRM = 3;
  public static final int DOWNLOAD = 4;
  public static final int RETRY = 5;
  private int state = NORMAR;
  private boolean isReTry = false;
  private String chagelist;

  private BroadcastReceiver myReceiver = new BroadcastReceiver() {

    @Override
    public void onReceive(Context context, Intent intent) {
      String action = intent.getAction();
      if (action.equals("cn.redcdn.meeting.changeui")) {
        String ui = intent.getStringExtra("ui");
        if (ui.equals("DOWNLOAD")) {
          changeUI(DOWNLOAD, "");
        } else {
          String msg = intent.getStringExtra("msg");
          changeUI(RETRY, msg);
        }
      } else if (action.equals("cn.redcdn.meeting.changeprogress")) {
        int percent = intent.getIntExtra("progress", 0);
        setMyProgress(percent);
        setProgressTxt(percent);
      } else {
        CustomLog.e(TAG, "ForcedInstallActivity.this.finish(); ");       
        MeetingVersionManager.getInstance().switchToAppInstall();
        ForcedInstallActivity.this.finish();
      }
    }

  };

  public boolean isReTry() {
    return isReTry;
  }

  public void setReTry(boolean isReTry) {
    this.isReTry = isReTry;
  }

  private class MyListener implements OnClickListener {
    @Override
    public void onClick(View v) {
      switch (v.getId()) {
      case R.id.install_sure_btn:
        if (state == TIP) {
          MeetingVersionManager.getInstance().autoDismiss();
        } else {
          checkWeb();
        }
        break;
      case R.id.install_cancel_btn:
        ForcedInstallActivity.this.finish();
        MedicalApplication.shareInstance().exit();
        break;
      default:
        break;
      }

    }
  }

  private void checkWeb() {
    if (NetConnectHelper.getNetWorkType(this) == NetConnectHelper.NETWORKTYPE_INVALID){
      changeUI(RETRY, "网络异常,请检查网络连接!");
    } else if (NetConnectHelper.getNetWorkType(this) == NetConnectHelper.NETWORKTYPE_WIFI) {
      MeetingVersionManager.getInstance().autoDismiss();
    } else if (MedicalApplication.shareInstance().getDownloadSetting()) {
      CustomToast.show(ForcedInstallActivity.this,
          "您现在处于2G/3G/4G网络，建议在wifi下下载安装包！", CustomToast.LENGTH_LONG);
      MeetingVersionManager.getInstance().autoDismiss();
    } else {
      changeUI(TIP, "");
    }
  }

  private void initUI() {
    CustomLog.d(TAG, "initUI   ");
    confirmL = (RelativeLayout) findViewById(R.id.confirme_container);
    dowloadL = (RelativeLayout) findViewById(R.id.download_container);
    okBtn = (TextView) findViewById(R.id.install_sure_btn);
    progressBar = (ProgressBar) findViewById(R.id.progress_bar);
    exitBtn = (TextView) findViewById(R.id.install_cancel_btn);
    txt = (TextView) findViewById(R.id.download_txt);
    okBtn.setOnClickListener(new MyListener());
    exitBtn.setOnClickListener(new MyListener());
    title = (TextView) findViewById(R.id.confirme_titile);
    content = (TextView) findViewById(R.id.confirme_content);
    content.setMovementMethod(ScrollingMovementMethod.getInstance());
    changeUI(CONFIRM, "");
    IntentFilter filter = new IntentFilter();
    filter.addAction("cn.redcdn.meeting.changeui");
    filter.addAction("cn.redcdn.meeting.changeprogress");
    filter.addAction("cn.redcdn.meeting.dimiss");
    registerReceiver(myReceiver, filter);
    CustomLog.d(TAG, "initUI  end   ");
  }

  /**
   * 设置当前进度
   * 
   * @param progress
   *          进度值
   */
  public void setMyProgress(int progress) {
    // CustomLog.d(TAG, "setProgress " + progress);
    progressBar.setProgress(progress);
  }

  /**
   * 设置进度的最大值
   * 
   * @param max
   *          最大值
   */
  public void setMax(int max) {
    progressBar.setMax(max);
  }

  public void dosss() {
    ClipDrawable drawable = (ClipDrawable) progressBar.getBackground();
    drawable.setLevel(drawable.getLevel() + 200);
  }

  /**
   * 获取进度最大值
   * 
   * @return
   */
  public int getMax() {
    return progressBar.getMax();
  }

  public void changeUI(int pt, String txt) {
    switch (pt) {
    case CONFIRM:
      state = CONFIRM;
      dowloadL.setVisibility(View.GONE);
      confirmL.setVisibility(View.VISIBLE);
      title.setText("请升级");
      content.setText(chagelist);
      okBtn.setText("立即升级");
      exitBtn.setText("退出");
      break;
    case DOWNLOAD:
      state = DOWNLOAD;
      // setMyProgress(0);
      confirmL.setVisibility(View.GONE);
      dowloadL.setVisibility(View.VISIBLE);
      break;
    case RETRY:
      state = RETRY;
      confirmL.setVisibility(View.VISIBLE);
      dowloadL.setVisibility(View.GONE);
      title.setText("请升级");
      content.setText(txt);
      okBtn.setText("重试");
      exitBtn.setText("取消");
      break;
    case TIP:
      state = TIP;
      dowloadL.setVisibility(View.GONE);
      confirmL.setVisibility(View.VISIBLE);
      title.setText("提示");
      content.setText("您当前正在使用移动网络，继续下载安装包将产生流量费用");
      okBtn.setText("继续");
      exitBtn.setText("取消");
      break;
    default:
      break;
    }
  }

  public void setProgressTxt(int percent) {
    txt.setText(percent + "%");

  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    CustomLog.d(TAG, "onCreate   ");
    setFinishOnTouchOutside(false);
    setContentView(R.layout.dialog_forced_install);
    String list=getIntent().getStringExtra("changelist");
    if(list!=null&&!list.equals("")){
    	chagelist="新功能：\r\n"+list;
    }else{
    	chagelist="发现您有新版本，请及时更新";
    }
    initUI();
  }

  @Override
  protected void onDestroy() {
    CustomLog.d(TAG, "onDestroy   ");
    if (myReceiver != null) {
      unregisterReceiver(myReceiver);
    }
    MeetingVersionManager.getInstance().resetStateWhenForcedException();
    super.onDestroy();
  }

  @Override
  public void onBackPressed() {
    CustomLog.d("onBackPressed", "ignore backpress key");
    return;
  }

  @Override
  public void onResume() {
    CustomLog.d(TAG, "onResume   ");
    super.onResume();
    MobclickAgent.onResume(this);
  }

  @Override
  public void onPause() {
    CustomLog.d(TAG, "onPause   ");
    super.onPause();
    MobclickAgent.onPause(this);
  }

  @Override
  protected void onStop() {
    CustomLog.d(TAG, "onStop   ");
    super.onStop();  
  }
}
