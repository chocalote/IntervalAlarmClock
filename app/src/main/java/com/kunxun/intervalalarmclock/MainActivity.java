package com.kunxun.intervalalarmclock;

import android.app.Activity;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends Activity {

    private ListView mListView;
    private List<HashMap<String, Object>> listData;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        updateLayout();
    }


    private void updateLayout() {
        mListView = findViewById(R.id.mListView);
        listData = getData();
        mListView.setAdapter(new alarmAdapter());

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Toast.makeText(getApplicationContext(),"ItemClick", Toast.LENGTH_LONG).show();
            }
        });

    }

    private List<HashMap<String, Object>> getData(){
        List<HashMap<String, Object>> list = new ArrayList<>();

        for(int i=0;i<4;i++)
        {
            HashMap<String, Object> map = new HashMap<>();
            map.put("itemTitle", "Alarm"+i);
            map.put("itemText","Working Day");
            map.put("itemEnabled", true);
            list.add(map);
        }

        return list;
    }


    private class alarmAdapter extends BaseAdapter{
        @Override
        public int getCount()
        {
            return listData.size();
        }

        @Override
        public Object getItem(int position){
            return listData.get(position);
        }

        @Override
        public long getItemId(int position){
            return position;
        }

        @Override
        public View getView(int position, View v, ViewGroup parent) {
            View view;
            ViewHolder viewHolder;
            if (v == null) {
                view = getLayoutInflater().inflate(R.layout.list_alarm, null);
                viewHolder = new ViewHolder();
                viewHolder.itemTitle= view.findViewById(R.id.Item_Title);
                viewHolder.itemText = view.findViewById(R.id.Item_Text);
                viewHolder.itemEnabled = view.findViewById(R.id.Item_Enabled);
                view.setTag(viewHolder);
            } else {
                view = v;
                viewHolder = (ViewHolder) view.getTag();
            }

            viewHolder.itemTitle.setText(listData.get(position).get("itemTitle").toString());
            viewHolder.itemText.setText(listData.get(position).get("itemText").toString());
            viewHolder.itemEnabled.setChecked((boolean)listData.get(position).get("itemEnabled"));

            viewHolder.itemEnabled.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    if(b)
                    {
                        Toast.makeText(getApplicationContext(),"checked",Toast.LENGTH_LONG).show();
                    }
                    else
                    {
                        Toast.makeText(getApplicationContext(),"unchecked",Toast.LENGTH_LONG).show();
                    }

                }
            });

            return view;
        }
    }

    private class ViewHolder{
        private TextView itemTitle;
        private TextView itemText;
        private Switch itemEnabled;
    }

}
