package cn.redcdn.hvs.officialaccounts.adapter;

import android.content.Context;
import android.content.Intent;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.ImageLoader;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import cn.redcdn.datacenter.offaccscenter.data.ArtcleInfo;
import cn.redcdn.datacenter.offaccscenter.data.MDSRecommedOffaccInfo;
import cn.redcdn.hvs.MedicalApplication;
import cn.redcdn.hvs.R;
import cn.redcdn.hvs.contacts.contact.diaplayImageListener.DisplayImageListener;
import cn.redcdn.hvs.officialaccounts.DingYueActivity;
import cn.redcdn.hvs.officialaccounts.activity.OfficialMainActivity;
import cn.redcdn.hvs.officialaccounts.activity.VideoPublishActivity;
import cn.redcdn.hvs.officialaccounts.listener.DingyueDisplayImageListener;

import static cn.redcdn.hvs.officialaccounts.activity.VideoPublishActivity.INTENT_DATA_ARTICLE_ID;


/**
 * Created by ${chenghb} on 2017/2/25.
 */
public class RecommondRecyAdapter extends RecyclerView.Adapter<RecommondRecyAdapter.MyHolder> {
    private static final String TAG = RecommondRecyAdapter.class.getName();

    private Context mContext;
    public boolean loading;
    public List<MDSRecommedOffaccInfo> mRecommedList;
    private DingyueDisplayImageListener mDisplayImageListener = null;

    public RecommondRecyAdapter(List<MDSRecommedOffaccInfo> recommedList, Context context, RecyclerView recyclerView) {
        this.mRecommedList = recommedList;
        this.mContext = context;
        mDisplayImageListener = new DingyueDisplayImageListener();
    }


    class MyHolder extends RecyclerView.ViewHolder {
        public ImageView recommond_headImag; // 公众号头像
        public ImageView lock_third, lock_second, lock_first;
        public TextView officalname; // 公众号名称
        public TextView introduction; // 简介
        public TextView tv_content_frist; // 第一条信息
        public TextView tv_datatime_frist; //第一条信息的时间
        public TextView tv_content_second, tv_datatime_second,
                tv_content_third, tv_datatime_third;
        public LinearLayout btn_tomainPage;
        public LinearLayout linerlayout_content_frist, linerlayout_content_second, linerlayout_content_third;

        public MyHolder(View itemView, int viewType) {
            super(itemView);
            init(itemView);
        }

        private void init(View itemView) {
            recommond_headImag = (ImageView) itemView.findViewById(R.id.recommond_headImag);//公众号头像
            officalname = (TextView) itemView.findViewById(R.id.officalname);//公众号名字
            introduction = (TextView) itemView.findViewById(R.id.introduction);//公众号简介
            tv_content_frist = (TextView) itemView.findViewById(R.id.tv_content_frist);//第一条信息
            tv_datatime_frist = (TextView) itemView.findViewById(R.id.tv_datatime_frist);//第一条发布时间
            tv_content_second = (TextView) itemView.findViewById(R.id.tv_content_second);
            tv_datatime_second = (TextView) itemView.findViewById(R.id.tv_datatime_second);
            tv_content_third = (TextView) itemView.findViewById(R.id.tv_content_third);
            tv_datatime_third = (TextView) itemView.findViewById(R.id.tv_datatime_third);
            btn_tomainPage = (LinearLayout) itemView.findViewById(R.id.btn_tomainPage);
            linerlayout_content_frist = (LinearLayout) itemView.findViewById(R.id.linerlayout_content_frist);
            linerlayout_content_second = (LinearLayout) itemView.findViewById(R.id.linerlayout_content_second);
            linerlayout_content_third = (LinearLayout) itemView.findViewById(R.id.linerlayout_content_third);
            lock_first = (ImageView) itemView.findViewById(R.id.lock_first);
            lock_second = (ImageView) itemView.findViewById(R.id.lock_second);
            lock_third = (ImageView) itemView.findViewById(R.id.lock_third);
        }
    }


