package cn.redcdn.hvs.officialaccounts.adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.nfc.Tag;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.display.RoundedBitmapDisplayer;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import cn.redcdn.datacenter.offaccscenter.data.MDSfocusOffAccArtcleInfo;
import cn.redcdn.hvs.MedicalApplication;
import cn.redcdn.hvs.R;
import cn.redcdn.hvs.contacts.contact.diaplayImageListener.DisplayImageListener;
import cn.redcdn.hvs.im.view.RoundImageView;
import cn.redcdn.hvs.officialaccounts.DingYueActivity;
import cn.redcdn.hvs.officialaccounts.activity.VideoPublishActivity;
import cn.redcdn.hvs.officialaccounts.listener.DingyueDisplayImageListener;
import cn.redcdn.hvs.officialaccounts.activity.OfficialMainActivity;
import cn.redcdn.hvs.util.CustomToast;
import cn.redcdn.log.CustomLog;

import static cn.redcdn.hvs.officialaccounts.activity.VideoPublishActivity.INTENT_DATA_ARTICLE_ID;

/**
 * Created by ${chenghb} on 2017/2/24.
 */
public class OrderFragmentRecyAdapter extends RecyclerView.Adapter<OrderFragmentRecyAdapter.MyViewHolder> {
    private static final String TAG = OrderFragmentRecyAdapter.class.getName();
    private Context mContext;
    private boolean loading = false; //标识是否在做上滑加载更多.ture:正在执行； false：结束执行
    public List<MDSfocusOffAccArtcleInfo> focusList;
    private DingyueDisplayImageListener mDisplayImageListener = null;
    public OrderFragmentRecyAdapter(List<MDSfocusOffAccArtcleInfo> focusList, Context context, RecyclerView recyclerView) {
        this.focusList = focusList;
        this.mContext = context;
    }



    class MyViewHolder extends RecyclerView.ViewHolder {
        private LinearLayout btn_toMainPage;
        private LinearLayout Liner_content;
        private TextView title, content, public_time, mvisit_count, official_name;
        private ImageView content_Image, lock;
        private RoundImageView officical_head;
        private LinearLayout order_linerLayout;

        public MyViewHolder(View itemView, int viewType) {
            super(itemView);
            init(itemView, viewType);
        }

        private void init(View itemView, int viewType) {
                    mvisit_count = (TextView) itemView.findViewById(R.id.ordervisit_count);
                    lock = (ImageView) itemView.findViewById(R.id.lock);//锁
                    public_time = (TextView) itemView.findViewById(R.id.public_time);
                    btn_toMainPage = (LinearLayout) itemView.findViewById(R.id.btn_tomainPage);
                    Liner_content = (LinearLayout) itemView.findViewById(R.id.Liner_content);
                    official_name = (TextView) itemView.findViewById(R.id.official_name);//公众号名字
                    officical_head = (RoundImageView) itemView.findViewById(R.id.officical_head);//公众号logo
                    title = (TextView) itemView.findViewById(R.id.title);//文章题目
                    content = (TextView) itemView.findViewById(R.id.content);//文章内容
                    content_Image = (ImageView) itemView.findViewById(R.id.content_Image);//公众号图片
                    order_linerLayout = (LinearLayout) itemView.findViewById(R.id.order_linerLayout);
        }
    }


    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.offical_order_list_view, parent, false);

        return new MyViewHolder(view, viewType);
    }


    ImageLoader imageLoader = ImageLoader.getInstance();

    @Override
    public void onBindViewHolder(final MyViewHolder holder, final int position) {
        CustomLog.e(TAG,"Position"+position);
        CustomLog.e(TAG,"ItemCount"+OrderFragmentRecyAdapter.this.getItemCount());
                mDisplayImageListener = new DingyueDisplayImageListener();
                holder.lock.setVisibility(View.VISIBLE);
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
                holder.mvisit_count.setText("访问" + focusList.get(position).getPlayCount() + "" + "次");//访问次数
                holder.official_name.setText(focusList.get(position).getOffaccName());//公众号名字
                holder.content.setText(focusList.get(position).getInstroduction());//文章内容
                holder.title.setText(focusList.get(position).getArticleTitle());//文章题目
                //公众号头像
                imageLoader.displayImage(focusList.get(position).getOffaccLoginUrl(),
                        holder.officical_head,
                        MedicalApplication.shareInstance().options,
                        mDisplayImageListener);
                CustomLog.e(TAG, focusList.get(position).getOffaccLoginUrl());
                DisplayImageOptions options2 = new DisplayImageOptions.Builder()
                        .showStubImage(R.drawable.image)//设置图片在下载期间显示的图片
                        .showImageForEmptyUri(R.drawable.image)//设置图片Uri为空或是错误的时候显示的图片
                        .showImageOnFail(R.drawable.image)//设置图片加载/解码过程中错误时候显示的图片
                        .cacheInMemory(true)//是否緩存都內存中
                        .cacheOnDisc(true)//是否緩存到sd卡上
                        .displayer(new RoundedBitmapDisplayer(0))//设置图片的显示方式 : 设置圆角图片  int roundPixels
                        .bitmapConfig(Bitmap.Config.RGB_565)//设置为RGB565比起默认的ARGB_8888要节省大量的内存
                        .delayBeforeLoading(100)//载入图片前稍做延时可以提高整体滑动的流畅度
                        .build();
                //公众号图片
                imageLoader.displayImage(focusList.get(position).getShowImgUrl(),
                        holder.content_Image,
                        options2,
                        mDisplayImageListener);
                CustomLog.e(TAG, "showImagUrl" + focusList.get(position).getShowImgUrl());
                if (focusList.get(position).getIsEncipher() == 1) {
                    holder.lock.setVisibility(View.INVISIBLE);
                } else if (focusList.get(position).getIsEncipher() == 2) {
                    holder.lock.setImageResource(R.drawable.lock);
                }

                holder.Liner_content.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(MedicalApplication.getContext(), VideoPublishActivity.class);
                        intent.putExtra(INTENT_DATA_ARTICLE_ID, focusList.get(position).getArticleId());
                        CustomLog.e(TAG, "ArticleId" + focusList.get(position).getArticleId());
                        mContext.startActivity(intent);
                    }
                });
                //跳转到公众号主页
                holder.btn_toMainPage.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent_public = new Intent(mContext, OfficialMainActivity.class);
                        intent_public.putExtra("officialAccountId", focusList.get(position).getOffaccId());
                        intent_public.putExtra("officialName",focusList.get(position).getOffaccName());
                        CustomLog.e(TAG,"AdapterofficialName"+focusList.get(position).getOffaccName());

                        mContext.startActivity(intent_public);
                    }
                });
                //跳转到公众号名片页
                holder.officical_head.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent_card = new Intent(MedicalApplication.getContext(), DingYueActivity.class);
                        intent_card.putExtra("officialAccountId", focusList.get(position).getOffaccId());
                        intent_card.putExtra("officialName",focusList.get(position).getOffaccName());
                        //Toast.makeText(mContext, "id" + focusList.get(position).getOffaccId(), Toast.LENGTH_SHORT).show();
                        mContext.startActivity(intent_card);
                    }
                });
    }


    @Override
    public int getItemCount() {
        return focusList.size();
    }
}
