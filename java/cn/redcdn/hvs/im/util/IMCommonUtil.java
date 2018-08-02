package cn.redcdn.hvs.im.util;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.TextView;
import cn.redcdn.commonutil.NetConnectHelper;
import cn.redcdn.hvs.AccountManager;
import cn.redcdn.hvs.MedicalApplication;
import cn.redcdn.hvs.R;
import cn.redcdn.hvs.im.IMConstant;
import cn.redcdn.hvs.im.bean.ContactFriendBean;
import cn.redcdn.hvs.im.bean.ThreadsBean;
import cn.redcdn.hvs.im.column.NubeFriendColumn;
import cn.redcdn.hvs.im.dao.NoticesDao;
import cn.redcdn.hvs.im.dao.ThreadsDao;
import cn.redcdn.hvs.im.fileTask.FileTaskManager;
import cn.redcdn.hvs.im.preference.DaoPreference.PrefType;
import cn.redcdn.hvs.im.view.CommonDialog;
import cn.redcdn.hvs.util.CommonUtil;
import cn.redcdn.hvs.util.CustomToast;
import cn.redcdn.hvs.util.StringUtil;
import cn.redcdn.log.CustomLog;
import java.io.File;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.view.View.GONE;
import static com.butel.connectevent.utils.CommonUtil.getImageRotationFromUrl;

/**
 * Desc
 * Created by wangkai on 2017/2/25.
 */

public class IMCommonUtil {

    private static final String TAG = "IMCommonUtil";
    public static final String KEY_BROADCAST_INTENT_DATA = "KEY_BROADCAST_INTENT_DATA";
    private static Point deviceSize = null;


    /**
     * 根据性别男女返回默认头像id
     *
     */
    public static int getHeadIdBySex(String sex) {
        int headId = R.drawable.head_default;// 默认头像为男
        if (CommonUtil.getString(R.string.woman).equals(sex)
                || String.valueOf(NubeFriendColumn.SEX_FEMALE).equals(sex)) {
            headId = R.drawable.head_default;// 头像改为女
        }
        return headId;
    }

    private static int screen_w = 0; // 手机屏幕的宽度，单位像素
    private static int screen_h = 0; // 手机屏幕的高度，单位像素

    private static void initScreenInfo(Context mContext) {
        DisplayMetrics dm = new DisplayMetrics();
        dm = mContext.getResources().getDisplayMetrics();

        float density = dm.density; // 屏幕密度（像素比例：0.75/1.0/1.5/2.0）
        int densityDPI = dm.densityDpi; // 屏幕密度（每寸像素：120/160/240/320）
        float xdpi = dm.xdpi;
        float ydpi = dm.ydpi;


        density = dm.density; // 屏幕密度（像素比例：0.75/1.0/1.5/2.0）
        densityDPI = dm.densityDpi; // 屏幕密度（每寸像素：120/160/240/320）
        xdpi = dm.xdpi;
        ydpi = dm.ydpi;

        CustomLog.e("commonutil" + "  DisplayMetrics", "xdpi=" + xdpi + "; ydpi=" + ydpi);
        CustomLog.e("commonutil" + "  DisplayMetrics", "density=" + density
                + "; densityDPI=" + densityDPI);

        screen_w = dm.widthPixels;
        screen_h = dm.heightPixels;

        // Log.e(TAG + "  DisplayMetrics(222)", "screenWidthDip=" +
        // screenWidthDip + "; screenHeightDip=" + screenHeightDip);
        //
        // screen_w = (int)(dm.widthPixels * density + 0.5f); // 屏幕宽（px，如：480px）
        // screen_h = (int)(dm.heightPixels * density + 0.5f); //
        // 屏幕高（px，如：800px）

        CustomLog.e("commonutil" + "  DisplayMetrics(222)", "screenWidth=" + screen_w
                + "; screenHeight=" + screen_h);
    }

    public static int getScreenWidth(Context mContext) {
        // if(screen_w == 0)
        // {
        initScreenInfo(mContext);
        // }
        return screen_w;
    }

