package cn.redcdn.hvs.im.asyncTask;

import android.content.Context;
import android.text.TextUtils;
import cn.redcdn.hvs.MedicalApplication;
import cn.redcdn.hvs.im.bean.ContactFriendBean;
import cn.redcdn.hvs.im.bean.NumberCacheBean;
import cn.redcdn.hvs.im.dao.NetPhoneDaoImpl;
import cn.redcdn.hvs.im.dao.NumberCacheDao;
import com.butel.connectevent.base.CommonConstant;
import com.butel.connectevent.utils.LogUtil;
import com.butel.connectevent.utils.NetWorkUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


/**
 *
 * QueryPhoneNumberHelper.java
 * @Descripition: 通过视讯号查询手机号的帮助类
 * @Copyright: Copyright(C) 2015
 * @Company: 安徽青牛信息技术有限公司
 * @time : 2015-5-20 下午3:01:45
 */
public class QueryPhoneNumberHelper {

    public static String NOPHONENUMBER = "nonumber";

    /**
     * 通过视频号码查询手机号码，查找顺序为（1、本地联系人-》2、缓存表-》3、网络接口查询）
     * 当前一步失败时，才进行下一步查询；在第三步查询成功后，需要将结果保存到缓存表中。
     * @param number  视频号码
     * @param sync    是否是同步查询
     * @param lister  当sync==false时有意义
     * @return     手机号码或空字符串，当sync==true时，返回结果一样明确；
     *             当sync==false时,若前两部能查询到，则即刻返回，
     *             否则等第三步网络查询成功后通过回调告知结果，方法返回NULL；
     */
    public static String getPhoneNumberByNubeNumer(final String number,boolean sync ,final QueryNumberLister lister){
        Context context = MedicalApplication.getContext();
        // 查询缓存表
        final NumberCacheDao numberDao  = new NumberCacheDao(context);
        String phoneNumber = numberDao.getPhoneNumberByNebu(number);
        if(!TextUtils.isEmpty(phoneNumber)){
            if(phoneNumber.equals(NOPHONENUMBER)){
                return "";
            }else{
                return phoneNumber;
            }
        }

        // 缓存没有，查询本地联系人
        NetPhoneDaoImpl contactDao = new NetPhoneDaoImpl(context);
        ContactFriendBean friendPo = contactDao.queryFriendInfoByNube(number);
        if(friendPo!=null){//查到后插入缓存
            addPhoneAndNubeToCache(friendPo.getNumber(), number);
            return friendPo.getNumber();
        }

        // 二者都没有，查询网络
        //		String url = UrlConstant.getCommUrl(PrefType.KEY_BAIKU_PASSPORT_URL);
        // String url = UrlConstant.getCommUrl(PrefType.KEY_BAIKU_UNIFIEDSERVICET_URL);
        // RequestParams params = GetInterfaceParams.getSearchAccountParams(number);
        // HttpUtils http = new HttpUtils();
        if(sync){
            if(!NetWorkUtil.isNetworkConnected(context)){
                return "";
            }
            // SyncResult result = http.sendSync(HttpMethod.POST, url, params);
            // if(result!=null&&result.isOK()){
            //     String phone = getNumber(result.getResult(),true);
            //     if(!TextUtils.isEmpty(phone)){
            //         NumberCacheBean bean = new NumberCacheBean();
            //         bean.setId("");
            //         bean.setNebunumber(number);
            //         bean.setPhonenumber(phone);
            //         numberDao.insertItem(bean);
            //     }
            //     return NOPHONENUMBER.equals(phone)?"":phone;
            // }else{
            //     return "";
            // }

        }else{

            if(!NetWorkUtil.isNetworkConnected(context)){
                return "";
            }

            // http.send(HttpMethod.POST, url, params, new RequestCallBack<Object>() {
            //     @Override
            //     public void onStart() {
            //         super.onStart();
            //         if (lister != null) {
            //             lister.onStarted();
            //         }
            //     }
            //
            //     @Override
            //     public void onSuccess(Object result) {
            //         super.onSuccess(result);
            //         String phone = getNumber(result.toString(),true);
            //         if(!TextUtils.isEmpty(phone)){
            //             NumberCacheBean bean = new NumberCacheBean();
            //             bean.setId("");
            //             bean.setNebunumber(number);
            //             bean.setPhonenumber(phone);
            //             numberDao.insertItem(bean);
            //         }
            //
            //         if (lister != null) {
            //             lister.onFinished(true, NOPHONENUMBER.equals(phone)?"":phone);
            //         }
            //     }
            //
            //     @Override
            //     public void onFailure(Throwable error, String msg) {
            //         super.onFailure(error, msg);
            //         if (lister != null) {
            //             lister.onFinished(false, "");
            //         }
            //     }
            // });
        }

        return null;
    }

