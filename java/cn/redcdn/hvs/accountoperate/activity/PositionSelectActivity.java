package cn.redcdn.hvs.accountoperate.activity;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ListView;

import java.util.ArrayList;

import cn.redcdn.hvs.R;
import cn.redcdn.hvs.accountoperate.adapter.DepartmentSelectAdapter;
import cn.redcdn.hvs.accountoperate.adapter.PositionSelectAdapter;
import cn.redcdn.hvs.accountoperate.info.Department;
import cn.redcdn.hvs.accountoperate.info.Position;

public class PositionSelectActivity extends AppCompatActivity {
    private ListView listView;
    private ArrayList<Position> arrayList;
    private PositionSelectAdapter adapter;
    private Position position;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_position_select);
        listView = (ListView) findViewById(R.id.position_list);
        arrayList = new ArrayList<Position>();
        init();
        adapter = new PositionSelectAdapter(arrayList, PositionSelectActivity.this);
        listView.setAdapter(adapter);

    }
    private void init(){
        for (int i = 0; i < 3; i++) {
            position = new Position();
            if (i == 0) {
                position.setChoose_Position("主任医生");
                arrayList.add(position);
            } else if (i == 1) {
                position.setChoose_Position("副主任医生");
                arrayList.add(position);
            } else if (i == 2) {
                position.setChoose_Position("主治医生");
                arrayList.add(position);
            }
        }

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intentback=new Intent(PositionSelectActivity.this,DoctorActivity.class);
        startActivity(intentback);
        finish();
    }
}
