package cn.redcdn.hvs.contacts;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.ImageLoader;

import cn.redcdn.buteldataadapter.DataAdapter;
import cn.redcdn.hvs.MedicalApplication;
import cn.redcdn.hvs.R;
import cn.redcdn.hvs.contacts.contact.diaplayImageListener.DisplayImageListener;
import cn.redcdn.hvs.contacts.contact.interfaces.Contact;

public class ContactsGroupChatListViewAdapter extends DataAdapter  {

    private Context context;
    private ViewHolder viewHolder;
    private String indexStr =  "ABCDEFGHIJKLMNOPQRSTUVWXYZ" ;

    private DisplayImageListener mDisplayImageListener = null;

    public ContactsGroupChatListViewAdapter(Context context) {
        super(context);
        this.context = context;

        mDisplayImageListener = new DisplayImageListener();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        Contact item = (Contact) getItem(position);
        String firstString=item.getFirstName();
        String preFirstString=null;
        String backFirstString=null;
        if(!indexStr.contains(firstString)){
            firstString="#";
        }
        if(position==0){
            preFirstString="#";
        }else{
            preFirstString=((Contact) getItem(position-1)).getFirstName();
            if(!indexStr.contains(preFirstString)){
                preFirstString="#";
            }
        }
        if(position+1<getCount()){
            backFirstString=((Contact) getItem(position+1)).getFirstName();
            if(!indexStr.contains(backFirstString)){
                backFirstString="#";
            }
        }
        viewHolder = null;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(
                    R.layout.contactitemindex, null);
            viewHolder = new ViewHolder();
            viewHolder.indexTv = (TextView) convertView
                    .findViewById(R.id.indexTv);
            viewHolder.contactItemIndex=(LinearLayout)convertView.findViewById(R.id.indexitem);
            viewHolder.itemTv = (TextView) convertView.findViewById(R.id.itemTv);
            viewHolder.headImage = (ImageView) convertView
                    .findViewById(R.id.headimage);
            viewHolder.contactItemLine = (LinearLayout) convertView
                    .findViewById(R.id.contact_item_line);
            viewHolder.deviceType = (ImageView) convertView
                    .findViewById(R.id.deviceimage);
            viewHolder.contactItem = (LinearLayout) convertView
                    .findViewById(R.id.ll_contact_item);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        if (position == 0) {
            viewHolder.indexTv.setVisibility(View.VISIBLE);
            viewHolder.itemTv.setVisibility(View.VISIBLE);
            viewHolder.contactItemIndex.setVisibility(View.VISIBLE);
            viewHolder.headImage.setVisibility(View.VISIBLE);
            viewHolder.contactItemLine.setVisibility(View.VISIBLE);
            viewHolder.deviceType.setVisibility(View.VISIBLE);
            viewHolder.indexTv.setText(firstString);
        }else if(!firstString.equals(preFirstString)){
            viewHolder.indexTv.setVisibility(View.VISIBLE);
            viewHolder.itemTv.setVisibility(View.VISIBLE);
            viewHolder.contactItemIndex.setVisibility(View.VISIBLE);
            viewHolder.headImage.setVisibility(View.VISIBLE);
            viewHolder.contactItemLine.setVisibility(View.VISIBLE);
            viewHolder.deviceType.setVisibility(View.VISIBLE);
            viewHolder.indexTv.setText(firstString);
        }else{
            viewHolder.indexTv.setVisibility(View.VISIBLE);
            viewHolder.itemTv.setVisibility(View.VISIBLE);
            viewHolder.headImage.setVisibility(View.VISIBLE);
            viewHolder.contactItemLine.setVisibility(View.VISIBLE);
            viewHolder.deviceType.setVisibility(View.VISIBLE);
            viewHolder.contactItemIndex.setVisibility(View.INVISIBLE);
            viewHolder.contactItemIndex.setVisibility(View.GONE);
        }

        if(position>0&&((Contact) getItem(position)).getNubeNumber()
                .equals(((Contact) getItem(position-1)).getNubeNumber())){
            viewHolder.contactItem.setVisibility(View.INVISIBLE);
            viewHolder.contactItem.setVisibility(View.GONE);
        } else {
            viewHolder.contactItem.setVisibility(View.VISIBLE);
        }

        if (item.getAppType() != null) {
            if (item.getAppType().equals("mobile")
                    || item.getAppType().isEmpty()) {
                viewHolder.deviceType.setBackgroundDrawable(context
                        .getResources().getDrawable(R.drawable.devicetele));

            } else if (item.getAppType().equals("n8")) {
                viewHolder.deviceType.setBackgroundDrawable(context
                        .getResources().getDrawable(R.drawable.devicen8));
            } else {
                viewHolder.deviceType.setBackgroundDrawable(context
                        .getResources().getDrawable(R.drawable.devicem1));



            }
        }
        viewHolder.contactItemLine.setVisibility(View.VISIBLE);
        if(position+1<getCount()){
            if(!firstString.equals(backFirstString)){
                viewHolder.contactItemLine.setVisibility(View.VISIBLE);
            }
        }
        if (position + 1 == getCount()) {
            viewHolder.contactItemLine.setVisibility(View.VISIBLE);
        }

        ImageLoader imageLoader = ImageLoader.getInstance();

        imageLoader.displayImage(item.getPicUrl(),
                viewHolder.headImage,
                MedicalApplication.shareInstance().options,
                mDisplayImageListener);

        viewHolder.itemTv.setText(item.getNickname());

        viewHolder.deviceType.setVisibility(View.INVISIBLE);

        return convertView;
    }

    private class ViewHolder {
        private TextView indexTv;
        private TextView itemTv;
        private ImageView headImage;
        private ImageView deviceType;
        private LinearLayout contactItemLine;
        private LinearLayout contactItemIndex;
        private LinearLayout contactItem;

    }

}