    public static String getPhoneNumberByNubeNumerFromLocal(final String number){
        Context context = MedicalApplication.getContext();
        // 查询缓存表
        final NumberCacheDao  numberDao  = new NumberCacheDao(context);
        String phoneNumber = numberDao.getPhoneNumberByNebu(number);
        if(!TextUtils.isEmpty(phoneNumber)){
            if(phoneNumber.equals(NOPHONENUMBER)){
                return "";
            }else{
                return phoneNumber;
            }
        }

        // 缓存没有，查询本地联系人
        NetPhoneDaoImpl contactDao = new NetPhoneDaoImpl(context);
        ContactFriendBean friendPo = contactDao.queryFriendInfoByNube(number);
        if(friendPo!=null){//查到本地插入缓存
            addPhoneAndNubeToCache(friendPo.getNumber(), number);
            return friendPo.getNumber();
        }

        return "";
    }

    /**
     * 逻辑同上
     * @param phone
     * @param sync
     * @param lister
     * @return
     */
    public static String getNebuNumberByPhone(final String phone,boolean sync , final QueryNumberLister lister){
        Context context = MedicalApplication.getContext();

        // 查询缓存表
        final NumberCacheDao  numberDao  = new NumberCacheDao(context);
        String nubeNumber = numberDao.getNebuNumberByPhone(phone);
        if(!TextUtils.isEmpty(nubeNumber)){
            return nubeNumber;
        }

        // 缓存没有，查询本地联系人
        NetPhoneDaoImpl contactDao = new NetPhoneDaoImpl(context);
        ContactFriendBean friendPo = contactDao.queryFriendInfoByPhone(phone);
        if(friendPo!=null){//查到本地插入缓存
            addPhoneAndNubeToCache(phone, friendPo.getNubeNumber());
            return friendPo.getNubeNumber();
        }

        // 二者都没有，查询网络
        //		String url = UrlConstant.getCommUrl(PrefType.KEY_BAIKU_PASSPORT_URL);
        // String url = UrlConstant.getCommUrl(PrefType.KEY_BAIKU_UNIFIEDSERVICET_URL);
        // RequestParams params = GetInterfaceParams.getSearchAccountParams(phone);
        // HttpUtils http = new HttpUtils();
        if(sync){
            if(!NetWorkUtil.isNetworkConnected(context)){
                return "";
            }
            // SyncResult result = http.sendSync(HttpMethod.POST, url, params);
            // if(result!=null&&result.isOK()){
            //     String nube = getNumber(result.getResult(),false);
            //     if(!TextUtils.isEmpty(nube)){
            //         NumberCacheBean bean = new NumberCacheBean();
            //         bean.setId("");
            //         bean.setNebunumber(nube);
            //         bean.setPhonenumber(phone);
            //         numberDao.insertItem(bean);
            //     }
            //     return nube;
            // }else{
            //     return "";
            // }

        }else{

            if(!NetWorkUtil.isNetworkConnected(context)){
                return "";
            }

            // http.send(HttpMethod.POST, url, params, new RequestCallBack<Object>() {
            //     @Override
            //     public void onStart() {
            //         super.onStart();
            //         if (lister != null) {
            //             lister.onStarted();
            //         }
            //     }
            //
            //     @Override
            //     public void onSuccess(Object result) {
            //         super.onSuccess(result);
            //         String nube = getNumber(result.toString(),false);
            //         if(!TextUtils.isEmpty(nube)){
            //             NumberCacheBean bean = new NumberCacheBean();
            //             bean.setId("");
            //             bean.setNebunumber(nube);
            //             bean.setPhonenumber(phone);
            //             numberDao.insertItem(bean);
            //         }
            //
            //         if (lister != null) {
            //             lister.onFinished(true, nube);
            //         }
            //     }
            //
            //     @Override
            //     public void onFailure(Throwable error, String msg) {
            //         super.onFailure(error, msg);
            //         if (lister != null) {
            //             lister.onFinished(false, "");
            //         }
            //     }
            // });
        }

        return null;
    }

