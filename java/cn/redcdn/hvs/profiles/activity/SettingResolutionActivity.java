package cn.redcdn.hvs.profiles.activity;

import android.app.AlertDialog;
import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.HandlerThread;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cn.redcdn.butelopensdk.vo.VideoParameter;
import cn.redcdn.hvs.R;
import cn.redcdn.hvs.base.BaseActivity;
import cn.redcdn.hvs.meeting.meetingManage.MedicalMeetingManage;
import cn.redcdn.hvs.util.CustomDialog;
import cn.redcdn.log.CustomLog;

/**
 * Created by Administrator on 2017/2/27.
 */

public class SettingResolutionActivity extends BaseActivity {

    private boolean BackCameraCanOpen = false;
    private boolean FrontCameraCanOpen = false;
    private boolean OpenCameraCatch = false;

    public static final int CAMERA_FACING_BACK = 0;
    public static final int CAMERA_FACING_FRONT = 1;
    private LinearLayout setResolution;
    private LinearLayout setFrontCam;
    private LinearLayout setBackCam;
    private RelativeLayout setFrontCamResolution;
    private RelativeLayout setFrontCamCodeRate;
    private RelativeLayout setBackCamResolution;
    private RelativeLayout setBackCamCodeRate;
    private TextView frontResolution;
    private TextView backResolution;
    private TextView frontCodeRate;
    private TextView backCodeRate;
    private Button setting_resolution_back;
    private Camera mCamera = null;
    private List<String> front = null;
    private List<String> back = null;
    private String frontDefaultR;
    private String backDefaultR;
    private String frontR;
    private String backR;
    private String frontCodeR;
    private String backCodeR;
    private String content;
    private String[] codeRate = {"3000", "2000", "1800", "1200", "1000",
            "800", "300", "150"};
    private AlertDialog dialog;
    private Handler notifyWorkHandler;
    private WorkHandlerThread mWorkHandlerThread = new WorkHandlerThread(
            "SettingResolutionActivityWorkHandlerThreaed");
    private Handler mUiRreashHandler = new Handler() {
        @SuppressWarnings("unchecked")
        public void handleMessage(Message msg) {
            if (msg.what == 0) {
                if (front != null && front.size() > 0) {
                    setVideoDate(CAMERA_FACING_FRONT);
                    setFrontCam.setVisibility(View.VISIBLE);
                } else {
                    setFrontCam.setVisibility(View.GONE);
                }
                if (back != null && back.size() > 0) {
                    setVideoDate(CAMERA_FACING_BACK);
                    setBackCam.setVisibility(View.VISIBLE);
                } else {
                    setBackCam.setVisibility(View.GONE);
                }
            } else {
                SettingResolutionActivity.this.finish();
            }
            SettingResolutionActivity.this.removeLoadingView();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_resolution_setting);
        initWidget();

        mWorkHandlerThread.start();
        notifyWorkHandler = new Handler(mWorkHandlerThread.getLooper(),
                mWorkHandlerThread);
        SettingResolutionActivity.this.showLoadingView("请稍后...");
        Message msg = Message.obtain();
        msg.what = WorkHandlerThread.GET_DATE;
        notifyWorkHandler.sendMessage(msg);

    }

    private void setVideoDate(int id) {

        if (CAMERA_FACING_BACK == id) {
            VideoParameter p = MedicalMeetingManage.getInstance().getVideoParameter(
                    id);
            if (p == null) {
                backResolution.setText(backDefaultR);
                backCodeRate.setText("300Kbps");
                backR = backDefaultR;
                backCodeR = "300";
            } else {
                backResolution
                        .setText(p.getCapWidth() + "X" + p.getCapHeight());
                backCodeRate.setText(p.getEncBitrate() + "Kbps");
                backR = p.getCapWidth() + "X" + p.getCapHeight();
                backCodeR = p.getEncBitrate() + "";
            }
        } else {
            VideoParameter p = MedicalMeetingManage.getInstance().getVideoParameter(
                    id);
            if (p == null) {
                frontR = frontDefaultR;
                frontCodeR = "300";
                frontResolution.setText(frontDefaultR);
                frontCodeRate.setText("300Kbps");
            } else {
                frontR = p.getCapWidth() + "X" + p.getCapHeight();
                frontCodeR = p.getEncBitrate() + "";
                frontResolution.setText(p.getCapWidth() + "X"
                        + p.getCapHeight());
                frontCodeRate.setText(p.getEncBitrate() + "Kbps");
            }
        }

    }

