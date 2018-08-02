package cn.redcdn.hvs.accountoperate.fragment;

import android.support.v4.app.Fragment;

/**
 * Created by thinkpad on 2017/2/9.
 */

public class FragmentFactory {
    /**
     * 根据不同的position生产对应的Fragment对象
     * */
  public static Fragment create(int position){
      Fragment fragment = null;
      switch (position){
          case 0:
              fragment = new MessageFragment();
              break;
          case 1:
              fragment = new SubscribeFragment();
              break;
          case 2:
              fragment = new ContactsFragment();
              break;
          case 3:
              fragment = new MyFragment();
              break;
      }
      return fragment;
  }
}