    /**
     * @author: chuwx
     * @Title: simpleFormatMoPhone
     * @Description: 简单格式化手机号码
     */
    public static String simpleFormatMoPhone(String phone) {
        if (TextUtils.isEmpty(phone)) {
            return "";
        }
        String oldPhone = phone;
        phone = phone.replace("-", "").replace(" ", "");
        if (phone.startsWith("+86") && phone.length() == 14) {
            phone = phone.substring(3);
        }
        // LogUtil.d("简单格式化手机号码:" + oldPhone + "---->" + phone);
        return phone;
    }

    /**
     *
     * Description:过滤非法字符
     * @param str
     * @return
     */
    public static String fliteIllegalChar(String str) {
        String illStrs = "\"" + "\'" + "\\" + "\n" + "\r" + "&<>/%“‘”";
        if (!TextUtils.isEmpty(str)) {
            for (char illStr : illStrs.toCharArray()) {
                str = str.replace(illStr, ' ').replace(" ", "");
            }
        }
        return str.trim();
    }

    public static ArrayList<String> getList(String str) {
        CustomLog.d(TAG,"解析@功能字符串里面的特定的name：" + str);
        ArrayList<String> result = new ArrayList<String>();
        String nameStr = "";
        int pos = str.indexOf(IMConstant.SPECIAL_CHAR);
        if (pos != -1 && str.indexOf("@") != -1) {
            String startStr = str.substring(0, pos + 1);
            if (startStr.indexOf("@") != -1) {
                nameStr = startStr
                        .substring(startStr.lastIndexOf("@"), pos + 1);
                result.add(nameStr);
            }
            String endStr = str.substring(pos + 1, str.length());
            while (endStr.indexOf(IMConstant.SPECIAL_CHAR) != -1
                    && endStr.indexOf("@") != -1) {
                int position = endStr.indexOf(IMConstant.SPECIAL_CHAR);
                startStr = endStr.substring(0, position + 1);
                if (startStr.indexOf("@") != -1) {
                    nameStr = startStr.substring(startStr.lastIndexOf("@"),
                            position + 1);
                    result.add(nameStr);
                }
                endStr = endStr.substring(position + 1, endStr.length());
            }
        }
        return result;
    }

    /**
     * @author: zhaguitao
     * @Title: getImageRotationByPath
     * @Description: 根据图片路径获得其旋转角度
     * @param ctx
     * @param path
     * @return
     * @date: 2013-10-16 下午12:53:34
     */
    public static int getImageRotationByPath(Context ctx, String path) {
        int rotation = 0;
        if (TextUtils.isEmpty(path)) {
            return rotation;
        }

        Cursor cursor = null;
        try {
            cursor = ctx.getContentResolver().query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    new String[] { MediaStore.Images.Media.ORIENTATION },
                    MediaStore.Images.Media.DATA + " = ?",
                    new String[] { "" + path }, null);
            if (cursor != null && cursor.getCount() > 0) {
                cursor.moveToFirst();
                rotation = cursor.getInt(0);
            } else {
                rotation = getImageRotationFromUrl(path);
            }
        } catch (Exception e) {
            CustomLog.e(TAG, "getImageRotationByPath"+ "Exception" + e.toString());
        } finally {
            if (cursor != null) {
                cursor.close();
                cursor = null;
            }
        }

