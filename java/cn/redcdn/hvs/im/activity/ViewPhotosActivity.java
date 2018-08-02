package cn.redcdn.hvs.im.activity;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import cn.redcdn.commonutil.NetConnectHelper;
import cn.redcdn.hvs.MedicalApplication;
import cn.redcdn.hvs.R;
import cn.redcdn.hvs.base.BaseActivity;
import cn.redcdn.hvs.im.activity.ViewImages.PhotoView;
import cn.redcdn.hvs.im.activity.ViewImages.PhotoViewAttacher;
import cn.redcdn.hvs.im.bean.ButelFileInfo;
import cn.redcdn.hvs.im.bean.CollectionEntity;
import cn.redcdn.hvs.im.bean.FileTaskBean;
import cn.redcdn.hvs.im.bean.NoticesBean;
import cn.redcdn.hvs.im.bean.PhotoBean;
import cn.redcdn.hvs.im.collection.CollectionFileManager;
import cn.redcdn.hvs.im.dao.CollectionDao;
import cn.redcdn.hvs.im.dao.NoticesDao;
import cn.redcdn.hvs.im.fileTask.ChangeUIInterface;
import cn.redcdn.hvs.im.fileTask.DownloadTaskManager;
import cn.redcdn.hvs.im.fileTask.FileTaskManager;
import cn.redcdn.hvs.im.manager.CollectionManager;
import cn.redcdn.hvs.im.util.IMCommonUtil;
import cn.redcdn.hvs.im.util.ViewPages;
import cn.redcdn.hvs.im.view.BottomButtonMenu;
import cn.redcdn.hvs.im.view.BottomMenuWindow;
import cn.redcdn.hvs.im.view.CommonDialog;
import cn.redcdn.hvs.util.TitleBar;
import cn.redcdn.log.CustomLog;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.ref.WeakReference;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * <dt>ViewPhotosActivity.java</dt>
 * <dd>Description:图片浏览界面</dd>
 * <dd>Copyright: Copyright (C) 2014</dd>
 * <dd>Company: 安徽青牛信息技术有限公司</dd>
 * <dd>CreateDate: 2014-3-17 上午10:21:05</dd>
 * <dd>modify:更多-转发：采用本地分享的交互设计 on 2015-9-9 by wxy</dd>
 */

public class ViewPhotosActivity extends BaseActivity {
    private static final String TAG = "ViewPhotosActivity";

    public static final String KEY_PHOTOS_LIST = "photos_list";
    public static final String KEY_PHOTOS_SELECT_INDEX = "photos_select_index";
    public static final String KEY_REMOTE_FILE = "key_remote_file";
    public static final String KEY_VIDEO_FILE = "key_video_file";
    public static final String KEY_VIDEO_LEN = "key_video_len";
    public static final String KEY_COLLECTION_TYPE = "key_collection_type";
    public static final String KEY_COLLECTION_SCAN = "key_collection_scan";
    public static final int ACTION_SELECT_LINKMAN = 2004; // 选择联系人返回

    private Context mContext = null;
    private RelativeLayout rootViewContainer = null;

    private ViewPages mViewPager = null;
    private List<String> mListImage = null;
    private List<PhotoBean> mListPhoto = null;

    private boolean isRemoteFile = false;
    private boolean isVideoFile = false;
    private SamplePagerAdapter mAdapter = null;

    private TextView mTextViewInfo = null;
    private static int mSize = 0;

    private int selectedIndex = -1;

    private int len = 0;
    private String str;
    // 图片的存储目录
    private File PHOTO_DIR;
    private String filePath = "";
//	public String existFile;

    private TitleBar titleBar = null;

    private LayoutInflater layoutInflater = null;

    // 下载图片进度列表，便于对象销毁后再次显示时重新绑定
    private SparseArray<ProgressListener> progressListArray = new SparseArray<ProgressListener>();
    // // 显示页是否加载到内存
    // private SparseBooleanArray pageLoadedArray = new SparseBooleanArray();
    // private SparseArray<View> pages = new SparseArray<View>();

    private static int downLoadExceptWifi = 0;
    private CommonDialog downLoadDlg = null;

    private boolean isFromAlarm = false;
    private String[] times;
    private TextView takeTimeText = null;
    private long entryTime = 0;
    //0表示从消息页面跳转，1表示从收藏页面跳转
    private int collectionType = 0;
    //true 表示可以收藏  false不可以收藏
    private boolean canCollect = true;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.viewphotos);

        CustomLog.d(TAG,"onCreate begin");

        this.mContext = this;
        this.layoutInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        Bundle boudle = getIntent().getExtras();
        isRemoteFile = boudle.getBoolean(KEY_REMOTE_FILE);
        isVideoFile = boudle.getBoolean(KEY_VIDEO_FILE);
        // 从报警页面进入图片查看，不显示“更多”
        if (boudle.getString(SelectLinkManActivity.ACTIVITY_FLAG) != null) {
            String flag = boudle.getString(SelectLinkManActivity.ACTIVITY_FLAG);
//            if (flag.equals(AlarmMsgActivity.TAG)) {
//                Log.d(TAG, flag);
//                isFromAlarm = true;
//                entryTime = System.currentTimeMillis();
//                if (!TextUtils.isEmpty(boudle.getString("TAKETIME"))) {
//                    times = boudle.getString("TAKETIME").split(";");
//                    Log.d(TAG, "times:" + boudle.getString("TAKETIME"));
//                }
//            }
        }

        str = getString(R.string.photos);
        PHOTO_DIR = new File(Environment.getExternalStorageDirectory() + str);
        titleBar = getTitleBar();
