package cn.redcdn.hvs.im.manager;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.widget.Toast;
import cn.redcdn.commonutil.NetConnectHelper;
import cn.redcdn.hvs.AccountManager;
import cn.redcdn.hvs.im.IMConstant;
import cn.redcdn.hvs.im.UrlConstant;
import cn.redcdn.hvs.im.agent.AppGroupManager;
import cn.redcdn.hvs.im.bean.GroupMemberBean;
import cn.redcdn.hvs.im.bean.ShowNameUtil;
import cn.redcdn.hvs.im.column.GroupMemberTable;
import cn.redcdn.hvs.im.dao.GroupDao;
import cn.redcdn.hvs.im.dao.ThreadsDao;
import cn.redcdn.hvs.im.task.AsyncTasks;
import cn.redcdn.hvs.im.util.xutils.http.client.RequestParams;
import cn.redcdn.hvs.im.work.MessageGroupEventParse;
import cn.redcdn.hvs.util.StringUtil;
import cn.redcdn.log.CustomLog;
import com.butel.connectevent.base.CommonConstant;
import com.butel.connectevent.utils.NetWorkUtil;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * @Title: GroupChatInterface.java
 * @Description: 1.新建对象；2.设置调用接口参数；3设置是否显示锁屏等待；4调用run接口；5等待回调;6回调结束后，进行数据库操作
 * @author niuben
 * @date 2015-6-16
 * @version V1.0
 */

public class GroupChatInterfaceManager {

    private String urlString=null;
    private RequestParams mParams=null;
    private boolean isShowWaitDailog=false;//是否锁屏,默认不显示
    private String hintString=null;//
    private String accessToken=null;	//token
    private Activity mActivity=null;
    private Context mContext = null;
    private String interfaceName=null;//接口名称
    private int quitType=-1;//0：移交群主并退出

    private GroupInterfaceListener listener = null;// 回调监听
    private GroupDao mGroupDao;
    private ThreadsDao mThreadsDao;
    private String mGid;
    private String mGName;

    private final String TAG = "GroupChatInterfaceManager";

    public GroupChatInterfaceManager(Context context){
        this.mContext = context;
        this.mGroupDao = new GroupDao(context);
        this.mThreadsDao=new ThreadsDao(context);
    }
    public GroupChatInterfaceManager(Activity  _activity ,GroupInterfaceListener _listener) {
        if (_activity==null||_listener==null){
            CustomLog.d(TAG,"_activity==null||_listener==null");
            return;
        }
        this.mContext = _activity.getBaseContext();
        this.mActivity = _activity;
        this.listener = _listener;
        this.accessToken= AccountManager.getInstance(mContext).getToken();
        this.mGroupDao=new GroupDao(_activity);
        this.mThreadsDao=new ThreadsDao(_activity);
    }
    /**
     * @param gName
     * @param userList
     */
    public void createGroup(String gName,List<String> userList) {
        CustomLog.d(TAG,"创建群组");
        this.interfaceName= UrlConstant.METHOD_CREATE_GROUP;
//        this.urlString=UrlConstant.getCommUrl(PrefType.KEY_GROUP_MANAGER_URL,this.interfaceName);
//        this.mParams=GetInterfaceParams.getCreateGroupParams(gName,this.accessToken,userList);

         if(preCheck()){
            if(listener!=null){
                AppGroupManager.getInstance(mContext).setGroupInterfaceListener(listener);
            }
            AppGroupManager.getInstance(mContext).GroupCreate(gName, userList);
         }
    }


    /**
     * 修改群组
     * @param gName
     * @param gid
     */
    public void editGroupInfo(String gName,String gid) {
        CustomLog.d(TAG,"修改群组");
        this.mGid=gid;
        this.mGName=gName;
        this.interfaceName=UrlConstant.METHOD_EDIT_GROUP;
//        this.urlString=UrlConstant.getCommUrl(PrefType.KEY_GROUP_MANAGER_URL,this.interfaceName);
//        this.mParams=GetInterfaceParams.getEditGroupInfoParams(gName, gid,this.accessToken);

        if(preCheck()){
            if(listener!=null){
                AppGroupManager.getInstance(mContext).setGroupInterfaceListener(listener);
            }
            AppGroupManager.getInstance(mContext).GroupUpdate(gid, gName);
        }
    }


