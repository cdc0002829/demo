package cn.redcdn.hvs.accountoperate.adapter;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;



import cn.redcdn.hvs.R;
import cn.redcdn.hvs.accountoperate.fragment.FragmentFactory;
import cn.redcdn.hvs.util.CommonUtil;

/**
 * Created by thinkpad on 2017/2/7.
 */
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
}