    @Override
    public MyHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.recommond_recyview_item, parent, false);
        return new MyHolder(view, viewType);
    }

    ImageLoader imageLoader = ImageLoader.getInstance();

    @Override
    public void onBindViewHolder(MyHolder holder, final int position) {
        if (mRecommedList != null) {
            final MDSRecommedOffaccInfo recommendOffAccs = mRecommedList.get(position);
            //设置公众号名字
            if (recommendOffAccs.getName() != null) {
                holder.officalname.setText(recommendOffAccs.getName());
            }
            //设置公众号简介
            if (recommendOffAccs.getIntroduction() != null) {
                holder.introduction.setText(recommendOffAccs.getIntroduction());
            }
            //加载公众号头像
            if (recommendOffAccs.getLogoUrl() != null) {
                imageLoader.displayImage(recommendOffAccs.getLogoUrl(),
                        holder.recommond_headImag,
                        MedicalApplication.shareInstance().options,
                        mDisplayImageListener);
            }
            //公众号头像
            holder.recommond_headImag.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent_card = new Intent();
                    intent_card.setClass(mContext, DingYueActivity.class);
                    intent_card.putExtra("officialAccountId", recommendOffAccs.getOffaccid());
                    intent_card.putExtra("officialName",recommendOffAccs.getName());
                    mContext.startActivity(intent_card);
                }
            });
            //跳转到公众号主页
            holder.btn_tomainPage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent_offi = new Intent(mContext, OfficialMainActivity.class);
                    intent_offi.putExtra("officialAccountId", recommendOffAccs.getOffaccid());//公众号id
                    intent_offi.putExtra("officialName",recommendOffAccs.getName());//公众号名称
                    mContext.startActivity(intent_offi);

                }
            });
            if (recommendOffAccs.getArtcleList() != null && recommendOffAccs.getArtcleList().size() > 0) {
                if (recommendOffAccs.getArtcleList().size() == 1) {
                    holder.linerlayout_content_frist.setVisibility(View.VISIBLE);
                    holder.lock_first.setVisibility(View.VISIBLE);
                    //artcleList = recommendOffAccs.getArtcleList();
                    holder.tv_content_frist.setText(recommendOffAccs.getArtcleList().get(0).getTitle());
                    holder.tv_datatime_frist.setText(recommendOffAccs.getArtcleList().get(0).getPublishTime());
                    //跳转到视频发布页 第一篇文章
                    holder.linerlayout_content_frist.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Intent intent = new Intent(mContext, VideoPublishActivity.class);
                            intent.putExtra(INTENT_DATA_ARTICLE_ID, recommendOffAccs.getArtcleList().get(0).getArticleId());
                            mContext.startActivity(intent);
                        }
                    });
                    if (recommendOffAccs.getArtcleList().get(0).getIsEncipher() == 1) {
                        holder.lock_first.setVisibility(View.INVISIBLE);
                    } else {
                        holder.lock_first.setImageResource(R.drawable.lock);
                    }
                    holder.linerlayout_content_second.setVisibility(View.GONE);
                    holder.linerlayout_content_third.setVisibility(View.GONE);
                                /*
                                * 第一篇文章的发布时间
                                * **/
                    //获取当前时间
                    Long currentTime = System.currentTimeMillis() / 1000;
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd ");
                    //获取发布时间
                    Long publishTime1 = Long.valueOf(recommendOffAccs.getArtcleList().get(0).getPublishTime());
                    String times1 = sdf.format(new Date(publishTime1 * 1000L));
                    if (currentTime - publishTime1 <= 86400) {
                        if (currentTime - publishTime1 < 3600) {
                            holder.tv_datatime_frist.setText("刚刚");
                        } else {
                            holder.tv_datatime_frist.setText((int) ((currentTime - publishTime1) / 3600) + "" + "小时前");
                        }
                    } else {
                        holder.tv_datatime_frist.setText(times1);
                    }
                } else if (recommendOffAccs.getArtcleList().size() == 2) {
                    holder.linerlayout_content_frist.setVisibility(View.VISIBLE);
                    holder.linerlayout_content_second.setVisibility(View.VISIBLE);
                    holder.lock_first.setVisibility(View.VISIBLE);
                    holder.lock_second.setVisibility(View.VISIBLE);

                    holder.tv_content_frist.setText(recommendOffAccs.getArtcleList().get(0).getTitle());
                    holder.tv_content_second.setText(recommendOffAccs.getArtcleList().get(1).getTitle());
                    //跳转到视频发布页 第一篇文章
                    holder.linerlayout_content_frist.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Intent intent = new Intent(mContext, VideoPublishActivity.class);
                            intent.putExtra(INTENT_DATA_ARTICLE_ID, recommendOffAccs.getArtcleList().get(0).getArticleId());
                            mContext.startActivity(intent);
                        }
                    });

                    //跳转到视频发布页 第二篇文章
                    holder.linerlayout_content_second.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Intent intent = new Intent(mContext, VideoPublishActivity.class);
                            intent.putExtra(INTENT_DATA_ARTICLE_ID, recommendOffAccs.getArtcleList().get(1).getArticleId());
                            mContext.startActivity(intent);
                        }
                    });
                    //获取当前时间
                    Long currentTime = System.currentTimeMillis() / 1000;
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd ");
                                   /*
                                * 第一篇文章的发布时间
                                * */
                    //获取发布时间
                    Long publishTime1 = Long.valueOf(recommendOffAccs.getArtcleList().get(0).getPublishTime());
                    String times1 = sdf.format(new Date(publishTime1 * 1000L));
                    if (currentTime - publishTime1 <= 86400) {
                        if (currentTime - publishTime1 < 3600) {
                            holder.tv_datatime_frist.setText("刚刚");
                        } else {
                            holder.tv_datatime_frist.setText((int) ((currentTime - publishTime1) / 3600) + "" + "小时前");
                        }
                    } else {
                        holder.tv_datatime_frist.setText(times1);
                    }
                    if (recommendOffAccs.getArtcleList().get(0).getIsEncipher() == 1) {
                        holder.lock_first.setVisibility(View.INVISIBLE);
                    } else {
                        holder.lock_first.setImageResource(R.drawable.lock);
                    }

                                  /*
                                * 第二篇文章的发布时间
                                * **/
                    Long publishTime2 = Long.valueOf(recommendOffAccs.getArtcleList().get(1).getPublishTime());
                    String times2 = sdf.format(new Date(publishTime2 * 1000L));
                    if (currentTime - publishTime2 <= 86400) {
                        if (currentTime - publishTime2 < 3600) {
                            holder.tv_datatime_second.setText("刚刚");
                        } else {
                            holder.tv_datatime_second.setText((int) ((currentTime - publishTime2) / 3600) + "" + "小时前");
                        }
                    } else {
                        holder.tv_datatime_second.setText(times2);
                    }

                    if (recommendOffAccs.getArtcleList().get(1).getIsEncipher() == 1) {
                        holder.lock_second.setVisibility(View.INVISIBLE);
                    } else {
                        holder.lock_second.setImageResource(R.drawable.lock);
                    }
                    holder.linerlayout_content_third.setVisibility(View.GONE);
                } else if (recommendOffAccs.getArtcleList().size() == 3) {
                    holder.linerlayout_content_frist.setVisibility(View.VISIBLE);
                    holder.linerlayout_content_second.setVisibility(View.VISIBLE);
                    holder.linerlayout_content_third.setVisibility(View.VISIBLE);
                    holder.lock_first.setVisibility(View.VISIBLE);
                    holder.lock_second.setVisibility(View.VISIBLE);
                    holder.lock_third.setVisibility(View.VISIBLE);
                    holder.tv_content_frist.setText(recommendOffAccs.getArtcleList().get(0).getTitle());
                    holder.tv_content_second.setText(recommendOffAccs.getArtcleList().get(1).getTitle());
                    holder.tv_datatime_third.setText(recommendOffAccs.getArtcleList().get(2).getTitle());
                    //跳转到视频发布页 第一篇文章
                    holder.linerlayout_content_frist.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Intent intent = new Intent(mContext, VideoPublishActivity.class);
                            intent.putExtra(INTENT_DATA_ARTICLE_ID, recommendOffAccs.getArtcleList().get(0).getArticleId());
                            mContext.startActivity(intent);
                        }
                    });

                    //跳转到视频发布页 第二篇文章
                    holder.linerlayout_content_second.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Intent intent = new Intent(mContext, VideoPublishActivity.class);
                            intent.putExtra(INTENT_DATA_ARTICLE_ID, recommendOffAccs.getArtcleList().get(1).getArticleId());
                            mContext.startActivity(intent);
                        }
                    });
                    //跳转到视频发布页 第三篇文章
                    holder.linerlayout_content_third.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Intent intent = new Intent(mContext, VideoPublishActivity.class);
                            intent.putExtra(INTENT_DATA_ARTICLE_ID, mRecommedList.get(position).getArtcleList().get(2).getArticleId());
                            mContext.startActivity(intent);
                        }
                    });
                    //获取当前时间
                    Long currentTime = System.currentTimeMillis() / 1000;
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd ");
                                   /*
                                * 第一篇文章的发布时间
                                * **/
                    //获取发布时间
                    Long publishTime1 = Long.valueOf(recommendOffAccs.getArtcleList().get(0).getPublishTime());
                    String times1 = sdf.format(new Date(publishTime1 * 1000L));
                    if (currentTime - publishTime1 <= 86400) {
                        if (currentTime - publishTime1 < 3600) {
                            holder.tv_datatime_frist.setText("刚刚");
                        } else {
                            holder.tv_datatime_frist.setText((int) ((currentTime - publishTime1) / 3600) + "" + "小时前");
                        }
                    } else {
                        holder.tv_datatime_frist.setText(times1);
                    }

                    if (recommendOffAccs.getArtcleList().get(0).getIsEncipher() == 1) {
                        holder.lock_first.setVisibility(View.INVISIBLE);
                    } else {
                        holder.lock_first.setImageResource(R.drawable.lock);
                    }

                    holder.tv_content_second.setText(recommendOffAccs.getArtcleList().get(1).getTitle());
                                  /*
                                * 第二篇文章的发布时间
                                * **/
                    Long publishTime2 = Long.valueOf(recommendOffAccs.getArtcleList().get(1).getPublishTime());
                    String times2 = sdf.format(new Date(publishTime2 * 1000L));
                    if (currentTime - publishTime2 <= 86400) {
                        if (currentTime - publishTime2 < 3600) {
                            holder.tv_datatime_second.setText("刚刚");
                        } else {
                            holder.tv_datatime_second.setText((int) ((currentTime - publishTime2) / 3600) + "" + "小时前");
                        }
                    } else {
                        holder.tv_datatime_second.setText(times2);
                    }


                    if (recommendOffAccs.getArtcleList().get(1).getIsEncipher() == 1) {
                        holder.lock_second.setVisibility(View.INVISIBLE);
                    } else {
                        holder.lock_second.setImageResource(R.drawable.lock);
                    }

                    holder.tv_content_third.setText(recommendOffAccs.getArtcleList().get(2).getTitle());
                                  /*
                                * 第三篇文章的发布时间
                                * **/
                    Long publishTime3 = Long.valueOf(recommendOffAccs.getArtcleList().get(2).getPublishTime());
                    String times3 = sdf.format(new Date(publishTime3 * 1000L));
                    if (currentTime - publishTime3 <= 86400) {
                        if (currentTime - publishTime3 < 3600) {
                            holder.tv_datatime_third.setText("刚刚");
                        } else {
                            holder.tv_datatime_third.setText((int) ((currentTime - publishTime3) / 3600) + "" + "小时前");
                        }
                    } else {
                        holder.tv_datatime_third.setText(times3);
                    }


                    if (recommendOffAccs.getArtcleList().get(2).getIsEncipher() == 1) {
                        holder.lock_third.setVisibility(View.INVISIBLE);
                    } else {
                        holder.lock_third.setImageResource(R.drawable.lock);
                    }
                }
            } else {
                holder.linerlayout_content_frist.setVisibility(View.GONE);
                holder.linerlayout_content_second.setVisibility(View.GONE);
                holder.linerlayout_content_third.setVisibility(View.GONE);

            }
        }
    }


    @Override
    public int getItemCount() {
        return mRecommedList.size();
    }

}
