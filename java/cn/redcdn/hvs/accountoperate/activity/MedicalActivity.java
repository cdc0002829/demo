package cn.redcdn.hvs.accountoperate.activity;

import android.Manifest;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cn.redcdn.datacenter.cdnuploadimg.CdnUploadDataInfo;
import cn.redcdn.hvs.AccountManager;
import cn.redcdn.hvs.MedicalApplication;
import cn.redcdn.hvs.R;
import cn.redcdn.hvs.cdnmanager.UploadManager;
import cn.redcdn.hvs.config.SettingData;
import cn.redcdn.hvs.profiles.helper.DocumentsHelper;
import cn.redcdn.hvs.profiles.listener.DisplayImageListener;
import cn.redcdn.hvs.util.BitmapUtils;
import cn.redcdn.hvs.util.CameraImageDialog;
import cn.redcdn.hvs.util.CommonUtil;
import cn.redcdn.hvs.util.CustomToast;
import cn.redcdn.hvs.util.TitleBar;
import cn.redcdn.log.CustomLog;
import cn.redcdn.network.httprequest.HttpErrorCode;

import static android.media.MediaRecorder.VideoSource.CAMERA;
import static cn.redcdn.hvs.R.id.name_hint_nameafter;


/**
 * Created by thinkpad on 2017/2/21.
 */
