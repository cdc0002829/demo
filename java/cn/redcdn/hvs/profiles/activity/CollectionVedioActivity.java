package cn.redcdn.hvs.profiles.activity;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.ImageLoadingListener;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.Date;

import cn.redcdn.datacenter.collectcenter.DataBodyInfo;
import cn.redcdn.datacenter.collectcenter.DeleteCollectItems;
import cn.redcdn.hvs.AccountManager;
import cn.redcdn.hvs.MedicalApplication;
import cn.redcdn.hvs.R;
import cn.redcdn.hvs.base.BaseActivity;
import cn.redcdn.hvs.im.collection.CollectionFileManager;
import cn.redcdn.hvs.im.manager.CollectionManager;
import cn.redcdn.hvs.profiles.listener.MyDisplayImageListener;
import cn.redcdn.hvs.util.CustomToast;
import cn.redcdn.hvs.util.TitleBar;
import cn.redcdn.log.CustomLog;

/**
 * Created by Administrator on 2017/3/7.
 */
public class CollectionVedioActivity extends BaseActivity {
    public static final String COLLECTION_VEDIO_DATA = "collection_vedio_data";

    private DataBodyInfo bean;
    // 视频播放控件
    private VideoView videoView;
    // 播放进度条
    private ProgressBar videoProgressBar;

    private TitleBar titlebar;
    private TextView savaTv;
    private ImageView videoIcon;
    private TextView videoName;
    private TextView videoTime;

