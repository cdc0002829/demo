package cn.redcdn.hvs;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import cn.redcdn.hvs.base.BaseActivity;
import cn.redcdn.hvs.base.FragmentFactory;
import cn.redcdn.hvs.im.MessageFragment;
import cn.redcdn.hvs.im.activity.ChatActivity;
import cn.redcdn.hvs.im.agent.AppP2PAgentManager;
import cn.redcdn.hvs.im.dao.NoticesDao;
import cn.redcdn.hvs.im.provider.ProviderConstant;
import cn.redcdn.hvs.util.CommonUtil;
import cn.redcdn.log.CustomLog;

public class HomeActivity extends BaseActivity implements RadioGroup.OnCheckedChangeListener {
    private ViewPager main_ViewPager;
    private RadioGroup main_tab_RadioGroup;
    private RadioButton rb_message, rb_contacts, rb_subscribe, rb_me;

    private final String TAG = "HomeActivity";
    private static final String TAG_MESSAGE = MessageFragment.class.getName();
    private static final String CHAT_ACTIVITY = ChatActivity.class.getName();
    public static final int TAB_INDEX_CHAT = 0;
    // 记录顶层操作，当前fragment的TAG，上面定义的4个固定TAG
    private static String currentTag = "";

    //    private final int REQUEST_READ_PHONE_STATE = 111;

    private NoticesDao noticeDao = null;
    private TextView newNoticeNum = null;
    private MessageObserver msgObserver = null;

    public static final String TAB_INDICATOR_INDEX = "HomeActivity.indicator.index";
    public static final String TAB_INDEX_MESSAGE = "CHAT_ACTIVITY";

    public int index;

    public static Boolean isFromChatActivity = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        allowTwiceToExit();
        noticeDao = new NoticesDao(this);
        //初始化布局
        InitView();
        //初始化viewPager
        InitViewPager();

        //        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE);
        //
        //        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
        //            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_PHONE_STATE}, REQUEST_READ_PHONE_STATE);
        //        } else {
        //            //TODO
        //        }

        CustomLog.d(TAG, "[性能监控] ==> 启动SIP服务 start ");
        // 启动CallManageService（SIP服务）
        if (Build.CPU_ABI.equalsIgnoreCase("armeabi-v7a")) {
            //            new Thread(new Runnable() {
            //                @Override
            //                public void run() {
            //                    // TODO Auto-generated method stub
            //                    CustomLog.d(TAG, "AppP2PAgentManager 开始启动");
            //                    AppP2PAgentManager.init();
            //                }
            //            }).start();
            AppP2PAgentManager.init();
        } else {
            CommonUtil.showToast(getString(R.string.cpu_version_too_low));
            CustomLog.d(TAG, "Toast:CPU ABI版本过低,启动聊天服务失败");
        }
        CustomLog.d(TAG, "[性能监控] ==> 启动SIP服务 end ");

        if (msgObserver == null) {
            msgObserver = new MessageObserver();
            getContentResolver().registerContentObserver(
                ProviderConstant.NETPHONE_NOTICE_URI, true, msgObserver);
        }

        //应用重启重新发送未完成的消息
        MedicalApplication.getFileTaskManager().updateRunningTask2Fail();

