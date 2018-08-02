package cn.redcdn.hvs.util;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import cn.redcdn.hvs.MedicalApplication;
import cn.redcdn.hvs.R;
import cn.redcdn.hvs.base.BaseActivity;
import cn.redcdn.hvs.contacts.contact.diaplayImageListener.DisplayImageListener;
import cn.redcdn.hvs.im.activity.ViewImages.PhotoView;
import cn.redcdn.hvs.im.activity.ViewImages.PhotoViewAttacher;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.nostra13.universalimageloader.core.ImageLoader;

import java.io.File;

public class OpenBigImageActivity extends BaseActivity {
    public static String CURRENT_HEAD_URL = "current_head_url";
    public static String DATE_URL = "dateUrl";
    public static String DATE_TYPE = "dateType";


    public static String DATE_value_image = "drawable_id";

    public static int DATE_TYPE_Internet = 0;//internet
    public static int DATE_TYPE_memory = 1;//memory
    public static int DATE_TYPE_value_image = 2;//value_image
//    public static String CURRENT_SEX="current_sex";

    private PhotoView originalIcon;
    private DisplayImageListener mDisplayImageListener = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.original_contact_icon);
        originalIcon = (PhotoView) findViewById(R.id.original_icon);
        originalIcon.setOnViewTapListener(new PhotoViewAttacher.OnViewTapListener() {

            @Override
            public void onViewTap(View view, float x, float y) {
                OpenBigImageActivity.this.finish();
            }
        });

        loadContactIcon();
    }

    private void loadContactIcon() {
        // 加载联系人头像
        Intent i = getIntent();
        Integer type = i.getIntExtra(DATE_TYPE, -1);
//        String sex = i.getStringExtra(CURRENT_SEX);
        if(type==null){
            CustomToast.show(OpenBigImageActivity.this, "对不起，您没有传图片类型，显示默认图片", CustomToast.LENGTH_SHORT);
            Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.default_head_image);
            originalIcon.setImageBitmap(bitmap);
        }else if(type==-1){
            CustomToast.show(OpenBigImageActivity.this, "对不起，您没有传图片类型,使用了默认值，显示默认图片", CustomToast.LENGTH_SHORT);
            Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.default_head_image);
            originalIcon.setImageBitmap(bitmap);
        }
        if (type == DATE_TYPE_Internet) {
            String headUrl = i.getStringExtra(DATE_URL);//服务器虚拟地址上图片的放大
            if(headUrl==null){
                CustomToast.show(OpenBigImageActivity.this, "该图片地址为空，显示默认图片", CustomToast.LENGTH_SHORT);
                Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.default_head_image);
                originalIcon.setImageBitmap(bitmap);
            }else {
                if (headUrl.equals(null) || headUrl.equals("")) {
                    CustomToast.show(OpenBigImageActivity.this, "该图片不存在,显示默认图片", CustomToast.LENGTH_SHORT);
                    Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.default_head_image);
                    originalIcon.setImageBitmap(bitmap);
                } else {
//                    Glide.with(this)
//                            .load(headUrl)
////                .placeholder(IMCommonUtil.getHeadIdBySex(sex))
////                .error(IMCommonUtil.getHeadIdBySex(sex))
//                            .diskCacheStrategy(DiskCacheStrategy.SOURCE).crossFade()
//                            .into(originalIcon);
                    mDisplayImageListener = new DisplayImageListener();
                    ImageLoader imageLoader = ImageLoader.getInstance();

                    imageLoader.displayImage(headUrl,
                            originalIcon,
                            MedicalApplication.shareInstance().options,
                            mDisplayImageListener);
                }
            }
        } else if (type == DATE_TYPE_memory) { //本地图片的放大
            // 设置默认头像
//            originalIcon.setImageResource(IMCommonUtil.getHeadIdBySex(sex));
            String headUrl = i.getStringExtra(DATE_URL);
            if(headUrl==null){
                CustomToast.show(OpenBigImageActivity.this, "该图片地址为空，显示默认图片", CustomToast.LENGTH_SHORT);
                Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.default_head_image);
                originalIcon.setImageBitmap(bitmap);
            }else {
                if (headUrl.equals(null) || headUrl.equals("")) {
                    CustomToast.show(OpenBigImageActivity.this, "该图片不存在，显示默认图片", CustomToast.LENGTH_SHORT);
                    Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.default_head_image);
                    originalIcon.setImageBitmap(bitmap);
                } else {
                    Uri uri = Uri.fromFile(new File(headUrl));
                    originalIcon.setImageURI(uri);
                }
            }
        } else if (type == DATE_TYPE_value_image) {  //资源文件中图片的放大（注意：headUrl传过来的值应该是value_drawable）
            Integer image = i.getIntExtra(DATE_value_image, -1);
            if(image==null){
                CustomToast.show(OpenBigImageActivity.this, "该图片地址为空，显示默认图片", CustomToast.LENGTH_SHORT);
                Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.default_head_image);
                originalIcon.setImageBitmap(bitmap);
            }else {
                if (image == -1) {
                    CustomToast.show(OpenBigImageActivity.this, "该图片不存在，显示默认图片", CustomToast.LENGTH_SHORT);
                    Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.default_head_image);
                    originalIcon.setImageBitmap(bitmap);
                } else {
                    Bitmap bitmap = BitmapFactory.decodeResource(getResources(), image);
//            Bitmap bitmap=i.getParcelableExtra("bitmap");
                    originalIcon.setImageBitmap(bitmap);
                }
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

}
