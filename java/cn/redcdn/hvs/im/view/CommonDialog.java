package cn.redcdn.hvs.im.view;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnDismissListener;
import android.graphics.Color;
import android.text.TextUtils;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import cn.redcdn.hvs.R;
import com.butel.connectevent.utils.CommonUtil;
import com.butel.connectevent.utils.LogUtil;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <dl>
 * <dt>CommonDialog.java</dt>
 * <dd>Description:家事通自定义通用对话框</dd>
 * <dd>Copyright: Copyright (C) 2011</dd>
 * <dd>Company: 安徽青牛信息技术有限公司</dd>
 * <dd>CreateDate: 2014-1-16 上午10:09:19</dd>
 * </dl>
 * 
 * @author lihs
 */
public class CommonDialog {

    // 网络连接设置对话框
    public static final int DIALOG_TYPE_NETWORK = 1;
    // 强制登录对话框
    public static final int DIALOG_TYPE_FORCE_LOGIN = 2;
    // 退出应用对话框
    public static final int DIALOG_TYPE_LOGOUT = 3;
    // APK下载空间不足对话框
    public static final int DIALOG_TYPE_ULTRA_SPACE = 4;
    // APK下载失败对话框
    public static final int DIALOG_TYPE_APK_DOWNLOAD_FAIL = 5;

    private View contentView;
    private View selfDifineView;
    private Dialog dialog;

    // 确定按钮之后是否关闭对话框
    private boolean stillShow = false;
    private boolean canceledOnTouchOutside = false;

    private Context context;
    private SparseArray<Button> btns = null;

    private OnDismissListener dismissListener = null;

    private String activityName;

    // 此map维护了应用全部对话框，每个界面对应一个栈，
    // 即控制每个界面不能连续弹出两次相同类型的对话框，不能同时弹出两个及以上对话框
    private static Map<String, ActivityDialogStack> activityDlgStkMap = new ConcurrentHashMap<String, ActivityDialogStack>();

    public static void clearStkByActivityName(String activityName) {
        activityDlgStkMap.remove(activityName);
    }

