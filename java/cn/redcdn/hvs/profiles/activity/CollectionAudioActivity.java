package cn.redcdn.hvs.profiles.activity;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.nostra13.universalimageloader.core.ImageLoader;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;

import cn.redcdn.datacenter.collectcenter.DataBodyInfo;
import cn.redcdn.datacenter.collectcenter.DeleteCollectItems;
import cn.redcdn.hvs.AccountManager;
import cn.redcdn.hvs.MedicalApplication;
import cn.redcdn.hvs.R;
import cn.redcdn.hvs.base.BaseActivity;
import cn.redcdn.hvs.im.manager.CollectionManager;
import cn.redcdn.hvs.profiles.collection.Player;
import cn.redcdn.hvs.profiles.listener.DisplayImageListener;
import cn.redcdn.hvs.util.CustomToast;

/**
 * Created by Administrator on 2017/3/7.
 */
public class CollectionAudioActivity extends BaseActivity {
    boolean startState = false;
    boolean isPause = false;
    public static final String COLLECTION_AUDIO_DATA = "collection_audio_data";
    private DataBodyInfo bean;
    private CheckBox playPauseCb;
    private SeekBar seekBar;
    private TextView pastTv;
    private TextView allTime;
    private Player player;
    private CompoundButton.OnCheckedChangeListener ocl = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (isChecked) {

                player.play();

            } else {

                player.pause();
            }
        }
    };
    private TextView createName;
    private ImageView icon;
    private String forwarderHeaderUrl;
    private DisplayImageListener mDisplayImageListener;
    private TextView time;
    private String collecTimeOne;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_collection_audio);
        mDisplayImageListener = new DisplayImageListener();
        initData();
        initView();
        player.playUrl(bean.getRemoteUrl());
        CollectionAudioActivity.this.showLoadingView("加载数据中", new DialogInterface.OnCancelListener() {

            @Override
            public void onCancel(DialogInterface dialog) {

                dialog.dismiss();
                CollectionAudioActivity.this.finish();
                player.release();
                CustomToast.show(getApplicationContext(), "取消加载", Toast.LENGTH_SHORT);
            }
        });
        getTitleBar().enableBack();
        getTitleBar().setTitle(R.string.collection_xiangqing);

        player.mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                        playPauseCb.setChecked(false);
                seekBar.setProgress(0);
            }
        });
        player.mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                removeLoadingView();
            }
        });
        getTitleBar().enableRightBtn("", R.drawable.meeting_title_more, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog();
            }
        });

    }


    private void initView() {
        playPauseCb = (CheckBox) findViewById(R.id.playPauseCb);
        playPauseCb.setOnCheckedChangeListener(ocl);
        seekBar = (SeekBar) findViewById(R.id.seekBar);
        seekBar.setOnSeekBarChangeListener(seekBarChangeListener);
//        seekBar.setMax(bean.getDuration());
        pastTv = (TextView) findViewById(R.id.pastTv);
        allTime = (TextView) findViewById(R.id.all_time_tv);
        String showAllTime="";
        int mDuration = bean.getDuration();
        if (mDuration<10){
            showAllTime="0"+mDuration;
        }else {
            showAllTime=""+mDuration;
        }
        allTime.setText(showAllTime);
        player = new Player(seekBar);
        createName = (TextView) findViewById(R.id.collection_text_name);
        String forwarderName = bean.getForwarderName();
        if (!forwarderName.equals("")) {
            createName.setText(forwarderName);
        }
        icon = (ImageView) findViewById(R.id.collection_text_icon);
        forwarderHeaderUrl = bean.getForwarderHeaderUrl();
        if (!forwarderName.equals("")) {
            ImageLoader imageLoader = ImageLoader.getInstance();
            imageLoader.displayImage(forwarderHeaderUrl, icon,
                    MedicalApplication.shareInstance().options, mDisplayImageListener);
        }

        time = (TextView) findViewById(R.id.collection_text_time);
        collecTimeOne = bean.getCollecTime();
        if (!collecTimeOne.equals("")) {
            String collecTime = collecTimeOne + "000";
            long l = Long.parseLong(collecTime);
            Date d = new Date(l);
            SimpleDateFormat format = new SimpleDateFormat("yyyy年MM月dd日");
            time.setText("收藏于" + format.format(d));
        }
    }


    private SeekBar.OnSeekBarChangeListener seekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {


        int progress;

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress,
                                      boolean fromUser) {
            pastTv.setText(formatTime(progress * bean.getDuration() / 100));
            // 原本是(progress/seekBar.getMax())*player.mediaPlayer.getDuration()
            this.progress = progress * player.mediaPlayer.getDuration()
                    / seekBar.getMax();

        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            // seekTo()的参数是相对与影片时间的数字，而不是与seekBar.getMax()相对的数字
            player.mediaPlayer.seekTo(progress);
        }


    };

    private void initData() {
        Intent i = getIntent();
        bean = (DataBodyInfo) i.getSerializableExtra(COLLECTION_AUDIO_DATA);
    }


    private static String formatTime(long time) {
        return String.format("%02d", time % 60);
    }


    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        player.stop();//停止音频的播放

    }

    private View inflate;
    private TextView cacleZhuanfa;
    private TextView deleteZhuanfa;
    private Dialog dialog;

    private void showDialog() {
        dialog = new Dialog(this, R.style.ActionSheetDialogStyle);
        //填充对话框的布局
        inflate = LayoutInflater.from(this).inflate(R.layout.zhuanfa_dialog2, null);
        //初始化控件
        deleteZhuanfa = (TextView) inflate.findViewById(R.id.delete_zhuanfa_tv);
        cacleZhuanfa = (TextView) inflate.findViewById(R.id.cancle_zhuanfa_tv);
        deleteZhuanfa.setOnClickListener(mbtnHandleEventListener);
        cacleZhuanfa.setOnClickListener(mbtnHandleEventListener);
        //将布局设置给Dialog
        dialog.setContentView(inflate);


        //获取当前Activity所在的窗体
        Window dialogWindow = dialog.getWindow();

        dialogWindow.setGravity(Gravity.BOTTOM); //可设置dialog的位置
        dialogWindow.getDecorView().setPadding(0, 0, 0, 0); //消除边距
        //设置Dialog从窗体底部弹出
        dialogWindow.setGravity(Gravity.BOTTOM);
        //获得窗体的属性
        WindowManager.LayoutParams lp = dialogWindow.getAttributes();
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;   //设置宽度充满屏幕
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        lp.y = 20;//设置Dialog距离底部的距离
//       将属性设置给窗体
        dialogWindow.setAttributes(lp);
        dialog.show();//显示对话框
    }

    @Override
    public void todoClick(int i) {
        super.todoClick(i);
        switch (i) {
            case R.id.delete_zhuanfa_tv:

                DeleteCollectItems deleteCollectItems = new DeleteCollectItems() {
                    @Override
                    protected void onSuccess(JSONObject responseContent) {
                        super.onSuccess(responseContent);
                        CustomToast.show(getApplicationContext(), "删除收藏成功", 5000);
                        CollectionManager.getInstance().deleteCollectionById(bean.getCollectionId());
                        CollectionAudioActivity.this.finish();
                    }

                    @Override
                    protected void onFail(int statusCode, String statusInfo) {
                        super.onFail(statusCode, statusInfo);
                        CustomToast.show(getApplicationContext(), "删除收藏失败", 5000);
                    }
                };
                String id = bean.getCollectionId();
                String nube = AccountManager.getInstance(this)
                        .getAccountInfo().getNube();
                String accessToken = AccountManager.getInstance(this)
                        .getAccountInfo().getAccessToken();
                deleteCollectItems.deleteCollectionItems(nube, id, accessToken);
                dialog.dismiss();
                break;
            case R.id.cancle_zhuanfa_tv:
                dialog.dismiss();
                break;

        }
    }
}
