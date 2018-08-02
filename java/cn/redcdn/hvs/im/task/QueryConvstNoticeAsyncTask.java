package cn.redcdn.hvs.im.task;

import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.text.TextUtils;

import com.butel.connectevent.utils.LogUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import cn.redcdn.hvs.im.IMConstant;
import cn.redcdn.hvs.im.agent.AppP2PAgentManager;
import cn.redcdn.hvs.im.asyncTask.NetPhoneAsyncTask;
import cn.redcdn.hvs.im.bean.NoticesBean;
import cn.redcdn.hvs.im.column.NoticesTable;
import cn.redcdn.hvs.im.dao.NoticesDao;
import cn.redcdn.hvs.util.CustomToast;
import cn.redcdn.log.CustomLog;

/**
 * Desc    查询会话消息
 * Created by wangkai on 2017/2/24.
 */

public class QueryConvstNoticeAsyncTask extends NetPhoneAsyncTask<String, String, Integer> {


    /**
     * 全部查询
     */
    public static final int QUERY_TYPE_ALL = 1;
    /**
     * 条件查询
     */
    public static final int QUERY_TYPE_COND = 2;
    /**
     * 分页查询
     */
    public static final int QUERY_TYPE_PAGE = 3;

    private NoticesDao dao = null;
    private String convstId = "";
    private Cursor dataCursor = null;
    // 查询方式
    private int queryType = QUERY_TYPE_ALL;
    // 起始消息时间
    private long recvTimeBegin = 0l;
    // 分页查询条数
    private int pageCnt = IMConstant.NOTICE_PAGE_CNT;

    private QueryTaskPostListener listener = null;

    // private Map<String, String> nubeNamesMap = null;

    public QueryConvstNoticeAsyncTask(Context context,
                                      String convstId,
                                      int queryType,
                                      long recvTimeBegin,
                                      int pageCnt) {
        dao = new NoticesDao(context);
        this.convstId = convstId;
        this.queryType = queryType;
        this.recvTimeBegin = recvTimeBegin;
        this.pageCnt = pageCnt;
        dataCursor = dao.queryAllNotice(convstId);
        //dataCursor 返回可能为null 直接调用getCount会崩
        if (dataCursor != null && dataCursor.getCount() == 1 && this.recvTimeBegin >= 1) {
            this.recvTimeBegin = 1;
        }
        dataCursor = null;
    }

    @Override
    protected Integer doInBackground(String... params) {

        if (TextUtils.isEmpty(convstId)) {
            return 0;
        }

        try {
            // 更新该会话下所有消息isnew为非新消息  进入会话钱，更新改内容
            //设置消息已读
            setMsgRead();
            dao.updateNewStatusInConvst(convstId);
            switch (queryType) {
                case QUERY_TYPE_ALL:
                    // 全部查询
                    dataCursor = dao.queryConvstNoticesCursor(convstId);
                    break;
                case QUERY_TYPE_COND:
                    // 条件查询
                    dataCursor = dao.queryNotices(convstId, recvTimeBegin);
                    break;
                case QUERY_TYPE_PAGE:
                    // 分页查询
                    dataCursor = dao.queryPageNotices(convstId, recvTimeBegin, pageCnt);
                    break;
                default:
                    break;
            }
            return 0;
        } catch (Exception e) {
            LogUtil.e("Exception", e);
            return -1;
        }
    }


    private void setMsgRead() throws Exception {
        Cursor unReadCursor = null;
        ArrayList<NoticesBean> unReadMsgList = new ArrayList<NoticesBean>();
        try {
            unReadCursor = dao.getUnreadNotice(convstId);
            if (unReadCursor != null && unReadCursor.getCount() > 0) {
                unReadCursor.moveToFirst();
                do {
                    NoticesBean item = NoticesTable.pureUnReadCursor(unReadCursor);
                    unReadMsgList.add(item);
                } while (unReadCursor.moveToNext());
            }
        } catch (Exception e) {
            CustomLog.e("updateRunningTask2Fail", e.toString());
        } finally {
            if (unReadCursor != null) {
                unReadCursor.close();
                unReadCursor = null;
            }
        }
        HashMap<String,String> serverIds = new HashMap<String,String>();
        if(unReadMsgList.size() > 0){
            for(int i=0;i<unReadMsgList.size();i++){
                NoticesBean bean = unReadMsgList.get(i);
                if(serverIds.containsKey(bean.getServerId())){
                    String tmpStr = serverIds.get(bean.getServerId());
                    tmpStr = tmpStr + "," + bean.getId();
                    serverIds.put(bean.getServerId(),tmpStr);

                }else {
                    serverIds.put(bean.getServerId(),bean.getId());
                }
            }
        }
        AppP2PAgentManager.getInstance().markMsgRead(serverIds);
    }

    @Override
    protected void onPostExecute(Integer result) {
        super.onPostExecute(result);

        if (listener != null) {
            if (result < 0) {
                listener.onQueryFailure();
            } else {
                listener.onQuerySuccess(dataCursor);
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
}
