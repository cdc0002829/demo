package cn.redcdn.hvs.profiles.activity;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.jcodecraeer.xrecyclerview.XRecyclerView;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import cn.redcdn.datacenter.collectcenter.CollectionInfo;
import cn.redcdn.datacenter.collectcenter.DataBodyInfo;
import cn.redcdn.datacenter.collectcenter.DeleteCollectItems;
import cn.redcdn.datacenter.collectcenter.GetCollectItems;
import cn.redcdn.datacenter.medicalcenter.data.MDSAccountInfo;
import cn.redcdn.hvs.AccountManager;
import cn.redcdn.hvs.MedicalApplication;
import cn.redcdn.hvs.R;
import cn.redcdn.hvs.base.BaseActivity;
import cn.redcdn.hvs.im.fileTask.FileTaskManager;
import cn.redcdn.hvs.im.manager.CollectionManager;
import cn.redcdn.hvs.im.view.CommonDialog;
import cn.redcdn.hvs.im.view.CommonDialog.BtnClickedListener;
import cn.redcdn.hvs.officialaccounts.activity.VideoPublishActivity;
import cn.redcdn.hvs.profiles.adapter.CollectionListAdapter;
import cn.redcdn.hvs.util.CustomToast;
import cn.redcdn.hvs.util.TitleBar;
import cn.redcdn.log.CustomLog;

import static cn.redcdn.datacenter.medicalcenter.MDSErrorCode.MDS_TOKEN_DISABLE;
import static cn.redcdn.hvs.officialaccounts.activity.VideoPublishActivity.INTENT_DATA_ARTICLE_ID;

/**
 * Created by Administrator on 2017/2/24.
 */

public class CollectionActivity extends BaseActivity implements CollectionListAdapter.CollectionDataListCallBack {
    public static final int IMAGE_TYPE = 2;
    public static final int VEDIO_TYPE = 3;
    public static final int AUDIO_TYPE = 7;
    public static final int WORD_TYPE = 8;
    public static final int ARTICAL_TYPE = 30;
    private XRecyclerView recyclerView;
    private CollectionListAdapter mCollectionListAdapter;
    List<DataBodyInfo> mCollectionInfo;
    private RelativeLayout collectionDataList1;
    private RelativeLayout collectionNoData;


