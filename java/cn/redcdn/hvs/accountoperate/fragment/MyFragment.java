package cn.redcdn.hvs.accountoperate.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;


import cn.redcdn.hvs.R;

/**
 * Created by thinkpad on 2017/2/7.
 */

public class MyFragment extends BaseFragment {
    @Override
    protected View createView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        LinearLayout l3 = (LinearLayout) inflater.inflate(R.layout.my_fragment,null);
        return l3;
    }

    @Override
    protected void setListener() {

    }

    @Override
    protected void initData() {

    }
}