public class MedicalActivity extends cn.redcdn.hvs.base.BaseActivity {
    private File croppedIconFile;//压缩后文件
    public static final String KEY_FILE_CROPPEDICON_PATH = "key_file_croppedicon_path";
    private String croppedIconFilepath = null;// 压缩后图片位置
    public static final String KEY_FILE_ABSOLUTELY = "key_file_absolutely";
    private final String IMAGE_TYPE = "image/*";
    private final int IMAGE_CODE = 0;
    private File headIconFile = null;// 相册或者拍照保存的文件
    private Button button_next, button_back;
    private LinearLayout medical_name;
    private EditText ET_name, medical_officeTel, medical_position_edit, medical_department_edit, medical_company_edit;
    private TextWatcher mTextWatcher;
    private TextView name_text_hintname;
    private TextView name_text_hintnameafter;
    private int editStart;
    private int editEnd;
    private int MAX_COUNT = 16;
    private LinearLayout medicalNumberLl;
    private CameraImageDialog cid;
    public static final String HEAD_ICON_DIC = Environment
            .getExternalStorageDirectory()
            + File.separator
            + "ipNetPhone"
            + File.separator + "headIcon";
    private DisplayImageListener mDisplayImageListener = null;
    private ImageView headRv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_medical);
        init();
        if (savedInstanceState != null) {
            String path = (String) savedInstanceState.getString(KEY_FILE_ABSOLUTELY);
            String croppedIconPath = (String) savedInstanceState
                    .getString(KEY_FILE_CROPPEDICON_PATH);
            if (!TextUtils.isEmpty(path)) {
                headIconFile = new File(path);
            }
            if (!TextUtils.isEmpty(croppedIconPath)) {
                croppedIconFile = new File(croppedIconPath);
            }
        }
        mDisplayImageListener = new DisplayImageListener();
    }

    private void init() {
        TitleBar titleBar = getTitleBar();
        titleBar.setTitle("完善资料");
        titleBar.setTitleTextColor(Color.BLACK);
        titleBar.enableBack();
        medical_name = (LinearLayout) findViewById(R.id.medical_include_name);
        medical_company_edit = (EditText) findViewById(R.id.medical_company_edit);
        medical_department_edit = (EditText) findViewById(R.id.medical_department_edit);
        medical_position_edit = (EditText) findViewById(R.id.medical_position_edit);
        medical_officeTel = (EditText) findViewById(R.id.medical_officeTel);
        medical_officeTel.setFilters(new InputFilter[]{new InputFilter.LengthFilter(15)});
        medical_position_edit.setFilters(new InputFilter[]{new InputFilter.LengthFilter(32)});
        medical_department_edit.setFilters(new InputFilter[]{new InputFilter.LengthFilter(32)});
        medical_company_edit.setFilters(new InputFilter[]{new InputFilter.LengthFilter(32)});
        medical_officeTel.setInputType(InputType.TYPE_CLASS_NUMBER);
        ET_name = (EditText) medical_name.findViewById(R.id.ET_name);
        name_text_hintname = (TextView) medical_name.findViewById(R.id.name_hint_name);
        name_text_hintnameafter = (TextView) medical_name.findViewById(name_hint_nameafter);
        button_next = (Button) findViewById(R.id.btn_medical_next);
        button_back = (Button) findViewById(R.id.back_btn);
        button_next.setOnClickListener(mbtnHandleEventListener);
        button_back.setOnClickListener(mbtnHandleEventListener);
        medical_name = (LinearLayout) findViewById(R.id.medical_include_name);
        medicalNumberLl = (LinearLayout) findViewById(R.id.medical_number_tv);
        headRv = (ImageView) medicalNumberLl.findViewById(R.id.btn_head);
        showHead(AccountManager.getInstance(MedicalApplication.context).getAccountInfo().getHeadPreviewUrl());
        medicalNumberLl.setOnClickListener(mbtnHandleEventListener);

        ((TextView) (findViewById(R.id.medical_number_tv).findViewById(R.id.video_number))).setText(AccountManager.getInstance(MedicalApplication.context).getNube());

        mTextWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                name_text_hintname.setVisibility(View.GONE);
                name_text_hintnameafter.setVisibility(View.GONE);
                if (ET_name.getText().toString().isEmpty()) {
                    button_next.setBackgroundResource(R.drawable.button_btn_notclick);
                } else {
                    button_next.setClickable(true);
                    button_next.setBackgroundResource(R.drawable.button_selector);
                }
                editStart = ET_name.getSelectionStart();
                editEnd = ET_name.getSelectionEnd();
                ET_name.removeTextChangedListener(mTextWatcher);
                while (calculateLength(editable.toString()) > MAX_COUNT) {
                    editable.delete(editStart - 1, editEnd);
                    editStart--;
                    editEnd--;
                }
                ET_name.setSelection(editStart);
                ET_name.addTextChangedListener(mTextWatcher);
            }
        };
        ET_name.addTextChangedListener(mTextWatcher);
    }

    private void showHead(final String str) {
        DisplayImageOptions options = new DisplayImageOptions.Builder()
                .showStubImage(R.drawable.head)//设置图片在下载期间显示的图片
                .showImageForEmptyUri(R.drawable.head)//片加载/解码过程中错误时候显示的图片设置图片Uri为空或是错误的时候显示的图片
                .showImageOnFail(R.drawable.head)//设置图
                .cacheInMemory(true)//是否緩存都內存中
                .cacheOnDisc(true)//是否緩存到sd卡上
                .bitmapConfig(Bitmap.Config.RGB_565)//设置为RGB565比起默认的ARGB_8888要节省大量的内存
                .delayBeforeLoading(100)//载入图片前稍做延时可以提高整体滑动的流畅度
                .build();
        ImageLoader imageLoader = ImageLoader.getInstance();
        imageLoader.displayImage(str, headRv,
                options, mDisplayImageListener);
    }

    private int calculateLength(String etstring) {

        char[] ch = etstring.toCharArray();

        int varlength = 0;
        for (int i = 0; i < ch.length; i++) {
            if ((ch[i] >= 0x2E80 && ch[i] <= 0xFE4F)
                    || (ch[i] >= 0xA13F && ch[i] <= 0xAA40) || ch[i] >= 0x80) {
                varlength = varlength + 2;
            } else {
                varlength++;
            }
        }
        return varlength;

    }


    @Override
    public void todoClick(int i) {
        switch (i) {
            case R.id.btn_medical_next:
                Intent intent_next = new Intent(MedicalActivity.this, MedicalApproveActivity.class);
                String name = ET_name.getText().toString().trim();
                String company = medical_company_edit.getText().toString().trim();
                String department = medical_department_edit.getText().toString().trim();
                String position = medical_position_edit.getText().toString().trim();
                String officeTel = medical_officeTel.getText().toString().trim();
                String headUrl = AccountManager.getInstance(MedicalApplication.context).getAccountInfo().getHeadPreviewUrl();
                if (headUrl==null||headUrl.length()==0) {
                    CustomToast.show(MedicalActivity.this, "请上传头像", Toast.LENGTH_LONG);
                    return;
                }

                if (TextUtils.isEmpty(name) || TextUtils.isEmpty(company) || TextUtils.isEmpty(department) || TextUtils.isEmpty(position) || TextUtils.isEmpty(officeTel)) {
                    CustomToast.show(MedicalActivity.this, "资料不能为空", Toast.LENGTH_LONG);
                    return;
                }

                if (!isPhoneNumber(officeTel)){
                    CustomToast.show(MedicalActivity.this, "电话格式不正确", Toast.LENGTH_LONG);
                    return;
                }

                intent_next.putExtra("name", name);
                intent_next.putExtra("company", company);
                intent_next.putExtra("department", department);
                intent_next.putExtra("position", position);
                intent_next.putExtra("officeTel", officeTel);
                startActivity(intent_next);
                break;
            case R.id.back_btn:
                finish();
                break;
            case R.id.medical_number_tv:
                showDialog();
                break;
            default:
                break;
        }
    }

    private boolean isPhoneNumber(String phoneNumber){
        boolean isValid = false;
        String expression ="1([\\d]{10})|((\\+[0-9]{2,4})?\\(?[0-9]+\\)?-?)?[0-9]{7,8}";
        CharSequence inputStr = phoneNumber;
        Pattern pattern = Pattern.compile(expression);
        Matcher matcher = pattern.matcher(inputStr);
        if (matcher.matches() ) {
            isValid = true;
        }
        return isValid;
    }

    private void showDialog() {


        cid = new cn.redcdn.hvs.util.CameraImageDialog(MedicalActivity.this,
                R.style.contact_del_dialog);

        cid.setCameraClickListener(new cn.redcdn.hvs.util.CameraImageDialog.CameraClickListener() {

            @Override
            public void clickListener() {
                boolean result = CommonUtil.selfPermissionGranted(MedicalActivity.this, Manifest.permission.CAMERA);
                if (!result){
                    CustomToast.show(MedicalActivity.this, "请开启相机权限", CustomToast.LENGTH_SHORT);
                }
                camera();
            }
        });
        cid.setPhoneClickListener(new cn.redcdn.hvs.util.CameraImageDialog.PhoneClickListener() {

            @Override
            public void clickListener() {
                photoFile();
            }
        });
        cid.setNoClickListener(new cn.redcdn.hvs.util.CameraImageDialog.NoClickListener() {

            @Override
            public void clickListener() {
                cid.dismiss();
            }
        });
        Window window = cid.getWindow();

        window.setGravity(Gravity.BOTTOM);

        cid.setCanceledOnTouchOutside(true);

        cid.show();

        WindowManager windowManager = getWindowManager();
        Display display = windowManager.getDefaultDisplay();
        WindowManager.LayoutParams lp = cid.getWindow().getAttributes();
        lp.width = (int) (display.getWidth()); // 设置宽度
        lp.height = (int) (0.3 * display.getHeight()); // 设置高度
        cid.getWindow().setAttributes(lp);
    }

    private void photoFile() {
            String state = Environment.getExternalStorageState();
            if (state.equals(Environment.MEDIA_MOUNTED)) {

                Intent getAlbum = new Intent(Intent.ACTION_GET_CONTENT);

                getAlbum.setType(IMAGE_TYPE);
                startActivityForResult(getAlbum, IMAGE_CODE);

            } else {
                CustomToast.show(getApplicationContext(), "请开启存储权限",
                        Toast.LENGTH_SHORT);
            }
    }

    private void camera() {
            String state = Environment.getExternalStorageState();
            if (state.equals(Environment.MEDIA_MOUNTED)) {
                initHeadIconFile();
                Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
                Uri imageUri = FileProvider.getUriForFile(this, "com.jph.takephoto.fileprovider", headIconFile);//通过FileProvider创建一个content类型的Uri
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); //添加这一句表示对目标应用临时授权该Uri所代表的文件
                intent.setAction(MediaStore.ACTION_IMAGE_CAPTURE);//设置Action为拍照
                intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                startActivityForResult(intent, CAMERA);
            } else {
                CustomToast.show(getApplicationContext(), "请开启存储权限",
                        Toast.LENGTH_SHORT);
            }
    }


    private void initHeadIconFile() {
        headIconFile = new File(HEAD_ICON_DIC);
        if (!headIconFile.exists()) {
            headIconFile.mkdirs();
        }
        headIconFile = new File(HEAD_ICON_DIC, "nube_photo"
                + System.currentTimeMillis() + ".jpg");
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        CustomLog.e(TAG, "onActivityResult");
        if (resultCode != RESULT_OK) {
            return;
        }
        if (requestCode == CAMERA) {
            CustomLog.d(TAG, "进入拍照后currentMyPid===="
                    + android.os.Process.myPid());
            CustomLog.d(TAG, "onActivityResult..CAMERA..headIconFile.getPath()="
                    + headIconFile.getPath());

            croppedIconFilepath = getThumPath(headIconFile.getPath(), 400);
            upLoad(getThumPath(headIconFile.getPath(), 400));
        } else {
            if (requestCode == IMAGE_CODE) {
                String filePath = "";
                ContentResolver resolver = getContentResolver();
                Uri originalUri = data.getData(); // 获得图片的uri
                if (originalUri != null) {
                    filePath = DocumentsHelper.getPath(MedicalActivity.this,
                            originalUri);
                }

                if (TextUtils.isEmpty(filePath)) {
                    filePath = data.getDataString();
                    if (!TextUtils.isEmpty(filePath) && filePath.startsWith("file:///")) {
                        filePath = filePath.replace("file://", "");
                        try {
                            // java的文件系统是linux,而编码格式是UTF-8的编码格式
                            filePath = URLDecoder.decode(filePath, "UTF-8");
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                    }
                }
                if (filePath == null || filePath.equalsIgnoreCase("")) {
                    CustomToast.show(getApplicationContext(), "无法获取图片路径",
                            Toast.LENGTH_SHORT);
                    return;
                }
                if (BitmapUtils.isImageType(filePath)) {
                    upLoad(getThumPath(filePath, 400));

                } else
                    CustomToast.show(getApplicationContext(), "文件格式错误",
                            Toast.LENGTH_SHORT);
            }
        }
    }


    private void upLoad(String path) {
        UploadManager.UploadImageListener listener = new UploadManager.UploadImageListener() {
            @Override
            public void onSuccess(CdnUploadDataInfo dataInfo) {
                MedicalActivity.this.removeLoadingView();
                String filepath = dataInfo.getFilepath();
                if (filepath == null) {
                    CustomToast.show(getApplicationContext(), "上传头像失败",
                            Toast.LENGTH_SHORT);
                    return;
                }
                CustomToast.show(getApplicationContext(), "上传头像成功",
                        Toast.LENGTH_SHORT);
                cid.dismiss();
                AccountManager.getInstance(getApplicationContext())
                        .getAccountInfo().setHeadThumUrl(filepath);
                AccountManager.getInstance(getApplicationContext())
                        .getAccountInfo().setHeadPreviewUrl(filepath);
                show(filepath);
            }

            private void show(final String str) {
                DisplayImageOptions options = new DisplayImageOptions.Builder()
                        .showStubImage(R.drawable.head)//设置图片在下载期间显示的图片
                        .showImageForEmptyUri(R.drawable.head)//片加载/解码过程中错误时候显示的图片设置图片Uri为空或是错误的时候显示的图片
                        .showImageOnFail(R.drawable.head)//设置图
                        .cacheInMemory(true)//是否緩存都內存中
                        .cacheOnDisc(true)//是否緩存到sd卡上
                        .bitmapConfig(Bitmap.Config.RGB_565)//设置为RGB565比起默认的ARGB_8888要节省大量的内存
                        .delayBeforeLoading(100)//载入图片前稍做延时可以提高整体滑动的流畅度
                        .build();
                ImageLoader imageLoader = ImageLoader.getInstance();
                imageLoader.displayImage(str, headRv,
                        options, mDisplayImageListener);
            }

            @Override
            public void onFailed(int statusCode, String msg) {
                MedicalActivity.this.removeLoadingView();
                if (HttpErrorCode.checkNetworkError(statusCode)) {
                    CustomToast.show(MedicalActivity.this, "网络不给力，请检查网络！",
                            Toast.LENGTH_LONG);
                    return;
                }
                if (statusCode == SettingData.getInstance().tokenUnExist
                        || statusCode == SettingData.getInstance().tokenInvalid) {
                    CustomToast.show(getApplicationContext(), "token无效",
                            Toast.LENGTH_SHORT);
                    AccountManager.getInstance(getApplicationContext()).tokenAuthFail(
                            statusCode);
                }
                CustomToast.show(getApplicationContext(), "上传头像失败=" + statusCode,
                        Toast.LENGTH_SHORT);
            }
        };
        UploadManager.getInstance().uploadImage(new File(path), listener);


        MedicalActivity.this.showLoadingView("上传头像中", new DialogInterface.OnCancelListener() {

            @Override
            public void onCancel(DialogInterface dialog) {
                dialog.dismiss();
                UploadManager.getInstance().cancel();
                CustomToast.show(getApplicationContext(), "上传头像取消", Toast.LENGTH_SHORT);
            }
        });

    }

    private String getThumPath(String oldPath, int bitmapMaxWidth) {
        return BitmapUtils.getThumPath(oldPath, bitmapMaxWidth);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        MedicalActivity.this.finish();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        CustomLog.e(TAG, "onSaveInstanceState");
        // 针对三星4.1.2 拍照手返回 文件路径丢失，先保存文件路径，然后在oncreate方法进行恢复
        if (headIconFile != null
                && !TextUtils.isEmpty(headIconFile.getAbsolutePath())) {
            outState.putString(KEY_FILE_ABSOLUTELY, headIconFile.getAbsolutePath());
        }
        if (croppedIconFilepath != null
                && !croppedIconFilepath.equalsIgnoreCase("")) {
            outState.putString(KEY_FILE_CROPPEDICON_PATH, croppedIconFilepath);
        }
    }
}