        IntentFilter filter = new IntentFilter();
        filter.addAction("NoticeCountBroaddcase");
        this.registerReceiver(noticeChangeReceive, filter);

    }


    BroadcastReceiver noticeChangeReceive = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("NoticeCountBroaddcase")) {
                int count = intent.getIntExtra("newNoticeCount", 0);
                CustomLog.d(TAG, "updateNoticeCount");
                updateNoticesInfo(count);
            }
        }
    };

    //    @Override
    //    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    //        switch (requestCode) {
    //            case REQUEST_READ_PHONE_STATE:
    //                if ((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
    //                    //TODO
    //                    CustomLog.d(TAG,"权限申请成功");
    //                }
    //                break;
    //
    //            default:
    //                break;
    //        }
    //    }


    private void InitViewPager() {
        main_ViewPager = (ViewPager) findViewById(R.id.main_ViewPager);
        //填充ViewPager
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());
        main_ViewPager.setAdapter(adapter);
        //设置当前为第一个页面
        main_ViewPager.setCurrentItem(0);
        //设置viewPager的页面监听器
        main_ViewPager.setOnPageChangeListener(new ViewPagerListener());
    }


    private void InitView() {
        main_tab_RadioGroup = (RadioGroup) findViewById(R.id.main_tab_RadioGroup);

        int btnWidth = (int) getResources().getDimension(R.dimen.x50);
        int btnHeight = (int) getResources().getDimension(R.dimen.y50);

        rb_message = (RadioButton) findViewById(R.id.rb_message);
        Drawable drawable_message = getResources().getDrawable(R.drawable.message_button);
        drawable_message.setBounds(0, 0, btnWidth, btnHeight);
        rb_message.setCompoundDrawables(null, drawable_message, null, null);

        rb_contacts = (RadioButton) findViewById(R.id.rb_contacts);
        Drawable drawable_contacts = getResources().getDrawable(R.drawable.subscribe_button);
        drawable_contacts.setBounds(0, 0, btnWidth, btnHeight);
        rb_contacts.setCompoundDrawables(null, drawable_contacts, null, null);

        rb_subscribe = (RadioButton) findViewById(R.id.rb_subscribe);
        Drawable drawable_subs = getResources().getDrawable(R.drawable.contact_button);
        drawable_subs.setBounds(0, 0, btnWidth, btnHeight);
        rb_subscribe.setCompoundDrawables(null, drawable_subs, null, null);

        rb_me = (RadioButton) findViewById(R.id.rb_me);
        Drawable drawable_me = getResources().getDrawable(R.drawable.me_button);
        drawable_me.setBounds(0, 0, btnWidth, btnHeight);
        rb_me.setCompoundDrawables(null, drawable_me, null, null);

        main_tab_RadioGroup.check(R.id.rb_message);

        main_tab_RadioGroup.setOnCheckedChangeListener(this);
        newNoticeNum = (TextView) this.findViewById(R.id.total_new_notice_num);

    }


    @Override
    protected void showLoadingView(String message) {
        super.showLoadingView(message);
    }


    @Override
    public void onCheckedChanged(RadioGroup radioGroup, int i) {
        //获取当前被选中的RadioButton的ID,用于改变viewPager的页面
        int current = 0;
        switch (i) {
            case R.id.rb_message:
                current = 0;
                currentTag = TAG_MESSAGE;
                break;
            case R.id.rb_subscribe:
                current = 1;
                break;
            case R.id.rb_contacts:
                current = 2;
                break;
            case R.id.rb_me:
                current = 3;
                break;
        }
        if (main_ViewPager.getCurrentItem() != current) {
            main_ViewPager.setCurrentItem(current);
        }
    }


    private class ViewPagerListener implements ViewPager.OnPageChangeListener {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

        }


        @Override
        public void onPageSelected(int position) {
            //获取当前页面用于改变radioButton的状态
            int current = main_ViewPager.getCurrentItem();
            switch (current) {
                case 0:
                    main_tab_RadioGroup.check(R.id.rb_message);

                    break;
                case 1:
                    main_tab_RadioGroup.check(R.id.rb_subscribe);
                    break;
                case 2:
                    main_tab_RadioGroup.check(R.id.rb_contacts);
                    break;
                case 3:
                    main_tab_RadioGroup.check(R.id.rb_me);
                    break;
            }
        }


        @Override
        public void onPageScrollStateChanged(int state) {

        }
    }


    public class ViewPagerAdapter extends FragmentStatePagerAdapter {
        private String[] rbs;


        public ViewPagerAdapter(FragmentManager fm) {
            super(fm);
            rbs = CommonUtil.getStringArray(R.array.rb_names);
        }


        /*返回每一页需要的fragment*/
        @Override
        public Fragment getItem(int position) {
            return FragmentFactory.create(position);
        }


        @Override
        public int getCount() {
            return rbs.length;
        }


        @Override
        public CharSequence getPageTitle(int position) {
            return rbs[position];
        }


        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            //super.destroyItem(container, position, object);
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        // updateNoticesInfo();

        showMessageFragment();
    }


    private void showMessageFragment() {

        if (isFromChatActivity) {
            main_ViewPager.setCurrentItem(0);
            isFromChatActivity = false;
        }

    }


    /**
     * @Title: getCurrentTag
     * @Description: 返回当前显示页面tag
     * @return: String
     */
    public String getCurrentTag() {
        return currentTag;
    }


    private void updateNoticesInfo(int count) {
        // count = noticeDao.getNewNoticeCount();
        if (count == 0) {
            newNoticeNum.setVisibility(View.INVISIBLE);
        } else {
            if (count > 99) {
                // newNoticeNum
                //     .setBackgroundResource(R.drawable.chat_unread_count_bar);
                newNoticeNum.setText(R.string.main_bottom_count_99);
            } else {
                // newNoticeNum
                //     .setBackgroundResource(R.drawable.chat_unread_count_bar);
                newNoticeNum.setText(String.valueOf(count));
            }
            newNoticeNum.setVisibility(View.VISIBLE);
        }

    }


    private class MessageObserver extends ContentObserver {

        public MessageObserver() {
            super(new Handler());
        }


        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            // updateNoticesInfo();
        }
    }


    @Override protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(noticeChangeReceive);
    }
}