    /**
     * 增加群成员
     * @param gid
     * @param userList
     */
    public void addUser(String gid, List<String> userList) {
        CustomLog.d(TAG,"增加群成员");
        this.mGid=gid;
        this.interfaceName=UrlConstant.METHOD_ADD_USERS;
//        this.urlString=UrlConstant.getCommUrl(PrefType.KEY_GROUP_MANAGER_URL,this.interfaceName);
//        this.mParams=GetInterfaceParams.getAddUserParams(gid,this.accessToken,userList);

        if(preCheck()){
            if(listener!=null){
                AppGroupManager.getInstance(mContext).setGroupInterfaceListener(listener);
            }
            AppGroupManager.getInstance(mContext).GroupAddUsers(gid, userList);
        }
    }


    /**
     * 移除群成员
     * @param gid
     * @param userList
     */
    public void delUser(String gid, List<String> userList) {
        CustomLog.d(TAG,"移除群成员");
        this.mGid=gid;
        this.interfaceName=UrlConstant.METHOD_DEL_USERS;
//        this.urlString=UrlConstant.getCommUrl(PrefType.KEY_GROUP_MANAGER_URL,this.interfaceName);
//        this.mParams=GetInterfaceParams.getDelUserParams(gid,this.accessToken,userList);

        if(preCheck()){
            if(listener!=null){
                AppGroupManager.getInstance(mContext).setGroupInterfaceListener(listener);
            }
            AppGroupManager.getInstance(mContext).GroupDelUsers(gid, userList);
        }
    }


    /**
     * 退出群组1，不移交群主/普通成员退出群
     * @param gid
     */
    public void quiteGroup(String gid) {
        CustomLog.d(TAG,"退出群组1，不移交群主/普通成员退出群");
        this.mGid=gid;
        this.quitType=-1;
        this.interfaceName=UrlConstant.METHOD_QUITE_GROUP;
//        this.urlString=UrlConstant.getCommUrl(PrefType.KEY_GROUP_MANAGER_URL,this.interfaceName);
//        this.mParams=GetInterfaceParams.getQuiteGroupParams(gid, this.accessToken);

        if(preCheck()){
            if(listener!=null){
                AppGroupManager.getInstance(mContext).setGroupInterfaceListener(listener);
            }
            AppGroupManager.getInstance(mContext).GroupQuit(gid, "");
        }
    }


    /**
     * 退出群组2，移交群主并退出群
     * @param gid
     * @param newGroupOwner
     */
    public void quiteGroup(String gid,String newGroupOwner) {
        CustomLog.d(TAG,"退出群组2，移交群主并退出群");
        this.quitType=0;
        this.mGid=gid;
        this.interfaceName=UrlConstant.METHOD_QUITE_GROUP;
//        this.urlString=UrlConstant.getCommUrl(PrefType.KEY_GROUP_MANAGER_URL,this.interfaceName);
//        this.mParams=GetInterfaceParams.getQuiteGroupParams(gid, this.accessToken,newGroupOwner);

        if(preCheck()){
            if(listener!=null){
                AppGroupManager.getInstance(mContext).setGroupInterfaceListener(listener);
            }
            AppGroupManager.getInstance(mContext).GroupQuit(gid, newGroupOwner);
        }
    }


    /**
     * 解散群组
     * @param gid
     */
    public void delGroup(String gid) {
        CustomLog.d(TAG,"解散群组");
        this.mGid=gid;
        this.interfaceName=UrlConstant.METHOD_DEL_GROUP;
//        this.urlString=UrlConstant.getCommUrl(PrefType.KEY_GROUP_MANAGER_URL,this.interfaceName);
//        this.mParams=GetInterfaceParams.getDelGroupParams(gid, this.accessToken);

        if(preCheck()){
            if(listener!=null){
                AppGroupManager.getInstance(mContext).setGroupInterfaceListener(listener);
            }
            AppGroupManager.getInstance(mContext).GroupDelete(gid);
        }
    }


