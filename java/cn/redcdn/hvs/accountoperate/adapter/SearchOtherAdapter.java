package cn.redcdn.hvs.accountoperate.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

import cn.redcdn.datacenter.medicalcenter.data.MDSDepartmentInfoA;
import cn.redcdn.hvs.R;

/**
 * Created by Administrator on 2017/3/16.
 */

public class SearchOtherAdapter extends BaseAdapter {
    private List<MDSDepartmentInfoA> arrayList;
    private Context context;

    public SearchOtherAdapter(Context context, List<MDSDepartmentInfoA> arrayList) {
        this.arrayList = arrayList;
        this.context = context;
    }

    public void setArrayList(List<MDSDepartmentInfoA> arrayList) {
        this.arrayList = arrayList;
        this.notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return arrayList.size();
    }

    @Override
    public Object getItem(int i) {
        return arrayList.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        DepartmentSelectAdapter.ViewHolder holder = null;
        if (view == null) {
            view = LayoutInflater.from(context).inflate(R.layout.hospital_item, null);
            holder = new DepartmentSelectAdapter.ViewHolder();
            holder.textView = (TextView) view.findViewById(R.id.hospital_text);
            view.setTag(holder);
        } else {
            holder = (DepartmentSelectAdapter.ViewHolder) view.getTag();
        }
        MDSDepartmentInfoA department = arrayList.get(i);
        holder.textView.setText(department.getClass_a_departmentName());
        return view;
    }

    public static class ViewHolder {
        public TextView textView;
    }
}
