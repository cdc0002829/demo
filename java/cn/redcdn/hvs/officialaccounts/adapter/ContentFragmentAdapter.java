package cn.redcdn.hvs.officialaccounts.adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.display.RoundedBitmapDisplayer;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import cn.redcdn.datacenter.offaccscenter.data.MDSfocusOffAccArtcleInfo;
import cn.redcdn.hvs.MedicalApplication;
import cn.redcdn.hvs.R;
import cn.redcdn.hvs.contacts.contact.diaplayImageListener.DisplayImageListener;
import cn.redcdn.hvs.meeting.util.DateUtil;
import cn.redcdn.hvs.officialaccounts.activity.VideoPublishActivity;

import cn.redcdn.hvs.officialaccounts.listener.DingyueDisplayImageListener;
import cn.redcdn.log.CustomLog;

import static cn.redcdn.hvs.officialaccounts.activity.VideoPublishActivity.INTENT_DATA_ARTICLE_ID;

/**
 * Created by ${chenghb} on 2017/2/27.
 */

public class ContentFragmentAdapter extends RecyclerView.Adapter<ContentFragmentAdapter.ViewHolder> {
    private static final String TAG = ContentFragmentAdapter.class.getName();

    private Context mContext;

    public static final int LOADMORE = 0;
    public static final int NORMAL = 1;
    private int visibleItemCount;
    private int totalItemCount;
    private int firstVisibleItem;
    private boolean loading = false; //标识是否在做上滑加载更多.ture:正在执行； false：结束执行
    private List<MDSfocusOffAccArtcleInfo> focusList;
    private DingyueDisplayImageListener mDisplayImageListener = null;

    public ContentFragmentAdapter(List<MDSfocusOffAccArtcleInfo> list, Context context, RecyclerView recyclerView) {
        this.focusList = list;
        this.mContext = context;
        final LinearLayoutManager mLayoutManager;

    }


    class ViewHolder extends RecyclerView.ViewHolder {
        private LinearLayout content_liner;
        private TextView content_topic, content_brief, public_time, visit_count;
        private ImageView lock, content_Image;


        public ViewHolder(View itemView, int viewType) {
            super(itemView);
            init(itemView, viewType);
        }

        private void init(View itemView, int viewType) {
            visit_count = (TextView) itemView.findViewById(R.id.visit_count);
            content_liner = (LinearLayout) itemView.findViewById(R.id.content_liner);
            public_time = (TextView) itemView.findViewById(R.id.public_time);
            content_Image = (ImageView) itemView.findViewById(R.id.content_Image);
            lock = (ImageView) itemView.findViewById(R.id.lock);
            content_liner = (LinearLayout) itemView.findViewById(R.id.content_liner);
            content_topic = (TextView) itemView.findViewById(R.id.content_topic);
            content_brief = (TextView) itemView.findViewById(R.id.content_brief);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.content_recy_item, parent, false);


        return new ViewHolder(view, viewType);
    }

    ImageLoader imageLoader = ImageLoader.getInstance();

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {

        MDSfocusOffAccArtcleInfo byOffAcc = focusList.get(position);
        if (focusList != null) {
            holder.lock.setVisibility(View.VISIBLE);
            holder.content_brief.setText(focusList.get(position).getInstroduction());//文章简介
            CustomLog.e(TAG, "contentTAG" + focusList.get(position).getInstroduction());
            holder.content_topic.setText(focusList.get(position).getArticleTitle());//标题
            //获取当前时间
            Long currentTime = System.currentTimeMillis() / 1000;
            //获取发布时间
            Long publishTime = Long.valueOf(focusList.get(position).getPublishTime());
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd ");
            String times = sdf.format(new Date(publishTime * 1000L));
            if (currentTime - publishTime <= 86400) {
                if (currentTime - publishTime < 3600) {
                    holder.public_time.setText("刚刚");
                } else {
                    holder.public_time.setText((int) ((currentTime - publishTime) / 3600) + "" + "小时前");
                }
            } else {
                holder.public_time.setText(times);
            }

            if (focusList.get(position).getPlayCount() != 0) {
                holder.visit_count.setText("访问" + focusList.get(position).getPlayCount() + "" + "次");
                CustomLog.e(TAG, "accountNumber" + focusList.get(position).getPlayCount());
            }
            mDisplayImageListener = new DingyueDisplayImageListener();

            if (byOffAcc.getShowImgUrl() != "") {
                DisplayImageOptions options = new DisplayImageOptions.Builder()
                        .showStubImage(R.drawable.image)//设置图片在下载期间显示的图片
                        .showImageForEmptyUri(R.drawable.image)//设置图片Uri为空或是错误的时候显示的图片
                        .showImageOnFail(R.drawable.image)//设置图片加载/解码过程中错误时候显示的图片
                        .cacheInMemory(true)//是否緩存都內存中
                        .cacheOnDisc(true)//是否緩存到sd卡上
                        .displayer(new RoundedBitmapDisplayer(0))//设置图片的显示方式 : 设置圆角图片  int roundPixels
                        .bitmapConfig(Bitmap.Config.RGB_565)//设置为RGB565比起默认的ARGB_8888要节省大量的内存
                        .delayBeforeLoading(100)//载入图片前稍做延时可以提高整体滑动的流畅度
                        .build();
                imageLoader.displayImage(focusList.get(position).getShowImgUrl(),
                        holder.content_Image,
                        options,
                        mDisplayImageListener);
            }
            if (focusList.get(position).getIsEncipher() == 1) {
                holder.lock.setVisibility(View.INVISIBLE);
            } else {
                holder.lock.setImageResource(R.drawable.lock);
            }
            holder.content_liner.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //跳转到输入密码界面
                    Intent intent = new Intent(mContext, VideoPublishActivity.class);
                    intent.putExtra(INTENT_DATA_ARTICLE_ID, focusList.get(position).getArticleId());
                    mContext.startActivity(intent);
                }
            });

        }
    }


//    @Override
//    public int getItemViewType(int position) {
//        if (focusList.get(position) == null) {
//            return LOADMORE;
//        } else {
//            return NORMAL;
//        }
//    }

    @Override
    public int getItemCount() {
        return focusList.size();
    }
}
