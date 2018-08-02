package cn.redcdn.hvs.profiles.activity;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.File;

import cn.redcdn.hvs.MedicalApplication;
import cn.redcdn.hvs.R;
import cn.redcdn.hvs.base.BaseActivity;
import cn.redcdn.hvs.meeting.meetingManage.MedicalMeetingManage;
import cn.redcdn.hvs.profiles.view.SlideSwitch;
import cn.redcdn.hvs.util.CommonUtil;
import cn.redcdn.hvs.util.CustomToast;
import cn.redcdn.hvs.util.TitleBar;
import cn.redcdn.log.CustomLog;

/**
 * Created by Administrator on 2017/2/24.
 */

public class SettingActivity extends BaseActivity {

    private SlideSwitch webContro;

    private SlideSwitch downloadContro;


    private RelativeLayout voice_detect_rl;

    private RelativeLayout setResolutionRl;

    private TextView voice_detect_result;


    private TextView tvMeetingSettingTitle;

    private RelativeLayout rlAutoSpeakSetting;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        initWidget();
        TitleBar titleBar = getTitleBar();
        titleBar.setTitle("设置");
        titleBar.enableBack();
    }

    private void initWidget() {


        voice_detect_result = (TextView) findViewById(R.id.voice_detect_result);
        webContro = (SlideSwitch) findViewById(R.id.set_web);
        downloadContro = (SlideSwitch) findViewById(R.id.set_download);
        voice_detect_rl = (RelativeLayout) findViewById(R.id.goto_voice_detect_rl);
        setResolutionRl = (RelativeLayout) findViewById(R.id.set_resolution_rl);
        voice_detect_rl.setOnClickListener(mbtnHandleEventListener);

        webContro.SetOnChangedListener(new SlideSwitch.OnChangedListener() {
            @Override
            public void OnChanged(boolean checkState) {
                changeWebSettingStats(MedicalApplication.shareInstance().getWebSetting());
            }
        });
        downloadContro.SetOnChangedListener(new SlideSwitch.OnChangedListener() {
            @Override
            public void OnChanged(boolean checkState) {
                changeDownloadSettingStats(MedicalApplication.shareInstance().getDownloadSetting());
            }
        });
        setResolutionRl.setOnClickListener(mbtnHandleEventListener);

        if (MedicalApplication.shareInstance().getWebSetting()) {
            webContro.setChecked(true);
        } else {
            webContro.setChecked(false);
        }
        if (MedicalApplication.shareInstance().getDownloadSetting()) {
            downloadContro.setChecked(true);
        } else {
            downloadContro.setChecked(false);
        }
        SharedPreferences sharedPreferences = getSharedPreferences("VDS", Activity.MODE_PRIVATE);
        String voiceDetectStatus = sharedPreferences.getString("vds", "");
        int hasVoiceDetect = sharedPreferences.getInt("hasVoiceDetect", 0);
        System.out.println("vds: " + voiceDetectStatus + "  hasVoiceDetect: " + hasVoiceDetect);
        if (voiceDetectStatus.equals("") || voiceDetectStatus.equals("未通过")) {
            voice_detect_result.setVisibility(View.VISIBLE);

        } else {
            voice_detect_result.setTextColor(0xFF3CB744);
            voice_detect_result.setText(voiceDetectStatus);
        }
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
    public void todoClick(int i) {
        super.todoClick(i);
        switch (i) {
            case R.id.goto_voice_detect_rl:
                CustomLog.d(TAG, "开启声音检测");
                File mfile = new File(Environment.getExternalStorageDirectory().getPath() + "/meeting/asyRecord.pcm");
                if (mfile.exists()) {
                    CustomLog.d(TAG, "asyRecord.pcm存在");
                    if (mfile.isFile()) {
                        mfile.delete();
                        CustomLog.d(TAG, "asyRecord.pcm删除");
                    }
                } else {
                    CustomLog.d(TAG, "asyRecord.pcm不存在");
                }
                boolean result = CommonUtil.selfPermissionGranted(SettingActivity.this, Manifest.permission.RECORD_AUDIO);
                if (!result) {
                    CustomToast.show(SettingActivity.this,"请开启麦克风权限",CustomToast.LENGTH_SHORT);
                    return;
                }
                Intent in = new Intent();
                in.setClass(SettingActivity.this, VoiceDetectActivity.class);
                startActivity(in);
                SettingActivity.this.finish();
                break;
            case R.id.set_resolution_rl:
                Intent intent = new Intent();
                intent.setClass(SettingActivity.this, SettingResolutionActivity.class);
                startActivity(intent);
                break;
            default:
                break;
        }
    }


    private void changeWebSettingStats(boolean open) {
        if (open) {
            webContro.setChecked(false);
            saveSetting("webSetting", false);
        } else {
            webContro.setChecked(true);
            saveSetting("webSetting", true);
        }
    }

    private void saveSetting(String setting, boolean isOpen) {
        SharedPreferences sharedPreferences = this.getSharedPreferences("setting", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(setting, isOpen);
        editor.commit();
//        CustomToast.show(this,"isopen="+isOpen,7000);
        MedicalMeetingManage.getInstance().setIsAllowNetJoinMeeting(isOpen);
    }

    private void changeDownloadSettingStats(boolean open) {
        if (open) {
            downloadContro.setChecked(false);
            saveSetting("downloadSetting", false);
        } else {
            downloadContro.setChecked(true);
            saveSetting("downloadSetting", true);
        }
    }
}