    public static String getNebuNumberByPhoneFromLocal(final String phone){
        Context context = MedicalApplication.getContext();

        // 查询缓存表
        final NumberCacheDao  numberDao  = new NumberCacheDao(context);
        String nubeNumber = numberDao.getNebuNumberByPhone(phone);
        if(!TextUtils.isEmpty(nubeNumber)){
            return nubeNumber;
        }

        // 缓存没有，查询本地联系人
        NetPhoneDaoImpl contactDao = new NetPhoneDaoImpl(context);
        ContactFriendBean friendPo = contactDao.queryFriendInfoByPhone(phone);
        if(friendPo!=null){//查到本地，插入缓存
            addPhoneAndNubeToCache(phone, friendPo.getNubeNumber());
            return friendPo.getNubeNumber();
        }

        return "";
    }


    private static String getNumber(String response, boolean phoneOrnebu){

        String number = "";
        JSONObject resp;
        try {
            resp = new JSONObject(response);
            if (CommonConstant.SUCCESS_RESLUT.equals(resp.getString("status"))) {
                //2015-07-07 更换searchAccount接口
                JSONObject response2;
                response2 = new JSONObject(resp.getString("response"));
                JSONArray array = (JSONArray) response2.get("users");
                //				JSONArray array = (JSONArray) resp.get("users");
                LogUtil.d("array = " + array.length() + "users" + array);
                if (array != null && array.length() > 0) {

                    JSONObject jsonObject = array.getJSONObject(0);

                    String phone = jsonObject.optString("mobile");
                    String nebu = jsonObject.optString("nubeNumber");
                    if (phoneOrnebu) {
                        number = TextUtils.isEmpty(phone)?NOPHONENUMBER:phone;
                    } else {
                        number = nebu;
                    }
                }
            }
        } catch (JSONException e) {

        }

        return number;
    }

    public static String getPhoneNumberByNubeFromCache(String nube){
        Context context = MedicalApplication.getContext();
        // 查询缓存表
        final NumberCacheDao  numberDao  = new NumberCacheDao(context);
        String phoneNumber = numberDao.getPhoneNumberByNebu(nube);
        if(!TextUtils.isEmpty(phoneNumber)){
            if(phoneNumber.equals(NOPHONENUMBER)){
                return "";
            }else{
                return phoneNumber;
            }
        }
        return "";
    }

    public static String addPhoneAndNubeToCache(String phoneNumber,String nubeNumber){
        String result="";
        if (!TextUtils.isEmpty(nubeNumber)){
            NumberCacheDao  numberDao  = new NumberCacheDao(MedicalApplication.getContext());
            String phone=TextUtils.isEmpty(phoneNumber)?NOPHONENUMBER:phoneNumber;
            if (!numberDao.isExistNebuNumber(nubeNumber)){
                NumberCacheBean bean = new NumberCacheBean();
                bean.setId("");
                bean.setNebunumber(nubeNumber);
                bean.setPhonenumber(phone);
                result = numberDao.insertItem(bean);
            }else {
                if (!phone.equals(numberDao.getPhoneNumberByNebu(nubeNumber))){
                    numberDao.updatePhoneNumberByNubeNumber(nubeNumber, phone);
                }
            }
        }
        return result;
    }



    public static interface QueryNumberLister{
        public void onStarted();

        /**
         *
         * @param succ  Http接口是否成功返回
         * @param data  查询得到的号码
         */
        public void onFinished(boolean succ, String data);
    }
}