    public static final String KEY_RECEIVER = "key_receive";
    private boolean isComeIM = false;
    private String mReceive = "";
    private GetCollectItems mGetCollectItems;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_collection);
        mCollectionListAdapter = new CollectionListAdapter(this);
        initDataFirst();
        initView();
        TitleBar titleBar = getTitleBar();
        titleBar.setTitle("收藏夹");

        if (isComeIM){
            titleBar.setBackText("取消");
        }else {
            titleBar.enableBack();
        }
    }

    private void initDataFirst(){
        mReceive = getIntent().getStringExtra(KEY_RECEIVER);
        if (mReceive != null && mReceive.length() > 0) {
            isComeIM = true;
            CustomLog.d(TAG, "IM 跳转过来");
        } else {
            isComeIM = false;
            CustomLog.d(TAG, "其他activity 跳转过来");
        }
        mGetCollectItems = new GetCollectItems() {
            @Override
            protected void onSuccess(List<CollectionInfo> responseContent) {
                super.onSuccess(responseContent);
                removeLoadingView();
                mCollectionInfo = new ArrayList<>();
                for (int i = 0; i < responseContent.size(); i++) {
                    CollectionInfo collectionInfo = responseContent.get(i);
                    int type = collectionInfo.getType();
                    List<DataBodyInfo> dataList = collectionInfo.getDataList();
                    for (int j = 0; j < dataList.size(); j++) {
                        DataBodyInfo dataBodyInfo = dataList.get(j);
                        dataBodyInfo.setType(type);
                        mCollectionInfo.add(dataBodyInfo);
                    }
                }
                mCollectionListAdapter.setData(mCollectionInfo);
                recyclerView.refreshComplete();
            }

            @Override
            protected void onFail(int statusCode, String statusInfo) {
                super.onFail(statusCode, statusInfo);
                removeLoadingView();
                if (statusCode == MDS_TOKEN_DISABLE) {
                    AccountManager.getInstance(CollectionActivity.this).tokenAuthFail(statusCode);
                } else {
                    CustomToast.show(CollectionActivity.this, statusInfo, Toast.LENGTH_LONG);
                }
            }
        };

        MDSAccountInfo userInfo = AccountManager.getInstance(MedicalApplication.getContext()).getAccountInfo();
        mGetCollectItems.getCollectionItem(userInfo.getNube(), userInfo.getAccessToken(), 0);
        CollectionActivity.this.showLoadingView("加载数据中", new DialogInterface.OnCancelListener() {

            @Override
            public void onCancel(DialogInterface dialog) {
                dialog.dismiss();
                mGetCollectItems.cancel();
                CollectionActivity.this.finish();
                CustomToast.show(getApplicationContext(), "取消加载", Toast.LENGTH_SHORT);
            }
        });
    };

    private void initData() {

        mReceive = getIntent().getStringExtra(KEY_RECEIVER);
        if (mReceive != null && mReceive.length() > 0) {
            isComeIM = true;
            CustomLog.d(TAG, "IM 跳转过来");
        } else {
            isComeIM = false;
            CustomLog.d(TAG, "其他activity 跳转过来");
        }
        mGetCollectItems = new GetCollectItems() {
            @Override
            protected void onSuccess(List<CollectionInfo> responseContent) {
                super.onSuccess(responseContent);
                mCollectionInfo = new ArrayList<>();
                for (int i = 0; i < responseContent.size(); i++) {
                    CollectionInfo collectionInfo = responseContent.get(i);
                    int type = collectionInfo.getType();
                    List<DataBodyInfo> dataList = collectionInfo.getDataList();
                    for (int j = 0; j < dataList.size(); j++) {
                        DataBodyInfo dataBodyInfo = dataList.get(j);
                        dataBodyInfo.setType(type);
                        mCollectionInfo.add(dataBodyInfo);
                    }
                }
                mCollectionListAdapter.setData(mCollectionInfo);
                recyclerView.refreshComplete();
            }

            @Override
            protected void onFail(int statusCode, String statusInfo) {
                super.onFail(statusCode, statusInfo);
                if (statusCode == MDS_TOKEN_DISABLE) {
                    AccountManager.getInstance(CollectionActivity.this).tokenAuthFail(statusCode);
                } else {
                    CustomToast.show(CollectionActivity.this, statusInfo, Toast.LENGTH_LONG);
                }
            }
        };

        MDSAccountInfo userInfo = AccountManager.getInstance(MedicalApplication.getContext()).getAccountInfo();
        mGetCollectItems.getCollectionItem(userInfo.getNube(), userInfo.getAccessToken(), 0);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mGetCollectItems.cancel();
    }

    private View inflate;
    private TextView choosePhoto;
    private TextView takePhoto;
    private Dialog dialog;

    private void mShowDialog(final DataBodyInfo data) {
        dialog = new Dialog(this, R.style.ActionSheetDialogStyle);
        //填充对话框的布局
        inflate = LayoutInflater.from(this).inflate(R.layout.delete_dialog, null);
        //初始化控件
        choosePhoto = (TextView) inflate.findViewById(R.id.dy_tv);
        takePhoto = (TextView) inflate.findViewById(R.id.cancle_tv);
        choosePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                DeleteCollectItems deleteCollectItems = new DeleteCollectItems() {
                    @Override
                    protected void onSuccess(JSONObject responseContent) {
                        super.onSuccess(responseContent);
                        mCollectionInfo.remove(data);
                        mCollectionListAdapter.setData(mCollectionInfo);
                        CollectionManager.getInstance().deleteCollectionById(data.getCollectionId());
                        CustomToast.show(getApplicationContext(), "删除收藏成功", 5000);
                    }

                    @Override
                    protected void onFail(int statusCode, String statusInfo) {
                        super.onFail(statusCode, statusInfo);
                        CustomToast.show(getApplicationContext(), "删除收藏失败", 5000);
                    }
                };
                String id = data.getCollectionId();
                String nube = AccountManager.getInstance(CollectionActivity.this)
                        .getAccountInfo().getNube();
                String accessToken = AccountManager.getInstance(CollectionActivity.this)
                        .getAccountInfo().getAccessToken();
                deleteCollectItems.deleteCollectionItems(nube ,id,accessToken);
                dialog.dismiss();
            }
        });
        takePhoto.setOnClickListener(mbtnHandleEventListener);
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

            case R.id.cancle_tv:
                dialog.dismiss();
                break;
        }
    }


    private void initView() {
        collectionDataList1 = (RelativeLayout) findViewById(R.id.collection_data_list1);
        collectionNoData = (RelativeLayout) findViewById(R.id.collection_no_data);
        recyclerView = (XRecyclerView) findViewById(R.id.collection_recyclerview);
        LinearLayoutManager mLinearLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(mLinearLayoutManager);
        recyclerView.setAdapter(mCollectionListAdapter);
        recyclerView.setLoadingListener(new XRecyclerView.LoadingListener() {
            @Override
            public void onRefresh() {
                initData();
            }

            @Override
            public void onLoadMore() {
                recyclerView.loadMoreComplete();
            }
        });
        mCollectionListAdapter.setOnItemClickListener(new CollectionListAdapter.OnRecyclerViewItemClickListener() {
            @Override

            public void onItemClick(View view, DataBodyInfo data) {
                if (isComeIM) {
                    CustomLog.d(TAG, "发送收藏消息，直接返回");
                    showForwardDialog(data);
                    return;
                }
                switch (data.getType()) {
                    case WORD_TYPE:
                        Intent intentWordActivity = new Intent();
                        intentWordActivity.setClass(getApplicationContext(), CollectionWordActivity.class);
                        intentWordActivity.putExtra(CollectionWordActivity.COLLECTION_TEXT_DATA, data);
                        startActivity(intentWordActivity);
                        break;
                    case IMAGE_TYPE:
                        Intent intentImageActivity = new Intent();
                        intentImageActivity.setClass(getApplicationContext(), CollectionImageActivity.class);
                        intentImageActivity.putExtra(CollectionImageActivity.COLLECTION_IMAGE_DATA, data);
                        startActivity(intentImageActivity);
                        break;
                    case VEDIO_TYPE:
                        Intent intentVedioActivity = new Intent();
                        intentVedioActivity.setClass(getApplicationContext(), CollectionVedioActivity.class);
                        intentVedioActivity.putExtra(CollectionVedioActivity.COLLECTION_VEDIO_DATA, data);
                        startActivity(intentVedioActivity);
                        break;
                    case AUDIO_TYPE:
                        Intent intentAudioActivity = new Intent();
                        intentAudioActivity.setClass(getApplicationContext(), CollectionAudioActivity.class);
                        intentAudioActivity.putExtra(CollectionAudioActivity.COLLECTION_AUDIO_DATA, data);
                        startActivity(intentAudioActivity);
                        break;
                    case ARTICAL_TYPE:
                        Intent intentArticalActivity = new Intent();
                        intentArticalActivity.setClass(getApplicationContext(), VideoPublishActivity.class);
                        intentArticalActivity.putExtra(INTENT_DATA_ARTICLE_ID, data.getArticleId());
                        startActivity(intentArticalActivity);
                        break;
                    default:
                        break;
                }
            }

        });
        mCollectionListAdapter.setLongItemClickListener(new CollectionListAdapter.OnLongViewItemClickListener() {
            @Override
            public void longClick(View view, DataBodyInfo data) {
                mShowDialog(data);
            }
        });
    }

    private void showForwardDialog(final DataBodyInfo bean) {
        if (bean.getType() == FileTaskManager.NOTICE_TYPE_AUDIO_SEND) {
            showToast("收藏的语音消息不能转发");
            return;
        }

        if(bean.getType() == 30){
            showToast("收藏的文章消息不能转发");
            return;
        }

        // 图片，视频，文件，转发之前需判断数据有效性
        if (bean.getType() == FileTaskManager.NOTICE_TYPE_PHOTO_SEND
                || bean.getType() == FileTaskManager.NOTICE_TYPE_VEDIO_SEND) {
            boolean isValidFile = CollectionManager.getInstance().isValidFilePath(bean.getLocalUrl());
            if (!isValidFile && TextUtils.isEmpty(bean.getRemoteUrl())) {
                // 本地文件存在，或者服务端文件存在，则任务此数据有效
                CustomLog.d(TAG, "本地文件或者服务器文件不存在,localUrl:"
                        + bean.getLocalUrl() + "remoteURL:" + bean.getRemoteUrl());
                return;
            }
        }

        CustomLog.d(TAG, "显示 确定发送该收藏内容到当前聊天？ 对话框");
        CommonDialog dialog = new CommonDialog(this, getLocalClassName(), 415);
        dialog.setMessage("确定发送该收藏内容到当前聊天？");
        dialog.setCancleButton(null, R.string.cancel_message);
        dialog.setPositiveButton(new BtnClickedListener() {
            @Override
            public void onBtnClicked() {
                CustomLog.d(TAG, "点击了提示框中的 确定 按钮");
                doSendMsg(bean);
            }
        }, R.string.confirm_message);
        dialog.showDialog();
    }

    private void doSendMsg(DataBodyInfo itemInfo) {
        if (new FileTaskManager(CollectionActivity.this).forwardMessageForCollectionOther(mReceive, itemInfo, -1)) {
            CollectionActivity.this.finish();
        } else {
            showToast("收藏消息发送失败");
        }
    }

    private void showToast(String toast) {
        Toast.makeText(this, toast, Toast.LENGTH_SHORT).show();
        CustomLog.d(TAG, toast);
    }

    @Override
    public void onDataSizeChanged(int count) {
        if (count <= 0) {
            collectionNoData.setVisibility(View.VISIBLE);
            collectionDataList1.setVisibility(View.GONE);
        } else {
            collectionNoData.setVisibility(View.GONE);
            collectionDataList1.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        initData();
    }
}