    public CommonDialog(Context context, final String activityName, int mtype) {
        // LogUtil.begin("activityName:" + activityName + "|mtype:" + mtype);
        this.context = context;
        this.activityName = activityName;

        btns = new SparseArray<Button>();
        dialog = new Dialog(context, R.style.CustomProgressDialog);
        contentView = LayoutInflater.from(context).inflate(
                R.layout.common_dialog_layout, null);

        LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT);
        dialog.addContentView(contentView, params);
        setTitle(null);
        dialog.setCanceledOnTouchOutside(false);
        dialog.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,
                KeyEvent.KEYCODE_BACK));
        dialog.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP,
                KeyEvent.KEYCODE_BACK));
        dialog.setOnDismissListener(new OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                if (dismissListener != null) {
                    dismissListener.onDismiss(dialog);
                }
                LogUtil.d("CommonDialog,OnDismissListener");

                // 对话框关闭时，初始化disDialogType为-1，以便打开栈中下一个对话框
                ActivityDialogStack actDlgStack = activityDlgStkMap
                        .get(activityName);
                if (actDlgStack != null) {
                    actDlgStack.disDialogType = -1;
                    activityDlgStkMap.put(activityName, actDlgStack);

                    showDialog();
                }
            }
        });

        ActivityDialogStack actDlgStack = activityDlgStkMap.get(activityName);
        if (actDlgStack == null) {
            actDlgStack = new ActivityDialogStack();
        }
        if (actDlgStack.disDialogType != mtype
                && !actDlgStack.typeStack.contains(mtype + "")) {
            LogUtil.d("CommonDialog activityName: + " + activityName + "|type:"
                    + mtype);
            actDlgStack.dialogStack.push(this);
            actDlgStack.typeStack.push(mtype + "");
        }
        activityDlgStkMap.put(activityName, actDlgStack);
    }

    public Dialog getDialog() {
        return dialog;
    }

    public SparseArray<Button> getBtns() {
        return btns;
    }

    public View getContentView() {
        return contentView;
    }

    public void setCancelable(boolean cancelable) {
        if (dialog != null) {
            dialog.setCancelable(cancelable);
        }
    }

    public void setOnDismissListener(OnDismissListener _linstener) {
        dismissListener = _linstener;
    }

    public boolean isShowing() {
        if (dialog != null) {
            return dialog.isShowing();
        } else {
            return false;
        }
    }

    public void dismiss() {
        if (dialog != null) {
            dialog.dismiss();
        }
    }

    public void setTitle(int id) {
        if (id < 0) {
            // contentView.findViewById(R.id.tv_title).setVisibility(View.GONE);
            // contentView.findViewById(R.id.line1).setVisibility(View.GONE);
            TextView titleTv = (TextView) contentView
                    .findViewById(R.id.tv_title);
            titleTv.setText(R.string.commomdialog_title);
        } else {
            TextView titleTv = (TextView) contentView
                    .findViewById(R.id.tv_title);
            titleTv.setText(id);
//            titleTv.setVisibility(View.VISIBLE);
//            contentView.findViewById(R.id.line1).setVisibility(View.VISIBLE);
        }
    }

    public void setTitle(String title) {
        if (TextUtils.isEmpty(title)) {
            // contentView.findViewById(R.id.tv_title).setVisibility(View.GONE);
            // contentView.findViewById(R.id.line1).setVisibility(View.GONE);
            TextView titleTv = (TextView) contentView
                    .findViewById(R.id.tv_title);
            titleTv.setText(R.string.commomdialog_title);
        } else {
            TextView titleTv = (TextView) contentView
                    .findViewById(R.id.tv_title);
            titleTv.setText(title);
//            titleTv.setVisibility(View.VISIBLE);
//            contentView.findViewById(R.id.line1).setVisibility(View.VISIBLE);
        }
    }

    public void setTilteGravity(int gravity) {
        TextView titleTv = (TextView) contentView.findViewById(R.id.tv_title);
        titleTv.setGravity(gravity);
    }

    public void setMessage(int id) {
    	
        if (id < 0) {
            contentView.findViewById(R.id.lt_difine).setVisibility(View.GONE);
        } else {
            
            contentView.findViewById(R.id.lt_difine)
                    .setVisibility(View.VISIBLE);
            contentView.findViewById(R.id.tv_message).setVisibility(View.VISIBLE);
            ((TextView) contentView.findViewById(R.id.tv_message)).setText(id);
            updateMsgTextViewGravity();         
        }
    }

    public void setMessage(String msg) {
        if (TextUtils.isEmpty(msg)) {
            contentView.findViewById(R.id.lt_difine).setVisibility(View.GONE);
        } else {

            contentView.findViewById(R.id.lt_difine)
                    .setVisibility(View.VISIBLE);
            contentView.findViewById(R.id.tv_message).setVisibility(View.VISIBLE);
            ((TextView) contentView.findViewById(R.id.tv_message)).setText(msg);
            updateMsgTextViewGravity();
        }
    }

    
    private void updateMsgTextViewGravity(){
        String txt = ((TextView) contentView.findViewById(R.id.tv_message)).getText().toString();
        int length = (int) CommonUtil.getLength(txt);
//      int lineCount = ((TextView) contentView.findViewById(R.id.tv_message))
//                .getLineCount();
        if(length<=14){
                    ((TextView) contentView.findViewById(R.id.tv_message))
                            .setGravity(Gravity.CENTER);
                } else {
                    ((TextView) contentView.findViewById(R.id.tv_message))
                            .setGravity(Gravity.LEFT);
                }
        }

    /**
     * 设置对话框正文字体大小
     * 
     * @param txtSize
     *            字体大小，单位sp
     */
    public void setMessageTxtSize(int txtSize) {

        ((TextView) contentView.findViewById(R.id.tv_message))
                .setTextSize(txtSize);
    }

    public void setMessageForPrivate(int id) {
        contentView.findViewById(R.id.lt_difine).setVisibility(View.VISIBLE);
        ((TextView) contentView.findViewById(R.id.tv_message)).setText(id);
        contentView.findViewById(R.id.et_verify).setVisibility(View.VISIBLE);

    }

    public void setMessageForPrivate(String msg) {
        contentView.findViewById(R.id.lt_difine).setVisibility(View.VISIBLE);
        ((TextView) contentView.findViewById(R.id.tv_message)).setText(msg);
        contentView.findViewById(R.id.et_verify).setVisibility(View.VISIBLE);
    }

    public void setPositiveButton(final BtnClickedListener btnOk, String text) {
    	
    	contentView.findViewById(R.id.line2).setVisibility(View.VISIBLE);
        Button sureBtn = (Button) contentView.findViewById(R.id.confirm_btn);
        sureBtn.setVisibility(View.VISIBLE);
        sureBtn.setText(text);
        btns.put(1, sureBtn);
        sureBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (btnOk != null) {
                    btnOk.onBtnClicked();
                }
                if (dialog != null && !stillShow) {
                    dialog.dismiss();
                    dialog = null;
                }
            }
        });
    }

    public void setPositiveButton(final BtnClickedRespListener btnLstner,
            String text) {

        Button sureBtn = (Button) contentView.findViewById(R.id.confirm_btn);
        sureBtn.setVisibility(View.VISIBLE);
        sureBtn.setText(text);
        btns.put(1, sureBtn);
        sureBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (btnLstner != null) {
                    if (btnLstner.onBtnClicked()) {
                        // 正常处理完成，关闭dialog，否则不自动关闭dialog
                        if (dialog != null && !stillShow) {
                            dialog.dismiss();
                            dialog = null;
                        }
                    }
                }
            }
        });
    }

    public void setPositiveButton(final BtnClickedListener btnOk, int id) {
        setPositiveButton(btnOk, context.getResources().getString(id));
    }

    public void setCancleButton(final BtnClickedListener cancleOk, String text) {
    	
    	contentView.findViewById(R.id.line2).setVisibility(View.VISIBLE);
        Button canBtn = (Button) contentView.findViewById(R.id.cancel_btn);
        canBtn.setVisibility(View.VISIBLE);
        canBtn.setText(text);
        btns.put(0, canBtn);
        canBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (dialog != null) {
                    dialog.dismiss();
                    dialog = null;
                }
                if (cancleOk != null) {
                    cancleOk.onBtnClicked();
                }
            }
        });
    }

    public void setCancleButton(final BtnClickedListener cancleOk, int id) {
        setCancleButton(cancleOk, context.getResources().getString(id));
    }

    public void addView(View view) {
        if (contentView != null) {
            LinearLayout addLt = (LinearLayout) contentView
                    .findViewById(R.id.lt_difine);
            addLt.removeAllViews();
            selfDifineView = view;
            LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT,
                    LayoutParams.WRAP_CONTENT);
            addLt.addView(selfDifineView, params);
        }
    }

    // 添加自定义的 布局
    public void addSpecialView(View view) {
        if (contentView != null) {
            LinearLayout addLt = (LinearLayout) contentView
                    .findViewById(R.id.lt_difine);
            addLt.removeAllViews();
            selfDifineView = view;
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, 0, 0);
            addLt.setLayoutParams(lp);
            addLt.addView(selfDifineView);
        }
    }

    // 添加自定义的 布局
    public void addSpecialView(int viewId) {
        if (contentView != null) {
            LinearLayout addLt = (LinearLayout) contentView
                    .findViewById(R.id.lt_difine);
            addLt.removeAllViews();
            selfDifineView = LayoutInflater.from(context).inflate(viewId, null);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, 0, 0);
            addLt.setLayoutParams(lp);
            addLt.addView(selfDifineView);
        }
    }

    // 添加自定义的 布局
    public void addView(int viewId) {
        if (contentView != null) {
            LinearLayout addLt = (LinearLayout) contentView
                    .findViewById(R.id.lt_difine);
            addLt.removeAllViews();
            selfDifineView = LayoutInflater.from(context).inflate(viewId, null);
            LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT,
                    LayoutParams.WRAP_CONTENT);
            addLt.addView(selfDifineView, params);
        }
    }

    public View getSelfDifineView() {
        return selfDifineView;
    }

    public void showDialog() {
        LogUtil.begin("showDialog,activityName:" + activityName);
        ActivityDialogStack actDlgStk = activityDlgStkMap.get(activityName);
        if (actDlgStk != null) {
            if (!actDlgStk.dialogStack.empty()) {
                // 当前是否有对话框正在显示，如果有后续对话框不弹出
                if (actDlgStk.disDialogType > -1) {
                    LogUtil.d("当前有对话框正在显示，后续对话框不弹出");
                    return;
                }
                CommonDialog dlg = actDlgStk.dialogStack.pop();
                if (dlg.getBtns().size() > 0) {
                    if (dlg.getBtns().size() == 2) {
                        dlg.getContentView().findViewById(R.id.line3)
                                .setVisibility(View.VISIBLE);
                    } else if (dlg.getBtns().size() == 1) {
                        Button cancelBtn = dlg.getBtns().get(0);
                        if (cancelBtn != null) {
                            cancelBtn
                                    .setBackgroundResource(R.drawable.bg_common_dialog_btn);
                        }
                    }
                }
                dlg.getDialog().setCanceledOnTouchOutside(
                        canceledOnTouchOutside);
                dlg.getDialog().show();

                if (!actDlgStk.typeStack.isEmpty()) {
                    actDlgStk.disDialogType = Integer
                            .parseInt(actDlgStk.typeStack.pop());
                } else {
                    actDlgStk.disDialogType = -1;
                }
            }

            activityDlgStkMap.put(activityName, actDlgStk);
        }
    }

    public void setCloseButton(final BtnClickedListener listener) {

        RelativeLayout closeBtn = (RelativeLayout) contentView
                .findViewById(R.id.lt_close);
        closeBtn.setVisibility(View.VISIBLE);
        TextView tv = (TextView) contentView.findViewById(R.id.tv_title);
        tv.setFocusable(true);
        tv.setClickable(true);
        tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (dialog != null) {
                    dialog.dismiss();
                    dialog = null;
                }
                if (listener != null) {
                    listener.onBtnClicked();
                }
            }
        });
    }

    public interface BtnClickedListener {
        public void onBtnClicked();
    }

    public interface BtnClickedRespListener {
        public boolean onBtnClicked();
    }

    public void setOnCancelListener(OnCancelListener listener) {
        if (dialog != null) {
            dialog.setOnCancelListener(listener);
        }
    }

    public class ActivityDialogStack {
        // 当前显示对话框的类型
        public int disDialogType = -1;
        // 对话框栈
        public Stack<CommonDialog> dialogStack = new Stack<CommonDialog>();
        // 对话框的类型栈
        public Stack<String> typeStack = new Stack<String>();
    }

    public boolean isStillShow() {
        return stillShow;
    }

    public void setStillShow(boolean stillShow) {
        this.stillShow = stillShow;
    }

    /**
     * 
     * Description:是否允许点击窗口外区域，隐藏弹出框
     * 
     * @param tag
     *            ture:允许
     */
    public void setCanceledOnTouchOutside(boolean tag) {
        this.canceledOnTouchOutside = tag;
    }

    
    public void setCommondialogDrawal() {
        ImageView tv = (ImageView) contentView.findViewById(R.id.tv_close);
        tv.setVisibility(View.VISIBLE);
        tv.setFocusable(true);
        tv.setClickable(true);
        tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (dialog != null) {
                    dialog.dismiss();
                    dialog = null;
                }
            }
        });
    }

	public void setPositiveButtonChat(final BtnClickedListener btnOk, int id) {
		setPositiveButtonChat(btnOk, context.getResources().getString(id));
	}

	public void setPositiveButtonChat(final BtnClickedListener btnOk,
			String text) {
		contentView.findViewById(R.id.line2).setVisibility(View.VISIBLE);
		Button sureBtn = (Button) contentView.findViewById(R.id.confirm_btn);
		sureBtn.setVisibility(View.VISIBLE);
		sureBtn.setTextColor(Color.parseColor("#3eaaaf"));
		sureBtn.setText(text);
		btns.put(1, sureBtn);
		sureBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (btnOk != null) {
					btnOk.onBtnClicked();
				}
				if (dialog != null && !stillShow) {
					dialog.dismiss();
					dialog = null;
				}
			}
		});
	}

	public void setTitleChat(int id) {
		TextView titleTv = (TextView) contentView.findViewById(R.id.tv_title);
		contentView.findViewById(R.id.lt_difine).setMinimumHeight(230);
		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
				RelativeLayout.LayoutParams.WRAP_CONTENT,
				RelativeLayout.LayoutParams.WRAP_CONTENT);
		params.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
		titleTv.setPadding(20, 10, 0, 10);
		titleTv.setLayoutParams(params);
		if (id < 0) {
			// contentView.findViewById(R.id.tv_title).setVisibility(View.GONE);
			// contentView.findViewById(R.id.line1).setVisibility(View.GONE);
			titleTv.setText(R.string.commomdialog_title);
		} else {
			titleTv.setText(id);
			titleTv.setVisibility(View.VISIBLE);
			contentView.findViewById(R.id.line1).setVisibility(View.VISIBLE);
		}
	}
	
	public void showDialogTitle() {
		TextView titleTv = (TextView) contentView.findViewById(R.id.tv_title);
		contentView.findViewById(R.id.lt_difine).setMinimumHeight(230);
		titleTv.setVisibility(View.VISIBLE);
		contentView.findViewById(R.id.line1).setVisibility(View.VISIBLE);
	}
}