    /**
     * 查询群组详情
     * @param gid
     */
    public void queryGroupDetail(String gid) {
        CustomLog.d(TAG,"查询群组详情");
        this.mGid=gid;
        this.interfaceName=UrlConstant.METHOD_QUERY_GROUP_DETAIL;
//        this.urlString=UrlConstant.getCommUrl(PrefType.KEY_GROUP_MANAGER_URL,this.interfaceName);
//        this.mParams=GetInterfaceParams.getGroupDetailParams(gid);

        if(preCheck()){
            if(listener!=null){
                AppGroupManager.getInstance(mContext).setGroupInterfaceListener(listener);
            }
            AppGroupManager.getInstance(mContext).GroupQueryDetail(gid);
        }
    }

    /**
     * 同步调用Http接口查询该群组的详细
     * （仅在接收消息时，可能需要使用；其他场景请用异步接口）
     * @param gid
     */
    public boolean syncQeryGroupDetail(String gid){
//        String urlString=UrlConstant.getCommUrl(PrefType.KEY_GROUP_MANAGER_URL,UrlConstant.METHOD_QUERY_GROUP_DETAIL);
//        RequestParams mParams=GetInterfaceParams.getGroupDetailParams(gid);
//
//        HttpUtils http = new HttpUtils();
//        SyncResult result = http.sendSync(HttpMethod.POST, urlString, mParams);
//        if(result.isOK()){
//            String data = result.getResult();
//            if(!TextUtils.isEmpty(data)){
//                return saveGroupDetail(data);
//            }
//        }
        return false;
    }

