package cn.redcdn.hvs.im.activity;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.util.SparseBooleanArray;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import cn.redcdn.hvs.R;
import cn.redcdn.hvs.util.CommonUtil;
import cn.redcdn.log.CustomLog;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.butel.connectevent.utils.LogUtil;
import java.util.ArrayList;

/**
 * <dl>
 * <dt>MultiImageChooserActivity.java</dt>
 * <dd>Description:图片选择界面（支持多选）</dd>
 * <dd>Copyright: Copyright (C) 2011</dd>
 * <dd>Company: 安徽青牛信息技术有限公司</dd>
 * <dd>CreateDate: 2013-9-10 下午3:18:17</dd>
 * </dl>
 *
 * @author zhaguitao
 */
public class MultiBucketChooserActivity extends
    AbstractMediaFolderChooserActivity {
    private static final String TAG = "MultiBucketChooserActivity";

    private static final int REQUEST_CODE_PICKER = 1236;

    /** 一次最多发送照片数 */
    public static final int MAX_IMAGE_COUNT = 9;

    public static final String KEY_ACTIVITY_TYPE = "key_activity_type";
    public static final String ACTIVITY_START_FOR_RESULT = "activity_start_for_result";

    public static final String KEY_BUCKET_TYPE = "key_bucket_type";
    public static final int BUCKET_TYPE_IMAGE = 1;
    public static final int BUCKET_TYPE_VIDEO = 2;

    public static final String KEY_FROM_TYPE = "key_from_type";
    public static final int FROM_TYPE_SEND = 1;
    public static final int FROM_TYPE_COLLECT = 2;

    public static final String KEY_NUBENUMBER = "key_nubenumber";
    public static final String KEY_BUCKETID = "key_bucketid";

    /** 列名：相册照片数 */
    private static final String COLUMN_BUCKET_CNT = "BUCKET_CNT";

    //    public static Activity bucketChooserActivity = null;
    //    public static String activityType = null;

    public static int fromType = FROM_TYPE_SEND;

    // 照片墙grid
    private GridView gridView;
    // 照片墙列数
    private static final int IMAGE_COLUMN = 2;
    private static final int IMAGE_COLUMN_LANDSCAPE = 4;

    // 图片适配器
    private BucketAdapter ba;
    private Cursor bucketCursor;
    // 列索引
    private int imageIdColumnIndex;
    private int bucketIdColumnIndex;
    private int bucketNameColumnIndex;
    private int bucketCntColumnIndex;
    // 图片尺寸
    private int imgSize = 240;

    private boolean shouldRequestThumb = true;

    // 选择状态
    private SparseBooleanArray checkStatus = new SparseBooleanArray();

    private LoaderCallbacks<Cursor> loaderCallbacks = null;

    private String nubenumber = "";

    private int bucketBgMargin = 20;

    // bucket类型，默认为图片
    private int bucketType = BUCKET_TYPE_IMAGE;
    // 相册对应的封面照片id
    private SparseIntArray bucketImageIds = new SparseIntArray();
    // 分享界面已选照片数
    private int shareSelectedCnt = 0;


    @Override
    protected int getContentView() {
        return R.layout.select_multi_bucket;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        LogUtil.begin("");
        super.onCreate(savedInstanceState);

        shareSelectedCnt = getIntent().getIntExtra(
            PreviewActivity.KEY_SELECTED_CNT, 0);

        nubenumber = getIntent().getStringExtra(KEY_NUBENUMBER);

        bucketType = getIntent()
            .getIntExtra(KEY_BUCKET_TYPE, BUCKET_TYPE_IMAGE);

        fromType = getIntent()
            .getIntExtra(KEY_FROM_TYPE, FROM_TYPE_SEND);

//        titleBar.enableBack();
        titleBar.enableRightBtn("取消", 0, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MultiBucketChooserActivity.this.finish();
            }
        });
        if (bucketType == BUCKET_TYPE_VIDEO) {
            titleBar.setTitle(getString(R.string.video_select_title));
        } else {
            titleBar.setTitle(getString(R.string.image_select_title));
        }

        bucketBgMargin = getResources().getDimensionPixelSize(
            R.dimen.multi_bucket_chooser_bg_margin);
        imgSize = getResources().getDimensionPixelSize(
            R.dimen.multi_bucket_chooser_size);

        gridView = (GridView) findViewById(R.id.gridview);
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            gridView.setNumColumns(IMAGE_COLUMN);
        } else {
            gridView.setNumColumns(IMAGE_COLUMN_LANDSCAPE);
        }

        gridView.setColumnWidth(imgSize);
        gridView.setOnItemClickListener(this);
        gridView.setOnScrollListener(new OnScrollListener() {

            private int lastFirstItem = 0;
            private long timestamp = System.currentTimeMillis();


            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                if (scrollState == SCROLL_STATE_IDLE) {
                    LogUtil.d("MultiImageChooserActivity IDLE - Reload!");
                    shouldRequestThumb = true;
                    ba.notifyDataSetChanged();
                } else if (scrollState == SCROLL_STATE_FLING) {
                    LogUtil.d("MultiBucketChooserActivity 列表正在滚动...");
                    // list列表滚动过程中，暂停图片上传下载
                } else {
                }
            }


            @Override
            public void onScroll(AbsListView view, int firstVisibleItem,
                                 int visibleItemCount, int totalItemCount) {
                float dt = System.currentTimeMillis() - timestamp;
                if (firstVisibleItem != lastFirstItem) {
                    double speed = 1 / dt * 1000;
                    lastFirstItem = firstVisibleItem;
                    timestamp = System.currentTimeMillis();
                    LogUtil.d("MultiImageChooserActivity Speed: " + speed
                        + " elements/second");

                    shouldRequestThumb = speed < visibleItemCount;
                }
            }
        });

        ba = new BucketAdapter(this);
        gridView.setAdapter(ba);

        LoaderManager.enableDebugLogging(false);

        loaderCallbacks = new LoaderCallbacks<Cursor>() {
            @Override
            public void onLoaderReset(Loader<Cursor> loader) {
                bucketCursor.close();
                bucketCursor = null;
            }


            @Override
            public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
                CustomLog.d(TAG, "onLoadFinished");
                if (cursor == null) {
                    // NULL cursor. This usually means there's no image database
                    // yet....
                    return;
                }

                bucketImageIds.clear();

                switch (loader.getId()) {
                    case BUCKET_TYPE_IMAGE:
                        bucketCursor = cursor;
                        imageIdColumnIndex = bucketCursor
                            .getColumnIndex(MediaStore.Images.Media._ID);
                        //                    imageDataColIdx = bucketCursor
                        //                            .getColumnIndex(MediaStore.Images.Media.DATA);
                        bucketIdColumnIndex = bucketCursor
                            .getColumnIndex(MediaStore.Images.Media.BUCKET_ID);
                        bucketNameColumnIndex = bucketCursor
                            .getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME);
                        bucketCntColumnIndex = bucketCursor
                            .getColumnIndex(COLUMN_BUCKET_CNT);
                        ba.notifyDataSetChanged();
                        break;
                    case BUCKET_TYPE_VIDEO:
                        bucketCursor = cursor;
                        imageIdColumnIndex = bucketCursor
                            .getColumnIndex(MediaStore.Video.Media._ID);
                        bucketIdColumnIndex = bucketCursor
                            .getColumnIndex(MediaStore.Video.Media.BUCKET_ID);
                        bucketNameColumnIndex = bucketCursor
                            .getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME);
                        bucketCntColumnIndex = bucketCursor
                            .getColumnIndex(COLUMN_BUCKET_CNT);
                        ba.notifyDataSetChanged();
                        break;
                    default:
                        break;
                }
            }


            @Override
            public Loader<Cursor> onCreateLoader(int cursorID, Bundle arg1) {
                CursorLoader cl = null;

                ArrayList<String> img = new ArrayList<String>();
                String order = null;
                String selection = null;
                switch (cursorID) {

                    case BUCKET_TYPE_IMAGE:
                        img.add(MediaStore.Images.Media._ID);
                        img.add(MediaStore.Images.Media.DATA);
                        img.add(MediaStore.Images.Media.BUCKET_ID);
                        img.add(MediaStore.Images.Media.BUCKET_DISPLAY_NAME);
                        img.add("COUNT(" + "distinct " + MediaStore.Images.Media.DATA
                            + ") AS " + COLUMN_BUCKET_CNT);
                        order = MediaStore.Images.Media.DATE_MODIFIED + " DESC ";
                        selection = MediaStore.Images.Media.SIZE
                            + " > 0) GROUP BY ("
                            + MediaStore.Images.Media.BUCKET_ID;

                        cl = new CursorLoader(MultiBucketChooserActivity.this,
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            img.toArray(new String[img.size()]), selection,
                            null, order);
                        break;
                    case BUCKET_TYPE_VIDEO:
                        img.add(MediaStore.Video.Media._ID);
                        img.add(MediaStore.Video.Media.DATA);
                        img.add(MediaStore.Video.Media.BUCKET_ID);
                        img.add(MediaStore.Video.Media.BUCKET_DISPLAY_NAME);
                        img.add("COUNT(" + "distinct " + MediaStore.Images.Media.DATA
                            + ") AS " + COLUMN_BUCKET_CNT);
                        order = MediaStore.Video.Media.DATE_MODIFIED + " desc ";
                        selection = " 0 == 0) GROUP BY ("
                            + MediaStore.Video.Media.BUCKET_ID;

                        cl = new CursorLoader(MultiBucketChooserActivity.this,
                            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                            img.toArray(new String[img.size()]), selection,
                            null, order);
                        break;
                    default:
                        break;
                }
                return cl;
            }
        };
        getSupportLoaderManager().initLoader(bucketType, null, loaderCallbacks);
        LogUtil.end("");
    }


    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        LogUtil.begin("");
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            gridView.setNumColumns(IMAGE_COLUMN_LANDSCAPE);
        } else {
            gridView.setNumColumns(IMAGE_COLUMN);
        }
        ba.notifyDataSetChanged();
        LogUtil.end("");
    }


    @Override
    protected void onResume() {
        LogUtil.begin("");
        super.onResume();

        LogUtil.end("");
    }


    @Override
    protected void onPause() {
        LogUtil.begin("");
        super.onPause();

        LogUtil.end("");
    }


    @Override
    protected void onDestroy() {
        LogUtil.begin("");
        super.onDestroy();
        getSupportLoaderManager().destroyLoader(bucketType);

        //        bucketChooserActivity = null;
        //        activityType = null;
        LogUtil.end("");
    }

    //    @Override
    //    protected boolean needRefresh() {
    //        return true;
    //    }


    @Override
    protected int getDataCnt() {
        if (bucketCursor == null) {
            return 0;
        } else {
            return bucketCursor.getCount();
        }
    }


    @Override
    protected void clickPosition(int position) {
        if (CommonUtil.isFastDoubleClick()) {
            return;
        }
        bucketCursor.moveToPosition(position);
        String bucketId = bucketCursor.getString(bucketIdColumnIndex);

        CustomLog.d(TAG,"选择的 bucketId=" + bucketId);
        Intent i = new Intent(this, MultiImageChooserActivity.class);
        if (!TextUtils.isEmpty(nubenumber)) {
            i.putExtra(KEY_NUBENUMBER, nubenumber);
        }
        i.putExtra(KEY_BUCKETID, bucketId);
        if (bucketType == BUCKET_TYPE_VIDEO) {
            i.putExtra(MultiImageChooserActivity.KEY_CHOOSER_TYPE,
                MultiImageChooserActivity.CHOOSER_TYPE_VIDEO);
            CustomLog.d(TAG, "选择的是视频相册");
        } else {
            i.putExtra(MultiImageChooserActivity.KEY_CHOOSER_TYPE,
                MultiImageChooserActivity.CHOOSER_TYPE_IMAGE);
            CustomLog.d(TAG, "选择的是图片相册");
        }
        //      if (ACTIVITY_START_FOR_RESULT.equals(activityType)) {
        i.putExtra(PreviewActivity.KEY_SELECTED_CNT, shareSelectedCnt);
        startActivityForResult(i, REQUEST_CODE_PICKER);
        //    } else {
        //        startActivity(i);
        //    }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        LogUtil.d("onActivityResult");
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_CODE_PICKER) {
                LogUtil.d("onActivityResult REQUEST_CODE_PICKER");
                setResult(RESULT_OK, data);
                MultiBucketChooserActivity.this.finish();
            }
        }
    }


    @Override
    protected int getMaxCnt() {
        return MAX_IMAGE_COUNT;
    }


    public class BucketAdapter extends BaseAdapter {
        private LayoutInflater layoutInflater = null;
        private GalleryViewHolder viewHolder = null;


        public BucketAdapter(Context c) {
            this.layoutInflater = (LayoutInflater) c
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }


        public int getCount() {
            if (bucketCursor != null) {
                return bucketCursor.getCount();
            } else {
                return 0;
            }
        }


        public Object getItem(int position) {
            return position;
        }


        public long getItemId(int position) {
            return position;
        }


        // create a new ImageView for each item referenced by the Adapter
        public View getView(int pos, View convertView, ViewGroup parent) {

            if (convertView == null) {
                convertView = layoutInflater.inflate(
                    R.layout.multi_bucket_chooser_item, parent, false);
                viewHolder = new GalleryViewHolder();
                viewHolder.imageBg = (ImageView) convertView
                    .findViewById(R.id.image_bg);
                viewHolder.imageTarget = (ImageView) convertView
                    .findViewById(R.id.image_target);
                viewHolder.videoIcon = (ImageView) convertView
                    .findViewById(R.id.video_icon);
                if (bucketType == BUCKET_TYPE_VIDEO) {
                    viewHolder.videoIcon.setVisibility(View.VISIBLE);
                }

                viewHolder.bucketName = (TextView) convertView
                    .findViewById(R.id.bucket_name);
                // viewHolder.bucketCount = (TextView)
                // convertView.findViewById(R.id.bucket_count);

                viewHolder.selectedBg = (TextView) convertView
                    .findViewById(R.id.selected_bg);
                viewHolder.checkboxImg = (ImageView) convertView
                    .findViewById(R.id.checkbox_img);

                convertView.setTag(viewHolder);
            } else {
                viewHolder = (GalleryViewHolder) convertView.getTag();
            }

            //            viewHolder.imageTarget
            //                    .setBackgroundResource(R.drawable.empty_photo);
            viewHolder.selectedBg.setVisibility(View.GONE);
            viewHolder.checkboxImg.setVisibility(View.GONE);

            final int position = pos;
            if (!bucketCursor.moveToPosition(position)) {
                return convertView;
            }
            if (imageIdColumnIndex == -1) {
                return convertView;
            }

            final int bucketCount = Integer.parseInt(bucketCursor.getString(bucketCntColumnIndex));


            viewHolder.bucketName
                .setText(bucketCursor.getString(bucketNameColumnIndex)
                    + "(" + bucketCount + ")");

            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) viewHolder.imageBg
                .getLayoutParams();
            if (bucketCount > 1) {
                lp.leftMargin = 0;
                lp.topMargin = 0;
                lp.rightMargin = 0;
                lp.bottomMargin = 0;
                viewHolder.imageBg.setLayoutParams(lp);
                viewHolder.imageBg.setImageResource(R.drawable.bucket_img_bg);
            } else {
                lp.leftMargin = bucketBgMargin;
                lp.topMargin = bucketBgMargin;
                lp.rightMargin = bucketBgMargin;
                lp.bottomMargin = bucketBgMargin;
                viewHolder.imageBg.setLayoutParams(lp);
                viewHolder.imageBg.setImageResource(android.R.color.white);
            }

            //            int id = bucketCursor.getInt(imageIdColumnIndex);
            int bucketId = bucketCursor.getInt(bucketIdColumnIndex);

            int id = bucketImageIds.get(bucketId);
            if (id <= 0) {
                Cursor cursor = null;
                try {
                    if (bucketType == BUCKET_TYPE_VIDEO) {
                        cursor = getContentResolver().query(
                            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                            new String[] { MediaStore.Video.Media._ID },
                            MediaStore.Video.Media.BUCKET_ID + " = ? and "
                                + MediaStore.Video.Media.SIZE + ">0 ",
                            new String[] { "" + bucketId },
                            MediaStore.Video.Media.DATE_MODIFIED + " desc "
                                + " LIMIT 0,1");
                    } else {
                        cursor = getContentResolver().query(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            new String[] { MediaStore.Images.Media._ID },
                            MediaStore.Images.Media.BUCKET_ID + " = ? and "
                                + MediaStore.Images.Media.SIZE + ">0 ",
                            new String[] { "" + bucketId },
                            MediaStore.Images.Media.DATE_MODIFIED
                                + " desc " + " LIMIT 0,1");
                    }
                    if (cursor != null && cursor.getCount() > 0) {
                        cursor.moveToFirst();
                        id = cursor.getInt(0);
                    }
                } catch (Exception e) {
                    LogUtil.e("图片选择异常", e);
                } finally {
                    if (cursor != null) {
                        cursor.close();
                        cursor = null;
                    }
                }
                bucketImageIds.put(bucketId, id);
            }

            if (shouldRequestThumb) {
                if (bucketType == BUCKET_TYPE_VIDEO) {
                    //                    mImageFetcher.loadThumbnail(id, viewHolder.imageTarget,
                    //                            ImageFetcher.THUMBNAIL_TYPE_VIDEO);
                    String vedioPath = getVideoPathById(
                        MultiBucketChooserActivity.this, id);
                    Glide.with(MultiBucketChooserActivity.this).load(vedioPath)
                        .placeholder(R.drawable.empty_photo)
                        .error(R.drawable.empty_photo).centerCrop()
                        .crossFade()
                        .into(viewHolder.imageTarget);
                    //                    mImageFetcher.loadImageThumb(viewHolder.imageTarget, id,
                    //                            ImageFetcher.THUMBNAIL_TYPE_VIDEO, null);
                } else {
                    //                    mImageFetcher.loadThumbnail(id, viewHolder.imageTarget,
                    //                            ImageFetcher.THUMBNAIL_TYPE_IMAGE);
                    String imagePath = getImagePathById(
                        MultiBucketChooserActivity.this, id);
                    Glide.with(MultiBucketChooserActivity.this).load(imagePath)
                        .placeholder(R.drawable.empty_photo)
                        .error(R.drawable.empty_photo).centerCrop()
                        .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                        .crossFade()
                        .into(viewHolder.imageTarget);
                    //                    mImageFetcher.loadImageThumb(viewHolder.imageTarget, id,
                    //                            ImageFetcher.THUMBNAIL_TYPE_IMAGE, null);
                }
            }
            // TODO:使用id作为key来记住选择状态，否则数据刷新后，可能会导致选择状态混乱
            if (isChecked(pos)) {
                viewHolder.selectedBg.setVisibility(View.VISIBLE);
                viewHolder.checkboxImg.setVisibility(View.VISIBLE);
            }

            return convertView;
        }
    }


    public boolean isChecked(int position) {
        return checkStatus.get(position);
    }


    private static class GalleryViewHolder {
        //         FrameLayout bucketBg;
        ImageView imageBg;
        ImageView imageTarget;
        ImageView videoIcon;
        TextView bucketName;
        // TextView bucketCount;

        TextView selectedBg;
        ImageView checkboxImg;
    }


    @Override
    protected int getCursorId() {
        return bucketType;
    }


    protected LoaderCallbacks<Cursor> getLoaderCallbacks() {
        return loaderCallbacks;
    }


    @Override
    protected void registerContentObserver(MyContentObserver observer) {
        LogUtil.d("");
        if (bucketType == BUCKET_TYPE_VIDEO) {
            LogUtil.d("bucketType == BUCKET_TYPE_VIDEO");
            getContentResolver()
                .registerContentObserver(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI, true,
                    observer);
            getContentResolver().registerContentObserver(
                MediaStore.Video.Thumbnails.EXTERNAL_CONTENT_URI, true,
                observer);
        } else {
            LogUtil.d("bucketType == BUCKET_TYPE_IMAGE");
            getContentResolver().registerContentObserver(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true,
                observer);
            getContentResolver().registerContentObserver(
                MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI, true,
                observer);
        }
    }


    @Override
    protected boolean needContentObserver() {
        return false;
    }


    public static String getImagePathById(Context ctx, int imageId) {

        Cursor cursor = null;
        try {
            cursor = ctx.getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                new String[] { MediaStore.Images.Media.DATA },
                MediaStore.Images.Media._ID + " = ?",
                new String[] { "" + imageId },
                null);
            if (cursor != null && cursor.getCount() > 0) {
                cursor.moveToFirst();
                return cursor.getString(0);
            }
        } catch (Exception e) {
            LogUtil.e("Exception", e);
        } finally {
            if (cursor != null) {
                cursor.close();
                cursor = null;
            }
        }
        return "";
    }


    public static String getVideoPathById(Context ctx, int videoId) {

        Cursor cursor = null;
        try {
            cursor = ctx.getContentResolver().query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                new String[] { MediaStore.Video.Media.DATA },
                MediaStore.Video.Media._ID + " = ?",
                new String[] { "" + videoId },
                null);
            if (cursor != null && cursor.getCount() > 0) {
                cursor.moveToFirst();
                return cursor.getString(0);
            }
        } catch (Exception e) {
            LogUtil.e("Exception", e);
        } finally {
            if (cursor != null) {
                cursor.close();
                cursor = null;
            }
        }

        return "";
    }
}
