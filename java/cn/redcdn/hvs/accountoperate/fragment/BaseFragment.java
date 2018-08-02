package cn.redcdn.hvs.accountoperate.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import java.lang.reflect.Field;

/**
 * @ClassName: BaseFragment
 * @Description: Fragment的基类
 * @Author: yaodetao
 * @Date: 2017/1/7 20:01.
 */

public abstract class BaseFragment extends Fragment {
    private View mViewRoot;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // 为空就去加载布局，onCreateView在界面切换的时候会被多次调用,防止界面跳转回来的时候显示空白
        if (mViewRoot == null) {
            mViewRoot = createView(inflater, container, savedInstanceState);
        }
        return mViewRoot;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        initView();
        // view初始化完毕，就设置设置各种监听器
        setListener();
        // 加载数据
        initData();
    }

    /**
     * 当前的界面被切换出去的时候被调用,解决ViewGroup只有一个子View的bug
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (mViewRoot != null) {
            ViewParent parent = mViewRoot.getParent();
            if (parent instanceof ViewGroup) {
                ViewGroup viewGroup = (ViewGroup) parent;
                viewGroup.removeView(mViewRoot);
            }
        }

        // 解决Fragment嵌套问题（MainActivity切换标签崩溃的问题）
        try {
            Field childFragmentManager = Fragment.class.getDeclaredField("mChildFragmentManager");
            childFragmentManager.setAccessible(true);
            childFragmentManager.set(this, null);

        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    protected abstract View createView(LayoutInflater inflater, ViewGroup container,
                                       Bundle savedInstanceState);

    /**
     * 初始化布局
     */
    protected void initView() {

    }

    /**
     * 设置监听器
     */
    protected abstract void setListener();

    /**
     * 加载数据
     */
    protected abstract void initData();
}
