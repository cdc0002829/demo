package cn.redcdn.hvs.profiles;


import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.ImageLoader;

import cn.redcdn.datacenter.medicalcenter.data.MDSAccountInfo;
import cn.redcdn.hvs.AccountManager;
import cn.redcdn.hvs.MedicalApplication;
import cn.redcdn.hvs.R;
import cn.redcdn.hvs.base.BaseFragment;
import cn.redcdn.hvs.meeting.activity.ConsultingRoomActivity;
import cn.redcdn.hvs.profiles.activity.AboutActivity;
import cn.redcdn.hvs.profiles.activity.CollectionActivity;
import cn.redcdn.hvs.profiles.activity.MyFileCardActivity;
import cn.redcdn.hvs.profiles.activity.MyMaActivity;
import cn.redcdn.hvs.profiles.activity.SettingActivity;
import cn.redcdn.hvs.profiles.listener.DisplayImageListener;
import cn.redcdn.hvs.util.ScannerActivity;
import cn.redcdn.hvs.util.TitleBar;
import cn.redcdn.log.CustomLog;

/**
 * Created by thinkpad on 2017/2/7.
 */

public class ProfilesFragment extends BaseFragment {
    protected final String TAG = getClass().getName();

    // 视图
    private View contentView = null;

    public static final String HTTPS = "https://www.baidu.com/";
    public static final String PERSON_TYPE = "person";
    public static final String GROUP_TYPE = "group";
    public static final String WE_TYPE = "weChat";

    private RelativeLayout gotoMyfilecard;
    private RelativeLayout gotoMeeting;
    private RelativeLayout gotoScan;
    private RelativeLayout gotoCollection;
    private RelativeLayout gotoSetting;
    private RelativeLayout gotoAbout;


    private DisplayImageListener mDisplayImageListener = null;
    private TextView name;
    private TextView acccountId;
    private ImageView headIv;
    /**
     * 扫描跳转Activity RequestCode
     */
    public static final int SCAN_CODE = 222;
    private ImageView scanIbtn;