    ImageLoadingListener mDisplayImageListener;
//    private ProgressDialog progress;
    private String remoteUrl;
    private String split1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_collection_vedio);
        initView();
        initData();
        mDisplayImageListener = new MyDisplayImageListener();
    }

    private void initView() {
        videoView = (VideoView) findViewById(R.id.video_view_collecion);
        videoIcon = (ImageView) findViewById(R.id.collection_video_icon);
        videoName = (TextView) findViewById(R.id.collection_video_name);
        videoTime = (TextView) findViewById(R.id.collection_video_time);
        MediaController mc = new MediaController(this);//Video是我类名，是你当前的类
        videoView.setMediaController(mc);//设置VedioView与MediaController相关联
        titlebar = getTitleBar();
        getTitleBar().enableBack();
        getTitleBar().setTitle(R.string.scan_vedio);
        getTitleBar().enableRightBtn("", R.drawable.meeting_title_more, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog();
            }
        });

    }

    private void setNameAndIcon() {
        if (bean.getForwarderName() != null) {
            videoName.setText(bean.getForwarderName());
        }
        if (bean.getCollecTime() != null) {
            String collecTime = bean.getCollecTime() + "000";
            long l = Long.parseLong(collecTime);
            Date d = new Date(l);
            SimpleDateFormat format = new SimpleDateFormat("yyyy年MM月dd日");
            videoTime.setText(format.format(d));
        }
        ImageLoader imageLoader = ImageLoader.getInstance();
        imageLoader.displayImage(bean.getForwarderHeaderUrl(),
                videoIcon,
                MedicalApplication.shareInstance().options,
                mDisplayImageListener);
    }

    private void initData() {
        Intent i = getIntent();
        bean = (DataBodyInfo) i.getSerializableExtra(COLLECTION_VEDIO_DATA);
        remoteUrl = bean.getRemoteUrl();
        String[] split = remoteUrl.split("\\.");
        split1 = split[split.length - 1];

        if (!remoteUrl.equals(null)) {
            Uri uri = Uri.parse(remoteUrl);
            videoView.setVideoURI(uri);
            CollectionVedioActivity.this.showLoadingView("加载数据中", new DialogInterface.OnCancelListener() {

                @Override
                public void onCancel(DialogInterface dialog) {
                    dialog.dismiss();
                    CollectionVedioActivity.this.finish();
                    videoView = null;
                    CustomToast.show(getApplicationContext(), "取消加载", Toast.LENGTH_SHORT);
                }
            });
            videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    removeLoadingView();
                    videoView.start();
                }
            });
            videoView
                    .setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                        @Override
                        public void onCompletion(MediaPlayer arg0) {
                            CustomLog.d("TAG", "播放完毕，退出播放界面");
//                            finish();
                        }
                    });
            videoView.requestFocus();
        }
        setNameAndIcon();
    }

    @Override
    protected void onPause() {
        super.onPause();
        finish();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        videoView = null;
    }

    private View inflate;
    private TextView zhuanfa;
    private TextView cacleZhuanfa;
    private TextView deleteZhuanfa;
    private Dialog dialog;

    private void showDialog() {
        dialog = new Dialog(this, R.style.ActionSheetDialogStyle);
        //填充对话框的布局
        inflate = LayoutInflater.from(this).inflate(R.layout.zhuanfa_dialog, null);
        //初始化控件
        zhuanfa = (TextView) inflate.findViewById(R.id.zhuanfa_tv);
        deleteZhuanfa = (TextView) inflate.findViewById(R.id.delete_zhuanfa_tv);
        cacleZhuanfa = (TextView) inflate.findViewById(R.id.cancle_zhuanfa_tv);
        savaTv = (TextView) inflate.findViewById(R.id.save_tv);
        savaTv.setOnClickListener(mbtnHandleEventListener);
        zhuanfa.setOnClickListener(mbtnHandleEventListener);
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
            case R.id.zhuanfa_tv:
                dialog.dismiss();
                CollectionFileManager.getInstance().onCollectMsgForward(
                        CollectionVedioActivity.this, bean);
                break;
            case R.id.delete_zhuanfa_tv:

                DeleteCollectItems deleteCollectItems = new DeleteCollectItems() {
                    @Override
                    protected void onSuccess(JSONObject responseContent) {
                        super.onSuccess(responseContent);
                        CustomToast.show(getApplicationContext(), "删除收藏成功", 5000);
                        CollectionManager.getInstance().deleteCollectionById(bean.getCollectionId());
                        CollectionVedioActivity.this.finish();
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
            case R.id.save_tv:
//                progress = new ProgressDialog(this);
//                progress.setMessage("下载视频中");
//                progress.setCancelable(false);
                new LoadVideo().execute(bean.getRemoteUrl());
                dialog.dismiss();
                break;
        }
    }

    public static void scanIntoMediaStore(Context context, String filePath) {
        Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        intent.setData(Uri.fromFile(new File(filePath)));
        context.sendBroadcast(intent);
    }

    public class LoadVideo extends AsyncTask<String, Integer, Void> {
        private String s;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            CustomToast.show(CollectionVedioActivity.this,"视频下载中",CustomToast.LENGTH_SHORT);
//            progress.show();
        }

        @Override
        protected Void doInBackground(String... params) {
            int count;
            for (int i = 0; i < params.length; i++) {
                try {
                    URL url = new URL(params[i]);
                    URLConnection conection = url.openConnection();
                    conection.connect();
                    int lenghtOfFile = conection.getContentLength();

                    // download the file
                    InputStream input = new BufferedInputStream(
                            url.openStream(), 8192);// 1024*8
                    File f = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/DCIM/Camera"
                    );
                    if (f.isDirectory()) {
//                        System.out.println("exist!");
                    } else {
//                        System.out.println("not exist!");
                        f.mkdirs();
                    }
                    // Output stream
                    OutputStream output = new FileOutputStream(Environment
                            .getExternalStorageDirectory().getAbsolutePath() + "/DCIM/Camera"+"/"+bean.getForwarderName()+bean.getCollecTime()+"."+split1);

                    byte data[] = new byte[1024];
                    while ((count = input.read(data)) != -1) {
                        // writing data to file
                        output.write(data, 0, count);
                    }

                    // flushing output
                    output.flush();

                    // closing streams
                    output.close();
                    input.close();

                } catch (Exception e) {
                    Log.e("Error: ", e.getMessage());
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            CustomToast.show(CollectionVedioActivity.this,"视频保存成功",CustomToast.LENGTH_LONG);
//            progress.dismiss();
            scanIntoMediaStore(CollectionVedioActivity.this,Environment
                    .getExternalStorageDirectory().getAbsolutePath() + "/DCIM/Camera"+"/"+bean.getForwarderName()+bean.getCollecTime()+"."+split1);
        }
    }

}
