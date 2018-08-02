package cn.redcdn.hvs.officialaccounts.activity;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.media.AudioManager;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.jcodecraeer.xrecyclerview.ProgressStyle;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.display.RoundedBitmapDisplayer;

import java.util.Timer;
import java.util.TimerTask;

import cn.redcdn.datacenter.medicalcenter.MDSErrorCode;
import cn.redcdn.datacenter.offaccscenter.MDSAppGetArticleInfo;
import cn.redcdn.datacenter.offaccscenter.MDSAppGetVideoInfo;
import cn.redcdn.datacenter.offaccscenter.data.PlayerInfo;
import cn.redcdn.datacenter.offaccscenter.data.VideoInfo;
import cn.redcdn.hvs.AccountManager;
import cn.redcdn.hvs.MedicalApplication;
import cn.redcdn.hvs.R;
import cn.redcdn.hvs.base.BaseActivity;
import cn.redcdn.hvs.officialaccounts.jsinterface.JSLocalObj;
import cn.redcdn.hvs.officialaccounts.listener.DingyueDisplayImageListener;
import cn.redcdn.hvs.officialaccounts.widget.ManagerApplication;
import cn.redcdn.hvs.player.PlayManager;
import cn.redcdn.hvs.util.CustomToast;
import cn.redcdn.log.CustomLog;

import static cn.redcdn.datacenter.medicalcenter.MDSErrorCode.MDS_TOKEN_DISABLE;
import static cn.redcdn.hvs.R.style.dialog;

public class VideoPublishActivity extends BaseActivity implements View.OnClickListener, PlayManager.PlayerListener {

    private final static String TAG = VideoPublishActivity.class.getName();
    public final static String INTENT_DATA_ARTICLE_ID = "INTENT_DATA_ARTICLE_ID";
    private View mProcessBarContainer;
    private ImageView mProcessBar;
    private TextView mProcessTV;

    private WebView mWebView;

    private PlayManager mPlayManager;

    private MDSAppGetArticleInfo getArticleInfo;
    private MDSAppGetVideoInfo mGetVideoInfo;

    private int mPlayPreviewType = -1; //当前播放类型
    private final static int PLAY_PREVIEW_POSTER = 1; //播放类型为海报
    private final static int PLAY_PREVIEW_VIDEO = 2; //播放类型为视频

    private int mPlaySrcType = -1; //片源类型
    private final static int PLAY_SRC_VOD = 1; //点播
    private final static int PLAY_SRC_LIVE = 2; //直播

    private int srcWidht = 16;
    private int srcHeight = 9;

    private String articleId; //文章ID：根据文章ID获取视频ID、视频名称、H5地址、是否加密、视频类型
    private String contentId; //视频ID：根据视频ID获取播放地址、海报地址、预播类型（直播、点播）
    private String mPlaySrcUrl; //播放地址
    private String mPosterUrl = ""; //海报地址
    private String webviewUrl = ""; //H5 页面地址
    private int accessType; //1.不加密 2.加密
    private String accessPassword; //加密密码

    private Timer mRequestVideoInfoTimer;
    private TimerTask mRequestVideoInfoTasker;
    private final static int TIME_INTERVAL = 3000; //对于直播3s轮询一次获取当前直播状态

    private int totalDuration; //节目总时长，点播片源才有该值
    private int currentPos; //当前播放进度

    private DisplayImageOptions options;
    private LinearLayout netRequestionLayout = null;
    private Button refreshBtn = null;
    private TextView refreshText;
    private ImageView refreshImage;

    private String titleNameString;
    private RelativeLayout mPosterContainer; //海报容器
    private ImageView posterLogo; //海报
    private Button posterPlayBtn; //海报上的播放按钮

    private RelativeLayout mVideoLoadingContainer; // 视频加载中容器

    private TextView m_LoadText; // 加载文字显示控件
    private TextView m_ErrorText; // 错误信息显示

    private View m_ControlPanel; // 进度条View:包含暂停/播放、播放进度、播放时间、总时间、全屏/小屏
    private Button playContrButton;
    private Button fullScreenButton;

    private View mPauseView;  //播放刷新组件
    private ImageView mPause_log; //播放
    private TextView mTv_delate;//视频文件已经删除
    private ImageView mReplay_log; //刷新按钮

    private SeekBar m_SeekBar; // 进度条控件
    private TextView m_StartTimeText; // 进度条开始时间文本控件
    private TextView m_EndTimeText; // 进度条结束时间文本控件
    private TextView m_Division;  // 分割符

    private RelativeLayout titleLayout;
    private Button backButton;

    private AudioManager mAudioManager; // 声音控制对象
    private DisplayMetrics screenDm;

    private FrameLayout m_mediaViewFrameLayout;

    private SurfaceView m_VideoWindow; // 视频渲染surface
    private SurfaceHolder m_SurfaceHolder;