    @Override
    protected View createView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        contentView = inflater.inflate(R.layout.profiles_fragment,
                container, false);
        initWidget(contentView);
        mDisplayImageListener = new DisplayImageListener();
        return contentView;
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            //相当于Fragment的onResume
            if (contentView != null){
                contentView.requestLayout();
            }
        } else {
            //相当于Fragment的onPause
        }
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        TitleBar titleBar = getTitleBar();
        titleBar.setTitle("我");

    }

    private void initWidget(View view) {
        gotoMyfilecard = (RelativeLayout) view.findViewById(R.id.gotomyfilecard_rl);
        gotoMeeting = (RelativeLayout) view.findViewById(R.id.gotomeeting_rl);
        gotoScan = (RelativeLayout) view.findViewById(R.id.gotoscan_rl);
        gotoCollection = (RelativeLayout) view.findViewById(R.id.gotocollection_rl);
        gotoSetting = (RelativeLayout) view.findViewById(R.id.gotosetting_rl);
        gotoAbout = (RelativeLayout) view.findViewById(R.id.gotoabout_rl);
        headIv = (ImageView) view.findViewById(R.id.head_iv);
        scanIbtn = (ImageView) view.findViewById(R.id.scan_ibtn);

        gotoMyfilecard.setOnClickListener(mbtnHandleEventListener);
        gotoMeeting.setOnClickListener(mbtnHandleEventListener);
        gotoScan.setOnClickListener(mbtnHandleEventListener);
        gotoCollection.setOnClickListener(mbtnHandleEventListener);
        gotoSetting.setOnClickListener(mbtnHandleEventListener);
        gotoAbout.setOnClickListener(mbtnHandleEventListener);
        scanIbtn.setOnClickListener(mbtnHandleEventListener);

        name = (TextView) view.findViewById(R.id.nube_tv);
        acccountId = (TextView) view.findViewById(R.id.setattend_nube_tv);
    }

    @Override
    protected void setListener() {

    }

    @Override
    protected void initData() {

    }

    @Override
    public void todoClick(int id) {
        super.todoClick(id);
        switch (id) {
            case R.id.scan_ibtn:
                Intent erWeiMaIntent = new Intent();
                erWeiMaIntent.setClass(getActivity(), MyMaActivity.class);
                startActivity(erWeiMaIntent);
                break;
            case R.id.gotomyfilecard_rl:
                Intent intentMyfilecard = new Intent();
                intentMyfilecard.setClass(getActivity(), MyFileCardActivity.class);
                startActivity(intentMyfilecard);
                break;
            case R.id.gotomeeting_rl:
                Intent intentMeeting = new Intent();
                intentMeeting.setClass(getActivity(), ConsultingRoomActivity.class);
                startActivity(intentMeeting);
                break;
            case R.id.gotoscan_rl:
                Intent intentScan = new Intent();
                intentScan.setClass(getActivity(), ScannerActivity.class);
                startActivityForResult(intentScan, SCAN_CODE);
                break;
            case R.id.gotocollection_rl:
                Intent intentCollection = new Intent();
                intentCollection.setClass(getActivity(), CollectionActivity.class);
                startActivity(intentCollection);
                break;
            case R.id.gotosetting_rl:
                Intent intentSetting = new Intent();
                intentSetting.setClass(getActivity(), SettingActivity.class);
                startActivity(intentSetting);
                break;
            case R.id.gotoabout_rl:
                Intent intentAbout = new Intent();
                intentAbout.setClass(getActivity(), AboutActivity.class);
                startActivity(intentAbout);
                break;
            default:
                break;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        initstatus();
    }

    private void initstatus() {
        CustomLog.d(TAG, "initstatus");
        MDSAccountInfo info = AccountManager.getInstance(getActivity())
                .getAccountInfo();
        if (info.headThumUrl != null && !info.headThumUrl.equalsIgnoreCase("")) {
            CustomLog.d(TAG, "显示图片");
            show(info.headThumUrl);
        } else {
            headIv.setImageResource(R.drawable.doctor_default);
        }
        String nickName = "";
        if (AccountManager.getInstance(getActivity())
                .getAccountInfo() != null)
            nickName = AccountManager.getInstance(getActivity())
                    .getAccountInfo().nickName;
        if (nickName != null && !nickName.equalsIgnoreCase(""))
            name.setText(nickName);
        else
            name.setText("未命名");
        if (AccountManager.getInstance(getActivity())
                .getAccountInfo() != null)
            acccountId.setText("视讯号：" + AccountManager.getInstance(getActivity())
                    .getAccountInfo().nube);
    }

    private void show(final String str) {
        ImageLoader imageLoader = ImageLoader.getInstance();
        imageLoader.displayImage(str, headIv, MedicalApplication.shareInstance().options, mDisplayImageListener);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SCAN_CODE) {
            parseBarCodeResult(data);
//            //处理扫描结果（在界面上显示）
//            if (data != null) {
////                parseBarCodeResult(data);
//                Bundle bundle = data.getExtras();
//                if (bundle == null) {
//                    return;
//                }
//                if (bundle.getInt(CodeUtils.RESULT_TYPE) == CodeUtils.RESULT_SUCCESS) {
//                    String result = bundle.getString(CodeUtils.RESULT_STRING);
////                    Toast.makeText(getActivity(), "解析结果:" + result, Toast.LENGTH_LONG).show();
//                    String[] split = result.split("\\?");
////                    String https = split[0];
////                    CustomLog.e("TAG", https);
////                    if (https.equals(HTTPS)) {
//                    String[] split1 = split[1].split("=");
//                    String s = split1[1];
//                    String[] split2 = s.split("_");
//                    CustomLog.e("TAG", split2[0]);
//                    CustomLog.e("TAG", split2[1]);
//                    CustomLog.e("TAG", split2[2]);
////                        if (https.contains(HTTPS)) {
//                    if (split2[0].equals(PERSON_TYPE)) {
//                        Intent intent = new Intent();
//                        intent.setClass(getActivity(), ContactCardActivity.class);
//                        intent.putExtra("nubeNumber", split2[1]);
//                        intent.putExtra("searchType", "4");
//                        startActivity(intent);
//
//                    } else if (split2[0].equals(GROUP_TYPE)) {
//                        long nowTime = System.currentTimeMillis();
//                        long startTime = Long.parseLong(split2[2]);
//                        long time = nowTime - startTime;
//                        long days = time / (1000 * 60 * 60 * 24);
//                        if (days >= 7) {
//                            Intent outDateIntent = new Intent();
//                            outDateIntent.setClass(getContext(), OutDateActivity.class);
//                            startActivity(outDateIntent);
//                        } else {
//                            Intent personIntent = new Intent();
//                            personIntent.putExtra(GroupAddActivity.GROUP_ID, split2[1]);
//                            personIntent.putExtra(GroupAddActivity.GROUP_ID_FROM, GroupAddActivity.GROUP_ID_FROM);
//                            personIntent.setClass(getContext(), GroupAddActivity.class);
//                            startActivity(personIntent);
//                        }
//
//                    } else if (split2[0].equals(WE_TYPE)) {
//                        MDSAppGetOffAccInfo mdsAppGetOffAccInfo = new MDSAppGetOffAccInfo() {
//                            @Override
//                            protected void onSuccess(OffAccdetailInfo responseContent) {
//                                super.onSuccess(responseContent);
//                                String id = responseContent.getId();
//                                Intent intentWeChat = new Intent();
//                                intentWeChat.putExtra("officialAccountId", id);
//                                intentWeChat.setClass(getActivity(), DingYueActivity.class);
//                                startActivity(intentWeChat);
//                            }
//
//                            @Override
//                            protected void onFail(int statusCode, String statusInfo) {
//                                super.onFail(statusCode, statusInfo);
//                                CustomToast.show(getContext(), "亲，此公众号不存在哦", 8000);
//                                return;
//                            }
//                        };
//                        mdsAppGetOffAccInfo.appGetOffAccInfo(AccountManager.getInstance(getActivity())
//                                .getAccountInfo().getAccessToken(), split2[1]);
//                    } else {
//                        CustomToast.show(getContext(), "亲，这不是本公司的二维码哦", 8000);
//                        return;
//                    }
////                        }
////                    } else {
////                        CustomToast.show(getContext(), "亲，这不是本公司的二维码哦", 8000);
////                        return;
////                    }
//
//                } else if (bundle.getInt(CodeUtils.RESULT_TYPE) == CodeUtils.RESULT_FAILED) {
//                    Toast.makeText(getActivity(), "解析二维码失败", Toast.LENGTH_LONG).show();
//                    CustomToast.show(getContext(), "解析二维码失败哦", 8000);
//                }
//            }
        }


    }

}