//		existFile = "\""
//				+ Environment.getExternalStorageDirectory().getAbsolutePath()
//				+ str + "\"";
        collectionType = boudle.getInt(KEY_COLLECTION_TYPE,0);
        canCollect = boudle.getBoolean(KEY_COLLECTION_SCAN, false);
        if (isRemoteFile) {
            mListPhoto = boudle.getParcelableArrayList(KEY_PHOTOS_LIST);
            if (null != mListPhoto)
                mSize = mListPhoto.size();
        } else {
            mListImage = boudle.getStringArrayList(KEY_PHOTOS_LIST);
            if (null != mListImage)
                mSize = mListImage.size();
        }
        // 默认标题不显示
        titleBar.enableBack();
        if (!isVideoFile) {
            if (!isFromAlarm) {
                titleBar.setTitle(R.string.scan_photo);
                titleBar.enableRightBtn("", R.drawable.butel_detial_more,
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                CustomLog.d(TAG,"更多操作按钮");
                                // 更多操作
                                displayMoreMenu();
                            }
                        });
            } else {
                titleBar.setTitle("查看图片");
                titleBar.setRightBtnVisibility(View.GONE);
            }

            if (!isFromAlarm) {
                if (isRemoteFile) {
                    titleBar.setRightBtnVisibility(View.VISIBLE);
                } else {
                    titleBar.setRightBtnVisibility(View.GONE);
                }
            }

        } else {
            len = boudle.getInt(KEY_VIDEO_LEN);
            titleBar.setTitle(R.string.browse_vedio);
        }

        RelativeLayout container = (RelativeLayout) findViewById(R.id.image_container);
        rootViewContainer = (RelativeLayout) findViewById(R.id.view_container);

        mViewPager = new ViewPages(this);
        container.addView(mViewPager, 0);

        mTextViewInfo = (TextView) findViewById(R.id.text_info);
        rootViewContainer.bringChildToFront(mTextViewInfo);

        // 显示报警照片拍摄时间-wxy
        takeTimeText = (TextView) findViewById(R.id.take_time);
        if (isFromAlarm) {
            rootViewContainer.bringChildToFront(takeTimeText);
        }
        initControl();
        CustomLog.d(TAG,"onCreate end");
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    /**
     * @author: zhaguitao
     * @Title: displayMoreMenu
     * @Description: 更多操作菜单
     * @date: 2014-1-9 下午3:28:25
     */
    private void displayMoreMenu() {
        BottomButtonMenu bottomMenu = new BottomButtonMenu(
                ViewPhotosActivity.this);
        final PhotoBean bean = mListPhoto.get(mViewPager.getCurrentItem());
        if (collectionType == 1) {
            int type = 0;
            final CollectionEntity CollectionEntity = new CollectionDao(
                    getBaseContext()).getCollectionEntityById(bean.getTaskId());
            if (isVideoFile) {
                type = FileTaskManager.NOTICE_TYPE_VEDIO_SEND;
            } else {
                type = FileTaskManager.NOTICE_TYPE_PHOTO_SEND;
            }
            ArrayList<PhotoBean> photos = CollectionFileManager.getInstance().getAllPFileList(CollectionEntity,type);
            if(isValidImagePath(photos.get(selectedIndex).getLocalPath())
                    || !TextUtils.isEmpty(photos.get(selectedIndex).getRemoteUrl())){
                bottomMenu.addButtonFirst(new BottomMenuWindow.MenuClickedListener() {
                    @Override
                    public void onMenuClicked() {
//                        MobclickAgent.onEvent(mContext,
//                                UmengEventConstant.EVENT_PRE_FORWARD_PIC);
                        // 转发图片
                        CustomLog.d(TAG,"点击转发按钮操作");
                        selectContact();
                    }
                }, getString(R.string.share_information));
            }
        } else {
            final NoticesBean noticebean = new NoticesDao(getBaseContext())
                    .getNoticeById(bean.getTaskId());
            // 只有发送成功的消息，才能进行转发
            // 发送成功的消息，服务端已产生数据，只要再发一次消息即可，这样可以加快转发效率和节省流量
            if (noticebean != null) {

                ButelFileInfo fileInfo = ButelFileInfo.parseJsonStr(noticebean
                        .getBody(), false);
                if (isValidImagePath(fileInfo.getLocalPath())
                        || !TextUtils.isEmpty(fileInfo.getRemoteUrl())) {
                    // 有数据源的场合，才能转发
                    bottomMenu.addButtonFirst(new BottomMenuWindow.MenuClickedListener() {
                        @Override
                        public void onMenuClicked() {
//                            MobclickAgent.onEvent(mContext,
//                                    UmengEventConstant.EVENT_PRE_FORWARD_PIC);
                            // 转发图片
                            CustomLog.d(TAG,"点击转发按钮操作");
                            selectContact();
                        }
                    }, getString(R.string.share_information));

                    if (canCollect) {
                        bottomMenu.addButtonSecond(new BottomMenuWindow.MenuClickedListener() {

                            @Override
                            public void onMenuClicked() {

                                CollectionManager.getInstance()
                                        .addCollectionByNoticesBean(
                                                ViewPhotosActivity.this,
                                                noticebean);
                            }
                        }, "收藏");
                    }
                }
            }
        }

        String localPath = bean.getLocalPath();
        if (isValidImagePath(localPath)) {
            // 图片已下载的场合，显示存储到本地
            bottomMenu.addButtonThird(new BottomMenuWindow.MenuClickedListener() {
                @Override
                public void onMenuClicked() {
//                    MobclickAgent.onEvent(mContext,
//                            UmengEventConstant.EVENT_PRE_SAVE_PIC);
                    CustomLog.d(TAG,"onMenuClicked");
                    // 存储到本地
                    savePhotoToSDCard();
                }
            }, getString(R.string.save_mobile));
        }
        // 20141119 邮件【消息评审确定问题周知】第9点：Android预览图片，暂时不加删除按钮
        // int type = bean.getType();
        // if (FileTaskManager.NOTICE_TYPE_PHOTO_SEND == type
        // || FileTaskManager.NOTICE_TYPE_VEDIO_SEND == type) {
        // // 接收图片才能被删除
        // bottomMenu.addButtonChangeNumber(new MenuClickedListener() {
        // @Override
        // public void onMenuClicked() {
        // logD("onMenuClicked");
        // CommonDialog delDialog = new CommonDialog(mContext,
        // getLocalClassName(), 103);
        // //delDialog.setTitle("删除图片");
        // logD("与这张图片同时分享的一组图片都会被删除");
        // delDialog.setMessage("与这张图片同时分享的一组图片都会被删除");
        // delDialog.setCancleButton(null,"取消");
        // delDialog.setPositiveButton(new BtnClickedListener() {
        // @Override
        // public void onBtnClicked() {
        // // 停止下载任务
        // new Thread(new Runnable() {
        // @Override
        // public void run() {
        // // 删除下载的文件
        // NetPhoneApplication.getFileTaskManager()
        // .cancelTaskAndDelFile(bean.getTaskId());
        // ViewPhotosActivity.this.finish();
        // }
        // }).start();
        // }
        // }, "删除");
        // delDialog.showDialog();
        // }
        // },"删除");
        // }
        bottomMenu.show();
    }

    private void initControl() {
        mAdapter = new SamplePagerAdapter();
        mViewPager.setOnPageChangeListener(pageListener);
        mViewPager.setAdapter(mAdapter);
        if (selectedIndex == -1) {
            selectedIndex = getIntent().getIntExtra(KEY_PHOTOS_SELECT_INDEX, 0);
        }
        mViewPager.setCurrentItem(selectedIndex);
        onImgPageSelected(selectedIndex);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        CustomLog.d(TAG,"onStop begin");
        if (isFromAlarm) {
            long endTime = System.currentTimeMillis();
            int duration = (int) ((endTime - entryTime) / 1000);
//            MobclickAgent.onEventValue(getBaseContext(),
//                    UmengEventConstant.EVENT_ALARM_IMAGE_DURATION, null,
//                    duration);
        }
        CustomLog.d(TAG,"onStop end");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        CustomLog.d(TAG,"onDestroy begin");
        mViewPager.clearAnimation();
        mViewPager.destroyDrawingCache();
        mViewPager.removeAllViews();

        if (downLoadDlg != null && downLoadDlg.isShowing()) {
            downLoadDlg.dismiss();
            downLoadDlg = null;
        }

        downLoadExceptWifi = 0;
        mContext = null;

        if (dlObjList != null) {
            dlObjList.clear();
            dlObjList = null;
        }

        CustomLog.d(TAG,"onDestroy end");
    }

    private void onImgPageSelected(int index) {
        mTextViewInfo.setText((index + 1) + "/" + mSize);
        if (isFromAlarm) {
            takeTimeText.setText(times[index]);
        }
    }

    public class SamplePagerAdapter extends PagerAdapter {

        @Override
        public int getCount() {
            if (isRemoteFile) {
                if (mListPhoto != null) {
                    return mListPhoto.size();
                } else {
                    return 0;
                }
            } else {
                if (null == mListImage) {
                    return 0;
                } else {
                    return mListImage.size();
                }
            }
        }

        @Override
        public View instantiateItem(ViewGroup container, final int position) {
            CustomLog.d(TAG,"SamplePagerAdapter instantiateItem:" + position);

            // final PhotoView photoView = new
            // PhotoView(container.getContext());
            // // photoView.setScaleType(ScaleType.CENTER_INSIDE);
            // photoView
            // .setOnViewTapListener(new PhotoViewAttacher.OnViewTapListener() {
            // @Override
            // public void onViewTap(View view, float x, float y) {
            // logD("setOnViewTapListener");
            // if (titleBar.isShowing()) {
            // titleBar.hide();
            // } else {
            // titleBar.show();
            // }
            // }
            // });
            // photoView
            // .setOnLongClickListener(new
            // android.view.View.OnLongClickListener() {
            // @Override
            // public boolean onLongClick(View view) {
            // logD("setOnLongClickListener");
            // if (titleBar.isShowing()) {
            // titleBar.hide();
            // } else {
            // titleBar.show();
            // }
            // return false;
            // }
            // });

            final PhotoView photoViewThumbnail = new PhotoView(
                    container.getContext());
            photoViewThumbnail
                    .setOnViewTapListener(new PhotoViewAttacher.OnViewTapListener() {
                        @Override
                        public void onViewTap(View view, float x, float y) {
                            CustomLog.d(TAG,"photoViewThumbnail.setOnViewTapListener");
                            if (isRemoteFile && mListPhoto != null
                                    && mListPhoto.size() > 0) {
                                PhotoBean photo = mListPhoto.get(position);
                                if (photo != null
                                        && photo.getType() == FileTaskManager.NOTICE_TYPE_VEDIO_SEND) {
                                    String localPathf = photo.getLocalPath();
                                    if (!TextUtils.isEmpty(localPathf)
                                            && !localPathf.endsWith(".temp")) {
                                        File locVidFile = new File(localPathf);
                                        if (locVidFile.exists()) {
                                            Intent i = new Intent(mContext,
                                                    PlayVideoActivity.class);
                                            i.putExtra(
                                                    RecordedVideoActivity.KEY_VIDEO_FILE_PATH,
                                                    localPathf);
                                            i.putExtra(
                                                    RecordedVideoActivity.KEY_VIDEO_FILE_DURATION,
                                                    len);

                                            mContext.startActivity(i);
                                            return;
                                        }
                                    }
                                }
                            }

                            if (titleBar.isShowing()) {
                                titleBar.hide();
                            } else {
                                titleBar.show();
                            }
                        }
                    });
            photoViewThumbnail
                    .setOnLongClickListener(new android.view.View.OnLongClickListener() {
                        @Override
                        public boolean onLongClick(View view) {
                            CustomLog.d(TAG,"photoViewThumbnail.setOnLongClickListener");
                            if (isRemoteFile && mListPhoto != null
                                    && mListPhoto.size() > 0) {
                                PhotoBean photo = mListPhoto.get(position);
                                if (photo != null
                                        && photo.getType() == FileTaskManager.NOTICE_TYPE_VEDIO_SEND) {
                                    return false;
                                }
                            }
                            if (titleBar.isShowing()) {
                                titleBar.hide();
                            } else {
                                titleBar.show();
                            }
                            return false;
                        }
                    });

            // photoViewThumbnail.setTag("thumbnail");
            // photoView.setTag("photoView");

            View downloadingView = layoutInflater.inflate(
                    R.layout.view_photo_downloading, container, false);
            LinearLayout progressLine = (LinearLayout) downloadingView
                    .findViewById(R.id.loading_line);
            final FrameLayout viewContainer = (FrameLayout) downloadingView
                    .findViewById(R.id.container);
            container.addView(downloadingView, 0, new LayoutParams(
                    RelativeLayout.LayoutParams.MATCH_PARENT,
                    RelativeLayout.LayoutParams.MATCH_PARENT));

            downloadingView.findViewById(R.id.no_pic_line).setVisibility(
                    View.GONE);
            viewContainer.addView(photoViewThumbnail, 0, new LayoutParams(
                    RelativeLayout.LayoutParams.MATCH_PARENT,
                    RelativeLayout.LayoutParams.MATCH_PARENT));

            if (isRemoteFile) {

                boolean isLoaded = false;
                final PhotoBean photo = mListPhoto.get(position);
                final String localPath = photo.getLocalPath();

                if (isValidImagePath(localPath)) {
                    CustomLog.d(TAG,"加载原图：" + localPath);
                    Glide.with(ViewPhotosActivity.this).load(localPath)
                            .placeholder(R.drawable.empty_photo)
                            .error(R.drawable.empty_photo)
                            .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                            .crossFade().into(photoViewThumbnail);
                    isLoaded = true;
                }

                if (!isLoaded) {
                    CustomLog.d(TAG,"图片暂未下载的场合，先显示缩略图，同时开始下载原图");
                    // 图片暂未下载的场合，先显示缩略图，同时开始下载原图
                    String thumbnailUrl = photo.getLittlePicUrl();
                    if (!TextUtils.isEmpty(thumbnailUrl)) {
                        Glide.with(ViewPhotosActivity.this).load(thumbnailUrl)
                                .placeholder(R.drawable.empty_photo)
                                .error(R.drawable.empty_photo)
                                .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                                .crossFade().into(photoViewThumbnail);
                    }

                    // 报警页面图片查看不下载原图，只查看缩略图--wxy-15-3-30
                    // 此处增加一个逻辑判断
                    if (!isFromAlarm) {
                        if (!TextUtils.isEmpty(photo.getRemoteUrl())) {
                            // 开始下载原图或视频
                            downLoad(progressLine, photo.getTaskId(),
                                    photo.getRemoteUrl(), photoViewThumbnail,
                                    position);
                        } else {
                            downloadingView.findViewById(R.id.no_pic_line)
                                    .setVisibility(View.VISIBLE);
                        }
                    } else {
                        CustomLog.d(TAG,"i看家  报警消息->查看图片,不下载原图,只下载缩略图");
                    }
                }

                if (isVideoFile) {
                    // 下载视频的场合，显示视频图标
                    downloadingView.findViewById(R.id.video_icon)
                            .setVisibility(View.VISIBLE);
                }
            } else {

                Glide.with(ViewPhotosActivity.this)
                        .load(mListImage.get(position))
                        .placeholder(R.drawable.empty_photo)
                        .error(R.drawable.empty_photo)
                        .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                        .crossFade().into(photoViewThumbnail);

            }

            // pages.put(position, downloadingView);
            return downloadingView;
        }

        // @Override
        // public void setPrimaryItem(ViewGroup container, int position,
        // Object object) {
        // if (pageLoadedArray.get(position)) {
        // return;
        // }
        // LogUtil.d2File(TAG, "SamplePagerAdapter setPrimaryItem:" + position);
        // pageLoadedArray.put(position, true);
        // final PhotoView photoView = new PhotoView(container.getContext());
        // photoView
        // .setOnViewTapListener(new PhotoViewAttacher.OnViewTapListener() {
        // @Override
        // public void onViewTap(View view, float x, float y) {
        // if (titleBar.isShowing()) {
        // titleBar.hide();
        // } else {
        // titleBar.show();
        // }
        // }
        // });
        // photoView
        // .setOnLongClickListener(new android.view.View.OnLongClickListener() {
        // @Override
        // public boolean onLongClick(View view) {
        // if (titleBar.isShowing()) {
        // titleBar.hide();
        // } else {
        // titleBar.show();
        // }
        // return false;
        // }
        // });
        //
        // View downloadingView = pages.get(position);
        // LinearLayout progressLine = (LinearLayout)
        // downloadingView.findViewById(R.id.loading_line);
        // final FrameLayout viewContainer1 = (FrameLayout)
        // downloadingView.findViewById(R.id.container);
        //
        // if (isRemoteFile) {
        //
        // PhotoBean photo = mListPhoto.get(position);
        // final String localPath = photo.getLocalPath();
        // boolean isLoaded = false;
        // if (isValidImagePath(localPath)) {
        // // 显示原图
        // mImageFetcher.loadLocalImage(photoView, localPath, mWinWidth,
        // mWinHeight,
        // new OnImgLoadAfterListener() {
        // @Override
        // public void onImgLoadAfter() {
        // LogUtil.d2File(TAG, "原图加载完成，显示原图1，onImgLoadAfter");
        // viewContainer1.addView(photoView, 1, new LayoutParams(
        // RelativeLayout.LayoutParams.MATCH_PARENT,
        // RelativeLayout.LayoutParams.MATCH_PARENT));
        // // 隐藏缩略图
        // Animation invisibleAnim = new AlphaAnimation(1.0f, 0.0f);
        // invisibleAnim.setDuration(500);
        // invisibleAnim.setFillAfter(true);
        // invisibleAnim.setRepeatCount(0);
        // viewContainer1.getChildAt(0).startAnimation(invisibleAnim);
        // }
        // });
        //
        // isLoaded = true;
        // }
        //
        // if (!isLoaded) {
        // viewContainer1.addView(photoView, 0, new LayoutParams(
        // RelativeLayout.LayoutParams.MATCH_PARENT,
        // RelativeLayout.LayoutParams.MATCH_PARENT));
        // // 图片暂未下载的场合，先显示缩略图，同时开始下载原图
        // String thumbnailUrl = photo.getLittlePicUrl();
        // if (!TextUtils.isEmpty(thumbnailUrl)) {
        // mImageFetcher.loadHttpImage(photoView, photo.getLittlePicUrl(),
        // mWinWidth, mWinHeight, null);
        // } else {
        // photoView.setImageResource(R.drawable.empty_photo);
        // }
        //
        // if (!TextUtils.isEmpty(photo.getRemoteUrl())) {
        // // 开始下载原图
        // downLoad(progressLine, photo.getTaskId(), photo.getRemoteUrl(),
        // photoView, position);
        // }
        // }
        // } else {
        // // 加载原图
        // mImageFetcher.loadLocalImage(photoView, mListImage.get(position),
        // mWinWidth, mWinHeight,
        // new OnImgLoadAfterListener() {
        // @Override
        // public void onImgLoadAfter() {
        // LogUtil.d2File(TAG, "原图加载完成，显示原图2，onImgLoadAfter");
        // viewContainer1.addView(photoView, 1, new LayoutParams(
        // RelativeLayout.LayoutParams.MATCH_PARENT,
        // RelativeLayout.LayoutParams.MATCH_PARENT));
        // // 隐藏缩略图
        // Animation invisibleAnim = new AlphaAnimation(1.0f, 0.0f);
        // invisibleAnim.setDuration(500);
        // invisibleAnim.setFillAfter(true);
        // invisibleAnim.setRepeatCount(0);
        // viewContainer1.getChildAt(0).startAnimation(invisibleAnim);
        // }
        // });
        // }
        // }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
            // pageLoadedArray.put(position, false);
            // pages.remove(position);
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }
    }

    private boolean isValidImagePath(String imagePath) {
        if (TextUtils.isEmpty(imagePath) || imagePath.endsWith(".temp")) {
            return false;
        }
        File file = new File(imagePath);
        if (!file.exists()) {
            return false;
        }
        return true;
    }

    ViewPager.OnPageChangeListener pageListener = new ViewPager.OnPageChangeListener() {

        @Override
        public void onPageSelected(int arg0) {
            selectedIndex = mViewPager.getCurrentItem();
            CustomLog.d(TAG,"pageListener:onPageSelected:" + arg0 + "|" + selectedIndex);
            onImgPageSelected(selectedIndex);

            if (isRemoteFile) {
                PhotoBean photo = mListPhoto.get(selectedIndex);
                final String localPath = photo.getLocalPath();
                if (isValidImagePath(localPath)
                        || !TextUtils.isEmpty(photo.getRemoteUrl())) {
                    // 有数据源的场合，才显示更多按钮
                    titleBar.setRightBtnVisibility(View.VISIBLE);
                } else {
                    titleBar.setRightBtnVisibility(View.GONE);
                }
            }
        }

        @Override
        public void onPageScrolled(int arg0, float arg1, int arg2) {
        }

        @Override
        public void onPageScrollStateChanged(int arg0) {
        }
    };

    private Handler callbackHandler = new Handler() {
        public void handleMessage(Message msg) {
            CustomLog.d(TAG, "callbackHandler msg =" + msg.what);
            switch (msg.what) {
                case 1:
                    final DownLoadObject obj = (DownLoadObject) msg.obj;

                    if (NetConnectHelper.NETWORKTYPE_WIFI == NetConnectHelper.getNetWorkType(mContext)) {
                        // 当前是wifi连接，直接下载
                        attachProgressView(obj.progressLine, obj.id, obj.remoteUrl,
                                obj.photoView, obj.position);
                    } else {
                        if (NetConnectHelper.NETWORKTYPE_INVALID == NetConnectHelper.getNetWorkType(mContext)) {
                            // 当前无网络连接，放弃下载
                            Toast.makeText(ViewPhotosActivity.this,
                                    getString(R.string.setting_internet),
                                    Toast.LENGTH_SHORT).show();
                            break;
                        }
                        // 视频大小大于3M时候出现提醒框
                        if(!isVideoFile){
                            attachProgressView(obj.progressLine, obj.id, obj.remoteUrl,
                                    obj.photoView, obj.position);
                            break;
                        }
                        if (downLoadExceptWifi == 0) {
                            downLoadExceptWifi = -1;

                            // 非wifi连接下，提示是否继续下载
                            downLoadDlg = new CommonDialog(ViewPhotosActivity.this,
                                    getLocalClassName(), 104);
                            downLoadDlg.setMessage(R.string.load_tip);
                            downLoadDlg.setCancelable(false);
                            downLoadDlg.setCancleButton(new CommonDialog.BtnClickedListener() {
                                @Override
                                public void onBtnClicked() {
                                    downLoadExceptWifi = 2;
                                }
                            }, R.string.cancel_message);
                            downLoadDlg.setPositiveButton(new CommonDialog.BtnClickedListener() {
                                @Override
                                public void onBtnClicked() {
                                    downLoadExceptWifi = 1;
                                    // attachProgressView(obj.progressLine, obj.id,
                                    // obj.remoteUrl, obj.photoView, obj.position);
                                    resentDownloadMsg();
                                }
                            }, R.string.confirm_message);
                            downLoadDlg.showDialog();
                        } else if (downLoadExceptWifi == 1) {
                            // 用户选择，非wifi时，下载
                            Toast.makeText(ViewPhotosActivity.this,
                                    getString(R.string.load_tips),
                                    Toast.LENGTH_SHORT).show();
                            attachProgressView(obj.progressLine, obj.id,
                                    obj.remoteUrl, obj.photoView, obj.position);
                        } else if (downLoadExceptWifi == 2) {
                            // 用户选择，非wifi时，不下载
                        }
                    }
                    break;
                default:
                    break;
            }
        }
    };

    private void downLoad(final LinearLayout progressLine, final String id,
                          final String remoteUrl, final PhotoView photoView,
                          final int position) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (downLoadExceptWifi == -1) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                DownLoadObject obj = new DownLoadObject();
                obj.progressLine = progressLine;
                obj.id = id;
                obj.remoteUrl = remoteUrl;
                obj.photoView = photoView;
                obj.position = position;

                if (dlObjList != null) {
                    dlObjList.add(obj);
                } else {
                    dlObjList = new ArrayList<DownLoadObject>();
                    dlObjList.add(obj);
                }

                Message msg = callbackHandler.obtainMessage();
                msg.what = 1;
                msg.obj = obj;
                callbackHandler.sendMessage(msg);
            }
        }).start();
    }

    /***
     * 因目前‘非WIFI下，下载弹出框’仅在page 0页面出现一次，其他page不弹出
     * 而handler消息在instantiateItem回调方法中及时性的send完成； 以致点对话框中的同意按钮仅对page 0页面的原图进行下载；
     * 故用list暂存数据，点‘同意’后再重新发handler消息
     */
    private ArrayList<DownLoadObject> dlObjList = new ArrayList<DownLoadObject>();

    private void resentDownloadMsg() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (dlObjList != null && dlObjList.size() > 0) {
                    DownLoadObject obj = dlObjList.get(0);
                    Message msg = callbackHandler.obtainMessage();
                    msg.what = 1;
                    msg.obj = obj;
                    callbackHandler.sendMessage(msg);
                    if (dlObjList.size() > 1) {
                        for (int i = 1; i < dlObjList.size(); i++) {
                            try {
                                Thread.sleep(300);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            obj = dlObjList.get(i);
                            msg = callbackHandler.obtainMessage();
                            msg.what = 1;
                            msg.obj = obj;
                            callbackHandler.sendMessage(msg);
                        }
                    }
                }
            }
        }).start();
    }

    private class DownLoadObject {
        public LinearLayout progressLine;
        public String id;
        public String remoteUrl;
        public PhotoView photoView;
        public int position;
    }

    private void attachProgressView(LinearLayout progressLine, String id,
                                    String remoteUrl, PhotoView photoView, int position) {
        CustomLog.d(TAG,"id:" + id + " | remoteUrl:" + remoteUrl + " | position:"
                + position);
        ProgressListener progressListener = progressListArray.get(position);
        if (progressListener != null) {
            progressListener.reBindWidget(progressLine, photoView);
        } else {
            progressListener = new ProgressListener(progressLine, photoView);
            progressListener.setParams(id, remoteUrl, position);
            progressListArray.put(position, progressListener);
            // fileTaskMgr中管理文件下载进度（已经开始了，绑定一下；未开始，则开始下载并绑定）
            if(collectionType ==0){
                MedicalApplication.getFileTaskManager().addSingleFileDownloadTask(
                        id, remoteUrl, true, progressListener);
            }else{
                PhotoBean bean = mListPhoto.get(position);
                if (mListPhoto.size() == 1) {
                    DownloadTaskManager.getInstance(this).downloadFile(id, "",
                            true, progressListener, 0);
                } else {
                    DownloadTaskManager.getInstance(this).downloadFile(id,
                            remoteUrl, true, progressListener, position);
                }
            }
        }
    }

    /**
     * <dl>
     * <dt>ViewPhotosActivity.java</dt>
     * <dd>Description:文件下载进度监控</dd>
     * <dd>Copyright: Copyright (C) 2014</dd>
     * <dd>Company: 安徽青牛信息技术有限公司</dd>
     * <dd>CreateDate: 2014-1-6 下午4:37:44</dd>
     * </dl>
     *
     * @author zhaguitao
     */
    private class ProgressListener extends ChangeUIInterface {
        private WeakReference<LinearLayout> progressLineReference;
        private WeakReference<PhotoView> photoViewReference;

        // private String id = "";
        // private String remoteUrl = "";
        private int position = -1;

        public void setParams(String id, String remoteUrl, int position) {
            // this.id = id;
            // this.remoteUrl = remoteUrl;
            this.position = position;
        }

        public ProgressListener(LinearLayout progressLine, PhotoView photoView) {
            progressLineReference = new WeakReference<LinearLayout>(
                    progressLine);
            photoViewReference = new WeakReference<PhotoView>(photoView);
        }

        public void reBindWidget(LinearLayout progressLine, PhotoView photoView) {
            progressLineReference = new WeakReference<LinearLayout>(
                    progressLine);
            photoViewReference = new WeakReference<PhotoView>(photoView);
        }

        public void onStart(FileTaskBean bean) {
            // 开始文件任务
            CustomLog.d(TAG,"onStart:" + position);

            final LinearLayout progressLine = progressLineReference.get();
            if (progressLine != null) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        progressLine.setVisibility(View.VISIBLE);
                    }
                });
            }
        }

        public void onProcessing(FileTaskBean bean, long current, long total) {
            // 文件任务进度
            CustomLog.d(TAG,"onProcessing:" + position + "|" + current + "/" + total);

            if (current < 0 || total <= 0) {
                CustomLog.d(TAG,"onProcessing:数据不合法，不做更新");
                return;
            }

            final float pro = current / (total * 1.0f);

            final LinearLayout progressLine = progressLineReference.get();
            if (progressLine != null) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        if (progressLine.getVisibility() != View.VISIBLE) {
                            progressLine.setVisibility(View.VISIBLE);
                        }
                        ProgressBar progressBar = (ProgressBar) progressLine
                                .findViewById(R.id.loading_progressbar);
                        TextView progressTxt = (TextView) progressLine
                                .findViewById(R.id.loading_txt);
                        NumberFormat numFormat = NumberFormat
                                .getNumberInstance();
                        numFormat.setMaximumFractionDigits(2);
                        String progressStr = numFormat.format(pro * 100);
                        numFormat.setMaximumFractionDigits(0);
                        int progressInt = Integer.parseInt(numFormat
                                .format(pro * 100));
                        progressBar.setProgress(progressInt);
                        progressTxt.setText(progressStr + "%");
                    }
                });
            }
        }

        public void onSuccess(FileTaskBean bean, final String result) {
            // 文件任务成功完成
            CustomLog.d(TAG,"onSuccess:" + position + "|" + result);

            final LinearLayout progressLine = progressLineReference.get();
            if (progressLine != null) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        progressLine.setVisibility(View.GONE);
                    }
                });
            }

            PhotoBean photoBean = mListPhoto.get(position);
            photoBean.setLocalPath(result);
            mListPhoto.set(position, photoBean);

            final PhotoView photoView = photoViewReference.get();
            if (photoView != null && mContext != null) {
                if (isVideoFile) {
                    // 自动开始播放
                    // 判断duration=0时，取默认的30s
                    // N7分享过来的视频消息中没有duration字段。防止直接播放时，len=0导致的进度条没反应
                    if (len == 0) {
                        len = 30;
                    }
                    Intent i = new Intent(mContext, PlayVideoActivity.class);
                    i.putExtra(PlayVideoActivity.SILENT_PLAY,true);
                    i.putExtra(RecordedVideoActivity.KEY_VIDEO_FILE_PATH,
                            result);
                    i.putExtra(RecordedVideoActivity.KEY_VIDEO_FILE_DURATION,
                            len);
                    mContext.startActivity(i);

                    ViewPhotosActivity.this.finish();
                } else {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            // 文件下载完成后显示原图
                            Glide.with(ViewPhotosActivity.this)
                                    .load(result)
                                    .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                                    .crossFade().into(photoView);
                        }
                    });
                }
            }
        }

        public void onFailure(FileTaskBean bean, Throwable error, String msg) {
            // 文件任务失败
            CustomLog.d(TAG,"onFailure:" + position + "|" + msg);

//			Toast.makeText(ViewPhotosActivity.this,
//					getString(R.string.try_again), Toast.LENGTH_SHORT).show();

            final LinearLayout progressLine = progressLineReference.get();
            if (progressLine != null) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(ViewPhotosActivity.this,
                                getString(R.string.try_again), Toast.LENGTH_SHORT).show();
                        progressLine.setVisibility(View.GONE);
                    }
                });
            }
        }
    }

    /**
     * @author: qn-lihs
     * @Title: savePhotoToSDCard
     * @Description: 保存当前的图片到SD卡
     * @date: 2013-10-30 下午2:21:45
     */
    private void savePhotoToSDCard() {
        if (mSize > 0) {
            try {
                final String fromFilePath = getFilePath();
                filePath = getDesFilePath(fromFilePath);
                if (!TextUtils.isEmpty(filePath)) {
                    if (new File(filePath).exists()) {
                        alertMassage(getString(R.string.picture_exist,
                                "\"" + PHOTO_DIR.getAbsolutePath() + "\""));
                        return;
                    }
                } else {
                    alertMassage(getString(R.string.copy_fail));
                    return;
                }
                new Thread(new Runnable() {

                    @Override
                    public void run() {
                        boolean isSuccess = saveLocalFile(fromFilePath);
                        Message msg = myHandler.obtainMessage();
                        msg.obj = isSuccess;
                        msg.what = 1;
                        myHandler.sendMessage(msg);
                    }
                }).start();
            } catch (Exception e) {
                CustomLog.e(TAG, e.toString());
                Message msg = myHandler.obtainMessage();
                msg.obj = false;
                msg.what = 1;
                myHandler.sendMessage(msg);
            }
        } else {
            CustomLog.d(TAG,"无法保存图片");
        }
    }

    private String getFilePath() {
        if (isRemoteFile) {
            return mListPhoto.get(mViewPager.getCurrentItem()).getLocalPath();
        } else {
            return mListImage.get(mViewPager.getCurrentItem());
        }
    }

    private Handler myHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1:
                    if ((Boolean) msg.obj) {
                        alertMassage(getString(R.string.save_picture_tip, filePath));
                        // 发广播刷新系统媒体库
                        IMCommonUtil.scanFileAsync(ViewPhotosActivity.this, filePath);
                    } else {
                        if (!TextUtils.isEmpty(filePath)) {
                            File file = new File(filePath);
                            if (file.exists()) {
                                file.delete();
                            }
                        }
                    }
                    break;
                default:
                    break;
            }
        }
    };

    private boolean saveLocalFile(String copyFilePath) {

        if (TextUtils.isEmpty(copyFilePath)) {
            return false;
        } else {
            CustomLog.d(TAG,"saveLocalFile 源文件路径：" + copyFilePath + "|" + "目标文件路径："
                    + filePath);
            FileOutputStream os = null;
            FileInputStream in = null;
            try {
                os = new FileOutputStream(filePath);
                in = new FileInputStream(copyFilePath);
                byte[] buffer = new byte[8 * 1024];
                int c = -1;
                if (in != null) {
                    while ((c = in.read(buffer)) > 0) {
                        os.write(buffer, 0, c);
                    }
                }
                os.flush();
                return true;
            } catch (OutOfMemoryError e) {
                CustomLog.d(TAG,"os.write and os.flush出现异常" + e.toString());
            } catch (Exception e) {
            } finally {
                try {
                    if (in != null) {
                        in.close();
                        os = null;
                    }
                    if (os != null) {
                        os.close();
                        os = null;
                    }
                } catch (Exception e2) {
                    CustomLog.d(TAG,"in.close and os.close出现异常" + e2.toString());
                }
            }
        }
        return false;
    }

    private String getDesFilePath(String fromPhotoPath) {

        if (TextUtils.isEmpty(fromPhotoPath)) {
            return "";
        }
        try {
            if (!PHOTO_DIR.exists()) {
                PHOTO_DIR.mkdirs();
            }
            String sourceFileName = fromPhotoPath.substring(fromPhotoPath
                    .lastIndexOf("/") + 1);

            return PHOTO_DIR.getAbsolutePath() + "/" + sourceFileName;
        } catch (Exception e) {

        }
        return "";
    }

    private void alertMassage(String id) {
        Toast.makeText(getApplicationContext(), id, Toast.LENGTH_SHORT).show();
    }

    // @Override
    // public void onActivityResult(int requestCode, int resultCode, Intent
    // data) {
    // if (Activity.RESULT_CANCELED == resultCode) {
    // return;
    // }
    //
    // switch (requestCode) {
    // case ACTION_SELECT_LINKMAN:
    // // 选择好联系人，转发消息
    // if (resultCode == Activity.RESULT_OK) {
    // if (data != null && data.getExtras() != null) {
    // Bundle selRes = data.getExtras();
    // ArrayList<String> receiverNumberList =
    // selRes.getStringArrayList(SelectLinkManActivity.START_RESULT_NUBE);
    // if (receiverNumberList!=null) {
    // if (receiverNumberList.size() > 0) {
    // String recieverNubeNumbers = StringUtil.list2String(receiverNumberList,
    // ';');
    // // 转发消息
    // PhotoBean forwardBean = mListPhoto.get(mViewPager.getCurrentItem());
    // NetPhoneApplication.getFileTaskManager().forwardMessage(recieverNubeNumbers,
    // forwardBean.getTaskId());
    // Toast.makeText(ViewPhotosActivity.this, getString(R.string.toast_sent),
    // Toast.LENGTH_SHORT).show();
    // // 返回到消息列表
    // finish();
    // }
    // }
    // }
    // }
    // break;
    // }
    // }

    private void selectContact() {

        PhotoBean forwardBean = mListPhoto.get(mViewPager.getCurrentItem());
        String forwardNoticeId = forwardBean.getTaskId();

        CustomLog.d(TAG,"从浏览图片页面转发图片:forwardNoticeId=" + forwardNoticeId);

        Intent i = new Intent(this, ShareLocalActivity.class);
        i.putExtra(ShareLocalActivity.KEY_ACTION_FORWARD, true);
        i.putExtra(ShareLocalActivity.MSG_ID, forwardNoticeId);
        if(collectionType ==1){
            i.putExtra(ShareLocalActivity.KEY_COLLECTION_FORWARD, 1);
            i.putExtra(ShareLocalActivity.KEY_COLLECTION_FORWARD_POS, selectedIndex);
        }
        startActivity(i);

        // 返回到消息列表
        if(collectionType ==0){
            finish();
        }
    }
}
