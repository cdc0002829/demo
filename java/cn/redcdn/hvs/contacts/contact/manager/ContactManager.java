package cn.redcdn.hvs.contacts.contact.manager;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Build;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

import cn.redcdn.hvs.AccountManager;
import cn.redcdn.hvs.config.SettingData;
import cn.redcdn.hvs.contacts.contact.StringHelper;
import cn.redcdn.hvs.contacts.contact.database.DBConf;
import cn.redcdn.hvs.contacts.contact.interfaces.Contact;
import cn.redcdn.hvs.contacts.contact.interfaces.ContactCallback;
import cn.redcdn.hvs.contacts.contact.interfaces.ContactOperation;
import cn.redcdn.hvs.contacts.contact.interfaces.ResponseEntry;
import cn.redcdn.hvs.util.CommonUtil;
import cn.redcdn.log.CustomLog;

public class ContactManager implements ContactOperation {
    private static final String TAG = ContactManager.class.getSimpleName();
    private static ContactManager mInstance = null;
    private Context mContext = null;
    private List<IContactListChanged> iContactListChanged = null;
    // private int changeCount;
    private String myTable;
    public static final String suffix = "_contact";
    public static final String prefix = "my_";
    //  myTable = prefix + account + suffix;
    public static final String TABLE_NAME = "t_nubefriend";
    private ContactSync dataSync = null;


    public static String customerServiceNum1 = "68000001";
    public static String customerServiceNum2 = "68000002";


    public static final String customerServiceName = "视频客服";

    public static final String groupChat = "群聊";
    public static final String publicNumber = "公众号";

    public static int cannotAdd = -100;//不能加自己为好友

    /**
     * <pre>
     * CAUTION :
     * 实例化 给外界调用
     * </pre>
     */
    public synchronized static ContactManager getInstance(Context context) {
        if (mInstance == null) {
            CustomLog.d(TAG, "ContactManager getInstance");
            mInstance = new ContactManager();
            mInstance.mContext = context;
            // ContactDBOperater.getInstance(context);
            // mInstance.dataHandlerThread = new HandlerThread("updateHandlerThread");
            // mInstance.dataHandlerThread.start();
            // mInstance.dataHandler = new Handler(
            // mInstance.dataHandlerThread.getLooper());
            // mInstance.state = INIT_STATE;
        }
        return mInstance;
    }

    private ContactManager(){

        if(!TextUtils.isEmpty(SettingData.getInstance().CUSTEMER_TEL1)){
            customerServiceNum1 = SettingData.getInstance().CUSTEMER_TEL1;
        }

        if(!TextUtils.isEmpty(SettingData.getInstance().CUSTEMER_TEL2)){
            customerServiceNum2 = SettingData.getInstance().CUSTEMER_TEL2;
        }

    }

    public void initData(String account) {
        CustomLog.d(TAG, "initData NOW account: " + account);
        myTable = TABLE_NAME;
        CustomLog.d(TAG, "initData myTable " + myTable);
        ContactDBOperater.getInstance(mContext);
        RecommendManager.getInstance(mContext);

        // TODO
        contactDataSync(true);
    }

    public String getMyTable() {
        return myTable;
    }

    public boolean checkNubeIsCustomService(String nubeNumber) {
        if (nubeNumber == null || nubeNumber.equals("")) {
            return false;
        }
        if (nubeNumber.equals(customerServiceNum1)) {
            return true;
        }
        if (nubeNumber.equals(customerServiceNum2)) {
            return true;
        }
        return false;
    }

    public boolean isContactExist(String nubeNumber) {
        if (checkNubeIsCustomService(nubeNumber)) {
            return true;
        }
        boolean isExist = false;
        String sql = "select count(*) from " + myTable + " where nubeNumber = '"
                + nubeNumber + "' and isDeleted = 0 ";
        Cursor c = ContactDBOperater.getInstance(mContext).rawQuery(sql, myTable);
        if (c != null && c.moveToNext()) {
            if (c.getInt(0) > 0) {
                isExist = true;
            }
        }
        if (c != null) {
            c.close();
        }
        CustomLog.e(TAG, "isContactExist " + isExist);
        return isExist;
    }

