package com.kunxun.intervalalarmclock;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.CursorAdapter;
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

    private LayoutInflater mFactory;
    private Cursor mCursor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mFactory = LayoutInflater.from(this);
        mCursor = Alarms.getAlarmCursor(getContentResolver());
        updateLayout();
    }


    private void updateLayout() {
        mListView = findViewById(R.id.mListView);

        AlarmAdapter adapter = new AlarmAdapter(this, mCursor);
        mListView.setAdapter(adapter);

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Toast.makeText(getApplicationContext(),"ItemClick", Toast.LENGTH_LONG).show();
            }
        });

    }

//    private List<HashMap<String, Object>> getData(){
//        List<HashMap<String, Object>> list = new ArrayList<>();
//
//        for(int i=0;i<4;i++)
//        {
//            HashMap<String, Object> map = new HashMap<>();
//            map.put("itemTitle", "Alarm"+i);
//            map.put("itemText","Working Day");
//            map.put("itemEnabled", true);
//            list.add(map);
//        }
//
//        return list;
//    }


    private class AlarmAdapter extends CursorAdapter{
        public AlarmAdapter(Context context, Cursor cursor){
            super(context,cursor);
        }

        @Override
        public View newView(Context context,Cursor cursor, ViewGroup parent){
            View ret = mFactory.inflate(R.layout.list_alarm, parent, false);
            return ret;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {

            final Alarm alarm = new Alarm(cursor);
            TextView itemTitle = view.findViewById(R.id.Item_Title);
            String titleStr = alarm.starthour + ":" + alarm.startminutes;
            if (alarm.intervalenabled) {
                titleStr += "~" + alarm.endhour + ":" + alarm.endminutes + " ";
                titleStr += alarm.interval + " minutes interval";
            }

            itemTitle.setText(titleStr);

            TextView itemText = view.findViewById(R.id.Item_Text);
            String daysofweek = alarm.daysOfWeek.toString(MainActivity.this, false);
            itemText.setText(alarm.message + ", " + daysofweek);
            Switch itemEnabled = view.findViewById(R.id.Item_Enabled);
            itemEnabled.setChecked(alarm.enabled);
            itemEnabled.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
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
        }
    }


//    private class alarmAdapter extends BaseAdapter{
//        @Override
//        public int getCount()
//        {
//            return listData.size();
//        }
//
//        @Override
//        public Object getItem(int position){
//            return listData.get(position);
//        }
//
//        @Override
//        public long getItemId(int position){
//            return position;
//        }
//
//        @Override
//        public View getView(int position, View v, ViewGroup parent) {
//            View view;
//            ViewHolder viewHolder;
//            if (v == null) {
//                view = getLayoutInflater().inflate(R.layout.list_alarm, null);
//                viewHolder = new ViewHolder();
//                viewHolder.itemTitle= view.findViewById(R.id.Item_Title);
//                viewHolder.itemText = view.findViewById(R.id.Item_Text);
//                viewHolder.itemEnabled = view.findViewById(R.id.Item_Enabled);
//                view.setTag(viewHolder);
//            } else {
//                view = v;
//                viewHolder = (ViewHolder) view.getTag();
//            }
//
//            viewHolder.itemTitle.setText(listData.get(position).get("itemTitle").toString());
//            viewHolder.itemText.setText(listData.get(position).get("itemText").toString());
//            viewHolder.itemEnabled.setChecked((boolean)listData.get(position).get("itemEnabled"));
//
//            viewHolder.itemEnabled.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//                @Override
//                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
//                    if(b)
//                    {
//                        Toast.makeText(getApplicationContext(),"checked",Toast.LENGTH_LONG).show();
//                    }
//                    else
//                    {
//                        Toast.makeText(getApplicationContext(),"unchecked",Toast.LENGTH_LONG).show();
//                    }
//
//                }
//            });
//
//            return view;
//        }
//    }

//    private class ViewHolder{
//        private TextView itemTitle;
//        private TextView itemText;
//        private Switch itemEnabled;
//    }

}
