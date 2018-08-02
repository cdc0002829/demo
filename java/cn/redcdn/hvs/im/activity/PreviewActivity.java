package cn.redcdn.hvs.im.activity;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import cn.redcdn.hvs.R;
import cn.redcdn.hvs.base.BaseActivity;
import cn.redcdn.hvs.im.util.IMCommonUtil;
import cn.redcdn.hvs.im.util.ViewPages;
import cn.redcdn.log.CustomLog;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.butel.connectevent.utils.CommonUtil;
import com.butel.connectevent.utils.LogUtil;
import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * <dl>
 * <dt>PreviewActivity.java</dt>
 * <dd>Description:照片/视频预览界面</dd>
 * <dd>Copyright: Copyright (C) 2014</dd>
 * <dd>Company: 安徽青牛信息技术有限公司</dd>
 * <dd>CreateDate: 2014-3-17 上午9:55:20</dd>
 * </dl>
 *
 * @author zhaguitao
 */
public class PreviewActivity extends BaseActivity {

    private static final String TAG = "PreviewActivity";
    //back flag--区别backBtn返回和发送结束返回--在相片页判断是否刷新选中状态
    public static final String BACK_CODE="back_code";
    public static final String RETURN_WITHOUT_SEND="return_without_send";
    public static final String RETURN_AFTER_SEND="return_after_send";

    //    public static String ACTIVITY_FLAG = "activity_flag";
    //    public static String AVITVITY_START_FOR_RESULT = "activity_strat_for_result";
    /***********start**/
    public static final String KEY_IS_VIDEO = "key_is_video";
    public static final String KEY_SELECTED_CNT = "key_selected_cnt";
    public static final String KEY_ACTION_CUSTOM = "action_custom";
    public static final String KEY_FOR_SHARE_TYPE = "key_for_sharetype";

    // 跳转页面传入nube号码KEY
    public static final String KEY_FOR_NUBE_NUMBER = "key_for_nube_number";
    public static final String KEY_FILE_PATH = "file_path";
    public static final int SHARE_TYPE_PIC = 0;
    public static final int SHARE_TYPE_VIDEO = 1;
    public static final int SHARE_TYPE_AUDIO = 2;
    /***********end**/
    public static String SELECTED_IMG_INDEX = "selected_img_index";

    private ArrayList<String> mListPhotoPath = null;
    private int shareType = 0;
    private int mIndexImage = 0;

    private String recieverNumber = null;

    private ImageView videoTag = null;
    private TextView mTextViewInfo = null;
    private Button mBntShare = null;
    private TextView shareAccount;

    // 界面初始化时，全部选择状态
    private SparseBooleanArray selectStatus = new SparseBooleanArray();

    private LinearLayout galleryView = null;
    private GridView gridview = null;
    private GridItemAdapter gridAdapter = null;
    private HorizontalScrollView scrollView = null;
    private ViewPages mViewPager = null;
    private RelativeLayout mImageContainer = null;
    private SamplePagerAdapter mAdapter = null;

    // private ImageFetcher mShareImageFetcher = null;

    // private int bigImgSize = 0;
    private static long MAX_FILE_SIZE = 15 * 1024 * 1024;
    public static long MAX_IMAGEFILE_SIZE = 2 * 1024 * 1024;

    private TextView fileSize = null;

    private int thumbnailSize = 0;
    private int thumbnailSpacing = 0;

    private int account;

    private CheckBox selectedStatusCb = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // logBegin();

        setContentView(R.layout.preview_layout);

        initData();

        initControl();