        return rotation;
    }

    /**
     * @author: zhaguitao
     * @Title: getDeviceSize
     * @Description: 获取手机屏幕宽高
     * @param context
     * @return
     * @date: 2014-3-13 上午9:45:55
     */
    @SuppressLint("NewApi")
    public static Point getDeviceSize(Context context) {
        if (deviceSize == null) {
            deviceSize = new Point(0, 0);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
                ((WindowManager) context
                    .getSystemService(Context.WINDOW_SERVICE))
                    .getDefaultDisplay().getSize(deviceSize);
            } else {
                Display display = ((WindowManager) context
                    .getSystemService(Context.WINDOW_SERVICE))
                    .getDefaultDisplay();
                deviceSize.x = display.getWidth();
                deviceSize.y = display.getHeight();
                display = null;
            }
        }
        return deviceSize;
    }

    public static void scanFileAsync(Context ctx, String filePath) {
        CustomLog.d(TAG,"filePath:" + filePath);
        Intent scanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        scanIntent.setData(Uri.fromFile(new File(filePath)));
        ctx.sendBroadcast(scanIntent);
    }

    public static void setKeyValue(String txt, String gid) {
        if (!TextUtils.isEmpty(gid)) {
            CustomLog.d(TAG,"setKeyValue KEY_CHAT_REMIND_LIST:" + txt + gid);
            ArrayList<String> resultList = new ArrayList<String>();
            resultList = getDispList(txt);
            if (resultList.toString().contains(
                    AccountManager.getInstance(MedicalApplication.getContext()).getNube())) {
                String value = MedicalApplication.getPreference().getKeyValue(
                        PrefType.KEY_CHAT_REMIND_LIST, "");
                value = value + gid + ";";
                MedicalApplication.getPreference().setKeyValue(
                        PrefType.KEY_CHAT_REMIND_LIST, value);
            }
        }
    }


    public static ArrayList<String> getDispList(String str) {
        CustomLog.d(TAG,"解析@功能字符串里面的特定的nube：" + str);
        ArrayList<String> result = new ArrayList<String>();
        String nameStr = "";
        int pos = str.indexOf(IMConstant.SPECIAL_CHAR);
        if (pos != -1 && str.indexOf("@") != -1) {
            String startStr = str.substring(0, pos + 1);
            nameStr = startStr.substring(startStr.lastIndexOf("@") + 1, pos);
            if (nameStr.length() == 8 && StringUtil.isNumeric(nameStr)) {
                result.add(nameStr);
            }
            String endStr = str.substring(pos + 1, str.length());
            while (endStr.indexOf(IMConstant.SPECIAL_CHAR) != -1
                    && endStr.indexOf("@") != -1) {
                int position = endStr.indexOf(IMConstant.SPECIAL_CHAR);
                startStr = endStr.substring(0, position + 1);
                nameStr = startStr.substring(startStr.lastIndexOf("@") + 1,
                        position);
                if (nameStr.length() == 8 && StringUtil.isNumeric(nameStr)) {
                    result.add(nameStr);
                }
                endStr = endStr.substring(position + 1, endStr.length());
            }
        }
        return result;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static void copy2Clipboard(Context context, String value) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            android.text.ClipboardManager cmb = (android.text.ClipboardManager) context
                    .getSystemService(Context.CLIPBOARD_SERVICE);
            cmb.setText(value);
        } else {
            android.content.ClipboardManager cmb = (android.content.ClipboardManager) context
                    .getSystemService(Context.CLIPBOARD_SERVICE);
            cmb.setText(value);
        }
    }

    /**
     *
     * Description: dp 转换 px
     *
     * @param context
     * @param dp
     * @return
     */
    public static int dp2px(Context context, int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                context.getResources().getDisplayMetrics());
    }

    public static boolean isChineseChar(String str) {
        boolean temp = false;
        Pattern p = Pattern.compile("[\u4e00-\u9fa5]");
        Matcher m = p.matcher(str);
        if (m.find()) {
            temp = true;
        }
        return temp;
    }

    /**
     * @param str
     * @return 汉字2个，其他1个
     */
    public static int getStringLength(String str) {
        int length = 0;
        for (int i = 0; i < str.length(); i++) {
            String c = String.valueOf(str.charAt(i));
            length = length + (isChineseChar(c) ? 2 : 1);
        }
        return length;
    }

    /**
     *
     * @param text 输入字符串
     * @param length（汉字2个）
     * @return
     */
    public static String getSubStringByMaxLength(String text,int length){
        String sub="";
        for (int i=text.length();i>0;i--){
            if (getStringLength(text.substring(0, i))<=length){
                sub=text.substring(0, i);
                break;
            }
        }
        return sub;
    }

    /**
     * @Title: isNetworkAvailable
     * @Description: 网络状态判断
     * @param context
     * @return
     * @return boolean
     * @throws
     */
    public static boolean isNetworkAvailable(Context context) {
        if(NetConnectHelper.NETWORKTYPE_INVALID == NetConnectHelper.getNetWorkType(MedicalApplication.getContext())){
            return false;
        }
        return true;

    }


    /**
     * @author: zhaguitao
     * @Title: makeCusPhotoFileName
     * @Description: 自定义照片文件名
     * @return
     * @date: 2014-5-27 下午4:19:20
     */
    public static String makeCusPhotoFileName() {
        return "IMG_" + System.currentTimeMillis() + ".jpg";
    }

    public static Intent getTakePickIntent(File f) {
        // 部分三星手机，在启动照相机后，onActivityResult返回的intent为空，
        // 不能将照相后的图片传递到本页面,故此处用指定路径的形式做透传
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(f));
        return intent;
    }

    /**
     * 拨号盘和消息列表界面添加好友，要插入默认消息
     *
     * @throws Exception
     */
    public static void addFriendTxt(final Activity activity, String str,
                                    ContactFriendBean currentInfo) {
        NoticesDao noticeDao = new NoticesDao(activity);
        ThreadsDao threadDao = new ThreadsDao(activity);
        String convstId;
        ThreadsBean th = threadDao.getThreadByRecipentIds(currentInfo
            .getNubeNumber());
        // 判断要插入默认消息的场景
        if (th == null) {
            convstId = "";
            noticeDao.createAddFriendTxt("",
                currentInfo.getNubeNumber(),
                null,
                "",
                FileTaskManager.NOTICE_TYPE_DESCRIPTION,
                MedicalApplication.getContext().getString(
                    R.string.add_friend_text), convstId, null, "");
        } else if (th != null) {
            Cursor cusor = null;
            convstId = th.getId();
            try {
                cusor = noticeDao.queryConvstNoticesCursor(th.getId());
                if (cusor == null || cusor.getCount() == 0) {
                    noticeDao.createAddFriendTxt("",
                        currentInfo.getNubeNumber(),
                        null,
                        "",
                        FileTaskManager.NOTICE_TYPE_DESCRIPTION,
                        MedicalApplication.getContext().getString(
                            R.string.add_friend_text), convstId, null,
                        "");
                }
            } catch (Exception e) {
                CustomLog.e("addFriendTxt :", String.valueOf(e));
            } finally {
                if (cusor != null) {
                    cusor.close();
                    cusor = null;
                }
            }
        }
    }

    public static void alertPermissionDialog(final Context mContext,
                                             final CommonDialog.BtnClickedListener btnOkLister,
                                             final CommonDialog.BtnClickedListener btnCancleLister, int id) {
        CustomLog.d(TAG,"show 非Wifi网络下，流量使用确认对话框");
        CommonDialog dialog = new CommonDialog(mContext,
                ((Activity) mContext).getLocalClassName(), 301);
        dialog.setMessage(id);
        dialog.setCancelable(false);
        dialog.setPositiveButton(btnOkLister, "确定");
        dialog.showDialog();
    }


    /**
     * @Description: 调用系统播放器，播放视频文件
     * @param context
     * @param videoPath
     */
    public static void playVideo(Context context, String videoPath) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        String strend = "";
        if (videoPath.toLowerCase().endsWith(".mp4")) {
            strend = "mp4";
        } else if (videoPath.toLowerCase().endsWith(".mpg4")) {
            strend = "mp4";
        } else if (videoPath.toLowerCase().endsWith(".3gp")) {
            strend = "3gpp";
        } else if (videoPath.toLowerCase().endsWith(".mov")) {
            strend = "quicktime";
        } else if (videoPath.toLowerCase().endsWith(".wmv")) {
            strend = "wmv";
        } else if (videoPath.toLowerCase().endsWith(".avi")) {
            strend = "x-msvideo";
        } else if (videoPath.toLowerCase().endsWith(".mpe")) {
            strend = "mpeg";
        } else if (videoPath.toLowerCase().endsWith(".mpeg")) {
            strend = "mpeg";
        } else if (videoPath.toLowerCase().endsWith(".mpg")) {
            strend = "mpeg";
        }

        intent.setDataAndType(Uri.fromFile(new File(videoPath)), "video/"
            + strend);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            CustomLog.e("playVideo" + "ActivityNotFoundException", String.valueOf(e));
            CustomToast.show(context, R.string.play_media_error, 1);
        }
    }

    public static void makeModeChangeToast(Context mContext,CharSequence text) {
        final RelativeLayout playModeViewGroup
            = (RelativeLayout) ((Activity) mContext)
            .findViewById(R.id.container_toast);
        TextView textView = (TextView) playModeViewGroup.findViewById(R.id.slogan);
        textView.setText(text);
        playModeViewGroup.setVisibility(View.VISIBLE);

        new Handler().postDelayed(new Runnable() {
            @Override public void run() {
                playModeViewGroup.setVisibility(GONE);
            }
        }, 2000);

    }
}
