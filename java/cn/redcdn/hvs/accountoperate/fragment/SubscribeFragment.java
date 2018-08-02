package cn.redcdn.hvs.accountoperate.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import cn.redcdn.hvs.R;


/**
 * Created by thinkpad on 2017/2/14.
 */
public class SubscribeFragment extends BaseFragment{
    @Override
    protected View createView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        LinearLayout l2 = (LinearLayout) inflater.inflate(R.layout.subscribe_fragment,null);
        return l2;
    }

    @Override
    protected void setListener() {

    }

    @Override
    protected void initData() {

    }
}
