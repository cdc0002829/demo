package cn.redcdn.hvs.accountoperate.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import cn.redcdn.hvs.R;
import cn.redcdn.hvs.config.SettingData;
import cn.redcdn.hvs.util.TitleBar;

public class AuditingActivity extends cn.redcdn.hvs.base.BaseActivity{
    private TextView Review_NUM;
    private Button btn_back;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auditing);
        init();

    }

    private void init(){
        TitleBar titleBar = getTitleBar();
        titleBar.setTitle("认证审核");
        titleBar.setTitleTextColor(Color.BLACK);
        titleBar.enableBack();
        Review_NUM= (TextView) findViewById(R.id.auding_tv);
        Review_NUM.setOnClickListener(mbtnHandleEventListener);
        Review_NUM.setText("审核专线："+SettingData.getInstance().REVIEW_NUM);
        btn_back= (Button) findViewById(R.id.back_btn);
        btn_back.setOnClickListener(mbtnHandleEventListener);
    }


    @Override
    public void todoClick(int i) {
        switch (i) {
            case R.id.auding_tv:
                if (!SettingData.getInstance().REVIEW_NUM.isEmpty()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if(AuditingActivity.this.checkSelfPermission(Manifest.permission.CALL_PHONE)== PackageManager.PERMISSION_GRANTED) {
                            Intent i1 = new Intent(Intent.ACTION_CALL, Uri.parse("tel:"
                                    + SettingData.getInstance().REVIEW_NUM));
                            startActivity(i1);
                        }else{
                        }
                    }else{
                        Intent i1 = new Intent(Intent.ACTION_CALL, Uri.parse("tel:"
                                + SettingData.getInstance().REVIEW_NUM));
                        startActivity(i1);
                    }

                }
                break;
            case R.id.back_btn:
                Intent intent=new Intent(AuditingActivity.this,LoginActivity.class);
                startActivity(intent);
                AuditingActivity.this.finish();
                break;
            default:
                break;
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(AuditingActivity.this, LoginActivity.class);
        startActivity(intent);
        AuditingActivity.this.finish();
    }
}
