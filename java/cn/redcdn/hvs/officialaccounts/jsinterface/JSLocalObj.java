package cn.redcdn.hvs.officialaccounts.jsinterface;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

import org.json.JSONObject;

import cn.redcdn.datacenter.collectcenter.AddCollectionItems;
import cn.redcdn.datacenter.collectcenter.DataBodyInfo;
import cn.redcdn.hvs.AccountManager;
import cn.redcdn.hvs.MedicalApplication;
import cn.redcdn.hvs.officialaccounts.DingYueActivity;
import cn.redcdn.hvs.util.CustomToast;
import cn.redcdn.log.CustomLog;

/**
 * Created by KevinZhang on 2017/3/11.
 */

public abstract class JSLocalObj {
    private static final String TAG = JSLocalObj.class.getName();
    private Context mContext;
    private Handler mHandler;
    public static final String JS_INTERFACE_NAME = "jsInterFace"; //与前端页面约定的交互对象名称
    private static final int MSG_COLLECT_ARTICLE = 1;
    private static final int MSG_SHOW_OFFICE_PAGE = 2;
    private static final int MSG_CHOOSE_CONTENT = 3;
    private static final int MSG_SHOW_TOAST = 4;

    public JSLocalObj(Context context) {
        mContext = context;
        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case MSG_CHOOSE_CONTENT:
                        onChooseContent(msg.getData().getString("contentId"), msg.getData().getString("contentName"), msg.arg1);
                        break;
                    case MSG_SHOW_OFFICE_PAGE:
                        switchToDingYueActivity((String) msg.obj);
                        break;
                    case MSG_SHOW_TOAST:
                        CustomToast.show(mContext, msg.obj.toString(), Toast.LENGTH_LONG);
                        break;
                }
            }
        };
    }

    //调用dataCenter 收藏接口进行收藏，如果收藏失败进行错误日志打印，不做提示
    @JavascriptInterface
    public void collectArticle(String officeId, String officeName, String officeLogoUrl, String articleId, String articleTile, String previewUrl, String createTime) {
        CustomLog.d(TAG, "officeId:" + officeId + " | officeName: " + officeName + " |officeLogoUrl: " + officeLogoUrl + " |articleId: " + articleId + " |articleTile:" + articleTile + " |previewUrl" + previewUrl + " |createTime:" + createTime);
        DataBodyInfo info = new DataBodyInfo();
        info.officialAccountId = officeId;
        info.name = officeName;
        info.ArticleId = articleId;
        info.title = articleTile;
        info.previewUrl = previewUrl;
        info.offAccLogoUrl = officeLogoUrl;
        info.createTime = createTime;
        info.isforwarded = 0;
        info.ForwarderNube = "";
        info.ForwarderName = "";
        info.ForwarderHeaderUrl = "";
        AddCollectionItems items = new AddCollectionItems() {
            @Override
            protected void onFail(int statusCode, String statusInfo) {
                super.onFail(statusCode, statusInfo);
            }

            @Override
            protected void onSuccess(JSONObject responseContent) {
                super.onSuccess(responseContent);

            }
        };
        items.addFavoriteItems(AccountManager.getInstance(mContext).getNube(), "", AccountManager.getInstance(mContext).getToken(), 30, info);
    }


    //切换到公众号名片页面
    @JavascriptInterface
    public void showOfficePage(String officeId) {
        CustomLog.d(TAG, "showOfficePage:" + officeId);
        Message msg = new Message();
        msg.what = MSG_SHOW_OFFICE_PAGE;
        msg.obj = officeId;
        mHandler.sendMessage(msg);

    }

    @JavascriptInterface
    public void chooseContent(String contentId, String contentName, int type) {
        CustomLog.d(TAG, "chooseContent:" + contentId + " | contentName: " + contentName + " |type: " + type);
        Message msg = new Message();
        Bundle data = new Bundle();
        data.putString("contentId", contentId);
        data.putString("contentName", contentName);
        msg.what = MSG_CHOOSE_CONTENT;
        msg.arg1 = type;
        msg.setData(data);
        mHandler.sendMessage(msg);
    }

    @JavascriptInterface
    public void showToast(String toastMsg) {
        CustomLog.d(TAG, "showToast:" + toastMsg);
        if (!TextUtils.isEmpty(toastMsg)) {
            Message msg = new Message();
            msg.what = MSG_SHOW_TOAST;
            msg.obj = toastMsg;
            mHandler.sendMessage(msg);
        }
    }

    @JavascriptInterface
    private void switchToDingYueActivity(String officeId) {
        Intent intent = new Intent(mContext, DingYueActivity.class);
        intent.putExtra("officialAccountId",officeId);
        mContext.startActivity(intent);
    }

    public abstract void onChooseContent(String contentId, String contentName, int type);
}