        if (shareType == SHARE_TYPE_PIC) {
            getTitleBar().setTitle(getString(R.string.preview_picture));
        } else if (shareType == SHARE_TYPE_VIDEO) {
            getTitleBar().setTitle(getString(R.string.preview_vedio));
        }
        // logEnd();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        CustomLog.d("TAG","onConfigurationChanged");
    }

    private void setLinearLayoutWidth(int count, LinearLayout linearlayout) {
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) linearlayout
            .getLayoutParams();
        params.width = (thumbnailSize + thumbnailSpacing) * count;
        linearlayout.setLayoutParams(params);
    }

    private void initData() {
        // bigImgSize = getResources().getDimensionPixelSize(
        // R.dimen.share_big_img_size);
        thumbnailSize = getResources().getDimensionPixelSize(
            R.dimen.image_thumbnail_size);
        thumbnailSpacing = getResources().getDimensionPixelSize(
            R.dimen.gallery_column_spacing);

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (null != extras) {
            //            activityFlag = extras.getString(ACTIVITY_FLAG);
            mIndexImage = extras.getInt(SELECTED_IMG_INDEX, 0);
            shareType = extras.getInt(KEY_FOR_SHARE_TYPE,
                0);
            recieverNumber = extras
                .getString(KEY_FOR_NUBE_NUMBER);
            localAction(extras);
        }

        account = mListPhotoPath.size();
    }

    private void setAccount() {
        if (account <= 0) {
            shareAccount.setVisibility(View.GONE);
            mBntShare.setClickable(false);
            mBntShare.setTextColor(getResources().getColor(
                R.color.img_choose_text_disable_color));
        } else {
            mBntShare.setClickable(true);
            mBntShare.setTextColor(getResources().getColor(
                R.color.img_choose_text_enable_color));

            if (View.GONE == shareAccount.getVisibility()) {
                shareAccount.setVisibility(View.VISIBLE);
            }
            Animation scaleAnimation = AnimationUtils.loadAnimation(this,
                R.anim.img_selected_cnt_anim);
            shareAccount.startAnimation(scaleAnimation);
            shareAccount.setText(String.valueOf(account));
        }
    }

    private void initControl() {
        // logBegin();
        getTitleBar().enableBack();
        // 返回时，保存mListPhotoPath
        getTitleBar().setBack(null, new OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                Intent intent = getIntent();
                // 根据预览页对图片的编辑修改mListPhotoPath
                for (int i = mListPhotoPath.size() - 1; i >= 0; i--) {
                    if (!selectStatus.get(i)) {
                        mListPhotoPath.remove(i);
                    }
                }
                intent.putStringArrayListExtra(Intent.EXTRA_STREAM,
                    mListPhotoPath);
                intent.putExtra(BACK_CODE, RETURN_WITHOUT_SEND);
                setResult(RESULT_OK, intent);
                finish();
            }
        });

        selectedStatusCb = new CheckBox(this);
        LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT);
        selectedStatusCb.setLayoutParams(lp);
        selectedStatusCb.setBackgroundResource(0);
        selectedStatusCb.setButtonDrawable(R.drawable.img_select_cb_selector);
        selectedStatusCb.setChecked(true);
        selectedStatusCb
            .setOnCheckedChangeListener(new OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView,
                                             boolean isChecked) {
                    int curIdx = mViewPager.getCurrentItem();
                    CustomLog.d("TAG","选中状态改变:" + curIdx + "|" + isChecked);
                    if (selectStatus.get(curIdx) != isChecked) {
                        selectStatus.put(curIdx, isChecked);
                        if (isChecked) {
                            account++;
                        } else {
                            account--;
                        }
                        setAccount();
                    }
                }
            });
        getTitleBar().addCustomRightView(selectedStatusCb);

        fileSize = (TextView) this.findViewById(R.id.file_size);
        videoTag = (ImageView) this.findViewById(R.id.tag_video);

        scrollView = (HorizontalScrollView) this.findViewById(R.id.scroll_view);
        mImageContainer = (RelativeLayout) findViewById(R.id.image_container);

        mTextViewInfo = (TextView) findViewById(R.id.text_info);
        shareAccount = (TextView) findViewById(R.id.share_account);
        mBntShare = (Button) findViewById(R.id.bnt_share_photos);

        if (MultiBucketChooserActivity.fromType == MultiBucketChooserActivity.FROM_TYPE_SEND) {
            mBntShare.setText(R.string.send_message);
        } else if (MultiBucketChooserActivity.fromType == MultiBucketChooserActivity.FROM_TYPE_COLLECT) {
            mBntShare.setText(R.string.collect_str);
        }

        setAccount();

        if (shareType == SHARE_TYPE_VIDEO) {
            videoTag.setVisibility(View.VISIBLE);
            videoTag.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    CustomLog.d(TAG,"点击视频按钮");
                    if (CommonUtil.isFastDoubleClick()) {
                        return;
                    }
                    IMCommonUtil.playVideo(getBaseContext(),
                        mListPhotoPath.get(mViewPager.getCurrentItem()));
                }
            });
        } else {
            videoTag.setVisibility(View.INVISIBLE);
        }

        mViewPager = new ViewPages(this);
        mImageContainer.addView(mViewPager, 0);
        mAdapter = new SamplePagerAdapter();
        mAdapter.setItems(mListPhotoPath);
        mViewPager.setAdapter(mAdapter);
        mViewPager.setOnPageChangeListener(pageListener);
        mBntShare.setOnClickListener(shareClickListener);

        if (mListPhotoPath != null && mListPhotoPath.size() > 0) {
            gridview = (GridView) this.findViewById(R.id.image_grid);
            galleryView = (LinearLayout) this.findViewById(R.id.gallery_parent);

            setLinearLayoutWidth(mListPhotoPath.size(), galleryView);
            gridAdapter = new GridItemAdapter(getBaseContext());
            gridview.setAdapter(gridAdapter);
            gridAdapter.setData(mListPhotoPath);
            gridview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> arg0, View arg1,
                                        int arg2, long arg3) {
                    CustomLog.d(TAG,"点击每个缩略图");
                    int preIndex = mIndexImage;
                    mIndexImage = arg2;
                    showImage(preIndex);
                }
            });

            if (mListPhotoPath.size() == 1) {
                scrollView.setVisibility(View.GONE);
                mTextViewInfo.setVisibility(View.INVISIBLE);
            }
            showImage(-1);
        }
        // logEnd();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // logBegin();

        // if (mShareImageFetcher != null) {
        // mShareImageFetcher.setExitTasksEarly(false);
        // }
        // logEnd();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // logBegin();

        // if (mShareImageFetcher != null) {
        // mShareImageFetcher.setPauseWork(false);
        // mShareImageFetcher.setExitTasksEarly(true);
        // mShareImageFetcher.flushCache();
        // }
        // logEnd();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // logBegin();
        //
        // logEnd();
    }

    private void localAction(Bundle bundle) {
        if (null == mListPhotoPath)
            mListPhotoPath = new ArrayList<String>();

        if (null != bundle) {
            if (bundle.containsKey(Intent.EXTRA_STREAM)) {
                try {
                    mListPhotoPath = bundle
                        .getStringArrayList(Intent.EXTRA_STREAM);
                    if (mListPhotoPath != null && mListPhotoPath.size() > 0) {
                        for (int i = 0; i < mListPhotoPath.size(); i++) {
                            selectStatus.put(i, true);
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, e.toString());
                }
            }
        }
    }

    private void showImage(int preIndex) {
        displayIndex(preIndex);
        mViewPager.setCurrentItem(mIndexImage);
    }

    private void displayIndex(int preIndex) {
        // logBegin();
        long size = getFileSize(mListPhotoPath.get(mIndexImage));
        if (size == 0 || size > MAX_FILE_SIZE) {
            fileSize.setTextColor(Color.RED);
        } else {
            fileSize.setTextColor(getResources().getColor(
                R.color.share_txt_color));
        }
        if (size == 0) {
            fileSize.setText(R.string.file_no_exist);
        } else {
            fileSize.setText(getFileSizeString(size));
        }
        mTextViewInfo.setText("" + (mIndexImage + 1) + "/"
            + mListPhotoPath.size());

        if (View.VISIBLE == scrollView.getVisibility()) {

            if (gridview != null && gridAdapter != null) {
                if (preIndex < mListPhotoPath.size() && preIndex > -1) {
                    setThumbnailSelected(preIndex, false);
                }
                setThumbnailSelected(mIndexImage, true);
            }
            if (gridAdapter != null) {
                gridAdapter.setSeclected(mIndexImage);
            }
        }
        // logEnd();
    }

    private void setThumbnailSelected(int index, boolean selected) {
        View preView = gridview.getChildAt(index);
        if (preView != null) {
            View thumbnail_layout = preView.findViewById(R.id.thumbnail_layout);
            if (selected) {
                thumbnail_layout.setBackgroundColor(getResources().getColor(
                    R.color.grid_select_bg));
            } else {
                thumbnail_layout.setBackgroundColor(getResources().getColor(
                    R.color.share_pic_bg));
            }
        }
    }

    OnClickListener shareClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            // logD("点击分享按钮");
            if (CommonUtil.isFastDoubleClick()) {
                return;
            }
            if (mListPhotoPath == null || mListPhotoPath.size() == 0) {
                Toast.makeText(getApplicationContext(), "没有选择文件，无法分享",
                    Toast.LENGTH_SHORT).show();
                CustomLog.d("TAG","没有选择文件，无法分享");
                return;
            }

            // 将选择结果传至分享界面
            for (int i = mListPhotoPath.size() - 1; i >= 0; i--) {
                if (!selectStatus.get(i)) {
                    mListPhotoPath.remove(i);
                }
            }

            // 确定
            Intent intent = getIntent();
            intent.putStringArrayListExtra(Intent.EXTRA_STREAM,
                mListPhotoPath);
            intent.putExtra(BACK_CODE, RETURN_AFTER_SEND);
            setResult(RESULT_OK, intent);
            finish();


            //            if (AVITVITY_START_FOR_RESULT.equals(activityFlag)) {
            //                // 分享界面预览照片
            //                Intent intent = getIntent();
            //                intent.putStringArrayListExtra(Intent.EXTRA_STREAM,
            //                        mListPhotoPath);
            //                setResult(RESULT_OK, intent);
            //                finish();
            //            } else if (MultiBucketChooserActivity.ACTIVITY_START_FOR_RESULT
            //                    .equals(MultiBucketChooserActivity.activityType)) {
            //                // 分享界面添加照片
            //                Intent intent = new Intent();
            //                intent.putStringArrayListExtra(Intent.EXTRA_STREAM,
            //                        mListPhotoPath);
            //                setResult(RESULT_OK, intent);
            //                finish();
            //            } else {
            //                String umengKey = "";
            //                if (TextUtils.isEmpty(recieverNumber)) {
            //                    // nubenumber为空时，从家信界面分享
            //                    if (shareType == ShareConfirmActivity.SHARE_TYPE_PIC) {
            //                        umengKey = CommonConstant.UMENG_KEY_N_share_photo_pick;
            //                    } else {
            //                        umengKey = CommonConstant.UMENG_KEY_N_Share_Video_pick;
            //                    }
            //                } else {
            //                    // nubenumber不为空时，从联系人详情界面分享
            //                    if (shareType == ShareConfirmActivity.SHARE_TYPE_PIC) {
            //                        umengKey = CommonConstant.UMENG_KEY_Share_photos_pick;
            //                    } else {
            //                        umengKey = CommonConstant.UMENG_KEY_Share_Video_pick;
            //                    }
            //                }
            //                MobclickAgent.onEvent(PreviewActivity.this, umengKey);
            //                Bundle bundle = new Bundle();
            //                bundle.putStringArrayList(Intent.EXTRA_STREAM, mListPhotoPath);
            //                bundle.putBoolean(ShareConfirmActivity.KEY_ACTION_CUSTOM, true);
            //                bundle.putInt(ShareConfirmActivity.KEY_FOR_SHARE_TYPE,
            //                        shareType);
            //                bundle.putString(ShareConfirmActivity.KEY_FOR_NUBE_NUMBER,
            //                        recieverNumber);
            //                Intent intent = new Intent(PreviewActivity.this,
            //                        ShareConfirmActivity.class);
            //                intent.putExtras(bundle);
            //                startActivity(intent);
            //
            //                finishSelectPic();
            //            }
        }
    };

    private void finishSelectPic() {
        //        if (MultiBucketChooserActivity.bucketChooserActivity != null) {
        //            MultiBucketChooserActivity.bucketChooserActivity.finish();
        //        }
        // if (MultiImageChooserActivity.mltiImageChooserActivity != null) {
        //     MultiImageChooserActivity.mltiImageChooserActivity.finish();
        // }
        finish();
    }

    private long getFileSize(String path) {
        long size = 0;
        File file = new File(path);
        if (file != null && file.exists()) {
            size = file.length();
            file = null;
        }
        return size;
    }

    private String getFileSizeString(double size) {
        String sizeString = "";
        if (size < 1024) {
            sizeString = size + " B";
        } else {
            BigDecimal b = null;
            double cutsize = 0;
            if (size >= 1024 * 1024) {
                cutsize = size / (1024 * 1024);
                b = new BigDecimal(cutsize);
                double f1 = b.setScale(2, BigDecimal.ROUND_HALF_UP)
                    .doubleValue();
                sizeString = f1 + " MB";
            } else {
                cutsize = size / (1024);
                b = new BigDecimal(cutsize);
                double f2 = b.setScale(2, BigDecimal.ROUND_HALF_UP)
                    .doubleValue();
                sizeString = f2 + " KB";
            }
        }
        return sizeString;
    }

    public class GridItemAdapter extends BaseAdapter {

        private LayoutInflater layoutInflater = null;
        private List<String> imagePath = new ArrayList<String>();
        private int selectedNo = 0;

        public GridItemAdapter(Context context) {
            this.layoutInflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        public void setData(List<String> path) {
            this.imagePath.clear();
            if (path != null) {
                imagePath.addAll(path);
            }
            this.notifyDataSetChanged();
        }

        public void setSeclected(int index) {
            this.selectedNo = index;
        }

        public int getCount() {
            return imagePath.size();
        }

        @Override
        public String getItem(int position) {
            return imagePath.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            convertView = (View) layoutInflater.inflate(
                R.layout.share_grid_item, parent, false);
            // logBegin();
            View thumbnail_layout = convertView
                .findViewById(R.id.thumbnail_layout)	;
            ImageView thumbnail = (ImageView) convertView
                .findViewById(R.id.thumbnail_image);
            ImageView thumbnail_tag = (ImageView) convertView
                .findViewById(R.id.thumbnail_video);
            ImageView thumbnail_close = (ImageView) convertView
                .findViewById(R.id.thumbnail_image_close);
            thumbnail_close.setVisibility(View.INVISIBLE);

            if (shareType == SHARE_TYPE_PIC) {
                // mImageFetcher.loadThumbnail(getItem(position), thumbnail,
                // ImageFetcher.THUMBNAIL_TYPE_IMAGE);
                Glide.with(PreviewActivity.this).load(getItem(position))
                    .placeholder(R.drawable.empty_photo)
                    .error(R.drawable.empty_photo).centerCrop()
                    .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                    .crossFade()
                    .into(thumbnail);
                //                mImageFetcher.loadImageThumb(thumbnail, getItem(position),
                //                        ImageFetcher.THUMBNAIL_TYPE_IMAGE, null);
                thumbnail_tag.setVisibility(View.INVISIBLE);
            } else if (shareType == SHARE_TYPE_VIDEO) {
                // mImageFetcher.loadThumbnail(getItem(position), thumbnail,
                // ImageFetcher.THUMBNAIL_TYPE_VIDEO);
                Glide.with(PreviewActivity.this).load(getItem(position))
                    .placeholder(R.drawable.empty_photo)
                    .error(R.drawable.empty_photo).centerCrop()
                    .crossFade().into(thumbnail);
                //                mImageFetcher.loadImageThumb(thumbnail, getItem(position),
                //                        ImageFetcher.THUMBNAIL_TYPE_VIDEO, null);
                thumbnail_tag.setVisibility(View.VISIBLE);
            } else if (shareType == SHARE_TYPE_AUDIO) {
                thumbnail.setImageResource(R.drawable.default_music);
                thumbnail_tag.setVisibility(View.INVISIBLE);
            }

            if (selectedNo == position) {
                thumbnail_layout.setBackgroundColor(getResources().getColor(
                    R.color.grid_select_bg));
            } else {
                thumbnail_layout.setBackgroundColor(getResources().getColor(
                    R.color.share_pic_bg));
            }
            // logEnd();
            return convertView;
        }

    }

    OnPageChangeListener pageListener = new OnPageChangeListener() {

        @Override
        public void onPageSelected(int arg0) {
            CustomLog.d(TAG,"pageListener onPageSelected:" + arg0);
            if (View.VISIBLE == scrollView.getVisibility()) {
                scrollView.scrollTo((thumbnailSize + thumbnailSpacing)
                    * (mIndexImage - 2), scrollView.getScrollY());
            }

            // 显示右上角checkbox状态
            boolean shouldChecked = selectStatus.get(arg0);
            if (selectedStatusCb.isChecked() != shouldChecked) {
                selectedStatusCb.setChecked(shouldChecked);
            }
        }

        @Override
        public void onPageScrolled(int arg0, float arg1, int arg2) {
        }

        @Override
        public void onPageScrollStateChanged(int arg0) {
            CustomLog.d(TAG,"pageListener onPageScrollStateChanged:" + arg0);
            int preIndex = mIndexImage;
            mIndexImage = mViewPager.getCurrentItem();
            displayIndex(preIndex);
        }
    };

    public class SamplePagerAdapter extends PagerAdapter {
        private List<String> mListLog = null;

        public void setItems(List<String> logs) {
            mListLog = logs;
        }

        @Override
        public int getCount() {
            if (null == mListLog)
                return 0;
            return mListLog.size();
        }

        @Override
        public View instantiateItem(ViewGroup container, int position) {

            String photo = mListLog.get(position);
            ImageView photoView = new ImageView(container.getContext());
            // TODO:点击视频，调用系统播放器播放
            // photoView.setTag(photo);
            // photoView.setOnClickListener(new OnClickListener() {
            // @Override
            // public void onClick(View v) {
            // if (shareType == ShareConfirmActivity.SHARE_TYPE_VIDEO) {
            // CommonUtil.playVideo(getBaseContext(),
            // (String) v.getTag());
            // }
            // }
            // });

            try {
                LogUtil.d("ShareActivity src:" + photo);

                if (shareType == SHARE_TYPE_PIC) {
                    // mShareImageFetcher.loadImage(photo, photoView,
                    // mShareImageFetcher.getImageWidth());
                    Glide.with(PreviewActivity.this).load(photo)
                        .placeholder(R.drawable.empty_photo)
                        .error(R.drawable.empty_photo).centerCrop()
                        .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                        .crossFade().into(photoView);
                    //                    mImageFetcher.loadLocalImage(photoView, photo,
                    //                            AndroidUtil.getDeviceSize(PreviewActivity.this).x,
                    //                            AndroidUtil.getDeviceSize(PreviewActivity.this).y,
                    //                            null);
                } else if (shareType == SHARE_TYPE_VIDEO) {
                    Glide.with(PreviewActivity.this).load(photo)
                        .placeholder(R.drawable.empty_photo)
                        .error(R.drawable.empty_photo).centerCrop()
                        .crossFade().into(photoView);
                    //                    mImageFetcher.loadImageThumb(photoView, photo,
                    //                            ImageFetcher.THUMBNAIL_TYPE_VIDEO, null);
                }
            } catch (Exception e) {
                CustomLog.e("ShareActivity 异常", String.valueOf(e));
            }
            container.addView(photoView, 0, new LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT));
            return photoView;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            // logBegin();
            container.removeView((View) object);
            // logEnd();
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }
    }
}