    public String getHeadUrlByNube(String nubeNumber) {
        String url = "";
        String sql = "select headUrl from " + myTable + " where nubeNumber = '"
                + nubeNumber + "' and isDeleted = 0 ";
        Cursor c = ContactDBOperater.getInstance(mContext).rawQuery(sql, myTable);
        if (c != null && c.moveToNext()) {
            url = c.getString(0);
        }
        if (c != null) {
            c.close();
        }
        return url;
    }

    @Override
    public void addContact(final Contact contact, ContactCallback callback) {
        CustomLog.d(TAG, "addContact ： " + contact.toString());
        if (contact != null) {
            if (TextUtils.isEmpty(contact.getNubeNumber())) {
                CustomLog.d(TAG, "NubeNumber is empty");
                ResponseEntry response = new ResponseEntry();
                response.status=-1;
                response.content = -1;
                callback.onFinished(response);
            } else if(contact.getNubeNumber().equals(AccountManager.getInstance(mContext).getNube())){
                CustomLog.d(TAG,"不能添加自己为好友");
                ResponseEntry response = new ResponseEntry();
                response.status=cannotAdd;
                response.content = -1;
                callback.onFinished(response);
            }
            else {
                /** insert */
                // changeCount++;
                try {
                    ContactCallback mycallback = new ContactCallback() {
                        @Override
                        public void onFinished(ResponseEntry result) {
                            if (result != null && result.status == 0) {
                                CustomLog.e(TAG, "addContact onFinished ");
                                // 若推荐列表中有，则删除后，添加一个新的作为推荐，contactId要变
                                // TODO
                                // 推荐列表 未添加变为已添加
                                RecommendManager.getInstance(mContext).changeBeAdded(contact);
                                // TODO 同步
                                contactDataSync(false);
                            }

                        }
                    };
                    contact.setFullPym(StringHelper.getPingYin(contact.getNickname()));
                    CustomLog.d(TAG, "addContact insert start： ");
                    CustomAsyncTask task = new CustomAsyncTask();
                    task.setCallback(callback);
                    task.setCallback(mycallback);
                    task.setContentValues(contactToContentValues(contact));
                    task.setTable(myTable);
                    task.setOpertionStatus(CustomAsyncTask.OPERATE_INSERT);
                    task.setContext(mContext);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "");
                    } else {
                        task.execute("");
                    }

                } catch (Exception e) {
                    ResponseEntry response = new ResponseEntry();
                    response.status=-1;
                    response.content = -1;
                    callback.onFinished(response);
                }
            }
        }
    }

    /**
     * 添加群组到通讯录
     * @param groupId 群组id
     * @param groupName 群组名称
     * @param headUrl 群头像url地址
     * @param callback 回调结果监听
     */
    public void saveGroupToContacts(final String groupId, final String groupName, final String headUrl, ContactCallback callback) {
        CustomLog.d(TAG, "addGroup groupId: " + groupId + " |groupName: " + groupName + " |headUrl: " + headUrl);
        final Contact contact = new Contact();
        contact.setAccountType(1); // 账号类型 0: 个人， 1：群
        contact.setNubeNumber(groupId);
        contact.setName(groupName);
        contact.setNickname(groupName);
        contact.setHeadUrl(headUrl);

        addContact(contact, callback);
    }

    /**
     * 从通讯录删除群组
     * @param groupId 群组id
     * @param callback 回调结果监听
     */
    public void removeGroupFromContacts(final String groupId, ContactCallback callback) {
        groupLogicDeleteContactByNube(groupId, callback);
    }

    public void groupLogicDeleteContactByNube(final String id, ContactCallback callback) {
        try {
            // changeCount++;
            ContactCallback mycallback = new ContactCallback() {
                @Override
                public void onFinished(ResponseEntry result) {
                    if (result != null && result.status == 0) {
                        CustomLog.e(TAG, "logicDeleteContactByNube onFinished ");
                        // 若推荐列表中有，则删除后，添加一个新的作为推荐，contactId要变
//                        // TODO
//                        RecommendManager.getInstance(mContext).changeIdAndBeAdded(id);
                        // TODO 同步
                        contactDataSync(false);
                    }

                }
            };
            ContentValues values = new ContentValues();
            values.put(DBConf.ISDELETED, 1);
            values.put(DBConf.SYNCSTAT, 0);
            CustomAsyncTask task = new CustomAsyncTask();
            task.setCallback(callback);
            task.setCallback(mycallback);
            task.setContentValues(values);
            task.setTable(myTable);
            task.setWhereClause(DBConf.NUBENUMBER + " = ? ");
            task.setWhereArgs(new String[]{id});
            task.setOpertionStatus(CustomAsyncTask.OPERATE_UPDATE);
            task.setContext(mContext);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "");
            } else {
                task.execute("");
            }
        } catch (Exception e) {
            CustomLog.e(TAG, "更新好友表删除状态   Exception " + e);
        }
    }

    /**
     * 更新群组名称
     * @param groupId 群组id
     * @param groupName 群组名称
     * @param callback 回调结果监听
     */
    public void updateGroupName(final String groupId, final String groupName, ContactCallback callback ) {

        CustomLog.d(TAG, "ContactManager::updateGroupName() groupId: " + groupId + " |groupName: " + groupName);

        ContactCallback mycallback = new ContactCallback() {
            @Override
            public void onFinished(ResponseEntry result) {
                if (result != null && result.status == 0) {
                    CustomLog.e(TAG, "updateGroupName onFinished ");
                    // TODO 同步
                    contactDataSync(false);
                }
            }
        };

        CustomAsyncTask task = new CustomAsyncTask();
        task.setCallback(callback);
        task.setCallback(mycallback);
        ContentValues values = new ContentValues();
        if (TextUtils.isEmpty(groupName)) {
            values.put(DBConf.NICKNAME, "未命名");
            values.put(DBConf.FIRSTNAME, StringHelper.getHeadChar("未命名"));
            values.put(DBConf.PINYIN, StringHelper.getAllPingYin("未命名"));
            values.put(DBConf.NAME, "未命名");
        } else {
            values.put(DBConf.NICKNAME, groupName);
            values.put(DBConf.FIRSTNAME, StringHelper.getHeadChar(groupName));
            values.put(DBConf.PINYIN, StringHelper.getAllPingYin(groupName));
            values.put(DBConf.NAME, groupName);
        }
        values.put(DBConf.SYNCSTAT, 0);

        task.setContentValues(values);
        task.setTable(myTable);
        task.setWhereClause(DBConf.NUBENUMBER + " = ? ");
        task.setWhereArgs(new String[]{checkIsNull(groupId)});

        task.setOpertionStatus(CustomAsyncTask.OPERATE_UPDATE);
        task.setContext(mContext);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "");
        } else {
            task.execute("");
        }
    }

    public void updateGroupHeadUrl(final String groupId, final String headUrl, ContactCallback callback) {
        CustomLog.d(TAG, "ContactManager::updateGroupName() groupId: " + groupId + " |groupName: " + headUrl);
        ContactCallback mycallback = new ContactCallback() {
            @Override
            public void onFinished(ResponseEntry result) {
                if (result != null && result.status == 0) {
                    CustomLog.e(TAG, "updateGroupName onFinished ");
                    // TODO 同步
                    contactDataSync(false);
                }
            }
        };
        CustomAsyncTask task = new CustomAsyncTask();
        task.setCallback(callback);
        task.setCallback(mycallback);
        ContentValues values = new ContentValues();
        values.put(DBConf.PICURL,checkIsNull(headUrl));
        values.put(DBConf.SYNCSTAT,0);
        task.setTable(myTable);
        task.setWhereClause(DBConf.NUBENUMBER + " = ? ");
        task.setWhereArgs(new String[]{checkIsNull(groupId)});
        task.setOpertionStatus(CustomAsyncTask.OPERATE_UPDATE);
        task.setContext(mContext);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "");
        } else {
            task.execute("");
        }
    }

    private void contactDataSync(boolean isSync) {
        if (dataSync != null) {
            dataSync.cancle();
            dataSync = null;
        }
        dataSync = new ContactSync();
        dataSync.init(mContext, isSync, iContactListChanged, myTable);
        dataSync.start();
    }

    private ContentValues contactToContentValues(Contact contact) {
        CustomLog.d(TAG, "contactToContentValues " + contact.toString());
        ContentValues values = new ContentValues();
        if (TextUtils.isEmpty(contact.getContactId())) {
            values.put(DBConf.CONTACTID, CommonUtil.getUUID());
        } else {
            values.put(DBConf.CONTACTID, contact.getContactId());
        }
        if (TextUtils.isEmpty(contact.getNickname())) {
            values.put(DBConf.NICKNAME, "未命名");
            values.put(DBConf.FIRSTNAME, StringHelper.getHeadChar("未命名"));
            values.put(DBConf.PINYIN, StringHelper.getAllPingYin("未命名"));
        } else {
            values.put(DBConf.NICKNAME, contact.getNickname());
            values.put(DBConf.FIRSTNAME, StringHelper.getHeadChar(contact.getNickname()));
            values.put(DBConf.PINYIN, StringHelper.getAllPingYin(contact.getNickname()));
        }
        if (TextUtils.isEmpty(contact.getName())) {
            values.put(DBConf.NAME, "未命名");
        } else {
            values.put(DBConf.NAME, contact.getName());
        }
        values.put(DBConf.LASTNAME, String.valueOf(contact.getLastTime()));
        values.put(DBConf.ISDELETED, contact.getIsDeleted());
        values.put(DBConf.PHONENUMBER, checkIsNull(contact.getNumber()));
        values.put(DBConf.PICURL, checkIsNull(contact.getPicUrl()));
        values.put(DBConf.USERTYPE, contact.getUserType());
        values.put(DBConf.NUBENUMBER, checkIsNull(contact.getNubeNumber()));
        values.put(DBConf.USERFROM, contact.getUserFrom());
        values.put(DBConf.CONTACTUSERID, checkIsNull(contact.getContactUserId()));
        values.put(DBConf.APPTYPE, checkIsNull(contact.getAppType()));
        values.put(DBConf.SYNCSTAT, 0);

        values.put(DBConf.EMAIL, contact.getEmail());
        values.put(DBConf.ACCOUNT_TYPE, contact.getAccountType());
        values.put(DBConf.WORKUNIT_TYPE, contact.getWorkUnitType());
        values.put(DBConf.WORK_UNIT, contact.getWorkUnit());
        values.put(DBConf.DEPARTMENT, contact.getDepartment());
        values.put(DBConf.PROFESSIONAL, contact.getProfessional());
        values.put(DBConf.OFFICETEL, contact.getOfficeTel());
        values.put(DBConf.SAVE_TO_CONTACTS_TIME, contact.getSaveToContactsTime());

        return values;
    }

    public void logicDeleteContactById(final String id, ContactCallback callback) {
        try {
            // changeCount++;
            ContactCallback mycallback = new ContactCallback() {

                @Override
                public void onFinished(ResponseEntry result) {
                    if (result != null && result.status == 0) {
                        CustomLog.e(TAG, "logicDeleteContactById onFinished ");
                        // 若推荐列表中有，则删除后，添加一个新的作为推荐，contactId要变
                        // TODO
                        RecommendManager.getInstance(mContext).changeIdAndBeAdded(id);
                        // TODO 同步
                        contactDataSync(false);
                    }
                }
            };
            ContentValues values = new ContentValues();
            values.put(DBConf.ISDELETED, 1);
            values.put(DBConf.SYNCSTAT, 0);
            CustomAsyncTask task = new CustomAsyncTask();
            task.setCallback(callback);
            task.setCallback(mycallback);
            task.setContentValues(values);
            task.setTable(myTable);
            task.setWhereClause(DBConf.CONTACTID + " = ? ");
            task.setWhereArgs(new String[]{id});
            task.setOpertionStatus(CustomAsyncTask.OPERATE_UPDATE);
            task.setContext(mContext);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "");
            } else {
                task.execute("");
            }
        } catch (Exception e) {
            CustomLog.e(TAG, "更新好友表删除状态   Exception " + e);
        }
    }

    public void registerUpdateListener(IContactListChanged listener) {
        CustomLog.i(TAG, "registerLoadListener");
        if (iContactListChanged == null) {
            iContactListChanged = new ArrayList<IContactListChanged>();
        }
        if (listener != null && !iContactListChanged.contains(listener)) {
            iContactListChanged.add(listener);
        }
    }

    // 推荐页面和主页面销毁时要调用这个接口，否则一直增加，内存泄露
    public void unRegisterUpdateListener(IContactListChanged listener) {
        if (iContactListChanged != null && iContactListChanged.size() > 0
                && listener != null && iContactListChanged.contains(listener)) {
            iContactListChanged.remove(listener);
        }
    }

    /**
     * 查询所有联系人（无序）
     *
     * @return 所有联系人列表 还要排序
     */
    @Override
    public void getAllContacts(ContactCallback callback,
                               boolean isNeedCustomerService) {
        String sql = "select * from " + myTable
                + " where isDeleted= 0 and accountType= 0  order by fullPym ";
        CustomLog.e(TAG, "getAllContacts " + sql);
        CustomAsyncTask task = new CustomAsyncTask();
        task.setCallback(callback);
        task.setTable(myTable);
        task.setCustomerServiceType(isNeedCustomerService);
        task.setOpertionStatus(CustomAsyncTask.OPERATE_RAWQUERY);
        task.setContext(mContext);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, sql);
        } else {
            task.execute(sql);
        }
    }

    /**
     * 查询组信息
     *
     * @param callback
     */
    public void getAllGroups(ContactCallback callback) {
        String sql = "select * from " + myTable
                + " where isDeleted= 0 and accountType= 1 order by fullPym ";
        CustomLog.e(TAG, "getAllGroups " + sql);
        CustomAsyncTask task = new CustomAsyncTask();
        task.setCallback(callback);
        task.setTable(myTable);
        task.setCustomerServiceType(false);
        task.setOpertionStatus(CustomAsyncTask.OPERATE_RAWQUERY);
        task.setContext(mContext);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, sql);
        } else {
            task.execute(sql);
        }
    }

    public void clearInfos() {
        if (dataSync != null) {
            dataSync.cancle();
            dataSync = null;
        }
        RecommendManager.getInstance(mContext).clearInfos();
        ContactDBOperater.getInstance(mContext).release();
    }

    private String checkIsNull(String str) {
        if (str != null) {
            return str;
        } else {
            return "";
        }
    }

    public Contact getContactInfoByNubeNumber(String nubenumber) {

        Contact contact = new Contact();

        contact.setNumber(select("number",nubenumber));
        contact.setUserFrom(select2("userFrom",nubenumber));
        contact.setHeadUrl(select("headUrl",nubenumber));
        contact.setEmail(select("email",nubenumber));
        contact.setContactId(select("contactId",nubenumber));
        contact.setNickname(select("nickname",nubenumber));
        contact.setAppType(select("appType",nubenumber));
        contact.setWorkUnit(select("workUnit",nubenumber));
        contact.setWorkUnitType(select2("workUnitType",nubenumber));
        contact.setDepartment(select("department",nubenumber));
        contact.setProfessional(select("professional",nubenumber));
        contact.setOfficeTel(select("officeTel",nubenumber));

        return contact;
    }

    private String select(String paramater,String nubeNumber){
        String result="";

        String sql = "select "+ paramater +" from " + myTable + " where nubeNumber = '"
                + nubeNumber + "' and isDeleted = 0 ";

        Cursor c = ContactDBOperater.getInstance(mContext).rawQuery(sql, myTable);
        if (c != null && c.moveToNext()) {
            result = c.getString(0);
        }
        if (c != null) {
            c.close();
        }

        return result;
    }

    private int select2(String paramater,String nubeNumber){
        int result = 6;

        String sql = "select "+ paramater +" from " + myTable + " where nubeNumber = '"
                + nubeNumber + "' and isDeleted = 0 ";

        Cursor c = ContactDBOperater.getInstance(mContext).rawQuery(sql, myTable);
        if (c != null && c.moveToNext()) {
            result = c.getInt(0);
        }
        if (c != null) {
            c.close();
        }

        return result;
    }



}
