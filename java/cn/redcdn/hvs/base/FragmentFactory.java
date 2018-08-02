package cn.redcdn.hvs.base;

import android.support.v4.app.Fragment;

import cn.redcdn.hvs.contacts.ContactsFragment;
import cn.redcdn.hvs.im.MessageFragment;
import cn.redcdn.hvs.officialaccounts.OfficialAccountFragment;
import cn.redcdn.hvs.profiles.ProfilesFragment;

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
              fragment = new OfficialAccountFragment();
              break;
          case 2:
              fragment = new ContactsFragment();
              break;
          case 3:
              fragment = new ProfilesFragment();
              break;
      }
      return fragment;
  }
}