    private void setVideoParameter(int id) {

        if (id == CAMERA_FACING_FRONT) {
            if (frontR != null) {
                String s[] = frontR.split("X");
                if (s != null && frontCodeR != null) {
                    VideoParameter p = new VideoParameter(Integer.parseInt(s[0]),
                            Integer.parseInt(s[1]), 15,
                            Integer.parseInt(frontCodeR));
                    MedicalMeetingManage.getInstance().setVideoParameter(id, p);
                } else {
                    CustomLog.e("SettingResolutionActivity", "frontR == null");
                }

            }
        } else {
            if (backR != null) {
                String s[] = backR.split("X");
                if (s != null && backCodeR != null) {
                    VideoParameter p = new VideoParameter(Integer.parseInt(s[0]),
                            Integer.parseInt(s[1]), 15, Integer.parseInt(backCodeR));
                    MedicalMeetingManage.getInstance().setVideoParameter(id, p);
                } else {
                    CustomLog.e("SettingResolutionActivity", "backR == null");
                }

            }
        }

    }

    private String getNearResolution(List<MySize> list, int w, int h) {

        if (list == null)
            return null;
        int dif = 9999;
        int index = 0;
        for (int i = 0; i < list.size(); i++) {
            int n = (Math.abs((list.get(i).x) - w) + Math.abs((list.get(i).y)
                    - h));
            if (n < dif) {
                dif = n;
                index = i;
            }
        }
        CustomLog.e("SettingResolutionActivity",
                "最接近 640X360 is " + list.get(index).toString());

        return list.get(index).toString();

    }


    private void initWidget() {
        setFrontCam = (LinearLayout) findViewById(R.id.set_front_camare);
        setBackCam = (LinearLayout) findViewById(R.id.set_back_camare);
        setFrontCamResolution = (RelativeLayout) findViewById(R.id.set_front_camare_resolution_rl);
        setFrontCamCodeRate = (RelativeLayout) findViewById(R.id.set_front_camare_rate_rl);
        setBackCamResolution = (RelativeLayout) findViewById(R.id.set_back_camare_resolution_rl);
        setBackCamCodeRate = (RelativeLayout) findViewById(R.id.set_back_camare_rate_rl);
        setting_resolution_back = (Button) findViewById(R.id.setting_resolution_back);
        frontResolution = (TextView) findViewById(R.id.set_front_camare_resolution_txt);
        backResolution = (TextView) findViewById(R.id.set_back_camare_resolution_txt);
        frontCodeRate = (TextView) findViewById(R.id.set_front_camare_rate_txt);
        backCodeRate = (TextView) findViewById(R.id.set_back_camare_rate_txt);
        setFrontCamResolution.setOnClickListener(mbtnHandleEventListener);
        setFrontCamCodeRate.setOnClickListener(mbtnHandleEventListener);
        setBackCamResolution.setOnClickListener(mbtnHandleEventListener);
        setBackCamCodeRate.setOnClickListener(mbtnHandleEventListener);
        setting_resolution_back.setOnClickListener(mbtnHandleEventListener);
        setResolution = (LinearLayout) findViewById(R.id.ll_set_resolution);
    }

