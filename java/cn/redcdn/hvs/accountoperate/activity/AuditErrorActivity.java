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


public class AuditErrorActivity extends cn.redcdn.hvs.base.BaseActivity {
    private Button backbtn;
    private Button nextbtn;
    private TextView Review_NUM;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audit_error);
        backbtn = (Button) findViewById(R.id.back_btn);
        nextbtn = (Button) findViewById(R.id.auditerrorBtn2);
        backbtn.setOnClickListener(mbtnHandleEventListener);
       nextbtn.setOnClickListener(mbtnHandleEventListener);
        Review_NUM = (TextView) findViewById(R.id.audit_REVIEW_NUM);
        Review_NUM.setOnClickListener(mbtnHandleEventListener);
        Review_NUM.setText("审核专线："+SettingData.getInstance().REVIEW_NUM);
        TitleBar titleBar = getTitleBar();
        titleBar.setTitle("认证审核");
        titleBar.setTitleTextColor(Color.BLACK);
        titleBar.enableBack();
    }



    @Override
    public void todoClick(int i) {
        super.todoClick(i);
        switch (i) {
            case R.id.audit_REVIEW_NUM:
                if (!SettingData.getInstance().REVIEW_NUM.isEmpty()) {

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if(AuditErrorActivity.this.checkSelfPermission(Manifest.permission.CALL_PHONE)== PackageManager.PERMISSION_GRANTED) {
                            Intent i1 = new Intent(Intent.ACTION_CALL, Uri.parse("tel:"
                                    + SettingData.getInstance().REVIEW_NUM));
                            startActivity(i1);
                        }else{
                            //
                        }
                    }else{
                        Intent i1 = new Intent(Intent.ACTION_CALL, Uri.parse("tel:"
                                + SettingData.getInstance().REVIEW_NUM));
                        startActivity(i1);
                    }

                }
                break;
            case R.id.back_btn:
                AuditErrorActivity.this.finish();
                break;
            case R.id.auditerrorBtn2:
                Intent intentbut = new Intent(AuditErrorActivity.this, CardTypeActivity.class);
                startActivity(intentbut);
                break;
            default:
                break;
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        AuditErrorActivity.this.finish();
    }
}
