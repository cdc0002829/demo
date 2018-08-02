package cn.redcdn.hvs.im.asyncTask;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import cn.redcdn.hvs.MedicalApplication;
import cn.redcdn.hvs.im.MessageFragment;
import cn.redcdn.hvs.im.dao.GroupDao;
import cn.redcdn.hvs.im.dao.NoticesDao;
import cn.redcdn.hvs.im.dao.ThreadsDao;

/**
 * <dl>
 * <dt>QueryNoticeAsyncTask.java</dt>
 * <dd>Description:动态消息异步查询任务</dd>
 *
 * @author zhaguitao
 */
public class QueryNoticeAsyncTask extends
    NetPhoneAsyncTask<String, String, Cursor> {

    private NoticesDao noticesDao;
    private NoticesDao dao = null;

    private ThreadsDao threadDao = null;

    private GroupDao groupDao = null;

    //    private int vcardCnt = 0;

    //    private Map<String, String> nubeNamesMap = null;

    // 群聊相关信息 gid_gName: 群号-群名称
    //    private Map<String, String> gNamesMap = null;

    //    // 群号 - 群成员名称列表
    //    private Map<String,Map<String,String>>gid_memberNames=null;

    private QueryTaskPostListener listener = null;
    private int noticeCount;


    public QueryNoticeAsyncTask(Context context) {
        dao = new NoticesDao(context);
        threadDao = new ThreadsDao(context);
        groupDao = new GroupDao(context);
    }


    public void setNoticesDao(NoticesDao noticesDao) {
        this.noticesDao = noticesDao;
    }


    @Override
    protected Cursor doInBackground(String... args) {
        boolean updateIsNew = true;

        if (args != null && args.length > 0) {
            String currentTag = args[0];
            if (!MessageFragment.class.getName().equals(currentTag)) {
                // 当前标签不是动态列表时，不更新isnew字段
                updateIsNew = false;
            }
        }

        if (updateIsNew) {
            // 更新所有消息isnew为非新消息
            //            dao.updateReadStatus();
        }

        //        vcardCnt = dao.countVcardNotice();

        //产品要求按照ShowNameUtil中的显示规则显示名字,暂时去掉这种查询出所有名字的方法--add on 2015/6/29

        // 查询所有nube好友
        //        Cursor nubeNameCursor = dao.queryNubeNames();
        //        if (nubeNameCursor != null && nubeNameCursor.getCount() > 0) {
        //            nubeNamesMap = new HashMap<String, String>();
        //            while (nubeNameCursor.moveToNext()) {
        //                nubeNamesMap.put(nubeNameCursor.getString(0),
        //                        nubeNameCursor.getString(1));
        //            }
        //        }
        //        if (nubeNameCursor != null) {
        //            nubeNameCursor.close();
        //            nubeNameCursor = null;
        //        }

        if(noticesDao != null){
            noticeCount = noticesDao.getNewNoticeCount();
            Intent intent = new Intent("NoticeCountBroaddcase");
            intent.putExtra("newNoticeCount", noticeCount);
            MedicalApplication.getContext().sendBroadcast(intent);
        }
        return threadDao.getAllThreadsInfo();
    }


    @Override
    protected void onPostExecute(Cursor cursor) {
        super.onPostExecute(cursor);

        if (listener != null) {
            if (cursor == null) {
                listener.onQueryFailure();
            } else {
                listener.onQuerySuccess(cursor);
            }
        }
    }


    public void setQueryTaskListener(QueryTaskPostListener listener) {
        this.listener = listener;
    }


    public interface QueryTaskPostListener {
        public void onQuerySuccess(Cursor cursor);

        public void onQueryFailure();
    }

    /**
     * 更新 titlebar 未读消息数
     */

}