    @SuppressWarnings("unchecked")
    private List<String> getCameraResolution(int type) {
        List<String> result = null;
        try {
            if (mCamera != null) {
                mCamera.release();
                mCamera = null;
            }
            int n = getCameraId(type);
            if (n < 0) {
                return null;
            }
            List<Size> list = null;
            List<MySize> mList = new ArrayList<MySize>();
            result = new ArrayList<String>();

            try {

                mCamera = Camera.open(n);

                OpenCameraCatch = false;

            } catch (Exception e) {

                OpenCameraCatch = true;

            }

            if (n == CAMERA_FACING_BACK) {

                if (!OpenCameraCatch) {

                    BackCameraCanOpen = true;

                    CustomLog.d(TAG, "n==CAMERA_FACING_BACK&&!OpenCameraCatch&&BackCameraCanOpen");

                }

                if (OpenCameraCatch) {

                    BackCameraCanOpen = false;

                    CustomLog.d(TAG, "n==CAMERA_FACING_BACK&&OpenCameraCatch&&!BackCameraCanOpen");

                }
            }

            if (n == CAMERA_FACING_FRONT) {

                if (!OpenCameraCatch) {

                    FrontCameraCanOpen = true;

                    CustomLog.d(TAG, "n==CAMERA_FACING_FRONT&&!OpenCameraCatch&&FrontCameraCanOpen");

                }

                if (OpenCameraCatch) {

                    FrontCameraCanOpen = false;

                    CustomLog.d(TAG, "n==CAMERA_FACING_FRONT&&OpenCameraCatch&&!FrontCameraCanOpen");

                }

                if (!BackCameraCanOpen && !FrontCameraCanOpen) {

                    final CustomDialog dialog = new CustomDialog(this);

                    dialog.setOkBtnOnClickListener(new CustomDialog.OKBtnOnClickListener() {
                        @Override
                        public void onClick(CustomDialog customDialog) {
                            dialog.cancel();
                            finish();
                        }
                    });
                    dialog.setTip("请开启摄像头权限");
                    dialog.removeCancelBtn();
                    dialog.setCancelable(false);
                    dialog.setOkBtnText("退出");
                    dialog.show();

                    CustomLog.d(TAG, "!BackCameraCanOpen&&!FrontCameraCanOpen");

                }


            }


            Parameters parameters = mCamera.getParameters();
            if (parameters != null) {
                list = parameters.getSupportedPreviewSizes();
                if (list != null) {
                    for (int i = 0; i < list.size(); i++) {
                        mList.add(new MySize(list.get(i).width,
                                list.get(i).height));
                    }
                    Collections.sort(mList);
                    if (CAMERA_FACING_BACK == type) {
                        backDefaultR = getNearResolution(mList, 640, 360);
                    } else {
                        frontDefaultR = getNearResolution(mList, 640, 360);
                    }
                    for (int i = 0; i < mList.size(); i++) {
                        result.add(mList.get(i).toString());
                    }
                }
            }
        } catch (Exception e) {
            CustomLog.e(TAG, "getCameraResolution Exception " + e);
        }
        return result;
    }

