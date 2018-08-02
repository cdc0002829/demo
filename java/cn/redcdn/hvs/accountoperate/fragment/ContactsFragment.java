package cn.redcdn.hvs.accountoperate.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import cn.redcdn.hvs.R;


/**
 * Created by thinkpad on 2017/2/7.
 *
 */

public class ContactsFragment extends BaseFragment {
        //定义标题栏上的按钮
        private ImageButton Ib_addContacts;

        @Override
        protected View createView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View l1 = View.inflate(getActivity(), R.layout.contacts_fragment, null);
            /**
             * 通过button按钮跳转到手机通讯录推荐界面
             * */
//            l1.findViewById(R.id.button_addContacts).setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View view) {
//                    startActivity(new Intent(getActivity(), RecommondPhoneActivity.class));
//                }
//            });
            /*
            * 通过点击按钮展示dialog中的添加好友和发起多人会诊
            */
//            l1.findViewById(R.id.Ib_addContacts).setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View view) {
//                    startActivity(new Intent(getActivity(),DialogActivity.class));
//                }
//            });
            return l1;
        }

        @Override
        protected void setListener() {

        }

        @Override
        protected void initData() {

        }
    }

