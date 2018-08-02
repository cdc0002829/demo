package cn.redcdn.hvs.profiles.activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import cn.redcdn.commonutil.NetConnectHelper;
import cn.redcdn.hvs.R;
import cn.redcdn.hvs.appinstall.InstallCallBackListerner;
import cn.redcdn.hvs.appinstall.MeetingVersionManager;
import cn.redcdn.hvs.base.BaseActivity;
import cn.redcdn.hvs.util.CustomToast;
import cn.redcdn.hvs.util.TitleBar;
import cn.redcdn.log.CustomLog;

/**
 * Created by Administrator on 2017/2/24.
 */



public class AboutActivity extends BaseActivity {


    private RelativeLayout check;
    private TextView txtVersion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about_layout);
        TitleBar titleBar = getTitleBar();
        titleBar.setTitle("关于");
        titleBar.enableBack();
        initWidget();
        initAppVersionName();
    }

    private void initAppVersionName() {
        try {
            PackageManager pm = AboutActivity.this.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(
                    AboutActivity.this.getPackageName(), 0);
            String version = pi.versionName;
            if (version != null && version.length() > 0) {
                txtVersion.setText("V" + version);
            }
        } catch (Exception e) {
            CustomLog.e("AboutActivity", "initAppVersionName Exception");
        }
    }

    private void initWidget() {

        txtVersion = (TextView) findViewById(R.id.app_version);

        check = (RelativeLayout) findViewById(R.id.check_app);

        check.setOnClickListener(mbtnHandleEventListener);




    }

    @Override
    public void todoClick(int i) {
        // TODO Auto-generated method stub
        super.todoClick(i);
        switch (i) {
            case R.id.check_app:
                String list = MeetingVersionManager.getInstance().getChangelist();
                if (list != null && !list.equals("")) {
                    list = "新功能：\r\n" + list;
                } else {
                    list = "发现您有新版本，请及时更新";
                }
                if (NetConnectHelper.getNetWorkType(getApplicationContext()) == -1) {
                    CustomToast.show(AboutActivity.this, "网络不给力，请检查网络！",
                            Toast.LENGTH_LONG);
                } else {
                    if (!MeetingVersionManager.getInstance().isHasInstall(
                            AboutActivity.this)) {


                        InstallCallBackListerner appVersionCheckListener = new InstallCallBackListerner() {

                            @Override
                            public void needForcedInstall() {
                                // TODO Auto-generated method stub
                                AboutActivity.this.removeLoadingView();
                            }

                            @Override
                            public void needOptimizationInstall() {
                                // TODO Auto-generated method stub
                                AboutActivity.this.removeLoadingView();
                                CustomToast.show(AboutActivity.this, "发现新版本，正在下载中", 1);
                            }

                            @Override
                            public void noNeedInstall() {
                                // TODO Auto-generated method stub
                                AboutActivity.this.removeLoadingView();
                                CustomToast.show(AboutActivity.this, "当前版本为最新版本", 1);
                            }

                            @Override
                            public void errorCondition(int error) {
                                // TODO Auto-generated method stub
                                AboutActivity.this.removeLoadingView();
                                CustomToast.show(AboutActivity.this, "当前版本为最新版本"/*String.valueOf(error)*/, 1);
                            }

                        };

                        AboutActivity.this.showLoadingView("正在检测版本", new DialogInterface.OnCancelListener() {

                            @Override
                            public void onCancel(DialogInterface dialog) {
                                dialog.dismiss();
                                MeetingVersionManager.getInstance().cancelCheckVersion();
                                CustomToast.show(AboutActivity.this, "检测版本取消",Toast.LENGTH_LONG);
                            }
                        });

                        MeetingVersionManager.getInstance().checkOrInstall(AboutActivity.this.getApplicationContext(),
                                appVersionCheckListener);

                    } else {

                    }
                }
                break;
            case R.id.app_webview:
                Intent itent = new Intent();
                startActivity(itent);
                break;
        }

    }
}