    private int getCameraId(int type) {
        try {
            int numberOfCameras = Camera.getNumberOfCameras();
            Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
            for (int i = 0; i < numberOfCameras; i++) {
                Camera.getCameraInfo(i, cameraInfo);
                if (cameraInfo.facing == type) {
                    return i;
                }
            }
        } catch (Exception e) {
            CustomLog.e(TAG, "getCameraId Exception " + e);
        }
        return -1;
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onStop() {
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
        super.onDestroy();
    }

    @Override
    public void todoClick(int i) {
        super.todoClick(i);
        switch (i) {
            case R.id.set_front_camare_resolution_rl:
                // 弹框
                String[] s = new String[front.size()];
                front.toArray(s);
                showMyDialog(0, "分辨率", s, frontR);
                break;
            case R.id.set_front_camare_rate_rl:
                showMyDialog(1, "码率", codeRate, frontCodeR);
                break;
            case R.id.set_back_camare_resolution_rl:
                String[] ss = new String[back.size()];
                ;
                back.toArray(ss);
                showMyDialog(2, "分辨率", ss, backR);
                break;
            case R.id.set_back_camare_rate_rl:
                showMyDialog(3, "分辨率", codeRate, backCodeR);
                break;
            case R.id.setting_resolution_back:
                SettingResolutionActivity.this.showLoadingView("请稍后...");
                Message msg = Message.obtain();
                msg.what = WorkHandlerThread.SET_DATE;
                notifyWorkHandler.sendMessage(msg);
                break;
            default:
                break;
        }
    }


    @Override
    public void onBackPressed() {
        SettingResolutionActivity.this.showLoadingView("请稍后...");
        Message msg = Message.obtain();
        msg.what = WorkHandlerThread.SET_DATE;
        notifyWorkHandler.sendMessage(msg);
        //super.onBackPressed();
    }


    @SuppressWarnings("rawtypes")
    private class MySize implements Comparable {
        int x;
        int y;

        MySize(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public int compareTo(Object o) {
            MySize obj = (MySize) o;
            if (x != obj.x) {
                return x - obj.x;
            }
            return y - obj.y;
        }

        public String toString() {
            return x + "X" + y;
        }
    }

    public void showMyDialog(final int id, String title, String[] list,
                             String str) {
        dialog = new AlertDialog.Builder(this).create();
        dialog.show();
        Window window = dialog.getWindow();
        window.setContentView(R.layout.activity_resolution_dialog);
        ListView listView = (ListView) window
                .findViewById(R.id.operate_resolution_dialog_listview);
        TextView txt = (TextView) window
                .findViewById(R.id.operate_resolution_dialog_titile);
        txt.setText(title);
        ViewAdapter adapter = new ViewAdapter(SettingResolutionActivity.this,
                list, str);
        listView.setAdapter(adapter);
        dialog.setCanceledOnTouchOutside(true);// 使除了dialog以外的地方不能被点击
        listView.setOnItemClickListener(new OnItemClickListener() {// 响应listview中的item的点击事件

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
                                    long arg3) {
                TextView tv = (TextView) arg1.findViewById(R.id.txt);// 取得每条item中的textview控件
                content = tv.getText().toString();
                CustomLog.d(TAG, "content " + content);
                switch (id) {
                    case 0:
                        frontR = content;
                        frontResolution.setText(frontR);
                        break;
                    case 1:
                        frontCodeR = content;
                        frontCodeRate.setText(frontCodeR + "Kpbs");
                        break;
                    case 2:
                        backR = content;
                        backResolution.setText(backR);
                        break;
                    case 3:
                        backCodeR = content;
                        backCodeRate.setText(backCodeR + "Kpbs");
                        break;
                }
                dialog.cancel();
            }
        });

    }

    private class ViewAdapter extends BaseAdapter {
        private final String TAG = ViewAdapter.this.getClass().getName();
        private LayoutInflater mInflater;
        private String[] list;
        private Context mContext;
        private String myDefault;

        public ViewAdapter(Context context, String[] mList, String defaultStr) {
            mInflater = LayoutInflater.from(context);
            list = mList;
            mContext = context;
            myDefault = defaultStr;
        }

        @Override
        public int getCount() {
            return list.length;
        }

        @Override
        public Object getItem(int arg0) {
            return null;
        }

        @Override
        public long getItemId(int arg0) {
            return 0;
        }


        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (null == convertView) {
                convertView = mInflater.inflate(
                        R.layout.camera_set_paramer_item, null);
                holder = new ViewHolder();
                holder.content = (TextView) convertView.findViewById(R.id.txt);
                holder.img = (ImageView) convertView
                        .findViewById(R.id.check_img);
                // holder.line = (ImageView)
                // convertView.findViewById(R.id.line);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            holder.content.setText(list[position]);
            /*
             * if (position == (list.length - 1)) {
			 * holder.line.setVisibility(View.INVISIBLE); } else {
			 * holder.line.setVisibility(View.VISIBLE); }
			 */
            // CustomLog.e("sssssssssssssssssss",
            // list[position]+"sssssssssssssssssss!!!!! "+myDefault);
            if (myDefault != null && myDefault.equals(list[position])) {
                CustomLog.e("sssssssssssssssssss", "fffffffffffffffffffff");
                holder.img.setVisibility(View.VISIBLE);
            } else {
                holder.img.setVisibility(View.INVISIBLE);
            }
            return convertView;
        }

        class ViewHolder {
            TextView content;
            // ImageView line;
            ImageView img;
        }

    }


    private class WorkHandlerThread extends HandlerThread implements Callback {
        public static final int GET_DATE = 0;
        public static final int SET_DATE = 1;

        public WorkHandlerThread(String name) {
            super(name);
        }

        @Override
        public boolean handleMessage(Message msg) {
            CustomLog.d(TAG, "handleMessage处理指定发言命令");
            switch (msg.what) {
                case GET_DATE:
                    back = getCameraResolution(CAMERA_FACING_BACK);
                    front = getCameraResolution(CAMERA_FACING_FRONT);
                    Message message = Message.obtain();
                    message.what = 0;
                    mUiRreashHandler.sendMessage(message);
                    break;
                case SET_DATE:
                    // TODO 保存分辨率
                    if (front != null) {
                        setVideoParameter(CAMERA_FACING_FRONT);
                    }
                    if (back != null) {
                        setVideoParameter(CAMERA_FACING_BACK);
                    }
                    Message message1 = Message.obtain();
                    message1.what = 1;
                    mUiRreashHandler.sendMessage(message1);
                    break;
                default:
                    break;
            }
            CustomLog.d(TAG, "handleMessage结束");
            return false;
        }
    }
}