    private FrameLayout m_VideoFrameLayout;
    private FrameLayout.LayoutParams m_VideoLayoutParams;

    private DingyueDisplayImageListener mMainDisplayimageListener = null;

    private ImageView webLoading;
    private TextView mDelate;

    private LinearLayout mediaWeb;

    private LinearLayout delate;
    private TextView article_delate;

    private Button btn_back;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_videopublic_playvideo);
        ManagerApplication.addDestoryActivity(VideoPublishActivity.this, "videoPublishiActivity");
        articleId = getIntent().getStringExtra(INTENT_DATA_ARTICLE_ID);
        CustomLog.d(TAG, "onCreate articleId:" + articleId);
        mMainDisplayimageListener = new DingyueDisplayImageListener();
        initUI();//初始化布局
        initWebView();//初始化webview
        requestArticleInfo();//请求内容详情页接口
    }

    /**
     * 请求内容详情页面
     */
    private void requestArticleInfo() {
        showLoadingView("加载中", new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                if (getArticleInfo != null) {
                    getArticleInfo.cancel();
                    VideoPublishActivity.this.finish();
                }
            }
        });
        getArticleInfo = new MDSAppGetArticleInfo() {

            @Override
            protected void onSuccess(VideoInfo responseContent) {
                super.onSuccess(responseContent);
                removeLoadingView();
                contentId = responseContent.getChannelId();
                CustomLog.e(TAG, "contentId" + contentId);
                //片源类型  直播 ,点播
                mPlaySrcType = responseContent.getContentType() == 1 ? PLAY_SRC_VOD : PLAY_SRC_LIVE;
                CustomLog.e(TAG, "mPlaySrcType" + mPlaySrcType);
                webviewUrl = responseContent.getWebViewUrl();
                CustomLog.e(TAG, "webviewUrl" + webviewUrl);
                accessType = responseContent.getAccessType();//加密类型
                accessPassword = responseContent.getAccessPassword();//加密密码
                CustomLog.d(TAG, "accessPassword" + accessPassword);
                mWebView.loadUrl(webviewUrl);
                mWebView.setWebViewClient(new WebViewClient() {
                    /*
                     *这个方法防止点击webview超链接
                     * */
                    @Override
                    public boolean shouldOverrideUrlLoading(WebView view, String url) {
                        return true;
                    }

                    @Override
                    public void onPageFinished(WebView view, String url) {
                        //加载完成
                        hideWebProcessBar();
                    }

                    @Override
                    public void onPageStarted(WebView view, String url, Bitmap favicon) {
                        showWebProcessBar();
                    }

                });
                handlePasswdDialog();
                requestVideoInfo();
            }

            @Override
            protected void onFail(int statusCode, String statusInfo) {
                super.onFail(statusCode, statusInfo);
                removeLoadingView();
                CustomLog.e(TAG, "onFail" + "statusCode:" + statusCode + " statusInfo:" + statusInfo);
                if (statusCode == MDS_TOKEN_DISABLE) {
                    AccountManager.getInstance(MedicalApplication.getContext()).tokenAuthFail(statusCode);
                } else if (statusCode == MDSErrorCode.MDS_EXTERNAL_REWUEDT_UNUAUAL) {
                    //CustomToast.show(MedicalApplication.getContext(), "文章已删除", Toast.LENGTH_LONG);
                    mediaWeb.setVisibility(View.INVISIBLE);
                    delate.setVisibility(View.VISIBLE);
                    article_delate.setText("文章已删除");
                } else {
                    CustomToast.show(MedicalApplication.getContext(), statusInfo, Toast.LENGTH_LONG);
                    finish();
                }
            }
        };

        getArticleInfo.appGetArticleInfo(AccountManager.getInstance(MedicalApplication.context).getToken(), articleId);
    }

    /**
     * 获取视频信息
     */
    private void requestVideoInfo() {
        if (mGetVideoInfo != null) {
            CustomLog.d(TAG, "requestVideoInfo() 获取视频信息过程中，取消获取");
            mGetVideoInfo.cancel();
        }
        mGetVideoInfo = new MDSAppGetVideoInfo() {
            @Override
            protected void onSuccess(PlayerInfo responseContent) {
                super.onSuccess(responseContent);

                //视频地址
                String playSrcUrl = responseContent.getUrl() == null ? "" : responseContent.getUrl();
                CustomLog.e(TAG, "playSrcUrl" + playSrcUrl);
                //海报地址
                String posterUrl = responseContent.getPoster() == null ? "" : responseContent.getPoster();
                CustomLog.e(TAG, "posterUrl" + posterUrl);
                int previewType = responseContent.getPreviewType() == 2 ? PLAY_PREVIEW_VIDEO : PLAY_PREVIEW_POSTER;
                CustomLog.d(TAG, " GetVideoInfo() previewType: " + previewType + " | playSrcUrl: " + playSrcUrl + " |posterUrl: " + posterUrl);
                refreshPlayerUI(previewType, posterUrl, playSrcUrl);
                posterPlayBtn.setBackgroundResource(R.drawable.video_play);
                posterPlayBtn.setEnabled(true);
                mGetVideoInfo = null;
                findViewById(R.id.poster_articel_del_error).setVisibility(View.GONE);
            }

            @Override
            protected void onFail(int statusCode, String statusInfo) {
                super.onFail(statusCode, statusInfo);
                CustomLog.d(TAG, "requestVideoInfo onFail()" + " code: " + statusCode + " |info:" + statusInfo);
                //TODO 解析可能的错误码，比如内容被删除
                if (statusCode == MDSErrorCode.MDS_EXTERNAL_REWUEDT_UNUAUAL) {

                    //内容被删除
                    ((TextView) findViewById(R.id.poster_articel_del_error)).setText("视频已删除");
                    stopRequestVideoInfoTimer();
                    if (mPlayManager.getPlayState() != PlayManager.PLAY_STATE.NULL) { //正在播放视频，
                        stopPlay();
                    }
                    findViewById(R.id.poster_articel_del_error).setVisibility(View.VISIBLE);
                    posterPlayBtn.setVisibility(View.GONE);
                    posterLogo.setVisibility(View.GONE);
                    mPlaySrcType = -1;
                    mPlaySrcUrl = "";
                    mPosterUrl = "";

                } else if (statusCode == MDS_TOKEN_DISABLE) {
                    AccountManager.getInstance(MedicalApplication.getContext()).tokenAuthFail(statusCode);
                }else {
                    CustomToast.show(MedicalApplication.getContext(),"网络不给力,请稍后重试",Toast.LENGTH_SHORT);
                }
                mGetVideoInfo = null;
            }
        };

        mGetVideoInfo.appGetVideoInfo(AccountManager.getInstance(MedicalApplication.context).getToken(), contentId, mPlaySrcType);
    }

    private void startRequestVideoInfoTimer() {
        stopRequestVideoInfoTimer();
        mRequestVideoInfoTimer = new Timer();
        mRequestVideoInfoTasker = new TimerTask() {
            @Override
            public void run() {
                if (mGetVideoInfo == null) {
                    CustomLog.d(TAG, "startRequestVideoInfoTimer run");
                    requestVideoInfo();
                }
            }
        };

        mRequestVideoInfoTimer.schedule(mRequestVideoInfoTasker, TIME_INTERVAL, TIME_INTERVAL);
    }

    private void stopRequestVideoInfoTimer() {
        CustomLog.d(TAG, "stopRequestVideoInfoTimer()");
        if (mRequestVideoInfoTimer != null) {
            mRequestVideoInfoTasker.cancel();
            mRequestVideoInfoTimer.cancel();
            mRequestVideoInfoTasker = null;
            mRequestVideoInfoTimer = null;
        }
    }

    //TODO 对于有密码的文章如果之前没有输入过密码，则显示密码弹框
    private void handlePasswdDialog() {
        //当加密类型为不加密,直接跳转到VideoPublishActivity
        SharedPreferences preferences = getSharedPreferences("data", MODE_APPEND);
        SharedPreferences.Editor editor = getSharedPreferences("data", MODE_APPEND).edit();
        //新拼接的字符串 nube+文章id
        String newKey = AccountManager.getInstance(MedicalApplication.context).getNube() + "_" + articleId;
        CustomLog.e(TAG, "newPass" + newKey);
        String newPassWord = preferences.getString(newKey, "");

        /**
         * 当加密类型==1 或者是sp取出的密码和获取到的密码相等的话
         * */
        if (accessType == 1 || accessPassword.equalsIgnoreCase(newPassWord)) {

        } else {
            editor.remove(newKey);//如果密码有做修改,删除对应项是调用editor.remove(articleId),
            Intent intentDialigActivity = new Intent(VideoPublishActivity.this, DialogActivity.class);
            intentDialigActivity.putExtra(INTENT_DATA_ARTICLE_ID, articleId);
            intentDialigActivity.putExtra("accessPassword", accessPassword);
            CustomLog.d(TAG, "accessPassword" + accessPassword);
            startActivity(intentDialigActivity);
        }
    }

    private void initUI() {
        article_delate = (TextView) findViewById(R.id.article_delate);
        btn_back = (Button) findViewById(R.id.btn_back);
        btn_back.setOnClickListener(this);
        mWebView = (WebView) findViewById(R.id.webview);
        mediaWeb = (LinearLayout) findViewById(R.id.mediaWeb);
        delate = (LinearLayout) findViewById(R.id.delate);
        webLoading = (ImageView) findViewById(R.id.webLoading);
        // 视频加载的进度的圈圈
        mProcessBarContainer = findViewById(R.id.video_loading_dialog);
        mProcessBar = (ImageView) findViewById(R.id.iv_loading);
        mProcessTV = (TextView) findViewById(R.id.tv_wait_bar);
        // 刚开始的时候隐藏加载的进度圈
        netRequestionLayout = (LinearLayout) findViewById(R.id.live_room_playvideo_netquestion_layout);
        netRequestionLayout.setVisibility(View.GONE);


        mPosterContainer = (RelativeLayout) findViewById(R.id.live_room_poster);//海报容器
        posterLogo = (ImageView) mPosterContainer.findViewById(R.id.poster_logo);
        posterPlayBtn = (Button) mPosterContainer.findViewById(R.id.poster_play_btn);

        posterPlayBtn.setOnClickListener(mbtnHandleEventListener);

        m_LoadText = (TextView) findViewById(R.id.loadText);
        m_ErrorText = (TextView) findViewById(R.id.errorText);
        m_ControlPanel = (View) findViewById(R.id.seekBar_view);//进度条view

        mPauseView = (View) findViewById(R.id.pause_view);//播放刷新view
        mPause_log = (ImageView) findViewById(R.id.pause_logo);

        mTv_delate = (TextView) findViewById(R.id.tv_videoDelate);
        mReplay_log = (ImageView) findViewById(R.id.replay_logo);


        m_StartTimeText = (TextView) findViewById(R.id.init_time);
        m_Division = (TextView) findViewById(R.id.tv_slash);
        m_EndTimeText = (TextView) findViewById(R.id.finish_time);

        m_SeekBar = (SeekBar) findViewById(R.id.seek);

        m_SeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(
                    android.widget.SeekBar seekBar, int progress,
                    boolean fromUser) {
                if (fromUser) {
                    m_StartTimeText.setText(PlayManager.getFormatTime(progress));
                }
            }

            @Override
            public void onStartTrackingTouch(
                    android.widget.SeekBar seekBar) {
                CustomLog.d(TAG, "onStartTrackingTouch");
                mPlayManager.markRequestState(mPlayManager.getPlayState());
                pausePlay();
            }

            @Override
            public void onStopTrackingTouch(
                    android.widget.SeekBar seekBar) {

                CustomLog.d(TAG, "m_SeekBar onStopTrackingTouch " + " pos = "
                        + seekBar.getProgress() + " max = "
                        + seekBar.getMax());
                int pos = seekBar.getProgress();

                mPlayManager.seekTo(pos);

                m_StartTimeText.setText(PlayManager.getFormatTime(pos));
                showProcessBar();
            }
        });

        playContrButton = (Button) findViewById(R.id.control_bt);
        fullScreenButton = (Button) findViewById(R.id.fullscreen_bt);

        playContrButton.setOnClickListener(mbtnHandleEventListener);
        fullScreenButton.setOnClickListener(mbtnHandleEventListener);

        backButton = (Button) findViewById(R.id.live_room_player_back);
        backButton.setOnClickListener(this);
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        screenDm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(screenDm);
        CustomLog.i(TAG, "DisplayMetrics:" + screenDm.toString());

        hideProcessBar();

        // 设置播放器大小
        m_mediaViewFrameLayout = (FrameLayout) findViewById(R.id.mediaView);
        m_mediaViewFrameLayout.getLayoutParams().width = screenDm.widthPixels;
        m_mediaViewFrameLayout.getLayoutParams().height = screenDm.widthPixels * 9 / 16;

        m_VideoFrameLayout = (FrameLayout) findViewById(R.id.VideoFrame1);
        m_VideoLayoutParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT, Gravity.CENTER);

        m_VideoWindow = new SurfaceView(this);
        m_VideoWindow.setLayoutParams(m_VideoLayoutParams);

        m_VideoFrameLayout.addView(m_VideoWindow, m_VideoLayoutParams);
        m_VideoWindow.setVisibility(View.GONE);
        m_VideoFrameLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (m_ControlPanel.getVisibility() == View.GONE) {
                    showControlPanel();
                } else {
                    hideControlPanel();
                }
            }
        });

        m_SurfaceHolder = m_VideoWindow.getHolder();

        //播放窗口上的叠加view，用于点击后控制显示、隐藏控制面板
        findViewById(R.id.info_view).setOnClickListener(mbtnHandleEventListener);

        mDelate = (TextView) findViewById(R.id.tv_videoDelate);//视频已删除

        m_SurfaceHolder.addCallback(new SurfaceHolder.Callback() {// 定义surface状态回调处理函数
            // 发生改变
            public void surfaceChanged(SurfaceHolder holder,
                                       int format, int width, int height) {
                CustomLog.w(TAG, "surfaceChanged format=" + format
                        + " width=" + width + " height=" + height);
            }

            // 创建完成
            public void surfaceCreated(SurfaceHolder holder) {
                CustomLog.i(TAG, "surface Created");
                if (mPlayManager.getPlayState() == PlayManager.PLAY_STATE.NULL) {
                    int result = mPlayManager.startPlay(holder, mPlaySrcUrl, mPlaySrcType);
                    if (result != 0) {
                        CustomLog.d(TAG, "VideoPublishActivity::startPlay() error! result: " + result);
                        CustomToast.show(VideoPublishActivity.this, "片源切换中，请稍后重试", Toast.LENGTH_SHORT);
                    } else {
                        playSuccess();
                    }
                }
            }

            // 销毁
            public void surfaceDestroyed(SurfaceHolder holder) {
                CustomLog.i(TAG, "surface Destroyed");
            }
        });

        mPlayManager = new PlayManager(this, this);

        // 锁屏监听
        final IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        try {
            registerReceiver(mBatInfoReceiver, filter);
        } catch (Exception ex) {
            CustomLog.d(TAG, ex.getMessage());
        }
    }

    //锁定画布，避免贴上背景图后，最后一帧的闪现
    private void lockCanvas(SurfaceView surfaceView) {
        if (surfaceView == null) {
            return;
        }
        Canvas canvas = surfaceView.getHolder().lockCanvas();
        try {
            if (canvas != null) {
                surfaceView.getHolder().unlockCanvasAndPost(canvas);
            }
        } catch (Exception e) {
            CustomLog.e(TAG, e.toString());
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mPlayManager.markRequestState(PlayManager.PLAY_STATE.PAUSE);
        stopRequestVideoInfoTimer();

        pausePlay();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mPlaySrcType == PLAY_SRC_LIVE) {
            startRequestVideoInfoTimer();
        }

        if (mPlayManager.getRequestState() == PlayManager.PLAY_STATE.PLAYING) {
            resumePlay();
        }
    }

    private void initWebView() {
        WebSettings webSettings = mWebView.getSettings();
        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);

        mWebView.clearCache(true);
        mWebView.destroyDrawingCache();
        webSettings.setJavaScriptEnabled(true);

        mWebView.addJavascriptInterface(new JSLocalObj(this) {
            @Override
            public void onChooseContent(String contentId, String contentName, int type) {
                CustomLog.d(TAG, "onChooseContent contentId: " + contentId + " | contentName: " + contentName + " |type: " + type);
                if (contentId != null && contentId.equalsIgnoreCase(VideoPublishActivity.this.contentId)) {
                    CustomLog.d(TAG, "当前播放内容与目标播放内容一致，不做处理");
                    return;
                }
                stopRequestVideoInfoTimer();

                VideoPublishActivity.this.contentId = contentId;
                VideoPublishActivity.this.mPlaySrcType = type;
                mPlayPreviewType = -1;
                mPlaySrcUrl = "";
                mPosterUrl = "";

                if (mPlayManager.getPlayState() != PlayManager.PLAY_STATE.NULL) { //正在播放视频，

                    stopPlay();
                }
                requestVideoInfo();
            }
        }, JSLocalObj.JS_INTERFACE_NAME);
    }

    /**
     * 根据片源类型和服务器获取到的当前播放类型
     * 点播：显示海报和播放按钮；
     * 直播：判断服务器获取到的播放类型为海报（显示海报、不显示播放按钮）、视频（显示海报和播放按钮）
     * 如果是点播，不进行后台状态轮询；如果是直播，每隔3s轮询一下服务器设置的当前播放类型
     *
     * @param previewType 预播类型：海报、视频
     * @param posterUrl   海报地址
     * @param playSrcUrl  片源地址
     */
    private void refreshPlayerUI(int previewType, String posterUrl, String playSrcUrl) {
        if (mPlayPreviewType == previewType && mPosterUrl.equalsIgnoreCase(posterUrl) && mPlaySrcUrl.equalsIgnoreCase(playSrcUrl)) {
            CustomLog.d(TAG, "refreshPlayerUI() 状态未变 return!");
            return;
        }

        if (mPlayPreviewType == PLAY_PREVIEW_VIDEO && previewType == PLAY_PREVIEW_POSTER) { //视频切换为海报
            CustomLog.d(TAG, "refreshPlayerUI() 视频切换为海报，停止直播");
            stopPlay();
        }

        mPlaySrcUrl = playSrcUrl;
        mPosterUrl = posterUrl;
        mPlayPreviewType = previewType;

        showPoster(mPosterUrl);

        if (mPlaySrcType == PLAY_SRC_LIVE) { //对于直播，开启轮询
            startRequestVideoInfoTimer();
        }
    }

    private void hideProcessBar() {
        playContrButton.setEnabled(true);
        if (mProcessBarContainer.getVisibility() != View.GONE) {
            mProcessTV.setText(String.valueOf(0) + "%");
            mProcessBarContainer.setVisibility(View.GONE);
            // 消除动画
            mProcessTV.clearAnimation();
        }
    }

    //显示加载webview的loading的动画
    private void showWebProcessBar() {
        netRequestionLayout.setVisibility(View.VISIBLE);
        webLoading.post(new Runnable() {
            @Override
            public void run() {
                webLoading.startAnimation(getRotateAnimation());
            }
        });
    }

    //隐藏webview的loading动画
    private void hideWebProcessBar() {
        if (netRequestionLayout.getVisibility() != View.GONE) {
            netRequestionLayout.setVisibility(View.GONE);
        }
    }

    private void showProcessBar() {
        playContrButton.setEnabled(false); //缓存过程中不允许暂停、播放
        mProcessBarContainer.setVisibility(View.VISIBLE);
        mProcessBar.post(new Runnable() {
            @Override
            public void run() {
                mProcessBar.startAnimation(getRotateAnimation());
            }
        });
    }

    private void showPoster(final String str) {
        mPosterContainer.setVisibility(View.VISIBLE);
        posterLogo.setVisibility(View.VISIBLE);
        ImageLoader imageLoader = ImageLoader.getInstance();
        DisplayImageOptions options = new DisplayImageOptions.Builder()
                .cacheInMemory(true)//是否緩存都內存中
                .cacheOnDisc(true)//是否緩存到sd卡上
                .displayer(new RoundedBitmapDisplayer(0))//设置图片的显示方式 : 设置圆角图片  int roundPixels
                .bitmapConfig(Bitmap.Config.RGB_565)//设置为RGB565比起默认的ARGB_8888要节省大量的内存
                .delayBeforeLoading(100)//载入图片前稍做延时可以提高整体滑动的流畅度
                .build();
        imageLoader.displayImage(str, posterLogo, options,
                mMainDisplayimageListener);

        //点播或者直播已经开始，需要显示播放按钮
        if (mPlaySrcType == PLAY_SRC_VOD || (mPlaySrcType == PLAY_SRC_LIVE && mPlayPreviewType == PLAY_PREVIEW_VIDEO)) {
            posterPlayBtn.setVisibility(View.VISIBLE);
        } else {
            posterPlayBtn.setVisibility(View.GONE);
        }
    }

    private void hidePoster() {
        mPosterContainer.setVisibility(View.GONE);
    }

    //显示进度条等组件
    private void showControlPanel() {
        if (mPlaySrcType == PLAY_SRC_LIVE) {
            m_ControlPanel.setVisibility(View.VISIBLE);
            m_SeekBar.setVisibility(View.GONE);
            m_StartTimeText.setVisibility(View.GONE);
            m_Division.setVisibility(View.GONE);
            m_EndTimeText.setVisibility(View.GONE);
            backButton.setVisibility(View.VISIBLE);
        } else if (mPlaySrcType == PLAY_SRC_VOD) {
            m_ControlPanel.setVisibility(View.VISIBLE);
            m_SeekBar.setVisibility(View.VISIBLE);
            m_StartTimeText.setVisibility(View.VISIBLE);
            m_Division.setVisibility(View.VISIBLE);
            m_EndTimeText.setVisibility(View.VISIBLE);
            backButton.setVisibility(View.VISIBLE);
        }
    }

    //隐藏进度条等组件
    private void hideControlPanel() {
        m_ControlPanel.setVisibility(View.GONE);
        backButton.setVisibility(View.GONE);

    }

    private void startPlay() {
        m_VideoWindow.setVisibility(View.VISIBLE);
        PlayManager.PLAY_STATE state = mPlayManager.getPlayState();
        if (state == PlayManager.PLAY_STATE.PREPARE_PLAY || state == PlayManager.PLAY_STATE.PLAYING) {
            CustomLog.d(TAG, "startPlay() 播放中. state: " + state);
            return;
        }

        if (state == PlayManager.PLAY_STATE.STOPPING) {
            CustomLog.d(TAG, "VideoPublishActivity::startPlay() error! state == PlayManager.PLAY_STATE.STOPPING");
            CustomToast.show(this, "片源切换中，请稍后重试", Toast.LENGTH_SHORT);
            return;
        }

        if (m_VideoWindow.getHolder().getSurface() != null && m_VideoWindow.getHolder().getSurface().isValid()) {
            int result = mPlayManager.startPlay(m_VideoWindow.getHolder(), mPlaySrcUrl, mPlaySrcType);
            if (result != 0) {
                CustomLog.e(TAG, "VideoPublishActivity::startPlay() error! result: " + result);
            } else {
                playSuccess();
            }
        }
    }

    private void playSuccess() {
        showProcessBar();
        mProcessTV.setText(String.valueOf(0) + "%");
        hidePoster();
        showControlPanel();
        playContrButton.setBackgroundResource(R.drawable.liveroom_pause_bt_selector);
    }

    private void pausePlay() {
        if (mPlayManager.pause() != 0) {
            CustomLog.e(TAG, "pausePlay() error");
            return;
        }

        playContrButton.setBackgroundResource(R.drawable.liveroom_start_bt_selector);
    }

    private void resumePlay() {
        int ret = -1;

        if (mPlaySrcType == PLAY_SRC_VOD) {
            ret = mPlayManager.resume();
        } else if (mPlaySrcType == PLAY_SRC_LIVE) {
            ret = mPlayManager.restartLive();
        }

        if (ret != 0) {
            CustomLog.e(TAG, "resumePlay() error");
            return;
        }

        playContrButton.setBackgroundResource(R.drawable.liveroom_pause_bt_selector);
    }

    private void stopPlay() {
        if (mPlayManager.stop() != 0) {
            CustomLog.e(TAG, "stopPlay() error");
            return;
        }
        showPoster(mPosterUrl);
        hideControlPanel();
        hideProcessBar();
        totalDuration = 0;
        currentPos = 0;
        m_VideoWindow.setVisibility(View.GONE);
    }

    private void switchFullSreenPlay() {
        if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            fullScreenButton
                    .setBackgroundResource(R.drawable.liveroom_normalscreen_bt_selector);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            fullScreenButton
                    .setBackgroundResource(R.drawable.liveroom_fullscreen_bt_selector);
        }
    }

    private void showWebviewReloadUI() {
        netRequestionLayout.setVisibility(View.VISIBLE);
    }

    // 设置进度圈的动画
    private RotateAnimation mRotateAnimation;

    private RotateAnimation getRotateAnimation() {
        if (mRotateAnimation == null) {
            mRotateAnimation = new RotateAnimation(0.0F, 360.0F, 1, 0.5F, 1,
                    0.5F);
            mRotateAnimation.setFillAfter(false);
            mRotateAnimation.setRepeatCount(Animation.INFINITE);
            mRotateAnimation.setDuration(1000);
            mRotateAnimation.setInterpolator(new LinearInterpolator());
        }
        return mRotateAnimation;
    }

    private final BroadcastReceiver mBatInfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            String action = intent.getAction();
            CustomLog.i(TAG, "mBatInfoReceiver::onReceive action=" + action);

            if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                pausePlay();
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mPlayManager.getPlayState() != PlayManager.PLAY_STATE.NULL) {
            stopPlay();
        }
        try {
            unregisterReceiver(mBatInfoReceiver);
        } catch (Exception ex) {
            CustomLog.d(TAG, ex.getMessage());
        }

        if (getArticleInfo != null) {
            getArticleInfo.cancel();
        }

        if (mGetVideoInfo != null) {
            mGetVideoInfo.cancel();
        }

        stopRequestVideoInfoTimer();
    }

    @Override
    public void onBackPressed() {
        if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            switchFullSreenPlay();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        //横向
        if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
//            m_mediaViewFrameLayout.getLayoutParams().width = screenDm.heightPixels;
//            m_mediaViewFrameLayout.getLayoutParams().height = screenDm.widthPixels;
//            m_VideoLayoutParams = new FrameLayout.LayoutParams(
//                    FrameLayout.LayoutParams.MATCH_PARENT,
//                    FrameLayout.LayoutParams.MATCH_PARENT, Gravity.CENTER);
//            m_VideoWindow.setLayoutParams(m_VideoLayoutParams);
//            CustomLog.i(TAG, "onConfigurationChanged orientation=Configuration.ORIENTATION_LANDSCAPE");
//            WindowManager.LayoutParams lp = getWindow().getAttributes();
//            lp.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
//            getWindow().setAttributes(lp);
//            getWindow().addFlags(
//                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
            CustomLog.i(TAG, "onConfigurationChanged orientation=Configuration.ORIENTATION_LANDSCAPE");
            updateSurfaceWindowSize(true);
        } else if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) { //纵向
//            m_mediaViewFrameLayout.getLayoutParams().width = screenDm.widthPixels;
//            m_mediaViewFrameLayout.getLayoutParams().height = screenDm.widthPixels * 9 / 16;
//            m_VideoLayoutParams = new FrameLayout.LayoutParams(
//                    FrameLayout.LayoutParams.MATCH_PARENT,
//                    FrameLayout.LayoutParams.MATCH_PARENT, Gravity.CENTER);
//            m_VideoWindow.setLayoutParams(m_VideoLayoutParams);
//
//            CustomLog.i(TAG, "onConfigurationChanged orientation=Configuration.ORIENTATION_PORTRAIT");
//
//            WindowManager.LayoutParams lp = getWindow().getAttributes();
//            lp.flags &= (~WindowManager.LayoutParams.FLAG_FULLSCREEN);
//            getWindow().setAttributes(lp);
//            getWindow().clearFlags(
//                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
            CustomLog.i(TAG, "onConfigurationChanged orientation=Configuration.ORIENTATION_PORTRAIT");
            updateSurfaceWindowSize(false);
        }
    }

    private void updateSurfaceWindowSize(boolean fullscreen) {
        int parentWidth;
        int parentHeight;
        if (fullscreen) {
            parentWidth = m_mediaViewFrameLayout.getLayoutParams().width = screenDm.heightPixels;
            parentHeight = m_mediaViewFrameLayout.getLayoutParams().height = screenDm.widthPixels;
        } else {
            parentWidth = m_mediaViewFrameLayout.getLayoutParams().width = screenDm.widthPixels;
            parentHeight = m_mediaViewFrameLayout.getLayoutParams().height = screenDm.widthPixels * 9 / 16;
        }

        if (((float) parentWidth / parentHeight) - ((float) srcWidht / srcHeight) > 0.001f) {
            parentWidth = parentHeight * srcWidht / srcHeight;
        } else {
            parentHeight = parentWidth * srcHeight / srcWidht;
        }

        m_VideoLayoutParams = new FrameLayout.LayoutParams(
                parentWidth,
                parentHeight, Gravity.CENTER);
        m_VideoWindow.setLayoutParams(m_VideoLayoutParams);

        if (fullscreen) {
            WindowManager.LayoutParams lp = getWindow().getAttributes();
            lp.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
            getWindow().setAttributes(lp);
            getWindow().addFlags(
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        } else {
            WindowManager.LayoutParams lp = getWindow().getAttributes();
            lp.flags &= (~WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getWindow().setAttributes(lp);
            getWindow().clearFlags(
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        }
    }


    /**
     * 加载完成：媒体格式已经可以解析、长度、点播直播、编码格式
     *
     * @param state 1:加载成功
     */
    @Override
    public void onLoadingCompleted(int state) {
        totalDuration = mPlayManager.getDuration();
        m_EndTimeText.setText(PlayManager.getFormatTime(totalDuration));
        if (mPlaySrcType == PLAY_SRC_VOD) {
            m_ControlPanel.setVisibility(View.VISIBLE);
            m_SeekBar.setMax(totalDuration);
        }
    }

    /**
     * 播放进度更新
     *
     * @param currentPos 当前播放进度
     */
    @Override
    public void onPlayingUpdate(int currentPos) {
        this.currentPos = currentPos;
        m_StartTimeText.setText(PlayManager.getFormatTime(currentPos));
        m_SeekBar.setProgress(this.currentPos);
        hideProcessBar();
    }

    /**
     * 缓冲加载进度更新
     *
     * @param percent 进度百分比
     */
    @Override
    public void onBufferingUpdate(int percent) {
        showProcessBar();
        mProcessTV.setText(String.valueOf(percent) + "%");
        if (percent == 100) {
            hideProcessBar();
        }
    }

    /**
     * seek完成
     */
    @Override
    public void onSeekCompleted() {
        if (mPlayManager.getPlayState() == PlayManager.PLAY_STATE.PAUSE) {
            playContrButton.setBackgroundResource(R.drawable.liveroom_start_bt_selector);
        } else if (mPlayManager.getPlayState() == PlayManager.PLAY_STATE.PLAYING) {
            playContrButton.setBackgroundResource(R.drawable.liveroom_pause_bt_selector);
        }
    }

    /**
     * 点播播放完成
     */
    @Override
    public void onPlayingCompleted() {
        stopPlay();
        if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            switchFullSreenPlay();
        }
    }

    /**
     * 出错异常
     *
     * @param code  错误码
     * @param extra 额外信息
     */
    @Override
    public void onError(int code, int extra) {

    }

    /**
     * 媒体格式回调
     *
     * @param width  宽
     * @param height 高
     */
    @Override
    public void onMediaFormat(int width, int height) {
        if ((float) width / height - (float) 16 / 9 < 0.1f) { //片源非 16：9，需要更新播放窗口状态
            boolean fullScreen = this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE ? true : false;
            srcWidht = width;
            srcHeight = height;
            updateSurfaceWindowSize(fullScreen);
        }
    }

    @Override
    public void todoClick(int i) {
        super.todoClick(i);
        switch (i) {
            case R.id.poster_play_btn:
                startPlay();
                break;
            case R.id.control_bt:
                if (mPlayManager.getPlayState() == PlayManager.PLAY_STATE.PLAYING) {
                    pausePlay();
                } else if (mPlayManager.getPlayState() == PlayManager.PLAY_STATE.PAUSE) {
                    resumePlay();
                }
                break;
            case R.id.fullscreen_bt:
                switchFullSreenPlay();
                break;
            case R.id.info_view:
                if (m_ControlPanel.getVisibility() == View.GONE) {
                    showControlPanel();
                } else {
                    hideControlPanel();
                }
                break;
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.live_room_player_back://返回键
                if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    switchFullSreenPlay();
                } else {
                    finish();
                }
                break;
            case R.id.pause_logo://中间播放按钮
                break;
            case R.id.replay_logo://中间刷新按钮
                break;
            case R.id.control_bt://控制按钮
                break;
            case R.id.btn_back:
                finish();
                break;
        }
    }
}