    private boolean saveGroupDetail(String data){
        try {
            JSONObject resp = new JSONObject(data);
            if (CommonConstant.SUCCESS_RESLUT.equals(resp.optString("status"))) {
                JSONObject detail = resp.getJSONObject("GroupDetail");
                mGid =detail.optString("gid");
                doSaveorUpdateGroupDetail(mGid, detail.optString("groupName"), detail.optString("headUrl"),
                        detail.optString("managerNube"), detail.optString("createDate"),
                        JSONArray2GroupMemberBeanList(detail.getJSONArray("groupMembers")));
                return true;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void tagUserName(String gid,String nubeNumber,String name) {
        CustomLog.d(TAG,"群组中昵称修改");
        this.interfaceName=UrlConstant.METHOD_TAG_USERNAME;
//        this.urlString=UrlConstant.getCommUrl(PrefType.KEY_GROUP_MANAGER_URL,this.interfaceName);
//        this.mParams=GetInterfaceParams.getTagUserNameParams(gid, this.accessToken, nubeNumber, name);
    }

    public void getAllGroup() {
        CustomLog.d(TAG,"获取与某人相关群的列表");
        this.interfaceName=UrlConstant.METHOD_GET_ALL_GROUP;
//        this.urlString=UrlConstant.getCommUrl(PrefType.KEY_GROUP_MANAGER_URL,this.interfaceName);
//        this.mParams=GetInterfaceParams.getAllGroupParams(this.accessToken);
    }


    public void setShowWaitDailog(boolean _isShowWaitDailog,String _hintString){
        this.isShowWaitDailog=_isShowWaitDailog;
        this.hintString=_hintString;
    }

    private boolean preCheck(){

//        if (urlString==null){
//            if(listener!=null){
//                listener.onResult(interfaceName,false,"未设置接口名称");
//            }
//            return false;
//        }

        if( NetConnectHelper.NETWORKTYPE_INVALID == NetConnectHelper.getNetWorkType(mContext)){
            showToast("网络连接不可用,请稍后重试");
            return false;
        }

        if(!IMConstant.isP2PConnect){
            if(listener!=null){
                listener.onResult(interfaceName,false,"正在连接服务，请稍候");
            }
            showToast("正在连接服务，请稍候");
            return false;
        }
        return true;
    }


    public void run(){
        // if (urlString==null){
        //     listener.onResult(interfaceName,false,"未设置接口名称");
        //     return;
        // }

       if (!NetWorkUtil.isNetworkConnected(mContext)){
           listener.onResult(interfaceName,false,"网络连接不可用,请稍后重试");
           showToast("网络连接不可用,请稍后重试");
           return;
       }

        AsyncTasks invokeTask = new AsyncTasks(urlString,
                mParams,
                hintString,
                mActivity,
                true,
                this.isShowWaitDailog);

        invokeTask.setListenerResult(new AsyncTasks.ListenerResult() {
            @Override
            public void getResluts(String reslut) {
                CustomLog.d(TAG,interfaceName+"接口返回" + reslut);
                try {
                    JSONObject resp = new JSONObject(reslut);
                    if (CommonConstant.SUCCESS_RESLUT.equals(resp.optString("status"))) {
                        if (interfaceName.equals(UrlConstant.METHOD_CREATE_GROUP)){//创建群成功
                            JSONObject GroupInfo = resp.getJSONObject("GroupDetail");
                            mGid=GroupInfo.optString("gid");
                            ArrayList<GroupMemberBean> members = JSONArray2GroupMemberBeanList(GroupInfo.getJSONArray("groupMembers"));
                            doSaveorUpdateGroupDetail(mGid, GroupInfo.optString("groupName"), GroupInfo.optString("headUrl"),
                                    GroupInfo.optString("managerNube"), GroupInfo.optString("createDate"),members);
                            insertInvitateMemberInfor(members);
                            listener.onResult(interfaceName, true, mGid);
                        }else if (interfaceName.equals(UrlConstant.METHOD_EDIT_GROUP)){// 修改群信息
                            mGroupDao.updateGroupName(mGid, mGName);
                            insertEditGroupNameMsg();
                            listener.onResult(interfaceName,true,"成功");
                        }else if (interfaceName.equals(UrlConstant.METHOD_ADD_USERS)){// 群增加成员（邀请）
                            ArrayList<GroupMemberBean> members = JSONArray2GroupMemberBeanList(resp.getJSONArray("AddUsers"));
                            mGroupDao.addMemberAfterDelete(members);
                            insertInvitateMemberInfor(members);
                            listener.onResult(interfaceName,true,"成功");
                        }else if (interfaceName.equals(UrlConstant.METHOD_DEL_USERS)){// 删除群成员（踢人）
                            ArrayList<GroupMemberBean> members = JSONArray2GroupMemberBeanList(resp.getJSONArray("DeleteUsers"));
                            for(int i=0;i<members.size();i++){
                                mGroupDao.setMemberRemoved(mGid, members.get(i).getNubeNum());
                            }
                            insertdeleteMemberInfor(members);
                            listener.onResult(interfaceName,true,"成功");
                        }else if (interfaceName.equals(UrlConstant.METHOD_QUITE_GROUP)){// 退出群
                            mGroupDao.delGroup(mGid);
                            mGroupDao.delMembersByGid(mGid);
                            mThreadsDao.deleteThread(mGid);
                            listener.onResult(interfaceName,true,"成功");
                        }else if (interfaceName.equals(UrlConstant.METHOD_DEL_GROUP)){// 解散群
//							TODO 目前未处理
                            listener.onResult(interfaceName,true,"成功");
                        }else if (interfaceName.equals(UrlConstant.METHOD_QUERY_GROUP_DETAIL)){// 查询群详情
                            JSONObject detail = resp.getJSONObject("GroupDetail");
                            doSaveorUpdateGroupDetail(mGid, detail.optString("groupName"), detail.optString("headUrl"),
                                    detail.optString("managerNube"), detail.optString("createDate"),
                                    JSONArray2GroupMemberBeanList(detail.getJSONArray("groupMembers")));
                            listener.onResult(interfaceName,true,"成功");
                        }else if (interfaceName.equals(UrlConstant.METHOD_TAG_USERNAME)){ // 备注用户名
//							TODO 目前未处理
                            listener.onResult(interfaceName,true,"成功");
                        }else if (interfaceName.equals(UrlConstant.METHOD_GET_ALL_GROUP)){ // 获取与某人相关群的列表
//							TODO 目前未处理
                            listener.onResult(interfaceName,true,"成功");
                        }
                    } else {
                        if ("-207".equals(resp.optString("status"))){
                            if (interfaceName.equals(UrlConstant.METHOD_QUITE_GROUP)){
                                listener.onResult(interfaceName,false,"无法移交，该成员已拥有20个群");
                                showToast("无法移交，该成员已拥有20个群");
                            }else if (interfaceName.equals(UrlConstant.METHOD_CREATE_GROUP)) {
                                listener.onResult(interfaceName,false,"最多创建20个群聊");
                                showToast("最多创建20个群聊");
                            }else{
                                listener.onResult(interfaceName,false,resp.optString("message"));
                                showToast(resp.optString("message"));
                            }
                        }else if ("-208".equals(resp.optString("status"))){
                            listener.onResult(interfaceName,false,"群成员最多100人");
                            showToast("群成员最多100人");
                        }else if ("-205".equals(resp.optString("status"))){
                            if (interfaceName.equals(UrlConstant.METHOD_QUITE_GROUP)){
                                if (quitType==0){
                                    listener.onResult(interfaceName,false,"该成员已退群，请选择其他成员");
                                    showToast("该成员已退群，请选择其他成员");
                                }else {//认为退群成功
                                    mGroupDao.delGroup(mGid);
                                    mGroupDao.delMembersByGid(mGid);
                                    mThreadsDao.deleteThread(mGid);
                                    listener.onResult(interfaceName,true,"成功");
                                }
                            }else if (interfaceName.equals(UrlConstant.METHOD_DEL_USERS)){
                                //	TODO 无法判断是否移除成功，目前只需要刷新页面
                                listener.onResult(interfaceName,true,"成功");
                            }else{
                                listener.onResult(interfaceName,false,resp.optString("message"));
                                showToast(resp.optString("message"));
                            }
                        }else {
                            listener.onResult(interfaceName,false,resp.optString("message"));
                            showToast(resp.optString("message"));
                        }
                    }
                } catch (Exception e) {
                    CustomLog.d(TAG,"Exception：" + e.toString());
                    listener.onResult(interfaceName,false,"JSON解析出错");
                }
            }
        });
        invokeTask.setListenerFaliureResult(new AsyncTasks.ListenerFaliureResult() {
            @Override
            public void getResluts(String msg,boolean alerted) {
                CustomLog.d(TAG,"接口调用异常"+msg);
                showToast("接口调用异常");
                listener.onResult(interfaceName,false,"接口调用异常");
            }
        });
        invokeTask.exeuteTask();
    }

    public void resultParse(int reason,String reslutJson, String interfaceName, String gid, String gname,
                            GroupInterfaceListener listener,int type){
        mGid = gid;
        mGName = gname;
        if(reason!=0|| TextUtils.isEmpty(reslutJson)||TextUtils.isEmpty(interfaceName)){
            CustomLog.d(TAG,"reslutJson==null||interfaceName==null");
            if (listener!=null){
                showToast("接口调用异常");
                listener.onResult(interfaceName,false,"接口调用异常");
            }
            return;
        }

        try {
            JSONObject resp = new JSONObject(reslutJson);
            if (CommonConstant.SUCCESS_RESLUT.equals(resp.optString("status"))) {
                if (interfaceName.equals(UrlConstant.METHOD_CREATE_GROUP)){//创建群成功
                    JSONObject GroupInfo = resp.getJSONObject("GroupDetail");
                    mGid=GroupInfo.optString("gid");
                    ArrayList<GroupMemberBean> members = JSONArray2GroupMemberBeanList(GroupInfo.getJSONArray("groupMembers"));
                    doSaveorUpdateGroupDetail(mGid, GroupInfo.optString("groupName"), GroupInfo.optString("headUrl"),
                            GroupInfo.optString("managerNube"), GroupInfo.optString("createDate"),members);
                    insertInvitateMemberInfor(members);
                    CustomLog.d(TAG,"resultParse listener"+ listener);
                    if(listener!=null){
                        listener.onResult(interfaceName, true, mGid);
                    }
                }else if (interfaceName.equals(UrlConstant.METHOD_EDIT_GROUP)){// 修改群信息
                    mGroupDao.updateGroupName(mGid, mGName);
                    insertEditGroupNameMsg();
                    if(listener!=null){
                        listener.onResult(interfaceName,true,"成功");
                    }
                }else if (interfaceName.equals(UrlConstant.METHOD_ADD_USERS)){// 群增加成员（邀请）
                    ArrayList<GroupMemberBean> members = JSONArray2GroupMemberBeanList(resp.getJSONArray("AddUsers"));
                    mGroupDao.addMemberAfterDelete(members);
                    insertInvitateMemberInfor(members);
                    if(listener!=null){
                        listener.onResult(interfaceName,true,"成功");
                    }
                }else if (interfaceName.equals(UrlConstant.METHOD_DEL_USERS)){// 删除群成员（踢人）
                    ArrayList<GroupMemberBean> members = JSONArray2GroupMemberBeanList(resp.getJSONArray("DeleteUsers"));
                    for(int i=0;i<members.size();i++){
                        mGroupDao.setMemberRemoved(mGid, members.get(i).getNubeNum());
                    }
                    insertdeleteMemberInfor(members);
                    if(listener!=null){
                        listener.onResult(interfaceName,true,"成功");
                    }
                }else if (interfaceName.equals(UrlConstant.METHOD_QUITE_GROUP)){// 退出群
                    mGroupDao.delGroup(mGid);
                    mGroupDao.delMembersByGid(mGid);
                    mThreadsDao.deleteThread(mGid);
                    if(listener!=null){
                        listener.onResult(interfaceName,true,"成功");
                    }
                }else if (interfaceName.equals(UrlConstant.METHOD_DEL_GROUP)){// 解散群
                    // TODO 目前未处理
                    listener.onResult(interfaceName,true,"成功");
                }else if (interfaceName.equals(UrlConstant.METHOD_QUERY_GROUP_DETAIL)){// 查询群详情
                    JSONObject detail = resp.getJSONObject("GroupDetail");
                    doSaveorUpdateGroupDetail(mGid, detail.optString("groupName"), detail.optString("headUrl"),
                            detail.optString("managerNube"), detail.optString("createDate"),
                            JSONArray2GroupMemberBeanList(detail.getJSONArray("groupMembers")));
                    if(listener!=null){
                        listener.onResult(interfaceName,true,"成功");
                    }
                }else if (interfaceName.equals(UrlConstant.METHOD_TAG_USERNAME)){ // 备注用户名
                    // TODO 目前未处理
                    if(listener!=null){
                        listener.onResult(interfaceName,true,"成功");
                    }
                }else if (interfaceName.equals(UrlConstant.METHOD_GET_ALL_GROUP)){ // 获取与某人相关群的列表
                    // TODO 目前未处理
                    if(listener!=null){
                        listener.onResult(interfaceName,true,"成功");
                    }
                }
            } else {
                if ("-207".equals(resp.optString("status"))){
                    if (interfaceName.equals(UrlConstant.METHOD_QUITE_GROUP)){
                        if(listener!=null){
                            listener.onResult(interfaceName,false,"无法移交，该成员已拥有20个群");
                        }
                        showToast("无法移交，该成员已拥有20个群");
                    }else if (interfaceName.equals(UrlConstant.METHOD_CREATE_GROUP)) {
                        if(listener!=null){
                            listener.onResult(interfaceName,false,"最多创建20个群聊");
                        }
                        showToast("最多创建20个群聊");
                    }else{
                        if(listener!=null){
                            listener.onResult(interfaceName,false,resp.optString("message"));
                        }
                        showToast(resp.optString("message"));
                    }
                }else if ("-208".equals(resp.optString("status"))){
                    if(listener!=null){
                        listener.onResult(interfaceName,false,"群成员最多100人");
                    }
                    showToast("群成员最多100人");
                }else if ("-205".equals(resp.optString("status"))){
                    if (interfaceName.equals(UrlConstant.METHOD_QUITE_GROUP)){
                        if (type==0){
                            if(listener!=null){
                                listener.onResult(interfaceName,false,"该成员已退群，请选择其他成员");
                            }
                            showToast("该成员已退群，请选择其他成员");
                        }else {//认为退群成功
                            mGroupDao.delGroup(mGid);
                            mGroupDao.delMembersByGid(mGid);
                            mThreadsDao.deleteThread(mGid);
                            if(listener!=null){
                                listener.onResult(interfaceName,true,"成功");
                            }
                        }
                    }else if (interfaceName.equals(UrlConstant.METHOD_DEL_USERS)){
                        //	TODO 无法判断是否移除成功，目前只需要刷新页面
                        if(listener!=null){
                            listener.onResult(interfaceName,true,"成功");
                        }
                    }else{
                        if(listener!=null){
                            listener.onResult(interfaceName,false,resp.optString("message"));
                        }
                        showToast(resp.optString("message"));
                    }
                }else {
                    if(listener!=null){
                        listener.onResult(interfaceName,false,resp.optString("message"));
                    }
                    showToast(resp.optString("message"));
                }
            }
        } catch (Exception e) {
            CustomLog.d(TAG,"JSON解析出错" + e.toString());
            if(listener!=null){
                showToast("接口调用异常");
                listener.onResult(interfaceName,false,"JSON解析出错");
            }
        }
        // 只是打日志，确定是否上报
        if(listener!=null){
            CustomLog.d(TAG,"真的处理回调了哦！！！！！");
        }else {
            CustomLog.d(TAG,"这个回调不处理了！！！！！");
        }
    }


    private void doSaveorUpdateGroupDetail(String gid,String groupName,String headUrl,String managerNube,String createDate, ArrayList<GroupMemberBean> members){
        if(!mGroupDao.existGroup(gid)){
            mGroupDao.createGroup(gid, groupName, headUrl, managerNube, createDate);
//			mGroupDao.addGroupMember(members);
            //极端场景下，群成员在，但群详情不在时，如果直接插入会导致有2条相同的信息，会导致在搜索显示chatActivity时，显示出两条记录
            mGroupDao.setAllMemberRemoved(gid);
            mGroupDao.addOrUpdateMember(members);
        }else {
            mGroupDao.updateGroup(gid, groupName, headUrl, managerNube, createDate);
            //更新群成员信息
            mGroupDao.setAllMemberRemoved(gid);
            mGroupDao.addOrUpdateMember(members);
        }
    }

    private void insertdeleteMemberInfor(ArrayList<GroupMemberBean> members){
        // 由于移除群组需要插入“你将xxx移除了群聊”， 此处先建立一个会话，否则刚插入的文字无法显示
        if(!mThreadsDao.isExistThread(mGid)){
            mThreadsDao.createThreadFromGroup(mGid);
        }
        ArrayList<String> nameArrayList=new ArrayList<String>();
        for (GroupMemberBean bean:members){
            nameArrayList.add(mGroupDao.queryGroupMember(mGid, bean.getNubeNum()).getDispName());
        }
        String eventMsg="你将"+StringUtil.list2String(nameArrayList, '、')+"移出了群聊";
        MessageGroupEventParse.insertGroupEventDiscription("",mGid, eventMsg, "");
    }

    private void insertEditGroupNameMsg(){
        // 由于创建群组需要插入“你修改了群名"XXX"”
        // 此处先建立一个会话，否则刚插入的文字无法显示
        if(!mThreadsDao.isExistThread(mGid)){
            mThreadsDao.createThreadFromGroup(mGid);
        }
        String eventMsg="你修改了群名\"" +mGName+"\"";
        MessageGroupEventParse.insertGroupEventDiscription("",mGid, eventMsg, "");
        CustomLog.d(TAG,eventMsg);
    }

    private void insertInvitateMemberInfor(ArrayList<GroupMemberBean> members){
        // 由于创建群组需要插入“你邀请xxx加入了群聊”
        // 此处先建立一个会话，否则刚插入的文字无法显示
        if(!mThreadsDao.isExistThread(mGid)){
            mThreadsDao.createThreadFromGroup(mGid);
        }
        String loginNube=AccountManager.getInstance(mContext).getAccountInfo().nube;
        ArrayList<String> nameArrayList=new ArrayList<String>();
        for (GroupMemberBean bean:members){
            if (!bean.getNubeNum().equals(loginNube)){
                nameArrayList.add(mGroupDao.queryGroupMember(mGid, bean.getNubeNum()).getDispName());
            }
        }
        String eventMsg="你邀请"+ StringUtil.list2String(nameArrayList, '、')+"加入了群聊";
        MessageGroupEventParse.insertGroupEventDiscription("",mGid, eventMsg, "");
    }

    private ArrayList<GroupMemberBean> JSONArray2GroupMemberBeanList(JSONArray membersArray){
        CustomLog.d(TAG,"JSONArray2GroupMemberBeanList begin");
        ArrayList<GroupMemberBean> members = new ArrayList<GroupMemberBean>();
        if(membersArray!=null&&membersArray.length()>0){
            GroupMemberBean bean = null;
            int length = membersArray.length();
            for(int i=0;i<length;i++){
                try {
                    JSONObject item = membersArray.getJSONObject(i);
                    bean = new GroupMemberBean();
                    bean.setGid(mGid);//必填（加人时，不返回，因此采用mGid）
                    bean.setMid(item.optString("mid"));
                    bean.setUid(item.optString("uid"));
                    bean.setGroupNick(item.optString("groupNick"));
                    bean.setHeadUrl(item.optString("headUrl"));
                    String nickname=item.optString("nickName");
                    //接口返回的是null字符串，需区分，否则显示的名称为"null"
                    nickname =nickname.equals("null")?null:nickname;
                    String mobile=item.optString("mobile");
                    mobile =mobile.equals("null")?null:mobile;
                    String nube=item.optString("nubeNumber");
                    bean.setNickName(nickname);
                    bean.setNubeNum(nube);
                    bean.setPhoneNum(mobile);
                    bean.setShowName(ShowNameUtil.getShowName(ShowNameUtil.getNameElement(null, nickname, mobile, nube)));
                    bean.setGender((item.optString("gender").trim()).equals("女")? GroupMemberTable.GENDER_FEMALE:GroupMemberTable.GENDER_MALE);
                    bean.setRemoved(GroupMemberTable.REMOVED_FALSE);
                    members.add(bean);
                } catch (JSONException e) {
                    CustomLog.e(TAG,"JSONArray解析出错" + e.toString());
                }
            }
        }
        CustomLog.d(TAG,"\"members.size()=\"+members.size()");
        return members;
    }

    private void showToast(String text){
        Toast.makeText(mContext, text, Toast.LENGTH_SHORT).show();
        CustomLog.d(TAG,interfaceName+":"+text);
    }
    /**
     * @Description
     *@pamas _interfaceName：调用接口名
     *					successOrfaliure：true 调用接口成功，false 调用接口失败
     *                 result 信息(创建群是返回是群ID)
     */
    public static interface GroupInterfaceListener {
        public void onResult(String _interfaceName,boolean isSuccess,String result);
    }
}
