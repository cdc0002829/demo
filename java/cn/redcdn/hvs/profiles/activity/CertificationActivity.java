package cn.redcdn.hvs.profiles.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.ImageView;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;

import cn.redcdn.hvs.AccountManager;
import cn.redcdn.hvs.R;
import cn.redcdn.hvs.base.BaseActivity;
import cn.redcdn.hvs.profiles.listener.DisplayImageListener;
import cn.redcdn.hvs.util.OpenBigImageActivity;
import cn.redcdn.hvs.util.TitleBar;
import cn.redcdn.log.CustomLog;

import static cn.redcdn.hvs.AccountManager.getInstance;

/**
 * Created by Administrator on 2017/3/11.
 */
public class CertificationActivity extends BaseActivity {

    private ImageView imageIv;
    private DisplayImageListener mDisplayImageListener = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_certifycation);
        TitleBar titleBar = getTitleBar();
        titleBar.enableBack();
        String workUnitType = getInstance(getApplicationContext())
                .getAccountInfo().getWorkUnitType();
        if (!workUnitType.equals("")){
            if (workUnitType.equals("2")){
                titleBar.setTitle("医疗从业人员认证");
            }else {
                titleBar.setTitle("医生认证");
            }
        }

        initView();
        initData();
        mDisplayImageListener=new DisplayImageListener();
    }

    private void initView() {
        imageIv = (ImageView) findViewById(R.id.image);
    }

    private void initData() {
      String certificatePreview=  AccountManager.getInstance(this)
                .getAccountInfo().certificateThum;
        if (certificatePreview != null && !certificatePreview.equalsIgnoreCase("")) {
            CustomLog.d(TAG, "显示图片");
            show(certificatePreview);
            imageIv.setOnClickListener(mbtnHandleEventListener);
        }
    }

    private void show(String image) {
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
        imageLoader.displayImage(image, imageIv, options, mDisplayImageListener);
    }

    @Override
    public void todoClick(int i) {
        super.todoClick(i);
        switch (i){
            case R.id.image:
                Intent intent_inputimage = new Intent(this, OpenBigImageActivity.class);
                intent_inputimage.putExtra(OpenBigImageActivity.DATE_TYPE, OpenBigImageActivity.DATE_TYPE_Internet);
                intent_inputimage.putExtra(OpenBigImageActivity.DATE_URL,  AccountManager.getInstance(this)
                        .getAccountInfo().getCertificateThum());
                startActivity(intent_inputimage);
            break;
        }
    }
}
